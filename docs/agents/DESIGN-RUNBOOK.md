# Design Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook for a Ball boundary, state, protocol, or Decision change. Its output is a semantic delta in authoritative source, not a second architecture dossier.

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

A local singleton, empty context, immediate output, or same-stack read may be representation-erased exactly where Core permits it. Do not materialize identity, revision, handle, wrapper, lifecycle, or manifest rows merely for uniformity.

Context fields are independently triggered. Reserved semantic IDs appear only when this Decision needs a new trusted ID; actor/configuration/policy/time/limit/validity/version fields appear only when they affect the Decision or their trust/version boundary must be verified. The current `Pulse` is never duplicated in context. If actor, tenant, issuer, realm, assurance, or delegation can change the Decision, PBA-44 requires field-minimized context from a Trusted Boundary with valid approved-issuer evidence; an actor-independent path creates no actor artifact.

## 3. Boundary and state

Change a boundary only when invariants, authority, lifecycle/recovery, trust, or scaling require it. A screen, endpoint, table, repository, or team is not boundary evidence.

When a materialized instance identity is triggered, resolve:

```text
BallType + namespace + StateKey
single-writer mechanism and movable-owner fencing if ownership can move
```

Classify only state kinds that exist: Sovereign, captured, replica, read-model, projection, ephemeral, or runtime. For a value consumed by a later Decision, record the minimum lineage in the authoritative state/type or its test:

| Value | Source/correlation | Version/provenance | First atomic assignment | Later consumer | Retention or trusted reintroduction |
|---|---|---|---|---|---|

If no value crosses Decisions, this artifact does not exist. A persistent shape change increments `stateSchemaVersion` and activates migration/quarantine work; a transient source refactor does not.

## 4. Closed protocol

Inventory only non-empty owned categories in typed source or one manifest:

```text
Intent
Query -> ResultPayload
Fact
ObservedSignal
ControlPulse
Projection / Reply
Effect / Signal / Timer
```

Imported `ModuleCommand`/`ModuleResult` contracts remain target-owned and resolve through Assembly. An omitted category is empty. A `Query` remains pure; a stamp/result wrapper is materialized only for the consistency/status/boundary triggers in Core.

Output ownership and dispatch are explicit: `ProjectionOutput` and `ReplyOutput` return through Interaction for encoding/delivery; `EffectRequest` reaches the private Resource interpreter; `ModuleCommandRequest` and `SignalPublication` follow their declared Assembly routes; and `TimerRequest` reaches its declared timer Resource/route. Assembly may bind an owned payload but cannot define or synthesize it.

Specific path checks:

- external mutation or cancellation becomes an `Intent` after validation;
- private-resource outcome becomes a `Fact`;
- target outcome becomes a `ModuleResult`;
- post-commit mechanical observation becomes a typed `ControlPulse` only if it affects Sovereign State;
- pre-acceptance validation/admission/decision rejection is a `BoundaryResponse`, with runtime or shared-capacity overload represented as `AdmissionFailure`, never `BusinessRejection`; and
- no output type is invented for an absent path.

## 5. Decision and output

Verify identical explicit inputs produce a semantically equal result, transition code performs no I/O, and capacity cannot rewrite the business choice. Every output field derives from committed State, current Pulse, or present Context.

If outputs exist, prove complete-batch acceptance and commit-before-dispatch. Add a stable `SemanticHandle` only when work is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible. Immediate call-scope output uses accepted sequence position without a domain handle object.

When capacity can race acceptance or the complete candidate frame can exceed an effective cap, preflight or reserve state, outputs, and required continuation together. Admission failure returns before acceptance; it neither rewrites the business Decision nor discards already accepted work.

## 6. Effective bounds and profiles

Resolve each present dimension through exactly one source:

1. bounded type or static control-flow proof;
2. immutable project/profile/binding policy reference; or
3. local declaration or an allowlisted local delta.

Resolve one effective execution/state/isolation/security profile in the same way. A stronger selected profile activates its runbook and tests. A claim activates a separate claim record; profile selection alone does not.

## 7. Change impact

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

## 8. Routine review output

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
