# Async and Status Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open only the sections activated by reachable detached work, late or duplicate result, separate ACK, ambiguous execution, retry, cancellation/deadline/timer, delivery observation, or operation status. A synchronous call-scope completion does not acquire the whole async model.

Every rule and test in this runbook is a projection of its named Core source clause. If a projection differs in modal force, trigger, owner, scope, failure behavior, reuse, or absence behavior, repair the projection in favor of Core.

## 1. Detached causal identity

When work can detach, retain, retry, reorder, cancel, recover, cross a Ball boundary, or enter status, resolve:

```text
logical OperationId when an independent operation lifecycle exists
SemanticHandle for the addressable work
accepted source ordinal or equivalent sequence identity
source Ball/revision when that identity crosses call scope or is observed
OutputId and AttemptId only after materialization
result issuer and accepted-operation proof only across the corresponding trust/acceptance boundary
generation and stale policy only for late/reorderable generations
duplicate identity and retention only for duplicate/redelivery risk
causal depth/budget only when the work can cause a later Decision
```

A retry changes attempt identity, not the logical operation or handle. A same-stack synchronous completion may use accepted call-scope position and needs no fabricated operation/transport lineage.

For a value used by a later Decision, retain its field-minimized value and add metadata only by subtrigger: source correlation when matching crosses call scope; version when rollout/persistence is observed; provenance across a trust boundary; consumer/deletion data when lifetime is not statically fixed. Inbox, outbox, participant ledger, and ambient history are not Decision inputs.

## 2. Operation facets

Materialize only facets that have reachable observations:

| Facet | Meanings when triggered |
|---|---|
| Dispatch | not sent, sent/might be sent, finite policy stopped |
| Acceptance | not accepted, accepted, rejected before acceptance, unknown |
| Business outcome | pending, succeeded, rejected, failed, cancelled, unknown |
| Cancellation | requested and accepted-before-start/in-progress/too-late/rejected-with-reason/unknown races |

Do not flatten independent meanings. A source commit does not imply dispatch; a result with accepted-operation proof implies acceptance; delivery stop proves neither failure nor non-execution; cancellation does not erase a legitimate result.

## 3. Canonical ingress

| Observation that exists | Input category |
|---|---|
| External start/cancel request | validated `Intent` |
| Private resource outcome | provenance-bound `Fact` |
| Accepted target Ball outcome | verified `ModuleResultPulse` constructed from an accepted `ModuleResultOutput` frame |
| Committed publication | `ObservedSignal` over one declared route |
| Runtime/route dispatch or ACK observation affecting state | typed trusted `ControlPulse` |

Runtime never writes operation facets directly into Sovereign State. Duplicate identity, source tuple, bounded/redacted evidence, and conflict policy are required only for observation paths that exist.

## 4. Command/result round trip

For every declared command path, resolve one target-owned, versioned `ModuleCommand -> ModuleResult` mapping and one static refusal classification. The complete accepted round trip is:

```text
accepted source ModuleCommandRequest frame
-> trusted target boundary verifies source tuple, effective protocol identity,
   target-owned payload, bounds, issuer provenance, and triggered actor/grant evidence
-> ModuleCommandPulse(commandSource, effectiveProtocolIdentity, command, issuerProvenance)
-> target decide as the sole acceptance point
-> accepted target Decision containing ModuleResultOutput
-> verified result route derives resultSource from the accepted target frame
-> ModuleResultPulse(commandSource, resultSource, effectiveProtocolIdentity,
                     result, issuerProvenance)
-> source decide
```

