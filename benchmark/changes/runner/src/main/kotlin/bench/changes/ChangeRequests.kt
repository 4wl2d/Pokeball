package bench.changes

import bench.env.CheckoutSystem
import bench.env.LoyaltyPoints
import bench.env.OrderCancellation
import bench.env.Points
import bench.env.Scenario
import bench.env.Terminal
import bench.env.World
import bench.harness.Extension
import bench.harness.Violation

/*
 * The client requests and requirements of each change request, as specified
 * in benchmark/changes/README.md. A design without the change runs too: its
 * violations show that the new requirements catch the missing change.
 */

/** A change request: its key (the patch directory), its title, and a fresh [Extension] for each cell. */
class ChangeRequest(val key: String, val title: String, val extension: () -> Extension)

val changeRequests = listOf(
    ChangeRequest("cr1-refund", "Cancel a completed order within a window, and refund it", ::RefundOnCancel),
    ChangeRequest("cr2-loyalty", "Award loyalty points once per completed order", ::LoyaltyAward),
    ChangeRequest("cr3-failover", "A second payment provider with failover", ::Failover),
    ChangeRequest("cr4-no-inventory", "Remove the inventory", ::NoInventory),
)

/**
 * CR1. The customer of `o1` checks it every 100 ms until it has a terminal
 * state, and cancels as soon as they see it `Completed`; the customer of `o2`
 * cancels at 40 s, after the 30 s window; `o3` never cancels. After a restart
 * the client sends again every cancellation it sent before. Every order is
 * accepted at 0 or 50 ms, and the customer of `o1` stops checking at 30 s, so
 * only `o1`'s cancellation is inside the window.
 */
class RefundOnCancel : Extension {
    override val oracles = listOf("CancelIgnored", "RefundWithoutCancel", "FalseRefund", "UnreportedRefund", "RefundStockLeak")

    private val sent = LinkedHashSet<String>()
    private var o1SentCompleted = false

    private fun send(sut: CheckoutSystem, orderId: String) {
        sent += orderId
        if (orderId == IN_WINDOW && sut.outcome(orderId) == Terminal.Completed) o1SentCompleted = true
        (sut as? OrderCancellation)?.cancel(orderId)
    }

    private fun follow(world: World, sut: CheckoutSystem) {
        world.scheduler.schedule(POLL_MILLIS) {
            when (sut.outcome(IN_WINDOW)) {
                Terminal.Completed -> send(sut, IN_WINDOW)
                null -> if (world.scheduler.now() + POLL_MILLIS <= STOP_CHECKING_MILLIS) follow(world, sut)
                else -> Unit
            }
        }
    }

    override fun begin(world: World, sut: CheckoutSystem) {
        sent.clear()
        o1SentCompleted = false
        follow(world, sut)
        world.scheduler.schedule(LATE_CANCEL_MILLIS) { send(sut, LATE) }
    }

    override fun restarted(world: World, sut: CheckoutSystem) {
        for (orderId in sent.toList()) send(sut, orderId)
    }

    override fun check(world: World, sut: CheckoutSystem): Set<String> {
        val v = LinkedHashSet<String>()
        val reserved = sut.reserved()
        for (o in Scenario.orders) {
            val refunded = (world.provider.refunds[o.id] ?: 0) > 0
            val outcome = sut.outcome(o.id)
            val cancelledInWindow = o.id == IN_WINDOW && o.id in sent
            if (!cancelledInWindow && (refunded || outcome == Terminal.Refunded)) v += "RefundWithoutCancel"
            if (outcome == Terminal.Refunded && !refunded) v += "FalseRefund"
            if (refunded && outcome != Terminal.Refunded && outcome != Terminal.NeedsAttention) v += "UnreportedRefund"
            if (outcome == Terminal.Refunded && (reserved[o.id] ?: 0) > 0) v += "RefundStockLeak"
        }
        val o1 = sut.outcome(IN_WINDOW)
        if (o1SentCompleted && o1 != Terminal.Refunded && o1 != Terminal.NeedsAttention) v += "CancelIgnored"
        return v
    }

