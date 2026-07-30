# Manifest and Assembly Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

A manifest is an optional materialized view, not a mandatory source file or runtime registry. Prefer one authoritative typed source and generate a resolved view for tooling or claims instead of copying protocol, policy, and evidence.

This runbook is a projection of the marked Core source clauses. Core controls any conflict in trigger, owner, scope, failure semantics, reuse, or absence behavior.

## 1. Authority map

| Fact | Sole authoritative location |
|---|---|
| owned state/protocol/Decision entry | typed Ball source or one manifest |
| Ball-local utility | owning Ball and one logical role in typed source/compile-time graph; no semantic dependency or Assembly route row |
| shared utility | mechanical Foundation source/policy only; domain/business semantics remain local or acquire one Ball/Flow owner and an Application Surface/protocol |
| owned `Application Surface` | owning Ball/Flow source or one manifest; only its public semantic types and entrypoints, imported by a caller Nucleus only when required by a closed Query/Pulse/Decision contract |
| inter-Ball routes and effective protocol identities; explicit versions when independently versioned | Assembly |
| one `FlowParticipation` declaration per Flow/participant pair | Flow/participant composition source; referenced read/command/signal declarations remain authoritative for route contracts |
| triggered `DecisionWorkMeter` | exact binding/transition declaration or immutable in-scope policy reference |
| shared profiles, limits, bindings, ceilings | static/local declaration or optional exact project/binding policy |
| Ball-specific fact or difference | local declaration or allowlisted delta |
| deliberate deviation and conformance effect | exact eight-field `WaiverRecord` |
| claim evidence | exact named boundary/scope, mechanism, assumptions, retention, evidence, and non-guarantees in one claim record |
| absence used by a conformance/release verdict or accepted ambiguity decision | exact evidence-owner `TriggerAbsenceProof`, outside routine Ball metadata |

An independently versioned owned category is declared inline or by one exact authoritative reference. A same-build closed type is already the inventory. An absent category is empty and has no row. A physical helper-package boundary is classified before the semantic dependency taxonomy: every import remains in the acyclic compile-time graph, but a Ball-local or mechanical-Foundation utility is not a fifth protocol dependency and creates no route. An ownerless shared domain/business utility is invalid rather than a manifest category.

Every Query that exists maps to one target-owned result payload that is total over all reachable post-admission semantic outcomes. That payload contains only the applicable target-declared permitted, denied, redacted, or deliberately non-disclosing variants; an intentionally denial-as-absence meaning exists only as one explicit tested target variant. A cross-authority `ReadDependency` resolves exactly one caller, target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Protocol version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. Caller and Assembly never redeclare, re-export, synthesize, or alter target payload, status fact, stamp, snapshot identity, or business meaning, and neither they nor boundary/runtime substitutes `BusinessRejection`, exception, post-admission `BoundaryResponse`, or undeclared `NotFound`.

Imported `ModuleCommand`/`ModuleResult` payload types remain target-owned. The target declares one exact versioned mapping and statically classifies every refusal as verified pre-acceptance carrier or accepted target result. The caller imports that mapping through `dependencies.commands`; Assembly binds accepted-source ingress and accepted-target-frame return through one edge without business authority.

Every routed Signal likewise resolves exactly once in the producer-owned protocol. Assembly owns the endpoints, version pair, delivery binding, and route limits; it cannot define, synthesize, or re-own the producer payload. Catalog's canonical version-2 route therefore resolves `ProductSelectionConfirmed` from `CatalogSignal` and pins the independently versioned producer/consumer pair to `2.0.0/2.0.0`.

An `Application Surface` contains only owner-authored public semantic types and entrypoints deliberately exposed to another Ball or Assembly. A Nucleus may import the exact declared target- or producer-owned surfaces required by its closed Query, Pulse, and Decision contracts, but import transfers no ownership. Mutable State, internals, runtime/transport mechanics, private Resource adapters, caller-owned mirrors or redeclarations, and re-exported foreign contracts are excluded; Interaction and Assembly cannot synthesize the imported semantics. Each Flow/participant pair resolves once through this declaration view:

