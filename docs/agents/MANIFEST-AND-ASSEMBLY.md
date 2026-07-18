# Manifest and Assembly Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

A manifest is an optional materialized view, not a mandatory source file or runtime registry. Prefer one authoritative typed source and generate a resolved view for tooling or claims instead of copying protocol, policy, and evidence.

## 1. Authority map

| Fact | Sole authoritative location |
|---|---|
| owned state/protocol/Decision entry | typed Ball source or one manifest |
| inter-Ball routes and effective protocol identities; explicit versions when independently versioned | Assembly |
| shared profiles, limits, bindings, ceilings | static/local declaration or optional exact project/binding policy |
| Ball-specific fact or difference | local declaration or allowlisted delta |
| claim evidence | claim record |

An independently versioned owned category is declared inline or by one exact authoritative reference. A same-build closed type is already the inventory. An absent category is empty and has no row.

Every Query that exists maps to one result payload. Imported `ModuleCommand`/`ModuleResult` types remain target-owned and resolve through one Assembly edge; the caller never redeclares them.

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

A local route may compile to a direct call. A process boundary may use IPC. Neither changes domain semantics, and neither requires a universal mediator.

## 5. Static validation

For routine work, validate the affected source/delta/edge:

- used owned variants and Query results resolve exactly once;
- imported types resolve to the target and Assembly effective identity; explicit versions agree when independent versioning/deployment triggers them;
- inferred applicability cannot be disabled by missing metadata;
- every triggered guardrail has one construction proof, local declaration, or exact policy reference plus allowed delta;
- every present variable dimension has a finite effective bound;
- output paths have acceptance-before-dispatch and route/capability binding;
- state-shape persistence changes have migration/quarantine; and
- direct/control graphs, async feedback, fan-out, foundation, and unsafe checks run only when those paths exist.

For a claim, generate the complete resolved view, prove absent triggers once from the closed inventory, and run the `RG-*` gates. Manifest text alone never proves enforcement.

## 6. Change discipline

Change only the authority that owns the fact. A protocol meaning, ownership, route, effective policy, allowed delta, persistent schema, or guarantee change is semantic. Shared-policy updates create a new immutable revision/digest and an explicit affected-reference set; they never mutate an existing reference silently.
