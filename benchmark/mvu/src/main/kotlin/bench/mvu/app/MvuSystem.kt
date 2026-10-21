package bench.mvu.app

import bench.env.CheckoutSystem
import bench.env.Order
import bench.env.Process
import bench.env.ProviderClient
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import bench.mvu.core.Cmd
import bench.mvu.core.Model
import bench.mvu.core.Msg
import bench.mvu.core.OrderModel
import bench.mvu.core.init
import bench.mvu.core.update
import bench.mvu.kit.Program
import kotlinx.coroutines.withTimeout

/** Wires the program: persistence of the model, and the effects of commands. */
class MvuSystem : CheckoutSystem {
    private lateinit var world: World
    private lateinit var process: Process
    private lateinit var program: Program<Model, Msg, Cmd>

    private val saved: Model? get() = world.db.get("mvu/model") as Model?

    override fun boot(world: World) {
        this.world = world
        process = Process(world.scheduler)
        val client = ProviderClient(world.provider)
        process.execute {
            program = Program(
                scope = process.scope,
                init = init(saved, Scenario.STOCK),
                update = ::update,
                persist = { model -> world.db.transaction { it.put("mvu/model", model) } },
                perform = { cmd ->
                    when (cmd) {
                        is Cmd.Charge -> Msg.ChargeCompleted(cmd.orderId, runCatching { withTimeout(1_000) { client.charge(cmd.orderId, cmd.amount) } })
                    }
                },
            )
        }
    }

    override fun start(order: Order) = process.execute { program.dispatch(Msg.Place(order)) }

    override fun crash() = process.kill()

    override fun accepted(orderId: String) = saved?.orders?.containsKey(orderId) == true

    override fun outcome(orderId: String): Terminal? = when (saved?.orders?.get(orderId)?.stage) {
        OrderModel.Stage.Completed -> Terminal.Completed
        OrderModel.Stage.Failed -> Terminal.Failed
        else -> null
    }

    override fun reserved(): Map<String, Int> =
        saved?.orders?.values.orEmpty().filter { it.stage != OrderModel.Stage.Failed }.associate { it.order.id to it.order.quantity }
}
