package bench.layered.app

import bench.env.CheckoutSystem
import bench.env.Order
import bench.env.Process
import bench.env.ProviderClient
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import bench.layered.adapters.DbInventoryRepository
import bench.layered.adapters.DbOrderRepository
import bench.layered.adapters.ProviderPaymentGateway
import bench.layered.application.CheckoutInteractor
import bench.layered.domain.CheckoutOrder
import kotlinx.coroutines.launch

/** Main component: wires the layers and starts a background job that resumes interrupted orders. */
class LayeredSystem : CheckoutSystem {
    private lateinit var world: World
    private lateinit var process: Process
    private lateinit var interactor: CheckoutInteractor
    private lateinit var orders: DbOrderRepository

    override fun boot(world: World) {
        this.world = world
        process = Process(world.scheduler)
        orders = DbOrderRepository(world.db)
        val inventory = DbInventoryRepository(world.db, mapOf(Scenario.SKU to Scenario.STOCK))
        interactor = CheckoutInteractor(orders, inventory, ProviderPaymentGateway(ProviderClient(world.provider)))
        process.scope.launch { interactor.resumeUnfinished() }
    }

    override fun start(order: Order) = process.execute {
        interactor.place(CheckoutOrder(order.id, order.sku, order.quantity, order.amount))
        process.scope.launch { interactor.process(order.id) }
    }

    override fun crash() = process.kill()

    override fun accepted(orderId: String) = orders.find(orderId) != null

    override fun outcome(orderId: String): Terminal? = when (orders.find(orderId)?.status) {
        CheckoutOrder.Status.COMPLETED -> Terminal.Completed
        CheckoutOrder.Status.FAILED -> Terminal.Failed
        else -> null
    }

    override fun reserved(): Map<String, Int> =
        world.db.keys("reservation/").associate { it.removePrefix("reservation/") to world.db.get(it) as Int }
}
