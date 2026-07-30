# Core part — Decision and acceptance

[Core contents](../pokeball-architecture-core.md) · [← State and authority](07-state-and-authority.md) · [Asynchrony and delivery →](09-asynchrony-and-delivery.md)

> Canonical part 5 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 8. Decision and commit semantics

### 8.1. DecisionContext

<!-- pkb:term:start name="DecisionContext" -->
**DecisionContext** — a closed field-minimized record of contextual observations that actually change one Decision; it may be empty. The trusted binding boundary verifies, bounds, and constructs it for that invocation's current `Pulse`, while the Ball/Nucleus owns its semantic schema and interpretation. Each Inline deque or continuation item preserves its own Pulse-to-Context association; actor, authorization, config, policy, time, reserved IDs, semantic limits, validity, and version fields appear only when relevant to that Pulse. Every present version field names the exact artifact it versions and is not interchangeable with another artifact's version or ambient build/deployment state. Remaining causal budget, execution quantum, and current runtime capacity are admission state rather than Context, and the current cause remains the separate `Pulse`.
<!-- pkb:term:end -->


`pulse` in canonical `decide(state, pulse, context)` is the current cause of the transition. `DecisionContext` is a closed, field-minimized record of contextual observations that actually affect interpretation of that cause but are not themselves the current cause. It may be `Unit`/empty when no such observation exists.

The trusted binding boundary constructs each non-empty `DecisionContext` from verified, bounded observations after checking every triggered origin, authenticity, integrity, version, validity, and size rule. The Ball/Nucleus owns the semantic schema and interpretation of those fields; the boundary neither adds undeclared business meaning nor decides policy. Same-stack representation erasure is legal only when the enclosing trusted binding proves the same facts and limits.

The following is an applicability catalog, not a mandatory struct shape:

```text
DecisionContext {
    actorContext?
    authorizationSnapshot?
    configurationSnapshot?
    featurePolicySnapshot?
    trustedTimeObservation?
    reservedSemanticIds[]?
    deterministicSeed?
    semanticLimits?
    artifactVersion?
    issuedAt?
    notBefore?
    expiresAt?
    contextGeneration?
    contextDigest?
}
```

The catalog label `artifactVersion?` is not an ambiguous ambient version slot. A concrete Context schema assigns it one exact artifact meaning, or uses more specific field names when several artifact versions are present. Each triggered version is independently verified for the current Pulse; the current binary, deployment metadata, a ledger lookup, or another field such as `transitionArtifactVersion` cannot substitute for it.

All decision-relevant data appears explicitly in committed `State`, the current `Pulse`, or `DecisionContext`. A value from an earlier Pulse or Context that is needed later is retained in State according to §7.5 or reintroduced through a new declared trusted input; hidden history or ledger reads are prohibited. The current `Pulse` is not duplicated in context; conflicting copies make the input invalid. Actor, configuration, policy, trusted time, reserved IDs, semantic limits, and version metadata appear only when they can change this decision or when their trust/version boundary must be verified. Unused catalog fields are absent and require no placeholder.

Each `decide` invocation receives the context constructed for that invocation's current `Pulse`. If an Inline deque or retained continuation carries more than one cause, every item carries or resolves before `decide` its own trusted `(Pulse, DecisionContext)` pair. A later `Fact` or `ControlPulse` does not inherit the root actor, grant, reserved-ID, or other context fields merely because execution is inline; each field is independently triggered and verified for that current Pulse, or it is absent and the context may be `Unit`.

<!-- pkb:pba-source:start id="PBA-05" title="Explicit Decision Inputs" -->
**Source clause for PBA-05 — Explicit Decision Inputs.**

- **Rule:**
  - The explicit-input rule is the complete State/current-Pulse/field-minimized-Context and prior-value retention or trusted-reintroduction contract in the preceding paragraphs.
  - Every invocation, including each Inline deque or continuation item, uses the trusted context for its own current Pulse rather than another item's context.
  - The trusted binding boundary constructs verified and bounded context.
  - The Ball/Nucleus owns its semantic schema and interpretation.
  - Every triggered artifact-version Context field names and verifies the exact artifact version relevant to the current Pulse; another artifact's version, the current binary, or ambient deployment state cannot substitute for it.
  - Remaining causal budget, execution quantum, and current runtime capacity are reservation/admission inputs, not DecisionContext fields or hidden inputs to `decide`.
  - No hidden constructor, history, ledger, or ambient value may supplement those inputs.

  `DecisionContext` need not be versioned, serialized, or cryptographically signed in one compiled trusted call scope. When it crosses a persistence, independently deployed, replay, or hostile boundary, the applicable profile defines exact version, issuer, validity, and authenticity requirements.

  Runtime capacity—including remaining causal depth/budget and completion slots, execution quantum, CPU, mailbox occupancy, and disk pressure—must not change a business choice invisibly and is not a `DecisionContext` field. It belongs to reservation/admission and may reject the entire candidate `Decision` before acceptance. A declared semantic limit that genuinely changes the Decision remains a separate, explicitly triggered Context field; current capacity never masquerades as such a limit.
