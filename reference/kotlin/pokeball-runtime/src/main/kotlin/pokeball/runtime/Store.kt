package pokeball.runtime

import pokeball.kernel.BallId
import pokeball.kernel.CallId
import pokeball.kernel.CallKey
import pokeball.kernel.Outcome
import pokeball.kernel.RequestId
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything the runtime keeps for one Ball instance. A record is replaced as a
 * whole, atomically, so the business state, the call ledger, the inbound
 * request table and the notice outbox can never disagree after a crash.
 */
public data class InstanceRecord(
    val id: BallId,
    val state: Any?,
    /** Revision of the last committed decision; 0 before the first commit. */
    val revision: Long,
    /** Open outbound calls, keyed by call key. */
    val calls: Map<CallKey, CallRecord> = emptyMap(),
    /** Inbound requests that are still owed a reply, and recently answered ones kept for deduplication. */
    val inbound: Map<RequestId, InboundRecord> = emptyMap(),
    /** Published notices not yet committed by every subscriber. */
    val outbox: List<NoticeRecord> = emptyList(),
    val nextNoticeSequence: Long = 1,
    /** Highest notice sequence applied from each publisher (subscriber-side deduplication). */
    val lastSeen: Map<BallId, Long> = emptyMap(),
)

/** Durable phase of an open call (see the write-ahead rule in the specification). */
public enum class Phase {
    /** Committed; never handed to a transport. The runtime can still withdraw it truthfully. */
    Pending,

    /** Recorded as possibly sent before any transport was invoked. */
    Sending,
}

public data class CallRecord(
    val key: CallKey,
    val portName: String,
    val target: String?,
    val request: Any?,
    val deadlineAt: Long,
    val phase: Phase,
    val attempts: Int,
    val cancelRequested: Boolean = false,
)

public sealed interface ReplyTarget {
    /** A caller outside the runtime. Its callback is volatile and does not survive a restart. */
    public data object External : ReplyTarget

    /** Another Ball instance waiting for the completion of [callId]. */
    public data class ToCall(val callId: CallId) : ReplyTarget
}

public data class InboundRecord(
    val requestId: RequestId,
    val replyTo: ReplyTarget,
    /** Kept to detect idempotency-key reuse with a different message. */
    val message: Any?,
    val deadlineAt: Long,
    /** `null` while the reply is owed. */
    val reply: Outcome<Any?>? = null,
    val repliedAt: Long? = null,
)

public data class NoticeRecord(
    val sequence: Long,
    val topic: String,
    val payload: Any?,
    val pendingSubscribers: Set<BallId>,
)

/**
 * Where instance records live. [save] must replace the record atomically.
 *
 * Profiles differ only in what survives a crash: a Transient runtime starts with
 * an empty store after a restart; a Durable runtime is restarted on the same
 * store.
 */
public interface Store {
    public fun load(id: BallId): InstanceRecord?

    public fun save(record: InstanceRecord)

    public fun ids(): Set<BallId>

    /** Increments and returns the incarnation counter; called once per runtime start. */
    public fun nextIncarnation(): Long
}

/**
 * An in-memory store. Records are immutable values, so sharing the same
 * [MemoryStore] between a crashed runtime and its successor simulates a durable
 * database; giving the successor a new instance simulates the Transient profile.
 */
public class MemoryStore : Store {
    private val records = ConcurrentHashMap<BallId, InstanceRecord>()
    private var incarnation = 0L

    /** Number of successful [save] calls; used by tests and benchmarks. */
    @Volatile public var saves: Long = 0
        private set

    override fun load(id: BallId): InstanceRecord? = records[id]

    override fun save(record: InstanceRecord) {
        records[record.id] = record
        saves++
    }

    override fun ids(): Set<BallId> = records.keys.toSet()

    @Synchronized
    override fun nextIncarnation(): Long = ++incarnation
}
