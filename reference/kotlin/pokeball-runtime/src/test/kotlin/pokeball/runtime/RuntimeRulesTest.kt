package pokeball.runtime

import pokeball.kernel.AdapterPort
import pokeball.kernel.BallId
import pokeball.kernel.BallPort
import pokeball.kernel.Call
import pokeball.kernel.CallId
import pokeball.kernel.CallKey
import pokeball.kernel.Cancel
import pokeball.kernel.EffectClass
import pokeball.kernel.Notice
import pokeball.kernel.NotDoneReason
import pokeball.kernel.Outcome
import pokeball.kernel.Publish
import pokeball.kernel.Reply
import pokeball.kernel.RequestId
import pokeball.kernel.Timer
import pokeball.kernel.Topic
import pokeball.kernel.UnknownReason
import pokeball.kernel.accept
import pokeball.kernel.acceptLater
import pokeball.kernel.step
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Rule-by-rule tests of the runtime at the boundaries the exploration tests do
 * not reach: resource limits, deadlines at the exact instant, retry budgets,
 * reports from earlier attempts, deduplication retention, recovery of each call
 * phase, and notice delivery under back-pressure.
 */
class RuntimeRulesTest {
    private val s = BallId("script", "s")
    private val nonIdem = AdapterPort<String, String>("charge", EffectClass.NonIdempotent)
    private val idem = AdapterPort<String, String>("idem", EffectClass.Idempotent)
    private val safe = AdapterPort<String, String>("lookup", EffectClass.Safe)
    private val topic = Topic<Int>("t")

    private fun rig(
        limits: Limits = Limits(),
        store: MemoryStore = MemoryStore(),
        ball: ScriptBall = ScriptBall(),
        build: Composition.() -> Unit = {},
    ): Rig = Rig(limits, store) { Composition().ball(ball).apply(build) }.start()

    private fun callAct(port: AdapterPort<String, String>, timeout: Long = 100, label: String = "call ${port.name}") =
        Act(label) { st, ctx ->
            val k = ctx.newKey()
            accept(st + k, "ok", Call(k, port, label, timeout))
        }

    private fun ok(label: String = "ok") = Act(label) { st, _ -> accept(st + label, "ok") }

    private fun Rig.keys(id: BallId = s): List<CallKey> = log(id).filterIsInstance<CallKey>()

    private fun Rig.outcomeOf(key: CallKey, id: BallId = s): Outcome<*> = completions(id).single { it.key == key }.outcome

    private fun assertFault(outcome: Outcome<Any?>?, fragment: String) {
        val notDone = assertIs<Outcome.NotDone<*>>(outcome)
        val fault = assertIs<NotDoneReason.Fault>(notDone.reason)
        assertTrue(fragment in fault.message, "expected '$fragment' in '${fault.message}'")
    }

    // ---------------------------------------------------------------- R11 bounds and R6/R9 preflight

    @Test
    fun `outputs at the per-decision limit are accepted and one more is refused as Overloaded`() {
        val r = rig(Limits(maxOutputsPerDecision = 3))
        val atLimit = r.send(s, Act("3") { st, _ -> accept(st + 3, "ok", *Array(3) { Publish(topic, it) }) })
        val over = r.send(s, Act("4") { st, _ -> accept(st + 4, "ok", *Array(4) { Publish(topic, it) }) })
        assertEquals(Outcome.Done("ok"), r.outcome(atLimit))
        assertEquals(Outcome.NotDone(NotDoneReason.Overloaded), r.outcome(over))
        assertEquals(listOf<Any?>(3), r.log(s))
    }

    @Test
    fun `a call key used twice, or equal to an open call's key, is a fault and commits nothing`() {
        val r = rig { adapter(nonIdem, ScriptedAdapter()) }
        val k = CallKey("k")
        val twice = r.send(s, Act("twice") { st, _ -> accept(st + "twice", "ok", Call(k, nonIdem, "a", 100), Call(k, nonIdem, "b", 100)) })
        val first = r.send(s, Act("first") { st, _ -> accept(st + "first", "ok", Call(k, nonIdem, "a", 100)) })
        val reused = r.send(s, Act("reused") { st, _ -> accept(st + "reused", "ok", Call(k, nonIdem, "b", 100)) })
        assertFault(r.outcome(twice), "not unique")
        assertEquals(Outcome.Done("ok"), r.outcome(first))
        assertFault(r.outcome(reused), "not unique")
        assertEquals(listOf<Any?>("first"), r.log(s))
        assertEquals(1L, r.runtime.record(s)!!.revision)
        assertEquals(2, r.events<TraceEvent.Fault>().size)
    }

    @Test
    fun `calls on unbound ports are faults, for adapters and for Ball types alike`() {
        val r = rig()
        val adapter = r.send(s, Act("adapter") { st, ctx -> accept(st, "ok", Call(ctx.newKey(), AdapterPort<String, String>("nowhere", EffectClass.Safe), "x", 100)) })
        val ball = r.send(s, Act("ball") { st, ctx -> accept(st, "ok", Call(ctx.newKey(), BallPort<String, String>("ghost"), "x", 100, target = "g")) })
        assertFault(r.outcome(adapter), "not bound")
        assertFault(r.outcome(ball), "not bound")
        assertEquals(0L, r.runtime.record(s)?.revision ?: 0L)
    }

    @Test
    fun `a call timeout at the limit is accepted and one millisecond more is a fault`() {
        val r = rig(Limits(maxTimeoutMillis = 500)) { adapter(nonIdem, ScriptedAdapter()) }
        assertEquals(Outcome.Done("ok"), r.outcome(r.send(s, callAct(nonIdem, timeout = 500))))
        assertFault(r.outcome(r.send(s, callAct(nonIdem, timeout = 501))), "exceeds")
    }

