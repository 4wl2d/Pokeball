# Core part — Catalog search example

[Core contents](../../pokeball-architecture-core.md) · [← Manifest and source organization](../14-manifest-and-organization.md) · [Checkout Flow: model and ingress →](16-01-checkout-model-and-ingress.md)

> Canonical part 11 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 15. End-to-end example I: catalog search

This example shows a small `FeatureBall` with an `Inline` decision path and an asynchronous resource result. It requires no Flow, broker, or durable workflow engine.

### 15.1. Protocol

The authoritative Catalog contract identities are:

```text
protocolVersion = 2.0.0
stateSchemaVersion = 2
transitionArtifactVersion = 2.0.1
```

```text
CatalogIntent =
    SearchRequested(query, pageSize)
  | SearchCancelled(operationId)
  | ProductSelected(productId)

CatalogQuery =
    GetCatalogView

CatalogSignal =
    ProductSelectionConfirmed(productId)

CatalogFact =
    ProductsFound(searchHandle, generation, products, provenance)
  | ProductSearchFailed(searchHandle, generation, failure, provenance)
  | ProductSearchOutcomeUnknown(searchHandle, generation, provenance)
  | ProductSearchCancelledBeforeStart(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationAcceptedInProgress(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancelled(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationTooLate(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationRejected(cancellationHandle, targetSearchHandle, generation, reason, provenance)
  | ProductSearchCancellationOutcomeUnknown(cancellationHandle, targetSearchHandle, generation, provenance)

CatalogSearchCancellation =
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected(reason)
  | CancellationUnknown

CatalogSearchLifecycle =
    Searching(generation, query, pageSize)
  | Ready(generation, products)
  | Failed(generation, reason)
  | OutcomeUnknown(generation, query, pageSize)
  | Cancelled(generation)

CatalogView =
    CatalogIdle
  | CatalogSearchStatus(operationId, lifecycle, cancellation)

CatalogEffect =
    FindProducts(searchHandle, query, pageSize, mode, deadline)
  | CancelProductSearch(targetSearchHandle)
```

`ProductSelectionConfirmed` is owned by Catalog. It can leave the Ball only as the payload of an accepted `SignalPublication`; the Assembly route in §14.4 cannot define or synthesize it.

The owned query mapping is `GetCatalogView -> CatalogView`. A successful local read maps the exact committed `CatalogState` through the total function below and returns `ReadResult<CatalogView>` with the stamp of that snapshot; it creates neither a `ProjectionOutput` nor a new commit. Search-state `ProjectionOutput` uses the same `CatalogView` payload so the live and queried view cannot select different lifecycle facets.

### 15.2. State

```text
CatalogState =
    Idle(revision)
  | Searching {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Ready {
        operationId
        generation
        query
        products
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Failed {
        operationId
        generation
        reason
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | OutcomeUnknown {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Cancelled {
        operationId
        generation
        cancellation: CancellationAcceptedBeforeStart | CancellationAcceptedInProgress
        revision
}
```

The illustrative `revision` field inside `CatalogState` is a Ball-owned **domain revision** used by Catalog transitions and projections. It is distinct from the acceptor-owned `CommitRevision` in §3.2. A concrete binding may derive or map one from the other only when its declared contract proves their exact equality and preserves both ownership meanings; otherwise they remain separate values and neither is evidence for the other.

`CancellationRejected(reason)` retains the bounded typed rejection reason in committed state. It is not reconstructed from logs or a transient callback. `OutcomeUnknown` retains the current search identity and request fields so a later matching result, cancellation observation, or declared reconciliation decision can refine the same operation without reading runtime history.

Committed `CatalogState` is the sole authority. The version-2 mapping to `CatalogView` is exactly these six cases, with no fallback/default branch and no multiple matching case:

