package bench.ballarch.api

import ballarch.kernel.BallPort
import bench.env.Order

// Inventory: one instance per SKU.
sealed interface InventoryRequest {
    data class Reserve(val orderId: String, val quantity: Int) : InventoryRequest
    data class Release(val orderId: String) : InventoryRequest
}

sealed interface InventoryReply {
    data object Reserved : InventoryReply
    data object OutOfStock : InventoryReply
    data object Released : InventoryReply
}

val InventoryPort = BallPort<InventoryRequest, InventoryReply>("inventory")

// Payments: one instance per order. Capture is idempotent per order.
data class Capture(val amount: Long)

sealed interface PaymentReply {
    data class Captured(val reference: String) : PaymentReply
    data class NotCaptured(val reason: String) : PaymentReply

    /** Reconciliation gave up: an operator must decide. */
    data object Unresolved : PaymentReply
}

val PaymentsPort = BallPort<Capture, PaymentReply>("payment")

// Checkout: one instance per order.
data class StartCheckout(val order: Order)
