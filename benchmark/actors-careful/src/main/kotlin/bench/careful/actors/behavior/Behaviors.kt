package bench.careful.actors.behavior

import bench.actors.kit.ActorContext
import bench.actors.kit.ActorRef
import bench.actors.kit.ActorSystem
import bench.careful.actors.protocol.ChargeReply
import bench.careful.actors.protocol.CheckoutCommand
import bench.careful.actors.protocol.CheckoutState
import bench.careful.actors.protocol.CheckoutState.Stage
import bench.careful.actors.protocol.InventoryCommand
import bench.careful.actors.protocol.InventoryState
import bench.careful.actors.protocol.PaymentCommand
import bench.env.ChargeResult
import bench.env.ChargeStatus
import bench.env.ConnectionRefused
import bench.env.ProviderClient
import bench.env.SimDb
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

/** Durable state: an actor persists its new state before it acts on it (like Akka's DurableStateBehavior). */
class DurableState<S : Any>(private val db: SimDb, private val key: String) {
    @Suppress("UNCHECKED_CAST")
    fun load(): S? = db.get(key) as S?

    fun persist(state: S): S = state.also { s -> db.transaction { it.put(key, s) } }
}

fun ActorSystem.inventory(store: DurableState<InventoryState>, initial: InventoryState): ActorRef<InventoryCommand> {
    var state = store.load() ?: store.persist(initial)
    return spawn { command ->
        when (command) {
            is InventoryCommand.Reserve -> {
                val ok = command.orderId in state.reservations || state.stock >= command.quantity
                if (ok && command.orderId !in state.reservations) {
                    state = store.persist(InventoryState(state.stock - command.quantity, state.reservations + (command.orderId to command.quantity)))
                }
                command.replyTo.complete(ok)
            }
            is InventoryCommand.Release -> {
                val quantity = state.reservations[command.orderId]
                if (quantity != null) state = store.persist(InventoryState(state.stock + quantity, state.reservations - command.orderId))
                command.replyTo.complete(Unit)
            }
        }
    }
}

/** Calls the provider and reports what each charge request proves; the asker's timeout bounds the wait. */
fun ActorSystem.payments(client: ProviderClient): ActorRef<PaymentCommand> = spawn { command ->
    when (command) {
        is PaymentCommand.Charge -> run {
            val reply = try {
                when (val result = client.charge(command.orderId, command.amount)) {
                    is ChargeResult.Charged -> ChargeReply.Charged(result.reference)
                    ChargeResult.Declined -> ChargeReply.Declined
                }
            } catch (e: ConnectionRefused) {
                ChargeReply.NotSent
            } catch (e: IOException) {
                ChargeReply.Unknown
            }
            command.replyTo.complete(reply)
        }
        is PaymentCommand.QueryStatus -> run {
            runCatching { client.status(command.orderId) }
                .fold({ command.replyTo.complete(it) }, { command.replyTo.completeExceptionally(it) })
        }
    }
}

/**
 * One actor per checkout. A charge is sent again only after evidence that it
 * was not sent. When its outcome is unknown (the ask timed out, or the
 * connection failed after the request was written), a timer waits past the
 * provider's delivery horizon and the actor asks the provider's status
 * endpoint. After a restart, a checkout that may have been charged is
 * reconciled the same way, never charged again: side effects are not re-run on
 * recovery, as with Akka's durable state.
 */
fun ActorSystem.checkout(
    store: DurableState<CheckoutState>,
    initial: CheckoutState,
    inventory: ActorRef<InventoryCommand>,
    payments: ActorRef<PaymentCommand>,
    horizonMillis: Long,
    askTimeoutMillis: Long = 1_000,
    maxSends: Int = 3,
    maxQueries: Int = 3,
): ActorRef<CheckoutCommand> {
    var state = store.load() ?: store.persist(initial)
    return spawn { command ->
        fun persist(next: CheckoutState) {
            state = store.persist(next)
        }
        fun ActorContext<CheckoutCommand>.reserve() = ask(inventory, askTimeoutMillis, { reply: CompletableDeferred<Boolean> ->
            InventoryCommand.Reserve(state.order.id, state.order.sku, state.order.quantity, reply)
        }) { CheckoutCommand.Reserved(it) }

        // Persisted before the request, so that a restart knows the charge may have been sent.
        fun ActorContext<CheckoutCommand>.charge(sends: Int) {
            persist(state.copy(stage = Stage.Charging, sends = sends))
            ask(payments, askTimeoutMillis, { reply: CompletableDeferred<ChargeReply> ->
                PaymentCommand.Charge(state.order.id, state.order.amount, reply)
            }) { CheckoutCommand.Charged(it.getOrDefault(ChargeReply.Unknown)) }
        }

        fun ActorContext<CheckoutCommand>.reconcile(queries: Int) {
            persist(state.copy(stage = Stage.Reconciling, queries = queries))
            scheduleOnce(horizonMillis, CheckoutCommand.CheckStatus)
        }

        fun ActorContext<CheckoutCommand>.release(reason: String) {
            persist(state.copy(stage = Stage.Releasing, reason = reason))
            ask(inventory, askTimeoutMillis, { reply: CompletableDeferred<Unit> ->
                InventoryCommand.Release(state.order.id, reply)
            }) { CheckoutCommand.Released(it.isSuccess) }
        }

        when (command) {
            CheckoutCommand.Drive -> when (state.stage) {
                Stage.Reserving -> reserve()
                Stage.Charging -> reconcile(0) // possibly sent before the restart
                Stage.Reconciling -> scheduleOnce(horizonMillis, CheckoutCommand.CheckStatus)
                Stage.Releasing -> release(state.reason ?: "payment failed")
                Stage.Completed, Stage.Failed, Stage.NeedsAttention -> Unit
            }
            is CheckoutCommand.Reserved -> if (state.stage == Stage.Reserving) {
                when (command.result.getOrNull()) {
                    true -> charge(1)
                    false -> persist(state.copy(stage = Stage.Failed, reason = "out of stock"))
                    null -> reserve() // the inventory did not answer in time: ask again
                }
            }
            is CheckoutCommand.Charged -> if (state.stage == Stage.Charging) {
                when (val reply = command.reply) {
                    is ChargeReply.Charged -> persist(state.copy(stage = Stage.Completed))
                    ChargeReply.Declined -> release("declined")
                    ChargeReply.NotSent -> if (state.sends < maxSends) charge(state.sends + 1) else release("payment provider unreachable")
                    ChargeReply.Unknown -> reconcile(0)
                }
            }
            CheckoutCommand.CheckStatus -> if (state.stage == Stage.Reconciling) {
                ask(payments, askTimeoutMillis, { reply: CompletableDeferred<ChargeStatus> ->
                    PaymentCommand.QueryStatus(state.order.id, reply)
                }) { CheckoutCommand.StatusChecked(it.getOrNull()) }
            }
            is CheckoutCommand.StatusChecked -> if (state.stage == Stage.Reconciling) {
                when (command.status) {
                    is ChargeStatus.Charged -> persist(state.copy(stage = Stage.Completed))
                    ChargeStatus.NoCharge -> release("not charged")
                    null -> if (state.queries + 1 < maxQueries) reconcile(state.queries + 1) else persist(state.copy(stage = Stage.NeedsAttention))
                }
            }
            is CheckoutCommand.Released -> if (state.stage == Stage.Releasing) {
                if (command.acknowledged) persist(state.copy(stage = Stage.Failed)) else release(state.reason ?: "payment failed")
            }
        }
    }
}
