package pokeball.examples.shop.payments

import pokeball.examples.shop.payments.api.PaymentReply
import pokeball.examples.shop.payments.api.PaymentRequest
import pokeball.examples.shop.payments.api.PaymentResolution
import pokeball.examples.shop.payments.api.PaymentResolved
import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.Call
import pokeball.kernel.CallKey
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.EffectClass
import pokeball.kernel.Outcome
import pokeball.kernel.Output
import pokeball.kernel.Publish
import pokeball.kernel.Reply
import pokeball.kernel.RequestContext
import pokeball.kernel.RequestId
import pokeball.kernel.Step
import pokeball.kernel.Timer
import pokeball.kernel.accept
import pokeball.kernel.acceptLater
import pokeball.kernel.outcomeOf
import pokeball.kernel.reject
import pokeball.kernel.step

// ---------------------------------------------------------------- provider ports (private to this feature)

data class ChargeRequest(val orderId: String, val amount: Long)

sealed interface ChargeResult {
    data class Charged(val reference: String) : ChargeResult
    data object Declined : ChargeResult
}

/** The provider has no idempotency keys: every delivered charge charges again. */
val ProviderCharge = AdapterPort<ChargeRequest, ChargeResult>("provider.charge", EffectClass.NonIdempotent)

data class StatusRequest(val orderId: String)

sealed interface ChargeStatus {
    data class Charged(val reference: String) : ChargeStatus
    data object NoCharge : ChargeStatus
}

val ProviderStatus = AdapterPort<StatusRequest, ChargeStatus>("provider.status", EffectClass.Safe)

// ---------------------------------------------------------------- state

sealed interface PaymentState {
    data object New : PaymentState

    data class Charging(val amount: Long, val call: CallKey, val waiting: List<RequestId>) : PaymentState

    /** The charge may or may not have happened; [call] is the pending timer or status query. */
    data class Reconciling(val amount: Long, val call: CallKey, val queries: Int) : PaymentState

    data class Captured(val amount: Long, val reference: String) : PaymentState

    data class NotCaptured(val amount: Long, val reason: String) : PaymentState

    /** Reconciliation gave up; an operator must decide. */
    data class Unresolved(val amount: Long) : PaymentState
}

/**
 * S2: captures a payment exactly once per order through a non-idempotent provider.
 *
 * - A charge is sent once (the runtime never re-sends a non-idempotent call).
 * - `NotDone` from the runtime proves nothing was charged.
 * - `Unknown` starts reconciliation: wait until the provider's delivery horizon
 *   has passed, then ask its status endpoint, whose answer is then final.
 *
 * @param horizonMillis time after which an unanswered charge can no longer be applied.
 */
