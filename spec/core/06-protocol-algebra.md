# Core part — Protocol algebra

[Core contents](../pokeball-architecture-core.md) · [← Model, boundaries, and zones](03-model-boundaries-zones.md) · [State and authority →](07-state-and-authority.md)

> Canonical part 3 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 6. Protocol algebra

### 6.1. Surfaces

Every `Ball` separates at least three protocol surfaces.

| Surface | Purpose | Primary types |
|---|---|---|
| External Interaction | User, HTTP, CLI, OS, UI | `Intent`, `Query`, `ReadResult`, `BoundaryResponse`, `Projection`, `Reply` |
| Private Resource | Owned adapters and executors | `Effect`, `Fact` |
| Integration | Other Balls and Flows | target-owned `ModuleCommand`/`ModuleResult`, `ModuleCommandPulse`, `ModuleResultPulse`, `ModuleResultOutput`, `Signal`, public read contracts |

The surfaces are logical ownership boundaries, not mandatory non-empty APIs. A Ball declares only the closed variants it uses; an empty Resource or Integration surface requires no types, files, limit zeros, or manifest section.

A transport envelope, serialization, authentication token, and broker metadata do not automatically become part of the semantic payload.

### 6.2. Intent

<!-- pkb:term:start name="Intent" -->
**Intent** — a validated request to initiate a state change or operation.
<!-- pkb:term:end -->

```text
SearchRequested(query, pageSize)
CheckoutStarted(cartId, paymentMethodRef)
DocumentCloseRequested(documentId)
```

An `Intent` contains no raw HTTP request, JSON tree, mutable DTO, repository or service reference, or arbitrary map.

### 6.3. Query

<!-- pkb:term:start name="Query" -->
**Query** — a non-mutating read request to committed state or a read authority; for a concrete protocol version it maps unambiguously to one target-owned result payload that is total over every reachable post-admission semantic outcome.
<!-- pkb:term:end -->

```text
GetCatalogView
GetCheckoutStatus(operationId)
```

<!-- pkb:term:start name="ConsistencyStamp" -->
**ConsistencyStamp** — the exact available identity of the committed snapshot used by a successful read when the stamped-read trigger applies: revision plus only the triggered instance/schema identities; by itself it promises neither subsequent freshness nor multi-source atomicity.
<!-- pkb:term:end -->

The records below are structural projections of the marked definitions.

<!-- pkb:term:start name="ReadContext" -->
**ReadContext** — a field-minimized trusted local-read context whose actor-dependent form is verified, bounded, and constructed by the trusted binding boundary under a Ball/Nucleus-owned semantic schema. Actor context appears only when it changes read authorization/result selection; fixed same-stack issuer/realm may be proven statically, actor-independent reads create no actor artifacts, and selectors belong to the typed `Query`.
<!-- pkb:term:end -->

The records below are structural projections of the marked definitions.

<!-- pkb:term:start name="CommittedStateSnapshot" -->
**CommittedStateSnapshot** — immutable stamped-read input that combines Sovereign State with its `CommitRevision`; `BallInstanceId` and `stateSchemaVersion` appear only when instance or persisted-schema identity is observed.
<!-- pkb:term:end -->

The records below are structural projections of the marked definitions.

Materialized stamped `read`, used when the §8.10/`PBA-30` trigger applies:

```text
CommittedStateSnapshot<State> {
    commitRevision
    ballInstanceId?        # instance identity crosses scope
    stateSchemaVersion?    # persisted/migrated schema identity is observed
    state: State
}

ReadContext {
    protocolVersion?       # independently versioned boundary
    actorContext?          # read authorization/result selection depends on actor context
}

ConsistencyStamp {
    commitRevision
    ballInstanceId?
    stateSchemaVersion?
}

ReadResult<ResultPayload> {
    payload: ResultPayload
    consistencyStamp: ConsistencyStamp
}

read(
    snapshot: CommittedStateSnapshot<State>,
    query: Query,
    context: ReadContext
) -> ReadResult<ResultPayload>
```

For each `Query` in one effective protocol identity, the authoritative typed source or manifest MUST map exactly one statically known `ResultPayload` unambiguously. The materialized records above are field-minimized: `commitRevision` identifies the stamped snapshot; instance identity appears only when more than one instance or a cross-scope identity path exists; schema identity appears only when persisted/migrated state identity is observed; `protocolVersion` appears only at an independently versioned boundary; and actor context appears only when read authorization depends on it. Exact enclosing type/build scope may prove an optional identity without a runtime field. Additional selectors are fields of the `Query` itself, not ambient observations. A `ConsistencyStamp` identifies the committed snapshot from which the payload was built, but promises neither freshness after return nor multi-source atomicity.

