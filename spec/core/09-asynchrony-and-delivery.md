# Core part — Asynchrony and delivery

[Core contents](../pokeball-architecture-core.md) · [← Decision and acceptance](08-decision-and-acceptance.md) · [System composition →](10-system-composition.md)

> Canonical part 6 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 9. Asynchrony, causality, and delivery semantics

This section is a catalog of independently triggered contracts, not one mandatory operation wrapper. Reachability is derived from the closed protocol, route, profile, resource, and failure model:

| Contract | Trigger |
|---|---|
| causal identity / revision (§§9.1–9.2) | work or its result can outlive the call, reorder, retry, recover, or be observed independently; |
| independent operation facets (§9.3) | dispatch, target acceptance, business result, cancellation, or ambiguity can diverge; |
| ACK/refusal/result separation (§9.4) | delivery or target acceptance, pre-acceptance refusal, accepted business result, or delivery exhaustion is observed separately; |
| `OutcomeUnknown` (§9.5) | external execution may have occurred without a proven terminal result; |
| idempotency (§9.6) | duplicate acceptance, delivery, resume, or execution is possible; |
| cancellation (§9.7) | semantic cancellation can race accepted start, execution, or completion; abandoning only a local observer is not this trigger; |
| deadline/timeout (§9.8) | a path waits or acts under a semantic or resource deadline; |
| retry ownership (§9.9) | any semantic, transport, executor, SDK/provider, or reconciliation retry exists; hidden retries count; |
| timer (§9.10) | time must cause a later semantic Decision; |
| operation status (§9.11) | an operation outlives its initiating call or is independently queried/reconciled; |
| ordering (§9.12) | two observations can be concurrent, delayed, replayed, or reordered; |
| live/durable output (§9.13) | an accepted output leaves the call scope or a durability/delivery claim is made. |

If a trigger is absent, its types, fields, tables, tests, and `N/A` placeholders are absent. A proven synchronous local operation may use call-scope correlation and a direct result. If the external behavior is unknown, the conservative trigger applies.

### 9.1. Causal identity

<!-- pkb:term:start name="CausalToken" -->
**CausalToken** — a field-minimized portable correlation record that binds detached, addressable, late, retryable, recoverable, or independently observed work to an accepted frame; generation, revision, depth, and budget fields appear only when their actual lifecycle or growing-work triggers exist. An immediate typed call needs no token solely because it crosses a Ball boundary or involves multiple Decisions. When materialized, one causal-budget scope preserves every triggered remaining depth and cumulative-fan-out capacity without adding a second scope field. `commandSource` derives from the accepted source command frame and `resultSource` from the accepted target result frame.
<!-- pkb:term:end -->

Every detached, addressable, or routed asynchronous output has sufficient stable identity for its actual lifecycle. The `CausalToken` block below is a field-minimized structural catalog, not a mandatory record shape:

```text
CausalToken {
    operationId?            # independently tracked operation lifecycle
    semanticHandle          # detached/addressable work
    sourceBallInstanceId?   # source identity crosses call scope
    sourceCommitRevision?   # accepted source revision is observed
    sourceOrdinal           # position in the accepted source Decision
    operationGeneration?    # late or reorderable generations exist
    causalDepth?            # growing causal work needs a depth bound
    causalBudgetScope?      # growing causal work needs a shared budget
}
```

`Fact` returns the accepted `EffectRequest` token or an equivalent sufficient correlation identity when completion needs portable evidence. On a portable command path, `commandSource` identifies accepted source work and `resultSource` identifies the accepted target result frame as specified in §§6.8–6.9. An immediate same-build call uses the typed target, trusted binding, actual acceptance order, and return through the source's serialized handler; it needs no materialized token or reconstruction of absent tuple fields.

`causalDepth` and `causalBudgetScope` appear only when the actual execution needs those bounds under §8.4. A statically finite synchronous structure can bound the whole execution, including result/refusal/failure handling, without numerical depth or reservation levels; an import DAG alone does not prove termination. When a causal budget exists, yield, continuation, asynchronous handoff, and transport retry preserve the same scope and every triggered remaining depth/work capacity. They neither reset nor double-count the budget. A transport retry changes attempt metadata, not the logical operation, semantic handle, or branch identity.