`ModuleResultOutput` has the target-frame `sourceOrdinal`, carries `commandSource` and `payload: ModuleResult`, and uses `semanticHandle = commandSource.semanticHandle` only for correlation. It remains target-owned, counts against target output-count and byte bounds, and, when retained, retried, or independently observable, occupies one target stop-eligible delivery/status slot. The result-delivery key is exactly the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`.

Assembly or generated same-stack code selects/binds the route and version pair and transports verified values. It cannot synthesize or modify `commandSource`, `resultSource`, issuer provenance, target payload, or refusal meaning. Representation erasure proves the same accepted source and target tuples.

Pre-acceptance refusal uses only:

```text
CommandRejectedBeforeAcceptance {
  commandSource
  effectiveProtocolIdentity
  boundaryResponse:
      ValidationFailure
    | AdmissionFailure
    | DecisionRejected(BusinessRejection)
  targetBoundaryProvenance
}
```

The carrier is neither Reply nor result and creates no accepted target Decision, revision, output, Resource action, operation record, status fact, or reconciliation fact. Forged, tampered, missing, stale, or wrong-target provenance creates no trusted source `ControlPulse`, refusal facet, or compensation. An accepted result is required whenever the target accepted any state, operation/idempotency outcome, output, Resource action, durable/status/reconciliation fact, or accepted-operation evidence. Carrier/result conflict fails closed; dispatch failure never downgrades an accepted result; reclassification requires a new target protocol version and Assembly version pair.

A snapshot refusal on the accepted-result path is a same-state accepted Decision with revision advance and `ModuleResultOutput(Rejected(...))`; EventJournal records accepted `NoDomainChange`. Redelivery of the same effective identity and `commandSource` within the idempotency horizon returns the prior accepted result and target proof without another Decision, revision, or Resource action.

For a same-stack round trip, source/root, target-command, and source-result Decisions consume causal levels `0/1/2`. The source reserves the target-completion slot and the target reserves the result-completion slot before their respective acceptance points. Failure of the second reservation returns `AdmissionFailure(CausalBudgetExceeded)` through the verified carrier and creates no target result. There is one direct-control edge `source -> target`; the return is not a reverse edge. Async handoff removes the direct edge but does not reset causal scope, depth, or remaining budget.

## 5. ACK, ambiguity, and retry

When ACK is separate, test the reachable orderings among source acceptance, dispatch, ACK, and result. A result may prove acceptance before an ACK. A timeout creates `AcceptanceUnknown` or `OutcomeUnknown` only after the declared possible-send/execution boundary; otherwise it is an ordinary attempt failure.

For each active retry layer, record one row in the authoritative route or operation policy:

| Failure mode | Primary owner | Stable logical key | Finite attempt/time budget | Idempotency horizon, if required | Other active layers |
|---|---|---|---|---|---|

Check cumulative SDK × adapter × runtime × Flow attempts. A blind retry after possible irreversible execution requires preserved identity and a target/provider idempotency horizon; otherwise use status, reconciliation, or manual policy.

## 6. Cancellation, deadline, and timer

Cancellation is a new typed operation with its own handle when it can race execution. Its executor or target returns only the accepted-before-start, accepted-in-progress, too-late, rejected, or unknown outcomes that can actually occur. A timer path has stable timer identity and an explicit late policy; generation appears only for cancellation/rescheduling/replacement, while firing identity and deduplication appear only for duplicate/redelivery risk.

Test only reachable races, but include both orders. For the canonical Catalog semantics:

- accepted-in-progress before/after a legitimate result converges without lifecycle regression;
- terminal cancellation itself proves acceptance, and a later weaker accepted-in-progress observation is a no-op;
- terminal result versus mutually exclusive terminal cancellation preserves the first accepted frame, while the contradictory second proof fails before acceptance with no state/output; and
- the conflict rule is symmetric in both arrival orders;
- `ProductSearchOutcomeUnknown` creates the explicit `OutcomeUnknown` lifecycle, while a later matching proven result may refine it and a later weaker unknown observation cannot regress `Ready` or `Failed`; and
- cancellation rejection retains its bounded reason through later compatible result/cancellation orderings.

## 7. Operation status

Add a status authority only when the Core §9.11 trigger exists. Authenticate it only when status access crosses a trust/access-control boundary. Record the base authority and then only triggered facets in authoritative source or an exact reusable policy:

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

Each status namespace has one committed revisioned single-writer authority. The authority does not become command authority for source facts, and its stamp proves only its own snapshot. Co-located and separate authorities are both legal when the same ownership rule holds.

Catalog v2 uses committed `CatalogState` as that authority and `GetCatalogView -> CatalogView` as its one read contract. The mapping is total over exactly `Idle`, `Searching`, `Ready`, `Failed`, `OutcomeUnknown`, and `Cancelled`: `Idle` maps to `CatalogIdle`; every other variant maps to one composite `CatalogSearchStatus(operationId, lifecycle, cancellation)`. There is no fallback or overlapping case, and the payload preserves a proven result/failure, cancellation state, and any `CancellationRejected(reason)` together. Search-state `ProjectionOutput` uses the same mapping so a live projection cannot disagree with a query.

The generic materializer applies a causal source in order or retains it in bounded pending storage. It merges equivalent duplicates idempotently; nonequivalent evidence for one key follows the declared monotonic conflict rule and fails closed rather than selecting by arrival order. Lifecycle, acceptance, cancellation, business-result, workflow, ambiguity, delivery-stop, and retention facets remain lossless and independent.

Finite capacity for the operation row, pending evidence, every reachable facet, retention marker, and bounded stop set is reserved before source acceptance. Later pressure never evicts or truncates an accepted fact. A retention marker commits only after its covered source positions are applied and pending is empty; the marker establishes expiry before absence and a covered late duplicate cannot resurrect or erase it.

If several independently delivered outputs can stop, retain one bounded canonically ordered record per complete `SemanticHandle`, including its declared reason, attempts, and last observation. An identical duplicate is a no-op; conflicting evidence for one handle fails closed; another handle never evicts an existing record; lifecycle and cancellation remain independent. A stop before its causal workflow record is bounded-pending or causally ordered, never dropped.

An accepted target `ModuleResultOutput` survives a crash before result dispatch under the selected target profile. Result-route exhaustion records target `DispatchStopped` by the complete result-delivery tuple; it does not fabricate source receipt or change a source acceptance, outcome, cancellation, workflow, or status facet. The source remains at its previously proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` until a verified result or declared reconciliation proof arrives. A late result may refine source facets without erasing the target stop fact.

