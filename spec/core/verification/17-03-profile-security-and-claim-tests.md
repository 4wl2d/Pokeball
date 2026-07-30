# Core part — Verification: profile, security, and claim tests

[Core contents](../../pokeball-architecture-core.md) · [← Verification: boundary and architecture tests](17-02-boundary-and-architecture-tests.md) · [Practical checklist and anti-patterns →](18-checklist-and-antipatterns.md)

> Canonical part 17 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

### 17.6. Concurrent profile tests

For `BoundedConcurrent`, add the cases whose subpaths exist:

- out-of-order `Fact`/`ModuleResultPulse` inputs when completion can reorder;
- mailbox full/backpressure returns a closed `BoundaryResponse(AdmissionFailure(reason))` before acceptance;
- concurrent duplicate ingress when duplication is possible;
- concurrent same-identity target duplicate in both accepted-command/pending-result and accepted-result states, with one accepted target frame/result and only ACK/exact-frame replay;
- cancel versus completion when cancellation exists;
- timeout versus late result when deadlines/ambiguity exist;
- worker crash when workers exist;
- bounded causal chain when completion can re-enter a Decision;
- starvation and priority behavior, if claimed.

For `Transient` with an output-bearing path, fault injection verifies both sides of the single publication point:

- failure during reservation/materialization before publication makes neither state nor any output visible;
- failure immediately after atomic publication but before the first dispatch leaves the complete retained `AcceptedSnapshotDecisionFrame` or `AcceptedEventCommit` visible, from which the entire undelivered batch can continue under the profile policy;
- failure between dispatch of two outputs neither rolls back state/the first output nor loses the retained record of the second;
- no separate fallible state-publication-then-enqueue window exists.

### 17.7. Durable-state profile tests

For `SnapshotOutbox`/`EventJournal`, select crash points from the paths that exist:

```text
before transaction
inside transaction
commit acknowledged but response lost
root accepted ReplyOutput committed but response lost     # exact root replay path
before dispatch                                      # output path
request sent but ACK lost                            # ACK path
after target acceptance before source update         # detached result path
after target acceptance before target result exists  # pending-result duplicate ACK path
after accepted ModuleResultOutput before result dispatch # target result path
target result delivery exhausted                     # target stop tuple path
post-commit observation before/after ControlPulse    # delivery-observation path
before/after result commit                           # result path
recovery with pending outbox                         # output path
recovery after accepted NoDomainChange               # EventJournal path
delivery exhaustion persisted as DispatchStopped     # terminal delivery/status path
initial ReplyOutput exhaustion in status authority   # detached reply/status path
```

When a durable operation-status materializer exists, fault/race tests also cover an observation before its causal source, crash with bounded pending before/after source application, duplicate and conflicting same-key evidence, independent lifecycle/cancellation/result/stop facet orders, `N/N+1` reservation before source acceptance, pressure after acceptance with no eviction/truncation, marker attempted while a source position lags or pending is nonempty, committed marker before row absence, covered late duplicate, marker-horizon expiry, and no resurrection. A reserved candidate ID whose root validation/admission/Decision rejected has no accepted status source to recover and cannot be synthesized into a known row or retention marker. Crash after accepted target `ModuleResultOutput` but before result dispatch preserves the target accepted-result source; target route exhaustion adds target `DispatchStopped`, while source remains Pending/Unknown until verified result or reconciliation.

Every delivery, durability, recovery, receipt, acceptance, once-only, RPO, or RTO claim fixture names its exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees. Paired negatives prove that source durability alone and retained pending work alone establish neither target receipt/acceptance nor any stronger downstream outcome.

The canonical Checkout example additionally tests:

```text
Checkout recovery after accepted CheckoutStarted/CartLocked before InventoryReserved
Checkout recovery after accepted CheckoutStarted preserves exact equality of retained ingress fingerprint and atomic idempotency record
Checkout recovery after accepted InventoryReserved before PaymentCaptured
Checkout recovery after direct or reconciled PaymentCaptured before Order result
Checkout recovery after Order rejection before/between Refund, Release and Unlock
Checkout recovery after accepted StillUnknown preserves the terminal NeedsManualReconciliation frame and creates no new status generation
```

The canonical Catalog v2 example additionally tests persisted-state rollout:

```text
each v1 Idle/Searching/Ready/Failed/Cancelled variant -> exact authoritative v2 upcast
v1 CancellationRejected + authoritative bounded reason evidence -> v2 CancellationRejected(reason)
v1 CancellationRejected without that evidence -> quarantine/manual remediation
pending v1 output or Fact retains its v1 protocol/artifact meaning and is not decoded as v2
no schema-v1 record enters normal v2 decide and no missing field receives a null/default substitute
```

