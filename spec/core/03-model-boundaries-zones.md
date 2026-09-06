# Core part — Model, boundaries, and zones

[Core contents](../pokeball-architecture-core.md) · [← Status, scope, and goals](00-status-scope-goals.md) · [Protocol algebra →](06-protocol-algebra.md)

> Canonical part 2 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 3. Canonical model

### 3.1. Ball type and instance

`BallType` defines the protocol and decision semantics. A `BallInstance` is a concrete state scope and single-writer authority.

```text
BallInstanceId {
    namespace
    ballType
    stateKey
}
```

- `namespace` prevents collisions between deployments, tenants, or realms;
- `ballType` identifies the protocol family;
- `stateKey` defines the local consistency and partition boundary.

The semantic scope always exists. The `BallInstanceId` record is materialized only when there are multiple instances or when identity crosses a persistence, route, detached-delivery, status, ownership-transfer, or observability boundary. For a local singleton Ball, the enclosing typed scope may prove the identity; no serialized `namespace`, `ballType`, or `stateKey: singleton` object is required.

### 3.2. State revision

Accepted mutations are totally ordered within one instance. A materialized monotonic `CommitRevision` is required when that order is used for optimistic concurrency, asynchronous ordering, durable acceptance, stamped reads/status, movable ownership, replay, or externally observable snapshot identity. A sequential local binding with none of those paths may prove order by its single-writer call scope and need not add a domain-visible revision field.

```text
CommitRevision: UInt64 or wider monotonic value
```

When present, the revision increases even when semantic state bytes do not change but an input is accepted, operation state is updated, or a delivery or cancellation result is recorded. A revision is not a wall-clock timestamp.

### 3.3. Canonical mutation and read forms

The selected state profile fixes exactly one of the following mutation functions and result types. They do not form a runtime union, and a binding is not required to implement both accepted branches.

Snapshot form, selected by `Transient` or `SnapshotOutbox`:

```text
decide(
    state: State,
    pulse: Pulse,
    context: DecisionContext
) -> SnapshotDecisionResult<State>

SnapshotDecisionResult<State> =
    Accepted(SnapshotDecision<State>)
  | Rejected(BusinessRejection)

SnapshotDecision<State> {
    nextState
    outputs: BoundedSequence<SemanticOutput>
}
```

Event form, selected by `EventJournal`:

```text
decide(
    state: State,
    pulse: Pulse,
    context: DecisionContext
) -> EventDecisionResult

EventDecisionResult =
    Accepted(EventDecision)
  | Rejected(BusinessRejection)

EventDecision =
    EventMutation {
        events: NonEmptySequence<DomainEvent>
        outputs: BoundedSequence<SemanticOutput>
    }
  | NoDomainChange {
        outputs: BoundedSequence<SemanticOutput>
    }
```

When the closed output set is empty, `outputs` is the statically empty sequence and may be representation-erased. A snapshot binding may implement the accepted value as `SnapshotDecision(nextState)` without allocating a collection or envelope. EventJournal never returns an independent `nextState`; §8.9 defines its exact accepted commit and `evolve` contract.

This is the compact canonical comparison. The detailed sections add only fields and behavior activated by their named trigger; they do not replace a row with a different operation algebra.

| Operation form | Semantic owner and explicit input | Returned semantic value | Acceptance and revision | Legal rejection or failure stage |
|---|---|---|---|---|
| Snapshot mutation | Nucleus; `State + Pulse + DecisionContext` | `Accepted(SnapshotDecision { nextState, outputs }) \| Rejected(BusinessRejection)` | Runtime/acceptor atomically publishes the snapshot accepted frame; `CommitRevision` follows §3.2 | representation/type validation or admission before `decide`; business rejection by `decide`; declared fault paths after acceptance |
| EventJournal mutation | Nucleus; `State + Pulse + DecisionContext` | `Accepted(EventMutation { events, outputs } \| NoDomainChange { outputs }) \| Rejected(BusinessRejection)` | Runtime/acceptor atomically records `AcceptedEventCommit`; every accepted result advances `CommitRevision`; State is reconstructed through `evolve` | the same stage classes as snapshot mutation; no synthetic `nextState` or event on rejection |
| Query/read | Nucleus/read Policy Gate; committed snapshot + `Query + ReadContext` | successful evaluation returns the one target-owned `ResultPayload`, wrapped in `ReadResult` when the stamped-read trigger applies; that payload closes every reachable permitted, denied, redacted, or deliberately non-disclosing semantic outcome | no Decision acceptance, accepted marker, new revision, handle, or semantic output; the stamp reports the snapshot's existing revision | validation/admission may fail before `read`; after admission the target-owned payload carries the semantic result, never `BusinessRejection`, exception, or an invented `BoundaryResponse` |