When committed State, trusted `ReadContext`, or read policy can change permission or result selection, that one target-owned `ResultPayload` MUST be a closed union total over every reachable post-admission semantic outcome for the Query. It contains exactly the applicable permitted, denied, redacted, and deliberately non-disclosing variants; a path that cannot reach one of those categories declares no placeholder. The pure Nucleus/read Policy Gate selects the variant after admission. Successful read evaluation means that this target-owned semantic selection completed; it does not mean that access was permitted or that an underlying record was disclosed.

A target may intentionally make policy denial observationally indistinguishable from absence only by declaring one exact non-disclosing result variant and its mapping in the target protocol. The boundary, caller, Assembly, runtime, and generated binding cannot invent or substitute `NotFound`, redaction, denial, exception, `BusinessRejection`, or another carrier. Wrong provenance, target, version, authority, mapping, or stamp remains a boundary failure and never becomes a semantic non-disclosure result.

When actor context can change read authorization or select the returned semantic result, the trusted binding boundary constructs `ReadContext` from verified, bounded, field-minimized observations under `PBA-44`; the Ball/Nucleus owns the context schema and pure interpretation. A fixed trusted same-stack issuer/realm may be proven statically without materialized issuer, authentication, or actor-evidence fields. An actor-independent Query uses `Unit` or an actor-free `ReadContext` and creates no actor artifact.

A same-stack local getter with no cache, cross-time comparison, status-absence proof, aggregation, or freshness/consistency claim may representation-erase the wrappers:

```text
read(state: State, query: Query, context: Unit | ReadContext)
    -> ResultPayload
```

The call-scope immutable state and closed result type prove the same non-mutation and result-mapping invariants; no `BallInstanceId`, revision, schema version, or stamp object is fabricated.

A materialized `ReadResult` is only a successfully evaluated non-committed response: its payload may itself be a target-declared denied, redacted, or non-disclosing semantic outcome. It is not a `SemanticOutput`, has no `SemanticHandle`, `sourceOrdinal`, or new revision, and is not subject to commit-before-dispatch. An unsuccessful boundary, validation, or admission path returns the existing `BoundaryResponse`, not an invented read-result variant. Any local read form does not mutate state, create an `Effect`, or hide external I/O. If a sensitive read requires an atomic audit record, model it as an explicit audited operation or a separate audited-read profile, not as a hidden side effect inside pure `read`.

### 6.4. Effect

<!-- pkb:term:start name="Effect" -->
**Effect** — a private declarative external operation of the owning Ball.
<!-- pkb:term:end -->

```text
FindProducts(query, pageSize)
PersistSession(snapshot)
ReadCurrentTime
CaptureProviderPayment(...)
```

An `Effect` describes a permitted action, not a service object or function call.

### 6.5. Fact

<!-- pkb:term:start name="Fact" -->
**Fact** — a validated provenance-bound outcome of a previously accepted `EffectRequest`; immediate completion may use call-scope correlation, while detached/reorderable completion and subscription observations carry stable materialized causal identity.
<!-- pkb:term:end -->

```text
ProductsFound(handle, products)
ProductSearchFailed(handle, reason)
CurrentTimeObserved(handle, timestamp)
```

A Fact contains a causal reference that matches it to the accepted `EffectRequest`. An immediate synchronous completion may use accepted call-scope position; a completion that can detach, reorder, retry, recover, or be observed independently carries the complete stable handle/source identity from §9.1. A push or subscription observation is a `Fact` only when the subscription itself was previously accepted and the observation returns its stable `SemanticHandle`. An unsolicited resource observation must not masquerade as a `Fact`.

### 6.6. Projection

<!-- pkb:term:start name="Projection" -->
**Projection** — immutable semantic view state for a consumer/renderer.
<!-- pkb:term:end -->

```text
CatalogSearchStatus(operationId, lifecycle, cancellation)
CheckoutReady(summary)
```

A Projection is not a database entity, mutable shared view model, or transport DTO.

### 6.7. BoundaryResponse and Reply

