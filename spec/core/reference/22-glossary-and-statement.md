# Core part — Glossary and canonical statement

[Core contents](../../pokeball-architecture-core.md) · [← Adoption strategy](21-adoption.md) · [Core entrypoint →](../../pokeball-architecture-core.md)

> Canonical part 24 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 22. Glossary

This exact glossary projection is generated from the marked term definitions in §§0–21. The marked body definition controls if this projection is stale.

<!-- pkb:generated:start id="core-glossary" -->
**AdmissionFailure** — a pre-acceptance `BoundaryResponse` whose reason belongs to the concrete profile/binding's finite closed union. It creates no accepted state, revision, operation, handle, or output; an unknown discriminator/open string is rejected before trusted construction and cannot become a business rejection or later-stage status.

**Applicability Trigger** — a normative condition that activates a guardrail: `always`, a reachable path, a named risk, or a concrete claim. `always` cannot be absent; a proven-absent conditional trigger removes its ceremony, while ambiguity is treated as present under §0.2 unless an accepted decision resolves it.

**Application Surface** — the exact Ball-owned set of deliberate public semantic types and entrypoints exposed to another Ball or Assembly. It excludes mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, caller-owned mirrors or redeclarations, and re-exported foreign contracts. A caller Nucleus may import it only when required by a closed Query/Pulse/Decision contract; import is a compile-time relation, not a new protocol, route, synthesis authority, or ownership transfer.

**Assembly** — an explicit composition root that selects routes, effective protocol/version pairs, delivery bindings, and command-result return bindings. It transports verified values and has no authority to synthesize or modify causal tokens, target-owned payloads, context, refusal meaning, policy, read-result selection, or other business semantics.

**AttemptId** — the mechanical identity of one delivery/execution attempt; it changes on retry and does not replace `OperationId`, `SemanticHandle`, or `OutputId`.

**AuthenticatedActorContext** — a verified, bounded, field-minimized trusted description of a stable subject used only when actor data changes a Decision or Query/status-read authorization/result selection. Its approved issuer/realm is materialized or proven by fixed trusted same-stack binding scope; method, assurance, time, expiry, and delegation appear only when their policy/lifetime paths exist, and the context grants no authority by itself.

**Ball** — the smallest operationally useful scope of an application decision, state authority, and lifecycle. Core constrains valid authority graphs but may permit more than one decomposition that independently satisfies the same invariants and boundaries.

**BallInstance** — a concrete state-owning and single-writer scope. `BallType + namespace + StateKey` is materialized when identity crosses an instance, persistence, route, status, ownership, or observability boundary; a local singleton may use typed call scope.

**BallType** — the versioned protocol and decision semantics of a family of instances.

**BoundaryResponse** — a stage-specific boundary/runtime response for a request with no accepted Decision: `ValidationFailure`, `AdmissionFailure`, or `DecisionRejected`; it is not a `SemanticOutput` and does not pass through commit-before-dispatch. On a command route, a verified response may be carried back only inside `CommandRejectedBeforeAcceptance`; it cannot represent an accepted target result, post-acceptance Resource outcome, or delivery stop.

**BusinessRejection** — an expected domain-level `Rejected(...)` result of a mutating Nucleus decision before acceptance, without state mutation or a runtime fault. An admitted pure read instead returns its target-owned semantic outcome inside `ResultPayload` and does not use this carrier.

**Capability** — the minimum explicit technical authority to perform a bounded class of resource operations, enforced at a real capability boundary. A wrapper or local check over unrestricted authority is not that boundary.

**Captured Input** — an immutable bounded field-minimized value from a prior ingress/result/context/authority snapshot, retained for a later decision/recovery. It adds source correlation, version, provenance, and deletion metadata only when the actual source, rollout, trust, ambiguity, or lifetime trigger requires them; it does not become a second mutable authority.

**CausalToken** — a field-minimized correlation record that binds detached, addressable, routed, late, or causal work to an accepted frame; generation, revision, depth, and budget fields appear only when their respective lifecycle or multi-Decision triggers exist. When materialized, one causal-budget scope preserves every triggered remaining depth and cumulative-fan-out capacity without adding a second scope field. `commandSource` derives from the accepted source command frame and `resultSource` from the accepted target result frame.

