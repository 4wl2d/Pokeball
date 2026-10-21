package bench.runner

import bench.ballarch.app.BallarchSystem
import bench.careful.mvu.app.CarefulMvuSystem
import bench.env.FaultProfile
import bench.hardened.app.HardenedSystem
import bench.harness.Cell
import bench.harness.Crash
import bench.harness.Trace
import bench.harness.RunResult
import bench.harness.Violation
import bench.harness.crashSweep
import bench.harness.crashTime
import bench.harness.experiment
import bench.harness.runOnce
import bench.harness.seedOf
import bench.harness.trace
import bench.layered.app.LayeredSystem
import bench.mvu.app.MvuSystem
import bench.runner.fixtures.Ccn
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Keeps the benchmark's headline observations and its measuring instruments honest in every build. */
class BenchmarkTest {
    private fun cell(key: String, profile: FaultProfile, crashes: Boolean, runs: Int = 300): Cell {
        val a = architectures.single { it.key == key }
        return experiment(a.key, a.system, profile, crashes, runs)
    }

    /** The class files compiled from the source set that holds [type]: a class directory or, for another module, its jar. */
    private fun classes(type: Class<*>, rootPackage: String): ClassModel {
        val location = File(type.protectionDomain.codeSource.location.toURI())
        val bytes: List<ByteArray> = if (location.isDirectory) {
            location.walkTopDown().filter { it.isFile && it.extension == "class" }.sortedBy { it.path }.map { it.readBytes() }.toList()
        } else {
            JarFile(location).use { jar ->
                jar.entries().toList().filter { it.name.endsWith(".class") }.sortedBy { it.name }.map { e -> jar.getInputStream(e).use { it.readBytes() } }
            }
        }
        val nodes = bytes.map { b -> ClassNode().also { ClassReader(b).accept(it, 0) } }.filter { it.name.startsWith(rootPackage) }
        return ClassModel(nodes)
    }

    private fun ccn(model: ClassModel): Map<String, Int> = complexity(model).associate { it.name to it.ccn }

    private val reconciling = listOf("hardened", "actors-careful", "mvu-careful", "saga-careful", "ballarch")

    @Test
    fun `careful and Ballarch checkouts violate no requirement while the delivery horizon holds`() {
        for (key in reconciling) {
            for (profile in listOf(FaultProfile.Stress, FaultProfile.Mild)) {
                for (crashes in listOf(false, true)) assertEquals(0, cell(key, profile, crashes).anyViolation.hits, "$key $profile crash=$crashes")
            }
        }
    }

    @Test
    fun `beyond the delivery horizon, careful and Ballarch checkouts report charged orders as failed`() {
        for (key in reconciling) {
            val c = cell(key, FaultProfile.BeyondHorizon, crashes = false)
            assertTrue(c.anyViolation.hits > 0, key)
            assertEquals(c.anyViolation.hits, c.byViolation.getValue(Violation.FalseFailure).hits, "$key: only FalseFailure")
        }
    }

    @Test
    fun `every run of a crash cell crashes exactly once`() {
        for (a in architectures) {
            assertEquals(100, cell(a.key, FaultProfile.Stress, crashes = true, runs = 100).crashedRuns, a.key)
            assertEquals(0, cell(a.key, FaultProfile.Stress, crashes = false, runs = 100).crashedRuns, a.key)
        }
    }

    @Test
    fun `crash times are uniform over the whole milliseconds of the busy period`() {
        // The task times do not matter: only the busy period does.
        val trace = Trace(longArrayOf(0, 0, 0, 20, 40, 40, 1000), busyMillis = 40)
        val times = (1L..4100L).map { crashTime(trace, it) }
        assertEquals((0L..40L).toSet(), times.toSet(), "every millisecond of the busy period, and nothing else")
        val perMillisecond = times.groupingBy { it }.eachCount().values
        assertTrue(perMillisecond.max() < 3 * perMillisecond.min(), "crash times are not uniform: $perMillisecond")
    }

    @Test
    fun `a crash at a time comes before every task due at that time`() {
        for (a in architectures) {
            for (i in 0 until 4) {
                val seed = seedOf(i, FaultProfile.Mild, crashes = true)
                val trace = trace(a.system, seed, FaultProfile.Mild)
                val times = trace.taskMillis
                for ((first, at) in times.withIndex()) {
                    if (at > trace.busyMillis || (first > 0 && times[first - 1] == at)) continue
                    val before = runOnce(a.system, seed, FaultProfile.Mild, Crash.BeforeTask(first.toLong()))
                    // At the time of a task, the crash is the crash immediately before the first task of that instant.
                    assertEquals(before, runOnce(a.system, seed, FaultProfile.Mild, Crash.AtTime(at)), "${a.key} seed $seed t=$at")
                    // In the idle gap before it, the process crashes in the same state, at the drawn time.
                    if (first > 0 && times[first - 1] < at - 1) {
                        val gap = runOnce(a.system, seed, FaultProfile.Mild, Crash.AtTime(at - 1))
                        assertEquals(at - 1, gap.crashMillis, "${a.key} seed $seed t=${at - 1}")
                        assertEquals(before.acceptedAtCrash, gap.acceptedAtCrash, "${a.key} seed $seed t=${at - 1}")
                    }
                }
            }
        }
    }

