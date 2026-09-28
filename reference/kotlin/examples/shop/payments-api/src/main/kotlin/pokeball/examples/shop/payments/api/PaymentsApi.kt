package pokeball.examples.shop.payments.api

import pokeball.kernel.BallPort
import pokeball.kernel.Topic

/**
 * Public contract of the payments feature. Other features depend on this module
 * only. One payment instance exists per order; its instance key is the order id.
 */
sealed interface PaymentRequest {
    /** Capture [amount] for this order. Repeating it is safe: the answer reflects the order's payment. */
    data class Capture(val amount: Long) : PaymentRequest

    data object GetStatus : PaymentRequest
}

sealed interface PaymentReply {
    data class Captured(val reference: String) : PaymentReply

    data class NotCaptured(val reason: String) : PaymentReply

    /** The provider's answer is not known yet; a [PaymentResolved] notice follows. */
    data object Pending : PaymentReply

    data object AmountMismatch : PaymentReply

    data object NoPayment : PaymentReply

    data object Busy : PaymentReply
}

val PaymentsPort = BallPort<PaymentRequest, PaymentReply>("payment")

/** Published once per order when its payment is known to be captured or not captured. */
data class PaymentResolution(val orderId: String, val captured: Boolean, val reference: String?)

val PaymentResolved = Topic<PaymentResolution>("payment.resolved")
