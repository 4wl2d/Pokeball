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
- one pure terminating mutation algebra selected by the state profile: `SnapshotDecisionResult<State> = Accepted(SnapshotDecision<State>) | Rejected(BusinessRejection)` for `Transient`/`SnapshotOutbox`, or `EventDecisionResult = Accepted(EventDecision) | Rejected(BusinessRejection)` for `EventJournal`; these are distinct selected result types, not a runtime union requiring both accepted forms;
- atomic acceptance and no reentrant mutation;
- closed used protocol categories;
- one finite effective bound for every present variable dimension; and
- no ambient global resource authority.

For same-file, same-stack, or generated layouts, record a logical-role and call-graph map covering the Interaction adapter role, Nucleus, Resource/route, Assembly, runtime, status/read authority, and shared foundation, and label each Trusted Boundary verification/construction edge separately. Interaction is a role; Trusted Boundary is an authorized edge commonly realized at Interaction or route ingress, not another authority or a synonym. Physical packages and adapters may disappear; distinct duties, evidence, authority, and permitted calls do not. The map traces every trusted cause, context, result, and accepted write to its verified origin and rejects hidden constructors, direct runtime state writes, Assembly-created meaning, service-locator/global inputs, or foundation-mediated business communication.

The Core `Runtime / acceptor` is only the mechanical binding role for applicable validation handoff, admission/reservation, atomic publication of the selected mode-specific accepted frame, scheduling of its retained outputs, and conversion of verified mechanical observations into declared `ControlPulse` routes. It owns no business schema, policy, permission, read-result selection, retry/fallback choice, or direct Sovereign-State write outside accepted snapshot/event publication and a later serialized `decide`. Inline representation erasure does not transfer those authorities.

A local singleton, empty context, immediate output, or same-stack read may use representation erasure—removing a runtime wrapper or object while preserving the same typed fields, authority, ordering, and evidence—exactly where Core permits it; see Core §22. Do not materialize identity, revision, handle, wrapper, lifecycle, or manifest rows merely for uniformity.

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

Inventory only non-empty owned categories in typed source or one manifest:

```text
Pulse = Intent | Fact | ModuleCommandPulse | ModuleResultPulse
      | ObservedSignal | ControlPulse
Query -> ResultPayload
SemanticOutput = ProjectionOutput | ReplyOutput | EffectRequest
               | ModuleCommandRequest | ModuleResultOutput
               | SignalPublication | TimerRequest
```

Bare `ModuleResult`, top-level `TimerFired`, and `SignalOutput` are not union members or compatibility aliases. A Nucleus may import the exact declared target- or producer-owned Application Surfaces needed by its closed Query, Pulse, and Decision contracts. Imported `Query`/result, `ModuleCommand`/`ModuleResult`, and Signal payload types remain target/producer-owned and resolve through their declared dependency and Assembly binding; import transfers no ownership. Reject foreign mutable State, internals, private Resource adapters, caller-owned mirrors or redeclarations, protocol re-export, and any semantic contract synthesized by Interaction or Assembly. An omitted category is empty. Each `Query` maps to one target-owned `ResultPayload` that is total over its reachable post-admission semantic outcomes; it declares only applicable permitted, denied, redacted, or deliberately non-disclosing variants, and no boundary, caller, Assembly, runtime, or binding invents another. A `Query` remains pure; a stamp/result wrapper is materialized only for the consistency/status/boundary triggers in Core.

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

The trusted target boundary constructs the command Pulse only from a verified accepted source frame; target `decide` is the sole acceptance point. Only an accepted target Decision creates the result output, and the verified return route derives `resultSource` from that accepted target frame. Same-stack erasure preserves the exact tuples. `semanticHandle` equality is correlation, not re-export or ownership transfer. Within the idempotency horizon, duplicate-before-result returns only verified ACK proof of the existing accepted target frame and pending-result state; duplicate-after-result redelivers the exact accepted result frame. Neither invokes another Decision, increments revision, fabricates a result, or repeats a Resource action.

For a same-stack command, the source pre-reserves one level-1 alternative-completion slot. Accepted target `decide` consumes it and separately reserves the level-2 result completion. Validation, admission, or target `decide` rejection before acceptance consumes no target level/frame/revision/output and atomically transfers level 1 to the source carrier-handling `decide(ControlPulse)`. Missing level 1 prevents source acceptance/dispatch; missing level 2 prevents target acceptance and returns `AdmissionFailure(CausalBudgetExceeded)` through the carrier. Any further synchronous output from the carrier-handling Decision reserves normally from the remaining total budget.

A cross-authority `ReadDependency` resolves one target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Triggered-only version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, or status fields create no placeholders when absent. Pre-read validation/admission may return an existing `BoundaryResponse`; admitted `read(...) -> ReadResult` returns only its successfully evaluated target-owned result, whose payload closes every reachable post-admission permitted, denied, redacted, or deliberately non-disclosing outcome, and creates no accepted marker, Decision, revision, handle, or output. Caller/Assembly cannot alter or re-export payload, status fact, stamp, or meaning, or substitute `BusinessRejection`, exception, post-admission `BoundaryResponse`, or undeclared `NotFound`; multiple reads imply no atomic multi-source snapshot.

