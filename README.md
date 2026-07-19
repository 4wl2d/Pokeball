<h1 align="center">Pokeball</h1>

<p align="center">
  <a href="spec/pokeball-architecture-core.md">
    <img src="assets/pokeball-architecture-hero.svg" alt="Pokeball Architecture" width="100%" />
  </a>
</p>

<p align="center">
  <strong>An architecture specification for applications built from explicitly composed, state-owning functional modules with pure bounded decisions, closed typed protocols, and evidence-qualified guarantees.</strong>
</p>

<p align="center">
  <a href="spec/pokeball-architecture-core.md"><img alt="Core specification" src="https://img.shields.io/badge/CORE-SPECIFICATION-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="docs/agents/README.md"><img alt="Agent Pack" src="https://img.shields.io/badge/AGENT-PACK-0969DA?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="#adopt-pokeball"><img alt="Adoption guide" src="https://img.shields.io/badge/ADOPTION-GUIDE-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a><br />
  <a href="docs/ru/README.md"><img alt="Russian overview" src="https://img.shields.io/badge/RU-OVERVIEW-0969DA?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="#documentation"><img alt="Documentation" src="https://img.shields.io/badge/READ-DOCUMENTATION-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="#license-and-authorship"><img alt="CC BY 4.0 license" src="https://img.shields.io/badge/CC_BY_4.0-LICENSE-58A6FF?style=flat-square&amp;labelColor=0D1117" /></a>
</p>

<table>
  <tr>
    <td width="33%" align="center">
      <strong>Explicit ownership</strong><br />
      <sub>One explicit canonical state scope per Ball.</sub>
    </td>
    <td width="33%" align="center">
      <strong>Pure bounded decisions</strong><br />
      <sub>No I/O or ambient authority in the Nucleus.</sub>
    </td>
    <td width="33%" align="center">
      <strong>Closed typed protocols</strong><br />
      <sub>Only used paths, each with an effective finite bound.</sub>
    </td>
  </tr>
</table>

> [!IMPORTANT]
> For each law, the marked `Source clause for PBA-xx` in the numbered body of the [Core specification](spec/pokeball-architecture-core.md) is the sole normative authority. The law list, applicability matrix, tests, checklist, examples, glossary, this README, and the [Agent Pack](docs/agents/README.md) are projections. If a projection differs from its source clause, the source clause controls.

This repository contains the Core specification and practical adoption guidance. It does not contain a Pokeball runtime, framework, library, or reference implementation.

## What problem does Pokeball address?

Stateful applications become difficult to reason about when state ownership, side effects, asynchronous results, authorization, retries, and delivery guarantees are implicit. Pokeball makes those decisions visible in the architecture:

- Who owns each mutable fact?
- Which exact inputs may change it?
- Which typed outputs may leave the module, and only after what acceptance point?
- How does an asynchronous result prove which operation caused it?
- Which queues, retries, fan-out, state, and work limits are finite?
- Which guarantees come from Core semantics, and which require a concrete runtime profile and evidence?

Pokeball does this without requiring a mediator, broker, queue, reflection, serialization, runtime DI container, or a particular database for a local `Inline` binding.

## The architecture in one sentence

A `Ball` is the smallest operationally feasible scope in which one authority can enforce the required invariants without reading another Ball's mutable state.

Each Ball:

1. owns one explicit canonical state scope;
2. separates external interaction, pure decision logic, and resource execution;
3. exposes only the closed typed protocol paths it actually uses;
4. atomically accepts its snapshot state mutation or `EventDecision` together with the complete semantic-output batch;
5. dispatches no `SemanticOutput` before that acceptance;
6. gives detached or independently observable work stable causal identity and verified provenance;
7. keeps every present variable dimension within one finite effective bound.

Pokeball has one sparse Core, not separate “Lite” and “full” architectures. Its always-applicable invariants remain universal. A reachable path or named risk activates only its guardrail; a concrete claim activates its evidence. Each activated guardrail resolves once through construction, a local declaration, or an exact immutable project-, profile-, Assembly-, or binding-scoped policy, with only permitted Ball-specific deltas. A proven-absent trigger needs no empty table, zero, `N/A`, or evidence placeholder.

`TriggerAbsenceProof` is materialized only when a Pokeball conformance or release claim, or an accepted ambiguity-resolution decision, relies on trigger absence. Routine design, implementation, adoption, or piloting creates no proof merely because a category is absent. An `always` obligation or present trigger cannot use such a proof; contrary present evidence invalidates it and requires the guardrail to resolve.

