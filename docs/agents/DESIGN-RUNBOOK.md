# Design Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook for a Ball boundary, state, protocol, or Decision change. Its output is a semantic delta in authoritative source, not a second architecture dossier.

This runbook is a projection of the marked Core source clauses. Core owns each obligation's modal force, trigger, semantic/declaration owner, scope, failure behavior, reuse, and absent-trigger behavior.

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
- a pure terminating `DecisionResult = Accepted(Decision) | Rejected(BusinessRejection)`;
- atomic acceptance and no reentrant mutation;
- closed used protocol categories;
- one finite effective bound for every present variable dimension; and
- no ambient global resource authority.

For same-file, same-stack, or generated layouts, record a logical-role and call-graph map covering Interaction/trusted boundary, Nucleus, Resource/route, Assembly, runtime, status/read authority, and shared foundation. Physical packages and adapters may disappear; distinct authority and permitted calls do not. The map traces every trusted cause, context, result, and accepted write to its verified origin and rejects hidden constructors, direct runtime state writes, Assembly-created meaning, service-locator/global inputs, or foundation-mediated business communication.

A local singleton, empty context, immediate output, or same-stack read may be representation-erased exactly where Core permits it. Do not materialize identity, revision, handle, wrapper, lifecycle, or manifest rows merely for uniformity.

Context fields are independently triggered. Reserved semantic IDs appear only when this Decision needs a new trusted ID; actor/configuration/policy/time/limit/validity/version fields appear only when they affect the Decision or their trust/version boundary must be verified. The current `Pulse` is never duplicated in context. The trusted binding boundary verifies bounded observations and constructs `DecisionContext` or actor-dependent `ReadContext`; the Ball/Nucleus owns the semantic schema and interpretation and performs neither raw provenance verification nor I/O.

If actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status authorization or result selection, PBA-44 requires field-minimized context with valid approved-issuer evidence or equivalent fixed trusted same-stack issuer/realm proof. Forged, tampered, wrong-issuer/realm, missing, stale, or unverifiable required evidence fails closed. An actor-independent Decision or read creates no actor context, issuer data, authentication artifact, or actor-specific evidence.

## 3. Boundary and state

Change a boundary only when invariants, authority, lifecycle/recovery, trust, or scaling require it. A screen, endpoint, table, repository, team, call count, or one command hop is not boundary evidence. Pokeball constrains valid authority graphs but does not promise one unique decomposition graph.

Use these falsifiers:

- combine parts only while one authority can enforce their invariants without reading another Ball's mutable state;
- separate parts when mutable-fact authority, invariant enforcement, independent lifecycle/recovery, trust boundary, or scaling owner differs;
- keep a component a utility when it is stateless mechanics with no protocol, lifecycle, resource, or mutable business authority;
- use a Feature Ball for one local capability and state authority;
- use a Read Model Ball only when it owns derived query state, source positions, freshness, and rebuild policy without command authority over source facts; and
- use a Flow Ball when it owns at least one material coordination property: lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or an independent terminal outcome.

For active adoption or pilot work, a project may keep a `SHOULD` boundary worksheet recording candidate authority, invariants, owned state/protocol, falsifiers, cost, and exit decision. It is project-owned guidance, not a universal Core artifact, conformance placeholder, or required Agent Pack file. Prefer an ordinary module or stop the pilot when the candidate is only a stateless library/value/algorithm, presentation-only mechanical state, passive adapter, lacks willingness to establish one writer/closed bounded authority, or costs more than its authority/invariant benefit.

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

Inventory only non-empty owned categories in typed source or one manifest:

```text
Pulse = Intent | Fact | ModuleCommandPulse | ModuleResultPulse
      | ObservedSignal | ControlPulse
Query -> ResultPayload
SemanticOutput = ProjectionOutput | ReplyOutput | EffectRequest
               | ModuleCommandRequest | ModuleResultOutput
               | SignalPublication | TimerRequest
```

Bare `ModuleResult`, top-level `TimerFired`, and `SignalOutput` are not union members or compatibility aliases. Imported `ModuleCommand`/`ModuleResult` payloads remain target-owned and resolve through Assembly. An omitted category is empty. A `Query` remains pure; a stamp/result wrapper is materialized only for the consistency/status/boundary triggers in Core.

Output ownership and dispatch are explicit: `ProjectionOutput` and `ReplyOutput` return through Interaction for encoding/delivery; `EffectRequest` reaches the private Resource interpreter; `ModuleCommandRequest`, `ModuleResultOutput`, and `SignalPublication` follow their declared routes; and `TimerRequest` reaches its declared timer Resource/route. Assembly may bind an owned payload but cannot define, re-own, or synthesize it.

For each command path, the target owns one versioned `ModuleCommand -> ModuleResult` mapping and static refusal classification:

```text
ModuleCommandPulse {
  commandSource, effectiveProtocolIdentity, command, issuerProvenance
}
ModuleResultOutput {
  semanticHandle = commandSource.semanticHandle,
  sourceOrdinal, commandSource, payload: ModuleResult
}
ModuleResultPulse {
  commandSource, resultSource, effectiveProtocolIdentity, result, issuerProvenance
}
result-delivery key = (effectiveProtocolIdentity, commandSource, resultSource)
```

