package pokeball.examples.catalog

import pokeball.kernel.AdapterPort
import pokeball.kernel.Ball
import pokeball.kernel.Call
import pokeball.kernel.CallKey
import pokeball.kernel.Cancel
import pokeball.kernel.Completion
import pokeball.kernel.Context
import pokeball.kernel.Decision
import pokeball.kernel.EffectClass
import pokeball.kernel.Outcome
import pokeball.kernel.RequestContext
import pokeball.kernel.Step
import pokeball.kernel.accept
import pokeball.kernel.outcomeOf
import pokeball.kernel.step

/**
 * S1: a search screen shows results for the most recent query only.
 *
 * The search service has no side effects, so its port is [EffectClass.Safe]:
 * an unanswered search ends `NotDone(NoAnswer)` and this Ball never has to
 * handle an "unknown" outcome.
 */
data class Product(val name: String)

data class SearchQuery(val text: String)

val CatalogSearch = AdapterPort<SearchQuery, List<Product>>("catalog.search", EffectClass.Safe)

sealed interface Display {
    data object Idle : Display
    data class Loading(val query: String) : Display
    data class Results(val query: String, val products: List<Product>) : Display
    data class Failed(val query: String) : Display
}

/** [pending] is the key of the only search whose answer may change the display. */
data class SearchState(val pending: CallKey?, val display: Display)

sealed interface SearchRequest {
    data class Search(val query: String) : SearchRequest
}

object CatalogSearchBall : Ball<SearchState, SearchRequest, Display> {
    const val SEARCH_TIMEOUT_MS = 2_000L

    override val type = "catalog-search"

    override fun initial(key: String) = SearchState(pending = null, display = Display.Idle)

    override fun decide(state: SearchState, request: SearchRequest, ctx: RequestContext): Decision<SearchState, Display> =
        when (request) {
            is SearchRequest.Search -> {
                val key = ctx.newKey()
                val loading = Display.Loading(request.query)
                val outputs = listOfNotNull(
                    state.pending?.let { Cancel(it) }, // saves work; staleness is handled by the key check below
                    Call(key, CatalogSearch, SearchQuery(request.query), SEARCH_TIMEOUT_MS),
                )
                accept(SearchState(key, loading), loading, *outputs.toTypedArray())
            }
        }

    override fun complete(state: SearchState, completion: Completion<*>, ctx: Context): Step<SearchState, Display> {
        if (completion.key != state.pending) return step(state) // an older search: never replaces the display
        val query = (state.display as Display.Loading).query
        val display = when (val outcome = completion.outcomeOf(CatalogSearch)!!) {
            is Outcome.Done -> Display.Results(query, outcome.value)
            is Outcome.NotDone, is Outcome.Unknown -> Display.Failed(query)
        }
        return step(SearchState(pending = null, display = display))
    }
}
