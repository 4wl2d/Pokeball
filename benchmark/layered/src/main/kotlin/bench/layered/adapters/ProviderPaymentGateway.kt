package bench.layered.adapters

import bench.env.ChargeResult
import bench.env.ProviderClient
import bench.layered.application.PaymentFailedException
import bench.layered.application.PaymentGateway
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** Calls the provider once, with a timeout, and maps every failure to [PaymentFailedException]. */
class ProviderPaymentGateway(private val client: ProviderClient, private val timeoutMillis: Long = 1_000) : PaymentGateway {
    override suspend fun charge(orderId: String, amount: Long): String {
        val result = try {
            withTimeout(timeoutMillis) { client.charge(orderId, amount) }
        } catch (e: TimeoutCancellationException) {
            throw PaymentFailedException("payment provider timed out", e)
        } catch (e: IOException) {
            throw PaymentFailedException("payment provider unavailable", e)
        }
        return when (result) {
            is ChargeResult.Charged -> result.reference
            ChargeResult.Declined -> throw PaymentFailedException("declined")
        }
    }
}
