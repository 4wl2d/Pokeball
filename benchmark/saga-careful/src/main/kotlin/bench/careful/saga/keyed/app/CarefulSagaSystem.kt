package bench.careful.saga.keyed.app

import bench.careful.saga.inventory.InventoryService
import bench.careful.saga.messages.Channels
import bench.careful.saga.messages.SagaState
import bench.careful.saga.order.CreateOrderSaga
import bench.careful.saga.keyed.payment.PaymentService
import bench.env.CheckoutSystem
import bench.env.Order
import bench.env.Process
import bench.env.ProviderClient
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import bench.saga.kit.MessageRelay

/** Application wiring: the saga, its participants and the message relay. */
class CarefulSagaSystem : CheckoutSystem {
    private lateinit var world: World
    private lateinit var process: Process
    private lateinit var saga: CreateOrderSaga

    override fun boot(world: World) {
        this.world = world
        process = Process(world.scheduler)
        saga = CreateOrderSaga(world.db)
        val inventory = InventoryService(world.db, Scenario.STOCK)
        val payment = PaymentService(world.db, ProviderClient(world.provider), Scenario.PROVIDER_HORIZON_MILLIS)
        MessageRelay(
            world.db, process.scope, process::execute,
            mapOf(
                Channels.INVENTORY to { m -> inventory.handle(m) },
                Channels.PAYMENT to { m -> payment.handle(m) },
                Channels.SAGA_REPLIES to { m -> saga.onReply(m) },
            ),
        ).start()
    }

    override fun start(order: Order) = process.execute { saga.start(order) }

    override fun crash() = process.kill()

    private fun state(orderId: String) = world.db.get(CreateOrderSaga.key(orderId)) as SagaState?

    override fun accepted(orderId: String) = state(orderId) != null

    override fun outcome(orderId: String): Terminal? = when (state(orderId)?.outcome) {
        SagaState.Outcome.Approved -> Terminal.Completed
        SagaState.Outcome.Rejected -> Terminal.Failed
        SagaState.Outcome.NeedsAttention -> Terminal.NeedsAttention
        null -> null
    }

    override fun reserved(): Map<String, Int> =
        world.db.keys("inventory/reservation/").associate { it.removePrefix("inventory/reservation/") to world.db.get(it) as Int }
}
