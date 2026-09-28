package pokeball.archrules.fixtures.javatime

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept
import java.time.Instant

/** Violation: java.time value types are fine, but Instant.now() reads the clock. */
object JavaTimeBall : Ball<Long, Unit, Long> {
    override val type = "javatime"
    override fun initial(key: String) = 0L
    override fun decide(state: Long, request: Unit, ctx: RequestContext) = accept(Instant.now().toEpochMilli(), state)
}