A programming fault, crash, nontermination, or violated internal invariant is not a `BusinessRejection`. It is handled by the runtime fault policy and does not create a partial `Decision`.

### 3.4. Cause, decision, and consequence

```text
Cause:
    Intent | Fact | ModuleCommandPulse | ModuleResultPulse
  | ObservedSignal | ControlPulse

Decision:
    SnapshotDecision(nextState + semantic outputs)
  | EventMutation(nonempty events + semantic outputs)
  | NoDomainChange(semantic outputs)

Consequence:
    ProjectionOutput | ReplyOutput | EffectRequest
  | ModuleCommandRequest | ModuleResultOutput
  | SignalPublication | TimerRequest
```

`Projection`, `Reply`, `Effect`, `ModuleCommand`, `ModuleResult`, `Signal`, and `TimerIntent` are semantic payload types. `ModuleCommand` and `ModuleResult` are owned by the target contract. The `Nucleus` returns output payloads only as the semantic output family defined in §6.12; immediate typed calls need no materialized envelope. Mechanical delivery records, transport attempts, storage rows, and tracing metadata are not business outputs of the `Nucleus`.

### 3.5. Semantic and mechanical identity

Identifiers fall into two classes.

**Semantic IDs** belong to the domain and may be stored in state:

<!-- pkb:pba-source:start id="PBA-16" title="Trusted Identifier Allocation" -->
**Source clause for PBA-16 — Trusted Identifier Allocation.**

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
<!-- pkb:pba-source:end -->
<!-- pkb:term:start name="SemanticHandle" -->
**SemanticHandle** — the stable domain-visible identity of planned work that can outlive its call, be retried or reordered, be independently cancelled, reconciled, recovered, or observed. Temporary retention within the current call and crossing a Ball boundary alone do not require a handle; immediate output uses its accepted sequence position and typed call scope.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-15" title="Semantic Handle" -->
**Source clause for PBA-15 — Semantic Handle.**

- **Rule:**
  Semantic state refers to planned work through a stable `SemanticHandle` when that work is retained beyond the current call, can complete later, be retried or reordered, be independently cancelled or reconciled, survive recovery, or be observed independently, including through operation status:

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

  but it does not rewrite business state merely to materialize a transport ID. An immediate same-build call may use its typed target and call scope as command identity and provenance. Crossing a Ball boundary alone does not require a materialized handle, source token, result token, or protocol identifier. Retaining an intermediate value only for the current call does not activate stable identity. The accepted output position and call/return relation identify this work without an `OperationId`, `SemanticHandle`, or wrapper object.

  The stable semantic-identity rule for detached or addressable planned work is the `SemanticHandle` contract in the preceding paragraph; immediate accepted call-scope work remains identified by its accepted frame position.
- **Applicability:** `P`: work outlives the current call, can complete later, be retried or reordered, be independently cancelled, reconciled, recovered, or observed.
- **Declaration owner:** Ball state/protocol owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Nucleus/runtime identity mapping tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Allocation may be shared; omit operation/handle artifacts for immediate call-scope work.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 3.6. Identity hierarchy

The following identifiers are not interchangeable:

| Identity | What it identifies | When it appears | Reuse |
|---|---|---|---|
| `RequestId` | One independently correlated transport or request attempt | When an attempt crosses or outlives call scope | New for each new attempt |
| `IdempotencyKey` | Repeated acceptance of one logical mutation | Before ingress acceptance | Preserved across client retries |
| `OperationId` | An accepted business operation with an independently observable lifecycle | Reserved before `decide`, authoritative after acceptance | Stable through that lifecycle |
| `SemanticHandle` | Detached, addressable planned work or step within an operation | In a Nucleus decision when the detached-work trigger applies | Stable across delivery retries |
| `CommitId` | An accepted runtime commit record | At acceptance or commit | Stable for that commit record |
| `OutputId` | A committed runtime output record | At acceptance or commit | Stable across redelivery |
| `AttemptId` | One dispatch or execution attempt | Before a specific attempt | New for each attempt |
| `StorageTransactionId` | The committed storage transaction that supported acceptance | After a successful storage commit | Unique to that transaction |