```text
FlowParticipation {
  flowAuthority
  participantAuthority
  participantApplicationSurface
  flowOwnedCoordination: NonEmptyBoundedSet<
    lifecycle | orderingOrBranchJoin | compensationOrRecovery
    | cancellation | reconciliation | terminalOutcome
  >
  dependencyRefs: BoundedSet<
    ReadDependencyRef | DeclaredCommandDependencyRef | DeclaredSignalDependencyRef
  >
}
```

This is not a runtime envelope or route protocol. The dependency references preserve their existing owner, payload, effective identity, route, return binding, limit, and triggered fields. Semantic ownership, Application Surface import, synchronous direct control, asynchronous data path, and Assembly route/version/binding remain separately inspectable; the Application Surface import is recorded in both the compile-time-import and `Direct Control Dependency` graphs.

## 2. Exact policy resolution

When reusable project scope is useful, the authoritative project/binding/Assembly source selects one immutable policy for its exact covered scope:

```yaml
bindingPolicy:
  ref: policy:ShopApplication/local-standard@3#sha256:4f923e8b1dbfa6a628f180be90218e9ff9ed5e4a86ad6ff08b3a1c1ad5fe8a77
```

A covered Ball repeats no reference. It records only an allowlisted difference when one exists:

```yaml
policyDelta:
  overrides:
    limits:
      maxStateBytes: 131072
```

Omit `policyDelta` when empty. A Ball-local policy reference is used only for a genuinely different explicit selection. The referenced policy identifies owner, revision/digest, covered guardrails, scope, effective values/mechanisms, override allowlist, enforcement/evidence ownership, and review/expiry condition when relevant.

Static resolution must produce one effective contract for every inferred trigger. Reject missing, mutable, stale, cyclic, conflicting, wrong-version/profile/binding/environment references and unauthorized deltas. Resolution is build/review work, never runtime lookup or ambient inheritance.

