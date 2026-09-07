# Manifest and Assembly Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

A manifest is an optional materialized view, not a mandatory source file or runtime registry. Prefer one authoritative typed source and generate a resolved view for tooling or claims instead of copying protocol, policy, and evidence.

This runbook is a projection of the marked Core source clauses. Core controls any conflict in trigger, owner, scope, failure semantics, reuse, or absence behavior.

**Task routing:** authority mapping, exact policy resolution, and the sparse local example remain below; imported edges, Assembly, static validation, and change discipline continue in [ASSEMBLY-AND-MANIFEST-VALIDATION.md](ASSEMBLY-AND-MANIFEST-VALIDATION.md).

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

Imported operation and result types remain target-owned. For a same-build local command, the operation type and wired target resolve the mapping, and the call scope resolves its immediate return without a protocol identifier or carrier shape. A portable independently delivered command retains its target-owned mapping, static refusal classification and required identity/version binding.

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

Static operation types and wiring expose the complete finite composition. No numeric dependency, participant or route ceiling is required. A project may adopt one with a concrete purpose; its limit and counting semantics belong to that policy. Generate any needed manifest and route table from these sources, so an added permitted consumer does not require hand-editing derived descriptions.

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

Moved to [Imported edge and Assembly](ASSEMBLY-AND-MANIFEST-VALIDATION.md#4-imported-edge-and-assembly).

## 5. Static validation

Moved to [Static validation](ASSEMBLY-AND-MANIFEST-VALIDATION.md#5-static-validation).

## 6. Change discipline

Moved to [Change discipline](ASSEMBLY-AND-MANIFEST-VALIDATION.md#6-change-discipline).