- **Applicability:** `A`: every Decision.
- **Declaration owner:** Ball/Nucleus owns State, current Pulse, sparse per-Pulse Context schema/interpretation, and retained-value policy; trusted binding owns context construction/verification.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Context-constructor provenance/bound checks, exact-artifact missing/mismatch/untrusted-version tests, Inline Pulse/Context association across yield/resume, runtime-budget-independence, and transition hidden-input tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared constructor mechanics may be reused in exact scope; unused Context fields and runtime admission state are absent, and no prior-item or ambient supplement exists.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.2. Determinism

For the same canonical state, pulse, valid context, and transition artifact version, `decide` must return a semantically equal result.

Sources of nondeterminism—a clock, randomness, locale, time-zone data, configuration, unordered iteration, or an external query result—must be:

- excluded from the `Nucleus`;
- supplied as an explicit observation or context;
- or fixed by an implementation contract so that the semantic result does not depend on a platform accident.

Full bit-exact cross-language replay is not a Core guarantee.

When a numeric `maxTransitionSteps` declaration is used, determinism also applies to its admission boundary: the same canonical State, Pulse, valid Context, transition artifact version, binding, and exact Decision Work Meter identity/version consume the same total metered units. This scoped meter equality does not imply universal work units or cross-binding replay.

### 8.3. Bounded decision

<!-- pkb:pba-source:start id="PBA-38" title="Bounded Execution" -->
**Source clause for PBA-38 — Bounded Execution.**

