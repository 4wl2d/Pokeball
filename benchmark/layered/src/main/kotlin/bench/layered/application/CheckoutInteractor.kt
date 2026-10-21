package bench.layered.application

import bench.layered.domain.CheckoutOrder
import bench.layered.domain.CheckoutOrder.Status

/** Input port. */
interface CheckoutInputPort {
    fun place(order: CheckoutOrder)
    suspend fun process(orderId: String)
}

/**
 * The checkout use case (application business rules). It depends only on its
 * output ports; the framework and the provider live behind them. Processing
 * resumes from the persisted status, so the same use case drives new orders
 * and orders interrupted by a restart.
 */
class CheckoutInteractor(
    private val orders: OrderRepository,
    private val inventory: InventoryRepository,
    private val payments: PaymentGateway,
) : CheckoutInputPort {
    override fun place(order: CheckoutOrder) {
        if (orders.find(order.id) == null) orders.save(order)
    }

    override suspend fun process(orderId: String) {
        var order = orders.find(orderId) ?: return
        if (order.status == Status.NEW) {
            if (!inventory.reserve(order.id, order.sku, order.quantity)) {
                orders.save(order.failed("out of stock"))
                return
            }
            order = order.reserved()
            orders.save(order)
        }
        if (order.status == Status.RESERVED) {
            order = try {
                order.paid(payments.charge(order.id, order.amount))
            } catch (e: PaymentFailedException) {
                inventory.release(order.id)
                orders.save(order.failed(e.message ?: "payment failed"))
                return
            }
            orders.save(order)
        }
        if (order.status == Status.PAID) orders.save(order.completed())
    }

    /** Resumes every order that a restart interrupted. */
    suspend fun resumeUnfinished() {
        for (order in orders.unfinished()) process(order.id)
    }
}
