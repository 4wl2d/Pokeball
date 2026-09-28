package pokeball.runtime

import pokeball.kernel.Accept
import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.BallId
import pokeball.kernel.BallPort
import pokeball.kernel.Call
import pokeball.kernel.CallId
import pokeball.kernel.CallKey
import pokeball.kernel.Caller
import pokeball.kernel.Cancel
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.EffectClass
import pokeball.kernel.Notice
import pokeball.kernel.NotDoneReason
import pokeball.kernel.Outcome
import pokeball.kernel.Output
import pokeball.kernel.Port
import pokeball.kernel.Publish
import pokeball.kernel.Reject
import pokeball.kernel.Reply
import pokeball.kernel.ReplyPlan
import pokeball.kernel.RequestContext
import pokeball.kernel.RequestId
import pokeball.kernel.Step
import pokeball.kernel.Timer
import pokeball.kernel.Topic
import pokeball.kernel.UnknownReason

/**
 * The reference runtime for Pokeball Core 2.
 *
 * It enforces the kernel rules that do not depend on the Ball author:
 * serial processing per instance (R3), atomic commit (R4), commit before
 * dispatch (R5), exactly one completion per call (R6), outcome classification
 * by send point and effect class (R7), runtime-owned bounded retries with no
 * re-send of non-idempotent calls (R8), reply accounting (R9), and bounds (R11).
 *
 * Every method except [inspect] may be called from any thread; work is
 * executed on the [scheduler]'s loop. [inspect] reads committed state and is
 * meant for tests and single-threaded simulations.
 */