<!-- pkb:pba-source:start id="PBA-18" title="Provenance-Bound Result" -->
**Source clause for PBA-18 — Provenance-Bound Result.**

- **Rule:**
  - Every result-producing Effect, Command, or accepted-subscription path binds its result to previously accepted source work with trusted provenance and correlation sufficient for that path.
  - The target owns the exact command/result mapping; the target's serialized `decide` and acceptance are the sole acceptance point for target work.
  - An accepted command result is created as target semantic output and reaches the source through its serialized result handler. Assembly neither invents accepted work nor selects business outcomes.
  - An immediate same-build call uses its typed target, trusted construction, call scope, and acceptance-before-return order as identity and provenance. Source dispatch follows acceptance and remains outside pure `decide`.
  - Crossing a Ball boundary or retaining an intermediate value for the current call alone requires no handle, source/result token, issuer field, protocol identifier, or proof of equivalence to absent tuples.
  - Detached, reordered, retryable, recoverable, or independently observed delivery preserves verified accepted-source `commandSource`, accepted-target `resultSource`, effective protocol identity, target-owned payload, and required issuer provenance.
  - Untrusted or independently delivered messages undergo the actual provenance and authenticity checks required by their boundary.
- **Applicability:** `P`: Effect/Command/subscription produces a result.
- **Declaration owner:** Effect source or command source/target owns semantic mapping; result issuer owns provenance.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Binding tests prove source acceptance before dispatch, target ownership/acceptance before accepted return, and serialized source completion; portable routes additionally verify accepted tuples, protocol identity, and required provenance.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Binding/verifier evidence may be reused; immediate calls use actual execution properties without tuple reconstruction; omit when no result path exists.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 9.2. Revisioned causality

<!-- pkb:pba-source:start id="PBA-17" title="Revisioned Causality" -->
**Source clause for PBA-17 — Revisioned Causality.**

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
<!-- pkb:pba-source:end -->
### 9.3. Independent dimensions of operation state

When dispatch, target acceptance, business result, cancellation, or ambiguity can vary independently, one flat enum must not mix them. Materialize only the reachable facets and variants from this catalog:

```text
Dispatch facet:
    NotDispatched | Dispatched | DispatchStopped

Acceptance facet:
    NotAccepted | Accepted | RejectedBeforeAcceptance | AcceptanceUnknown

Business outcome facet:
    NotExpected | Pending | Succeeded | Rejected | Failed | Cancelled | OutcomeUnknown

Cancellation facet:
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected
  | CancellationUnknown
```

An operation with all four triggered facets may simultaneously be in a state such as:

```text
dispatch = Dispatched
acceptance = AcceptanceUnknown
outcome = Pending
cancellation = Requested
```

This is a normal distributed race, not a modeling error.

When the referenced variants exist, the cross-facet invariants are:

- `NotDispatched` requires `NotAccepted` and cannot have a target-produced terminal business outcome.
- A verified `CommandRejectedBeforeAcceptance` sets `acceptance = RejectedBeforeAcceptance` and keeps `outcome = NotExpected`. A carried `DecisionRejected(BusinessRejection)` is an informational pre-acceptance reason and does not set `outcome = Rejected`.
- A verified `Fact` or `ModuleResultPulse` with accepted-operation provenance first sets `acceptance = Accepted`, then the terminal `outcome`; an accepted `ModuleResultOutput(Rejected(...))` therefore projects `acceptance = Accepted, outcome = Rejected`. After such proof, `AcceptanceUnknown + Succeeded|Rejected|Failed|Cancelled` is invalid.
- `OutcomeUnknown` may coexist with `Accepted` or `AcceptanceUnknown` until reconciliation proves a terminal result.
- `CancellationAcceptedInProgress` is not a terminal business outcome; `CancellationTooLate`, `CancellationRejected`, and `CancellationUnknown` do not erase a legitimate result.
- `DispatchStopped` is a terminal state of the current delivery policy, not a `BusinessRejection` or proof of target non-execution.