Minimum properties are likewise triggered. The Checkout-specific bullets below are evidence for §16, not a template for an unrelated durable Ball:

- no dispatch before source commit when outputs exist;
- no state without its corresponding durable output record when outputs exist;
- no outbox regeneration by rerunning transition when an outbox exists;
- accepted `NoDomainChange` restores the same revision/acceptance marker/output batch when EventJournal is selected;
- crash-before-dispatch and delivery-observation rules apply when those output/ACK/ambiguity paths exist;
- crash after accepted target `ModuleResultOutput` preserves that target frame under the selected profile; replay dispatch uses the same result-delivery tuple and never reconstructs business meaning in Assembly;
- crash after target command-frame acceptance but before result creation preserves one pending target operation; a same-identity duplicate returns verified ACK proof only and never reruns the target Decision/Effect, while a later accepted result is replayed from its exact frame;
- target result-route exhaustion records `DispatchStopped` with `(effectiveProtocolIdentity, commandSource, resultSource)` and leaves Checkout Pending/Unknown until a verified result or reconciliation; a late result may refine Checkout without erasing the target stop;
- target output-byte/count and conditional stop-slot `N/N+1` admission are tested independently of Checkout's ten source-output stop slots;
- the initial reply stop and command-step stops are materialized over one bounded `SemanticHandle` universe; a reply observation does not masquerade as a command Pulse and does not change `CheckoutState`;
- status materialization does not lose a stop that arrived before its causal workflow record and does not replace a retention marker with a late duplicate after the covered source position;
- SnapshotOutbox/EventJournal restore exact retained M/S/I/P values and provenance without rerunning `decide` or reading runtime/participant history; Transient explicitly does not promise this after process loss;
- after a crash, `InventoryReserved(I)` creates the exact Capture from retained M + S + a current valid action grant, direct/reconciled `PaymentCaptured(P)` creates the exact Confirm from retained S/I + P, and order rejection creates Refund/Release/Unlock only for retained P/I/cart;
- the same authority action with a conflicting value/version/proof, premature cleanup, and wrong-target compensation fail closed; cleanup after the last terminal consumer survives repeated recovery;
- the complete retained C/RS/RI/RP frame satisfies exact `maxStateBytes = N` under one State-dimension measure tuple; a semantically equal alternate retained representation maps to the same measured sequence, while `N+1` rejects the entire Decision before State/revision/output acceptance without truncation;
- active Checkout v1 state without authoritative M/S/I/P migration evidence receives no synthesized defaults and does not enter normal v2 `decide`; the binding proves migration or quarantine/manual routing;
- a schema-v2 Checkout record from a prior transition artifact does not enter normal recovery based only on matching shape: the retained ingress fingerprint is verified against the authoritative accepted record/declared migration, otherwise the operation is quarantined; the normal Nucleus does not read the ledger;
- duplicate root ingress after accepted reply loss redelivers the exact original accepted `ReplyOutput` frame with a new `AttemptId`; the idempotency record and frame share the declared retry horizon, and a fingerprint conflict remains a pre-Intent validation failure with no semantic record;
- accepted Checkout `StillUnknown` recovery restores the same terminal `NeedsManualReconciliation`, original capture/status evidence, and empty output sequence; duplicate delivery creates no second Decision/handle and normal v1 cannot auto-reopen it;
- a stale owner cannot commit when ownership is movable;
- ambiguous external outcome remains unknown until reconciled when that risk exists.

### 17.8. Security tests

For each present actor, privileged, capability, interpreter, secret, or abuse trigger, select only the applicable tests below. The Capture/Cancel/Refund/Release/Unlock entries are Checkout example evidence only:

