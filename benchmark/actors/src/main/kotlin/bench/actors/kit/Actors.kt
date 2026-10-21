package bench.actors.kit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * A minimal typed-actor toolkit standing in for a library such as Akka Typed:
 * mailboxes, one message at a time per actor, `ask` with a timeout,
 * `pipeToSelf` for asynchronous results, and single timers.
 */
class ActorRef<M> internal constructor(private val mailbox: Channel<M>) {
    fun tell(message: M) {
        mailbox.trySend(message)
    }
}

class ActorContext<M> internal constructor(val self: ActorRef<M>, private val scope: CoroutineScope) {
    /** Runs [block] outside the actor, like a future started from a message handler. */
    fun run(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    /** Runs [block] outside the actor and delivers its result, or its failure, to the actor as a message. */
    fun <T> pipeToSelf(block: suspend () -> T, map: (Result<T>) -> M) {
        scope.launch { self.tell(map(runCatching { block() })) }
    }

    /** Sends [message] to this actor after [delayMillis], like a single timer of Akka's `TimerScheduler`. */
    fun scheduleOnce(delayMillis: Long, message: M) {
        scope.launch {
            delay(delayMillis)
            self.tell(message)
        }
    }

    /** Asks [target] with a reply promise; the reply, or a timeout, comes back as a message. */
    fun <Q, R> ask(target: ActorRef<Q>, timeoutMillis: Long, request: (CompletableDeferred<R>) -> Q, map: (Result<R>) -> M) =
        pipeToSelf({
            val reply = CompletableDeferred<R>()
            target.tell(request(reply))
            withTimeout(timeoutMillis) { reply.await() }
        }, map)
}

class ActorSystem(private val scope: CoroutineScope) {
    fun <M> spawn(behavior: suspend ActorContext<M>.(M) -> Unit): ActorRef<M> {
        val mailbox = Channel<M>(Channel.UNLIMITED)
        val ref = ActorRef(mailbox)
        val context = ActorContext(ref, scope)
        scope.launch { for (message in mailbox) context.behavior(message) }
        return ref
    }
}