**Claim Record** — a binding-specific statement of a performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance guarantee together with its exact named boundary, scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees. Source durability or retained pending work alone proves no stronger downstream outcome. When no claim is made, no evidence dossier is required.

**CommandRejectedBeforeAcceptance** — the verified mechanical command-route carrier of `commandSource`, effective protocol identity, exactly one pre-acceptance `BoundaryResponse`, and target-boundary provenance. It is neither Reply nor target result; at a source it projects `RejectedBeforeAcceptance + NotExpected` and creates no trusted Pulse when provenance is invalid. On a same-stack target-not-accepted branch, no accepted target level exists and the source Decision applying the carrier atomically consumes the transferred level-1 alternative-completion slot in the original causal scope.

**Commit-before-dispatch** — the rule under which dispatch of a `SemanticOutput` is permitted only after successful acceptance/commit of the complete source Decision frame.

**CommitId** — the mechanical identity of an accepted runtime commit record; it does not replace `CommitRevision` and is not used as pre-commit semantic identity.

**CommitRevision** — the materialized monotonic number of an accepted mutating input/Decision within a BallInstance when ordering is observed across concurrency, persistence, async, reads/status, ownership, replay, or another boundary; sequential local call scope may prove order without storing it.

**CommittedStateSnapshot** — immutable stamped-read input that combines Sovereign State with its `CommitRevision`; `BallInstanceId` and `stateSchemaVersion` appear only when instance or persisted-schema identity is observed.

**ConsistencyStamp** — the exact available identity of the committed snapshot used by a successful read when the stamped-read trigger applies: revision plus only the triggered instance/schema identities; by itself it promises neither subsequent freshness nor multi-source atomicity.

**ControlPulse** — a declared trusted lifecycle/timer/cancellation/delivery observation from a runtime/resource/route boundary; it is not raw external mutation ingress. If a post-commit mechanical dispatch/ACK or pre-acceptance command-carrier observation changes Sovereign State, it passes as this typed input through single-writer `decide` rather than being written directly by runtime; a business `Fact` or `ModuleResultPulse` is not reclassified. A same-stack carrier-handling Decision consumes the transferred alternative-completion slot and creates no new causal root.

**Decision** — the bounded mode-specific Nucleus mutation result selected by the state profile: a `SnapshotDecision` with next State or an `EventDecision` with events/NoDomainChange, each carrying the complete ordered `SemanticOutput` batch and accepted atomically through its exact frame.

**Decision Work Meter** — the immutable versioned binding-owned definition that interprets numeric `maxTransitionSteps`. It charges non-negative integral units monotonically over exactly one `decide`, never resets within that invocation, and produces equal consumption for equal canonical inputs under the same binding, transition artifact version, and meter identity/version. Exact `N` may complete, `N+1` accepts nothing, and counts under unlike meter/artifact/unit identities are not comparable.

**DecisionContext** — a closed field-minimized record of contextual observations that actually change one Decision; it may be empty. The trusted binding boundary verifies, bounds, and constructs it for that invocation's current `Pulse`, while the Ball/Nucleus owns its semantic schema and interpretation. Each Inline deque or continuation item preserves its own Pulse-to-Context association; actor, authorization, config, policy, time, reserved IDs, semantic limits, validity, and version fields appear only when relevant to that Pulse. Every present version field names the exact artifact it versions and is not interchangeable with another artifact's version or ambient build/deployment state. Remaining causal budget, execution quantum, and current runtime capacity are admission state rather than Context, and the current cause remains the separate `Pulse`.

**DeclaredCommandDependency** — an explicit one-hop command dependency without an independent multi-participant workflow; the target owns one exact command-to-result mapping/refusal classification, the caller imports it, and Assembly binds verified command ingress and accepted-result return.

**DeclaredSignalDependency** — an explicit one-hop route from a committed `SignalPublication` to an `ObservedSignal` with exact effective protocol identity, delivery, source identity/provenance, and finite fan-out/observation bounds; version, deduplication, ordering, buffering, causal, and retry fields appear only when triggered.

