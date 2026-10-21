package bench.mvu.kit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A minimal Elm-architecture runtime standing in for a library: messages are
 * processed one at a time by a pure `update`, the new model is persisted, and
 * then the commands it returned are performed; each command's result comes
 * back as a message.
 */
class Program<Model, Msg, Cmd>(
    private val scope: CoroutineScope,
    init: Pair<Model, List<Cmd>>,
    private val update: (Model, Msg) -> Pair<Model, List<Cmd>>,
    private val persist: (Model) -> Unit,
    private val perform: suspend (Cmd) -> Msg,
) {
    private val inbox = Channel<Msg>(Channel.UNLIMITED)
    private var model: Model = init.first

    init {
        run(init.second)
        scope.launch { for (msg in inbox) step(msg) }
    }

    fun dispatch(msg: Msg) {
        inbox.trySend(msg)
    }

    private fun step(msg: Msg) {
        val (next, commands) = update(model, msg)
        model = next
        persist(next)
        run(commands)
    }

    private fun run(commands: List<Cmd>) {
        for (cmd in commands) scope.launch { dispatch(perform(cmd)) }
    }
}