<!-- pkb:term:start name="BoundaryResponse" -->
**BoundaryResponse** — a stage-specific boundary/runtime response for a request with no accepted Decision: `ValidationFailure`, `AdmissionFailure`, or `DecisionRejected`; it is not a `SemanticOutput` and does not pass through commit-before-dispatch. On a command route, a verified response may be carried back only inside `CommandRejectedBeforeAcceptance`; it cannot represent an accepted target result, post-acceptance Resource outcome, or delivery stop.
<!-- pkb:term:end -->

```text
BoundaryResponse =
    ValidationFailure
  | AdmissionFailure
  | DecisionRejected(BusinessRejection)
```

A `BoundaryResponse` is not a `SemanticOutput`, has no `SemanticHandle`, `sourceOrdinal`, or `CommitRevision`, and is not subject to commit-before-dispatch. Malformed input and authentication or validation failures are rejected before an `Intent` is created; Interaction encodes an actual Nucleus `Rejected(BusinessRejection)` as `DecisionRejected`, without turning it into a committed `Reply`.

An ingress idempotency mismatch is detected by the declared Interaction/idempotency authority before it constructs an `Intent`. Within the covered record horizon, same `IdempotencyScope + IdempotencyKey` with a different `IntentFingerprint` returns exactly `BoundaryResponse(ValidationFailure(IdempotencyConflict))`. It is never `DecisionRejected`: no Nucleus rejection occurred. It creates no candidate or accepted operation mapping, Decision, revision, handle, Reply, output, or status source and leaves the previously accepted operation unchanged. By contrast, a same-fingerprint retry of an already accepted root is not a `BoundaryResponse`; §9.6 redelivers proof of its original accepted `ReplyOutput` frame.

<!-- pkb:term:start name="Reply" -->
**Reply** — an addressed semantic response bound to a request/operation and existing only as the payload of an accepted `ReplyOutput`; a pre-acceptance boundary failure or rejection is not a Reply.
<!-- pkb:term:end -->

```text
RequestAccepted(operationId)
CheckoutCompleted(orderId)
```

A `Reply` differs from a `Projection`: a reply has requester and correlation semantics, whereas a projection describes an observable representation of state. A `Reply` exists only as the payload of an accepted `ReplyOutput`.

### 6.8. ModuleCommand

<!-- pkb:term:start name="ModuleCommand" -->
**ModuleCommand** — a target-owned addressed public command payload for another Ball authority.
<!-- pkb:term:end -->

```text
Counter.increment()
Inventory.ReserveItems(...)
Payment.Capture(...)
```

A Command is not a private `Effect`. For each command operation, the target owns exactly one closed `ModuleCommand -> ModuleResult` mapping with defined compatibility semantics. The caller imports that mapping through its declared command dependency; it neither aliases the command to an external `Intent` nor redeclares either payload as caller-owned. A same-build interface can define the operation and its reachable return variants without a separate protocol identifier or version field.

<!-- pkb:term:start name="ModuleCommandPulse" -->
**ModuleCommandPulse** — the target's trusted command input from previously accepted source work; an immediate same-build call uses its typed target and call scope, while independently delivered input carries verified accepted-source identity and provenance. Target `decide` is its sole acceptance point.
<!-- pkb:term:end -->

For an immediate same-build call, the executor of an already accepted source output invokes the typed target interface. The target binding admits that input to its serialized owner, evaluates pure `decide`, and accepts State and present outputs together. The call never occurs inside the source or target's pure `decide`. The selected interface, trusted construction, and actual call path establish the command's target and origin; no source token, issuer field, or materialized `ModuleCommandPulse` wrapper is required. A method call alone is not acceptance: the target owner's acceptance must occur before an accepted result is returned.

When command delivery can detach, reorder, retry, recover, or be independently observed, the trusted boundary instead verifies portable causal evidence from the accepted source frame:

```text
ModuleCommandPulse {
    commandSource: CausalToken
    effectiveProtocolIdentity
    command: ModuleCommand
    issuerProvenance
}
```

Here `commandSource` derives from the accepted `ModuleCommandRequest` frame, including its stable semantic handle and source ordinal. `effectiveProtocolIdentity` resolves the target-owned command/result mapping. The boundary verifies source acceptance, target, protocol identity, payload ownership, applicable bounds, provenance, and every triggered authenticity rule before constructing trusted input. A transport receive, inbox row, ACK, or route invocation does not replace target acceptance.

Assembly supplies the allowed target interface and connects the binding. It does not select business outcomes or invent accepted work. For portable delivery it transports verified causal values without synthesizing or modifying them. For an immediate call, verification follows actual source acceptance, target ownership, and call/return order; it does not reconstruct absent tuple fields to prove equivalence to a materialized record. Caller names belong in composition unless caller identity changes target business policy, which remains with the target owner.