public class Runtime(
    composition: Composition,
    private val scheduler: Scheduler,
    private val store: Store,
    private val limits: Limits = Limits(),
    private val tracer: Tracer = Tracer.None,
) {
    private val balls = composition.balls.toMap()
    private val adapters: Map<String, BoundAdapter> = composition.adapters.toMap().let { bound ->
        if (Timer.port.name in bound) bound
        // A timer is Safe: after a Durable restart it is re-armed (a new attempt) to its original due time.
        else bound + (Timer.port.name to BoundAdapter(Timer.port, timerAdapter(scheduler), RetryPolicy(maxAttempts = Int.MAX_VALUE), Int.MAX_VALUE))
    }
    private val subscriptions = composition.subscriptions.mapValues { it.value.toList() }

    init {
        composition.validate()
    }

    /** Distinguishes runtime starts; stale callbacks from an earlier incarnation are ignored. */
    public val incarnation: Long = store.nextIncarnation()

    @Volatile private var alive = true
    private var started = false
    private var externalSeq = 0L
    private val instances = HashMap<BallId, Instance>()
    private val inFlight = HashMap<String, Int>()
    private val waiting = HashMap<String, ArrayDeque<CallId>>()
    private val watchers = HashMap<BallId, MutableList<(Any?) -> Unit>>()

    // ---------------------------------------------------------------- public API

    /** Recover open work from the store and begin processing. Call once. */
    public fun start() {
        onLoop {
            check(!started) { "runtime already started" }
            started = true
            recover()
        }
    }

    /**
     * Simulate an abrupt crash: nothing more is processed and every pending
     * callback is ignored. Durable state is whatever the store already holds.
     */
    public fun crash() {
        alive = false
    }

    /**
     * Submit a request from outside the runtime (UI, HTTP handler, test).
     *
     * With an [idempotencyKey], repeats with an equal message join or replay the
     * original, and a repeat with a different message is refused with
     * [NotDoneReason.IdempotencyConflict].
     */
    public fun <M, R> request(
        target: BallId,
        message: M,
        timeoutMillis: Long,
        idempotencyKey: String? = null,
        caller: Caller = Caller.Anonymous,
        onOutcome: (Outcome<R>) -> Unit,
    ) {
        require(timeoutMillis > 0) { "timeout must be positive" }
        @Suppress("UNCHECKED_CAST")
        val callback = onOutcome as (Outcome<Any?>) -> Unit
        onLoop {
            val inst = instanceOrNull(target)
            if (inst == null) {
                callback(Outcome.NotDone(NotDoneReason.NotApplied("unknown Ball type '${target.type}'")))
                return@onLoop
            }
            val requestId = idempotencyKey?.let { RequestId("idem:$it") } ?: RequestId("ext:$incarnation:${externalSeq++}")
            val waiter = ExternalWaiter(callback)
            val input = Input.Request(
                requestId = requestId,
                message = message,
                caller = caller,
                deadlineAt = scheduler.now() + timeoutMillis,
                replyTo = ReplyTarget.External,
                dedup = idempotencyKey != null,
                waiter = waiter,
            )
            waiter.timer = scheduler.schedule(timeoutMillis) { onLoopNow { externalDeadline(inst, input, waiter) } }
            if (!inst.offer(input)) waiter.finish(refusal(inst))
        }
    }

    /** Observe committed states of [id]. The listener runs on the loop after each commit. */
    public fun watch(id: BallId, listener: (Any?) -> Unit) {
        onLoop { watchers.getOrPut(id) { mutableListOf() } += listener }
    }

    /** Committed state of [id], or `null` if the type is unknown. For tests and simulations. */
    public fun inspect(id: BallId): Any? = instanceOrNull(id)?.record?.state

    /** The full committed record of [id], if the instance was ever addressed. For tests. */
    public fun record(id: BallId): InstanceRecord? = instances[id]?.record ?: store.load(id)

    /** Whether an instance stopped after a fault in a completion or notice handler. */
    public fun failure(id: BallId): String? = instances[id]?.failed

    // ---------------------------------------------------------------- internals: instances

    private sealed interface Input {
        data class Request(
            val requestId: RequestId,
            val message: Any?,
            val caller: Caller,
            val deadlineAt: Long,
            val replyTo: ReplyTarget,
            val dedup: Boolean,
            val waiter: ExternalWaiter? = null,
        ) : Input

        data class CompletionInput(val key: CallKey, val port: Port<*, *>, val outcome: Outcome<Any?>) : Input

        data class NoticeInput(val topic: String, val source: BallId, val sequence: Long, val payload: Any?) : Input
    }

    private class ExternalWaiter(val callback: (Outcome<Any?>) -> Unit) {
        var done = false
        var timer: Cancellable? = null

        fun finish(outcome: Outcome<Any?>) {
            if (done) return
            done = true
            timer?.cancel()
            callback(outcome)
        }
    }

    private inner class Instance(val id: BallId, val ball: Ball<Any?, Any?, Any?>, var record: InstanceRecord) {
        val inbox = ArrayDeque<Input>()
        var drainScheduled = false
        var deciding = false
        var failed: String? = null
        val resolved = HashSet<CallKey>()
        val deadlineTimers = HashMap<CallKey, Cancellable>()
        val cancelHandles = HashMap<CallKey, Cancellable>()
        val currentAttempt = HashMap<CallKey, Int>()
        val externalWaiters = HashMap<RequestId, MutableList<ExternalWaiter>>()
        val noticesInFlight = HashSet<Pair<Long, BallId>>()
        var outboxRetryScheduled = false

        fun offer(input: Input): Boolean {
            if (failed != null || inbox.size >= limits.maxInbox) return false
            inbox.addLast(input)
            scheduleDrain()
            return true
        }

        fun scheduleDrain() {
            if (drainScheduled || failed != null) return
            drainScheduled = true
            scheduler.execute { onLoopNow { drainOne(this) } }
        }
    }

    private fun instanceOrNull(id: BallId): Instance? {
        instances[id]?.let { return it }
        val ball = balls[id.type] ?: return null
        val record = store.load(id) ?: InstanceRecord(id = id, state = ball.initial(id.key), revision = 0)
        return Instance(id, ball, record).also { instances[id] = it }
    }

    private fun drainOne(inst: Instance) {
        inst.drainScheduled = false
        if (inst.failed != null) return
        val input = inst.inbox.removeFirstOrNull() ?: return
        when (input) {
            is Input.Request -> handleRequest(inst, input)
            is Input.CompletionInput -> handleCompletion(inst, input)
            is Input.NoticeInput -> handleNotice(inst, input)
        }
        if (inst.inbox.isNotEmpty()) inst.scheduleDrain()
    }

    /** Runs a decider function under the serial-processing guard. Returns null on a fault. */
    private fun <T> decideGuarded(inst: Instance, what: String, block: () -> T): Result<T> {
        check(!inst.deciding) { "re-entrant decision on ${inst.id}" }
        inst.deciding = true
        tracer.record(TraceEvent.DecideStarted(scheduler.now(), inst.id, what))
        return try {
            Result.success(block())
        } catch (e: VirtualMachineError) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            inst.deciding = false
            tracer.record(TraceEvent.DecideFinished(scheduler.now(), inst.id))
        }
    }

    // ---------------------------------------------------------------- inputs

    private fun handleRequest(inst: Instance, input: Input.Request) {
        val now = scheduler.now()
        val existing = inst.record.inbound[input.requestId]
        if (existing != null) {
            when {
                input.replyTo == ReplyTarget.External && existing.message != input.message ->
                    input.waiter?.finish(Outcome.NotDone(NotDoneReason.IdempotencyConflict))
                existing.reply != null -> deliverReplyTo(inst, input, existing.reply)
                else -> input.waiter?.let { inst.externalWaiters.getOrPut(input.requestId) { mutableListOf() } += it }
            }
            return
        }
        if (input.waiter?.done == true) return // the external caller already timed out; withdrawn
        if (now > input.deadlineAt) {
            reject(inst, input, Outcome.NotDone(NotDoneReason.DeadlineBeforeSend))
            return
        }
        val ctx = RtRequestContext(inst.id, now, inst.record.revision + 1, input.requestId, input.caller)
        val result = decideGuarded(inst, "request ${input.requestId}") { inst.ball.decide(inst.record.state, input.message, ctx) }
        val decision: Decision<Any?, Any?> = result.getOrElse { e ->
            fault(inst, "decide threw ${e::class.simpleName}: ${e.message}")
            reject(inst, input, Outcome.NotDone(NotDoneReason.Fault("decide failed: ${e.message}")))
            return
        }
        when (decision) {
            is Reject -> reject(inst, input, Outcome.NotDone(NotDoneReason.Refused(decision.reason)))
            is Accept -> {
                val problem = preflight(inst, decision.outputs, currentRequest = input.requestId)
                if (problem != null) {
                    if (problem.overload) {
                        reject(inst, input, Outcome.NotDone(NotDoneReason.Overloaded))
                    } else {
                        fault(inst, problem.message)
                        reject(inst, input, Outcome.NotDone(NotDoneReason.Fault(problem.message)))
                    }
                    return
                }
                val immediate = (decision.reply as? ReplyPlan.Now)?.let { Outcome.Done(it.value) }
                val entry = InboundRecord(
                    requestId = input.requestId,
                    replyTo = input.replyTo,
                    message = input.message,
                    deadlineAt = input.deadlineAt,
                    reply = immediate,
                    repliedAt = immediate?.let { now },
                )
                val keepEntry = immediate == null || input.dedup
                input.waiter?.let { inst.externalWaiters.getOrPut(input.requestId) { mutableListOf() } += it }
                commit(inst, ctx, decision.state, decision.outputs, completed = null,
                    inboundEntry = if (keepEntry) entry else null, lastSeenUpdate = null,
                    immediateReply = input.requestId.takeIf { immediate != null })
                if (immediate != null) {
                    deliverReply(inst, input.requestId, input.replyTo, immediate)
                } else {
                    scheduler.schedule((input.deadlineAt - now).coerceAtLeast(0)) {
                        onLoopNow { checkReplyOverdue(inst, input.requestId) }
                    }
                }
            }
        }
    }

    private fun handleCompletion(inst: Instance, input: Input.CompletionInput) {
        val call = inst.record.calls[input.key]
        if (call == null) {
            tracer.record(TraceEvent.LateReportDropped(scheduler.now(), CallId(inst.id, input.key), "completion for closed call"))
            return
        }
        val ctx = RtContext(inst.id, scheduler.now(), inst.record.revision + 1)
        @Suppress("UNCHECKED_CAST")
        val completion = Completion(input.key, input.port as Port<*, Any?>, input.outcome)
        val result = decideGuarded(inst, "completion ${input.key}") { inst.ball.complete(inst.record.state, completion, ctx) }
        val step = result.getOrElse { e ->
            quarantine(inst, input, "complete threw ${e::class.simpleName}: ${e.message}")
            return
        }
        applyStep(inst, input, ctx, step, completed = input.key, lastSeenUpdate = null, completedOutcome = input.outcome)
    }

    private fun handleNotice(inst: Instance, input: Input.NoticeInput) {
        val seen = inst.record.lastSeen[input.source] ?: 0L
        if (input.sequence <= seen) {
            acknowledgeNotice(input.source, input.sequence, inst.id)
            return
        }
        val ctx = RtContext(inst.id, scheduler.now(), inst.record.revision + 1)
        val notice = Notice(Topic<Any?>(input.topic), input.source, input.sequence, input.payload)
        val result = decideGuarded(inst, "notice ${input.topic}#${input.sequence}") { inst.ball.observe(inst.record.state, notice, ctx) }
        val step = result.getOrElse { e ->
            quarantine(inst, input, "observe threw ${e::class.simpleName}: ${e.message}")
            return
        }
        if (applyStep(inst, input, ctx, step, completed = null, lastSeenUpdate = input.source to input.sequence)) {
            acknowledgeNotice(input.source, input.sequence, inst.id)
        }
    }

    private fun applyStep(
        inst: Instance,
        input: Input,
        ctx: RtContext,
        step: Step<Any?, Any?>,
        completed: CallKey?,
        lastSeenUpdate: Pair<BallId, Long>?,
        completedOutcome: Outcome<Any?>? = null,
    ): Boolean {
        val problem = preflight(inst, step.outputs, currentRequest = null, closing = completed)
        if (problem != null) {
            quarantine(inst, input, problem.message)
            return false
        }
        commit(inst, ctx, step.state, step.outputs, completed, inboundEntry = null, lastSeenUpdate = lastSeenUpdate, completedOutcome = completedOutcome)
        return true
    }

    /** A completion or notice cannot be refused; a faulty handler stops the instance. */
    private fun quarantine(inst: Instance, input: Input, message: String) {
        fault(inst, message)
        inst.failed = message
        inst.inbox.addFirst(input)
    }

    private fun fault(inst: Instance, message: String) {
        tracer.record(TraceEvent.Fault(scheduler.now(), inst.id, message))
    }

    // ---------------------------------------------------------------- preflight (R6, R9, R11)

    private class Problem(val message: String, val overload: Boolean)

    private fun preflight(
        inst: Instance,
        outputs: List<Output<Any?>>,
        currentRequest: RequestId?,
        closing: CallKey? = null,
    ): Problem? {
        if (outputs.size > limits.maxOutputsPerDecision) {
            return Problem("decision has ${outputs.size} outputs; limit is ${limits.maxOutputsPerDecision}", overload = true)
        }
        val openKeys = inst.record.calls.keys - setOfNotNull(closing)
        val newKeys = HashSet<CallKey>()
        val replied = HashSet<RequestId>()
        var notices = 0
        for (o in outputs) {
            when (o) {
                is Call<*, *> -> {
                    if (o.key in openKeys || !newKeys.add(o.key)) return Problem("call key ${o.key} is not unique", overload = false)
                    if (!isBound(o.port)) return Problem("port ${o.port.name} is not bound in the composition", overload = false)
                    if (o.port.name == Timer.port.name && (o.request !is Long || o.timeoutMillis != (o.request as Long) + Timer.MARGIN_MILLIS)) {
                        return Problem("calls on ${Timer.port.name} must be made with Timer.after", overload = false)
                    }
                    if (o.timeoutMillis > limits.maxTimeoutMillis) {
                        return Problem("timeout ${o.timeoutMillis} ms exceeds ${limits.maxTimeoutMillis} ms", overload = false)
                    }
                }
                is Cancel -> if (o.key !in openKeys) return Problem("cancel of unknown or closed call ${o.key}", overload = false)
                is Reply -> {
                    val open = inst.record.inbound[o.to]
                    if (o.to == currentRequest) return Problem("reply to the current request must use ReplyPlan.Now", overload = false)
                    if (open == null || open.reply != null) return Problem("reply to ${o.to}, which is not an open request", overload = false)
                    if (!replied.add(o.to)) return Problem("two replies to ${o.to}", overload = false)
                }
                is Publish<*> -> notices++
            }
        }
        if (openKeys.size + newKeys.size > limits.maxOpenCalls) {
            return Problem("open calls would exceed ${limits.maxOpenCalls}", overload = true)
        }
        if (inst.record.outbox.size + notices > limits.maxOutbox) {
            return Problem("notice outbox would exceed ${limits.maxOutbox}", overload = true)
        }
        return null
    }

    private fun isBound(port: Port<*, *>): Boolean = when (port) {
        is AdapterPort<*, *> -> port.name in adapters
        is BallPort<*, *> -> port.ballType in balls
    }

    // ---------------------------------------------------------------- commit (R4) and dispatch (R5)

    private fun commit(
        inst: Instance,
        ctx: Context,
        newState: Any?,
        outputs: List<Output<Any?>>,
        completed: CallKey?,
        inboundEntry: InboundRecord?,
        lastSeenUpdate: Pair<BallId, Long>?,
        completedOutcome: Outcome<Any?>? = null,
        immediateReply: RequestId? = null,
    ) {
        val now = ctx.now
        val old = inst.record
        val calls = LinkedHashMap(old.calls)
        if (completed != null) calls.remove(completed)
        val inbound = LinkedHashMap(old.inbound)
        if (inboundEntry != null) inbound[inboundEntry.requestId] = inboundEntry
        val outbox = old.outbox.toMutableList()
        var nextSeq = old.nextNoticeSequence
        val newCalls = ArrayList<CallKey>()
        val cancels = ArrayList<CallKey>()
        val replies = ArrayList<Pair<RequestId, Outcome<Any?>>>()
        val newNotices = ArrayList<NoticeRecord>()
        for (o in outputs) {
            when (o) {
                is Call<*, *> -> {
                    calls[o.key] = CallRecord(o.key, o.port.name, o.target, o.request, now + o.timeoutMillis, Phase.Pending, attempts = 0)
                    newCalls += o.key
                }
                is Cancel -> {
                    calls[o.key] = calls.getValue(o.key).copy(cancelRequested = true)
                    cancels += o.key
                }
                is Reply -> {
                    val done = Outcome.Done(o.value)
                    inbound[o.to] = inbound.getValue(o.to).copy(reply = done, repliedAt = now)
                    replies += o.to to done
                }
                is Publish<*> -> {
                    val subs = resolveSubscribers(o.topic.name, o.payload)
                    val n = NoticeRecord(nextSeq++, o.topic.name, o.payload, subs)
                    if (subs.isNotEmpty()) outbox += n
                    newNotices += n
                }
            }
        }
        pruneReplied(inbound, now)
        val lastSeen = if (lastSeenUpdate == null) old.lastSeen else old.lastSeen + lastSeenUpdate
        val record = old.copy(
            state = newState,
            revision = old.revision + 1,
            calls = calls,
            inbound = inbound,
            outbox = outbox,
            nextNoticeSequence = nextSeq,
            lastSeen = lastSeen,
        )
        store.save(record) // the atomic commit point
        inst.record = record
        completed?.let {
            inst.resolved.remove(it)
            inst.deadlineTimers.remove(it)?.cancel()
            inst.cancelHandles.remove(it)
            inst.currentAttempt.remove(it)
        }
        tracer.record(
            TraceEvent.Committed(
                at = now,
                id = inst.id,
                revision = record.revision,
                newCalls = newCalls.map { CallId(inst.id, it) },
                completedCall = completed?.let { CallId(inst.id, it) },
                completedOutcome = completedOutcome,
                replies = listOfNotNull(immediateReply) + replies.map { it.first },
                published = newNotices.size,
            ),
        )
        // ---- dispatch strictly after the commit ----
        watchers[inst.id]?.forEach { it(record.state) }
        for ((to, outcome) in replies) deliverReply(inst, to, inbound.getValue(to).replyTo, outcome)
        for (key in newCalls) {
            armDeadline(inst, key)
            startSend(inst, key)
        }
        for (key in cancels) cancelCall(inst, key)
        if (newNotices.any { it.pendingSubscribers.isNotEmpty() }) deliverOutbox(inst)
    }

    private fun pruneReplied(inbound: LinkedHashMap<RequestId, InboundRecord>, now: Long) {
        val replied = inbound.values.filter { it.reply != null }
        val expired = replied.filter { now - (it.repliedAt ?: now) > limits.repliedRetentionMillis }
        expired.forEach { inbound.remove(it.requestId) }
        val excess = replied.size - expired.size - limits.repliedRetention
        if (excess > 0) {
            replied.filter { it !in expired }.sortedBy { it.repliedAt }.take(excess).forEach { inbound.remove(it.requestId) }
        }
    }

    private fun saveLedger(inst: Instance, call: CallRecord) {
        val record = inst.record.copy(calls = inst.record.calls + (call.key to call))
        store.save(record)
        inst.record = record
    }

    // ---------------------------------------------------------------- calls (R6, R7, R8)

    private fun armDeadline(inst: Instance, key: CallKey) {
        val call = inst.record.calls[key] ?: return
        inst.deadlineTimers[key]?.cancel()
        inst.deadlineTimers[key] = scheduler.schedule((call.deadlineAt - scheduler.now()).coerceAtLeast(0)) {
            onLoopNow { onDeadline(inst, key) }
        }
    }

    private fun startSend(inst: Instance, key: CallKey) {
        val call = inst.record.calls[key] ?: return
        if (key in inst.resolved || !alive) return
        val now = scheduler.now()
        if (now >= call.deadlineAt) return // the deadline handler decides
        if (call.cancelRequested && call.phase == Phase.Pending) {
            resolve(inst, key, Outcome.NotDone(NotDoneReason.Cancelled))
            return
        }
        val bound = adapters[call.portName]
        if (bound != null) {
            val busy = inFlight[call.portName] ?: 0
            if (busy >= bound.maxInFlight) {
                val q = waiting.getOrPut(call.portName) { ArrayDeque() }
                val id = CallId(inst.id, key)
                if (id !in q) q.addLast(id)
                return
            }
            inFlight[call.portName] = busy + 1
        }
        // Write-ahead: the durable record says "possibly sent" before any transport runs.
        val attempt = call.attempts + 1
        saveLedger(inst, call.copy(phase = Phase.Sending, attempts = attempt))
        inst.currentAttempt[key] = attempt
        val callId = CallId(inst.id, key)
        tracer.record(TraceEvent.CallSent(now, callId, call.portName, attempt))
        if (bound != null) {
            val invocation = Invocation(callId, call.request, attempt, call.deadlineAt)
            // A report that arrives after this runtime crashed is dropped by onLoopNow;
            // its successor has already classified the call from the durable record.
            val handle = try {
                bound.adapter.execute(invocation) { result ->
                    scheduler.execute { onLoopNow { onAdapterResult(inst, key, attempt, result) } }
                }
            } catch (e: Exception) {
                scheduler.execute { onLoopNow { onAdapterResult(inst, key, attempt, AdapterResult.Failed("adapter threw: ${e.message}")) } }
                null
            }
            if (handle != null) inst.cancelHandles[key] = handle
        } else {
            deliverToBall(inst, call, callId)
        }
    }

    private fun releaseSlot(portName: String) {
        val busy = inFlight[portName] ?: return
        inFlight[portName] = (busy - 1).coerceAtLeast(0)
        val q = waiting[portName] ?: return
        while (q.isNotEmpty()) {
            val next = q.removeFirst()
            val inst = instances[next.caller] ?: continue
            val call = inst.record.calls[next.key] ?: continue
            if (next.key in inst.resolved || call.phase != Phase.Pending) continue
            startSend(inst, next.key)
            return
        }
    }

    private fun onAdapterResult(inst: Instance, key: CallKey, attempt: Int, result: AdapterResult<Any?>) {
        val call = inst.record.calls[key]
        val callId = CallId(inst.id, key)
        if (call == null || key in inst.resolved) {
            tracer.record(TraceEvent.LateReportDropped(scheduler.now(), callId, result.toString()))
            return
        }
        val bound = adapters.getValue(call.portName)
        val current = inst.currentAttempt[key] == attempt
        if (current) {
            inst.currentAttempt.remove(key)
            releaseSlot(call.portName)
        }
        // Evidence of non-application covers only the attempt that produced it. It is
        // conclusive for the call when no other attempt can have been delivered, or
        // when the port has no effects at all.
        val conclusive = call.attempts == 1 || bound.port.effect == EffectClass.Safe
        when (result) {
            is AdapterResult.Ok -> resolve(inst, key, Outcome.Done(result.value))
            is AdapterResult.Refused ->
                if (conclusive) resolve(inst, key, Outcome.NotDone(NotDoneReason.Refused(result.value)))
                else if (current) failAttempt(inst, key, call, bound)
            is AdapterResult.NotApplied ->
                if (conclusive) resolve(inst, key, Outcome.NotDone(NotDoneReason.NotApplied(result.evidence)))
                else if (current) failAttempt(inst, key, call, bound)
            is AdapterResult.Failed -> if (current) failAttempt(inst, key, call, bound)
        }
    }

    private fun failAttempt(inst: Instance, key: CallKey, call: CallRecord, bound: BoundAdapter) {
        val effect = bound.port.effect
        val retry = bound.retry
        val now = scheduler.now()
        val canRetry = effect != EffectClass.NonIdempotent &&
            call.attempts < retry.maxAttempts &&
            now + retry.backoffMillis < call.deadlineAt &&
            !call.cancelRequested
        if (canRetry) {
            scheduler.schedule(retry.backoffMillis) { onLoopNow { if (key !in inst.resolved) startSend(inst, key) } }
            return
        }
        val outcome: Outcome<Any?> = when (effect) {
            EffectClass.Safe -> Outcome.NotDone(NotDoneReason.NoAnswer)
            EffectClass.Idempotent ->
                Outcome.Unknown(if (call.attempts > 1) UnknownReason.RetriesExhausted else UnknownReason.LostAfterSend)
            EffectClass.NonIdempotent -> Outcome.Unknown(UnknownReason.LostAfterSend)
        }
        resolve(inst, key, outcome)
    }

    private fun onDeadline(inst: Instance, key: CallKey) {
        val call = inst.record.calls[key] ?: return
        if (key in inst.resolved) return
        inst.deadlineTimers.remove(key)
        if (call.phase == Phase.Pending) {
            waiting[call.portName]?.remove(CallId(inst.id, key))
            resolve(inst, key, Outcome.NotDone(NotDoneReason.DeadlineBeforeSend))
            return
        }
        inst.cancelHandles.remove(key)?.cancel()
        if (inst.currentAttempt.remove(key) != null && call.portName in adapters) releaseSlot(call.portName)
        val effect = portEffect(call.portName)
        resolve(inst, key, if (effect == EffectClass.Safe) Outcome.NotDone(NotDoneReason.NoAnswer) else Outcome.Unknown(UnknownReason.DeadlineAfterSend))
    }

    private fun cancelCall(inst: Instance, key: CallKey) {
        val call = inst.record.calls[key] ?: return
        if (key in inst.resolved) return
        if (call.phase == Phase.Pending) {
            waiting[call.portName]?.remove(CallId(inst.id, key))
            resolve(inst, key, Outcome.NotDone(NotDoneReason.Cancelled))
            return
        }
        // Possibly sent: ask the adapter to stop. Its report decides the outcome;
        // a cancellation never erases a result that already happened.
        inst.cancelHandles[key]?.cancel()
    }

    private fun portEffect(portName: String): EffectClass =
        adapters[portName]?.port?.effect ?: EffectClass.Idempotent // Ball ports are idempotent

    private fun portOf(inst: Instance, call: CallRecord): Port<*, *> =
        adapters[call.portName]?.port ?: BallPort<Any?, Any?>(call.portName.removePrefix("ball:"))

    /** Chooses the single terminal outcome of a call and queues it for the caller (R6). */
    private fun resolve(inst: Instance, key: CallKey, outcome: Outcome<Any?>) {
        val call = inst.record.calls[key] ?: return
        if (!inst.resolved.add(key)) return
        inst.deadlineTimers.remove(key)?.cancel()
        if (inst.currentAttempt.remove(key) != null && call.portName in adapters) releaseSlot(call.portName)
        tracer.record(TraceEvent.CallResolved(scheduler.now(), CallId(inst.id, key), outcome))
        val input = Input.CompletionInput(key, portOf(inst, call), outcome)
        // Completions bypass the inbox bound: they close obligations the instance already holds.
        inst.inbox.addLast(input)
        inst.scheduleDrain()
    }

    // ---------------------------------------------------------------- Ball-to-Ball requests (R9)

    private fun deliverToBall(caller: Instance, call: CallRecord, callId: CallId) {
        val destType = call.portName.removePrefix("ball:")
        val dest = instanceOrNull(BallId(destType, call.target!!))
        if (dest == null) {
            resolve(caller, call.key, Outcome.NotDone(NotDoneReason.NotApplied("unknown Ball type '$destType'")))
            return
        }
        val input = Input.Request(
            requestId = RequestId(callId.toString()),
            message = call.request,
            caller = Caller(ball = caller.id),
            deadlineAt = call.deadlineAt,
            replyTo = ReplyTarget.ToCall(callId),
            dedup = true,
        )
        if (!dest.offer(input)) resolve(caller, call.key, refusal(dest))
    }

    private fun refusal(inst: Instance): Outcome<Any?> =
        inst.failed?.let { Outcome.NotDone(NotDoneReason.Fault("instance ${inst.id} is quarantined: $it")) }
            ?: Outcome.NotDone(NotDoneReason.Overloaded)

    private fun reject(inst: Instance, input: Input.Request, outcome: Outcome<Any?>) {
        tracer.record(TraceEvent.RequestRejected(scheduler.now(), inst.id, input.requestId, outcome))
        deliverReplyTo(inst, input, outcome)
    }

    private fun deliverReplyTo(inst: Instance, input: Input.Request, outcome: Outcome<Any?>) {
        when (val target = input.replyTo) {
            ReplyTarget.External -> input.waiter?.finish(outcome)
            is ReplyTarget.ToCall -> routeBallReply(target.callId, outcome)
        }
    }

    private fun deliverReply(inst: Instance, requestId: RequestId, target: ReplyTarget, outcome: Outcome<Any?>) {
        tracer.record(TraceEvent.Replied(scheduler.now(), inst.id, requestId, outcome))
        when (target) {
            ReplyTarget.External -> inst.externalWaiters.remove(requestId)?.forEach { it.finish(outcome) }
            is ReplyTarget.ToCall -> routeBallReply(target.callId, outcome)
        }
    }

    private fun routeBallReply(callId: CallId, outcome: Outcome<Any?>) {
        val caller = instanceOrNull(callId.caller) ?: return
        val call = caller.record.calls[callId.key]
        if (call == null || callId.key in caller.resolved) {
            tracer.record(TraceEvent.LateReportDropped(scheduler.now(), callId, outcome.toString()))
            return
        }
        resolve(caller, callId.key, outcome)
    }

    private fun externalDeadline(inst: Instance, input: Input.Request, waiter: ExternalWaiter) {
        if (waiter.done) return
        val stillQueued = inst.inbox.remove(input)
        val accepted = inst.record.inbound[input.requestId]
        waiter.finish(
            when {
                stillQueued -> Outcome.NotDone(NotDoneReason.DeadlineBeforeSend)
                accepted != null -> Outcome.Unknown(UnknownReason.DeadlineAfterSend)
                else -> Outcome.Unknown(UnknownReason.DeadlineAfterSend)
            },
        )
        inst.externalWaiters[input.requestId]?.remove(waiter)
    }

    private fun checkReplyOverdue(inst: Instance, requestId: RequestId) {
        val entry = inst.record.inbound[requestId] ?: return
        if (entry.reply == null) tracer.record(TraceEvent.ReplyOverdue(scheduler.now(), inst.id, requestId))
    }

    // ---------------------------------------------------------------- notices

    private fun resolveSubscribers(topic: String, payload: Any?): Set<BallId> =
        subscriptions[topic].orEmpty().mapTo(LinkedHashSet()) { BallId(it.subscriberType, it.route(payload)) }

    /** Delivers unacknowledged notices in sequence order per subscriber; never drops accepted ones. */
    private fun deliverOutbox(publisher: Instance) {
        val blocked = HashSet<BallId>()
        for (n in publisher.record.outbox) {
            for (sub in n.pendingSubscribers) {
                if (sub in blocked) continue
                val tag = n.sequence to sub
                if (tag in publisher.noticesInFlight) continue
                val target = instanceOrNull(sub)
                if (target == null) continue
                if (target.offer(Input.NoticeInput(n.topic, publisher.id, n.sequence, n.payload))) {
                    publisher.noticesInFlight += tag
                } else {
                    blocked += sub
                }
            }
        }
        if (blocked.isNotEmpty() && !publisher.outboxRetryScheduled) {
            publisher.outboxRetryScheduled = true
            scheduler.schedule(limits.noticeRetryMillis) {
                onLoopNow {
                    publisher.outboxRetryScheduled = false
                    deliverOutbox(publisher)
                }
            }
        }
    }

    private fun acknowledgeNotice(source: BallId, sequence: Long, subscriber: BallId) {
        val publisher = instanceOrNull(source) ?: return
        publisher.noticesInFlight -= (sequence to subscriber)
        val outbox = publisher.record.outbox.mapNotNull { n ->
            if (n.sequence != sequence) n
            else (n.pendingSubscribers - subscriber).takeIf { it.isNotEmpty() }?.let { n.copy(pendingSubscribers = it) }
        }
        if (outbox != publisher.record.outbox) {
            val record = publisher.record.copy(outbox = outbox)
            store.save(record)
            publisher.record = record
        }
    }

    // ---------------------------------------------------------------- recovery

    private fun recover() {
        val now = scheduler.now()
        val recovered = store.ids().mapNotNull { instanceOrNull(it) }
        for (inst in recovered) {
            for (call in inst.record.calls.values.sortedBy { it.key.value }) {
                if (now >= call.deadlineAt) {
                    val outcome: Outcome<Any?> = when {
                        call.phase == Phase.Pending -> Outcome.NotDone(NotDoneReason.DeadlineBeforeSend)
                        portEffect(call.portName) == EffectClass.Safe -> Outcome.NotDone(NotDoneReason.NoAnswer)
                        else -> Outcome.Unknown(UnknownReason.DeadlineAfterSend)
                    }
                    resolve(inst, call.key, outcome)
                    continue
                }
                armDeadline(inst, call.key)
                when {
                    call.phase == Phase.Pending -> startSend(inst, call.key)
                    portEffect(call.portName) == EffectClass.NonIdempotent ->
                        resolve(inst, call.key, Outcome.Unknown(UnknownReason.RecoveredAfterCrash))
                    else -> {
                        val bound = adapters[call.portName]
                        if (bound == null || call.attempts < bound.retry.maxAttempts) {
                            startSend(inst, call.key) // Ball ports deduplicate; repeatable adapters retry
                        } else {
                            resolve(
                                inst, call.key,
                                if (bound.port.effect == EffectClass.Safe) Outcome.NotDone(NotDoneReason.NoAnswer)
                                else Outcome.Unknown(UnknownReason.RetriesExhausted),
                            )
                        }
                    }
                }
            }
            for (entry in inst.record.inbound.values) {
                if (entry.reply == null) {
                    scheduler.schedule((entry.deadlineAt - now).coerceAtLeast(0)) { onLoopNow { checkReplyOverdue(inst, entry.requestId) } }
                }
            }
            if (inst.record.outbox.isNotEmpty()) deliverOutbox(inst)
        }
        tracer.record(TraceEvent.Recovered(now, incarnation, recovered.size))
    }

    // ---------------------------------------------------------------- loop helpers

    private inline fun onLoop(crossinline block: () -> Unit) {
        scheduler.execute { onLoopNow { block() } }
    }

    private inline fun onLoopNow(block: () -> Unit) {
        if (alive) block()
    }

    private open class RtContext(
        override val self: BallId,
        override val now: Long,
        override val revision: Long,
    ) : Context {
        private var issued = 0
        override fun newKey(): CallKey = CallKey("$revision.${issued++}")
    }

    private class RtRequestContext(
        self: BallId,
        now: Long,
        revision: Long,
        override val requestId: RequestId,
        override val caller: Caller,
    ) : RtContext(self, now, revision), RequestContext
}

/**
 * The built-in timer adapter: answers at the timer's due time, on the runtime's scheduler.
 * Preflight admits only calls made with [Timer.after], whose deadline is the due time
 * plus [Timer.MARGIN_MILLIS]; so every attempt, including a re-arm after a Durable
 * restart, waits until the original due time and never fires before it.
 */
@Suppress("UNCHECKED_CAST")
private fun timerAdapter(scheduler: Scheduler): Adapter<Any?, Any?> = Adapter<Long, Unit> { invocation, done ->
    val due = invocation.deadlineAt - Timer.MARGIN_MILLIS
    val fired = scheduler.schedule((due - scheduler.now()).coerceAtLeast(0)) { done(AdapterResult.Ok(Unit)) }
    Cancellable {
        fired.cancel()
        done(AdapterResult.NotApplied("timer cancelled"))
    }
} as Adapter<Any?, Any?>
