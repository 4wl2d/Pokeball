# Core part — System composition

[Core contents](../pokeball-architecture-core.md) · [← Asynchrony and delivery](09-asynchrony-and-delivery.md) · [Security and privacy →](11-security-and-privacy.md)

> Canonical part 7 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 10. System composition

Composition artifacts are path-triggered by an actual edge between authorities. A project with no such edge has no Assembly route, participant list, dependency worksheet, fan-out row, or zero-valued composition placeholder. When an edge exists, the contracts below remain mandatory and are resolved from the producer/consumer types, Assembly, and exact project policy.

### 10.1. Application component kinds

#### Feature Ball

Owns a local business capability and its state.

```text
AuthBall
CatalogBall
CartBall
PaymentBall
ProfileBall
```

A Feature Ball does not import another Ball's internals or read another Ball's state.

#### Flow Ball

Owns the coordination state of an independent multi-step workflow.

```text
CheckoutFlow
AccountDeletionFlow
OrderCancellationFlow
```

#### Read Model Ball

Owns materialized read state, source positions, freshness, and rebuild policy. It does not become the command authority for source facts.

#### Utility package

Pure stateless code without state, protocol, lifecycle, or resource authority. It need not be a `Ball`, but its ownership and import graph are still explicit:

- a **Ball-local utility** is an implementation artifact owned by exactly one Ball and one logical role; calls remain inside that authority/role and create neither an Assembly route nor a PBA-25 protocol-dependency row;
- a **shared mechanical utility** belongs to Foundation and satisfies PBA-43;
- an ownerless **shared domain- or business-semantic utility is prohibited**. The semantics either remain as Ball-local implementations, including deliberate small duplication, or acquire one Ball/Flow owner and are consumed through that owner's declared Application Surface and protocol.

Every physical utility import remains visible in the compile-time import graph and that graph remains acyclic. A Ball-local or mechanical-Foundation import is not by itself a `Direct Control Dependency`; that relation exists only when the import exposes another Ball's Application Surface or the call synchronously transfers control across Ball authorities under the definition below. Moving code to a helper package therefore neither hides a cross-authority dependency nor invents a fifth runtime dependency kind.

### 10.2. Permitted inter-module dependencies

Between application authorities, every semantic dependency is declared as one of four kinds below. A physical source-package boundary alone is not an application-authority boundary; §10.1 classifies Ball-local and shared mechanical utilities before this four-kind taxonomy is applied.

