# Core part — Adoption strategy

[Core contents](../../pokeball-architecture-core.md) · [← Law applicability and navigation index](20-04-law-index.md) · [Glossary and canonical statement →](22-glossary-and-statement.md)

> Canonical part 23 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 21. Adoption strategy

### 21.1. Start with one vertical slice

Minimal reading path for an adoption decision:

For a first implementation, start from the local typed-source contract in §§14.1–14.2 before adding optional mechanisms. For an ordinary change in an established project, use §21.7. The authoritative route below is for resolving the adoption/binding choices; it is not a prerequisite to every feature edit.

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

### 21.7. Everyday development and production responsibility

An ordinary feature change starts from the owning source, its behavior tests, and the project's effective binding. It does not start by reproducing the full Core inventory. The following workflow applies the existing responsibilities in §§0.2, 5, 8, 13.5, and 14; it introduces no additional runtime role, required document, profile, or conformance shortcut.

| Work | What the responsible developer resolves | Reused within its existing exact scope |
|---|---|---|
| Change a feature's business rule | Owned State and inputs, the pure Decision/read, changed consequences and behavioral tests | Existing writer, acceptance, adapter and bound mechanisms whose assumptions remain true. |
| Add an input, effect or dependency | Its closed type and authority, representation/trust boundary, reachable failure cases, new bounds and causal/lifecycle obligations | Existing verified ingress, resource and route bindings that actually cover the new path. |
| Implement or replace a binding | Applicable writer/admission/atomic-acceptance/dispatch mechanics, provenance, finite execution and profile-specific failure evidence | One implementation and its tests can cover multiple features with equal effective contracts. |
| Approve a production deployment or publish a guarantee | Exact project revision, binding, workload/environment, intended guarantee boundary, relevant tests/operational evidence and invalidation conditions | Existing in-scope evidence only; a source guide or a successful lint cannot supply missing deployment evidence. |

For a routine change:

1. Locate the owner of the mutable fact and the existing decision/read that owns the rule.
2. Change the smallest relevant source and behavior test. Keep binding mechanics reusable and business choices in the owner.
3. Check whether the change introduces or alters an external action, detached work, another authority, persistence, trust, variable dimension, or explicit claim. Follow only the affected source and verification routes in §§0.4–0.5; a new trigger is not covered merely because the old code was reviewed.
4. Reuse unchanged mechanism/evidence at its exact scope. Revisit it when the source, profile, bounds, dependency, environment or evidence assumptions change; no fresh copy of unchanged policy is needed.

The same person may perform all of these jobs in a small project. Assigning responsibility does not require a platform team or a framework. Before reuse is possible, someone still implements and verifies the selected binding; Core does not ship one. Source-backed role/edge evidence under §5 and authoritative typed contracts under §14.1 can make the local implementation reviewable without a second architecture document. An unresolved required mechanism remains unresolved, even if a short guide omits it.

For adoption, use the project-selected comparison in §§4.5/13.5. A human exercise checks whether a developer can complete a useful change from the task guide and project source without mastering the whole reference; an agent's successful walkthrough does not establish that result. Generality across languages or application types is not evidence of suitability for every project. Production readiness belongs to the exact implemented system and claimed boundary under §13.4, not to the architecture's name.

---