- actor context from a valid approved issuer with verified authenticity/integrity becomes trusted Decision input but grants no authority by itself;
- valid actor-dependent Query/status context from an approved issuer selects only the namespace/result permitted by the Ball's pure semantic read; typed cross-Ball and statically proven same-stack forms are equivalent;
- a secured Query fixture admits the same valid approved-issuer actor context into each reachable target-owned `ResultPayload` case: permitted data, explicit denial, redaction, and a deliberately declared non-disclosing variant; the pure Policy Gate selects the case and every admitted execution returns `ReadResult` without a Decision, revision, output, exception, `BusinessRejection`, or post-admission `BoundaryResponse`;
- a wrong-provenance, wrong-issuer, malformed, or inadmissible secured Query fails at the declared pre-read boundary and is never converted into semantic `NotFound`; a valid-context denial may be non-disclosing only when that exact target-owned payload variant is declared, and neither the boundary, caller, nor Assembly may invent it;
- removing any reachable permitted/denied/redacted/non-disclosing payload variant fails closed-protocol exhaustiveness, while an actor-independent or unconditionally permitted Query contains no unreachable denial placeholder;
- forged/tampered actor evidence, wrong or unapproved issuer/realm, and missing, stale, or unverifiable required evidence fail closed before any actor-dependent Decision or Query/status result, including a typed unprivileged inter-Ball command/read;
- an otherwise equivalent actor-independent Decision or read creates no actor context, issuer row, authentication artifact, or actor-specific test fixture;
- forged context/proof, wrong target/action/object/operation/constrained field/version, and missing capability fail at the owning trusted boundary or Execution Gate without an ambient credential or new business decision;
- a Payment Nucleus business-policy rejection is an accepted target result, while a post-acceptance Execution-Gate rejection is a bound technical Fact/accepted result and neither is the pre-acceptance carrier;
- same-stack/generated erasure preserves the exact verified context, accepted cause/result provenance, gate ordering, and role call graph;
- hidden service locators, mutable global business state, foundation policy/route selection, and foundation-mediated role communication fail architecture/security scans;
- missing/expired/wrong-audience grant;
- forged/tampered grant and untrusted/wrong issuer;
- substitution of amount/currency/object/operation or another constrained command field after authorization;
- a constrained, duplicate-permitting privileged action binds every action-contract constrained field and the applicable idempotency identity at the Execution Gate; a mismatch or conflicting same-identity payload fails closed;
- a privileged cross-authority Grant whose action contract defines no constrained target or payload field and whose execution cannot duplicate still verifies approved issuer, authenticity/integrity, actor context, audience, action, object, operation, and validity, but omits `constraints` and any additional replay/idempotency artifact; immutable accepted-output payload binding remains its separate §11.3 check when that trigger exists;
- wrong retained actor binding, payment method, action handle, context version, quote version or grant validity across async delay/recovery;
- a rotated/expired actor-context reference requires a current `subjectBindingProof` for the same stable subject/issuer/realm; possession of the digest/ID without proof does not authorize the action;
- Capture/Cancel/Refund/Release/Unlock receive separate current action grants; the capture grant is not reused, and missing fallback authorization leads to a typed residual/manual outcome with no privileged output;
- raw grant/session/principal is absent from live workflow state, status, Projection, and ordinary telemetry; the exact committed grant remains only in the protected/redacted output record until the declared dispatch/audit horizon;
- insufficient actor assurance;
- revoked capability;
- target object version changed;
- endpoint/credential scope too broad;
- restricted capability boundary succeeds for the declared bounded operation class; an administrator credential plus a local wrapper/check fails;
- capability-rooted filesystem resolution succeeds and raw traversal fails; the other applicable parameterized, structured, and context-encoded sinks have paired injection negatives;
- a trusted `InProcess + Hardened` binding with an ordinary scoped external credential and no hostile-component-containment or separate-principal requirement remains InProcess, while a credential that must be isolated from less-trusted co-resident code or a high-privilege credential requiring its own security principal selects `Isolated`;
- SSRF/path traversal/shell/raw SQL attempts;
- complete secret-flow policy covers each reachable State, any output, persistence, serialization, log, and telemetry path at its exact scope; a log-only policy fails when any non-log path is reachable;
- authenticated resource exhaustion.

### 17.9. Claim records and benchmarks

This subsection is claim-triggered. Every claim fixture first materializes the complete `ClaimRecord`: exact named boundary, binding and scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees. Field-deletion, wrong-boundary/scope, stale-evidence, source-durability-only, and retained-pending-only negatives prohibit the claim rather than inferring a stronger guarantee.

When a performance claim is made, the report additionally records:

```text
hardware and OS
language/runtime/compiler version
optimization flags
allocator/GC mode
payload sizes and distribution
warmup and sample method
instrumentation overhead
profile combination
latency distribution
allocation/copy counts
```

As a claim-evidence projection of the marked source clause for `PBA-41`, such a benchmark compares the following baselines where applicable:

```text
direct hand-written call
Pokeball Inline binding
concurrent binding where applicable
existing framework baseline
```

If the claim compares numeric `maxTransitionSteps` counts across bindings, the report records `meterIdentity`, `meterVersion`, `transitionArtifactVersion`, and `unitDefinition` for every compared count and proves all four values equal before making the numeric comparison. Otherwise the counts remain explicitly incomparable and may be reported only as separate binding-local observations.

Do not claim "zero overhead" based on one microbenchmark without a realistic payload and build settings.

### 17.10. Observability

Recommended causal fields for paths that materialize them:

```text
ballType
ballInstanceId
commitRevision
pulseType
semanticHandle
operationId
outputId when available
stateRevisionBefore/After
attemptId
outcome
latency
traceId
```

An operational trace, security audit, and exact replay data are distinct artifacts. A redacted trace does not guarantee replay.

---
