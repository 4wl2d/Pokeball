package bench.harness

import bench.env.CheckoutSystem
import bench.env.FaultProfile
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import kotlin.math.sqrt

/*
 * The simulation harness: runs the checkout scenario on a design, with faults
 * and at most one crash, and judges it. The benchmark runner and the change
 * requests' runner (benchmark/changes) share it.
 */

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
    /** Scheduler tasks executed, by the system and by the simulated environment (not the harness's crash event). */
    val tasks: Long,
    /** Whether the requested [Crash] happened. */
    val crashed: Boolean,
    /** Virtual time of the crash; null without one. */
    val crashMillis: Long?,
    /** Orders durably accepted when the crash hit; null without one. */
    val acceptedAtCrash: Int?,
    /** Violations of a change request's own requirements (see [Extension]); empty in the base scenario. */
    val changeViolations: Set<String> = emptySet(),
    /** A change request's per-run counts (see [Extension.counts]). */
    val counts: Map<String, Int> = emptyMap(),
)

/**
 * What a change request (benchmark/changes) adds to the base scenario: client
 * requests, states the busy period waits for, and requirements. The base
 * scenario runs without one.
 */
interface Extension {
    /** Names of the change request's own requirements, in report order. */
    val oracles: List<String>

    /** Base requirements that no longer apply under the change request. */
    val dropped: Set<Violation> get() = emptySet()

    /** Called once at the start of a run, after the first incarnation booted and the first orders were scheduled: schedules the client's other requests. */
    fun begin(world: World, sut: CheckoutSystem) {}

    /** Called right after a restart, after the client sent again the orders the system had not accepted. */
    fun restarted(world: World, sut: CheckoutSystem) {}

    /** States, besides each order's outcome, whose changes the busy period follows. */
    fun watch(sut: CheckoutSystem): List<Any?> = emptyList()

    /** Whether some obligation of the change request still has no final state. */
    fun unresolved(sut: CheckoutSystem): Boolean = false

    /** The change request's requirements that the run violated. */
    fun check(world: World, sut: CheckoutSystem): Set<String>

    /** Per-run counts reported as means (for example refunds or awards). */
    fun counts(world: World, sut: CheckoutSystem): Map<String, Int> = emptyMap()
}

/**
 * A crash-free run, seen from outside: the virtual time of every scheduler task
 * in order, and the end of the checkout's busy period, which is the time of the
 * task after which every accepted order had its terminal state (the last task
 * of the run if an order never got one).
 */
class Trace(val taskMillis: LongArray, val busyMillis: Long)

/** Where a run crashes. The process restarts [Scenario.RESTART_MILLIS] after the crash. */
sealed interface Crash {
    /** Immediately before scheduler task number [index] of the run, counting from 0: one point of the exhaustive sweep. */
    data class BeforeTask(val index: Long) : Crash

    /**
     * At virtual time [millis], as an event of its own: before every task due
     * at that time, and, in an idle gap, while nothing runs. Scheduler tasks
     * take no virtual time, so a crash at a time cannot fall between two tasks
     * of the same instant; the sweep covers those points.
     */
    data class AtTime(val millis: Long) : Crash
}

/**
 * Runs the checkout scenario once on [system], with at most one [crash]. The
 * client is at least once: right after the restart it sends again every order
 * that the system has not durably accepted. With [keys], the provider honours
 * idempotency keys (scenario B). [extension] adds a change request's client
 * requests and requirements. [observe] is called before every scheduler task,
 * with its virtual time.
 */
fun runOnce(
    system: () -> CheckoutSystem,
    seed: Long,
    profile: FaultProfile,
    crash: Crash?,
    keys: Boolean = false,
    extension: Extension? = null,
    observe: ((CheckoutSystem, Long) -> Unit)? = null,
): RunResult {
    val world = World(seed, profile, idempotencyKeys = keys)
    val sut = system()
    var tasks = 0L
    var crashMillis: Long? = null
    var acceptedAtCrash: Int? = null
    fun crashNow() {
        crashMillis = world.scheduler.now()
        acceptedAtCrash = Scenario.orders.count { sut.accepted(it.id) }
        sut.crash()
        world.incarnation++
        world.scheduler.schedule(Scenario.RESTART_MILLIS) {
            sut.boot(world)
            for (o in Scenario.orders) if (!sut.accepted(o.id)) sut.start(o)
            extension?.restarted(world, sut)
        }
    }
    world.scheduler.beforeTask = {
        observe?.invoke(sut, world.scheduler.now())
        if (crash is Crash.BeforeTask && tasks == crash.index) crashNow()
        tasks++
    }
    // Scheduled before anything else, the crash event runs first among the tasks due at its time.
    if (crash is Crash.AtTime) world.scheduler.schedule(crash.millis) { crashNow() }
    sut.boot(world)
    world.scheduler.execute { for (o in Scenario.orders) sut.start(o) }
    extension?.begin(world, sut)
    world.scheduler.runUntilIdle(until = Scenario.END_MILLIS)

    val v = LinkedHashSet<Violation>()
    val reserved = sut.reserved()
    var completed = 0
    var failed = 0
    var attention = 0
    var accepted = 0
    for (o in Scenario.orders) {
        // Both providers: the secondary one exists only in change request CR3 and charges nothing otherwise.
        val charges = (world.provider.charges[o.id] ?: 0) + (world.secondaryProvider.charges[o.id] ?: 0)
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
            Terminal.Refunded, null -> Unit
        }
    }
    if (completed > Scenario.STOCK) v += Violation.Oversell
    if (extension != null) v -= extension.dropped
    val providers = listOf(world.provider, world.secondaryProvider)
    return RunResult(
        v, accepted, completed, failed, attention,
        providers.sumOf { it.chargeRequests.values.sum() }, providers.sumOf { it.statusRequests.values.sum() },
        tasks = if (crash is Crash.AtTime && crashMillis != null) tasks - 1 else tasks,
        crashed = crashMillis != null, crashMillis = crashMillis, acceptedAtCrash = acceptedAtCrash,
        changeViolations = extension?.check(world, sut).orEmpty(),
        counts = extension?.counts(world, sut).orEmpty(),
    )
}

