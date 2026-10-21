package ballarch.runtime

import ballarch.kernel.AdapterPort
import ballarch.kernel.BallId
import ballarch.kernel.CallId
import ballarch.kernel.CallKey
import ballarch.kernel.EffectClass
import ballarch.kernel.accept
import ballarch.kernel.acceptLater
import ballarch.testkit.ExhaustiveChooser
import ballarch.testkit.Fate
import ballarch.testkit.Harness
import ballarch.testkit.Profile
import ballarch.testkit.RandomChooser
import ballarch.testkit.SimPort
import ballarch.testkit.explore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Systematic exploration: every combination of network fates and one crash
 * point (or none) within a bounded window, with every kernel oracle checked on
 * every run. These are bounded checks of the implementation, complementing the
 * Alloy model of the abstract protocol.
 */
class ExplorationTest {
    private val payer = BallId("payer", "p1")

    /** Names a sample trace: the run's result, and whether the runtime crashed before or after it sent a call. */
    private fun variant(result: String, h: Harness): String {
        if (h.crashes == 0) return result.lowercase()
        val events = h.tracer.events
        val restart = events.indexOfLast { it is TraceEvent.Recovered }.coerceAtLeast(0)
        val sentBeforeCrash = events.take(restart).any { it is TraceEvent.CallSent }
        return result.lowercase() + if (sentBeforeCrash) "-crash-after-send" else "-crash"
    }

    private fun exploreCalls(
        scenario: String,
        port: AdapterPort<Int, String>,
        retry: RetryPolicy,
        profile: Profile,
        crashWindow: Int,
    ): Pair<Int, Map<String, Int>> {
        val finals = HashMap<String, Int>()
        val samples = ConformanceTraces.Sampler(scenario)
        val runs = explore(ExhaustiveChooser(maxChoices = 8)) { chooser ->
            val h = Harness(chooser, profile) {
                Composition()
                    .ball(PayerBall(port))
                    .adapter(port, SimPort(port, scheduler, chooser, truth, refuse = { "refused" }) { "ok-$it" }, retry)
            }
            h.maybeCrashOnce(window = crashWindow)
            h.start()
            h.send(payer, Pay(10, timeoutMillis = 200))
            h.runUntilIdle()
            h.assertInvariants()
            val state = h.runtime.inspect(payer)
            val label = when (state) {
                is PayerState.Finished -> state.outcome.substringBefore(':')
                else -> state!!::class.simpleName!!
            }
            finals.merge(label, 1, Int::plus)
            samples.offer(variant(label, h), h)
            // Business-level honesty: the Ball's own conclusion agrees with ground truth.
            if (state is PayerState.Finished) {
                val applied = h.truth.applied.values.sum()
                when {
                    state.outcome.startsWith("paid") && port.effect != EffectClass.Safe && applied == 0 ->
                        fail("paid but nothing applied; choices=${chooser.history}")
                    state.outcome.startsWith("not-paid") && port.effect != EffectClass.Safe && applied > 0 ->
                        fail("not-paid but effect applied; choices=${chooser.history}")
                }
            }
            // Durable liveness: once the request was accepted, the Ball reaches a conclusion
            // even if the runtime crashed. (A crash before acceptance loses the request.)
            if (profile == Profile.Durable && state !is PayerState.Idle) {
                assertTrue(state is PayerState.Finished, "durable run did not finish: $state; choices=${chooser.history}")
            }
        }
        samples.crossCheck()
        return runs to finals
    }

    @Test
    fun `the harness reports an accepted request left unanswered past its deadline`() {
        fun run(act: Act): Harness = Harness(RandomChooser(1)) { Composition().ball(ScriptBall()) }.apply {
            start()
            send(BallId("script", "s"), act, timeoutMillis = 100)
            runUntilIdle()
        }
        val owing = run(Act("owe") { st, ctx -> acceptLater(st + ctx.requestId) })
        owing.assertInvariants() // at most one reply: R9's MUST half holds
        assertEquals(1, owing.replyLeaks().size)
        assertFailsWith<AssertionError> { owing.assertNoReplyLeaks() }
        run(Act("answer") { st, _ -> accept(st, "ok") }).assertNoReplyLeaks()
    }

    @Test
    fun `non-idempotent call under every fate and crash point`() {
        val (runs, finals) = exploreCalls("non-idempotent-durable", PaymentPort, RetryPolicy.None, Profile.Durable, crashWindow = 24)
        println("non-idempotent durable: $runs runs, outcomes $finals")
        assertTrue(finals.keys.containsAll(setOf("paid", "not-paid", "unknown")))
    }

