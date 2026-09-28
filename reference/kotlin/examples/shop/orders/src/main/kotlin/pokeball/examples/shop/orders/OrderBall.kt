package pokeball.examples.shop.orders

import pokeball.examples.shop.orders.api.OrderReply
import pokeball.examples.shop.orders.api.OrderRequest
import pokeball.kernel.Ball
import pokeball.kernel.Decision
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

data class OrderState(val confirmedWith: String?)

object OrderBall : Ball<OrderState, OrderRequest, OrderReply> {
    override val type = "order"

    override fun initial(key: String) = OrderState(confirmedWith = null)

    override fun decide(state: OrderState, request: OrderRequest, ctx: RequestContext): Decision<OrderState, OrderReply> = when (request) {
        is OrderRequest.Confirm -> {
            val next = state.confirmedWith?.let { state } ?: OrderState(request.paymentReference)
            accept(next, OrderReply.Confirmed(next.confirmedWith!!))
        }
        OrderRequest.Get -> accept(state, state.confirmedWith?.let { OrderReply.Confirmed(it) } ?: OrderReply.NotConfirmed)
    }
}
