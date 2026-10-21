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

/** The connection was refused before the request was written: nothing reached the provider. */
class ConnectionRefused : IOException("connection refused")

/** The connection failed after the request was written: the provider may have processed it. */
class ConnectionReset : IOException("connection reset")

/** What one attempt of a call ends as, seen by the client library. */
sealed interface Reply<out T> {
    data class Answer<T>(val value: T) : Reply<T>
    data class Failure(val error: IOException) : Reply<Nothing>
}

/** The fate of one request on the network and at the provider. */
enum class Fate { Reply, ReplyLate, LoseRequest, LoseReply, ErrorAfterApply, RefuseConnection }

/**
 * The external payment provider. It has no idempotency keys: every charge
 * request that reaches it charges again. It records the truth that the
 * oracles compare with what each system believes.
 */
class Provider(private val world: World) {
    /** Ground truth: charges applied per order. */
    val charges = LinkedHashMap<String, Int>()

    /** Charge and status requests sent, per order. */
    val chargeRequests = LinkedHashMap<String, Int>()
    val statusRequests = LinkedHashMap<String, Int>()

    private fun fate(): Fate {
        val weights = Fate.entries.map { if (it == Fate.Reply) world.profile.reply else 1 }
        var x = world.random.nextInt(weights.sum())
        for ((i, w) in weights.withIndex()) {
            if (x < w) return Fate.entries[i]
            x -= w
        }
        return Fate.Reply
    }

    /** One charge attempt. [done] is called at most once, possibly never. */
    fun charge(orderId: String, amount: Long, done: (Reply<ChargeResult>) -> Unit) {
        chargeRequests.merge(orderId, 1, Int::plus)
        send(fate(), done) {
            charges.merge(orderId, 1, Int::plus)
            ChargeResult.Charged("ch-$orderId-${charges.getValue(orderId)}")
        }
    }

    /** One status query. It has no effect. */
    fun status(orderId: String, done: (Reply<ChargeStatus>) -> Unit) {
        statusRequests.merge(orderId, 1, Int::plus)
        send(fate(), done) {
            if ((charges[orderId] ?: 0) > 0) ChargeStatus.Charged("ch-$orderId-${charges.getValue(orderId)}") else ChargeStatus.NoCharge
        }
    }

    private fun <T> send(fate: Fate, done: (Reply<T>) -> Unit, process: () -> T) {
        val s = world.scheduler
        when (fate) {
            Fate.Reply -> s.schedule(ARRIVE) { val v = process(); s.schedule(ARRIVE) { done(Reply.Answer(v)) } }
            Fate.ReplyLate -> s.schedule(ARRIVE) { val v = process(); s.schedule(LATE) { done(Reply.Answer(v)) } }
            Fate.LoseRequest -> Unit
            Fate.LoseReply -> s.schedule(ARRIVE) { process() }
            Fate.ErrorAfterApply -> s.schedule(ARRIVE) { process(); s.schedule(ARRIVE) { done(Reply.Failure(ConnectionReset())) } }
            Fate.RefuseConnection -> s.schedule(REFUSE) { done(Reply.Failure(ConnectionRefused())) }
        }
    }

    companion object {
        /** Network latency each way. A request is processed on arrival or never. */
        const val ARRIVE = 20L
        const val LATE = 5_000L
        const val REFUSE = 5L
    }
}

/**
 * The provider's client library for coroutine code: one attempt per call,
 * no retries, no timeout of its own (callers add `withTimeout`). A request
 * whose answer is lost never resumes.
 */
class ProviderClient(private val provider: Provider) {
    suspend fun charge(orderId: String, amount: Long): ChargeResult = call { provider.charge(orderId, amount, it) }

    suspend fun status(orderId: String): ChargeStatus = call { provider.status(orderId, it) }

    private suspend fun <T> call(start: ((Reply<T>) -> Unit) -> Unit): T = suspendCancellableCoroutine { cont ->
        start { reply ->
            if (cont.isActive) {
                when (reply) {
                    is Reply.Answer -> cont.resume(reply.value)
                    is Reply.Failure -> cont.resumeWithException(reply.error)
                }
            }
        }
    }
}