    @Test
    fun `idempotent call with retries under every fate and crash point`() {
        val port = AdapterPort<Int, String>("idem", EffectClass.Idempotent)
        val (runs, finals) = exploreCalls("idempotent-durable", port, RetryPolicy(maxAttempts = 3), Profile.Durable, crashWindow = 16)
        println("idempotent durable: $runs runs, outcomes $finals")
        assertTrue(finals.keys.containsAll(setOf("paid", "not-paid", "unknown")))
    }

    @Test
    fun `safe call with retries never reports an unknown effect`() {
        val port = AdapterPort<Int, String>("safe", EffectClass.Safe)
        val (runs, finals) = exploreCalls("safe-durable", port, RetryPolicy(maxAttempts = 2), Profile.Durable, crashWindow = 16)
        println("safe durable: $runs runs, outcomes $finals")
        assertTrue("unknown" !in finals, "a Safe port must never end Unknown: $finals")
    }

    @Test
    fun `transient profile keeps the safety rules under every fate and crash point`() {
        val (runs, finals) = exploreCalls("non-idempotent-transient", PaymentPort, RetryPolicy.None, Profile.Transient, crashWindow = 24)
        println("non-idempotent transient: $runs runs, outcomes $finals")
    }

    @Test
    fun `after a transient restart a new payment reuses the call identities of the lost world`() {
        val samples = ConformanceTraces.Sampler("non-idempotent-transient-pay-again")
        val call = CallId(payer, CallKey("1.0"))
        var reused = 0
        val runs = explore(ExhaustiveChooser(maxChoices = 8)) { chooser ->
            val h = Harness(chooser, Profile.Transient) {
                Composition()
                    .ball(PayerBall())
                    .adapter(PaymentPort, SimPort(PaymentPort, scheduler, chooser, truth, refuse = { "refused" }) { "ok-$it" })
            }
            h.maybeCrashOnce(window = 24)
            h.start()
            h.send(payer, Pay(10, timeoutMillis = 200))
            h.runUntilIdle()
            // A restarted Transient runtime starts from an empty store, so this payment is call 1.0 again.
            h.send(payer, Pay(20, timeoutMillis = 200))
            h.runUntilIdle()
            h.assertInvariants()
            val again = call in h.truth.at(0).effectOf && call in h.truth.at(1).effectOf
            if (again) reused++
            val state = h.runtime.inspect(payer)
            val label = (state as? PayerState.Finished)?.outcome?.substringBefore(':') ?: state!!::class.simpleName!!
            samples.offer(variant(label, h) + if (again) "-sent-again" else "", h)
        }
        samples.crossCheck()
        println("non-idempotent transient, paying again: $runs runs, $reused send call 1.0 in two epochs")
        assertTrue(reused > 0)
    }

    @Test
    fun `Ball-to-Ball request is applied at most once across a crash anywhere`() {
        var runs = 0
        val samples = ConformanceTraces.Sampler("ball-to-ball-durable")
        explore(ExhaustiveChooser()) { chooser ->
            val h = Harness(chooser, Profile.Durable) {
                Composition().ball(CounterBall()).ball(RelayBall())
            }
            h.maybeCrashOnce(window = 20)
            h.start()
            h.send(BallId("relay", "r"), Forward("c1", CounterMsg.Increment), timeoutMillis = 5_000)
            h.runUntilIdle()
            h.assertInvariants()
            val count = h.runtime.inspect(BallId("counter", "c1")) as Int
            val relay = h.runtime.inspect(BallId("relay", "r"))
            assertTrue(count <= 1, "counter applied twice; choices=${chooser.history}")
            if (relay is RelayState.Done && relay.text.startsWith("done")) assertEquals(1, count)
            samples.offer(variant(if (relay is RelayState.Done) relay.text.substringBefore(':') else relay!!::class.simpleName!!, h), h)
            runs++
        }
        samples.crossCheck()
        println("ball-to-ball durable: $runs runs")
    }

    @Test
    fun `notices are delivered exactly once in order across a crash anywhere`() {
        var runs = 0
        val samples = ConformanceTraces.Sampler("notices-durable")
        explore(ExhaustiveChooser()) { chooser ->
            val h = Harness(chooser, Profile.Durable) {
                Composition().ball(CounterBall()).ball(AuditBall()).subscribe(CounterChanged, "audit") { "all" }
            }
            h.maybeCrashOnce(window = 30)
            h.start()
            repeat(3) { h.send(BallId("counter", "c1"), CounterMsg.Increment, timeoutMillis = 5_000) }
            h.runUntilIdle()
            h.assertInvariants()
            val committed = h.runtime.inspect(BallId("counter", "c1")) as Int
            val audit = h.runtime.inspect(BallId("audit", "all"))
            assertEquals((1..committed).toList(), audit, "choices=${chooser.history}")
            samples.offer(variant("committed-$committed", h), h)
            runs++
        }
        samples.crossCheck()
        println("notices durable: $runs runs")
    }
}
