package bench.saga.order

import bench.env.Order
import bench.env.SimDb
import bench.saga.messages.SagaState.Outcome
import bench.saga.messages.SagaState.Step
import bench.saga.kit.Message
import bench.saga.kit.Outbox
import bench.saga.kit.markProcessed
import bench.saga.kit.processedBefore
import bench.saga.messages.Channels
import bench.saga.messages.ChargeCard
import bench.saga.messages.InventoryCommand
import bench.saga.messages.SagaReply
import bench.saga.messages.SagaState

/**
 * The orchestrator of an orchestration-based saga: reserve stock, charge the
 * card (the pivot transaction), approve; compensate by releasing stock. The
 * saga state and the commands it sends commit in one transaction.
 */
class CreateOrderSaga(private val db: SimDb) {
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
        val next = when {
            saga.step == Step.Reserving && reply is SagaReply.StockReserved -> {
                Outbox.send(tx, Channels.PAYMENT, ChargeCard(saga.order.id, saga.order.amount))
                saga.copy(step = Step.Charging)
            }
            saga.step == Step.Reserving && reply is SagaReply.StockUnavailable -> saga.copy(step = Step.Done, outcome = Outcome.Rejected)
            saga.step == Step.Charging && reply is SagaReply.PaymentSucceeded -> saga.copy(step = Step.Done, outcome = Outcome.Approved)
            saga.step == Step.Charging && reply is SagaReply.PaymentFailed -> {
                Outbox.send(tx, Channels.INVENTORY, InventoryCommand.ReleaseStock(saga.order.id))
                saga.copy(step = Step.Compensating)
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
