package bench.saga.kit

import bench.env.SimDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A minimal transactional-messaging toolkit standing in for a framework such
 * as Eventuate Tram: a transactional outbox, a relay that publishes outbox
 * messages at least once, and idempotent consumers.
 */
data class Message(val id: String, val channel: String, val payload: Any)

object Outbox {
    /** Inserts a message in the same transaction as the state change that produces it. */
    fun send(tx: SimDb.Tx, channel: String, payload: Any) {
        val seq = (tx.get<Int>("outbox-seq") ?: 0) + 1
        tx.put("outbox-seq", seq)
        tx.put("outbox/%08d".format(seq), Message("m$seq", channel, payload))
    }
}

/** Idempotent consumer: the message id is recorded in the same transaction as the consumer's changes. */
fun SimDb.Tx.processedBefore(consumer: String, message: Message): Boolean = get<Boolean>("processed/$consumer/${message.id}") == true

fun SimDb.Tx.markProcessed(consumer: String, message: Message) = put("processed/$consumer/${message.id}", true)

/**
 * Publishes outbox messages to their channel's handler, triggered by the
 * database's change stream. A message is deleted (acknowledged) after its
 * handler returns; after a restart, unacknowledged messages are delivered again.
 */
class MessageRelay(
    private val db: SimDb,
    private val scope: CoroutineScope,
    private val execute: (() -> Unit) -> Unit,
    private val handlers: Map<String, suspend (Message) -> Unit>,
) {
    private val inFlight = HashSet<String>()

    fun start() {
        db.onCommit { keys -> if (keys.any { it.startsWith("outbox/") }) execute { poll() } }
        execute { poll() }
    }

    private fun poll() {
        for (key in db.keys("outbox/")) {
            val message = db.get(key) as Message
            if (!inFlight.add(message.id)) continue
            scope.launch {
                handlers.getValue(message.channel)(message)
                db.transaction { it.delete(key) }
            }
        }
    }
}
