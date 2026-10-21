package bench.runner

import bench.env.CheckoutSystem
import bench.env.FaultProfile
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import kotlin.math.sqrt

/** The requirements every implementation was written against (benchmark/README.md). */
enum class Violation(val description: String) {
    DoubleCharge("an order was charged more than once"),
    FalseFailure("an order reported Failed was charged"),
    UnpaidCompletion("an order reported Completed was not charged"),
    Stranded("an accepted order has no terminal state at the end"),
    StockLeak("an order reported Failed still holds stock"),
    Oversell("more orders completed than units in stock"),
    LostCharge("a charge exists for an order the system never accepted"),
}

data class RunResult(
    val violations: Set<Violation>,
    val accepted: Int,
    val completed: Int,
    val failed: Int,
    val needsAttention: Int,
    val chargeRequests: Int,
    val statusRequests: Int,
    val tasks: Long,
)

/** Runs the checkout scenario once on [system]; crashes it before scheduler task [crashAt] if given. */
fun runOnce(system: () -> CheckoutSystem, seed: Long, profile: FaultProfile, crashAt: Long?): RunResult {
    val world = World(seed, profile)
    val sut = system()
    var tasks = 0L
    world.scheduler.beforeTask = {
        if (tasks++ == crashAt) {
            sut.crash()
            world.incarnation++
            world.scheduler.schedule(Scenario.RESTART_MILLIS) { sut.boot(world) }
        }
    }
    sut.boot(world)
    world.scheduler.execute { for (o in Scenario.orders) sut.start(o) }
    world.scheduler.runUntilIdle(until = Scenario.END_MILLIS)

    val v = LinkedHashSet<Violation>()
    val reserved = sut.reserved()
    var completed = 0
    var failed = 0
    var attention = 0
    var accepted = 0
    for (o in Scenario.orders) {
        val charges = world.provider.charges[o.id] ?: 0
        val outcome = sut.outcome(o.id)
        val isAccepted = sut.accepted(o.id)
        if (isAccepted) accepted++
        if (charges > 1) v += Violation.DoubleCharge
        if (outcome == Terminal.Failed && charges > 0) v += Violation.FalseFailure
        if (outcome == Terminal.Completed && charges == 0) v += Violation.UnpaidCompletion
        if (isAccepted && outcome == null) v += Violation.Stranded
        if (outcome == Terminal.Failed && (reserved[o.id] ?: 0) > 0) v += Violation.StockLeak
        if (!isAccepted && charges > 0) v += Violation.LostCharge
        when (outcome) {
            Terminal.Completed -> completed++
            Terminal.Failed -> failed++
            Terminal.NeedsAttention -> attention++
            null -> Unit
        }
    }
    if (completed > Scenario.STOCK) v += Violation.Oversell
    return RunResult(
        v, accepted, completed, failed, attention,
        world.provider.chargeRequests.values.sum(), world.provider.statusRequests.values.sum(), tasks,
    )
}

/** A proportion with its Wilson score 95% interval. */
data class Rate(val hits: Int, val n: Int) {
    val value: Double get() = if (n == 0) 0.0 else hits.toDouble() / n

    val interval: Pair<Double, Double>
        get() {
            if (n == 0) return 0.0 to 1.0
            val z = 1.959964
            val p = value
            val d = 1 + z * z / n
            val center = (p + z * z / (2 * n)) / d
            val half = z * sqrt(p * (1 - p) / n + z * z / (4.0 * n * n)) / d
            return (center - half).coerceAtLeast(0.0) to (center + half).coerceAtMost(1.0)
        }
}

data class Cell(
    val architecture: String,
    val profile: FaultProfile,
    val crashes: Boolean,
    val runs: Int,
    val anyViolation: Rate,
    val byViolation: Map<Violation, Rate>,
    val needsAttentionPerOrder: Double,
    val acceptedPerRun: Double,
    val completedPerRun: Double,
    val chargeRequestsPerOrder: Double,
    val statusRequestsPerOrder: Double,
)

/**
 * Runs [runs] seeds for one architecture, profile and crash setting. With
 * crashes, each run crashes once before a task chosen uniformly among the
 * tasks of a typical crash-free run of the same architecture ([typicalTasks]).
 */
fun experiment(name: String, system: () -> CheckoutSystem, profile: FaultProfile, crashes: Boolean, runs: Int, typicalTasks: Long): Cell {
    val results = (0 until runs).map { i ->
        val seed = 1_000_003L * (i + 1) + profile.ordinal * 7_919L + (if (crashes) 104_729L else 0L)
        val crashAt = if (crashes) kotlin.random.Random(seed xor 0x5DEECE66DL).nextLong(typicalTasks) else null
        runOnce(system, seed, profile, crashAt)
    }
    val orders = runs * Scenario.orders.size
    return Cell(
        architecture = name,
        profile = profile,
        crashes = crashes,
        runs = runs,
        anyViolation = Rate(results.count { it.violations.isNotEmpty() }, runs),
        byViolation = Violation.entries.associateWith { v -> Rate(results.count { v in it.violations }, runs) },
        needsAttentionPerOrder = results.sumOf { it.needsAttention }.toDouble() / orders,
        acceptedPerRun = results.sumOf { it.accepted }.toDouble() / runs,
        completedPerRun = results.sumOf { it.completed }.toDouble() / runs,
        chargeRequestsPerOrder = results.sumOf { it.chargeRequests }.toDouble() / orders,
        statusRequestsPerOrder = results.sumOf { it.statusRequests }.toDouble() / orders,
    )
}

/** Median scheduler tasks of crash-free runs: the window from which crash points are drawn. */
fun typicalTasks(system: () -> CheckoutSystem, profile: FaultProfile): Long {
    val counts = (1..41).map { runOnce(system, it * 31L, profile, null).tasks }.sorted()
    return counts[counts.size / 2].coerceAtLeast(1)
}
