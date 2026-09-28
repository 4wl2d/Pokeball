# Tutorial: a reminder that sends one email

This tutorial builds a small feature on Pokeball Core 2: a reminder that waits, then sends an email through a mail service that has no idempotency key. It covers every kernel concept a typical feature needs:

- a Ball and its decider;
- a timer;
- a call to an unreliable external system and its three possible outcomes;
- the composition root;
- two kinds of test: the decider as a plain function, and the whole feature under every network fate and crash point.

The code is the module [`reference/kotlin/examples/tutorial`](../reference/kotlin/examples/tutorial/). The build checks that every snippet below appears verbatim in it (`python3 tools/check_snippets.py`). Run the tests with `cd reference/kotlin && ./gradlew :examples:tutorial:test`.

## 1. Name the effect and its effect class

A port is a typed contract with an effect class. Sending an email has an effect, and repeating the request sends it again, so the port is `NonIdempotent`. The runtime will never re-send such a call. If it cannot know whether the email went out, it says so with `Unknown` instead of guessing.

<!-- snippet: reference/kotlin/examples/tutorial/src/main/kotlin/pokeball/examples/tutorial/Reminder.kt -->
```kotlin
data class Email(val to: String, val text: String)

val Mailer = AdapterPort<Email, Unit>("mailer", EffectClass.NonIdempotent)
```

A read-only lookup would be `Safe`: it may be retried freely and never ends `Unknown`. A call to a service that deduplicates by key would be `Idempotent`: the runtime may retry it, always with the same call identity.

## 2. Model the state, including what you cannot know

Each stage of the reminder is an explicit case. `MaybeSent` is not an error state; it is the honest description of a timeout after the request may have reached the mail service.

<!-- snippet: reference/kotlin/examples/tutorial/src/main/kotlin/pokeball/examples/tutorial/Reminder.kt -->
```kotlin
sealed interface ReminderState {
    data object Empty : ReminderState
    data class Waiting(val email: Email, val timer: CallKey) : ReminderState
    data class Sending(val email: Email, val call: CallKey) : ReminderState
    data object Sent : ReminderState
    data class NotSent(val reason: String) : ReminderState
    /** The mail service may or may not have sent it. Sending again could send it twice. */
    data object MaybeSent : ReminderState
}
```

## 3. Decide requests

`decide` is a pure function of the state, the request and the context. It returns `accept` (new state, reply and outputs) or `reject` (nothing changes). It reads no clock and no random numbers, and it performs no I/O. Time and call keys come from the context `ctx`.

Scheduling asks for a timer. A timer is an ordinary call on the built-in timer port, and its key is stored in the state so that the completion can be matched later.

<!-- snippet: reference/kotlin/examples/tutorial/src/main/kotlin/pokeball/examples/tutorial/Reminder.kt -->
```kotlin
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
```

The runtime commits the new state and the timer call together, and only then starts the timer. If the process crashes right after the commit, the Durable profile still has the timer on record and re-arms it for its original due time.

## 4. Handle completions

Every call ends in exactly one completion, delivered to `complete`. When the timer completes, the Ball sends the email. When the send completes, the Ball records one of three outcomes:

<!-- snippet: reference/kotlin/examples/tutorial/src/main/kotlin/pokeball/examples/tutorial/Reminder.kt -->
```kotlin
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
```

What each outcome means:

- `Done`: the mail service processed the request.
- `NotDone`: there is evidence that nothing was sent. For example, the connection was refused before the request was written, or the deadline passed before the call left.
- `Unknown`: the request may have been sent, for example after a timeout, an error after the request was written, or a crash while it was in flight.

The runtime picks the outcome, not the adapter or the Ball. The rule is in §3.2 of the [specification](../spec/pokeball-core-2.0.md).

What to do with `MaybeSent` is a business decision. You can show it to the user, or ask the mail service's status API after its delivery horizon (see the [payment example](../reference/kotlin/examples/shop/payments/src/main/kotlin/pokeball/examples/shop/payments/PaymentBall.kt)). Sending again is the one thing you should not do blindly.

## 5. Compose

