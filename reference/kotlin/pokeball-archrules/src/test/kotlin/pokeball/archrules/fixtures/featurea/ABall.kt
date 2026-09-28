package pokeball.archrules.fixtures.featurea

import pokeball.archrules.fixtures.featureb.BPricing
import pokeball.archrules.fixtures.featureb.api.BRequest
import pokeball.archrules.fixtures.featurea.api.ARequest
import pokeball.kernel.Ball
import pokeball.kernel.RequestContext
import pokeball.kernel.accept

/** Uses B's API (allowed) and B's internal pricing (violation of R10). */
object ABall : Ball<Int, ARequest, BRequest> {
    override val type = "a"
    override fun initial(key: String) = 0
    override fun decide(state: Int, request: ARequest, ctx: RequestContext) = accept(BPricing.price(request.n), BRequest(state))
}
