package bench.ballarch.refadapter.payment

import ballarch.kernel.AdapterPort
import ballarch.kernel.Ball
import ballarch.kernel.Call
import ballarch.kernel.CallKey
import ballarch.kernel.Completion
import ballarch.kernel.Context
import ballarch.kernel.Decision
import ballarch.kernel.EffectClass
import ballarch.kernel.Outcome
import ballarch.kernel.Reply
import ballarch.kernel.RequestContext
import ballarch.kernel.RequestId
import ballarch.kernel.Step
import ballarch.kernel.Timer
import ballarch.kernel.accept
import ballarch.kernel.acceptLater
import ballarch.kernel.outcomeOf
import ballarch.kernel.step
import bench.ballarch.api.Capture
import bench.ballarch.api.PaymentReply
import bench.env.ChargeResult
import bench.env.ChargeStatus

data class ChargeRequest(val orderId: String, val amount: Long)

/** The provider honours idempotency keys: the adapter sends the call's identity as the key. */
val ProviderCharge = AdapterPort<ChargeRequest, ChargeResult>("provider.charge", EffectClass.Idempotent)

val ProviderStatus = AdapterPort<String, ChargeStatus>("provider.status", EffectClass.Safe)

sealed interface PaymentState {
    data object New : PaymentState
    data class Charging(val amount: Long, val call: CallKey, val sends: Int, val waiting: List<RequestId>) : PaymentState
    data class Reconciling(val amount: Long, val call: CallKey, val queries: Int, val waiting: List<RequestId>) : PaymentState
    data class Resolved(val reply: PaymentReply) : PaymentState
}

/**
 * Captures one order's payment. A charge is sent again only after `NotDone`
 * (evidence that nothing was charged); `Unknown` starts reconciliation: wait
 * past the provider's horizon, then ask its status.
 */
class PaymentBall(
    private val horizonMillis: Long,
    private val timeoutMillis: Long = 1_000,
    private val maxSends: Int = 3,
    private val maxQueries: Int = 3,
) : Ball<PaymentState, Capture, PaymentReply> {
    override val type = "payment"

    override fun initial(key: String): PaymentState = PaymentState.New

    override fun decide(state: PaymentState, request: Capture, ctx: RequestContext): Decision<PaymentState, PaymentReply> =
        when (state) {
            PaymentState.New -> charge(request.amount, 1, listOf(ctx.requestId), ctx).let { acceptLater(it.state, *it.outputs.toTypedArray()) }
            is PaymentState.Charging -> acceptLater(state.copy(waiting = state.waiting + ctx.requestId))
            is PaymentState.Reconciling -> acceptLater(state.copy(waiting = state.waiting + ctx.requestId))
            is PaymentState.Resolved -> accept(state, state.reply)
        }

    override fun complete(state: PaymentState, completion: Completion<*>, ctx: Context): Step<PaymentState, PaymentReply> = when {
        state is PaymentState.Charging && completion.key == state.call ->
            when (val outcome = completion.outcomeOf(ProviderCharge)!!) {
                is Outcome.Done -> resolve(state.waiting, outcome.value.toReply())
                is Outcome.NotDone ->
                    if (state.sends < maxSends) charge(state.amount, state.sends + 1, state.waiting, ctx)
                    else resolve(state.waiting, PaymentReply.NotCaptured("not charged"))
                is Outcome.Unknown -> wait(PaymentState.Reconciling(state.amount, ctx.newKey(), 0, state.waiting))
            }
        state is PaymentState.Reconciling && completion.key == state.call -> reconcile(state, completion, ctx)
        else -> step(state)
    }

    private fun reconcile(state: PaymentState.Reconciling, completion: Completion<*>, ctx: Context): Step<PaymentState, PaymentReply> {
        if (completion.outcomeOf(Timer.port) != null) {
            val key = ctx.newKey()
            return step(state.copy(call = key, queries = state.queries + 1), Call(key, ProviderStatus, ctx.self.key, timeoutMillis))
        }
        val status = completion.outcomeOf(ProviderStatus)!!
        return when {
            status is Outcome.Done -> resolve(state.waiting, status.value.toReply())
            state.queries >= maxQueries -> resolve(state.waiting, PaymentReply.Unresolved)
            else -> wait(state.copy(call = ctx.newKey()))
        }
    }

    private fun charge(amount: Long, sends: Int, waiting: List<RequestId>, ctx: Context) = ctx.newKey().let { key ->
        step<PaymentState, PaymentReply>(
            PaymentState.Charging(amount, key, sends, waiting),
            Call(key, ProviderCharge, ChargeRequest(ctx.self.key, amount), timeoutMillis),
        )
    }

    private fun wait(state: PaymentState.Reconciling) = step<PaymentState, PaymentReply>(state, Timer.after(state.call, horizonMillis))

    private fun resolve(waiting: List<RequestId>, reply: PaymentReply): Step<PaymentState, PaymentReply> =
        Step(PaymentState.Resolved(reply), waiting.map { Reply(it, reply) })

    private fun ChargeResult.toReply(): PaymentReply = when (this) {
        is ChargeResult.Charged -> PaymentReply.Captured(reference)
        ChargeResult.Declined -> PaymentReply.NotCaptured("declined")
    }

    private fun ChargeStatus.toReply(): PaymentReply = when (this) {
        is ChargeStatus.Charged -> PaymentReply.Captured(reference)
        ChargeStatus.NoCharge -> PaymentReply.NotCaptured("not charged")
    }
}
