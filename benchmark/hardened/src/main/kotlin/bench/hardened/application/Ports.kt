package bench.hardened.application

import bench.hardened.domain.CheckoutOrder

interface OrderRepository {
    fun find(id: String): CheckoutOrder?
    fun save(order: CheckoutOrder)
    fun unfinished(): List<CheckoutOrder>
}

interface InventoryRepository {
    fun reserve(orderId: String, sku: String, quantity: Int): Boolean
    fun release(orderId: String)
}

/** What one charge request established. */
sealed interface ChargeAttempt {
    data class Charged(val reference: String) : ChargeAttempt
    data object Declined : ChargeAttempt

    /** Evidence that the request never reached the provider (connection refused). */
    data object NotSent : ChargeAttempt

    /** The request may have been processed: timeout, or an error after it was written. */
    data object Uncertain : ChargeAttempt
}

sealed interface PaymentStatus {
    data class Charged(val reference: String) : PaymentStatus
    data object NoCharge : PaymentStatus
    data object Unavailable : PaymentStatus
}

/** The provider has no idempotency keys, so a charge is never repeated after [ChargeAttempt.Uncertain]. */
interface PaymentGateway {
    suspend fun charge(orderId: String, amount: Long): ChargeAttempt
    suspend fun status(orderId: String): PaymentStatus
}
