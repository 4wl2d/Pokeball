# Design Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook for a Ball boundary, state, protocol, or Decision change. Its output is a semantic delta in authoritative source, not a second architecture dossier.

This runbook is a projection of the marked Core source clauses. Core owns each obligation's modal force, trigger, semantic/declaration owner, scope, failure behavior, reuse, and absent-trigger behavior.

**Task routing:** task resolution, always-applicable design, boundary, and state remain below. Closed protocol, decisions, outputs, lifecycle, and reads continue in [PROTOCOL-DESIGN-RUNBOOK.md](PROTOCOL-DESIGN-RUNBOOK.md); bounds, profiles, change impact, and review output continue in [BOUNDS-AND-CHANGE-DESIGN.md](BOUNDS-AND-CHANGE-DESIGN.md).

## 1. Resolve the task

Record only facts that can change the result:

```text
affected Ball/authority
business decision and exact semantic delta
changed mutable facts or protocol paths
effective construction proof/local declaration or policy reference and delta, if present
new path, risk, or claim triggers
explicit exclusions
```

Derive triggers from the closed source, profile, route, and reachable-control-flow inventory. Do not copy unchanged policy or list absent surfaces.

## 2. Always-applicable design

Every Ball preserves:

- one semantic authority and one writer;
- logical Interaction/Nucleus/Resource separation without requiring separate classes or files;
- one current typed `Pulse` and a field-minimized `DecisionContext` containing only trusted observations that affect the Decision;
- one pure terminating mutation algebra selected by the state profile: `SnapshotDecisionResult<State> = Accepted(SnapshotDecision<State>) | Rejected(BusinessRejection)` for `Transient`/`SnapshotOutbox`, or `EventDecisionResult = Accepted(EventDecision) | Rejected(BusinessRejection)` for `EventJournal`; these are distinct selected result types, not a runtime union requiring both accepted forms;
- atomic acceptance and no reentrant mutation;
- closed used protocol categories;
- one finite effective bound for every present variable dimension; and
- no ambient global resource authority.

For same-file, same-stack, or generated layouts, identify the present logical roles and Trusted Boundary verification/construction edges in authoritative source and its enclosing binding. Typed entrypoints, visibility/import restrictions, actual call sites, verified construction sites, and accepted-write sites may supply the role/call map directly under Core §5/PBA-01. Add annotations or exact source references only where needed to make the facts inspectable; annotations do not enforce them. Do not create a separate map document or rows for absent paths merely because the roles share a file. The Resource role remains logically present even when it has no operation or implementation artifact. Interaction is a role; Trusted Boundary is an authorized edge commonly realized at Interaction or route ingress, not another authority or a synonym. Physical packages and adapters may disappear; distinct duties, evidence, authority, and permitted calls do not. The map traces every trusted cause, context, result, and accepted write to its verified origin and rejects hidden constructors, direct runtime state writes, Assembly-created meaning, service-locator/global inputs, or foundation-mediated business communication.

The Core `Runtime / acceptor` is only the mechanical binding role for applicable validation handoff, admission/reservation, atomic publication of the selected mode-specific accepted frame, scheduling of its retained outputs, and conversion of verified mechanical observations into declared `ControlPulse` routes. It owns no business schema, policy, permission, read-result selection, retry/fallback choice, or direct Sovereign-State write outside accepted snapshot/event publication and a later serialized `decide`. Inline representation erasure does not transfer those authorities.

A local singleton, empty context, immediate output or same-build call uses the representation its actual contract needs. Verify owner access, acceptance order, isolated accepted reads and serialized completion; do not reconstruct absent carrier fields for tuple equivalence. Materialize identity and provenance only when delayed, reordered, repeated, recovered or independently observed work needs them.

Context fields are independently triggered for the current Pulse. Reserved semantic IDs appear only when this Decision needs a new trusted ID; actor/configuration/policy/time/semantic-limit/validity/version fields appear only when they affect the Decision or their trust/version boundary must be verified. Every version consumed by the Decision or retained as lineage is an explicit named verified current field, or an exact enclosing-binding proof; never read an absent generic `context.artifactVersion` or ambient build value. The current `Pulse` is never duplicated in context. The trusted binding boundary verifies bounded observations and constructs `DecisionContext` or actor-dependent `ReadContext`; the Ball/Nucleus owns the semantic schema and interpretation and performs neither raw provenance verification nor I/O. Each Inline deque or continuation item preserves its own trusted `(Pulse, DecisionContext)` association across ordering and yield/resume; a later `Fact` or `ControlPulse` does not inherit root context fields. Remaining causal budget, fan-out reservation, execution quantum, and current capacity are inputs only to reservation/admission/continuation and never to `decide`.

If actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status authorization or result selection, PBA-44 requires field-minimized context with valid approved-issuer evidence or equivalent fixed trusted same-stack issuer/realm proof. Forged, tampered, wrong-issuer/realm, missing, stale, or unverifiable required evidence fails closed. An actor-independent Decision or read creates no actor context, issuer data, authentication artifact, or actor-specific evidence.

## 3. Boundary and state

Change a boundary only when invariants, authority, lifecycle/recovery, trust, or scaling require it. A screen, endpoint, table, repository, team, call count, or one command hop is not boundary evidence. Pokeball constrains valid authority graphs but does not promise one unique decomposition graph.

Use these falsifiers:

- combine parts only while one authority can enforce their invariants without reading another Ball's mutable state;
- separate parts when mutable-fact authority, invariant enforcement, independent lifecycle/recovery, trust boundary, or scaling owner differs;
- keep stateless mechanics as a Ball-local utility owned by exactly one Ball and logical role, or as shared mechanical Foundation under PBA-43; shared domain/business semantics instead remain explicit local implementations or acquire one Ball/Flow owner and a declared Application Surface/protocol;
- use a Feature Ball for one local capability and state authority;
- use a Read Model Ball only when it owns derived query state, source positions, freshness, and rebuild policy without command authority over source facts; and
- use a Flow Ball when it owns at least one material coordination property: lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or an independent terminal outcome.

For active adoption or pilot work, a project may keep a `SHOULD` boundary worksheet recording candidate authority, invariants, owned state/protocol, falsifiers, cost, and exit decision. The project selects the measurement method and acceptable range for `expectedStateSize`, `peakOpsPerKey`, `maximumTransitionCost`, and any §13.5 design-time-tax or benefit measure; Core supplies no universal Feature/Flow threshold. Continue, reshape, or stop by comparing those measures with the authority, invariant, recovery, containment, and change-radius benefit claimed for the slice. The worksheet is project-owned guidance, not a universal Core artifact, conformance placeholder, or required Agent Pack file. Prefer an ordinary Ball-local utility/module or stop the pilot when the candidate is only stateless mechanics, presentation-only mechanical state, a passive adapter, lacks willingness to establish one writer/closed bounded authority, or costs more than its claimed benefit. Do not use that simplification to create an ownerless shared domain-policy package.

When a materialized instance identity is triggered, resolve:

```text
BallType + namespace + StateKey
single-writer mechanism and movable-owner fencing if ownership can move
```

Classify only state kinds that exist: Sovereign, captured, replica, read-model, projection, ephemeral, or runtime. `EphemeralState` is limited to focus, scroll, animation, parser/socket buffers, and equivalent non-decision mechanics. A UI or transport value that can change a business Decision is committed State or an explicit trusted current `Pulse`/`DecisionContext` input. For a value consumed by a later Decision, record the minimum lineage in the authoritative state/type or its test:

| Value | Source/correlation | Version/provenance | First atomic assignment | Later consumer | Retention or trusted reintroduction |
|---|---|---|---|---|---|

If no value crosses Decisions, this artifact does not exist. A persistent shape change increments `stateSchemaVersion` and activates migration/quarantine work; a transient source refactor does not.


## 4. Closed protocol

Moved to [Closed protocol](PROTOCOL-DESIGN-RUNBOOK.md#4-closed-protocol).

## 5. Decision and output

Moved to [Decision and output](PROTOCOL-DESIGN-RUNBOOK.md#5-decision-and-output).

## 6. Lifecycle and reads

Moved to [Lifecycle and reads](PROTOCOL-DESIGN-RUNBOOK.md#6-lifecycle-and-reads).

## 7. Effective bounds and profiles

Moved to [Effective bounds and profiles](BOUNDS-AND-CHANGE-DESIGN.md#7-effective-bounds-and-profiles).

## 8. Change impact

Moved to [Change impact](BOUNDS-AND-CHANGE-DESIGN.md#8-change-impact).

## 9. Routine review output

Moved to [Routine review output](BOUNDS-AND-CHANGE-DESIGN.md#9-routine-review-output).
