package bench.runner

import bench.actors.app.ActorsSystem
import bench.ballarch.app.BallarchSystem
import bench.careful.actors.app.CarefulActorsSystem
import bench.careful.mvu.app.CarefulMvuSystem
import bench.careful.saga.app.CarefulSagaSystem
import bench.env.CheckoutSystem
import bench.env.Fate
import bench.env.FaultProfile
import bench.env.Scenario
import bench.hardened.app.HardenedSystem
import bench.harness.Cell
import bench.harness.Rate
import bench.harness.crashSweep
import bench.harness.experiment
import bench.layered.app.LayeredSystem
import bench.mvu.app.MvuSystem
import bench.saga.app.SagaSystem
import java.io.File
import bench.actors.keyed.app.ActorsSystem as KeyedActorsSystem
import bench.ballarch.keyed.app.BallarchSystem as KeyedBallarchSystem
import bench.ballarch.refadapter.app.BallarchSystem as RefAdapterBallarchSystem
import bench.careful.actors.keyed.app.CarefulActorsSystem as KeyedCarefulActorsSystem
import bench.careful.mvu.keyed.app.CarefulMvuSystem as KeyedCarefulMvuSystem
import bench.careful.saga.keyed.app.CarefulSagaSystem as KeyedCarefulSagaSystem
import bench.hardened.keyed.app.HardenedSystem as KeyedHardenedSystem
import bench.layered.keyed.app.LayeredSystem as KeyedLayeredSystem
import bench.layered.retry.app.LayeredSystem as RetryLayeredSystem
import bench.mvu.keyed.app.MvuSystem as KeyedMvuSystem
import bench.saga.keyed.app.SagaSystem as KeyedSagaSystem

/**
 * One implementation of the checkout, as measured in scenario A (a provider
 * without idempotency keys). [module] is its Gradle module under benchmark/.
 * [toolkit] packages stand in for a framework and are measured separately;
 * they live in [toolkitModule]. [keyed] is the same design adapted to a
 * provider with idempotency keys (scenario B); its package [keyedPackage] is
 * not part of the scenario-A feature code.
 */
data class Architecture(
    val key: String,
    val label: String,
    val system: () -> CheckoutSystem,
    val keyed: () -> CheckoutSystem,
    val rootPackage: String,
    val module: String = key,
    val toolkit: List<String> = emptyList(),
    val toolkitModule: String = module,
    val keyedPackage: String = "$rootPackage.keyed",
    /** Other packages of the module that are not part of this design's feature code. */
    val otherPackages: List<String> = emptyList(),
) {
    /** Packages excluded from the feature code measured in scenario A. */
    val excluded: List<String> get() = toolkit + keyedPackage + otherPackages
}

val architectures = listOf(
    Architecture("layered", "Layered, naive provider handling", ::LayeredSystem, ::KeyedLayeredSystem, "bench.layered", otherPackages = listOf("bench.layered.retry")),
    Architecture("hardened", "Layered, careful (hardened by hand)", { HardenedSystem() }, { KeyedHardenedSystem() }, "bench.hardened"),
    Architecture("actors", "Actors, naive provider handling", ::ActorsSystem, ::KeyedActorsSystem, "bench.actors", toolkit = listOf("bench.actors.kit")),
    Architecture(
        "actors-careful", "Actors, careful", ::CarefulActorsSystem, ::KeyedCarefulActorsSystem, "bench.careful.actors",
        toolkit = listOf("bench.actors.kit"), toolkitModule = "actors",
    ),
    Architecture("mvu", "MVU, naive provider handling", ::MvuSystem, ::KeyedMvuSystem, "bench.mvu", toolkit = listOf("bench.mvu.kit")),
    Architecture(
        "mvu-careful", "MVU, careful", { CarefulMvuSystem() }, { KeyedCarefulMvuSystem() }, "bench.careful.mvu",
        toolkit = listOf("bench.mvu.kit"), toolkitModule = "mvu",
    ),
    Architecture("saga", "Outbox + saga, naive provider handling", ::SagaSystem, ::KeyedSagaSystem, "bench.saga", toolkit = listOf("bench.saga.kit")),
    Architecture(
        "saga-careful", "Outbox + saga, careful", ::CarefulSagaSystem, ::KeyedCarefulSagaSystem, "bench.careful.saga",
        toolkit = listOf("bench.saga.kit"), toolkitModule = "saga",
    ),
    Architecture("ballarch", "Ballarch", { BallarchSystem() }, { KeyedBallarchSystem() }, "bench.ballarch", otherPackages = listOf("bench.ballarch.refadapter")),
)

/**
 * A design run in scenario B, where the provider honours idempotency keys:
 * [base]'s design changed in package [packageName], whose files mirror the
 * paths of the files of [base] they replace.
 */
data class KeyedDesign(val key: String, val label: String, val system: () -> CheckoutSystem, val base: String, val packageName: String)

