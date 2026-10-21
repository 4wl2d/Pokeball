package bench.runner

import bench.actors.app.ActorsSystem
import bench.ballarch.app.BallarchSystem
import bench.env.CheckoutSystem
import bench.env.FaultProfile
import bench.env.Scenario
import bench.hardened.app.HardenedSystem
import bench.layered.app.LayeredSystem
import bench.mvu.app.MvuSystem
import bench.saga.app.SagaSystem
import java.io.File

/** One implementation of the checkout. [toolkit] packages stand in for a framework and are measured separately. */
data class Architecture(
    val key: String,
    val label: String,
    val system: () -> CheckoutSystem,
    val rootPackage: String,
    val toolkit: List<String> = emptyList(),
)

val architectures = listOf(
    Architecture("layered", "Layered (Clean Architecture)", ::LayeredSystem, "bench.layered"),
    Architecture("hardened", "Layered, hardened by hand", ::HardenedSystem, "bench.hardened"),
    Architecture("actors", "Actors with durable state", ::ActorsSystem, "bench.actors", listOf("bench.actors.kit")),
    Architecture("mvu", "MVU (Elm architecture)", ::MvuSystem, "bench.mvu", listOf("bench.mvu.kit")),
    Architecture("saga", "Hexagonal + outbox + saga", ::SagaSystem, "bench.saga", listOf("bench.saga.kit")),
    Architecture("ballarch", "Ballarch", ::BallarchSystem, "bench.ballarch"),
)

/**
 * Runs the benchmark and writes `results.json` to the output directory.
 * Usage: run --args="[runs per cell] [output directory]".
 */
fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toInt() ?: 1_000
    val out = File(args.getOrNull(1) ?: System.getProperty("bench.out") ?: "build/bench").also { it.mkdirs() }
    val dynamic = ArrayList<Cell>()
    val windows = LinkedHashMap<String, Long>()
    for (a in architectures) {
        for (profile in FaultProfile.entries) {
            val window = typicalTasks(a.system, profile)
            windows["${a.key}/${profile.name}"] = window
            for (crashes in listOf(false, true)) {
                val cell = experiment(a.key, a.system, profile, crashes, runs, window)
                dynamic += cell
                println("%-9s %-6s crash=%-5s any violation %5.1f%%  needs attention/order %.3f".format(a.key, profile, crashes, 100 * cell.anyViolation.value, cell.needsAttentionPerOrder))
            }
        }
    }
    val seededCells = seeded.flatMap { (name, system) ->
        FaultProfile.entries.map { profile ->
            experiment(name, system, profile, crashes = true, runs, typicalTasks(system, profile)).also {
                println("seeded bug %-50s %-6s any violation %5.1f%%".format(name, profile, 100 * it.anyViolation.value))
            }
        }
    }
    val structure = architectures.associate { a ->
        val dir = File(System.getProperty("bench.classes.${a.key}") ?: error("system property bench.classes.${a.key} is not set"))
        a.key to analyze(dir, a.rootPackage, a.toolkit)
    }
    val toolkits = architectures.filter { it.toolkit.isNotEmpty() }.associate { a ->
        val dir = File(System.getProperty("bench.classes.${a.key}")!!)
        a.key to a.toolkit.sumOf { pkg -> analyze(dir, pkg, emptyList()).instructions }
    }
    File(out, "results.json").writeText(
        json(
            mapOf(
                "config" to mapOf(
                    "runsPerCell" to runs,
                    "orders" to Scenario.orders.map { it.id },
                    "stock" to Scenario.STOCK,
                    "endMillis" to Scenario.END_MILLIS,
                    "crashWindows" to windows,
                ),
                "architectures" to architectures.map { mapOf("key" to it.key, "label" to it.label, "root" to it.rootPackage, "toolkit" to it.toolkit) },
                "dynamic" to dynamic.map { c ->
                    mapOf(
                        "architecture" to c.architecture,
                        "profile" to c.profile.name,
                        "crashes" to c.crashes,
                        "runs" to c.runs,
                        "anyViolation" to rate(c.anyViolation),
                        "violations" to c.byViolation.mapKeys { it.key.name }.mapValues { rate(it.value) },
                        "needsAttentionPerOrder" to c.needsAttentionPerOrder,
                        "acceptedPerRun" to c.acceptedPerRun,
                        "completedPerRun" to c.completedPerRun,
                        "chargeRequestsPerOrder" to c.chargeRequestsPerOrder,
                        "statusRequestsPerOrder" to c.statusRequestsPerOrder,
                    )
                },
                "seeded" to seededCells.map { c ->
                    mapOf(
                        "name" to c.architecture,
                        "profile" to c.profile.name,
                        "anyViolation" to rate(c.anyViolation),
                        "violations" to c.byViolation.mapKeys { it.key.name }.mapValues { rate(it.value) },
                    )
                },
                "structure" to structure.mapValues { (_, s) ->
                    mapOf(
                        "units" to s.units,
                        "packages" to s.packages,
                        "cboMean" to s.cboMean,
                        "cboMax" to s.cboMax,
                        "lcom4Mean" to s.lcom4Mean,
                        "lcom4Cohesive" to s.lcom4Cohesive,
                        "lcom4Measured" to s.lcom4Measured,
                        "camcMean" to s.camcMean,
                        "camcMeasured" to s.camcMeasured,
                        "distanceMean" to s.distanceMean,
                        "propagationCost" to s.propagationCost,
                        "cyclicGroups" to s.cyclicGroups,
                        "unitsInCycles" to s.unitsInCycles,
                        "frameworkTypes" to s.frameworkTypes,
                        "frameworkTypeNames" to s.frameworkTypeNames,
                        "instructions" to s.instructions,
                        "pureShare" to s.pureShare,
                        "packagesDetail" to s.packagesDetail.map {
                            mapOf("name" to it.name, "ca" to it.ca, "ce" to it.ce, "A" to it.abstractness, "I" to it.instability, "D" to it.distance)
                        },
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
