package pokeball.runtime

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** A handle to a scheduled task. */
public fun interface Cancellable {
    public fun cancel()
}

/**
 * The single logical thread on which a [Runtime] runs. All decider invocations,
 * commits and dispatch decisions happen in tasks submitted here, one at a time.
 * Adapters may complete on other threads; their callbacks are re-submitted here.
 */
public interface Scheduler {
    /** Current time in epoch milliseconds. Deciders see it only through the context. */
    public fun now(): Long

    /** Run [task] on the loop after the tasks already submitted. Safe to call from any thread. */
    public fun execute(task: () -> Unit)

    /** Run [task] on the loop after [delayMillis]. Safe to call from any thread. */
    public fun schedule(delayMillis: Long, task: () -> Unit): Cancellable
}

/** A [Scheduler] backed by one dedicated thread and the system clock. */
public class ThreadScheduler(name: String = "pokeball-loop") : Scheduler, AutoCloseable {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, name).apply { isDaemon = true }
    }

    override fun now(): Long = System.currentTimeMillis()

    override fun execute(task: () -> Unit) {
        executor.execute(task)
    }

    override fun schedule(delayMillis: Long, task: () -> Unit): Cancellable {
        val future = executor.schedule(task, delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
        return Cancellable { future.cancel(false) }
    }

    override fun close() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }
}
