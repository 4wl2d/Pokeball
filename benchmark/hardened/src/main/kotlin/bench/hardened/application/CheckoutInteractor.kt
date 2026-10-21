package bench.hardened.application

import bench.hardened.domain.CheckoutOrder
import bench.hardened.domain.CheckoutOrder.Status
import kotlinx.coroutines.delay

interface CheckoutInputPort {
    fun place(order: CheckoutOrder)
    suspend fun process(orderId: String)
}

/**
 * The checkout use case, hardened by hand against an unreliable provider
 * without idempotency keys:
 * - the order is saved as CHARGING before each charge request (write-ahead);
 * - a charge is repeated only after evidence that it was not sent;
 * - an uncertain charge is reconciled: wait until no request can still arrive,
 *   then ask the provider's status endpoint, and never charge again;
 * - after a restart, CHARGING orders are reconciled, never re-charged.
 */
class CheckoutInteractor(
    private val orders: OrderRepository,
    private val inventory: InventoryRepository,
    private val payments: PaymentGateway,
    private val horizonMillis: Long,
    private val maxSends: Int = 3,
    private val maxQueries: Int = 3,
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
        if (order.status == Status.RESERVED) order = pay(order)
        if (order.status == Status.CHARGING) order = reconcile(order)
        if (order.status == Status.PAID) orders.save(order.completed())
    }

    suspend fun resumeUnfinished() {
        for (order in orders.unfinished()) process(order.id)
    }

    private suspend fun pay(order: CheckoutOrder): CheckoutOrder {
        repeat(maxSends) {
            orders.save(order.charging())
            when (val attempt = payments.charge(order.id, order.amount)) {
                is ChargeAttempt.Charged -> return order.paid(attempt.reference).also(orders::save)
                ChargeAttempt.Declined -> return fail(order, "declined")
                ChargeAttempt.NotSent -> orders.save(order) // provably not charged: back to RESERVED
                ChargeAttempt.Uncertain -> return order.charging()
            }
        }
        return fail(order, "payment provider unreachable")
    }

    private suspend fun reconcile(order: CheckoutOrder): CheckoutOrder {
        repeat(maxQueries) {
            delay(horizonMillis)
            when (val status = payments.status(order.id)) {
                is PaymentStatus.Charged -> return order.paid(status.reference).also(orders::save)
                PaymentStatus.NoCharge -> return fail(order, "not charged")
                PaymentStatus.Unavailable -> Unit
            }
        }
        return order.needsAttention().also(orders::save)
    }

    private fun fail(order: CheckoutOrder, reason: String): CheckoutOrder {
        inventory.release(order.id)
        return order.failed(reason).also(orders::save)
    }
}
