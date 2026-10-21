package bench.actors.app

import bench.actors.behavior.DurableState
import bench.actors.behavior.checkout
import bench.actors.behavior.inventory
import bench.actors.behavior.payments
import bench.actors.kit.ActorRef
import bench.actors.kit.ActorSystem
import bench.actors.protocol.CheckoutCommand
import bench.actors.protocol.CheckoutState
import bench.actors.protocol.InventoryCommand
import bench.actors.protocol.InventoryState
import bench.actors.protocol.PaymentCommand
import bench.env.CheckoutSystem
import bench.env.Order
import bench.env.Process
import bench.env.ProviderClient
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World

/** Guardian: spawns the inventory, payment and checkout actors, and recovers checkouts after a restart. */
class ActorsSystem : CheckoutSystem {
    private lateinit var world: World
    private lateinit var process: Process
    private lateinit var system: ActorSystem
    private lateinit var inventory: ActorRef<InventoryCommand>
    private lateinit var payments: ActorRef<PaymentCommand>

    private fun store(orderId: String) = DurableState<CheckoutState>(world.db, "actor/checkout/$orderId")

    override fun boot(world: World) {
        this.world = world
        process = Process(world.scheduler)
        process.execute {
            system = ActorSystem(process.scope)
            inventory = system.inventory(DurableState(world.db, "actor/inventory"), InventoryState(Scenario.STOCK))
            payments = system.payments(ProviderClient(world.provider))
            for (key in world.db.keys("actor/checkout/")) {
                val state = world.db.get(key) as CheckoutState
                system.checkout(store(state.order.id), state, inventory, payments).tell(CheckoutCommand.Drive)
            }
        }
    }

    override fun start(order: Order) {
        process.execute {
            if (store(order.id).load() == null) {
                system.checkout(store(order.id), CheckoutState(order, CheckoutState.Stage.Reserving), inventory, payments)
                    .tell(CheckoutCommand.Drive)
            }
        }
    }

    override fun crash() = process.kill()

    override fun accepted(orderId: String) = store(orderId).load() != null

    override fun outcome(orderId: String): Terminal? = when (store(orderId).load()?.stage) {
        CheckoutState.Stage.Completed -> Terminal.Completed
        CheckoutState.Stage.Failed -> Terminal.Failed
        else -> null
    }

    override fun reserved(): Map<String, Int> = (world.db.get("actor/inventory") as InventoryState?)?.reservations.orEmpty()
}
