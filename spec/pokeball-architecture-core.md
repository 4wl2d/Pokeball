# Pokeball Architecture — Core Specification

**Version:** `1.4.0-draft`<br>
**Status:** canonical draft<br>
**Date:** 2026-07-20<br>
**Language:** English; exact protocol type and field names use Latin identifiers

---

## Contents

```text
0.  Document status and scope
1.  Definition of Pokeball
2.  Goals and non-goals
3.  Canonical model
4.  Choosing a Ball boundary
5.  Three logical zones
6.  Protocol algebra
7.  State and authority
8.  Decision and commit semantics
9.  Asynchrony, causality, and delivery semantics
10. System composition
11. Security and privacy
12. Execution profiles
13. Limits, budgets, and performance
14. Minimal manifest and source-code organization
15. End-to-end example I: catalog search
16. End-to-end example II: Checkout Flow
17. Testing, review, and operational verification
18. Practical checklist
19. Anti-patterns
20. Canonical Pokeball laws
21. Adoption strategy
22. Glossary
23. Canonical statement
```

Suggested first reading path:

```text
0–3   -> meaning and semantic foundation
4–10  -> Ball, state, protocol, and composition design
12    -> runtime profile selection
15–16 -> complete operational examples
18–20 -> checklist, anti-patterns, and canonical laws
```

Sections 11, 13, 14, 17, 21, and 22 serve as implementation and review references.

---

## 0. Document status and scope

This document is the sole normative text for understanding and implementing the Pokeball Architecture baseline. It describes the architectural core, the minimum executable semantics, and profile-specific extensions only to the extent required to design a real application.

> `canonical draft` is the current working truth rather than a stable compatibility promise: `1.4.0-draft` retains the closed `Pulse` and `SemanticOutput` unions introduced by `1.3.0-draft` and adds exact causal-reservation, composition-view, and decision-work-meter semantics. Consumers re-resolve bindings against this exact declared baseline; no compatibility alias changes the ordered protocol algebra.

This document is intentionally not a specification for a cloud platform, message broker, workflow engine, IAM system, disaster-recovery framework, or conformance-certification service.

The baseline scope includes:

- the `Ball` boundary and three logical zones;
- state ownership and authority scope;
- closed protocols of causes, decisions, and consequences;
- a pure bounded decision;
- atomic decision acceptance and the commit-before-dispatch rule;
- single-writer semantics;
- causality, idempotency, cancellation, and `OutcomeUnknown`;
- explicit inter-module composition;
- a minimal actor, grant, and capability model;
- finite resource limits;
- independent profile dimensions: execution, state durability, isolation, and security;
- two end-to-end examples and a practical checklist.

The following separate future specifications remain outside this document:

```text
Durable Runtime and Recovery
Distributed Delivery and Executors
Event Sourcing and Replay
Secure Isolation and Audit
Read Models and Subscriptions
Tooling, Schema and Conformance
Dynamic Extensions and Ownership Transfer
```

The absence of these extension specifications does not prevent use of the baseline architecture. It only prohibits attributing stronger guarantees to this document when those guarantees require a separate formal protocol.

### 0.1. Normative words

- **MUST** — a mandatory rule when the applicability trigger owned by its marked primary source clause is true within the stated scope.
- **MUST NOT** — a prohibited construction.
- **SHOULD** — a recommended rule; deviation requires a deliberate rationale.
- **MAY** — a permitted option.

A primary source clause is the visible `pkb:pba-source` record marked `Source clause for PBA-xx` in the numbered body. That record is the sole normative authority for the corresponding law. Its eight fields state the Rule, Applicability, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route exactly once. Every record incorporates the resolution, reuse, absence, and invalid-reference rules of §0.2 unless its Rule states a stricter local rule.

Within that compact record, `Declaration owner` names the accountable semantic or architectural owner. `Enforcement / evidence owner` names the enforcing role when one exists, the owned static or behavioral evidence route when enforcement is proved by evidence, or both; the slash does not require a second owner or duplicate artifact. The separate `Primary verification route` supplies the navigation target. A field that names fixtures or a scan therefore does not transfer architectural ownership from the declaration owner.

Section §20 is the complete set-equal human-readable projection of those records. Section §20.1 is deliberately narrower: it is a generated navigation and ownership index, not an independently complete law statement. Tests in §17, the checklist in §18, and examples in §§15–16 are verification or teaching projections and cannot complete a rule. Each definition marked `pkb:term` in §§0–21 is the sole definition source for its term; §22 is its exact generated lookup projection. If any projection differs from its source record, the source record controls. A projection may repeat `MUST` or `MUST NOT` only when its complete tuple is set-equal (§22).

The comments delimiting source and generated regions are maintenance structure inside this one canonical document. The rendered Core is complete without executing a generator. Generation and linting are not runtime mechanisms, project implementation requirements, certification, or conformance evidence; they prove only structural identity between the visible source records and their designated projections.

The following is an authoritative Core-documentation maintenance rule rather than a PBA law: the maintainer of this canonical Core owns every diagram introduced or changed in this document. Every new or changed Mermaid diagram in Core carries a local explicit legend that distinguishes semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows. If one of those distinctions is absent, the documentation change is not eligible for acceptance or publication until corrected. This rule does not make a consuming project's diagram or binding non-conforming unless that project's accepted policy separately adopts it; §17.5 and §18.1 are evidence projections of this exact Core-only rule.

Compact projection rule map for every major numbered section that owns at least one PBA source record:

<!-- pkb:generated:start id="core-pba-map" -->
<!-- pkb:inventory pba-sha256="f4f6418428077fe0f6f80bff01c18675ec545ccce36d457f24410ecc7ee9678d" term-sha256="17c0545dfc313792e8825b8fe3e72535a175d36a33652e81229c23472a4eb91e" -->
| Major section | Core-embedded PBA source records |
|---:|---|
| §0 | `PBA-39` |
| §3 | `PBA-15`, `PBA-16` |
| §5 | `PBA-01`, `PBA-02`, `PBA-03`, `PBA-06` |
| §6 | `PBA-04` |
| §7 | `PBA-11`, `PBA-12`, `PBA-13`, `PBA-14` |
| §8 | `PBA-05`, `PBA-07`, `PBA-08`, `PBA-09`, `PBA-10`, `PBA-30`, `PBA-38` |
| §9 | `PBA-17`, `PBA-18`, `PBA-19`, `PBA-20`, `PBA-21`, `PBA-22`, `PBA-23`, `PBA-24`, `PBA-42` |
| §10 | `PBA-25`, `PBA-26`, `PBA-27`, `PBA-28`, `PBA-29` |
| §11 | `PBA-31`, `PBA-32`, `PBA-33`, `PBA-34`, `PBA-35`, `PBA-36`, `PBA-37`, `PBA-44` |
| §13 | `PBA-40`, `PBA-41` |
| §14 | `PBA-43` |
<!-- pkb:generated:end -->

### 0.2. Proportionality principle

A simple local `Ball` does not require a durable outbox, broker manifest, audit sink, transaction coordinator, operation-status algebra, or set of signed evidence artifacts. A guardrail appears only when its normative applicability condition is true.

> **The strength of the mechanism must be proportional to the strength of the guarantee.**

<!-- pkb:term:start name="TriggerAbsenceProof" -->
**TriggerAbsenceProof** — the closed static evidence record materialized only when a Pokeball conformance/release claim or accepted ambiguity-resolution decision relies on absence of a `path-triggered`, `risk-triggered`, or `claim-triggered` predicate. It binds the exact anchor, scope/profile, inventory and revisions/digests, evaluated predicate, `Absent` conclusion, evidence owner, and invalidation conditions; it cannot negate `always` or a present trigger/claim and becomes unusable until reevaluated after invalidation.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-39" title="Profile-Proportional Mechanism" -->
**Source clause for PBA-39 — Profile-Proportional Mechanism.**

- **Rule:**
  - Every guardrail uses exactly one of the four closed applicability classes below.
  - A present trigger resolves statically to one effective guardrail.
  - An absent `path-triggered`, `risk-triggered`, or `claim-triggered` predicate removes its mechanism and ceremony only under the inventory and evidence rules in this subsection.
  - An `always` obligation is never absent.
  - A boundary/adoption worksheet is project-owned `SHOULD` guidance only while adoption or pilot work is active.
  - It is not a universal Core artifact or conformance placeholder.

  Core uses four closed applicability classes:

  | Class | A guardrail applies when |
  |---|---|
  | `always` | the rule is an invariant of every conforming Ball or binding in its stated scope; |
  | `path-triggered` | the closed protocol, type graph, selected profile, Assembly, or reachable control flow contains the named path; |
  | `risk-triggered` | the architecture contains the named trust, failure, concurrency, irreversibility, secrecy, abuse, or recovery condition; |
  | `claim-triggered` | a concrete binding or distribution makes the named performance, durability, delivery, isolation, security, or conformance claim. |

  Applicability and declaration location are different questions. An applicable guardrail MUST resolve statically to exactly one effective mechanism, value, or evidence contract through one of these forms:

  1. construction or a bounded type proves the invariant directly;
  2. the Ball declares the guardrail locally;
  3. an accepted project, profile, Assembly, or binding scope selects one exact reusable declaration covering the Ball and, where that declaration permits it, the Ball supplies only a field-specific local delta or a different explicit selection.

  The enclosing scope owns that single reference; a covered Ball does not repeat it merely to acknowledge the default. A Ball-local reference is needed only when selection is local rather than already fixed by the enclosing scope.

  A reusable declaration is immutable at its referenced revision and identifies:

  ```text
  owner
  policyId + revision + content digest
  covered guardrail IDs
  exact project/Ball/profile/Assembly/binding/environment scope
  effective values or mechanisms
  allowed override fields, if any
  enforcement owner
  evidence references and their scope, if evidence is required
  review or expiry condition, if one exists
  ```

  Resolution follows the closed reference graph before implementation or review. A missing, mutable, stale, cyclic, conflicting, wrong-version, wrong-profile, wrong-binding, or wrong-environment reference is invalid. A local delta is valid only for a field explicitly made overridable by the referenced declaration and produces one unique effective value. Reuse never creates a runtime registry, service locator, ambient default, or hidden inheritance.

  An existing complete local declaration remains valid. Replacing it with a reference is semantics-preserving only when the resolved mechanism, values, failure behavior, guarantee scope, and evidence scope are equal; migration may be incremental Ball by Ball.

  When the closed inventory proves a trigger absent, the corresponding mechanism, evidence, empty table, zero-valued placeholder, and `not applicable` row are omitted. If absence cannot be proved, the trigger is treated as present unless an explicit accepted decision closes the ambiguity. Missing evidence for a claim prohibits the claim; it does not permit an unprotected triggered path.

  When a Pokeball conformance or release claim, or an accepted ambiguity-resolution decision, relies on trigger absence, that conclusion is materialized in exactly this closed static evidence shape:

  ```text
  TriggerAbsenceProof {
      triggerClass:
          path-triggered
        | risk-triggered
        | claim-triggered
      triggerAnchor
      exactScopeAndEffectiveProfile
      inventoryEvidence:
          PathInventoryRef
        | RiskInventoryRef
        | DenialByConstructionRef
        | ClaimPublicationInventoryRef
      inventoryRevisionsOrDigests
      evaluatedPredicate
      conclusion: Absent
      evidenceOwner
      invalidationConditions
  }
  ```

  An `always` obligation cannot use `TriggerAbsenceProof`. A present trigger predicate invalidates a contrary proof and requires its guardrail to resolve; in particular, a published present claim cannot be negated by a claim-trigger absence proof. An unaccepted ambiguity interpretation cannot establish absence. Ordinary design, implementation, and adoption work materializes no proof or placeholder merely because a category is absent.

  The proof is bound to its exact trigger anchor, scope, effective profile, referenced inventory, and inventory revisions or digests. A listed invalidation condition, scope/profile/version/inventory change, missing or stale digest, unresolved reference, or conflicting evidence invalidates it and requires reevaluation before the dependent conformance/release claim or accepted decision may continue to rely on absence. `evidenceOwner` owns both the proof and its invalidation review. This is static review evidence, not a runtime registry, ambient default, hidden inheritance path, waiver, or authority to broaden a reusable declaration's scope.
- **Applicability:** `A`: every applicability decision; `P/R/C`: a verdict/decision relies on absence; `P`: adoption/pilot work requests boundary guidance.
- **Declaration owner:** Ball/project architecture owner; proof names evidence owner; project owns any worksheet.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Resolver/proof contradiction/invalidation plus worksheet-present/absent and negative-adoption fixtures.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** `always`/present predicates cannot use proof; exact-scope reuse only; worksheet is `SHOULD` during adoption/pilot and absent otherwise.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->
### 0.3. Waivers and conformance effect

A waiver is a reviewable record of a deliberate deviation. It does not cancel an applicability trigger, change the meaning or force of a normative word, override a law, or satisfy missing evidence. Compensating controls may reduce risk, but they do not make a violated `MUST` or `MUST NOT` conforming.

When a project records a waiver, the closed record has exactly these eight top-level fields:

```text
WaiverRecord {
    owner
    approvedBy
    governingAnchor
    exactScope
    reason
    constraintsAndCompensatingControls
    testsAndEvidence
    review {
        expiryOrReviewAt
        remediation
        conformanceEffect
    }
}
```

- `governingAnchor` identifies the exact marked Core source clause, accepted extension, or project rule from which the scope deviates.
- A `PBA-*` ID is usable only as the stable projection identifier that resolves to its unique marked Core source clause.
- `approvedBy` is the authority permitted to accept the project risk.
- Approval does not create authority over Core.
- `conformanceEffect` states the consequence literally:
  - A deviation from a `SHOULD` may remain conforming when the required deliberate rationale exists.
  - A violation of `MUST` or `MUST NOT` blocks a Pokeball conformance claim for `exactScope` until remediation restores compliance.
- Tests and evidence are immutable or version-pinned within their stated scope.

A waiver for a compile-time import or `Direct Control Dependency` cycle therefore records deliberate non-conformance. It cannot make that graph conforming. A guardrail policy reference, unsafe-site record, claim record, or project overlay is likewise never an implicit waiver.

### 0.4. Practical decision and change routes

The table below routes the ten recurring architecture questions to their controlling sections and primary verification. It intentionally does not restate their rules.

| Decision question | Authoritative sections | Primary verification route |
|---|---|---|
| 1. Where does a new responsibility belong? | §§4.4–4.5, 5, 10.1–10.5, 21.3 | §§17.5, 18.1–18.2 |
| 2. What may it depend on? | §§5.5, 10.2, 10.6–10.11, 14.5 | §§17.5, 18.2 |
| 3. Who defines the required contract? | §§6.1–6.14, 10.2, 10.7, 14.1, 14.4 | §§17.4–17.5, 18.2 |
| 4. What data may cross a boundary? | §§5.1–5.3, 6, 7.5–7.6, 10.2, 11.1–11.3 | §§17.4–17.5, 18.1–18.2 |
| 5. Where does representation transformation occur? | §§5.1–5.3, 6.11, 10.7, 11.1 | §§17.3–17.5, 18.1–18.2 |
| 6. Where is an external action performed? | §§5.2–5.3, 6.7, 8.3–8.8, 11.3–11.6 | §§17.1, 17.3, 17.8, 18.1 |
| 7. How does an error travel? | §§6.13, 8.5–8.8, 9.3–9.5 | §§17.6–17.8, 18.1 |
| 8. How is an implementation replaced? | §§5.3, 10.7, 12–14 | §§17.3, 17.5, 17.9, 18 |
| 9. How is an exception recorded? | §§0.2–0.3, 11.10, 21.6 | §§17.5, 17.8, 18.1 |
| 10. How is conformance checked? | §§0.1–0.3, 17–18, 20–20.1 | §§17.5, 18 |

The following change-impact routes state who decides, what must be revisited, and which boundary remains stable unless that change explicitly alters its semantics.

| Change scenario | Decision owner | Revisit | Remains stable unless explicitly changed |
|---|---|---|---|
| Change an internal rule | Owning Ball/Nucleus | State invariants, affected Pulse/Context fields, Decision outputs, transition and bound tests (§§7–8, 17.1) | Interaction/Resource implementations and public Application Surfaces. |
| Change an external interaction | Ball protocol owner plus Interaction/binding owner | Closed ingress/read/response types, validation, Trusted Boundary construction, encoding, contract tests (§§5.1, 6, 11.1, 17.4) | Nucleus State/Decision and Resource contract when semantic inputs/outputs are unchanged. |
| Add a new way to start an operation | Ball protocol owner | New Interaction adapter and its mapping to an existing or deliberately new `Intent`/`Query`, bounds and tests (§§5.1, 6.1–6.3, 17.4) | Existing Nucleus/Resource paths when the semantic protocol is reused exactly. |
| Replace an implementation | Resource, Assembly, or profile-binding owner | Capability, sink, failure mapping, route/binding, profile claim and adapter tests (§§5.3, 10.7, 11, 12, 17.3) | Ball State, Decision meaning, and producer-owned public contract. |
| Change a data format | Owner of the crossed representation or semantic protocol | Boundary mapping, schema/version/compatibility, retained-output or migration evidence (§§6, 10.11, 17.4, 17.7) | Semantic ownership and authority when the change is representation-only. |
| Add a proposed architectural element | Project architecture owner; extension owner if Core roles cannot express it | §4.4 boundary falsifier, authority/writer, permitted edges, closed protocol, bounds, profile and evidence; §21.6 if an extension is required | Existing one-authority/one-writer, closed-union, no-ambient-authority, and Assembly non-authority guarantees. |

### 0.5. Reader and maintenance surface

Core is a complete normative reference, not one mandatory linear reading assignment. The documentation maintainer owns this reader surface and keeps each route anchored to the authoritative source and its verification path. A stale, broken, or circular route blocks acceptance of the affected documentation change because a reader could no longer determine the controlling text without a whole-document search.

| Reader task | Minimum authoritative route | Views that are alternatives, not cumulative required reading |
|---|---|---|
| Place a responsibility or choose a boundary | §0.4 question 1, §4.4, then only the source laws and falsifiers activated by that choice | The full §20 catalog and unrelated examples. |
| Design or change one Ball/path | The applicable §0.4 question/change row; the owning sections; triggered source laws; their primary §17 route and affected §18 items | Untriggered profiles, absent-path evidence, and both a source record and its complete §20 duplicate. |
| Implement or replace a binding | §0.4 questions 4–8; the selected Interaction/Resource/Assembly/profile sections; applicable tests | Unselected profiles and unrelated Ball examples. |
| Review one claim or conformance scope | §0.2 trigger resolution; applicable source records; the exact tests/evidence and Claim Record for that scope | Empty `N/A` categories and non-applicable mechanisms. |
| Look up a law, owner, or definition | §20.1 for navigation, then either the marked source or its set-equal §20 view; §22 for a generated definition lookup | Reading both source and generated copies merely to acknowledge equality. |
| Compare mutation, read, acceptance, and failure forms | The compact canonical comparison in §3.3, then only the selected detailed route in §§6.3, 8.5, 8.9, or 8.10 | Reconstructing one operation form from unrelated persistence, status, or example sections. |
| Audit all of Core | Every unique authoritative numbered-body statement and marked source record; the complete verification/checklist scope in §§17–18; and the generated-equality gate from §17.5 | Semantically rereading both a marked PBA/term source and its byte-checked generated §20/§22 alternative. |

Maintainers edit each PBA source record and each marked term definition exactly once. Sections 20 and 22, the compact maps, and derived indexes are generated views and have zero manual synchronization obligation; their byte equality is a tooling gate, not additional prose to maintain or a second semantic reading assignment. A full audit still covers every unique architectural source and all applicable evidence, but generated alternatives add structural equality verification rather than duplicate semantic review. Core therefore claims task-routed reading, one semantic pass per unique source, and zero manually maintained normative duplication; it does not claim that exhaustive review of the unique content is trivial. Under §0.2/PBA-39, the presence of a mechanism in Core never justifies using it on an inapplicable path, and reviewers do not credit or require untriggered ceremony merely because the reference documents it.

### Definition source records for §0

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Applicability Trigger" -->
**Applicability Trigger** — a normative condition that activates a guardrail: `always`, a reachable path, a named risk, or a concrete claim. `always` cannot be absent; a proven-absent conditional trigger removes its ceremony, while ambiguity is treated as present under §0.2 unless an accepted decision resolves it.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Effective Guardrail" -->
**Effective Guardrail** — the one mechanism, value, or evidence contract obtained after static applicability and reference resolution through construction, a local declaration, or an exact reusable policy plus permitted delta.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Guardrail Policy Reference" -->
**Guardrail Policy Reference** — an immutable owner/policy/revision/digest reference whose artifact declares exact scope, covered guardrails, mechanisms or values, enforcement/evidence ownership, and permitted overrides. It is resolved statically and is not a runtime registry or ambient default.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Set-Equal" -->
**Set-Equal** — a derived rendering whose complete tuple—Rule and modal force, Applicability and trigger, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route—matches its named primary source record with nothing added, omitted, strengthened, or weakened.
<!-- pkb:term:end -->



---

## 1. Definition of Pokeball

**Pokeball Architecture** is an architecture of statically or explicitly composed application authorities called `Ball`s. Every `Ball`:

1. has an explicit application decision scope;
2. owns its own canonical state;
3. separates external input, pure decision-making, and external effects;
4. interacts through closed typed protocols;
5. executes within finite resource limits;
6. receives no implicit access to the state or authority of other modules.

The architectural unit is called a `Ball`.

A physical source module or package is not automatically an architectural authority. A utility is classified by its owning Ball role or by the mechanical Foundation rule in §§10.1/14.7; it does not create an ownerless fifth kind of application module or protocol dependency.

Every `Ball` contains three logical zones:

```text
Interaction Hemisphere     <- Projection / Reply
        ↓ Intent / Query
Protocol Nucleus
        ↑ Fact / Command Pulse / Result Pulse / Observed Signal / Control
        ↓ Effect / Command / Result / Signal / Timer
Resource Hemisphere and explicit routes
```

`ProjectionOutput` and `ReplyOutput` return to Interaction for presentation or response encoding. `EffectRequest`, `ModuleCommandRequest`, `ModuleResultOutput`, `SignalPublication`, and `TimerRequest` leave through the Resource Hemisphere or an explicit route. A verified command route creates `ModuleCommandPulse` at the target; a verified result route creates `ModuleResultPulse` at the source. These are logical directions; a same-stack binding may use **representation erasure**—omit materialized adapters while proving the same semantic values, ownership, authority, bounds, verification, and accepted tuples (§22).

The basic mutation formula is mode-specific:

```text
State + Pulse + DecisionContext
              ↓
           decide
              ↓
SnapshotDecision = NextState + bounded semantic outputs
            or
EventDecision = nonempty DomainEvents or NoDomainChange
                + bounded semantic outputs
```

The selected state profile chooses exactly one of those mutation forms. A Query instead uses the non-mutating read form in §§3.3 and 6.3.

For a local resource cycle:

```text
External input
  -> Interaction: parse / normalize / authenticate / validate
  -> Intent
  -> Nucleus: decide
  -> commit Decision
  -> Effect
  -> Resource: execute through capability and safe sink
  -> validated Fact
  -> next Nucleus decision
```

Canonical statement:

> **Pokeball separates a validated cause, a local decision, atomic acceptance, and an observable consequence.**

### Definition source records for §1

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Ball" -->
**Ball** — the smallest operationally useful scope of an application decision, state authority, and lifecycle. Core constrains valid authority graphs but may permit more than one decomposition that independently satisfies the same invariants and boundaries.
<!-- pkb:term:end -->

<!-- pkb:term:start name="BallInstance" -->
**BallInstance** — a concrete state-owning and single-writer scope. `BallType + namespace + StateKey` is materialized when identity crosses an instance, persistence, route, status, ownership, or observability boundary; a local singleton may use typed call scope.
<!-- pkb:term:end -->

<!-- pkb:term:start name="BallType" -->
**BallType** — the versioned protocol and decision semantics of a family of instances.
<!-- pkb:term:end -->


---

## 2. Goals and non-goals

### 2.1. Goals

#### Local reasoning

A change inside a `Ball` must be understandable from its state, protocol, transition, and declared dependencies, without reading a global store or searching for hidden handlers.

#### Unambiguous state ownership

Every mutable semantic fact has one authority within a given scope. Copies are permitted only as projections, replicas, captured inputs, or materialized views with explicit provenance.

#### Controlled causality

An external action is the result of an explicit `Nucleus` decision, not an incidental call from a UI, controller, callback, or repository.

#### Bounded coupling

Inter-module relationships are declared, typed, bounded in fan-out and causal depth, and do not create a universal runtime graph.

#### Predictable cost

Inputs, outputs, state, queues, retries, fan-out, concurrency, and external responses have finite limits.

#### Zero mandatory runtime tax

A local `Inline` binding can compile to a direct call without a mandatory mediator, reflection, serialization, queue, thread hop, or structural allocation. This is a design target, not an unsupported universal guarantee.

#### Security by construction

The standard protocol makes it difficult to express raw SQL, arbitrary shell commands, arbitrary URLs, unrestricted filesystem access, or the transfer of secrets through ordinary projections. Actual security still requires correct adapters, credentials, policies, and deployment boundaries.

### 2.2. Non-goals

Pokeball does not claim that:

- every screen, endpoint, table, aggregate, or use case must be a separate `Ball`;
- event sourcing is mandatory;
- CQRS, a broker, an actor runtime, a DI framework, or a particular database is mandatory;
- a typed `Effect` is automatically safe;
- an in-process module is isolated from hostile native code;
- a distributed side effect executes exactly once;
- a Flow turns multiple systems into one ACID transaction;
- every timeout means that the action did not occur;
- every read model is a consistent snapshot;
- one linter can prove semantic ownership, least privilege, or the absence of ambient authority;
- zero allocation exists on every toolchain without a benchmark;
- unlimited architectural ceremony is acceptable merely because runtime overhead is small.

### 2.3. When the approach is especially useful

- a long-lived stateful application;
- a mobile or desktop product with offline operation and asynchronous callbacks;
- a modular monolith with multiple business capabilities;
- a backend with external side effects and retries;
- a workflow with cancellation, compensation, or reconciliation;
- security-sensitive resource access;
- a plugin host or component that requires an isolation profile;
- a system where ownership and change radius matter more than the convenience of a global store.

### 2.4. One sparse Core

Pokeball does not define a second “Lite” architecture. A small prototype, script, stateless transform, or simple CRUD component uses the same Core, but only its always-applicable rules and the guardrails activated by paths, risks, and claims that actually exist.

The minimum Ball preserves:

```text
one state authority
explicit input validation
no Interaction -> Resources business shortcut
pure bounded decision
atomic acceptance
closed used protocol
no ambient global authority
```

An external operation, asynchronous identity, retry, cancellation, status, durability, isolation, grant, or claim-evidence artifact is added when the exact trigger in its source record first becomes true; §20.1 locates that record. Closed type absence proves an unused path; it does not require a zero or `N/A` declaration.

A utility without state, protocol, or resource authority does not become a Ball merely for uniformity. It is either a Ball-local implementation artifact owned by exactly one logical role, or shared mechanical Foundation under PBA-43. Shared domain or business semantics cannot remain in an ownerless utility: they stay local to each owning Ball or are owned by one Ball/Flow and exposed through its declared Application Surface and protocol.

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

`Projection`, `Reply`, `Effect`, `ModuleCommand`, `ModuleResult`, `Signal`, and `TimerIntent` are semantic payload types. `ModuleCommand` and `ModuleResult` are owned by the target contract. The `Nucleus` returns output payloads only inside the canonical output envelopes defined in §6.12. Mechanical delivery records, transport attempts, storage rows, and tracing metadata are not business outputs of the `Nucleus`.

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
**SemanticHandle** — the stable domain-visible identity of planned work that is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible; immediate call-scope output needs only its accepted sequence position.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-15" title="Semantic Handle" -->
**Source clause for PBA-15 — Semantic Handle.**

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

A new `RequestId` must not be used to bypass idempotency, and a new `OperationId` must not be created merely because a transport attempt was repeated. A path that has no retry, detached work, durable record, status, or cross-boundary correlation does not materialize this full lineage.

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

