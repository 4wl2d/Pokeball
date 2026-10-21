package bench.actors.protocol

import bench.env.ChargeResult
import bench.env.Order
import kotlinx.coroutines.CompletableDeferred

sealed interface InventoryCommand {
    data class Reserve(val orderId: String, val sku: String, val quantity: Int, val replyTo: CompletableDeferred<Boolean>) : InventoryCommand
    data class Release(val orderId: String) : InventoryCommand
}

sealed interface PaymentCommand {
    data class Charge(val orderId: String, val amount: Long, val replyTo: CompletableDeferred<ChargeResult>) : PaymentCommand
}

sealed interface CheckoutCommand {
    /** Starts or, after recovery, re-drives the current stage. */
    data object Drive : CheckoutCommand
    data class Reserved(val result: Result<Boolean>) : CheckoutCommand
    data class Charged(val result: Result<ChargeResult>) : CheckoutCommand
}

/** The durable state of one checkout actor. */
data class CheckoutState(val order: Order, val stage: Stage, val reason: String? = null) {
    enum class Stage { Reserving, Paying, Completed, Failed }
}

/** The durable state of the inventory actor. */
data class InventoryState(val stock: Int, val reservations: Map<String, Int> = emptyMap())
