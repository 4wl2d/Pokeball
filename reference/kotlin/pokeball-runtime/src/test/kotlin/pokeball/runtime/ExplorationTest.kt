package pokeball.runtime

import pokeball.kernel.AdapterPort
import pokeball.kernel.BallId
import pokeball.kernel.EffectClass
import pokeball.testkit.ExhaustiveChooser
import pokeball.testkit.Fate
import pokeball.testkit.Harness
import pokeball.testkit.Profile
import pokeball.testkit.SimPort
import pokeball.testkit.explore
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun exploreCalls(port: AdapterPort<Int, String>, retry: RetryPolicy, profile: Profile, crashWindow: Int): Pair<Int, Map<String, Int>> {
        val finals = HashMap<String, Int>()
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
        return runs to finals
    }

    @Test
    fun `non-idempotent call under every fate and crash point`() {
        val (runs, finals) = exploreCalls(PaymentPort, RetryPolicy.None, Profile.Durable, crashWindow = 24)
        println("non-idempotent durable: $runs runs, outcomes $finals")
        assertTrue(finals.keys.containsAll(setOf("paid", "not-paid", "unknown")))
    }

    @Test
    fun `idempotent call with retries under every fate and crash point`() {
        val port = AdapterPort<Int, String>("idem", EffectClass.Idempotent)
        val (runs, finals) = exploreCalls(port, RetryPolicy(maxAttempts = 3), Profile.Durable, crashWindow = 16)
        println("idempotent durable: $runs runs, outcomes $finals")
        assertTrue(finals.keys.containsAll(setOf("paid", "not-paid", "unknown")))
    }

    @Test
    fun `safe call with retries never reports an unknown effect`() {
        val port = AdapterPort<Int, String>("safe", EffectClass.Safe)
        val (runs, finals) = exploreCalls(port, RetryPolicy(maxAttempts = 2), Profile.Durable, crashWindow = 16)
        println("safe durable: $runs runs, outcomes $finals")
        assertTrue("unknown" !in finals, "a Safe port must never end Unknown: $finals")
    }

    @Test
    fun `transient profile keeps the safety rules under every fate and crash point`() {
        val (runs, finals) = exploreCalls(PaymentPort, RetryPolicy.None, Profile.Transient, crashWindow = 24)
        println("non-idempotent transient: $runs runs, outcomes $finals")
    }

    @Test
    fun `Ball-to-Ball request is applied at most once across a crash anywhere`() {
        var runs = 0
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
            runs++
        }
        println("ball-to-ball durable: $runs runs")
    }

    @Test
    fun `notices are delivered exactly once in order across a crash anywhere`() {
        var runs = 0
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
            runs++
        }
        println("notices durable: $runs runs")
    }
}
