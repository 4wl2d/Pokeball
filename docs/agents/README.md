# Pokeball Agent Pack

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Before using it, verify the integrity manifest and published immutable Core snapshot recorded in [BASELINE.md](BASELINE.md). If package guidance conflicts with Core, Core prevails.

This package turns Core into a sparse workflow for project agents. It preserves every applicable guardrail while avoiding duplicate policy, empty sections, and claim evidence for properties the project does not claim.

## Start

1. Verify the package files and published immutable Core snapshot against [BASELINE.md](BASELINE.md); this is an integrity check, not an architectural-quality verdict.
2. Read the authoritative typed source or manifest for the affected Ball; read an exact accepted project policy only when referenced, and Assembly only when an inter-Ball edge exists.
3. Classify each relevant law as `always`, `path-triggered`, `risk-triggered`, or `claim-triggered` using Core §20.1.
4. Resolve every triggered guardrail through construction, one local declaration, or one exact immutable policy reference plus a permitted delta.
5. Read only the runbooks selected by the triggers below.
6. For routine work, test the affected semantics and deltas. For a conformance or other guarantee claim, run the full gates in [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md).

A missing optional path produces no placeholder artifact. An inferred trigger cannot be disabled by omitting metadata. If absence is ambiguous, treat the trigger as present or obtain an accepted decision.

## Task routing

| Trigger or task | Primary document | Additional source |
|---|---|---|
| Ball boundary, state, protocol, Decision | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) | authoritative Ball source |
| Detached work, result, retry, cancellation, unknown, status | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) | Core §§3, 9 |
| Inter-Ball edge, Flow, profile, foundation | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) | Assembly; project policy if referenced |
| Trust edge, capability, unsafe path, secret, effective bound | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) | static/local declaration or exact reusable policy |
| Manifest view, policy resolution, Assembly | [MANIFEST-AND-ASSEMBLY.md](MANIFEST-AND-ASSEMBLY.md) | [REFERENCE-INDEX.md](REFERENCE-INDEX.md) |
| Runtime concern placement or ownership | [REFERENCE-INDEX.md](REFERENCE-INDEX.md) §8.12 runtime concern index | the exact Core anchors named there |
| Routine validation or conformance/release claim | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) | [TRACEABILITY.md](TRACEABILITY.md) |
| Catalog/Checkout analogy | [EXAMPLE-CROSSWALK.md](EXAMPLE-CROSSWALK.md) | canonical Core §§15–16 |
| Install or update the package | [INSTALL.md](INSTALL.md) | [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) |

## Source precedence

1. the exact Core pinned in `BASELINE.md`;
2. an accepted extension, only in its declared scope;
3. an accepted exact project policy, only for mechanisms or values Core leaves open;
4. `AGENT-CONTRACT.md`, runbooks, traceability, and indexes;
5. examples.

Policy reuse is static. A reference includes owner, ID, revision, digest, scope, covered guardrails, effective mechanisms or values, allowed override fields, enforcement ownership, and scoped evidence when evidence is required. It is not a runtime lookup, ambient default, or permission to weaken Core.

A policy cannot suppress an applicability trigger, weaken a law, or authorize a direct-control cycle. An exact Core WaiverRecord may document deliberate nonconformance, but it is neither policy nor precedent and does not satisfy the waived guardrail. A violated `MUST` or `MUST NOT` blocks a conformance claim for the waiver's exact scope according to its recorded conformance effect; a waived direct-control cycle remains deliberate nonconformance.

## Two workflows

### Routine application

Resolve the affected Ball and any policy it references, infer triggers from the closed source/profile/route/risk inventory, change only the semantic delta, and run base plus triggered tests. Reuse shared mechanism and evidence at its exact scope. No review dossier is required.

### Claim or release

Freeze exact baselines, resolve the whole claimed scope, prove absent triggers once, run all applicable profile and `RG-*` gates, evaluate every applicable waiver's conformance effect, and attach evidence to the claim record. Without that record, omit the claim.

## Package contents

`AGENT-CONTRACT.md` defines all stable `PKB-AR-*` rules once. The focused runbooks provide procedures; `TRACEABILITY.md` maps rules to Core, triggers, authoritative sources, and gates. `PROJECT-OVERLAY.template.md` is an optional compact shared project-policy template, not a Ball inventory or installation requirement. `REFERENCE-INDEX.md` supplies complete law/glossary lookup and the reference-only §8.12 runtime concern routes; it is not a statement that every indexed rule applies to every Ball.

The package does not select project identities, profiles, policies, routes, grants, or claims; create a runtime or extension; or turn Catalog and Checkout into mandatory templates.

## License

Retain the scoped [LICENSING.md](LICENSING.md) when transferring the exact package. It does not replace or alter the consuming project's software license.
