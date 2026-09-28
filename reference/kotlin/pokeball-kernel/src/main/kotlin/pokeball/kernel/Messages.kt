package pokeball.kernel

/**
 * Something a decision asks the runtime to do after the decision is committed.
 * `R` is the reply type of the deciding Ball (used only by [Reply]).
 */
public sealed interface Output<out R>

/**
 * Ask a port to perform a request. Committing a decision with a call creates an
 * obligation: the runtime later delivers exactly one [Completion] with this [key].
 *
 * @property target instance key when [port] is a [BallPort]; ignored for adapters.
 * @property timeoutMillis finite deadline, measured from commit.
 */
public data class Call<Req, Res>(
    val key: CallKey,
    val port: Port<Req, Res>,
    val request: Req,
    val timeoutMillis: Long,
    val target: String? = null,
) : Output<Nothing> {
    init {
        require(timeoutMillis > 0) { "a call needs a positive, finite timeout" }
        require(port !is BallPort<*, *> || target != null) { "a call to ${port.name} needs a target instance key" }
    }
}

/** Ask the runtime to cancel an open call. Its completion still arrives and may be [Outcome.Done]. */
public data class Cancel(val key: CallKey) : Output<Nothing>

/** Answer an inbound request that was accepted earlier with a deferred reply. */
public data class Reply<out R>(val to: RequestId, val value: R) : Output<R>

/** Publish a notice to the subscribers declared for [topic]. */
public data class Publish<T>(val topic: Topic<T>, val payload: T) : Output<Nothing>

/** The terminal outcome of a call made by this instance. Delivered exactly once per committed call. */
public data class Completion<Res>(
    val key: CallKey,
    val port: Port<*, Res>,
    val outcome: Outcome<Res>,
)

/**
 * Returns the outcome typed by [expected] when this completion belongs to a call
 * made on that port, or `null` otherwise.
 */
@Suppress("UNCHECKED_CAST")
public fun <Res> Completion<*>.outcomeOf(expected: Port<*, Res>): Outcome<Res>? =
    if (port === expected || port.name == expected.name) outcome as Outcome<Res> else null

/** A published notice delivered to a subscribing instance. */
public data class Notice<T>(
    val topic: Topic<T>,
    val source: BallId,
    val sequence: Long,
    val payload: T,
)

/** Returns the payload typed by [expected] when this notice belongs to that topic, or `null`. */
@Suppress("UNCHECKED_CAST")
public fun <T> Notice<*>.payloadOf(expected: Topic<T>): T? =
    if (topic === expected || topic.name == expected.name) payload as T else null
