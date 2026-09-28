package pokeball.testkit

import pokeball.kernel.AdapterPort
import pokeball.kernel.CallId
import pokeball.kernel.EffectClass
import pokeball.runtime.Adapter
import pokeball.runtime.AdapterResult
import pokeball.runtime.Cancellable
import pokeball.runtime.Invocation

/**
 * What really happened at simulated destinations, independent of what any
 * caller believes. Oracles compare callers' outcomes against it.
 */
public class GroundTruth {
    /**
     * Identity epoch. Durable runtimes keep call identities across restarts (epoch
     * stays 0); a Transient restart starts a new world whose call identities may
     * repeat, so the harness increments the epoch and ground truth is kept per epoch.
     */
    public var epoch: Int = 0
    private val byEpoch = HashMap<Int, EpochTruth>()

    public fun at(epoch: Int): EpochTruth = byEpoch.getOrPut(epoch) { EpochTruth() }

    public val current: EpochTruth get() = at(epoch)

    /** Ground truth of epoch 0 (the only epoch for Durable runs). */
    public val applied: MutableMap<CallId, Int> get() = at(0).applied
    public val processed: MutableSet<CallId> get() = at(0).processed
    public val deliveries: MutableMap<CallId, Int> get() = at(0).deliveries
    public val effectOf: MutableMap<CallId, EffectClass> get() = at(0).effectOf

    public fun appliedCount(id: CallId): Int = applied[id] ?: 0
}

public class EpochTruth {
    /** Effect applications per call. Safe ports never apply effects. */
    public val applied: MutableMap<CallId, Int> = LinkedHashMap()

    /** Requests the destination actually handled, whatever the effect class. */
    public val processed: MutableSet<CallId> = LinkedHashSet()

    /** Requests that reached the destination (including duplicates). */
    public val deliveries: MutableMap<CallId, Int> = LinkedHashMap()

    /** Effect class of each call's port, recorded on first delivery attempt. */
    public val effectOf: MutableMap<CallId, EffectClass> = LinkedHashMap()

    public fun appliedCount(id: CallId): Int = applied[id] ?: 0
}

/** The fate of one attempt in the simulated network and destination. */
public enum class Fate {
    /** The destination applies the request and answers in time. */
    Reply,

    /** The destination applies the request; the answer arrives after the deadline. */
    ReplyLate,

    /** The request is lost before reaching the destination. */
    LoseRequest,

    /** The destination applies the request; the answer is lost. */
    LoseReply,

    /** The destination applies the request; the connection then fails with an error. */
    ErrorAfterApply,

    /** The connection is refused before the request is written: provably nothing applied. */
    RefuseConnection,

    /** The destination refuses the request without applying it. */
    Refuse,
}

/**
 * A simulated adapter plus destination. Each attempt's fate is taken from the
 * [chooser]; the destination deduplicates by call identity when the port is
 * [EffectClass.Idempotent], like a provider honouring idempotency keys.
 */
public class SimPort<Req, Res>(
    public val port: AdapterPort<Req, Res>,
    private val scheduler: SimScheduler,
    private val chooser: Chooser,
    private val truth: GroundTruth,
    private val fates: List<Fate> = Fate.entries,
    private val latencyMillis: Long = 10,
    private val refuse: ((Req) -> Res)? = null,
    private val handle: (Req) -> Res,
) : Adapter<Req, Res> {
    private val answers = HashMap<Pair<Int, CallId>, Res>()

    override fun execute(invocation: Invocation<Req>, done: (AdapterResult<Res>) -> Unit): Cancellable? {
        val allowed = if (refuse == null) fates - Fate.Refuse else fates
        val fate = allowed[chooser.choose(allowed.size, "${port.name}:${invocation.callId.key}#${invocation.attempt}")]
        val id = invocation.callId
        val epochOf = truth.epoch
        val truth = truth.current // captured at invocation: late deliveries count in the sender's epoch
        truth.effectOf.putIfAbsent(id, port.effect)
        fun deliver(): Res {
            truth.deliveries.merge(id, 1, Int::plus)
            truth.processed += id
            val answerKey = epochOf to id
            val previous = answers[answerKey]
            if (previous != null && port.effect == EffectClass.Idempotent) return previous
            if (port.effect != EffectClass.Safe) truth.applied.merge(id, 1, Int::plus)
            return handle(invocation.request).also { answers[answerKey] = it }
        }
        when (fate) {
            Fate.Reply -> scheduler.schedule(latencyMillis) { done(AdapterResult.Ok(deliver())) }
            Fate.ReplyLate -> scheduler.schedule(latencyMillis) {
                val answer = deliver()
                scheduler.schedule((invocation.deadlineAt - scheduler.now()).coerceAtLeast(0) + latencyMillis) {
                    done(AdapterResult.Ok(answer))
                }
            }
            Fate.LoseRequest -> Unit
            Fate.LoseReply -> scheduler.schedule(latencyMillis) { deliver() }
            Fate.ErrorAfterApply -> scheduler.schedule(latencyMillis) {
                deliver()
                done(AdapterResult.Failed("connection reset after write"))
            }
            Fate.RefuseConnection -> scheduler.schedule(latencyMillis) { done(AdapterResult.NotApplied("connection refused")) }
            Fate.Refuse -> scheduler.schedule(latencyMillis) {
                truth.deliveries.merge(id, 1, Int::plus)
                truth.processed += id
                done(AdapterResult.Refused(refuse!!(invocation.request)))
            }
        }
        return null
    }
}