Reserving a candidate `OperationId` supplies deterministic identity to a possible Decision; it does not create an operation. If root validation, admission, or `decide` rejects before acceptance, that candidate never becomes authoritative and creates no `SemanticHandle`, known operation-status row, retention marker, output, or accepted-operation lineage. The boundary returns only its typed §6.13 response.

Typical lineage for a retryable detached operation:

```text
RequestId attempt-1 ─┐
RequestId attempt-2 ─┼─ same IdempotencyKey
                     └─ OperationId
                          └─ SemanticHandle
                               └─ OutputId
                                    ├─ AttemptId 1
                                    └─ AttemptId 2
```

A new `RequestId` must not be used to bypass idempotency, and a new `OperationId` must not be created merely because a transport attempt was repeated. A path that has no retry, detached work, durable record, status, or correlation beyond its current call does not materialize this full lineage.

### Definition source records for §3

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="AttemptId" -->
**AttemptId** — the mechanical identity of one delivery/execution attempt; it changes on retry and does not replace `OperationId`, `SemanticHandle`, or `OutputId`.
<!-- pkb:term:end -->


<!-- pkb:term:start name="CommitId" -->
**CommitId** — the mechanical identity of an accepted runtime commit record; it does not replace `CommitRevision` and is not used as pre-commit semantic identity.
<!-- pkb:term:end -->

<!-- pkb:term:start name="CommitRevision" -->
**CommitRevision** — the materialized monotonic number of an accepted mutating input/Decision within a BallInstance when ordering is observed across concurrency, persistence, async, reads/status, ownership, replay, or another boundary; sequential local call scope may prove order without storing it.
<!-- pkb:term:end -->




<!-- pkb:term:start name="OperationId" -->
**OperationId** — the stable identity of an accepted logical operation with a detached or independently observable lifecycle; it is not equal to a RequestId or delivery attempt and is absent when call scope is sufficient. A pre-acceptance reservation is only a candidate value and, without an accepted root frame, creates no operation or status authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="OutputId" -->
**OutputId** — the runtime/storage identity of a committed output record. It need not be part of domain state.
<!-- pkb:term:end -->



<!-- pkb:term:start name="RequestId" -->
**RequestId** — the materialized identity of one independently correlated transport/request attempt; direct call-scope ingress needs none, and the ID does not replace `IdempotencyKey` or `OperationId`.
<!-- pkb:term:end -->




<!-- pkb:term:start name="StorageTransactionId" -->
**StorageTransactionId** — the mechanical identity of the successfully committed storage transaction that supported acceptance; it is not a domain-visible handle, operation identity, or independent proof of a business outcome.
<!-- pkb:term:end -->



---

## 4. Choosing a Ball boundary

A `Ball` is not automatically a screen, endpoint, table, repository, service, aggregate, or bounded context. Its boundary is chosen according to authority and invariant scope.

### 4.1. Parts should be combined into one Ball when they have

- invariants that must be accepted by one decision;
- one state key and one local consistency boundary;
- a shared lifecycle and recovery unit;
- one semantic owner;
- a closely aligned trust boundary;
- compatible scale and contention profiles;
- state size and transition complexity suitable for one instance.

### 4.2. Parts should be separated when they have

- independent terminal outcomes;
- different consistency or durability requirements;
- different privilege or trust boundaries;
- different partition keys or materially different load profiles;
- independent lifecycles;
- a need for separate crash containment;
- different semantic owners and change cadences.

### 4.3. Practical rule

> **A Ball is the smallest operationally feasible scope within which the required decision can be made by one authority without access to another Ball's mutable state.**

A boundary that is too small creates protocol chatter and artificial workflows. A boundary that is too large creates a God Nucleus, a hot key, and an enormous change radius.

Core constrains which authority and dependency graphs are valid; it does not promise one unique decomposition graph for a system. Two partitions may both conform when each independently proves the same ownership, invariant, lifecycle, trust, boundedness, and dependency rules. Naming or package layout alone does not select a canonical partition.

