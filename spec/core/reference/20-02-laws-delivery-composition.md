# Core part — Laws PBA-19–30

[Core contents](../../pokeball-architecture-core.md) · [← Laws PBA-01–18](20-01-laws-boundary-decision-state.md) · [Laws PBA-31–44 →](20-03-laws-security-bounds-claims.md)

> Canonical part 20 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

<!-- pkb:generated:start id="core-laws-19-30" -->
### PBA-19 — ACK/Result Separation

Source: §9.4.

- **Rule:**
  - Validation, admission, pre-acceptance Decision rejection, target acceptance, accepted business outcome, post-acceptance Resource failure/timeout/unknown, and delivery-policy exhaustion retain the exact §6.13 carrier/result/status meaning.
  - Those stages cannot rewrite one another.
  - A post-commit mechanical observation, including verified `CommandRejectedBeforeAcceptance`, changes Sovereign State only through its declared typed `ControlPulse` path.
  - On a same-stack pre-acceptance command branch, that carrier path consumes the level-1 alternative-completion reservation transferred under §8.4; it creates neither a target accepted level nor a fresh causal scope.
  - An accepted target outcome reaches the source only through `ModuleResultPulse`.
  - Neither form may be synthesized from the other or selected dynamically by a binding.

  - An ACK answers: **was the declared acceptance point reached?**
  - A `Fact` or `ModuleResultPulse` answers: **what was the accepted business outcome?**
  - A `CommandRejectedBeforeAcceptance` answers: **which verified boundary/admission/Decision rejection prevented target acceptance?**
  - A terminal delivery observation answers: **can the current delivery policy continue delivery?**

  A result may arrive before a separate ACK if it contains verifiable proof of acceptance. In that case, one serialized transition applies the proof as `acceptance = Accepted` together with the result outcome; the state `AcceptanceUnknown + proven terminal outcome` is not retained. An ACK without a result is permitted if the target accepted the work but has not yet completed it.

  For a same-identity target-command duplicate after target acceptance but before any result frame has been accepted, the only legal immediate response is verified ACK proof of the original accepted target command frame and its pending-result state. It is not a result and does not wait for one, run another target Decision, increment revision, create an Effect/Resource action, or fabricate a pending `ModuleResult`. Once the result frame exists, a duplicate may instead redeliver proof of that exact accepted result under §9.6. Conflicting fingerprint or acceptance/result evidence fails closed.
- **Applicability:** `P`: any §6.13 validation/admission/refusal/accepted-result/Resource/delivery stage is reachable or separately observed.
- **Declaration owner:** Ball owns typed outcome/facet meaning; binding owns legal carrier/result/status mapping and same-stack carrier slot transfer.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Stage table, provenance, same-stack carrier level/slot ownership, dispatcher/result-route, duplicate-before-result ACK, exact post-result replay, later-failure, both-order, and conflict tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Mechanics may be shared; omit unreachable stages and never substitute one stage's carrier/status for another or create a new carrier scope.
- **Primary verification route:** `§17.1`

### PBA-20 — First-Class Unknown

Source: §9.5.

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

### PBA-21 — Explicit Idempotency

Source: §9.6.

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

### PBA-22 — Stable Logical Retry

Source: §9.6.

- **Rule:** A transport or delivery retry preserves the accepted logical operation and semantic work identity; it creates only a new mechanical attempt.
- **Applicability:** `P`: transport/delivery retry exists.
- **Declaration owner:** Route/binding owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dispatcher retry-identity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Retry engine may be referenced; omit when disabled.
- **Primary verification route:** `§17.3`

### PBA-23 — Cancellation Is a Protocol

Source: §9.7.

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

### PBA-24 — Owned Retry Policy

Source: §9.9.

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

### PBA-25 — Declared Dependency

Source: §10.2.

