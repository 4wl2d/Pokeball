package bench.env

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The provider's answer to a charge. */
sealed interface ChargeResult {
    data class Charged(val reference: String) : ChargeResult
    data object Declined : ChargeResult
}

/** The provider's record of an order's charge. */
sealed interface ChargeStatus {
    data class Charged(val reference: String) : ChargeStatus
    data object NoCharge : ChargeStatus
}

/** The provider's answer to a refund (change request CR1, benchmark/changes). */
sealed interface RefundResult {
    data class Refunded(val reference: String) : RefundResult

    /** Nothing charged for the order is left to refund; the request had no effect. */
    data object NothingToRefund : RefundResult
}

/** The connection was refused before the request was written: nothing reached the provider. */
class ConnectionRefused : IOException("connection refused")

/** The connection failed after the request was written: the provider may have processed it. */
class ConnectionReset : IOException("connection reset")

/** What one attempt of a call ends as, seen by the client library. */
sealed interface Reply<out T> {
    data class Answer<T>(val value: T) : Reply<T>
    data class Failure(val error: IOException) : Reply<Nothing>
}

/**
 * The fate of one request on the network and at the provider. Every fate but
 * [ArriveAfterHorizon] takes effect within [Provider.ARRIVE] of the send or
 * never, so the declared delivery horizon ([Scenario.PROVIDER_HORIZON_MILLIS])
 * holds; [ArriveAfterHorizon] breaks it and occurs only in [FaultProfile.BeyondHorizon].
 */
enum class Fate { Reply, ReplyLate, LoseRequest, LoseReply, ErrorAfterApply, RefuseConnection, ArriveAfterHorizon }

/**
 * The external payment provider. It records the truth that the oracles
 * compare with what each system believes.
 *
 * In scenario A ([World.idempotencyKeys] false) it has no idempotency keys:
 * every charge request that reaches it charges again, and a key sent with it
 * is ignored. In scenario B it honours them, as payment APIs with an
 * `Idempotency-Key` header do: a charge whose key already took effect is not
 * charged again, and the answer repeats the first charge's result. A request
 * without a key charges again in both scenarios.
 *
 * [World.secondaryProvider] is a second instance, used only by change request
 * CR3; its charge references start with [prefix].
 */
class Provider(world: World, private val prefix: String = "ch") {
    private val link = Link(world)
    private val keyed = world.idempotencyKeys

    /** Ground truth: charges applied per order. */
    val charges = LinkedHashMap<String, Int>()

    /** Ground truth: refunds applied per order (CR1). */
    val refunds = LinkedHashMap<String, Int>()

    /** Scenario B: the result of the charge each idempotency key took effect with. */
    private val byKey = HashMap<String, ChargeResult>()

    /** Charge, status and refund requests sent, per order. */
    val chargeRequests = LinkedHashMap<String, Int>()
    val statusRequests = LinkedHashMap<String, Int>()
    val refundRequests = LinkedHashMap<String, Int>()

    /** One charge attempt without an idempotency key. [done] is called at most once, possibly never. */
    fun charge(orderId: String, amount: Long, done: (Reply<ChargeResult>) -> Unit) = charge(orderId, amount, null, done)

    /**
     * One charge attempt with an [idempotencyKey], which only scenario B
     * honours. [done] is called at most once, possibly never.
     */
    fun charge(orderId: String, amount: Long, idempotencyKey: String?, done: (Reply<ChargeResult>) -> Unit) {
        chargeRequests.merge(orderId, 1, Int::plus)
        link.send(done) {
            val key = idempotencyKey?.takeIf { keyed }
            key?.let { byKey[it] } ?: run {
                charges.merge(orderId, 1, Int::plus)
                val result = ChargeResult.Charged("$prefix-$orderId-${charges.getValue(orderId)}")
                if (key != null) byKey[key] = result
                result
            }
        }
    }

    /** One status query. It has no effect. */
    fun status(orderId: String, done: (Reply<ChargeStatus>) -> Unit) {
        statusRequests.merge(orderId, 1, Int::plus)
        link.send(done) {
            if ((charges[orderId] ?: 0) > 0) ChargeStatus.Charged("$prefix-$orderId-${charges.getValue(orderId)}") else ChargeStatus.NoCharge
        }
    }

