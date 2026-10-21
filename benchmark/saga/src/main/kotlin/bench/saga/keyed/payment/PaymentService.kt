package bench.saga.keyed.payment

import bench.env.ChargeResult
import bench.env.ProviderClient
import bench.env.SimDb
import bench.saga.kit.Message
import bench.saga.kit.Outbox
import bench.saga.kit.markProcessed
import bench.saga.kit.processedBefore
import bench.saga.messages.ChargeCard
import bench.saga.messages.Channels
import bench.saga.messages.SagaReply
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * The payment participant. The provider call cannot be part of a database
 * transaction, so the handler checks for duplicates, calls the provider, and
 * then records the message as processed together with its reply. Every charge
 * request for an order carries the same idempotency key, so a request that got
 * no answer is repeated, up to [attempts] requests.
 */
class PaymentService(private val db: SimDb, private val client: ProviderClient, private val timeoutMillis: Long = 1_000, private val attempts: Int = 3) {
    suspend fun handle(message: Message) {
        if (db.transaction { it.processedBefore("payment", message) }) return
        val command = message.payload as ChargeCard
        var attempt = 1
        var reply: SagaReply
        while (true) {
            reply = try {
                when (val result = withTimeout(timeoutMillis) { client.charge(command.orderId, command.amount, idempotencyKey = "charge-${command.orderId}") }) {
                    is ChargeResult.Charged -> SagaReply.PaymentSucceeded(command.orderId, result.reference)
                    ChargeResult.Declined -> SagaReply.PaymentFailed(command.orderId, "declined")
                }
            } catch (e: TimeoutCancellationException) {
                if (attempt++ < attempts) continue
                SagaReply.PaymentFailed(command.orderId, "provider timeout")
            } catch (e: IOException) {
                if (attempt++ < attempts) continue
                SagaReply.PaymentFailed(command.orderId, "provider unavailable")
            }
            break
        }
        db.transaction { tx ->
            tx.markProcessed("payment", message)
            Outbox.send(tx, Channels.SAGA_REPLIES, reply)
        }
    }
}
