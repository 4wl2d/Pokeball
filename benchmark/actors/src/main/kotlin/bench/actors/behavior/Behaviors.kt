package bench.actors.behavior

import bench.actors.protocol.CheckoutState.Stage
import bench.actors.kit.ActorContext
import bench.actors.kit.ActorRef
import bench.actors.kit.ActorSystem
import bench.actors.protocol.CheckoutCommand
import bench.actors.protocol.CheckoutState
import bench.actors.protocol.InventoryCommand
import bench.actors.protocol.InventoryState
import bench.actors.protocol.PaymentCommand
import bench.env.ChargeResult
import bench.env.ProviderClient
import bench.env.SimDb
import kotlinx.coroutines.CompletableDeferred

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
            }
        }
    }
}

/** Calls the provider for each charge; the asker's timeout bounds the wait. */
fun ActorSystem.payments(client: ProviderClient): ActorRef<PaymentCommand> = spawn { command ->
    when (command) {
        is PaymentCommand.Charge -> run {
            runCatching { client.charge(command.orderId, command.amount) }
                .fold({ command.replyTo.complete(it) }, { command.replyTo.completeExceptionally(it) })
        }
    }
}

/** One actor per checkout. On recovery it is re-created from its durable state and re-driven. */
fun ActorSystem.checkout(
    store: DurableState<CheckoutState>,
    initial: CheckoutState,
    inventory: ActorRef<InventoryCommand>,
    payments: ActorRef<PaymentCommand>,
    askTimeoutMillis: Long = 1_000,
): ActorRef<CheckoutCommand> {
    var state = store.load() ?: store.persist(initial)
    return spawn { command ->
        fun ActorContext<CheckoutCommand>.drive() = when (state.stage) {
            Stage.Reserving -> ask(inventory, askTimeoutMillis, { reply: CompletableDeferred<Boolean> ->
                InventoryCommand.Reserve(state.order.id, state.order.sku, state.order.quantity, reply)
            }) { CheckoutCommand.Reserved(it) }
            Stage.Paying -> ask(payments, askTimeoutMillis, { reply: CompletableDeferred<ChargeResult> ->
                PaymentCommand.Charge(state.order.id, state.order.amount, reply)
            }) { CheckoutCommand.Charged(it) }
            Stage.Completed, Stage.Failed -> Unit
        }
        when (command) {
            CheckoutCommand.Drive -> drive()
            is CheckoutCommand.Reserved -> if (state.stage == Stage.Reserving) {
                when (command.result.getOrNull()) {
                    true -> {
                        state = store.persist(state.copy(stage = Stage.Paying))
                        drive()
                    }
                    false -> state = store.persist(state.copy(stage = Stage.Failed, reason = "out of stock"))
                    null -> drive() // the inventory did not answer in time: ask again
                }
            }
            is CheckoutCommand.Charged -> if (state.stage == Stage.Paying) {
                val charged = command.result.getOrNull()
                state = if (charged is ChargeResult.Charged) {
                    store.persist(state.copy(stage = Stage.Completed))
                } else {
                    inventory.tell(InventoryCommand.Release(state.order.id))
                    store.persist(state.copy(stage = Stage.Failed, reason = "payment failed"))
                }
            }
        }
    }
}
