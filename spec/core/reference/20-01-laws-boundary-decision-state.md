# Core part — Laws PBA-01–18

[Core contents](../../pokeball-architecture-core.md) · [← Practical checklist and anti-patterns](../verification/18-checklist-and-antipatterns.md) · [Laws PBA-19–30 →](20-02-laws-delivery-composition.md)

> Canonical part 19 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 20. Canonical Pokeball laws

The 44 entries below are the complete, set-equal audit projection of the Core-embedded source records in the numbered body. Each entry copies all eight fields—Rule, Applicability, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route—without creating independent normative authority.

<!-- pkb:generated:start id="core-laws-01-18" -->
### PBA-01 — Three-Zone Boundary

Source: §5.

- **Rule:**
  Every application `Ball` has the logically separated Interaction, Nucleus, and Resource/route roles defined in §§5.1–5.3. This separation preserves their distinct authority and permitted calls even when one file, call stack, or generated binding uses **representation erasure**—omitting a materialized wrapper or adapter while proving the same semantic facts and role edges (§22); physical co-location alone is not evidence of separation.
  The logical-role and verification/construction-edge map may be carried directly by authoritative source and its enclosing binding: typed entrypoints, visibility/import restrictions, actual call sites, verified construction sites, and accepted-write sites. When those facts are unambiguous and inspectable there, no separate map document, table, or manifest is required. Add annotations or exact source references only for facts not already evident; an annotation is not enforcement. Every present path still has an identifiable role, authority, verified origin where required, and permitted call direction; empty Resource roles require no implementation artifact.
- **Applicability:** `A`: every Ball, including same-file/stack/generated layouts.
- **Declaration owner:** Ball owner defines the logical-role map and distinct authority.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Binding source/type graph, role-versus-Trusted-Boundary-edge map, call-graph, provenance, and accepted-write evidence.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared layout/linter may be referenced; physical adapters and empty-zone packages are optional, role separation is not.
- **Primary verification route:** `§17.5`

### PBA-02 — Polar Isolation

Source: §5.4.

- **Rule:** Interaction and Resource/route roles form no direct application or business path. Shared or generated mechanics may serve both only when they carry no mutable business meaning and create no hidden communication path.
- **Applicability:** `A`: every Ball.
- **Declaration owner:** Ball owner defines permitted role edges.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Compiler/dependency/call-graph and hidden-communication tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared graph rule may be reused; mechanical sharing proves no mutable business path rather than adding a per-Ball boolean.
- **Primary verification route:** `§17.5`

### PBA-03 — Pure Bounded Nucleus

Source: §5.2.

- **Rule:** For every Decision path, the Nucleus owns the semantic schema and interpretation of its explicit inputs and context, is the sole local business-decision and business-permission authority, performs no I/O or raw provenance verification, and terminates within the effective bounds resolved by §8.3. When a numeric `maxTransitionSteps` declaration supplies that bound rather than a static proof alone, the binding-owned Decision Work Meter from §8.3 measures it without changing business meaning.
- **Applicability:** `A`: every Decision path; a numeric `maxTransitionSteps` declaration activates the exact meter contract.
- **Declaration owner:** Ball/Nucleus owns pure signature, semantic context interpretation, policy, and bounds; binding owns meter mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Nucleus/binding purity, sole-policy-site, provenance-boundary, termination, static-bound or meter-resolution/determinism tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact meter mechanics may be referenced; semantic ownership remains local, and a static proof alone needs no meter artifact.
- **Primary verification route:** `§17.1`

### PBA-04 — Closed Protocol

Source: §6.14.

- **Rule:**
  - For a given protocol version, the set of variants must be statically known and handled exhaustively.
  - Every concrete profile/binding likewise exposes one finite closed `AdmissionFailure.reason` union.
  - Every concrete profile/binding rejects unknown or open-string reasons.
  - Generic `Message`, `Any`, `Map<String, Any>`, and string-based handler discovery are not a canonical protocol.
  - Pre-semantic `ValidationFailure` covers only representation or a declared closed-protocol/type invariant whose truth is independent of committed State, semantic Context, and business policy.
  - A rule owned as a business choice, or whose result depends on committed State or semantic Context, is evaluated by the Nucleus: a mutation returns `Rejected(BusinessRejection)` before acceptance, while an admitted read selects one target-owned `ResultPayload` variant.
  - A fixed range, size, or shape constraint may be enforced by Interaction only when the protocol owner deliberately declares it as a versioned closed-type invariant. Moving a constraint between that invariant and Nucleus policy changes protocol semantics and follows §10.11 compatibility rules.
- **Applicability:** `A`: every used protocol category; `P`: admission can fail.
- **Declaration owner:** Protocol/profile/binding owner owns the closed variants/reasons.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Type/schema exhaustiveness, compatibility, and unknown/open-reason rejection.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact protocol ref may replace a copied list; absent category/admission path is empty and has no reason union.
- **Primary verification route:** `§17.1`

### PBA-05 — Explicit Decision Inputs

Source: §8.1.

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

