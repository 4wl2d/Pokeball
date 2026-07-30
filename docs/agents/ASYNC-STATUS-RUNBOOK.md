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

A retry changes attempt identity, not the logical operation or handle. At root mutation ingress within the declared legal retry horizon, same key and fingerprint redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with its `BallInstanceId`, `CommitRevision`, materialized `OutputId`, `semanticHandle`, `sourceOrdinal`, payload, `OperationId`, and accepted Interaction artifact/fingerprint lineage unchanged; only `AttemptId` changes, and no second `decide`, revision, accepted frame, semantic output, command, or status source exists. While the covered idempotency record remains retained, same key with a different fingerprint is rejected before Intent construction as `BoundaryResponse(ValidationFailure(IdempotencyConflict))`. The accepted root frame and idempotency record are retained for at least the declared legal retry horizon; after the record is absent, no replay or conflict behavior is inferred. A reserved `OperationId` remains a candidate until root acceptance: root validation, admission, conflict, or `decide` rejection creates only its typed `BoundaryResponse`, never an operation, authoritative handle, output, status source, known row, or retention marker. A same-stack synchronous completion may use accepted call-scope position and needs no fabricated operation/transport lineage.

For a value used by a later Decision, retain its field-minimized value and add metadata only by subtrigger: source correlation when matching crosses call scope; version when rollout/persistence is observed; provenance across a trust boundary; consumer/deletion data when lifetime is not statically fixed. Inbox, outbox, participant ledger, and ambient history are not Decision inputs.

## 2. Operation facets

Materialize only facets that have reachable observations:

| Facet | Meanings when triggered |
|---|---|
| Dispatch | not sent, sent/might be sent, finite policy stopped |
| Acceptance | not accepted, accepted, downstream target rejected before acceptance, unknown |
| Business outcome | pending, succeeded, rejected, failed, cancelled, unknown |
| Cancellation | requested and accepted-before-start/in-progress/too-late/rejected-with-reason/unknown races |

Do not flatten independent meanings. `RejectedBeforeAcceptance` is a downstream target-acceptance facet only inside an already accepted source operation; it is not a root lifecycle for an unaccepted `OperationId`. A source commit does not imply dispatch; a result with accepted-operation proof implies acceptance; delivery stop proves neither failure nor non-execution; cancellation does not erase a legitimate result.

## 3. Canonical ingress

| Observation that exists | Input category |
|---|---|
| External start/cancel request | validated `Intent` |
| Private resource outcome | provenance-bound `Fact` |
| Accepted target Ball outcome | verified `ModuleResultPulse` constructed from an accepted `ModuleResultOutput` frame |
| Committed publication | `ObservedSignal` over one declared route |
| Runtime/route dispatch or ACK observation affecting state | typed trusted `ControlPulse` |

The Runtime/acceptor performs only applicable validation handoff, admission/reservation, mode-specific accepted-frame publication, retained-output scheduling, and conversion of verified mechanical observations into declared `ControlPulse` routes. It never owns business schema, policy, permission, read-result selection, or retry/fallback choice and never writes operation facets directly into Sovereign State. Duplicate identity, source tuple, bounded/redacted evidence, and conflict policy are required only for observation paths that exist.

Root idempotency remains at its real ingress stage. Within the declared legal retry horizon, a covered same-fingerprint retry is delivery of proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with all Core §9.6 semantic/frame identity preserved and only `AttemptId` changed, not a new uncommitted success, a status lookup, or a reconstructed Reply. While the idempotency record remains retained, a covered fingerprint mismatch is a pre-Intent validation conflict owned by the ingress/idempotency authority; it is never `DecisionRejected(BusinessRejection)` and never enters the Nucleus through a hidden ledger lookup. After the record is absent, no replay or conflict behavior is inferred.

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