**Delivery Observation** — a provenance-bound mechanical fact about a dispatch attempt, target acceptance or pre-acceptance refusal, ambiguity, or exhaustion of the delivery policy; it differs from a business `Fact`/`ModuleResultPulse` and affects Sovereign State only through a declared typed `ControlPulse`. A stopped target result delivery uses `(effectiveProtocolIdentity, commandSource, resultSource)` and sets no source business facet.

**Direct Control Dependency** — a compile-time import of another Ball's application surface or a synchronous cross-Ball call that transfers control before an asynchronous handoff/yield, including generated inline dispatch with that behavior. Ball-local utility and shared mechanical-Foundation imports remain ordinary compile-time graph edges but are not Direct Control Dependencies without one of those cross-Ball conditions. A same-stack command round trip contributes the source-to-target synchronous-invocation relation only; its causally bound result return is not a reverse edge. The graph is unconditionally acyclic. Handoff removes only the synchronous-invocation contribution; any separately present compile-time-import edge remains, while bounded asynchronous feedback after handoff adds no new synchronous-invocation edge.

**DispatchStopped** — a terminal delivery observation that the declared finite dispatch policy is exhausted; it does not prove business failure, cancellation, or non-execution. Operation status with multiple independently delivered outputs/steps retains a bounded record for every stopped `SemanticHandle` rather than selecting one.

**Draining** — a runtime lifecycle state that rejects new logical mutations, continues declared inputs for already accepted operations, and serves available declared Query/status Query paths from committed authorities. Unavailable reads fail before `read`; admitted reads retain the successfully evaluated `ReadResult` codomain, including any target-declared denial/redaction/non-disclosure payload, and create no Decision.

**Effect** — a private declarative external operation of the owning Ball.

**Effective Guardrail** — the one mechanism, value, or evidence contract obtained after static applicability and reference resolution through construction, a local declaration, or an exact reusable policy plus permitted delta.

**Effective Protocol Identity** — the exact same-build identity or independently materialized protocol/version pair that selects one target-owned command/result mapping and its static refusal classification for a route.

**EffectRequest** — a canonical `SemanticOutput` envelope with an `Effect` payload and accepted sequence position; detached/addressable execution also carries a complete `SemanticHandle` and materialized `sourceOrdinal`.