    @Test
    fun `cancelling a call that is not open is a fault`() {
        val r = rig()
        assertFault(r.outcome(r.send(s, Act("cancel") { st, _ -> accept(st, "ok", Cancel(CallKey("nope"))) })), "cancel of unknown")
    }

    @Test
    fun `replies go only to open requests, once, and never to the request being decided`() {
        val r = rig()
        val open = r.send(s, Act("owe") { st, ctx -> acceptLater(st + ctx.requestId) })
        val rid = r.log(s).single() as RequestId
        val self = r.send(s, Act("self") { st, ctx -> acceptLater(st, Reply(ctx.requestId, "self")) })
        val twice = r.send(s, Act("twice") { st, _ -> accept(st, "ok", Reply(rid, "a"), Reply(rid, "b")) })
        val unknown = r.send(s, Act("unknown") { st, _ -> accept(st, "ok", Reply(RequestId("nope"), "x")) })
        assertNull(r.outcome(open))
        val answer = r.send(s, Act("answer") { st, _ -> accept(st, "ok", Reply(rid, "answer")) })
        val again = r.send(s, Act("again") { st, _ -> accept(st, "ok", Reply(rid, "again")) })
        assertFault(r.outcome(self), "current request")
        assertFault(r.outcome(twice), "two replies")
        assertFault(r.outcome(unknown), "not an open request")
        assertEquals(Outcome.Done("answer"), r.outcome(open))
        assertEquals(Outcome.Done("ok"), r.outcome(answer))
        assertFault(r.outcome(again), "not an open request")
    }

    @Test
    fun `open calls are bounded, and a completion may open a call in place of the one it closes`() {
        val adapter = ScriptedAdapter<String, String>()
        val ball = ScriptBall(onComplete = { st, c, ctx ->
            if (st.none { it is pokeball.kernel.Completion<*> }) step(st + c, Call(ctx.newKey(), idem, "follow-up", 100)) else step(st + c)
        })
        val r = rig(Limits(maxOpenCalls = 2), ball = ball) { adapter(idem, adapter) }
        val two = r.send(s, Act("two") { st, ctx -> accept(st, "ok", Call(ctx.newKey(), idem, "a", 100), Call(ctx.newKey(), idem, "b", 100)) })
        val third = r.send(s, callAct(idem))
        assertEquals(Outcome.Done("ok"), r.outcome(two))
        assertEquals(Outcome.NotDone(NotDoneReason.Overloaded), r.outcome(third))
        adapter.sent[0].report(AdapterResult.Ok("done"))
        r.scheduler.runReady()
        assertNull(r.runtime.failure(s))
        assertEquals(3, adapter.sent.size)
        assertEquals(2, r.runtime.record(s)!!.calls.size)
    }

    @Test
    fun `the notice outbox is bounded, counting notices still waiting for a subscriber`() {
        val sub = BallId("sub", "x")
        val r = rig(Limits(maxOutbox = 2)) {
            ball(ScriptBall("sub", onObserve = { _, _, _ -> error("broken subscriber") }))
            subscribe(topic, "sub") { "x" }
        }
        val one = r.send(s, Act("one") { st, _ -> accept(st + 1, "ok", Publish(topic, 1)) })
        assertTrue(r.runtime.failure(sub)!!.contains("observe threw"))
        val fills = r.send(s, Act("fills") { st, _ -> accept(st + 2, "ok", Publish(topic, 2)) })
        val over = r.send(s, Act("over") { st, _ -> accept(st + 3, "ok", Publish(topic, 3)) })
        assertEquals(Outcome.Done("ok"), r.outcome(one))
        assertEquals(Outcome.Done("ok"), r.outcome(fills))
        assertEquals(Outcome.NotDone(NotDoneReason.Overloaded), r.outcome(over))
        assertEquals(listOf(1L, 2L), r.store.load(s)!!.outbox.map { it.sequence })
    }

    @Test
    fun `a completion handler that breaks a rule quarantines its instance, which then refuses requests`() {
        val adapter = ScriptedAdapter<String, String>()
        val ball = ScriptBall(onComplete = { st, c, _ -> step(st + c, Cancel(CallKey("nope"))) })
        val r = rig(ball = ball) { adapter(nonIdem, adapter) }
        r.send(s, callAct(nonIdem))
        adapter.sent[0].report(AdapterResult.Ok("x"))
        r.scheduler.runReady()
        assertTrue(r.runtime.failure(s)!!.contains("cancel of unknown"))
        assertEquals(r.keys(), r.log(s))
        assertFault(r.outcome(r.send(s, ok())), "quarantined")
    }

    // ---------------------------------------------------------------- request deadlines and reply accounting (R9)

    @Test
    fun `a request decided exactly at its deadline is accepted, one millisecond later it is refused unseen`() {
        val r = rig()
        val t0 = r.scheduler.now()
        val onTime = r.send(s, ok("on time"), timeoutMillis = 5, run = false)
        r.scheduler.runReady(1)
        r.scheduler.jumpTo(t0 + 5)
        r.scheduler.runReady()
        assertEquals(Outcome.Done("ok"), r.outcome(onTime))
        val t1 = r.scheduler.now()
        val late = r.send(s, ok("late"), timeoutMillis = 5, run = false)
        r.scheduler.runReady(1)
        r.scheduler.jumpTo(t1 + 6)
        r.scheduler.runReady()
        assertEquals(Outcome.NotDone(NotDoneReason.DeadlineBeforeSend), r.outcome(late))
        assertEquals(listOf<Any?>("on time"), r.log(s))
    }

    @Test
    fun `a request still queued when its caller gives up is withdrawn and never decided`() {
        val r = rig()
        val t0 = r.scheduler.now()
        val i = r.send(s, ok("never"), timeoutMillis = 50, run = false)
        r.scheduler.runReady(1)
        r.scheduler.advanceTo(t0 + 50, readyFirst = false)
        assertEquals(Outcome.NotDone(NotDoneReason.DeadlineBeforeSend), r.outcome(i))
        assertTrue(r.events<TraceEvent.DecideStarted>().isEmpty())
        assertEquals(emptyList(), r.log(s))
    }