### 6.9. ModuleResult

<!-- pkb:term:start name="ModuleResult" -->
**ModuleResult** — a target-owned business-outcome payload for one command mapping, produced by accepted target work and returned to the source's serialized result handler. An immediate typed return represents that result directly; portable delivery uses `ModuleResultOutput` and `ModuleResultPulse` records.
<!-- pkb:term:end -->

```text
Changed(value)
InventoryReserved(reservationId)
InventoryReservationRejected(reason)
PaymentCaptureOutcomeUnknown(reference)
```

<!-- pkb:term:start name="ModuleResultOutput" -->
**ModuleResultOutput** — a target-owned semantic result output created only inside an accepted target Decision. An immediate return uses its accepted position and call scope; portable delivery carries target-frame `sourceOrdinal`, accepted-source `commandSource`, target-owned payload, and `semanticHandle = commandSource.semanticHandle` for correlation without ownership transfer.
<!-- pkb:term:end -->

An immediate API such as `CounterCommands.increment(): IncrementResult` may return `Changed(value)` or `NotAccepted(reason)`. `Changed` is returned only after Counter accepts its change and result output; `NotAccepted` describes refusal before target acceptance under §6.13. The executor delivers the return through the source's serialized handler, which makes any source change through ordinary `decide` and acceptance. The return is sufficient correlation with that invocation. No `commandSource`, `resultSource`, result token, protocol identifier, or separate carrier class is required.

A failure after acceptance cannot become `NotAccepted`. A declared executor or Resource failure preserves accepted work and follows the contract's failure path; a programming fault follows the runtime fault policy. If an external action may have happened without a known outcome, `OutcomeUnknown` retains that meaning. Concrete result types distinguish only the stages reachable for that operation; the Core does not require every implementation to use one carrier shape.

For detached, reorderable, retryable, recoverable, or independently observed delivery, a target Nucleus creates the portable result only inside an accepted target Decision:

```text
ModuleResultOutput {
    semanticHandle = commandSource.semanticHandle
    sourceOrdinal
    commandSource: CausalToken
    payload: ModuleResult
}
```

`sourceOrdinal` is the result's position in the accepted target output sequence. The shared handle provides correlation only; it transfers no ownership and does not re-export the payload. Every result counts as present output work under the target's applicable output bounds. A retained, retryable, or independently observed route also occupies its required target delivery/status capacity; it cannot accept a result that its mechanism will then lose through overflow.

<!-- pkb:term:start name="ModuleResultPulse" -->
**ModuleResultPulse** — the source's trusted input for an accepted target result; immediate delivery uses the typed return and call scope, while portable delivery carries verified `commandSource`, `resultSource`, effective protocol identity, target-owned payload, and issuer provenance.
<!-- pkb:term:end -->

For portable delivery, the verified route constructs the source input from the accepted target frame:

```text
ModuleResultPulse {
    commandSource: CausalToken
    resultSource: CausalToken
    effectiveProtocolIdentity
    result: ModuleResult
    issuerProvenance
}
```

`commandSource` identifies the accepted source command. `resultSource` identifies the accepted target frame and its result output position. The route verifies both accepted tuples, the target-owned payload, effective protocol identity, and issuer provenance. Assembly transports those values without synthesizing or modifying either token or the result payload. The portable result-delivery key is `(effectiveProtocolIdentity, commandSource, resultSource)`; a target-side `DispatchStopped` uses that key without asserting source receipt or a source business outcome. An immediate same-build return needs none of these records or tuple-equivalence checks.

A transport ACK and a result are different observations: an ACK may prove target acceptance, but not business success. An independently delivered result proves target acceptance through verified accepted-target evidence; an immediate return establishes it through the trusted target binding and actual acceptance-before-return order.

For a same-identity/fingerprint target-command redelivery after command acceptance but before a result frame exists, the target returns only verified ACK proof of the original accepted frame and pending-result state. It neither fabricates a provisional result nor repeats semantic/Resource work. After result acceptance, redelivery preserves the existing frame's `commandSource`, `resultSource`, effective protocol identity, target revision, handle/ordinal, and payload; only mechanical attempt metadata changes. Conflicting fingerprint or frame evidence fails closed.

