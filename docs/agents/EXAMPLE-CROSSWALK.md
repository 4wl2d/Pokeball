# Example Crosswalk

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Catalog (§15) and Checkout (§16) are advanced fixtures. Use only the property whose trigger matches the project. Do not copy their profiles, identities, limits, routes, participants, grants, status authority, or artifact count.

These examples are projections, not independent authority. When an example and a marked Core source clause differ, Core controls and the crosswalk must be repaired.

## Catalog Feature Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| local authority and logical zones | §§15.2–15.5 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| bounded-class capability at a real boundary and parameterized/dialect-safe sink | §§15.3, 15.5 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| detached result identity/stale policy | §§15.2, 15.4, 15.6–15.7 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| cancellation/result races | §§15.1–15.2, 15.8 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| possible-send unknown without result regression | §§15.2, 15.6, 15.9 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| one lossless Query/Projection view | §§15.1–15.2, 17.1–17.2 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| producer-owned Signal and Assembly route | §§14.4, 15.1/15.4, 17.5 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| protocol/state/transition migration boundary | §§10.11, 15.1–15.2, 17.7 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |

Catalog's Resource receives only the bounded `Catalog.Search` capability through a real restricted client/credential boundary and has no administrator client. Its SQL fixture demonstrates parameter binding plus explicit dialect escaping; it does not replace the separate capability-rooted-filesystem positive and raw-traversal negative fixtures when a filesystem interpreter path exists.

Canonical causal trace:

```text
SearchRequested generation N
-> accepted state + detached FindProducts handle N
-> provenance-bound result
-> apply current / ignore stale / retain typed failure or OutcomeUnknown
-> map committed state through one total CatalogView function
-> publish output only after acceptance
```

When generation `N+1` starts before result `N`, Core §15.7 intentionally omits the intervening restart block but assumes its separate Decision was accepted and established the new generation and handle before stale-result matching.

Catalog v2 pins `protocolVersion = 2.0.0`, `stateSchemaVersion = 2`, and `transitionArtifactVersion = 2.0.1`. Its state field `revision` is a Catalog-owned domain revision, not acceptor-owned `CommitRevision`; a concrete binding equates or derives them only through an explicit contract proving exact equality and preserving both ownership meanings. Its query is exactly `GetCatalogView -> CatalogView`. Committed `CatalogState` maps through six explicit, non-overlapping cases with no fallback:

```text
Idle           -> CatalogIdle
Searching      -> CatalogSearchStatus(operationId, Searching(...), cancellation)
Ready          -> CatalogSearchStatus(operationId, Ready(...), cancellation)
Failed         -> CatalogSearchStatus(operationId, Failed(...), cancellation)
OutcomeUnknown -> CatalogSearchStatus(operationId, OutcomeUnknown(...), cancellation)
Cancelled      -> CatalogSearchStatus(operationId, Cancelled(...), cancellation)
```

The composite status retains lifecycle/result and the independent cancellation facet together, including `CancellationRejected(reason)`. Search-state `ProjectionOutput` uses this same mapping. A weaker unknown observation cannot regress a proven `Ready`/`Failed` result; a later matching proven result may refine `OutcomeUnknown`.

For its cancellation trigger in `Searching` or `OutcomeUnknown` with present `pendingSearch`, `SearchCancelled(intentOperationId)` first requires `intentOperationId = state.operationId = pendingSearch.operationId`. A mismatch returns exactly `Rejected(BusinessRejection.StaleSearchOperation(expectedOperationId = state.operationId, receivedOperationId = intentOperationId))`, encoded by Interaction as `BoundaryResponse(DecisionRejected(...))`, and accepts no Decision, State, revision, handle, `ProjectionOutput`, cancellation `EffectRequest`, or output; it cannot target the current different search. Another source state follows its own closed rejection contract and cannot borrow a handle. On an exact match, compatible accepted-in-progress/result orderings converge without losing lifecycle, cancellation, or rejection reason; terminal cancellation itself proves acceptance; a later weaker observation is a no-op. Mutually exclusive terminal result/cancellation proof preserves the first accepted terminal frame and rejects the contradictory second proof before acceptance, symmetrically in both orders.

`ProductSelected(productId)` has six explicit transitions, set-equal to `Idle`, `Searching`, `Ready`, `Failed`, `OutcomeUnknown`, and `Cancelled`. Each preserves all state-specific fields and facets except the accepted revision advance and emits exactly one output:

```text
SignalPublication(
    payload = ProductSelectionConfirmed(productId),
    sourceOrdinal = 0
)
```

Catalog owns `CatalogSignal.ProductSelectionConfirmed`; Assembly only binds its `2.0.0/2.0.0` producer/consumer route and cannot define or synthesize the payload. The selection path shown by Core is actor-independent and carries only its reserved selection ID; actor/configuration/time fields remain absent unless their independent trigger appears.

Persisted schema-v1 state enters v2 `decide` only after authoritative upcast. A v1 cancellation-rejection record is upcast only when bounded accepted evidence supplies the exact newly required reason; otherwise it is quarantined or sent to declared manual remediation. No null, empty, generic, log-derived, or inferred reason is accepted, and retained v1 outputs keep v1 meaning.

The Catalog regression suite preserves the domain-revision versus `CommitRevision` distinction, all six state-to-view mappings and six `ProductSelected` transitions, exact-match and stale A-after-B cancellation Intents with no wrong-search Effect, every compatible lifecycle × cancellation × rejection-reason combination, both terminal conflict orders, late result/unknown refinement, and actor-independent sparsity. A present status trigger cannot be discharged by omitting its Query or one reachable facet.

A Ball with no external operation, detached result, or cancellation path inherits none of this machinery.

## Checkout Flow Ball

Moved to [EXAMPLE-CHECKOUT.md](EXAMPLE-CHECKOUT.md#checkout-flow-ball).

## Review use

1. Identify one analogous path or risk, not a similar domain name.
2. Cite Core and the matching `PKB-AR-*` rule, not the example alone.
3. Compare authoritative project source and effective policy with the invariant.
4. Use only the tests activated by that path.
5. Return to Core when the example does not cover the property.