`ModuleResultOutput` has the target-frame `sourceOrdinal`, carries `commandSource` and `payload: ModuleResult`, and uses `semanticHandle = commandSource.semanticHandle` only for correlation. It remains target-owned, counts against target output-count and canonical `maxOutputBytesPerDecision`, and, when retained, retried, or independently observable, occupies one target stop-eligible delivery/status slot. The `DecisionOutputs` dimension of the binding's `BoundedByteMeasure` covers the complete ordered `Decision.outputs` semantic representation: sequence structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload count; `nextState`, accepted-frame fields outside an output envelope, and later transport framing, compression, encryption, retry headers, and `AttemptId` do not. The result-delivery key is exactly the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`.

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

The carrier is neither Reply nor result and creates no accepted target Decision, revision, output, Resource action, operation record, status fact, or reconciliation fact. It exists only because its source command frame was already accepted: its verified source `ControlPulse` may refine that accepted operation's matching Step, but it cannot create a rejected root operation. Root validation, admission, or `decide` rejection instead returns its own `BoundaryResponse` and produces no carrier-to-status projection. Forged, tampered, missing, stale, or wrong-target provenance creates no trusted source `ControlPulse`, refusal facet, or compensation. An accepted result is required whenever the target accepted any state, operation/idempotency outcome, output, Resource action, durable/status/reconciliation fact, or accepted-operation evidence. Carrier/result conflict fails closed; dispatch failure never downgrades an accepted result; reclassification requires a new target protocol version and Assembly version pair.

A snapshot refusal on the accepted-result path is a same-state accepted `SnapshotDecision` published through the flattened `AcceptedSnapshotDecisionFrame<State> { commitRevision?, nextState, outputs }`, with revision advance and `ModuleResultOutput(Rejected(...))`; EventJournal records an `AcceptedEventCommit` whose `decision` is accepted `NoDomainChange`, with no independent `nextState`. Within the idempotency horizon, redelivery of the same effective identity and `commandSource` before a result has been accepted returns only verified ACK proof of the already accepted target frame and pending-result state. It does not wait for completion, invoke `decide`, increment revision, fabricate a pending `ModuleResult`, or repeat a Resource action. After result acceptance, redelivery transports the exact accepted target result frame with unchanged effective identity, `commandSource`, `resultSource`, target revision, `semanticHandle`, `sourceOrdinal`, and payload; only `AttemptId` changes. Same identity with a different command fingerprint or conflicting evidence fails closed.

For a same-stack round trip, source/root, target-command, and source-result Decisions consume causal levels `0/1/2`. Before source acceptance, the source reserves one level-1 alternative-completion slot. Exactly one mutually exclusive branch consumes it: an accepted target Decision consumes level 1 after separately reserving the level-2 result completion, while validation, admission, or target `decide` rejection before acceptance consumes no target level/frame/revision/output and atomically transfers level 1 to the source `decide(ControlPulse)` that applies the verified carrier. If level 1 cannot be reserved, the source is not accepted and nothing is dispatched. If level 2 cannot be reserved, the target is not accepted and `AdmissionFailure(CausalBudgetExceeded)` returns through the carrier. The carrier-handling Decision reserves any further synchronous outputs from the remaining budget. The invocation contributes `source -> target` to direct control; the return is not a reverse edge. Async handoff removes only that synchronous-invocation contribution, retains any separately present compile-time-import edge, and preserves causal scope, depth, and remaining budget; same-stack slot transfer is not inferred merely from asynchronous transport.

## 5. ACK, ambiguity, and retry

When ACK is separate, test the reachable orderings among source acceptance, dispatch, ACK, and result. A result may prove acceptance before an ACK. A timeout creates `AcceptanceUnknown` or `OutcomeUnknown` only after the declared possible-send/execution boundary; otherwise it is an ordinary attempt failure.

Duplicate target delivery is tested at both sides of result acceptance. Before result acceptance, only the verified ACK of the existing accepted target frame is legal; after result acceptance, only exact accepted-frame result replay is legal. ACK is not a fabricated pending business result, and waiting for a result is not an alternate duplicate contract.