Every target contract fixes each refusal's acceptance meaning: either no target work was accepted, or it is an accepted target outcome. The binding cannot change that meaning according to delivery or runtime conditions. An accepted result is required when the refusal creates target-owned state, an operation/idempotency record, output, Resource action, status/reconciliation visibility, or a durable claim. Pre-acceptance refusal is legal only when none of those facts was accepted. Post-acceptance Execution Gate, Resource, or dispatch failure never downgrades accepted work to a pre-acceptance response. Conflicting refusal and accepted-result evidence for the same call or portable identity fails closed.

> A read-like command pays target acceptance cost when its caller requires an accepted result, idempotent replay, status, or reconciliation. When no accepted target record is required, use `ReadDependency`/`Query`, which creates no target Decision, revision, or semantic output.

### 6.10. Signal

<!-- pkb:term:start name="Signal" -->
**Signal** — a semantic publication without one mandatory requester; not a universal `DataChanged`.
<!-- pkb:term:end -->

```text
SessionRevoked
OrderConfirmed
CatalogIndexUpdated
```

A Signal is not a command. The recipient must not interpret a publication as authority to perform a privileged action without a separate grant or command contract.

<!-- pkb:term:start name="ObservedSignal" -->
**ObservedSignal** — a trusted causal input from a previously accepted `SignalPublication` over a declared route; immediate delivery may use typed call scope, while independent delivery or observation carries verified portable provenance and causal identity.
<!-- pkb:term:end -->

The recipient accepts only an observation of a previously accepted `SignalPublication`. For independent delivery or observation, its portable causal input is:

```text
ObservedSignal {
    sourceBallInstanceId
    sourceCommitRevision
    semanticHandle
    sourceOrdinal
    signal: Signal
    issuerProvenance
}
```

An `ObservedSignal` is accepted only after its provenance and correspondence to an accepted `SignalPublication` have been established. An immediate same-build typed route may establish these through trusted construction and actual acceptance/call order without materialized identity; independently delivered, reordered, retryable, recovered, or independently observed signals retain the required stable evidence. Resource subscription observations use a causally bound `Fact`, not an `ObservedSignal`.

### 6.11. Pulse

<!-- pkb:term:start name="Pulse" -->
**Pulse** — the ordered closed family `Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse`; raw external mutation/cancellation enters only as an `Intent` after Interaction validation and is not a field of `DecisionContext`.
<!-- pkb:term:end -->


<!-- pkb:term:start name="ControlPulse" -->
**ControlPulse** — a declared trusted lifecycle/timer/cancellation/delivery observation from a runtime/resource/route boundary; it is not raw external mutation ingress. If a post-commit mechanical dispatch/ACK or pre-acceptance command-carrier observation changes Sovereign State, it passes as this typed input through single-writer `decide` rather than being written directly by runtime; a business `Fact` or `ModuleResultPulse` is not reclassified. Completion handling preserves any applicable causal-work bound; a statically finite synchronous execution needs no causal token or reservation levels.
<!-- pkb:term:end -->


```text
Pulse =
    Intent
  | Fact
  | ModuleCommandPulse
  | ModuleResultPulse
  | ObservedSignal
  | ControlPulse
```

A `ControlPulse` includes only declared lifecycle, timer, cancellation, or delivery observations. Arbitrary string-based system events are not used.

A `ControlPulse` is created only by a trusted runtime, resource, or route boundary after the declared origin and provenance have been verified. A raw customer, API, or UI request cannot become a `ControlPulse`: after Double Quarantine, an external request to start or cancel a semantic operation is an `Intent`. The result of target or resource cancellation remains a causally bound `Fact` or `ModuleResultPulse`; a runtime cancellation observation may be a `ControlPulse` only if that owned category is explicitly declared.

If a post-commit mechanical runtime or route observation about dispatch or an ACK changes Sovereign State—for example, by moving a stored command step to `Dispatched`, recording acceptance or ambiguity, or recording a terminal delivery stop—the runtime MUST NOT write that state directly. It creates a declared typed `ControlPulse`, after which the ordinary single-writer `decide` applies the observation. An already typed business `Fact` or `ModuleResultPulse` retains its own causal category and is not wrapped in a `ControlPulse`. A verified `CommandRejectedBeforeAcceptance` from §6.13 is carried by such a declared mechanical route observation, not by a result Pulse. A mechanical delivery Pulse cannot exist before source acceptance; the source commit itself does not prove dispatch.

An immediate typed pre-acceptance return may represent this declared observation directly. Its source handler remains serialized and changes State only through `decide`. Under §8.4, statically finite synchronous execution needs no reservation levels; when growing or retained work needs capacity accounting, refusal and result handling preserve that bound and cannot lose an accepted completion or create a fresh budget.