A Fact contains a causal reference that matches it to the accepted `EffectRequest`. An immediate synchronous completion may use accepted call-scope position; a completion that can detach, reorder, retry, cross a boundary, recover, or enter status carries the complete stable handle/source identity from §9.1. A push or subscription observation is a `Fact` only when the subscription itself was previously accepted and the observation returns its stable `SemanticHandle`. An unsolicited resource observation must not masquerade as a `Fact`.

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
Inventory.ReserveItems(...)
Payment.Capture(...)
Notification.SendReceipt(...)
```

A Command is not a private `Effect`. For each command operation, the target owns exactly one closed, versioned `ModuleCommand -> ModuleResult` mapping. The caller imports that mapping through `dependencies.commands`; it neither aliases the command to an external `Intent` nor redeclares either payload as caller-owned.

<!-- pkb:term:start name="ModuleCommandPulse" -->
**ModuleCommandPulse** — the canonical verified target input carrying accepted-source `commandSource`, effective protocol identity, target-owned `ModuleCommand`, and issuer provenance; target `decide` is its sole acceptance point.
<!-- pkb:term:end -->

The trusted target boundary constructs the canonical command ingress only from a verified accepted source frame:

```text
ModuleCommandPulse {
    commandSource: CausalToken
    effectiveProtocolIdentity
    command: ModuleCommand
    issuerProvenance
}
```

`commandSource` is derived from the accepted source `ModuleCommandRequest` frame, including its complete semantic handle and source ordinal. `effectiveProtocolIdentity` resolves the target-owned command/result mapping. The target boundary verifies source acceptance, protocol identity, payload ownership, size, provenance, and every triggered authenticity rule before constructing the Pulse. The target's canonical `decide` is the only acceptance point; a transport receive, adapter call, inbox row, ACK, or route invocation is not target acceptance.

Assembly selects the route, effective protocol/version pair, and binding and transports the verified value. It does not synthesize or modify `commandSource`, the command payload, issuer provenance, or other business meaning. A same-stack binding may erase the materialized adapter or envelope only when the exact accepted source tuple and target input remain provably identical.

### 6.9. ModuleResult

<!-- pkb:term:start name="ModuleResult" -->
**ModuleResult** — a target-owned business-outcome payload for one target command mapping; it is emitted only through `ModuleResultOutput` and received as the payload of `ModuleResultPulse`.
<!-- pkb:term:end -->

```text
InventoryReserved(reservationId)
InventoryReservationRejected(reason)
PaymentCaptureOutcomeUnknown(reference)
```

<!-- pkb:term:start name="ModuleResultOutput" -->
**ModuleResultOutput** — the canonical target `SemanticOutput` created only inside an accepted target Decision, carrying target-frame `sourceOrdinal`, accepted-source `commandSource`, target-owned `ModuleResult`, and `semanticHandle = commandSource.semanticHandle` as correlation without ownership transfer.
<!-- pkb:term:end -->

A target Nucleus creates a result only inside an accepted target Decision:

```text
ModuleResultOutput {
    semanticHandle = commandSource.semanticHandle
    sourceOrdinal
    commandSource: CausalToken
    payload: ModuleResult
}
```

The `sourceOrdinal` is the result output's position in the accepted target frame. Equality of `semanticHandle` with `commandSource.semanticHandle` provides correlation only; it neither transfers target ownership nor re-exports the target payload. The output always counts against the target's `maxOutputsPerDecision` and `maxOutputBytesPerDecision` contracts. When its route is retained, retried, or independently observable, it also occupies one stop-eligible target delivery/status slot and counts against the target's corresponding finite bounds.

<!-- pkb:term:start name="ModuleResultPulse" -->
**ModuleResultPulse** — the canonical verified source input carrying accepted-source `commandSource`, accepted-target `resultSource`, effective protocol identity, target-owned `ModuleResult`, and issuer provenance.
<!-- pkb:term:end -->

The verified result route constructs the source input only from that accepted target frame:

```text
ModuleResultPulse {
    commandSource: CausalToken
    resultSource: CausalToken
    effectiveProtocolIdentity
    result: ModuleResult
    issuerProvenance
}
```

`commandSource` remains the accepted source-command token. `resultSource` is derived from the accepted target frame and identifies the target result output at its target `sourceOrdinal`. The route verifies both accepted tuples, the target-owned payload, effective protocol identity, and issuer provenance. Assembly transports those verified values but does not synthesize or modify either token or the result payload. Same-stack representation erasure must prove the same source and target tuples.

The result-delivery key is exactly the tuple `(effectiveProtocolIdentity, commandSource, resultSource)`; Core introduces no separately named identity type. A target-side `DispatchStopped` for this output uses that complete tuple. It is a target result-delivery fact and does not by itself set a source acceptance, business-outcome, cancellation, or workflow-status facet.

A transport ACK and a `ModuleResultPulse` are different observations. An ACK may prove target acceptance, but not business success. A result Pulse proves target acceptance only through its verified accepted-target provenance.

For a same-identity/fingerprint target-command redelivery after the command frame is accepted but before a result frame exists, the target returns only verified ACK proof of that original accepted frame and pending-result state. It does not construct a provisional `ModuleResult`, wait for completion, or repeat target semantic/Resource work. After result acceptance, redelivery uses the exact existing result-frame proof and preserves `commandSource`, `resultSource`, effective protocol identity, target revision, handle/ordinal, and payload; only the mechanical `AttemptId` changes. Conflicting fingerprint or frame evidence fails closed.

Every versioned target contract statically classifies each reachable refusal as either a pre-acceptance boundary response under §6.13 or an accepted `ModuleResultOutput`. A binding, profile, retry, or runtime condition cannot choose the form dynamically. An accepted result is mandatory when the refusal creates any target-owned state or operation record, idempotent replay outcome, output, Resource action, status/reconciliation visibility, durable claim, or other proof of an accepted operation. A pre-acceptance `DecisionRejected(BusinessRejection)` is legal only when none of those target facts was accepted. An Execution Gate or Resource failure after target acceptance therefore remains an accepted target result path; dispatch failure never downgrades an accepted result to a pre-acceptance response. Carrier evidence and accepted-result evidence for the same effective protocol identity and `commandSource` conflict and fail closed.

> A read-like command is legal and deliberately pays target acceptance cost when its caller requires a provenance-bound accepted result, stable command/step identity, idempotent replay, status, or reconciliation semantics. When no accepted target record is required, the correct construction is `ReadDependency`/`Query`, which creates no target Decision, revision, or semantic output.

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
**ObservedSignal** — a provenance-checked causal input envelope created from a previously accepted `SignalPublication` over a declared signal route.
<!-- pkb:term:end -->

The recipient transforms a previously accepted `SignalPublication` into a causal input envelope:

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

An `ObservedSignal` is accepted only after its provenance and correspondence to a committed `SignalPublication` have been verified. Resource subscription observations use a causally bound `Fact`, not an `ObservedSignal`.

### 6.11. Pulse

<!-- pkb:term:start name="Pulse" -->
**Pulse** — the ordered closed family `Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse`; raw external mutation/cancellation enters only as an `Intent` after Interaction validation and is not a field of `DecisionContext`.
<!-- pkb:term:end -->


<!-- pkb:term:start name="ControlPulse" -->
**ControlPulse** — a declared trusted lifecycle/timer/cancellation/delivery observation from a runtime/resource/route boundary; it is not raw external mutation ingress. If a post-commit mechanical dispatch/ACK or pre-acceptance command-carrier observation changes Sovereign State, it passes as this typed input through single-writer `decide` rather than being written directly by runtime; a business `Fact` or `ModuleResultPulse` is not reclassified. A same-stack carrier-handling Decision consumes the transferred alternative-completion slot and creates no new causal root.
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

On the same-stack pre-acceptance command branch, the source `decide(ControlPulse)` that applies this carrier consumes the transferred level-1 alternative-completion reservation defined in §8.4. The boundary creates neither a fresh causal root nor an unreserved completion slot.

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
**ModuleCommandRequest** — a canonical `SemanticOutput` envelope with a `ModuleCommand` payload and accepted sequence position; its inter-Ball path activates a complete `SemanticHandle` and materialized `sourceOrdinal`.
<!-- pkb:term:end -->


<!-- pkb:term:start name="SignalPublication" -->
**SignalPublication** — a canonical routed `SemanticOutput` envelope with a `Signal` payload, complete `SemanticHandle`, and materialized `sourceOrdinal`.
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

For `ModuleResultOutput`, the cross-Ball result path always activates a complete materialized handle, and §6.9 fixes it to `commandSource.semanticHandle`; its additional `commandSource` field is not part of the generic envelope. `sourceOrdinal` is the unique zero-based position within ordered `Decision.outputs`: values run from `0..outputs.size-1`. It is materialized and preserved when an output crosses the accepted call scope, is persisted, routed, redelivered, or observed independently. For one immediate local output, list position proves the same fact without a stored field. When `semanticHandle` is required it is complete and stable; omission on a triggered path is invalid. These envelope names and meanings are canonical; representation erasure does not create an alias protocol.

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
| validation before semantic input | `BoundaryResponse(ValidationFailure)`, including pre-Intent `IdempotencyConflict`; on a command route, only the verified pre-acceptance carrier | no accepted Decision, revision, operation, output, or business outcome; a conflict leaves the prior accepted operation unchanged | validation is not `BusinessRejection`, Reply, accepted result, or `NotFound` |
| admission before acceptance | `BoundaryResponse(AdmissionFailure(reason))`; on a command route, only the verified pre-acceptance carrier | no accepted Decision/revision/output; source command facet may become `RejectedBeforeAcceptance + NotExpected` after verified carrier receipt | capacity failure is not business rejection, target acceptance, or delivery stop |
| target `decide` rejects before acceptance | `BoundaryResponse(DecisionRejected(BusinessRejection))`; on a command route, only the verified pre-acceptance carrier | no accepted target revision/operation/output; source carrier projection remains `RejectedBeforeAcceptance + NotExpected` | informational reason does not become accepted `outcome = Rejected` |
| target Decision accepts a business outcome | accepted `ModuleResultOutput`, then verified `ModuleResultPulse` | target acceptance/result status is retained; source can project `Accepted` plus the target-owned outcome only after verified result evidence | dispatch or later failure never downgrades the result to a carrier or `BoundaryResponse` |
| Resource/Execution Gate acts after accepted work | provenance-bound `Fact` and, for a command target, a later accepted `ModuleResultOutput`; reachable result/status variants include `ResourceFailure`, `TimedOut`, or `OutcomeUnknown` | the prior source/target acceptance remains; outcome/status refines only from declared evidence | no rollback, pre-acceptance carrier, or fabricated business rejection |
| delivery policy exhausts after accepted output | trusted `DispatchStopped` observation | delivery/status facet only, keyed to the accepted output/result route | not business failure, cancellation, target non-execution, or erasure of accepted result |
| programming fault | runtime fault policy; pre-acceptance publishes nothing, post-acceptance preserves already accepted facts under the selected profile | operational failure/quarantine and only already-declared status evidence | never fabricated as validation, admission, business result, Reply, or delivery success |

A later-stage failure never rewrites an earlier acceptance or accepted result. Each row uses only the closed variants reachable for the concrete protocol/profile.

`ValidationFailure`, `AdmissionFailure`, and `BusinessRejection` may be encoded in a `BoundaryResponse`, but do not become a `SemanticOutput` and receive no commit identity.

For a root request, any semantic ID already reserved when one of those pre-acceptance stages rejects remains a candidate only. The boundary returns the applicable `BoundaryResponse`, does not expose the candidate as an authoritative `OperationId` or status lookup key, and creates no root operation/status source, known status row, retention marker, authoritative `SemanticHandle`, or output. A verified `CommandRejectedBeforeAcceptance` is different only at the already accepted source: it may refine that source operation's matching participant Step through a declared `ControlPulse`, while still creating no accepted target operation.

<!-- pkb:term:start name="CommandRejectedBeforeAcceptance" -->
**CommandRejectedBeforeAcceptance** — the verified mechanical command-route carrier of `commandSource`, effective protocol identity, exactly one pre-acceptance `BoundaryResponse`, and target-boundary provenance. It is neither Reply nor target result; at a source it projects `RejectedBeforeAcceptance + NotExpected` and creates no trusted Pulse when provenance is invalid. On a same-stack target-not-accepted branch, no accepted target level exists and the source Decision applying the carrier atomically consumes the transferred level-1 alternative-completion slot in the original causal scope.
<!-- pkb:term:end -->

For a command route, the canonical pre-acceptance mechanical carrier is:

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

- The carrier is constructed only after `commandSource`, effective protocol identity, the closed response, and target-boundary provenance have been verified.
- It is neither `ReplyOutput` nor `ModuleResult`.
- It creates no target accepted Decision/revision/output.
- A same-stack target attempt that produces it consumes no accepted target level; §8.4 atomically transfers the already reserved level-1 alternative-completion slot to the source carrier Decision.
- It reaches source state only through a declared typed mechanical `ControlPulse` projection.
- The source does not receive the embedded `BoundaryResponse` as a standalone result.
- Forged, tampered, missing, stale, or wrong-target provenance creates no trusted source Pulse, refusal facet, or compensation.
- A target contract's refusal classification is part of its effective protocol identity.

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

## 7. State and authority

### 7.1. State kinds

<!-- pkb:pba-source:start id="PBA-14" title="Explicit State Kind" -->
**Source clause for PBA-14 — Explicit State Kind.**

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
<!-- pkb:pba-source:end -->
### 7.2. One mutable fact—one authority

<!-- pkb:pba-source:start id="PBA-11" title="Single Semantic Authority" -->
**Source clause for PBA-11 — Single Semantic Authority.**

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
<!-- pkb:pba-source:end -->
### 7.3. State Belt

<!-- pkb:term:start name="State Belt" -->
**State Belt** — a logical map of isolated state authorities; not a global mutable-store API.
<!-- pkb:term:end -->

```text
StateBelt {
    AuthBall owns auth.session
    CatalogBall owns catalog.searchSession
    CartBall owns cart.contents
    CheckoutFlow owns checkout.operation
}
```

The State Belt IS NOT a mandatory global store, singleton API, or object available to every module. It is an architectural ownership model and, when needed, a generated registry.

### 7.4. Prohibition on direct cross-cell reads

<!-- pkb:pba-source:start id="PBA-13" title="State Isolation" -->
**Source clause for PBA-13 — State Isolation.**

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
<!-- pkb:pba-source:end -->
### 7.5. Captured input

A Flow or long-running operation may retain only the required foreign or point-in-time fields. The following is an applicability catalog, not a mandatory wrapper shape:

```text
CapturedInput {
    sourceCorrelation?
    sourceProtocolOrSchemaVersion?
    verifiedProvenance?
    capturedAt?
    purpose?
    fields
    expiresAtOrDeletionTrigger?
}
```

The same rule applies to a value from an earlier `Pulse` or `DecisionContext` that a later Decision needs: it becomes a bounded typed value in committed State or is explicitly reintroduced by a new trusted input. Correlation is added when several sources/operations can be confused; an explicit version when an independent protocol/schema can change or the value survives rollout/recovery; provenance when it crosses a trust/authority boundary; and a retention/deletion field when its lifetime differs from the containing state. A simple locally authoritative value in one build may be stored directly without that metadata. Inbox, outbox, runtime, or participant history is never a decision-readable substitute. Captured input does not become a second mutable authority. Before a strict action, the target may require an expected version, reservation, or current revalidation.

### 7.6. Single writer

<!-- pkb:pba-source:start id="PBA-12" title="Single Writer" -->
**Source clause for PBA-12 — Single Writer.**

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
<!-- pkb:pba-source:end -->
### 7.7. Commutative updates under single-writer acceptance

A Ball may declare a commutative or CRDT-like merge contract for concurrent ingress or independently prepared update values with proven properties:

```text
associative
commutative
idempotent or duplicate-aware
deterministic conflict handling
bounded metadata
```

Such a contract changes conflict and merge semantics but does not remove single-writer acceptance: one logical writer serializes the merge and assigns the `CommitRevision`. Two independent writers concurrently accepting a mutation of the same `BallInstance` are outside Core even when the algebraic properties hold. A genuine multi-writer profile requires a separate specification; without one, §7.6 and PBA-12 always apply.

### Definition source records for §7

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Captured Input" -->
**Captured Input** — an immutable bounded field-minimized value from a prior ingress/result/context/authority snapshot, retained for a later decision/recovery. It adds source correlation, version, provenance, and deletion metadata only when the actual source, rollout, trust, ambiguity, or lifetime trigger requires them; it does not become a second mutable authority.
<!-- pkb:term:end -->


<!-- pkb:term:start name="EphemeralState" -->
**EphemeralState** — Interaction/transport mechanics such as focus, scroll, animation, parser, or socket state that cannot change a business Decision. A decision-relevant UI/transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext`, not ephemeral authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Field-Minimized" -->
**Field-Minimized** — containing exactly the semantic, correlation, version, provenance, validity, and lifetime fields activated by the value's actual decision and boundary triggers; fields with absent triggers are omitted rather than filled with defaults or placeholders.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Replica State" -->
**Replica State** — an external/cache copy with provenance, revision, and freshness; it is not decision authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Sovereign State" -->
**Sovereign State** — canonical mutable state with one decision authority.
<!-- pkb:term:end -->


<!-- pkb:term:start name="StateKey" -->
**StateKey** — the identity of a BallInstance's local state/consistency/partition boundary.
<!-- pkb:term:end -->


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

## 9. Asynchrony, causality, and delivery semantics

This section is a catalog of independently triggered contracts, not one mandatory operation wrapper. Reachability is derived from the closed protocol, route, profile, resource, and failure model:

| Contract | Trigger |
|---|---|
| causal identity / revision (§§9.1–9.2) | work or its result can outlive the call, reorder, retry, cross an authority, recover, or appear in status; |
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
**CausalToken** — a field-minimized correlation record that binds detached, addressable, routed, late, or causal work to an accepted frame; generation, revision, depth, and budget fields appear only when their respective lifecycle or multi-Decision triggers exist. When materialized, one causal-budget scope preserves every triggered remaining depth and cumulative-fan-out capacity without adding a second scope field. `commandSource` derives from the accepted source command frame and `resultSource` from the accepted target result frame.
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
    causalDepth?            # the output can cause a later Decision
    causalBudgetScope?      # multi-hop or synchronous causal budget exists
}
```

`Fact` returns the accepted `EffectRequest` token or an equivalent sufficient correlation identity. A `ModuleCommandPulse` carries `commandSource` derived from the accepted source command frame. Its accepted target `ModuleResultOutput` retains that token, and the resulting `ModuleResultPulse` also carries `resultSource` derived from the accepted target frame. `causalDepth` is present and incremented only when a semantic output can cause a later Decision; a detached Reply or Projection with no such path needs no depth or causal budget. When a causal budget exists, yield, continuation, asynchronous handoff, and transport retry preserve `causalBudgetScope` and every triggered remaining depth/fan-out capacity; they do not reset or double-count the total budget. A transport retry changes attempt metadata, not the logical operation, semantic handle, or fan-out branch identity. Immediate local completion may remain in call scope and needs no materialized token except that a cross-Ball command/result bridge still proves the set-equal accepted tuples under same-stack representation erasure.

<!-- pkb:pba-source:start id="PBA-18" title="Provenance-Bound Result" -->
**Source clause for PBA-18 — Provenance-Bound Result.**

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
**Assembly** — an explicit composition root that selects routes, effective protocol/version pairs, delivery bindings, and command-result return bindings. It transports verified values and has no authority to synthesize or modify causal tokens, target-owned payloads, context, refusal meaning, policy, read-result selection, or other business semantics.
<!-- pkb:term:end -->

Under the marked definition, Assembly is responsible for the route, selected effective protocol/version pair, command-ingress and result-return bindings, and deployment wiring. It transports only values verified under the target-owned contract. It makes no business decision and reads no private state. Assembly MUST NOT synthesize or modify `commandSource`, `resultSource`, a `ModuleCommand` or `ModuleResult` payload, mapping, refusal classification or reason, or other business meaning.

For a `ReadDependency`, Assembly binds the caller to the selected target authority, target-owned `Query -> ResultPayload` mapping, read/status authority, and transport. It does not own or alter the caller's freshness/consistency requirement, target result, status fact, `ConsistencyStamp`, or read meaning. Explicit versions, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, and status fields appear only under their existing triggers; generated same-stack wiring proves the same resolved contract without needing materialized route wrappers.

For a `DeclaredSignalDependency`, Assembly fixes the producer, consumer, effective protocol identity, delivery semantics, source identity/provenance, and finite fan-out/observation-size bounds. Independent protocol versions, deduplication, ordering, buffering, causal-depth, and delivery-attempt fields are added only when their §10.2 triggers exist. Such a route delivers an `ObservedSignal` but does not turn the Signal into a command or grant the consumer additional authority.

Static composition is the default. It may be generated direct dispatch and does not require a runtime service registry. Generated same-stack code may erase transport objects only while proving the same accepted source frame, target frame, effective protocol identity, and issuer provenance as the materialized bridge.

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

Permitted asynchronous feedback begins only after an explicit bounded handoff/yield and does not itself add a `Direct Control Dependency` edge. The handoff preserves the existing causal depth and cumulative-fan-out scope/budget; it cannot reset either counter at the queue, broker, worker, or target hop. It does not make a compile-time import or direct-control cycle permissible.

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
**DeclaredCommandDependency** — an explicit one-hop command dependency without an independent multi-participant workflow; the target owns one exact command-to-result mapping/refusal classification, the caller imports it, and Assembly binds verified command ingress and accepted-result return.
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

## 11. Security and privacy

`No Ambient Authority` (§11.5) is always applicable. The remaining guardrails are activated independently:

| Guardrail | Trigger |
|---|---|
| Double Quarantine | untrusted bytes/platform input or raw resource/SDK output crosses into semantic code; |
| actor context | actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization or result selection; |
| Policy/Execution Gate and Grant | a privileged action depends on both current business and technical authorization and activates both gates; an actor-dependent read that can select more than one semantic permission/result outcome activates the Policy Gate only; a cross-authority action proof activates the Grant contract; |
| capability | a consequence accesses I/O or another external authority/resource; an already scoped constructor dependency may be the capability; |
| safe sink | data reaches SQL, HTML/JS, shell, paths, URLs, deserialization, or another interpreter/dialect; |
| secret containment | a secret can enter state, output, persistence, serialization, logs, or telemetry; |
| unsafe escape hatch | the closed effect algebra deliberately admits raw interpreter authority; if it does not, no repeated forbidden-capability list is required; |
| isolation/hardening evidence | the corresponding profile or security claim is selected. |

Shared ingress, capability, sink, redaction, and isolation mechanisms may be declared and tested once at exact project/binding scope. A Ball records only its semantic wiring, triggered local policy, and permitted delta. Unknown trust or external behavior activates the conservative guardrail; omission of local boilerplate never suppresses an inferred trigger.

### 11.1. Double Quarantine

<!-- pkb:pba-source:start id="PBA-31" title="Double Quarantine" -->
**Source clause for PBA-31 — Double Quarantine.**

- **Rule:** Every raw external-input or raw resource-output edge that exists passes through the applicable parsing, validation, finite bounds, and provenance checks before its value reaches the Nucleus.
- **Applicability:** `P`: raw input or resource output crosses a trust/representation edge.
- **Declaration owner:** Interaction/Resource adapter.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Parser/provenance/fuzz tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact validator evidence reusable; absent side needs no placeholder.
- **Primary verification route:** `§17.4`
<!-- pkb:pba-source:end -->

Both external sides of the Nucleus are treated as untrusted:

```text
User / HTTP / UI / OS        -> untrusted input
Network / DB / File / SDK    -> untrusted resource output
```

Data path:

```text
Untrusted bytes
  -> parse
  -> normalize by field contract
  -> validate and bound
  -> validated semantic value + trusted DecisionContext/ReadContext when required
  -> Nucleus Policy Gate or pure semantic read
```

A Resource response passes through an analogous parse, validation, and provenance path before a `Fact` is created.

When actor identity affects semantics, the trusted binding boundary before the `Nucleus` authenticates the actor and constructs the verified, bounded, field-minimized `DecisionContext` or actor-dependent `ReadContext`, but does not make the business authorization or result-selection decision. A value becomes an authorized cause of a privileged action only after the `Nucleus` Policy Gate; authoritative execution then passes separately through the Execution Gate. A Decision or read path with no actor-context trigger creates no actor record or authentication row.

### 11.2. Actor context

<!-- pkb:term:start name="AuthenticatedActorContext" -->
**AuthenticatedActorContext** — a verified, bounded, field-minimized trusted description of a stable subject used only when actor data changes a Decision or Query/status-read authorization/result selection. Its approved issuer/realm is materialized or proven by fixed trusted same-stack binding scope; method, assurance, time, expiry, and delegation appear only when their policy/lifetime paths exist, and the context grants no authority by itself.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-44" title="Trusted Actor Context" -->
**Source clause for PBA-44 — Trusted Actor Context.**

- **Rule:**
  - When actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization or result selection, a trusted binding boundary constructs a verified, bounded, field-minimized actor context.
  - Its authenticity and integrity resolve relative to a valid approved issuer or an equivalent fixed trusted same-stack issuer/realm proof before the Ball interprets it.
  - Forged, tampered, wrong-issuer/realm, missing, stale, or otherwise unverifiable required evidence fails closed.
  - Actor context grants no authority by itself.
  - After valid evidence and read admission, the Nucleus/read Policy Gate selects one variant of the Query's total target-owned `ResultPayload`, including the declared denied, redacted, or non-disclosing outcome when policy does not permit ordinary disclosure; it does not return `BusinessRejection` or a post-admission `BoundaryResponse`.
  - An actor-independent Decision or read materializes no actor-context, issuer, authentication, or actor-evidence artifact.

  When the actor-context trigger exists, authentication and raw provenance verification are performed by the trusted binding boundary, not by the request payload or Nucleus. The Ball/Nucleus owns the semantic context schema and interpretation. Fixed issuer/realm facts may be proven by the enclosing trusted type or binding rather than copied into every value.

  Applicability catalog:

  ```text
  AuthenticatedActorContext {
      stableSubjectId
      issuer?                # issuer is not fixed by trusted enclosing scope
      namespaceOrRealm?      # more than one realm/namespace is possible
      authenticationMethod? # method affects policy or evidence
      assuranceLevel?        # assurance affects policy
      authenticatedAt?       # authentication time affects validity/audit
      expiresAt?             # context can expire
      delegation?            # delegation exists
  }
  ```

  The effective `stableSubjectId` must be scoped by issuer and realm, whether those identities are materialized or statically fixed. Credential rotation does not automatically create a new subject.

  The presence of an `issuer` field is not proof of origin. The selected security or isolation profile MUST define verifiable authenticity and integrity of actor context relative to a valid approved issuer or the equivalent fixed trusted same-stack proof. Forged or tampered evidence, evidence from a wrong or unapproved issuer/realm, and missing, stale, or otherwise unverifiable required evidence fail closed before the actor context can authorize or otherwise change a Decision or Query/status-read result. `AuthenticatedActorContext` itself describes the actor but grants no authority to perform an arbitrary action. A path whose Decision and read authorization/result are actor-independent creates no actor-context value, issuer row, authentication artifact, or actor-specific evidence.
- **Applicability:** `R`: actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection.
- **Declaration owner:** Ball semantic owner defines context schema/interpretation; Trusted Boundary/security-binding owner defines construction, approved issuer/static proof, and evidence contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Trusted Boundary verifier plus Decision/read authorization, permitted/denied/redacted/non-disclosing result selection, actor-origin, wrong-provenance, and actor-independent sparsity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact issuer/verifier policy may be reused only in scope; fixed trusted same-stack issuer/realm may be static; actor-independent Decision/read paths omit actor context, issuer data, authentication, and actor-evidence artifacts.
- **Primary verification route:** `§17.8`
<!-- pkb:pba-source:end -->
### 11.3. Policy Gate and Execution Gate

<!-- pkb:pba-source:start id="PBA-33" title="Dual Gate" -->
**Source clause for PBA-33 — Dual Gate.**

- **Rule:**
  - When a privileged action requires business and current technical authorization, the Nucleus Policy Gate alone makes the business permission decision from committed State, the current cause, and trusted semantic context.
  - Immediately before authoritative execution, the target/resource Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding.
  - It does so without making a new business decision.
  - Failure after target acceptance is a declared Resource/result/status path.
  - It neither rolls back nor downgrades accepted work.
  - It never becomes a pre-acceptance command carrier.

  #### Policy Gate

  The `Nucleus` alone decides whether an action or actor-dependent read is permitted and which business-visible result follows from the current State, cause or Query, and trusted context. After an actor-dependent read is admitted, the pure read Policy Gate selects one variant from the Query's total target-owned `ResultPayload`: permitted, denied, redacted, or deliberately non-disclosing as that exact protocol declares. It never turns that semantic outcome into `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary-invented `NotFound`. A pure read remains non-mutating and creates no `Decision`; this does not move its semantic permission or result selection into Interaction, Assembly, runtime, or Resource.

  #### Execution Gate

  Immediately before an authoritative action, the target or resource enforces the applicable subset of this catalog without revisiting business permission:

  - accepted-action identity and immutable payload binding when execution follows an accepted output;
  - minimum technical capability when an external resource is used;
  - authenticity and integrity of actor context and, for a privileged action, any required grant from an approved issuer for the relevant realm, audience, and action;
  - binding of actor, context, audience, action, object, operation, and each constrained field when such constraints authorize the actual target or payload;
  - proof/grant version and expiry, freshness, or revocation when those policies exist;
  - expected target/object version when concurrency or a current-version constraint exists;
  - idempotency identity when duplicate execution is possible;
  - endpoint identity for a network endpoint;
  - local quota or budget when quota enforcement is present;
  - safe-sink constraints at an interpreter edge.

  Resource does not reinterpret business intent or apply a second business policy. It verifies only the technical authorization needed to execute the already accepted action. Fields and checks whose trigger is absent are omitted rather than populated with defaults.

  The Execution Gate fails closed for every triggered proof: required evidence that is missing, unverifiable, expired, stale, revoked, wrong-version, or mismatched does not authorize an authoritative action and is not replaced by an ambient credential, transport authentication, or a trusted string-valued `issuer`. Rejection returns as a declared typed Resource outcome bound to the accepted action. If target acceptance already occurred, the owning Nucleus may accept the corresponding target-owned result/status transition, but neither the gate nor its failure rewrites that history as `CommandRejectedBeforeAcceptance`. Authentication of an IPC peer alone does not make that peer an approved authorization issuer.
- **Applicability:** `R`: a privileged action needs business and current technical authorization, or an actor-dependent read can select more than one semantic permission/result outcome. Only the gate obligations present on that path materialize.
- **Declaration owner:** Ball/Nucleus owns action/read business permission and read result selection; target/resource owns the immediate technical Execution Gate for an action.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Sole-policy-site, gate-order, proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink, and post-acceptance failure tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Gate mechanics/evidence reusable; action/read meaning remains local; omit the Execution Gate for a pure read and omit the whole rule when neither trigger exists.
- **Primary verification route:** `§17.8`
<!-- pkb:pba-source:end -->
### 11.4. Capability

<!-- pkb:pba-source:start id="PBA-32" title="Capability-Sealed Effect" -->
**Source clause for PBA-32 — Capability-Sealed Effect.**

- **Rule:** Every external effect uses the minimum explicit technical authority for its bounded operation class, enforced at a real capability boundary.
- **Applicability:** `P`: an `EffectRequest` reaches the Ball's external resource/authority.
- **Declaration owner:** Ball capability semantics and binding realization.
- **Scope:** The exact external effect path and its bounded operation class.
- **Enforcement / evidence owner:** Binding/resource owner at the scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolation boundary; positive restriction and unrestricted-authority negative tests.
- **Resolution, failure, and conformance:** Authority wider than the declared operation class, or a local check behind unrestricted authority, fails the rule at that effect path.
- **Reuse and absent-trigger behavior:** Capability policy reusable; omit when no external Effect path exists.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->

<!-- pkb:term:start name="Capability" -->
**Capability** — the minimum explicit technical authority to perform a bounded class of resource operations, enforced at a real capability boundary. A wrapper or local check over unrestricted authority is not that boundary.
<!-- pkb:term:end -->

Prefer:

```text
CatalogSearchCapability(index=products, maxPageSize=100)
PaymentCaptureCapability(merchant=42, currency=EUR, maxAmount=500)
SandboxFileCapability(root=/app/cache, operations=[read, write])
```

Instead of:

```text
DatabaseAdmin
ArbitraryHttp
FileSystemAll
ShellExecute
```

A Capability must be enforced by a real boundary: a scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolated process. `if (allowed)` inside an adapter that already holds an unrestricted credential is only code discipline.

### 11.5. No ambient authority

<!-- pkb:pba-source:start id="PBA-34" title="No Ambient Authority" -->
**Source clause for PBA-34 — No Ambient Authority.**

- **Rule:** Every Ball and binding keeps application authority and mutable business communication out of ambient globals, credentials, service locators, runtime registries, and shared foundation state; dependencies and communication paths are explicit in construction, protocol, or static Assembly.
- **Applicability:** `A`: every Ball/binding.
- **Declaration owner:** Project dependency and explicit-communication contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports, call graph, global-state/service-locator/runtime-registry/foundation scans.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared injection convention may be reused; actual graph proves absence of ambient authority and hidden communication.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

Application code does not obtain resources through:

```text
Database.global
HttpClient.default
ServiceLocator.get(...)
ApplicationContext
GlobalScope
```

Dependencies are supplied through explicit construction or static assembly. The `Nucleus` receives semantic context and values, not service objects.

### 11.6. Safe sinks

<!-- pkb:pba-source:start id="PBA-35" title="Safe Sink" -->
**Source clause for PBA-35 — Safe Sink.**

- **Rule:** Every interpreter or dialect boundary that exists uses the applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- **Applicability:** `P`: data reaches an interpreter/dialect.
- **Declaration owner:** Resource/sink owner.
- **Scope:** The exact interpreter or dialect edge and its data/context interpretation.
- **Enforcement / evidence owner:** Owning Interaction/Resource sink implementation; parameterized, structured, capability-rooted, and context-encoded positive fixtures plus raw-injection/traversal negatives as applicable.
- **Resolution, failure, and conformance:** A typed wrapper without an applicable safe sink, or raw data interpreted as code/path/dialect syntax, fails that edge.
- **Reuse and absent-trigger behavior:** Adapter/evidence reusable; omit with no interpreter edge.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->

A typed operation is necessary but insufficient.

| Resource | Safe sink requirements |
|---|---|
| SQL | Parameterization, timeout, row/byte limit, least-privilege DB role |
| Pattern search | Explicit literal/wildcard semantics and dialect escaping |
| Filesystem | Capability-rooted path resolution; no raw traversal |
| HTTP | Allowed service identity, DNS/redirect/response bounds |
| OS | Structured API; shell only as an explicit unsafe exception |
| HTML/JS | Context-specific encoding in Interaction |
| Serialization | Schema, size/depth bounds, unknown-field policy |

### 11.7. Authorization grant

A privileged cross-Ball command or effect may carry attenuated proof:

```text
AuthorizationGrant {
    actorContextId
    issuer
    audience
    action
    objectRef
    operationId
    constraints?
    expectedObjectVersion?
    issuedAt
    expiresAt
}
```

<!-- pkb:term:start name="Grant" -->
**Grant** — scoped business-authorization proof from an approved issuer with verified authenticity/integrity and complete binding to actor, action, object, audience/target, operation, and validity period. Its optional `constraints` bind only fields made authorization-relevant by the action contract for the actual target or payload. Immutable binding to an accepted output and duplicate-execution idempotency are separate Execution-Gate checks activated by their §11.3 triggers; an absent subtrigger adds no field, check, default, or placeholder.
<!-- pkb:term:end -->

A Grant is effective only when the Execution Gate has verified the approved issuer, authenticity and integrity, and binding to trusted actor context and the actual operation. The action contract defines mandatory constrained fields; each such field in the actual payload, including amount and currency or equivalent restrictions, must match or fall within the grant. An unverified or absent constraint grants no additional authority.

A Flow passes only the grant required by the next participant, not the entire principal or session object. A Flow may forward or attenuate a grant within the original authority, but does not expand constraints and is not considered an issuer unless a trusted issuance policy separately permits it.

Exact cryptographic format, wire canonicalization, and key distribution belong to the Secure extension. Core requires a semantic verification contract but does not choose a signature, MAC, protected channel, unforgeable handle, or other concrete mechanism.

### 11.8. In-process limitations

In-process visibility, a private modifier, and a wrapper type do not isolate credentials from malicious native code. `InProcess + Hardened` may reduce accidental misuse but does not claim hostile-component containment.

An ordinary in-process data object with an `issuer` field is not proof. A Hardened binding must have a profile-defined trusted issuance and verification boundary even when cryptographic encoding is unnecessary within one trusted process.

A hostile plugin, parser, or SDK requires an `Isolated` boundary: a process or sandbox, bounded IPC, separate credentials, resource quotas, and crash containment.

### 11.9. Secrets

<!-- pkb:pba-source:start id="PBA-36" title="Secret Containment" -->
**Source clause for PBA-36 — Secret Containment.**

- **Rule:** A secret does not enter state, output, persistence, serialization, logs, or telemetry without an explicit policy for that exact path and scope.
- **Applicability:** `R`: a secret can enter State, any output, persistence, serialization, logs, or telemetry.
- **Declaration owner:** Project classification and affected owner.
- **Scope:** Each exact reachable secret path and its Ball/project/binding scope.
- **Enforcement / evidence owner:** Affected State/output owner plus persistence, serializer, log, and telemetry owners; full data-flow and non-log leakage tests.
- **Resolution, failure, and conformance:** Any reachable path without an explicit policy for that exact path and scope fails closed; a policy for one sink does not cover another.
- **Reuse and absent-trigger behavior:** Exact secret policy reusable; omit when closed data flow proves no secret.
- **Primary verification route:** `§17.8`
<!-- pkb:pba-source:end -->

```text
Secret<T>
CredentialHandle
PrivateKeyHandle
AccessToken
```

Prohibited by default:

- ordinary `toString`/debug rendering;
- Projection, Signal, and generic Reply;
- logs/metric labels;
- unencrypted persistence;
- arbitrary serialization;
- generic read model/replica;
- plugin transfer.

A wrapper does not promise physical zeroization in a garbage-collected runtime. The implementation honestly documents copies, swap, crash dumps, and key lifecycle.

### 11.10. Unsafe escape hatch

<!-- pkb:pba-source:start id="PBA-37" title="Explicit Unsafe Escape Hatch" -->
**Source clause for PBA-37 — Explicit Unsafe Escape Hatch.**

- **Rule:**
  Raw SQL, shell, an arbitrary URL, unsafe deserialization, or unrestricted filesystem access is permitted only as an explicitly named unsafe operation with:

  ```text
  owner
  reason
  scope
  capability
  isolation decision
  security review
  tests
  expiry/remediation
  ```

  An escape hatch must not masquerade as an ordinary `Effect`.
- **Applicability:** `R`: raw authority/unsafe escape exists.
- **Declaration owner:** Named security owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Capability gate/scan/audit tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Controls reusable, but each site resolves; omit register when scan proves none.
- **Primary verification route:** `§17.8`
<!-- pkb:pba-source:end -->
### 11.11. Observability and privacy

Tracing is path-triggered by actual observability use and claim-triggered when trace evidence supports a guarantee. A present operational trace contains only the causal identifiers, type names, revisions, timing, and outcome fields materialized by the observed path; it adds no placeholder ID or revision. It contains no secrets or optional raw payloads.

A security audit, replay bundle, metrics, and debug trace are distinct data products. None substitutes for another.

### Definition source records for §11

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="Execution Gate" -->
**Execution Gate** — the target/resource check immediately before authoritative execution that enforces every triggered proof, capability, constrained-field, version, freshness/revocation, idempotency, endpoint, quota, and safe-sink binding for an already accepted action without inventing a new business decision. Post-acceptance failure is a typed Resource/result/status path, never a pre-acceptance carrier or downgrade.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Policy Gate" -->
**Policy Gate** — the Nucleus semantic rule that alone determines business permission and, for an actor-dependent pure read, selects one permitted, denied, redacted, or deliberately non-disclosing variant from the Query's total target-owned `ResultPayload` using State, Query, and trusted context. It is distinct from the target/resource Execution Gate; a pure read creates no `Decision` and uses no `BusinessRejection` or post-admission `BoundaryResponse`.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Safe Sink" -->
**Safe Sink** — the applicable parameterized, structured, capability-rooted, or context-encoded API at an interpreter or dialect boundary; it prevents untrusted data from being interpreted as code, traversal, or dialect syntax.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Trusted Boundary" -->
**Trusted Boundary** — an explicitly authorized binding edge that verifies representation, finite bounds, provenance, accepted-frame correspondence, protocol identity, and every triggered authenticity/integrity/version/validity rule before constructing trusted semantic input, non-empty `DecisionContext`, or actor-dependent `ReadContext`. It is an edge, not the Interaction role; physical co-location transfers neither semantic interpretation nor business authority. The Ball/Nucleus owns semantic schema and interpretation. For a command/result bridge it constructs `ModuleCommandPulse`/`ModuleResultPulse` only from the corresponding accepted frame; it does not replace target `decide`, select policy/read results, synthesize business meaning, or grant authority merely by authenticating origin.
<!-- pkb:term:end -->


---

## 12. Execution profiles

Profiles are independent dimensions. A single overloaded label such as `DurableSecureFast` is not used.

```text
execution:   Inline | BoundedConcurrent
state:       Transient | SnapshotOutbox | EventJournal
isolation:   InProcess | Isolated
security:    Standard | Hardened
composition: Static
```

`Secure`, as a strong adversarial claim, is reserved for a separate isolation and security specification.

A project or binding may select an exact default profile once. A Ball that uses it records no duplicate profile row; it records only an explicit permitted override. The effective profile is the statically resolved project selection plus that delta. Selecting a stronger profile activates its mechanisms and tests; merely listing all profiles does not.

### 12.1. Semantics common to all profiles

Every profile preserves the always-applicable semantics:

- the three logical zones;
- explicit ownership;
- pure bounded decision;
- no reentrant transition;
- atomic Decision acceptance;
- no ambient authority;
- finite bounds on every present variable dimension.

When an output, detached-work, duplicate, retry, cancellation, ambiguity, concurrency, durability, isolation, or security trigger exists, the selected profile also preserves the corresponding §8–§11 contract. A profile name does not require machinery for an absent path, and it cannot waive machinery for a present one.

### 12.2. Inline

```text
Interaction -> decide -> atomically publish AcceptedSnapshotDecisionFrame | AcceptedEventCommit -> dispatch retained outputs
```

Properties:

- one mutating transition at a time;
- no mandatory mailbox;
- no mandatory parallel effects;
- no imposed synchronization, queue, coroutine, serialization, or thread hop;
- synchronous completion processed in the next iteration after acceptance;
- a caller-owned or fixed output buffer is permitted.

Suitable for mobile, desktop, embedded, a local backend use case, and a unit-test binding.

### 12.3. BoundedConcurrent

```text
bounded mailbox
  -> single-writer decision loop
  -> bounded effect/command workers
```

Required:

- finite mailbox capacity;
- typed `BoundaryResponse(AdmissionFailure(reason))` for pre-acceptance backpressure/admission;
- finite worker counts when workers exist;
- a fairness policy when separately queued control inputs could starve;
- out-of-order Fact/Result handling when completion can reorder;
- cancellation/deadline race tests when those paths exist;
- no concurrent mutation of one state instance.

### 12.4. Transient state

- state lives only in process memory;
- a crash may lose state, live replies, and pending operations;
- the source claims no durable delivery;
- an external side effect may have an unknown outcome after a crash;
- recovery uses reinitialization or resynchronization according to the application contract.

### 12.5. SnapshotOutbox

The always-present durable-state contract records the state snapshot and commit revision in one authoritative transaction and recovers from that committed snapshot. The outbox part is empty and may be representation-erased when no output exists.

Additional clauses materialize independently with their triggers:

- when a Decision has durable source outputs, the same transaction records the complete output batch and dispatch begins only after commit;
- a present durable source record remains `Pending` only while its declared delivery policy permits work and is retained through that policy's terminal horizon;
- when retry exists, attempts are finite under the effective time/attempt budget and preserve the same `OutputId`;
- target or executor deduplication is mandatory only for duplicate-permitting irreversible execution;
- when a retrying/retained delivery policy can exhaust, the source atomically records `DispatchStopped` with the available reason/evidence and does not silently discard the accepted output;
- ingress/inbox or idempotency records and their retention horizon exist only when duplicate acceptance/delivery is possible;
- expected revision or a storage-enforced ownership epoch is verified only when concurrency or movable ownership activates it;
- an ambiguous storage commit of an irreversible input does not permit blind re-evaluation;
- operation status exists only under the §9.11 trigger and contains only its reachable facets.

The unconditional Core guarantee ends at durable state acceptance. For present source outputs or status, it additionally includes only the retention and terminal semantics resolved for those paths. It does not promise target receipt, target acceptance, or external execution under every failure schedule.

A concrete binding may claim `at-least-once` only for a present output path, relative to an explicitly named delivery point and with recorded liveness/failure assumptions, retry and retention horizon, and terminal policy. Without such a contract, a present outbox provides only its declared bounded duplicate-permitting dispatch semantics, not unconditional eventual delivery or exactly-once external execution.

### 12.6. EventJournal

- authoritative mutation consists of ordered domain events;
- every Accepted result records an `AcceptedEventCommit`, including `NoDomainChange` with an empty event batch;
- the commit envelope, event batch, and accepted-input marker are accepted atomically; idempotency metadata, status changes, and source output records join the transaction only when their independent triggers exist;
- state is reconstructed through `evolve`;
- present durable outputs are not regenerated by rerunning current transition code;
- exact replay requires pinned artifacts and context and belongs to a separate extension specification.

### 12.7. InProcess

- boundaries are logical and enforced by compile-time or runtime discipline;
- direct calls and a shared allocator are permitted, but every synchronous cross-Ball call before handoff/yield is a `Direct Control Dependency` and the direct-control graph remains acyclic;
- crash containment and hostile credential isolation are not claimed;
- least privilege requires scoped external credentials and adapters, not only visibility modifiers.

### 12.8. Isolated

- process/sandbox boundary;
- IPC is not automatically asynchronous: a synchronous cross-Ball IPC call before handoff/yield is still a `Direct Control Dependency` and remains subject to the acyclic graph;
- versioned authenticated IPC; peer authentication does not replace authorization-issuer verification when actor/grant paths exist;
- profile-defined authenticity and integrity for present actor context and grants under §11.3;
- bounded frame size/depth;
- no shared mutable business memory;
- separate/scoped credentials for present external-resource paths;
- CPU/memory/wall limits for the isolated execution; network/file authority is denied by construction or receives its own finite limits only when exposed;
- crash/restart/quarantine policy;
- anti-replay/idempotency when the operation class permits duplicates or retries.

### 12.9. Hardened

`Hardened` strengthens every present security path in the selected execution/isolation profile; it does not create unused actor, resource, interpreter, secret, or grant paths. Apply only the triggered items:

- actor context from an approved issuer or fixed trusted same-stack issuer/realm proof when actor identity affects a Decision or Query/status-read authorization/result selection;
- least-privilege capabilities for present external resources;
- safe sinks at present interpreter boundaries;
- secret handling for present secret flows;
- explicit grants for privileged proof crossing an authority;
- bounded resource abuse controls;
- security tests for the triggered paths and evidence for the claimed threat assumptions.

It does not mean absolute security.

### 12.10. Profile selection

| Scenario | Recommended starting profile |
|---|---|
| Local UI state machine | `Inline + Transient + InProcess + Standard` |
| Async mobile feature | `BoundedConcurrent + Transient/SnapshotOutbox + InProcess` |
| Durable backend aggregate | `BoundedConcurrent + SnapshotOutbox + InProcess + Hardened` |
| Hostile plugin/parser | `BoundedConcurrent + Transient/SnapshotOutbox + Isolated + Hardened` |
| Distributed irreversible effect | Durable source + separate executor/provider contract; not only a Core profile |

### Definition source records for §12

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="EventJournal" -->
**EventJournal** — a durability profile in which authoritative state is reconstructed from committed domain events and every accepted input, including `NoDomainChange`, receives a durable `AcceptedEventCommit`; idempotency, status, and source-output records join that transaction only when their paths exist.
<!-- pkb:term:end -->

<!-- pkb:term:start name="SnapshotOutbox" -->
**SnapshotOutbox** — a durability profile in which a state snapshot is accepted durably and any present durable source outputs join the same atomic transaction; retry, duplicate handling, terminal `DispatchStopped`, and status/retention facets appear only when their delivery paths exist, and eventual delivery is not guaranteed.
<!-- pkb:term:end -->


---

## 13. Limits, budgets, and performance

### 13.1. Three classes of limits

#### Semantic limits

Affect the business or orchestration decision:

```text
maxItemsPerOrder
maxWorkflowSteps
maxCommands
maxRetriesByPolicy
businessDeadline
```

They belong in state or context and are visible to the `Nucleus`.

#### Runtime caps

Constrain implementation resources on paths where those resources exist:

```text
CPU
wall time
memory
mailbox
IPC bytes
storage bytes
parallel workers
delivery attempts per output
outbox and operation-status retention
```

They are enforced by the runtime and may cause an admission failure or fault, but do not rewrite the business decision. The project/binding may declare and verify a cap once; a Ball repeats it only when it overrides the shared value or needs different semantic failure behavior.

`maxCumulativeFanout` is a causal-composition cap rather than a business choice: §10.9 may prove it statically from the closed output/route graph, or the binding enforces the same branch count through existing pre-acceptance causal reservation. In the latter form it is runtime admission state and cannot enter `DecisionContext` or be reset by an async worker/handoff.

`maxInputBytes`, `maxStateBytes`, and `maxOutputBytesPerDecision` are pre-acceptance resource caps, not business choices. Section 8.3 fixes each active numeric dimension through an immutable binding-owned `BoundedByteMeasure`: exact boundary stage and included input metadata/Context, complete candidate-next-State representation, or complete ordered output-sequence representation. A static bounded type and representation need no runtime byte counter; otherwise the boundary or acceptor applies the selected measure at the exact stage and cannot substitute an incomparable wire/heap/storage representation, move the value into `DecisionContext`, or truncate a value.

#### Economic/shared quotas

Money, provider calls, API credits, and a shared tenant balance require an authoritative ledger or reservation. Core requires only an explicit contract and prohibits copying the entire parent balance into parallel branches. A complete distributed budget protocol is an extension.

### 13.2. Backpressure

When admission can fail because a bounded runtime/shared resource is saturated, overload returns a typed outcome. The following is an illustrative Core applicability catalog, not an open or mandatory concrete union:

```text
TemporarilyUnavailable(retryAfter?)
CapacityExceeded(scope)
QueueFull
StoragePressure
```

Each concrete profile/binding with fallible admission declares one finite closed `AdmissionFailure.reason` union containing only its reachable cases. Decoding, construction, and exhaustiveness checks reject an unknown discriminator or free-form/open string; they do not map it to `TemporarilyUnavailable`, `BusinessRejection`, or another stage. A direct Inline path with no fallible admission declares no empty reason union.

Before Decision acceptance, such overload is returned as `BoundaryResponse(AdmissionFailure(<typed overload outcome>))`. It creates no accepted state, `CommitRevision`, `SemanticHandle`, or `SemanticOutput`. A post-acceptance delivery observation is not retroactively converted into admission failure.

An admission failure does not masquerade as a permanent business rejection. An unbounded queue is prohibited. A direct Inline path with no queue or fallible admission needs no backpressure type or empty policy.

`DispatchStopped` after an already accepted durable output is not an admission failure or business rejection. It is a terminal delivery observation: the source retains the output and status according to the declared retention policy and delegates the next decision to reconciliation or manual policy.

### 13.3. Zero Mandatory Runtime Tax

<!-- pkb:pba-source:start id="PBA-40" title="Zero Mandatory Runtime Tax" -->
**Source clause for PBA-40 — Zero Mandatory Runtime Tax.**

- **Rule:**
  Core semantics do not require:

  ```text
  runtime handler lookup
  reflection discovery
  in-process serialization
  mandatory queue
  mandatory thread hop
  object message hierarchy
  service locator
  ```

  Only a concrete Inline binding with a benchmark on the exact toolchain may claim `maxStructuralAllocationsPerDecision = 0`.

  Distinguish:

  - **structural allocation**—created solely by the architectural runtime mechanism;
  - **payload allocation**—useful domain or result data;
  - **payload copy**—copying useful payload across a boundary.
- **Applicability:** `A`: Core design; implementation check when Inline selected.
- **Declaration owner:** Core/profile and Inline binding owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Build inspection; benchmark only for a claim.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One binding serves many Balls; no per-Ball wrapper.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->
### 13.4. Claim contract

<!-- pkb:pba-source:start id="PBA-41" title="Measured Claim" -->
**Source clause for PBA-41 — Measured Claim.**

- **Rule:** A performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim names its exact guarantee boundary, binding and scope, mechanism, assumptions, retention, evidence, and non-guarantees; without that complete in-scope evidence, the claim is not made. A claim that compares `maxTransitionSteps` counts across bindings additionally proves equality of Decision Work Meter identity/version, transition artifact version, and unit definition; otherwise the counts are intentionally incomparable.
- **Applicability:** `C`: any named performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim is made; cross-binding transition-step comparison additionally requires equal meter/artifact/unit identity.
- **Declaration owner:** Claimant names the guarantee boundary, exact binding/scope, mechanism, assumptions, retention, evidence/non-guarantees, and any compared meter/artifact/unit identity.
- **Scope:** The exact named boundary, binding, environment, workload, retention horizon, and audience of the claim.
- **Enforcement / evidence owner:** Claimant plus benchmark/security/fault/recovery/delivery/conformance evidence owner; meter-identity equality owner when counts are compared.
- **Resolution, failure, and conformance:** Missing or out-of-scope claim fields/evidence remove the claim rather than creating an implied guarantee; unsupported strengthening is non-conforming.
- **Reuse and absent-trigger behavior:** Reuse only on identical digest/scope; unlike meters remain incomparable, and without evidence omit the claim.
- **Primary verification route:** `§17.9`
<!-- pkb:pba-source:end -->

<!-- pkb:term:start name="Claim Record" -->
**Claim Record** — a binding-specific statement of a performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance guarantee together with its exact named boundary, scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees. Source durability or retained pending work alone proves no stronger downstream outcome. When no claim is made, no evidence dossier is required.
<!-- pkb:term:end -->

A Ball, project, or binding materializes a Claim Record only for a claim it actually makes:

```text
ClaimRecord {
    exactNamedBoundary
    exactBindingAndScope
    mechanism
    assumptions
    retention
    evidence
    explicitNonGuarantees
}
```

Its metrics and evidence may include:

```text
transition p50/p95/p99
structural allocations
payload copies
state bytes
mailbox occupancy
outbox age
max effects/commands
external response bytes
CPU/wall limits
binary contribution
```

An absolute number without a workload, hardware, compiler and runtime, payload distribution, and measurement method is not a substantiated claim. In the absence of evidence the claim is prohibited; ordinary implementation work does not require an empty evidence dossier.

### 13.5. Design-time tax

Architectural cost includes more than runtime:

```text
manifest size
number of artifacts
routes per operation
manual review hours
waiver count
schema/evidence maintenance
flow-to-feature ratio
```

A simple Ball should not pay ceremony for unused distributed guarantees.

For an adoption pilot, the project selects the relevant measure, workload, measurement method, baseline, and continue/reshape/stop threshold. Candidate measures include state size, peak operations per key, transition cost, contention, change radius, recovery cost, and the design-time items above. Core supplies no universal number. A pilot measurement becomes a performance claim only when the project makes that claim under §13.4.

Shared mechanisms, policies, and evidence are reviewed once at their exact scope and referenced by every covered Ball. A local change records only a newly triggered guardrail, semantic delta, override, or invalidated evidence item. Copying an unchanged project policy or repeating `N/A` for absent paths is itself avoidable design-time tax.

### Definition source records for §13

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Structural Allocation" -->
**Structural Allocation** — an allocation that exists only because of an architecture runtime mechanism, not because of useful payload.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Zero Mandatory Runtime Tax" -->
**Zero Mandatory Runtime Tax** — the absence of mandatory mediator/reflection/serialization/queue/thread-hop/object-hierarchy overhead in Core Inline semantics; concrete zero-allocation claims require a benchmark.
<!-- pkb:term:end -->


---
## 14. Minimal manifest and source-code organization

### 14.1. Role of the manifest

`BallManifest` is one possible materialized view of a `Ball`'s architectural contract. It supports deployment, review, documentation generation, static checks, and conformance claims, but is neither a mandatory source file nor a runtime registry. A closed typed source declaration may be authoritative; tools SHOULD derive a manifest view instead of making developers repeat the same protocol and policy.

The presence of a manifest field does not prove that the implementation complies with it. For example:

- `singleWriter: true` requires actual scheduler or storage enforcement;
- `maxStructuralAllocations: 0` requires a benchmark;
- `capability: Catalog.Search` requires a concrete restricted client, credential, or broker;
- `noCrossStateReads: true` requires inspection of the dependency graph and code.

At an early stage, the contract MAY be a small YAML or JSON file or a typed declaration in source code. A complete schema package is not part of Core. The authoritative source for each fact must be unambiguous:

```text
owned protocol/state       -> typed Ball source or one manifest
inter-Ball routes          -> Assembly
Flow participation         -> Flow source + participant Application Surface + existing dependency references
shared profiles/limits     -> exact project/binding policy
Ball-specific differences  -> local policy delta
claim evidence             -> claim record
```

An ordinary local Ball needs no standalone manifest when its used closed protocol, state authority, decision entry point, resources, and effective construction/local/policy resolution are statically recoverable from source and enclosing scope. A release or conformance tool may generate the fully resolved view.

`spec.protocols` lists only protocol variants owned by the `Ball` itself when the manifest is the authoritative inventory. For an independently versioned protocol, every non-empty owned category MUST be declared inline or resolved through exactly one explicit, version-pinned authoritative reference. In a same-build typed source, the closed union/type declarations are the inventory and need not be copied. An absent category denotes an empty closed set; absence requires no empty list or zero-valued limit. This rule is the same for `FeatureBall` and `FlowBall`.

Every owned `queries` entry that exists MUST define one `query -> result` pair in which both payload types belong to the same effective protocol identity. The result type is the payload of canonical `ReadResult<ResultPayload>` in §6.3 when the stamped-read trigger applies, not a committed `ReplyOutput` or `ProjectionOutput`. A query with zero or multiple result mappings is invalid.

When a manifest is the authoritative dependency inventory, each non-empty `dependencies.reads` entry imports exactly one target-owned query/result mapping and names the target authority, target read/status authority, effective protocol identity, and caller freshness/consistency requirement. The Assembly route supplies the binding. The caller does not repeat the target types under `spec.protocols`, and optional route fields are present only when triggered. For example:

```yaml
target:
  authority: Pricing
  ownedQueryMapping: { query: Pricing.GetCurrentQuote, result: Pricing.CurrentQuote }
  readAuthority: PricingQuoteAuthority
caller:
  authority: Catalog
  dependencies:
    reads:
      - targetAuthority: Pricing
        query: Pricing.GetCurrentQuote
        result: Pricing.CurrentQuote
        effectiveProtocolIdentity: "same-build:PricingQuoteProtocol"
        readAuthority: PricingQuoteAuthority
        freshnessAndConsistency: selected-target-snapshot-only
```

If Pricing and Catalog can version independently, the dependency replaces the same-build identity with the exact target protocol version selected by Assembly. The caller's declared requirement cannot strengthen the target stamp into a multi-source or later-freshness promise.

When a manifest is the authoritative Flow composition view, each Flow/participant pair appears exactly once as the §10.2 `FlowParticipation` tuple. The participant Application Surface contains only participant-owned public types/entrypoints. Every referenced read, command, or signal dependency remains declared in its existing dependency section and bound by Assembly; the participation view neither duplicates its protocol identity nor becomes a runtime message. In the Checkout example below, enclosing `metadata.id: Checkout` is the manifest identifier for the §16 `CheckoutFlowBall` and resolves `flowAuthority`; `flowOwnedCoordinationRef` resolves the non-empty bounded `ownedCoordination` set; and each row supplies the participant authority, exact Application Surface, and dependency references without repeating those common values.

Imported `ModuleCommand` and `ModuleResult` types belong to the target. The authoritative target source, or its manifest projection, declares exactly one command-to-result mapping for each operation and statically classifies every reachable refusal. For example, the Payment-owned mapping for its accepted status command may project as:

```yaml
protocols:
  commands:
    - command: Payment.GetOperationStatus
      result: Payment.ModuleResult
      refusalClassification:
        ValidationFailure: preAcceptance
        AdmissionFailure: preAcceptance
        Captured: acceptedResult
        DefinitelyNotCaptured: acceptedResult
        StillUnknown: acceptedResult
```

The concrete mapping lists only reachable responses/results; it does not add empty categories. In `dependencies.commands`, a qualified `operation` together with an explicit target protocol version or exact same-build type identity MUST unambiguously import that one target-owned mapping and MUST NOT redeclare either payload as an owned type of the caller or Flow. At an independently versioned boundary, the selected version MUST match `Assembly.consumerProtocolVersion`. Assembly binds the command ingress and result return but has no authority to change the mapping or refusal classification. Internal state variants and operation facets do not become separate manifest variants merely because they are stored in `State`.

An applicable shared guardrail is selected once by its authoritative project/profile/Assembly/binding scope with one exact immutable reference, for example:

```yaml
policy:
  ref: policy:ShopApplication/local-standard@3#sha256:4f923e8b1dbfa6a628f180be90218e9ff9ed5e4a86ad6ff08b3a1c1ad5fe8a77
```

This URI and digest are illustrative syntax, not a policy artifact shipped by Core. A real trigger is unresolved until the consuming project replaces the example with an existing immutable in-scope artifact whose exact bytes/digest and authority can be verified. A policy selects only mechanisms, values, evidence, or permitted deltas that Core leaves to that scope; it cannot suppress a trigger, weaken a law, or act as a waiver.

The referenced artifact contains the complete §0.2 ownership, scope, mechanism/value, enforcement, and evidence metadata. A covered Ball repeats no scope-level reference; it records only a different explicit selection or allowed local delta. Static resolution MUST produce one complete effective contract for every triggered guardrail; the reference is not a runtime lookup.

### 14.2. Minimal Core manifest

This same-build singleton `Inline + Transient + InProcess + Standard` Ball has one fixed-size Intent, fixed-size state, no collection, no semantic output, no external resource, no retry, no lifecycle/status path, and no claim. Typed source/control flow closes the protocol and proves the fixed dimensions; the enclosing binding selects the default profile once, so the Ball needs neither `StateKey` nor a policy row.

```yaml
apiVersion: pokeball.dev/core/v1alpha1
kind: BallManifest
metadata:
  id: Preferences
  ballKind: FeatureBall

spec:
  owns: [preferences]

  protocols:
    intents: [ThemeSelected]
```

The absence of `effects`, `commands`, outputs, retry, profiles, and evidence is meaningful because the closed source inventory proves those paths absent. No `0`, empty optional section, forbidden-capability list, lifecycle enum, or `N/A` explanation is added. The effective contract still has one authority, one writer, bounded input/state/decision work, pure decision, atomic acceptance, and no ambient authority.

Because source and the enclosing binding already carry the authoritative facts, this standalone YAML is optional and may be generated.

### 14.3. Manifest for a Flow

```yaml
apiVersion: pokeball.dev/core/v1alpha1
kind: BallManifest
metadata:
  id: Checkout
  ballKind: FlowBall
  protocolVersion: 1.0.0
  stateSchemaVersion: 2

spec:
  instance:
    stateKey: CheckoutOperationId
    singleWriter: true

  owns:
    - checkout.workflow

  protocols:
    intents: [CheckoutStarted, CancellationRequested]
    controlPulses: [CheckoutCommandDeliveryObserved]
    queries:
      - { query: GetCheckoutStatus, result: CheckoutStatus }
    replies: [RequestAccepted]

  flowParticipations:
    - participantAuthority: Cart
      participantApplicationSurface: [Cart.LockForCheckout, Cart.Unlock]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Cart.LockForCheckout, Cart.Unlock]
    - participantAuthority: Inventory
      participantApplicationSurface: [Inventory.Reserve, Inventory.Release]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Inventory.Reserve, Inventory.Release]
    - participantAuthority: Payment
      participantApplicationSurface: [Payment.Capture, Payment.CancelCapture, Payment.Refund, Payment.GetOperationStatus]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Payment.Capture, Payment.CancelCapture, Payment.Refund, Payment.GetOperationStatus]
    - participantAuthority: Order
      participantApplicationSurface: [Order.Confirm]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Order.Confirm]

  ownedCoordination:
    - lifecycle
    - orderingOrBranchJoin
    - compensationOrRecovery
    - cancellation
    - reconciliation
    - terminalOutcome

  dependencies:
    commands:
      - { operation: Cart.LockForCheckout, targetProtocolVersion: 1.0.0 }
      - { operation: Cart.Unlock, targetProtocolVersion: 1.0.0 }
      - { operation: Inventory.Reserve, targetProtocolVersion: 1.0.0 }
      - { operation: Inventory.Release, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.Capture, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.CancelCapture, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.Refund, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.GetOperationStatus, targetProtocolVersion: 1.0.0 }
      - { operation: Order.Confirm, targetProtocolVersion: 1.0.0 }

  policy:
    ref: policy:ShopApplication/workflow-durable@7#sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
    overrides:
      limits:
        maxStateBytes: 262144
        maxCollectionItems: 256
        maxCommandsPerDecision: 8
        maxFlowParticipants: 4
        maxParallelBranches: 2
        maxOutputsPerDecision: 8
        maxCausalDepth: 16
        maxTransitionSteps: 4096
        maxCompensations: 4
        maxCapturedInputBytes: 65536
        maxRoutesPerFlow: 9
        maxRetainedDispatchStops: 10
```

The inline manifest declares every non-empty Checkout-owned surface used by the example: mutation intents `CheckoutStarted` and `CancellationRequested`, the trusted post-commit control input `CheckoutCommandDeliveryObserved`, the query and result mapping `GetCheckoutStatus -> CheckoutStatus`, and the committed reply payload `RequestAccepted`. `CheckoutStatus` is the sole closed payload in §16.13 for a known, absent, or retention-expired operation; neither a transport 404 nor a second result type replaces it. Customer cancellation passes through Interaction and is not declared as a `ControlPulse`; `CheckoutCommandDeliveryObserved` is accepted only from a declared trusted runtime or route origin after handle and provenance verification. The four `flowParticipations` rows resolve Cart, Inventory, Payment, and Order exactly once against `Checkout.ownedCoordination`; Auth is a supporting context authority and Checkout is the Flow owner, so neither is a participant row. Participant command/result mappings and their static refusal classifications remain imported through exactly nine versioned `dependencies.commands` entries rather than copied into `spec.protocols` or the participation rows; this preserves target ownership and the prohibition on protocol re-export. Under §10.9, Checkout therefore owns thirteen counted declarations: four distinct `FlowParticipation` rows plus nine distinct `DeclaredCommandDependency` rows. A participation reference does not erase the referenced command declaration or create a third copy. The corresponding §14.4 route inventory contains exactly nine effective command round-trip rows; each row binds verified `ModuleCommandPulse` ingress and accepted-frame `ModuleResultPulse` return for one target-owned mapping and counts once, not twice.

The exact `workflow-durable` policy supplies the unchanged shared profile, ingress, retry, delivery, capability, and base limit contracts. Checkout records only its permitted workflow-specific deltas. The displayed `maxTransitionSteps: 4096` is complete only because that exact policy also resolves one immutable `DecisionWorkMeter` identity/version, transition artifact version, unit definition, and overflow policy under §8.3; the numeric delta cannot silently replace the meter. The same policy supplies Checkout's effective numeric `maxStateBytes` and `maxOutputBytesPerDecision` through their dimension-specific immutable `BoundedByteMeasure` tuples, or closes either dimension by a static bounded-type-and-representation proof; the local manifest does not copy those shared contracts. Checkout's closed output algebra, nine-route graph, per-Decision output bound, and causal-depth bound provide the static §10.9 proof for its effective `maxCumulativeFanout`, so no local counter or duplicate numeric row is shown; a binding that cannot retain that exact static proof must instead resolve and enforce the same cumulative branch ceiling through its existing causal reservation. Its closed protocol contains no private `EffectRequest`, so no zero-valued effect limit or `not-applicable` row is required: all work is performed through declared participant commands. Domain-specific `maxRetainedDispatchStops: 10` covers the initial `RequestAccepted` `ReplyOutput` and the nine closed Checkout command-step slots in §16.3, does not exceed the effective `maxCollectionItems`, and does not become a new mandatory Core limit. `stateSchemaVersion: 2` records the retained workflow values added in §16.3; owned and imported protocol variants, dependency versions, and Assembly routes do not change. Migration of already stored state remains a separate rollout or extension contract under §§8.9/10.11: active v1 state must not be silently supplemented with null or default M/S/I/P values; the binding either reconstructs them from declared authoritative migration evidence or disallows normal v2 `decide` and moves the operation to a declared quarantine or manual path.

### 14.4. Assembly declaration

The relationship between a producer and consumer belongs to the composition root—`Assembly`—rather than being hidden in a global locator.

```yaml
assembly: ShopApplication
routes:
  - kind: ReadDependency
    from: Catalog.PricingQuoteRead
    to: Pricing.GetCurrentQuote
    result: Pricing.CurrentQuote
    targetAuthority: Pricing
    readAuthority: PricingQuoteAuthority
    effectiveProtocolIdentity: "same-build:PricingQuoteProtocol"
    callerFreshnessAndConsistency: selected-target-snapshot-only
    binding: InProcess

  - kind: DeclaredCommandDependency
    from: Checkout.CartLockCommand
    to: Cart.LockForCheckout
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.InventoryReservationCommand
    to: Inventory.Reserve
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentCaptureCommand
    to: Payment.Capture
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.OrderConfirmationCommand
    to: Order.Confirm
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentStatusCommand
    to: Payment.GetOperationStatus
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentRefundCommand
    to: Payment.Refund
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.InventoryReleaseCommand
    to: Inventory.Release
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.CartUnlockCommand
    to: Cart.Unlock
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentCancellationCommand
    to: Payment.CancelCapture
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredSignalDependency
    producer: Catalog
    signalType: CatalogSignal.ProductSelectionConfirmed
    consumer: RecommendationsReadModel
    producerProtocolVersion: 2.0.0
    consumerProtocolVersion: 2.0.0
    deliverySemantics: live-duplicate-permitting
    identityPolicy: sourceBallInstanceId+sourceCommitRevision+semanticHandle+sourceOrdinal
    idempotencyOrDedupPolicy: deduplicate-by-identity-within-retention
    orderingScope: CatalogSessionId
    limits:
      maxConsumersPerSignal: 1
      maxObservationBytes: 16384
      maxBufferedOrInFlightObservations: 64
      maxCausalDepth: 4
      maxDeliveryAttempts: 2
```

Assembly is responsible for concrete wiring. The `ReadDependency` row binds Catalog to the exact Pricing-owned mapping and `PricingQuoteAuthority`; it neither makes those types Catalog-owned nor changes `selected-target-snapshot-only` into a multi-source promise. Each command `to` selects the target-owned command/result mapping; the common `resultTo: CheckoutFlowBall.ModuleResultPulse` is a receiving endpoint, not a Checkout-owned result alias. The Pulse's verified `commandSource` and effective protocol identity select the matching Checkout step and imported target payload. A `Ball` is responsible for the required semantic contract. The runtime is responsible for the invocation or delivery mechanism. Neither Assembly nor generated route code may construct causal tokens, result payloads, stamps, status facts, read meaning, or refusal meaning outside the accepted target/read or command frames in §6.

For the Catalog route, Catalog's version-2 owned protocol is the sole authority for `ProductSelectionConfirmed`; Assembly owns only the producer/consumer version pair and delivery binding. The consumer's `2.0.0` route contract accepts that exact producer payload as `ObservedSignal` and does not redefine it. Its one declared consumer counts as one cumulative branch for the accepted Signal source tuple; redelivery under the same route/consumer identity adds no branch, and the preserved causal scope/depth plus closed graph supplies or selects the remaining §10.9 bound.

For a local monolith, a route may compile to a direct function call. For another process, the same route may use IPC. This must not change domain semantics.

### 14.5. Recommended directory structure

```text
features/
  catalog/
    interaction/
      mobile/
      http/
      test/

    nucleus/
      protocol/
      state/
      transition/
      policy/

    resources/
      search/

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

Physical directories are not normative. Dependency direction is normative:

```text
interaction -> own public interaction protocol
resources   -> own private resource protocol
nucleus     -> own state + own protocols + Ball-local Nucleus utilities + exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts + mechanical foundation
assembly    -> public protocols + explicit routes + resolved contract views
```

The Nucleus row permits its own Ball-local utility artifacts and owner-authored public semantic types/entrypoints only. Ball-local and mechanical-Foundation imports remain ordinary compile-time graph edges, not a fifth semantic dependency kind. The row transfers no ownership and excludes foreign mutable State, internals, private Resource adapters, platform/I/O implementations, ownerless shared domain utilities, caller-owned mirrors or redeclarations, and protocol re-export. Interaction and Assembly cannot synthesize the imported contract or business meaning.

An optional helper package is recorded under its owning Ball and logical role even if the physical layout places it elsewhere. Only a shared mechanical helper belongs under `foundation/`; physical reuse or directory placement never erases semantic ownership.

Prohibited directions:

```text
interaction -> resources
resources   -> interaction
nucleus     -> UI / HTTP / SQL / filesystem / platform SDK
feature A   -> feature B internals
feature A   -> mutable state of feature B
```

### 14.6. Interfaces, DI, and code generation

Pokeball does not require an interface for every class, a repository abstraction merely for mocking, or a runtime DI container.

An interface is justified when a real boundary exists:

- multiple production implementations;
- platform boundary;
- process/plugin boundary;
- public library contract;
- independently deployable adapter;
- a test double for an external nondeterministic resource, not for a pure Nucleus.

Constructor injection and compile-time wiring are permitted. A service locator and ambient global client are prohibited for application authority.

Code generation MAY create:

- tagged unions;
- exhaustive dispatch;
- bounded output frames;
- manifest consistency checks;
- state diagrams;
- route maps;
- test fixtures.

The generator must not become a hidden runtime framework. Stabilize the semantic model first, then automate repeatable mechanics.

### 14.7. Foundation Quarantine

<!-- pkb:pba-source:start id="PBA-43" title="Foundation Quarantine" -->
**Source clause for PBA-43 — Foundation Quarantine.**

- **Rule:** When a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, business-policy decision, domain authority, route selection, or hidden communication state. A utility shared across Balls is valid Foundation only under that mechanical-only rule. Domain- or business-semantic helper code remains Ball-local or acquires one Ball/Flow owner and is consumed through its declared Application Surface and protocol; it cannot remain an ownerless shared utility or be relabelled as Foundation.
- **Applicability:** `P`: shared foundation exists or a helper/utility is proposed for use by more than one Ball.
- **Declaration owner:** Project foundation/shared-code owner defines its mechanical-only surface; each Ball/Flow retains its domain semantics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency/ownership/state/call-graph scan for local-versus-shared utilities, policy, authority, routes, service locators, and hidden communication.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One project policy serves all Balls for mechanical Foundation only; domain semantics stay local or Ball/Flow-owned, and the Foundation artifact is omitted when no shared mechanical code exists.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

The shared foundation contains only stable mechanical primitives:

```text
BoundedString / BoundedList
checked numeric operations
Revision / Deadline / Cancellation
SemanticHandle / TraceId
Secret wrapper
validation helpers
pure proof/encoding primitives
small result/error primitives
```

Domain authorities and universal business models do not belong in the foundation:

```text
User
Order
Payment
Cart
Session
Product
CommonResponse
BaseEntity
UniversalDto
```

The foundation may implement pure verification or bounded-container mechanics selected by a trusted binding, but it does not select an approved issuer, interpret permission, choose an Assembly route, retain a domain/status record, or communicate mutable facts between logical roles. Foundation-owned global caches, registries, callbacks, and service locators are invalid when application behavior can observe them as business meaning or a hidden path.

Shared **mechanical** code is extracted when its mechanics, invariants, and change cadence match, not merely when field structure matches. Domain or business semantics do not enter Foundation: they remain as Ball-local implementation artifacts or acquire one Ball/Flow owner and a declared Application Surface/protocol. Small deliberate local duplication is cheaper than an ownerless domain abstraction with high fan-in.

### Definition source records for §14

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Foundation Quarantine" -->
**Foundation Quarantine** — the rule that limits shared foundation to mechanical primitives and prohibits mutable business meaning, policy decisions, domain authority, route selection, service locators, hidden communication state, or an ownerless shared domain/business utility. Domain semantics remain Ball-local or acquire one Ball/Flow owner and a declared Application Surface/protocol.
<!-- pkb:term:end -->


---

## 15. End-to-end example I: catalog search

This example shows a small `FeatureBall` with an `Inline` decision path and an asynchronous resource result. It requires no Flow, broker, or durable workflow engine.

### 15.1. Protocol

The authoritative Catalog contract identities are:

```text
protocolVersion = 2.0.0
stateSchemaVersion = 2
transitionArtifactVersion = 2.0.1
```

```text
CatalogIntent =
    SearchRequested(query, pageSize)
  | SearchCancelled(operationId)
  | ProductSelected(productId)

CatalogQuery =
    GetCatalogView

CatalogSignal =
    ProductSelectionConfirmed(productId)