### PBA-06 — Controlled Causality

Source: §5.2.

- **Rule:** When a semantic external-action output exists, its semantic intent is created only by the Nucleus inside a Decision. This includes constructing a target-owned `ModuleCommand` through its exact declared imported Application Surface; Interaction and Assembly verify, bind, and transport but do not create or synthesize that semantic intent.
- **Applicability:** `P`: semantic external-action output exists.
- **Declaration owner:** Ball Nucleus/output protocol.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency rule and output tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Envelope mechanics may be shared; omit when output set is empty.
- **Primary verification route:** `§17.1`

### PBA-07 — Atomic Decision

Source: §8.5.

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

### PBA-08 — Commit Before Dispatch

Source: §8.6.

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

### PBA-09 — No Reentrant Transition

Source: §8.4.

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

### PBA-10 — Fault Atomicity

Source: §8.8.

- **Rule:**
  A failure is classified at its exact §6.13 stage:

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

### PBA-11 — Single Semantic Authority

Source: §7.2.

- **Rule:**
  > **Within one overlapping semantic scope, there cannot be two independent writers that each consider their own value canonical.**

  The scope may include:

  ```text
  semanticId
  namespace / tenant / realm
  stateKey or key range
  region or jurisdiction
  validity interval
  ```

  An external provider or database may be the source of record. In that case, the local `Ball` owns decision state, operation state, or a replica, but does not pretend to own the external canonical fact.
- **Applicability:** `A`: every mutable semantic fact.
- **Declaration owner:** Project ownership map and owning Ball.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Access boundaries/ownership tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One State Belt may be referenced; no copied ownership prose.
- **Primary verification route:** `§17.5`

### PBA-12 — Single Writer

Source: §7.6.

- **Rule:**
  At any moment, one `BallInstance` has one logical writer. The Inline profile provides this through call discipline; a concurrent profile through a single-writer loop; and a movable durable profile through a storage-enforced ownership epoch or fence.

  A lease or process-local mutex is insufficient if two runtimes can independently consider themselves the owner and write to the same authoritative store.
- **Applicability:** `A`: sequential acceptance; `R`: owner can move.
- **Declaration owner:** Execution/state binding.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Call loop or storage fence and concurrency tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Binding contract reusable in exact store scope; omit fencing for fixed owner.
- **Primary verification route:** `§17.6`

### PBA-13 — State Isolation

Source: §7.4.

- **Rule:**
  One Ball's `Nucleus` MUST NOT read another Ball's mutable state through a memory reference, global-store selector, or shared ORM session.

  Permitted ways to obtain another Ball's data:

  - public read contract;
  - versioned immutable snapshot;
  - target-owned `ModuleResult` carried by a verified `ModuleResultPulse`;
  - observed signal/event;
  - Read Model;
  - target-side validation/reservation.

  The view layer may combine multiple projections for presentation, provided that the combination makes no cross-authority business decision and is not claimed to be a consistent snapshot without a separate mechanism.
- **Applicability:** `A`: every Ball graph.
- **Declaration owner:** Ball and Assembly.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports/storage boundaries.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared graph check; no `noCrossStateReads` flag.
- **Primary verification route:** `§17.5`

### PBA-14 — Explicit State Kind

Source: §7.1.

