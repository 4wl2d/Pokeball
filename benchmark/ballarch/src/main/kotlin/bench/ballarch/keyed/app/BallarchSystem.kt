package bench.ballarch.keyed.app

import ballarch.kernel.BallId
import ballarch.runtime.Adapter
import ballarch.runtime.AdapterResult
import ballarch.runtime.Cancellable
import ballarch.runtime.Composition
import ballarch.runtime.MemoryStore
import ballarch.runtime.RetryPolicy
import ballarch.runtime.Runtime
import ballarch.runtime.Scheduler
import bench.ballarch.api.StartCheckout
import bench.ballarch.checkout.CheckoutFlow
import bench.ballarch.checkout.CheckoutState
import bench.ballarch.inventory.InventoryBall
import bench.ballarch.inventory.Stock
import bench.ballarch.keyed.payment.ChargeRequest
import bench.ballarch.keyed.payment.PaymentBall
import bench.ballarch.keyed.payment.ProviderCharge
import bench.ballarch.keyed.payment.ProviderStatus
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
 * The charge adapter sends the call identity as the idempotency key and gives
 * each attempt [attemptMillis]: an attempt with no answer by then is reported
 * as failed, and a later answer to it is ignored, so that the runtime can send
 * the call again, with the same key, before the call's deadline. With the key,
 * a refused request is sent again like an unanswered one, so it is reported as
 * a failed attempt too, not as evidence that ends the call.
 */
fun chargeAdapter(provider: Provider, scheduler: Scheduler, attemptMillis: Long) = Adapter<ChargeRequest, ChargeResult> { invocation, done ->
    var reported = false
    fun once(result: AdapterResult<ChargeResult>) {
        if (!reported) {
            reported = true
            done(result)
        }
    }
    val timer = scheduler.schedule(attemptMillis) { once(AdapterResult.Failed("no answer within $attemptMillis ms")) }
    provider.charge(invocation.request.orderId, invocation.request.amount, idempotencyKey = invocation.callId.toString()) { reply ->
        timer.cancel()
        once(
            when (reply) {
                is Reply.Answer -> AdapterResult.Ok(reply.value)
                is Reply.Failure -> AdapterResult.Failed(reply.error.toString())
            },
        )
    }
    Cancellable { timer.cancel() }
}

fun statusAdapter(provider: Provider) = Adapter<String, ChargeStatus> { invocation, done ->
    provider.status(invocation.request) { done(report(it)) }
    null
}

/** Charge requests per call, and the time each one gets. */
private const val ATTEMPTS = 3
private const val ATTEMPT_MILLIS = 1_000L

/** The composition root. */
fun composition(provider: Provider, scheduler: Scheduler): Composition = Composition()
    .ball(InventoryBall(Scenario.STOCK))
    .ball(PaymentBall(horizonMillis = Scenario.PROVIDER_HORIZON_MILLIS, chargeTimeoutMillis = ATTEMPTS * ATTEMPT_MILLIS))
    .ball(CheckoutFlow())
    .adapter(ProviderCharge, chargeAdapter(provider, scheduler, ATTEMPT_MILLIS), RetryPolicy(maxAttempts = ATTEMPTS))
    .adapter(ProviderStatus, statusAdapter(provider))

class BallarchSystem(private val wiring: (Provider, Scheduler) -> Composition = ::composition) : CheckoutSystem {
    private lateinit var store: MemoryStore
    private lateinit var runtime: Runtime

    override fun boot(world: World) {
        store = world.db.durable("ballarch.store") { MemoryStore() }
        runtime = Runtime(wiring(world.provider, world.scheduler), world.scheduler, store)
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
