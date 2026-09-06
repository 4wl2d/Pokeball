# Core part — Status, scope, and goals

[Core contents](../pokeball-architecture-core.md) · [← Core entrypoint](../pokeball-architecture-core.md) · [Model, boundaries, and zones →](03-model-boundaries-zones.md)

> Canonical part 1 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 0. Document status and scope

This ordered canonical document set is the sole normative text for understanding and implementing the Pokeball Architecture baseline. It describes the architectural core, the minimum executable semantics, and profile-specific extensions only to the extent required to design a real application.

> `canonical draft` is the current working truth rather than a stable compatibility promise. `1.5.0-draft` permits immediate same-build typed calls and returns without mandatory handles, provenance tokens or protocol identifiers; a Ball crossing or temporary call-local retention alone activates none of them. Complete structurally finite execution replaces mandatory static graph ceilings, carried causal levels and completion-slot protocols. Delayed, reordered, repeated, recovered or independently observed work still requires the identity and source verification its contract needs; growing, queued, external and retained work retains real capacity and no-accepted-loss obligations. State isolation is observable and permits immutable sharing. Existing laws and semantic roles remain one Core; consumers re-resolve affected bindings against this exact baseline.

This Core is intentionally not a specification for a cloud platform, message broker, workflow engine, IAM system, disaster-recovery framework, or conformance-certification service.

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

The following separate future specifications remain outside this Core:

```text
Durable Runtime and Recovery
Distributed Delivery and Executors
Event Sourcing and Replay
Secure Isolation and Audit
Read Models and Subscriptions
Tooling, Schema and Conformance
Dynamic Extensions and Ownership Transfer
```

The absence of these extension specifications does not prevent use of the baseline architecture. It only prohibits attributing stronger guarantees to this Core when those guarantees require a separate formal protocol.

### 0.1. Normative words

- **MUST** — a mandatory rule when the applicability trigger owned by its marked primary source clause is true within the stated scope.
- **MUST NOT** — a prohibited construction.
- **SHOULD** — a recommended rule; deviation requires a deliberate rationale.
- **MAY** — a permitted option.

A primary source clause is the visible `pkb:pba-source` record marked `Source clause for PBA-xx` in the numbered body. That record is the sole normative authority for the corresponding law. Its eight fields state the Rule, Applicability, Declaration owner, Scope, Enforcement/evidence owner, Resolution/failure/conformance behavior, Reuse/absent-trigger behavior, and Primary verification route exactly once. Every record incorporates the resolution, reuse, absence, and invalid-reference rules of §0.2 unless its Rule states a stricter local rule.

Within that compact record, `Declaration owner` names the accountable semantic or architectural owner. `Enforcement / evidence owner` names the enforcing role when one exists, the owned static or behavioral evidence route when enforcement is proved by evidence, or both; the slash does not require a second owner or duplicate artifact. The separate `Primary verification route` supplies the navigation target. A field that names fixtures or a scan therefore does not transfer architectural ownership from the declaration owner.

Section §20 is the complete set-equal human-readable projection of those records. Section §20.1 is deliberately narrower: it is a generated navigation and ownership index, not an independently complete law statement. Tests in §17, the checklist in §18, and examples in §§15–16 are verification or teaching projections and cannot complete a rule. Each definition marked `pkb:term` in §§0–21 is the sole definition source for its term; §22 is its exact generated lookup projection. If any projection differs from its source record, the source record controls. A projection may repeat `MUST` or `MUST NOT` only when its complete tuple is set-equal (§22).

The comments delimiting source and generated regions are maintenance structure inside this one canonical document set. The rendered Core is complete without executing a generator. Generation and linting are not runtime mechanisms, project implementation requirements, certification, or conformance evidence; they prove only structural identity between the visible source records and their designated projections.

The following is an authoritative Core-documentation maintenance rule rather than a PBA law: the maintainer of this canonical Core owns every diagram introduced or changed in this Core. Every new or changed Mermaid diagram in Core carries a local explicit legend that distinguishes semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows. If one of those distinctions is absent, the documentation change is not eligible for acceptance or publication until corrected. This rule does not make a consuming project's diagram or binding non-conforming unless that project's accepted policy separately adopts it; §17.5 and §18.1 are evidence projections of this exact Core-only rule.

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
| Make an ordinary change inside an established project binding | §21.7; the affected source and tests; the applicable §0.4 change row and newly triggered source/test routes | Relearning unchanged binding internals, every profile, or the complete law catalog for each feature edit. |
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

`ProjectionOutput` and `ReplyOutput` return to Interaction for presentation or response encoding. `EffectRequest`, `ModuleCommandRequest`, `ModuleResultOutput`, `SignalPublication`, and `TimerRequest` leave through Resources or an explicit route. A command reaches the target owner and its result returns to the source owner. These are logical directions: an immediate same-build typed call and return preserve them through actual target access, acceptance order and serialized source completion without reconstructing absent envelope fields (§6.9). Independently delivered messages retain their required verified causal representation.

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
