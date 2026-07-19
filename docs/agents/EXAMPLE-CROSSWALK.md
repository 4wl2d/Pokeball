# Example Crosswalk

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Catalog (§15) and Checkout (§16) are advanced fixtures. Use only the property whose trigger matches the project. Do not copy their profiles, identities, limits, routes, participants, grants, status authority, or artifact count.

## Catalog Feature Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| local authority and logical zones | §§15.2–15.5 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| external resource and safe sink | §§15.3, 15.5 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| detached result identity/stale policy | §§15.2, 15.4, 15.6–15.7 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| cancellation/result races | §§15.1–15.2, 15.8 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| possible-send unknown without result regression | §§15.2, 15.6, 15.9 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| one lossless Query/Projection view | §§15.1–15.2, 17.1–17.2 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| producer-owned Signal and Assembly route | §§14.4, 15.1/15.4, 17.5 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| protocol/state/transition migration boundary | §§10.11, 15.1–15.2, 17.7 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |

Canonical causal trace:

```text
SearchRequested generation N
-> accepted state + detached FindProducts handle N
-> provenance-bound result
-> apply current / ignore stale / retain typed failure or OutcomeUnknown
-> map committed state through one total CatalogView function
-> publish output only after acceptance
```

Catalog v2 pins `protocolVersion = 2.0.0`, `stateSchemaVersion = 2`, and `transitionArtifactVersion = 2.0.0`. Its query is exactly `GetCatalogView -> CatalogView`. Committed `CatalogState` maps through six explicit, non-overlapping cases with no fallback:

```text
Idle           -> CatalogIdle
Searching      -> CatalogSearchStatus(operationId, Searching(...), cancellation)
Ready          -> CatalogSearchStatus(operationId, Ready(...), cancellation)
Failed         -> CatalogSearchStatus(operationId, Failed(...), cancellation)
OutcomeUnknown -> CatalogSearchStatus(operationId, OutcomeUnknown(...), cancellation)
Cancelled      -> CatalogSearchStatus(operationId, Cancelled(...), cancellation)
```

The composite status retains lifecycle/result and the independent cancellation facet together, including `CancellationRejected(reason)`. Search-state `ProjectionOutput` uses this same mapping. A weaker unknown observation cannot regress a proven `Ready`/`Failed` result; a later matching proven result may refine `OutcomeUnknown`.

For its cancellation trigger, compatible accepted-in-progress/result orderings converge without losing lifecycle, cancellation, or rejection reason; terminal cancellation itself proves acceptance; a later weaker observation is a no-op. Mutually exclusive terminal result/cancellation proof preserves the first accepted terminal frame and rejects the contradictory second proof before acceptance, symmetrically in both orders.

`ProductSelected(productId)` has six explicit transitions, set-equal to `Idle`, `Searching`, `Ready`, `Failed`, `OutcomeUnknown`, and `Cancelled`. Each preserves all state-specific fields and facets except the accepted revision advance and emits exactly one output:

```text
SignalPublication(
    payload = ProductSelectionConfirmed(productId),
    sourceOrdinal = 0
)
```

Catalog owns `CatalogSignal.ProductSelectionConfirmed`; Assembly only binds its `2.0.0/2.0.0` producer/consumer route and cannot define or synthesize the payload. The selection path shown by Core is actor-independent and carries only its reserved selection ID; actor/configuration/time fields remain absent unless their independent trigger appears.

Persisted schema-v1 state enters v2 `decide` only after authoritative upcast. A v1 cancellation-rejection record is upcast only when bounded accepted evidence supplies the exact newly required reason; otherwise it is quarantined or sent to declared manual remediation. No null, empty, generic, log-derived, or inferred reason is accepted, and retained v1 outputs keep v1 meaning.

A Ball with no external operation, detached result, or cancellation path inherits none of this machinery.

## Checkout Flow Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| stateful multi-authority coordination | §§16.1, 16.14 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| durable ingress/idempotency | §16.2 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| cross-Decision retained values | §§16.3, 16.5–16.9 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| delayed privileged actions | §16.4 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| unknown/reconciliation/compensation | §§16.8, 16.10 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| post-commit delivery observation | §16.12 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| durable operation status/multi-stop | §16.13 | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) |

Canonical retained-value trace:

```text
CheckoutStarted retains M + ingress fingerprint + actor binding
CartLocked retains S
InventoryReserved retains I -> Capture(S, M, current grant)
PaymentCaptured retains P -> Confirm(S, I, P)
Order rejection -> Refund(P) + Release(I) + Unlock(original cart/lock)
```

Each retained value carries the authority/observation correlation, versions, provenance, and last-consumer retention required by the durable path. Direct and reconciled proof of the same P corroborate; conflicting proof fails closed. Recovery uses committed values, never a participant/runtime ledger or rerun of current transition code.

The delivery/status fixture includes the initial Reply plus all command handles, bounded pending/retention, duplicate/conflict, and capacity-edge tests. Those are Checkout's reachable paths, not universal Ball requirements.

## Review use

1. Identify one analogous path or risk, not a similar domain name.
2. Cite Core and the matching `PKB-AR-*` rule, not the example alone.
3. Compare authoritative project source and effective policy with the invariant.
4. Use only the tests activated by that path.
5. Return to Core when the example does not cover the property.