The composition root is the one place that says which Balls exist and which adapter performs each port:

<!-- snippet: reference/kotlin/examples/tutorial/src/main/kotlin/pokeball/examples/tutorial/ReminderApp.kt -->
```kotlin
fun reminderComposition(mailer: Adapter<Email, Unit>): Composition =
    Composition()
        .ball(ReminderBall)
        .adapter(Mailer, mailer)
```

In production, `mailer` is an adapter around your HTTP or SMTP client. An adapter reports each invocation once, never retries, and reports `NotApplied` only with evidence. [`HttpProviderAdapters`](../reference/kotlin/examples/shop/payments-http/src/main/kotlin/pokeball/examples/shop/payments/http/HttpProviderAdapters.kt) is a real example. Check yours with the adapter conformance kit (`pokeball.testkit.AdapterConformance`). Run the whole thing on a `Runtime` with a `ThreadScheduler` and a store.

## 6. Test the decider as a function

<!-- snippet: reference/kotlin/examples/tutorial/src/test/kotlin/pokeball/examples/tutorial/ReminderTest.kt -->
```kotlin
    fun `scheduling accepts once and asks for a timer`() {
        val ctx = FixedContext(id)
        val decision = ReminderBall.decide(ReminderState.Empty, ReminderRequest.Schedule("ann@example.com", "Call Bob", 60_000), ctx)
        assertIs<Accept<ReminderState, ReminderReply>>(decision)
        assertEquals(listOf(Timer.after(pokeball.kernel.CallKey("1.0"), 60_000)), decision.outputs)
        val again = ReminderBall.decide(decision.state, ReminderRequest.Schedule("ann@example.com", "Call Bob", 60_000), ctx)
        assertEquals(Reject(ReminderReply.AlreadyScheduled), again)
    }
```

## 7. Test the feature under every failure

The test kit runs the real runtime on a simulated clock. A simulated mail service draws one fate per attempt: answer, late answer, lost request, lost answer, error after sending, or connection refused. The kit injects at most one crash at any point, and explores every combination:

<!-- snippet: reference/kotlin/examples/tutorial/src/test/kotlin/pokeball/examples/tutorial/ReminderTest.kt -->
```kotlin
        val runs = explore(ExhaustiveChooser()) { chooser ->
            val h = Harness(chooser, Profile.Durable) {
                reminderComposition(SimPort(Mailer, scheduler, chooser, truth) { })
            }
            h.maybeCrashOnce(window = 12)
            h.start()
            h.send(id, ReminderRequest.Schedule("ann@example.com", "Call Bob", delayMillis = 100))
            h.runUntilIdle()
            h.assertInvariants() // the kernel's rules, checked on the trace
            val sent = h.truth.applied.values.sum()
            assertTrue(sent <= 1, "email sent $sent times")
            when (h.runtime.inspect(id)) {
                ReminderState.Sent -> assertEquals(1, sent)
                is ReminderState.NotSent -> assertEquals(0, sent)
                ReminderState.MaybeSent, ReminderState.Empty -> Unit // Empty: the crash came before the request was accepted
                else -> throw AssertionError("unfinished: ${h.runtime.inspect(id)}")
            }
        }
```

`assertInvariants` checks the runtime's rules (serial steps, commit before dispatch, one completion per call, honest outcomes, no blind re-send) against the simulated ground truth. The last block checks your own rule: the state never contradicts what the mail service actually did.

Try breaking it. Change `is Outcome.Unknown -> step(ReminderState.MaybeSent)` to `step(ReminderState.NotSent("assumed"))` and run the test again. It fails with `expected: <0> but was: <1>`: some schedule sent the email and your Ball claimed it did not.

## Where next

- The [specification](../spec/pokeball-core-2.0.md) defines the model and the rules R1–R13.
- The [decision guide](decision-guide.md) says when this architecture is worth its cost, and when it is not.
- [Anti-patterns](anti-patterns.md) lists mistakes the rules and checks exist to catch.
- The [shop example](../reference/kotlin/examples/shop/) is a checkout workflow across three Balls, with reservations, payment reconciliation and module boundaries.