### 9.4. ACK and business result

<!-- pkb:pba-source:start id="PBA-19" title="ACK/Result Separation" -->
**Source clause for PBA-19 — ACK/Result Separation.**

- **Rule:**
  - Validation, admission, pre-acceptance Decision rejection, target acceptance, accepted business outcome, post-acceptance Resource failure/timeout/unknown, and delivery-policy exhaustion retain the exact §6.13 carrier/result/status meaning.
  - Those stages cannot rewrite one another.
  - A post-commit mechanical observation, including verified `CommandRejectedBeforeAcceptance`, changes Sovereign State only through its declared typed `ControlPulse` path.
  - An immediate typed call may return pre-acceptance refusal directly under §6.13; it creates no accepted target frame. Statically finite execution needs no reservation levels; applicable growing-work and completion-capacity bounds remain in force.
  - An accepted target outcome reaches the source through its serialized result input, represented by the immediate typed return or verified portable `ModuleResultPulse`.
  - The binding cannot rewrite the acceptance meaning of either form; a concrete return type may carry their distinct variants.

  - An ACK answers: **was the declared acceptance point reached?**
  - A `Fact` or `ModuleResultPulse` answers: **what was the accepted business outcome?**
  - A `CommandRejectedBeforeAcceptance` answers: **which verified boundary/admission/Decision rejection prevented target acceptance?**
  - A terminal delivery observation answers: **can the current delivery policy continue delivery?**

  A result may arrive before a separate ACK if it contains verifiable proof of acceptance. In that case, one serialized transition applies the proof as `acceptance = Accepted` together with the result outcome; the state `AcceptanceUnknown + proven terminal outcome` is not retained. An ACK without a result is permitted if the target accepted the work but has not yet completed it.

  For a same-identity target-command duplicate after target acceptance but before any result frame has been accepted, the only legal immediate response is verified ACK proof of the original accepted target command frame and its pending-result state. It is not a result and does not wait for one, run another target Decision, increment revision, create an Effect/Resource action, or fabricate a pending `ModuleResult`. Once the result frame exists, a duplicate may instead redeliver proof of that exact accepted result under §9.6. Conflicting fingerprint or acceptance/result evidence fails closed.
- **Applicability:** `P`: any §6.13 validation/admission/refusal/accepted-result/Resource/delivery stage is reachable or separately observed.
- **Declaration owner:** Ball owns typed outcome/facet meaning; binding owns faithful typed return or portable carrier/result/status mapping and applicable capacity admission.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Stage table, provenance, immediate return order, applicable completion capacity, dispatcher/result-route, duplicate-before-result ACK, exact post-result replay, later-failure, both-order, and conflict tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Mechanics may be shared; omit unreachable stages and never substitute one stage's carrier/status for another or create a new carrier scope.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 9.5. OutcomeUnknown

<!-- pkb:pba-source:start id="PBA-20" title="First-Class Unknown" -->
**Source clause for PBA-20 — First-Class Unknown.**

- **Rule:**
  - `OutcomeUnknown` is legal only after evidence shows that an action may have executed without a proven terminal outcome.
  - Validation, admission, pre-acceptance rejection, and an unsent action cannot be rewritten as unknown.
  - A post-acceptance timeout or ambiguous Resource/result path retains prior acceptance.
  - It uses declared reconciliation rather than a carrier or fabricated failure.

  If a request may have left the process and the target or provider may have accepted it, the absence of a response does not prove non-execution.

  ```text
  Timeout after possible send
      -> AcceptanceUnknown or OutcomeUnknown
      -> status query / reconciliation / manual decision
  ```

  A blind retry is permitted only when:

  - the target or provider guarantees idempotency on the same key;
  - the duplicate horizon covers the retry window;
  - the retry preserves semantic identity;
  - policy explicitly permits the retry.
- **Applicability:** `R`: an action may execute without proven outcome after acceptance/possible send.
- **Declaration owner:** Operation owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Stage/failure mapping, reconciliation, timeout, and fault tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Provider model may be referenced; validation/admission/unsent paths cannot use unknown, and omit only when non-execution/outcome is always proven.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->
### 9.6. Idempotency

