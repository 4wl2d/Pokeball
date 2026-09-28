package pokeball.archrules.fixtures.mutablecollection

import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

/** Violation: a mutable collection in the committed state. */
data class Basket(val items: MutableList<String>)

object MutableCollectionBall : Ball<Basket, String, Int> {
    override val type = "mutablecollection"
    override fun initial(key: String) = Basket(mutableListOf())
    override fun decide(state: Basket, request: String, ctx: RequestContext) =
        accept(Basket((state.items + request).toMutableList()), state.items.size)
}