## 8. Triggered tests

Select the rows whose paths exist:

| Trigger | Minimum test |
|---|---|
| synchronous causal completion | pre-reserve total budget; at N+1 the Decision fails before acceptance or the already accepted completion is retained in one bounded `RetainedContinuation`; never drop, truncate, or reset budget |
| command/result round trip | routed and same-stack accepted tuples; exact fields and unnamed delivery key; all three pre-acceptance responses; accepted same-state refusal; carrier/result conflict; versioned reclassification; duplicate replay; source/target forgery; ACK/result order; crash/stop/late result; handle collision; target N/N+1; levels 0/1/2 and two reservations; exact-one target mapping; command-vs-read |
| detached result | exact source/provenance; stale, duplicate, and conflict cases only when reachable |
| possible send without proof | possible-send boundary, unknown, one reconcile identity |
| retry | stable logical identity, finite budget, cumulative owners, plus idempotency horizon only when blind duplicate execution is possible |
| cancellation | reachable result/cancel orders and terminal conflict |
| timer | stable identity/late policy; old/new generation only for replacement and dedup only for duplicate/redelivery risk |
| delivery observation | commit-before-observation, no direct runtime state write, plus monotonic duplicate handling only for duplicate observations |
| status | exact stamped single-writer authority; causal-order/bounded-pending; every reachable facet; duplicate/monotonic conflict; pre-acceptance N/N+1 reservation and no eviction; covered-source/empty-pending marker; expiry and no resurrection; target-result crash before dispatch |
| durable retained value | crash at each assignment/output boundary and exact recovery lineage |
| Catalog v2 status | six set-equal state-to-view mappings, shared Query/Projection mapping, no fallback, lifecycle/cancellation/reason preservation, unknown-to-result refinement, and no proven-result regression |
| Catalog v1 persistence | every v1 state is authoritatively upcast before v2 `decide`; a missing cancellation-rejection reason or other required evidence quarantines the record instead of inventing a default |

Checkout has exactly ten source-output stop slots in canonical order: the initial acceptance `ReplyOutput` plus the nine command handles. Its regression suite covers reply-only, reply plus all nine commands, both orders for multiple stops, exact-handle duplicate/conflict, lifecycle/cancellation preservation, and capacity `10/11`. Participant result outputs use separate target-owned result-delivery slots and do not create an eleventh Checkout source slot. See Core §16 and [EXAMPLE-CROSSWALK.md](EXAMPLE-CROSSWALK.md). These fixtures are not inherited by a Ball whose closed paths do not contain those risks.