<!-- pkb:term:start name="FlowParticipation" -->
**FlowParticipation** — a resolved design-time, non-envelope relation naming one Flow authority, one participant authority, the exact participant Application Surface, a non-empty bounded set of Flow-owned material-coordination responsibilities, and bounded references to existing read/command/signal dependencies and Assembly bindings. It creates no route or protocol and transfers no participant-owned fact, contract, or authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="ReadDependency" -->
**ReadDependency** — the resolved cross-authority read contract naming one caller, target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Triggered version/auth/cache/ordering/buffering/retry/status fields are sparse; caller/Assembly cannot redefine target payload, stamp, status fact, or meaning, and independent reads imply no atomic multi-source snapshot.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Direct Control Dependency" -->
**Direct Control Dependency** — a compile-time import of another Ball's application surface or a synchronous cross-Ball call that transfers control before an asynchronous handoff/yield, including generated inline dispatch with that behavior. Ball-local utility and shared mechanical-Foundation imports remain ordinary compile-time graph edges but are not Direct Control Dependencies without one of those cross-Ball conditions. A same-stack command round trip contributes the source-to-target synchronous-invocation relation only; its causally bound result return is not a reverse edge. The graph is unconditionally acyclic. Handoff removes only the synchronous-invocation contribution; any separately present compile-time-import edge remains, while bounded asynchronous feedback after handoff adds no new synchronous-invocation edge.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Application Surface" -->
**Application Surface** — the exact Ball-owned set of deliberate public semantic types and entrypoints exposed to another Ball or Assembly. It excludes mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, caller-owned mirrors or redeclarations, and re-exported foreign contracts. A caller Nucleus may import it only when required by a closed Query/Pulse/Decision contract; import is a compile-time relation, not a new protocol, route, synthesis authority, or ownership transfer.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-25" title="Declared Dependency" -->
**Source clause for PBA-25 — Declared Dependency.**

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
  - the target owns one closed typed command/result contract for the selected operation, including the distinction between refusal before acceptance and an accepted result;
  - an idempotency contract is explicit when duplicate execution is possible;
  - a deadline contract is explicit when the command has a semantic or resource deadline;
  - the `Direct Control Dependency` graph remains bounded and acyclic.

  Example:

  ```text
  OrderBall -> NotificationBall.SendReceipt
  ```

  The source accepts its command output before execution. The target handles the typed command through its own `decide` and acceptance; the result or refusal then reaches the source through the source's serialized handler. For an immediate same-build call, the target-owned interface, trusted binding boundary, and call scope establish the operation and return association under §6.9. No additional source/result token, issuer field, or carrier type is required merely because the call crosses a Ball boundary. Separately delivered messages retain the applicable source verification and correlation contract.

  The caller imports the target-owned operation; Assembly supplies the permitted interface and binds execution and return. The operation describes Counter's capability, not a registry of consumer names. If business policy depends on the caller, Counter still checks that policy using trusted relevant input. A same-stack invocation contributes `source -> target` to the `Direct Control Dependency` graph; its return adds no reverse edge. An asynchronous handoff removes only the synchronous-invocation contribution and preserves any active causal bound.

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

  Additional route fields materialize only with their trigger: an idempotency or deduplication policy and retention bound for duplicate/redelivery risk; `orderingScope` when ordering is observable or relied upon; `maxBufferedOrInFlightObservations` when buffering exists; `maxCausalDepth` when the observation can regenerate work beyond a complete structural bound; and `maxDeliveryAttempts` when delivery is retried. An `ObservedSignal` carries only the source identity, revision, handle, ordinal, and issuer provenance required by those effective policies. The publisher does not import consumers: the producer-to-consumer edge belongs to the typed source or `Assembly`, has bounded fan-out, and does not become a wildcard subscription. Absent triggered fields are omitted rather than declared as `none`, zero, or `not-applicable`.

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
<!-- pkb:pba-source:end -->
### 10.3. When a Flow is needed

**Material coordination** exists when one authority must own any independent workflow lifecycle, semantic ordering or branch/join, compensation or recovery, cancellation, reconciliation, or terminal outcome across participant authorities. One such property is sufficient when it makes the §10.2 one-hop conditions false. Call count, sequential syntax, or one command round trip alone is not material coordination.

A separate Flow is needed when at least one of the following coordination properties is material:

- multiple participant authorities must lead to one terminal outcome;
- sequence, branch, or join is business state;
- compensation or reconciliation exists between participants;
- the operation lives independently of the initiating Feature state;
- a separate manual-intervention queue is needed;
- the workflow is reused by multiple initiators;
- it has separate retention, security, or scaling requirements;
- it has child workflows and independent deadlines.

A Flow is not needed merely because one command hop exists.

A one-hop `DeclaredCommandDependency` is permitted only while every condition in §10.2 holds. Adding a Flow does not legalize unbounded fan-out, wildcard routing, or a cycle of `Direct Control Dependency` edges. Fan-out and routes remain finitely bounded; a direct-control cycle must be removed or redesigned behind an explicit asynchronous handoff with the separate feedback contract in §10.10.

### 10.4. Workflow sovereignty

<!-- pkb:pba-source:start id="PBA-26" title="Workflow Sovereignty" -->
**Source clause for PBA-26 — Workflow Sovereignty.**

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
<!-- pkb:pba-source:end -->
<!-- pkb:pba-source:start id="PBA-27" title="No Wildcard Mediator" -->
**Source clause for PBA-27 — No Wildcard Mediator.**

- **Rule:** A Flow describes one specific workflow and its closed routes; it does not become a universal mediator, handler registry, or wildcard dispatcher.
- **Applicability:** `P`: Flow or multi-operation dispatcher exists.
- **Declaration owner:** Flow and Assembly.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Closed route graph/no-wildcard tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Runtime shared; workflow remains specific; omit with no Flow.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

A participant owns its local operation. A Flow owns only reachable orchestration facts. This is an applicability catalog, not a required state shape:

```text
phase                              # the workflow has explicit phases
participant semantic handles       # participant work is detached/addressable
captured versions                  # a later Decision observes rollout/state version
branch/join state                   # branching or joining exists
deadlines                           # a workflow deadline exists
cancellation state                  # cancellation exists
compensation/reconciliation state   # those paths exist
terminal workflow outcome           # an independent workflow outcome exists
```

If a later-phase decision uses a value from earlier ingress or a verified participant result, the Flow must either retain a field-minimized typed value in its state or obtain it again through an explicitly declared trusted `Pulse` or `DecisionContext`. As in §7.5, correlation appears only when source matching crosses call scope, version only across rollout/persistence, provenance only across a trust boundary, and retention metadata only when lifetime/deletion is not statically fixed. A runtime ledger, outbox, participant history, and ambient memory are not hidden inputs to the `Nucleus`.

A Flow does not copy participants' internal step ledgers and does not become the owner of their domain facts.

### 10.5. Compensation

Compensation is a new fallible action, not a rollback of time. It intrinsically has an independent `SemanticHandle` and outcome. Other fields materialize only with their trigger:

```text
idempotency             # duplicate execution is possible
capability              # external authority/resource is used
grant                   # privileged proof crosses an authority
deadline                # a compensation deadline exists
reconciliation policy   # execution can remain ambiguous
```

If the original action has `OutcomeUnknown`, conflicting compensation is not started before reconciliation unless the business risk is explicitly accepted.

For a parallel workflow, compensation order is derived from the dependency graph, not from incidental reverse completion order.

### 10.6. Read aggregation

Multiple read sources may be combined in:

- presentation composition layer;
- dedicated Read Model Ball;
- a Flow-captured snapshot for a specific operation.

A set of the latest independently read values is not called a consistent snapshot. A strict command decision must revalidate versions, use a target-owned reservation or constraint, or accept eventual workflow semantics.

### 10.7. Assembly

`Assembly` connects public protocol exports and imports to concrete targets and adapters.

```text
Catalog.FindProducts -> LocalCatalogSqlAdapter
CheckoutFlow.PaymentCapture -> PaymentBall.Capture
OrderConfirmed -> AnalyticsStream
CatalogIndexUpdated -> SearchReadModel.ObserveCatalogIndexUpdated
```

<!-- pkb:term:start name="Assembly" -->
**Assembly** — an explicit composition root that supplies permitted target-owned interfaces and selects routes, applicable protocol/version pairs, delivery bindings, and command-result return bindings. Immediate trusted calls use their interface and call scope; independently delivered values use the required source verification. Assembly has no authority to invent or modify causal information, target-owned payloads, context, refusal meaning, policy, read-result selection, or other business semantics.
<!-- pkb:term:end -->

Under the marked definition, Assembly binds the permitted target-owned interfaces and concrete execution/return routes. A trusted immediate same-build call uses this existing boundary and the interface available to the caller as source evidence. It needs no separate issuer field or protocol token. Independently delivered or untrusted messages require the source verification and portable provenance appropriate to their boundary. Assembly makes no business decision, reads no private State, and must not fabricate or alter payloads, refusal meaning, causal information that is actually required, or authorization policy.

For example, one Counter owner exposes `CounterRead.value()` and `CounterCommands.increment(): IncrementResult`. Assembly gives a display the read interface and gives each permitted increment consumer the command interface. Connecting another allowed consumer changes wiring; it does not change Counter's result type, decision logic, or a list of consumer names in Counter. Caller-dependent business authorization, when present, remains Counter's responsibility. This example prescribes neither these names nor a prohibition on domain-relevant caller categories.
For a `ReadDependency`, Assembly binds the caller to the selected target authority, target-owned `Query -> ResultPayload` mapping, read/status authority, and transport. It does not own or alter the caller's freshness/consistency requirement, target result, status fact, `ConsistencyStamp`, or read meaning. Explicit versions, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, and status fields appear only under their existing triggers; generated same-stack wiring proves the same resolved contract without needing materialized route wrappers.

For a `DeclaredSignalDependency`, Assembly fixes the producer, consumer, effective protocol identity, delivery semantics, source identity/provenance, and finite fan-out/observation-size bounds. Independent protocol versions, deduplication, ordering, buffering, causal-depth, and delivery-attempt fields are added only when their §10.2 triggers exist. Such a route delivers an `ObservedSignal` but does not turn the Signal into a command or grant the consumer additional authority.