<!-- pkb:pba-source:start id="PBA-21" title="Explicit Idempotency" -->
**Source clause for PBA-21 — Explicit Idempotency.**

- **Rule:**
  Idempotency is a contract, not a general aspiration.

  For mutation ingress:

  ```text
  IdempotencyScope + IdempotencyKey + IntentFingerprint
  ```

  - within the covered retry horizon, same key + same fingerprint redelivers proof of the original accepted root `ReplyOutput(RequestAccepted(operationId))` frame;
  - that replay preserves the original `BallInstanceId`, `CommitRevision` and `OutputId` when materialized, `semanticHandle`, `sourceOrdinal`, payload, `OperationId`, and accepted Interaction artifact/fingerprint lineage; it creates only a new mechanical delivery `AttemptId`;
  - replay runs no `decide`, creates no revision, accepted frame, semantic output, command, or status source, and cannot replace the original accepted values;
  - same key + different fingerprint is detected by the declared Interaction/idempotency authority before `Intent` construction and returns exactly `BoundaryResponse(ValidationFailure(IdempotencyConflict))`;
  - the conflict creates no operation mapping, Decision, revision, handle, Reply, output, or status source and leaves the prior accepted operation unchanged;
  - creation of the idempotency record and operation state must be atomic in a durable state profile;
  - the accepted root frame and idempotency record retention horizon must cover the declared legal retry horizon.

  The fingerprint includes business-semantic fields and stable actor and target scope, but excludes trace ID, retry count, socket address, and reply channel.

  For `ModuleCommandRequest` and `EffectRequest`, the idempotency key is normally derived from `OperationId + SemanticHandle`. A new transport attempt does not create a new logical operation.

  For a target command, the idempotency authority also retains the command fingerprint and whether the original accepted target frame has a pending or accepted result. Redelivery of the same effective protocol identity, `commandSource`, and command fingerprint before result acceptance returns only verified ACK proof of the original accepted target command frame and pending-result state. It neither blocks waiting for a result nor runs a second target Decision, increments revision, creates a `ModuleResultOutput`, repeats an Effect/Resource action, or reclassifies the refusal path. After result acceptance, redelivery returns a newly delivered proof of that exact accepted target result frame with unchanged `commandSource`, `resultSource`, effective protocol identity, `semanticHandle`, `sourceOrdinal`, payload, and target revision; only `AttemptId` changes. Same identity with a different command fingerprint or conflicting acceptance/result evidence fails closed.
- **Applicability:** `P`: duplicate acceptance/delivery/resume/execution is possible.
- **Declaration owner:** Operation/API owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Root accepted-frame replay/conflict-stage and target pending-ACK/exact-result/executor idempotency tests, including concurrency, crash, and retention horizons.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Scoped policy reusable; omit for proven non-duplicate path.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
<!-- pkb:pba-source:start id="PBA-22" title="Stable Logical Retry" -->
**Source clause for PBA-22 — Stable Logical Retry.**

- **Rule:** A transport or delivery retry preserves the accepted logical operation and semantic work identity; it creates only a new mechanical attempt.
- **Applicability:** `P`: transport/delivery retry exists.
- **Declaration owner:** Route/binding owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dispatcher retry-identity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Retry engine may be referenced; omit when disabled.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->

Pokeball does not use the term `exactly once` without identifying the exact authority and failure model.

### 9.7. Cancellation

<!-- pkb:pba-source:start id="PBA-23" title="Cancellation Is a Protocol" -->
**Source clause for PBA-23 — Cancellation Is a Protocol.**