When a numeric `maxTransitionSteps` declaration supplies the decision-work bound rather than a static proof alone, it resolves `meterIdentity`, `meterVersion`, `transitionArtifactVersion`, one immutable `unitDefinition`, the numeric cap, and a finite typed pre-acceptance or programming-fault `overflowPolicy`. The count is deterministic for equal binding, transition artifact version, meter identity/version, State, Pulse, and valid Context, non-negative, monotonic, and starts once at zero per `decide`; helpers, phases, retries, yields, and representation erasure cannot restart it. Attempt `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and cannot truncate work into a different business result. Cross-binding counts compare only when meter identity/version, transition artifact version, and unit definition all match. A static bound alone requires no meter artifact.

Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without a static type-and-representation proof resolves one immutable binding-owned `BoundedByteMeasure` with `dimension`, `measureIdentity`, `measureVersion`, `representationDefinition`, `limitName`, and `maxBytes`. `Input` selects exactly `RawBoundaryInput` or `NormalizedTrustedInput`, including the declared boundary metadata and Context fields, and fails at `N+1` before trusted semantic acceptance or `decide`. `State` measures the complete candidate `nextState` semantic representation, including declared semantic metadata and excluding heap, allocator, index, storage, compression, encryption, and transport mechanics; `N+1` rejects the whole Decision before State, revision, or output acceptance. `DecisionOutputs` measures the complete ordered `Decision.outputs` sequence, including sequence structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload, while excluding `nextState`, non-envelope accepted-frame fields, and later transport, framing, compression, encryption, retry, and attempt data. Alternate retained representations map deterministically to the selected semantic representation, representation erasure preserves counts for equal values, and tuples with different dimension, identity, version, representation, or limit name are incomparable. Exact `N` may pass; no branch truncates a value or partially accepts a Decision.

When `maxDeclaredDependenciesPerBall` is present, its resolved view counts each distinct `ReadDependency`, `DeclaredCommandDependency`, `DeclaredSignalDependency`, and `FlowParticipation` declaration owned by the Ball once. Referenced dependency rows still count in their own kind, multiple operations to one target remain distinct, and exact duplicate declarations or aliases/equivalent rows resolving to an existing identity are invalid. Static resolution accepts exactly `N` declarations and rejects the Ball contract when a first distinct declaration would make `N+1`, before execution. When `maxRoutesPerFlow` is present, its resolved view counts only distinct effective Assembly command/result round-trip mappings used by that Flow; canonical command ingress plus its bound result return is one unit and multiple operations remain distinct. Read and signal dependencies retain their own bounds but consume zero command-route units; `FlowParticipation` and `dependencyRefs` also consume zero. Any exact duplicate, spelling alias, equivalent repeated route row, or split-leg alias is invalid; static resolution accepts exactly `N` mappings and rejects the Flow/Assembly contract at `N+1`, before execution.

When `maxCumulativeFanout` is present, its authoritative declaration or generated view resolves the accepted root operation/explicit causal scope, numeric ceiling, and either the static proof or binding-owned existing causal-reservation enforcement. Its unit is a distinct accepted source-output-to-effective-route/consumer branch. Terminal and converging traversals count; co-reachable branches sum; mutually exclusive alternatives share their maximum reservation; retry/redelivery of the same source tuple and route does not increment; a new accepted output tuple does; async handoff preserves scope and remaining budget while an independent root starts fresh. Exact `N` may accept; `N+1` rejects the whole candidate Decision with `AdmissionFailure(CausalBudgetExceeded)` and no partial batch or dispatch. No new runtime protocol, Context field, or counter artifact is required when a static proof closes the bound.

A policy reference selects only mechanisms and values that Core leaves open. It never changes applicability, normative strength, protocol ownership, or conformance. A deliberate deviation is separate and uses exactly:

```text
WaiverRecord {
    owner
    approvedBy
    governingAnchor
    exactScope
    reason
    constraintsAndCompensatingControls
    testsAndEvidence
    review { expiryOrReviewAt, remediation, conformanceEffect }
}
```

The record does not satisfy the missing guardrail. A violated `MUST`/`MUST NOT` blocks conformance for `exactScope`; a compile-time-import or `Direct Control Dependency` cycle remains non-conforming regardless of waiver or compensating control.

Routine design and installation create no trigger-absence placeholder. Only when a Pokeball conformance/release verdict or an accepted ambiguity-resolution decision relies on absence, record the exact closed proof:

```text
TriggerAbsenceProof {
  triggerClass: path-triggered | risk-triggered | claim-triggered
  triggerAnchor
  exactScopeAndEffectiveProfile
  inventoryEvidence:
      PathInventoryRef | RiskInventoryRef
    | DenialByConstructionRef | ClaimPublicationInventoryRef
  inventoryRevisionsOrDigests
  evaluatedPredicate
  conclusion: Absent
  evidenceOwner
  invalidationConditions
}
```

`always` and present triggers/claims cannot use the proof. Wrong scope/profile/inventory/revision/digest, missing/stale/unresolved/conflicting evidence, or a listed invalidation condition blocks reliance until evidence-owner reevaluation. The proof is static evidence, not a registry, default, inheritance path, or permission to broaden a reusable policy.

## 3. Sparse local example

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

Closed source proves a singleton typed scope, fixed input/state/transition work, and no output, external resource, retry, detached lifecycle/status, or claim. The enclosing binding selects its default profile once; the Ball needs no identity or policy row. No optional surface, forbidden-capability list, or evidence table is materialized. If source already contains these facts, this YAML may be generated.

## 4. Imported edge and Assembly

For each edge that exists, the target/producer owns its Application Surface, the caller Nucleus owns only its use of that imported surface in a closed contract, and Assembly owns:

```text
route identity and dependency kind
producer/source and exact protocol identity
consumer/target and exact protocol identity
delivery point and semantics
identity/deduplication/ordering policy when triggered
retry failure mode, exact primary owner, secondary disabled-or-finite-transparent proof, cumulative cap, and unknown/status policy when triggered
queue/backpressure/retention and security binding when triggered
effective route/fan-out bounds
```

For `maxCumulativeFanout`, the resolved view additionally identifies the one causal scope, the complete accepted source-tuple/effective-route/consumer branch identity, mutually exclusive reservation rule, duplicate/redelivery treatment, async-handoff continuation, independent-root boundary, and static-proof or existing-admission enforcement owner. It does not replace the separate per-Decision, per-Signal, or causal-depth bounds.

For a command edge, the resolved view additionally proves:

```text
target-owned ModuleCommand -> ModuleResult mapping
static refusal classification and target protocol version
accepted source ModuleCommandRequest -> verified ModuleCommandPulse ingress
accepted target ModuleResultOutput -> verified ModuleResultPulse return
effective protocol/version pair
commandSource/resultSource/issuer provenance transport without synthesis
target output-count and the `DecisionOutputs` dimension of its resolved `BoundedByteMeasure` plus conditional result-delivery/status slot
same-stack source level-1 alternative-completion reservation
target level-2 result reservation on the accepted branch
atomic level-1 transfer to source carrier Decision on pre-acceptance branch
```

The result-delivery key is the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`. A same-stack representation may erase envelopes only while proving identical accepted tuples. The source reserves level 1 before acceptance; target acceptance consumes it only after reserving level 2, while verified target validation/admission/`decide` rejection consumes no target level/frame/revision/output and transfers level 1 to the source `decide(ControlPulse)` carrier path. Missing level 1 prevents source acceptance/dispatch; missing level 2 prevents target acceptance and returns `AdmissionFailure(CausalBudgetExceeded)` through the carrier. Further synchronous outputs from carrier handling reserve from the remaining total budget. Reclassification requires a new target protocol version and Assembly pair. A result return is not a reverse dependency edge; async transport preserves the causal scope/depth/budget but does not infer the same-stack transfer rule.