- **Rule:**
  - Every present variable dimension is finitely bounded as specified below.
  - A triggered operation-status materializer reserves its operation, pending, facet, and delivery-stop capacity before source acceptance.
  - It never evicts or truncates an accepted fact because of later pressure.
  - A `ReadDependency` remains subject to the already effective input, read-work, response, route, and buffering bounds.
  - The mere existence of a `Query` creates no new mandatory per-Ball read-limit field.
  - When admission can fail, the concrete finite closed `AdmissionFailure.reason` union bounds its outcome space.
  - No open reason string is permitted.
  - Each same-stack command uses §8.4's one next-level alternative-completion reservation: exactly one accepted target Decision or source carrier Decision consumes it, never both; successful target acceptance separately reserves the following result completion, and unavailable capacity prevents acceptance rather than losing a carrier or resetting the causal budget.
  - When `maxCumulativeFanout` is present, §10.9/PBA-29 defines its one causal-scope branch unit, aggregation, duplicate treatment, and exact boundary. A static proof may establish the effective ceiling; otherwise the existing total causal reservation admits the whole candidate output batch before acceptance. The first branch at `N+1` rejects that entire candidate Decision with `AdmissionFailure(CausalBudgetExceeded)` and dispatches no subset.

  Every variable dimension that exists on a reachable path has a finite effective bound. The proof for one dimension is exactly one of:

  1. a closed bounded type or static control-flow proof;
  2. an exact reusable policy reference resolved under §0.2;
  3. a local declaration, or an allowed local delta over that reference.

  The canonical dimensions are:

  ```text
  maxInputBytes
  maxStateBytes
  maxCollectionItems
  maxOutputsPerDecision
  maxOutputBytesPerDecision
  maxEffectsPerDecision
  maxCommandsPerDecision
  maxCausalDepth
  maxCumulativeFanout
  maxRetriesPerOperation
  maxTransitionSteps
  ```

  The names and units are canonical when a numeric declaration is used:

  | Limit | Trigger and scope |
  |---|---|
  | `maxInputBytes` | Raw or normalized input has variable byte size: maximum bytes for one candidate input at the exact selected boundary stage under the binding-owned byte-measure contract below. The representation definition names the included boundary metadata and `DecisionContext` fields. A bounded input type may prove it statically. |
  | `maxStateBytes` | Retained State has variable byte size: maximum bytes for the complete candidate `nextState` under the binding-owned byte-measure contract below. A fixed-size State type and representation may prove it statically. |
  | `maxCollectionItems` | A retained or processed collection has variable length: maximum items for each named collection; cumulative bytes still apply. |
  | `maxOutputsPerDecision` | A Decision can contain outputs: maximum complete output sequence length. A fixed output algebra/control flow may prove it statically. |
  | `maxOutputBytesPerDecision` | A Decision can contain a variable-size output sequence: maximum bytes for the complete ordered accepted output sequence under the binding-owned byte-measure contract below. A fixed bounded output algebra and representation may prove it statically. |
  | `maxEffectsPerDecision` | `EffectRequest` exists: maximum such outputs in one Decision. |
  | `maxCommandsPerDecision` | `ModuleCommandRequest` exists: maximum such outputs in one Decision. |
  | `maxCausalDepth` | An accepted consequence can cause another mutating Decision or a route spans multiple Decision hops: maximum total hops including the root. |
  | `maxCumulativeFanout` | Accepted outputs traverse declared routes within one causal scope: maximum distinct accepted output-to-effective-route/consumer branches under the exact §10.9/PBA-29 counting contract. |
  | `maxRetriesPerOperation` | A retry path exists: maximum repeated attempts beyond the initial attempt for the named retry owner and failure mode. |
  | `maxTransitionSteps` | A numeric declaration is used to bound decision work instead of or in addition to a closed bounded type or static control-flow proof: maximum units under one exact versioned binding-owned Decision Work Meter. |

  When a numeric `maxTransitionSteps` declaration is present, its resolved declaration view is:

  ```text
  DecisionWorkMeter {
      meterIdentity
      meterVersion
      transitionArtifactVersion
      unitDefinition
      maxTransitionSteps
      overflowPolicy
  }
  ```

  This is an effective contract view, not a mandatory runtime envelope. The identity and version select one immutable unit definition. For the same binding, transition artifact version, meter identity/version, canonical State, Pulse, and valid `DecisionContext`, the total count is deterministic. Units are non-negative integers and consumption is monotonic. The meter starts once at zero for one `decide` invocation and cannot be reset, split, or restarted by a nested helper, internal phase, loop, retry, yield, or representation erasure to evade the limit. Its scope ends with that invocation; a later `decide` starts a new scope, while causal depth, retry attempts, wall time, execution quantum, dispatch, and route work remain separate dimensions.

  At exactly `N = maxTransitionSteps`, the Decision may complete normally. Attempting unit `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and follows the binding's declared finite typed pre-acceptance or programming-fault policy; it cannot truncate work into a different business result. Numeric counts are comparable across bindings only when meter identity, meter version, transition artifact version, and unit definition are all equal. Core defines no universal step unit and still makes no bit-exact cross-language replay guarantee. When static bounded control flow already proves the transition-work limit, no meter artifact or runtime counter is required.

  When a numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` declaration is used without a static bounded-type-and-representation proof, the binding resolves one immutable measurement contract for that active dimension:

  ```text
  BoundedByteMeasure {
      dimension: Input | State | DecisionOutputs
      measureIdentity
      measureVersion
      representationDefinition
      limitName: maxInputBytes | maxStateBytes | maxOutputBytesPerDecision
      maxBytes
  }
  ```

  `BoundedByteMeasure` is the local name of this PBA-38 effective declaration/conformance view, not a new protocol, runtime envelope, or glossary-inventory term. `dimension + measureIdentity + measureVersion + representationDefinition + limitName` selects one immutable equality domain. Under one tuple, equal canonical values produce equal non-negative byte counts; counts from different tuples are incomparable. `maxBytes` is exactly the effective numeric value of `limitName`, not a second limit. If another representation is retained or transported, `representationDefinition` maps it deterministically to the selected measured byte sequence; representation erasure cannot change the count for equal canonical values under the same tuple.

  For `dimension = Input`, `representationDefinition` selects exactly one measurement stage—`RawBoundaryInput` or `NormalizedTrustedInput`—and one complete candidate-input sequence. It names every included raw or normalized input field, boundary metadata field, and constructed `DecisionContext` field; an omitted field is explicitly outside that input limit rather than ambiently counted. Later transport buffering, allocator layout, compression, encryption, and retry/attempt framing are excluded. The selected count is checked before the trusted semantic input is accepted or supplied to `decide`. At exact `N = maxInputBytes`, the input may proceed; `N+1` returns the binding's declared finite pre-acceptance response and invokes no `decide`.

  For `dimension = State`, the measured value is the complete candidate `nextState` semantic representation before acceptance. `representationDefinition` includes the State schema discriminator/version and every retained State field plus any other semantic metadata it names. It excludes heap/object headers, allocator padding, indexes, storage-record framing, compression, encryption, replication metadata, and transport mechanics. A binding that persists another representation maps it deterministically to the selected sequence. At exact `N = maxStateBytes`, the complete Decision may be accepted; `N+1` rejects the whole Decision before State, revision, or output acceptance and performs no truncation or dispatch.

  For `dimension = DecisionOutputs`, the measured value is one deterministic semantic representation of the complete ordered `Decision.outputs` sequence before dispatch. The bytes include sequence structure plus every required output-envelope field, correlation token, `sourceOrdinal`, and payload. They exclude `nextState`, accepted-frame fields that are not part of an output envelope, and later transport-only framing, compression, encryption, retry headers, or `AttemptId`. At exact `N = maxOutputBytesPerDecision`, the complete Decision may be accepted; `N+1` rejects the whole Decision before State, revision, or output acceptance, and no individual output is truncated or omitted.

  A closed bounded type plus static representation proof may establish any of these ceilings without a `BoundedByteMeasure` artifact or runtime serialization/counting. The proof still fixes the same dimension, complete value scope, and `N/N+1` result; it cannot rely on an unspecified implementation representation.

  An absent output kind, retry path, causal chain, collection, queue, or profile path requires no zero-valued field. Absence is proved by the closed protocol/type/profile/route inventory. A present dimension with no static proof, no applicable reusable policy, and no local declaration is invalid; `unbounded` is never an effective value. Delivery attempts, retention, concurrency, IPC, resource responses, and other profile-specific dimensions resolve by the same rule when their paths exist.

  Every `ModuleResultOutput` counts as one target output under `maxOutputsPerDecision` and contributes its complete target-owned envelope/payload bytes to the target's `maxOutputBytesPerDecision`. A retained, retried, or independently observable result route also counts against the target's finite delivery/status bounds and one stop-eligible target slot; source-side input, command, or output bounds do not substitute for these target bounds.

  When operation status is triggered, the source/status capacity plan has finite effective bounds for every reachable operation record, pending observation, lifecycle/cancellation/result facet, retention marker, and unique delivery-stop record. It reserves the newly reachable capacity before the acceptance point that can create the source fact. Capacity `N+1` prevents that source acceptance and dispatch under the declared typed admission/fault policy; after acceptance, pressure cannot justify eviction, truncation, or loss of the accepted source or observation.

  A shared policy update does not mutate existing effective contracts: it creates a new revision/digest, and a Ball or Assembly adopts that revision explicitly. Two referenced policies that provide the same effective key without an explicit single override relation conflict and fail resolution.

  Overflow MUST NOT cause truncation, partial state, or dispatch of a subset of non-drop-eligible outputs. The Decision is accepted in full or rejected in full.
