package pokeball.archrules.fixtures.thread

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

/** Violation: blocks the loop and depends on scheduling. */
object ThreadBall : Ball<Int, Unit, Int> {
    override val type = "thread"
    override fun initial(key: String) = 0
    override fun decide(state: Int, request: Unit, ctx: RequestContext): pokeball.kernel.Decision<Int, Int> {
        Thread.sleep(1)
        return accept(state + 1, state)
    }
}