    @Test
    fun `an accepted request unanswered at its deadline is Unknown to the caller and reported overdue exactly then`() {
        val r = rig()
        val t0 = r.scheduler.now()
        val owed = r.send(s, Act("owe") { st, ctx -> acceptLater(st + ctx.requestId) }, timeoutMillis = 100)
        val rid = r.log(s).single() as RequestId
        r.scheduler.advanceTo(t0 + 99)
        assertNull(r.outcome(owed))
        assertTrue(r.events<TraceEvent.ReplyOverdue>().isEmpty())
        r.scheduler.advanceTo(t0 + 100)
        assertEquals(Outcome.Unknown(UnknownReason.DeadlineAfterSend), r.outcome(owed))
        val overdue = r.events<TraceEvent.ReplyOverdue>().single()
        assertEquals(t0 + 100, overdue.at)
        assertEquals(rid, overdue.requestId)
        // The Ball answers later: the reply commits, the caller who gave up hears nothing more.
        val answer = r.send(s, Act("answer") { st, _ -> accept(st, "ok", Reply(rid, "late")) })
        assertEquals(Outcome.Done("ok"), r.outcome(answer))
        assertEquals(1, r.outcomes[owed].size)
        assertEquals(Outcome.Done("late"), r.runtime.record(s)!!.inbound.getValue(rid).reply)
    }

    @Test
    fun `a reply in time is not reported overdue`() {
        val r = rig()
        val t0 = r.scheduler.now()
        val owed = r.send(s, Act("owe") { st, ctx -> acceptLater(st + ctx.requestId) }, timeoutMillis = 100)
        val rid = r.log(s).single() as RequestId
        r.send(s, Act("answer") { st, _ -> accept(st, "ok", Reply(rid, "in time")) })
        r.scheduler.advanceTo(t0 + 500)
        assertEquals(Outcome.Done("in time"), r.outcome(owed))
        assertTrue(r.events<TraceEvent.ReplyOverdue>().isEmpty())
    }

    // ---------------------------------------------------------------- deduplication retention

    @Test
    fun `only requests that may be repeated or still owe a reply are kept for deduplication`() {
        val r = rig()
        r.send(s, ok("plain"))
        assertTrue(r.runtime.record(s)!!.inbound.isEmpty())
        val now = r.scheduler.now()
        r.send(s, ok("keyed"), idempotencyKey = "k")
        r.send(s, Act("later") { st, _ -> acceptLater(st) })
        val inbound = r.runtime.record(s)!!.inbound.values
        assertEquals(2, inbound.size)
        val keyed = inbound.single { it.requestId == RequestId("idem:k") }
        assertEquals(Outcome.Done("ok"), keyed.reply)
        assertEquals(now, keyed.repliedAt)
        assertNull(inbound.single { it.requestId != RequestId("idem:k") }.reply)
        // Every commit reports the requests it answered, whether or not the entry is kept.
        val committed = r.events<TraceEvent.Committed>()
        assertEquals(listOf(1, 1, 0), committed.map { it.replies.size })
        assertEquals(RequestId("idem:k"), committed[1].replies.single())
    }

    @Test
    fun `answered requests are retained up to the count limit, oldest first out`() {
        val r = rig(Limits(repliedRetention = 2))
        val acts = (1..3).map { ok("k$it") }
        acts.forEachIndexed { i, a ->
            r.scheduler.advanceBy(10)
            r.send(s, a, idempotencyKey = "k${i + 1}")
        }
        assertEquals(setOf("idem:k2", "idem:k3"), r.runtime.record(s)!!.inbound.keys.map { it.value }.toSet())
        r.send(s, acts[2], idempotencyKey = "k3") // replayed
        r.send(s, acts[0], idempotencyKey = "k1") // forgotten, so decided again
        assertEquals(listOf<Any?>("k1", "k2", "k3", "k1"), r.log(s))
    }

    @Test
    fun `answered requests expire strictly after the retention time`() {
        val r = rig(Limits(repliedRetentionMillis = 100))
        val t0 = r.scheduler.now()
        r.send(s, ok("k1"), idempotencyKey = "k1")
        r.scheduler.jumpTo(t0 + 100)
        r.send(s, ok("k2"), idempotencyKey = "k2")
        assertEquals(setOf("idem:k1", "idem:k2"), r.runtime.record(s)!!.inbound.keys.map { it.value }.toSet())
        r.scheduler.jumpTo(t0 + 101)
        r.send(s, ok("k3"), idempotencyKey = "k3")
        assertEquals(setOf("idem:k2", "idem:k3"), r.runtime.record(s)!!.inbound.keys.map { it.value }.toSet())
    }

    @Test
    fun `time and count retention combine without evicting more than needed`() {
        val r = rig(Limits(repliedRetention = 1, repliedRetentionMillis = 100))
        val t0 = r.scheduler.now()
        r.send(s, ok("k1"), idempotencyKey = "k1")
        r.scheduler.jumpTo(t0 + 50)
        r.send(s, ok("k2"), idempotencyKey = "k2")
        assertEquals(setOf("idem:k2"), r.runtime.record(s)!!.inbound.keys.map { it.value }.toSet())
        r.scheduler.jumpTo(t0 + 200)
        r.send(s, ok("k3"), idempotencyKey = "k3")
        assertEquals(setOf("idem:k3"), r.runtime.record(s)!!.inbound.keys.map { it.value }.toSet())
    }

    // ---------------------------------------------------------------- calls: write-ahead, retries, attempts (R5, R7, R8)