| `CatalogState` source variant | Exact `CatalogView` result |
|---|---|
| `Idle(revision)` | `CatalogIdle` |
| `Searching(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Searching(generation, query, pageSize), cancellation)` |
| `Ready(operationId, generation, ..., products, cancellation, revision)` | `CatalogSearchStatus(operationId, Ready(generation, products), cancellation)` |
| `Failed(operationId, generation, reason, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Failed(generation, reason), cancellation)` |
| `OutcomeUnknown(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, OutcomeUnknown(generation, query, pageSize), cancellation)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `CatalogSearchStatus(operationId, Cancelled(generation), cancellation)` |

Call this pure total mapping `toCatalogView`. Every state-changing Catalog search/cancellation Decision publishes `toCatalogView(nextState)` when its transition below names a projection. The mapping exposes lifecycle, proven result or failure, cancellation status, and cancellation rejection reason together; private pending handles remain state, not public status.

As a worked-example projection of the migration and recovery clauses in §§8.9 and 10.11, persisted schema-v1 state enters normal version-2 `decide` only after authoritative upcast. An authoritative upcaster maps each v1 `Idle`, `Searching`, `Ready`, `Failed`, or `Cancelled` record and its exact fields to the corresponding v2 variant before execution. A v1 `CancellationRejected` record lacks the required reason: it is upcast only when bounded authoritative accepted evidence supplies that exact reason; otherwise the record is quarantined or routed to a declared manual remediation path. No null, empty, generic, or inferred reason is synthesized. Existing retained v1 protocol outputs keep their v1 meaning and are not reinterpreted as v2 `CatalogView` or `CatalogSignal` payloads.

State stores a `SemanticHandle`, not a storage-generated `OutputId`. A runtime ledger may associate:

```text
SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
} -> OutputId(out-8f...)
```

This mechanical association does not change business state.

### 15.3. Interaction: parsing and validation

Raw request:

```text
query = "50%_off' OR 1=1 --"
pageSize = "100"
```

Interaction performs:

```text
UTF-8 validation
NFC normalization according to field contract
SearchText.parse(maxUtf8Bytes = 128)
PageSize.parse(range = 1..100)
request deadline construction
```

The string is valid as literal user text. Special characters are not removed in the name of "security."

Request:

```text
pageSize = "2000000000"
```

is rejected as `BoundaryResponse(ValidationFailure.InvalidPageSize)`. Interaction does not create a `SearchRequested`, `Decision`, `CommitRevision`, or `SemanticHandle`: a boundary response is not a committed `ReplyOutput`. Clamping is permitted only as a separate, explicitly declared product policy.

The trusted boundary passes:

```text
DecisionContext {
    trustedTimeObservation
    reservedSemanticIds = [search-88]
    expiresAt
}
```

`operationId` is neither read from an arbitrary client field nor generated from ambient randomness inside the Nucleus.

For `ProductSelected(productId)`, Interaction validates the product identifier and supplies one reserved selection operation ID, for example `reservedSemanticIds = [selection-501]`. The selection path shown here is actor-independent and uses no configuration or trusted time, so it creates no actor/configuration/time context fields. A project adds actor context only when actor, tenant, issuer, realm, assurance, or delegation can change that selection Decision, in which case PBA-44 applies independently of privilege.

### 15.4. First decision

```text
Idle
+ SearchRequested(query, pageSize)
+ DecisionContext(reservedSemanticIds = [search-88])

searchHandle = SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
}

-> Searching {
     operationId = search-88
     generation = 1
     query
     pageSize
     pendingSearch = searchHandle
     pendingCancellation = none
     cancellation = NotRequested
   }

+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId = search-88
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-searching-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         search-88,
         Searching(generation = 1, query, pageSize),
         NotRequested
     ) # exactly toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = searchHandle
     sourceOrdinal = 1
     payload = FindProducts(
         searchHandle,
         query,
         pageSize,
         mode = LiteralContains,
         deadline = context.expiresAt
     )
   }
```

Runtime checks output bounds and capability binding, accepts the `Decision`, and then dispatches the Effect. If state is persistent, state and the effect outbox are recorded atomically.

`ProductSelected` has one explicit transition from each of the six state variants. These are six closed cases, not a wildcard fallback:

| Source state + `ProductSelected(productId)` | Next state |
|---|---|
| `Idle(revision)` | `Idle(revision + 1)` |
| `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision)` | `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision + 1)` |
| `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision)` | `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision + 1)`; a `CancellationRejected(reason)` facet retains its own typed cancellation reason independently of the search-failure reason. |
| `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `Cancelled(operationId, generation, cancellation, revision + 1)` |