    @Test
    fun `careful and Ballarch checkouts survive a crash before any task`() {
        for (key in reconciling) {
            for (profile in listOf(FaultProfile.Stress, FaultProfile.Mild)) {
                val a = architectures.single { it.key == key }
                val sweep = crashSweep(a.key, a.system, profile, seeds = 60)
                assertTrue(sweep.points > 60 * 10, "$key $profile")
                assertEquals(0L, sweep.violating, "$key $profile")
            }
        }
        // The same sweep finds violating crash points in a naive baseline.
        assertTrue(crashSweep("layered", ::LayeredSystem, FaultProfile.Mild, seeds = 20).violating > 0)
    }

    @Test
    fun `the oracles detect violations in naive baselines and in seeded bugs`() {
        for (key in listOf("layered", "actors", "mvu", "saga")) assertTrue(cell(key, FaultProfile.Stress, crashes = true).anyViolation.hits > 0, key)
        for (bug in seeded) {
            assertTrue(experiment(bug.name, bug.system, FaultProfile.Stress, crashes = true, runs = 300).anyViolation.hits > 0, bug.name)
            val crashFree = experiment(bug.name, bug.system, FaultProfile.Stress, crashes = false, runs = 300).anyViolation.hits
            // A recovery bug changes only what a restarted incarnation finds: it shows up only in runs that crash.
            if (bug.recovery) assertEquals(0, crashFree, bug.name) else assertTrue(crashFree > 0, bug.name)
        }
    }

    @Test
    fun `with idempotency keys, no design charges twice, and naive retries still report charged orders as failed`() {
        val naive = setOf("layered", "actors", "mvu", "saga")
        for (d in keyedDesigns) {
            for (profile in listOf(FaultProfile.Stress, FaultProfile.Mild)) {
                for (crashes in listOf(false, true)) {
                    val c = experiment(d.key, d.system, profile, crashes, runs = 200, keys = true)
                    val where = "${d.key} $profile crash=$crashes"
                    assertEquals(0, c.byViolation.getValue(Violation.DoubleCharge).hits, where)
                    when {
                        d.key !in naive -> assertEquals(0, c.anyViolation.hits, where)
                        // Three requests that all go unanswered end as Failed although one of them may have charged.
                        profile == FaultProfile.Stress && !crashes -> assertTrue(c.byViolation.getValue(Violation.FalseFailure).hits > 0, where)
                    }
                }
            }
        }
    }

    @Test
    fun `with idempotency keys, Ballarch and the careful designs have the same request budget`() {
        fun system(key: String) = keyedDesigns.single { it.key == key }.system
        // The same policy and budgets give the same outcomes and provider requests, seed for seed, until a crash.
        fun seen(r: RunResult) = listOf(r.violations, r.completed, r.failed, r.needsAttention, r.chargeRequests, r.statusRequests)
        for (profile in FaultProfile.entries) {
            for (i in 0 until 100) {
                val seed = seedOf(i, profile, crashes = false)
                val ballarch = seen(runOnce(system("ballarch"), seed, profile, crash = null, keys = true))
                for (key in listOf("hardened", "actors-careful", "mvu-careful", "saga-careful")) {
                    assertEquals(seen(runOnce(system(key), seed, profile, crash = null, keys = true)), ballarch, "$key $profile seed $seed")
                }
            }
        }
        // With the reference adapter, an attempt ends at the call's deadline, so a lost answer is never sent again.
        fun requests(key: String) = experiment(key, system(key), FaultProfile.Stress, crashes = false, runs = 200, keys = true).chargeRequestsPerOrder
        assertTrue(requests("ballarch-refadapter") < 0.7 * requests("ballarch"))
    }

    @Test
    fun `generated classes belong to the class that declares them`() {
        assertEquals("a/B", unitOf("a/B\$process\$1"))
        assertEquals("a/B\$State", unitOf("a/B\$State"))
        assertEquals("a/B\$State\$Done", unitOf("a/B\$State\$Done"))
        assertEquals("a/B", unitOf("a/B\$WhenMappings"))
        assertEquals("a/FileKt", unitOf("a/FileKt\$checkout\$1\$1"))
    }

