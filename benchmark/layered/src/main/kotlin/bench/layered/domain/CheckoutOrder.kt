package bench.layered.domain

/** Enterprise business rules: an order moves through checkout in one direction. */
data class CheckoutOrder(
    val id: String,
    val sku: String,
    val quantity: Int,
    val amount: Long,
    val status: Status = Status.NEW,
    val paymentReference: String? = null,
    val failureReason: String? = null,
) {
    enum class Status { NEW, RESERVED, PAID, COMPLETED, FAILED }

    val isTerminal: Boolean get() = status == Status.COMPLETED || status == Status.FAILED

    fun reserved() = copy(status = Status.RESERVED)

    fun paid(reference: String) = copy(status = Status.PAID, paymentReference = reference)

    fun completed() = copy(status = Status.COMPLETED)

    fun failed(reason: String) = copy(status = Status.FAILED, failureReason = reason)
}