Static composition is the default. Ordinary typed calls or generated direct dispatch require no runtime service registry. For an immediate same-build call, verify the actual source acceptance, selected target owner, target acceptance/refusal, and serialized return order. Do not reconstruct absent tuple fields merely to prove equivalence to a materialized bridge.

Assembly owns route selection and delivery binding, but it does not erase graph facts. A generated route that invokes another Ball synchronously before asynchronous handoff/yield is also a `Direct Control Dependency`; an asynchronous enqueue/handoff followed by later target execution is not.

### 10.8. No protocol re-export

<!-- pkb:pba-source:start id="PBA-28" title="No Protocol Re-export" -->
**Source clause for PBA-28 — No Protocol Re-export.**

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
<!-- pkb:pba-source:end -->
### 10.9. Bounded composition

<!-- pkb:pba-source:start id="PBA-29" title="Bounded Composition" -->
**Source clause for PBA-29 — Bounded Composition.**

- **Rule:**
  Static composition keeps its actual dependencies, participants, routes, and authority owners visible through source types and Assembly wiring. Core does not impose numeric maxima on the number of declared dependencies per Ball, participants, or routes in a statically finite workflow. A project may choose such limits for a concrete maintenance or resource reason; adding an allowed static consumer does not by itself require changing a global connection budget.

  A synchronous workflow whose complete execution is bounded by its structure needs no carried causal scope/depth, per-level reservation scheme, or separately computed geometric fan-out total. That structure includes all reachable command, result, refusal, failure, and signal handlers and the work they can generate. A source that issues one command, accepts its result, and stops is finite; a handler that repeatedly issues a new command is not bounded merely because its imports form a DAG.

  Every actually growing dimension remains finitely bounded under §0.2: queued or in-flight messages, external requests, retained outputs/completions, dynamic consumers, retries, and other work that the execution structure does not bound. A fixed output algebra or closed wiring may establish a bound directly. Otherwise an exact local declaration or reusable policy supplies a finite effective bound, enforced before accepting work that the mechanism cannot preserve. No overflow permits partial acceptance or silent loss of already accepted non-drop-eligible work. Wildcard routes and undeclared consumers remain prohibited.

  Numeric causal limits are used only where the structural bound is insufficient or the project deliberately selects a tighter operational limit. Their existing names retain these meanings:

  - `maxOutputsPerDecision`: maximum complete accepted output sequence length;
  - `maxConsumersPerSignal`: maximum effective consumers of one Signal when that count needs a numeric cap;
  - `maxCausalDepth`: maximum accepted Decision hops, including the root, within the declared causal scope;
  - `maxCumulativeFanout`: maximum accepted output-delivery branches within that scope.

  For a numeric `maxCumulativeFanout`, one unit is one distinct accepted output delivery from its source to one effective route and consumer/executor. A single-destination output counts once; a Signal with `k` consumers counts `k`. Sum all co-reachable branches, including terminal branches and separate paths that converge on the same authority. Mutually exclusive future alternatives reserve only their maximum and only the selected branch consumes it. Retry or redelivery of the same output on the same route to the same consumer/executor does not add a unit; a newly accepted output does. Source identity is represented according to §§3.5/6.9, without inventing absent tuple fields for an immediate call.

  A numeric budget retains its root/scope across yield, resume, asynchronous handoff, retry, and redelivery. Only a separately declared independent root with no causal continuation starts a fresh scope. Admission checks the whole candidate batch and required completion capacity before acceptance: exact `N` may be accepted; the first required unit `N+1` rejects the whole candidate with typed `AdmissionFailure(CausalBudgetExceeded)` and dispatches no subset. The counter is runtime admission state, not `DecisionContext`. Depth, aggregate branches, and actual storage/resource capacities bound different risks; selecting one cannot silently waive another present risk.
- **Applicability:** `P`: composition exists; numeric causal accounting only when structure does not bound the full work or an explicit operational cap is selected; real capacity limits when work/storage can grow.
- **Declaration owner:** Ball/Flow owns its work and completion behavior; Assembly owns actual route/consumer resolution; project/binding owns any selected operational limits and capacity/admission mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Types and wiring, full execution/control-flow checks including completion handlers, no-wildcard/wrong-target checks, and preservation/overflow tests for actual growing resources; branch, duplicate, handoff, and exact `N/N+1` tests when numeric accounting is used.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Reuse source types, wiring, and binding checks. Static finite composition has no mandatory dependency/participant/route counts, causal fields, or geometric fan-out total; selected policies are exact and accepted work retains the guarantees of its actual path.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->
### 10.10. Cycles

