# Checkout Example Crosswalk

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation only for an activated property analogous to the canonical Checkout Flow fixture. It supplies no project default.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Example Crosswalk](EXAMPLE-CROSSWALK.md) · [Agent Pack index](README.md)

## Checkout Flow Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| stateful multi-authority coordination | §§16.1, 16.14 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| participant-owned Application Surface imports | §§16.1, 17.5 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| durable ingress/idempotency | §16.2 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| explicit Interaction artifact version and cross-Decision retained values | §§16.2–16.5, 16.6–16.9 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| delayed privileged actions | §16.4 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| unknown/reconciliation/compensation | §§16.8, 16.10 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| post-commit delivery observation | §16.12 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| durable operation status/multi-stop | §16.13 | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) |

Checkout has four participant authorities—Cart, Inventory, Payment and Order—and nine target-owned command mappings/round-trip routes. Four `FlowParticipation` relations describe coordination. These counts are visible facts of this example, not mandatory Core limits. Auth supplies trusted context and Checkout owns the Flow; neither is a participant. Types and wiring may generate the manifest and route view.

Canonical retained-value trace:

```text
CheckoutStarted + DecisionContext(actorContext=A, artifactVersion=IV)
  retains M + ingress fingerprint + IV + actor binding
CartLocked retains S
InventoryReserved retains I -> Capture(S, M, current grant)
PaymentCaptured retains P -> Confirm(S, I, P)
Order rejection -> Refund(P) + Release(I) + Unlock(original cart/lock)
```

The initial trusted binding verifies and bounds `IV` as the exact Interaction fingerprint-artifact version selected for this ingress. Checkout retains `interactionArtifactVersion = IV`; it is distinct from the Nucleus `transitionArtifactVersion` and never comes from an absent generic Context field or ambient build state. Missing, stale, mismatched, or untrusted `IV` rejects before acceptance. Within the declared legal retry horizon, a covered same-key/fingerprint retry redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with its `BallInstanceId`, `CommitRevision`, materialized `OutputId`, `semanticHandle`, `sourceOrdinal`, payload, `OperationId`, and retained `IV` unchanged; only `AttemptId` changes, and no second Checkout Decision, accepted frame, output, command, or status source exists. While the covered idempotency record remains retained, same key with a different fingerprint returns pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, never `DecisionRejected`, and creates no Checkout operation mapping, Decision, revision, handle, Reply, output, or status source. The accepted frame and record remain retained for at least the retry horizon; after the record is absent, neither replay nor conflict behavior is inferred. Recovery, migration, and quarantine use the originally retained lineage.

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

A participant call dispatched from an accepted Checkout output preserves target ownership, refusal stage and serialized Flow completion. A complete finite local execution needs no reservation levels. Checkout's independently delivered, retained, retried and external paths still require portable causal proof, finite capacity and no accepted loss; changing transport never resets a bound that applies to that work.

Payment demonstrates the Dual Gate and command-vs-read boundary. The trusted target boundary verifies the accepted command source, effective protocol identity, action-scoped actor/grant context, bounds, and provenance before constructing `ModuleCommandPulse`. The Payment Nucleus owns the business schema and Policy Gate and alone accepts a business Decision/result. Immediately before provider execution, the Execution Gate verifies the current proof, the minimum technical authority for the bounded provider operation class at a real capability boundary, constraints, version, freshness/revocation, endpoint, quota, and applicable safe sink without inventing a business decision. Pre-`decide` validation/admission/provenance refusal uses the carrier; an accepted Nucleus refusal uses accepted `ModuleResultOutput(Rejected(...))`; post-acceptance provider/Execution-Gate failure remains Resource/result/status evidence and cannot become the carrier.

For duplicate Payment command delivery within the idempotency horizon, the observation depends on whether a target result frame already exists. Before result acceptance—for example, after `Payment.Capture` accepted its provider `EffectRequest` but before its result-producing `Fact`—Payment returns only verified ACK proof of the original accepted target frame and pending result state; it does not wait, run another Decision, revise state, fabricate a pending result, or execute the provider again. After result acceptance, it redelivers that exact accepted result frame with unchanged effective identity, `commandSource`, `resultSource`, target revision, `semanticHandle`, `sourceOrdinal`, and payload; only `AttemptId` changes. A different command fingerprint or conflicting evidence fails closed.

`Payment.GetOperationStatus` remains a command because Checkout requires accepted provenance, stable step identity, idempotent replay, status, and reconciliation. Its same-state snapshot acceptance advances Payment revision and emits an accepted result through the flattened `AcceptedSnapshotDecisionFrame<State> { commitRevision?, nextState, outputs }`; EventJournal records `AcceptedEventCommit` with `decision = NoDomainChange { outputs = [...] }` and no independent `nextState`. Duplicate delivery within the horizon returns the prior result and target proof without another Decision/revision. An ordinary non-recording lookup uses `ReadDependency`/`Query` instead.

`Captured` and `DefinitelyNotCaptured` follow their declared automatic transitions. Accepted `StillUnknown` exhausts Checkout v1's sole normal `payment-status` reconciliation slot: Checkout preserves the original capture acceptance/outcome facets and exact status evidence, advances that slot through `Accepted + Succeeded`, accepts one source Decision with `outputs = []`, and sets `terminalOutcome = NeedsManualReconciliation`. It creates no second status generation, timer, command, Effect, compensation, or reopen path. Duplicate/redelivery creates no second Decision, revision, handle, or output. Normal v1 does not silently rewrite the terminal frame; later proof requires a separately declared manual/recovery artifact.

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

Checkout creates `CheckoutKnownStatus` only from an accepted Checkout operation record. A reserved root candidate followed by validation, admission, or Checkout `decide` rejection produces only the typed root `BoundaryResponse`: no authoritative `OperationId`, handle, output, status row, or retention marker exists. A namespace-covering committed status snapshot may therefore return `CheckoutNotFound` for that candidate under the ordinary materializer-absence barrier. By contrast, participant command nonacceptance remains the matching `Step.acceptance = RejectedBeforeAcceptance` facet of an already accepted Checkout operation; it is not a root lifecycle.

The status materializer reserves all reachable operation/pending/facet/marker/stop capacity before source acceptance, never evicts accepted facts, applies causal sources or bounded pending, merges identical duplicates idempotently, fails closed on conflicting same-key evidence, and creates a retention marker only after covered source positions and empty pending. Tests cover root reserved-ID rejection versus acceptance, participant carrier isolation, reply-only, reply plus all nine commands, both orders of two stops, duplicate/conflict, lifecycle/cancellation preservation, marker/pending races, no resurrection, and `10/11` capacity.

A participant's accepted `ModuleResultOutput` uses a separate participant-owned target stop slot keyed by the result-delivery tuple. Crash before result dispatch does not erase that accepted result under the selected target profile. Target route exhaustion records target `DispatchStopped` but does not add an eleventh Checkout source slot or set a Checkout facet; Checkout remains `Pending`/unknown until verified result or reconciliation, and a late result may refine it without erasing the target stop fact.

Those are Checkout's reachable paths, not universal Ball requirements.
