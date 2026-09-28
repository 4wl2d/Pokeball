// Seeded violations of the Core 2 author rules. Each package contains exactly
// one kind of violation, so the tests can show which rule detects which kind.
@file:Suppress("unused")

package pokeball.archrules.fixtures

import pokeball.kernel.Ball
import pokeball.kernel.Decision
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

/** A pure Ball: the negative control. `java.time.Duration` as a value is allowed. */
object CleanBall : Ball<Int, Int, Int> {
    override val type = "clean"
    override fun initial(key: String) = 0
    override fun decide(state: Int, request: Int, ctx: RequestContext): Decision<Int, Int> =
        accept(state + request + java.time.Duration.ofMillis(ctx.now).toSecondsPart(), state)
}