- **Rule:**
  Cancellation is a new cause, not the erasure of a past fact.

  ```text
  CancellationRequested(targetHandle, generation, reason)
  ```

  After Interaction validation, an external user request to cancel a semantic operation is an `Intent`. An outgoing cancellation to a resource or another Ball is encoded as a typed `Effect` or `ModuleCommand`, respectively, and its outcome returns as a causally bound `Fact` or `ModuleResultPulse`. A `ControlPulse` is not an external-ingress bypass.

  The target or resource returns one of these outcomes:

  ```text
  CancellationAcceptedBeforeStart
  CancellationAcceptedInProgress
  CancellationTooLate
  CancellationRejected
  CancellationUnknown
  ```

  These are the same canonical variants as in the `Cancellation facet` in §9.3. If a result and cancellation race, both observations are accepted and serialized by the state machine. A cancellation request does not authorize ignoring a legitimate late result.

  If two matching observations prove mutually exclusive terminal business outcomes for one logical operation, arrival order does not select the “true” outcome, and the second proof does not overwrite the already accepted terminal state. An explicit conflict transition is required: the second input is classified as an invariant or provenance fault before acceptance of its `Decision`; no state or outputs are accepted for it, and the previously accepted terminal frame is preserved under §8.8. The rule is symmetric for `terminal cancellation -> contradictory result` and `terminal result -> contradictory cancellation`; an exact duplicate of the same proof is handled idempotently.
- **Applicability:** `P`: semantic cancellation can race start/execution/result.
- **Declaration owner:** Operation and provider/binding owners.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Nucleus/resource both-order race tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Provider mechanics shared; omit when cancellation is impossible.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 9.8. Deadlines and timeout

Distinguish:

- **business deadline**—the point after which a result loses its domain meaning;
- **transport timeout**—how long a caller waits for one attempt;
- **runtime execution limit**—how much CPU or wall time a resource may consume;
- **reconciliation horizon**—how long an operation remains potentially executed.

A hop may shorten a deadline but may not expand the authority of the original request.

The Nucleus uses a trusted time observation, not an ambient clock. A local scheduler may use a monotonic clock mechanically.

### 9.9. Retry ownership

Retry layers must be declared separately:

```text
semantic retry      # new decision under business policy
transport retry     # repeat of the same delivery
executor retry      # repeat of execution before/after acceptance
SDK retry           # low-level retry by the client library
reconciliation      # status/check operation after unknown outcome
```

<!-- pkb:pba-source:start id="PBA-24" title="Owned Retry Policy" -->
**Source clause for PBA-24 — Owned Retry Policy.**

- **Rule:**
  - One failure mode must have one primary retry owner.
  - Other retries are either disabled or bounded and proven semantically transparent.
  - The one-primary-owner and bounded transparent-secondary-layer rule in the two preceding bullets governs every active retrying failure mode.

  **Rationale/example:** Otherwise, `2 × 3 × 4` attempts across layers become 24 external calls.
- **Applicability:** `P`: any retry layer is active for a failure mode.
- **Declaration owner:** Owners of retry layers; exactly one primary owner is designated for each active failure mode.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Exact-one-primary, disabled-or-bounded-transparent secondary-layer, and cumulative-attempt tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Policies may be referenced per layer; inactive layers and empty tables are omitted, and reuse cannot create a second primary owner.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->
### 9.10. Timers

A timer is an external observation, not a clock read inside the Nucleus.

```text
TimerIntent(schedule/cancel, semanticTimerId, deadline, generation?)
TimerFired(semanticTimerId, scheduledAt, observedAt, generation?, firingId?)
```

Every timer path has stable timer identity and an explicit late-firing policy. `generation` materializes only when cancellation, rescheduling, or replacement can make an older firing stale. `firingId` and deduplication materialize only when duplicate or redelivered firing is possible. A durable replicated timer race protocol belongs to a separate runtime extension.

### 9.11. Operation status

This subsection materializes only when an operation outlives its initiating call, is independently queried/reconciled, or is the subject of an explicit status claim. The binding then provides a status query independent of a live reply channel. Authentication and actor-scoped namespace checks materialize only when status authorization or result selection crosses their trust/access-control trigger; the trusted binding constructs the actor-dependent `ReadContext`, while the status authority's pure semantic read owns namespace interpretation and result selection:

```text
GetOperationStatus(OperationId)
```