The compile-time import graph and the `Direct Control Dependency` graph MUST be acyclic. No project policy, overlay, compensating control, or `WaiverRecord` can make a cycle in either graph conforming.

A business process may return to an earlier phase or create feedback through declared asynchronous signal dependencies or other explicit asynchronous routes, but such a cycle must have:

- stateful owner;
- stable identity;
- finite causal budget;
- terminal/escape condition;
- protection from self-amplifying fan-out.

If the feedback path can duplicate, it also has idempotency/deduplication. If it retries, it also has one finite owned retry budget.

Permitted asynchronous feedback begins only after an explicit bounded handoff/yield and does not itself add a `Direct Control Dependency` edge. The handoff preserves every active causal bound and any numeric depth/fan-out scope or budget; it cannot reset accounting at the queue, broker, worker, or target hop. It does not make a compile-time import or direct-control cycle permissible.

### 10.11. Versioning and compatibility

Version identities are materialized at the boundary that can observe incompatible versions:

```text
protocolVersion           # public, independently deployed, persisted, or retained protocol
stateSchemaVersion        # persisted or migrated state
transitionArtifactVersion # replay, durable lineage, or concurrent artifact coexistence
wireCodecVersion        # only when there is a wire boundary
```

One compiled local artifact may use its exact type/build identity for protocol and transition compatibility and need not repeat per-Ball version strings. A Transient state that is never recovered or migrated needs no `stateSchemaVersion`. Once a value crosses an independent deployment, persistence, replay, retained-output, or wire boundary, the corresponding explicit version is mandatory.

Rules:

1. An existing variant does not change semantic meaning retroactively.
2. A breaking contract receives a new protocol version.
3. Assembly selects a concrete producer and consumer version pair for every independently versioned dependency, including a signal route; a same-build typed route may use the exact build/type identity.
4. An unknown variant is not ignored as a business no-op without an explicit forwarding policy.
5. Persistent state is migrated or upcast before ordinary `decide`.
6. A committed durable output retains the protocol and codec semantics under which it was accepted; the current Assembly does not silently reinterpret it.
7. The transition artifact version is part of deterministic replay or retained durable lineage when that path exists.
8. A target command contract's classification of each refusal as pre-acceptance carrier or accepted `ModuleResultOutput` is part of its protocol meaning. Moving a refusal between those paths is breaking and requires a new target protocol version and a new Assembly producer/consumer version pair; a binding/profile/retry change cannot reclassify it.

For a local Inline build, the compiler may provide exact compatibility. Independently deployed boundaries require contract tests and an explicit rollout window. Full wire canonicalization and a rolling-migration protocol belong to extension specifications.

### Definition source records for §10

These marked definitions are the sole glossary inputs for the terms owned in this section.



<!-- pkb:term:start name="DeclaredCommandDependency" -->
**DeclaredCommandDependency** — an explicit one-hop command dependency without an independent multi-participant workflow; the target owns the operation and acceptance/refusal meaning, the caller imports it, and Assembly binds the permitted target and result return. An immediate trusted same-build call may represent this with its typed interface and call scope.
<!-- pkb:term:end -->

<!-- pkb:term:start name="DeclaredSignalDependency" -->
**DeclaredSignalDependency** — an explicit one-hop route from a committed `SignalPublication` to an `ObservedSignal` with exact effective protocol identity, delivery, source identity/provenance, and finite fan-out/observation bounds; version, deduplication, ordering, buffering, causal, and retry fields appear only when triggered.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Effective Protocol Identity" -->
**Effective Protocol Identity** — the exact same-build identity or independently materialized protocol/version pair that selects one target-owned command/result mapping and its static refusal classification for a route.
<!-- pkb:term:end -->



<!-- pkb:term:start name="Workflow Sovereignty" -->
**Workflow Sovereignty** — the rule of one coordination owner for one stateful multi-participant workflow.
<!-- pkb:term:end -->


---