### 6.12. SemanticOutput

<!-- pkb:term:start name="SemanticOutput" -->
**SemanticOutput** — the ordered closed family `ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest`; every present variant has a typed payload and accepted zero-based sequence position, while a stable `SemanticHandle` is added only for detached/addressable work.
<!-- pkb:term:end -->


<!-- pkb:term:start name="ProjectionOutput" -->
**ProjectionOutput** — a canonical `SemanticOutput` envelope with a `Projection` payload and accepted sequence position; a stable handle is present only when the projection is detached/addressable.
<!-- pkb:term:end -->


<!-- pkb:term:start name="ReplyOutput" -->
**ReplyOutput** — a canonical `SemanticOutput` envelope with a `Reply` payload and accepted sequence position; a stable handle is present only when reply delivery/status is detached or independently addressable.
<!-- pkb:term:end -->


<!-- pkb:term:start name="EffectRequest" -->
**EffectRequest** — a canonical `SemanticOutput` envelope with an `Effect` payload and accepted sequence position; detached/addressable execution also carries a complete `SemanticHandle` and materialized `sourceOrdinal`.
<!-- pkb:term:end -->


<!-- pkb:term:start name="ModuleCommandRequest" -->
**ModuleCommandRequest** — a canonical `SemanticOutput` envelope with a `ModuleCommand` payload and accepted sequence position; a stable `SemanticHandle` and materialized `sourceOrdinal` appear only when the actual lifecycle extends beyond immediate call scope.
<!-- pkb:term:end -->


<!-- pkb:term:start name="SignalPublication" -->
**SignalPublication** — a routed semantic output with a `Signal` payload and accepted sequence position; a complete `SemanticHandle` and materialized `sourceOrdinal` are required only when its actual lifecycle needs stable identity under §3.5.
<!-- pkb:term:end -->


<!-- pkb:term:start name="TimerRequest" -->
**TimerRequest** — a canonical detached `SemanticOutput` envelope with a complete `SemanticHandle`, materialized `sourceOrdinal`, and a `TimerIntent` payload for a Ball with declared timers.
<!-- pkb:term:end -->


```text
SemanticOutput =
    ProjectionOutput
  | ReplyOutput
  | EffectRequest
  | ModuleCommandRequest
  | ModuleResultOutput
  | SignalPublication
  | TimerRequest           # only if the Ball uses timers
```

All variants have a typed payload and an ordinal in the accepted output sequence. A stable handle is present only when the detached-work trigger in §3.5 applies; §20.1 is its navigation route:

```text
SemanticOutputEnvelope<Payload> {
    semanticHandle: SemanticHandle?   # required for detached/addressable work
    sourceOrdinal: UInt32             # semantic position; may be representation-erased in call scope
    payload: Payload
}

ProjectionOutput     = SemanticOutputEnvelope<Projection>
ReplyOutput          = SemanticOutputEnvelope<Reply>
EffectRequest        = SemanticOutputEnvelope<Effect>
ModuleCommandRequest = SemanticOutputEnvelope<ModuleCommand>
ModuleResultOutput   = accepted target-result envelope defined in §6.9
SignalPublication    = SemanticOutputEnvelope<Signal>
TimerRequest         = SemanticOutputEnvelope<TimerIntent>
```

For portable `ModuleResultOutput` delivery, §6.9 fixes the handle to `commandSource.semanticHandle`; an immediate typed return uses the accepted target output position and call scope without those fields. `sourceOrdinal` is the unique zero-based position within ordered `Decision.outputs`: values run from `0..outputs.size-1`. It is materialized and preserved when an output crosses the accepted call scope, is persisted, independently delivered, redelivered, or observed independently. For immediate output, including a same-build cross-Ball call, list position and call order establish the fact without a stored field. When `semanticHandle` is required it is complete and stable; omission on a triggered path is invalid. These names describe the semantic output family; their record layout is not mandatory for an immediate typed call.

### 6.13. Error classes

Errors are separated into:

```text
BusinessRejection       # intent is valid but prohibited by state/policy
ValidationFailure       # Interaction does not create an Intent
AdmissionFailure        # runtime capacity unavailable; Decision not accepted
ResourceFailure         # external operation failed with known outcome
Cancelled               # cancellation outcome is known
TimedOut                 # local wait/deadline elapsed; action outcome may differ
OutcomeUnknown          # action may have happened; reconciliation required
ProgrammingFault        # bug, invariant violation, trap, nontermination
```

