package bench.env

import ballarch.testkit.SimScheduler
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * One incarnation of an operating-system process for coroutine code: a
 * dispatcher on the simulated clock. [kill] models a crash: from then on no
 * task of this incarnation runs, not even `finally` blocks, exactly as when
 * a process dies. Durable data lives only in the [World].
 */
@OptIn(InternalCoroutinesApi::class)
class Process(private val scheduler: SimScheduler) : CoroutineDispatcher(), Delay {
    private var alive = true

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + this)

    fun kill() {
        alive = false
    }

    /** Runs [block] on the simulated clock while this incarnation is alive. */
    fun execute(block: () -> Unit) = scheduler.execute { if (alive) block() }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduler.execute { if (alive) block.run() }
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val timer = scheduler.schedule(timeMillis) { if (alive) continuation.resume(Unit) }
        continuation.invokeOnCancellation { timer.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val timer = scheduler.schedule(timeMillis) { if (alive) block.run() }
        return DisposableHandle { timer.cancel() }
    }
}

/**
 * A checkout system built in one architectural style. The runner boots it,
 * sends each order, may crash and reboot it, and finally reads its durable
 * records to compare them with the provider's ground truth. The client is
 * at least once: after a restart it sends again every order that the system
 * has not durably accepted, and never an order that it has.
 */
interface CheckoutSystem {
    /** Starts an incarnation on [world]. Recovery after a crash happens here. */
    fun boot(world: World)

    /**
     * The client's checkout request: sent at time 0, and again right after a
     * restart while [accepted] is false for it. Must durably record acceptance
     * before any effect.
     */
    fun start(order: Order)

    /** Abrupt stop. */
    fun crash()

    /** Whether [orderId] was durably accepted. */
    fun accepted(orderId: String): Boolean

    /** The terminal state recorded for [orderId], or null while it has none. */
    fun outcome(orderId: String): Terminal?

    /** Units of stock currently reserved, per order, according to the durable records. */
    fun reserved(): Map<String, Int>
}

/** Change request CR1 (benchmark/changes): the customer may cancel an order. Repeated cancellations count as one. */
interface OrderCancellation {
    fun cancel(orderId: String)
}

/** The state of an order's loyalty award, as recorded by the system (change request CR2). */
enum class Points { Awarded, Failed, NeedsAttention }

/** Change request CR2 (benchmark/changes): completed orders earn loyalty points. */
interface LoyaltyPoints {
    /** The recorded state of [orderId]'s award, or null while it has none. */
    fun points(orderId: String): Points?
}
