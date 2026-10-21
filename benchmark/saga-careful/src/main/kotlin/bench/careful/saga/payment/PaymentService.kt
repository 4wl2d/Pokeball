package bench.careful.saga.payment

import bench.careful.saga.messages.Channels
import bench.careful.saga.messages.PaymentCommand
import bench.careful.saga.messages.SagaReply
import bench.env.ChargeResult
import bench.env.ChargeStatus
import bench.env.ConnectionRefused
import bench.env.ProviderClient
import bench.env.SimDb
import bench.saga.kit.Message
import bench.saga.kit.Outbox
import bench.saga.kit.markProcessed
import bench.saga.kit.processedBefore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * The payment participant. The provider call cannot be part of a database
 * transaction, so before it calls the provider the handler commits a
 * charge-attempted row for the message. A redelivered command whose row exists
 * may already have charged the card: it is never charged again, and the
 * provider's status is asked instead. A charge is repeated only after evidence
 * that it was not sent. The handler then records the message as processed
 * together with its reply.
 */
class PaymentService(
    private val db: SimDb,
    private val client: ProviderClient,
    private val horizonMillis: Long,
    private val timeoutMillis: Long = 1_000,
    private val maxSends: Int = 3,
) {
    suspend fun handle(message: Message) {
        if (db.transaction { it.processedBefore("payment", message) }) return
        val reply = when (val command = message.payload as PaymentCommand) {
            is PaymentCommand.ChargeCard -> charge(message, command)
            is PaymentCommand.CheckPayment -> check(command.orderId)
        }
        db.transaction { tx ->
            tx.markProcessed("payment", message)
            Outbox.send(tx, Channels.SAGA_REPLIES, reply)
        }
    }

    private suspend fun charge(message: Message, command: PaymentCommand.ChargeCard): SagaReply {
        val attempted = "payment/attempted/${message.id}"
        if (db.get(attempted) != null) return check(command.orderId)
        db.transaction { it.put(attempted, command.orderId) }
        repeat(maxSends) {
            try {
                return when (val result = withTimeout(timeoutMillis) { client.charge(command.orderId, command.amount) }) {
                    is ChargeResult.Charged -> SagaReply.PaymentSucceeded(command.orderId, result.reference)
                    ChargeResult.Declined -> SagaReply.PaymentFailed(command.orderId, "declined")
                }
            } catch (e: ConnectionRefused) {
                // Provably not sent: send again.
            } catch (e: TimeoutCancellationException) {
                return SagaReply.PaymentUnknown(command.orderId)
            } catch (e: IOException) {
                return SagaReply.PaymentUnknown(command.orderId)
            }
        }
        return SagaReply.PaymentFailed(command.orderId, "payment provider unreachable")
    }

    /** Waits until no earlier request can still take effect, then asks the provider's status endpoint. */
    private suspend fun check(orderId: String): SagaReply {
        delay(horizonMillis)
        return try {
            when (val status = withTimeout(timeoutMillis) { client.status(orderId) }) {
                is ChargeStatus.Charged -> SagaReply.PaymentSucceeded(orderId, status.reference)
                ChargeStatus.NoCharge -> SagaReply.PaymentFailed(orderId, "not charged")
            }
        } catch (e: TimeoutCancellationException) {
            SagaReply.PaymentUnknown(orderId)
        } catch (e: IOException) {
            SagaReply.PaymentUnknown(orderId)
        }
    }
}
