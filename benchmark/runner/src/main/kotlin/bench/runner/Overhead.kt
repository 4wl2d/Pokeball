package bench.runner

import ballarch.kernel.Accept
import ballarch.kernel.Ball
import ballarch.kernel.BallId
import ballarch.kernel.Decision
import ballarch.kernel.FixedContext
import ballarch.kernel.Outcome
import ballarch.kernel.RequestContext
import ballarch.kernel.accept
import ballarch.runtime.Composition
import ballarch.runtime.FileStore
import ballarch.runtime.MemoryStore
import ballarch.runtime.Runtime
import ballarch.runtime.Store
import ballarch.runtime.ThreadScheduler
import ballarch.testkit.SimScheduler
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/*
 * Overhead of a request through the reference runtime, against a direct call
 * of the same decider. Its numbers depend on the machine, so they are written
 * to their own file (results/overhead.json), which the benchmark's
 * byte-for-byte check does not cover.
 */

/** The smallest useful Ball: a counter that answers its new value. */
private object Counter : Ball<Long, Unit, Long> {
    override val type = "counter"

    override fun initial(key: String) = 0L

    override fun decide(state: Long, request: Unit, ctx: RequestContext): Decision<Long, Long> = accept(state + 1, state + 1)
}

private val counterId = BallId("counter", "c")

/** One way to perform the request; [call] performs it once and returns when its answer is in. */
private class Variant(val key: String, val description: String, val warmup: Int, val iterations: Int, val call: () -> Unit, val close: () -> Unit = {})

private fun direct(): Variant {
    var state = 0L
    var revision = 0L
    return Variant(
        "direct", "the decider called on the caller's thread with a fresh context: the same work without the runtime",
        WARMUP, ITERATIONS,
        call = {
            val decision = Counter.decide(state, Unit, FixedContext(counterId, revision = ++revision))
            state = (decision as Accept).state
        },
    )
}

/** Runtime.request whose tasks run on the caller's thread: the runtime's own work, without a thread hand-off. */
private fun sameThread(): Variant {
    val scheduler = SimScheduler()
    val runtime = Runtime(Composition().ball(Counter), scheduler, MemoryStore())
    runtime.start()
    scheduler.runUntilIdle()
    return Variant(
        "runtimeSameThread", "Runtime.request with an in-memory store; the test kit's SimScheduler runs its tasks on the caller's thread (no hand-off)",
        WARMUP, ITERATIONS,
        call = {
            var done = false
            runtime.request<Unit, Long>(counterId, Unit, timeoutMillis = 1_000) { check(it is Outcome.Done); done = true }
            scheduler.runUntilIdle()
            check(done)
        },
    )
}

/** Runtime.request on the production scheduler, a dedicated thread; the caller waits for the answer. */
private fun threaded(key: String, description: String, store: Store, warmup: Int, iterations: Int, cleanup: () -> Unit = {}): Variant {
    val scheduler = ThreadScheduler()
    val runtime = Runtime(Composition().ball(Counter), scheduler, store)
    runtime.start()
    return Variant(
        key, description, warmup, iterations,
        call = {
            val answer = CompletableFuture<Outcome<Long>>()
            runtime.request<Unit, Long>(counterId, Unit, timeoutMillis = 10_000) { answer.complete(it) }
            check(answer.get(10, TimeUnit.SECONDS) is Outcome.Done)
        },
        close = {
            scheduler.close()
            cleanup()
        },
    )
}

private fun allocatedBytes(): Long =
    (ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean).getThreadAllocatedBytes(Thread.currentThread().id)

/** One round of [v]: its warm-up, then [Variant.iterations] timed requests. */
private fun measure(v: Variant): Map<String, Long> {
    repeat(v.warmup) { v.call() }
    val samples = LongArray(v.iterations)
    val bytesBefore = allocatedBytes()
    val start = System.nanoTime()
    for (i in 0 until v.iterations) {
        val t0 = System.nanoTime()
        v.call()
        samples[i] = System.nanoTime() - t0
    }
    val total = System.nanoTime() - start
    val bytes = allocatedBytes() - bytesBefore
    v.close()
    samples.sort()
    return linkedMapOf(
        "medianNanos" to samples[samples.size / 2],
        "p99Nanos" to samples[(samples.size * 99) / 100],
        "meanNanos" to total / v.iterations,
        "callerThreadBytesPerRequest" to bytes / v.iterations,
    )
}