Within the command idempotency horizon, the binding returns only verified ACK proof of the existing accepted target frame while its result is pending; it does not wait, re-invoke `decide`, revise, fabricate a result, or repeat Resource execution. Once a result frame exists, it redelivers that exact accepted frame with unchanged effective identity, `commandSource`, and `resultSource`, changing only mechanical attempt identity. Same identity with a different command fingerprint or conflicting evidence fails closed.

For a read edge, pre-read validation/admission may return only an already declared `BoundaryResponse`. Once admitted, target `read(...) -> ReadResult` returns only the successfully evaluated target-owned result, whose payload closes every reachable post-admission permitted, denied, redacted, or deliberately non-disclosing outcome, and creates no accepted marker, Decision, revision, handle, or output. Wrong target/version/authority/mapping/stamp fails at the boundary and is not converted into semantic `NotFound`; boundary, caller, Assembly, runtime, and generated code cannot add `BusinessRejection`, exception, post-admission `BoundaryResponse`, undeclared `NotFound`, or another payload. Multiple read dependencies do not imply an atomic multi-source snapshot. Query alone adds no new canonical per-Ball read-limit field.

A local route may compile to a direct call. A process boundary may use IPC. Neither changes domain semantics, and neither requires a universal mediator.

Classify the binding separately for graph review. A compile-time import or synchronous cross-Ball invocation before asynchronous handoff/yield—including generated inline dispatch—is a `Direct Control Dependency`. Compile-time-import and direct-control graphs are independently and unconditionally acyclic. Bounded asynchronous feedback begins after handoff/yield, creates no direct-control edge, and retains its owner/identity/budget/escape/fan-out contract.

## 5. Static validation

For routine work, validate the affected source/delta/edge:

