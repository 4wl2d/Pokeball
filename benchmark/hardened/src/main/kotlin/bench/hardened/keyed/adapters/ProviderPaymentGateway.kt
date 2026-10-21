package bench.hardened.keyed.adapters

import bench.env.ChargeResult
import bench.env.ChargeStatus
import bench.env.ConnectionRefused
import bench.env.ProviderClient
import bench.hardened.application.ChargeAttempt
import bench.hardened.application.PaymentGateway
import bench.hardened.application.PaymentStatus
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * The provider honours idempotency keys: every charge request for an order
 * carries the same key, so a request that got no answer is repeated, up to
 * [attempts] requests. When they run out, the charge is uncertain and the use
 * case reconciles it.
 */
class ProviderPaymentGateway(private val client: ProviderClient, private val timeoutMillis: Long = 1_000, private val attempts: Int = 3) : PaymentGateway {
    override suspend fun charge(orderId: String, amount: Long): ChargeAttempt {
        repeat(attempts) {
            val attempt = send(orderId, amount)
            if (attempt is ChargeAttempt.Charged || attempt == ChargeAttempt.Declined) return attempt
        }
        return ChargeAttempt.Uncertain
    }

    private suspend fun send(orderId: String, amount: Long): ChargeAttempt = try {
        when (val r = withTimeout(timeoutMillis) { client.charge(orderId, amount, idempotencyKey = "charge-$orderId") }) {
            is ChargeResult.Charged -> ChargeAttempt.Charged(r.reference)
            ChargeResult.Declined -> ChargeAttempt.Declined
        }
    } catch (e: ConnectionRefused) {
        ChargeAttempt.NotSent
    } catch (e: TimeoutCancellationException) {
        ChargeAttempt.Uncertain
    } catch (e: IOException) {
        ChargeAttempt.Uncertain
    }

    override suspend fun status(orderId: String): PaymentStatus = try {
        when (val s = withTimeout(timeoutMillis) { client.status(orderId) }) {
            is ChargeStatus.Charged -> PaymentStatus.Charged(s.reference)
            ChargeStatus.NoCharge -> PaymentStatus.NoCharge
        }
    } catch (e: TimeoutCancellationException) {
        PaymentStatus.Unavailable
    } catch (e: IOException) {
        PaymentStatus.Unavailable
    }
}
