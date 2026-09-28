package pokeball.kernel

/** Address of one Ball instance: its type name plus an instance key. */
public data class BallId(val type: String, val key: String) {
    init {
        require(type.isNotBlank()) { "Ball type must not be blank" }
    }

    override fun toString(): String = "$type/$key"
}

/**
 * Identifies an outbound call within the calling instance.
 *
 * Keys are created with [Context.newKey], which derives them from the
 * revision being committed, so they are deterministic and unique among the
 * instance's calls without reading a clock or a random source.
 */
@JvmInline
public value class CallKey(public val value: String) {
    override fun toString(): String = value
}

/** Globally unique identity of an outbound call: caller instance plus key. */
public data class CallId(val caller: BallId, val key: CallKey) {
    override fun toString(): String = "$caller#$key"
}

/** Identity of an inbound request, unique within the receiving instance. */
@JvmInline
public value class RequestId(public val value: String) {
    override fun toString(): String = value
}