`OperationId` remains the identity of an accepted operation. Reserving a candidate value before `decide` does not by itself create an operation or a status identity. If root validation, admission, or the root Decision rejects before acceptance, the boundary returns the typed `BoundaryResponse` from §6.13 and creates no known status row, retention marker, `SemanticHandle`, output, or “rejected acceptance record.” A later query using that candidate value may return `NotFound` only when its committed snapshot covers the declared namespace and satisfies the same materializer-absence barrier as any other absence; the pre-acceptance response cannot create a known operation status.

When another authority performs this lookup without requiring a new accepted target record, the edge is a §10.2 `ReadDependency`: it resolves the target-owned status Query/result and status authority, returns the stamp of that selected target snapshot, and creates no target Decision, revision, or semantic output. A caller that requires provenance-bound accepted result, stable command identity, idempotent replay, status evidence, or reconciliation instead uses the versioned command path described in §6.13.

Status is a closed algebra containing only reachable lifecycle/facet variants. The following is an applicability catalog, not a mandatory base superset:

```text
NotFound                       # absence is observable in the declared namespace
Accepted                       # accepted state is independently observable
InProgress                     # progress is independently observable
Completed                      # successful terminal outcome is retained
Rejected                       # business rejection is retained
Failed                         # failure is retained
RejectedBeforeAcceptance       # an accepted source operation separately tracks downstream target nonacceptance
Cancelled                      # cancellation exists
OutcomeUnknown                 # ambiguous execution exists
DispatchStopped(               # independently delivered output can exhaust
    stopped: one-or-more bounded records {
        semanticHandle,
        reason,
        attempts,
        lastObservation
    }
)
ExpiredFromStatusRetention     # a known record can age out of status retention
```

Only variants whose comments are true for the operation are included; there is no mandatory lifecycle superset. `RejectedBeforeAcceptance` is reachable only as a facet of an already accepted source operation that separately tracks whether a downstream target accepted its command. It is never the root lifecycle of the unaccepted operation identified by the queried `OperationId`. `Rejected`, `Failed`, and `Cancelled` remain distinct terminal outcomes; `DispatchStopped` reports only delivery-policy exhaustion and remains separate from business outcome and `OutcomeUnknown`. Reply delivery is not the sole source of a terminal outcome.

A status query reads one declared committed **operation status authority**: a status ledger, `ReadModelState`, or other query-oriented state with its own revision. Each declared status namespace has exactly one logical writer for its records, bounded pending state, source coverage, retention markers, and revision. It losslessly projects only the sources required by its reachable variants—acceptance/rejection records, workflow state, accepted results, and trusted runtime delivery observations when each exists—but does not become a second command authority for the original business facts. A `ConsistencyStamp` refers to a snapshot of this status authority; if the authority is materialized from multiple sources, the stamp does not promise their atomic freshness. Tables, processes, co-location, and other physical representation are selected by the project/binding and do not change this ownership contract.

The ownership trade-off is explicit: co-locating status with a source authority may reduce lag and transaction boundaries, while a separate status/Read Model authority isolates query load and combines declared sources at the cost of materialization lag and source-position evidence. Either choice still has one status query authority for the namespace and never transfers command authority or ownership of the underlying business facts. Two independently writable status answers for the same namespace are not an alternative trade-off.

The canonical status materializer applies a causal source or trusted observation only after the source positions and prerequisite records declared by that status contract are available. When a valid observation arrives first, it is retained in finite bounded pending state by its operation and causal identity; it is neither dropped as `NotFound` nor exposed as a lossy partial status row. When the prerequisite source arrives, the single writer applies the source and every now-applicable pending item in one committed revision. The selected profile retains the source/observation until application for its declared horizon; a `Transient` binding promises this only for process lifetime, while a durable promise requires the corresponding retained-source mechanism and evidence.

Merge is explicit and monotonic:

- an equivalent duplicate with the same causal identity is idempotent and creates no new revision merely for redelivery;
- compatible evidence refines only the facet it proves, while lifecycle, cancellation, accepted result, and delivery-stop facets that remain applicable are retained independently and losslessly;
- weaker, older, or less authoritative evidence does not regress a proven facet or erase another facet;
- nonequivalent evidence for the same causal identity, source position, generation, or unique stop key is an invariant/provenance conflict and fails closed without arrival-order overwrite;
- independent delivery-stop keys merge in the status contract's canonical order without eviction of an existing key.