For each active retrying failure mode, designate exactly one primary owner and record every retry layer in the authoritative route or operation policy:

| Failure mode | Layer | Primary owner for this failure mode | Stable logical key | Finite attempt/time budget | Idempotency horizon, if required | Disabled or bounded semantically transparent secondary proof |
|---|---|---|---|---|---|---|

Every non-primary layer is disabled or has both a finite bound and evidence that it is semantically transparent. Check cumulative SDK × adapter × runtime × Flow attempts; a `2 × 3 × 4` configuration with three primary-like owners fails even though each local cap is finite. A blind retry after possible irreversible execution requires preserved identity and a target/provider idempotency horizon; otherwise use status, reconciliation, or manual policy.

## 6. Cancellation, deadline, and timer

Cancellation is a new typed operation with its own handle when it can race execution. Its executor or target returns only the accepted-before-start, accepted-in-progress, too-late, rejected, or unknown outcomes that can actually occur. A timer path has stable timer identity and an explicit late policy; generation appears only for cancellation/rescheduling/replacement, while firing identity and deduplication appear only for duplicate/redelivery risk.

Test only reachable races, but include both orders. For the canonical Catalog semantics:

- in `Searching` or `OutcomeUnknown` with present `pendingSearch`, `SearchCancelled(intentOperationId)` is accepted only when `intentOperationId = state.operationId = pendingSearch.operationId`; mismatch returns exactly `Rejected(BusinessRejection.StaleSearchOperation(expectedOperationId = state.operationId, receivedOperationId = intentOperationId))`, encoded by Interaction as `BoundaryResponse(DecisionRejected(...))`, and accepts no Decision, State, revision, handle, Projection, cancellation Effect, or output; another source state uses its own closed rejection contract and borrows no handle;
- accepted-in-progress before/after a legitimate result converges without lifecycle regression;
- terminal cancellation itself proves acceptance, and a later weaker accepted-in-progress observation is a no-op;
- terminal result versus mutually exclusive terminal cancellation preserves the first accepted frame, while the contradictory second proof fails before acceptance with no state/output; and
- the conflict rule is symmetric in both arrival orders;
- `ProductSearchOutcomeUnknown` creates the explicit `OutcomeUnknown` lifecycle, while a later matching proven result may refine it and a later weaker unknown observation cannot regress `Ready` or `Failed`; and
- cancellation rejection retains its bounded reason through later compatible result/cancellation orderings.

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
| synchronous causal completion | preserve each queued Pulse's own trusted field-minimized Context across ordering/yield/resume; remaining budget reaches reservation/admission only; pre-reserve total budget; at N+1 the Decision fails before acceptance or the already accepted completion is retained in one bounded `RetainedContinuation`; never inherit root context, change the candidate Decision, drop, truncate, or reset budget |
| command/result round trip | routed and same-stack accepted tuples; exact fields and unnamed delivery key; all three pre-acceptance responses; accepted same-state refusal through flattened `AcceptedSnapshotDecisionFrame` or `AcceptedEventCommit(NoDomainChange)` with no Event `nextState`; carrier/result conflict; versioned reclassification; duplicate-before-result verified ACK only with no wait/redecision/revision/result/Resource action; duplicate-after-result exact accepted-frame replay with only a new attempt; command-fingerprint/evidence conflict; source/target forgery; ACK/result order; complete-sequence `DecisionOutputs` byte-measure `N/N+1`; crash/stop/late result; handle collision; source level-1 N/N+1; target level-2 N/N+1 for each carrier variant; no target frame/revision/output on pre-acceptance failure; atomic level-1 transfer and committed source `RejectedBeforeAcceptance + NotExpected`; carrier-handling output reservation; successful levels 0/1/2; async budget preservation without inferred slot transfer; exact-one target mapping; command-vs-read |
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