    @Test
    fun `a call is durably recorded as possibly sent before its adapter runs`() {
        lateinit var r: Rig
        val seen = ArrayList<CallRecord?>()
        val adapter = ScriptedAdapter<String, String> { inv -> seen += r.store.load(s)!!.calls[inv.callId.key] }
        r = rig { adapter(nonIdem, adapter) }
        val t0 = r.scheduler.now()
        val savesBefore = r.store.saves
        r.send(s, callAct(nonIdem, timeout = 100))
        val record = seen.single()!!
        assertEquals(Phase.Sending, record.phase)
        assertEquals(1, record.attempts)
        assertEquals(t0 + 100, adapter.sent.single().invocation.deadlineAt)
        assertEquals(2, r.store.saves - savesBefore) // the commit, then the write-ahead record
    }

    @Test
    fun `durable writes per operation follow the cost model of the results`() {
        // Cost model: one write per committed decision,
        // one write-ahead per attempt, one acknowledgement write per delivered notice.
        val a = ScriptedAdapter<String, String>()
        val sub = BallId("sub", "x")
        val r = rig {
            adapter(idem, a, RetryPolicy(maxAttempts = 2))
            ball(ScriptBall("sub"))
            subscribe(topic, "sub") { "x" }
        }
        fun writes(block: () -> Unit): Long {
            val before = r.store.saves
            block()
            return r.store.saves - before
        }
        assertEquals(1, writes { r.send(s, ok("local")) }) // decision only
        assertEquals(2, writes { r.send(s, callAct(idem, timeout = 1_000)) }) // decision + write-ahead
        assertEquals(1, writes { a.sent[0].report(AdapterResult.Failed("reset")); r.scheduler.advanceBy(0) }) // retry: write-ahead of attempt 2
        assertEquals(1, writes { a.sent[1].report(AdapterResult.Ok("done")); r.scheduler.runReady() }) // completion committed
        assertEquals(3, writes { r.send(s, Act("publish") { st, _ -> accept(st, "ok", Publish(topic, 1)) }) }) // publisher + subscriber + ack
        assertEquals(1, r.log(sub).size)
    }

    @Test
    fun `a retry is scheduled only when its backoff ends before the deadline`() {
        fun failAt(offset: Long): Pair<Rig, ScriptedAdapter<String, String>> {
            val a = ScriptedAdapter<String, String>()
            val r = rig { adapter(idem, a, RetryPolicy(maxAttempts = 3, backoffMillis = 50)) }
            val t0 = r.scheduler.now()
            r.send(s, callAct(idem, timeout = 100))
            r.scheduler.jumpTo(t0 + offset)
            a.sent[0].report(AdapterResult.Failed("reset"))
            r.scheduler.advanceTo(t0 + 99)
            return r to a
        }
        val (retried, a1) = failAt(49)
        assertEquals(listOf(1, 2), a1.sent.map { it.invocation.attempt })
        assertTrue(retried.completions(s).isEmpty())
        val (gaveUp, a2) = failAt(50)
        assertEquals(1, a2.sent.size)
        assertEquals(Outcome.Unknown(UnknownReason.LostAfterSend), gaveUp.completions(s).single().outcome)
    }

