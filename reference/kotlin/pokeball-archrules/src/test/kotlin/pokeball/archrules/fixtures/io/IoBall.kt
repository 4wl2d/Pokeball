package pokeball.archrules.fixtures.io

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

/** Violation: file I/O inside a decision. */
object IoBall : Ball<String, String, String> {
    override val type = "io"
    override fun initial(key: String) = ""
    override fun decide(state: String, request: String, ctx: RequestContext) =
        accept(java.io.File(request).readText(), state)
}
