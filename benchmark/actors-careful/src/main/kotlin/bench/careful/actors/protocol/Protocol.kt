package bench.careful.actors.protocol

import bench.env.ChargeStatus
import bench.env.Order
import kotlinx.coroutines.CompletableDeferred

sealed interface InventoryCommand {
    data class Reserve(val orderId: String, val sku: String, val quantity: Int, val replyTo: CompletableDeferred<Boolean>) : InventoryCommand

    /** Acknowledged, so that a checkout is not reported failed before its stock is back. */
    data class Release(val orderId: String, val replyTo: CompletableDeferred<Unit>) : InventoryCommand
}

/** What one charge request established, as the payment actor reports it. */
sealed interface ChargeReply {
    data class Charged(val reference: String) : ChargeReply
    data object Declined : ChargeReply

    /** Evidence that the request never reached the provider (connection refused). */
    data object NotSent : ChargeReply

    /** The request may have been processed: an error after it was written, or no answer before the ask timed out. */
    data object Unknown : ChargeReply
}

sealed interface PaymentCommand {
    data class Charge(val orderId: String, val amount: Long, val replyTo: CompletableDeferred<ChargeReply>) : PaymentCommand
    data class QueryStatus(val orderId: String, val replyTo: CompletableDeferred<ChargeStatus>) : PaymentCommand
}

sealed interface CheckoutCommand {
    /** Starts or, after recovery, resumes the current stage. A charge that may have been sent is not sent again. */
    data object Drive : CheckoutCommand
    data class Reserved(val result: Result<Boolean>) : CheckoutCommand
    data class Charged(val reply: ChargeReply) : CheckoutCommand

    /** The timer that ends the wait past the provider's delivery horizon. */
    data object CheckStatus : CheckoutCommand

    /** The provider's status, or null when it could not be read. */
    data class StatusChecked(val status: ChargeStatus?) : CheckoutCommand
    data class Released(val acknowledged: Boolean) : CheckoutCommand
}

/**
 * The durable state of one checkout actor. [Stage.Charging] is persisted before
 * every charge request, so after a restart it means "possibly charged".
 */
data class CheckoutState(val order: Order, val stage: Stage, val sends: Int = 0, val queries: Int = 0, val reason: String? = null) {
    enum class Stage { Reserving, Charging, Reconciling, Releasing, Completed, Failed, NeedsAttention }
}

/** The durable state of the inventory actor. */
data class InventoryState(val stock: Int, val reservations: Map<String, Int> = emptyMap())
