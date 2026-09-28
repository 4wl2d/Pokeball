package pokeball.examples.shop.app

import pokeball.examples.shop.payments.ChargeRequest
import pokeball.examples.shop.payments.ChargeResult
import pokeball.examples.shop.payments.ChargeStatus
import pokeball.examples.shop.payments.PaymentBall
import pokeball.examples.shop.payments.ProviderCharge
import pokeball.examples.shop.payments.ProviderStatus
import pokeball.examples.shop.payments.StatusRequest
import pokeball.runtime.Adapter
import pokeball.runtime.Composition

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