    /**
     * One refund attempt (CR1). The provider never refunds more than it charged
     * for an order, so repeating a refund is safe: once every charge of the
     * order is refunded, the answer is [RefundResult.NothingToRefund].
     */
    fun refund(orderId: String, amount: Long, done: (Reply<RefundResult>) -> Unit) {
        refundRequests.merge(orderId, 1, Int::plus)
        link.send(done) {
            if ((refunds[orderId] ?: 0) < (charges[orderId] ?: 0)) {
                refunds.merge(orderId, 1, Int::plus)
                RefundResult.Refunded("rf-$orderId-${refunds.getValue(orderId)}")
            } else {
                RefundResult.NothingToRefund
            }
        }
    }

    companion object {
        /** Network latency each way. Except under [Fate.ArriveAfterHorizon], a request is processed on arrival or never. */
        const val ARRIVE = 20L
        const val LATE = 5_000L
        const val REFUSE = 5L
    }
}

/**
 * The network between the system and one external service: it draws the fate
 * of each request from the world's random source, in the order the requests
 * are sent, and delivers the request and its answer accordingly.
 */
internal class Link(private val world: World) {
    private fun fate(): Fate {
        // A fate with weight 0 is never drawn and leaves the draws of the others unchanged.
        val weights = Fate.entries.map { world.profile.weight(it) }
        var x = world.random.nextInt(weights.sum())
        for ((i, w) in weights.withIndex()) {
            if (x < w) return Fate.entries[i]
            x -= w
        }
        return Fate.Reply
    }

    fun <T> send(done: (Reply<T>) -> Unit, process: () -> T) {
        val s = world.scheduler
        when (fate()) {
            Fate.Reply -> s.schedule(Provider.ARRIVE) { val v = process(); s.schedule(Provider.ARRIVE) { done(Reply.Answer(v)) } }
            Fate.ReplyLate -> s.schedule(Provider.ARRIVE) { val v = process(); s.schedule(Provider.LATE) { done(Reply.Answer(v)) } }
            Fate.LoseRequest -> Unit
            Fate.LoseReply -> s.schedule(Provider.ARRIVE) { process() }
            Fate.ErrorAfterApply -> s.schedule(Provider.ARRIVE) { process(); s.schedule(Provider.ARRIVE) { done(Reply.Failure(ConnectionReset())) } }
            Fate.RefuseConnection -> s.schedule(Provider.REFUSE) { done(Reply.Failure(ConnectionRefused())) }
            // Held in the network (for example a queued retransmission): it takes effect long after
            // the sender gave up on it, and no answer reaches the sender.
            Fate.ArriveAfterHorizon -> s.schedule(Provider.LATE) { process() }
        }
    }
}

/**
 * The provider's client library for coroutine code: one attempt per call,
 * no retries, no timeout of its own (callers add `withTimeout`). A request
 * whose answer is lost never resumes.
 */
class ProviderClient(private val provider: Provider) {
    suspend fun charge(orderId: String, amount: Long): ChargeResult = charge(orderId, amount, null)

    suspend fun charge(orderId: String, amount: Long, idempotencyKey: String?): ChargeResult =
        call { provider.charge(orderId, amount, idempotencyKey, it) }

    suspend fun status(orderId: String): ChargeStatus = call { provider.status(orderId, it) }

    /** CR1: one refund attempt. */
    suspend fun refund(orderId: String, amount: Long): RefundResult = call { provider.refund(orderId, amount, it) }
}

/** Suspends until [start]'s callback reports an answer (resumes with it) or a failure (throws it); a lost answer never resumes. */
internal suspend fun <T> call(start: ((Reply<T>) -> Unit) -> Unit): T = suspendCancellableCoroutine { cont ->
    start { reply ->
        if (cont.isActive) {
            when (reply) {
                is Reply.Answer -> cont.resume(reply.value)
                is Reply.Failure -> cont.resumeWithException(reply.error)
            }
        }
    }
}
