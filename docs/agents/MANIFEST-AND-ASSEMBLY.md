# Manifest and Assembly Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

A manifest is an optional materialized view, not a mandatory source file or runtime registry. Prefer one authoritative typed source and generate a resolved view for tooling or claims instead of copying protocol, policy, and evidence.

This runbook is a projection of the marked Core source clauses. Core controls any conflict in trigger, owner, scope, failure semantics, reuse, or absence behavior.

## 1. Authority map

| Fact | Sole authoritative location |
|---|---|
| owned state/protocol/Decision entry | typed Ball source or one manifest |
| inter-Ball routes and effective protocol identities; explicit versions when independently versioned | Assembly |
| shared profiles, limits, bindings, ceilings | static/local declaration or optional exact project/binding policy |
| Ball-specific fact or difference | local declaration or allowlisted delta |
| deliberate deviation and conformance effect | exact eight-field `WaiverRecord` |
| claim evidence | claim record |
| absence used by a conformance/release verdict or accepted ambiguity decision | exact evidence-owner `TriggerAbsenceProof`, outside routine Ball metadata |

An independently versioned owned category is declared inline or by one exact authoritative reference. A same-build closed type is already the inventory. An absent category is empty and has no row.

Every Query that exists maps to one target-owned result payload. A cross-authority `ReadDependency` resolves exactly one caller, target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Protocol version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. Caller and Assembly never redeclare, re-export, synthesize, or alter target payload, status fact, stamp, snapshot identity, or business meaning.

Imported `ModuleCommand`/`ModuleResult` payload types remain target-owned. The target declares one exact versioned mapping and statically classifies every refusal as verified pre-acceptance carrier or accepted target result. The caller imports that mapping through `dependencies.commands`; Assembly binds accepted-source ingress and accepted-target-frame return through one edge without business authority.

Every routed Signal likewise resolves exactly once in the producer-owned protocol. Assembly owns the endpoints, version pair, delivery binding, and route limits; it cannot define, synthesize, or re-own the producer payload. Catalog's canonical version-2 route therefore resolves `ProductSelectionConfirmed` from `CatalogSignal` and pins the independently versioned producer/consumer pair to `2.0.0/2.0.0`.

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

For each edge that exists, Assembly owns:

```text
route identity and dependency kind
producer/source and exact protocol identity
consumer/target and exact protocol identity
delivery point and semantics
identity/deduplication/ordering policy when triggered
retry owner/cap and unknown/status policy when triggered
queue/backpressure/retention and security binding when triggered
effective route/fan-out bounds
```

For a command edge, the resolved view additionally proves:

```text
target-owned ModuleCommand -> ModuleResult mapping
static refusal classification and target protocol version
accepted source ModuleCommandRequest -> verified ModuleCommandPulse ingress
accepted target ModuleResultOutput -> verified ModuleResultPulse return
effective protocol/version pair
commandSource/resultSource/issuer provenance transport without synthesis
target output/byte bounds and conditional result-delivery/status slot
```

The result-delivery key is the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`. A same-stack representation may erase envelopes only while proving identical accepted tuples. Reclassification requires a new target protocol version and Assembly pair. A result return is not a reverse dependency edge.

For a read edge, pre-read validation/admission may return only an already declared `BoundaryResponse`. Once admitted, target `read(...) -> ReadResult` returns only the successful target-owned result and creates no accepted marker, Decision, revision, handle, or output. Wrong target/version/authority/mapping/stamp fails at the boundary and is not converted into `NotFound`. Multiple read dependencies do not imply an atomic multi-source snapshot. Query alone adds no new canonical per-Ball read-limit field.

A local route may compile to a direct call. A process boundary may use IPC. Neither changes domain semantics, and neither requires a universal mediator.

Classify the binding separately for graph review. A compile-time import or synchronous cross-Ball invocation before asynchronous handoff/yield—including generated inline dispatch—is a `Direct Control Dependency`. Compile-time-import and direct-control graphs are independently and unconditionally acyclic. Bounded asynchronous feedback begins after handoff/yield, creates no direct-control edge, and retains its owner/identity/budget/escape/fan-out contract.

## 5. Static validation

For routine work, validate the affected source/delta/edge:

- used owned variants and Query results resolve exactly once;
- every `ReadDependency` resolves its complete target-owned identity/authority/requirement/binding contract, triggered-only fields, and successful-read codomain; wrong version/authority/mapping/stamp and command/read substitution fail;
- imported command/result types resolve to one target-owned mapping and static refusal classification; verified ingress/accepted-frame return preserve exact tuples and target bounds; explicit versions agree when independent versioning/deployment triggers them;
- inferred applicability cannot be disabled by missing metadata;
- every triggered guardrail has one construction proof, local declaration, or exact policy reference plus allowed delta;
- every present variable dimension has a finite effective bound;
- output paths have acceptance-before-dispatch and route/capability binding;
- a concrete fallible-admission profile/binding exposes one finite closed `AdmissionFailure.reason` union and rejects unknown/open strings; a non-fallible profile has no empty union;
- state-shape persistence changes have authoritative migration or quarantine, with no invented defaults for newly required semantic fields;
- every routed type resolves once from its owner and independently versioned endpoint pairs match Assembly; command reclassification changes the target/Assembly version pair; and
- compile-time/direct-control graphs, async feedback, fan-out, foundation, and unsafe checks run only when those paths exist, while policy and waiver records never weaken their result.

For a claim, generate the complete resolved view, record `TriggerAbsenceProof` only for absence on which the verdict relies, and run the `RG-*` gates. Manifest text alone never proves enforcement.

## 6. Change discipline

Change only the authority that owns the fact. A protocol meaning, ownership, route, effective policy, allowed delta, persistent schema, or guarantee change is semantic. Shared-policy updates create a new immutable revision/digest and an explicit affected-reference set; they never mutate an existing reference silently.
