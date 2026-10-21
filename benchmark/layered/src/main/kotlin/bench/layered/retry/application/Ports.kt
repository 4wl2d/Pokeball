package bench.layered.retry.application

import bench.layered.retry.domain.CheckoutOrder

/** Output port: persistence of orders. */
interface OrderRepository {
    fun find(id: String): CheckoutOrder?
    fun save(order: CheckoutOrder)
    fun unfinished(): List<CheckoutOrder>
}

/** Output port: stock reservations, one per order. */
interface InventoryRepository {
    /** Reserves [quantity] for [orderId]; repeating it for the same order changes nothing. Returns false when out of stock. */
    fun reserve(orderId: String, sku: String, quantity: Int): Boolean
    fun release(orderId: String)
}

/** Output port: the payment provider. */
interface PaymentGateway {
    /**
     * Charges [amount] for [orderId]. Throws [PaymentFailedException] when the payment did not succeed,
     * and [PaymentUncertainException] when it is not known whether it did.
     */
    suspend fun charge(orderId: String, amount: Long): String
}

class PaymentFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PaymentUncertainException(message: String, cause: Throwable? = null) : Exception(message, cause)