Core constrains valid authority and dependency graphs; it does not promise one unique decomposition graph. Boundary choices are falsifiable:

- combine parts when one strict invariant requires one state key, writer, lifecycle, and recovery unit, unless a stronger trust, ownership, partition, durability, or containment boundary forbids it;
- separate parts when they need independent owners, state keys, lifecycles, terminal outcomes, trust/durability boundaries, or material load/containment;
- keep a candidate as a utility or adapter when it owns no mutable semantic fact, business decision, protocol/lifecycle, or resource consequence;
- use a `Feature Ball` for one local capability's state and decisions, a `Flow Ball` for material cross-authority coordination, and a `Read Model Ball` only for derived query state, source positions, freshness, and rebuild policy without command authority.

A Ball is not automatically a screen, endpoint, table, repository, service, aggregate, or use case. `EphemeralState` is limited to focus, scroll, animation, layout, transport, and equivalent mechanics that cannot change a business `Decision`; a decision-relevant UI or transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext` input.

## One Ball at a glance

```mermaid
flowchart LR
    X["External input"] -->|"raw bytes / platform event"| I["Interaction Hemisphere<br/>parse · authenticate · validate · bound"]
    I -->|"validated Intent"| N["Protocol Nucleus<br/>Sovereign State<br/>pure bounded decide"]
    N -->|"Accepted Decision<br/>state mutation / event decision + bounded outputs"| A["Atomic acceptance / commit"]
    N -->|"Rejected(BusinessRejection)"| I
    A -->|"ProjectionOutput / ReplyOutput"| I
    A -->|"EffectRequest<br/>only after acceptance"| R["Resource Hemisphere<br/>minimum capability + triggered checks"]
    R -->|"validated Fact when a result path exists"| N
    I -->|"encoded view / response"| X
```

**Arrow legend.** Solid arrows show semantic input/output direction between logical roles; no arrow transfers state, policy, resource, or business authority.

These are logical roles, not necessarily separate processes, classes, or heap objects.

- **Interaction Hemisphere** parses, validates, bounds, and encodes channel-specific data; it authenticates actor/origin only when that identity affects the decision. It cannot mutate canonical state or create business effects.
- **Protocol Nucleus** owns the Ball's `SovereignState` and is the only place that makes a local business decision. It performs no I/O and reads no ambient clock, random source, environment, service locator, or platform SDK.
- **Resource Hemisphere** executes a present private `Effect` through the minimum capability. It adds a safe sink at an interpreter edge and validates/maps a response to `Fact` only for a result-producing path; it does not make a new business decision.

The diagram shows a mutating resource cycle. A `Query` is separate and never mutates state or creates a `Decision`/`SemanticOutput`. It carries a consistency stamp when it crosses an authority or time boundary, can be cached or compared, proves status, aggregates sources, or makes a consistency claim; a same-stack getter may use call-scope snapshot identity.

## The decision and acceptance model

The canonical snapshot transition shape is:

```text
decide(
    state: State,
    pulse: Pulse,
    context: DecisionContext
) -> Accepted(Decision { nextState, boundedOutputs })
   | Rejected(BusinessRejection)
```

`Pulse` is the following exact ordered closed union:

```text
Pulse =
    Intent
  | Fact
  | ModuleCommandPulse
  | ModuleResultPulse
  | ObservedSignal
  | ControlPulse
```

In snapshot form, a successful `Decision` contains the complete next state and this exact ordered, bounded union of canonical `SemanticOutput` envelopes:

```text
SemanticOutput =
    ProjectionOutput
  | ReplyOutput
  | EffectRequest
  | ModuleCommandRequest
  | ModuleResultOutput
  | SignalPublication
  | TimerRequest