/** Runs [seed] without a crash and records its [Trace]. With an [extension], the busy period also waits for its watched states. */
fun trace(system: () -> CheckoutSystem, seed: Long, profile: FaultProfile, keys: Boolean = false, extension: Extension? = null): Trace {
    val times = ArrayList<Long>()
    var outcomes: List<Any?> = Scenario.orders.map { null }
    var lastChange = 0L
    fun look(sut: CheckoutSystem) {
        val now: List<Any?> = Scenario.orders.map { sut.outcome(it.id) } + extension?.watch(sut).orEmpty()
        // The outcomes changed during the task that ran last.
        if (now != outcomes) {
            outcomes = now
            lastChange = times.last()
        }
    }
    var finished: CheckoutSystem? = null
    runOnce(system, seed, profile, crash = null, keys = keys, extension = extension) { sut, now ->
        if (times.isNotEmpty()) look(sut) else outcomes = outcomes + extension?.watch(sut).orEmpty()
        times += now
        finished = sut
    }
    val sut = checkNotNull(finished) { "seed $seed ran no task" }
    look(sut)
    val unresolved = Scenario.orders.any { sut.accepted(it.id) && sut.outcome(it.id) == null } || extension?.unresolved(sut) == true
    return Trace(times.toLongArray(), if (unresolved) times.last() else lastChange)
}

/**
 * The crash time of a crash-cell run: drawn uniformly from the whole
 * milliseconds of the busy period [0, busyMillis] of the same seed's
 * crash-free run. The run up to that time is the crash-free run, so the crash
 * always happens. Every design gets the same distribution of crash times
 * relative to its own busy period, whatever the number of scheduler tasks it
 * runs.
 */
fun crashTime(trace: Trace, seed: Long): Long = kotlin.random.Random(seed xor 0x5DEECE66DL).nextLong(trace.busyMillis + 1)

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
    /** Scenario B: the provider honours idempotency keys. */
    val keys: Boolean,
    val runs: Int,
    val anyViolation: Rate,
    val byViolation: Map<Violation, Rate>,
    val needsAttentionPerOrder: Double,
    val acceptedPerRun: Double,
    val completedPerRun: Double,
    val chargeRequestsPerOrder: Double,
    val statusRequestsPerOrder: Double,
    /** Runs in which the process crashed: all of them in a crash cell, none otherwise. */
    val crashedRuns: Int,
    /** Mean busy period of the crash-free runs that crash times were drawn from (0 without crashes). */
    val busyMillisMean: Double,
    /** Mean virtual time of the crash, which is the drawn crash time (0 without crashes). */
    val crashMillisMean: Double,
    /** Runs whose crash came before the system had durably accepted any order. */
    val crashesBeforeAcceptance: Int,
    /** Runs that violated each of a change request's own requirements (empty in the base scenario). */
    val byChangeViolation: Map<String, Rate> = emptyMap(),
    /** Runs that violated at least one of a change request's own requirements. */
    val anyChangeViolation: Rate = Rate(0, runs),
    /** Means per run of a change request's counts. */
    val countsPerRun: Map<String, Double> = emptyMap(),
)

/** Whether a run violated any requirement, a change request's included. */
val RunResult.violates: Boolean get() = violations.isNotEmpty() || changeViolations.isNotEmpty()

/** The seed of run [i] of a cell; crash cells use different seeds from crash-free cells. */
fun seedOf(i: Int, profile: FaultProfile, crashes: Boolean): Long =
    1_000_003L * (i + 1) + profile.ordinal * 7_919L + (if (crashes) 104_729L else 0L)