    @Test
    fun `cohesion ignores compiler-generated members and follows lambdas and default-argument helpers`() {
        val model = classes(Ccn::class.java, "bench/runner/fixtures/")
        fun node(name: String) = model.nodes.single { it.name == "bench/runner/fixtures/$name" }
        // Value-class mangling, enum members and a PascalCase top-level val leave nothing to measure.
        for (name in listOf("Keyed", "Light", "TopLevelKt")) {
            assertNull(lcom4(model, node(name)), name)
            assertNull(camc(node(name)), name)
        }
        assertEquals(1, lcom4(model, node("Counter")), "a field used only inside a lambda")
        assertEquals(1, lcom4(model, node("Launcher")), "a private field used only inside a coroutine, through access\$getCount\$p")
        assertEquals(1, lcom4(model, node("Ticket")), "fields read by copy's default-argument helper")
        assertEquals(9.0 / 16, camc(node("Employee"))!!, 1e-12)
    }

    @Test
    fun `cyclomatic complexity matches hand counts on fixtures`() {
        val counted = ccn(classes(Ccn::class.java, "bench/runner/fixtures/"))
        val expected = mapOf(
            "straight(int)" to 1, "branch(int)" to 2, "loop(List)" to 3, "both(boolean, boolean)" to 3,
            "elvis(String)" to 2, "safeCall(String)" to 2, "caught(Function0)" to 1, "whenSealed(Shape)" to 3,
            "whenEnum(Color)" to 3, "mapped(List)" to 2, "lambda()" to 2, "suspending(int)" to 2,
            "usesLateinit()" to 1, "cast(Object)" to 1, "resultOrNull(Object)" to 2, "folded(Object)" to 2,
            "orEmptyMap(Map)" to 2, "repeated(int)" to 2, "orDefault(Object)" to 2, "taken(String, boolean)" to 2,
        ).mapKeys { "bench.runner.fixtures.Ccn.${it.key}" }
        assertEquals(expected, counted.filterKeys { it.startsWith("bench.runner.fixtures.Ccn.") })
    }

    @Test
    fun `cyclomatic complexity matches hand counts on benchmark functions`() {
        // Each count is 1 + the decisions in the source, under the rules in benchmark/README.md.
        val hardened = ccn(classes(HardenedSystem::class.java, "bench/hardened/"))
        // `?: return`, and five `if`s on the status.
        assertEquals(7, hardened["bench.hardened.application.CheckoutInteractor.process(String)"])
        // The `repeat` loop, and three of the four cases of `when (attempt)`; the last case's check is generated.
        assertEquals(5, hardened["bench.hardened.application.CheckoutInteractor.pay(CheckoutOrder)"])
        // The `repeat` loop, and two of the three cases of `when (status)`.
        assertEquals(4, hardened["bench.hardened.application.CheckoutInteractor.reconcile(CheckoutOrder)"])
        val ballarch = ccn(classes(BallarchSystem::class.java, "bench/ballarch/"))
        // `is Reserve` (the last case, `is Release`, is generated), `in reservations`, `<`, and `?: 0`.
        assertEquals(5, ballarch["bench.ballarch.inventory.InventoryBall.decide(Stock, InventoryRequest, RequestContext)"])
        // `is Charging`, `&&`, `is Done`, `is NotDone`, `if (sends < maxSends)`, `is Reconciling`, `&&`.
        assertEquals(8, ballarch["bench.ballarch.payment.PaymentBall.complete(PaymentState, Completion, Context)"])
        // `as?`, `?.`, and three of the four cases of `when (r)`.
        assertEquals(6, ballarch["bench.ballarch.checkout.CheckoutFlow.paid(Paying, Outcome, Context)"])
        val layered = ccn(classes(LayeredSystem::class.java, "bench/layered/"))
        // In the transaction lambda: `!= null`, two `?:`, and `<`.
        assertEquals(5, layered["bench.layered.adapters.DbInventoryRepository.reserve(String, String, int)"])
        val mvu = ccn(classes(MvuSystem::class.java, "bench/mvu/"))
        // `is Msg.Place` (the last case, `is Msg.ChargeCompleted`, is generated), `in`, `<`, `== null`, `!=`,
        // the branch inside the @InlineOnly `getOrNull`, `as?`, and `!= null`.
        assertEquals(9, mvu["bench.mvu.core.CheckoutKt.update(Model, Msg)"])
        val carefulMvu = ccn(classes(CarefulMvuSystem::class.java, "bench/careful/mvu/"))
        // `is Msg.Place`, `in`, `<`; then twice (the last case, `is Msg.StatusChecked`, is generated): `?.`, `==`,
        // the branch inside the @InlineOnly `takeIf`, `?.let` and `?:`; and `is Msg.ChargeCompleted`.
        assertEquals(15, carefulMvu["bench.careful.mvu.core.CheckoutKt.update(Config, Model, Msg)"])
    }
}