- **Rule:**
  - Every semantic inter-authority dependency that exists is declared as one of the read, one-hop command, bounded signal-observation, or Flow-participation contracts in this subsection.
  - It is bound explicitly by Assembly where a route exists.
  - A physical helper-package import is classified by its authority owner rather than treated as a fifth semantic dependency kind.
  - A Ball-local utility is owned by exactly one Ball and one logical role, is called only inside that authority/role, and creates no Assembly route or protocol-dependency row.
  - A utility shared across Balls is mechanical Foundation under PBA-43; an ownerless shared domain- or business-semantic utility is prohibited.
  - Domain or business semantics needed by more than one Ball remain explicit Ball-local implementations or acquire one Ball/Flow owner and are consumed through that owner's declared Application Surface and protocol.
  - Every physical import remains in the acyclic compile-time import graph; a utility import contributes a `Direct Control Dependency` only when it exposes another Ball's Application Surface or synchronously transfers control across Ball authorities.
  - A caller Nucleus may import only the exact declared target- or producer-owned `Application Surface` needed by its closed `Query`, `Pulse`, and `Decision` contracts.
  - Such an import transfers no ownership and admits no foreign mutable State, internals, private Resource adapters, runtime/transport mechanics, caller-owned mirror or redeclaration, protocol re-export, or semantic synthesis by Interaction or Assembly.
  - A `ReadDependency` resolves exactly one target authority.
  - It resolves exactly one target-owned `Query -> ResultPayload` mapping under exactly one effective protocol identity.
  - It resolves exactly one target read/status authority.
  - It resolves exactly one caller freshness/consistency requirement.
  - It resolves exactly one Assembly route/binding without transferring read meaning to caller or Assembly.
  - A `FlowParticipation` resolves exactly one Flow authority, one participant authority, one exact participant `Application Surface`, one non-empty bounded set of Flow-owned material-coordination responsibilities, and bounded references to the existing read, command, or signal dependencies used by that pair.
  - Those references retain their own target/producer-owned payloads, effective protocol identities, routes, return bindings, limits, and triggered fields; neither the Flow nor Assembly copies or synthesizes them.

  An `Application Surface` is the exact set of public semantic types and entrypoints that a Ball deliberately exposes to another Ball or Assembly. It contains only types owned by that authority and excludes mutable State, internals, runtime/transport mechanics, private Resource adapters, caller-owned mirrors or redeclarations, and re-exported foreign contracts. A caller Nucleus imports it only when a closed `Query`, `Pulse`, or `Decision` contract requires the target/producer-owned types. Importing it is a compile-time relation and transfers neither ownership nor synthesis authority; whether an invocation also transfers control synchronously is classified separately below.

  The following table is non-normative orientation only; it is not a protocol, an authority transfer, or a second dependency taxonomy:

  | Relation | Recorded by | Does not imply |
  |---|---|---|
  | semantic ownership | owning Ball or Flow source | import, route, synchronous control, or Assembly authority |
  | Ball-local utility import | owning Ball/role and compile-time import graph | an inter-authority protocol dependency, route, or separate authority |
  | shared mechanical Foundation import | Foundation ownership plus compile-time import graph | domain/business ownership, route, or direct control by itself |
  | Application Surface import | compile-time dependency graph and the `Direct Control Dependency` graph under the definition below | target execution, a routed data path, or ownership transfer by itself |
  | declared protocol/data dependency | `ReadDependency`, `DeclaredCommandDependency`, or `DeclaredSignalDependency` | an Application Surface import in one build, ownership transfer, one transport, or synchronous/asynchronous timing by itself |
  | synchronous control before handoff/yield | `Direct Control Dependency` graph | reverse control on a result, ownership transfer, or protocol re-export |
  | asynchronous causal route after handoff/yield | declared dependency plus Assembly route and preserved causal scope | a synchronous direct-control edge merely because generated code later invokes a target |
  | route/version/binding selection | Assembly | payload, policy, refusal, causal-token, or workflow authority |

  `Direct Control Dependency` is an orthogonal graph classification: a compile-time import of another Ball's application surface, or a synchronous cross-Ball call that can transfer control before an asynchronous handoff or yield. Generated inline dispatch is direct control when it executes the target on the current control path before that boundary. A declared asynchronous command or signal route does not create a direct-control edge merely because its binding later uses generated code; if the binding invokes the target synchronously before handoff/yield, the direct edge exists and must be included in the graph.

  #### ReadDependency

  A `ReadDependency` is the smallest complete cross-authority read contract. The following is a resolved declaration view, not a mandatory runtime envelope:

  ```text
  ReadDependency {
      caller
      targetAuthority
      effectiveProtocolIdentity
      targetOwnedMapping: Query -> ResultPayload
      targetReadOrStatusAuthority
      callerFreshnessAndConsistencyRequirements
      assemblyRouteAndBinding
  }
  ```

  For example, `Catalog -> Pricing.GetCurrentQuote` imports exactly the Pricing-owned mapping `Pricing.GetCurrentQuote -> Pricing.CurrentQuote`; Catalog owns only its freshness/consistency requirement, and Assembly owns only the route/version/binding. Pricing owns the `Query`, `ResultPayload`, snapshot/stamp meaning, and read authority. Neither caller nor Assembly redeclares, re-exports, synthesizes, or alters that meaning.

  Protocol versions materialize only when caller and target can version or deploy independently; otherwise exact same-build type identity proves the effective protocol identity. The successful cross-authority form carries the field-minimized `ConsistencyStamp` required by §8.10. Actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. Same-stack generated wiring may erase route wrappers only while proving the same target mapping, authority, requirements, and stamp. Actor-independent reads create no actor artifacts, and absent fields create no `none`, zero, or `not-applicable` placeholders.

  The caller's freshness and consistency requirements state only what the selected target snapshot/stamp proves. Multiple read dependencies do not imply one atomic multi-source snapshot without a separate declared mechanism. Existing general boundary, work, response, route, and buffering bounds remain effective; `Query` alone creates no new mandatory per-Ball read-limit field.

  Use this `Query` path when the caller needs no accepted target operation record: after any pre-read validation/admission response, admitted execution returns canonical `ReadResult` and creates no target Decision, accepted-input marker, target revision, semantic output, stable command/step identity, idempotent command replay, or command reconciliation record. If the caller requires provenance-bound accepted result, stable command/step identity, idempotent replay, status, or reconciliation semantics, the operation is deliberately a `DeclaredCommandDependency` even when its business payload is read-like. Therefore Checkout's `Payment.GetOperationStatus` remains a same-state accepted command, while an ordinary non-recording lookup uses `ReadDependency`.

  #### DeclaredCommandDependency

  A one-hop addressed command is permitted without a separate Flow when all of the following hold:

  - there is one target;
  - there is no multi-participant ordering;
  - there is no independent coordination lifecycle;
  - there is no cross-participant compensation;
  - the source stores only local operation state;
  - command and result contracts are explicit;
  - the target owns exactly one closed `ModuleCommand -> ModuleResult` mapping for the selected operation, including static pre-acceptance-versus-accepted-result refusal classification;
  - an idempotency contract is explicit when duplicate execution is possible;
  - a deadline contract is explicit when the command has a semantic or resource deadline;
  - the `Direct Control Dependency` graph remains bounded and acyclic.

  Example:

  ```text
  OrderBall -> NotificationBall.SendReceipt
  ```

  The source accepts `ModuleCommandRequest`; the trusted target boundary constructs `ModuleCommandPulse`; the target accepts only through `decide` and creates any `ModuleResultOutput` inside that accepted Decision; the verified return route constructs `ModuleResultPulse` for the source. The caller imports the target mapping through `dependencies.commands`, while Assembly binds both ingress and return routes. A same-stack invocation contributes `source -> target` to the `Direct Control Dependency` graph; returning the causally bound result does not add an edge in the opposite direction. An asynchronous handoff removes only the synchronous-invocation contribution, not any separately present compile-time-import edge, and retains the original causal scope, depth, and budget.

  #### DeclaredSignalDependency

  An explicit dependency on another `Ball`'s semantic publication. It is used when a consumer accepts a producer-owned `Signal` as a typed `ObservedSignal` Pulse but neither addresses a command to the producer nor gains authority from the Signal to perform a privileged action.

  Every such dependency declares only its base route contract:

  ```text
  producer
  signalType
  consumer
  effectiveProtocolIdentity
  deliverySemantics
  sourceIdentityAndProvenance
  limits {
      maxConsumersPerSignal
      maxObservationBytes
  }
  ```

  `effectiveProtocolIdentity` may be exact same-build type identity. Explicit producer and consumer protocol versions materialize only when the two sides can version or deploy independently. `deliverySemantics` names the observation point and whether loss or redelivery is permitted. The route has finite effective fan-out and observation-size bounds. Strong durable delivery is not inferred from the dependency itself and requires a corresponding channel contract.

  Additional route fields materialize only with their trigger: an idempotency or deduplication policy and retention bound for duplicate/redelivery risk; `orderingScope` when ordering is observable or relied upon; `maxBufferedOrInFlightObservations` when buffering exists; `maxCausalDepth` when the observation can re-enter a Decision chain; and `maxDeliveryAttempts` when delivery is retried. An `ObservedSignal` carries only the source identity, revision, handle, ordinal, and issuer provenance required by those effective policies. The publisher does not import consumers: the producer-to-consumer edge belongs to the typed source or `Assembly`, has bounded fan-out, and does not become a wildcard subscription. Absent triggered fields are omitted rather than declared as `none`, zero, or `not-applicable`.

  #### FlowParticipation

  Used when material coordination belongs to a separate Flow. Its minimum resolved declaration view is:

  ```text
  FlowParticipation {
      flowAuthority
      participantAuthority
      participantApplicationSurface
      flowOwnedCoordination: NonEmptyBoundedSet<
          lifecycle
        | orderingOrBranchJoin
        | compensationOrRecovery
        | cancellation
        | reconciliation
        | terminalOutcome
      >
      dependencyRefs: BoundedSet<
          ReadDependencyRef
        | DeclaredCommandDependencyRef
        | DeclaredSignalDependencyRef
      >
  }
  ```

  This is neither a runtime envelope nor a fifth route protocol. One Flow/participant pair resolves once; `dependencyRefs` point to existing declarations whose payload ownership, effective identity, route, return binding, and triggered optional fields remain authoritative. `flowOwnedCoordination` records only the coordination meaning owned by the Flow and cannot copy participant domain truth. Compile-time import, direct control, asynchronous routing, data direction, semantic ownership, and Assembly binding remain independently reviewable relations.
