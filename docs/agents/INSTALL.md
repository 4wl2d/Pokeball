# Installing and Updating the Agent Pack

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Installation transfers a projection of Core, never a second normative source. If installed guidance differs from the marked Core source clause, Core prevails and the package must be replaced by a synchronized snapshot.

## Install skills with your agent

For everyday coding workflows, start with the standalone skills. Open your application project in a coding agent with file and network access, then paste this prompt. It installs all five skills in that project; remove names from the list if you want a smaller selection.

```text
Install the official Pokeball skills into the current project only.

Source: https://github.com/4wl2d/Pokeball, branch master.
Resolve master to one commit and use that exact snapshot for all directories:
skills/pokeball
skills/pokeball-async
skills/pokeball-composition
skills/pokeball-binding
skills/pokeball-review

Use this coding agent's supported project skill directory. For Codex, use
.agents/skills at the project root. For another host, establish its documented
project location first; if project skills are unsupported, explain and stop.
Do not install globally.

Use an available skill installer or Git with temporary staging outside the
project. Copy only the selected complete directories, retaining SKILL.md,
LICENSE and NOTICE.md. Do not copy the source repository, Core, Agent Pack,
runtime, or installation helpers into the project.

Preserve existing project instructions, application code, dependencies and
accepted Pokeball contracts. Inspect existing skill names and destinations:
leave identical installations unchanged; report differing installations or
name collisions without overwriting them or following destination symlinks.

Verify the installed files match the chosen commit. Report the source commit,
installed paths, skipped conflicts, and how to invoke a skill in this host.
Check discovery if the host exposes it; otherwise say it is unverified and
whether a new session is needed. Installation alone is not a conformance claim.
```