Specific path checks:

- external mutation or cancellation becomes an `Intent` after validation;
- root same-key/same-fingerprint retry redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with all semantic/frame identity unchanged and only a new `AttemptId`; different fingerprint is pre-Intent `ValidationFailure(IdempotencyConflict)`, never Nucleus `DecisionRejected`;
- private-resource outcome becomes a `Fact`;
- accepted target outcome becomes a verified `ModuleResultPulse`;
- post-commit mechanical observation becomes a typed `ControlPulse` only if it affects Sovereign State;
- pre-acceptance validation/admission/Decision rejection is a `BoundaryResponse`; for a command it travels only in verified `CommandRejectedBeforeAcceptance` and never as Reply or result;
- no output type is invented for an absent path.

Classify a predicate by its declared owner and inputs, not by whether it resembles a range, size, or shape check. Representation or a declared closed-protocol/type invariant independent of committed State, semantic Context, and business policy belongs to Interaction. A valid typed rule that depends on State/Context or is owned as a business choice belongs to the Nucleus. Promoting a fixed constraint into the protocol/type invariant changes the effective versioned protocol semantics; a binding cannot switch stages dynamically for one identity.

Classify failures and admitted read outcomes at their exact stage and never rewrite an earlier accepted fact:

| Stage | Legal projection |
|---|---|
| representation or declared closed-protocol/type validation before semantic admission | `BoundaryResponse(ValidationFailure)`; no `Intent`/`Query` reaches the Nucleus |
| valid typed State/Context/business rule during mutation | `Rejected(BusinessRejection)` before acceptance |
| valid typed State/Context/business rule during an admitted Query | one target-owned `ResultPayload` variant inside the successfully evaluated read result |
| fixed constraint deliberately promoted to a closed protocol/type invariant | `ValidationFailure` only under the corresponding new effective protocol identity; same-identity stage switching is invalid |
| admission before acceptance | `BoundaryResponse(AdmissionFailure(reason))`; verified command carrier on a command route |
| target Decision rejection before acceptance | `DecisionRejected(BusinessRejection)` inside the verified carrier |
| accepted target business outcome, including rejection | accepted `ModuleResultOutput`, then verified `ModuleResultPulse` |
| post-acceptance Resource/Execution-Gate failure, timeout, or unknown | provenance-bound `Fact` and later accepted target result/status path |
| delivery policy exhaustion | `DispatchStopped` for the exact accepted output/delivery identity |
| programming/invariant fault | runtime fault path; no business rejection, retry, or accepted-frame rewrite |

Every concrete fallible-admission profile/binding uses one finite closed `AdmissionFailure.reason` union and rejects an unknown discriminator or open string before trusted construction. Core's overload list is illustrative; a non-fallible profile has no empty reason union.

## 5. Decision and output

Verify identical explicit inputs produce a semantically equal result—equal in the fields that define the semantic value, independent of runtime representation—transition code performs no I/O, and capacity cannot rewrite the business choice. In particular, equal State/Pulse/valid per-Pulse Context/artifact produces the same candidate Decision under different remaining runtime causal budgets; only admission or continuation may differ. Every output field derives from committed State, current Pulse, or present Context.

Verify the exact selected accepted form. Snapshot profiles publish the flattened semantic tuple `AcceptedSnapshotDecisionFrame<State> { commitRevision?, nextState, outputs }`. EventJournal records `AcceptedEventCommit` containing `decision: EventDecision(EventMutation | NoDomainChange)` and reconstructs State through `evolve`; it never adds an independent Event `nextState`. A binding implements only the form chosen by its state profile.

If outputs exist, prove complete-batch acceptance and commit-before-dispatch. When numeric `maxOutputBytesPerDecision` is present, the `DecisionOutputs` dimension of the binding's immutable `BoundedByteMeasure` covers the complete ordered `Decision.outputs` semantic representation. Count sequence structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload; exclude `nextState`, accepted-frame fields outside an output envelope, and later transport framing, compression, encryption, retry headers, and `AttemptId`. Add a stable `SemanticHandle` only when work is detached, retained, retryable, reorderable, cancellable, recoverable, cross-Ball, or status-visible. Immediate call-scope output uses accepted sequence position without a domain handle object.

When capacity can race acceptance or the complete candidate frame can exceed an effective cap, preflight or reserve state, outputs, and required continuation together. Admission failure returns before acceptance; it neither rewrites the business Decision nor discards already accepted work.

## 6. Lifecycle and reads

`Draining` rejects new logical mutations while continuing already accepted completion, cancellation, and status inputs. It serves every available declared Query/status Query from its committed authority. If a read is unavailable, the trusted boundary returns only an existing declared validation/admission response before `read`; once admitted, canonical `read` returns only the successfully evaluated `ReadResult`, whose target-owned payload still covers every reachable permitted, denied, redacted, or deliberately non-disclosing outcome. Neither path creates a Decision. A status authority has one query writer without acquiring the underlying command or business-fact authority.

