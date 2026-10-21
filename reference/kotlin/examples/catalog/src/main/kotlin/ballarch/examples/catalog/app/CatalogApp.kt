package ballarch.examples.catalog.app

import ballarch.examples.catalog.CatalogSearch
import ballarch.examples.catalog.CatalogSearchBall
import ballarch.examples.catalog.Product
import ballarch.examples.catalog.SearchQuery
import ballarch.runtime.Adapter
import ballarch.runtime.Composition
import ballarch.runtime.RetryPolicy

/** Composition root of the search screen. Searches have no effect, so the runtime may retry them. */
fun catalogComposition(search: Adapter<SearchQuery, List<Product>>): Composition =
    Composition()
        .ball(CatalogSearchBall)
        .adapter(CatalogSearch, search, RetryPolicy(maxAttempts = 3, backoffMillis = 100))
