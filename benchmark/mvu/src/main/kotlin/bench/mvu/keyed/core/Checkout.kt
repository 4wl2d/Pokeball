package bench.mvu.keyed.core

import bench.env.ChargeResult
import bench.env.Order

/** The whole application state, one store (as in Redux). */
data class Model(val stock: Int, val orders: Map<String, OrderModel> = emptyMap())

/** [charges] counts the charge requests issued; each carries the same idempotency key. */
data class OrderModel(val order: Order, val stage: Stage, val reference: String? = null, val charges: Int = 1) {
    enum class Stage { Paying, Completed, Failed }
}

const val MAX_CHARGES = 3

sealed interface Msg {
    data class Place(val order: Order) : Msg
    data class ChargeCompleted(val orderId: String, val result: Result<ChargeResult>) : Msg
}

sealed interface Cmd {
    data class Charge(val orderId: String, val amount: Long) : Cmd
}

/**
 * Starts from the persisted model and re-issues the commands of orders that
 * were being paid, each with a new budget of requests, as a restarted handler
 * would have.
 */
fun init(saved: Model?, stock: Int): Pair<Model, List<Cmd>> {
    val model = saved ?: Model(stock)
    val paying = model.orders.values.filter { it.stage == OrderModel.Stage.Paying }
    val resumed = model.copy(orders = model.orders + paying.associate { it.order.id to it.copy(charges = 1) })
    return resumed to paying.map { Cmd.Charge(it.order.id, it.order.amount) }
}

/** Pure: the next model and the commands to perform. */
fun update(model: Model, msg: Msg): Pair<Model, List<Cmd>> = when (msg) {
    is Msg.Place -> when {
        msg.order.id in model.orders -> model to emptyList()
        model.stock < msg.order.quantity ->
            model.copy(orders = model.orders + (msg.order.id to OrderModel(msg.order, OrderModel.Stage.Failed))) to emptyList()
        else -> model.copy(
            stock = model.stock - msg.order.quantity,
            orders = model.orders + (msg.order.id to OrderModel(msg.order, OrderModel.Stage.Paying)),
        ) to listOf(Cmd.Charge(msg.order.id, msg.order.amount))
    }
    is Msg.ChargeCompleted -> {
        val current = model.orders[msg.orderId]
        if (current == null || current.stage != OrderModel.Stage.Paying) {
            model to emptyList()
        } else {
            val charged = msg.result.getOrNull() as? ChargeResult.Charged
            if (charged != null) {
                model.copy(orders = model.orders + (msg.orderId to current.copy(stage = OrderModel.Stage.Completed, reference = charged.reference))) to emptyList()
            } else if (msg.result.isFailure && current.charges < MAX_CHARGES) {
                // No answer: issue the charge again, with the same key.
                model.copy(orders = model.orders + (msg.orderId to current.copy(charges = current.charges + 1))) to
                    listOf(Cmd.Charge(current.order.id, current.order.amount))
            } else {
                model.copy(
                    stock = model.stock + current.order.quantity,
                    orders = model.orders + (msg.orderId to current.copy(stage = OrderModel.Stage.Failed)),
                ) to emptyList()
            }
        }
    }
}