CatalogFact =
    ProductsFound(searchHandle, generation, products, provenance)
  | ProductSearchFailed(searchHandle, generation, failure, provenance)
  | ProductSearchOutcomeUnknown(searchHandle, generation, provenance)
  | ProductSearchCancelledBeforeStart(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationAcceptedInProgress(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancelled(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationTooLate(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationRejected(cancellationHandle, targetSearchHandle, generation, reason, provenance)
  | ProductSearchCancellationOutcomeUnknown(cancellationHandle, targetSearchHandle, generation, provenance)

CatalogSearchCancellation =
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected(reason)
  | CancellationUnknown

CatalogSearchLifecycle =
    Searching(generation, query, pageSize)
  | Ready(generation, products)
  | Failed(generation, reason)
  | OutcomeUnknown(generation, query, pageSize)
  | Cancelled(generation)

CatalogView =
    CatalogIdle
  | CatalogSearchStatus(operationId, lifecycle, cancellation)

CatalogEffect =
    FindProducts(searchHandle, query, pageSize, mode, deadline)
  | CancelProductSearch(targetSearchHandle)
```

`ProductSelectionConfirmed` is owned by Catalog. It can leave the Ball only as the payload of an accepted `SignalPublication`; the Assembly route in §14.4 cannot define or synthesize it.

The owned query mapping is `GetCatalogView -> CatalogView`. A successful local read maps the exact committed `CatalogState` through the total function below and returns `ReadResult<CatalogView>` with the stamp of that snapshot; it creates neither a `ProjectionOutput` nor a new commit. Search-state `ProjectionOutput` uses the same `CatalogView` payload so the live and queried view cannot select different lifecycle facets.

### 15.2. State

```text
CatalogState =
    Idle(revision)
  | Searching {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Ready {
        operationId
        generation
        query
        products
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Failed {
        operationId
        generation
        reason
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | OutcomeUnknown {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Cancelled {
        operationId
        generation
        cancellation: CancellationAcceptedBeforeStart | CancellationAcceptedInProgress
        revision
}
```

The illustrative `revision` field inside `CatalogState` is a Ball-owned **domain revision** used by Catalog transitions and projections. It is distinct from the acceptor-owned `CommitRevision` in §3.2. A concrete binding may derive or map one from the other only when its declared contract proves their exact equality and preserves both ownership meanings; otherwise they remain separate values and neither is evidence for the other.

`CancellationRejected(reason)` retains the bounded typed rejection reason in committed state. It is not reconstructed from logs or a transient callback. `OutcomeUnknown` retains the current search identity and request fields so a later matching result, cancellation observation, or declared reconciliation decision can refine the same operation without reading runtime history.

Committed `CatalogState` is the sole authority. The version-2 mapping to `CatalogView` is exactly these six cases, with no fallback/default branch and no multiple matching case:

| `CatalogState` source variant | Exact `CatalogView` result |
|---|---|
| `Idle(revision)` | `CatalogIdle` |
| `Searching(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Searching(generation, query, pageSize), cancellation)` |
| `Ready(operationId, generation, ..., products, cancellation, revision)` | `CatalogSearchStatus(operationId, Ready(generation, products), cancellation)` |
| `Failed(operationId, generation, reason, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Failed(generation, reason), cancellation)` |
| `OutcomeUnknown(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, OutcomeUnknown(generation, query, pageSize), cancellation)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `CatalogSearchStatus(operationId, Cancelled(generation), cancellation)` |

Call this pure total mapping `toCatalogView`. Every state-changing Catalog search/cancellation Decision publishes `toCatalogView(nextState)` when its transition below names a projection. The mapping exposes lifecycle, proven result or failure, cancellation status, and cancellation rejection reason together; private pending handles remain state, not public status.

As a worked-example projection of the migration and recovery clauses in §§8.9 and 10.11, persisted schema-v1 state enters normal version-2 `decide` only after authoritative upcast. An authoritative upcaster maps each v1 `Idle`, `Searching`, `Ready`, `Failed`, or `Cancelled` record and its exact fields to the corresponding v2 variant before execution. A v1 `CancellationRejected` record lacks the required reason: it is upcast only when bounded authoritative accepted evidence supplies that exact reason; otherwise the record is quarantined or routed to a declared manual remediation path. No null, empty, generic, or inferred reason is synthesized. Existing retained v1 protocol outputs keep their v1 meaning and are not reinterpreted as v2 `CatalogView` or `CatalogSignal` payloads.

State stores a `SemanticHandle`, not a storage-generated `OutputId`. A runtime ledger may associate:

```text
SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
} -> OutputId(out-8f...)
```

This mechanical association does not change business state.

### 15.3. Interaction: parsing and validation

Raw request:

```text
query = "50%_off' OR 1=1 --"
pageSize = "100"
```

Interaction performs:

```text
UTF-8 validation
NFC normalization according to field contract
SearchText.parse(maxUtf8Bytes = 128)
PageSize.parse(range = 1..100)
request deadline construction
```

The string is valid as literal user text. Special characters are not removed in the name of "security."

Request:

```text
pageSize = "2000000000"
```

is rejected as `BoundaryResponse(ValidationFailure.InvalidPageSize)`. Interaction does not create a `SearchRequested`, `Decision`, `CommitRevision`, or `SemanticHandle`: a boundary response is not a committed `ReplyOutput`. Clamping is permitted only as a separate, explicitly declared product policy.

The trusted boundary passes:

```text
DecisionContext {
    trustedTimeObservation
    reservedSemanticIds = [search-88]
    expiresAt
}
```

`operationId` is neither read from an arbitrary client field nor generated from ambient randomness inside the Nucleus.

For `ProductSelected(productId)`, Interaction validates the product identifier and supplies one reserved selection operation ID, for example `reservedSemanticIds = [selection-501]`. The selection path shown here is actor-independent and uses no configuration or trusted time, so it creates no actor/configuration/time context fields. A project adds actor context only when actor, tenant, issuer, realm, assurance, or delegation can change that selection Decision, in which case PBA-44 applies independently of privilege.

### 15.4. First decision

```text
Idle
+ SearchRequested(query, pageSize)
+ DecisionContext(reservedSemanticIds = [search-88])

searchHandle = SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
}

-> Searching {
     operationId = search-88
     generation = 1
     query
     pageSize
     pendingSearch = searchHandle
     pendingCancellation = none
     cancellation = NotRequested
   }

+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId = search-88
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-searching-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         search-88,
         Searching(generation = 1, query, pageSize),
         NotRequested
     ) # exactly toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = searchHandle
     sourceOrdinal = 1
     payload = FindProducts(
         searchHandle,
         query,
         pageSize,
         mode = LiteralContains,
         deadline = context.expiresAt
     )
   }
```

Runtime checks output bounds and capability binding, accepts the `Decision`, and then dispatches the Effect. If state is persistent, state and the effect outbox are recorded atomically.

`ProductSelected` has one explicit transition from each of the six state variants. These are six closed cases, not a wildcard fallback:

| Source state + `ProductSelected(productId)` | Next state |
|---|---|
| `Idle(revision)` | `Idle(revision + 1)` |
| `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision)` | `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision + 1)` |
| `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision)` | `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision + 1)`; a `CancellationRejected(reason)` facet retains its own typed cancellation reason independently of the search-failure reason. |
| `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `Cancelled(operationId, generation, cancellation, revision + 1)` |

Each of those six Decisions contains exactly this one output and no Projection, Reply, Effect, Command, or Timer:

```text
SignalPublication {
    semanticHandle = SemanticHandle {
        operationId = context.reservedSemanticIds.single # selection-501
        outputKind = Catalog.ProductSelectionConfirmed
        localOrdinalOrName = "selected"
    }
    sourceOrdinal = 0
    payload = ProductSelectionConfirmed(productId)
}
```

The accepted revision advance gives the routed publication one exact committed source revision while preserving every state-specific lifecycle and cancellation fact. The Signal is dispatched only after that state/output frame is accepted. Assembly owns the route binding; Catalog owns the payload and publication Decision.

### 15.5. Resource adapter and safe sink

The adapter receives only the `Catalog.Search` capability. It has no unrestricted database client, shell, or arbitrary HTTP client.

For SQL `LIKE` with literal semantics:

```text
escaped = escapeLikeLiteral(query, '\\')
pattern = "%" + escaped + "%"

SELECT product_id, title, price
FROM product_search
WHERE searchable_text LIKE ? ESCAPE '\\'
ORDER BY rank DESC, product_id ASC
LIMIT ?
```

The following are mandatory:

- parameter binding;
- explicit literal/wildcard semantics;
- read-only credential;
- approved index/query plan policy;
- timeout;
- row and response-byte bounds;
- schema validation of the external result.

Parameterization prevents SQL interpretation. Escaping `%` and `_` defines literal-search semantics specifically. These are separate responsibilities.

### 15.6. Successful result

The Resource produces a validated, provenance-bound `CatalogFact`:

```text
ProductsFound {
    searchHandle = SemanticHandle {
        operationId = search-88
        outputKind = Catalog.FindProducts
        localOrdinalOrName = "generation-1"
    }
    generation = 1
    products = BoundedList(max = 100)
    provenance = CatalogSearchExecutor / attempt-1
}
```

Decision:

```text
Searching(generation = 1, pendingSearch = searchHandle, cancellation)
or OutcomeUnknown(generation = 1, pendingSearch = searchHandle, cancellation)
+ ProductsFound(searchHandle, generation = 1, products, provenance)
+ DecisionContext = Unit

-> Ready(operationId, generation = 1, products, pendingCancellation, cancellation)
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-ready-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         operationId,
         Ready(generation = 1, products),
         cancellation
     ) # exactly toCatalogView(nextState)
  }
```

This result Decision has its own per-Pulse `Unit` context in the shown actor-independent fixture. It does not inherit the initiating `SearchRequested` context's reserved ID, trusted time, or validity fields merely because an Inline binding may execute both causes in one causal scope.

`ProductSearchFailed` follows the same `Searching|OutcomeUnknown -> Failed` refinement and publishes the complete `CatalogSearchStatus(..., Failed(generation, reason), cancellation)`. A proven result or failure refines an earlier `OutcomeUnknown` but preserves the current cancellation facet, including `CancellationRejected(reason)`.

### 15.7. Stale result

The user starts a second search before the first one completes:

```text
first  -> generation 1 / SemanticHandle(search-88, Catalog.FindProducts, "generation-1")
second -> generation 2 / SemanticHandle(search-89, Catalog.FindProducts, "generation-2")
```

The second line abbreviates a separate accepted restart Decision; its full transition block is intentionally omitted. The stale-result trace begins only after that frame has committed generation 2 and its new handle/output batch, so no callback or runtime component changes Catalog state implicitly.

A late `ProductsFound` with the complete handle of the first operation and `generation = 1` does not replace generation 2. The Nucleus:

- ignores the stale result;
- or records a diagnostic outcome;
- or uses only safe cache information under an explicitly declared merge rule.

It does not apply a result according to "the last callback wins."

### 15.8. Cancellation race

`SearchCancelled(receivedOperationId)` is a valid Catalog Intent, but it may target only the current operation. Before constructing a cancellation handle or accepting any cancellation Decision, the Catalog Nucleus requires exact equality:

```text
receivedOperationId
    = state.operationId
    = state.pendingSearch.operationId
```

This guard applies only to `Searching` or `OutcomeUnknown` with a present `pendingSearch`; another source state follows its own closed rejection contract and cannot borrow a handle. If the three values differ, `decide` returns exactly `Rejected(BusinessRejection.StaleSearchOperation(expectedOperationId = state.operationId, receivedOperationId))`. Interaction encodes that actual Nucleus rejection as `BoundaryResponse(DecisionRejected(...))`. The bounded reason contains only the expected and received semantic IDs. The rejection accepts no Decision, State, revision, cancellation handle, Projection, Effect, or output, and it cannot cancel the current search. It is not a stale-result no-op and never combines A's Intent identity with B's `pendingSearch`.

```text
Searching or OutcomeUnknown(
    pendingSearch = searchHandle,
    cancellation = NotRequested
)
+ SearchCancelled(receivedOperationId = operationId)

cancelHandle = SemanticHandle {
    operationId
    outputKind = Catalog.CancelProductSearch
    localOrdinalOrName = "cancel-generation-2"
}

-> same lifecycle and search fields,
   pendingCancellation = cancelHandle,
   cancellation = Requested
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-cancel-requested-generation-2"
     }
     sourceOrdinal = 0
     payload = toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = cancelHandle
     sourceOrdinal = 1
     payload = CancelProductSearch(targetSearchHandle = searchHandle)
  }
```

Every cancellation Fact must match simultaneously on `cancellationHandle`, `targetSearchHandle`, `generation`, and trusted provenance. After matching, the following exhaustive transitions apply:

| Fact | State transition | Subsequent behavior |
|---|---|---|
| `ProductSearchCancelledBeforeStart` | `Searching\|OutcomeUnknown -> Cancelled(cancellation = CancellationAcceptedBeforeStart)`; from `Ready\|Failed` with an accepted terminal search result, an invariant/provenance fault with no new Decision | The terminal Fact proves cancellation before start. A mutually exclusive terminal proof in either order does not overwrite the first accepted state. |
| `ProductSearchCancellationAcceptedInProgress` | From `Searching\|OutcomeUnknown\|Ready\|Failed(cancellation = Requested)`, retain the exact lifecycle/result fields and change only the facet to `CancellationAcceptedInProgress`; a repeat in that facet or a late weaker proof from `Cancelled(CancellationAcceptedInProgress)` is a corroborating no-op | Acceptance does not prove physical stop; a matching result before or after this Fact remains authoritative. |
| `ProductSearchCancelled` | `Searching\|OutcomeUnknown(cancellation = Requested\|CancellationAcceptedInProgress) -> Cancelled(cancellation = CancellationAcceptedInProgress)`; from `Ready\|Failed`, an invariant/provenance fault with no new Decision | The terminal Fact itself proves accepted cancellation even if the separate accepted-in-progress Fact is delayed or lost. |
| `ProductSearchCancellationTooLate` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationTooLate` | An already received or subsequent matching result remains authoritative. |
| `ProductSearchCancellationRejected(reason)` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationRejected(reason)` | The bounded reason is committed and remains in every subsequent `CatalogView`. |
| `ProductSearchCancellationOutcomeUnknown` | Retain the exact `Searching\|OutcomeUnknown\|Ready\|Failed` lifecycle/result fields and set `cancellation = CancellationUnknown` | A matching result is not discarded; reconciliation remains explicit. |

Every accepted state-changing transition in the table creates one `ProjectionOutput` with a complete view handle, `sourceOrdinal = 0`, and payload `toCatalogView(nextState)`. It therefore publishes lifecycle/result and cancellation together. An exact duplicate or specified corroborating no-op creates no duplicate semantic state/output. A conflict path is not a transition: it accepts no Decision, state, or output and follows §8.8.

For one exact cancellation/search lineage, observation order is not a hidden precondition:

```text
ProductSearchCancellationAcceptedInProgress
ProductsFound
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductsFound
-> Ready(cancellation = Requested)
ProductSearchCancellationAcceptedInProgress
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductSearchCancellationAcceptedInProgress
ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)

ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)
late ProductSearchCancellationAcceptedInProgress
-> corroboration; no new semantic state/output
```

`ProductSearchFailed` follows the same compatible-ordering rules. They imply neither AIP-first transport ordering nor unbounded buffering.

Too-late, rejected, and cancellation-unknown observations commute with a legitimate result because each changes only the cancellation facet:

```text
CancellationRejected(reason-R)
ProductsFound(products-P)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))

ProductsFound(products-P)
CancellationRejected(reason-R)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))
```

`CancellationTooLate` and `CancellationUnknown` follow the same two-order rule. A prior search `OutcomeUnknown` may be refined by a later matching `ProductsFound` or `ProductSearchFailed`; that result changes only the lifecycle and retains the latest accepted cancellation facet and rejection reason.

Mutually exclusive terminal proofs have a symmetric contract:

```text
ProductSearchCancelled -> Cancelled
ProductsFound          -> invariant/provenance fault; no Decision; Cancelled remains accepted

ProductsFound          -> Ready
ProductSearchCancelled -> invariant/provenance fault; no Decision; Ready remains accepted
```

`ProductSearchFailed` follows the same terminal-conflict rule. This is neither last-arrival-wins nor an implicit conversion of typed `ProductSearchCancelled` into `CancellationTooLate`. Catalog v2 pins this closed transition family, including stale-cancellation admission, to `transitionArtifactVersion: 2.0.1`; `protocolVersion = 2.0.0`, `stateSchemaVersion = 2`, route versions, and the version-2 migration boundary in §§14.4/15.1–15.2 remain unchanged.

Both permitted ordering traces have the same meaning:

```text
CancellationTooLate -> Searching(cancellation = CancellationTooLate)
ProductsFound       -> Ready(cancellation = CancellationTooLate)

ProductsFound       -> Ready(cancellation = Requested)
CancellationTooLate -> Ready(cancellation = CancellationTooLate)
```

Likewise, a matching `ProductsFound` after `CancellationAcceptedInProgress`, `CancellationRejected(reason)`, or `CancellationUnknown` is accepted and retains the corresponding cancellation facet. A linear `Loading | Cancelled | Ready` enum without an independent facet is insufficient.

### 15.9. Timeout and unknown

Local deadline expiry means that the caller stopped waiting. If the external request might have been accepted, the result:

```text
ProductSearchOutcomeUnknown
```

is not converted into `ProductSearchFailed`. For a current matching search:

```text
Searching(all search fields, cancellation)
+ ProductSearchOutcomeUnknown(searchHandle, generation, provenance)

-> OutcomeUnknown(the same operation/search/pending fields, cancellation)
+ ProjectionOutput(
     sourceOrdinal = 0,
     payload = toCatalogView(nextState)
   )
```

A later matching `ProductsFound` or `ProductSearchFailed` refines `OutcomeUnknown` to `Ready` or `Failed` and preserves the cancellation facet. A later weaker `ProductSearchOutcomeUnknown` received after `Ready`, `Failed`, or proven `Cancelled` is a corroborating no-op with no state or output; it never regresses stronger proof. An exact duplicate in `OutcomeUnknown` is also a no-op.

Repetition is usually safe for a read-only search, but that is a property of this exact Effect contract, not a general timeout rule. Any retry preserves the operation/search handle, remains finite, and satisfies the complete §9.9 retry-ownership rule; this example does not select a retry policy.

### 15.10. What the example demonstrates

- Interaction does not know SQL.
- Resource does not know UI state.
- The Nucleus does not know the database driver.
- User text does not become query code.
- State is protected by generation/handle causality.
- Committed state maps through one six-case total `CatalogView` for both Query and Projection.
- Product selection publishes one producer-owned `CatalogSignal` from every state without losing search lifecycle or cancellation facts.
- Unknown can be refined by proof but cannot regress a proven terminal result.
- Runtime ID is separate from the semantic handle.
- The Inline path requires no mediator or queue.
- The asynchronous adapter can be replaced without changing decision semantics.

---

## 16. End-to-end example II: Checkout Flow

Checkout requires a separate `FlowBall` because it has multiple authorities, an independent terminal outcome, cancellation, compensation, and reconciliation.

### 16.1. Roles and Flow participants

```text
Flow authority:
CheckoutFlowBall

Supporting context authority (not a Flow participant):
AuthBall

Flow participants:
CartBall
InventoryBall
PaymentBall
OrderBall
```

These are six example authorities/roles, but exactly four Flow participants. `CheckoutFlowBall` is the coordination owner and `AuthBall` supplies trusted actor/context support; neither participates in its own or an undeclared Flow relation. The four resolved `FlowParticipation` participant authorities are exactly `Cart`, `Inventory`, `Payment`, and `Order`, with the nine route dependencies listed below.

Local contracts:

```text
Cart.LockForCheckout
Cart.Unlock

Inventory.Reserve
Inventory.Release

Payment.Capture
Payment.CancelCapture
Payment.Refund
Payment.GetOperationStatus

Order.Confirm
```

Each entry resolves through exactly one target-owned version-1 command/result mapping. Every normal business refusal in this Checkout version is an accepted target result, never a pre-acceptance carrier:

| Command | Accepted result path |
|---|---|
| `Cart.LockForCheckout` | Success or domain lock refusal/conflict. |
| `Cart.Unlock` | Success, rejection, failure, or unknown. |
| `Inventory.Reserve` | `InventoryReserved` or `InventoryReservationRejected`. |
| `Inventory.Release` | Success, rejection, failure, or unknown. |
| `Payment.Capture` | Captured, rejected, failed, or outcome-unknown. |
| `Payment.CancelCapture` | Accepted-before-start/in-progress, too-late, rejected, or unknown. |
| `Payment.Refund` | Success, rejection, failure, or unknown. |
| `Payment.GetOperationStatus` | `Captured`, `DefinitelyNotCaptured`, or `StillUnknown`. |
| `Order.Confirm` | Confirmed or definitive rejected, failed, or unknown. |

`InventoryReservationRejected` is always an accepted `Inventory.Reserve` result and never `CommandRejectedBeforeAcceptance`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` is an accepted result of the new `Payment.GetOperationStatus` command that describes the original `Payment.Capture` acceptance facet; it is not pre-acceptance refusal of the status command. A definitive `Order.Confirm` refusal after capture is an accepted Order result. The Flow may later accept its own `RejectedBeforeExternalCommitment`, but that workflow outcome does not copy a carrier reason. Validation/admission failures and any separately enumerated target `DecisionRejected(BusinessRejection)` remain pre-acceptance only under the static versioned target mapping in §§6.9 and 14.1.

`CheckoutFlowBall` knows the sequence and terminal meaning. Participants know only their own invariants and operations.

The owned surface inventory for Checkout `protocolVersion: 1.0.0` is defined by `spec.protocols` in the §14.3 manifest. `CheckoutStarted` and `CancellationRequested` resolve as `Intent`, `CheckoutCommandDeliveryObserved` as a trusted post-commit `ControlPulse`, `GetCheckoutStatus -> CheckoutStatus` as an owned Query/result mapping, and `RequestAccepted` as a committed `Reply` payload. Named participant results, including `CartLocked`, `InventoryReserved`, `PaymentCaptured`, status, and cancellation outcomes, are imported target-owned `ModuleResult` payload variants delivered inside canonical `ModuleResultPulse`; they are not bare Checkout Pulse variants or owned Checkout types.

### 16.2. Ingress and idempotency

The external mutation contains:

```text
CheckoutStarted {
    cartId
    paymentMethodRef
    expectedCartVersion
    idempotencyKey
}
```

For `CheckoutStarted start` and trusted `AuthenticatedActorContext A`, one canonical semantic function applies:

```text
CheckoutStartFingerprintV1(start, A) =
    fingerprintV1(
        stableSubject = ScopedStableSubject(
            issuer = A.issuer,
            namespaceOrRealm = A.namespaceOrRealm,
            stableSubjectId = A.stableSubjectId
        ),
        namespace = A.namespaceOrRealm,
        operationKind = Checkout.Start,
        cartId = start.cartId,
        paymentMethodRef = start.paymentMethodRef,
        expectedCartVersion = start.expectedCartVersion
    )
```

`ScopedStableSubject` makes a lexical subject ID unambiguous within issuer/realm scope under §11.2. `fingerprintV1` receives one labeled typed tuple in the order shown; the concrete binding defines deterministic canonical encoding/digest and an artifact version, but does not change the field set or its sources. Interaction and the Checkout transition use the same `CheckoutStartFingerprintV1`, selected by compatible artifact versions.

`start.idempotencyKey`, `RequestId`, trace ID, retry/attempt data, and the reply channel are not fingerprint operands. They are not added through whole-Pulse hashing or ambient metadata. Interaction stores `IdempotencyScope + start.idempotencyKey + F`, where `F = CheckoutStartFingerprintV1(start, A)`; the accepted Checkout frame must atomically store the same `F` in `CapturedCheckoutInput.source.ingressFingerprint`. The Nucleus receives `start` as the current `Pulse`, `A` as trusted `DecisionContext.actorContext`, and verified `IV` as `DecisionContext.artifactVersion`; `IV` identifies the selected Interaction fingerprint artifact and is distinct from the Nucleus transition artifact. The accepted-input ledger/history is not read as hidden input.

After a Checkout root operation is accepted, same key + same fingerprint within the declared legal retry horizon redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame. The replay preserves the exact Checkout `BallInstanceId`, accepted revision, materialized `OutputId`, reply `semanticHandle`, `sourceOrdinal = 1`, payload/`OperationId`, retained ingress fingerprint, and Interaction artifact version; only the mechanical delivery `AttemptId` changes. It runs no second Checkout `decide`, creates no revision, accepted frame, command, semantic output, or status source, and cannot replace the original accepted actor/payment/artifact binding. The accepted reply frame and idempotency record are retained for at least that retry horizon.

Root validation, admission, or Decision rejection commits no operation/`OperationId` mapping, so a later retry cannot observe or return a rejected root operation and may make a new acceptance attempt under the same idempotency contract. Same key + different fingerprint is detected only by the declared ingress/idempotency authority while its conflicting covered record is retained and before any `CheckoutStarted` Intent is constructed. It returns exactly `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, leaves the prior accepted operation unchanged, and creates no Checkout operation, Decision, revision, handle, Reply, output, or status source. `IdempotencyConflict` is neither `DecisionRejected` nor a separate owned manifest protocol variant.

### 16.3. Flow state

```text
CheckoutState {
    operationId
    phase
    cartSnapshot?: VerifiedStepValue<CheckoutCartSnapshot>
    retained {
        checkoutInput?: CapturedCheckoutInput
        inventoryReservation?: VerifiedStepValue<InventoryReservationRef>
        paymentCapture?: VerifiedStepValue<PaymentRef>
    }
    steps {
        cartLock
        inventoryReservation
        paymentCapture
        paymentCancellation?
        paymentReconciliation?
        orderConfirmation
        inventoryRelease?
        paymentRefund?
        cartUnlock?
    }
    cancellation
    deadline
    terminalOutcome?
}
```

Retained values have the following closed, bounded forms:

```text
CapturedCheckoutInput {
    paymentMethodRef: PaymentMethodRef
    source {
        operationId
        ingressFingerprint
        sourceProtocolVersion
        interactionArtifactVersion
    }
    actorBinding {
        stableSubjectBindingDigest
        issuer
        namespaceOrRealm
        actorContextId?
    }
}

VerifiedStepValue<T> {
    value: T
    authorityActionHandle: SemanticHandle
    observedViaStepHandle: SemanticHandle
    authorityProtocolVersion
    observedViaProtocolVersion
    resultProvenance
}
```

`CapturedCheckoutInput.source` binds M to the current `operationId`, exact output of `CheckoutStartFingerprintV1(start, A)`, Checkout protocol version, and exact verified Interaction fingerprint artifact version `IV` supplied by the current Context; `ingressFingerprint` is byte-for-byte equal to the fingerprint in the atomically accepted Interaction idempotency record. `interactionArtifactVersion` is not `transitionArtifactVersion`. The typed Intent itself has already been created by the verified Interaction boundary, while actor provenance is retained in the field-minimized binding below. No field requires reading the accepted-input ledger after commit. `actorBinding` is a stable-subject digest from the verified initial `AuthenticatedActorContext`, not a credential, session, or unrestricted principal. Optional `actorContextId` is permitted only as a non-authorizing issuer record reference that remains stable for at least the workflow horizon; if the issuer rotates/expires it, a fresh context/grant proves the same subject through digest + issuer + realm. Possession of the binding by itself authorizes nothing. `resultProvenance` is a bounded redacted descriptor/digest of already verified issuer and causal evidence for the result's authority action, not a reference that the `Nucleus` later dereferences in runtime history. Input `sourceProtocolVersion` equals the selected Checkout version; result authority/observation versions come from version-pinned dependencies. These fields are captured workflow values, but they do not make the Flow the authority for the cart, payment method, reservation, or payment: the participant still checks its own target and invariants.

For a direct participant result, `authorityActionHandle`, `observedViaStepHandle`, both protocol-version aliases, and `resultProvenance` derive only from the verified `ModuleResultPulse.commandSource`, `resultSource`, `effectiveProtocolIdentity`, and `issuerProvenance`. For reconciliation, the observation aliases derive from the current status-result Pulse, while the original authority aliases derive from its nested verified accepted-command proof. Checkout does not accept an independently supplied handle/version metadata envelope or let Assembly synthesize these aliases.

This derivation correction changes `transitionArtifactVersion`, but not the public `protocolVersion: 1.0.0`, declared state shape, `stateSchemaVersion: 2`, or routes. A persisted schema-v2 record created by a prior artifact cannot be considered correct based on shape alone: before normal recovery/`decide`, rollout verifies equality of the retained value with authoritative accepted idempotency evidence or performs a declared deterministic migration. Missing evidence or a mismatch leads to a quarantine/manual path; the normal Nucleus receives no ledger read. If a concrete binding cannot distinguish artifacts or safely migrate a record in its persisted form, it increments its own state schema version under §10.11.

While a dependent transition remains reachable, these invariants apply:

- `retained.checkoutInput` appears atomically with acceptance of `CheckoutStarted`; M is retained while a capture decision is reachable, and the actor binding until the last reachable privileged order/cancellation/compensation decision. This closed example shape retains the entire bounded record until the later of the two triggers;
- `cartSnapshot` appears only from a verified result matching `steps.cartLock.handle` and is retained until a terminal order or terminal unlock/reconciliation;
- `retained.inventoryReservation` appears only from a verified result matching `steps.inventoryReservation.handle` and is retained until a terminal order or terminal release/reconciliation;
- `retained.paymentCapture` appears only from a verified direct/reconciled capture result matching the original payment handle and is retained until a terminal order and terminal refund/reconciliation;
- an exact duplicate of one observation is idempotent; another declared direct/reconciliation proof for the same `authorityActionHandle`, authority version, and canonical value is corroboration and creates no output/overwrite, while another value/authority version or a conflicting proof for the same authority action is an invariant/provenance fault;
- a value cannot be deleted merely because a dependent command was emitted: cleanup is permitted after the last reachable consumer and acceptance of all mandatory terminal/compensation outputs under the declared retention policy.

All retained records pass the selected `maxInputBytes` measure, `maxCapturedInputBytes` where applicable, and the selected `maxStateBytes` measure for the complete candidate next frame. Each numeric Core byte dimension resolves its exact §8.3 tuple unless a static type-and-representation proof closes it. Values are not truncated: input `N+1` fails before trusted semantic acceptance/`decide`, and State `N+1` rejects the entire Decision before acceptance. Payment references, actor binding, and provenance are classified as sensitive and do not enter Reply, Projection, status, or an ordinary log/metric/trace; status exposes only a redacted typed lifecycle.

Every step stores semantic data:

```text
Step {
    handle: SemanticHandle
    dispatch: NotDispatched | Dispatched | DispatchStopped
    acceptance: NotAccepted | Accepted | RejectedBeforeAcceptance | AcceptanceUnknown
    outcome: NotExpected | Pending | Succeeded | Rejected | Failed | Cancelled | OutcomeUnknown
    cancellation: NotRequested | Requested | CancellationAcceptedBeforeStart | CancellationAcceptedInProgress | CancellationTooLate | CancellationRejected | CancellationUnknown
}
```

The facets are independent and preserve these invariants:

- `NotDispatched` requires `NotAccepted` and `NotExpected`;
- `AcceptanceUnknown` is permitted only while no verifiable ACK/result/status proof exists;
- a verified `ModuleResultPulse` with accepted-target provenance itself proves target acceptance, advances `NotDispatched -> Dispatched` if the delivery policy has not already stopped, and then advances acceptance to `Accepted`, even if the separate ACK was lost;
- terminal `Succeeded | Rejected | Failed | Cancelled` requires `acceptance = Accepted`;
- `outcome = OutcomeUnknown` does not imply `acceptance = AcceptanceUnknown`: these cases are stored separately;
- a cancellation observation does not delete an already accepted business result.

Post-commit delivery/acceptance observations enter the Flow only as an owned `ControlPulse`:

```text
CheckoutCommandDeliveryObserved {
    source: CausalToken   # Checkout projection of commandSource
    observationId
    observation:
        CommandDispatched(attemptId, deliveryEvidence)
      | CommandAccepted(acceptanceEvidence)
      | CommandRejectedBeforeAcceptance(reason, acceptanceEvidence)
      | CommandDeliveryAmbiguous(attemptId, ambiguityEvidence)
      | CommandDispatchStopped(reason, attempts, lastObservation)
    issuerProvenance
}
```

`CheckoutCommandDeliveryObserved.observation.CommandRejectedBeforeAcceptance` is a set-equal Checkout projection of the canonical carrier in §6.13:

- the enclosing `source` projects the complete field-minimized `commandSource`; its `semanticHandle` selects the Checkout Step;
- the resolved `dependencies.commands` operation and Assembly version pair project `effectiveProtocolIdentity`;
- `reason` projects `boundaryResponse` without reclassification;
- `acceptanceEvidence` projects `targetBoundaryProvenance`.

At the Checkout source, a verified carrier sets exactly `acceptance = RejectedBeforeAcceptance` and `outcome = NotExpected`. A carried `DecisionRejected(BusinessRejection)` is only the informational pre-acceptance reason and never sets `outcome = Rejected`. By contrast, a verified `ModuleResultPulse` whose accepted target result is `Rejected(...)` sets exactly `acceptance = Accepted` and `outcome = Rejected`. Carrier/result evidence for the same effective protocol identity and source tuple is conflicting evidence and fails closed.

For a same-stack participant route, a pre-acceptance target attempt consumes no accepted target level. The Checkout `ControlPulse` Decision that commits this carrier projection consumes the source command's transferred level-1 alternative-completion slot under §8.4; it is not deferred into a new root and does not reset the total causal budget.

If `acceptanceEvidence` is forged, tampered, missing, stale, or bound to the wrong target/version/source tuple, the route constructs no trusted `CheckoutCommandDeliveryObserved`, changes no Step facet, and initiates no compensation. These aliases add or omit no semantic field required by the canonical carrier in this Checkout scope.

The trusted runtime/route boundary creates this Pulse only after source acceptance and verifies that `source.semanticHandle` matches a previously committed Checkout step, that `observationId`/provenance are authentic, and that size and attempts satisfy `maxInputBytes`/`maxDeliveryAttempts`. A source commit by itself leaves a new step in `NotDispatched | NotAccepted | NotExpected`; runtime does not write `CheckoutState` facets directly.

The complete source tuple must resolve to exactly one previously committed `ModuleCommandRequest`; a handle without source commit/ordinal is insufficient. `observationId` is scoped to trusted issuer + source tuple, remains stable when one observation is redelivered, and differs for a new observation; it is a mechanical dedup identity, not a `SemanticHandle` or `AttemptId`. The dedup record is retained for at least the declared duplicate-delivery horizon. Every `reason`/evidence/`lastObservation` is a protocol-versioned bounded redacted value object: a raw transport exception, secret, or unbounded provider payload does not enter the Pulse/status.

The serialized transition applies an observation monotonically:

| Observation | Change to the matching `Step` |
|---|---|
| `CommandDispatched` | `NotDispatched -> Dispatched`; acceptance/outcome are not inferred from send evidence alone. |
| `CommandAccepted` | `NotDispatched -> Dispatched` if dispatch has not already stopped; `NotAccepted \| AcceptanceUnknown -> Accepted`; `NotExpected -> Pending`. |
| `CommandRejectedBeforeAcceptance` | `NotDispatched -> Dispatched` if dispatch has not already stopped; `NotAccepted \| AcceptanceUnknown -> RejectedBeforeAcceptance`; `NotExpected \| Pending \| OutcomeUnknown -> NotExpected`. Accepted/terminal proof conflicts with such an observation and fails closed. |
| `CommandDeliveryAmbiguous` | Permitted only after evidence that the attempt crossed the declared source dispatch point; `NotDispatched -> Dispatched`, and in the absence of acceptance proof, `NotAccepted -> AcceptanceUnknown` and `NotExpected -> Pending`. |
| `CommandDispatchStopped` | Any unfinished dispatch facet advances to `DispatchStopped`; acceptance, outcome, and cancellation are retained, except when verified `lastObservation = may-have-left` without acceptance proof: then `NotAccepted -> AcceptanceUnknown` and `NotExpected -> Pending`. |

The same `observationId` with the same canonical payload is idempotent and creates no new output. The same ID with a different payload, an unknown/stale handle, or incompatible accepted/rejected proofs are invalid trusted input/an invariant fault and are not applied through arrival-order overwrite. A weaker late observation does not regress proven acceptance/result. A late ACK or provenance-bound `ModuleResultPulse` may refine acceptance/outcome after `DispatchStopped`, but the terminal fact that the current delivery policy is exhausted is retained. The current Checkout protocol version does not implicitly reopen a stopped policy.

For example, a result that arrives before the separate ACK is stored as:

```text
dispatch = Dispatched
acceptance = Accepted
outcome = Succeeded
cancellation = CancellationTooLate
```

A result with accepted-command provenance is proof of acceptance; the state `acceptance = AcceptanceUnknown, outcome = Succeeded` is prohibited.

### 16.4. Authorization

`AuthenticatedActorContext` is issued by the trusted boundary. The Nucleus checks policy in the current state and creates target-scoped requirements.

For payment:

```text
AuthorizationGrant {
    actorContextId
    issuer
    audience = PaymentBall
    action = Payment.Capture
    objectRef = checkout operation / payment method reference
    operationId
    constraints {
        amount = exact total
        currency = snapshot.currency
        paymentMethodRef
    }
    expectedObjectVersion = snapshot.quoteVersion
    issuedAt
    expiresAt
}
```

For the initial actor context, the trusted boundary defines a bounded `actorContextId` unambiguously bound to verified `stableSubjectId + issuer + namespaceOrRealm`; the Flow retains only this binding, not the original principal/session. Every later decision that creates a privileged output receives current authorization as a versioned part of `DecisionContext.authorizationSnapshot`:

```text
CheckoutAuthorizationSnapshot {
    actionGrants: BoundedMap<SemanticHandle, CurrentActionAuthorization>
}

