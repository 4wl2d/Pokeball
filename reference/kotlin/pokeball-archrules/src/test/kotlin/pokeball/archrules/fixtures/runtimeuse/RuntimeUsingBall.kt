package pokeball.archrules.fixtures.runtimeuse

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept
import pokeball.runtime.MemoryStore

/** Violation: a decider reaching into the runtime (and through it, other Balls' records). */
object RuntimeUsingBall : Ball<Int, Unit, Int> {
    override val type = "runtimeuse"
    private val store = MemoryStore()
    override fun initial(key: String) = 0
    override fun decide(state: Int, request: Unit, ctx: RequestContext) = accept(store.ids().size, state)
}
