package ballarch.testkit

import ballarch.kernel.BallId
import ballarch.kernel.Caller
import ballarch.kernel.Outcome
import ballarch.runtime.Composition
import ballarch.runtime.Limits
import ballarch.runtime.MemoryStore
import ballarch.runtime.RecordingTracer
import ballarch.runtime.Runtime
import ballarch.runtime.TraceEvent

/** Which state survives a crash (the profile under test). */
public enum class Profile { Transient, Durable }

/**
 * A simulated deployment: one runtime on a [SimScheduler], simulated ports,
 * ground truth, a shared trace across restarts, and optional crash injection.
 *
 * @param wiring builds the composition for this harness (called at every start,
 *   so restarted runtimes get fresh adapter objects bound to the same ground truth).
 */
public class Harness(
    public val chooser: Chooser,
    public val profile: Profile = Profile.Durable,
    public val limits: Limits = Limits(),
    private val wiring: Harness.() -> Composition,
) {
    public val scheduler: SimScheduler = SimScheduler(start = 1_000)
    public val truth: GroundTruth = GroundTruth()
    public val tracer: RecordingTracer = RecordingTracer()
    public var store: MemoryStore = MemoryStore()
        private set
    public lateinit var runtime: Runtime
        private set
    public var crashes: Int = 0
        private set
    private var tasksRun = 0
    private var crashAtTask = -1
    private var restartDelay = 50L

    public fun start(): Harness = apply {
        runtime = Runtime(wiring(), scheduler, store, limits, tracer)
        runtime.start()
    }

    /**
     * Chooses (through the chooser) whether and before which of the first
     * [window] loop tasks the runtime crashes once; it restarts after [delay] ms.
     */
    public fun maybeCrashOnce(window: Int, delay: Long = 50): Harness = apply {
        crashAtTask = chooser.choose(window + 1, "crashBeforeTask") - 1
        restartDelay = delay
        scheduler.beforeTask = {
            if (tasksRun++ == crashAtTask) crashAndRestart()
        }
    }

    public fun crashAndRestart(delay: Long = restartDelay) {
        runtime.crash()
        crashes++
        if (profile == Profile.Transient) truth.epoch++
        scheduler.schedule(delay) {
            if (profile == Profile.Transient) store = MemoryStore()
            runtime = Runtime(wiring(), scheduler, store, limits, tracer)
            runtime.start()
        }
    }

    /** Submits an external request and records its outcome in [externalOutcomes]. */
    public fun <M> send(target: BallId, message: M, timeoutMillis: Long = 1_000, idempotencyKey: String? = null, caller: Caller = Caller.Anonymous) {
        val index = externalOutcomes.size
        externalOutcomes += null
        runtime.request<M, Any?>(target, message, timeoutMillis, idempotencyKey, caller) { externalOutcomes[index] = it }
    }

    public val externalOutcomes: MutableList<Outcome<Any?>?> = ArrayList()

    public fun runUntilIdle(): Harness = apply { scheduler.runUntilIdle() }

    /** Checks every runtime invariant on the trace and ground truth. */
    public fun violations(): List<String> =
        Oracles.check(tracer.events, truth, quiescent = scheduler.pending == 0, profile = profile, crashed = crashes > 0)

    public fun assertInvariants() {
        val v = violations()
        if (v.isNotEmpty()) {
            throw AssertionError(
                "invariant violations:\n  " + v.joinToString("\n  ") +
                    "\nchoices: " + chooser.history.joinToString(", "),
            )
        }
    }

    /**
     * Accepted requests left unanswered past their deadline (R9). A Ball SHOULD
     * answer every accepted request, so these are not part of [violations].
     */
    public fun replyLeaks(): List<String> =
        tracer.events.filterIsInstance<TraceEvent.ReplyOverdue>()
            .map { "R9: ${it.id} left ${it.requestId.value} unanswered past its deadline (at ${it.at})" }

    public fun assertNoReplyLeaks() {
        val leaks = replyLeaks()
        if (leaks.isNotEmpty()) {
            throw AssertionError("reply leaks:\n  " + leaks.joinToString("\n  ") + "\nchoices: " + chooser.history.joinToString(", "))
        }
    }
}
