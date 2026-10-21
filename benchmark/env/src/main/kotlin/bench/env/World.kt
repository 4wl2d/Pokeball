package bench.env

import ballarch.testkit.SimScheduler
import kotlin.random.Random

/** One unit of the checkout scenario (the same for every architecture). */
data class Order(val id: String, val sku: String, val quantity: Int, val amount: Long)

/** How a checkout ends, as recorded by the system under test. */
enum class Terminal { Completed, Failed, NeedsAttention }

/** The functional scenario every implementation is given. See benchmark/README.md. */
object Scenario {
    const val SKU = "sku-1"
    const val STOCK = 2
    val orders = listOf(Order("o1", SKU, 1, 100), Order("o2", SKU, 1, 100), Order("o3", SKU, 1, 100))

    /** Time after which a charge request that got no answer can no longer reach the provider. */
    const val PROVIDER_HORIZON_MILLIS = 1_000L

    /** Virtual time after which the run stops and is judged. */
    const val END_MILLIS = 120_000L

    /** Delay before a crashed process is running again. */
    const val RESTART_MILLIS = 50L
}

/** Distribution of the fates of provider calls. */
enum class FaultProfile(val reply: Int) {
    /** Every fate equally likely: an adversarial network. */
    Stress(reply = 1),

    /** 9 in 10 calls answered in time; the rest spread over the faults. */
    Mild(reply = 45),
}

/**
 * The simulated environment shared by every architecture: a virtual clock,
 * a durable database, the external payment provider and its ground truth,
 * and the source of fault decisions. Nothing in it is lost by a crash; a
 * system under test keeps everything else in memory that a crash discards.
 */
class World(seed: Long, val profile: FaultProfile) {
    val scheduler = SimScheduler(start = 0)
    val random = Random(seed)
    val db = SimDb()
    val provider = Provider(this)

    /** Incremented at every crash. */
    var incarnation: Int = 0
}

/**
 * A database with atomic transactions. Each [transaction] applies all its
 * writes or none; because the simulation is single-threaded and a crash can
 * only happen between scheduler tasks, a transaction cannot be cut short.
 */
class SimDb {
    private val rows = LinkedHashMap<String, Any>()
    private val objects = HashMap<String, Any>()
    private val listeners = ArrayList<(Set<String>) -> Unit>()

    /** Change data capture: [listener] receives the keys written by each committed transaction. */
    fun onCommit(listener: (Set<String>) -> Unit) {
        listeners += listener
    }

    fun get(key: String): Any? = rows[key]

    fun keys(prefix: String): List<String> = rows.keys.filter { it.startsWith(prefix) }

    fun <T> transaction(block: (Tx) -> T): T {
        val tx = Tx(this)
        val result = block(tx)
        for ((k, v) in tx.writes) if (v == null) rows.remove(k) else rows[k] = v
        if (tx.writes.isNotEmpty()) for (l in listeners.toList()) l(tx.writes.keys.toSet())
        return result
    }

    /** A durable object kept by key (for runtimes that bring their own store). */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> durable(key: String, create: () -> T): T = objects.getOrPut(key, create) as T

    class Tx internal constructor(private val db: SimDb) {
        internal val writes = LinkedHashMap<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        fun <T> get(key: String): T? = (if (key in writes) writes[key] else db.rows[key]) as T?

        fun put(key: String, value: Any) {
            writes[key] = value
        }

        fun delete(key: String) {
            writes[key] = null
        }

        fun keys(prefix: String): List<String> =
            (db.rows.keys + writes.keys).distinct().filter { it.startsWith(prefix) && get<Any>(it) != null }
    }
}