```

Bare `ModuleResult` is not a `Pulse`; `SignalOutput` does not exist; and `TimerFired` is not a top-level `Pulse` but may remain a declared timer `ControlPulse`. `ObservedSignal`, `ProjectionOutput`, and `SignalPublication` remain first-class protocol variants.

In `EventJournal` form, `Accepted(EventDecision)` contains either `EventMutation { events, outputs }` or `NoDomainChange { outputs }`; it does not return an independent `nextState`. The shared invariant is atomic acceptance of the snapshot state mutation or event decision together with the complete source-output batch.

```mermaid
flowchart LR
    S["Committed State"] --> D["pure bounded decide"]
    P["current Pulse"] --> D
    C["field-minimized DecisionContext"] --> D
    D -->|"Accepted"| F["state mutation / event decision<br/>+ complete bounded output batch"]
    D -->|"Rejected"| B["BusinessRejection<br/>no accepted Decision"]
    F --> G["preflight + atomic acceptance"]
    G -->|"accepted"| K["publish / commit state or events<br/>and all source outputs"]
    G -->|"not accepted"| Z["no partial state<br/>no output dispatch"]
    K --> O["dispatch SemanticOutputs by kind"]
    O --> L["Projection / Reply<br/>delivery only"]
    O --> W["Effect / Command / declared observation path"]
    W --> V["validated completion / observation<br/>with causal identity"]
    V -. "matching Pulse at its owning Ball" .-> P
```

**Arrow legend.** Solid arrows show the current transition, acceptance, and post-acceptance dispatch; the dotted arrow is a later verified matching `Pulse`, never rollback or a reentrant nested transition.

The ordering is deliberate when outputs exist: **accept or commit first, dispatch second**. A crash or fault before acceptance cannot expose partial state or a partial accepted-output batch. For a present fallible delivery path, a fault after acceptance does not roll the Decision back and activates only its applicable retention, retry, status, or unknown-outcome contract.

Only outputs with a declared completion or observation path can later create a matching `Pulse` at the source or declared consumer. `ProjectionOutput` and `ReplyOutput` are delivery paths, not implicit result Pulses.

### Inter-Ball command/result bridge

`ModuleCommand` and `ModuleResult` are payload types owned by the target contract. Their canonical bridge is:

1. the source accepts `ModuleCommandRequest` in its Decision;
2. the trusted target boundary verifies the accepted source frame, protocol identity, ownership, bounds, and provenance before constructing `ModuleCommandPulse`;
3. target `decide` is the sole target acceptance point and can create `ModuleResultOutput` only inside an accepted target Decision;
4. the verified result route derives `resultSource` from that accepted target frame and constructs `ModuleResultPulse` for the source while preserving the accepted `commandSource`.

Assembly selects the route, effective protocol/version pair, and ingress/return bindings and transports only verified values. It cannot synthesize or modify `commandSource`, `resultSource`, a command/result payload, refusal classification or reason, or any other business meaning; generated same-stack wiring must prove the same accepted source and target tuples.

Before target acceptance, `CommandRejectedBeforeAcceptance` may carry only a verified `ValidationFailure`, `AdmissionFailure`, or `DecisionRejected(BusinessRejection)`. It is neither `ReplyOutput` nor `ModuleResult`, creates no accepted target Decision/revision/output, and reaches source state only through a typed mechanical `ControlPulse`; at the source it means `RejectedBeforeAcceptance + NotExpected`, not an accepted rejected outcome. A refusal that creates a target-owned record, replay result, status/reconciliation fact, Resource action, output, or other accepted-operation evidence is instead an accepted `ModuleResultOutput`; at the source its verified result can mean `Accepted + Rejected`. This classification is versioned target-contract meaning, and a later failure never downgrades an accepted result to the carrier.

A read-like command deliberately pays target acceptance and revision cost when its caller needs a provenance-bound accepted result, stable command/step identity, idempotent replay, status, or reconciliation. Otherwise it is a `ReadDependency`/`Query`, which creates no target Decision, revision, semantic output, or accepted operation record.

If a target crashes after accepting `ModuleResultOutput` but before dispatch, the result remains an accepted target fact under the selected profile. Result-route exhaustion creates target `DispatchStopped` keyed by `(effectiveProtocolIdentity, commandSource, resultSource)`; it does not fabricate source receipt or change a source business/status facet. The source remains at its already proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` state until a verified result Pulse, separate ACK, or declared reconciliation evidence arrives.

## How an application is composed

Pokeball distinguishes four application component roles:

| Role | Owns | Does not own |
|---|---|---|
| `Feature Ball` | A local business capability and its state authority | Another feature's internals or mutable state |
| `Flow Ball` | A specific multi-participant coordination lifecycle and terminal outcome | Participant domain facts or a global workflow engine |
| `Read Model Ball` | Derived query state, source positions, freshness, and rebuild policy | Command authority for source facts |
| Utility package | Pure stateless mechanics | State, protocol, lifecycle, or resource authority |