- **Applicability:** `A`: present input/state/decision/output dimensions; `P`: numeric decision metering, synchronous command completion, collections, reads/routes/buffers, queues, retries, fan-out, concurrency, IPC, delivery, retention, status, or fallible admission.
- **Declaration owner:** Ball/project; binding owns meter identity/unit, each byte-measure identity/version/representation, and source/target reservations; Assembly/runtime/status/profile owner owns other introduced dimensions/reasons.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Meter identity/version/unit resolution; input/State/output byte-measure dimension/identity/version/representation/limit resolution, mapping/erasure invariance, tuple incomparability and exact stage-specific `N/N+1`; equal-input determinism, monotonic no-reset per-`decide` scope, depth/fan-out exact `N/N+1`, alternative-completion transfer, branch identity/aggregation, read bounds, closed reasons, pressure, and overflow tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Static/local/shared proof; static work, byte-size, or fan-out needs no runtime counter or measure artifact, Query alone adds no limit field, absent dimensions/reasons are omitted, unlike meters/byte-measure tuples are not compared, retries/redelivery do not double-count one accepted route branch, and accepted status facts are never evicted/truncated.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.4. Run-to-completion

<!-- pkb:term:start name="RetainedContinuation" -->
**RetainedContinuation** — a bounded single-owner continuation of a reserved synchronous causal chain; every retained item preserves its own trusted Pulse-to-DecisionContext association, and resume continues the original total causal budget and declared terminal policy without adding that runtime budget to Context.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-09" title="No Reentrant Transition" -->
**Source clause for PBA-09 — No Reentrant Transition.**

- **Rule:**
  One mutating transition runs to acceptance or rejection. Reentrant transitions are prohibited.

  The remainder of this subsection is path-triggered when an accepted output can complete synchronously into another mutating `Pulse`, or when one causal scope spans more than one Decision hop. A Ball with no such path needs no causal-budget field, deque, reservation, or continuation artifact.

  The total causal budget is tied to the root operation or another explicit causal scope. It includes at least the remaining `maxCausalDepth` and, when present, the exact `maxCumulativeFanout` branch accounting defined by §10.9/PBA-29. It is not reset by yield, resume, transport retry, redelivery, or a hop between Balls. Static fan-out proof needs no runtime counter; otherwise fan-out capacity is part of this existing reservation/admission state and never a `DecisionContext` field or a new protocol.

  Before accepting a Decision, the runtime reserves depth in the same total causal budget and bounded completion slots for every output that may complete synchronously. If a full reservation is unavailable, the Decision is not accepted and a typed `AdmissionFailure(CausalBudgetExceeded)` is returned; no subset of outputs is dispatched. An already accepted synchronous completion is never dropped because the next execution quantum is exhausted.

  For a same-stack command round trip, the source/root Decision, target command Decision, and source result Decision consume causal levels `0`, `1`, and `2`: three total hops including the root. Before source acceptance, the source reserves one level-1 **alternative-completion slot** for each command output that can invoke its target synchronously. Exactly one of two mutually exclusive branches consumes each slot:

  - if the target accepts, its Decision consumes the slot at level 1; before that acceptance the target separately reserves the level-2 source-result completion;
  - if target validation, admission, or `decide` rejects before acceptance, the target attempt consumes no accepted target level and creates no target frame, revision, or output; the verified boundary atomically transfers the already reserved level-1 slot to the source `decide(ControlPulse)` that applies `CommandRejectedBeforeAcceptance`.

  If the source cannot reserve level 1, its Decision is not accepted and the command is not dispatched. If the target cannot reserve level 2, the target Decision is not accepted and the second branch returns `AdmissionFailure(CausalBudgetExceeded)` through the carrier; no target result output exists. The target and carrier branches cannot both consume the alternative slot. The carrier cannot be dropped, deferred outside a declared `RetainedContinuation` that preserves the same slot and total budget, applied by a direct State write, or moved to a reset or over-budget causal scope. Before accepting a carrier-handling source Decision, the runtime reserves any further synchronous completions emitted by that Decision from the remaining total budget under the ordinary rule above.

  The synchronous invocation contributes `source -> target` to the `Direct Control Dependency` graph; the causally bound result or carrier return does not create a reverse edge. An asynchronous handoff removes only that synchronous-invocation contribution; any independently present compile-time-import edge remains. The handoff preserves the same causal scope, depth, and remaining budget rather than resetting them, and same-stack slot transfer is not inferred merely from asynchronous transport.

  If an Inline executor completes an `EffectRequest` synchronously, the causally bound `Fact` is placed in a pre-reserved bounded local deque only after acceptance of the current Decision. The trusted binding boundary constructs each initial or completion item with the verified, bounded, field-minimized context for that item's own Pulse under §8.1:

  ```text
  InlineWorkItem {
      pulse
      decisionContext # trusted for this pulse; Unit when no context field is triggered
  }

  while deque not empty:
      if executionQuantum exhausted:
          return RetainedContinuation(deque, totalCausalBudget)
      item = pop_front()
      decision = decide(committedState, item.pulse, item.decisionContext)
      preflightAndReserve(decision, totalCausalBudget)
      acceptedFrame = accept(decision)
      dispatch acceptedFrame.outputs
      enqueue trusted InlineWorkItem completions into reserved slots
  ```

  `totalCausalBudget`, its remaining depth/completion/fan-out capacity, and `executionQuantum` are runtime reservation state passed only to the loop and `preflightAndReserve`; they are never added to `DecisionContext`. For equal committed State, Pulse, valid per-Pulse Context, and transition artifact version, changing only remaining runtime capacity cannot change the candidate Decision; it can change only whether that complete candidate is admitted.

  A `RetainedContinuation` has one owner, bounded capacity, and a resume and status policy; resume continues the same total causal budget. It may be caller-owned or use fixed storage and does not impose a mailbox or queue on a Ball that has no synchronous causal chain. When the total limit is reached, previously accepted causes and outputs are not rolled back and the budget is not reset: the current Decision with a new over-budget output is not accepted; the current `Pulse` remains in a retained or terminal state according to the declared policy, and the runtime returns a typed admission or status outcome.
