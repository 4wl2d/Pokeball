package pokeball.examples.shop.inventory

import pokeball.examples.shop.inventory.api.InventoryReply
import pokeball.examples.shop.inventory.api.InventoryRequest
import pokeball.kernel.Ball
import pokeball.kernel.Decision
import pokeball.kernel.RequestContext
import pokeball.kernel.accept
import pokeball.kernel.reject

data class Stock(val onHand: Int, val reservations: Map<String, Int>) {
    val available: Int get() = onHand - reservations.values.sum()
}

class InventoryBall(private val initialStock: Int = 10) : Ball<Stock, InventoryRequest, InventoryReply> {
    override val type = "inventory"

    override fun initial(key: String) = Stock(initialStock, emptyMap())

    override fun decide(state: Stock, request: InventoryRequest, ctx: RequestContext): Decision<Stock, InventoryReply> = when (request) {
        is InventoryRequest.Reserve -> when {
            request.checkoutId in state.reservations -> accept(state, InventoryReply.Reserved)
            request.quantity > state.available -> reject(InventoryReply.OutOfStock)
            else -> accept(state.copy(reservations = state.reservations + (request.checkoutId to request.quantity)), InventoryReply.Reserved)
        }
        is InventoryRequest.Release -> accept(state.copy(reservations = state.reservations - request.checkoutId), InventoryReply.Released)
        InventoryRequest.GetAvailable -> accept(state, InventoryReply.Available(state.available, state.reservations))
    }
}
