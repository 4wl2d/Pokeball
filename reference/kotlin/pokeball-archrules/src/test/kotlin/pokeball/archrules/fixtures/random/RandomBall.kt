package pokeball.archrules.fixtures.random

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept
import kotlin.random.Random

/** Violation: ambient randomness instead of ctx.newKey() or an input. */
object RandomBall : Ball<Int, Unit, Int> {
    override val type = "random"
    override fun initial(key: String) = 0
    override fun decide(state: Int, request: Unit, ctx: RequestContext) = accept(Random.nextInt(), state)
}
