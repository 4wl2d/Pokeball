# Installing and Updating the Agent Pack

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Installation transfers a projection of Core, never a second normative source. If installed guidance differs from the marked Core source clause, Core prevails and the package must be replaced by a synchronized snapshot.

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

1. Copy Core and every `docs/agents/` file from one published immutable snapshot, preserving exact bytes and filenames. Retain `LICENSING.md` without replacing the target software `LICENSE`.
2. Verify the Core/package integrity manifest in [BASELINE.md](BASELINE.md). Readiness requires both immutable publication provenance and exact matching metadata; the manifest contains no embedded validation verdict.
3. Inspect one real Ball and resolve its present guardrails by construction, local declaration, or reusable policy; do not create an overlay merely to complete installation.
4. Only when shared policies, bindings, ceilings, deviations, or claims are useful, copy [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) to `docs/pokeball-project-overlay.md`, remove unused sections, resolve its source paths, and obtain owner acceptance plus an immutable revision/digest.
5. Pin the exact policy once in its authoritative project/binding scope; a Ball records a reference only when not already covered, plus any allowlisted local delta. A fully local/static Ball has no policy row.
6. Add the routing block below to root `AGENTS.md`; keep existing repository/build/test instructions.
7. Run package portability plus a routine dry run. Run full `RG-*` gates only before a conformance or release claim.

Routine installation, design, and adoption do not create `TriggerAbsenceProof`. Only an absence-dependent conformance/release verdict or accepted ambiguity-resolution decision records one exact proof at its evidence-owning scope; an `always` or present trigger cannot be proved absent.

An exact policy selects only mechanisms or values that Core leaves open; it cannot suppress a trigger, weaken a law, or authorize a direct-control cycle. A `WaiverRecord` records a deliberate deviation and its conformance effect, but never satisfies the guardrail. A violated `MUST`/`MUST NOT` keeps its exact scope non-conforming.

Recommended routing block:

```markdown
## Pokeball Architecture

For Pokeball-scoped work, verify `docs/agents/BASELINE.md`, then read
`docs/agents/AGENT-CONTRACT.md` and only the runbooks activated by the closed
source/profile/route/risk/claim inventory. If the affected source references
`docs/pokeball-project-overlay.md`, read that exact accepted policy too.

Use the command/result runbooks for an inter-Ball command round trip, the
ReadDependency guidance for an ordinary cross-authority read, and the absence
proof gate only when a claim or accepted ambiguity decision relies on absence.

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

The dry run also fails if it aliases a command to external `Intent`/`Reply`, lets Assembly synthesize command/result tuples or refusal meaning, substitutes a command for a non-recording Query, widens admitted `ReadResult` with `BoundaryResponse`, creates actor evidence on an actor-independent path, or materializes an absence-proof placeholder.

Portability validation additionally resolves:

- a sparse local Ball with no optional path artifacts;
- a triggered Ball resolved entirely by construction/local declaration with no overlay;
- two Balls covered by one exact shared policy without copied rows, plus one allowlisted local delta;
- policy revision/digest, scope, override allowlist, cycle/conflict/mismatch failures, including wrong version/profile/environment and stale references;
- one accepted source/target/source command round trip in routed and same-stack form, all three verified pre-acceptance responses, accepted same-state refusal, carrier/result conflict, static versioned reclassification, target result crash/stop/late delivery, causal levels `0/1/2`, and command-vs-read selection;
- one complete `ReadDependency` with target-owned mapping/read authority, caller freshness/consistency requirement and Assembly binding; wrong version/authority/mapping/stamp, actor-dependent/independent, no semantic `NotFound` substitution, no accepted read marker, and no multi-source atomicity fixtures;
- one trusted context/Dual-Gate/logical-role fixture plus forged context/proof, wrong issuer/realm/target/field, missing capability, hidden runtime write/service locator, Assembly-created meaning, and foundation-mediated communication negatives;
- one status materializer fixture with observation-before-source, bounded pending, duplicate/monotonic conflict, reserved `N/N+1` capacity/no eviction, covered-source/empty-pending marker, expiry/no resurrection, and target-result crash-before-dispatch;
- Catalog's six state/view mappings and six selection transitions, cancellation/unknown/reason orderings, actor-independent sparsity, and authoritative migration/quarantine;
- Checkout's exact carrier aliases, nine accepted-result command routes, ten source stop slots including the initial Reply, reply-plus-nine, and `10/11` capacity;
- when an absence-dependent verdict exists, present/absent path/risk/claim predicates, `always` misuse, present-claim contradiction, accepted/unaccepted ambiguity, wrong scope/profile/inventory/revision/digest, invalidation and reevaluation; routine fixtures contain no proof;
- exact equality of the 44 source-clause, law-projection, and matrix ID sets; every package modal traces set-equally to its named source tuple and no orphan modal creates authority;
- relocated links/routing in a standalone target; and
- exact `LICENSING.md` while preserving the target software license;
- operation without any source-repository governance, review ledger, decision history, or release-evidence path; and
- one complete `WaiverRecord` fixture whose `MUST`/`MUST NOT` violation remains non-conforming rather than becoming policy precedence.

## Updating

1. Freeze old Core, package, Ball-source, and claim baselines plus any project policy affected by the update.
2. Replace Core and the whole Agent Pack from one new published immutable snapshot; never edit an installed snapshot into another revision in place.
3. If a shared policy changes, publish it as a new immutable revision/digest and enumerate referencing Balls and invalidated evidence.
4. Update only affected Ball references/deltas and triggered tests. Do not copy unchanged policy.
5. Verify the new `BASELINE.md` hash/bytes/version/package digest after semantic and portability checks; consumers do not update that manifest themselves.
6. Re-run full project gates only for an existing or proposed claim whose scope changed.

Prohibited partial updates include changing only a hash, retaining a mutable “latest” policy reference, copying one runbook without its baseline/contract/index/trace/gates, replacing `LICENSING.md`, treating the template as accepted policy, treating a waiver as precedence, or retaining a claim or absence proof after its profile/policy/route/inventory/evidence digest changes.

## Discontinuing Pokeball

Removing routing and artifacts is a project-owner decision. Preserve historical claims/deviations and stop using conformance wording. Do not leave routing that points to absent or stale Core/package/policy artifacts.