val keyedDesigns: List<KeyedDesign> = architectures.flatMap { a ->
    listOf(KeyedDesign(a.key, a.label, a.keyed, a.key, a.keyedPackage)) + when (a.key) {
        // Only in scenario B: the naive layered design whose same-key retries end in NeedsAttention instead of Failed.
        "layered" -> listOf(KeyedDesign("layered-retry", "Layered, same-key retries, then NeedsAttention", ::RetryLayeredSystem, "layered", "bench.layered.retry"))
        // Only in scenario B: Ballarch with an adapter like the reference HTTP adapter, whose attempts end at the call's deadline.
        "ballarch" -> listOf(
            KeyedDesign("ballarch-refadapter", "Ballarch, reference adapter (attempt timeout = call deadline)", { RefAdapterBallarchSystem() }, "ballarch", "bench.ballarch.refadapter"),
        )
        else -> emptyList()
    }
}

/**
 * Runs the benchmark and writes `results.json` to the output directory.
 * Usage: run --args="[runs per cell] [output directory] [seeds per crash sweep]".
 * The crash sweep uses half as many seeds as there are runs per cell unless told otherwise.
 */
fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toInt() ?: 1_000
    val out = File(args.getOrNull(1) ?: System.getProperty("bench.out") ?: "build/bench").also { it.mkdirs() }
    val sweepSeeds = args.getOrNull(2)?.toInt() ?: (runs / 2)
    // Scenario A, then scenario B (the provider honours idempotency keys).
    val scenarios = listOf(false to architectures.map { it.key to it.system }, true to keyedDesigns.map { it.key to it.system })
    val dynamic = ArrayList<Cell>()
    for ((keys, designs) in scenarios) {
        for ((key, system) in designs) {
            for (profile in FaultProfile.entries) {
                for (crashes in listOf(false, true)) {
                    val cell = experiment(key, system, profile, crashes, runs, keys)
                    dynamic += cell
                    println("%-14s keys=%-5s %-13s crash=%-5s any violation %5.1f%%  needs attention/order %.3f".format(key, keys, profile, crashes, 100 * cell.anyViolation.value, cell.needsAttentionPerOrder))
                }
            }
        }
    }
    val sweeps = scenarios.flatMap { (keys, designs) ->
        designs.flatMap { (key, system) ->
            FaultProfile.entries.map { profile ->
                crashSweep(key, system, profile, sweepSeeds, keys).also {
                    println("sweep %-14s keys=%-5s %-13s %d of %d single-crash points violate".format(key, keys, profile, it.violating, it.points))
                }
            }
        }
    }
    // Seeded bugs run only where the correct implementations have no violation, so that a hit means the bug.
    val seededCells = seeded.flatMap { bug ->
        listOf(FaultProfile.Stress, FaultProfile.Mild).flatMap { profile ->
            listOf(false, true).map { crashes ->
                experiment(bug.name, bug.system, profile, crashes, runs).also {
                    println("seeded bug %-62s %-6s crash=%-5s any violation %5.1f%%".format(bug.name, profile, crashes, 100 * it.anyViolation.value))
                }
            }
        }
    }
    fun classes(module: String) = File(System.getProperty("bench.classes.$module") ?: error("system property bench.classes.$module is not set"))
    val structure = architectures.associate { a -> a.key to analyze(classes(a.module), a.rootPackage, a.excluded) }
    val toolkits = architectures.filter { it.toolkit.isNotEmpty() }.associate { a ->
        a.key to a.toolkit.sumOf { pkg -> analyze(classes(a.toolkitModule), pkg, emptyList()).instructions }
    }
    File(out, "results.json").writeText(
        json(
            mapOf(
                "config" to mapOf(
                    "runsPerCell" to runs,
                    "sweepSeeds" to sweepSeeds,
                    "orders" to Scenario.orders.map { it.id },
                    "stock" to Scenario.STOCK,
                    "endMillis" to Scenario.END_MILLIS,
                    "restartMillis" to Scenario.RESTART_MILLIS,
                    "providerHorizonMillis" to Scenario.PROVIDER_HORIZON_MILLIS,
                    "profiles" to FaultProfile.entries.associate { p -> p.name to Fate.entries.associate { it.name to p.weight(it) } },
                ),
                "architectures" to architectures.map {
                    mapOf(
                        "key" to it.key, "label" to it.label, "root" to it.rootPackage, "module" to it.module,
                        "toolkit" to it.toolkit, "toolkitModule" to it.toolkitModule, "excluded" to it.excluded,
                    )
                },
                "keyedDesigns" to keyedDesigns.map {
                    val base = architectures.single { a -> a.key == it.base }
                    mapOf("key" to it.key, "label" to it.label, "base" to it.base, "module" to base.module, "root" to base.rootPackage, "package" to it.packageName)
                },
                "dynamic" to dynamic.map { c ->
                    mapOf(
                        "architecture" to c.architecture,
                        "keys" to c.keys,
                        "profile" to c.profile.name,
                        "crashes" to c.crashes,
                        "runs" to c.runs,
                        "crashedRuns" to c.crashedRuns,
                        "anyViolation" to rate(c.anyViolation),
                        "violations" to c.byViolation.mapKeys { it.key.name }.mapValues { rate(it.value) },
                        "needsAttentionPerOrder" to c.needsAttentionPerOrder,
                        "acceptedPerRun" to c.acceptedPerRun,
                        "completedPerRun" to c.completedPerRun,
                        "chargeRequestsPerOrder" to c.chargeRequestsPerOrder,
                        "statusRequestsPerOrder" to c.statusRequestsPerOrder,
                        "busyMillisMean" to c.busyMillisMean,
                        "crashMillisMean" to c.crashMillisMean,
                        "crashesBeforeAcceptance" to c.crashesBeforeAcceptance,
                    )
                },
                "crashSweep" to sweeps.map { s ->
                    mapOf(
                        "architecture" to s.architecture,
                        "keys" to s.keys,
                        "profile" to s.profile.name,
                        "seeds" to s.seeds,
                        "points" to s.points,
                        "violating" to s.violating,
                        "seedsWithViolation" to s.seedsWithViolation,
                        "violations" to s.byViolation.mapKeys { it.key.name },
                    )
                },
                "seeded" to seededCells.map { c ->
                    mapOf(
                        "name" to c.architecture,
                        "recovery" to seeded.single { it.name == c.architecture }.recovery,
                        "profile" to c.profile.name,
                        "crashes" to c.crashes,
                        "runs" to c.runs,
                        "crashedRuns" to c.crashedRuns,
                        "anyViolation" to rate(c.anyViolation),
                        "violations" to c.byViolation.mapKeys { it.key.name }.mapValues { rate(it.value) },
                    )
                },
                "structure" to structure.mapValues { (_, s) ->
                    val ccn = s.functions.map { it.ccn }
                    mapOf(
                        "units" to s.units,
                        "topLevelClasses" to s.topLevelClasses,
                        "sourceFiles" to s.sourceFiles,
                        "packages" to s.packages,
                        "cboMean" to s.cboMean,
                        "cboMax" to s.cboMax,
                        "lcom4Mean" to s.lcom4Mean,
                        "lcom4CohesiveClasses" to s.lcom4CohesiveClasses,
                        "lcom4Measured" to s.lcom4Measured,
                        "camcMean" to s.camcMean,
                        "camcMeasured" to s.camcMeasured,
                        "distanceMean" to s.distanceMean,
                        "distanceMeanAll" to s.distanceMeanAll,
                        "propagationCost" to s.propagationCost,
                        "propagationCostTopLevel" to s.propagationCostTopLevel,
                        "propagationCostFile" to s.propagationCostFile,
                        "cyclicGroups" to s.cyclicGroups,
                        "unitsInCycles" to s.unitsInCycles,
                        "functions" to ccn.size,
                        "ccnTotal" to ccn.sum(),
                        "ccnMean" to ccn.average(),
                        "ccnMax" to (ccn.maxOrNull() ?: 0),
                        "frameworkTypes" to s.frameworkTypes,
                        "frameworkTypeNames" to s.frameworkTypeNames,
                        "instructions" to s.instructions,
                        "pureShare" to s.pureShare,
                        "packagesDetail" to s.packagesDetail.map {
                            mapOf(
                                "name" to it.name, "ca" to it.ca, "ce" to it.ce, "ceAll" to it.ceAll, "A" to it.abstractness,
                                "I" to it.instability, "IAll" to it.instabilityAll, "D" to it.distance, "DAll" to it.distanceAll,
                            )
                        },
                        "cohesionDetail" to s.cohesion.map { mapOf("class" to it.name, "methods" to it.methods, "lcom4" to it.lcom4, "camc" to it.camc) },
                        "complexityDetail" to s.functions.map { mapOf("function" to it.name, "ccn" to it.ccn) },
                    )
                },
                "toolkitInstructions" to toolkits,
            ),
        ),
    )
    println("wrote ${File(out, "results.json")}")
}

private fun rate(r: Rate) = mapOf("hits" to r.hits, "n" to r.n, "rate" to r.value, "low" to r.interval.first, "high" to r.interval.second)

private fun json(value: Any?): String = when (value) {
    null -> "null"
    is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    is Number, is Boolean -> value.toString()
    is Map<*, *> -> value.entries.joinToString(",\n", "{", "}") { "${json(it.key.toString())}: ${json(it.value)}" }
    is Iterable<*> -> value.joinToString(", ", "[", "]") { json(it) }
    else -> json(value.toString())
}
