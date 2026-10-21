package bench.ballarch.refadapter.app

import ballarch.kernel.BallId
import ballarch.runtime.Adapter
import ballarch.runtime.AdapterResult
import ballarch.runtime.Composition
import ballarch.runtime.MemoryStore
import ballarch.runtime.RetryPolicy
import ballarch.runtime.Runtime
import bench.ballarch.api.StartCheckout
import bench.ballarch.checkout.CheckoutFlow
import bench.ballarch.checkout.CheckoutState
import bench.ballarch.inventory.InventoryBall
import bench.ballarch.inventory.Stock
import bench.ballarch.refadapter.payment.ChargeRequest
import bench.ballarch.refadapter.payment.PaymentBall
import bench.ballarch.refadapter.payment.ProviderCharge
import bench.ballarch.refadapter.payment.ProviderStatus
import bench.env.ChargeResult
import bench.env.ChargeStatus
import bench.env.CheckoutSystem
import bench.env.ConnectionRefused
import bench.env.Order
import bench.env.Provider
import bench.env.Reply
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World

/** Outbound adapters: one report per attempt, no retries, `NotApplied` only with evidence. */
private fun <T> report(reply: Reply<T>): AdapterResult<T> = when (reply) {
    is Reply.Answer -> AdapterResult.Ok(reply.value)
    is Reply.Failure -> if (reply.error is ConnectionRefused) AdapterResult.NotApplied("connection refused") else AdapterResult.Failed(reply.error.toString())
}

/**
 * The charge adapter sends the call identity as the idempotency key. As in the
 * reference HTTP adapter (`examples/shop/payments-http`), an attempt has no
 * timeout of its own: it ends at the call's deadline. The runtime therefore
 * repeats only an attempt that failed before the deadline (a connection reset,
 * or a refusal after an earlier attempt), never one whose answer was lost.
 */
fun chargeAdapter(provider: Provider) = Adapter<ChargeRequest, ChargeResult> { invocation, done ->
    provider.charge(invocation.request.orderId, invocation.request.amount, idempotencyKey = invocation.callId.toString()) { done(report(it)) }
    null
}

fun statusAdapter(provider: Provider) = Adapter<String, ChargeStatus> { invocation, done ->
    provider.status(invocation.request) { done(report(it)) }
    null
}

/** The composition root. */
fun composition(provider: Provider): Composition = Composition()
    .ball(InventoryBall(Scenario.STOCK))
    .ball(PaymentBall(horizonMillis = Scenario.PROVIDER_HORIZON_MILLIS))
    .ball(CheckoutFlow())
    .adapter(ProviderCharge, chargeAdapter(provider), RetryPolicy(maxAttempts = 3))
    .adapter(ProviderStatus, statusAdapter(provider))

class BallarchSystem(private val wiring: (Provider) -> Composition = ::composition) : CheckoutSystem {
    private lateinit var store: MemoryStore
    private lateinit var runtime: Runtime

    override fun boot(world: World) {
        store = world.db.durable("ballarch.store") { MemoryStore() }
        runtime = Runtime(wiring(world.provider), world.scheduler, store)
        runtime.start()
    }

    override fun start(order: Order) =
        runtime.request<StartCheckout, Unit>(BallId("checkout", order.id), StartCheckout(order), timeoutMillis = 1_000, idempotencyKey = order.id) { }

    override fun crash() = runtime.crash()

    private fun checkout(orderId: String) = store.load(BallId("checkout", orderId))?.state as CheckoutState?

    override fun accepted(orderId: String) = checkout(orderId)?.let { it != CheckoutState.New } == true

    override fun outcome(orderId: String): Terminal? = when (checkout(orderId)) {
        is CheckoutState.Completed -> Terminal.Completed
        is CheckoutState.Failed -> Terminal.Failed
        is CheckoutState.NeedsAttention -> Terminal.NeedsAttention
        else -> null
    }

    override fun reserved(): Map<String, Int> = (store.load(BallId("inventory", Scenario.SKU))?.state as Stock?)?.reservations.orEmpty()
}
