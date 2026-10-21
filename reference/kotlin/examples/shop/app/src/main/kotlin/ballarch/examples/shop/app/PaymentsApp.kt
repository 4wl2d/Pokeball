package ballarch.examples.shop.app

import ballarch.examples.shop.payments.ChargeRequest
import ballarch.examples.shop.payments.ChargeResult
import ballarch.examples.shop.payments.ChargeStatus
import ballarch.examples.shop.payments.PaymentBall
import ballarch.examples.shop.payments.ProviderCharge
import ballarch.examples.shop.payments.ProviderStatus
import ballarch.examples.shop.payments.StatusRequest
import ballarch.runtime.Adapter
import ballarch.runtime.Composition

/** Adapters for the payment provider; supplied by the deployment (HTTP client, simulator, …). */
class ProviderAdapters(
    val charge: Adapter<ChargeRequest, ChargeResult>,
    val status: Adapter<StatusRequest, ChargeStatus>,
    /** Time after which an unanswered charge can no longer be applied by the provider. */
    val horizonMillis: Long,
)

/** Composition root of the payments feature alone (slice S2). */
fun paymentsComposition(provider: ProviderAdapters): Composition =
    Composition()
        .ball(PaymentBall(provider.horizonMillis))
        .adapter(ProviderCharge, provider.charge) // non-idempotent: no retries allowed
        .adapter(ProviderStatus, provider.status)