For Codex, the project directory and discovery behavior follow the [official skills documentation](https://developers.openai.com/codex/skills#where-to-save-skills), checked on 2026-09-07. A host's installer may default to personal scope; the prompt explicitly requests the project destination. No application refactoring or automatic skill updater is part of installation.

The complete-directory [manual skills guide](https://github.com/4wl2d/Pokeball/blob/master/docs/SKILLS.md#download-once) remains available for project or personal installation. The sections below install the full Core and Agent Pack for projects that explicitly want that reference contract; standalone skill installation does not require it.

## Target layout

```text
target-repo/
├── spec/pokeball-architecture-core.md # stable Core entrypoint and ordered manifest
├── spec/core/**                       # every manifest-listed Core chapter
├── docs/agents/                    # exact portable package
├── docs/pokeball-project-overlay.md # optional accepted shared project policy
└── AGENTS.md
```

Ball contracts remain in their normal typed source or local manifests. Assembly remains in the project's composition root. The shared policy does not contain a Ball inventory.

## Initial installation

1. Copy the stable Core entrypoint, every exact `spec/core/**` path listed by its ordered manifest, and every `docs/agents/` file from one published immutable snapshot, preserving repository-relative paths, exact bytes, and filenames. Retain `LICENSING.md` without replacing the target software `LICENSE`.
2. Verify the ordered Core-set and package integrity manifest in [BASELINE.md](BASELINE.md). Readiness requires both immutable publication provenance and exact matching metadata; the manifest contains no embedded validation verdict.
3. Inspect one real Ball and resolve its present guardrails by construction, local declaration, or reusable policy; do not create an overlay merely to complete installation.
4. Only when shared policies, bindings, ceilings, deviations, or claims are useful, copy [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) to `docs/pokeball-project-overlay.md`, remove unused sections, resolve its source paths, and obtain owner acceptance plus an immutable revision/digest.
5. Pin the exact policy once in its authoritative project/binding scope; a Ball records a reference only when not already covered, plus any allowlisted local delta. A fully local/static Ball has no policy row.
6. Add the routing block below to root `AGENTS.md`; keep existing repository/build/test instructions.
7. Check the copied files, relocated links and routing, then run one relevant routine dry run from [PORTABILITY-VALIDATION.md](PORTABILITY-VALIDATION.md). The complete clean-layout catalogue validates the Agent Pack itself under `AP-GATE-10`; a consuming project does not implement absent features to repeat it. Run full applicable `RG-*` gates before a conformance or release claim.

Routine installation, design, and adoption do not create `TriggerAbsenceProof`. Only an absence-dependent conformance/release verdict or accepted ambiguity-resolution decision records one exact proof at its evidence-owning scope; an `always` or present trigger cannot be proved absent.

An exact policy selects only mechanisms or values that Core leaves open; it cannot suppress a trigger, weaken a law, or authorize a direct-control cycle. A `WaiverRecord` records a deliberate deviation and its conformance effect, but never satisfies the guardrail. A violated `MUST`/`MUST NOT` keeps its exact scope non-conforming.

Recommended routing block:

```markdown
## Pokeball Architecture

For Pokeball-scoped work, verify `docs/agents/BASELINE.md`, then read
`docs/agents/AGENT-CONTRACT.md` and only the runbooks activated by the closed
source/profile/route/risk/claim inventory. If the affected source references
`docs/pokeball-project-overlay.md`, read that exact accepted policy too.

Use `ASYNC-STATUS-RUNBOOK.md` for root idempotency, an inter-Ball command/result
round trip, retry, or cancellation, and continue to `STATUS-AND-ASYNC-TESTS.md`
only for operation status or its async test catalogue. Use
`COMPOSITION-PROFILES.md` for an ordinary cross-authority ReadDependency,
utility ownership, a Nucleus import of an owner-authored Application Surface,
FlowParticipation, dependency/Flow-route counting, or cumulative fan-out;
continue to `COMPOSITION-PROFILES-AND-CLAIMS.md` only for profile, claim, or
Foundation work.

Use `DESIGN-RUNBOOK.md` for boundary and State, then
`PROTOCOL-DESIGN-RUNBOOK.md` for the selected Snapshot/Event mutation,
accepted frame, protocol, output, lifecycle, or read. Use
`SECURITY-LIMITS-RUNBOOK.md` for protocol-validation versus State/Context
business-stage ownership, capabilities, sinks, secrets, or unsafe paths; use
`LIMITS-AND-EVIDENCE-RUNBOOK.md` for any numeric input/State/output byte limit,
a triggered Decision Work Meter, runtime-enforced fan-out ceiling, admission,
or evidence reuse. Use the absence-proof gate only when a claim or accepted
ambiguity decision relies on absence.

Canonical Core set: `spec/pokeball-architecture-core.md` plus every exact
ordered manifest-listed path under `spec/core/**`.
Core prevails. Shared policies are exact static references; Balls record only
allowed deltas. Absent paths create no placeholder artifacts.
```

## Routine dry run

Moved to [PORTABILITY-VALIDATION.md](PORTABILITY-VALIDATION.md#routine-dry-run), which owns the routine report shape and clean-layout fixture catalogue.

## Updating

1. Freeze the old ordered Core set, package, Ball-source, and claim baselines plus any project policy affected by the update.
2. Replace the Core entrypoint, every manifest-listed Core chapter, and the whole Agent Pack from one new published immutable snapshot; never edit an installed snapshot into another revision in place.
3. If a shared policy changes, publish it as a new immutable revision/digest and enumerate referencing Balls and invalidated evidence.
4. Update only affected Ball references/deltas and triggered tests. Do not copy unchanged policy.
5. Verify the new `BASELINE.md` Core entrypoint version/status, manifest file count, Core-set digest/bytes, and package file count/digest after semantic and portability checks; consumers do not update that manifest themselves.
6. Re-run full project gates only for an existing or proposed claim whose scope changed.

Prohibited partial updates include changing only a digest, retaining a mutable “latest” policy reference, copying one runbook without its baseline/contract/index/trace/gates, replacing `LICENSING.md`, treating the template as accepted policy, treating a waiver as precedence, or retaining a claim or absence proof after its profile/policy/route/inventory/evidence digest changes.

## Discontinuing Pokeball

Removing routing and artifacts is a project-owner decision. Preserve historical claims/deviations and stop using conformance wording. Do not leave routing that points to absent or stale Core/package/policy artifacts.