Before accepting any source operation or output that can first make a status record, pending item, reachable facet, retention marker, or unique delivery-stop slot necessary, the source/status capacity plan reserves the complete newly reachable capacity. If the reservation would exceed an effective bound, the source is not accepted and nothing from that source is dispatched. Once the source is accepted, a valid status fact remains retained or bounded-pending until it is applied; materializer lag may be visible through its own stamp, but pressure never permits eviction, truncation, silent drop, or rollback of that accepted fact.

When retention expiry is exposed, the materializer commits the retention marker only after every declared source position or source horizon capable of producing status evidence for the operation covers it and the operation's pending set is empty. The marker is committed before the known record may become absent. While the marker is retained, a duplicate or observation at or before its covered source position leaves it unchanged; conflicting supposedly covered evidence fails closed and never resurrects the operation. Only expiry of the declared marker horizon permits the later absence that maps to `NotFound`.

When `NotFound` exists, it is permitted only when the snapshot read covers the request's declared namespace—authenticated when the access-control trigger applies—and contains neither an operation record nor a retention marker after the materializer barrier above. When status retention can expire and the expiry distinction is exposed, a known record is first atomically replaced by an `ExpiredFromStatusRetention` marker; while the marker is retained, the query returns exactly that variant. After the declared marker horizon expires, absence once again means only `NotFound`. A status contract without absence or expiry semantics declares neither variant nor marker.

When the delivery-stop trigger exists, every `DispatchStopped` record comes from a trusted terminal observation, contains the complete `SemanticHandle`, reason, attempts, and last observation, and does not overwrite business lifecycle, cancellation, or a known outcome. If several outputs/steps dispatch independently, status stores a bounded unique set rather than selecting one. The collection has a finite effective bound, canonical order, and the idempotent/monotonic conflict behavior above. A domain-specific status payload represents the entire applicable set above as a closed, lossless model; absent facets need no variants.

