package pokeball.examples.catalog.app

import pokeball.examples.catalog.CatalogSearch
import pokeball.examples.catalog.CatalogSearchBall
import pokeball.examples.catalog.Product
import pokeball.examples.catalog.SearchQuery
import pokeball.runtime.Adapter
import pokeball.runtime.Composition
import pokeball.runtime.RetryPolicy

/** Composition root of the search screen. Searches have no effect, so the runtime may retry them. */
fun catalogComposition(search: Adapter<SearchQuery, List<Product>>): Composition =
    Composition()
        .ball(CatalogSearchBall)
        .adapter(CatalogSearch, search, RetryPolicy(maxAttempts = 3, backoffMillis = 100))