- **Applicability:** `A`: every mutating transition; `P`: synchronous completion or a causal scope spanning multiple Decisions; a same-stack command adds the exclusive target/carrier alternative completion.
- **Declaration owner:** Execution binding and route/source reservation contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Reentrancy, levels `0/1/2`, alternative-slot transfer, double-consumption, continuation, exact branch accounting, retry/redelivery/handoff preservation, and depth/fan-out causal-budget `N/N+1` tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Profile mechanics may be reused; absent synchronous/multi-Decision/fan-out paths need no slot or fan-out artifact, and no retry, handoff, or continuation resets or double-counts the scope.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.5. Atomic Decision acceptance

<!-- pkb:pba-source:start id="PBA-07" title="Atomic Decision" -->
**Source clause for PBA-07 — Atomic Decision.**

- **Rule:**
  An observer must not see a snapshot `nextState` or an EventJournal event/NoDomainChange acceptance without that Decision's present source outputs, and must not see an output before the selected mode's accepted state or event commit. A state-only snapshot Decision is ordinary atomic state publication; an EventJournal `NoDomainChange` remains an explicit accepted event commit even when its output sequence is empty.

  Source acceptance has exactly the frame selected by the state profile:

  ```text
  AcceptedSnapshotDecisionFrame<State> {
      commitRevision? # materialized when §3.2 trigger applies; otherwise accepted call scope
      nextState
      outputs: BoundedSequence<SemanticOutput>
  }

  AcceptedEventCommit {
      commitRevision
      acceptedInputMarker
      idempotencyMarker?
      operationStatusChanges?
      decision: EventDecision # EventMutation or NoDomainChange + complete ordered outputs
  }
  ```

  These are semantic tuples, not mandatory heap objects or classes. A snapshot Inline binding may representation-erase its frame into a stack tuple, fixed buffer, or equivalent control flow. A state-only Inline Decision need not construct an output frame. `AcceptedEventCommit` is the exact durable EventJournal frame from §8.9 and never contains an independent `nextState`.

  For `Transient`:

  1. validate applicable bounds and admission;
  2. when outputs exist, pre-reserve capacity to retain or hand off the complete batch and materialize its semantic frame without visible external changes;
  3. atomically publish state and the present frame—this is acceptance and the only point at which state becomes visible;
  4. dispatch only from the accepted frame after publication.

  State is not published through a separate pointer before present outputs. A queue is not mandatory: the frame and reservation may be caller-owned or fixed. While a Transient instance remains available after acceptance, the runtime does not lose the undelivered part of a present batch; a process crash may lose the entire transient state and frame according to the profile contract.

  For `SnapshotOutbox` or `EventJournal`, respectively:

  1. validate applicable bounds and any triggered expected-revision, ownership-fence, or present-output route condition;
  2. record the snapshot frame or `AcceptedEventCommit` and any present source delivery records in one authoritative transaction;
  3. permit dispatch of present outputs only after durable success.
- **Applicability:** `A`: every accepted mutation.
- **Declaration owner:** Ball algebra and binding acceptance contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Acceptance primitive/fault tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Binding mechanism/evidence may be reused; no atomicity flag.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.6. Commit-before-dispatch

<!-- pkb:pba-source:start id="PBA-08" title="Commit Before Dispatch" -->
**Source clause for PBA-08 — Commit Before Dispatch.**

- **Rule:**
  > **No `SemanticOutput`—including `ProjectionOutput`, `ReplyOutput`, `EffectRequest`, `ModuleCommandRequest`, `ModuleResultOutput`, `SignalPublication`, and `TimerRequest`—is dispatched before successful acceptance of its Decision.**

  Otherwise, a crash between dispatch and the state commit would create an external consequence without a recorded cause.
- **Applicability:** `P`: a Decision can contain output, including a target result output.
- **Declaration owner:** Owning Ball and binding/route ordering contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Source/target acceptor, dispatcher/result route, and crash tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One binding may cover many Balls; omit dispatch machinery for empty output set.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.7. Preflight and admission

Preflight checks only dimensions activated by the Decision and selected profile. It may check:

- output bounds;
- route or executor availability;
- capability shape;
- storage capacity;
- expected revision;
- runtime quotas.

