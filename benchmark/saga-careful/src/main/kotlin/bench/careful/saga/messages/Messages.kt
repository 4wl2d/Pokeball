package bench.careful.saga.messages

import bench.env.Order

sealed interface InventoryCommand {
    data class ReserveStock(val orderId: String, val sku: String, val quantity: Int) : InventoryCommand
    data class ReleaseStock(val orderId: String) : InventoryCommand
}

sealed interface PaymentCommand {
    data class ChargeCard(val orderId: String, val amount: Long) : PaymentCommand

    /** Ask the provider whether the order was charged, once no earlier request can still take effect. */
    data class CheckPayment(val orderId: String) : PaymentCommand
}

sealed interface SagaReply {
    val orderId: String

    data class StockReserved(override val orderId: String) : SagaReply
    data class StockUnavailable(override val orderId: String) : SagaReply
    data class StockReleased(override val orderId: String) : SagaReply
    data class PaymentSucceeded(override val orderId: String, val reference: String) : SagaReply

    /** The card was not charged. */
    data class PaymentFailed(override val orderId: String, val reason: String) : SagaReply

    /** The card may or may not have been charged. */
    data class PaymentUnknown(override val orderId: String) : SagaReply
}

data class SagaState(val order: Order, val step: Step, val outcome: Outcome? = null, val checks: Int = 0) {
    /** [Step.PaymentPending]: the saga is parked until a payment check settles the charge. */
    enum class Step { Reserving, Charging, PaymentPending, Compensating, Done }
    enum class Outcome { Approved, Rejected, NeedsAttention }
}

object Channels {
    const val INVENTORY = "inventory"
    const val PAYMENT = "payment"
    const val SAGA_REPLIES = "saga-replies"
}
