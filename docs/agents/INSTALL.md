# Installing and Updating the Agent Pack

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

## Target layout

```text
target-repo/
├── spec/pokeball-architecture-core.md
├── docs/agents/                    # exact portable package
├── docs/pokeball-project-overlay.md # optional accepted shared project policy
└── AGENTS.md
```

Ball contracts remain in their normal typed source or local manifests. Assembly remains in the project's composition root. The shared policy does not contain a Ball inventory.

## Initial installation

1. Copy Core and every `docs/agents/` file from one validated package, preserving exact bytes and filenames. Retain `LICENSING.md` without replacing the target software `LICENSE`.
2. Pass Core/package integrity in [BASELINE.md](BASELINE.md).
3. Inspect one real Ball and resolve its present guardrails by construction, local declaration, or reusable policy; do not create an overlay merely to complete installation.
4. Only when shared policies, bindings, ceilings, deviations, or claims are useful, copy [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) to `docs/pokeball-project-overlay.md`, remove unused sections, resolve its source paths, and obtain owner acceptance plus an immutable revision/digest.
5. Pin the exact policy once in its authoritative project/binding scope; a Ball records a reference only when not already covered, plus any allowlisted local delta. A fully local/static Ball has no policy row.
6. Add the routing block below to root `AGENTS.md`; keep existing repository/build/test instructions.
7. Run package portability plus a routine dry run. Run full `RG-*` gates only before a conformance or release claim.

Recommended routing block:

```markdown
## Pokeball Architecture

For Pokeball-scoped work, verify `docs/agents/BASELINE.md`, then read
`docs/agents/AGENT-CONTRACT.md` and only the runbooks activated by the closed
source/profile/route/risk/claim inventory. If the affected source references
`docs/pokeball-project-overlay.md`, read that exact accepted policy too.

Canonical Core: `spec/pokeball-architecture-core.md`.
Core prevails. Shared policies are exact static references; Balls record only
allowed deltas. Absent paths create no placeholder artifacts.
```

## Routine dry run

Choose one bounded change in a real Ball. The agent should report:

```text
baseline and affected authority
authoritative source and exact project-policy reference, if used
semantic delta and inferred triggers
local declaration or allowlisted policy delta, if present
base plus triggered tests
explicit claim boundary, if any; otherwise no claim is made
```

The dry run fails if the agent copies the whole project policy into the Ball, repeats routes or protocol inventories in the overlay, suppresses a trigger through missing metadata, uses a mutable policy alias, invents project values, or creates a claim without evidence.

Portability validation additionally resolves:

- a sparse local Ball with no optional path artifacts;
- a triggered Ball resolved entirely by construction/local declaration with no overlay;
- two Balls covered by one exact shared policy without copied rows, plus one allowlisted local delta;
- policy revision/digest, scope, override allowlist, cycle/conflict/mismatch failures;
- relocated links/routing in a standalone target; and
- exact `LICENSING.md` while preserving the target software license.

## Updating

1. Freeze old Core, package, Ball-source, and claim baselines plus any project policy affected by the update.
2. Apply the new Core and rebuild contract, runbooks, reference index, traceability, and package gates together.
3. If a shared policy changes, publish it as a new immutable revision/digest and enumerate referencing Balls and invalidated evidence.
4. Update only affected Ball references/deltas and triggered tests. Do not copy unchanged policy.
5. Update `BASELINE.md` hash/bytes/version/package digest only after semantic and portability validation.
6. Re-run full project gates only for an existing or proposed claim whose scope changed.

Prohibited partial updates include changing only a hash, retaining a mutable “latest” policy reference, copying one runbook without its baseline/contract, replacing `LICENSING.md`, treating the template as accepted policy, or retaining a claim after its profile/policy/route/evidence digest changes.

## Discontinuing Pokeball

Removing routing and artifacts is a project-owner decision. Preserve historical claims/deviations and stop using conformance wording. Do not leave routing that points to absent or stale Core/package/policy artifacts.
