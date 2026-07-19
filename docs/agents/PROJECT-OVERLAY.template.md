# Pokeball Project Policy — Template

> **Status:** derived noncanonical template. It is not an accepted project decision and neither defines nor extends Core. Verify `docs/agents/BASELINE.md`; Core prevails.

Use this template only when the project chooses reusable policies, bindings, waivers, or claims. A project whose applicable guardrails resolve entirely by construction or local declarations needs no overlay. When used, copy it to `docs/pokeball-project-overlay.md`, replace angle-bracket placeholders, remove unused optional keys/sections, and obtain project-owner acceptance. This policy declares shared facts once; it is not a Ball inventory.

## Project policy

```yaml
schema: pokeball-project-policy/v2

metadata:
  project: <project name>
  revision: <immutable revision>
  contentDigest: <sha256>
  owner: <accountable owner>
  acceptedBy: <project owner or review body>
  acceptedAt: <date>
  coreBaseline: docs/agents/BASELINE.md

sources:
  ballContracts: [<typed-source path or manifest glob>]
  # Include only when an inter-Ball edge exists.
  assembly: <sole route/composition source>

policies:
  <policy id>:
    revision: <immutable revision>
    digest: <sha256>
    owner: <policy owner>
    scope:
      ballsOrBindings: [<exact scope>]
      bindingsProfilesOrEnvironments: [<exact applicable scope>]
    coveredGuardrails: [<PBA or PKB-AR ids>]
    effective:
      # Keep only non-empty categories and present dimensions.
      profiles:
        <present profile dimension>: <exact choice>
      limits:
        <present dimension>: <finite value and unit>
      mechanisms:
        <triggered guardrail>: <exact mechanism or binding reference>
    overridableFields: [<exact allowlist>]
    enforcement:
      owner: <enforcement owner>
      # Include only concrete enforcement/evidence artifacts.
      artifacts: [<path or exact artifact reference>]
    # Include only when review or expiry exists for this declaration/evidence.
    reviewOrExpiry:
      condition: <exact date, event, or invalidation condition>
      owner: <review owner>

# Add only when the project graph contains them.
compositionCeilings:
  <present graph dimension>:
    value: <finite value and unit>
    policyRef: <exact revision and digest>

sharedBindings:
  <binding id>:
    policyRef: <exact revision and digest>
    source: <path>

# Add only when a deliberate deviation exists. Keep exactly the eight Core fields.
waivers:
  - owner: <accountable owner>
    approvedBy: <accepting authority>
    governingAnchor: <exact Core, extension, or project-policy anchor>
    exactScope: <exact nonconforming scope>
    reason: <why the deviation is accepted>
    constraintsAndCompensatingControls: [<constraint or control>]
    testsAndEvidence: [<test or evidence reference>]
    review:
      expiryOrReviewAt: <date, event, or review condition>
      remediation: <required remediation>
      conformanceEffect: <exact effect on claims and conformance>

# Add only as a non-empty reference list.
claims: [<claim record path>]

acceptance:
  decision: <accepted or rejected>
  revision: <policy revision>
  acceptedBy: <owner or review body>
  acceptedAt: <date>
  review: <accepted project record or path>
```

Keep only effective dimensions and mechanisms in each policy's scope. Add `evidence: {scope, artifacts}` to a policy only when its triggered mechanism or a concrete claim requires evidence. Shared evidence is recorded once at the exact artifact digest/scope.

## Scope selection and Ball delta

The accepted project/binding scope selects the exact policy once. A covered Ball carries nothing unless it has an allowlisted difference; then authoritative typed source or its generated manifest contains only the delta:

```yaml
policyDelta:
  overrides:
    <allowlisted field>: <local effective value>
```

Omit `policyDelta` when empty. A Ball-local exact `policySelection` is added only when it deliberately selects a different policy rather than inheriting the enclosing accepted scope. Static resolution rejects mutable/stale references, cycles, conflicts, scope/profile/binding/environment mismatch, uncovered triggers, and fields outside `overridableFields`.

A policy may select only a mechanism or value that Core leaves open. It cannot suppress an applicability trigger, weaken a law, or authorize a direct-control cycle.

A waiver uses exactly the eight fields shown above. It records deliberate nonconformance and does not become policy, precedent, or proof that the waived guardrail is satisfied. A violated `MUST` or `MUST NOT` blocks a conformance claim for the waiver's `exactScope` as stated by `review.conformanceEffect`; a waived direct-control cycle remains deliberate nonconformance. A claim record owns scope, mechanism, assumptions, evidence, non-guarantees, and gate results; this policy only references it.

Acceptance requires exact baseline/policy digests, resolvable source paths, an acyclic conflict-free reference graph, positive and negative trigger-resolution tests, exact WaiverRecord shape and conformance-effect checks when a waiver exists, and clean relocation under `docs/agents/INSTALL.md`. It requires no per-Ball inventory, route copy, empty optional section, or evidence table.