### 4.4. Decision tree and falsifiers

Apply these questions in order:

1. Does the candidate own a mutable semantic fact, a business Decision, a protocol/lifecycle authority, or a private resource consequence? If none, keep it an ordinary utility or adapter rather than a Ball.
2. Must two parts accept one strict invariant under one state key, writer, lifecycle, and recovery unit? If yes, combine them unless doing so violates a stronger trust, ownership, partition, durability, or containment boundary.
3. Do parts have independent owners, state keys, lifecycles, terminal outcomes, trust/durability boundaries, or material load/containment needs? If yes, separate them and use a declared dependency rather than shared mutable state.
4. Does the candidate own a local capability's state and decisions? If yes, it is a Feature Ball candidate.
5. Does it own material coordination across authorities—lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or an independent terminal outcome? If yes, it is a Flow Ball candidate; call count or one command hop alone is insufficient.
6. Does it own only derived query state, source positions, freshness, and rebuild policy without command authority over source facts? If yes, it is a Read Model Ball candidate.
7. Is a value only focus, scroll, animation, parser, socket, or equivalent non-decision mechanics? Keep it `EphemeralState`. If it can change a business Decision, model it as committed State or an explicit trusted current `Pulse`/`DecisionContext` input.

Concrete falsifiers keep the labels testable:

| Choice | Supporting evidence | Falsifier |
|---|---|---|
| combine | one invariant, state key, writer, lifecycle, recovery, and semantic owner | either part requires independent acceptance, ownership, trust/durability, partition, terminal outcome, or crash containment |
| separate | each side has its own authority and can interact through a closed contract | preserving a required invariant would need direct foreign mutable-state access or a non-atomic split decision |
| utility | pure/stateless mechanics owned by one Ball role, or shared mechanical Foundation under PBA-43, with no owned protocol, lifecycle, state, or resource authority | it owns mutable semantic facts, business decisions, lifecycle, external consequence authority, or shared domain/business policy without one Ball/Flow owner |
| Feature Ball | owns one local capability's canonical state/decisions | it only coordinates other authorities and owns no local capability fact |
| Flow Ball | owns at least one material coordination property and its terminal policy | it is only one hop/call sequence with no independent lifecycle, ordering, recovery/cancellation/reconciliation, or outcome authority |
| Read Model Ball | owns derived query state, positions, freshness, and rebuild | it accepts commands for source facts, becomes their second writer, or has no materialized read authority |

### 4.5. Adoption/pilot boundary worksheet

When a project is evaluating or piloting Pokeball boundaries, the project architecture owner SHOULD record or otherwise prove the following decision inputs:

```text
name
ownedSemanticFacts
stateKey
strictInvariants
externalSourcesOfRecord
declaredDependencies
trustBoundary
lifecycleOwner
expectedStateSize
peakOpsPerKey
maximumTransitionCost
publicProtocolSurface
recoveryUnit
```

This is project-owned adoption guidance, not a universal Core artifact, runtime record, or conformance placeholder. Outside adoption/pilot work, no worksheet or empty fields are required. If a strict invariant cannot be enforced by the selected state key or storage mechanism, the boundary is wrong regardless of package-structure convenience.

The project selects the measurement method and acceptable range for `expectedStateSize`, `peakOpsPerKey`, `maximumTransitionCost`, and any §13.5 design-time-tax or benefit measure. Core supplies no universal Feature/Flow threshold. A pilot continue/reshape/stop decision compares those project-selected measures with the authority, invariant, recovery, containment, and change-radius benefit claimed for the slice.

### 4.6. What is not a Ball

A pure stateless helper, parser combinator, numeric library, formatter, or value-object package without its own state, lifecycle, protocol, and resource authority is ordinarily a utility, not a `Ball`. A Ball-local utility is an implementation artifact of exactly one Ball and one logical role; another authority does not import it as shared domain policy. A utility shared across Balls is permitted only when it is mechanical Foundation under PBA-43. Shared domain or business semantics instead remain as explicit local implementations or acquire one Ball/Flow owner and a declared Application Surface/protocol.