Each of those six Decisions contains exactly this one output and no Projection, Reply, Effect, Command, or Timer:

```text
SignalPublication {
    semanticHandle = SemanticHandle {
        operationId = context.reservedSemanticIds.single # selection-501
        outputKind = Catalog.ProductSelectionConfirmed
        localOrdinalOrName = "selected"
    }
    sourceOrdinal = 0
    payload = ProductSelectionConfirmed(productId)
}
```

The accepted revision advance gives the routed publication one exact committed source revision while preserving every state-specific lifecycle and cancellation fact. The Signal is dispatched only after that state/output frame is accepted. Assembly owns the route binding; Catalog owns the payload and publication Decision.

### 15.5. Resource adapter and safe sink

The adapter receives only the `Catalog.Search` capability. It has no unrestricted database client, shell, or arbitrary HTTP client.

For SQL `LIKE` with literal semantics:

```text
escaped = escapeLikeLiteral(query, '\\')
pattern = "%" + escaped + "%"

SELECT product_id, title, price
FROM product_search
WHERE searchable_text LIKE ? ESCAPE '\\'
ORDER BY rank DESC, product_id ASC
LIMIT ?
```

The following are mandatory:

- parameter binding;
- explicit literal/wildcard semantics;
- read-only credential;
- approved index/query plan policy;
- timeout;
- row and response-byte bounds;
- schema validation of the external result.

Parameterization prevents SQL interpretation. Escaping `%` and `_` defines literal-search semantics specifically. These are separate responsibilities.

### 15.6. Successful result

The Resource produces a validated, provenance-bound `CatalogFact`:

```text
ProductsFound {
    searchHandle = SemanticHandle {
        operationId = search-88
        outputKind = Catalog.FindProducts
        localOrdinalOrName = "generation-1"
    }
    generation = 1
    products = BoundedList(max = 100)
    provenance = CatalogSearchExecutor / attempt-1
}
```

Decision:

```text
Searching(generation = 1, pendingSearch = searchHandle, cancellation)
or OutcomeUnknown(generation = 1, pendingSearch = searchHandle, cancellation)
+ ProductsFound(searchHandle, generation = 1, products, provenance)
+ DecisionContext = Unit

-> Ready(operationId, generation = 1, products, pendingCancellation, cancellation)
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-ready-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         operationId,
         Ready(generation = 1, products),
         cancellation
     ) # exactly toCatalogView(nextState)
  }
```

This result Decision has its own per-Pulse `Unit` context in the shown actor-independent fixture. It does not inherit the initiating `SearchRequested` context's reserved ID, trusted time, or validity fields merely because an Inline binding may execute both causes in one causal scope.

`ProductSearchFailed` follows the same `Searching|OutcomeUnknown -> Failed` refinement and publishes the complete `CatalogSearchStatus(..., Failed(generation, reason), cancellation)`. A proven result or failure refines an earlier `OutcomeUnknown` but preserves the current cancellation facet, including `CancellationRejected(reason)`.

### 15.7. Stale result

The user starts a second search before the first one completes:

```text
first  -> generation 1 / SemanticHandle(search-88, Catalog.FindProducts, "generation-1")
second -> generation 2 / SemanticHandle(search-89, Catalog.FindProducts, "generation-2")
```

The second line abbreviates a separate accepted restart Decision; its full transition block is intentionally omitted. The stale-result trace begins only after that frame has committed generation 2 and its new handle/output batch, so no callback or runtime component changes Catalog state implicitly.

A late `ProductsFound` with the complete handle of the first operation and `generation = 1` does not replace generation 2. The Nucleus:

- ignores the stale result;
- or records a diagnostic outcome;
- or uses only safe cache information under an explicitly declared merge rule.

It does not apply a result according to "the last callback wins."

### 15.8. Cancellation race