- **Rule:**
  - The state kinds in this subsection are distinct authority and meaning categories and are not interchangeable.
  - `EphemeralState` contains only UI/transport mechanics that cannot change a business Decision.
  - A decision-relevant UI/transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext` input, never hidden ephemeral authority.

  | Kind | Owner | Meaning |
  |---|---|---|
  | `SovereignState` | Nucleus of a specific Ball | Canonical mutable semantic facts |
  | `EphemeralState` | Interaction/transport | UI and transport mechanics that cannot change a business Decision |
  | `ReplicaState` | Resource/read adapter | Cache or copy of an external source with provenance |
  | `CapturedInput` | Flow/operation owner | Immutable versioned snapshot for decision and recovery |
  | `ReadModelState` | Read Model Ball | Derived query-oriented state |
  | `Projection` | No one | Immutable output, not an authority |
  | `RuntimeState` | Runtime | Mailbox, claims, attempts, breaker, tracing |
- **Applicability:** `A`: every state-like value, including UI/transport values that may affect a Decision.
- **Declaration owner:** State/protocol owner; Interaction owns only non-decision mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Type/state mapping plus decision-relevant UI current-input/retention tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Reuse vocabulary; non-decision mechanics may be ephemeral, absent state kinds need no row.
- **Primary verification route:** `§17.1`

### PBA-15 — Semantic Handle

Source: §3.5.

- **Rule:**
  Semantic state refers to planned work through a stable `SemanticHandle` when that work is retained, can outlive the call, can be retried or reordered, is cancellable or reconcilable, survives recovery, crosses a Ball boundary, or appears in operation status:

  ```text
  SemanticHandle {
      operationId
      outputKind
      localOrdinalOrName
  }
  ```

  The runtime may store a mapping for such detached work:

  ```text
  SemanticHandle -> OutputId
  ```

  but it does not rewrite business state merely to materialize a transport ID. An immediate local output that is completely consumed in the accepted call scope and has no detached-work trigger uses its accepted frame position; it does not require an `OperationId`, `SemanticHandle`, or wrapper object merely for uniformity.

  The stable semantic-identity rule for detached or addressable planned work is the `SemanticHandle` contract in the preceding paragraph; immediate accepted call-scope work remains identified by its accepted frame position.
- **Applicability:** `P`: work is retained, detached, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible.
- **Declaration owner:** Ball state/protocol owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Nucleus/runtime identity mapping tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Allocation may be shared; omit operation/handle artifacts for immediate call-scope work.
- **Primary verification route:** `§17.1`

### PBA-16 — Trusted Identifier Allocation

Source: §3.5.

- **Rule:**
  - Semantic IDs must be known before `decide`.
  - The `Nucleus` MUST NOT read an ambient random generator.
  - Semantic IDs arrive as:
    - a validated client-supplied ID, if the contract permits one;
    - a deterministic value derived from existing semantic data;
    - a trusted reserved ID from `DecisionContext`;
    - an ID reserved by ingress or the runtime before evaluation.
  - A reserved `OperationId` is only a candidate semantic value until the root operation is accepted.
  - If root validation, admission, or `decide` rejects before acceptance, that candidate creates no accepted operation record, authoritative `SemanticHandle`, output, status source, or retention marker.
  - The root `BoundaryResponse` does not present that candidate as an authoritative `OperationId` or status lookup key.
  - Mechanical IDs belong to the runtime.
  - Mechanical IDs may appear only after acceptance or commit and must not be required for a domain decision.

  **Examples and mechanics:**

  Illustrative semantic IDs:

  ```text
  OperationId
  OrderId
  PaymentId
  SearchSessionId
  ```

  Illustrative mechanical IDs:

  ```text
  CommitId
  OutputId
  AttemptId
  StorageTransactionId
  ```
- **Applicability:** `P`: Decision needs a newly allocated or trust-validated semantic ID.
- **Declaration owner:** Ball and ingress/context issuer.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Allocator/validator, purity/collision, and root reserve/reject/accept identity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Issuer policy may be referenced; omit reservation when no new ID exists.
- **Primary verification route:** `§17.4`

### PBA-17 — Revisioned Causality

Source: §9.2.

- **Rule:**
  - A late, duplicate, or reordered result or trusted observation is applied only to its matching operation/handle/generation under an explicit merge rule.
  - A triggered status materializer:
    - applies causal sources in order or retains them in bounded pending state,
    - merges equivalent duplicates idempotently,
    - and treats nonequivalent same-key evidence as a fail-closed conflict rather than selecting truth by arrival order.

  A late result is not applied merely because it is valid against the schema.

  Example:

  ```text
  Search A: operationGeneration = 7, query = "phone"
  Search B: operationGeneration = 8, query = "laptop"

  Result A arrives after Result B.
  ```

  The `Nucleus` applies an explicit policy:

  ```text
  AcceptCurrentGeneration
  IgnoreStale
  MergeByDeclaredAlgebra
  StoreAsHistoricalObservation
  TriggerResync
  ```

  The scheduler's incidental completion order is not a conflict policy.
- **Applicability:** `P`: result or trusted observation may be late, duplicate, or reordered; status materialization adds causal pending/merge.
- **Declaration owner:** Ball or status matching/merge authority.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Matching, bounded-pending, duplicate, conflict, and reordering tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Envelope/materializer mechanics may be shared; semantic keys and merge remain authority-owned; omit for proven synchronous order.
- **Primary verification route:** `§17.1`

### PBA-18 — Provenance-Bound Result

Source: §9.1.

- **Rule:**
  - Every result-producing Effect, Command, or accepted-subscription path binds its result to previously accepted source work with trusted provenance and correlation sufficient for that path.
  - For a command path, the target owns one exact command/result mapping.
  - A trusted target boundary constructs `ModuleCommandPulse` only from the verified accepted source frame.
  - Target `decide` is the sole acceptance point.
  - A command result is created only as `ModuleResultOutput` in an accepted target Decision.
  - It reaches the source only as a verified `ModuleResultPulse`.
  - It preserves the accepted source `commandSource`.
  - It preserves the accepted target `resultSource`.
  - It preserves the effective protocol identity.
  - It preserves the target-owned payload.
  - Assembly transports and does not synthesize or modify those identities or payload.
  - Detached or reorderable results materialize stable causal identity.
  - Same-stack erasure proves the same accepted tuples.
- **Applicability:** `P`: Effect/Command/subscription produces a result.
- **Declaration owner:** Effect source or command source/target owns semantic mapping; result issuer owns provenance.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Resource/route verifier proves accepted source and, for commands, target tuples plus effective protocol identity.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Verifier may be referenced; same-stack erasure proves equal tuples; omit when no result path exists.
- **Primary verification route:** `§17.1`
<!-- pkb:generated:end -->