- **Applicability:** `P`: an application import/dependency or inter-Ball edge exists; utility imports activate their ownership/graph classification, while semantic inter-authority edges activate one of the four complete resolved views.
- **Declaration owner:** Each Ball role owns its local utility artifacts; the project Foundation owner owns shared mechanical utilities; each target/participant owns its Application Surface and protocol/read meaning; caller owns read requirements; Flow owns coordination only; Assembly owns route/version/binding.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Utility ownership/import scan, no shared domain-utility check, complete compile-time/direct-control graphs, exact-one read mapping and participation tuple, non-empty bounded Flow coordination, existing dependency-reference/route resolution, ownership, no-copy/no-envelope, and wrong-binding tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Templates and mechanical Foundation may be shared; Ball-local utilities stay local, triggered fields only, same-build facts may be static, and no semantic inter-authority edge means no dependency or participation row.
- **Primary verification route:** `§17.5`

### PBA-26 — Workflow Sovereignty

Source: §10.4.

- **Rule:**
  - When material coordination exists across participant authorities, the specific Flow is its one owner.
  - Mere call count or a one-hop dependency does not trigger a Flow.
  - Any independently owned lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or terminal outcome can trigger a Flow.

  > **Every stateful cross-authority workflow has one coordination owner.**
- **Applicability:** `P`: multi-authority lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome needs an owner.
- **Declaration owner:** Specific Flow owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Boundary/Flow falsifiers and coordination-ownership tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Mechanics shared; one material property suffices, while call count/valid one-hop edge omits Flow.
- **Primary verification route:** `§17.5`