`SearchCancelled(receivedOperationId)` is a valid Catalog Intent, but it may target only the current operation. Before constructing a cancellation handle or accepting any cancellation Decision, the Catalog Nucleus requires exact equality:

```text
receivedOperationId
    = state.operationId
    = state.pendingSearch.operationId
```

This guard applies only to `Searching` or `OutcomeUnknown` with a present `pendingSearch`; another source state follows its own closed rejection contract and cannot borrow a handle. If the three values differ, `decide` returns exactly `Rejected(BusinessRejection.StaleSearchOperation(expectedOperationId = state.operationId, receivedOperationId))`. Interaction encodes that actual Nucleus rejection as `BoundaryResponse(DecisionRejected(...))`. The bounded reason contains only the expected and received semantic IDs. The rejection accepts no Decision, State, revision, cancellation handle, Projection, Effect, or output, and it cannot cancel the current search. It is not a stale-result no-op and never combines A's Intent identity with B's `pendingSearch`.

```text
Searching or OutcomeUnknown(
    pendingSearch = searchHandle,
    cancellation = NotRequested
)
+ SearchCancelled(receivedOperationId = operationId)

cancelHandle = SemanticHandle {
    operationId
    outputKind = Catalog.CancelProductSearch
    localOrdinalOrName = "cancel-generation-2"
}

-> same lifecycle and search fields,
   pendingCancellation = cancelHandle,
   cancellation = Requested
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-cancel-requested-generation-2"
     }
     sourceOrdinal = 0
     payload = toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = cancelHandle
     sourceOrdinal = 1
     payload = CancelProductSearch(targetSearchHandle = searchHandle)
  }
```

Every cancellation Fact must match simultaneously on `cancellationHandle`, `targetSearchHandle`, `generation`, and trusted provenance. After matching, the following exhaustive transitions apply:

| Fact | State transition | Subsequent behavior |
|---|---|---|
| `ProductSearchCancelledBeforeStart` | `Searching\|OutcomeUnknown -> Cancelled(cancellation = CancellationAcceptedBeforeStart)`; from `Ready\|Failed` with an accepted terminal search result, an invariant/provenance fault with no new Decision | The terminal Fact proves cancellation before start. A mutually exclusive terminal proof in either order does not overwrite the first accepted state. |
| `ProductSearchCancellationAcceptedInProgress` | From `Searching\|OutcomeUnknown\|Ready\|Failed(cancellation = Requested)`, retain the exact lifecycle/result fields and change only the facet to `CancellationAcceptedInProgress`; a repeat in that facet or a late weaker proof from `Cancelled(CancellationAcceptedInProgress)` is a corroborating no-op | Acceptance does not prove physical stop; a matching result before or after this Fact remains authoritative. |
| `ProductSearchCancelled` | `Searching\|OutcomeUnknown(cancellation = Requested\|CancellationAcceptedInProgress) -> Cancelled(cancellation = CancellationAcceptedInProgress)`; from `Ready\|Failed`, an invariant/provenance fault with no new Decision | The terminal Fact itself proves accepted cancellation even if the separate accepted-in-progress Fact is delayed or lost. |
| `ProductSearchCancellationTooLate` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationTooLate` | An already received or subsequent matching result remains authoritative. |
| `ProductSearchCancellationRejected(reason)` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationRejected(reason)` | The bounded reason is committed and remains in every subsequent `CatalogView`. |
| `ProductSearchCancellationOutcomeUnknown` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationUnknown` | A matching result is not discarded; reconciliation remains explicit. |

Every accepted state-changing transition in the table creates one `ProjectionOutput` with a complete view handle, `sourceOrdinal = 0`, and payload `toCatalogView(nextState)`. It therefore publishes lifecycle/result and cancellation together. An exact duplicate or specified corroborating no-op creates no duplicate semantic state/output. A conflict path is not a transition: it accepts no Decision, state, or output and follows §8.8.

For one exact cancellation/search lineage, observation order is not a hidden precondition:

```text
ProductSearchCancellationAcceptedInProgress
ProductsFound
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductsFound
-> Ready(cancellation = Requested)
ProductSearchCancellationAcceptedInProgress
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductSearchCancellationAcceptedInProgress
ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)

ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)
late ProductSearchCancellationAcceptedInProgress
-> corroboration; no new semantic state/output
```

`ProductSearchFailed` follows the same compatible-ordering rules. They imply neither AIP-first transport ordering nor unbounded buffering.

Too-late, rejected, and cancellation-unknown observations commute with a legitimate result because each changes only the cancellation facet:

```text
CancellationRejected(reason-R)
ProductsFound(products-P)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))

