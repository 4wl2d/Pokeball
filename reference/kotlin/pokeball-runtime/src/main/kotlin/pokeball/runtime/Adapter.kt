package pokeball.runtime

import pokeball.kernel.CallId

/**
 * Executes calls made on one [pokeball.kernel.AdapterPort].
 *
 * Contract (checked by the adapter conformance kit in `pokeball-testkit`):
 * - report through [done] at most once per invocation; later reports are ignored;
 * - never retry internally; retries belong to the runtime (see [RetryPolicy]);
 * - report [AdapterResult.NotApplied] only with evidence that the request had
 *   no effect; when in doubt report [AdapterResult.Failed], which the runtime
 *   classifies by the port's effect class;
 * - use [Invocation.callId] as the idempotency key when the destination supports one.
 */
public fun interface Adapter<Req, Res> {
    public fun execute(invocation: Invocation<Req>, done: (AdapterResult<Res>) -> Unit): Cancellable?
}

/** One attempt to perform a call. */
public data class Invocation<Req>(
    val callId: CallId,
    val request: Req,
    val attempt: Int,
    val deadlineAt: Long,
)

public sealed interface AdapterResult<out Res> {
    /** The destination processed the request. */
    public data class Ok<out Res>(val value: Res) : AdapterResult<Res>

    /** The destination refused the request without applying anything. */
    public data class Refused<out Res>(val value: Res) : AdapterResult<Res>

    /** Evidence that nothing was applied, for example a connection refused before the request was written. */
    public data class NotApplied(val evidence: String) : AdapterResult<Nothing>

    /** No answer, or an error after the request may have left. The runtime classifies it. */
    public data class Failed(val error: String) : AdapterResult<Nothing>
}

/** Runtime-owned retry budget for a port. Only [pokeball.kernel.EffectClass.Safe] and Idempotent ports retry. */
public data class RetryPolicy(val maxAttempts: Int = 1, val backoffMillis: Long = 0) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
        require(backoffMillis >= 0) { "backoffMillis must not be negative" }
    }

    public companion object {
        public val None: RetryPolicy = RetryPolicy()
    }
}