```mermaid
flowchart TB
    UI["External channels"] --> F1["Feature Ball<br/>local capability + state authority"]
    F1 -->|"accepted ModuleCommandRequest<br/>→ verified ModuleCommandPulse"| F2["Feature Ball<br/>target-owned operation"]
    F2 -->|"accepted ModuleResultOutput<br/>→ verified ModuleResultPulse"| F1
    F2 -->|"accepted SignalPublication<br/>→ verified ObservedSignal"| RM["Read Model Ball<br/>derived query state"]

    FL["Flow Ball<br/>coordination state + terminal outcome"]
    FL -->|"accepted ModuleCommandRequest<br/>→ verified ModuleCommandPulse"| F1
    F1 -->|"accepted ModuleResultOutput<br/>→ verified ModuleResultPulse"| FL
    FL -->|"accepted ModuleCommandRequest<br/>→ verified ModuleCommandPulse"| F2
    F2 -->|"accepted ModuleResultOutput<br/>→ verified ModuleResultPulse"| FL
    FL -->|"accepted ModuleCommandRequest<br/>→ verified ModuleCommandPulse"| F3["Feature Ball<br/>participant-owned invariants"]
    F3 -->|"accepted ModuleResultOutput<br/>→ verified ModuleResultPulse"| FL

    AS["Assembly<br/>routes + effective versions + bindings"]
    AS -.-> F1
    AS -.-> F2
    AS -.-> F3
    AS -.-> RM
    AS -.-> FL
```

**Arrow legend.** Solid arrows show protocol direction: an accepted source output becomes a verified later target/source Pulse. Dashed arrows show static Assembly route, effective-version, and binding selection—not execution, business authority, or a `Direct Control Dependency` edge by themselves.

All inter-Ball dependencies are explicit and bounded. Each has an exact effective protocol identity; explicit versions appear when the endpoints can version or deploy independently. `Assembly` declares route/version/binding only; it is not a runtime mediator, payload constructor, refusal classifier, or business authority.

**Material coordination** exists when one authority must own any independent workflow lifecycle, semantic ordering or branch/join, compensation or recovery, cancellation, reconciliation, or terminal outcome across participant authorities. One such property is sufficient when it makes the simple one-hop conditions false. Call count, sequential syntax, or one command round trip alone is not material coordination and does not require a `Flow Ball`.

### Reads, Draining, and status authority

A cross-authority `ReadDependency` resolves exactly one target authority, target-owned `Query -> ResultPayload` mapping and effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Caller and Assembly do not redefine the target result, stamp, status fact, or read meaning. Multiple reads remain independent and do not promise one atomic multi-source snapshot without a separate declared mechanism.

`Draining` rejects new logical mutations but serves every available declared `Query` and status Query from its committed authority. If the read authority is unavailable, the trusted boundary can return an existing validation/admission response before `read`; once admitted, `read(...) -> ReadResult` remains successful and creates no Decision.

Each status namespace has exactly one committed, revisioned, single-writer query authority without taking command authority over the underlying business facts. Co-location can reduce lag and transaction boundaries; a separate status/Read Model authority can isolate query load and combine declared sources but pays materialization lag and source-position evidence. Neither choice permits two independently writable status answers.

### Security, context, and foundation

When actor, tenant, issuer, realm, assurance, or delegation can change a `Decision` or `Query`/status-read authorization or result selection, the trusted binding boundary constructs verified, bounded, field-minimized context against an approved issuer or equivalent fixed trusted same-stack proof. Forged, tampered, wrong-issuer/realm, missing, stale, or otherwise unverifiable required evidence fails closed. Actor-independent Decisions and reads create no actor-context, issuer, authentication, or actor-evidence artifacts.

The Nucleus Policy Gate alone decides business permission from committed State, the current cause or Query, and trusted context. Immediately before authoritative execution, the Resource/target Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding without making a new business decision. A post-acceptance gate failure remains a declared Resource/result/status path; it cannot roll back or downgrade accepted work or become a pre-acceptance carrier.

Shared foundation code contains mechanical primitives only. It owns no mutable business meaning, business-policy decision, domain or status authority, route selection, service locator, or hidden communication state.

## Core rules developers must preserve

The Core specification has 44 stable law identifiers. Their §20 statements and §20.1 matrix are projection indexes back to the marked body source clauses, not independent authority. The groups below are also an orientation map and do not require 44 local artifacts.

