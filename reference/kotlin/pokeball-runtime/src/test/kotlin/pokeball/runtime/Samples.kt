package pokeball.runtime

import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.BallPort
import pokeball.kernel.Call
import pokeball.kernel.CallKey
import pokeball.kernel.Cancel
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.EffectClass
import pokeball.kernel.Notice
import pokeball.kernel.Outcome
import pokeball.kernel.Publish
import pokeball.kernel.Reply
import pokeball.kernel.RequestContext
import pokeball.kernel.RequestId
import pokeball.kernel.Step
import pokeball.kernel.Topic
import pokeball.kernel.accept
import pokeball.kernel.acceptLater
import pokeball.kernel.outcomeOf
import pokeball.kernel.payloadOf
import pokeball.kernel.reject
import pokeball.kernel.step

// ------------------------------------------------------------------ Counter

sealed interface CounterMsg {
    data object Increment : CounterMsg
    data object Get : CounterMsg
    data object Explode : CounterMsg
}

sealed interface CounterReply {
    data class Value(val value: Int) : CounterReply
    data object LimitReached : CounterReply
}

val CounterPort = BallPort<CounterMsg, CounterReply>("counter")
val CounterChanged = Topic<Int>("counter.changed")

class CounterBall(private val limit: Int = 3) : Ball<Int, CounterMsg, CounterReply> {
    override val type = "counter"
    override fun initial(key: String) = 0

    override fun decide(state: Int, request: CounterMsg, ctx: RequestContext): Decision<Int, CounterReply> = when (request) {
        CounterMsg.Increment ->
            if (state >= limit) reject(CounterReply.LimitReached)
            else accept(state + 1, CounterReply.Value(state + 1), Publish(CounterChanged, state + 1))
        CounterMsg.Get -> accept(state, CounterReply.Value(state))
        CounterMsg.Explode -> error("boom")
    }
}

// ------------------------------------------------------------------ Payer: one external, non-idempotent call

val PaymentPort = AdapterPort<Int, String>("payment", EffectClass.NonIdempotent)

sealed interface PayerState {
    data object Idle : PayerState
    data class Paying(val key: CallKey, val requestId: RequestId, val amount: Int) : PayerState
    data class Finished(val outcome: String) : PayerState
}

data class Pay(val amount: Int, val timeoutMillis: Long = 500)
data object CancelPayment

class PayerBall(
    private val port: AdapterPort<Int, String> = PaymentPort,
    override val type: String = "payer",
) : Ball<PayerState, Any, String> {
    override fun initial(key: String): PayerState = PayerState.Idle

    override fun decide(state: PayerState, request: Any, ctx: RequestContext): Decision<PayerState, String> = when (request) {
        is Pay -> {
            if (state is PayerState.Paying) {
                reject("busy")
            } else {
                val key = ctx.newKey()
                acceptLater(PayerState.Paying(key, ctx.requestId, request.amount), Call(key, port, request.amount, request.timeoutMillis))
            }
        }
        CancelPayment -> if (state is PayerState.Paying) accept(state, "cancelling", Cancel(state.key)) else reject("nothing to cancel")
        else -> reject("unknown request")
    }

    override fun complete(state: PayerState, completion: Completion<*>, ctx: Context): Step<PayerState, String> {
        val paying = state as? PayerState.Paying ?: return step(state)
        if (completion.key != paying.key) return step(state)
        val text = when (val o = completion.outcomeOf(port)!!) {
            is Outcome.Done -> "paid:${o.value}"
            is Outcome.NotDone -> "not-paid:${o.reason::class.simpleName}"
            is Outcome.Unknown -> "unknown:${o.reason}"
        }
        return step(PayerState.Finished(text), Reply(paying.requestId, text))
    }
}

// ------------------------------------------------------------------ Relay: a Ball that calls the Counter Ball

sealed interface RelayState {
    data object Idle : RelayState
    data class Waiting(val key: CallKey, val requestId: RequestId) : RelayState
    data class Done(val text: String) : RelayState
}

data class Forward(val counterKey: String, val message: CounterMsg)

class RelayBall : Ball<RelayState, Forward, String> {
    override val type = "relay"
    override fun initial(key: String): RelayState = RelayState.Idle

    override fun decide(state: RelayState, request: Forward, ctx: RequestContext): Decision<RelayState, String> {
        val key = ctx.newKey()
        return acceptLater(RelayState.Waiting(key, ctx.requestId), Call(key, CounterPort, request.message, 1_000, target = request.counterKey))
    }

    override fun complete(state: RelayState, completion: Completion<*>, ctx: Context): Step<RelayState, String> {
        val waiting = state as? RelayState.Waiting ?: return step(state)
        val text = when (val o = completion.outcomeOf(CounterPort)!!) {
            is Outcome.Done -> "done:${o.value}"
            is Outcome.NotDone -> "not-done:${o.reason}"
            is Outcome.Unknown -> "unknown:${o.reason}"
        }
        return step(RelayState.Done(text), Reply(waiting.requestId, text))
    }
}

// ------------------------------------------------------------------ Audit: subscribes to counter changes

class AuditBall : Ball<List<Int>, Unit, Unit> {
    override val type = "audit"
    override fun initial(key: String): List<Int> = emptyList()
    override fun decide(state: List<Int>, request: Unit, ctx: RequestContext): Decision<List<Int>, Unit> = accept(state, Unit)
    override fun observe(state: List<Int>, notice: Notice<*>, ctx: Context): Step<List<Int>, Unit> {
        val value = notice.payloadOf(CounterChanged) ?: return step(state)
        return step(state + value)
    }
}
