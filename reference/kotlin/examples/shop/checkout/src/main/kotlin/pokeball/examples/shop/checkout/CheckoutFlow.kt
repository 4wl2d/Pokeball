package pokeball.examples.shop.checkout

import pokeball.examples.shop.inventory.api.InventoryPort
import pokeball.examples.shop.inventory.api.InventoryReply
import pokeball.examples.shop.inventory.api.InventoryRequest
import pokeball.examples.shop.orders.api.OrderReply
import pokeball.examples.shop.orders.api.OrderRequest
import pokeball.examples.shop.orders.api.OrdersPort
import pokeball.examples.shop.payments.api.PaymentReply
import pokeball.examples.shop.payments.api.PaymentRequest
import pokeball.examples.shop.payments.api.PaymentResolved
import pokeball.examples.shop.payments.api.PaymentsPort
import pokeball.kernel.Ball
import pokeball.kernel.Call
import pokeball.kernel.CallKey
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.Notice
import pokeball.kernel.NotDoneReason
import pokeball.kernel.Outcome
import pokeball.kernel.RequestContext
import pokeball.kernel.Step
import pokeball.kernel.Timer
import pokeball.kernel.accept
import pokeball.kernel.outcomeOf
import pokeball.kernel.payloadOf
import pokeball.kernel.reject
import pokeball.kernel.step

/** What is being bought. */
data class OrderSpec(val sku: String, val quantity: Int, val amount: Long)

sealed interface CheckoutRequest {
    data class Start(val spec: OrderSpec) : CheckoutRequest
    data object GetStatus : CheckoutRequest
}

enum class Phase { New, Reserving, Paying, AwaitingPayment, Confirming, Releasing, Completed, Failed, Stuck }

sealed interface CheckoutReply {
    data class Status(val phase: Phase, val detail: String? = null) : CheckoutReply
    data object Conflict : CheckoutReply
}

sealed interface CheckoutState {
    val phase: Phase

    data object New : CheckoutState { override val phase = Phase.New }

    /** A step waiting for the completion of [call]; [attempts] counts calls made for this step. */
    sealed interface Waiting : CheckoutState {
        val spec: OrderSpec
        val call: CallKey
        val attempts: Int
    }

    data class Reserving(override val spec: OrderSpec, override val call: CallKey, override val attempts: Int = 1) : Waiting {
        override val phase = Phase.Reserving
    }

    /** [polls] counts earlier waiting rounds, so that waiting for payment is bounded. */
    data class Paying(override val spec: OrderSpec, override val call: CallKey, override val attempts: Int = 1, val polls: Int = 0) : Waiting {
        override val phase = Phase.Paying
    }

    /**
     * The payment outcome is not known yet. A PaymentResolved notice decides; as a
     * fallback, [call] is a timer after which payments is asked again ([attempts] polls).
     */
    data class AwaitingPayment(override val spec: OrderSpec, override val call: CallKey, override val attempts: Int = 1) : Waiting {
        override val phase = Phase.AwaitingPayment
    }

    data class Confirming(override val spec: OrderSpec, val reference: String, override val call: CallKey, override val attempts: Int = 1) : Waiting {
        override val phase = Phase.Confirming
    }

    data class Releasing(override val spec: OrderSpec, val reason: String, override val call: CallKey, override val attempts: Int = 1) : Waiting {
        override val phase = Phase.Releasing
    }

    data class Completed(val spec: OrderSpec, val reference: String) : CheckoutState { override val phase = Phase.Completed }

    data class Failed(val spec: OrderSpec, val reason: String) : CheckoutState { override val phase = Phase.Failed }

    /** Retries of one step were exhausted; an operator must act. */
    data class Stuck(val spec: OrderSpec, val step: Phase) : CheckoutState { override val phase = Phase.Stuck }
}

/**
 * S3: the checkout workflow, the one owner of its coordination state (a Flow).
 * It depends only on the public APIs of inventory, payments and orders.
 *
 * Every participant request is idempotent at the participant (keyed by the
 * checkout id), so a step whose outcome is unknown is simply asked again.
 */