/**
 * Measures a direct call against a request through the reference runtime and
 * writes overhead.json. Every variant is measured in [ROUNDS] rounds (or the
 * second argument), each with a fresh runtime and its own warm-up; the rounds
 * take the variants in turn, so that a change in the machine's load during the
 * run reaches every variant. Each statistic is reported for every round and as
 * the median of the rounds.
 * Usage: overhead --args="[output file] [rounds]"; the output file is, by
 * default, the published one, ../../benchmark/results/overhead.json (the task
 * runs in reference/kotlin).
 */
fun main(args: Array<String>) {
    val out = File(args.getOrNull(0) ?: "../../benchmark/results/overhead.json").absoluteFile
    val rounds = args.getOrNull(1)?.toInt() ?: ROUNDS
    // Each variant is set up just before it is measured, so that no other runtime's thread is alive meanwhile.
    val variants: List<() -> Variant> = listOf(
        ::direct,
        ::sameThread,
        {
            threaded(
                "runtimeThread", "Runtime.request with an in-memory store on ThreadScheduler, the production scheduler; the caller waits for the answer",
                MemoryStore(), WARMUP, ITERATIONS,
            )
        },
        {
            val directory = Files.createTempDirectory("ballarch-overhead")
            threaded(
                "runtimeThreadFileStore", "as runtimeThread, with the durable FileStore, which forces every commit to the device, in a temporary directory",
                FileStore(directory), FILE_WARMUP, FILE_ITERATIONS, cleanup = { directory.toFile().deleteRecursively() },
            )
        },
    )
    val measured = linkedMapOf<String, Pair<Variant, MutableList<Map<String, Long>>>>()
    for (round in 1..rounds) {
        for (setup in variants) {
            val v = setup()
            val r = measure(v)
            measured.getOrPut(v.key) { v to mutableListOf() }.second.add(r)
            println("round %d  %-24s median %8d ns   p99 %9d ns".format(round, v.key, r["medianNanos"], r["p99Nanos"]))
        }
    }
    val results = linkedMapOf<String, Any>()
    for ((key, entry) in measured) {
        val (v, rs) = entry
        val summary = linkedMapOf<String, Any>("description" to v.description, "warmupIterations" to v.warmup, "iterations" to v.iterations)
        for (stat in rs.first().keys) summary[stat] = rs.map { it.getValue(stat) }.sorted()[rs.size / 2]
        summary["rounds"] = rs
        results[key] = summary
    }
    val p = System.getProperties()
    val report = linkedMapOf(
        "note" to "Machine-dependent; not part of the byte-for-byte check of results.json. Each statistic is the median of the rounds; " +
            "rounds lists every round. Regenerate with ./gradlew :benchmark:runner:overhead (it writes this file).",
        "measuredAt" to Instant.now().toString(),
        "jvm" to "${p["java.vm.name"]} ${p["java.vm.version"]} (${p["java.vendor"]})",
        "os" to "${p["os.name"]} ${p["os.version"]} ${p["os.arch"]}",
        "availableProcessors" to java.lang.Runtime.getRuntime().availableProcessors(),
        "rounds" to rounds,
        "variants" to results,
    )
    out.parentFile.mkdirs()
    out.writeText(jsonValue(report) + "\n")
    println("wrote $out")
}

private const val ROUNDS = 3
private const val WARMUP = 20_000
private const val ITERATIONS = 100_000
private const val FILE_WARMUP = 200
private const val FILE_ITERATIONS = 2_000

private fun jsonValue(value: Any?, indent: String = ""): String = when (value) {
    null -> "null"
    is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    is Number, is Boolean -> value.toString()
    is Map<*, *> -> value.entries.joinToString(",\n", "{\n", "\n$indent}") { "$indent  ${jsonValue(it.key.toString())}: ${jsonValue(it.value, "$indent  ")}" }
    is List<*> -> value.joinToString(",\n", "[\n", "\n$indent]") { "$indent  ${jsonValue(it, "$indent  ")}" }
    else -> jsonValue(value.toString())
}
