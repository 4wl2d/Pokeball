package bench.saga.inventory

import bench.env.SimDb
import bench.saga.kit.Message
import bench.saga.kit.Outbox
import bench.saga.kit.markProcessed
import bench.saga.kit.processedBefore
import bench.saga.messages.Channels
import bench.saga.messages.InventoryCommand
import bench.saga.messages.SagaReply

/** The inventory participant: its changes and its reply commit together. */
class InventoryService(private val db: SimDb, private val initialStock: Int) {
    fun handle(message: Message) = db.transaction { tx ->
        if (tx.processedBefore("inventory", message)) return@transaction
        tx.markProcessed("inventory", message)
        val stock = tx.get<Int>("inventory/stock") ?: initialStock
        val reply = when (val command = message.payload as InventoryCommand) {
            is InventoryCommand.ReserveStock -> when {
                tx.get<Int>("inventory/reservation/${command.orderId}") != null -> SagaReply.StockReserved(command.orderId)
                stock < command.quantity -> SagaReply.StockUnavailable(command.orderId)
                else -> {
                    tx.put("inventory/stock", stock - command.quantity)
                    tx.put("inventory/reservation/${command.orderId}", command.quantity)
                    SagaReply.StockReserved(command.orderId)
                }
            }
            is InventoryCommand.ReleaseStock -> {
                val quantity = tx.get<Int>("inventory/reservation/${command.orderId}")
                if (quantity != null) {
                    tx.put("inventory/stock", stock + quantity)
                    tx.delete("inventory/reservation/${command.orderId}")
                }
                SagaReply.StockReleased(command.orderId)
            }
        }
        Outbox.send(tx, Channels.SAGA_REPLIES, reply)
    }
}