Before accepting any Decision under numeric `maxStateBytes`, preflight measures the complete candidate `nextState` once under its exact §8.3 `BoundedByteMeasure`; `N+1` rejects before State, revision, or output acceptance. Before Transient acceptance of an output-bearing Decision, preflight MUST also reserve the ability to retain the complete output batch. Under numeric `maxOutputBytesPerDecision`, it measures the complete ordered sequence once under its exact byte-measure tuple; it never checks each payload independently or substitutes later transport bytes. When synchronous completion can cause another mutating Decision, preflight also reserves the corresponding bounded slots and total causal budget. A state-only Decision needs no output-batch reservation, but still satisfies every applicable State bound. A reservation is not dispatch and does not make an output visible.

Before accepting a target Decision containing `ModuleResultOutput`, preflight includes that output in the target `maxOutputsPerDecision` count and complete-sequence `maxOutputBytesPerDecision` measurement. If the selected result route is retained, retried, or independently observable, it also reserves the output's target stop-eligible delivery/status slot. Failure at `N+1` rejects the entire target Decision before acceptance; when the missing capacity is the level-2 result-completion reservation for the current command, the target remains unaccepted, the verified target boundary projects `AdmissionFailure(CausalBudgetExceeded)` through `CommandRejectedBeforeAcceptance`, and §8.4 atomically transfers the existing level-1 alternative-completion slot to the source carrier Decision.

Preflight MUST NOT:

- change the amount, actor, target, or operation kind;
- add a business fallback absent from the Decision;
- remove a required output to fit capacity;
- replace a business rejection with an admission failure.

### 8.8. Fault atomicity

<!-- pkb:pba-source:start id="PBA-10" title="Fault Atomicity" -->
**Source clause for PBA-10 — Fault Atomicity.**

- **Rule:** A failure is classified at its exact §6.13 stage:

  - before acceptance it publishes no partial state/output;
  - after acceptance it cannot roll back, downgrade, or rewrite the accepted Decision/result;
  - only the declared Resource, delivery, status, unknown-outcome, or runtime-fault path may add later evidence.
- **Applicability:** `A`: every pre-acceptance failure; `P`: post-acceptance Resource/delivery fault; `R`: ambiguous execution.
- **Declaration owner:** Ball and binding own exact §6.13 stage/failure contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Acceptor plus stage mapping, later-failure, dispatcher/executor/status, and fault tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Reuse by exact scope; later stages never rewrite earlier acceptance, and absent paths create no variants.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->

Before acceptance, upon a trap, programming fault, violated invariant, allocation failure, or detected nontermination:

- partial state is not published;
- outputs are not dispatched;
- the fault is recorded operationally;
- the instance may be restarted, failed, or quarantined according to runtime policy.

When accepted outputs exist, the state and complete source output batch are already accepted facts and are not rolled back because of a dispatch or worker fault. Such a fault:

- does not revoke an output that has already been dispatched;
- retains an undelivered output in delivery state for as long as the selected state and output profile promises;
- creates a typed delivery observation, `OutcomeUnknown`, or terminal `DispatchStopped` according to the boundary contract;
- may move the instance to `Failed` or `Quarantined`, but does not turn an accepted Decision into partial acceptance.

For `Transient`, a process crash may lose the entire frame and pending work; this is a profile limit, not permission to continue serving visible state after partial loss of the batch. A Ball with no dispatch path has no delivery-status or unknown-outcome obligation from this paragraph.

### 8.9. Snapshot and event modes

Core permits two different persistence contracts, selected statically by the state profile. Section 3.3 is the sole canonical definition of their exact result/Decision shapes: `Transient` and `SnapshotOutbox` use `SnapshotDecisionResult<State>`, while `EventJournal` uses `EventDecisionResult`. A binding implements only its selected mutation form.

```text
evolve(State, DomainEvent) -> State
```

Event mode does not return an independent `nextState`. Every `Accepted(EventDecision)`, including `NoDomainChange` with an empty output batch, creates the exact durable `AcceptedEventCommit` defined by PBA-07 in §8.5.

One authoritative transaction records the envelope, the event batch (which may be empty only for `NoDomainChange`), the accepted-input marker, and source output records only when `decision.outputs` is non-empty. An idempotency marker appears only when duplicate acceptance is possible; operation-status changes appear only when the status trigger exists. `CommitRevision` increases for every Accepted result; the commit envelope is not a synthetic domain event. `Rejected` creates no domain event, source output, or new CommitRevision. Full replay, upcasting, and migration are defined by a separate extension specification.

A target-owned business refusal that the versioned command contract classifies as an accepted result uses an ordinary same-state accepted Decision in snapshot mode:

```text
Accepted(SnapshotDecision {
    nextState = state,
    outputs = [ModuleResultOutput(Rejected(...))]
})
```

The snapshot `CommitRevision` increases under §3.2 even though state bytes are unchanged. In `EventJournal`, the set-equal form is:

```text
Accepted(NoDomainChange {
    outputs = [ModuleResultOutput(Rejected(...))]
})
```

Here `Rejected(...)` is a target-owned `ModuleResult` variant, not either mode-specific mutation result's `Rejected(BusinessRejection)` branch; the command was accepted. A pre-acceptance `CommandRejectedBeforeAcceptance` instead creates no accepted target Decision, revision, event commit, output, Resource action, or target operation/status record.