The trusted target boundary constructs the command Pulse only from a verified accepted source frame; target `decide` is the sole acceptance point. Only an accepted target Decision creates the result output, and the verified return route derives `resultSource` from that accepted target frame. Same-stack erasure preserves the exact tuples. `semanticHandle` equality is correlation, not re-export or ownership transfer.

A cross-authority `ReadDependency` resolves one target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Triggered-only version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, or status fields create no placeholders when absent. Pre-read validation/admission may return an existing `BoundaryResponse`; admitted `read(...) -> ReadResult` returns only its successful declared result and creates no accepted marker, Decision, revision, handle, or output. Caller/Assembly cannot alter or re-export payload, status fact, stamp, or meaning, and multiple reads imply no atomic multi-source snapshot.

Specific path checks:

- external mutation or cancellation becomes an `Intent` after validation;
- private-resource outcome becomes a `Fact`;
- accepted target outcome becomes a verified `ModuleResultPulse`;
- post-commit mechanical observation becomes a typed `ControlPulse` only if it affects Sovereign State;
- pre-acceptance validation/admission/Decision rejection is a `BoundaryResponse`; for a command it travels only in verified `CommandRejectedBeforeAcceptance` and never as Reply or result;
- no output type is invented for an absent path.

Classify failures at their exact stage and never rewrite an earlier accepted fact:

| Stage | Legal projection |
|---|---|
| validation before semantic admission | `BoundaryResponse(ValidationFailure)` |
| admission before acceptance | `BoundaryResponse(AdmissionFailure(reason))`; verified command carrier on a command route |
| target Decision rejection before acceptance | `DecisionRejected(BusinessRejection)` inside the verified carrier |
| accepted target business outcome, including rejection | accepted `ModuleResultOutput`, then verified `ModuleResultPulse` |
| post-acceptance Resource/Execution-Gate failure, timeout, or unknown | provenance-bound `Fact` and later accepted target result/status path |
| delivery policy exhaustion | `DispatchStopped` for the exact accepted output/delivery identity |
| programming/invariant fault | runtime fault path; no business rejection, retry, or accepted-frame rewrite |

Every concrete fallible-admission profile/binding uses one finite closed `AdmissionFailure.reason` union and rejects an unknown discriminator or open string before trusted construction. Core's overload list is illustrative; a non-fallible profile has no empty reason union.

## 5. Decision and output

Verify identical explicit inputs produce a semantically equal result, transition code performs no I/O, and capacity cannot rewrite the business choice. Every output field derives from committed State, current Pulse, or present Context.

If outputs exist, prove complete-batch acceptance and commit-before-dispatch. Add a stable `SemanticHandle` only when work is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible. Immediate call-scope output uses accepted sequence position without a domain handle object.

When capacity can race acceptance or the complete candidate frame can exceed an effective cap, preflight or reserve state, outputs, and required continuation together. Admission failure returns before acceptance; it neither rewrites the business Decision nor discards already accepted work.

## 6. Lifecycle and reads

`Draining` rejects new logical mutations while continuing already accepted completion, cancellation, and status inputs. It serves every available declared Query/status Query from its committed authority. If a read is unavailable, the trusted boundary returns only an existing declared validation/admission response before `read`; once admitted, canonical `read` returns only `ReadResult`. Neither path creates a Decision. A status authority has one query writer without acquiring the underlying command or business-fact authority.

A read-like operation uses the accepted command bridge only when accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation is required. Otherwise it is an ordinary `Query`/`ReadDependency` and creates no target Decision, revision, or output.

## 7. Effective bounds and profiles

Resolve each present dimension through exactly one source:

1. bounded type or static control-flow proof;
2. immutable project/profile/binding policy reference; or
3. local declaration or an allowlisted local delta.

Resolve one effective execution/state/isolation/security profile in the same way. A stronger selected profile activates its runbook and tests. A claim activates a separate claim record; profile selection alone does not.

## 8. Change impact

Update only authoritative artifacts affected by the delta:

| Trigger | Possible affected source |
|---|---|
| owned protocol/state/Decision | typed Ball source and transition tests |
| policy selection or override | exact policy ref/local delta and resolver tests |
| inter-Ball edge | Assembly and target contract tests |
| cross-Decision value | state lineage and recovery test if durable |
| persistent schema | migration/quarantine artifact |
| async/security/profile path | corresponding focused runbook tests |
| explicit claim | claim record and full gate evidence |

Shared mechanisms and evidence remain referenced at their accepted digest. Do not copy them into the Ball or overlay.

For a persistent schema change, every old state shape enters ordinary new-version `decide` only after authoritative upcast. Missing evidence or an unsafe mapping routes the record to quarantine/manual remediation; nulls, empty values, generic reasons, logs, or runtime history do not fill newly required semantic fields. Retained old-version outputs keep their original protocol meaning.

## 9. Routine review output

For ordinary work, report:

```text
baseline and affected authority
semantic delta
new/removed triggers
effective declaration or policy ref/delta, if present
tests run
remaining decision, if one blocks the triggered path
```

Use the full review record in [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) only for a conformance or release claim.
