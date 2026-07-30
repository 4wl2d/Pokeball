# Core part — Verification: transition and property tests

[Core contents](../../pokeball-architecture-core.md) · [← Checkout Flow: recovery and status](../examples/16-03-checkout-recovery-and-status.md) · [Verification: boundary and architecture tests →](17-02-boundary-and-architecture-tests.md)

> Canonical part 15 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 17. Testing, review, and operational verification

A project does not copy every suite in this section into every Ball. Its test plan is derived as:

```text
always-applicable invariant tests
+ tests for reachable path/risk triggers
+ local policy-delta tests
+ evidence suites for claims actually made
```

An exact shared parser, acceptance primitive, profile binding, limit policy, capability adapter, or foundation rule may be tested once at its declared scope. A Ball tests its semantic wiring and local delta. A conformance review resolves all references and proves absent triggers once from the closed inventory; routine implementation work needs no repeated evidence matrix or `N/A` rows.

### 17.1. Transition tests

The Nucleus is tested without mocks of external framework objects:

```text
Given committed State
And Pulse
And DecisionContext
Expect Accepted(mode-specific Decision) or Rejected(BusinessRejection)
Expect next State for SnapshotDecision or events/NoDomainChange for EventDecision
Expect ordered SemanticOutputs
```

Base transition tests cover:

- every protocol variant;
- state invariants;
- identical explicit inputs produce the same semantic result;
- equal committed State, current Pulse, valid per-Pulse Context, and transition artifact produce the same candidate Decision under different remaining runtime causal budgets or execution quanta; only reservation/admission/continuation may differ;
- each Inline deque/continuation item preserves its own Pulse-to-Context association across ordering and yield/resume; a later `Fact` or `ControlPulse` receives `Unit` when no context field is triggered and never inherits a root actor, grant, reserved ID, time, or validity field by variable reuse;
- rejected input changes no state;
- the selected state profile exposes exactly its §3.3 mutation function and accepted frame: Snapshot rejects an Event decision, EventJournal rejects an independent `nextState`, and both preserve the same `Accepted | Rejected(BusinessRejection)` distinction without a mandatory runtime union;
- malformed representation or violation of a declared closed protocol/type invariant returns `ValidationFailure` before `Intent` construction and invokes no `decide`; a value that satisfies that type but violates a State- or semantic-Context-owned rule reaches the Nucleus and returns `Rejected(BusinessRejection)` with no accepted frame;
- if a fixed business constraint is deliberately promoted into a closed protocol/type invariant, the changed stage is part of a new versioned protocol identity; switching the same protocol version between `ValidationFailure` and `BusinessRejection` fails compatibility evidence;
- applicable effective-bound overflow rejects the whole Decision;
- when numeric transition metering is selected: equal canonical State/Pulse/valid Context/transition artifact version/binding/meter identity/version produces the same count; one `decide` starts at zero, charges non-negative integral units monotonically without reset, passes at exact `N`, and accepts no frame/State/revision/output/dispatch on unit `N+1`; a later `decide` starts a new scope and unlike meter identity/version, transition artifact version, or unit-definition tuples are not compared;
- when numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` is selected without static proof: one immutable `BoundedByteMeasure` tuple resolves the dimension, identity, version, exact representation, limit name, and maximum; equal canonical values under one tuple count equally, alternate representations map deterministically, representation erasure preserves the count, and different tuples are incomparable;
- input-byte fixtures select raw or normalized stage and exact metadata/Context inclusion, pass at exact `N`, and fail before trusted semantic acceptance/`decide` at `N+1`; State-byte fixtures count the complete candidate next State with explicit semantic metadata and no heap/storage/transport mechanics, pass at `N`, and reject the whole Decision before State/revision/output acceptance at `N+1`; output fixtures retain the complete ordered sequence inclusion/exclusion and whole-Decision boundary;
- no output is visible before acceptance;
- invalid present `DecisionContext` fields;
- terminal states that exist.

Triggered tests are added only for their reachable paths: stale/late results, duplicate input, cancellation, deadline, detached output identity, synchronous causal completion, delivery observations, status, grants, and retained cross-transition values. For the Catalog and Checkout examples in §§15–16, those triggers produce the following advanced fixtures:

- stale results;
- duplicate inputs;
- root idempotency fixtures retain the original accepted reply frame through the legal retry horizon: same key/fingerprint redelivers its exact `BallInstanceId`/revision/handle/ordinal/`RequestAccepted(operationId)`/fingerprint/artifact lineage with only a new `AttemptId` and no `decide`/revision/output; same key with a different fingerprint returns pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, changes nothing, and leaves the prior operation intact; concurrent first acceptance, crash after acceptance before reply delivery, retry at/after the retention boundary, and same-key races use that one algebra;
- Catalog `ProductSelected` from each of `Idle`, `Searching`, `Ready`, `Failed`, `OutcomeUnknown`, and `Cancelled`: every state-specific field/facet is preserved, only the accepted revision advances, and exactly one `ProductSelectionConfirmed(productId)` `SignalPublication` appears at `sourceOrdinal = 0` with no second output;
- the six explicit, fallback-free `CatalogState -> CatalogView` cases: `Idle -> CatalogIdle` and each of the five search states -> one composite `CatalogSearchStatus` retaining lifecycle/result, cancellation, and any rejection reason;
- every cancellation outcome and applicable observation order: `AcceptedInProgress -> ProductsFound|ProductSearchFailed` and `ProductsFound|ProductSearchFailed -> AcceptedInProgress` converge on the same `Ready|Failed(AcceptedInProgress)`; rejection/too-late/cancellation-unknown before or after result converges without losing result, facet, or rejection reason; `AcceptedInProgress -> ProductSearchCancelled` and `ProductSearchCancelled -> late AcceptedInProgress` converge on `Cancelled(AcceptedInProgress)` without duplicate output; mutually exclusive terminal result/cancellation proofs in both orders retain the first frame, while the second proof accepts no Decision/state/output and follows the invariant/provenance fault policy;
- stale Catalog cancellation admission: after A is replaced by B, `SearchCancelled(A)` against B's State/pending handle returns bounded `Rejected(BusinessRejection.StaleSearchOperation(B,A))` and accepts no Decision/revision/handle/Projection/Effect; exact B matches and follows the existing cancellation matrix; duplicate, crash/recovery, and every case prove that stale A never targets B;
- `Searching -> OutcomeUnknown -> Ready|Failed` refinement retains cancellation; a weaker or duplicate search-unknown observation after `Ready`, `Failed`, or proven `Cancelled` is a no-op and cannot regress proof;
- deadline transitions;
- output-limit overflow;
- complete byte-measure fixtures use the same dimension/identity/version/representation/limit tuple: raw versus normalized input and UTF-8 versus UTF-16-like retained State cannot be compared until each maps to the selected measure; exact input `N` reaches trusted semantic construction while `N+1` invokes no `decide`; exact complete-next-State `N` may accept while `N+1` accepts no State/revision/output; one output and two-output aggregation pass at exact `maxOutputBytesPerDecision = N`, while adding one measured byte, required envelope field, or second output at `N+1` rejects the complete Decision with no partial dispatch; transport/storage compression, encryption, retry headers, allocator layout, and valid static proofs follow §8.3;
- exact `N+1` synchronous completion trace at causal-budget exhaustion, with no dropped Fact or hidden continuation;
- cumulative-fan-out fixtures over one root scope: a two-level tree sums every accepted output-to-route/consumer traversal including terminal leaves; a diamond counts two converging route traversals rather than one unique authority; mixed output kinds use the same branch unit; co-reachable branches sum while mutually exclusive alternatives share the maximum reservation; equivalent retry/redelivery adds no unit, a new accepted source tuple does; async handoff preserves the remaining scope/budget and only a declared causally independent root starts fresh; exact `N` passes and the first `N+1` branch rejects the complete Decision with `AdmissionFailure(CausalBudgetExceeded)` and no partial dispatch;
- full operation-facet invariants, including the result-proof transition to `Accepted`;
- routed and representation-erased same-stack command success preserve the identical accepted source `commandSource`, target `resultSource`, effective protocol identity, target-owned payload, and issuer provenance; the target boundary alone constructs `ModuleCommandPulse`, target `decide` is the sole acceptance point, and only an accepted target frame can produce `ModuleResultPulse`;
- synthetic or modified source/target tokens, wrong protocol identity, wrong target-owned payload, and forged/tampered/missing/wrong issuer provenance create no trusted target/source Pulse or Decision;
- each of `ValidationFailure`, `AdmissionFailure`, and `DecisionRejected(BusinessRejection)` traverses the verified pre-acceptance carrier; invalid carrier provenance creates no source `ControlPulse`, refusal facet, or compensation;
- an accepted snapshot same-state target rejection increments revision and emits `ModuleResultOutput(Rejected(...))`; `EventJournal` uses accepted `NoDomainChange`; redelivery of the same command identity within its horizon returns the prior accepted result without another Decision/revision/Resource action;
- carrier `DecisionRejected(BusinessRejection)` projects `RejectedBeforeAcceptance + NotExpected`, whereas accepted target `Rejected(...)` projects `Accepted + Rejected`; carrier/result conflict in either arrival order fails closed;
- every refusal is accepted only on the statically declared path; reclassification under the old protocol/version pair is rejected, while a new target protocol version and Assembly pair may declare the changed path;
- ACK-before-result, result-before-ACK, duplicate result, late result, wrong-provenance result, and semantic-handle collision preserve ACK/result separation and never select truth by arrival order;
- target-command duplicate timing covers concurrent duplicate and crash after command-frame acceptance but before result creation: same identity/fingerprint returns only verified ACK proof of that accepted frame/pending result with no wait, Decision, revision, result, Effect, or second Resource action; crash before result dispatch preserves the exact accepted result frame, whose redelivery keeps `commandSource`, `resultSource`, effective protocol identity, target revision, handle/ordinal, and payload while changing only `AttemptId`; different fingerprints or conflicting ACK/result evidence fail closed;
- target result output-count/byte limits and conditional stop-slot limits pass at `N` and reject the target Decision at `N+1`; crash after target acceptance preserves the result under the selected profile, and result-route exhaustion records target `DispatchStopped` by `(effectiveProtocolIdentity, commandSource, resultSource)` without changing source facets;
- every triggered status namespace has one committed revisioned single-writer authority; its physical table/process/co-location varies without changing the same causal materializer semantics;
- root validation, admission, and Decision rejection after candidate `OperationId` reservation return only their typed `BoundaryResponse` and create no operation, known status row, retention marker, handle, output, or acceptance reply; a covered later lookup of the candidate value returns `NotFound`, retry invents no root operation, and participant `CommandRejectedBeforeAcceptance` still changes only the matching Step facet of an already accepted source operation;
- a lifecycle, cancellation, accepted-result, or delivery-stop observation arriving before its causal operation/source record enters bounded pending rather than becoming `NotFound`, a partial row, or a dropped fact; arrival of the source applies all now-ready pending evidence in one committed revision;
- equivalent status evidence is idempotent, compatible facets merge losslessly, weaker evidence cannot regress proof, and nonequivalent same-causal-key evidence fails closed without arrival-order overwrite;
- status operation/pending/facet/marker/stop capacity passes at `N` and prevents source acceptance at `N+1`; after acceptance, materializer pressure never evicts, truncates, or drops the accepted fact;
- retention-marker tests cover every declared source position/horizon plus empty pending, marker commit before absence, observation-before-marker and marker-before-observation orders, marker-horizon expiry, covered late duplicate, and no resurrection;
- same-stack command fixtures cover: unavailable level 1 prevents source acceptance and dispatch; available level 1 with unavailable level 2 returns each of the applicable `ValidationFailure`, `AdmissionFailure`, and `DecisionRejected(BusinessRejection)` carrier variants without an accepted target frame or level, atomically transfers the level-1 alternative-completion slot, and commits the source `RejectedBeforeAcceptance + NotExpected` facet; success consumes source/target/source-result levels `0/1/2` with one `source -> target` synchronous-invocation contribution and a separate level-2 result reservation; the target/carrier branches never both consume level 1; carrier handling neither drops evidence nor resets/exceeds the budget or creates undeclared deferral; a carrier Decision with further synchronous outputs reserves their later completions normally; async handoff removes only that synchronous-invocation contribution, retains any separately present compile-time-import edge, preserves causal scope/depth/budget, and does not infer the same-stack transfer;
- all nine Checkout command rows in §16.1 exercise their accepted normal-business-refusal results, including the non-carrier `InventoryReservationRejected`, original-Capture meaning of `DefinitelyNotCaptured(RejectedBeforeAcceptance, ...)`, accepted post-capture Order refusal, and Flow-owned `RejectedBeforeExternalCommitment` distinction;
- Checkout's one `payment-status` reconciliation slot consumes accepted `StillUnknown` into `NeedsManualReconciliation` with the original capture facets/status evidence retained and an empty output sequence; exact redelivery creates no Decision/revision/handle, crash/recovery reaches the same terminal frame, no automatic second status/Effect/compensation exists, and later evidence is rejected unless a separately declared manual/recovery artifact owns it;
- the Payment trace constructs verified `ModuleCommandPulse` and field-minimized `DecisionContext` only at the trusted target boundary, leaves business permission to the Payment Nucleus Policy Gate, and accepts the target-owned operation/result through canonical Decisions;
- Payment pre-`decide` validation/admission/provenance refusal uses only the declared carrier; an accepted Nucleus business refusal emits accepted `ModuleResultOutput(Rejected(...))`; a post-acceptance Execution-Gate/provider failure returns through a bound `Fact` and accepted target result and never rolls back or downgrades target acceptance;
- every declared post-commit delivery `ControlPulse`: exact committed source tuple/provenance, dispatch, accepted/rejected ACK, ambiguity, and terminal stop; an exact duplicate is idempotent, a stale/weaker observation does not regress proof, and conflicting evidence is not overwritten by arrival order;
- for every outgoing payload field, exact derivation from committed State, the current Pulse, or a declared versioned `DecisionContext`; a prior Intent/result, outbox, participant/runtime ledger, and ambient memory are not read;
- Checkout initial acceptance: the retained `ingressFingerprint` is exactly equal to the atomic Interaction record; the current Context explicitly supplies verified Interaction fingerprint artifact version `IV`, retained `interactionArtifactVersion` equals `IV`, and `IV` remains distinct from `transitionArtifactVersion`; the same typed `CheckoutStarted` for two stable subjects or two issuer/realm scopes produces different fingerprints, while changing only `idempotencyKey`/RequestId/trace/reply metadata preserves the fingerprint;
- Checkout Interaction-version fixtures run equal Pulse/actor values with `IV1` and `IV2`, reject missing/mismatched/stale/untrusted `IV` before acceptance, retain the originally accepted `IV` across retry, and select recovery/migration/quarantine from that retained version without an ambient artifact lookup;
- Checkout M→S→I→P transitions: initial M/actor binding, cart S, I, and direct/reconciled P are retained with exact correlation/authority+observation handles/versions/provenance in the same Decision that creates the dependent output; the same P through a second valid route corroborates without a second Order output, while conflicting P/original proof fails closed;
- invalid non-empty DecisionContext.

Local read tests are added when a `Query` exists. When the stamped-read trigger applies, they verify the single declared result payload, exact correspondence of `ConsistencyStamp` to the source `CommittedStateSnapshot`, a deterministic result for identical snapshot/query/context, and the absence of a `Decision`, mutation, new revision, detached handle, or semantic output. An actor-dependent Query/status read tests valid approved-issuer context, forged/tampered/missing/stale evidence, wrong issuer/realm, typed cross-Ball construction, and pure semantic authorization/result selection by the Ball; every reachable permitted, denied, redacted, and deliberately non-disclosing outcome is a target-owned payload variant, and removing one fails exhaustiveness. A fixed trusted same-stack scope may prove issuer/realm without runtime fields. An actor-independent read materializes no actor context, issuer, authentication, actor-evidence artifact, or unreachable denial placeholder. A same-stack getter without the stamped-read trigger tests the same purity against its call-scope snapshot without materializing wrappers. Operation-status fixtures apply only when that path exists and exercise the canonical §9.11 materializer cases above over every reachable lifecycle/cancellation/result/stop facet. The Checkout projection additionally retains the exhaustive `NotFound`/known/expired/unknown/stopped-handle, ten-slot order, and capacity `10/11` cases defined in §§16.13 and 17.7.

A present `ReadDependency` additionally tests exact-one resolution of caller, target authority, target-owned `Query -> ResultPayload`, effective protocol identity, target read/status authority, caller freshness/consistency requirements, and Assembly route/binding. Wrong version, target/read authority, mapping, or fabricated/mismatched stamp fails before semantic read and is never converted into `NotFound`. Pre-read validation/admission uses only the existing `BoundaryResponse`; admitted execution returns only the declared successfully evaluated `ReadResult` and creates no accepted marker, Decision, revision, handle, or output. When policy can deny or redact, permitted, valid-context-denied, redacted, and deliberately non-disclosing cases each resolve one declared target-owned payload variant; the wrong-provenance case remains a boundary failure, an undeclared `NotFound`/exception/`BusinessRejection` fails, and caller/Assembly synthesis fails. Actor-dependent and actor-independent routes, generated same-stack erasure, and independent multi-source reads preserve the same sparse target-owned semantics; an actor-independent or unconditionally permitted read has no unreachable denial placeholder. A command/read substitution fixture keeps `Payment.GetOperationStatus` on its accepted command path and uses `ReadDependency` only for the ordinary non-recording lookup.

Boundary/adoption fixtures execute every §4.4 choice and falsifier: combine versus separate, Ball-local utility, shared mechanical Foundation, Feature, Flow, and Read Model. A local helper owned by one role and a shared mechanical primitive pass; an ownerless shared domain/business utility fails; explicit Ball-local copies and one Ball-owned semantic contract exposed through its Application Surface both pass. Every physical import remains in the acyclic compile-time graph, while no `Direct Control Dependency` arises without another Ball's Application Surface or synchronous authority crossing. At least two different valid decompositions of the same toy domain prove that Core constrains authority graphs rather than selecting one unique graph. UI/transport cases distinguish focus/scroll/animation/parser mechanics from a value that changes a Decision; the latter is accepted only as committed State or an explicit trusted current `Pulse`/`DecisionContext`. Flow tests activate on each individual material-coordination property and reject call-count/one-hop-only pseudo-Flows. The adoption/pilot fixture treats the worksheet as project-owned `SHOULD` guidance, records the selected workload, measurement method, baseline, and continue/reshape/stop thresholds for relevant §4.5/§13.5 measures, introduces no universal number, and requires no worksheet outside that work; negative-adoption cases remain ordinary utilities/adapters rather than empty Balls.

Lifecycle/read fixtures prove that `Draining` rejects a new logical mutation, continues already accepted completion/cancellation/status inputs, and serves each available declared Query/status Query from its committed authority. An unavailable read returns only its declared pre-read validation/admission response; an admitted read returns only the successfully evaluated `ReadResult`, whose target-owned payload closes every reachable policy outcome, and creates no Decision. Status variants test both a co-located and separate read authority, exactly one query writer in either layout, honest lag/stamp behavior, and no transfer of command/business-fact authority.

Error fixtures cover every §6.13 row and reject every cross-stage rewrite: validation, admission, pre-acceptance Decision rejection, accepted target result, post-acceptance Resource failure/timeout/unknown, delivery stop, and programming fault. A later failure preserves prior acceptance/result. Each concrete profile/binding accepts every member of its finite closed `AdmissionFailure.reason` union and rejects an unknown discriminator or open string before trusted `AdmissionFailure` construction; a non-fallible Inline profile has no empty union.

### 17.2. Property-based tests

Useful properties are selected from the active trigger set; the Catalog/Checkout-specific properties below are not copied into an unrelated Ball:

```text
state invariant preserved after every Accepted Decision
outputs and every present dimension <= effective bounds
forbidden actor produces no privileged output
stale generation cannot replace current generation
same state+pulse+context+artifact gives same Decision
same state+pulse+valid per-Pulse context+artifact gives the same candidate Decision under different remaining runtime causal budgets; only admission/continuation may differ
Inline deque/continuation preserves each Pulse-to-Context association; later causes do not inherit root context fields
same state+pulse+valid context+transition artifact version+binding+meter identity/version gives the same transition-step count
one decide meter scope is monotonic and cannot reset; exact N may complete and first-unit N+1 accepts no frame/state/revision/output/dispatch
different meter identities or transition artifact versions are not numerically comparable
every active numeric maxInputBytes | maxStateBytes | maxOutputBytesPerDecision resolves one BoundedByteMeasure dimension/identity/version/representation/limit tuple; equal canonical values under one tuple count equally, alternate representations map exactly, erasure preserves the count, different tuples are incomparable, and stage-specific N+1 accepts no forbidden semantic artifact
maxCumulativeFanout counts each distinct accepted source-output-to-effective-route/consumer branch once across one causal scope; terminal and converging traversals count, duplicate/redelivery does not, async handoff preserves scope, and N+1 accepts no partial Decision
maxDeclaredDependenciesPerBall counts each distinct owner-declared read/command/signal/participation row once; references do not erase either row and duplicate/alias identities are invalid
maxRoutesPerFlow counts each distinct effective command/result round-trip mapping once; ingress/return legs and FlowParticipation references add no unit, exact N resolves, and N+1 rejects the graph
rejected input does not change state
fault does not publish partial output
compensation never reuses original action identity
every example output maps to one canonical SemanticOutput envelope and full SemanticHandle
every outgoing payload field has one explicit State | current Pulse | declared DecisionContext lineage
CheckoutStartFingerprintV1(P, A1) != CheckoutStartFingerprintV1(P, A2) for different stable subject/issuer/realm scope
CheckoutStartFingerprintV1(P with key/transport metadata X, A) = CheckoutStartFingerprintV1(P with key/transport metadata Y, A)
retained Checkout ingress fingerprint = atomically accepted Interaction idempotency-record fingerprint
retained Checkout interactionArtifactVersion = verified current Context artifactVersion IV != transitionArtifactVersion identity; retry never replaces the accepted IV
same root key+fingerprint within horizon redelivers the exact original accepted ReplyOutput frame with only a new AttemptId and no Decision/revision/output; different fingerprint returns pre-Intent ValidationFailure(IdempotencyConflict) and changes nothing
retained workflow value is not cleared while a dependent transition remains reachable
same authority action cannot bind different retained value/authority version/conflicting proof; alternate valid observation of the same value only corroborates
Catalog ProductSelected source-state set = {Idle, Searching, Ready, Failed, OutcomeUnknown, Cancelled}
every Catalog ProductSelected Decision has outputs = [SignalPublication(ProductSelectionConfirmed(productId), sourceOrdinal = 0)]
CatalogState-to-CatalogView source-state set = {Idle, Searching, Ready, Failed, OutcomeUnknown, Cancelled}, with one case per state and no fallback
CatalogView preserves lifecycle/result + cancellation + CancellationRejected(reason) for every reachable state
ProductSearchOutcomeUnknown cannot regress Ready, Failed, or proven Cancelled; a later proven result may refine OutcomeUnknown
nonterminal cancellation acceptance commutes with a legitimate matching result and preserves its lifecycle in both observation orders
too-late, rejected(reason), and cancellation-unknown commute with legitimate result without information loss
terminal cancellation proof subsumes a delayed accepted-in-progress observation; a later weaker proof does not change terminal state or publish duplicate output
mutually exclusive terminal result/cancellation proofs never overwrite an accepted terminal frame in either arrival order
stale SearchCancelled(A) against current operation B accepts no Decision/revision/handle/output/Effect and can never target B; exact B follows the cancellation matrix
ModuleCommandPulse commandSource always resolves to its accepted source ModuleCommandRequest frame
ModuleResultPulse preserves accepted commandSource + target-derived resultSource + effectiveProtocolIdentity
ModuleResultOutput semanticHandle = commandSource.semanticHandle without changing target payload ownership
verified pre-acceptance carrier implies RejectedBeforeAcceptance + NotExpected
root pre-acceptance rejection implies no authoritative OperationId/status row/handle/output; participant carrier remains a facet of an already accepted source operation
same-stack pre-acceptance carrier implies no accepted target level + exactly one transferred level-1 alternative consumed by the source carrier Decision
same-stack accepted target and source carrier branches never both consume the alternative-completion slot
accepted target Rejected result implies Accepted + Rejected
same command identity within idempotency horizon has at most one target Decision/revision and one accepted result frame
same target command duplicate before result returns only verified accepted-frame ACK/pending proof; after result it redelivers the exact result frame; neither path waits, re-decides, revises, fabricates a result, or repeats Resource work
accepted Checkout StillUnknown on the sole status slot yields terminal NeedsManualReconciliation with no output or automatic generation; duplicate replay adds no Decision and later proof requires a separately declared recovery artifact
equivalent status observation redelivery is idempotent
compatible status evidence order preserves the same lifecycle + cancellation + result + delivery-stop facets
weaker status evidence cannot regress a proven facet; conflicting same causal key never overwrites by arrival order
retention marker implies covered source positions/horizons + empty pending and prevents covered late-evidence resurrection
```

For a state machine, generating a sequence of Pulses is useful, not only individual examples.