### Definition source records for §4

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Feature Ball" -->
**Feature Ball** — a Ball that owns a local business capability/state authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Flow Ball" -->
**Flow Ball** — a Ball that owns material coordination for a specific workflow: any independent lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or terminal outcome across authorities. Call count or one hop alone is insufficient.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Material Coordination" -->
**Material Coordination** — cross-authority ownership of a workflow lifecycle, semantic ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome. One real property can require a Flow; repeated calls without such ownership do not.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Read Model Ball" -->
**Read Model Ball** — the owner of materialized read state, source positions, freshness/consistency metadata, and rebuild policy. It does not accept command authority over source facts or become their second writer.
<!-- pkb:term:end -->


---

## 5. Three logical zones

The zones are architectural roles. They do not require three processes, three classes, or three heap objects.

<!-- pkb:pba-source:start id="PBA-01" title="Three-Zone Boundary" -->
**Source clause for PBA-01 — Three-Zone Boundary.**

- **Rule:** Every application `Ball` has the logically separated Interaction, Nucleus, and Resource/route roles defined in §§5.1–5.3. This separation preserves their distinct authority and permitted calls even when one file, call stack, or generated binding uses **representation erasure**—omitting a materialized wrapper or adapter while proving the same semantic facts and role edges (§22); physical co-location alone is not evidence of separation.
  The logical-role and verification/construction-edge map may be carried directly by authoritative source and its enclosing binding: typed entrypoints, visibility/import restrictions, actual call sites, verified construction sites, and accepted-write sites. When those facts are unambiguous and inspectable there, no separate map document, table, or manifest is required. Add annotations or exact source references only for facts not already evident; an annotation is not enforcement. Every present path still has an identifiable role, authority, verified origin where required, and permitted call direction; empty Resource roles require no implementation artifact.
- **Applicability:** `A`: every Ball, including same-file/stack/generated layouts.
- **Declaration owner:** Ball owner defines the logical-role map and distinct authority.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Binding source/type graph, role-versus-Trusted-Boundary-edge map, call-graph, provenance, and accepted-write evidence.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared layout/linter may be referenced; physical adapters and empty-zone packages are optional, role separation is not.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

### 5.1. Interaction Hemisphere

Interaction adapts the external world to the semantic protocol and back.

Interaction is a logical adapter role. A Trusted Boundary is an authorized verification-and-construction edge, not another logical zone and not a synonym for Interaction. A concrete binding commonly realizes that edge at Interaction or route ingress; co-location or representation erasure is legal only while the edge's verification/provenance evidence and Interaction's adaptation duties remain separately inspectable. Neither gains business-policy or State authority from the other.

For each present boundary path, it MUST perform the applicable steps:

- parse wire/platform input when raw representation crosses the boundary;
- reject malformed input;
- normalize only according to declared field rules;
- authenticate through a trusted boundary when actor/tenant authority is used;
- validate representation and every declared closed-protocol/type invariant, including its owned ranges, sizes, and semantic shape;
- construct a verified, bounded, **field-minimized** `DecisionContext` or actor-dependent `ReadContext` from trusted observations when that context is required—containing only fields activated by the actual semantic and boundary triggers (§22);
- create a typed external `Intent` or `Query`, construct a verified `ModuleCommandPulse` from an accepted source frame on a declared command route, or return a non-committed `BoundaryResponse`;
- transform each present response/output kind into context-safe output;
- apply encoding and escaping when data crosses an interpreter/channel boundary.

Classification follows PBA-04 and §6.13, not the visual shape of a predicate. Interaction returns `ValidationFailure` only for representation or a declared protocol/type invariant whose truth is independent of committed State, semantic Context, and business policy. A State/Context-dependent rule or a rule deliberately owned as a business choice reaches the Nucleus. A fixed constraint moves to Interaction only when the protocol owner makes it a versioned closed-type invariant; changing that ownership changes protocol semantics rather than silently moving the same rule between stages.

An already trusted, bounded, typed same-stack input may need only semantic validation and adaptation; it does not require a parser, authentication object, or serialized DTO merely to satisfy the logical zone.

The trusted binding boundary owns raw origin, authenticity, integrity, version, validity, and finite-size verification before it constructs either context. The Ball/Nucleus owns the closed semantic context schema and interprets the verified fields. A fixed trusted same-stack issuer/realm may be proven by the enclosing binding instead of copied into a context value; this representation erasure does not move semantic interpretation or business permission into the boundary.