/**
 * Runs [runs] seeds for one architecture, profile and crash setting. With
 * crashes, every run crashes exactly once, at the time [crashTime] draws from
 * the busy period of the seed's own crash-free run. Both scenarios use the
 * same seeds.
 */
fun experiment(
    name: String,
    system: () -> CheckoutSystem,
    profile: FaultProfile,
    crashes: Boolean,
    runs: Int,
    keys: Boolean = false,
    extension: Extension? = null,
): Cell {
    val busy = ArrayList<Long>()
    val results = (0 until runs).map { i ->
        val seed = seedOf(i, profile, crashes)
        val crash = if (crashes) {
            trace(system, seed, profile, keys, extension).also { busy += it.busyMillis }.let { Crash.AtTime(crashTime(it, seed)) }
        } else {
            null
        }
        runOnce(system, seed, profile, crash, keys, extension).also {
            check(it.crashed == crashes) { "$name $profile seed $seed: crash $crash did not happen" }
        }
    }
    val orders = runs * Scenario.orders.size
    return Cell(
        architecture = name,
        profile = profile,
        crashes = crashes,
        keys = keys,
        runs = runs,
        anyViolation = Rate(results.count { it.violates }, runs),
        byViolation = Violation.entries.associateWith { v -> Rate(results.count { v in it.violations }, runs) },
        needsAttentionPerOrder = results.sumOf { it.needsAttention }.toDouble() / orders,
        acceptedPerRun = results.sumOf { it.accepted }.toDouble() / runs,
        completedPerRun = results.sumOf { it.completed }.toDouble() / runs,
        chargeRequestsPerOrder = results.sumOf { it.chargeRequests }.toDouble() / orders,
        statusRequestsPerOrder = results.sumOf { it.statusRequests }.toDouble() / orders,
        crashedRuns = results.count { it.crashed },
        busyMillisMean = if (busy.isEmpty()) 0.0 else busy.average(),
        crashMillisMean = results.mapNotNull { it.crashMillis }.let { if (it.isEmpty()) 0.0 else it.average() },
        crashesBeforeAcceptance = results.count { it.acceptedAtCrash == 0 },
        byChangeViolation = extension?.oracles.orEmpty().associateWith { v -> Rate(results.count { v in it.changeViolations }, runs) },
        anyChangeViolation = Rate(results.count { it.changeViolations.isNotEmpty() }, runs),
        countsPerRun = results.flatMap { it.counts.keys }.distinct().associateWith { k -> results.sumOf { it.counts[k] ?: 0 }.toDouble() / runs },
    )
}

/** The outcome of crashing each of [seeds] seeds once before every task of its crash-free run. */
data class Sweep(
    val architecture: String,
    val profile: FaultProfile,
    val keys: Boolean,
    val seeds: Int,
    /** Single-crash runs: one per seed and task index. */
    val points: Long,
    /** Runs with at least one violation. */
    val violating: Long,
    val seedsWithViolation: Int,
    val byViolation: Map<Violation, Long>,
    /** Points that violated each of a change request's own requirements (empty in the base scenario). */
    val byChangeViolation: Map<String, Long> = emptyMap(),
    /** Points that violated at least one of a change request's own requirements. */
    val changeViolating: Long = 0,
)

/**
 * Exhaustive single-crash sweep: for each of the first [seeds] crash-cell seeds,
 * one run per task index k of the seed's crash-free run, crashing before task k.
 * It covers every state a crash cell can crash in, the tail included, and also
 * the points between tasks of the same instant, which a crash time never selects.
 */
fun crashSweep(name: String, system: () -> CheckoutSystem, profile: FaultProfile, seeds: Int, keys: Boolean = false, extension: Extension? = null): Sweep {
    var points = 0L
    var violating = 0L
    var seedsWithViolation = 0
    var changeViolating = 0L
    val by = Violation.entries.associateWith { 0L }.toMutableMap()
    val byChange = extension?.oracles.orEmpty().associateWith { 0L }.toMutableMap()
    for (i in 0 until seeds) {
        val seed = seedOf(i, profile, crashes = true)
        val length = trace(system, seed, profile, keys, extension).taskMillis.size
        var seedViolates = false
        for (k in 0 until length) {
            val r = runOnce(system, seed, profile, Crash.BeforeTask(k.toLong()), keys, extension)
            check(r.crashed) { "$name $profile seed $seed: crash point $k not reached" }
            points++
            if (r.violates) {
                violating++
                seedViolates = true
                for (v in r.violations) by[v] = by.getValue(v) + 1
                for (v in r.changeViolations) byChange[v] = byChange.getValue(v) + 1
                if (r.changeViolations.isNotEmpty()) changeViolating++
            }
        }
        if (seedViolates) seedsWithViolation++
    }
    return Sweep(name, profile, keys, seeds, points, violating, seedsWithViolation, by, byChange, changeViolating)
}