    @Test
    fun `the retry budget bounds attempts, after which an idempotent call is Unknown`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(idem, a, RetryPolicy(maxAttempts = 2)) }
        r.send(s, callAct(idem, timeout = 1_000))
        a.sent[0].report(AdapterResult.Failed("reset"))
        r.scheduler.advanceBy(0)
        a.sent[1].report(AdapterResult.Failed("reset"))
        r.scheduler.advanceBy(0)
        assertEquals(2, a.sent.size)
        assertEquals(Outcome.Unknown(UnknownReason.RetriesExhausted), r.completions(s).single().outcome)
    }

    @Test
    fun `a cancel while sending asks the adapter to stop and suppresses further retries`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(idem, a, RetryPolicy(maxAttempts = 3)) }
        r.send(s, callAct(idem, timeout = 1_000))
        val key = r.keys().single()
        r.send(s, Act("cancel") { st, _ -> accept(st, "ok", Cancel(key)) })
        assertTrue(a.sent[0].cancelled)
        a.sent[0].report(AdapterResult.Failed("stopped"))
        r.scheduler.advanceBy(10)
        assertEquals(1, a.sent.size)
        assertEquals(Outcome.Unknown(UnknownReason.LostAfterSend), r.outcomeOf(key))
    }

    @Test
    fun `reports from an earlier attempt neither fail the call nor free its slot, but a success from any attempt counts`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(idem, a, RetryPolicy(maxAttempts = 3), maxInFlight = 1) }
        r.send(s, callAct(idem, timeout = 1_000, label = "first"))
        a.sent[0].report(AdapterResult.Failed("reset"))
        r.scheduler.advanceBy(0)
        assertEquals(2, a.sent.size) // attempt 2 holds the only slot
        a.sent[0].report(AdapterResult.NotApplied("stale evidence")) // about attempt 1 only: not conclusive
        r.scheduler.advanceBy(0)
        assertEquals(2, a.sent.size)
        assertTrue(r.completions(s).isEmpty())
        r.send(s, callAct(idem, timeout = 1_000, label = "second")) // waits for the slot
        assertEquals(2, a.sent.size)
        a.sent[0].report(AdapterResult.Ok("applied by attempt 1"))
        r.scheduler.runReady()
        val (first, second) = r.keys()
        assertEquals(Outcome.Done("applied by attempt 1"), r.outcomeOf(first))
        assertEquals(3, a.sent.size) // the slot passed to the waiting call
        assertEquals(second, a.sent[2].invocation.callId.key)
        a.sent[1].report(AdapterResult.Failed("too late"))
        r.scheduler.runReady()
        assertTrue(r.events<TraceEvent.LateReportDropped>().any { it.callId.key == first })
        assertEquals(1, r.completions(s).size)
    }

    @Test
    fun `evidence of non-application from a retry is conclusive only for a Safe port`() {
        fun secondAttemptNotApplied(port: AdapterPort<String, String>): Pair<Rig, ScriptedAdapter<String, String>> {
            val a = ScriptedAdapter<String, String>()
            val r = rig { adapter(port, a, RetryPolicy(maxAttempts = 3)) }
            r.send(s, callAct(port, timeout = 1_000))
            a.sent[0].report(AdapterResult.Failed("timeout"))
            r.scheduler.advanceBy(0)
            a.sent[1].report(AdapterResult.NotApplied("rejected before processing"))
            r.scheduler.advanceBy(0)
            return r to a
        }
        val (safeRig, safeAdapter) = secondAttemptNotApplied(safe)
        assertEquals(2, safeAdapter.sent.size)
        assertEquals(Outcome.NotDone(NotDoneReason.NotApplied("rejected before processing")), safeRig.completions(s).single().outcome)
        val (idemRig, idemAdapter) = secondAttemptNotApplied(idem)
        assertEquals(3, idemAdapter.sent.size) // attempt 1 may have been applied: retry instead
        idemAdapter.sent[2].report(AdapterResult.NotApplied("rejected before processing"))
        idemRig.scheduler.advanceBy(0)
        assertEquals(Outcome.Unknown(UnknownReason.RetriesExhausted), idemRig.completions(s).single().outcome)
    }

    @Test
    fun `an adapter that throws is treated as a failed attempt`() {
        val r = rig { adapter(nonIdem, Adapter<String, String> { _, _ -> throw IllegalStateException("socket closed") }) }
        r.send(s, callAct(nonIdem))
        assertEquals(Outcome.Unknown(UnknownReason.LostAfterSend), r.completions(s).single().outcome)
        assertEquals(1, r.events<TraceEvent.CallSent>().size)
    }

    @Test
    fun `at a sent call's deadline the adapter is told to stop and its slot goes to the next call at once`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(nonIdem, a, maxInFlight = 1) }
        val t0 = r.scheduler.now()
        r.send(s, callAct(nonIdem, timeout = 100, label = "a"))
        r.send(s, callAct(nonIdem, timeout = 1_000, label = "b"))
        r.scheduler.advanceTo(t0 + 100)
        val (first, second) = r.keys()
        assertTrue(a.sent[0].cancelled)
        assertEquals(Outcome.Unknown(UnknownReason.DeadlineAfterSend), r.outcomeOf(first))
        assertEquals(t0 + 100, r.events<TraceEvent.CallSent>().single { it.callId.key == second }.at)
        r.send(s, callAct(nonIdem, timeout = 1_000, label = "c")) // the slot is taken again: c waits
        assertEquals(2, a.sent.size)
    }

    @Test
    fun `a slot freed at the exact deadline of a waiting call does not send it`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(nonIdem, a, maxInFlight = 1) }
        val t0 = r.scheduler.now()
        r.send(s, callAct(nonIdem, timeout = 1_000, label = "a"))
        r.send(s, callAct(nonIdem, timeout = 100, label = "b"))
        r.scheduler.jumpTo(t0 + 100)
        a.sent[0].report(AdapterResult.Ok("a done"))
        r.scheduler.runReady()
        assertEquals(1, a.sent.size)
        r.scheduler.advanceTo(t0 + 100)
        val (_, second) = r.keys()
        assertEquals(Outcome.NotDone(NotDoneReason.DeadlineBeforeSend), r.outcomeOf(second))
    }

    // ---------------------------------------------------------------- Ball-to-Ball

    @Test
    fun `a call to a quarantined or overloaded Ball ends NotDone without reaching it`() {
        val a = ScriptedAdapter<String, String>()
        val destPort = BallPort<Any?, Any?>("dest")
        val r = rig(Limits(maxInbox = 1)) {
            ball(ScriptBall("dest", onComplete = { _, _, _ -> error("broken") }))
            adapter(nonIdem, a)
        }
        // Two calls to the same healthy instance in one decision: the second finds its inbox full.
        r.send(s, Act("two") { st, ctx ->
            val k1 = ctx.newKey()
            val k2 = ctx.newKey()
            accept(st + k1 + k2, "ok", Call(k1, destPort, ok("x"), 100, target = "healthy"), Call(k2, destPort, ok("y"), 100, target = "healthy"))
        })
        val (k1, k2) = r.keys()
        assertEquals(Outcome.Done("ok"), r.outcomeOf(k1))
        assertEquals(Outcome.NotDone(NotDoneReason.Overloaded), r.outcomeOf(k2))
        // A quarantined instance refuses with the reason.
        r.send(BallId("dest", "broken"), callAct(nonIdem))
        a.sent[0].report(AdapterResult.Ok("x"))
        r.scheduler.runReady()
        r.send(s, Act("to broken") { st, ctx ->
            val k = ctx.newKey()
            accept(st + k, "ok", Call(k, destPort, ok("z"), 100, target = "broken"))
        })
        val fault = assertIs<Outcome.NotDone<*>>(r.outcomeOf(r.keys().last())).reason
        assertTrue((fault as NotDoneReason.Fault).message.contains("quarantined"))
    }

    @Test
    fun `a Ball reply after the caller's deadline is dropped and the caller keeps Unknown`() {
        val destPort = BallPort<Any?, Any?>("dest")
        val dest = BallId("dest", "d")
        val r = rig { ball(ScriptBall("dest")) }
        val t0 = r.scheduler.now()
        r.send(s, Act("ask") { st, ctx ->
            val k = ctx.newKey()
            accept(st + k, "ok", Call(k, destPort, Act("owe") { ds, dctx -> acceptLater(ds + dctx.requestId) }, 100, target = "d"))
        })
        r.scheduler.advanceTo(t0 + 100)
        val key = r.keys().single()
        assertEquals(Outcome.Unknown(UnknownReason.DeadlineAfterSend), r.outcomeOf(key))
        val rid = r.log(dest).single() as RequestId
        r.send(dest, Act("answer") { ds, _ -> accept(ds, "ok", Reply(rid, "late")) })
        assertTrue(r.events<TraceEvent.LateReportDropped>().any { it.callId == CallId(s, key) })
        assertEquals(1, r.completions(s).size)
    }

    // ---------------------------------------------------------------- recovery (Durable profile)

    @Test
    fun `recovery sends calls that were never sent and closes possibly-sent non-idempotent ones`() {
        val a = ScriptedAdapter<String, String>()
        val r = rig { adapter(nonIdem, a, maxInFlight = 1) }
        val t0 = r.scheduler.now()
        r.send(s, callAct(nonIdem, timeout = 1_000, label = "a"))
        r.send(s, callAct(nonIdem, timeout = 1_000, label = "b"))
        assertEquals(1, a.sent.size)
        r.restartAt(t0 + 10)
        val (first, second) = r.keys()
        assertEquals(Outcome.Unknown(UnknownReason.RecoveredAfterCrash), r.outcomeOf(first))
        assertEquals(2, a.sent.size)
        assertEquals(second, a.sent[1].invocation.callId.key)
        assertEquals(1, a.sent[1].invocation.attempt)
        // A report addressed to the crashed runtime changes nothing.
        a.sent[0].report(AdapterResult.Ok("too late"))
        r.scheduler.runReady()
        assertEquals(Outcome.Unknown(UnknownReason.RecoveredAfterCrash), r.outcomeOf(first))
        assertEquals(1, r.completions(s).size)
    }

    @Test
    fun `recovery at the exact deadline classifies each call by its phase and port`() {
        val charge = ScriptedAdapter<String, String>()
        val lookup = ScriptedAdapter<String, String>()
        val r = rig {
            adapter(nonIdem, charge, maxInFlight = 1)
            adapter(safe, lookup, RetryPolicy(maxAttempts = 3))
        }
        val t0 = r.scheduler.now()
        r.send(s, callAct(nonIdem, timeout = 100, label = "sent"))
        r.send(s, callAct(nonIdem, timeout = 100, label = "pending"))
        r.send(s, callAct(safe, timeout = 100, label = "safe"))
        r.restartAt(t0 + 100)
        val (sent, pending, safeKey) = r.keys()
        assertEquals(Outcome.Unknown(UnknownReason.DeadlineAfterSend), r.outcomeOf(sent))
        assertEquals(Outcome.NotDone(NotDoneReason.DeadlineBeforeSend), r.outcomeOf(pending))
        assertEquals(Outcome.NotDone(NotDoneReason.NoAnswer), r.outcomeOf(safeKey))
        assertEquals(1, charge.sent.size)
        assertEquals(1, lookup.sent.size)
    }

    @Test
    fun `recovery retries a repeatable call only while its retry budget lasts`() {
        val a = ScriptedAdapter<String, String>()
        val lookup = ScriptedAdapter<String, String>()
        val r = rig {
            adapter(idem, a, RetryPolicy(maxAttempts = 2))
            adapter(safe, lookup, RetryPolicy.None)
        }
        val t0 = r.scheduler.now()
        r.send(s, callAct(idem, timeout = 1_000))
        r.send(s, callAct(safe, timeout = 1_000))
        r.restartAt(t0 + 10)
        val (idemKey, safeKey) = r.keys()
        assertEquals(listOf(1, 2), a.sent.map { it.invocation.attempt })
        assertEquals(Outcome.NotDone(NotDoneReason.NoAnswer), r.outcomeOf(safeKey))
        r.restartAt(t0 + 20)
        assertEquals(2, a.sent.size)
        assertEquals(Outcome.Unknown(UnknownReason.RetriesExhausted), r.outcomeOf(idemKey))
    }

    @Test
    fun `a cancel committed before a crash is honoured for a call that was never sent`() {
        val a = ScriptedAdapter<String, String>()
        val store = MemoryStore()
        val k = CallKey("1.0")
        store.save(
            InstanceRecord(
                id = s, state = emptyList<Any?>(), revision = 1,
                calls = mapOf(k to CallRecord(k, nonIdem.name, null, "req", deadlineAt = 1_500, phase = Phase.Pending, attempts = 0, cancelRequested = true)),
            ),
        )
        val r = rig(store = store) { adapter(nonIdem, a) }
        assertEquals(Outcome.NotDone(NotDoneReason.Cancelled), r.outcomeOf(k))
        assertTrue(a.sent.isEmpty())
    }

    @Test
    fun `an unanswered request survives a restart and is reported overdue at its original deadline`() {
        val r = rig()
        val t0 = r.scheduler.now()
        r.send(s, Act("owe") { st, ctx -> acceptLater(st + ctx.requestId) }, timeoutMillis = 100)
        r.restartAt(t0 + 10)
        r.scheduler.advanceTo(t0 + 99)
        assertTrue(r.events<TraceEvent.ReplyOverdue>().isEmpty())
        r.scheduler.advanceTo(t0 + 100)
        assertEquals(t0 + 100, r.events<TraceEvent.ReplyOverdue>().single().at)
    }

    @Test
    fun `a Ball call re-sent after a crash is answered from the destination's record, not decided twice`() {
        val destPort = BallPort<Any?, Any?>("dest")
        val dest = BallId("dest", "d")
        lateinit var r: Rig
        var crashed = false
        val work = Act("work") { ds, _ ->
            if (!crashed) {
                crashed = true
                r.runtime.crash() // the destination's commit completes; nothing after it is processed
            }
            accept(ds + "worked", "result")
        }
        r = rig { ball(ScriptBall("dest")) }
        val t0 = r.scheduler.now()
        r.send(s, Act("ask") { st, ctx ->
            val k = ctx.newKey()
            accept(st + k, "ok", Call(k, destPort, work, 1_000, target = "d"))
        })
        assertTrue(r.completions(s).isEmpty())
        r.restartAt(t0 + 10)
        assertEquals(Outcome.Done("result"), r.outcomeOf(r.keys().single()))
        assertEquals(listOf<Any?>("worked"), r.log(dest))
    }

    // ---------------------------------------------------------------- notices

    @Test
    fun `notices blocked by a full inbox are retried in order and acknowledged durably`() {
        val sub = BallId("sub", "x")
        val r = rig(Limits(maxInbox = 1, noticeRetryMillis = 7)) {
            ball(ScriptBall("sub"))
            subscribe(topic, "sub") { "x" }
        }
        val t0 = r.scheduler.now()
        r.send(s, Act("burst") { st, _ -> accept(st, "ok", Publish(topic, 1), Publish(topic, 2), Publish(topic, 3)) })
        r.scheduler.advanceTo(t0 + 100)
        assertEquals(listOf(1L, 2L, 3L), r.log(sub).map { (it as Notice<*>).sequence })
        assertEquals(listOf(t0, t0 + 7, t0 + 14), r.events<TraceEvent.Committed>().filter { it.id == sub }.map { it.at })
        assertTrue(r.store.load(s)!!.outbox.isEmpty())
    }

    @Test
    fun `a notice stays in the outbox until every subscriber has committed it`() {
        val r = rig {
            ball(ScriptBall("good"))
            ball(ScriptBall("bad", onObserve = { _, _, _ -> error("broken") }))
            subscribe(topic, "good") { "x" }
            subscribe(topic, "bad") { "y" }
        }
        r.send(s, Act("publish") { st, _ -> accept(st, "ok", Publish(topic, 1)) })
        val pending = r.store.load(s)!!.outbox.single()
        assertEquals(setOf(BallId("bad", "y")), pending.pendingSubscribers)
        assertEquals(1, r.log(BallId("good", "x")).size)
    }

    @Test
    fun `notices recovered from the outbox are applied once and acknowledged if already applied`() {
        fun recoverWith(lastSeen: Long): Rig {
            val store = MemoryStore()
            val sub = BallId("sub", "x")
            store.save(InstanceRecord(s, emptyList<Any?>(), 1, outbox = listOf(NoticeRecord(1, topic.name, 7, setOf(sub))), nextNoticeSequence = 2))
            store.save(InstanceRecord(sub, emptyList<Any?>(), 1, lastSeen = if (lastSeen > 0) mapOf(s to lastSeen) else emptyMap()))
            return rig(store = store) {
                ball(ScriptBall("sub"))
                subscribe(topic, "sub") { "x" }
            }
        }
        val fresh = recoverWith(lastSeen = 0)
        assertEquals(listOf<Any?>(7), fresh.log(BallId("sub", "x")).map { (it as Notice<*>).payload })
        assertTrue(fresh.store.load(s)!!.outbox.isEmpty())
        val duplicate = recoverWith(lastSeen = 1)
        assertEquals(emptyList(), duplicate.log(BallId("sub", "x")))
        assertTrue(duplicate.store.load(s)!!.outbox.isEmpty())
    }

    // ---------------------------------------------------------------- context, timers, trace

    @Test
    fun `call keys derive from the committed revision and timers fire exactly at their delay`() {
        val ball = ScriptBall(onComplete = { st, c, ctx -> step(st + c + ctx.revision) })
        val r = rig(ball = ball)
        val t0 = r.scheduler.now()
        r.send(s, Act("timers") { st, ctx ->
            val k1 = ctx.newKey()
            val k2 = ctx.newKey()
            accept(st + ctx.revision + k1 + k2, "ok", Timer.after(k1, 10), Timer.after(k2, 20))
        })
        assertEquals(listOf<Any?>(1L, CallKey("1.0"), CallKey("1.1")), r.log(s))
        r.scheduler.advanceTo(t0 + 9)
        assertTrue(r.completions(s).isEmpty())
        r.scheduler.advanceTo(t0 + 10)
        assertEquals(Outcome.Done(Unit), r.outcomeOf(CallKey("1.0")))
        assertEquals(2L, r.log(s).last())
        r.send(s, Act("cancel") { st, _ -> accept(st, "ok", Cancel(CallKey("1.1"))) })
        assertEquals(Outcome.NotDone(NotDoneReason.NotApplied("timer cancelled")), r.outcomeOf(CallKey("1.1")))
        val committed = r.events<TraceEvent.Committed>()
        assertEquals(listOf(CallId(s, CallKey("1.0")), CallId(s, CallKey("1.1"))), committed.first().newCalls)
        assertEquals(CallId(s, CallKey("1.0")), committed[1].completedCall)
        assertEquals(Outcome.Done(Unit), committed[1].completedOutcome)
    }

    @Test
    fun `an open timer survives a restart and still fires at its original due time, never before`() {
        fun timerAcrossRestart(restartAfter: Long): Pair<Rig, Long> {
            val r = rig()
            val t0 = r.scheduler.now()
            r.send(s, Act("timer") { st, ctx -> ctx.newKey().let { k -> accept(st + k, "ok", Timer.after(k, 100)) } })
            r.restartAt(t0 + restartAfter)
            return r to t0
        }
        val (early, t0) = timerAcrossRestart(restartAfter = 30)
        early.scheduler.advanceTo(t0 + 99)
        assertTrue(early.completions(s).isEmpty())
        early.scheduler.advanceTo(t0 + 100)
        assertEquals(Outcome.Done(Unit), early.completions(s).single().outcome)
        assertEquals(t0 + 100, early.events<TraceEvent.CallResolved>().single().at)
        assertEquals(listOf(1, 2), early.events<TraceEvent.CallSent>().map { it.attempt })
        // Restarted after the due time but before the deadline: fires at once (late, not early).
        val (late, t1) = timerAcrossRestart(restartAfter = 500)
        late.scheduler.advanceBy(0)
        assertEquals(Outcome.Done(Unit), late.completions(s).single().outcome)
        assertEquals(t1 + 500, late.events<TraceEvent.CallResolved>().single().at)
        // Restarted after the deadline: the runtime can only say the timer did not answer.
        val (expired, _) = timerAcrossRestart(restartAfter = 100 + Timer.MARGIN_MILLIS)
        assertEquals(Outcome.NotDone(NotDoneReason.NoAnswer), expired.completions(s).single().outcome)
    }

    @Test
    fun `timer calls must be made with Timer after`() {
        val r = rig()
        val raw = r.send(s, Act("raw") { st, ctx -> accept(st, "ok", Call(ctx.newKey(), Timer.port, 10L, 5_000)) })
        assertFault(r.outcome(raw), "Timer.after")
    }

    @Test
    fun `an explicitly bound timer adapter replaces the built-in one`() {
        val custom = ScriptedAdapter<Long, Unit>()
        val r = rig { adapter(Timer.port, custom) }
        r.send(s, Act("timer") { st, ctx -> accept(st, "ok", Timer.after(ctx.newKey(), 10)) })
        assertEquals(1, custom.sent.size)
    }

    @Test
    fun `every decision is bracketed in the trace and watchers see each committed state`() {
        val r = rig()
        val seen = ArrayList<Any?>()
        r.runtime.watch(s) { seen += it }
        r.scheduler.runReady()
        r.send(s, ok("a"))
        r.send(s, Act("no") { _, _ -> pokeball.kernel.reject("no") })
        r.send(s, ok("b"))
        assertEquals(3, r.events<TraceEvent.DecideStarted>().size)
        assertEquals(3, r.events<TraceEvent.DecideFinished>().size)
        assertEquals(listOf<Any?>(listOf("a"), listOf("a", "b")), seen)
    }

    @Test
    fun `requests need a positive timeout and a known Ball type`() {
        val r = rig()
        assertFailsWith<IllegalArgumentException> { r.runtime.request<Any?, Any?>(s, ok(), 0) { } }
        val unknown = r.send(BallId("ghost", "g"), ok(), timeoutMillis = 1)
        assertEquals(Outcome.NotDone(NotDoneReason.NotApplied("unknown Ball type 'ghost'")), r.outcome(unknown))
        assertNull(r.runtime.inspect(BallId("ghost", "g")))
    }

    @Test
    fun `limits, retry policies and compositions reject invalid configuration`() {
        val invalid = listOf<Pair<String, () -> Any>>(
            "maxInbox" to { Limits(maxInbox = 0) },
            "maxOpenCalls" to { Limits(maxOpenCalls = 0) },
            "maxOutputsPerDecision" to { Limits(maxOutputsPerDecision = 0) },
            "maxTimeoutMillis" to { Limits(maxTimeoutMillis = 0) },
            "maxOutbox" to { Limits(maxOutbox = 0) },
            "repliedRetention" to { Limits(repliedRetention = -1) },
            "repliedRetentionMillis" to { Limits(repliedRetentionMillis = -1) },
            "noticeRetryMillis" to { Limits(noticeRetryMillis = 0) },
            "maxAttempts" to { RetryPolicy(maxAttempts = 0) },
            "backoffMillis" to { RetryPolicy(backoffMillis = -1) },
            "duplicate Ball type" to { Composition().ball(ScriptBall()).ball(ScriptBall()) },
            "duplicate port" to { Composition().adapter(safe, ScriptedAdapter()).adapter(safe, ScriptedAdapter()) },
            "maxInFlight" to { Composition().adapter(safe, ScriptedAdapter(), maxInFlight = 0) },
            "subscription to unknown type" to { Runtime(Composition().subscribe(topic, "nobody") { "x" }, ManualScheduler(), MemoryStore()) },
        )
        for ((name, make) in invalid) assertFailsWith<IllegalArgumentException>(name) { make() }
        Limits(
            maxInbox = 1, maxOpenCalls = 1, maxOutputsPerDecision = 1, maxTimeoutMillis = 1, maxOutbox = 1,
            repliedRetention = 0, repliedRetentionMillis = 0, noticeRetryMillis = 1,
        )
        RetryPolicy(maxAttempts = 1, backoffMillis = 0)
    }

    // ---------------------------------------------------------------- the production scheduler

    @Test
    fun `the thread scheduler runs tasks in order on one thread and honours delays and cancellation`() {
        ThreadScheduler("test-loop").use { scheduler ->
            val order = Collections.synchronizedList(ArrayList<String>())
            val threads = ConcurrentHashMap.newKeySet<String>()
            val delayed = CountDownLatch(1)
            scheduler.execute { order += "a"; threads += Thread.currentThread().name }
            scheduler.execute { order += "b"; threads += Thread.currentThread().name }
            scheduler.schedule(30) { order += "cancelled" }.cancel()
            val start = scheduler.now()
            scheduler.schedule(20) { order += "delayed"; delayed.countDown() }
            assertTrue(delayed.await(5, TimeUnit.SECONDS))
            assertTrue(scheduler.now() - start >= 20)
            Thread.sleep(50)
            assertEquals(listOf("a", "b", "delayed"), order.toList())
            assertEquals(setOf("test-loop"), threads.toSet())
        }
    }

    @Test
    fun `a runtime on the thread scheduler answers requests`() {
        ThreadScheduler().use { scheduler ->
            val runtime = Runtime(Composition().ball(CounterBall()), scheduler, MemoryStore())
            runtime.start()
            val result = CompletableFuture<Outcome<Any?>>()
            runtime.request<CounterMsg, Any?>(BallId("counter", "c"), CounterMsg.Increment, 1_000) { result.complete(it) }
            assertEquals(Outcome.Done(CounterReply.Value(1)), result.get(5, TimeUnit.SECONDS))
        }
    }
}