CurrentActionAuthorization {
    grant: AuthorizationGrant
    subjectBindingProof
    issuerProvenance
    contextVersion
    observedAt
}
```

The map is bounded by `maxOutputsPerDecision`, keyed by the exact semantic handle, and is not the current cause of the transition. `subjectBindingProof` is bounded and proves that the current grant actor corresponds to the retained stable subject/issuer/realm; it is neither a credential nor authority by itself. The selected security profile and accepted project overlay declare the trusted issuer/issuance-or-reintroduction boundary, authenticity/integrity mechanism, freshness/revocation policy, and action-specific expiry. For capture, the Nucleus matches the grant against retained `actorBinding`, `paymentMethodRef`, current `cartSnapshot.value.total/currency/quoteVersion`, `operationId`, `paymentHandle`, audience/action/object, and trusted `CurrentActionAuthorization.observedAt`; the target repeats the expiry/revocation check at the actual execution time. Refund/Release/Unlock and other privileged compensation each use a separate entry for their own action handle; the capture grant is neither reused nor broadened.

The Payment Execution Gate accepts a grant only from the profile-declared trusted `issuer`, verifies authenticity/integrity, actor context, audience, action, object, operation, and expiry, and then compares every constrained field with the actual `Payment.Capture` payload fail-closed. Changed `amount`, `currency`, `paymentMethodRef`, or expected version does not inherit the original authorization. Missing, expired, stale-version, or mismatched current authorization before commit creates no privileged output and follows the exact capture/compensation transition in §§16.7/16.10; for compensation that cannot be completed, the terminal outcome is `NeedsManualReconciliation`. At source acceptance, the grant must be valid at trusted `observedAt`; the target checks it again during actual execution. After the command output is committed, its payload and grant are immutable: later expiry/rejection is handled through the existing typed rejected/unknown/reconciliation path, not by ambient refresh or hidden grant substitution.

A raw grant is not a field of live `CheckoutState`. The exact grant inside an accepted command output is retained only in the protected committed output record for the dispatch/redelivery and audit horizon; a durable profile retains the same record under its state guarantee. The grant is redacted from status/log/trace and deleted under the declared security-retention policy after that horizon. Exact cryptographic encoding remains a profile/extension detail; semantic binding is mandatory. The Flow does not pass a general unrestricted principal object.

### 16.5. Cart lock and captured snapshot

First decision:

```text
Idle + currentPulse: CheckoutStarted(
    cartId,
    paymentMethodRef = M,
    expectedCartVersion,
    idempotencyKey
)
with DecisionContext(actorContext = A, artifactVersion = IV)

startFingerprint = CheckoutStartFingerprintV1(currentPulse, A)

cartLockHandle = SemanticHandle {
    operationId
    outputKind = Cart.LockForCheckout
    localOrdinalOrName = "cart-lock"
}

-> LockingCart(
       retained.checkoutInput = CapturedCheckoutInput(
           paymentMethodRef = M,
           source = {
               operationId,
               ingressFingerprint = startFingerprint,
               sourceProtocolVersion = Checkout.protocolVersion,
               interactionArtifactVersion = IV
           },
           actorBinding = stableSubjectBinding(A)
       )
   )
+ ModuleCommandRequest {
      semanticHandle = cartLockHandle
      sourceOrdinal = 0
      payload = Cart.LockForCheckout(
          cartId,
          expectedCartVersion
      )
  }
