package bench.careful.mvu.keyed.core

import bench.env.Order

/** The whole application state, one store (as in Redux). */
data class Model(val stock: Int, val orders: Map<String, OrderModel> = emptyMap())

/**
 * [Stage.Paying] is persisted before its charge command runs, so after a
 * restart it means "possibly charged"; [sends] counts the requests made.
 */
data class OrderModel(val order: Order, val stage: Stage, val sends: Int = 0, val queries: Int = 0, val reference: String? = null) {
    enum class Stage { Paying, Reconciling, Completed, Failed, NeedsAttention }
}

/** What one charge request established. */
sealed interface ChargeOutcome {
    data class Charged(val reference: String) : ChargeOutcome
    data object Declined : ChargeOutcome

    /** Evidence that the request never reached the provider (connection refused). */
    data object NotSent : ChargeOutcome

    /** The request may have been processed: a timeout, or an error after it was written. */
    data object Unknown : ChargeOutcome
}

/** What the provider's status endpoint said. */
sealed interface StatusOutcome {
    data class Charged(val reference: String) : StatusOutcome
    data object NoCharge : StatusOutcome
    data object Unavailable : StatusOutcome
}

sealed interface Msg {
    data class Place(val order: Order) : Msg
    data class ChargeCompleted(val orderId: String, val outcome: ChargeOutcome) : Msg
    data class StatusChecked(val orderId: String, val status: StatusOutcome) : Msg
}

sealed interface Cmd {
    data class Charge(val orderId: String, val amount: Long) : Cmd

    /** Waits [afterMillis], then asks the provider's status endpoint. */
    data class CheckStatus(val orderId: String, val afterMillis: Long) : Cmd
}

/** The program's flags: the provider's delivery horizon and the retry budgets. */
data class Config(val horizonMillis: Long, val maxSends: Int = 3, val maxQueries: Int = 3)

/**
 * Starts from the persisted model. An order still paying may have been
 * charged before the restart, so its charge is not issued again: its status is
 * checked once the delivery horizon has passed.
 */
fun init(config: Config, saved: Model?, stock: Int): Pair<Model, List<Cmd>> {
    val model = saved ?: Model(stock)
    val open = model.orders.values.filter { it.stage == OrderModel.Stage.Paying || it.stage == OrderModel.Stage.Reconciling }
    val reconciling = open.fold(model) { m, o -> m.put(if (o.stage == OrderModel.Stage.Paying) o.copy(stage = OrderModel.Stage.Reconciling, queries = 0) else o) }
    return reconciling to open.map { Cmd.CheckStatus(it.order.id, config.horizonMillis) }
}

/** Pure: the next model and the commands to perform. */
fun update(config: Config, model: Model, msg: Msg): Pair<Model, List<Cmd>> = when (msg) {
    is Msg.Place -> when {
        msg.order.id in model.orders -> model to emptyList()
        model.stock < msg.order.quantity -> model.put(OrderModel(msg.order, OrderModel.Stage.Failed)) to emptyList()
        else -> model.copy(stock = model.stock - msg.order.quantity).put(OrderModel(msg.order, OrderModel.Stage.Paying, sends = 1)) to
            listOf(Cmd.Charge(msg.order.id, msg.order.amount))
    }
    is Msg.ChargeCompleted -> model.orders[msg.orderId]?.takeIf { it.stage == OrderModel.Stage.Paying }
        ?.let { charged(config, model, it, msg.outcome) } ?: (model to emptyList())
    is Msg.StatusChecked -> model.orders[msg.orderId]?.takeIf { it.stage == OrderModel.Stage.Reconciling }
        ?.let { checked(config, model, it, msg.status) } ?: (model to emptyList())
}

private fun charged(config: Config, model: Model, order: OrderModel, outcome: ChargeOutcome): Pair<Model, List<Cmd>> = when (outcome) {
    is ChargeOutcome.Charged -> model.put(order.copy(stage = OrderModel.Stage.Completed, reference = outcome.reference)) to emptyList()
    ChargeOutcome.Declined -> model.fail(order) to emptyList()
    // Every charge request carries the same idempotency key, so one that got no answer is sent again.
    ChargeOutcome.NotSent, ChargeOutcome.Unknown ->
        if (order.sends < config.maxSends) {
            model.put(order.copy(sends = order.sends + 1)) to listOf(Cmd.Charge(order.order.id, order.order.amount))
        } else {
            model.put(order.copy(stage = OrderModel.Stage.Reconciling, queries = 0)) to listOf(Cmd.CheckStatus(order.order.id, config.horizonMillis))
        }
}

private fun checked(config: Config, model: Model, order: OrderModel, status: StatusOutcome): Pair<Model, List<Cmd>> = when (status) {
    is StatusOutcome.Charged -> model.put(order.copy(stage = OrderModel.Stage.Completed, reference = status.reference)) to emptyList()
    StatusOutcome.NoCharge -> model.fail(order) to emptyList()
    StatusOutcome.Unavailable ->
        if (order.queries + 1 < config.maxQueries) {
            model.put(order.copy(queries = order.queries + 1)) to listOf(Cmd.CheckStatus(order.order.id, config.horizonMillis))
        } else {
            model.put(order.copy(stage = OrderModel.Stage.NeedsAttention)) to emptyList()
        }
}

private fun Model.put(order: OrderModel) = copy(orders = orders + (order.order.id to order))

private fun Model.fail(order: OrderModel) = copy(stock = stock + order.order.quantity).put(order.copy(stage = OrderModel.Stage.Failed))
