# Operation Status and Async Tests

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation only when operation status, materialization, delivery-stop evidence, or the corresponding async test inventory is triggered.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Async and Status Runbook](ASYNC-STATUS-RUNBOOK.md) · [Agent Pack index](README.md)

## 7. Operation status

Add a status authority only when the Core §9.11 trigger exists. `OperationId` identifies an accepted operation; reservation alone is not a status identity. Authenticate status only when access crosses a trust/access-control boundary. Record the base authority and then only triggered facets in authoritative source or an exact reusable policy:

```text
authority identity and operation key
revision/writer and ConsistencyStamp construction
reachable lifecycle sources

optional by subtrigger:
  authenticated namespace/access policy
  acceptance, workflow, cancellation, ambiguity, or delivery source
  NotFound proof
  expired-marker proof/horizon
  freshness/lag claim and mechanism
  retention mechanism
  ownership fence when movable
  pending/out-of-order capacity and backpressure when buffering/reordering exists
```

Each status namespace has one committed revisioned single-writer authority. A known root row begins only from accepted source-operation evidence. A query using a reserved-but-unaccepted candidate may return `NotFound` only when the committed snapshot covers the declared namespace and satisfies the materializer-absence barrier; it cannot reinterpret a root pre-acceptance response as operation status. The authority does not become command authority for source facts, and its stamp proves only its own snapshot. Co-located and separate authorities are both legal when the same ownership rule holds.

Catalog v2 uses committed `CatalogState` as that authority and `GetCatalogView -> CatalogView` as its one read contract. Its state field `revision` is a Catalog-owned domain revision, distinct from acceptor-owned `CommitRevision`; a binding equates or derives them only through an explicit contract proving exact equality and preserving both ownership meanings. The mapping is total over exactly `Idle`, `Searching`, `Ready`, `Failed`, `OutcomeUnknown`, and `Cancelled`: `Idle` maps to `CatalogIdle`; every other variant maps to one composite `CatalogSearchStatus(operationId, lifecycle, cancellation)`. There is no fallback or overlapping case, and the payload preserves a proven result/failure, cancellation state, and any `CancellationRejected(reason)` together. Search-state `ProjectionOutput` uses the same mapping so a live projection cannot disagree with a query.

The generic materializer applies a causal source in order or retains it in bounded pending storage. It merges equivalent duplicates idempotently; nonequivalent evidence for one key follows the declared monotonic conflict rule and fails closed rather than selecting by arrival order. Lifecycle, acceptance, cancellation, business-result, workflow, ambiguity, delivery-stop, and retention facets remain lossless and independent.

Finite capacity for the operation row, pending evidence, every reachable facet, retention marker, and bounded stop set is reserved before source acceptance. Later pressure never evicts or truncates an accepted fact. A retention marker commits only after its covered source positions are applied and pending is empty; the marker establishes expiry before absence and a covered late duplicate cannot resurrect or erase it.

If several independently delivered outputs can stop, retain one bounded canonically ordered record per complete `SemanticHandle`, including its declared reason, attempts, and last observation. An identical duplicate is a no-op; conflicting evidence for one handle fails closed; another handle never evicts an existing record; lifecycle and cancellation remain independent. A stop before its causal workflow record is bounded-pending or causally ordered, never dropped.

In canonical Checkout v1, accepted `Payment.GetOperationStatus -> StillUnknown` exhausts the sole normal automatic `payment-status` reconciliation slot. The source accepts one Decision that preserves the original capture acceptance/outcome facets and exact status evidence, advances that slot through `Accepted + Succeeded`, sets `terminalOutcome = NeedsManualReconciliation`, and has `outputs = []`. It creates no second status generation, timer, command, Effect, compensation, or reopen path. Duplicate/redelivery creates no Decision, revision, handle, or output. Normal v1 does not reopen capture or silently rewrite that terminal frame; later proof enters only through a separately declared manual/recovery artifact.