### 8.10. Local read

<!-- pkb:pba-source:start id="PBA-30" title="Honest Read Consistency" -->
**Source clause for PBA-30 — Honest Read Consistency.**

- **Rule:**
  - The materialized and representation-erased read forms in this subsection preserve exact snapshot identity for their triggered scope.
  - They make no unsupported multi-source consistency claim.
  - They create no mutating Decision or semantic output.
  - A cross-authority `ReadDependency` resolves one target-owned `Query -> ResultPayload` mapping and target read/status authority.
  - That one target-owned `ResultPayload` is total over every reachable post-admission semantic outcome for the Query, including only the permitted, denied, redacted, or deliberately non-disclosing variants that can occur.
  - The pure Nucleus/read Policy Gate selects that variant from the committed snapshot, Query, and trusted `ReadContext`; successful evaluation does not imply that access was permitted.
  - A denial may be observationally indistinguishable from absence only through one exact target-declared non-disclosing variant and mapping; boundary, caller, Assembly, runtime, and generated code cannot invent `NotFound`, redaction, denial, exception, or another carrier.
  - Its caller requirements describe only the selected target snapshot/stamp.
  - Neither caller nor Assembly may alter that meaning.
  - `Draining` rejects new logical mutations.
  - It serves every available declared Query/status Query from its committed authority.
  - An unavailable read can fail only before `read`.
  - An admitted read retains the successfully evaluated `ReadResult` codomain; a denied/redacted/non-disclosing payload remains a normal result and is never `BusinessRejection` or `BoundaryResponse`.
  - When operation status is triggered, §9.11 owns the generic materializer contract.
    - It has one committed revisioned single-writer authority per namespace.
    - It materializes only accepted operations; a reserved candidate `OperationId` that never reaches acceptance creates no known row, retention marker, handle, output, or status identity.
    - Root validation, admission, or Decision rejection returns only its typed `BoundaryResponse`; a later status read of the candidate value can yield `NotFound` only when the ordinary namespace and materializer-absence proof covers that read.
    - It applies facts in causal order or retains them in bounded pending state.
    - It merges facts idempotently, monotonically, and losslessly.
    - It reserves capacity before acceptance and never later evicts or truncates accepted facts.
    - Its covered-source and empty-pending retention markers precede absence and prohibit resurrection.

  ```text
  read(
      CommittedStateSnapshot<State>,
      Query,
      ReadContext
  ) -> ReadResult<ResultPayload>
  ```

  Canonical `CommittedStateSnapshot`, `ReadContext`, `ReadResult`, and `ConsistencyStamp` are defined in §6.3. They are materialized when a read crosses an authority or time boundary, may be cached/compared/aggregated, proves status absence or retention, or makes a freshness/consistency claim. Such a read operates on an immutable committed snapshot; the selected protocol identity unambiguously resolves `Query -> ResultPayload`, and the successfully evaluated response carries the exact available instance/revision/schema identity of the snapshot. Its target-owned payload closes every reachable post-admission business-visible outcome, including denial/redaction/non-disclosure when the Query can reach it.

  A same-stack local getter over the current immutable state, with no cache, status, cross-time observation, or freshness/consistency claim, may use call-scope snapshot identity and return its closed typed payload directly. It does not need wrapper objects or materialized revision/schema fields. In both forms, read does not mutate state or create a `Decision`, accepted-input marker, new revision, `SemanticHandle`, or `SemanticOutput`.

  If actor, tenant, issuer, realm, assurance, or delegation changes Query/status authorization or result selection, the trusted binding supplies the verified actor-dependent `ReadContext` and the Ball's pure semantic read/Policy Gate interprets it under `PBA-44`. The Policy Gate selects the target-owned permitted, denied, redacted, or declared non-disclosing payload variant after admission; the binding cannot choose or invent the business-visible result. A fixed trusted same-stack scope may prove issuer/realm statically; an actor-independent read materializes no actor context, issuer, authentication, or actor-evidence artifact and declares no unreachable denial placeholder.

  For a separate Read Model Ball, the stamp identifies its own authority and commit position. Multi-source source positions and stronger snapshot guarantees require a declared read contract or profile; a local stamp alone does not promise them. Multiple `ReadDependency` contracts remain independent target reads and do not compose into one atomic snapshot without a separate declared mechanism.

  On a cross-authority read, a trusted boundary may return an existing declared `ValidationFailure` or `AdmissionFailure` `BoundaryResponse` before invoking `read`. Once admitted, the target executes the canonical non-mutating `read(...) -> ReadResult<ResultPayload>` and returns only that declared successfully evaluated result; `BoundaryResponse`, `BusinessRejection`, and exceptions are not added to the read codomain. Wrong target authority, effective protocol identity, result mapping, or `ConsistencyStamp` fails at the boundary and is not converted into a semantic `NotFound` or non-disclosure variant. The caller, Assembly, and generated binding transport and verify the target-owned result and stamp without redefining, re-exporting, synthesizing, or altering the payload, status fact, snapshot identity, or business meaning.
