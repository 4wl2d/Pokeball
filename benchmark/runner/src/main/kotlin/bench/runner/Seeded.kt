package bench.runner

import ballarch.kernel.Ball
import ballarch.kernel.Completion
import ballarch.kernel.Context
import ballarch.kernel.NotDoneReason
import ballarch.kernel.Outcome
import ballarch.kernel.Step
import ballarch.runtime.Composition
import ballarch.runtime.MemoryStore
import ballarch.runtime.Phase
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
import bench.careful.actors.app.CarefulActorsSystem
import bench.careful.mvu.app.CarefulMvuSystem
import bench.careful.saga.app.CarefulSagaSystem
import bench.env.CheckoutSystem
import bench.env.ChargeResult
import bench.env.Provider
import bench.env.Scenario
import bench.env.World
import bench.hardened.adapters.ProviderPaymentGateway
import bench.hardened.app.HardenedSystem
import bench.hardened.application.ChargeAttempt
import bench.hardened.application.PaymentGateway
import bench.hardened.domain.CheckoutOrder
import bench.careful.actors.protocol.CheckoutState as CarefulCheckoutState
import bench.careful.mvu.core.Model as CarefulMvuModel
import bench.careful.mvu.core.OrderModel as CarefulMvuOrder

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

/*
 * Recovery bugs: each changes only what a restarted incarnation finds, so it
 * can show up only in a run that crashes. Each corrupts the durable records at
 * a restart in the way a faulty recovery path would read them.
 */

/**
 * Ballarch runtime: after a restart, recovery forgets the write-ahead record of
 * a charge that may have been sent (phase `Sending` read as `Pending`), so the
 * runtime sends it again instead of ending it `Unknown`.
 */
private class ForgetWriteAhead(private val inner: BallarchSystem = BallarchSystem()) : CheckoutSystem by inner {
    override fun boot(world: World) {
        if (world.incarnation > 0) {
            val store = world.db.durable("ballarch.store") { MemoryStore() }
            for (id in store.ids()) {
                val record = checkNotNull(store.load(id))
                val calls = record.calls.mapValues { (_, c) -> if (c.portName == ProviderCharge.name && c.phase == Phase.Sending) c.copy(phase = Phase.Pending) else c }
                if (calls != record.calls) store.save(record.copy(calls = calls))
            }
        }
        inner.boot(world)
    }
}

/** Hardened: after a restart, an order saved as CHARGING is resumed as RESERVED, so it is charged again. */
private class ForgetCharging(private val inner: HardenedSystem = HardenedSystem()) : CheckoutSystem by inner {
    override fun boot(world: World) {
        if (world.incarnation > 0) {
            world.db.transaction { tx ->
                for (k in tx.keys("order/")) {
                    val order = checkNotNull(tx.get<CheckoutOrder>(k))
                    if (order.status == CheckoutOrder.Status.CHARGING) tx.put(k, order.reserved())
                }
            }
        }
        inner.boot(world)
    }
}

/** Careful actors: after a restart, a checkout persisted as Charging is resumed from Reserving, so it is charged again. */
private class ActorsRedriveCharge(private val inner: CarefulActorsSystem = CarefulActorsSystem()) : CheckoutSystem by inner {
    override fun boot(world: World) {
        if (world.incarnation > 0) {
            world.db.transaction { tx ->
                for (k in tx.keys("actor/checkout/")) {
                    val state = checkNotNull(tx.get<CarefulCheckoutState>(k))
                    if (state.stage == CarefulCheckoutState.Stage.Charging) tx.put(k, state.copy(stage = CarefulCheckoutState.Stage.Reserving))
                }
            }
        }
        inner.boot(world)
    }
}

/** Careful outbox + saga: after a restart, the payment participant's charge-attempted rows are gone, so a redelivered command charges again. */
private class SagaForgetAttempt(private val inner: CarefulSagaSystem = CarefulSagaSystem()) : CheckoutSystem by inner {
    override fun boot(world: World) {
        if (world.incarnation > 0) world.db.transaction { tx -> for (k in tx.keys("payment/attempted/")) tx.delete(k) }
        inner.boot(world)
    }
}

/**
 * Careful MVU: after a restart, the saved model has lost the orders that were
 * still paying (their stock is back), so the client sends them again and they
 * are charged again, instead of being reconciled by `init`.
 */
private class MvuDropPaying(private val inner: CarefulMvuSystem = CarefulMvuSystem()) : CheckoutSystem by inner {
    override fun boot(world: World) {
        if (world.incarnation > 0) {
            world.db.transaction { tx ->
                val model = tx.get<CarefulMvuModel>("mvu/model")
                val paying = model?.orders?.values.orEmpty().filter { it.stage == CarefulMvuOrder.Stage.Paying }
                if (model != null && paying.isNotEmpty()) {
                    tx.put("mvu/model", model.copy(stock = model.stock + paying.sumOf { it.order.quantity }, orders = model.orders - paying.map { it.order.id }.toSet()))
                }
            }
        }
        inner.boot(world)
    }
}

/** A correct implementation with one injected bug. A [recovery] bug can only show up in a run that crashes. */
data class SeededBug(val name: String, val system: () -> CheckoutSystem, val recovery: Boolean)

val seeded: List<SeededBug> = listOf(
    SeededBug("ballarch: unknown charge treated as not charged", { BallarchSystem(::lyingComposition) }, recovery = false),
    SeededBug("hardened: uncertain charge treated as not sent", { HardenedSystem { OptimisticGateway(ProviderPaymentGateway(it)) } }, recovery = false),
    SeededBug("ballarch runtime: possibly-sent charge sent again after a restart", ::ForgetWriteAhead, recovery = true),
    SeededBug("hardened: CHARGING resumed as RESERVED after a restart", ::ForgetCharging, recovery = true),
    SeededBug("careful actors: Charging resumed as Reserving after a restart", ::ActorsRedriveCharge, recovery = true),
    SeededBug("careful MVU: paying orders dropped from the saved model at a restart", ::MvuDropPaying, recovery = true),
    SeededBug("careful saga: charge-attempted rows lost at a restart", ::SagaForgetAttempt, recovery = true),
)
