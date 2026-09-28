package pokeball.examples.tutorial

import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.Call
import pokeball.kernel.CallKey
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.EffectClass
import pokeball.kernel.Outcome
import pokeball.kernel.RequestContext
import pokeball.kernel.Step
import pokeball.kernel.Timer
import pokeball.kernel.accept
import pokeball.kernel.outcomeOf
import pokeball.kernel.reject
import pokeball.kernel.step

// ---- the port: sending an email is not idempotent (the mail service has no idempotency key)

data class Email(val to: String, val text: String)

val Mailer = AdapterPort<Email, Unit>("mailer", EffectClass.NonIdempotent)

// ---- messages

sealed interface ReminderRequest {
    data class Schedule(val to: String, val text: String, val delayMillis: Long) : ReminderRequest
    data object Status : ReminderRequest
}

sealed interface ReminderReply {
    data object Scheduled : ReminderReply
    data object AlreadyScheduled : ReminderReply
    data class Current(val state: ReminderState) : ReminderReply
}

// ---- state: every stage, including "we cannot know", is an explicit case

sealed interface ReminderState {
    data object Empty : ReminderState
    data class Waiting(val email: Email, val timer: CallKey) : ReminderState
    data class Sending(val email: Email, val call: CallKey) : ReminderState
    data object Sent : ReminderState
    data class NotSent(val reason: String) : ReminderState
    /** The mail service may or may not have sent it. Sending again could send it twice. */
    data object MaybeSent : ReminderState
}

// ---- the decider

object ReminderBall : Ball<ReminderState, ReminderRequest, ReminderReply> {
    override val type = "reminder"

    override fun initial(key: String): ReminderState = ReminderState.Empty

    override fun decide(state: ReminderState, request: ReminderRequest, ctx: RequestContext): Decision<ReminderState, ReminderReply> =
        when (request) {
            is ReminderRequest.Schedule -> {
                if (state != ReminderState.Empty) {
                    reject(ReminderReply.AlreadyScheduled)
                } else {
                    val timer = ctx.newKey()
                    val email = Email(request.to, request.text)
                    accept(ReminderState.Waiting(email, timer), ReminderReply.Scheduled, Timer.after(timer, request.delayMillis))
                }
            }
            ReminderRequest.Status -> accept(state, ReminderReply.Current(state))
        }

    override fun complete(state: ReminderState, completion: Completion<*>, ctx: Context): Step<ReminderState, ReminderReply> =
        when {
            // The timer woke us up: send the email.
            state is ReminderState.Waiting && completion.key == state.timer -> {
                val call = ctx.newKey()
                step(ReminderState.Sending(state.email, call), Call(call, Mailer, state.email, timeoutMillis = 5_000))
            }
            // The one completion of the send.
            state is ReminderState.Sending && completion.key == state.call ->
                when (val outcome = completion.outcomeOf(Mailer)!!) {
                    is Outcome.Done -> step(ReminderState.Sent)
                    is Outcome.NotDone -> step(ReminderState.NotSent(outcome.reason.toString()))
                    is Outcome.Unknown -> step(ReminderState.MaybeSent)
                }
            else -> step(state)
        }
}
