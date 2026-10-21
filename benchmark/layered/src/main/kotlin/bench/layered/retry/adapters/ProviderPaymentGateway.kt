package bench.layered.retry.adapters

import bench.env.ChargeResult
import bench.env.ProviderClient
import bench.layered.retry.application.PaymentFailedException
import bench.layered.retry.application.PaymentGateway
import bench.layered.retry.application.PaymentUncertainException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Calls the provider with a timeout and an idempotency key derived from the
 * order, repeats the request with the same key after a timeout or an I/O
 * error, and maps the last failure to [PaymentUncertainException]: one of the
 * requests may have charged.
 */
class ProviderPaymentGateway(private val client: ProviderClient, private val timeoutMillis: Long = 1_000, private val attempts: Int = 3) : PaymentGateway {
    override suspend fun charge(orderId: String, amount: Long): String {
        var attempt = 1
        while (true) {
            val result = try {
                withTimeout(timeoutMillis) { client.charge(orderId, amount, idempotencyKey = "charge-$orderId") }
            } catch (e: TimeoutCancellationException) {
                if (attempt++ < attempts) continue
                throw PaymentUncertainException("payment provider timed out", e)
            } catch (e: IOException) {
                if (attempt++ < attempts) continue
                throw PaymentUncertainException("payment provider unavailable", e)
            }
            return when (result) {
                is ChargeResult.Charged -> result.reference
                ChargeResult.Declined -> throw PaymentFailedException("declined")
            }
        }
    }
}