A read-like operation uses the accepted command bridge only when accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation is required. Otherwise it is an ordinary `Query`/`ReadDependency` and creates no target Decision, revision, or output.

`OperationId` names an accepted root operation. Reservation supplies only a candidate input to a possible Decision. Root validation, admission, idempotency conflict, or `decide` rejection returns its typed `BoundaryResponse` and creates no operation, authoritative handle, output, known status row, or retention marker. Same-fingerprint replay instead redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame with its accepted `BallInstanceId`, `CommitRevision`, materialized `OutputId`, `semanticHandle`, `sourceOrdinal`, payload, `OperationId`, and accepted Interaction artifact/fingerprint lineage unchanged and only a new `AttemptId`. `NotFound` for a rejected candidate is legal only under a committed namespace-covering snapshot and the ordinary materializer-absence barrier. A participant `CommandRejectedBeforeAcceptance` remains a Step acceptance facet of an already accepted source operation; it never becomes the rejected root lifecycle.

## 7. Effective bounds and profiles

Resolve each present dimension through exactly one source:

1. bounded type or static control-flow proof;
2. immutable project/profile/binding policy reference; or
3. local declaration or an allowlisted local delta.

When a numeric `maxTransitionSteps` declaration supplies the decision-work bound rather than a static proof alone, resolve exactly:

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

The identity/version selects one immutable unit definition. For equal binding, `transitionArtifactVersion`, meter identity/version, canonical State, Pulse, and valid Context, the total is deterministic; units are non-negative and consumption monotonic. Counting starts once at zero for each `decide` and cannot reset, split, or restart in helpers, phases, loops, retries, yields, or representation erasure. Completion at `N` is legal; attempting `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and follows the binding's finite typed pre-acceptance or programming-fault policy without truncating work into another business result. Compare numeric counts across bindings only when meter identity, meter version, transition artifact version, and unit definition all match; Core defines neither a universal unit nor bit-exact cross-language replay.

Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without a static type-and-representation proof resolves one exact immutable binding-owned `BoundedByteMeasure`:

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

`Input` selects exactly `RawBoundaryInput` or `NormalizedTrustedInput`, including the declared boundary metadata and Context fields, and rejects `N+1` before trusted semantic acceptance or `decide`. `State` measures the complete candidate `nextState` semantic representation, including declared semantic metadata and excluding heap, allocator, index, storage, compression, encryption, and transport mechanics; `N+1` rejects the whole Decision before State, revision, or output acceptance. `DecisionOutputs` measures the complete ordered output sequence, including its structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload, and excludes `nextState`, non-envelope accepted-frame fields, and later transport, framing, compression, encryption, retry, and attempt data. Any alternate retained representation maps exactly to the selected semantic representation, representation erasure preserves counts for equal values, and unequal dimension/identity/version/representation/limit tuples are incomparable. Exact `N` may pass; no value is truncated and no Decision is partially accepted. A static proof needs no measure runtime artifact.

For `maxDeclaredDependenciesPerBall`, count each distinct resolved read, command, signal, and FlowParticipation declaration once; references do not erase the referenced declaration's own unit, multiple operations remain distinct, and exact duplicates or aliases/equivalent rows resolving to an existing identity are invalid. Static resolution accepts exactly `N` declarations and rejects the Ball contract when a first distinct declaration would make `N+1`, before execution. For `maxRoutesPerFlow`, count only distinct effective Assembly command/result round-trip mappings: command ingress plus its bound result return is one unit, different operations remain distinct, and any exact duplicate, spelling alias, equivalent repeated row, or split-leg alias is invalid. Reads and signals use their own bounds, while `FlowParticipation` and `dependencyRefs` create no unit of this command-route limit. Static resolution accepts exactly `N` mappings and rejects the Flow/Assembly contract at `N+1`, before execution.

When `maxCumulativeFanout` is present, use Core §10.9 and [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md): count distinct accepted source-output-to-effective-route/consumer branches across one causal scope, sum co-reachable/terminal/converging traversals, share the maximum reservation for mutually exclusive alternatives, ignore retry/redelivery of the same tuple and route, preserve scope across async handoff, and reject the complete over-limit Decision at exact `N+1` without partial dispatch. This reservation is admission state, never Context.

Resolve one effective execution/state/isolation/security profile in the same way. A stronger selected profile activates its runbook and tests. A claim activates a separate record naming the exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees; profile selection or source durability alone does not establish a stronger downstream guarantee.

## 8. Change impact

Update only authoritative artifacts affected by the delta:

| Trigger | Possible affected source |
|---|---|
| owned protocol/state/Decision | typed Ball source and transition tests |
| policy selection or override | exact policy ref/local delta and resolver tests |
| inter-Ball edge | owner-authored Application Surface, caller Nucleus closed contract, Assembly, and target/producer contract tests |
| helper extraction or sharing | owning Ball/logical role or mechanical Foundation classification, compile-time graph, and shared-domain ownership test |
| cross-Decision value | state lineage and recovery test if durable |
| persistent schema | migration/quarantine artifact |
| async/security/profile path | corresponding focused runbook tests |
| explicit claim | named-boundary/scope/assumptions/retention/evidence record and full gate evidence |

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