### PBA-27 — No Wildcard Mediator

Source: §10.4.

- **Rule:** A Flow describes one specific workflow and its closed routes; it does not become a universal mediator, handler registry, or wildcard dispatcher.
- **Applicability:** `P`: Flow or multi-operation dispatcher exists.
- **Declaration owner:** Flow and Assembly.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Closed route graph/no-wildcard tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Runtime shared; workflow remains specific; omit with no Flow.
- **Primary verification route:** `§17.5`

### PBA-28 — No Protocol Re-export

Source: §10.8.

- **Rule:**
  A Ball may import another Ball's exact declared Application Surface, but it does not publish, redeclare, structurally mirror, or re-export the other Ball's owned types as its own contract. Import does not transfer ownership.

  Bad:

  ```text
  CheckoutProjection {
      CartEntity
      PaymentProviderResponse
      InventoryRow
  }
  ```

  Good:

  ```text
  CheckoutProjection {
      lineItems: CheckoutLineItem[]
      paymentStatus: CheckoutPaymentStatus
      availability: CheckoutAvailability
  }
  ```

  Opaque references are permitted when their semantics and ownership are clear.
- **Applicability:** `A`: every public protocol; foreign ownership is inspected when referenced.
- **Declaration owner:** Public protocol owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Ownership index/type review.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared linter; no foreign references proves the rule without a row.
- **Primary verification route:** `§17.5`

