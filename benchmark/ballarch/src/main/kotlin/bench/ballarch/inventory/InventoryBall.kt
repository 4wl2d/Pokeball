package bench.ballarch.inventory

import ballarch.kernel.Ball
import ballarch.kernel.Decision
import ballarch.kernel.RequestContext
import ballarch.kernel.accept
import ballarch.kernel.reject
import bench.ballarch.api.InventoryReply
import bench.ballarch.api.InventoryRequest

data class Stock(val available: Int, val reservations: Map<String, Int> = emptyMap())

/** Owns the stock of one SKU. Reservations are per order, so repeating a request changes nothing. */
class InventoryBall(private val initialStock: Int) : Ball<Stock, InventoryRequest, InventoryReply> {
    override val type = "inventory"

    override fun initial(key: String) = Stock(initialStock)

    override fun decide(state: Stock, request: InventoryRequest, ctx: RequestContext): Decision<Stock, InventoryReply> =
        when (request) {
            is InventoryRequest.Reserve -> when {
                request.orderId in state.reservations -> accept(state, InventoryReply.Reserved)
                state.available < request.quantity -> reject(InventoryReply.OutOfStock)
                else -> accept(
                    Stock(state.available - request.quantity, state.reservations + (request.orderId to request.quantity)),
                    InventoryReply.Reserved,
                )
            }
            is InventoryRequest.Release -> {
                val quantity = state.reservations[request.orderId] ?: 0
                accept(Stock(state.available + quantity, state.reservations - request.orderId), InventoryReply.Released)
            }
        }
}