This is the Core error catalog, not a mandatory per-Ball union. A concrete boundary exposes only reachable variants: for example, `OutcomeUnknown` appears only with ambiguous execution, and `QueueFull`/admission variants only with the corresponding capacity path. Omitted unreachable variants need no placeholder.

PBA-04 classifies a predicate by its declared owner and inputs, not by whether it happens to look like a range, size, or shape check:

| Predicate contract | Owner and evaluation stage | Legal semantic result |
|---|---|---|
| representation validity or a closed protocol/type invariant whose truth is independent of committed State, semantic Context, and business policy | Interaction, before semantic input construction | `BoundaryResponse(ValidationFailure)`; no `Intent`/`Query` reaches the Nucleus |
| a business choice or any rule whose result depends on committed State or semantic Context, including a §13.1 semantic limit | Nucleus after valid semantic input | mutation: `Rejected(BusinessRejection)` before acceptance; admitted read: one target-owned `ResultPayload` variant |
| a fixed constraint deliberately promoted from business policy to a closed protocol/type invariant | protocol owner, then Interaction for that new effective protocol identity | `ValidationFailure`; moving the constraint without the required protocol-semantic compatibility change is invalid |

For example, violating a declared collection type bound is validation. Passing that type but exceeding State/Context `maxItemsPerOrder` is a Nucleus business rejection. Declaring `maxItemsPerOrder` as a fixed protocol invariant instead is a versioned contract choice, not a second stage interpretation of the same effective protocol.

Error meaning is fixed by the stage at which evidence exists:

| Stage | Legal carrier or result | Status/facet effect | Forbidden rewrite |
|---|---|---|---|
| validation before semantic input | `BoundaryResponse(ValidationFailure)`, including pre-Intent `IdempotencyConflict`; on an immediate command call, its typed pre-acceptance return; on independently delivered routes, the verified pre-acceptance carrier | no accepted Decision, revision, operation, output, or business outcome; a conflict leaves the prior accepted operation unchanged | validation is not `BusinessRejection`, Reply, accepted result, or `NotFound` |
| admission before acceptance | `BoundaryResponse(AdmissionFailure(reason))`; on an immediate command call, its typed pre-acceptance return; on independently delivered routes, the verified pre-acceptance carrier | no accepted Decision/revision/output; source command facet may become `RejectedBeforeAcceptance + NotExpected` after verified carrier receipt | capacity failure is not business rejection, target acceptance, or delivery stop |
| target `decide` rejects before acceptance | `BoundaryResponse(DecisionRejected(BusinessRejection))`; on an immediate command call, its typed pre-acceptance return; on independently delivered routes, the verified pre-acceptance carrier | no accepted target revision/operation/output; source carrier projection remains `RejectedBeforeAcceptance + NotExpected` | informational reason does not become accepted `outcome = Rejected` |
| target Decision accepts a business outcome | accepted `ModuleResultOutput`, then verified `ModuleResultPulse` | target acceptance remains; where status exists it is retained; source can project `Accepted` plus the target-owned outcome only after verified result evidence | dispatch or later failure never downgrades the result to a carrier or `BoundaryResponse` |
| Resource/Execution Gate acts after accepted work | provenance-bound `Fact` and, for a command target, a later accepted `ModuleResultOutput`; reachable result/status variants include `ResourceFailure`, `TimedOut`, or `OutcomeUnknown` | the prior source/target acceptance remains; outcome/status refines only from declared evidence | no rollback, pre-acceptance carrier, or fabricated business rejection |
| delivery policy exhausts after accepted output | trusted `DispatchStopped` observation | delivery/status facet only, keyed to the accepted output/result route | not business failure, cancellation, target non-execution, or erasure of accepted result |
| programming fault | runtime fault policy; pre-acceptance publishes nothing, post-acceptance preserves already accepted facts under the selected profile | operational failure/quarantine and only already-declared status evidence | never fabricated as validation, admission, business result, Reply, or delivery success |

A later-stage failure never rewrites an earlier acceptance or accepted result. Each row uses only the closed variants reachable for the concrete protocol/profile. The table fixes stage meaning, not a uniform return-type layout: an immediate call may express pre-acceptance refusal and an accepted result in one closed operation-specific return type while preserving their distinct acceptance semantics.

`ValidationFailure`, `AdmissionFailure`, and `BusinessRejection` may be encoded in a `BoundaryResponse`, but do not become a `SemanticOutput` and receive no commit identity.

