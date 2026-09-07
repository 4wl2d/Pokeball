# Async and Status Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open only the sections activated by reachable detached work, late or duplicate result, separate ACK, ambiguous execution, retry, cancellation/deadline/timer, delivery observation, or operation status. A synchronous call-scope completion does not acquire the whole async model.

Every rule and test in this runbook is a projection of its named Core source clause. If a projection differs in modal force, trigger, owner, scope, failure behavior, reuse, or absence behavior, repair the projection in favor of Core.

**Task routing:** command/result identity, ingress, retry, and cancellation remain below; operation status and the triggered async test catalogue continue in [STATUS-AND-ASYNC-TESTS.md](STATUS-AND-ASYNC-TESTS.md).

## 1. Detached causal identity

When work outlives its call, can complete later, retry, reorder, be independently cancelled, recovered, reconciled, or observed, resolve:

```text
logical OperationId when an independent operation lifecycle exists
SemanticHandle for the addressable work
accepted source ordinal or equivalent sequence identity
source Ball/revision when that identity crosses call scope or is observed
OutputId and AttemptId only after materialization
result issuer and accepted-operation proof only across the corresponding trust/acceptance boundary
generation and stale policy only for late/reorderable generations
duplicate identity and retention only for duplicate/redelivery risk
causal depth/budget only when growing work needs those bounds
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

Resolve one target-owned closed command/result mapping and fixed refusal semantics. For an immediate same-build call, typed target and trusted call scope suffice: dispatch accepted source output outside pure `decide`, let the target serialize and accept its own change/result, and deliver the return through the source's serialized handler. `Changed(value)` and pre-acceptance `NotAccepted(reason)` may share one operation-specific return type; post-acceptance failure never becomes `NotAccepted`. No new carrier, source/result token, issuer type, or absent-tuple proof is required.

When the lifecycle requires portable evidence, the accepted round trip is:

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

For portable delivery, Assembly selects/binds the route and version pair and transports verified values. It cannot synthesize or modify `commandSource`, `resultSource`, issuer provenance, target payload, or refusal meaning. An immediate typed call is checked by actual target ownership and acceptance/return order, without reconstructing absent tuples.

Independently delivered pre-acceptance refusal uses:

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

A complete statically finite synchronous execution needs no causal-depth fields or reservation levels; include result/refusal/failure handlers in that structural bound. Real queues, retained completions, external work, or dynamically growing chains keep finite capacity and admission that cannot lose accepted work. Handoff and retry preserve any applicable causal budget. Invocation contributes `source -> target` to direct control; return is not a reverse edge. Async handoff removes only the synchronous-invocation contribution and retains any compile-time-import edge.

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

Moved to [Operation status](STATUS-AND-ASYNC-TESTS.md#7-operation-status).

## 8. Triggered tests

Moved to [Triggered tests](STATUS-AND-ASYNC-TESTS.md#8-triggered-tests).
