# Example Crosswalk

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Catalog (§15) and Checkout (§16) are advanced fixtures. Use only the property whose trigger matches the project. Do not copy their profiles, identities, limits, routes, participants, grants, status authority, or artifact count.

These examples are projections, not independent authority. When an example and a marked Core source clause differ, Core controls and the crosswalk must be repaired.

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

The Catalog regression suite preserves all six state-to-view mappings and six `ProductSelected` transitions, every compatible lifecycle × cancellation × rejection-reason combination, both terminal conflict orders, late result/unknown refinement, and actor-independent sparsity. A present status trigger cannot be discharged by omitting its Query or one reachable facet.

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

Every Checkout command follows the same accepted target bridge:

```text
accepted Checkout ModuleCommandRequest
-> verified target ModuleCommandPulse(commandSource, effectiveProtocolIdentity,
                                       command: ModuleCommand, issuerProvenance)
-> target decide
-> accepted target ModuleResultOutput(sourceOrdinal, commandSource,
                                      payload: ModuleResult)
-> verified Checkout ModuleResultPulse(commandSource, resultSource,
                                       effectiveProtocolIdentity,
                                       result: ModuleResult, issuerProvenance)
```

Assembly binds and transports the route; it cannot synthesize either causal token, payload, or refusal meaning. Same-stack erasure proves the same tuples. The result-delivery key is the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`.

Normal business outcomes for all nine Checkout routes use accepted target results:

| Command | Accepted result path |
|---|---|
| `Cart.LockForCheckout` | success or domain lock refusal/conflict |
| `Cart.Unlock` | success/rejection/failure/unknown |
| `Inventory.Reserve` | `InventoryReserved` or `InventoryReservationRejected` |
| `Inventory.Release` | success/rejection/failure/unknown |
| `Payment.Capture` | captured/rejected/failed/outcome-unknown |
| `Payment.CancelCapture` | accepted-before-start/in-progress, too-late, rejected, or unknown |
| `Payment.Refund` | success/rejection/failure/unknown |
| `Payment.GetOperationStatus` | `Captured`, `DefinitelyNotCaptured`, or `StillUnknown` |
| `Order.Confirm` | confirmed or definitive rejected/failed/unknown |

`InventoryReservationRejected` is never the pre-acceptance carrier. `DefinitelyNotCaptured(RejectedBeforeAcceptance, ...)` describes refusal of the original `Payment.Capture`, not refusal of `Payment.GetOperationStatus`. A definitive `Order.Confirm` refusal after capture is an accepted result. Checkout may accept its own `RejectedBeforeExternalCommitment`, but that Flow outcome does not copy a carrier reason.

The canonical carrier projection is exact: enclosing Checkout `source` projects `commandSource`; the resolved dependency projects `effectiveProtocolIdentity`; `reason` projects the one closed `boundaryResponse`; and `acceptanceEvidence` projects `targetBoundaryProvenance`. A verified carrier yields `acceptance = RejectedBeforeAcceptance` and `outcome = NotExpected`; carried `BusinessRejection` is informational. A verified accepted `ModuleResultOutput(Rejected(...))` yields `acceptance = Accepted` and `outcome = Rejected`. Carrier/result conflict fails closed.

Payment demonstrates the Dual Gate and command-vs-read boundary. The trusted target boundary verifies the accepted command source, effective protocol identity, action-scoped actor/grant context, bounds, and provenance before constructing `ModuleCommandPulse`. The Payment Nucleus owns the business schema and Policy Gate and alone accepts a business Decision/result. Immediately before provider execution, the Execution Gate verifies the current proof, capability, constraints, version, freshness/revocation, endpoint, quota, and sink without inventing a business decision. Pre-`decide` validation/admission/provenance refusal uses the carrier; an accepted Nucleus refusal uses accepted `ModuleResultOutput(Rejected(...))`; post-acceptance provider/Execution-Gate failure remains Resource/result/status evidence and cannot become the carrier.

`Payment.GetOperationStatus` remains a command because Checkout requires accepted provenance, stable step identity, idempotent replay, status, and reconciliation. Its same-state snapshot acceptance advances Payment revision and emits an accepted result; EventJournal uses `Accepted(NoDomainChange { outputs = [...] })`. Duplicate delivery within the horizon returns the prior result and target proof without another Decision/revision. An ordinary non-recording lookup uses `ReadDependency`/`Query` instead.

Checkout declares `maxRetainedDispatchStops: 10` and has exactly ten canonical source-delivery stop slots:

```text
0 Cart.LockForCheckout
1 initial RequestAccepted ReplyOutput
2 Inventory.Reserve
3 Payment.Capture
4 Payment.CancelCapture
5 Payment.GetOperationStatus
6 Order.Confirm
7 Inventory.Release
8 Payment.Refund
9 Cart.Unlock
```

`initialAcceptanceReply` is the exact handle `SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance")`. Its trusted status-only delivery `ControlPulse` binds to the accepted Reply source tuple; it is not a command Pulse or business result and does not write `CheckoutState` directly. The initial Decision reserves both `cartLock` and `initialAcceptanceReply` slots before acceptance.

The status materializer reserves all reachable operation/pending/facet/marker/stop capacity before source acceptance, never evicts accepted facts, applies causal sources or bounded pending, merges identical duplicates idempotently, fails closed on conflicting same-key evidence, and creates a retention marker only after covered source positions and empty pending. Tests cover reply-only, reply plus all nine commands, both orders of two stops, duplicate/conflict, lifecycle/cancellation preservation, marker/pending races, no resurrection, and `10/11` capacity.

A participant's accepted `ModuleResultOutput` uses a separate participant-owned target stop slot keyed by the result-delivery tuple. Crash before result dispatch does not erase that accepted result under the selected target profile. Target route exhaustion records target `DispatchStopped` but does not add an eleventh Checkout source slot or set a Checkout facet; Checkout remains `Pending`/unknown until verified result or reconciliation, and a late result may refine it without erasing the target stop fact.

Those are Checkout's reachable paths, not universal Ball requirements.

## Review use

1. Identify one analogous path or risk, not a similar domain name.
2. Cite Core and the matching `PKB-AR-*` rule, not the example alone.
3. Compare authoritative project source and effective policy with the invariant.
4. Use only the tests activated by that path.
5. Return to Core when the example does not cover the property.