- used owned variants and every target-owned `Query -> ResultPayload` mapping resolve exactly once; each payload is total over its reachable post-admission semantic outcomes, an intentional non-disclosure meaning is explicit and tested, and no non-owner invents a denial/redaction/absence carrier;
- every physical helper import resolves to one Ball/logical-role-local implementation or shared mechanical Foundation and remains in the acyclic compile-time graph; reject a fifth semantic dependency row, ownerless shared domain/business utility, and relabelling domain semantics as Foundation; Ball-owned shared semantics use a declared Application Surface/protocol;
- every `ReadDependency` resolves its complete target-owned identity/authority/requirement/binding contract, triggered-only fields, and successfully evaluated total-result codomain; target-declared permitted/denied/redacted/non-disclosing variants pass, while wrong version/authority/mapping/stamp, undeclared `NotFound`, `BusinessRejection`, post-admission `BoundaryResponse`, exception, payload invention, and command/read substitution fail;
- representation and declared closed-protocol/type invariants independent of committed State, semantic Context, and business policy fail at Interaction before semantic input; valid typed State/Context/business rules remain Nucleus-owned; promoting a fixed constraint into the type invariant changes the effective versioned protocol and is never a dynamic binding-stage choice;
- imported command/result types resolve to one target-owned mapping and static refusal classification; verified ingress/accepted-frame return preserve exact tuples and target bounds; duplicate-before-result returns verified ACK only, duplicate-after-result replays the exact accepted frame, and command-fingerprint/evidence conflict fails closed; explicit versions agree when independent versioning/deployment triggers them;
- every imported `Application Surface` is exact, declared, target/producer-owned, and required by a closed caller Nucleus contract; import transfers no ownership, exposes no foreign State/internal/private adapter, and creates no caller mirror/redeclaration, protocol re-export, or Interaction/Assembly-synthesized semantics; every Flow/participant pair has one resolved `FlowParticipation` with a non-empty bounded coordination set and only bounded refs to existing dependencies;
- same-stack command resolution proves source level-1 reservation, accepted-target level-2 reservation, target-pre-acceptance no-frame behavior, atomic level-1 carrier transfer, carrier-output reservation, and async budget preservation;
- inferred applicability cannot be disabled by missing metadata;
- every triggered guardrail has one construction proof, local declaration, or exact policy reference plus allowed delta;
- every present variable dimension has a finite effective bound;
- each numeric input/State/output byte limit is either closed by a static type-and-representation proof or resolves one exact dimension-specific `BoundedByteMeasure`: input fixes the raw-boundary or normalized-trusted stage and complete metadata/Context inclusion before `decide`; State covers the complete candidate `nextState` semantic representation before State/revision/output acceptance; DecisionOutputs covers the complete ordered output sequence and required envelope semantics while excluding later mechanics; every dimension proves exact alternate-representation mapping, erasure invariance, tuple incomparability, and exact `N/N+1` behavior without truncation or partial acceptance;
- each present `maxDeclaredDependenciesPerBall` counts distinct resolved read/command/signal/FlowParticipation declarations once with referenced rows retained, multiple operations distinct, duplicate/equivalent-alias declarations rejected, and static exact-`N`/reject-contract-at-`N+1`; each `maxRoutesPerFlow` counts only distinct effective Assembly command/result round-trip mappings with command ingress/result return unified, read/signal/participation/reference units at zero, exact duplicate/spelling-alias/equivalent-row/split-leg aliases rejected, and static exact-`N`/reject-Flow-Assembly-at-`N+1`;
- each present `maxCumulativeFanout` resolves one causal scope, exact accepted output/route/consumer branch identity, all-level tree/diamond/terminal/converging and co-reachable aggregation, mutually exclusive maximum reservation, duplicate/redelivery and new-tuple behavior, handoff/independent-root behavior, and static proof or exact `N/N+1` admission with no partial Decision;
- each triggered `DecisionWorkMeter` has one complete immutable meter identity/version, transition artifact version, unit/cap/overflow contract, deterministic monotonic per-`decide` counting, `N/N+1` behavior, and no illicit reset or cross-binding comparison;
- output paths have acceptance-before-dispatch and route/capability binding;
- a concrete fallible-admission profile/binding exposes one finite closed `AdmissionFailure.reason` union and rejects unknown/open strings; a non-fallible profile has no empty union;
- state-shape persistence changes have authoritative migration or quarantine, with no invented defaults for newly required semantic fields;
- every routed type resolves once from its owner and independently versioned endpoint pairs match Assembly; command reclassification changes the target/Assembly version pair; and
- compile-time/direct-control graphs, async feedback, fan-out, foundation, and unsafe checks run only when those paths exist, while policy and waiver records never weaken their result.

For a claim, generate the complete resolved view with exact named boundary and scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees; do not infer a stronger downstream guarantee from source durability or retained pending work. Record `TriggerAbsenceProof` only for absence on which the verdict relies, and run the `RG-*` gates. Manifest text alone never proves enforcement.

## 6. Change discipline

Change only the authority that owns the fact. A protocol meaning, ownership, route, effective policy, allowed delta, persistent schema, or guarantee change is semantic. Shared-policy updates create a new immutable revision/digest and an explicit affected-reference set; they never mutate an existing reference silently.
