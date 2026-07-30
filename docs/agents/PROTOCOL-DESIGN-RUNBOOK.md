# Protocol Design Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation when a task changes a closed protocol, mutation form, accepted frame, output, lifecycle, or read path.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Design Runbook](DESIGN-RUNBOOK.md) · [Agent Pack index](README.md)

## 4. Closed protocol

Inventory only non-empty owned categories in typed source or one manifest:

```text
Pulse = Intent | Fact | ModuleCommandPulse | ModuleResultPulse
      | ObservedSignal | ControlPulse
Query -> ResultPayload
SemanticOutput = ProjectionOutput | ReplyOutput | EffectRequest
               | ModuleCommandRequest | ModuleResultOutput
               | SignalPublication | TimerRequest
```

Bare `ModuleResult`, top-level `TimerFired`, and `SignalOutput` are not union members or compatibility aliases. A Nucleus may import the exact declared target- or producer-owned Application Surfaces needed by its closed Query, Pulse, and Decision contracts. Imported `Query`/result, `ModuleCommand`/`ModuleResult`, and Signal payload types remain target/producer-owned and resolve through their declared dependency and Assembly binding; import transfers no ownership. Reject foreign mutable State, internals, private Resource adapters, caller-owned mirrors or redeclarations, protocol re-export, and any semantic contract synthesized by Interaction or Assembly. An omitted category is empty. Each `Query` maps to one target-owned `ResultPayload` that is total over its reachable post-admission semantic outcomes; it declares only applicable permitted, denied, redacted, or deliberately non-disclosing variants, and no boundary, caller, Assembly, runtime, or binding invents another. A `Query` remains pure; a stamp/result wrapper is materialized only for the consistency/status/boundary triggers in Core.

Output ownership and dispatch are explicit: `ProjectionOutput` and `ReplyOutput` return through Interaction for encoding/delivery; `EffectRequest` reaches the private Resource interpreter; `ModuleCommandRequest`, `ModuleResultOutput`, and `SignalPublication` follow their declared routes; and `TimerRequest` reaches its declared timer Resource/route. Assembly may bind an owned payload but cannot define, re-own, or synthesize it.

For each command path, the target owns one versioned `ModuleCommand -> ModuleResult` mapping and static refusal classification:

```text
ModuleCommandPulse {
  commandSource, effectiveProtocolIdentity, command, issuerProvenance
}
ModuleResultOutput {
  semanticHandle = commandSource.semanticHandle,
  sourceOrdinal, commandSource, payload: ModuleResult
}
ModuleResultPulse {
  commandSource, resultSource, effectiveProtocolIdentity, result, issuerProvenance
}
result-delivery key = (effectiveProtocolIdentity, commandSource, resultSource)
```

The trusted target boundary constructs the command Pulse only from a verified accepted source frame; target `decide` is the sole acceptance point. Only an accepted target Decision creates the result output, and the verified return route derives `resultSource` from that accepted target frame. Same-stack erasure preserves the exact tuples. `semanticHandle` equality is correlation, not re-export or ownership transfer. Within the idempotency horizon, duplicate-before-result returns only verified ACK proof of the existing accepted target frame and pending-result state; duplicate-after-result redelivers the exact accepted result frame. Neither invokes another Decision, increments revision, fabricates a result, or repeats a Resource action.

For a same-stack command, the source pre-reserves one level-1 alternative-completion slot. Accepted target `decide` consumes it and separately reserves the level-2 result completion. Validation, admission, or target `decide` rejection before acceptance consumes no target level/frame/revision/output and atomically transfers level 1 to the source carrier-handling `decide(ControlPulse)`. Missing level 1 prevents source acceptance/dispatch; missing level 2 prevents target acceptance and returns `AdmissionFailure(CausalBudgetExceeded)` through the carrier. Any further synchronous output from the carrier-handling Decision reserves normally from the remaining total budget.

A cross-authority `ReadDependency` resolves one target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Triggered-only version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, or status fields create no placeholders when absent. Pre-read validation/admission may return an existing `BoundaryResponse`; admitted `read(...) -> ReadResult` returns only its successfully evaluated target-owned result, whose payload closes every reachable post-admission permitted, denied, redacted, or deliberately non-disclosing outcome, and creates no accepted marker, Decision, revision, handle, or output. Caller/Assembly cannot alter or re-export payload, status fact, stamp, or meaning, or substitute `BusinessRejection`, exception, post-admission `BoundaryResponse`, or undeclared `NotFound`; multiple reads imply no atomic multi-source snapshot.

Specific path checks:

- external mutation or cancellation becomes an `Intent` after validation;
- root same-key/same-fingerprint retry redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with all semantic/frame identity unchanged and only a new `AttemptId`; different fingerprint is pre-Intent `ValidationFailure(IdempotencyConflict)`, never Nucleus `DecisionRejected`;
- private-resource outcome becomes a `Fact`;
- accepted target outcome becomes a verified `ModuleResultPulse`;
- post-commit mechanical observation becomes a typed `ControlPulse` only if it affects Sovereign State;
- pre-acceptance validation/admission/Decision rejection is a `BoundaryResponse`; for a command it travels only in verified `CommandRejectedBeforeAcceptance` and never as Reply or result;
- no output type is invented for an absent path.