| Area | Developer-facing rule | Laws |
|---|---|---|
| Boundary and decision | Keep Interaction, Nucleus, and Resources logically separate. The Nucleus is pure, explicit, bounded, and the only source of semantic actions. Protocols are closed. | `PBA-01–06` |
| Acceptance and faults | Accept state and the full output batch atomically; dispatch only afterward; do not re-enter a transition; never expose partial acceptance. | `PBA-07–10` |
| State and identity | One mutable fact has one authority and one writer; state kinds remain distinct; detached or independently observable work uses stable semantic identity. | `PBA-11–18` |
| Async and delivery | When those paths exist, separate ACK/result and model ambiguity, idempotency, cancellation races, and retry ownership. | `PBA-19–24` |
| Composition | For actual inter-Ball edges and stateful coordination, declare dependencies/owners and bound routes and fan-out. | `PBA-25–30` |
| Security and resources | Apply quarantine, actor-context authenticity, capabilities, gates, safe sinks, secret, and unsafe controls at the trust/resource/risk edges that exist; ambient authority is always prohibited. | `PBA-31–37, PBA-44` |
| Cost and claims | Keep every present variable dimension finite; reuse exact policies; require binding evidence only for a concrete claim. | `PBA-38–43` |

See [Core sections 18 and 20](spec/pokeball-architecture-core.md) for the practical implementation checklist and the exact laws.

## Profiles: pay only for the guarantees you use

Profiles are independent dimensions. Every selection preserves the always-applicable Core semantics and cannot waive a guardrail activated by its actual paths, risks, or claims.

A project or binding may select an exact default profile policy once; a Ball records only a permitted override.

| Dimension | Choices | What changes |
|---|---|---|
| Execution | `Inline` / `BoundedConcurrent` | Direct run-to-completion versus bounded mailbox and workers, while retaining a single writer |
| State | `Transient` / `SnapshotOutbox` / `EventJournal` | Process-memory lifetime versus durable source state/output records or event commits |
| Isolation | `InProcess` / `Isolated` | Logical in-process boundary versus process or sandbox containment with bounded IPC |
| Security | `Standard` / `Hardened` | Base discipline versus stronger actor, grant, capability, secret, and abuse-control requirements |
| Composition | `Static` | Explicit compile-time or generated wiring; no mandatory runtime registry |

Typical starting points from the Core:

- Local UI state machine: `Inline + Transient + InProcess + Standard`.
- Asynchronous mobile feature: `BoundedConcurrent + Transient/SnapshotOutbox + InProcess`.
- Durable backend aggregate: `BoundedConcurrent + SnapshotOutbox + InProcess + Hardened`.
- Hostile plugin or parser: `BoundedConcurrent + Transient/SnapshotOutbox + Isolated + Hardened`.

## Guarantee boundaries

Pokeball makes guarantees explicit, but it does not turn a label into proof:

- A timeout does not prove that an external action failed or never happened; use `OutcomeUnknown` and reconciliation where required.
- Cancellation is a protocol and a race, not an instant physical stop.
- For a conforming, evidenced `SnapshotOutbox` binding, the guarantee ends at durable source acceptance and retained source output/status within its stated contract. It does not by itself prove target receipt, target acceptance, business success, exactly-once execution, or unconditional eventual delivery.
- A `Flow Ball` coordinates participants; it does not create a distributed ACID transaction. Compensation is new fallible work, not time reversal.
- A multi-source read is not an atomic snapshot without a mechanism that provides one.
- `Hardened`, performance, zero-allocation, durability, and security claims require evidence from a concrete binding and threat/failure model.

Stronger durable runtime, distributed delivery, full replay, secure isolation, subscription, conformance, and dynamic-extension protocols are deliberately left to future extension specifications.

## A typical project layout

Physical folders are not normative, but the Core recommends a shape that makes dependency direction visible:

```text
features/
  catalog/
    interaction/
    nucleus/
      protocol/
      state/
      transition/
      policy/
    resources/
    ball.yaml            # optional/generated resolved view

flows/
  checkout/
    interaction/
    nucleus/
      protocol/
      state/
      transition/
    ball.yaml            # optional/generated resolved view

application/
  assembly/
  runtime/
  observability/

foundation/
  bounded/
  security/
  time/
  tracing/
```

`ball.yaml` is optional: typed source may be authoritative, while tools may generate a fully resolved view for deployment, review, or a conformance claim.

The important part is the direction of authority and dependencies: Interaction adapts external channels, Resources adapt external systems, the Nucleus depends only on its own state and protocols plus mechanical foundation, and Assembly depends only on public protocols and explicit route declarations. Feature internals and mutable state never become another feature's dependency.