class PaymentBall(
    private val horizonMillis: Long,
    private val chargeTimeoutMillis: Long = 1_000,
    private val maxStatusQueries: Int = 5,
) : Ball<PaymentState, PaymentRequest, PaymentReply> {
    override val type = "payment"

    override fun initial(key: String): PaymentState = PaymentState.New

    override fun decide(state: PaymentState, request: PaymentRequest, ctx: RequestContext): Decision<PaymentState, PaymentReply> =
        when (request) {
            is PaymentRequest.Capture -> capture(state, request.amount, ctx)
            PaymentRequest.GetStatus -> accept(state, replyFor(state))
        }

    private fun capture(state: PaymentState, amount: Long, ctx: RequestContext): Decision<PaymentState, PaymentReply> = when (state) {
        PaymentState.New -> {
            val key = ctx.newKey()
            acceptLater(
                PaymentState.Charging(amount, key, listOf(ctx.requestId)),
                Call(key, ProviderCharge, ChargeRequest(ctx.self.key, amount), chargeTimeoutMillis),
            )
        }
        is PaymentState.Charging -> when {
            state.amount != amount -> reject(PaymentReply.AmountMismatch)
            state.waiting.size >= MAX_WAITING -> reject(PaymentReply.Busy)
            else -> acceptLater(state.copy(waiting = state.waiting + ctx.requestId))
        }
        else -> if (amountOf(state) != amount) reject(PaymentReply.AmountMismatch) else accept(state, replyFor(state))
    }

    override fun complete(state: PaymentState, completion: Completion<*>, ctx: Context): Step<PaymentState, PaymentReply> = when {
        state is PaymentState.Charging && completion.key == state.call -> charged(state, completion.outcomeOf(ProviderCharge)!!, ctx)
        state is PaymentState.Reconciling && completion.key == state.call -> reconcile(state, completion, ctx)
        else -> step(state) // stale completion
    }

    private fun charged(state: PaymentState.Charging, outcome: Outcome<ChargeResult>, ctx: Context): Step<PaymentState, PaymentReply> {
        val next = when (outcome) {
            is Outcome.Done -> when (val r = outcome.value) {
                is ChargeResult.Charged -> PaymentState.Captured(state.amount, r.reference)
                ChargeResult.Declined -> PaymentState.NotCaptured(state.amount, "declined")
            }
            is Outcome.NotDone -> PaymentState.NotCaptured(state.amount, "not charged")
            is Outcome.Unknown -> PaymentState.Reconciling(state.amount, ctx.newKey(), queries = 0)
        }
        val replies = state.waiting.map { Reply(it, replyFor(next)) }
        val follow = when (next) {
            is PaymentState.Reconciling -> listOf(Timer.after(next.call, horizonMillis))
            else -> listOf(resolved(ctx, next))
        }
        return Step(next, replies + follow)
    }

    private fun reconcile(state: PaymentState.Reconciling, completion: Completion<*>, ctx: Context): Step<PaymentState, PaymentReply> {
        completion.outcomeOf(Timer.port)?.let {
            val key = ctx.newKey()
            return step(state.copy(call = key, queries = state.queries + 1), Call(key, ProviderStatus, StatusRequest(ctx.self.key), chargeTimeoutMillis))
        }
        val next: PaymentState = when (val outcome = completion.outcomeOf(ProviderStatus)!!) {
            is Outcome.Done -> when (val s = outcome.value) {
                is ChargeStatus.Charged -> PaymentState.Captured(state.amount, s.reference)
                ChargeStatus.NoCharge -> PaymentState.NotCaptured(state.amount, "not charged")
            }
            is Outcome.NotDone, is Outcome.Unknown ->
                if (state.queries >= maxStatusQueries) PaymentState.Unresolved(state.amount)
                else return ctx.newKey().let { key -> step(state.copy(call = key), Timer.after(key, horizonMillis)) }
        }
        return if (next is PaymentState.Unresolved) step(next) else step(next, resolved(ctx, next))
    }

    private fun resolved(ctx: Context, s: PaymentState): Output<PaymentReply> = when (s) {
        is PaymentState.Captured -> Publish(PaymentResolved, PaymentResolution(ctx.self.key, captured = true, reference = s.reference))
        is PaymentState.NotCaptured -> Publish(PaymentResolved, PaymentResolution(ctx.self.key, captured = false, reference = null))
        else -> error("not a resolution: $s")
    }

    private fun replyFor(state: PaymentState): PaymentReply = when (state) {
        PaymentState.New -> PaymentReply.NoPayment
        is PaymentState.Charging, is PaymentState.Reconciling, is PaymentState.Unresolved -> PaymentReply.Pending
        is PaymentState.Captured -> PaymentReply.Captured(state.reference)
        is PaymentState.NotCaptured -> PaymentReply.NotCaptured(state.reason)
    }

    private fun amountOf(state: PaymentState): Long = when (state) {
        PaymentState.New -> -1
        is PaymentState.Charging -> state.amount
        is PaymentState.Reconciling -> state.amount
        is PaymentState.Captured -> state.amount
        is PaymentState.NotCaptured -> state.amount
        is PaymentState.Unresolved -> state.amount
    }

    companion object {
        const val MAX_WAITING = 8
    }
}