For a command route, the target's trusted Interaction/route boundary owns the §6.8 verification and construction step. It preserves the accepted source `commandSource`, effective protocol identity, target-owned command payload, and issuer provenance; it neither converts the command into an external `Intent` nor treats transport receipt as target acceptance. The corresponding verified result boundary constructs `ModuleResultPulse` only from an accepted target `ModuleResultOutput` frame.

It MUST NOT:

- mutate Sovereign State;
- make a business transition;
- make a business permission or read-result-selection decision;
- create an `Effect` or `ModuleCommand`;
- call a Resource adapter directly for an application operation;
- treat a request field as proof of authentication;
- pass a raw framework object into the `Nucleus`.

Interaction may hold ephemeral UI or transport state: focus, scroll, animation, socket buffers, and request-parser state. If a UI or transport value can change a business Decision, it is not `EphemeralState`: retain it as committed State when it must survive the current cause, or supply it as an explicit trusted current `Pulse`/`DecisionContext` input according to §8.1. A hidden widget/controller/cache value never becomes decision authority merely because it is locally available.

### 5.2. Protocol Nucleus

The `Nucleus` is the sole place for a local business decision.

<!-- pkb:term:start name="Nucleus" -->
**Nucleus** — the Ball's pure bounded semantic authority that owns State, protocol/context schemas and interpretation, and the sole local business-decision/Policy Gate. It performs no I/O or raw provenance verification and imports only its own artifacts—including Ball-local utilities owned by the Nucleus role—mechanical foundation, and exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts, without ownership transfer.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-03" title="Pure Bounded Nucleus" -->
**Source clause for PBA-03 — Pure Bounded Nucleus.**

- **Rule:** For every Decision path, the Nucleus owns the semantic schema and interpretation of its explicit inputs and context, is the sole local business-decision and business-permission authority, performs no I/O or raw provenance verification, and terminates within the effective bounds resolved by §8.3. When a numeric `maxTransitionSteps` declaration supplies that bound rather than a static proof alone, the binding-owned Decision Work Meter from §8.3 measures it without changing business meaning.
- **Applicability:** `A`: every Decision path; a numeric `maxTransitionSteps` declaration activates the exact meter contract.
- **Declaration owner:** Ball/Nucleus owns pure signature, semantic context interpretation, policy, and bounds; binding owns meter mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Nucleus/binding purity, sole-policy-site, provenance-boundary, termination, static-bound or meter-resolution/determinism tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact meter mechanics may be referenced; semantic ownership remains local, and a static proof alone needs no meter artifact.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->

It:

- owns the Sovereign State instance;
- owns the closed semantic schemas and interpretation of `DecisionContext` and any actor-dependent `ReadContext` used by its pure read surface;
- imports only its own State, protocols, and Ball-local Nucleus utility artifacts, mechanical foundation, and the exact declared target- or producer-owned `Application Surface` types required by its closed `Query`, `Pulse`, and `Decision` contracts;
- accepts a closed `Pulse` and explicit `DecisionContext`;
- returns a bounded `Decision`;
- performs no I/O;
- reads no ambient clock, randomness, environment, locale, feature flags, or service locator;
- imports no platform SDK or HTTP, SQL, or filesystem drivers;
- does not mutate runtime ledgers directly;
- does not call another Ball's Nucleus;
- imports no foreign mutable State, Nucleus internals, private Resource adapters, caller-owned mirror or redeclared target types, or re-exported foreign contracts.

The `Nucleus` Policy Gate alone decides business permission from committed State, the current cause or Query, and verified semantic context. It may form action-scoped grant constraints, but it neither verifies raw credential/provenance material through I/O nor delegates business choice to the binding or Resource. Final technical enforcement at the target or resource remains the separate Execution Gate in §11.3.

<!-- pkb:pba-source:start id="PBA-06" title="Controlled Causality" -->
**Source clause for PBA-06 — Controlled Causality.**

- **Rule:** When a semantic external-action output exists, its semantic intent is created only by the Nucleus inside a Decision. This includes constructing a target-owned `ModuleCommand` through its exact declared imported Application Surface; Interaction and Assembly verify, bind, and transport but do not create or synthesize that semantic intent.
- **Applicability:** `P`: semantic external-action output exists.
- **Declaration owner:** Ball Nucleus/output protocol.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency rule and output tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Envelope mechanics may be shared; omit when output set is empty.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->