+ ReplyOutput {
      semanticHandle = SemanticHandle {
          operationId
          outputKind = Checkout.RequestAccepted
          localOrdinalOrName = "initial-acceptance"
      }
      sourceOrdinal = 1
      payload = RequestAccepted(operationId)
}
```

Here the complete initial Context is `DecisionContext(actorContext = A, artifactVersion = IV)`. The trusted binding verifies and bounds `IV` as the exact version of the Interaction fingerprint artifact selected for this ingress. `IV` is distinct from the Nucleus `transitionArtifactVersion`; it is the only source for retained `interactionArtifactVersion`, and a missing, stale, mismatched, or untrusted value rejects before acceptance. The Interaction/Policy Gate requires trusted, valid `A`; the explicit operands `currentPulse + A` define `startFingerprint` without implicit Context capture or a ledger read. The idempotency record with the exact `startFingerprint`, `IV`, captured input, and both initial outputs belongs to one authoritative acceptance transaction. Repeating the same key/fingerprint after that acceptance redelivers proof of this exact accepted ordinal-1 `ReplyOutput(RequestAccepted(operationId))` frame with a new `AttemptId`; it does not run the transition, create another output/revision, or replace the captured value with another actor, payment, or Interaction-artifact binding. A pre-acceptance failure creates only its boundary response and no accepted-operation mapping.

The Cart route then follows one canonical round trip. From the accepted Checkout frame, the route derives `cartLockCommandSource` using the committed Checkout instance/revision, `cartLockHandle`, and `sourceOrdinal = 0`; Assembly does not construct that token. The trusted Cart boundary verifies the accepted frame and target mapping and constructs:

```text
cartCommandPulse = ModuleCommandPulse {
    commandSource = cartLockCommandSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    command = Cart.LockForCheckout(cartId, expectedCartVersion)
    issuerProvenance = verifiedCheckoutRoute
}
```

Cart accepts the command only through its canonical Decision. A successful target frame contains its target-owned result:

```text
Accepted(SnapshotDecision {
    nextState = cartStateAfterLock
    outputs = [
        ModuleResultOutput {
            semanticHandle = cartLockCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = cartLockCommandSource
            payload = CartLocked(snapshot)
        }
    ]
})
```

Only after target acceptance does the result route derive `cartLockResultSource` from that Cart frame and construct:

```text
cartResultPulse = ModuleResultPulse {
    commandSource = cartLockCommandSource
    resultSource = cartLockResultSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    result = CartLocked(snapshot)
    issuerProvenance = verifiedCartRoute
}
```

The next Checkout Decision consumes this Pulse. On successful same-stack completion, Checkout reserves the level-1 alternative-completion slot, accepted Cart `decide` consumes it, Cart reserves level 2, and the Checkout result Decision consumes level 2, producing accepted levels `0/1/2`. If Cart is not accepted, Cart consumes no accepted level and the Checkout carrier Decision consumes the same level-1 alternative instead. Only the direct invocation creates the edge `Checkout -> Cart`; neither return branch creates a `Cart -> Checkout` edge. Generated code may erase the transport objects but must prove these same accepted tokens, identities, payload ownership, reservations, and acceptance points.

After target acceptance/result, the Flow receives a field-minimized snapshot:

```text
CheckoutCartSnapshot {
    cartId
    cartVersion
    lineItems: productRef + quantity
    quoteId
    quoteVersion
    total
    currency
    expiresAt
}
```

The snapshot contains no profile history, UI draft, or unrelated cart fields. It is immutable and has provenance/version. Cart remains the authority for its state.

### 16.6. Inventory reservation

```text
LockingCart(
    retained.checkoutInput = C
) + currentPulse: ModuleResultPulse {
      commandSource = cartLockCommandSource,
      resultSource = cartLockResultSource,
      effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0,
      result = CartLocked(snapshot = S),
      issuerProvenance = R_C
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

verifiedCart = VerifiedStepValue(
    value = S,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

inventoryHandle = SemanticHandle {
    operationId
    outputKind = Inventory.Reserve
    localOrdinalOrName = "inventory-reservation"
}

-> ReservingInventory(
       cartSnapshot = verifiedCart,
       retained.checkoutInput = C
   )
+ ModuleCommandRequest {
      semanticHandle = inventoryHandle
      sourceOrdinal = 0
      payload = Inventory.Reserve(
          items = S.lineItems,
          operationId
      )
  }
```

The verified `CartLocked` result Pulse must have `commandSource.semanticHandle = cartLockHandle`, a target-derived `resultSource`, and the version-pinned Cart identity; the provenance-bound snapshot, preserved checkout input, and Inventory output are accepted in one frame. The aliases above derive only from those canonical fields and do not create alternate handle/version authority. Duplicate delivery uses the same command identity/idempotency identity. The Flow does not create a new logical reservation for every transport retry.

### 16.7. Payment capture

After inventory succeeds:

```text
ReservingInventory(
    cartSnapshot = RS,
    retained.checkoutInput = C
) + currentPulse: ModuleResultPulse {
      commandSource = inventoryCommandSource,
      resultSource = inventoryResultSource,
      effectiveProtocolIdentity = Inventory.Reserve@1.0.0,
      result = InventoryReserved(reservationRef = I),
      issuerProvenance = R_I
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

S = RS.value

verifiedInventory = VerifiedStepValue(
    value = I,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

paymentHandle = SemanticHandle {
    operationId
    outputKind = Payment.Capture
    localOrdinalOrName = "payment-capture"
}

captureAuthorization =
    context.authorizationSnapshot.actionGrants[paymentHandle]

-> CapturingPayment(
       cartSnapshot = RS,
       retained.checkoutInput = C,
       retained.inventoryReservation = verifiedInventory
   )
+ ModuleCommandRequest {
      semanticHandle = paymentHandle
      sourceOrdinal = 0
      payload = Payment.Capture(
          amount = S.total,
          currency = S.currency,
          paymentMethodRef = C.paymentMethodRef,
          idempotencyKey = operationId / "capture",
          grant = captureAuthorization.grant
      )
  }
```

Before acceptance, the Nucleus verifies the exact result handle/versions/provenance, equality of the actor/method/amount/currency/version constraints in §16.4, and grant validity at trusted `captureAuthorization.observedAt`; the Execution Gate repeats the expiry/revocation check during actual execution. `verifiedInventory`, next state, and the immutable capture output are accepted in one frame. Neither M, I, nor the grant is read from a prior Intent, participant/runtime ledger, or outbox.

If `captureAuthorization` is missing/expired/stale/mismatched, the current Checkout version does not wait for an ambient refresh or repeat the same result as a new cause. The same Decision retains `verifiedInventory`, creates no `Payment.Capture`, and advances to `Compensating`. Valid current Release/Unlock authorizations from the same context may atomically create the outputs in §16.10; an absent fallback grant is not replaced with a credential and records the residual action as `NeedsManualReconciliation`. After demonstrably successful Release/Unlock, the terminal outcome is `RejectedBeforeExternalCommitment`; an unknown/failed residual follows the ordinary compensation/reconciliation policy. A duplicate of the same `InventoryReserved` does not return the Flow to the capture path afterward.

#### Target-side Payment authorization and result trace

When Checkout has accepted the `Payment.Capture` output above, the route derives `paymentCommandSource` from that exact source frame. The trusted Payment binding boundary verifies the accepted source tuple, effective protocol identity, target-owned command payload, grant/provenance origin, field and byte bounds, and every triggered context validity rule before it constructs:

```text
paymentCommandPulse = ModuleCommandPulse {
    commandSource = paymentCommandSource
    effectiveProtocolIdentity = Payment.Capture@1.0.0
    command = Payment.Capture(
        amount = S.total,
        currency = S.currency,
        paymentMethodRef = C.paymentMethodRef,
        idempotencyKey = operationId / "capture",
        grant = captureAuthorization.grant
    )
    issuerProvenance = verifiedCheckoutRoute
}

paymentContext = DecisionContext {
    actorContext = verifiedFieldMinimizedActorContext
    authorizationSnapshot = VerifiedCaptureAuthorization {
        actionHandle = paymentHandle
        actorBinding = C.actorBinding
        grantBinding = captureAuthorization.grant
        issuerProvenance = captureAuthorization.issuerProvenance
        contextVersion = captureAuthorization.contextVersion
    }
    trustedTimeObservation = captureAuthorization.observedAt
}
```

`PaymentBall` owns the schema and semantic interpretation of `paymentContext`; the binding boundary only verifies and bounds its declared fields. The Payment Nucleus receives `paymentCommandPulse` as the current cause, applies the Policy Gate from Payment State plus that verified context, and is the only component that decides business permission.

On permission and invariant success, Payment accepts the target-owned operation and its immutable provider action in one target frame:

```text
Accepted(SnapshotDecision {
    nextState = paymentState.recordCaptureAccepted(
        commandSource = paymentCommandSource,
        operationId,
        amount = S.total,
        currency = S.currency
    )
    outputs = [
        EffectRequest {
            semanticHandle = paymentHandle
            sourceOrdinal = 0
            payload = CaptureProviderPayment(
                amount = S.total,
                currency = S.currency,
                paymentMethodRef = C.paymentMethodRef,
                providerIdempotencyKey = operationId / "capture",
                authorization = VerifiedCaptureAuthorization(...)
            )
        }
    ]
})
```

Between acceptance of that target frame and acceptance of a later provider-result frame, a redelivery with the same effective protocol identity, `paymentCommandSource`, and command fingerprint receives only verified ACK proof of the original accepted Payment frame and its pending-result state. Payment does not wait for the provider, rerun `decide`, increment revision, create a provisional `ModuleResultOutput`, or repeat `CaptureProviderPayment`. A crash in this interval recovers the accepted frame/pending action according to the selected profile and preserves the same duplicate outcome. Once Payment has accepted a result frame, the same duplicate instead redelivers proof of that exact frame with unchanged `commandSource`, `resultSource`, target revision, result handle/ordinal, and payload; only its delivery `AttemptId` changes. A different fingerprint or conflicting accepted-frame/result evidence fails closed.

This target Decision and revision are already accepted before provider execution. Immediately before executing `CaptureProviderPayment`, the Payment Execution Gate verifies the accepted Effect identity and immutable payload, approved proof issuer/integrity, minimum capture capability, actor/audience/action/object/operation and every constrained field, proof/context/object versions, current freshness/expiry/revocation, provider endpoint, quota, and safe-sink binding. It makes no business choice and cannot replace the accepted action with another payload or ambient credential.

Provider success, known provider failure, ambiguity, or Execution-Gate rejection returns as a closed `Fact` bound to that accepted `EffectRequest`. The Payment Nucleus then accepts the corresponding target-owned state/result frame, for example:

```text
Accepted(SnapshotDecision {
    nextState = paymentState.recordCaptureFailed(
        commandSource = paymentCommandSource,
        reason = ExecutionAuthorizationFailed(reason)
    )
    outputs = [
        ModuleResultOutput {
            semanticHandle = paymentCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = paymentCommandSource
            payload = PaymentCaptureFailed(ExecutionAuthorizationFailed(reason))
        }
    ]
})
```

The same target-owned result shape is used for a successful `PaymentCaptured`, a provider rejection/failure, or `PaymentCaptureOutcomeUnknown` according to the closed mapping in §16.1. The result route derives `resultSource` from this accepted Payment frame and constructs the verified `ModuleResultPulse`; neither Execution Gate nor Assembly constructs a target result.

A Payment business-policy refusal is different from that technical failure. Checkout v1 statically classifies its normal capture refusal as an accepted target result, so the Payment Nucleus accepts:

```text
Accepted(SnapshotDecision {
    nextState = paymentState
    outputs = [
        ModuleResultOutput {
            semanticHandle = paymentCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = paymentCommandSource
            payload = PaymentCaptureRejected(businessReason)
        }
    ]
})
```

The three stages are therefore disjoint:

| Stage | Authority and result |
|---|---|
| Before Payment `decide` | The trusted target boundary may return the statically declared `CommandRejectedBeforeAcceptance` carrier for invalid source/protocol/provenance/context, validation, or admission. It creates no Payment Decision, revision, Effect, or result. |
| Payment Nucleus Policy Gate | A normal business refusal is an accepted target Decision with target-owned `ModuleResultOutput(Rejected(...))`; source facets become `Accepted + Rejected`. |
| After target acceptance | Execution-Gate or provider failure is a Resource `Fact`/status path followed by an accepted target result. It cannot become the pre-acceptance carrier, roll back the accepted Payment operation, or downgrade accepted-result evidence. |

A payment result may arrive:

- after the ACK;
- before the separate ACK, but with accepted-command provenance;
- as `OutcomeUnknown` after a provider timeout.

The Flow does not treat the absence of an ACK as proof of nondelivery if the command might already have been accepted.

### 16.8. Unknown payment outcome

The two unknown cases are not conflated.

If delivery might have left the source but target acceptance has not yet been proven:

```text
payment.dispatch = Dispatched
payment.acceptance = AcceptanceUnknown
payment.outcome = Pending
phase = ReconcilingPaymentAcceptance
```

The cause of this transition is `CheckoutCommandDeliveryObserved(CommandDeliveryAmbiguous)` for the matching `paymentHandle`, not a source commit or direct runtime write. `CommandDispatchStopped` whose retained `lastObservation` also proves "might have left the source" without acceptance proof retains the stop facet and starts the same reconciliation path. The `steps.paymentReconciliation` field ensures that duplicates or multiple ambiguous attempts do not create a second status command with a different semantic identity.

If the target accepted the command and returned accepted-command proof, but the provider outcome was lost:

```text
payment.dispatch = Dispatched
payment.acceptance = Accepted
payment.outcome = OutcomeUnknown
phase = ReconcilingPaymentOutcome
```

The second form cannot degrade back to `AcceptanceUnknown`: target acceptance has already been proven. In both phases, the Flow creates a declared command with a new identity for the status step while preserving the identity of the original capture:

```text
statusHandle = SemanticHandle {
    operationId
    outputKind = Payment.GetOperationStatus
    localOrdinalOrName = "payment-status"
}

ModuleCommandRequest {
    semanticHandle = statusHandle
    sourceOrdinal = 0
    payload = Payment.GetOperationStatus(
        originalPaymentHandle = paymentHandle,
        originalProviderIdempotencyKey = operationId / "capture"
    )
}
```

`Payment.GetOperationStatus` is deliberately a command rather than an ordinary read because Checkout requires a provenance-bound accepted result, stable reconciliation-step identity, idempotent replay, and status evidence. For every new `statusHandle`, Payment accepts a same-state target Decision and increments its target revision:

```text
Accepted(SnapshotDecision {
    nextState = paymentState
    outputs = [
        ModuleResultOutput {
            semanticHandle = statusCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = statusCommandSource
            payload = Captured | DefinitelyNotCaptured | StillUnknown
        }
    ]
})
```

An `EventJournal` Payment binding records the same acceptance as `Accepted(NoDomainChange { outputs = [...] })`. Redelivery of the same effective protocol identity and `commandSource` within the idempotency horizon returns the prior accepted result and target-frame proof without another Payment Decision or revision. A status lookup that needs none of this accepted-operation evidence would instead be a `ReadDependency`/`Query` and would create no target Decision, revision, or output.

As a worked-example reconciliation projection of the source clauses for `PBA-20`, `PBA-21`, `PBA-22`, and `PBA-26`, together with the supporting workflow-sovereignty contract in §10.4, this transition:

- preserves the original capture identity;
- keeps a timeout as an unresolved outcome rather than treating it as permanent failure;
- retains inventory and keeps another checkout unavailable at this point; and
- establishes whether capture occurred before starting a refund, unless the provider contract makes earlier refund initiation safe.

Possible reconciliation results:

```text
Captured(acceptedCommandProof, paymentRef)
DefinitelyNotCaptured(acceptanceEvidence)
StillUnknown(acceptanceFacet, evidence)
```

All three values are accepted `Payment.GetOperationStatus` results. `Captured` immediately records the original capture as `acceptance = Accepted, outcome = Succeeded`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` describes the original `Payment.Capture`, whereas the status command itself has `acceptance = Accepted`; its other form distinguishes an accepted original capture with provider-level `Rejected/Failed`. `StillUnknown` preserves the supplied original-capture acceptance facet and does not collapse `AcceptanceUnknown + Pending` into `Accepted + OutcomeUnknown` without proof.

For Checkout protocol v1, accepted `StillUnknown` exhausts the only automatic payment-reconciliation generation. After verifying the exact `statusHandle`, `commandSource`, `resultSource`, effective Payment protocol identity, supplied original-capture facet, and bounded evidence, Checkout accepts this terminal source Decision:

```text
ReconcilingPaymentAcceptance | ReconcilingPaymentOutcome
+ ModuleResultPulse(
      commandSource = statusCommandSource,
      resultSource = statusResultSource,
      result = StillUnknown(originalAcceptanceFacet, evidence)
  )

-> preserve cart snapshot, inventory reservation, original payment Step facets,
            original capture unknown evidence, and accepted status-result source
   steps.paymentReconciliation = Accepted + Succeeded
   terminalOutcome = NeedsManualReconciliation
   outputs = []
```

The empty output sequence is deliberate: this version creates no second status handle or generation, timer, command, Effect, refund, release, unlock, or automatic reopening. The accepted status result and its bounded evidence remain available under the selected accepted-source/status retention contract for the declared audit/recovery horizon; source durability does not claim provider outcome. Exact redelivery of the same status result is idempotent and creates no new Checkout Decision, revision, handle, or terminal frame. Later stronger proof cannot silently rewrite `NeedsManualReconciliation`; consuming it requires a separately declared manual/recovery artifact with its own authority, input, transition, and evidence. No such artifact is part of Checkout v1.

`Captured(..., paymentRef)` does not leave the reference only inside the reconciliation Pulse: after status-result provenance is verified, it follows the same transition that retains `VerifiedStepValue<PaymentRef>` and creates the Order output as the direct `PaymentCaptured` below.

### 16.9. Order confirmation

After proven capture:

```text
CapturingPayment(
    cartSnapshot = RS,
    retained.checkoutInput = C,
    retained.inventoryReservation = RI
) + currentPulse: ModuleResultPulse {
      commandSource = paymentCommandSource,
      resultSource = paymentResultSource,
      effectiveProtocolIdentity = Payment.Capture@1.0.0,
      result = PaymentCaptured(paymentRef = P),
      issuerProvenance = R_P
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

or

ReconcilingPaymentAcceptance | ReconcilingPaymentOutcome(
    cartSnapshot = RS,
    retained.checkoutInput = C,
    retained.inventoryReservation = RI
) + currentPulse: ModuleResultPulse {
      commandSource = statusCommandSource,
      resultSource = statusResultSource,
      effectiveProtocolIdentity = Payment.GetOperationStatus@1.0.0,
      result = Captured(acceptedCommandProof, paymentRef = P),
      issuerProvenance = R_STATUS
    }

with derived currentResultAliases {
    authorityActionHandle = acceptedCommandProof.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.commandSource.semanticHandle,
    authorityProtocolVersion = acceptedCommandProof.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedResultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    ),
    resultProvenance = normalizeOriginalActionProof(
        acceptedCommandProof,
        observedResultProvenance
    )
}

S = RS.value

verifiedPayment = VerifiedStepValue(
    value = P,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

orderHandle = SemanticHandle {
    operationId
    outputKind = Order.Confirm
    localOrdinalOrName = "order-confirmation"
}

-> ConfirmingOrder(
       cartSnapshot = RS,
       retained.checkoutInput = C,
       retained.inventoryReservation = RI,
       retained.paymentCapture = verifiedPayment
   )
+ ModuleCommandRequest {
      semanticHandle = orderHandle
      sourceOrdinal = 0
      payload = Order.Confirm(
          cartSnapshot = S,
          inventoryReservationRef = RI.value,
          paymentRef = verifiedPayment.value
      )
  }
```

The direct result must have `commandSource.semanticHandle = paymentHandle`; the reconciliation result must have `commandSource.semanticHandle = statusHandle` and demonstrably describe the same original authority action/provider idempotency key through `acceptedCommandProof`. Both paths derive observation handle/version/provenance from the canonical `ModuleResultPulse`; no Checkout alias replaces `commandSource`, `resultSource`, or effective protocol identity. Independent valid proof of the same P through another declared route is corroboration: it creates no second `Order.Confirm` and does not overwrite the accepted value; another P/authority version or a conflicting original-action proof fails closed. `verifiedPayment`, next state, and the Order output are accepted in one frame. The Order target rechecks its own invariants and expected references. The Flow cannot force Order to accept invalid state merely because the preceding steps succeeded.

### 16.10. Compensation

If order confirmation is definitively rejected after payment capture, the Flow creates new operations:

```text
SemanticHandle(operationId, Payment.Refund, "payment-refund")
SemanticHandle(operationId, Inventory.Release, "inventory-release")
SemanticHandle(operationId, Cart.Unlock, "cart-unlock")
```

Target values are derived only from committed Checkout state:

```text
Payment.Refund target
    = {
        paymentRef = state.retained.paymentCapture.value,
        originalPaymentHandle = state.steps.paymentCapture.handle,
        operationId = state.operationId
      }

Inventory.Release target
    = {
        reservationRef = state.retained.inventoryReservation.value,
        originalReservationHandle = state.steps.inventoryReservation.handle,
        operationId = state.operationId
      }

Cart.Unlock target
    = {
        cartId = state.cartSnapshot.value.cartId,
        originalLockHandle = state.steps.cartLock.handle,
        operationId = state.operationId
      }
```

Every compensation command receives a `semanticHandle` from the closed slot shown, an `idempotencyKey` from that handle/operation, a deadline from the retained workflow deadline plus trusted current context, and `CurrentActionAuthorization` for the exact compensation handle. The participant Execution Gate repeats the action/target/operation/expiry checks. If a concrete target contract permits compensation only by original semantic handle instead of P/I, that handle-based target form must be versioned and declared by the participant contract itself; the Flow does not assume it silently. The Capture grant is not used for Refund/Release/Unlock.

Every compensation has:

- a new complete `SemanticHandle` and its own `ModuleCommandRequest`/zero-based `sourceOrdinal`;
- its own idempotency key;
- deadline;
- authorization/capability;
- an outcome and `OutcomeUnknown` path.

The Order rejection/result and retained P/I/cart values are accepted even if compensation authorization is absent; the compensation steps created and the complete available output batch are changed by the same accepted frame. A crash cannot leave a dependent output without its source value or clear a value before its last consumer. Missing/expired compensation authorization does not trigger ambient credential fallback: no action is created, the residual target remains in state, and the terminal outcome becomes `NeedsManualReconciliation`. Compensation is not rollback. It may partially fail.

Terminal outcomes may be:

```text
Completed(orderId)
RejectedBeforeExternalCommitment
FailedCompensated
FailedPartiallyCompensated
NeedsManualReconciliation
CancelledBeforeCommitment
CancelledWithResidualEffects
```

### 16.11. Cancellation race

Customer cancellation passes parsing/authentication/validation in Interaction and arrives during payment as an owned `Intent`:

```text
CancellationRequested(generation = 1)
```

The Flow records the request and sends a declared target command:

```text
paymentCancellationHandle = SemanticHandle {
    operationId
    outputKind = Payment.CancelCapture
    localOrdinalOrName = "payment-cancellation"
}

ModuleCommandRequest {
    semanticHandle = paymentCancellationHandle
    sourceOrdinal = 0
    payload = Payment.CancelCapture(targetHandle = paymentHandle, generation = 1)
}
```

If the target contract treats cancellation as a privileged action, its output uses a separate current grant for `paymentCancellationHandle`; the capture grant is not reused or stored in live state.

One possible sequence:

```text
PaymentCaptured result
CancellationTooLate
```

`PaymentCaptured` contains accepted-command proof, so under the same §16.9 rule the Flow atomically retains provenance-bound `retained.paymentCapture`, records `payment.acceptance = Accepted`, `payment.outcome = Succeeded`, and cancellation `CancellationTooLate`; the result is not discarded because of a local cancel flag. The declared policy then applies: continue the order or create a new `Payment.Refund` handle whose target comes from retained P and whose current grant is keyed by the refund handle. The cancellation branch does not keep P only in the current Pulse.

Another sequence is also possible:

```text
CancellationAcceptedBeforeStart
```

Then payment is not performed, and inventory/cart may be safely released.

### 16.12. Commit and runtime identity

Flow state stores:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
```

The runtime ledger stores:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
    -> OutputId(out-payment-...)
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
    -> OutputId(out-inventory-...)
```

Every source Decision atomically accepts:

```text
next workflow state
new EffectRequest/ModuleCommandRequest outputs
idempotency markers
operation status changes
```

If the current Pulse introduces a decision-relevant M/S/I/P value for the first time, its `CapturedCheckoutInput`/`VerifiedStepValue` and every new output that depends on it belong to the same accepted frame. No crash point can publish an output without the retained source value or retain the value without the complete output batch. The protected output record retains the exact immutable scoped grant with which the output was accepted; live state stores only the actor/value lineage from §§16.3–16.4.

Runtime then dispatches the outputs. This removes the dependency of domain state on commit-time infrastructure IDs.

The dispatch/ACK result is not written back into this commit retroactively. Runtime stores mechanical attempt evidence and returns a typed `CheckoutCommandDeliveryObserved` with the original `SemanticHandle`; only the next serialized Decision may change the corresponding `Step` and create a reconciliation output if needed.

Participant result dispatch is a separate accepted-frame path. After a participant accepts `ModuleResultOutput`, its result route derives `resultSource` from that target frame and attempts delivery of `ModuleResultPulse` to Checkout. A crash before result dispatch does not erase the accepted target result under the selected participant state/output profile. When that result route is retained, retried, or independently observable, the output occupies one participant-owned stop-eligible slot keyed by `(effectiveProtocolIdentity, commandSource, resultSource)`. Exhaustion records target `DispatchStopped`; it neither fabricates Checkout receipt nor sets a Checkout `Step` acceptance/outcome/cancellation/status facet. Checkout remains at its previously proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` state until a verified result Pulse or declared reconciliation proof arrives. A later verified result may refine those facets without erasing the target's retained delivery-stop fact.

### 16.13. Operation status and reply

Initial reply:

```text
ReplyOutput(
    semanticHandle = SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance"),
    sourceOrdinal = 1,
    payload = RequestAccepted(operationId)
)
```

is not a terminal business result. The client obtains terminal status through:

```text
GetCheckoutStatus(operationId)
```

The owned result payload is closed:

```text
CheckoutProgress =
    LockingCart
  | ReservingInventory
  | CapturingPayment
  | ReconcilingPaymentAcceptance
  | ReconcilingPaymentOutcome
  | ConfirmingOrder
  | Compensating

CheckoutCancellationFacet =
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected
  | CancellationUnknown

CheckoutTerminalOutcome =
    Completed(orderId)
  | RejectedBeforeExternalCommitment
  | FailedCompensated
  | FailedPartiallyCompensated
  | NeedsManualReconciliation
  | CancelledBeforeCommitment
  | CancelledWithResidualEffects

CheckoutLifecycle =
    Accepted
  | InProgress(progress: CheckoutProgress)
  | Completed(orderId)
  | Rejected(outcome: RejectedBeforeExternalCommitment)
  | Failed(outcome: FailedCompensated | FailedPartiallyCompensated | NeedsManualReconciliation)
  | Cancelled(outcome: CancelledBeforeCommitment | CancelledWithResidualEffects)
  | OutcomeUnknown(progress: CheckoutProgress)

CheckoutDispatchStop {
    semanticHandle: SemanticHandle
    reason
    attempts
    lastObservation
}

CheckoutDispatchFacet =
    NoDispatchStopped
  | OneOrMoreDispatchStopped(
        stops: BoundedSequence<CheckoutDispatchStop>
    )

CheckoutStatus =
    CheckoutNotFound(operationId)
  | CheckoutExpiredFromStatusRetention(operationId)
  | CheckoutKnownStatus(
        operationId,
        lifecycle: CheckoutLifecycle,
        cancellation: CheckoutCancellationFacet,
        dispatch: CheckoutDispatchFacet
    )
```

`CheckoutKnownStatus` retains independent lifecycle, cancellation, and dispatch facets. `Accepted` means an accepted Flow operation before entry into the first participant phase. `InProgress` and `OutcomeUnknown` retain the current `CheckoutProgress`. Terminal mapping is total: `Completed(orderId)` maps to `Completed`; `RejectedBeforeExternalCommitment` to `Rejected`; the three failure outcomes to `Failed`; and the two cancellation outcomes to `Cancelled`. The §16.8 accepted-`StillUnknown` transition therefore materializes as `Failed(NeedsManualReconciliation)`, not an indefinitely reconciling row. A root validation, admission, or Checkout Decision rejection returns only its typed `BoundaryResponse` and creates no Checkout operation/status record. Nonacceptance of a participant command remains in the matching `Step.acceptance` of an already accepted Flow operation and does not become a root lifecycle.

`NoDispatchStopped` means the empty set. `OneOrMoreDispatchStopped.stops` is nonempty, contains no more than the manifest's `maxRetainedDispatchStops` records, and has a unique complete `semanticHandle`; this tighter bound also satisfies the general `maxCollectionItems`. For the current protocol version, serialization follows the fixed order of ten stop-eligible source-output slots: `cartLock`, `initialAcceptanceReply`, `inventoryReservation`, `paymentCapture`, `paymentCancellation`, `paymentReconciliation`, `orderConfirmation`, `inventoryRelease`, `paymentRefund`, `cartUnlock`; arrival order does not affect the result. `initialAcceptanceReply` means the exact handle `SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance")` from §16.5; the remaining slots correspond to the `steps` fields in §16.3.

These ten slots belong to Checkout source-output delivery. A participant's accepted `ModuleResultOutput` and any target `DispatchStopped` for its return route belong to that participant's separate target delivery/status bounds and do not add an eleventh Checkout source slot or directly change `CheckoutStatus`.

As the exact ten-slot Checkout instantiation of §9.11's pre-acceptance capacity rule, before acceptance of any Decision that creates a stop-eligible `SemanticOutput` for the first time, the source/status capacity plan reserves a record slot for every new unique handle; the initial Decision in §16.5 reserves two slots—`cartLock` and `initialAcceptanceReply`. The number of exposed handles over the operation's lifetime cannot exceed either declared bound; `N+1` rejects the entire Decision before source acceptance and dispatch. After acceptance, a valid terminal observation cannot be lost because of materialization pressure: it remains retained/pending until applied by the status authority, which may honestly lag with its own stamp but may not evict/truncate the record. The status materializer handles a capacity/byte failure as typed backpressure/operational fault with retry within the profile policy, not as grounds to change an already accepted semantic fact.

Checkout instantiates §9.11's monotonic conflict rule as follows. Merging the same terminal observation is idempotent. For the current delivery policy, a nonequivalent second stop record with the same `semanticHandle` is an invariant fault and does not overwrite the first by arrival order; there is no implicit reopening of this policy. A record for another handle is added without deleting existing records. Stopped records are retained until the declared status-retention transition and may coexist with any compatible lifecycle/cancellation state; they do not turn it into `Failed` or `OutcomeUnknown`.

For this query, the declared operation status authority is committed `CheckoutStatusAuthority` with its own revision and one logical writer in an authenticated namespace. It is the Checkout projection of the canonical §9.11 authority; its physical storage/process form remains project/binding-owned. It materializes Flow status changes and trusted runtime delivery observations without becoming the workflow's command authority. Its records transition as follows:

```text
absent + accepted Checkout operation record -> CheckoutKnownStatus
CheckoutKnownStatus + workflow commit -> updated CheckoutKnownStatus
CheckoutKnownStatus + trusted terminal delivery observation
    -> same lifecycle/cancellation + idempotent merge by semanticHandle
delivery observation before its causal accepted/workflow record
    -> bounded pending by operationId/semanticHandle; no lossy status commit
causal record + pending observations
    -> one CheckoutKnownStatus update with canonical stop merge
CheckoutKnownStatus + covered sources + no pending + retention expiry
    -> CheckoutExpiredFromStatusRetention marker
expired marker + observation at/before covered source position
    -> unchanged expired marker
expired marker + marker-horizon expiry -> absent / CheckoutNotFound
```

A delivery observation causally references a previously committed stop-eligible source output. For a command Step, the same runtime fact enters the Flow through `CheckoutCommandDeliveryObserved(CommandDispatchStopped)` and the status authority through its declared typed delivery-observation input. For `initialAcceptanceReply`, only the status-authority path applies: its trusted `ControlPulse` under §6.11 is bound to the exact committed `ReplyOutput` source tuple and does not masquerade as a command Pulse or business result because reply delivery does not change `CheckoutState`. The concrete status-authority protocol/origin binding is recorded in the project overlay.

This transition table is the set-equal Checkout instantiation of §9.11: the materializer either applies sources in causal order or retains an out-of-order observation in bounded pending state; the source record/observation remains retained under the selected profile policy until application. For `Transient`, this promise ends with the process lifetime; durable retention requires a `SnapshotOutbox`/`EventJournal` binding and declared horizon. Per-operation pending stop keys are bounded by `maxRetainedDispatchStops`; the global queue/backpressure cap is defined by the concrete binding. A retention marker may be committed only after declared source positions/horizons covering the operation and with an empty pending set, and it is committed before the known row can become absent. Therefore, an observation at or before covered positions leaves the marker unchanged, a late duplicate does not resurrect an expired operation, and an accepted observation is not lost in a race with retention within the stated profile guarantee.

`GetCheckoutStatus -> CheckoutStatus` returns as `ReadResult<CheckoutStatus>` with the stamp of the exact committed `CheckoutStatusAuthority` snapshot for all three outer variants. Therefore, `NotFound` and expiry do not require an invented snapshot of an absent `CheckoutFlowBall` instance. The stamp does not promise that materialized status has already caught up with every source. A Query is not a `ReplyOutput`/`ProjectionOutput` and creates no new commit. A live/durable reply channel for the concrete profile remains an alternative. Loss of an HTTP connection does not delete an accepted operation under a durable profile.

### 16.14. What the example demonstrates

- A Flow appears because of independent coordination authority, not because of every inter-module call.
- Feature Balls preserve local invariants.
- Each participant target owns one command/result mapping; verified routes preserve accepted source and target causal tuples, while Assembly owns only transport/version binding.
- Captured input is minimal and versioned.
- The Checkout ingress fingerprint has one explicit actor/namespace-scoped derivation from the current Pulse + trusted Context, excludes key/transport metadata, and is retained equal to the atomic Interaction record together with the exact verified Interaction artifact version `IV`, distinct from `transitionArtifactVersion`.
- Values from prior ingress/results needed by a later Decision are retained as bounded provenance-bound workflow values until the last consumer; runtime/participant history is not decision input.
- A privileged output receives a current action-scoped grant through a declared trusted versioned context; a raw grant/principal does not live in Flow state, while the exact committed grant is protected inside the output record.
- ACK, acceptance, result, cancellation, and unknown are not conflated.
- Pre-acceptance carrier reasons remain `RejectedBeforeAcceptance + NotExpected`; all nine normal Checkout business refusals are accepted target results, and accepted rejection remains `Accepted + Rejected`.
- Compensation is new fallible work, not rewind.
- Root idempotency redelivers the original accepted Reply frame for an equivalent retry and classifies a conflicting fingerprint before Intent construction; target idempotency returns accepted-frame ACK while a result is pending and exact accepted-result proof afterward.
- The sole Checkout v1 status reconciliation consumes accepted `StillUnknown` into terminal `NeedsManualReconciliation`; no automatic generation or silent late-proof rewrite is implied.
- Runtime delivery identity is separate from semantic workflow state.
- Post-commit dispatch/ACK/ambiguity changes Flow state only through a declared trusted `ControlPulse`.
- The status authority honestly distinguishes a known, absent, and retention-expired operation, retains all bounded stopped output/step handles, and does not conflate delivery exhaustion with business outcome.
- A reserved root candidate ID that fails validation, admission, or the Checkout Decision creates only its typed `BoundaryResponse`, never a known Checkout status row; participant command nonacceptance remains an accepted operation's Step facet.

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

### 17.3. Resource contract tests

Every adapter that exists is tested separately. The cases below are selected by its reachable resource/risk paths; for example, an adapter with no retry, cancellation, secret, or ambiguous execution path receives none of those fixtures:

- positive capability fixture: a scoped credential, restricted client/broker/role, or capability-rooted OS handle enforces the exact bounded operation class at the real execution boundary;
- negative capability fixture: an administrator credential plus `if (allowed)` or a restricted-looking wrapper over unrestricted authority fails;
- each applicable safe-sink form—parameterized, structured, capability-rooted, and context-encoded—is exercised at its interpreter/dialect edge; a capability-rooted filesystem resolution succeeds while raw traversal fails;
- Execution-Gate verification immediately before authoritative execution covers every triggered proof, capability, constrained payload field, version, freshness/revocation, endpoint, quota, and safe-sink binding without a second business decision;
- an Execution-Gate refusal after accepted work maps to a bound typed `Fact`/target result or status and never to a pre-acceptance carrier;
- response schema validation;
- timeout/cancellation behavior;
- idempotency key propagation;
- retry ownership;
- retry-owner composition: each active failure mode has exactly one primary retry owner; every other retry layer is disabled or has a finite bound and proof of semantic transparency; cumulative SDK × adapter × runtime × Flow attempts are computed, and a `2 × 3 × 4` configuration with three primary-like owners fails;
- response-size and decompression bounds;
- mapping external errors into typed Facts;
- mapping every cancellation executor outcome into a closed `Fact`, target-owned `ModuleResult` carried by `ModuleResultPulse`, or declared trusted `ControlPulse` variant according to origin;
- `OutcomeUnknown` at ambiguous boundaries;
- secret redaction.

### 17.4. Interaction tests

For each present Interaction path, select the reachable cases below. Shared parser/authentication policy is tested once at its declared scope; the Ball tests its typed mapping and any delta:

- parser rejection;
- invalid boundary input returns `BoundaryResponse` but creates no Pulse, Decision, CommitRevision, SemanticHandle, or committed ReplyOutput;
- a malformed value or violation of the selected closed protocol/type invariant returns pre-Intent `ValidationFailure`; the same representation satisfying that type but exceeding a State- or semantic-Context-owned limit reaches `decide` and returns `DecisionRejected(BusinessRejection)`; for example, `items = 101` against a State-owned `maxItemsPerOrder = 100` follows the latter path;
- deliberately moving that fixed constraint into the closed input type requires a new protocol identity and moves the fixture to pre-Intent validation only for that version; changing the stage under the old identity fails compatibility evidence;
- normalization policy;
- duplicate/unknown field behavior where relevant;
- trusted binding construction of bounded, field-minimized `DecisionContext` and actor-dependent `ReadContext`, with approved-issuer or fixed trusted same-stack issuer/realm evidence only when the actor-context trigger exists; the Ball owns semantic schema/interpretation and an actor-independent Decision/read produces no actor artifact;
- Catalog search and product-selection IDs come only from validated `reservedSemanticIds`; selection receives one ID for its single routed Signal handle and no unused actor/configuration/time fields;
- Checkout uses one selected `CheckoutStartFingerprintV1` in Interaction and transition: the trusted initial Context carries its exact verified Interaction artifact version `IV`; after acceptance, same key + same fingerprint redelivers the exact original accepted `ReplyOutput(RequestAccepted(operationId))` frame and original `IV` with only a new `AttemptId`; root rejection before acceptance returns no operation mapping; same key + semantic/actor-scope mismatch under the covered record returns pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))` with no semantic artifact, while key/RequestId/trace/reply changes do not themselves change the fingerprint;
- querying the candidate ID before root acceptance or after root pre-acceptance rejection can return only namespace/absence-proven `NotFound`; after acceptance the same ID selects the one known operation record, and retry never creates both a rejected-root record and an accepted operation;
- external mutation/cancellation after validation creates exactly one declared `Intent`, not a `ControlPulse`; Catalog then rejects `SearchCancelled(A)` before acceptance when current State/pending search belongs to B, while exact identity reaches the existing cancellation transition;
- verified command ingress constructs exactly one `ModuleCommandPulse` from an accepted source frame and the target-owned mapping; malformed payload or unverifiable source/provenance returns the statically classified pre-acceptance response and never creates an accepted target Decision;
- verified result egress constructs `ModuleResultPulse` only from an accepted target `ModuleResultOutput`; Assembly/generated code cannot create or alter either causal token or result payload;
- a successfully evaluated Query response—including any declared denial, redaction, or non-disclosing payload variant—matches the target-owned result mapping, uses `ReadResult`/`ConsistencyStamp` when the stamped-read trigger applies or call-scope identity otherwise, and creates no commit identity;
- operation status returns absent/expired/known cases within the declared closed result payload, not through transport 404, `BoundaryResponse`, or an undeclared second result type;
- output encoding/escaping;
- request and reply correlation;
- absence of raw framework objects inside Nucleus protocol.

### 17.5. Architecture tests

As a conformance and release-evidence projection of the marked source clause for `PBA-39`, CI verifies the applicable subset derived from the closed inventory. A line about Query, status, async, delivery, Flow, persistence, or unsafe authority is absent when that path is absent.

Core-document maintenance has a separate structural gate: it parses exactly one ordered-field source record for each `PBA-01–44`, exactly one marked source for each of the 94 glossary terms, resolves every source and primary §17 route, renders §§20, 20.1, and 22 deterministically, compares generated bytes, and proves a second generation is a no-op. Temporary-copy mutations delete/duplicate/reorder records, fields, and terms; stale a source, anchor, test, Agent route, or generated block; introduce a second source registry or manual definition; and replay the PBA-32/35/36/42, Nucleus-import, accepted-only root-status, root replay/conflict-stage, PBA-10 source-extent, composition-unit, output-byte, target-duplicate, stale-cancellation, `StillUnknown`, cumulative-fan-out, utility-ownership, Checkout Interaction-version, mutation-mode/frame, admitted-read-totality, validation-stage, Runtime/reader-route, and Catalog-domain-revision regressions. Every mutation must fail. This publication-maintenance evidence prevents projection drift but is neither an application runtime nor proof that a consuming project conforms to Core.

If the conformance/release verdict or an accepted ambiguity-resolution decision relies on absence, CI first checks the exact `TriggerAbsenceProof` shape and its bound inventory/digests. Fixtures cover present and absent predicates for `path-triggered`, `risk-triggered`, and `claim-triggered`; attempted use for `always`; contradiction by a present claim; accepted and unaccepted ambiguity resolution; wrong scope/profile/version/revision/digest; missing/stale/unresolved/conflicting evidence; every listed invalidation condition followed by reevaluation; and actor-dependent versus actor-independent Query paths. Ordinary builds with no absence-dependent verdict create no proof placeholder.

For every concrete binding, the evidence set includes a logical-role and boundary-edge map. Physical packages are optional; ownership, verification edges, and permitted calls are not:

| Role or edge | Owned meaning | Required evidence boundary |
|---|---|---|
| Interaction role | raw ingress/egress adaptation, normalization, validation, and context-safe encoding | adapter sites and permitted calls; no policy, State write, or direct Resource business call |
| Trusted Boundary edge | origin/authenticity/integrity/version/validity/bound verification and trusted semantic input/context construction | verifier and constructor sites before Ball interpretation; may be co-located with Interaction/route code but has no business authority |
| Nucleus / Policy Gate | State, protocol/context schemas, business interpretation, permission, ordered Decision/result | pure entrypoints and sole accepted business-decision sites; no I/O or ambient authority |
| Resource / route / Execution Gate | execution of accepted actions; triggered technical verification; validated Fact/result routing | capability/sink/check sites and causal provenance; no new business choice or direct state write |
| Assembly | route, protocol/version pair, binding, and generated wiring selection | static route map; no causal token, payload, refusal, context, or policy synthesis |
| Runtime / acceptor | admission, atomic acceptance, scheduling, delivery observation, and mechanical status transport | accepted-frame/state-write sites and typed ControlPulse routes; no domain mutation outside `decide` |
| Status/read authority | committed query snapshot, closed read mapping, namespace authorization/result selection | stamped/call-scope snapshot source and pure read entrypoint; no command authority or hidden I/O |
| Ball-local utility / shared foundation | local helper belongs to exactly one Ball role; shared code is mechanical only | ownership/import/state scan proving local-role containment or Foundation mechanics and rejecting ownerless shared domain/business meaning, policy, route selection, service locator, or hidden communication state |

The binding's call-graph proof traces the concrete or generated edges corresponding to:

```text
verified origin -> Trusted Boundary verifier/constructor -> Interaction adaptation or route ingress -> Nucleus decide/read
Nucleus Accepted Decision -> acceptor -> Resource/route
accepted EffectRequest -> Execution Gate -> validated Fact -> Nucleus decide
accepted ModuleCommandRequest -> target boundary -> target decide
accepted ModuleResultOutput -> verified result route -> source decide
committed status snapshot -> pure read authority -> typed ReadResult/payload
Assembly/runtime/foundation -X-> business policy or direct Sovereign State mutation
```

A same-file, same-stack, or generated layout may representation-erase adapters only when this role map, call graph, accepted-frame ownership, and provenance trace remain mechanically inspectable. Evidence traces every trusted cause, `DecisionContext`, actor-dependent `ReadContext`, `Fact`, `ModuleCommandPulse`, `ModuleResultPulse`, and delivery `ControlPulse` to its verified origin and rejects undeclared constructors, hidden service-locator/global-state inputs, direct runtime writes, Assembly-created meaning, or foundation-mediated business communication.

```text
no interaction -> resources import
no resources -> interaction import
no nucleus -> platform/I/O import
every concrete/generated implementation symbol maps to Interaction, Nucleus, Resource/route, Assembly, runtime/acceptor, status/read authority or shared-foundation role, with all permitted call edges explicit
Interaction is classified as a logical adapter role and every present Trusted Boundary as an authorized verification/construction edge; physical overlap neither merges the classifications nor transfers Nucleus policy, interpretation, or State authority
same-file and same-stack layouts preserve the role call graph and provenance trace; physical co-location is not accepted as separation evidence
no direct foreign state access
trusted binding boundary is the only constructor of non-empty DecisionContext and actor-dependent ReadContext; every field is verified, bounded and declared by the Ball-owned semantic schema
Inline deque/continuation preserves one trusted per-item Pulse-to-DecisionContext association; root context is not reused, and remaining causal budget/execution quantum/current capacity has no call edge into decide
Nucleus/Policy Gate is the only business-permission and business-result-selection authority; Resource/Execution Gate, Assembly, runtime and foundation cannot choose policy
every downstream output value is present in committed State, current Pulse or a declared field-minimized DecisionContext; no free value or decision read from inbox/outbox/runtime/participant history
every prior-Pulse value needed after a crash is retained with a bounded type and only the correlation, rollout identity, provenance and last-consumer retention required by its actual source/trust/lifetime triggers
every retained ingress fingerprint equals the atomically accepted idempotency-record fingerprint from one declared versioned function over explicit Pulse/Context operands; key and transport metadata are excluded
persisted state-shape change increments stateSchemaVersion without silently changing owned/imported protocolVersion
no feature internals import
no protocol re-export
caller Nucleus may import the exact declared target- or producer-owned Application Surface required by its closed Query/Pulse/Decision contracts without acquiring ownership
Checkout Nucleus imports exactly the Cart, Inventory, Payment, and Order participant-owned Application Surfaces and resolves all nine target-owned command/result mappings
caller-owned redeclaration or structurally identical mirror of an imported type fails
foreign mutable State, Nucleus internals, private Resource adapters, platform/I/O implementations, and protocol re-export fail
Interaction or Assembly synthesis of an imported payload, mapping, refusal, or business meaning fails
no raw external request -> ControlPulse path; every external mutation/cancellation enters through one declared Intent after Interaction validation
every post-commit runtime/route observation that changes Sovereign State resolves to one declared typed ControlPulse, exact previously committed source tuple and trusted provenance, then passes through single-writer decide; source commit and direct runtime state write cannot materialize Dispatched/ACK/ambiguity/DispatchStopped facets
closed protocol exhaustiveness
selected state profile resolves exactly one mutation function and frame: SnapshotDecisionResult + AcceptedSnapshotDecisionFrame or EventDecisionResult + AcceptedEventCommit; Event has no independent nextState and no binding implements a mandatory cross-profile union
representation or declared closed protocol/type invariant violation = pre-Intent ValidationFailure; valid typed value violating a State- or semantic-Context-owned rule = Nucleus BusinessRejection; promoting the rule into the type requires a new protocol identity
Pulse union order = Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse
SemanticOutput union order = ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest
ModuleCommandPulse fields = commandSource + effectiveProtocolIdentity + command + issuerProvenance
ModuleResultOutput fields/invariant = semanticHandle equals commandSource.semanticHandle + target sourceOrdinal + commandSource + payload
ModuleResultPulse fields = commandSource + resultSource + effectiveProtocolIdentity + result + issuerProvenance
the result-delivery key is exactly the unnamed tuple (effectiveProtocolIdentity, commandSource, resultSource)
the canonical pre-acceptance carrier has exactly commandSource + effectiveProtocolIdentity + one closed BoundaryResponse + targetBoundaryProvenance; on a same-stack target-not-accepted branch it creates no accepted target level and its source Decision consumes the transferred level-1 alternative-completion slot
no bare ModuleResult is a Pulse variant; timer firing remains a declared ControlPulse and Catalog SignalPublication/ObservedSignal remain unchanged
every non-empty Ball-owned protocol category resolves each used variant exactly once through an inline declaration or one version-pinned authoritative reference; omitted categories are empty for FeatureBall and FlowBall alike
every routed Signal resolves exactly once in the producer-owned protocol; Assembly owns route/version/delivery binding and cannot define the producer payload
every owned Query resolves exactly one result payload for the same effective protocol identity; triggered stamped reads use canonical ReadResult/ConsistencyStamp, same-stack getters use call-scope identity, and neither creates a Decision, revision, SemanticHandle or SemanticOutput
every ReadDependency resolves exactly one caller + target authority + target-owned Query/result mapping + effective protocol identity + target read/status authority + caller freshness/consistency requirement + Assembly route/binding; caller/Assembly cannot redefine payload, stamp, status fact or read meaning
wrong ReadDependency version/authority/mapping/stamp fails before semantic read and is not NotFound; pre-read failure uses an existing BoundaryResponse, while admitted read returns only the declared ReadResult, whose target-owned payload exhausts every reachable permitted/denied/redacted/non-disclosing outcome selected by the pure Policy Gate, and creates no accepted marker/Decision/revision/handle/output
ReadDependency optional fields follow existing protocol-version, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry and status triggers; same-build/static erasure proves the same contract and Query alone creates no new per-Ball read-limit field
multiple ReadDependency results are independent target snapshots and do not imply one atomic multi-source snapshot without a separate mechanism
every operation-status result payload exhaustively distinguishes only its reachable lifecycle, acceptance, cancellation, ambiguity, delivery-stop and retention variants; any present absence/expiry result is derived from a stamped declared status-authority snapshot
every status namespace resolves one committed revisioned single-writer authority; physical representation is project/binding-owned and creates neither a second command authority nor an unsupported freshness/atomicity claim
Draining rejects new logical mutations but serves every available declared Query/status Query and already-accepted completion/cancellation/status input; unavailable read failure precedes read, admitted read returns only the successfully evaluated ReadResult with a total target-owned payload, and neither creates a Decision
co-located and separate status-authority layouts each have exactly one query writer and preserve underlying command/business-fact ownership; materialization lag is reflected only by the status stamp
every status source/observation applies in declared causal order or bounded pending; equivalent duplicate is idempotent, compatible facets merge losslessly, weaker evidence does not regress, and nonequivalent same-key evidence fails closed
every reachable operation/pending/lifecycle/cancellation/result/marker/stop capacity is finite and reserved before its source acceptance; N+1 prevents acceptance, while accepted evidence is never evicted, truncated, silently dropped or rolled back
every retention marker commits only after declared source positions/horizons cover the operation and pending is empty; marker precedes absence, covered late evidence cannot resurrect the operation, and marker-horizon expiry alone permits later NotFound
accepted target ModuleResultOutput remains a target status fact across crash-before-result-dispatch; target DispatchStopped preserves the exact result-delivery tuple while source status stays Pending/Unknown until verified result/reconciliation
Catalog protocolVersion = 2.0.0, stateSchemaVersion = 2, transitionArtifactVersion = 2.0.1, and the ProductSelectionConfirmed producer/consumer route pair = 2.0.0/2.0.0
CatalogState revision is a Ball-owned domain revision distinct from acceptor-owned CommitRevision unless the binding proves exact equality while preserving both ownership meanings
Catalog ProductSelected transitions are set-equal to {Idle, Searching, Ready, Failed, OutcomeUnknown, Cancelled}; each preserves its state-specific fields/facets and emits exactly one sourceOrdinal-0 ProductSelectionConfirmed SignalPublication
CatalogState-to-CatalogView mappings are set-equal to the same six states, one case each, with no fallback/default or multiple case; GetCatalogView and every search-state Projection use that mapping
Catalog v1 persisted state enters v2 decide only through authoritative upcast; missing rejection-reason or other required evidence routes to quarantine/manual remediation and never to an invented default
for Checkout, the stopped-handle universe includes the initial RequestAccepted ReplyOutput and all nine command Step handles; cap/order/reservation/materialization and 10/11 tests cover the same exact set
every imported ModuleCommand/ModuleResult resolves through exactly one target-owned command-to-result mapping and declared dependency whose effective protocol identity matches Assembly; refusal classification is static for that version, explicit producer/consumer versions are required only across independent versioning/deployment, and imported target types are not redeclared as caller-owned
every command Assembly route binds verified target ingress and accepted-frame result return while synthesizing/modifying no commandSource, resultSource, target-owned payload, or refusal meaning; same-stack erasure proves the same accepted tuples
every read-like operation requiring accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation uses the command bridge; an ordinary non-recording read uses Query/ReadDependency and creates no target Decision/revision/output
same-stack command success has one source-to-target Direct Control Dependency, no reverse return edge, accepted levels 0/1/2, one level-1 alternative-completion reservation, and a separate level-2 result reservation; every pre-acceptance target-not-accepted branch consumes the transferred level-1 alternative in the source carrier Decision, never in an accepted target, while async handoff preserves causal scope/depth/budget without inferring the transfer
CheckoutCommandDeliveryObserved carrier aliases are set-equal to commandSource, effectiveProtocolIdentity, boundaryResponse, and targetBoundaryProvenance; all nine normal Checkout business refusals are accepted results
every example output maps to the canonical envelope/payload algebra; no orphan EffectIntent/ModuleCommandIntent/CommandId
compile-time import and Direct Control Dependency graphs are independently and unconditionally acyclic; generated inline dispatch before async handoff/yield is included
a WaiverRecord on either direct cycle records deliberate non-conformance and cannot make the architecture test pass
async feedback after a bounded handoff/yield creates no direct-control edge and is tested separately for owner, identity, finite budget, escape condition and fan-out protection
every inter-Ball edge has ReadDependency, DeclaredCommandDependency, DeclaredSignalDependency or FlowParticipation
every physical helper import has one owner/classification: Ball-local to exactly one logical role or shared mechanical Foundation; ownerless shared domain/business utilities fail, and Ball-owned shared semantics use a declared Application Surface/protocol rather than a fifth dependency kind
every physical import remains in the acyclic compile-time graph; a utility edge enters Direct Control Dependency only through another Ball's Application Surface or synchronous cross-authority control
every FlowParticipation resolves exactly one Flow authority + participant authority + participant Application Surface + non-empty bounded Flow-owned coordination set + bounded references to existing read/command/signal dependencies; each reference preserves its existing owner, effective identity, route, return binding, limits, and Assembly binding
FlowParticipation creates no protocol variant, runtime envelope, fifth route, target-type copy, participant-State authority, or duplicate dependency; Checkout has exactly four Flow participants and nine existing command routes although six authorities/roles appear in the example
every Application Surface contains only the owning Ball's deliberate public semantic types/entrypoints and excludes mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, and re-exported foreign contracts
every produced command/read/effect has a declared route or private capability binding
every boundary choice passes its §4.4 positive evidence and falsifier; Core accepts multiple decompositions only when each independently proves the same authority/invariant/lifecycle/trust/dependency rules
decision-relevant UI/transport value is committed State or explicit trusted current Pulse/DecisionContext; EphemeralState contains only non-decision mechanics
Flow exists exactly when it owns material lifecycle/ordering/branch-join/compensation-recovery-cancellation/reconciliation/terminal-outcome coordination; call count and one hop alone do not qualify
every present variable dimension resolves to one finite effective bound through static proof, an exact reusable policy, or a local declaration/delta; absent dimensions need no zero or N/A row
when maxCumulativeFanout is present, one root causal scope sums distinct accepted source-output-to-effective-route/consumer traversals across levels; terminal and converging route branches count, co-reachable branches sum, mutually exclusive alternatives share their maximum reservation, duplicate/redelivery does not increment, async handoff preserves scope, and exact N/N+1 rejects the whole over-limit Decision
every numeric maxTransitionSteps resolves one immutable versioned Decision Work Meter; equal canonical inputs under the same binding/transition artifact version/meter identity/version consume the same non-negative integral total, one decide scope is monotonic and cannot reset, exact N may complete, N+1 accepts no frame/state/revision/output/dispatch, and unlike meter identity/version, transition artifact version, or unit-definition tuples are not compared
every concrete fallible-admission profile/binding exposes one finite closed AdmissionFailure.reason union; unknown discriminator/open string fails before trusted construction, and a non-fallible profile has no empty union
every §6.13 error maps from its exact stage to only its legal carrier/result/status effect; later failure cannot rewrite validation/admission/pre-acceptance rejection/accepted result/post-acceptance Resource evidence/delivery stop/programming fault into another stage
candidate OperationId reservation followed by root validation/admission/Decision rejection creates only BoundaryResponse and no accepted operation, known status row, marker, handle, output, or reply; covered lookup may return only NotFound, while participant refusal remains an accepted source operation's step facet
root same-key/same-fingerprint retry redelivers the exact accepted ReplyOutput(RequestAccepted) source frame with only a new AttemptId; same-key/different-fingerprint returns pre-Intent ValidationFailure(IdempotencyConflict) and creates no semantic artifact
target duplicate before accepted result returns only verified ACK proof of the original target frame/pending result; after result it redelivers the exact accepted result frame, and neither path re-decides or re-executes
maxDeclaredDependenciesPerBall unit = one distinct owner-declared ReadDependency | DeclaredCommandDependency | DeclaredSignalDependency | FlowParticipation row; dependencyRefs preserve both counts and aliases/duplicates fail
maxRoutesPerFlow unit = one distinct effective Flow command/result round-trip mapping; ingress+return = one, participation/reference = zero, and Checkout = nine
maxInputBytes unit = one exact raw-or-normalized candidate-input representation with declared boundary-metadata/Context inclusion; maxStateBytes unit = complete candidate nextState semantic representation; maxOutputBytesPerDecision unit = complete ordered Decision.outputs semantic representation; one measure tuple fixes each N/N+1 and later storage/transport mechanics are excluded
Catalog stale SearchCancelled operationId mismatch accepts no Decision/revision/handle/projection/effect; protocolVersion 2.0.0 + stateSchemaVersion 2 + transitionArtifactVersion 2.0.1 remain distinct
Checkout StillUnknown on its sole status slot terminates normal v1 at NeedsManualReconciliation with outputs = [] and no implicit reopening
every policy reference is exact, acyclic, in scope, current for its selected revision, and conflict-free; wrong-version/profile/binding/environment and unauthorized overrides fail
policy references and WaiverRecords cannot suppress an inferred trigger, weaken a law, or convert a MUST/MUST NOT violation into conformance
every absence-dependent conformance/release verdict or accepted ambiguity-resolution decision has one exact TriggerAbsenceProof bound to class/anchor/scope/profile/inventory/digests/predicate/owner/invalidation; always and present triggers reject the proof, and invalidation blocks reliance until reevaluation
ordinary design/adoption and a verdict not relying on absence materialize no TriggerAbsenceProof or placeholder
adoption/pilot worksheet and selected workload/method/baseline/continue-reshape-stop thresholds are project-owned guidance only while that work exists; Core contributes no universal threshold, and negative-adoption fixtures create neither empty Balls nor conformance placeholders
the authoritative §0.1 Core-documentation rule is projected exactly: every new or changed Mermaid diagram in Core carries a local legend for semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows; omission blocks that documentation change, not an unadopted consuming-project binding
when actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection, PBA-44 resolves approved-issuer authenticity/integrity or fixed trusted same-stack issuer/realm proof even for typed inter-Ball input; actor-independent Decision/read paths resolve no actor artifact
Payment trace separates pre-decide carrier, accepted Nucleus business refusal, and post-acceptance Execution-Gate/Resource failure; the last two remain accepted target result paths and cannot be downgraded
Checkout initial Context explicitly supplies verified Interaction fingerprint artifact version IV, retained interactionArtifactVersion equals IV across retry/recovery, missing or mismatched IV fails, and transitionArtifactVersion remains distinct
every accepted action reaches its Resource/target Execution Gate immediately before execution with the triggered proof + capability + constraints + version + freshness/revocation + endpoint + quota + sink checks and no second business decision
no hidden cause/context/result constructor, direct runtime State write, Assembly-created semantic meaning, global service locator, mutable foundation communication, or route-selected foundation policy
the §14.2 minimal Inline fixture resolves only always-applicable obligations and contains no absent-path placeholders
positive and negative fixtures for path-, risk-, and claim-triggered rules respectively activate the guardrail and reject the same reachable trigger when no effective guardrail/evidence resolves
two Balls can resolve one exact shared policy without copying it; a local delta changes only an explicitly overridable field and leaves all other effective values equal
unsafe effect registry
foundation domain-type quarantine
```

An import linter result does not prove semantic ownership or security isolation. It proves only the verifiable structural property.

### 17.6. Concurrent profile tests

For `BoundedConcurrent`, add the cases whose subpaths exist:

- out-of-order `Fact`/`ModuleResultPulse` inputs when completion can reorder;
- mailbox full/backpressure returns a closed `BoundaryResponse(AdmissionFailure(reason))` before acceptance;
- concurrent duplicate ingress when duplication is possible;
- concurrent same-identity target duplicate in both accepted-command/pending-result and accepted-result states, with one accepted target frame/result and only ACK/exact-frame replay;
- cancel versus completion when cancellation exists;
- timeout versus late result when deadlines/ambiguity exist;
- worker crash when workers exist;
- bounded causal chain when completion can re-enter a Decision;
- starvation and priority behavior, if claimed.

For `Transient` with an output-bearing path, fault injection verifies both sides of the single publication point:

- failure during reservation/materialization before publication makes neither state nor any output visible;
- failure immediately after atomic publication but before the first dispatch leaves the complete retained `AcceptedSnapshotDecisionFrame` or `AcceptedEventCommit` visible, from which the entire undelivered batch can continue under the profile policy;
- failure between dispatch of two outputs neither rolls back state/the first output nor loses the retained record of the second;
- no separate fallible state-publication-then-enqueue window exists.

### 17.7. Durable-state profile tests

For `SnapshotOutbox`/`EventJournal`, select crash points from the paths that exist:

```text
before transaction
inside transaction
commit acknowledged but response lost
root accepted ReplyOutput committed but response lost     # exact root replay path
before dispatch                                      # output path
request sent but ACK lost                            # ACK path
after target acceptance before source update         # detached result path
after target acceptance before target result exists  # pending-result duplicate ACK path
after accepted ModuleResultOutput before result dispatch # target result path
target result delivery exhausted                     # target stop tuple path
post-commit observation before/after ControlPulse    # delivery-observation path
before/after result commit                           # result path
recovery with pending outbox                         # output path
recovery after accepted NoDomainChange               # EventJournal path
delivery exhaustion persisted as DispatchStopped     # terminal delivery/status path
initial ReplyOutput exhaustion in status authority   # detached reply/status path
```

When a durable operation-status materializer exists, fault/race tests also cover an observation before its causal source, crash with bounded pending before/after source application, duplicate and conflicting same-key evidence, independent lifecycle/cancellation/result/stop facet orders, `N/N+1` reservation before source acceptance, pressure after acceptance with no eviction/truncation, marker attempted while a source position lags or pending is nonempty, committed marker before row absence, covered late duplicate, marker-horizon expiry, and no resurrection. A reserved candidate ID whose root validation/admission/Decision rejected has no accepted status source to recover and cannot be synthesized into a known row or retention marker. Crash after accepted target `ModuleResultOutput` but before result dispatch preserves the target accepted-result source; target route exhaustion adds target `DispatchStopped`, while source remains Pending/Unknown until verified result or reconciliation.

Every delivery, durability, recovery, receipt, acceptance, once-only, RPO, or RTO claim fixture names its exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees. Paired negatives prove that source durability alone and retained pending work alone establish neither target receipt/acceptance nor any stronger downstream outcome.

The canonical Checkout example additionally tests:

```text
Checkout recovery after accepted CheckoutStarted/CartLocked before InventoryReserved
Checkout recovery after accepted CheckoutStarted preserves exact equality of retained ingress fingerprint and atomic idempotency record
Checkout recovery after accepted InventoryReserved before PaymentCaptured
Checkout recovery after direct or reconciled PaymentCaptured before Order result
Checkout recovery after Order rejection before/between Refund, Release and Unlock
Checkout recovery after accepted StillUnknown preserves the terminal NeedsManualReconciliation frame and creates no new status generation
```

The canonical Catalog v2 example additionally tests persisted-state rollout:

```text
each v1 Idle/Searching/Ready/Failed/Cancelled variant -> exact authoritative v2 upcast
v1 CancellationRejected + authoritative bounded reason evidence -> v2 CancellationRejected(reason)
v1 CancellationRejected without that evidence -> quarantine/manual remediation
pending v1 output or Fact retains its v1 protocol/artifact meaning and is not decoded as v2
no schema-v1 record enters normal v2 decide and no missing field receives a null/default substitute
```

Minimum properties are likewise triggered. The Checkout-specific bullets below are evidence for §16, not a template for an unrelated durable Ball:

- no dispatch before source commit when outputs exist;
- no state without its corresponding durable output record when outputs exist;
- no outbox regeneration by rerunning transition when an outbox exists;
- accepted `NoDomainChange` restores the same revision/acceptance marker/output batch when EventJournal is selected;
- crash-before-dispatch and delivery-observation rules apply when those output/ACK/ambiguity paths exist;
- crash after accepted target `ModuleResultOutput` preserves that target frame under the selected profile; replay dispatch uses the same result-delivery tuple and never reconstructs business meaning in Assembly;
- crash after target command-frame acceptance but before result creation preserves one pending target operation; a same-identity duplicate returns verified ACK proof only and never reruns the target Decision/Effect, while a later accepted result is replayed from its exact frame;
- target result-route exhaustion records `DispatchStopped` with `(effectiveProtocolIdentity, commandSource, resultSource)` and leaves Checkout Pending/Unknown until a verified result or reconciliation; a late result may refine Checkout without erasing the target stop;
- target output-byte/count and conditional stop-slot `N/N+1` admission are tested independently of Checkout's ten source-output stop slots;
- the initial reply stop and command-step stops are materialized over one bounded `SemanticHandle` universe; a reply observation does not masquerade as a command Pulse and does not change `CheckoutState`;
- status materialization does not lose a stop that arrived before its causal workflow record and does not replace a retention marker with a late duplicate after the covered source position;
- SnapshotOutbox/EventJournal restore exact retained M/S/I/P values and provenance without rerunning `decide` or reading runtime/participant history; Transient explicitly does not promise this after process loss;
- after a crash, `InventoryReserved(I)` creates the exact Capture from retained M + S + a current valid action grant, direct/reconciled `PaymentCaptured(P)` creates the exact Confirm from retained S/I + P, and order rejection creates Refund/Release/Unlock only for retained P/I/cart;
- the same authority action with a conflicting value/version/proof, premature cleanup, and wrong-target compensation fail closed; cleanup after the last terminal consumer survives repeated recovery;
- the complete retained C/RS/RI/RP frame satisfies exact `maxStateBytes = N` under one State-dimension measure tuple; a semantically equal alternate retained representation maps to the same measured sequence, while `N+1` rejects the entire Decision before State/revision/output acceptance without truncation;
- active Checkout v1 state without authoritative M/S/I/P migration evidence receives no synthesized defaults and does not enter normal v2 `decide`; the binding proves migration or quarantine/manual routing;
- a schema-v2 Checkout record from a prior transition artifact does not enter normal recovery based only on matching shape: the retained ingress fingerprint is verified against the authoritative accepted record/declared migration, otherwise the operation is quarantined; the normal Nucleus does not read the ledger;
- duplicate root ingress after accepted reply loss redelivers the exact original accepted `ReplyOutput` frame with a new `AttemptId`; the idempotency record and frame share the declared retry horizon, and a fingerprint conflict remains a pre-Intent validation failure with no semantic record;
- accepted Checkout `StillUnknown` recovery restores the same terminal `NeedsManualReconciliation`, original capture/status evidence, and empty output sequence; duplicate delivery creates no second Decision/handle and normal v1 cannot auto-reopen it;
- a stale owner cannot commit when ownership is movable;
- ambiguous external outcome remains unknown until reconciled when that risk exists.

### 17.8. Security tests

For each present actor, privileged, capability, interpreter, secret, or abuse trigger, select only the applicable tests below. The Capture/Cancel/Refund/Release/Unlock entries are Checkout example evidence only:

- actor context from a valid approved issuer with verified authenticity/integrity becomes trusted Decision input but grants no authority by itself;
- valid actor-dependent Query/status context from an approved issuer selects only the namespace/result permitted by the Ball's pure semantic read; typed cross-Ball and statically proven same-stack forms are equivalent;
- a secured Query fixture admits the same valid approved-issuer actor context into each reachable target-owned `ResultPayload` case: permitted data, explicit denial, redaction, and a deliberately declared non-disclosing variant; the pure Policy Gate selects the case and every admitted execution returns `ReadResult` without a Decision, revision, output, exception, `BusinessRejection`, or post-admission `BoundaryResponse`;
- a wrong-provenance, wrong-issuer, malformed, or inadmissible secured Query fails at the declared pre-read boundary and is never converted into semantic `NotFound`; a valid-context denial may be non-disclosing only when that exact target-owned payload variant is declared, and neither the boundary, caller, nor Assembly may invent it;
- removing any reachable permitted/denied/redacted/non-disclosing payload variant fails closed-protocol exhaustiveness, while an actor-independent or unconditionally permitted Query contains no unreachable denial placeholder;
- forged/tampered actor evidence, wrong or unapproved issuer/realm, and missing, stale, or unverifiable required evidence fail closed before any actor-dependent Decision or Query/status result, including a typed unprivileged inter-Ball command/read;
- an otherwise equivalent actor-independent Decision or read creates no actor context, issuer row, authentication artifact, or actor-specific test fixture;
- forged context/proof, wrong target/action/object/operation/constrained field/version, and missing capability fail at the owning trusted boundary or Execution Gate without an ambient credential or new business decision;
- a Payment Nucleus business-policy rejection is an accepted target result, while a post-acceptance Execution-Gate rejection is a bound technical Fact/accepted result and neither is the pre-acceptance carrier;
- same-stack/generated erasure preserves the exact verified context, accepted cause/result provenance, gate ordering, and role call graph;
- hidden service locators, mutable global business state, foundation policy/route selection, and foundation-mediated role communication fail architecture/security scans;
- missing/expired/wrong-audience grant;
- forged/tampered grant and untrusted/wrong issuer;
- substitution of amount/currency/object/operation or another constrained command field after authorization;
- a constrained, duplicate-permitting privileged action binds every action-contract constrained field and the applicable idempotency identity at the Execution Gate; a mismatch or conflicting same-identity payload fails closed;
- a privileged cross-authority Grant whose action contract defines no constrained target or payload field and whose execution cannot duplicate still verifies approved issuer, authenticity/integrity, actor context, audience, action, object, operation, and validity, but omits `constraints` and any additional replay/idempotency artifact; immutable accepted-output payload binding remains its separate §11.3 check when that trigger exists;
- wrong retained actor binding, payment method, action handle, context version, quote version or grant validity across async delay/recovery;
- a rotated/expired actor-context reference requires a current `subjectBindingProof` for the same stable subject/issuer/realm; possession of the digest/ID without proof does not authorize the action;
- Capture/Cancel/Refund/Release/Unlock receive separate current action grants; the capture grant is not reused, and missing fallback authorization leads to a typed residual/manual outcome with no privileged output;
- raw grant/session/principal is absent from live workflow state, status, Projection, and ordinary telemetry; the exact committed grant remains only in the protected/redacted output record until the declared dispatch/audit horizon;
- insufficient actor assurance;
- revoked capability;
- target object version changed;
- endpoint/credential scope too broad;
- restricted capability boundary succeeds for the declared bounded operation class; an administrator credential plus a local wrapper/check fails;
- capability-rooted filesystem resolution succeeds and raw traversal fails; the other applicable parameterized, structured, and context-encoded sinks have paired injection negatives;
- a trusted `InProcess + Hardened` binding with an ordinary scoped external credential and no hostile-component-containment or separate-principal requirement remains InProcess, while a credential that must be isolated from less-trusted co-resident code or a high-privilege credential requiring its own security principal selects `Isolated`;
- SSRF/path traversal/shell/raw SQL attempts;
- complete secret-flow policy covers each reachable State, any output, persistence, serialization, log, and telemetry path at its exact scope; a log-only policy fails when any non-log path is reachable;
- authenticated resource exhaustion.

### 17.9. Claim records and benchmarks

This subsection is claim-triggered. Every claim fixture first materializes the complete `ClaimRecord`: exact named boundary, binding and scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees. Field-deletion, wrong-boundary/scope, stale-evidence, source-durability-only, and retained-pending-only negatives prohibit the claim rather than inferring a stronger guarantee.

When a performance claim is made, the report additionally records:

```text
hardware and OS
language/runtime/compiler version
optimization flags
allocator/GC mode
payload sizes and distribution
warmup and sample method
instrumentation overhead
profile combination
latency distribution
allocation/copy counts
```

As a claim-evidence projection of the marked source clause for `PBA-41`, such a benchmark compares the following baselines where applicable:

```text
direct hand-written call
Pokeball Inline binding
concurrent binding where applicable
existing framework baseline
```

If the claim compares numeric `maxTransitionSteps` counts across bindings, the report records `meterIdentity`, `meterVersion`, `transitionArtifactVersion`, and `unitDefinition` for every compared count and proves all four values equal before making the numeric comparison. Otherwise the counts remain explicitly incomparable and may be reported only as separate binding-local observations.

Do not claim "zero overhead" based on one microbenchmark without a realistic payload and build settings.

### 17.10. Observability

Recommended causal fields for paths that materialize them:

```text
ballType
ballInstanceId
commitRevision
pulseType
semanticHandle
operationId
outputId when available
stateRevisionBefore/After
attemptId
outcome
latency
traceId
```

An operational trace, security audit, and exact replay data are distinct artifacts. A redacted trace does not guarantee replay.

---

## 18. Practical checklist

### 18.1. Core Ball and trigger resolution

Always-applicable checks:

- [ ] The `Ball` boundary passes the §4.4 decision tree and its combine/separate/utility/Feature/Flow/Read Model falsifier.
- [ ] Core validity does not imply one unique decomposition graph.
- [ ] A materialized `StateKey` exists only when the instance-identity trigger requires it.
- [ ] Interaction, Nucleus, Resource/route, Assembly, runtime/acceptor, status/read authority, and shared-foundation roles—and every present Trusted Boundary edge—have an inspectable role/edge map, permitted call graph, accepted-write sites, and provenance trace; Interaction is a role, Trusted Boundary is an authorized verification/construction edge, and same-file/stack/generated co-location merges neither authority nor evidence.
- [ ] Used protocol categories are closed and typed; an absent category is genuinely empty and has no placeholder.
- [ ] The canonical ordered unions are exactly `Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse` and `ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest`; no payload-only alias is treated as an envelope.
- [ ] The current cause is one typed `Pulse`.
- [ ] The trusted binding boundary constructs every non-empty verified/bounded `DecisionContext` under a Ball/Nucleus-owned semantic schema and interpretation.
- [ ] The context contains only observations that affect the Decision.
- [ ] State has one writer.
- [ ] Every mutable semantic fact has one authority.
- [ ] No Ball reads another authority's mutable state directly.
- [ ] `EphemeralState` contains only non-decision UI/transport mechanics. Any UI/transport value that can change a business Decision is committed State or an explicit trusted current `Pulse`/`DecisionContext` input.
- [ ] `decide` is pure and terminating.
- [ ] Present variable dimensions have one finite effective bound.
- [ ] If `maxCumulativeFanout` is present, its one causal scope and accepted source-output-to-effective-route/consumer branch unit are explicit: terminal/converging branches count, co-reachable branches sum, mutually exclusive alternatives share the maximum reservation, duplicate/redelivery does not increment, async handoff preserves scope, and exact `N/N+1` rejects the whole over-limit Decision without partial dispatch.
- [ ] A numeric `maxTransitionSteps` resolves one immutable versioned Decision Work Meter; for equal canonical inputs under the same binding, transition artifact version, and meter identity/version it charges equal non-negative integral totals, starts once at zero and never resets within one `decide`, passes at exact `N`, accepts no frame/State/revision/output/dispatch at `N+1`, and is compared across bindings only when meter identity/version, transition artifact version, and unit definition are all equal.
- [ ] Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without static type-and-representation proof resolves one immutable `BoundedByteMeasure` dimension/identity/version/representation/limit tuple. Input fixes raw-or-normalized stage and metadata/Context inclusion; State fixes the complete candidate-next-State semantic representation; output fixes the complete ordered `Decision.outputs` sequence and envelopes. Alternate representations map exactly, erasure preserves counts, unequal tuples are incomparable, exact `N` passes, and stage-specific `N+1` accepts no prohibited semantic artifact.
- [ ] Overflow accepts neither partial state nor partial output.
- [ ] A Decision is accepted in full or not accepted.
- [ ] The selected state profile exposes exactly one §3.3 mutation algebra and accepted frame: Snapshot uses `SnapshotDecisionResult`/`AcceptedSnapshotDecisionFrame`, EventJournal uses `EventDecisionResult`/`AcceptedEventCommit`, and Event mode has no independent `nextState` or mandatory cross-profile runtime union.
- [ ] Representation and declared closed protocol/type invariants fail before semantic admission as `ValidationFailure`; State- or semantic-Context-owned rules reach the Nucleus and reject as `BusinessRejection`. Promoting a fixed rule into the type changes the versioned protocol identity rather than switching stages under the same version.
- [ ] Reentrant mutation is prohibited.
- [ ] No ambient resource authority, service locator, runtime registry, mutable business global, or foundation-mediated hidden communication is available to application code.

Applicability checks:

- [ ] The closed protocol/type/profile/route/risk/claim inventory has been evaluated against §20.1; inferred triggers cannot be disabled by missing metadata.
- [ ] Every triggered guardrail resolves to one effective static proof, local declaration, or exact in-scope reusable policy plus permitted delta; stale, cyclic, conflicting, wrong-version/profile/binding/environment references fail.
- [ ] Absent triggers create no mechanism, empty table, zero field, evidence dossier, or `N/A` row. Ambiguous absence is resolved explicitly or treated as present.
- [ ] If a conformance/release claim or accepted ambiguity-resolution decision relies on absence, one exact `TriggerAbsenceProof` binds the non-`always` class, anchor, scope/profile, inventory/digests, predicate, owner, and invalidation conditions.
- [ ] A present trigger or claim invalidates contrary proof.
- [ ] Wrong/stale/unresolved/conflicting evidence or any invalidation condition blocks reliance until reevaluation.
- [ ] Ordinary design/adoption creates no proof placeholder.
- [ ] A boundary worksheet is project-owned `SHOULD` guidance only for active adoption/pilot work; relevant workload, method, baseline, and continue/reshape/stop thresholds are project-selected, Core supplies no universal number, and negative-adoption cases or established non-pilot work create no worksheet, empty Ball, or conformance placeholder.
- [ ] If outputs exist, commit-before-dispatch and complete-batch retention are proven.
- [ ] If an Inline deque or continuation carries multiple causes, each item retains or resolves its own trusted per-Pulse `DecisionContext`; a later cause does not inherit root actor/grant/reserved-ID/time/validity fields by variable reuse.
- [ ] Every version consumed by a Decision or retained as lineage is a named verified current Context field or an exact enclosing-binding proof; Checkout explicitly supplies `artifactVersion = IV`, retains `interactionArtifactVersion = IV`, and keeps it distinct from `transitionArtifactVersion` across retry/recovery/migration.
- [ ] Remaining causal budget/depth/slots, execution quantum, and current runtime capacity reach only reservation/admission/continuation logic; they are absent from `DecisionContext` and cannot change the candidate Decision.
- [ ] Detached/addressable work has stable semantic identity, while immediate local output need not materialize it.
- [ ] If a command/result route exists, trusted boundaries derive `commandSource` from the accepted source frame and `resultSource` from the accepted target frame; Assembly and same-stack generated code transport/prove rather than synthesize those tuples or target-owned payloads.
- [ ] Every target refusal follows its static versioned carrier-or-accepted-result classification. Carrier `BusinessRejection` projects `RejectedBeforeAcceptance + NotExpected`; accepted result rejection projects `Accepted + Rejected`; conflicting evidence fails closed.
- [ ] On a same-stack pre-acceptance refusal, the target has no accepted frame or causal level and the source carrier Decision consumes the atomically transferred level-1 alternative-completion slot without a new root, reset, overrun, loss, direct State write, or undeclared deferral.
- [ ] If a Query/stamped read/status path exists, its exact result/stamp/authority contract is closed.
- [ ] All reads remain non-mutating.
- [ ] Each status namespace has one committed revisioned single-writer authority.
- [ ] Candidate `OperationId` reservation alone creates no operation/status authority. Root validation, admission, or Decision rejection creates only its typed `BoundaryResponse`; a covered later lookup may return `NotFound`, never a known rejected root row. Participant nonacceptance remains a facet of an already accepted source operation.
- [ ] When root idempotency is triggered, same key/fingerprint within the horizon redelivers the exact original accepted `ReplyOutput` frame and changes only `AttemptId`; different fingerprint is pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, creates no semantic artifact, and leaves the prior operation unchanged.
- [ ] Its materializer applies causal order or bounded pending.
- [ ] Its materializer merges idempotently and monotonically without facet loss.
- [ ] Its materializer reserves finite capacity before source acceptance.
- [ ] Its materializer evicts/truncates nothing after acceptance.
- [ ] Its materializer commits a covered-source/empty-pending marker before absence with no resurrection.
- [ ] `Draining` rejects new logical mutations.
- [ ] `Draining` serves available declared Query/status Query paths.
- [ ] `Draining` serves already-accepted completion/cancellation/status inputs.
- [ ] An unavailable read fails before `read`.
- [ ] An admitted read returns only the successfully evaluated `ReadResult`; its target-owned payload closes every reachable permitted/denied/redacted/non-disclosing semantic outcome, the pure Policy Gate selects the variant, no boundary/caller/Assembly invents one, and the read creates no Decision.
- [ ] Status has exactly one query authority per namespace, whether co-located or separate, and never acquires command/business-fact authority; materialization lag is represented by its own stamp.
- [ ] For Catalog v2, all six `CatalogState` variants map exactly once to the composite `CatalogView`, and all six `ProductSelected` transitions preserve state-specific facts and emit only one ordinal-0 `ProductSelectionConfirmed` Signal publication.
- [ ] Catalog's illustrative Ball-owned domain `revision` remains distinct from acceptor-owned `CommitRevision` unless a declared binding proves exact equality while preserving both ownership meanings.
- [ ] Catalog cancellation verifies `Intent.operationId = State.operationId = pendingSearch.operationId` before any handle/Decision/Effect; mismatch is a bounded business rejection with no accepted artifact, and Catalog retains protocol `2.0.0`, schema `2`, and transition artifact `2.0.1` as distinct identities.
- [ ] If a value crosses Decisions, it is retained or reintroduced with only the correlation/version/provenance/retention required by that path; runtime history is not a hidden decision input.
- [ ] If late results, ACK, ambiguity, retry, cancellation, deadline, timer, status, or durable delivery paths exist, only their reachable §9 contracts and race tests are present.
- [ ] A target-command duplicate before accepted result returns only verified ACK proof of the original accepted target frame/pending result and neither waits nor re-decides/re-executes; after result it redelivers the exact accepted result frame with only a new `AttemptId`; conflicting evidence fails closed.
- [ ] Checkout v1 consumes accepted `StillUnknown` on its single status slot into terminal `NeedsManualReconciliation` with `outputs = []`; duplicate delivery adds no Decision/handle, no automatic generation exists, and later proof requires a separately declared manual/recovery artifact.
- [ ] For every active retrying failure mode, exactly one layer is the primary retry owner; every other layer is disabled or finite and proven semantically transparent, and cumulative attempts are checked.
- [ ] If a raw, external-resource, privileged, interpreter, secret, or unsafe path exists, only its reachable §11 guardrails resolve; no forbidden-capability boilerplate substitutes for a closed type graph.
- [ ] Every external Effect uses minimum explicit authority for its bounded operation class at a real capability boundary; a wrapper or local check behind unrestricted authority does not qualify.
- [ ] Every interpreter/dialect edge uses its applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- [ ] Every reachable secret path through State, any output, persistence, serialization, logs, or telemetry has an explicit policy for that exact path and scope; policy for one path does not cover another.
- [ ] If a privileged action exists, the Nucleus Policy Gate alone decides business permission.
- [ ] Immediately before execution, the Resource/target Execution Gate verifies the triggered proof/capability/constraints/version/freshness/revocation/endpoint/quota/sink without a new business decision.
- [ ] A post-acceptance gate failure remains a typed Resource/result/status path, never a carrier or downgrade.
- [ ] If actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection, the trusted binding constructs verified, bounded, field-minimized context relative to a valid approved issuer or fixed trusted same-stack issuer/realm proof under PBA-44.
- [ ] Forged, wrong-issuer/realm, missing, stale, or unverifiable evidence fails closed.
- [ ] An actor-independent Decision/read creates no actor artifacts.
- [ ] If a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, policy decision, domain authority, route selection, service locator, or hidden communication state.
- [ ] A policy reference or `WaiverRecord` never suppresses a trigger or weakens a law; any violated `MUST`/`MUST NOT` keeps its exact scope non-conforming.
- [ ] Performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance language is used only with an exact claim record naming the guarantee boundary, scope, mechanism, assumptions, retention, evidence, and non-guarantees; source durability or retained pending work alone implies no stronger downstream outcome.
- [ ] Each error/fault follows its exact §6.13 stage carrier/result/status mapping.
- [ ] No later failure rewrites prior acceptance.
- [ ] Every concrete fallible-admission profile/binding has one finite closed `AdmissionFailure.reason` union.
- [ ] Unknown/open-string reasons fail.
- [ ] A non-fallible path has no empty union.
- [ ] The §0.1 Core-documentation rule is projected exactly: every new or changed Mermaid diagram in this Core includes a local legend distinguishing semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows; omission blocks that documentation change and does not affect a consuming binding unless project policy adopts the rule.

### 18.2. Composition

Apply this subsection only when an inter-Ball edge or Flow exists.

- [ ] A public dependency is declared as `ReadDependency`, `DeclaredCommandDependency`, `DeclaredSignalDependency`, or `FlowParticipation`.
- [ ] `maxDeclaredDependenciesPerBall`, when present, counts each distinct owner-declared read/command/signal/participation row once; referenced dependency and participation rows both count, same-target operations remain distinct, exact aliases/duplicates are invalid, exact `N` resolves, and `N+1` rejects the contract.
- [ ] A physical helper import is classified before that four-kind semantic taxonomy: Ball-local utility owned by exactly one logical role, or shared mechanical Foundation. An ownerless shared domain/business utility fails; shared domain semantics remain local copies or acquire one Ball/Flow owner and a declared Application Surface/protocol.
- [ ] Every utility import remains visible in the acyclic compile-time graph; it adds no Assembly route or dependency row and no `Direct Control Dependency` unless it exposes another Ball's Application Surface or synchronously crosses authority.
- [ ] Every `FlowParticipation` resolves one Flow authority, one participant authority, one exact participant `Application Surface`, a non-empty bounded Flow-owned coordination set, and bounded references to existing read/command/signal dependencies and their Assembly bindings.
- [ ] `FlowParticipation` is a design-time relation view, not a protocol, runtime envelope, fifth route, copied dependency, or transfer of participant facts/authority.
- [ ] Every referenced `Application Surface` remains participant-owned and exposes only deliberate public semantic types/entrypoints—not mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, or re-exported foreign contracts.
- [ ] A caller Nucleus imports only the exact declared target- or producer-owned Application Surfaces required by its closed `Query`, `Pulse`, and `Decision` contracts; import transfers no ownership.
- [ ] Checkout's positive fixture imports the four Cart/Inventory/Payment/Order participant-owned surfaces and resolves the nine target-owned command/result mappings.
- [ ] Checkout resolves thirteen dependency units (four participation plus nine command declarations) and nine `maxRoutesPerFlow` units; each unit is one effective command/result round trip, ingress/return legs count together, and participation/references create no route.
- [ ] Caller redeclaration or a structurally identical mirror, foreign State/internal/private-adapter import, protocol re-export, and Interaction/Assembly synthesis all fail.
- [ ] Every `ReadDependency` resolves exactly one target authority, target-owned Query/result mapping and effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding.
- [ ] Caller and Assembly cannot redefine the target payload, stamp, status fact, or read meaning.
- [ ] `ReadDependency` versions, actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields appear only under existing triggers.
- [ ] Same-build/static wiring may erase wrappers while proving the same contract.
- [ ] Independent reads make no atomic multi-source promise.
- [ ] Query alone creates no new per-Ball read-limit field.
- [ ] A read boundary may return an existing validation/admission response before `read`.
- [ ] Once admitted, only the declared successfully evaluated `ReadResult` returns; policy denial/redaction/non-disclosure is a target-owned payload variant, never `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary/Assembly-invented `NotFound`.
- [ ] An admitted read creates no accepted marker, Decision, revision, handle, or semantic output.
- [ ] Wrong version/authority/mapping/stamp is rejected rather than mapped to `NotFound`.
- [ ] Every imported `ModuleCommand`/`ModuleResult` resolves through exactly one target-owned mapping and dependency whose effective protocol identity matches both Assembly ingress and return routes.
- [ ] Explicit versions appear only across independent versioning/deployment.
- [ ] Imported target types are not redeclared as caller-owned.
- [ ] Assembly selects route/version/binding and transports verified `ModuleCommandPulse`/`ModuleResultPulse` values.
- [ ] Assembly does not create or alter `commandSource`, `resultSource`, target payloads, or refusal meaning.
- [ ] Same-stack erasure proves the same accepted tuples.
- [ ] Assembly/generated wiring does not construct semantic context, select business permission/read results, or move mutable business communication through foundation/runtime state.
- [ ] A read-like operation uses the command bridge only when accepted provenance, stable step identity, idempotent replay, status, or reconciliation is required; an ordinary non-recording read remains `ReadDependency`/`Query` with no target Decision/revision/output.
- [ ] A same-stack command round trip has one `source -> target` direct-control edge and no reverse return edge.
- [ ] The source reserves one level-1 alternative-completion slot per synchronous command: an accepted target or its source carrier Decision consumes it, never both.
- [ ] Successful result completion consumes accepted levels `0/1/2`; an unaccepted target consumes no level and its source carrier Decision consumes level 1.
- [ ] Missing level 1 prevents source acceptance/dispatch; missing level 2 prevents target acceptance and transfers level 1 to the carrier branch.
- [ ] Carrier handling cannot lose evidence, reset/overrun the budget, create a new root, or defer outside a declared continuation preserving the same slot and total scope; any further synchronous output reserves normally.
- [ ] Async handoff removes only the synchronous-invocation contribution to direct control, retains any separately present compile-time-import edge, and does not reset scope/depth/budget.
- [ ] A Feature does not import another Feature's internals.
- [ ] A one-hop command is not artificially turned into a micro-Flow.
- [ ] A Flow owns at least one material coordination property—lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome. Call count or one hop alone is not material coordination; one real property is sufficient when the one-hop conditions fail.
- [ ] A stateful multi-participant workflow has one coordinator owner.
- [ ] A Flow does not copy mutable participant truth.
- [ ] A Flow stores only field-minimized workflow values needed by later decisions/recovery; a participant/runtime ledger, outbox, and history do not become hidden decision input.
- [ ] A public contract does not re-export another authority's owned types.
- [ ] The route graph and fan-out are bounded; when `maxCumulativeFanout` exists, tree, diamond, mixed-route, mutually exclusive/co-reachable, duplicate/redelivery, async-handoff, independent-root, and exact `N/N+1` fixtures use the §10.9 branch unit.
- [ ] Compile-time import and `Direct Control Dependency` graphs are unconditionally acyclic, including generated inline dispatch before handoff/yield. A waiver records deliberate non-conformance and cannot make this check pass.
- [ ] Async feedback, if present, has an owner, stable identity, a finite causal budget, an escape condition, and fan-out protection; idempotency/deduplication and a retry budget appear only when duplicate or retry paths exist.
- [ ] A multi-source read is not called an atomic snapshot without a corresponding mechanism.

### 18.3. Concurrent

Apply this subsection only for `BoundedConcurrent` or another explicitly claimed concurrent binding.

- [ ] The mailbox is bounded.
- [ ] Pre-acceptance backpressure is a typed `BoundaryResponse(AdmissionFailure(reason))` and creates no accepted state/output.
- [ ] Worker concurrency is bounded when workers exist.
- [ ] Out-of-order result tests exist when completion can reorder.
- [ ] Causal depth and cumulative fan-out are bounded when causal feedback/fan-out exists; the complete causal scope preserves the same exact branch count across concurrency and handoff rather than resetting per worker, queue, or hop.
- [ ] Cancellation/deadline/late-result races are represented only when those paths exist.

### 18.4. Durable state

Apply this subsection only for `SnapshotOutbox`, `EventJournal`, or another explicit durability claim.

- [ ] State/events and every present durable output are accepted atomically.
- [ ] Commit-before-dispatch is proven when outputs exist.
- [ ] Ingress/inbox duplicate identity and retention are defined when duplicates are possible.
- [ ] Output identity remains stable when redelivery exists.
- [ ] ACK and business result are distinct when ACK is separately observed.
- [ ] An accepted `ModuleResultOutput` survives a post-acceptance crash according to the target profile; retained/retried/observable result delivery is finitely bounded and target `DispatchStopped` uses `(effectiveProtocolIdentity, commandSource, resultSource)` without setting source facets.
- [ ] Snapshot same-state accepted rejection increments target revision; EventJournal records accepted `NoDomainChange`; duplicate command identity within the horizon reuses the accepted result without another target Decision/revision.
- [ ] A durable target duplicate before result returns only accepted-frame/pending-result ACK proof and does not repeat target work; after result, recovery/redelivery uses the exact accepted result frame.
- [ ] A lost accepted root response is recovered by redelivery of the original accepted Reply frame rather than a new Decision/status lookup; conflicting fingerprint remains pre-Intent validation failure.
- [ ] `OutcomeUnknown` has reconciliation when ambiguous external execution exists.
- [ ] Checkout's accepted `StillUnknown` recovery restores the same terminal manual-reconciliation frame with no automatic output or silent late-proof rewrite.
- [ ] Only the reachable crash/ACK/result/exhaustion cases from §17.7 are tested.
- [ ] Recovery uses committed output records, not rerun `decide`, when dispatchable outputs exist.
- [ ] Recovery restores every retained cross-transition value with only its triggered metadata.
- [ ] A movable owner has a fence; a fixed owner needs none.
- [ ] Operation status is independent of a live reply when the status trigger exists; its physical representation is binding-owned, while causal pending, conflict, capacity, marker, and single-writer semantics remain §9.11-equivalent.
- [ ] Crash after accepted target `ModuleResultOutput` preserves the target result source; target route exhaustion records target `DispatchStopped`, and source status remains Pending/Unknown until verified result or reconciliation.
- [ ] RPO/RTO and downstream durability are not assumed automatically.

### 18.5. Hardened / Isolated

Apply this subsection only for the selected `Hardened`/`Isolated` profile or corresponding security/isolation claim.

PBA-44 already applies in every profile when actor context can change a Decision or Query/status-read authorization/result selection. This subsection checks only the stronger profile-specific realization and evidence.

- [ ] Actor-dependent `DecisionContext`/`ReadContext` comes from a trusted binding boundary when actor identity affects semantics; fixed trusted same-stack scope may prove issuer/realm statically, and actor-independent paths create no actor artifacts.
- [ ] A privileged output is bound to a scoped grant when proof crosses an authority.
- [ ] The Nucleus Policy Gate owns business permission; the Execution Gate checks every triggered technical proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink immediately before each privileged execution without a new business decision.
- [ ] A present grant is authentic and fail-closed bound to every constrained payload field.
- [ ] A delayed privileged action receives a current action-scoped grant; raw principals are not retained as live authority.
- [ ] An external-resource capability uses a restricted credential/client/broker/ACL.
- [ ] Interpreter authority is absent or controlled by an explicit unsafe contract.
- [ ] Present secrets do not enter ordinary projections/logs/metrics.
- [ ] Hostile code is placed in a process/sandbox if containment is claimed.
- [ ] `Isolated` is selected when a credential must be isolated from less-trusted co-resident code or a high-privilege credential requires its own security principal; an ordinary scoped external credential may remain `InProcess + Hardened` when the trusted in-process threat model makes no hostile-containment or separate-principal claim.
- [ ] Effective limits are enforced only for resources/exposures present at the boundary; absent network or file authority needs no row, while denial by construction is valid proof.

---

## 19. Anti-patterns

| Anti-pattern | Why it violates the model | Preferred construction |
|---|---|---|
| `UI -> Repository -> Network` as a business path | The decision and effect bypass the Nucleus | `Intent -> Decision -> Effect` |
| Global mutable store | Multiple implicit owners and a broad blast radius | Isolated state authorities |
| Cross-cell selector over multiple mutable stores | Hidden coordination authority | Read Model or Flow-owned operation |
| Universal mediator/event bus | Runtime discovery, wildcard coupling, hidden fan-out | Typed explicit routes |
| `Map<String, Any>` / string message names | No closed algebra or exhaustiveness | Tagged unions / sealed types |
| `ExecuteSql(String)` | Raw interpreter authority | `FindUserById(UserId)` + safe adapter |
| `FetchUrl(String)` | SSRF and arbitrary endpoint authority | Endpoint-scoped typed capability |
| `RunShell(String)` | Command injection and ambient OS authority | Structured OS operation; if raw shell is unavoidable, use the explicit unsafe escape-hatch contract in §11.10. Add a `WaiverRecord` only for a separate unresolved `MUST`/`MUST NOT` deviation; that deviation remains non-conforming in its exact scope. |
| Unbounded queue/retry/fan-out | Unpredictable memory/latency/cost | Declared finite limits |
| Retry in SDK + adapter + runtime + Flow for one failure mode | Multiplicative attempts and unclear ownership | One primary retry owner per failure mode; other layers disabled or bounded and proven semantically transparent |
| Timeout = failed action | The external action might have occurred | `OutcomeUnknown` + reconciliation |
| Cancel flag = physical stop | Cancellation may be too late | Cancellation protocol/state |
| Shared `common` domain model | Transitive coupling and foreign ownership | Local semantic types + opaque refs |
| Protocol re-export | An owner change propagates transitively | A dedicated consumer-facing contract |
| Feature calls the target Nucleus directly | Bypasses route, authority, and delivery semantics | Declared command/read contract |
| Flow for every one-hop call | Ceremony and coordination explosion | `DeclaredCommandDependency` |
| One God Flow for the entire system | Hot key, enormous state graph, change radius | Multiple workflow authorities |
| One Ball per DTO/table | Micromodularity without an invariant boundary | Boundary by authority/invariants/lifecycle |
| Repository interface only for a mock | Artificial abstraction without a real boundary | Pure transition tests + adapter contract tests |
| Service locator/global SDK client | Ambient authority and invisible dependency | Explicit scoped capability injection |
| Adapter "checks itself" while holding an admin client | No independent enforcement | Restricted credential/ACL/broker |
| Roll back state after an emitted external action | The consequence does not disappear | Forward recovery/reconciliation |
| Rebuild the outbox by rerunning the Nucleus | A new version may produce different outputs | Preserve the committed output ledger |
| One `Pending/Done/Failed` status for a distributed step | Does not express acceptance/cancel/unknown races | Orthogonal facets |
| Ownerless shared domain utility | Hides business meaning outside any Ball/Flow and bypasses the four semantic dependency kinds | Ball-local implementations, or one Ball/Flow owner exposed through a declared Application Surface/protocol |
| "Durable" without saying what is durable | Conflates source, target, executor, and reply guarantees | Qualified guarantee per boundary |

---

## 20. Canonical Pokeball laws

The 44 entries below are the complete, set-equal audit projection of the Core-embedded source records in the numbered body. Each entry copies all eight fields—Rule, Applicability, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route—without creating independent normative authority.

<!-- pkb:generated:start id="core-laws" -->
### PBA-01 — Three-Zone Boundary

Source: §5.

- **Rule:** Every application `Ball` has the logically separated Interaction, Nucleus, and Resource/route roles defined in §§5.1–5.3. This separation preserves their distinct authority and permitted calls even when one file, call stack, or generated binding uses **representation erasure**—omitting a materialized wrapper or adapter while proving the same semantic facts and role edges (§22); physical co-location alone is not evidence of separation.
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

### PBA-31 — Double Quarantine

Source: §11.1.

- **Rule:** Every raw external-input or raw resource-output edge that exists passes through the applicable parsing, validation, finite bounds, and provenance checks before its value reaches the Nucleus.
- **Applicability:** `P`: raw input or resource output crosses a trust/representation edge.
- **Declaration owner:** Interaction/Resource adapter.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Parser/provenance/fuzz tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact validator evidence reusable; absent side needs no placeholder.
- **Primary verification route:** `§17.4`

### PBA-32 — Capability-Sealed Effect

Source: §11.4.

- **Rule:** Every external effect uses the minimum explicit technical authority for its bounded operation class, enforced at a real capability boundary.
- **Applicability:** `P`: an `EffectRequest` reaches the Ball's external resource/authority.
- **Declaration owner:** Ball capability semantics and binding realization.
- **Scope:** The exact external effect path and its bounded operation class.
- **Enforcement / evidence owner:** Binding/resource owner at the scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolation boundary; positive restriction and unrestricted-authority negative tests.
- **Resolution, failure, and conformance:** Authority wider than the declared operation class, or a local check behind unrestricted authority, fails the rule at that effect path.
- **Reuse and absent-trigger behavior:** Capability policy reusable; omit when no external Effect path exists.
- **Primary verification route:** `§17.3`

### PBA-33 — Dual Gate

Source: §11.3.

- **Rule:**
  - When a privileged action requires business and current technical authorization, the Nucleus Policy Gate alone makes the business permission decision from committed State, the current cause, and trusted semantic context.
  - Immediately before authoritative execution, the target/resource Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding.
  - It does so without making a new business decision.
  - Failure after target acceptance is a declared Resource/result/status path.
  - It neither rolls back nor downgrades accepted work.
  - It never becomes a pre-acceptance command carrier.

  #### Policy Gate

  The `Nucleus` alone decides whether an action or actor-dependent read is permitted and which business-visible result follows from the current State, cause or Query, and trusted context. After an actor-dependent read is admitted, the pure read Policy Gate selects one variant from the Query's total target-owned `ResultPayload`: permitted, denied, redacted, or deliberately non-disclosing as that exact protocol declares. It never turns that semantic outcome into `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary-invented `NotFound`. A pure read remains non-mutating and creates no `Decision`; this does not move its semantic permission or result selection into Interaction, Assembly, runtime, or Resource.

  #### Execution Gate

  Immediately before an authoritative action, the target or resource enforces the applicable subset of this catalog without revisiting business permission:

  - accepted-action identity and immutable payload binding when execution follows an accepted output;
  - minimum technical capability when an external resource is used;
  - authenticity and integrity of actor context and, for a privileged action, any required grant from an approved issuer for the relevant realm, audience, and action;
  - binding of actor, context, audience, action, object, operation, and each constrained field when such constraints authorize the actual target or payload;
  - proof/grant version and expiry, freshness, or revocation when those policies exist;
  - expected target/object version when concurrency or a current-version constraint exists;
  - idempotency identity when duplicate execution is possible;
  - endpoint identity for a network endpoint;
  - local quota or budget when quota enforcement is present;
  - safe-sink constraints at an interpreter edge.

  Resource does not reinterpret business intent or apply a second business policy. It verifies only the technical authorization needed to execute the already accepted action. Fields and checks whose trigger is absent are omitted rather than populated with defaults.

  The Execution Gate fails closed for every triggered proof: required evidence that is missing, unverifiable, expired, stale, revoked, wrong-version, or mismatched does not authorize an authoritative action and is not replaced by an ambient credential, transport authentication, or a trusted string-valued `issuer`. Rejection returns as a declared typed Resource outcome bound to the accepted action. If target acceptance already occurred, the owning Nucleus may accept the corresponding target-owned result/status transition, but neither the gate nor its failure rewrites that history as `CommandRejectedBeforeAcceptance`. Authentication of an IPC peer alone does not make that peer an approved authorization issuer.
- **Applicability:** `R`: a privileged action needs business and current technical authorization, or an actor-dependent read can select more than one semantic permission/result outcome. Only the gate obligations present on that path materialize.
- **Declaration owner:** Ball/Nucleus owns action/read business permission and read result selection; target/resource owns the immediate technical Execution Gate for an action.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Sole-policy-site, gate-order, proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink, and post-acceptance failure tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Gate mechanics/evidence reusable; action/read meaning remains local; omit the Execution Gate for a pure read and omit the whole rule when neither trigger exists.
- **Primary verification route:** `§17.8`

### PBA-34 — No Ambient Authority

Source: §11.5.

- **Rule:** Every Ball and binding keeps application authority and mutable business communication out of ambient globals, credentials, service locators, runtime registries, and shared foundation state; dependencies and communication paths are explicit in construction, protocol, or static Assembly.
- **Applicability:** `A`: every Ball/binding.
- **Declaration owner:** Project dependency and explicit-communication contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports, call graph, global-state/service-locator/runtime-registry/foundation scans.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared injection convention may be reused; actual graph proves absence of ambient authority and hidden communication.
- **Primary verification route:** `§17.5`

### PBA-35 — Safe Sink

Source: §11.6.

- **Rule:** Every interpreter or dialect boundary that exists uses the applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- **Applicability:** `P`: data reaches an interpreter/dialect.
- **Declaration owner:** Resource/sink owner.
- **Scope:** The exact interpreter or dialect edge and its data/context interpretation.
- **Enforcement / evidence owner:** Owning Interaction/Resource sink implementation; parameterized, structured, capability-rooted, and context-encoded positive fixtures plus raw-injection/traversal negatives as applicable.
- **Resolution, failure, and conformance:** A typed wrapper without an applicable safe sink, or raw data interpreted as code/path/dialect syntax, fails that edge.
- **Reuse and absent-trigger behavior:** Adapter/evidence reusable; omit with no interpreter edge.
- **Primary verification route:** `§17.3`

### PBA-36 — Secret Containment

Source: §11.9.

- **Rule:** A secret does not enter state, output, persistence, serialization, logs, or telemetry without an explicit policy for that exact path and scope.
- **Applicability:** `R`: a secret can enter State, any output, persistence, serialization, logs, or telemetry.
- **Declaration owner:** Project classification and affected owner.
- **Scope:** Each exact reachable secret path and its Ball/project/binding scope.
- **Enforcement / evidence owner:** Affected State/output owner plus persistence, serializer, log, and telemetry owners; full data-flow and non-log leakage tests.
- **Resolution, failure, and conformance:** Any reachable path without an explicit policy for that exact path and scope fails closed; a policy for one sink does not cover another.
- **Reuse and absent-trigger behavior:** Exact secret policy reusable; omit when closed data flow proves no secret.
- **Primary verification route:** `§17.8`

### PBA-37 — Explicit Unsafe Escape Hatch

Source: §11.10.

- **Rule:**
  Raw SQL, shell, an arbitrary URL, unsafe deserialization, or unrestricted filesystem access is permitted only as an explicitly named unsafe operation with:

  ```text
  owner
  reason
  scope
  capability
  isolation decision
  security review
  tests
  expiry/remediation
  ```

  An escape hatch must not masquerade as an ordinary `Effect`.
- **Applicability:** `R`: raw authority/unsafe escape exists.
- **Declaration owner:** Named security owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Capability gate/scan/audit tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Controls reusable, but each site resolves; omit register when scan proves none.
- **Primary verification route:** `§17.8`

### PBA-38 — Bounded Execution

Source: §8.3.

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

### PBA-39 — Profile-Proportional Mechanism

Source: §0.2.

- **Rule:**
  - Every guardrail uses exactly one of the four closed applicability classes below.
  - A present trigger resolves statically to one effective guardrail.
  - An absent `path-triggered`, `risk-triggered`, or `claim-triggered` predicate removes its mechanism and ceremony only under the inventory and evidence rules in this subsection.
  - An `always` obligation is never absent.
  - A boundary/adoption worksheet is project-owned `SHOULD` guidance only while adoption or pilot work is active.
  - It is not a universal Core artifact or conformance placeholder.

  Core uses four closed applicability classes:

  | Class | A guardrail applies when |
  |---|---|
  | `always` | the rule is an invariant of every conforming Ball or binding in its stated scope; |
  | `path-triggered` | the closed protocol, type graph, selected profile, Assembly, or reachable control flow contains the named path; |
  | `risk-triggered` | the architecture contains the named trust, failure, concurrency, irreversibility, secrecy, abuse, or recovery condition; |
  | `claim-triggered` | a concrete binding or distribution makes the named performance, durability, delivery, isolation, security, or conformance claim. |

  Applicability and declaration location are different questions. An applicable guardrail MUST resolve statically to exactly one effective mechanism, value, or evidence contract through one of these forms:

  1. construction or a bounded type proves the invariant directly;
  2. the Ball declares the guardrail locally;
  3. an accepted project, profile, Assembly, or binding scope selects one exact reusable declaration covering the Ball and, where that declaration permits it, the Ball supplies only a field-specific local delta or a different explicit selection.

  The enclosing scope owns that single reference; a covered Ball does not repeat it merely to acknowledge the default. A Ball-local reference is needed only when selection is local rather than already fixed by the enclosing scope.

  A reusable declaration is immutable at its referenced revision and identifies:

  ```text
  owner
  policyId + revision + content digest
  covered guardrail IDs
  exact project/Ball/profile/Assembly/binding/environment scope
  effective values or mechanisms
  allowed override fields, if any
  enforcement owner
  evidence references and their scope, if evidence is required
  review or expiry condition, if one exists
  ```

  Resolution follows the closed reference graph before implementation or review. A missing, mutable, stale, cyclic, conflicting, wrong-version, wrong-profile, wrong-binding, or wrong-environment reference is invalid. A local delta is valid only for a field explicitly made overridable by the referenced declaration and produces one unique effective value. Reuse never creates a runtime registry, service locator, ambient default, or hidden inheritance.

  An existing complete local declaration remains valid. Replacing it with a reference is semantics-preserving only when the resolved mechanism, values, failure behavior, guarantee scope, and evidence scope are equal; migration may be incremental Ball by Ball.

  When the closed inventory proves a trigger absent, the corresponding mechanism, evidence, empty table, zero-valued placeholder, and `not applicable` row are omitted. If absence cannot be proved, the trigger is treated as present unless an explicit accepted decision closes the ambiguity. Missing evidence for a claim prohibits the claim; it does not permit an unprotected triggered path.

  When a Pokeball conformance or release claim, or an accepted ambiguity-resolution decision, relies on trigger absence, that conclusion is materialized in exactly this closed static evidence shape:

  ```text
  TriggerAbsenceProof {
      triggerClass:
          path-triggered
        | risk-triggered
        | claim-triggered
      triggerAnchor
      exactScopeAndEffectiveProfile
      inventoryEvidence:
          PathInventoryRef
        | RiskInventoryRef
        | DenialByConstructionRef
        | ClaimPublicationInventoryRef
      inventoryRevisionsOrDigests
      evaluatedPredicate
      conclusion: Absent
      evidenceOwner
      invalidationConditions
  }
  ```

  An `always` obligation cannot use `TriggerAbsenceProof`. A present trigger predicate invalidates a contrary proof and requires its guardrail to resolve; in particular, a published present claim cannot be negated by a claim-trigger absence proof. An unaccepted ambiguity interpretation cannot establish absence. Ordinary design, implementation, and adoption work materializes no proof or placeholder merely because a category is absent.

  The proof is bound to its exact trigger anchor, scope, effective profile, referenced inventory, and inventory revisions or digests. A listed invalidation condition, scope/profile/version/inventory change, missing or stale digest, unresolved reference, or conflicting evidence invalidates it and requires reevaluation before the dependent conformance/release claim or accepted decision may continue to rely on absence. `evidenceOwner` owns both the proof and its invalidation review. This is static review evidence, not a runtime registry, ambient default, hidden inheritance path, waiver, or authority to broaden a reusable declaration's scope.
- **Applicability:** `A`: every applicability decision; `P/R/C`: a verdict/decision relies on absence; `P`: adoption/pilot work requests boundary guidance.
- **Declaration owner:** Ball/project architecture owner; proof names evidence owner; project owns any worksheet.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Resolver/proof contradiction/invalidation plus worksheet-present/absent and negative-adoption fixtures.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** `always`/present predicates cannot use proof; exact-scope reuse only; worksheet is `SHOULD` during adoption/pilot and absent otherwise.
- **Primary verification route:** `§17.5`

### PBA-40 — Zero Mandatory Runtime Tax

Source: §13.3.

- **Rule:**
  Core semantics do not require:

  ```text
  runtime handler lookup
  reflection discovery
  in-process serialization
  mandatory queue
  mandatory thread hop
  object message hierarchy
  service locator
  ```

  Only a concrete Inline binding with a benchmark on the exact toolchain may claim `maxStructuralAllocationsPerDecision = 0`.

  Distinguish:

  - **structural allocation**—created solely by the architectural runtime mechanism;
  - **payload allocation**—useful domain or result data;
  - **payload copy**—copying useful payload across a boundary.
- **Applicability:** `A`: Core design; implementation check when Inline selected.
- **Declaration owner:** Core/profile and Inline binding owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Build inspection; benchmark only for a claim.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One binding serves many Balls; no per-Ball wrapper.
- **Primary verification route:** `§17.5`

### PBA-41 — Measured Claim

Source: §13.4.

- **Rule:** A performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim names its exact guarantee boundary, binding and scope, mechanism, assumptions, retention, evidence, and non-guarantees; without that complete in-scope evidence, the claim is not made. A claim that compares `maxTransitionSteps` counts across bindings additionally proves equality of Decision Work Meter identity/version, transition artifact version, and unit definition; otherwise the counts are intentionally incomparable.
- **Applicability:** `C`: any named performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim is made; cross-binding transition-step comparison additionally requires equal meter/artifact/unit identity.
- **Declaration owner:** Claimant names the guarantee boundary, exact binding/scope, mechanism, assumptions, retention, evidence/non-guarantees, and any compared meter/artifact/unit identity.
- **Scope:** The exact named boundary, binding, environment, workload, retention horizon, and audience of the claim.
- **Enforcement / evidence owner:** Claimant plus benchmark/security/fault/recovery/delivery/conformance evidence owner; meter-identity equality owner when counts are compared.
- **Resolution, failure, and conformance:** Missing or out-of-scope claim fields/evidence remove the claim rather than creating an implied guarantee; unsupported strengthening is non-conforming.
- **Reuse and absent-trigger behavior:** Reuse only on identical digest/scope; unlike meters remain incomparable, and without evidence omit the claim.
- **Primary verification route:** `§17.9`

### PBA-42 — Honest Guarantee Scope

Source: §9.13.

- **Rule:** Any delivery, durability, recovery, receipt, acceptance, once-only, RPO, or RTO claim is limited to its exact named boundary, scope, assumptions, retention, and evidence; source durability or retained pending work alone does not imply a stronger downstream guarantee.
- **Applicability:** `C`: delivery/durability/recovery/RPO/RTO/receipt/acceptance/once-only claim is made.
- **Declaration owner:** Claimant and binding owner.
- **Scope:** The exact named guarantee boundary, claim scope, assumptions, and retention horizon stated by the claim.
- **Enforcement / evidence owner:** Claimant and binding evidence owner for the named boundary, crash/recovery/delivery behavior, retention, and every downstream non-guarantee.
- **Resolution, failure, and conformance:** Missing boundary, scope, assumptions, retention, or evidence prohibits the claim; source durability or retained pending work alone cannot satisfy or strengthen it.
- **Reuse and absent-trigger behavior:** Failure model/evidence reusable only in identical scope; weaker/no claim needs no table.
- **Primary verification route:** `§17.7`

### PBA-43 — Foundation Quarantine

Source: §14.7.

- **Rule:** When a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, business-policy decision, domain authority, route selection, or hidden communication state. A utility shared across Balls is valid Foundation only under that mechanical-only rule. Domain- or business-semantic helper code remains Ball-local or acquires one Ball/Flow owner and is consumed through its declared Application Surface and protocol; it cannot remain an ownerless shared utility or be relabelled as Foundation.
- **Applicability:** `P`: shared foundation exists or a helper/utility is proposed for use by more than one Ball.
- **Declaration owner:** Project foundation/shared-code owner defines its mechanical-only surface; each Ball/Flow retains its domain semantics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency/ownership/state/call-graph scan for local-versus-shared utilities, policy, authority, routes, service locators, and hidden communication.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One project policy serves all Balls for mechanical Foundation only; domain semantics stay local or Ball/Flow-owned, and the Foundation artifact is omitted when no shared mechanical code exists.
- **Primary verification route:** `§17.5`

### PBA-44 — Trusted Actor Context

Source: §11.2.

- **Rule:**
  - When actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization or result selection, a trusted binding boundary constructs a verified, bounded, field-minimized actor context.
  - Its authenticity and integrity resolve relative to a valid approved issuer or an equivalent fixed trusted same-stack issuer/realm proof before the Ball interprets it.
  - Forged, tampered, wrong-issuer/realm, missing, stale, or otherwise unverifiable required evidence fails closed.
  - Actor context grants no authority by itself.
  - After valid evidence and read admission, the Nucleus/read Policy Gate selects one variant of the Query's total target-owned `ResultPayload`, including the declared denied, redacted, or non-disclosing outcome when policy does not permit ordinary disclosure; it does not return `BusinessRejection` or a post-admission `BoundaryResponse`.
  - An actor-independent Decision or read materializes no actor-context, issuer, authentication, or actor-evidence artifact.

  When the actor-context trigger exists, authentication and raw provenance verification are performed by the trusted binding boundary, not by the request payload or Nucleus. The Ball/Nucleus owns the semantic context schema and interpretation. Fixed issuer/realm facts may be proven by the enclosing trusted type or binding rather than copied into every value.

  Applicability catalog:

  ```text
  AuthenticatedActorContext {
      stableSubjectId
      issuer?                # issuer is not fixed by trusted enclosing scope
      namespaceOrRealm?      # more than one realm/namespace is possible
      authenticationMethod? # method affects policy or evidence
      assuranceLevel?        # assurance affects policy
      authenticatedAt?       # authentication time affects validity/audit
      expiresAt?             # context can expire
      delegation?            # delegation exists
  }
  ```

  The effective `stableSubjectId` must be scoped by issuer and realm, whether those identities are materialized or statically fixed. Credential rotation does not automatically create a new subject.

  The presence of an `issuer` field is not proof of origin. The selected security or isolation profile MUST define verifiable authenticity and integrity of actor context relative to a valid approved issuer or the equivalent fixed trusted same-stack proof. Forged or tampered evidence, evidence from a wrong or unapproved issuer/realm, and missing, stale, or otherwise unverifiable required evidence fail closed before the actor context can authorize or otherwise change a Decision or Query/status-read result. `AuthenticatedActorContext` itself describes the actor but grants no authority to perform an arbitrary action. A path whose Decision and read authorization/result are actor-independent creates no actor-context value, issuer row, authentication artifact, or actor-specific evidence.
- **Applicability:** `R`: actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection.
- **Declaration owner:** Ball semantic owner defines context schema/interpretation; Trusted Boundary/security-binding owner defines construction, approved issuer/static proof, and evidence contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Trusted Boundary verifier plus Decision/read authorization, permitted/denied/redacted/non-disclosing result selection, actor-origin, wrong-provenance, and actor-independent sparsity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact issuer/verifier policy may be reused only in scope; fixed trusted same-stack issuer/realm may be static; actor-independent Decision/read paths omit actor context, issuer data, authentication, and actor-evidence artifacts.
- **Primary verification route:** `§17.8`
<!-- pkb:generated:end -->

### 20.1. Applicability, ownership, and navigation index

This compact table is a nonnormative applicability, ownership, and navigation index. Each row preserves the exact applicability class and trigger, declaration owner, enforcement/evidence owner, source route, and primary test route from its Core-embedded source record. It intentionally omits the rule, scope, resolution, failure, conformance, reuse, and absent-trigger fields; §20 renders the complete tuple. `A`, `P`, `R`, and `C` retain the applicability classes defined in §0.2.

<!-- pkb:generated:start id="core-trace" -->
| Law | Applicability | Declaration owner | Enforcement / evidence owner | Source | Test |
|---|---|---|---|---|---|
| `PBA-01` | `A`: every Ball, including same-file/stack/generated layouts. | Ball owner defines the logical-role map and distinct authority. | Binding source/type graph, role-versus-Trusted-Boundary-edge map, call-graph, provenance, and accepted-write evidence. | §5 | `§17.5` |
| `PBA-02` | `A`: every Ball. | Ball owner defines permitted role edges. | Compiler/dependency/call-graph and hidden-communication tests. | §5.4 | `§17.5` |
| `PBA-03` | `A`: every Decision path; a numeric `maxTransitionSteps` declaration activates the exact meter contract. | Ball/Nucleus owns pure signature, semantic context interpretation, policy, and bounds; binding owns meter mechanics. | Nucleus/binding purity, sole-policy-site, provenance-boundary, termination, static-bound or meter-resolution/determinism tests. | §5.2 | `§17.1` |
| `PBA-04` | `A`: every used protocol category; `P`: admission can fail. | Protocol/profile/binding owner owns the closed variants/reasons. | Type/schema exhaustiveness, compatibility, and unknown/open-reason rejection. | §6.14 | `§17.1` |
| `PBA-05` | `A`: every Decision. | Ball/Nucleus owns State, current Pulse, sparse per-Pulse Context schema/interpretation, and retained-value policy; trusted binding owns context construction/verification. | Context-constructor provenance/bound checks, exact-artifact missing/mismatch/untrusted-version tests, Inline Pulse/Context association across yield/resume, runtime-budget-independence, and transition hidden-input tests. | §8.1 | `§17.1` |
| `PBA-06` | `P`: semantic external-action output exists. | Ball Nucleus/output protocol. | Dependency rule and output tests. | §5.2 | `§17.1` |
| `PBA-07` | `A`: every accepted mutation. | Ball algebra and binding acceptance contract. | Acceptance primitive/fault tests. | §8.5 | `§17.1` |
| `PBA-08` | `P`: a Decision can contain output, including a target result output. | Owning Ball and binding/route ordering contract. | Source/target acceptor, dispatcher/result route, and crash tests. | §8.6 | `§17.1` |
| `PBA-09` | `A`: every mutating transition; `P`: synchronous completion or a causal scope spanning multiple Decisions; a same-stack command adds the exclusive target/carrier alternative completion. | Execution binding and route/source reservation contract. | Reentrancy, levels `0/1/2`, alternative-slot transfer, double-consumption, continuation, exact branch accounting, retry/redelivery/handoff preservation, and depth/fan-out causal-budget `N/N+1` tests. | §8.4 | `§17.1` |
| `PBA-10` | `A`: every pre-acceptance failure; `P`: post-acceptance Resource/delivery fault; `R`: ambiguous execution. | Ball and binding own exact §6.13 stage/failure contract. | Acceptor plus stage mapping, later-failure, dispatcher/executor/status, and fault tests. | §8.8 | `§17.1` |
| `PBA-11` | `A`: every mutable semantic fact. | Project ownership map and owning Ball. | Access boundaries/ownership tests. | §7.2 | `§17.5` |
| `PBA-12` | `A`: sequential acceptance; `R`: owner can move. | Execution/state binding. | Call loop or storage fence and concurrency tests. | §7.6 | `§17.6` |
| `PBA-13` | `A`: every Ball graph. | Ball and Assembly. | Imports/storage boundaries. | §7.4 | `§17.5` |
| `PBA-14` | `A`: every state-like value, including UI/transport values that may affect a Decision. | State/protocol owner; Interaction owns only non-decision mechanics. | Type/state mapping plus decision-relevant UI current-input/retention tests. | §7.1 | `§17.1` |
| `PBA-15` | `P`: work is retained, detached, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible. | Ball state/protocol owner. | Nucleus/runtime identity mapping tests. | §3.5 | `§17.1` |
| `PBA-16` | `P`: Decision needs a newly allocated or trust-validated semantic ID. | Ball and ingress/context issuer. | Allocator/validator, purity/collision, and root reserve/reject/accept identity tests. | §3.5 | `§17.4` |
| `PBA-17` | `P`: result or trusted observation may be late, duplicate, or reordered; status materialization adds causal pending/merge. | Ball or status matching/merge authority. | Matching, bounded-pending, duplicate, conflict, and reordering tests. | §9.2 | `§17.1` |
| `PBA-18` | `P`: Effect/Command/subscription produces a result. | Effect source or command source/target owns semantic mapping; result issuer owns provenance. | Resource/route verifier proves accepted source and, for commands, target tuples plus effective protocol identity. | §9.1 | `§17.1` |
| `PBA-19` | `P`: any §6.13 validation/admission/refusal/accepted-result/Resource/delivery stage is reachable or separately observed. | Ball owns typed outcome/facet meaning; binding owns legal carrier/result/status mapping and same-stack carrier slot transfer. | Stage table, provenance, same-stack carrier level/slot ownership, dispatcher/result-route, duplicate-before-result ACK, exact post-result replay, later-failure, both-order, and conflict tests. | §9.4 | `§17.1` |
| `PBA-20` | `R`: an action may execute without proven outcome after acceptance/possible send. | Operation owner. | Stage/failure mapping, reconciliation, timeout, and fault tests. | §9.5 | `§17.3` |
| `PBA-21` | `P`: duplicate acceptance/delivery/resume/execution is possible. | Operation/API owner. | Root accepted-frame replay/conflict-stage and target pending-ACK/exact-result/executor idempotency tests, including concurrency, crash, and retention horizons. | §9.6 | `§17.1` |
| `PBA-22` | `P`: transport/delivery retry exists. | Route/binding owner. | Dispatcher retry-identity tests. | §9.6 | `§17.3` |
| `PBA-23` | `P`: semantic cancellation can race start/execution/result. | Operation and provider/binding owners. | Nucleus/resource both-order race tests. | §9.7 | `§17.1` |
| `PBA-24` | `P`: any retry layer is active for a failure mode. | Owners of retry layers; exactly one primary owner is designated for each active failure mode. | Exact-one-primary, disabled-or-bounded-transparent secondary-layer, and cumulative-attempt tests. | §9.9 | `§17.3` |
| `PBA-25` | `P`: an application import/dependency or inter-Ball edge exists; utility imports activate their ownership/graph classification, while semantic inter-authority edges activate one of the four complete resolved views. | Each Ball role owns its local utility artifacts; the project Foundation owner owns shared mechanical utilities; each target/participant owns its Application Surface and protocol/read meaning; caller owns read requirements; Flow owns coordination only; Assembly owns route/version/binding. | Utility ownership/import scan, no shared domain-utility check, complete compile-time/direct-control graphs, exact-one read mapping and participation tuple, non-empty bounded Flow coordination, existing dependency-reference/route resolution, ownership, no-copy/no-envelope, and wrong-binding tests. | §10.2 | `§17.5` |
| `PBA-26` | `P`: multi-authority lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome needs an owner. | Specific Flow owner. | Boundary/Flow falsifiers and coordination-ownership tests. | §10.4 | `§17.5` |
| `PBA-27` | `P`: Flow or multi-operation dispatcher exists. | Flow and Assembly. | Closed route graph/no-wildcard tests. | §10.4 | `§17.5` |
| `PBA-28` | `A`: every public protocol; foreign ownership is inspected when referenced. | Public protocol owner. | Ownership index/type review. | §10.8 | `§17.5` |
| `PBA-29` | `P`: routes, participants, fan-out, or multi-hop causality exists. | Producer/Flow owns the causal scope and output branches; Assembly owns effective route/consumer resolution; the binding owns any runtime reservation mechanics. | Static dependency/route resolution plus graph/control-flow proof or route/admission branch accounting, with four-kind declaration ownership/reference/alias tests, one-round-trip route tests, tree, diamond, mixed-route, mutual-exclusion, duplicate/redelivery, handoff, and exact `N/N+1` tests. | §10.9 | `§17.5` |
| `PBA-30` | `P`: Query exists, including during Draining; cross-authority/cache/status/aggregation/consistency triggers add their existing stamp/materializer fields. | Target read/status authority owns mapping/stamp/result; one query writer per namespace; caller owns requirement. | Exact-one/total-outcome/stamp/no-mutation, permitted/denied/redacted/non-disclosing and wrong-provenance, root candidate reserve/reject/accept/query/retry, Draining available/unavailable read, co-located/separate authority, and materializer tests. | §8.10 | `§17.1` |
| `PBA-31` | `P`: raw input or resource output crosses a trust/representation edge. | Interaction/Resource adapter. | Parser/provenance/fuzz tests. | §11.1 | `§17.4` |
| `PBA-32` | `P`: an `EffectRequest` reaches the Ball's external resource/authority. | Ball capability semantics and binding realization. | Binding/resource owner at the scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolation boundary; positive restriction and unrestricted-authority negative tests. | §11.4 | `§17.3` |
| `PBA-33` | `R`: a privileged action needs business and current technical authorization, or an actor-dependent read can select more than one semantic permission/result outcome. Only the gate obligations present on that path materialize. | Ball/Nucleus owns action/read business permission and read result selection; target/resource owns the immediate technical Execution Gate for an action. | Sole-policy-site, gate-order, proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink, and post-acceptance failure tests. | §11.3 | `§17.8` |
| `PBA-34` | `A`: every Ball/binding. | Project dependency and explicit-communication contract. | Imports, call graph, global-state/service-locator/runtime-registry/foundation scans. | §11.5 | `§17.5` |
| `PBA-35` | `P`: data reaches an interpreter/dialect. | Resource/sink owner. | Owning Interaction/Resource sink implementation; parameterized, structured, capability-rooted, and context-encoded positive fixtures plus raw-injection/traversal negatives as applicable. | §11.6 | `§17.3` |
| `PBA-36` | `R`: a secret can enter State, any output, persistence, serialization, logs, or telemetry. | Project classification and affected owner. | Affected State/output owner plus persistence, serializer, log, and telemetry owners; full data-flow and non-log leakage tests. | §11.9 | `§17.8` |
| `PBA-37` | `R`: raw authority/unsafe escape exists. | Named security owner. | Capability gate/scan/audit tests. | §11.10 | `§17.8` |
| `PBA-38` | `A`: present input/state/decision/output dimensions; `P`: numeric decision metering, synchronous command completion, collections, reads/routes/buffers, queues, retries, fan-out, concurrency, IPC, delivery, retention, status, or fallible admission. | Ball/project; binding owns meter identity/unit, each byte-measure identity/version/representation, and source/target reservations; Assembly/runtime/status/profile owner owns other introduced dimensions/reasons. | Meter identity/version/unit resolution; input/State/output byte-measure dimension/identity/version/representation/limit resolution, mapping/erasure invariance, tuple incomparability and exact stage-specific `N/N+1`; equal-input determinism, monotonic no-reset per-`decide` scope, depth/fan-out exact `N/N+1`, alternative-completion transfer, branch identity/aggregation, read bounds, closed reasons, pressure, and overflow tests. | §8.3 | `§17.1` |
| `PBA-39` | `A`: every applicability decision; `P/R/C`: a verdict/decision relies on absence; `P`: adoption/pilot work requests boundary guidance. | Ball/project architecture owner; proof names evidence owner; project owns any worksheet. | Resolver/proof contradiction/invalidation plus worksheet-present/absent and negative-adoption fixtures. | §0.2 | `§17.5` |
| `PBA-40` | `A`: Core design; implementation check when Inline selected. | Core/profile and Inline binding owner. | Build inspection; benchmark only for a claim. | §13.3 | `§17.5` |
| `PBA-41` | `C`: any named performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim is made; cross-binding transition-step comparison additionally requires equal meter/artifact/unit identity. | Claimant names the guarantee boundary, exact binding/scope, mechanism, assumptions, retention, evidence/non-guarantees, and any compared meter/artifact/unit identity. | Claimant plus benchmark/security/fault/recovery/delivery/conformance evidence owner; meter-identity equality owner when counts are compared. | §13.4 | `§17.9` |
| `PBA-42` | `C`: delivery/durability/recovery/RPO/RTO/receipt/acceptance/once-only claim is made. | Claimant and binding owner. | Claimant and binding evidence owner for the named boundary, crash/recovery/delivery behavior, retention, and every downstream non-guarantee. | §9.13 | `§17.7` |
| `PBA-43` | `P`: shared foundation exists or a helper/utility is proposed for use by more than one Ball. | Project foundation/shared-code owner defines its mechanical-only surface; each Ball/Flow retains its domain semantics. | Dependency/ownership/state/call-graph scan for local-versus-shared utilities, policy, authority, routes, service locators, and hidden communication. | §14.7 | `§17.5` |
| `PBA-44` | `R`: actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection. | Ball semantic owner defines context schema/interpretation; Trusted Boundary/security-binding owner defines construction, approved issuer/static proof, and evidence contract. | Trusted Boundary verifier plus Decision/read authorization, permitted/denied/redacted/non-disclosing result selection, actor-origin, wrong-provenance, and actor-independent sparsity tests. | §11.2 | `§17.8` |
<!-- pkb:generated:end -->

---

## 21. Adoption strategy

### 21.1. Start with one vertical slice

Minimal reading path for an adoption decision:

1. Read §§0.1–0.2 for authority, applicability, and reuse semantics.
2. Read §§3–5 for the model, boundary decision tree, and logical roles.
3. Read §§6–8 for the closed protocol, state authority, pure Decision, and acceptance boundary.
4. Read only the triggered parts of §§9–13 for async, composition, security, profile, and bound obligations.
5. Use §14 only when a manifest or Assembly view is materialized; use §§15–16 as worked examples, §§17–18 for evidence/checks, §20 for the complete generated audit projection, §20.1 for quick ownership/navigation, and §22 for definition lookup.

Pokeball is not automatically the right choice. Negative adoption cases include:

- a pure stateless library, formatter, value package, or algorithm with no state, protocol, lifecycle, or resource authority;
- a presentation component whose state is only focus, scroll, animation, layout, or other non-decision mechanics;
- a passive data/adapter path with no owned semantic Decision or local authority, when an ordinary typed adapter is sufficient;
- a project unwilling to establish one writer, explicit authority, closed contracts, and bounded paths—the label would not make its shared mutable state or wildcard communication conforming;
- a pilot in which the project-selected §4.5 runtime/scale and §13.5 design-time measures exceed the authority, invariant, recovery, containment, or change-radius benefit for the selected slice.

In these cases, use ordinary modules/utilities/adapters or stop the pilot; Core makes no claim that every system needs a Ball graph.

A suitable candidate:

- has a clear state lifecycle;
- accepts several input variants;
- performs one or two external effects;
- has observable success/failure;
- is not the most critical central workflow.

### 21.2. Sequence

1. List mutable semantic facts and identify their authority.
2. Choose the boundary based on invariants/lifecycle; materialize a `StateKey` only if more than one instance or a cross-boundary identity path exists.
3. Define only the used closed inputs, state, and outputs. An absent protocol category stays absent.
4. Implement pure `decide` without a DSL or framework.
5. Inventory the reachable paths, risks, selected profiles, and claims; use §20.1 to locate the controlling source records, then resolve their exact triggers and absent-path behavior from those records or the full §20 projection.
6. Resolve always-applicable and triggered guardrails through construction, a local declaration, or an exact project/binding policy plus only the necessary local deltas.
7. Add Interaction/Resource adapters, revision, handles, stale-result, retry, cancellation, status, security, and durability only when their first trigger appears.
8. Add base tests plus tests for the resolved triggers and local deltas; test shared mechanisms once at their scope.
9. Declare dependencies and Assembly routes only for actual inter-Ball edges.
10. Create evidence only for claims actually made; measure runtime cost and change radius when relevant.
11. Introduce a generated manifest view, schema, and linter only after several real Balls or when deployment/conformance requires them.

### 21.3. When to add a Flow

A Flow is added not at the first inter-module call, but when at least one substantial property appears:

- multiple participants and sequence;
- independent workflow identity;
- compensation/reconciliation;
- shared deadline/cancellation;
- a terminal outcome that does not belong to one Feature;
- a lifecycle that outlives the initiating feature state;
- manual intervention queue.

One such property is sufficient if it causes the conditions for a simple one-hop dependency in §10.2 to cease being satisfied together; counting multiple indicators is not required.

### 21.4. When to add durability

A durable state profile is needed when accepted state/work must survive process loss or when an operation or explicit guarantee crosses the process lifetime. Merely outliving the initiating call activates only the applicable detached-work/status contracts and may remain `Transient`.

Until then, `Transient` plus explicit external idempotency where needed is sufficient. Outbox, journals, and recovery infrastructure should not be introduced merely for architectural fashion.

### 21.5. When to add isolation

A process/sandbox boundary is needed when the selected threat model or guarantee requires a separate security principal or resource-containment boundary, including:

- hostile plugins;
- high-risk native parsers/SDKs;
- a credential that must be isolated from less-trusted co-resident code, or a high-privilege credential that requires protection by a separate security principal;
- crash containment;
- tenant isolation;
- enforceable CPU/memory/network limits.

An ordinary scoped external credential does not by itself select `Isolated`. A trusted `InProcess + Hardened` binding may use such a credential when it makes no hostile-component-containment claim and requires no separate principal for that credential; capability restriction, least privilege, the Policy/Execution Gates, and every other triggered security rule still apply.

Logical package separation does not replace a security-principal boundary when one of the conditions above requires that boundary.

### 21.6. Documentation evolution

The topics below are deliberately handed off rather than left as implicit Core behavior. Each handoff names the decision owner, the external artifact required for a stronger claim, the Core guarantee that remains in force, and the claim prohibited without that artifact.

| Handoff topic | Decision owner | Required external artifact | Core guarantee retained | Prohibited claim without the artifact |
|---|---|---|---|---|
| Cryptography and wire format | Security and binding owners | Accepted Secure extension or exact binding contract for primitive/protocol, canonicalization, keys, trust roots, rotation, wire version, and verification evidence | Trusted Boundary and Execution Gate verify required authenticity, integrity, binding, validity, and bounds fail-closed | Concrete cryptographic strength, wire interoperability, confidentiality, non-repudiation, or key-compromise containment. |
| Recovery and executor protocols | Runtime/profile and resource owners | Accepted recovery/executor extension or exact project protocol with crash points, durable authorities, retry/idempotency, liveness, reconciliation, retention, and tests | Atomic Decision acceptance, commit-before-dispatch, bounded attempts, identity preservation, and `OutcomeUnknown` remain mandatory | Crash recovery, eventual delivery, exactly-once execution, successful target acceptance, or loss bounds beyond the named evidence. |
| Certification tooling | Claimant and certification-scheme owner | Versioned conformance procedure, coverage model, implementation/tool identity, fixtures, evidence retention, and false-positive/negative limits | Every Core rule remains source-resolved, trigger-scoped, and objectively reviewable through §§17–18 | Formal certification, complete rule coverage, or equivalence of a lint pass and project conformance. |
| Concrete profile values | Project/profile/binding owner | Exact immutable policy or manifest selection plus workload, environment, bound, benchmark/fault evidence, and review conditions | Every present dimension is finite and every claim is scoped and measured; absent triggers add no placeholder | Universal safe values, suitability for another workload/environment, or a performance/durability/security claim without matching evidence. |
| Project decomposition and scale thresholds | Project architecture owner | Boundary/adoption worksheet using §§4.4–4.5 with actual invariants, lifecycle, authority, workload, change-radius evidence, and continue/reshape/stop criteria | One semantic authority, one writer, one workflow owner, explicit boundaries, and negative-adoption cases remain binding | One uniquely correct Ball graph, a universal size/load threshold, or automatic need for a Flow/Ball from count alone. |

Only material supported by real implementation demand should then be developed as a separate document:

```text
1. Durable Runtime and Recovery
2. Distributed Delivery and Executors
3. Event Sourcing and Replay
4. Secure Isolation and Audit
5. Read Models and Subscriptions
6. Tooling, Schema and Conformance
7. Dynamic Extensions and Ownership Transfer
```

Every extension must:

- have its own scope;
- not increase the mandatory cost of the Core;
- have executable examples;
- record limits and the failure model;
- not claim a guarantee without a reference implementation/tests.

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
