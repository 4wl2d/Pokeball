package bench.changes

import bench.actors.app.ActorsSystem
import bench.ballarch.app.BallarchSystem
import bench.careful.actors.app.CarefulActorsSystem
import bench.careful.mvu.app.CarefulMvuSystem
import bench.careful.saga.app.CarefulSagaSystem
import bench.env.CheckoutSystem
import bench.env.FaultProfile
import bench.hardened.app.HardenedSystem
import bench.harness.Cell
import bench.harness.Rate
import bench.harness.Sweep
import bench.harness.crashSweep
import bench.harness.experiment
import bench.layered.app.LayeredSystem
import bench.mvu.app.MvuSystem
import bench.saga.app.SagaSystem
import java.io.File

/** The scenario-A designs, by the key of their module; a patch of benchmark/changes changes one of them. */
val designs: List<Pair<String, () -> CheckoutSystem>> = listOf(
    "layered" to ::LayeredSystem,
    "hardened" to { HardenedSystem() },
    "actors" to ::ActorsSystem,
    "actors-careful" to ::CarefulActorsSystem,
    "mvu" to ::MvuSystem,
    "mvu-careful" to { CarefulMvuSystem() },
    "saga" to ::SagaSystem,
    "saga-careful" to ::CarefulSagaSystem,
    "ballarch" to { BallarchSystem() },
)

/**
 * Runs every design under each change request and writes the results as JSON:
 * every profile with and without a crash, and the exhaustive single-crash
 * sweep under Stress and Mild. The designs are whatever this build compiled:
 * unpatched, or patched by benchmark/changes/verify.py.
 * Usage: run --args="<output file> <runs per cell> <sweep seeds> [change request or design ...]";
 * without a change request, all of them run, and without a design, all of them.
 */
fun main(args: Array<String>) {
    val out = File(args[0]).absoluteFile
    val runs = args[1].toInt()
    val sweepSeeds = args[2].toInt()
    val filters = args.drop(3)
    val crs = changeRequests.filter { it.key in filters }.ifEmpty { changeRequests }
    val chosen = designs.filter { it.first in filters }.ifEmpty { designs }
    val results = LinkedHashMap<String, Any?>()
    for (cr in crs) {
        val byDesign = LinkedHashMap<String, Any?>()
        for ((key, system) in chosen) {
            val cells = FaultProfile.entries.flatMap { profile ->
                listOf(false, true).map { crashes -> experiment(key, system, profile, crashes, runs, extension = cr.extension()) }
            }
            val sweeps = listOf(FaultProfile.Stress, FaultProfile.Mild).map { crashSweep(key, system, it, sweepSeeds, extension = cr.extension()) }
            for (c in cells) println("%-17s %-15s %-13s crash=%-5s any violation %5.1f%%".format(cr.key, key, c.profile, c.crashes, 100 * c.anyViolation.value))
            for (s in sweeps) println("%-17s %-15s sweep %-6s %d of %d points violate".format(cr.key, key, s.profile, s.violating, s.points))
            byDesign[key] = mapOf("cells" to cells.map(::cell), "sweeps" to sweeps.map(::sweep))
        }
        results[cr.key] = byDesign
    }
    out.parentFile.mkdirs()
    out.writeText(json(mapOf("runsPerCell" to runs, "sweepSeeds" to sweepSeeds, "results" to results)) + "\n")
    println("wrote $out")
}

private fun hits(r: Rate) = r.hits

private fun cell(c: Cell) = mapOf(
    "profile" to c.profile.name,
    "crashes" to c.crashes,
    "runs" to c.runs,
    "crashedRuns" to c.crashedRuns,
    "violating" to hits(c.anyViolation),
    "violations" to c.byViolation.mapKeys { it.key.name }.mapValues { hits(it.value) }.filterValues { it > 0 },
    "changeViolating" to hits(c.anyChangeViolation),
    "changeViolations" to c.byChangeViolation.mapValues { hits(it.value) }.filterValues { it > 0 },
    "completedPerRun" to c.completedPerRun,
    "needsAttentionPerOrder" to c.needsAttentionPerOrder,
    "chargeRequestsPerOrder" to c.chargeRequestsPerOrder,
    "statusRequestsPerOrder" to c.statusRequestsPerOrder,
    "countsPerRun" to c.countsPerRun,
    "busyMillisMean" to c.busyMillisMean,
)

private fun sweep(s: Sweep) = mapOf(
    "profile" to s.profile.name,
    "seeds" to s.seeds,
    "points" to s.points,
    "violating" to s.violating,
    "violations" to s.byViolation.mapKeys { it.key.name }.filterValues { it > 0 },
    "changeViolating" to s.changeViolating,
    "changeViolations" to s.byChangeViolation.filterValues { it > 0 },
)

private fun json(value: Any?, indent: String = ""): String = when (value) {
    null -> "null"
    is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    is Number, is Boolean -> value.toString()
    is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(",\n", "{\n", "\n$indent}") { "$indent  ${json(it.key.toString())}: ${json(it.value, "$indent  ")}" }
    is Iterable<*> -> if (!value.iterator().hasNext()) "[]" else value.joinToString(",\n", "[\n", "\n$indent]") { "$indent  ${json(it, "$indent  ")}" }
    else -> json(value.toString(), indent)
}
