# Authoring Pokeball skills

Use this guide to edit `skills/<name>/SKILL.md` directly. Each installed skill contains final, concise work instructions plus the existing `LICENSE` and `NOTICE.md`. The legal files are not task context. Skills remain derived guidance; the [ordered Core set](../spec/pokeball-architecture-core.md) owns the architecture.

Apply the host's `skill-creator` when available, together with the [Agent Skills specification](https://agentskills.io/specification) and [authoring practices](https://agentskills.io/skill-creation/best-practices), checked on 2026-09-05. Follow the concrete requirements below when general skill patterns would add unnecessary infrastructure.

## Write the instructions agents will execute

- Define one coherent coding or review task. Use lowercase directory-matching names and a concise frontmatter description with the main capability and trigger first. Keep automatic selection available by default.
- Write direct steps against the consuming project's source code, accepted contracts, and tests. State the required implementation decisions, failure cases, and task-specific checks. Keep the working directory and editing target in that project.
- Keep the complete `SKILL.md`, including frontmatter, at or below 800 words; aim near 600 when the task permits. Remove rationale, tutorials, pasted specification sections, terminology catalogs, repeated rules, and generic advice the agent already knows.
- Put conditional work inline at its actual trigger. Preserve the distinction between always-applicable obligations and paths, risks, or claims activated by this task. Reuse accepted project mechanisms at their valid scope; do not invent unused machinery or a new project contract.
- Make each skill executable by itself. Do not require source-document reading, another skill, a source checkout, network access, helper scripts, generated references, or a templating runtime. Do not add a `references/` directory or a copied documentation corpus.
- Preserve the requested action and scope. Review instructions do not authorize edits, published comments, or a broader audit. Implementation instructions must not quietly decide an unresolved semantic choice for the project owner.
- Keep installation, updates, freshness, provenance procedures, and skill maintenance in [SKILLS.md](SKILLS.md) or this guide. Installed instructions contain only the work of implementing or reviewing Pokeball-based code.

## Sources for maintainers

Use these sources while authoring and validating instructions. They are not reading prerequisites for an agent executing an installed skill. Follow the affected clause's dependencies when checking meaning; the table is a starting map, not an exhaustive audit inventory.

| Skill | Owning sources to check for affected behavior |
|---|---|
| `pokeball` | [Mutation/read forms and zones, §§3–5](../spec/core/03-model-boundaries-zones.md), [State and authority, §7](../spec/core/07-state-and-authority.md), [acceptance and reads, §8](../spec/core/08-decision-and-acceptance.md). |
| `pokeball-async` | [Protocol algebra, §6](../spec/core/06-protocol-algebra.md), [causality, delivery, retry, cancellation and status, §9](../spec/core/09-asynchrony-and-delivery.md). |
| `pokeball-composition` | [Boundary selection, §4](../spec/core/03-model-boundaries-zones.md#4-choosing-a-ball-boundary), [dependencies, Flow and Assembly, §10](../spec/core/10-system-composition.md). |
| `pokeball-binding` | [Decision/acceptance mechanics, §8](../spec/core/08-decision-and-acceptance.md), [security, §11](../spec/core/11-security-and-privacy.md), [profiles and limits, §§12–13](../spec/core/12-profiles-and-limits.md). |
| `pokeball-review` | [Transition/property tests, §17](../spec/core/verification/17-01-transition-and-property-tests.md), [checklist and anti-patterns, §§18–19](../spec/core/verification/18-checklist-and-antipatterns.md), plus the owning sources for the behavior under review. |

Change affected skill instructions and behavioral cases in the same pull request as their Core or Agent Pack source change. Compare the resulting instructions manually with actual source clauses, preserving triggers, ownership, closed outcomes, failure behavior, and guarantee boundaries. Do not strengthen a guarantee through compression or define competing `PKB-AR-*` rules. Keep existing licensing and attribution intact.

## Validate behavior and packaging

From the Pokeball checkout, run the structural checks used by [CI](../.github/workflows/skills.yml):

```sh
python3 -m unittest discover -s tests -p 'test_skill_tools.py' -v
python3 scripts/check_skills.py
```

Check frontmatter, word budget, permitted files, and each skill copied alone. There is no generation step. Format and packaging checks cannot establish semantic completeness or useful agent behavior.

For a substantial edit, give an independent agent only the copied skill, a realistic task, and the necessary application artifacts in an isolated project. Keep the source checkout, sibling skills, and network unavailable. Do not provide the intended answer or suspected bug. Inspect its actions, code, tests, and final response.

Exercise the changed decisions: a local business-rule change, out-of-order results, a cross-authority read or workflow, acceptance failure, or a bounded review, as applicable. Include an unrelated request to check false activation. Confirm that the agent performs the work directly, preserves existing contracts, and runs the relevant checks without seeking documentation. Correct demonstrated failures and repeat only affected cases. Record the bounded results and any untested scenarios with the pull request.