ProductsFound(products-P)
CancellationRejected(reason-R)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))
```

`CancellationTooLate` and `CancellationUnknown` follow the same two-order rule. A prior search `OutcomeUnknown` may be refined by a later matching `ProductsFound` or `ProductSearchFailed`; that result changes only the lifecycle and retains the latest accepted cancellation facet and rejection reason.

Mutually exclusive terminal proofs have a symmetric contract:

```text
ProductSearchCancelled -> Cancelled
ProductsFound          -> invariant/provenance fault; no Decision; Cancelled remains accepted

ProductsFound          -> Ready
ProductSearchCancelled -> invariant/provenance fault; no Decision; Ready remains accepted
```

`ProductSearchFailed` follows the same terminal-conflict rule. This is neither last-arrival-wins nor an implicit conversion of typed `ProductSearchCancelled` into `CancellationTooLate`. Catalog v2 pins this closed transition family, including stale-cancellation admission, to `transitionArtifactVersion: 2.0.1`; `protocolVersion = 2.0.0`, `stateSchemaVersion = 2`, route versions, and the version-2 migration boundary in §§14.4/15.1–15.2 remain unchanged.

Both permitted ordering traces have the same meaning:

```text
CancellationTooLate -> Searching(cancellation = CancellationTooLate)
ProductsFound       -> Ready(cancellation = CancellationTooLate)

ProductsFound       -> Ready(cancellation = Requested)
CancellationTooLate -> Ready(cancellation = CancellationTooLate)
```

Likewise, a matching `ProductsFound` after `CancellationAcceptedInProgress`, `CancellationRejected(reason)`, or `CancellationUnknown` is accepted and retains the corresponding cancellation facet. A linear `Loading | Cancelled | Ready` enum without an independent facet is insufficient.

### 15.9. Timeout and unknown

Local deadline expiry means that the caller stopped waiting. If the external request might have been accepted, the result:

```text
ProductSearchOutcomeUnknown
```

is not converted into `ProductSearchFailed`. For a current matching search:

```text
Searching(all search fields, cancellation)
+ ProductSearchOutcomeUnknown(searchHandle, generation, provenance)

-> OutcomeUnknown(the same operation/search/pending fields, cancellation)
+ ProjectionOutput(
     sourceOrdinal = 0,
     payload = toCatalogView(nextState)
   )
```

A later matching `ProductsFound` or `ProductSearchFailed` refines `OutcomeUnknown` to `Ready` or `Failed` and preserves the cancellation facet. A later weaker `ProductSearchOutcomeUnknown` received after `Ready`, `Failed`, or proven `Cancelled` is a corroborating no-op with no state or output; it never regresses stronger proof. An exact duplicate in `OutcomeUnknown` is also a no-op.

Repetition is usually safe for a read-only search, but that is a property of this exact Effect contract, not a general timeout rule. Any retry preserves the operation/search handle, remains finite, and satisfies the complete §9.9 retry-ownership rule; this example does not select a retry policy.

### 15.10. What the example demonstrates

- Interaction does not know SQL.
- Resource does not know UI state.
- The Nucleus does not know the database driver.
- User text does not become query code.
- State is protected by generation/handle causality.
- Committed state maps through one six-case total `CatalogView` for both Query and Projection.
- Product selection publishes one producer-owned `CatalogSignal` from every state without losing search lifecycle or cancellation facts.
- Unknown can be refined by proof but cannot regress a proven terminal result.
- Runtime ID is separate from the semantic handle.
- The Inline path requires no mediator or queue.
- The asynchronous adapter can be replaced without changing decision semantics.

---
