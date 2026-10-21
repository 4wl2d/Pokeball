package bench.careful.saga.order

import bench.careful.saga.messages.Channels
import bench.careful.saga.messages.InventoryCommand
import bench.careful.saga.messages.PaymentCommand
import bench.careful.saga.messages.SagaReply
import bench.careful.saga.messages.SagaState
import bench.careful.saga.messages.SagaState.Outcome
import bench.careful.saga.messages.SagaState.Step
import bench.env.Order
import bench.env.SimDb
import bench.saga.kit.Message
import bench.saga.kit.Outbox
import bench.saga.kit.markProcessed
import bench.saga.kit.processedBefore

/**
 * The orchestrator of an orchestration-based saga: reserve stock, charge the
 * card (the pivot transaction), approve; compensate by releasing stock, but
 * only when the card was not charged. A payment whose outcome is unknown parks
 * the saga: it asks the payment participant to check, and hands the order to
 * an operator when the checks run out. The saga state and the commands it
 * sends commit in one transaction.
 */
class CreateOrderSaga(private val db: SimDb, private val maxChecks: Int = 3) {
    fun start(order: Order) = db.transaction { tx ->
        if (tx.get<SagaState>(key(order.id)) != null) return@transaction
        tx.put(key(order.id), SagaState(order, Step.Reserving))
        Outbox.send(tx, Channels.INVENTORY, InventoryCommand.ReserveStock(order.id, order.sku, order.quantity))
    }

    fun onReply(message: Message) = db.transaction { tx ->
        if (tx.processedBefore("saga", message)) return@transaction
        tx.markProcessed("saga", message)
        val reply = message.payload as SagaReply
        val saga = tx.get<SagaState>(key(reply.orderId)) ?: return@transaction
        val paying = saga.step == Step.Charging || saga.step == Step.PaymentPending
        val next = when {
            saga.step == Step.Reserving && reply is SagaReply.StockReserved -> {
                Outbox.send(tx, Channels.PAYMENT, PaymentCommand.ChargeCard(saga.order.id, saga.order.amount))
                saga.copy(step = Step.Charging)
            }
            saga.step == Step.Reserving && reply is SagaReply.StockUnavailable -> saga.copy(step = Step.Done, outcome = Outcome.Rejected)
            paying && reply is SagaReply.PaymentSucceeded -> saga.copy(step = Step.Done, outcome = Outcome.Approved)
            paying && reply is SagaReply.PaymentFailed -> {
                Outbox.send(tx, Channels.INVENTORY, InventoryCommand.ReleaseStock(saga.order.id))
                saga.copy(step = Step.Compensating)
            }
            paying && reply is SagaReply.PaymentUnknown ->
                if (saga.checks < maxChecks) {
                    Outbox.send(tx, Channels.PAYMENT, PaymentCommand.CheckPayment(saga.order.id))
                    saga.copy(step = Step.PaymentPending, checks = saga.checks + 1)
                } else {
                    saga.copy(step = Step.Done, outcome = Outcome.NeedsAttention)
                }
            saga.step == Step.Compensating && reply is SagaReply.StockReleased -> saga.copy(step = Step.Done, outcome = Outcome.Rejected)
            else -> saga
        }
        tx.put(key(reply.orderId), next)
    }

    companion object {
        fun key(orderId: String) = "saga/$orderId"
    }
}
