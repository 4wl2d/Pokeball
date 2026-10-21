package bench.env

/** The loyalty service's record of an order's points (change request CR2, benchmark/changes). */
sealed interface AwardStatus {
    data class Awarded(val reference: String) : AwardStatus
    data object NotAwarded : AwardStatus
}

/**
 * The loyalty service of change request CR2: a second external effect. Like
 * the payment provider in scenario A it has no idempotency keys (every award
 * request that reaches it awards the points again), a Safe status endpoint,
 * the same faults and the same delivery horizon. It records the truth that
 * the CR's oracles compare with what each system believes.
 */
class LoyaltyService(world: World) {
    private val link = Link(world)

    /** Ground truth: awards applied per order. */
    val awards = LinkedHashMap<String, Int>()

    val awardRequests = LinkedHashMap<String, Int>()
    val statusRequests = LinkedHashMap<String, Int>()

    /** One award attempt; the answer is the award's reference. [done] is called at most once, possibly never. */
    fun award(orderId: String, points: Int, done: (Reply<String>) -> Unit) {
        awardRequests.merge(orderId, 1, Int::plus)
        link.send(done) {
            awards.merge(orderId, 1, Int::plus)
            "pt-$orderId-${awards.getValue(orderId)}"
        }
    }

    /** One status query. It has no effect. */
    fun status(orderId: String, done: (Reply<AwardStatus>) -> Unit) {
        statusRequests.merge(orderId, 1, Int::plus)
        link.send(done) {
            if ((awards[orderId] ?: 0) > 0) AwardStatus.Awarded("pt-$orderId-${awards.getValue(orderId)}") else AwardStatus.NotAwarded
        }
    }
}

/** The loyalty service's client library for coroutine code, like [ProviderClient]: one attempt per call, no timeout of its own. */
class LoyaltyClient(private val service: LoyaltyService) {
    suspend fun award(orderId: String, points: Int): String = call { service.award(orderId, points, it) }

    suspend fun status(orderId: String): AwardStatus = call { service.status(orderId, it) }
}