An accepted target `ModuleResultOutput` survives a crash before result dispatch under the selected target profile. Result-route exhaustion records target `DispatchStopped` by the complete result-delivery tuple; it does not fabricate source receipt or change a source acceptance, outcome, cancellation, workflow, or status facet. The source remains at its previously proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` until a verified result or declared reconciliation proof arrives. A late result may refine source facets without erasing the target stop fact.

Any durability, delivery, receipt, acceptance, once-only, or recovery claim names its exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees. Source durability, an accepted target output, or retained pending work alone proves no stronger downstream receipt, acceptance, durable reply, eventual delivery, once-only execution, or recovery outcome.

## 8. Triggered tests

Select the rows whose paths exist:

| Trigger | Minimum test |
|---|---|
| synchronous causal completion | source accepts complete output before dispatch; target owns serialized acceptance; return/refusal/failure enters the source's serialized handler; each input keeps its own trusted Context; the whole static execution terminates including result handlers, without token/depth/level-reservation artifacts; when growing or retained work exists, admission enforces actual capacity and preserves applicable budget without losing, truncating, or rewriting accepted work |
| command/result round trip | immediate typed call: source acceptance-before-dispatch, target acceptance-before-successful-return, serialized source completion, no target acceptance on refusal, post-acceptance failure preserved, finite full execution, target-owned mapping, additional allowed caller through wiring; portable paths additionally verify accepted source/target identities, required provenance, refusal/result conflict, duplicate-before-result ACK without new work, exact post-result replay, ACK/result order, retained-capacity boundary, crash/stop/late result, and budget preservation when applicable |
| detached result | exact source/provenance; stale, duplicate, and conflict cases only when reachable |
| possible send without proof | possible-send boundary, unknown, one reconcile identity |
| retry | root same-fingerprint proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with all Core §9.6 identity preserved and only new `AttemptId`; pre-Intent `ValidationFailure(IdempotencyConflict)` mismatch; target ACK-before-result and exact result-frame replay with all Core §9.6 identity preserved and only new `AttemptId`; stable logical identity; exactly one primary owner per active failure mode; every other layer disabled or finite and proven semantically transparent; cumulative attempts; idempotency horizon only when blind duplicate execution is possible |
| cancellation | reachable result/cancel orders and terminal conflict; Catalog exact operation/search-handle guard plus mismatch no-Decision/no-output |
| timer | stable identity/late policy; old/new generation only for replacement and dedup only for duplicate/redelivery risk |
| delivery observation | commit-before-observation, no direct runtime state write, plus monotonic duplicate handling only for duplicate observations |
| status | exact stamped single-writer authority; reserved root ID before/after acceptance; each root validation/admission/Decision rejection returns only `BoundaryResponse` and creates no known row/marker/handle/output; namespace-and-barrier-proven `NotFound`; participant carrier remains an accepted source Step facet; causal-order/bounded-pending; every reachable facet; duplicate/monotonic conflict; pre-acceptance N/N+1 reservation and no eviction; covered-source/empty-pending marker; expiry and no resurrection; target-result crash before dispatch |
| durable retained value | crash at each assignment/output boundary and exact recovery lineage |
| Catalog v2 status | Catalog domain `revision` distinct from acceptor `CommitRevision`, with any equality/derivation explicit; `transitionArtifactVersion = 2.0.1`; six set-equal state-to-view mappings, shared Query/Projection mapping, no fallback, lifecycle/cancellation/reason preservation, exact cancellation operation-ID guard, mismatch no-wrong-Effect, unknown-to-result refinement, and no proven-result regression |
| Catalog v1 persistence | every v1 state is authoritatively upcast before v2 `decide`; a missing cancellation-rejection reason or other required evidence quarantines the record instead of inventing a default |

Checkout has exactly ten source-output stop slots in canonical order: the initial acceptance `ReplyOutput` plus the nine command handles. Its regression suite covers exact original accepted `ReplyOutput(RequestAccepted(operationId))` frame replay versus pre-Intent idempotency conflict, reply-only, reply plus all nine commands, both orders for multiple stops, exact-handle duplicate/conflict, lifecycle/cancellation preservation, and capacity `10/11`. It also proves that accepted `StillUnknown` preserves capture facets/evidence, advances the sole status slot through `Accepted + Succeeded`, accepts one Decision with `outputs = []`, creates no second generation/timer/command/Effect/compensation/reopen path, and terminates normal automation as `NeedsManualReconciliation`; duplicate/redelivery creates no new Decision/revision/handle/output, and later proof requires a separately declared manual/recovery artifact. A reserved root candidate followed by validation/admission/Checkout-Decision rejection creates no Checkout operation/status row, while the same ID becomes authoritative only through accepted Checkout source evidence and participant carrier observations remain Step facets. Participant result outputs use separate target-owned result-delivery slots and do not create an eleventh Checkout source slot. See Core §16 and [EXAMPLE-CROSSWALK.md](EXAMPLE-CROSSWALK.md). These fixtures are not inherited by a Ball whose closed paths do not contain those risks.