### 5.3. Resource Hemisphere

Resource executes its own `Ball`'s private `Effect` or provides an adapter to a declared external resource.

These obligations are path-triggered by a present Resource operation. A state-only Ball has an empty logical Resource zone and needs no adapter, capability wrapper, or resource test fixture.

For every present operation, Resource MUST:

- hold the minimal capability;
- obey the finite effective request and response bounds of that operation;
- make no new business decision.

Immediately before authoritative execution, the Resource/target Execution Gate verifies the triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink bindings from §11.3. It enforces the already accepted action and returns a declared typed technical outcome; it does not reinterpret business permission or create a second business decision.

Additional obligations materialize only with their trigger:

- an interpreter edge uses a safe sink;
- a deadline path obeys its timeout contract;
- a cancellation path obeys its cancellation contract;
- a result-producing path validates the external response and maps exceptions and statuses into a closed `Fact`;
- a result that crosses call scope returns the correlation and verified provenance required by that route.

Resource MUST NOT:

- mutate Sovereign State;
- call Interaction to make a decision;
- replace domain retry or fallback policy with a hidden unbounded loop;
- return a database entity, HTTP response, or SDK object to the `Nucleus`;
- use an unrestricted shared credential when least privilege is claimed.

### 5.4. Polar isolation

<!-- pkb:pba-source:start id="PBA-02" title="Polar Isolation" -->
**Source clause for PBA-02 — Polar Isolation.**

- **Rule:** Interaction and Resource/route roles form no direct application or business path. Shared or generated mechanics may serve both only when they carry no mutable business meaning and create no hidden communication path.
- **Applicability:** `A`: every Ball.
- **Declaration owner:** Ball owner defines permitted role edges.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Compiler/dependency/call-graph and hidden-communication tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared graph rule may be reused; mechanical sharing proves no mutable business path rather than adding a per-Ball boolean.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

```text
Interaction --X--> Resources
Resources   --X--> Interaction
```

The permitted semantic path passes through the `Nucleus`. Mechanical infrastructure—a logger, allocator, tracing primitive, or bounded collection—may be used by both sides only under the source clause above.

### 5.5. Permitted dependency graph

```text
interaction -> own external protocol
resources   -> own resource protocol
nucleus     -> own state + own protocols + Ball-local Nucleus utilities + exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts + mechanical foundation
assembly    -> public protocols + routes
runtime     -> Ball integration points, not business internals
```

This table states both intra-Ball role dependencies—including Ball-local utilities owned by one role—and the only permitted inter-Ball semantic import. A local utility import remains in the compile-time graph but creates no inter-authority route or dependency row. Importing an Application Surface does not transfer ownership. The Nucleus MUST NOT import foreign mutable State, internals, private Resource adapters, platform/I/O implementations, ownerless shared domain utilities, or caller-owned mirrors; it MUST NOT redeclare or re-export an imported protocol. Interaction and Assembly MUST NOT synthesize an imported payload, mapping, refusal, or other business meaning.

Prohibited:

```text
interaction -> concrete resource adapter
resource adapter -> UI/controller
nucleus -> platform SDK
Ball A -> internals/state of Ball B
business code -> global service locator
```

### Definition source records for §5

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="Interaction Hemisphere" -->
**Interaction Hemisphere** — the external/route input-output adapter role: parsing, normalization, validation, bounds, encoding, accepted-frame verification, and trusted `DecisionContext`/actor-dependent `ReadContext` construction when triggered; it does not interpret business permission or select a business-visible read result. A present path may contain or adjoin a Trusted Boundary edge, but the adapter role and authorized verification/construction edge are not synonymous.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Representation Erasure" -->
**Representation Erasure** — omission of a materialized adapter, envelope, wrapper, or field when exact enclosing type/control-flow evidence preserves the same semantic value, accepted tuple, ownership, authority, bounds, and verification. It changes representation only; it cannot create an alias protocol, remove a triggered guardrail, or transfer meaning.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Resource Hemisphere" -->
**Resource Hemisphere** — the adapter/executor role that implements an accepted Effect through the minimum capability and applies the immediate Execution Gate; it checks triggered proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink bindings and maps typed outcomes to provenance-bound `Fact` without making a new business decision.
<!-- pkb:term:end -->


---
