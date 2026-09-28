package pokeball.testkit

import pokeball.kernel.AdapterPort
import pokeball.kernel.BallId
import pokeball.kernel.CallId
import pokeball.kernel.CallKey
import pokeball.kernel.EffectClass
import pokeball.runtime.Adapter
import pokeball.runtime.AdapterResult
import pokeball.runtime.Invocation
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** How a controllable test destination treats the next request. */
public enum class DestinationFate {
    /** Apply and answer normally. */
    Answer,

    /** Apply, then answer only after the adapter's deadline. */
    AnswerLate,

    /** Apply, then drop the connection without an answer. */
    ApplyThenDisconnect,

    /** Apply, then answer with a server error. */
    ApplyThenServerError,

    /** Refuse without applying (for example a validation error). */
    RejectWithoutApplying,

    /** Be unreachable: the connection is refused before anything is written. */
    Unreachable,
}

/**
 * A destination whose behaviour the conformance kit controls and whose ground
 * truth it can read. Implementations wrap a real server (for example an HTTP
 * server in a test) or a fake.
 */
public interface ControllableDestination {
    public fun behave(fate: DestinationFate)

    /** Number of times the request identified by [key] reached the destination. */
    public fun received(key: String): Int

    /** Number of times the effect of [key] was applied. */
    public fun applied(key: String): Int

    /** The idempotency key the destination saw for [key], if the adapter sent one. */
    public fun idempotencyKeySeen(key: String): String?
}

/**
 * Checks an adapter against the adapter contract of Core 2 (rules R7, R8 and
 * specification §9.3) by driving it once per [DestinationFate]:
 *
 * - C1: at most one report per invocation;
 * - C2: no internal retry (at most one delivery per invocation);
 * - C3: `NotApplied`/`Refused` only when nothing was applied;
 * - C4: `Ok` only when the destination processed the request;
 * - C5: for Idempotent ports, the call identity is sent as idempotency key.
 *
 * It waits in real time, so it can test adapters that use real I/O and threads.
 * Every invocation uses a fresh call identity, so one destination can serve
 * several checks without their counts mixing.
 */
public object AdapterConformance {
    private val runs = AtomicLong()

    public data class Report(val fate: DestinationFate, val results: List<String>, val violations: List<String>)

    public fun <Req, Res> check(
        port: AdapterPort<Req, Res>,
        adapter: Adapter<Req, Res>,
        destination: ControllableDestination,
        request: (key: String) -> Req,
        /** How the destination identifies a request (usually from the request payload or idempotency key). */
        keyOf: (CallId) -> String = { it.toString() },
        deadlineMillis: Long = 300,
        settleMillis: Long = 600,
        fates: List<DestinationFate> = DestinationFate.entries,
    ): List<Report> {
        val run = runs.incrementAndGet()
        return fates.map { fate -> checkOne(port, adapter, destination, request, keyOf, deadlineMillis, settleMillis, fate, run) }
    }

    private fun <Req, Res> checkOne(
        port: AdapterPort<Req, Res>,
        adapter: Adapter<Req, Res>,
        destination: ControllableDestination,
        request: (key: String) -> Req,
        keyOf: (CallId) -> String,
        deadlineMillis: Long,
        settleMillis: Long,
        fate: DestinationFate,
        run: Long,
    ): Report {
        destination.behave(fate)
        val callId = CallId(BallId("conformance", "c"), CallKey("$run-${fate.name}"))
        val key = keyOf(callId)
        val reports = CopyOnWriteArrayList<AdapterResult<Res>>()
        val first = CountDownLatch(1)
        val invocation = Invocation(callId, request(key), attempt = 1, deadlineAt = System.currentTimeMillis() + deadlineMillis)
        try {
            adapter.execute(invocation) { r ->
                reports += r
                first.countDown()
            }
        } catch (e: Exception) {
            reports += AdapterResult.Failed("adapter threw ${e::class.simpleName}")
            first.countDown()
        }
        first.await(deadlineMillis + settleMillis, TimeUnit.MILLISECONDS)
        Thread.sleep(settleMillis) // late reports and late deliveries
        val v = ArrayList<String>()
        val received = destination.received(key)
        val applied = destination.applied(key)
        if (reports.size > 1) v += "C1: ${reports.size} reports for one invocation: $reports"
        if (received > 1) v += "C2: request reached the destination $received times (adapter retried)"
        for (r in reports) {
            when (r) {
                is AdapterResult.NotApplied, is AdapterResult.Refused ->
                    if (applied > 0) v += "C3: reported ${r::class.simpleName} but the effect was applied $applied time(s)"
                is AdapterResult.Ok -> if (received == 0) v += "C4: reported Ok but the destination never received the request"
                is AdapterResult.Failed -> Unit
            }
        }
        if (port.effect == EffectClass.Idempotent && received > 0 && destination.idempotencyKeySeen(key) != callId.toString()) {
            v += "C5: idempotency key ${destination.idempotencyKeySeen(key)} is not the call identity $callId"
        }
        return Report(fate, reports.map { it::class.simpleName ?: "?" }, v)
    }
}