Classify a predicate by its declared owner and inputs, not by whether it resembles a range, size, or shape check. Representation or a declared closed-protocol/type invariant independent of committed State, semantic Context, and business policy belongs to Interaction. A valid typed rule that depends on State/Context or is owned as a business choice belongs to the Nucleus. Promoting a fixed constraint into the protocol/type invariant changes the effective versioned protocol semantics; a binding cannot switch stages dynamically for one identity.

Classify failures and admitted read outcomes at their exact stage and never rewrite an earlier accepted fact:

| Stage | Legal projection |
|---|---|
| representation or declared closed-protocol/type validation before semantic admission | `BoundaryResponse(ValidationFailure)`; no `Intent`/`Query` reaches the Nucleus |
| valid typed State/Context/business rule during mutation | `Rejected(BusinessRejection)` before acceptance |
| valid typed State/Context/business rule during an admitted Query | one target-owned `ResultPayload` variant inside the successfully evaluated read result |
| fixed constraint deliberately promoted to a closed protocol/type invariant | `ValidationFailure` only under the corresponding new effective protocol identity; same-identity stage switching is invalid |
| admission before acceptance | `BoundaryResponse(AdmissionFailure(reason))`; verified command carrier on a command route |
| target Decision rejection before acceptance | `DecisionRejected(BusinessRejection)` inside the verified carrier |
| accepted target business outcome, including rejection | accepted `ModuleResultOutput`, then verified `ModuleResultPulse` |
| post-acceptance Resource/Execution-Gate failure, timeout, or unknown | provenance-bound `Fact` and later accepted target result/status path |
| delivery policy exhaustion | `DispatchStopped` for the exact accepted output/delivery identity |
| programming/invariant fault | runtime fault path; no business rejection, retry, or accepted-frame rewrite |

Every concrete fallible-admission profile/binding uses one finite closed `AdmissionFailure.reason` union and rejects an unknown discriminator or open string before trusted construction. Core's overload list is illustrative; a non-fallible profile has no empty reason union.

## 5. Decision and output

Verify identical explicit inputs produce a semantically equal result—equal in the fields that define the semantic value, independent of runtime representation—transition code performs no I/O, and capacity cannot rewrite the business choice. In particular, equal State/Pulse/valid per-Pulse Context/artifact produces the same candidate Decision under different remaining runtime causal budgets; only admission or continuation may differ. Every output field derives from committed State, current Pulse, or present Context.

Verify the exact selected accepted form. Snapshot profiles publish the flattened semantic tuple `AcceptedSnapshotDecisionFrame<State> { commitRevision?, nextState, outputs }`. EventJournal records `AcceptedEventCommit` containing `decision: EventDecision(EventMutation | NoDomainChange)` and reconstructs State through `evolve`; it never adds an independent Event `nextState`. A binding implements only the form chosen by its state profile.

If outputs exist, prove complete-batch acceptance and commit-before-dispatch. When numeric `maxOutputBytesPerDecision` is present, the `DecisionOutputs` dimension of the binding's immutable `BoundedByteMeasure` covers the complete ordered `Decision.outputs` semantic representation. Count sequence structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload; exclude `nextState`, accepted-frame fields outside an output envelope, and later transport framing, compression, encryption, retry headers, and `AttemptId`. Add a stable `SemanticHandle` only when work is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible. Immediate call-scope output uses accepted sequence position without a domain handle object.

When capacity can race acceptance or the complete candidate frame can exceed an effective cap, preflight or reserve state, outputs, and required continuation together. Admission failure returns before acceptance; it neither rewrites the business Decision nor discards already accepted work.

## 6. Lifecycle and reads

`Draining` rejects new logical mutations while continuing already accepted completion, cancellation, and status inputs. It serves every available declared Query/status Query from its committed authority. If a read is unavailable, the trusted boundary returns only an existing declared validation/admission response before `read`; once admitted, canonical `read` returns only the successfully evaluated `ReadResult`, whose target-owned payload still covers every reachable permitted, denied, redacted, or deliberately non-disclosing outcome. Neither path creates a Decision. A status authority has one query writer without acquiring the underlying command or business-fact authority.

A read-like operation uses the accepted command bridge only when accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation is required. Otherwise it is an ordinary `Query`/`ReadDependency` and creates no target Decision, revision, or output.

`OperationId` names an accepted root operation. Reservation supplies only a candidate input to a possible Decision. Root validation, admission, idempotency conflict, or `decide` rejection returns its typed `BoundaryResponse` and creates no operation, authoritative handle, output, known status row, or retention marker. Same-fingerprint replay instead redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with its accepted `BallInstanceId`, `CommitRevision`, materialized `OutputId`, `semanticHandle`, `sourceOrdinal`, payload, `OperationId`, and accepted Interaction artifact/fingerprint lineage unchanged and only a new `AttemptId`. `NotFound` for a rejected candidate is legal only under a committed namespace-covering snapshot and the ordinary materializer-absence barrier. A participant `CommandRejectedBeforeAcceptance` remains a Step acceptance facet of an already accepted source operation; it never becomes the rejected root lifecycle.
