package bench.runner

import ballarch.kernel.Ball
import ballarch.kernel.Completion
import ballarch.kernel.Context
import ballarch.kernel.NotDoneReason
import ballarch.kernel.Outcome
import ballarch.kernel.Step
import ballarch.runtime.Composition
import bench.ballarch.api.Capture
import bench.ballarch.api.PaymentReply
import bench.ballarch.app.BallarchSystem
import bench.ballarch.app.chargeAdapter
import bench.ballarch.app.statusAdapter
import bench.ballarch.checkout.CheckoutFlow
import bench.ballarch.inventory.InventoryBall
import bench.ballarch.payment.PaymentBall
import bench.ballarch.payment.PaymentState
import bench.ballarch.payment.ProviderCharge
import bench.ballarch.payment.ProviderStatus
import bench.env.CheckoutSystem
import bench.env.ChargeResult
import bench.env.Provider
import bench.env.Scenario
import bench.hardened.adapters.ProviderPaymentGateway
import bench.hardened.app.HardenedSystem
import bench.hardened.application.ChargeAttempt
import bench.hardened.application.PaymentGateway

/*
 * Seeded bugs: each correct implementation with one plausible mistake, injected
 * from outside its code. They check that a zero violation rate is not a blind
 * spot of the oracles or of how a system's records are read.
 */

/** Ballarch: the payment Ball treats an unknown charge as not charged. */
private class AssumeNotCharged(private val inner: PaymentBall) : Ball<PaymentState, Capture, PaymentReply> by inner {
    override fun complete(state: PaymentState, completion: Completion<*>, ctx: Context): Step<PaymentState, PaymentReply> {
        val lie = completion.port.name == ProviderCharge.name && completion.outcome is Outcome.Unknown
        return inner.complete(state, if (lie) Completion(completion.key, ProviderCharge, Outcome.NotDone<ChargeResult>(NotDoneReason.NotApplied("assumed"))) else completion, ctx)
    }
}

private fun lyingComposition(provider: Provider) = Composition()
    .ball(InventoryBall(Scenario.STOCK))
    .ball(AssumeNotCharged(PaymentBall(horizonMillis = Scenario.PROVIDER_HORIZON_MILLIS)))
    .ball(CheckoutFlow())
    .adapter(ProviderCharge, chargeAdapter(provider))
    .adapter(ProviderStatus, statusAdapter(provider))

/** Hardened: the gateway reports an uncertain charge as not sent, so it is charged again. */
private class OptimisticGateway(private val inner: PaymentGateway) : PaymentGateway by inner {
    override suspend fun charge(orderId: String, amount: Long): ChargeAttempt =
        inner.charge(orderId, amount).let { if (it == ChargeAttempt.Uncertain) ChargeAttempt.NotSent else it }
}

val seeded: List<Pair<String, () -> CheckoutSystem>> = listOf(
    "ballarch: unknown charge treated as not charged" to { BallarchSystem(::lyingComposition) },
    "hardened: uncertain charge treated as not sent" to { HardenedSystem { OptimisticGateway(ProviderPaymentGateway(it)) } },
)