For an accepted `ModuleResultOutput`, its accepted target frame is a status source fact even if the target crashes before result dispatch. A stopped result delivery additionally uses the exact tuple `(effectiveProtocolIdentity, commandSource, resultSource)` as its unique result-delivery key. The target materializer retains the accepted-result and delivery-stop facts under its selected profile/horizon. Neither fact creates or overwrites a source business/status facet; after target result-route exhaustion, the source remains at its previously proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` state until a verified result Pulse, separate ACK, or declared reconciliation evidence reaches the source.

### 9.12. Ordering

Ordering guarantees are local and explicit:

- Pulses for one instance are serialized by its single-writer mechanism.
- Outputs of one Decision have a `sourceOrdinal`.
- Synchronous completion is processed only after acceptance of the current Decision.
- Parallel Effects and Commands may complete in any order.
- There is no global order between different BallInstances.
- Transport order does not replace business sequence.

If two external actions must execute sequentially, a subsequent Decision creates the second output after the required outcome or acceptance of the first, or a separate ordered contract is used. Incidental worker scheduling must not be relied upon.

### 9.13. Live and durable outputs

By default:

| Output | Baseline semantics | Stronger semantics require |
|---|---|---|
| `ProjectionOutput` | Live delivery; drop or resync is permitted after source acceptance | Durable stream/subscription extension |
| `ReplyOutput` | Bound to a request channel and may be lost | Durable channel acceptance + status fallback |
| `EffectRequest` | Source accepted; execution contract is separate | Durable executor/provider idempotency/status |
| `ModuleCommandRequest` | Source accepted; target acceptance is separate | Target inbox/dedup/ACK contract |
| `ModuleResultOutput` | Target result accepted; source observation is separate | Retained result route/dedup/status contract |
| `SignalPublication` | Bounded publication | Broker append/replay contract |
| `TimerRequest` | Source accepted; scheduler contract is separate | Durable timer race/recovery contract |

Durability of source state does not automatically extend to every output channel.

<!-- pkb:pba-source:start id="PBA-42" title="Honest Guarantee Scope" -->
**Source clause for PBA-42 — Honest Guarantee Scope.**

- **Rule:** Any delivery, durability, recovery, receipt, acceptance, once-only, RPO, or RTO claim is limited to its exact named boundary, scope, assumptions, retention, and evidence; source durability or retained pending work alone does not imply a stronger downstream guarantee.
- **Applicability:** `C`: delivery/durability/recovery/RPO/RTO/receipt/acceptance/once-only claim is made.
- **Declaration owner:** Claimant and binding owner.
- **Scope:** The exact named guarantee boundary, claim scope, assumptions, and retention horizon stated by the claim.
- **Enforcement / evidence owner:** Claimant and binding evidence owner for the named boundary, crash/recovery/delivery behavior, retention, and every downstream non-guarantee.
- **Resolution, failure, and conformance:** Missing boundary, scope, assumptions, retention, or evidence prohibits the claim; source durability or retained pending work alone cannot satisfy or strengthen it.
- **Reuse and absent-trigger behavior:** Failure model/evidence reusable only in identical scope; weaker/no claim needs no table.
- **Primary verification route:** `§17.7`
<!-- pkb:pba-source:end -->

If a binding uses the term `at-least-once`, it must name the exact delivery boundary. In Core, this can mean only duplicate-permitting attempts to transfer an already accepted output to the named executor or transport boundary under explicitly stated liveness assumptions; it does not mean target receipt, target acceptance, or business success.

When a delivery policy retries, its attempts and retry horizon are finite. When a retained/retrying policy exposes exhaustion, the runtime records `DispatchStopped` with the available reason and attempt evidence; a durable profile preserves the accepted output and triggered terminal observation until their declared retention horizon, while a Transient profile is limited to the process lifetime. A non-retrying live delivery has neither an attempts ledger nor `DispatchStopped`. No policy may retry indefinitely, reset identity/budget, or claim target delivery. A stronger claim must define the boundary, availability assumptions, recovery ownership, retention, and any applicable status or reconciliation policy.

If the target crashes after accepting a `ModuleResultOutput` but before result dispatch, that result remains an accepted target fact under the selected target state/output profile. Retry exhaustion creates target `DispatchStopped` keyed by the complete result-delivery tuple; it does not fabricate source receipt or change the source business outcome. The source remains pending or unknown according to its already proven facets until result delivery or reconciliation supplies accepted-target evidence.

### Definition source records for §9

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="Delivery Observation" -->
**Delivery Observation** — a provenance-bound mechanical fact about a dispatch attempt, target acceptance or pre-acceptance refusal, ambiguity, or exhaustion of the delivery policy; it differs from a business `Fact`/`ModuleResultPulse` and affects Sovereign State only through a declared typed `ControlPulse`. A stopped target result delivery uses `(effectiveProtocolIdentity, commandSource, resultSource)` and sets no source business facet.
<!-- pkb:term:end -->

<!-- pkb:term:start name="DispatchStopped" -->
**DispatchStopped** — a terminal delivery observation that the declared finite dispatch policy is exhausted; it does not prove business failure, cancellation, or non-execution. Operation status with multiple independently delivered outputs/steps retains a bounded record for every stopped `SemanticHandle` rather than selecting one.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Operation Status Authority" -->
**Operation Status Authority** — the one committed revisioned single-writer query authority for a declared status namespace. It losslessly materializes only reachable lifecycle, acceptance, cancellation, accepted-result, ambiguity, delivery-stop, and retention facets through causal-order application or bounded pending, idempotent monotonic conflict handling, pre-acceptance capacity reservation with no later eviction/truncation, and a covered-source/empty-pending marker before absence with no resurrection. Co-location can reduce lag while separation can isolate query load; either form remains one query authority, transfers no command/business-fact authority, and makes no unsupported cross-source freshness promise.
<!-- pkb:term:end -->

<!-- pkb:term:start name="OutcomeUnknown" -->
**OutcomeUnknown** — a state in which an external action might have occurred but no proven outcome exists. Validation, admission, pre-acceptance rejection, and an unsent action cannot create it; prior acceptance remains while reconciliation seeks evidence.
<!-- pkb:term:end -->


---