For a root request, any semantic ID already reserved when one of those pre-acceptance stages rejects remains a candidate only. The boundary returns the applicable `BoundaryResponse`, does not expose the candidate as an authoritative `OperationId` or status lookup key, and creates no root operation/status source, known status row, retention marker, authoritative `SemanticHandle`, or output. A verified `CommandRejectedBeforeAcceptance` is different only at the already accepted source: it may refine that source operation's matching participant Step through a declared `ControlPulse`, while still creating no accepted target operation.

<!-- pkb:term:start name="CommandRejectedBeforeAcceptance" -->
**CommandRejectedBeforeAcceptance** — a trusted observation that the target refused before accepting work. An immediate call may represent it as a typed `NotAccepted(reason)` return; independently delivered refusal uses the verified carrier with command identity, closed boundary response, and target provenance. It is not an accepted target result; source state changes only through its serialized declared input handler.
<!-- pkb:term:end -->

For an immediate same-build call, the typed target and current invocation associate a pre-acceptance refusal with the accepted source output. `NotAccepted(reason)` may encode the reachable validation, admission, or business-rejection reason without a separate carrier class, issuer field, or token. The source applies it as the declared pre-acceptance observation through serialized `decide`; it does not classify it as an accepted target business outcome. No accepted target frame exists on this branch. A post-acceptance failure cannot use this return variant.

When refusal is delivered independently, portable evidence is required:

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

- The portable carrier is constructed only after command identity, the closed response, and target-boundary provenance have been verified.
- It is neither `ReplyOutput` nor an accepted `ModuleResult`; it creates no accepted target Decision, revision, or output.
- It reaches source State only through a declared typed `ControlPulse` observation; the embedded boundary response is not a standalone accepted result.
- Forged, tampered, missing, stale, or wrong-target required provenance creates no trusted source input, refusal facet, or compensation.
- The target contract fixes refusal classification and compatibility semantics.
- Statically finite synchronous execution needs no level-1/level-2 reservations. Where real growing work or retained completion capacity exists, §8.4 requires admission and completion handling that preserve the bound without overflow loss or a fresh causal budget.

For `AdmissionFailure`:

- `AdmissionFailure` has a finite closed profile/binding-specific `reason` union.
- The Core examples are an applicability catalog, not permission for an open string.
- An unknown discriminator or free-form reason fails before construction of a trusted `AdmissionFailure` and cannot be coerced to another stage.
- The Core reason `CausalBudgetExceeded(scope, limit)` means that the Decision was not accepted because the full causal budget could not be reserved.
- It is not a business rejection and does not permit the budget to be reset on retry or resume.

For timeout classification:

- `TimedOut` must not automatically become `ResourceFailure`.
- After dispatch, a timeout often means `OutcomeUnknown`.

### 6.14. Protocol closure

<!-- pkb:pba-source:start id="PBA-04" title="Closed Protocol" -->
**Source clause for PBA-04 — Closed Protocol.**

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
<!-- pkb:pba-source:end -->
### Definition source records for §6

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="AdmissionFailure" -->
**AdmissionFailure** — a pre-acceptance `BoundaryResponse` whose reason belongs to the concrete profile/binding's finite closed union. It creates no accepted state, revision, operation, handle, or output; an unknown discriminator/open string is rejected before trusted construction and cannot become a business rejection or later-stage status.
<!-- pkb:term:end -->


<!-- pkb:term:start name="BusinessRejection" -->
**BusinessRejection** — an expected domain-level `Rejected(...)` result of a mutating Nucleus decision before acceptance, without state mutation or a runtime fault. An admitted pure read instead returns its target-owned semantic outcome inside `ResultPayload` and does not use this carrier.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Decision" -->
**Decision** — the bounded mode-specific Nucleus mutation result selected by the state profile: a `SnapshotDecision` with next State or an `EventDecision` with events/NoDomainChange, each carrying the complete ordered `SemanticOutput` batch and accepted atomically through its exact frame.
<!-- pkb:term:end -->














<!-- pkb:term:start name="ReadResult" -->
**ReadResult** — the successfully evaluated noncommitted Query wrapper used when the stamped-read trigger applies, containing the target-owned payload and field-minimized `ConsistencyStamp`. The payload is total over every reachable post-admission semantic outcome, including declared denial/redaction/non-disclosure when present; a same-stack getter may return it directly, and neither form receives commit/semantic identity.
<!-- pkb:term:end -->




---