- **Applicability:** `P`: Query exists, including during Draining; cross-authority/cache/status/aggregation/consistency triggers add their existing stamp/materializer fields.
- **Declaration owner:** Target read/status authority owns mapping/stamp/result; one query writer per namespace; caller owns requirement.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Exact-one/total-outcome/stamp/no-mutation, permitted/denied/redacted/non-disclosing and wrong-provenance, root candidate reserve/reject/accept/query/retry, Draining available/unavailable read, co-located/separate authority, and materializer tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Same-stack call scope and physical placement may vary; no command-authority transfer, optional fields and unreachable result categories are omitted, no multi-source atomicity, and an unaccepted candidate ID never materializes a known status row.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 8.11. Minimal lifecycle

<!-- pkb:term:start name="Draining" -->
**Draining** — a runtime lifecycle state that rejects new logical mutations, continues declared inputs for already accepted operations, and serves available declared Query/status Query paths from committed authorities. Unavailable reads fail before `read`; admitted reads retain the successfully evaluated `ReadResult` codomain, including any target-declared denial/redaction/non-disclosure payload, and create no Decision.
<!-- pkb:term:end -->


The lifecycle model in this subsection is path-triggered when a runtime independently initializes, drains, fails, quarantines, restarts, leases, or exposes the availability of a Ball instance. A caller-owned Inline object whose lifetime is fully represented by its host scope needs no seven-state enum or lifecycle protocol.

When the trigger exists, runtime lifecycle must not masquerade as arbitrary business messages. The minimal operational model is:

```text
Uninitialized -> Initializing -> Ready -> Draining -> Stopped
                              \-> Failed | Quarantined
```

- `Ready` means that the current state and required runtime records are available to accept inputs.
- `Draining`:
  - prohibits new logical mutations;
  - continues declared completion/cancellation/status inputs for already accepted operations;
  - serves every available declared `Query` and status Query from the relevant committed authority;
  - if a read authority is unavailable, the trusted boundary may return its existing declared validation/admission response before invoking `read`;
  - once admitted, canonical `read(...) -> ReadResult` returns only the declared successfully evaluated result—including any target-owned denial/redaction/non-disclosure variant—and creates no Decision.
- `Failed` means a runtime failure, not a business rejection.
- `Quarantined` prohibits further mutations until an explicit repair or recovery action.

A domain-visible lifecycle reaction exists only as a declared typed `ControlPulse`. Loss of a process or lease alone does not give a stale owner the right to commit a domain decision. Complete recovery and ownership-reacquisition transitions belong to a durable runtime extension.

### 8.12. Principal runtime concern index

This subsection is reference-only. It introduces no additional runtime component, state, field, mechanism, guarantee, or applicability rule. The marked primary source clauses remain the sole normative authorities; the references below are navigation projections to those clauses and their supporting definitions. An implementation resolves only source clauses whose existing trigger applies.

`Runtime / acceptor` is the mechanical binding role that performs applicable validation handoff, admission and reservation, atomically materializes and publishes the selected mode-specific accepted frame, schedules only its retained outputs, and converts verified mechanical observations into declared `ControlPulse` routes when they can affect later State. It owns no business schema, permission, result selection, or retry/fallback policy; it cannot create a semantic cause, alter a candidate Decision, or mutate Sovereign State outside the accepted snapshot/event publication and a later serialized `decide`. This one role definition is realized proportionally: an Inline call may representation-erase its machinery, while durable profiles materialize only their triggered records.

| Principal runtime concern | Projected Core navigation anchors |
|---|---|
| Cause and field-minimized context | §§3.3–3.4, 6.11, 8.1 |
| Finite semantic and runtime bounds | §§8.3–8.4, 10.9, 13.1–13.2; PBA-38 |
| Semantic, causal, and mechanical identities | §§3.5–3.6, 9.1–9.2 |
| Preflight, reservation, and admission | §§8.4, 8.7, 13.2 |
| Atomic Decision acceptance | §§8.5, 8.9; PBA-07 |
| Commit-before-dispatch | §8.6; PBA-08 |
| Preservation of accepted work | §§8.4, 8.8, 9.13, 12.4–12.6 |
| Command ingress, accepted result return, and pre-acceptance refusal | §§6.8–6.13, 8.4–8.9, 9.1–9.4, 10.2/10.7/10.11; PBA-18/PBA-19 |
| Results, ACKs, delivery, and trusted observations | §§6.5, 6.9–6.11, 9.3–9.5, 9.11–9.13 |
| Rejections, admission failures, and runtime faults | §§6.7, 6.13, 8.7–8.8, 13.2 |
| Persistence, recovery, and migration | §§8.9, 10.11, 12.5–12.6, 17.7 |
| Local reads and operation-status reads | §§6.3, 8.10, 9.11; PBA-30 |
| Lifecycle, ownership, and fencing | §§7.6, 8.11, 12.3–12.6 |

### Definition source records for §8

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Commit-before-dispatch" -->
**Commit-before-dispatch** — the rule under which dispatch of a `SemanticOutput` is permitted only after successful acceptance/commit of the complete source Decision frame.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Decision Work Meter" -->
**Decision Work Meter** — the immutable versioned binding-owned definition that interprets numeric `maxTransitionSteps`. It charges non-negative integral units monotonically over exactly one `decide`, never resets within that invocation, and produces equal consumption for equal canonical inputs under the same binding, transition artifact version, and meter identity/version. Exact `N` may complete, `N+1` accepts nothing, and counts under unlike meter/artifact/unit identities are not comparable.
<!-- pkb:term:end -->



---
