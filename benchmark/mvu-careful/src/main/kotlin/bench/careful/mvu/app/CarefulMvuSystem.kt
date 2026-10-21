package bench.careful.mvu.app

import bench.careful.mvu.core.ChargeOutcome
import bench.careful.mvu.core.Cmd
import bench.careful.mvu.core.Config
import bench.careful.mvu.core.Model
import bench.careful.mvu.core.Msg
import bench.careful.mvu.core.OrderModel
import bench.careful.mvu.core.StatusOutcome
import bench.careful.mvu.core.init
import bench.careful.mvu.core.update
import bench.env.ChargeResult
import bench.env.ChargeStatus
import bench.env.CheckoutSystem
import bench.env.ConnectionRefused
import bench.env.Order
import bench.env.Process
import bench.env.ProviderClient
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import bench.mvu.kit.Program
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** Wires the program: persistence of the model, and the effects of commands, whose failures become values. */
class CarefulMvuSystem(private val timeoutMillis: Long = 1_000) : CheckoutSystem {
    private lateinit var world: World
    private lateinit var process: Process
    private lateinit var program: Program<Model, Msg, Cmd>
    private val config = Config(horizonMillis = Scenario.PROVIDER_HORIZON_MILLIS)

    private val saved: Model? get() = world.db.get("mvu/model") as Model?

    override fun boot(world: World) {
        this.world = world
        process = Process(world.scheduler)
        val client = ProviderClient(world.provider)
        process.execute {
            program = Program(
                scope = process.scope,
                init = init(config, saved, Scenario.STOCK),
                update = { model, msg -> update(config, model, msg) },
                persist = { model -> world.db.transaction { it.put("mvu/model", model) } },
                perform = { cmd ->
                    when (cmd) {
                        is Cmd.Charge -> Msg.ChargeCompleted(cmd.orderId, charge(client, cmd))
                        is Cmd.CheckStatus -> {
                            delay(cmd.afterMillis)
                            Msg.StatusChecked(cmd.orderId, status(client, cmd.orderId))
                        }
                    }
                },
            )
        }
    }

    private suspend fun charge(client: ProviderClient, cmd: Cmd.Charge): ChargeOutcome = try {
        when (val result = withTimeout(timeoutMillis) { client.charge(cmd.orderId, cmd.amount) }) {
            is ChargeResult.Charged -> ChargeOutcome.Charged(result.reference)
            ChargeResult.Declined -> ChargeOutcome.Declined
        }
    } catch (e: ConnectionRefused) {
        ChargeOutcome.NotSent
    } catch (e: TimeoutCancellationException) {
        ChargeOutcome.Unknown
    } catch (e: IOException) {
        ChargeOutcome.Unknown
    }

    private suspend fun status(client: ProviderClient, orderId: String): StatusOutcome = try {
        when (val status = withTimeout(timeoutMillis) { client.status(orderId) }) {
            is ChargeStatus.Charged -> StatusOutcome.Charged(status.reference)
            ChargeStatus.NoCharge -> StatusOutcome.NoCharge
        }
    } catch (e: TimeoutCancellationException) {
        StatusOutcome.Unavailable
    } catch (e: IOException) {
        StatusOutcome.Unavailable
    }

    override fun start(order: Order) = process.execute { program.dispatch(Msg.Place(order)) }

    override fun crash() = process.kill()

    override fun accepted(orderId: String) = saved?.orders?.containsKey(orderId) == true

    override fun outcome(orderId: String): Terminal? = when (saved?.orders?.get(orderId)?.stage) {
        OrderModel.Stage.Completed -> Terminal.Completed
        OrderModel.Stage.Failed -> Terminal.Failed
        OrderModel.Stage.NeedsAttention -> Terminal.NeedsAttention
        else -> null
    }

    override fun reserved(): Map<String, Int> =
        saved?.orders?.values.orEmpty().filter { it.stage != OrderModel.Stage.Failed }.associate { it.order.id to it.order.quantity }
}
