package bench.hardened.domain

/**
 * Enterprise business rules. CHARGING is written before every charge request:
 * from then on the order may have been charged, whatever happens to the request.
 */
data class CheckoutOrder(
    val id: String,
    val sku: String,
    val quantity: Int,
    val amount: Long,
    val status: Status = Status.NEW,
    val paymentReference: String? = null,
    val failureReason: String? = null,
) {
    enum class Status { NEW, RESERVED, CHARGING, PAID, COMPLETED, FAILED, NEEDS_ATTENTION }

    val isTerminal: Boolean get() = status == Status.COMPLETED || status == Status.FAILED || status == Status.NEEDS_ATTENTION

    fun reserved() = copy(status = Status.RESERVED)

    fun charging() = copy(status = Status.CHARGING)

    fun paid(reference: String) = copy(status = Status.PAID, paymentReference = reference)

    fun completed() = copy(status = Status.COMPLETED)

    fun failed(reason: String) = copy(status = Status.FAILED, failureReason = reason)

    fun needsAttention() = copy(status = Status.NEEDS_ATTENTION)
}