**EphemeralState** — Interaction/transport mechanics such as focus, scroll, animation, parser, or socket state that cannot change a business Decision. A decision-relevant UI/transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext`, not ephemeral authority.

**EventJournal** — a durability profile in which authoritative state is reconstructed from committed domain events and every accepted input, including `NoDomainChange`, receives a durable `AcceptedEventCommit`; idempotency, status, and source-output records join that transaction only when their paths exist.

**Execution Gate** — the target/resource check immediately before authoritative execution that enforces every triggered proof, capability, constrained-field, version, freshness/revocation, idempotency, endpoint, quota, and safe-sink binding for an already accepted action without inventing a new business decision. Post-acceptance failure is a typed Resource/result/status path, never a pre-acceptance carrier or downgrade.

**Fact** — a validated provenance-bound outcome of a previously accepted `EffectRequest`; immediate completion may use call-scope correlation, while detached/reorderable completion and subscription observations carry stable materialized causal identity.

**Feature Ball** — a Ball that owns a local business capability/state authority.

**Field-Minimized** — containing exactly the semantic, correlation, version, provenance, validity, and lifetime fields activated by the value's actual decision and boundary triggers; fields with absent triggers are omitted rather than filled with defaults or placeholders.

**Flow Ball** — a Ball that owns material coordination for a specific workflow: any independent lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or terminal outcome across authorities. Call count or one hop alone is insufficient.

**FlowParticipation** — a resolved design-time, non-envelope relation naming one Flow authority, one participant authority, the exact participant Application Surface, a non-empty bounded set of Flow-owned material-coordination responsibilities, and bounded references to existing read/command/signal dependencies and Assembly bindings. It creates no route or protocol and transfers no participant-owned fact, contract, or authority.

**Foundation Quarantine** — the rule that limits shared foundation to mechanical primitives and prohibits mutable business meaning, policy decisions, domain authority, route selection, service locators, hidden communication state, or an ownerless shared domain/business utility. Domain semantics remain Ball-local or acquire one Ball/Flow owner and a declared Application Surface/protocol.

**Grant** — scoped business-authorization proof from an approved issuer with verified authenticity/integrity and complete binding to actor, action, object, audience/target, operation, and validity period. Its optional `constraints` bind only fields made authorization-relevant by the action contract for the actual target or payload. Immutable binding to an accepted output and duplicate-execution idempotency are separate Execution-Gate checks activated by their §11.3 triggers; an absent subtrigger adds no field, check, default, or placeholder.

**Guardrail Policy Reference** — an immutable owner/policy/revision/digest reference whose artifact declares exact scope, covered guardrails, mechanisms or values, enforcement/evidence ownership, and permitted overrides. It is resolved statically and is not a runtime registry or ambient default.

**Intent** — a validated request to initiate a state change or operation.

**Interaction Hemisphere** — the external/route input-output adapter role: parsing, normalization, validation, bounds, encoding, accepted-frame verification, and trusted `DecisionContext`/actor-dependent `ReadContext` construction when triggered; it does not interpret business permission or select a business-visible read result. A present path may contain or adjoin a Trusted Boundary edge, but the adapter role and authorized verification/construction edge are not synonymous.

**Material Coordination** — cross-authority ownership of a workflow lifecycle, semantic ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome. One real property can require a Flow; repeated calls without such ownership do not.

**ModuleCommand** — a target-owned addressed public command payload for another Ball authority.

**ModuleCommandPulse** — the canonical verified target input carrying accepted-source `commandSource`, effective protocol identity, target-owned `ModuleCommand`, and issuer provenance; target `decide` is its sole acceptance point.

**ModuleCommandRequest** — a canonical `SemanticOutput` envelope with a `ModuleCommand` payload and accepted sequence position; its inter-Ball path activates a complete `SemanticHandle` and materialized `sourceOrdinal`.

**ModuleResult** — a target-owned business-outcome payload for one target command mapping; it is emitted only through `ModuleResultOutput` and received as the payload of `ModuleResultPulse`.

**ModuleResultOutput** — the canonical target `SemanticOutput` created only inside an accepted target Decision, carrying target-frame `sourceOrdinal`, accepted-source `commandSource`, target-owned `ModuleResult`, and `semanticHandle = commandSource.semanticHandle` as correlation without ownership transfer.

**ModuleResultPulse** — the canonical verified source input carrying accepted-source `commandSource`, accepted-target `resultSource`, effective protocol identity, target-owned `ModuleResult`, and issuer provenance.

**Nucleus** — the Ball's pure bounded semantic authority that owns State, protocol/context schemas and interpretation, and the sole local business-decision/Policy Gate. It performs no I/O or raw provenance verification and imports only its own artifacts—including Ball-local utilities owned by the Nucleus role—mechanical foundation, and exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts, without ownership transfer.

**ObservedSignal** — a provenance-checked causal input envelope created from a previously accepted `SignalPublication` over a declared signal route.

**Operation Status Authority** — the one committed revisioned single-writer query authority for a declared status namespace. It losslessly materializes only reachable lifecycle, acceptance, cancellation, accepted-result, ambiguity, delivery-stop, and retention facets through causal-order application or bounded pending, idempotent monotonic conflict handling, pre-acceptance capacity reservation with no later eviction/truncation, and a covered-source/empty-pending marker before absence with no resurrection. Co-location can reduce lag while separation can isolate query load; either form remains one query authority, transfers no command/business-fact authority, and makes no unsupported cross-source freshness promise.

**OperationId** — the stable identity of an accepted logical operation with a detached or independently observable lifecycle; it is not equal to a RequestId or delivery attempt and is absent when call scope is sufficient. A pre-acceptance reservation is only a candidate value and, without an accepted root frame, creates no operation or status authority.

**OutcomeUnknown** — a state in which an external action might have occurred but no proven outcome exists. Validation, admission, pre-acceptance rejection, and an unsent action cannot create it; prior acceptance remains while reconciliation seeks evidence.

**OutputId** — the runtime/storage identity of a committed output record. It need not be part of domain state.

**Policy Gate** — the Nucleus semantic rule that alone determines business permission and, for an actor-dependent pure read, selects one permitted, denied, redacted, or deliberately non-disclosing variant from the Query's total target-owned `ResultPayload` using State, Query, and trusted context. It is distinct from the target/resource Execution Gate; a pure read creates no `Decision` and uses no `BusinessRejection` or post-admission `BoundaryResponse`.

**Projection** — immutable semantic view state for a consumer/renderer.

**ProjectionOutput** — a canonical `SemanticOutput` envelope with a `Projection` payload and accepted sequence position; a stable handle is present only when the projection is detached/addressable.

**Pulse** — the ordered closed family `Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse`; raw external mutation/cancellation enters only as an `Intent` after Interaction validation and is not a field of `DecisionContext`.

**Query** — a non-mutating read request to committed state or a read authority; for a concrete protocol version it maps unambiguously to one target-owned result payload that is total over every reachable post-admission semantic outcome.

**Read Model Ball** — the owner of materialized read state, source positions, freshness/consistency metadata, and rebuild policy. It does not accept command authority over source facts or become their second writer.

**ReadContext** — a field-minimized trusted local-read context whose actor-dependent form is verified, bounded, and constructed by the trusted binding boundary under a Ball/Nucleus-owned semantic schema. Actor context appears only when it changes read authorization/result selection; fixed same-stack issuer/realm may be proven statically, actor-independent reads create no actor artifacts, and selectors belong to the typed `Query`.

**ReadDependency** — the resolved cross-authority read contract naming one caller, target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Triggered version/auth/cache/ordering/buffering/retry/status fields are sparse; caller/Assembly cannot redefine target payload, stamp, status fact, or meaning, and independent reads imply no atomic multi-source snapshot.

**ReadResult** — the successfully evaluated noncommitted Query wrapper used when the stamped-read trigger applies, containing the target-owned payload and field-minimized `ConsistencyStamp`. The payload is total over every reachable post-admission semantic outcome, including declared denial/redaction/non-disclosure when present; a same-stack getter may return it directly, and neither form receives commit/semantic identity.

**Replica State** — an external/cache copy with provenance, revision, and freshness; it is not decision authority.

**Reply** — an addressed semantic response bound to a request/operation and existing only as the payload of an accepted `ReplyOutput`; a pre-acceptance boundary failure or rejection is not a Reply.

**ReplyOutput** — a canonical `SemanticOutput` envelope with a `Reply` payload and accepted sequence position; a stable handle is present only when reply delivery/status is detached or independently addressable.

**Representation Erasure** — omission of a materialized adapter, envelope, wrapper, or field when exact enclosing type/control-flow evidence preserves the same semantic value, accepted tuple, ownership, authority, bounds, and verification. It changes representation only; it cannot create an alias protocol, remove a triggered guardrail, or transfer meaning.

**RequestId** — the materialized identity of one independently correlated transport/request attempt; direct call-scope ingress needs none, and the ID does not replace `IdempotencyKey` or `OperationId`.

**Resource Hemisphere** — the adapter/executor role that implements an accepted Effect through the minimum capability and applies the immediate Execution Gate; it checks triggered proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink bindings and maps typed outcomes to provenance-bound `Fact` without making a new business decision.

**RetainedContinuation** — a bounded single-owner continuation of a reserved synchronous causal chain; every retained item preserves its own trusted Pulse-to-DecisionContext association, and resume continues the original total causal budget and declared terminal policy without adding that runtime budget to Context.

**Safe Sink** — the applicable parameterized, structured, capability-rooted, or context-encoded API at an interpreter or dialect boundary; it prevents untrusted data from being interpreted as code, traversal, or dialect syntax.

**SemanticHandle** — the stable domain-visible identity of planned work that is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible; immediate call-scope output needs only its accepted sequence position.

**SemanticOutput** — the ordered closed family `ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest`; every present variant has a typed payload and accepted zero-based sequence position, while a stable `SemanticHandle` is added only for detached/addressable work.

**Set-Equal** — a derived rendering whose complete tuple—Rule and modal force, Applicability and trigger, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route—matches its named primary source record with nothing added, omitted, strengthened, or weakened.

**Signal** — a semantic publication without one mandatory requester; not a universal `DataChanged`.

**SignalPublication** — a canonical routed `SemanticOutput` envelope with a `Signal` payload, complete `SemanticHandle`, and materialized `sourceOrdinal`.

**SnapshotOutbox** — a durability profile in which a state snapshot is accepted durably and any present durable source outputs join the same atomic transaction; retry, duplicate handling, terminal `DispatchStopped`, and status/retention facets appear only when their delivery paths exist, and eventual delivery is not guaranteed.

**Sovereign State** — canonical mutable state with one decision authority.

**State Belt** — a logical map of isolated state authorities; not a global mutable-store API.

**StateKey** — the identity of a BallInstance's local state/consistency/partition boundary.

**StorageTransactionId** — the mechanical identity of the successfully committed storage transaction that supported acceptance; it is not a domain-visible handle, operation identity, or independent proof of a business outcome.

**Structural Allocation** — an allocation that exists only because of an architecture runtime mechanism, not because of useful payload.

**TimerRequest** — a canonical detached `SemanticOutput` envelope with a complete `SemanticHandle`, materialized `sourceOrdinal`, and a `TimerIntent` payload for a Ball with declared timers.

**TriggerAbsenceProof** — the closed static evidence record materialized only when a Pokeball conformance/release claim or accepted ambiguity-resolution decision relies on absence of a `path-triggered`, `risk-triggered`, or `claim-triggered` predicate. It binds the exact anchor, scope/profile, inventory and revisions/digests, evaluated predicate, `Absent` conclusion, evidence owner, and invalidation conditions; it cannot negate `always` or a present trigger/claim and becomes unusable until reevaluated after invalidation.

**Trusted Boundary** — an explicitly authorized binding edge that verifies representation, finite bounds, provenance, accepted-frame correspondence, protocol identity, and every triggered authenticity/integrity/version/validity rule before constructing trusted semantic input, non-empty `DecisionContext`, or actor-dependent `ReadContext`. It is an edge, not the Interaction role; physical co-location transfers neither semantic interpretation nor business authority. The Ball/Nucleus owns semantic schema and interpretation. For a command/result bridge it constructs `ModuleCommandPulse`/`ModuleResultPulse` only from the corresponding accepted frame; it does not replace target `decide`, select policy/read results, synthesize business meaning, or grant authority merely by authenticating origin.

**Workflow Sovereignty** — the rule of one coordination owner for one stateful multi-participant workflow.

**Zero Mandatory Runtime Tax** — the absence of mandatory mediator/reflection/serialization/queue/thread-hop/object-hierarchy overhead in Core Inline semantics; concrete zero-allocation claims require a benchmark.
<!-- pkb:generated:end -->

---

## 23. Canonical statement

> **Pokeball Architecture structures an application as explicitly composed functional modules. Every Ball preserves logically separated Interaction, Protocol Nucleus, and Resource/route authority even when code generation or one call stack erases physical adapters; trusted bindings construct verified context, the Nucleus owns business permission, execution gates enforce current technical constraints, and shared foundation remains mechanical. Each Ball has one owner and one writer of canonical state, decides through a pure bounded function, accepts each Decision atomically, and exposes only closed typed protocol paths. A reachable path or risk activates its causal, delivery, retry, cancellation, status, security, composition, or profile guardrail; a concrete claim activates its evidence. Each activated guardrail resolves once through construction, a local declaration, or an exact reusable project/binding policy, while absent paths create no placeholder ceremony.**

Short formulas:

```text
One mutable fact — one authority.
One instance — one writer.
One stateful workflow — one coordinator.
Only the Nucleus creates a semantic effect.
Commit first, then dispatch.
Timeout does not prove failure.
Cancellation does not prove stop.
Dependencies and resources are always bounded.
The mechanism is proportional to the guarantee.
Absent path — absent ceremony.
Present trigger — one effective guardrail.
Declare once; reference exactly.
```

---

**End of Pokeball Architecture — Core Specification `1.4.0-draft`.**
