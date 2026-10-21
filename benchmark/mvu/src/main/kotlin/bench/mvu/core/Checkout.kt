package bench.mvu.core

import bench.env.ChargeResult
import bench.env.Order

/** The whole application state, one store (as in Redux). */
data class Model(val stock: Int, val orders: Map<String, OrderModel> = emptyMap())

data class OrderModel(val order: Order, val stage: Stage, val reference: String? = null) {
    enum class Stage { Paying, Completed, Failed }
}

sealed interface Msg {
    data class Place(val order: Order) : Msg
    data class ChargeCompleted(val orderId: String, val result: Result<ChargeResult>) : Msg
}

sealed interface Cmd {
    data class Charge(val orderId: String, val amount: Long) : Cmd
}

/** Starts from the persisted model and re-issues the commands of orders that were being paid. */
fun init(saved: Model?, stock: Int): Pair<Model, List<Cmd>> {
    val model = saved ?: Model(stock)
    val resume = model.orders.values.filter { it.stage == OrderModel.Stage.Paying }.map { Cmd.Charge(it.order.id, it.order.amount) }
    return model to resume
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
            } else {
                model.copy(
                    stock = model.stock + current.order.quantity,
                    orders = model.orders + (msg.orderId to current.copy(stage = OrderModel.Stage.Failed)),
                ) to emptyList()
            }
        }
    }
}