## Adopt Pokeball

Pokeball is not automatically the right choice. Use an ordinary utility, module, or adapter—or stop the pilot—when the candidate is a stateless library/algorithm, presentation-only focus/scroll/animation state, a passive adapter with no owned semantic Decision, a project unwilling to establish one writer/authority/closed bounded paths, or a slice whose measured design/runtime cost exceeds its authority, invariant, recovery, or change-radius benefit.

During active adoption or pilot work, the project architecture owner `SHOULD` record or otherwise prove the boundary's semantic facts, state key, strict invariants, dependencies, trust/lifecycle/recovery owners, expected size/load, and public protocol surface. This is project-owned guidance only—not a universal Core artifact, runtime record, or conformance placeholder. Outside adoption/pilot work, no worksheet or empty fields are required.

For a suitable candidate, start with one vertical slice rather than redesigning the whole application:

1. Assign one authority to each mutable semantic fact.
2. Choose the Ball boundary; materialize `StateKey` only when there can be multiple instances or identity crosses call scope.
3. Define only the used closed inputs, state, and outputs.
4. Implement pure bounded `decide` before introducing a framework or DSL.
5. Inventory reachable paths, risks, selected profiles, and intended claims.
6. Resolve each applicable guardrail once through construction, a local declaration, or an exact project/binding policy plus permitted delta.
7. Add adapters, revisions, handles, stale-result, retry, cancellation, status, security, and durability only when first triggered.
8. Test base semantics, triggered paths, and local deltas; test shared mechanisms once at their scope.
9. Add dependencies and Assembly routes only for actual inter-Ball edges.
10. Create evidence only for claims actually made; generate a fully resolved contract view only when deployment or conformance review needs it.

For agent-assisted adoption in another repository, start with the [Agent Pack](docs/agents/README.md). It can guide ordinary design without a fully populated overlay. Its [installation guide](docs/agents/INSTALL.md) explains how to select exact reusable project policies and Ball deltas; an accepted resolved project contract and claim evidence are required before making a Pokeball conformance claim.

## Documentation

### Where to start

- **Learn the architecture:** read this overview, then Core §§0–3 for scope and semantics and §§4–10 for boundaries, state, protocols, and composition.
- **See complete flows:** study the Catalog and Checkout walkthroughs in Core §§15–16.
- **Apply Pokeball:** use the [Agent Pack](docs/agents/README.md) for design and review guidance, and follow its [installation guide](docs/agents/INSTALL.md) when bringing it into another repository.

### Minimal Core reading path

1. §§0.1–0.2 — source-clause authority, applicability, absence, and exact reuse.
2. §§3–5 — canonical model, falsifiable Ball boundaries, and logical roles.
3. §§6–8 — closed protocols, state authority, pure decisions, reads, and acceptance.
4. Read only the triggered parts of §§9–13 for async delivery, composition, security, profiles, and bounds.
5. Use §14 only when materializing a manifest or Assembly view; use §§15–16 as worked examples, §§17–18 for evidence/checks, and §20 only as a projection index back to source clauses.

### Main documents

| Document | What it contains |
|---|---|
| [Core specification](spec/pokeball-architecture-core.md) | Complete Core specification, examples, laws, checklist, and glossary |
| [Agent Pack](docs/agents/README.md) | Practical guidance and runbooks derived from the Core for applying Pokeball in another project |
| [Russian overview](docs/ru/README.md) | Russian-language introduction to the architecture |
| [License](LICENSE) and [notice](NOTICE.md) | License terms, authorship, scope, and reusable attribution |

## License and authorship

Copyright © 2026 **Vladislav Tomilov (4wl2d)**. `4wl2d` is his public
pseudonym. The original specification, documentation,
diagrams, examples, and Agent Pack are licensed under
[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/)
(`CC-BY-4.0`). Anyone may share and adapt those materials for any purpose,
including commercial use, subject to CC BY 4.0: retain the supplied creator,
copyright, license, and warranty-disclaimer notices; include the license text
or URL; link the source to the extent reasonably practicable; and indicate
changes while retaining prior change notices. No ShareAlike condition applies.

This licenses the copyrightable expression of Pokeball Architecture; it does
not create exclusive copyright ownership in abstract ideas, methods, systems,
or functional concepts. CC BY 4.0 does not grant patent or trademark rights.
See [`NOTICE.md`](NOTICE.md) for the exact scope and recommended attribution,
and [`LICENSE`](LICENSE) for the complete legal code.
