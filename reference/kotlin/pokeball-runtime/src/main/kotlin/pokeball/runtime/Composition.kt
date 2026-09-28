package pokeball.runtime

import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.Topic

/**
 * The composition root: the one place that states which Ball types exist,
 * which adapters implement which ports, and who subscribes to which topics.
 * Nothing is discovered at run time; unbound ports are rejected when a
 * decision tries to use them.
 */
public class Composition {
    internal val balls = LinkedHashMap<String, Ball<Any?, Any?, Any?>>()
    internal val adapters = LinkedHashMap<String, BoundAdapter>()
    internal val subscriptions = LinkedHashMap<String, MutableList<Subscription>>()

    @Suppress("UNCHECKED_CAST")
    public fun <S, M, R> ball(ball: Ball<S, M, R>): Composition = apply {
        require(ball.type !in balls) { "Ball type '${ball.type}' is registered twice" }
        balls[ball.type] = ball as Ball<Any?, Any?, Any?>
    }

    /**
     * Bind [adapter] to [port]. [retry] applies only to Safe and Idempotent ports;
     * [maxInFlight] bounds concurrent attempts, and further calls wait in phase Pending.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <Req, Res> adapter(
        port: AdapterPort<Req, Res>,
        adapter: Adapter<Req, Res>,
        retry: RetryPolicy = RetryPolicy.None,
        maxInFlight: Int = 64,
    ): Composition = apply {
        require(port.name !in adapters) { "port '${port.name}' is bound twice" }
        require(maxInFlight >= 1) { "maxInFlight must be at least 1" }
        require(retry.maxAttempts == 1 || port.effect != pokeball.kernel.EffectClass.NonIdempotent) {
            "port '${port.name}' is NonIdempotent and cannot be retried"
        }
        adapters[port.name] = BoundAdapter(port, adapter as Adapter<Any?, Any?>, retry, maxInFlight)
    }

    /** Deliver notices of [topic] to instances of [subscriberType]; [route] picks the instance key. */
    @Suppress("UNCHECKED_CAST")
    public fun <T> subscribe(topic: Topic<T>, subscriberType: String, route: (T) -> String): Composition = apply {
        subscriptions.getOrPut(topic.name) { mutableListOf() } += Subscription(subscriberType, route as (Any?) -> String)
    }

    internal fun validate() {
        for ((topic, subs) in subscriptions) {
            for (s in subs) require(s.subscriberType in balls) { "topic '$topic' routes to unknown Ball type '${s.subscriberType}'" }
        }
    }
}

internal class BoundAdapter(
    val port: AdapterPort<*, *>,
    val adapter: Adapter<Any?, Any?>,
    val retry: RetryPolicy,
    val maxInFlight: Int,
)

internal class Subscription(val subscriberType: String, val route: (Any?) -> String)

/** Finite bounds enforced by the runtime (rule R11). Exceeding one refuses new work before acceptance. */
public data class Limits(
    val maxInbox: Int = 1_024,
    val maxOpenCalls: Int = 256,
    val maxOutputsPerDecision: Int = 64,
    val maxTimeoutMillis: Long = 10 * 60 * 1_000L,
    val maxOutbox: Int = 1_024,
    /** Answered inbound requests kept per instance to deduplicate repeats. */
    val repliedRetention: Int = 1_024,
    val repliedRetentionMillis: Long = 60 * 60 * 1_000L,
    /** Delay before retrying delivery of a notice to a full inbox. */
    val noticeRetryMillis: Long = 10,
) {
    init {
        require(maxInbox > 0 && maxOpenCalls > 0 && maxOutputsPerDecision > 0 && maxTimeoutMillis > 0 && maxOutbox > 0)
        require(repliedRetention >= 0 && repliedRetentionMillis >= 0 && noticeRetryMillis > 0)
    }
}