class CheckoutFlow(
    private val stepTimeoutMillis: Long = 2_000,
    private val maxAttempts: Int = 5,
    private val pollMillis: Long = 500,
    private val maxPolls: Int = 10,
) : Ball<CheckoutState, CheckoutRequest, CheckoutReply> {
    override val type = "checkout"

    override fun initial(key: String): CheckoutState = CheckoutState.New

    override fun decide(state: CheckoutState, request: CheckoutRequest, ctx: RequestContext): Decision<CheckoutState, CheckoutReply> =
        when (request) {
            is CheckoutRequest.Start -> when {
                state is CheckoutState.New -> {
                    val next = reserve(request.spec, ctx, attempts = 1)
                    accept(next.state, status(next.state), *next.outputs.toTypedArray())
                }
                specOf(state) == request.spec -> accept(state, status(state))
                else -> reject(CheckoutReply.Conflict)
            }
            CheckoutRequest.GetStatus -> accept(state, status(state))
        }

    override fun complete(state: CheckoutState, completion: Completion<*>, ctx: Context): Step<CheckoutState, CheckoutReply> {
        if (state !is CheckoutState.Waiting || completion.key != state.call) return step(state) // stale
        return when (state) {
            is CheckoutState.Reserving -> reserved(state, completion.outcomeOf(InventoryPort)!!, ctx)
            is CheckoutState.Paying -> paid(state, completion.outcomeOf(PaymentsPort)!!, ctx)
            is CheckoutState.Confirming -> confirmed(state, completion.outcomeOf(OrdersPort)!!, ctx)
            is CheckoutState.Releasing -> released(state, completion.outcomeOf(InventoryPort)!!, ctx)
            // The poll timer fired: ask payments again (Capture is idempotent per order).
            is CheckoutState.AwaitingPayment -> pay(state.spec, ctx, attempts = 1, polls = state.attempts)
        }
    }

    override fun observe(state: CheckoutState, notice: Notice<*>, ctx: Context): Step<CheckoutState, CheckoutReply> {
        val resolution = notice.payloadOf(PaymentResolved) ?: return step(state)
        if (resolution.orderId != ctx.self.key) return step(state)
        val spec = when (state) {
            is CheckoutState.Paying -> state.spec
            is CheckoutState.AwaitingPayment -> state.spec
            else -> return step(state) // already decided by a reply
        }
        return if (resolution.captured) confirm(spec, resolution.reference!!, ctx, 1) else release(spec, "payment not captured", ctx, 1)
    }

    // ---------------------------------------------------------------- steps

    private fun reserve(spec: OrderSpec, ctx: Context, attempts: Int): Step<CheckoutState, CheckoutReply> {
        val key = ctx.newKey()
        return step(
            CheckoutState.Reserving(spec, key, attempts),
            Call(key, InventoryPort, InventoryRequest.Reserve(ctx.self.key, spec.quantity), stepTimeoutMillis, target = spec.sku),
        )
    }

    private fun pay(spec: OrderSpec, ctx: Context, attempts: Int, polls: Int = 0): Step<CheckoutState, CheckoutReply> {
        val key = ctx.newKey()
        return step(
            CheckoutState.Paying(spec, key, attempts, polls),
            Call(key, PaymentsPort, PaymentRequest.Capture(spec.amount), stepTimeoutMillis, target = ctx.self.key),
        )
    }

    private fun confirm(spec: OrderSpec, reference: String, ctx: Context, attempts: Int): Step<CheckoutState, CheckoutReply> {
        if (attempts > maxAttempts) return step(CheckoutState.Stuck(spec, Phase.Confirming))
        val key = ctx.newKey()
        return step(
            CheckoutState.Confirming(spec, reference, key, attempts),
            Call(key, OrdersPort, OrderRequest.Confirm(reference), stepTimeoutMillis, target = ctx.self.key),
        )
    }

    private fun await(spec: OrderSpec, ctx: Context, polls: Int): Step<CheckoutState, CheckoutReply> {
        if (polls >= maxPolls) return step(CheckoutState.Stuck(spec, Phase.AwaitingPayment))
        val key = ctx.newKey()
        return step(CheckoutState.AwaitingPayment(spec, key, polls + 1), Timer.after(key, pollMillis))
    }

    private fun release(spec: OrderSpec, reason: String, ctx: Context, attempts: Int): Step<CheckoutState, CheckoutReply> {
        if (attempts > maxAttempts) return step(CheckoutState.Stuck(spec, Phase.Releasing))
        val key = ctx.newKey()
        return step(
            CheckoutState.Releasing(spec, reason, key, attempts),
            Call(key, InventoryPort, InventoryRequest.Release(ctx.self.key), stepTimeoutMillis, target = spec.sku),
        )
    }

    // ---------------------------------------------------------------- completions

    private fun reserved(s: CheckoutState.Reserving, o: Outcome<InventoryReply>, ctx: Context) = when (o) {
        is Outcome.Done -> pay(s.spec, ctx, 1)
        is Outcome.NotDone -> when (o.reason) {
            is NotDoneReason.Refused -> step(CheckoutState.Failed(s.spec, "out of stock"))
            else -> if (s.attempts < maxAttempts) reserve(s.spec, ctx, s.attempts + 1) else step(CheckoutState.Failed(s.spec, "inventory unavailable"))
        }
        // The reservation may exist: release it (idempotent) rather than guess.
        is Outcome.Unknown -> release(s.spec, "reservation outcome unknown", ctx, 1)
    }

    private fun paid(s: CheckoutState.Paying, o: Outcome<PaymentReply>, ctx: Context) = when (o) {
        is Outcome.Done -> when (val r = o.value) {
            is PaymentReply.Captured -> confirm(s.spec, r.reference, ctx, 1)
            is PaymentReply.NotCaptured -> release(s.spec, "payment not captured: ${r.reason}", ctx, 1)
            PaymentReply.Pending -> await(s.spec, ctx, s.polls)
            PaymentReply.Busy, PaymentReply.NoPayment ->
                if (s.attempts < maxAttempts) pay(s.spec, ctx, s.attempts + 1, s.polls) else await(s.spec, ctx, s.polls)
            PaymentReply.AmountMismatch -> step(CheckoutState.Stuck(s.spec, Phase.Paying))
        }
        // Payments never accepted the request: nothing was charged.
        is Outcome.NotDone ->
            if (s.attempts < maxAttempts) pay(s.spec, ctx, s.attempts + 1, s.polls) else release(s.spec, "payments unavailable", ctx, 1)
        // Payments may have accepted: ask again (Capture is idempotent per order), then wait.
        is Outcome.Unknown ->
            if (s.attempts < maxAttempts) pay(s.spec, ctx, s.attempts + 1, s.polls) else await(s.spec, ctx, s.polls)
    }

    private fun confirmed(s: CheckoutState.Confirming, o: Outcome<OrderReply>, ctx: Context) = when (o) {
        is Outcome.Done -> step(CheckoutState.Completed(s.spec, s.reference))
        is Outcome.NotDone, is Outcome.Unknown -> confirm(s.spec, s.reference, ctx, s.attempts + 1)
    }

    private fun released(s: CheckoutState.Releasing, o: Outcome<InventoryReply>, ctx: Context) = when (o) {
        is Outcome.Done -> step(CheckoutState.Failed(s.spec, s.reason))
        is Outcome.NotDone, is Outcome.Unknown -> release(s.spec, s.reason, ctx, s.attempts + 1)
    }

    // ---------------------------------------------------------------- views

    private fun status(state: CheckoutState): CheckoutReply = CheckoutReply.Status(
        state.phase,
        when (state) {
            is CheckoutState.Failed -> state.reason
            is CheckoutState.Completed -> state.reference
            else -> null
        },
    )

    private fun specOf(state: CheckoutState): OrderSpec? = when (state) {
        CheckoutState.New -> null
        is CheckoutState.Waiting -> state.spec
        is CheckoutState.AwaitingPayment -> state.spec
        is CheckoutState.Completed -> state.spec
        is CheckoutState.Failed -> state.spec
        is CheckoutState.Stuck -> state.spec
    }
}