    override fun counts(world: World, sut: CheckoutSystem) = mapOf(
        "cancellationsSent" to sent.size,
        "refunded" to Scenario.orders.count { sut.outcome(it.id) == Terminal.Refunded },
        "refundRequests" to world.provider.refundRequests.values.sum(),
    )

    private companion object {
        const val IN_WINDOW = "o1"
        const val LATE = "o2"
        const val POLL_MILLIS = 100L
        const val STOP_CHECKING_MILLIS = 30_000L
        const val LATE_CANCEL_MILLIS = 40_000L
    }
}

/** CR2. Every completed order earns points through the loyalty service, awarded once. */
class LoyaltyAward : Extension {
    override val oracles = listOf("DoubleAward", "AwardWithoutCompletion", "FalseAward", "MissingAward")

    private fun points(sut: CheckoutSystem, orderId: String): Points? = (sut as? LoyaltyPoints)?.points(orderId)

    override fun watch(sut: CheckoutSystem): List<Any?> = Scenario.orders.map { points(sut, it.id) }

    override fun unresolved(sut: CheckoutSystem) = Scenario.orders.any { sut.outcome(it.id) == Terminal.Completed && points(sut, it.id) == null }

    override fun check(world: World, sut: CheckoutSystem): Set<String> {
        val v = LinkedHashSet<String>()
        for (o in Scenario.orders) {
            val awards = world.loyalty.awards[o.id] ?: 0
            val outcome = sut.outcome(o.id)
            val points = points(sut, o.id)
            if (awards > 1) v += "DoubleAward"
            if (awards > 0 && outcome != Terminal.Completed) v += "AwardWithoutCompletion"
            if (points == Points.Awarded && awards == 0) v += "FalseAward"
            if (outcome == Terminal.Completed && points != Points.Awarded && points != Points.NeedsAttention) v += "MissingAward"
        }
        return v
    }

    override fun counts(world: World, sut: CheckoutSystem) = mapOf(
        "awarded" to Scenario.orders.count { points(sut, it.id) == Points.Awarded },
        "awardNeedsAttention" to Scenario.orders.count { points(sut, it.id) == Points.NeedsAttention },
        "awardRequests" to world.loyalty.awardRequests.values.sum(),
        "awardStatusRequests" to world.loyalty.statusRequests.values.sum(),
    )
}

/**
 * CR3. The base requirements count the charges at both providers (see the
 * harness); an order fails only after the secondary was tried: asked to charge
 * it, or, when a crash came between deciding to charge there and sending the
 * request, asked for its status.
 */
class Failover : Extension {
    override val oracles = listOf("MissedFailover")

    override fun check(world: World, sut: CheckoutSystem): Set<String> {
        val secondary = world.secondaryProvider
        val missed = Scenario.orders.any { o ->
            sut.outcome(o.id) == Terminal.Failed && (world.provider.chargeRequests[o.id] ?: 0) > 0 &&
                (secondary.chargeRequests[o.id] ?: 0) + (secondary.statusRequests[o.id] ?: 0) == 0
        }
        return if (missed) setOf("MissedFailover") else emptySet()
    }

    override fun counts(world: World, sut: CheckoutSystem) = mapOf(
        "chargedAtSecondary" to Scenario.orders.count { (world.secondaryProvider.charges[it.id] ?: 0) > 0 },
        "secondaryChargeRequests" to world.secondaryProvider.chargeRequests.values.sum(),
    )
}

/**
 * CR4. No stock: Oversell and StockLeak no longer apply; no order fails before
 * the provider was asked to charge it (or, when a crash came between deciding
 * to charge and sending the request, for its status), and nothing is reserved.
 */
class NoInventory : Extension {
    override val oracles = listOf("UnchargedFailure", "ReservationKept")

    override val dropped = setOf(Violation.Oversell, Violation.StockLeak)

    override fun check(world: World, sut: CheckoutSystem): Set<String> {
        val v = LinkedHashSet<String>()
        val p = world.provider
        if (Scenario.orders.any { sut.outcome(it.id) == Terminal.Failed && (p.chargeRequests[it.id] ?: 0) + (p.statusRequests[it.id] ?: 0) == 0 }) v += "UnchargedFailure"
        if (sut.reserved().values.any { it > 0 }) v += "ReservationKept"
        return v
    }
}
