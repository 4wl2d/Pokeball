package bench.saga.messages

import bench.env.Order

sealed interface InventoryCommand {
    data class ReserveStock(val orderId: String, val sku: String, val quantity: Int) : InventoryCommand
    data class ReleaseStock(val orderId: String) : InventoryCommand
}

data class ChargeCard(val orderId: String, val amount: Long)

sealed interface SagaReply {
    val orderId: String

    data class StockReserved(override val orderId: String) : SagaReply
    data class StockUnavailable(override val orderId: String) : SagaReply
    data class StockReleased(override val orderId: String) : SagaReply
    data class PaymentSucceeded(override val orderId: String, val reference: String) : SagaReply
    data class PaymentFailed(override val orderId: String, val reason: String) : SagaReply
}

data class SagaState(val order: Order, val step: Step, val outcome: Outcome? = null) {
    enum class Step { Reserving, Charging, Compensating, Done }
    enum class Outcome { Approved, Rejected }
}

object Channels {
    const val INVENTORY = "inventory"
    const val PAYMENT = "payment"
    const val SAGA_REPLIES = "saga-replies"
}
