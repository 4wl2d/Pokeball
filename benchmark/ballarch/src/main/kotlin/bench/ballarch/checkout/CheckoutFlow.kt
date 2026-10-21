package bench.ballarch.checkout

import ballarch.kernel.Ball
import ballarch.kernel.Call
import ballarch.kernel.CallKey
import ballarch.kernel.Completion
import ballarch.kernel.Context
import ballarch.kernel.Decision
import ballarch.kernel.NotDoneReason
import ballarch.kernel.Outcome
import ballarch.kernel.RequestContext
import ballarch.kernel.Step
import ballarch.kernel.accept
import ballarch.kernel.outcomeOf
import ballarch.kernel.step
import bench.ballarch.api.Capture
import bench.ballarch.api.InventoryPort
import bench.ballarch.api.InventoryReply
import bench.ballarch.api.InventoryRequest
import bench.ballarch.api.PaymentReply
import bench.ballarch.api.PaymentsPort
import bench.ballarch.api.StartCheckout
import bench.env.Order

sealed interface CheckoutState {
    data object New : CheckoutState
    data class Reserving(val order: Order, val call: CallKey, val attempts: Int) : CheckoutState
    data class Paying(val order: Order, val call: CallKey, val attempts: Int) : CheckoutState
    data class Releasing(val order: Order, val call: CallKey, val attempts: Int) : CheckoutState
    data class Completed(val order: Order, val reference: String) : CheckoutState
    data class Failed(val order: Order, val reason: String) : CheckoutState
    data class NeedsAttention(val order: Order) : CheckoutState
}

/**
 * The checkout workflow (a Flow): reserve, capture, complete; release when the
 * payment is not captured. Inventory and payment requests are idempotent per
 * order, so a step whose outcome is unknown is asked again.
 */
class CheckoutFlow(private val stepTimeoutMillis: Long = 10_000, private val maxAttempts: Int = 3) :
    Ball<CheckoutState, StartCheckout, Unit> {
    override val type = "checkout"

    override fun initial(key: String): CheckoutState = CheckoutState.New

    override fun decide(state: CheckoutState, request: StartCheckout, ctx: RequestContext): Decision<CheckoutState, Unit> =
        if (state == CheckoutState.New) reserve(request.order, ctx, 1).let { accept(it.state, Unit, *it.outputs.toTypedArray()) }
        else accept(state, Unit)

    override fun complete(state: CheckoutState, completion: Completion<*>, ctx: Context): Step<CheckoutState, Unit> = when {
        state is CheckoutState.Reserving && completion.key == state.call -> reserved(state, completion.outcomeOf(InventoryPort)!!, ctx)
        state is CheckoutState.Paying && completion.key == state.call -> paid(state, completion.outcomeOf(PaymentsPort)!!, ctx)
        state is CheckoutState.Releasing && completion.key == state.call -> released(state, completion.outcomeOf(InventoryPort)!!, ctx)
        else -> step(state) // stale
    }

    private fun reserved(s: CheckoutState.Reserving, o: Outcome<InventoryReply>, ctx: Context) = when {
        o is Outcome.Done -> pay(s.order, ctx, 1)
        o is Outcome.NotDone && o.reason is NotDoneReason.Refused -> step(CheckoutState.Failed(s.order, "out of stock"))
        else -> retry(s.order, s.attempts) { reserve(s.order, ctx, it) } // Unknown or no answer: ask again (idempotent)
    }

    private fun paid(s: CheckoutState.Paying, o: Outcome<PaymentReply>, ctx: Context) = when (val r = (o as? Outcome.Done)?.value) {
        is PaymentReply.Captured -> step(CheckoutState.Completed(s.order, r.reference))
        is PaymentReply.NotCaptured -> release(s.order, ctx, 1)
        PaymentReply.Unresolved -> step(CheckoutState.NeedsAttention(s.order))
        null -> retry(s.order, s.attempts) { pay(s.order, ctx, it) } // Capture is idempotent per order
    }

    private fun released(s: CheckoutState.Releasing, o: Outcome<InventoryReply>, ctx: Context) =
        if (o is Outcome.Done) step(CheckoutState.Failed(s.order, "payment not captured"))
        else retry(s.order, s.attempts) { release(s.order, ctx, it) }

    private fun retry(order: Order, attempts: Int, next: (Int) -> Step<CheckoutState, Unit>) =
        if (attempts < maxAttempts) next(attempts + 1) else step(CheckoutState.NeedsAttention(order))

    private fun reserve(order: Order, ctx: Context, attempts: Int) = ctx.newKey().let { key ->
        step<CheckoutState, Unit>(
            CheckoutState.Reserving(order, key, attempts),
            Call(key, InventoryPort, InventoryRequest.Reserve(order.id, order.quantity), stepTimeoutMillis, target = order.sku),
        )
    }

    private fun pay(order: Order, ctx: Context, attempts: Int) = ctx.newKey().let { key ->
        step<CheckoutState, Unit>(CheckoutState.Paying(order, key, attempts), Call(key, PaymentsPort, Capture(order.amount), stepTimeoutMillis, target = order.id))
    }

    private fun release(order: Order, ctx: Context, attempts: Int) = ctx.newKey().let { key ->
        step<CheckoutState, Unit>(
            CheckoutState.Releasing(order, key, attempts),
            Call(key, InventoryPort, InventoryRequest.Release(order.id), stepTimeoutMillis, target = order.sku),
        )
    }
}