### PBA-29 — Bounded Composition

Source: §10.9.

- **Rule:**
  Every present composition dimension resolves under §0.2 through a static bounded type/control-flow proof, a local declaration, or an optional exact reusable project policy plus an explicitly permitted Ball or Flow delta. The applicable catalog includes:

  ```text
  maxDeclaredDependenciesPerBall
  maxFlowParticipants
  maxRoutesPerFlow
  maxConsumersPerSignal
  maxOutputsPerDecision
  maxCausalDepth
  maxCumulativeFanout
  ```

  When a policy reference is used, its revision is exact and does not change silently. Effective limits must remain constant with respect to growth in the total number of modules. Absent dimensions need no zero-valued entry. One universal event bus with wildcard subscriptions destroys the ability to calculate change radius.

  `maxDeclaredDependenciesPerBall` counts the complete resolved semantic-dependency inventory owned by one Ball. One unit is one distinct resolved declaration of exactly one of the four §10.2 kinds:

  - `ReadDependency`: identity is caller + target authority + target-owned query/result mapping + effective protocol identity + target read/status authority; the caller owns the count;
  - `DeclaredCommandDependency`: identity is caller + target authority + target-owned command/result operation + effective protocol identity; the caller owns the count;
  - `DeclaredSignalDependency`: identity is consumer + producer + producer-owned signal type + effective protocol identity; the consumer owns the count;
  - `FlowParticipation`: identity is Flow authority + participant authority; the Flow owns the count.

  A `FlowParticipation.dependencyRefs` entry only references an existing read/command/signal declaration: that existing declaration still counts once in its own kind, and the participation row counts once independently. Multiple operations against one target remain distinct command declarations. An exact duplicate or alias that resolves to an already present identity is an invalid declaration rather than free deduplicated capacity. Every `DeclaredSignalDependency` therefore counts toward `maxDeclaredDependenciesPerBall`, and each distinct consumer also counts toward `maxConsumersPerSignal` and cumulative fan-out. An undeclared consumer and a wildcard signal route are prohibited. Static resolution accepts exactly `N` resolved declarations and rejects the contract when the first distinct declaration would make `N+1`; no runtime Decision is partially admitted under an invalid graph.

  `maxRoutesPerFlow` counts the distinct effective Assembly command/result round-trip mappings used by one Flow after dependency and version resolution. One unit is the complete mapping from the accepted Flow source command through target ingress to its bound canonical result return; its ingress and result-return transport legs do not count as two routes. Read and signal dependencies remain counted under `maxDeclaredDependenciesPerBall` and their own read/consumer/fan-out/Assembly bounds; they do not consume this command-route limit. `FlowParticipation` and `dependencyRefs` create no route. Distinct command operations remain distinct route rows even when they share a target or receiving endpoint. An exact duplicate, spelling alias, or equivalent repeated row is invalid and cannot create free capacity; coexisting distinct command mappings count separately. Static resolution accepts exactly `N` rows and rejects the Flow/Assembly contract at `N+1` before execution.

  When `maxCumulativeFanout` is present, its contract is exact:

  - the counting scope is one accepted root operation or another explicitly named causal scope;
  - one unit is one distinct accepted `SemanticOutput` delivery branch from its complete accepted source tuple to one effective route and consumer/executor; a single-destination output counts once, and one `SignalPublication` with `k` declared consumers counts `k` branches;
  - a terminal delivery branch counts once even when it causes no later Decision; every co-reachable output branch at every causal level is summed, including separate branches that converge on the same downstream authority;
  - a diamond therefore counts route traversals, not unique authorities; two accepted source branches into the same target count twice;
  - mutually exclusive future alternatives share one reservation sized to the maximum permitted alternative and only the selected accepted branch consumes it; alternatives that can both occur are co-reachable and are summed;
  - retry or redelivery of the same accepted source tuple through the same effective route to the same consumer/executor does not add a unit, while a new accepted output source tuple does;
  - an asynchronous handoff preserves the same causal scope and remaining fan-out budget; only a separately declared independent root with no causal continuation starts a fresh scope.

  `maxOutputsPerDecision` still bounds one accepted source batch, `maxConsumersPerSignal` still bounds one Signal, and `maxCausalDepth` still bounds accepted hops. None substitutes for the cumulative branch count across the causal scope, and the cumulative bound does not weaken any of them.

  A static bounded graph/control-flow proof may establish the ceiling without a runtime artifact. Otherwise, before accepting each Decision, the existing total causal reservation from §8.4 reserves the units for the complete candidate output batch and any mutually exclusive reservation it owns. At exact `N = maxCumulativeFanout`, the Decision may be accepted. The first required unit `N+1` rejects the whole candidate Decision with typed `AdmissionFailure(CausalBudgetExceeded)`; no State, revision, partial output batch, or dispatch is accepted. The fan-out counter/reservation is runtime admission state, not `DecisionContext`, and does not create a new protocol or conformance authority.
- **Applicability:** `P`: routes, participants, fan-out, or multi-hop causality exists.
- **Declaration owner:** Producer/Flow owns the causal scope and output branches; Assembly owns effective route/consumer resolution; the binding owns any runtime reservation mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Static dependency/route resolution plus graph/control-flow proof or route/admission branch accounting, with four-kind declaration ownership/reference/alias tests, one-round-trip route tests, tree, diamond, mixed-route, mutual-exclusion, duplicate/redelivery, handoff, and exact `N/N+1` tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Static/local ceilings or optional exact project policy plus deltas; exact aliases/repeated rows are invalid rather than free, Flow references create neither a duplicate dependency nor a route, retries/redeliveries reuse the same branch identity, independent roots receive fresh scope, and absent composition/fan-out dimensions need no counter or field.
- **Primary verification route:** `§17.5`

### PBA-30 — Honest Read Consistency

Source: §8.10.

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
<!-- pkb:generated:end -->
