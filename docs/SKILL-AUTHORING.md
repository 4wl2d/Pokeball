# Authoring Pokeball skills

Use this guide when creating or changing the skills in this repository. Skills are practical, derived coding workflows; the [ordered Core set](../spec/pokeball-architecture-core.md) owns the architecture. A skill must not introduce a new guarantee, mechanism requirement, or project policy through its wording.

The workflow below applies the [Agent Skills specification](https://agentskills.io/specification), its [authoring practices](https://agentskills.io/skill-creation/best-practices), and the [Codex skill guidance](https://developers.openai.com/codex/skills), checked on 2026-09-05. When available, use the host's `skill-creator` for the edit and its format validator. This repository's guidance also applies when that helper is unavailable.

## Define a coherent task

Start with a realistic request and affected project artifacts. Identify the decisions that an agent needs Pokeball-specific help to make. Choose the existing skill whose task matches, rather than adding a new entry for each law or edge case.

Keep names lowercase and hyphenated, matching their directory. Write a concise frontmatter `description` that states the actual coding capability and its trigger. Put the main use case first. Add an exclusion only when it prevents likely misrouting, such as a review skill activating during an unrelated implementation task.

Keep automatic selection available by default. Avoid host-specific tool permissions, model choices, invocation syntax, or runtime assumptions in portable instructions. Optional host metadata must remain consistent with the same workflow.

## Spend context on the current task

Use three levels of detail:

| Level | Contents |
|---|---|
| Frontmatter | Enough to choose the skill without opening it. |
| `SKILL.md` | Purpose, required inputs, essential workflow, source routing, and completion evidence. |
| Referenced sections | Conditional procedures and the exact source clauses needed for the present behavior. |

Keep the entrypoint as short as the workflow allows. The format's 500-line / 5,000-token guidance is a ceiling, not a target. Do not repeat generic coding advice or teach vocabulary that the current operation does not use. Avoid making an agent load several sibling skills merely to complete one ordinary feature change.

Maintain common project-context and precedence instructions in `skill-src/`; generation includes the needed copy inside each skill. Keep conditional routes close to the step that activates them. A route must say what causes the read and which local reference to open; a directory listing with no selection rule is insufficient.

Each installed skill must work by itself. Keep operational links inside its directory, and bundle the source sections and workflow references needed for its advertised tasks. Do not depend on a sibling skill, a source checkout, a generator, a repository reader, or network access. Stable `PKB-AR-*` definitions remain owned by [AGENT-CONTRACT.md](agents/AGENT-CONTRACT.md); skill instructions must not create competing definitions.

## Generate self-contained directories

Edit workflow templates and section selections under `skill-src/`, and change architectural meaning only in its authorized source. The repository's [skill generator](../scripts/build_skills.py) builds the publishable `skills/<name>/` directories from those inputs and Core sources:

```sh
python3 scripts/build_skills.py --write
python3 scripts/build_skills.py --check
```

Run these commands only while maintaining the Pokeball repository. Use `--help` before relying on an unfamiliar option. Consumers install the generated files directly; they do not run the generator or need Python to read a skill.

Treat generated files under `skills/` as output. Do not edit an installed or generated reference to repair the repository's source. Change its template or source selection, regenerate, and inspect the resulting diff. Preserve bundled attribution and notices using the existing licensing terms.

Select complete relevant sections, including their trigger, authority, inputs, output algebra, failure behavior, exceptions, and necessary dependencies. A small excerpt that removes a qualifying paragraph is not equivalent to its source. Ensure that required cross-references resolve locally; an upstream attribution URL must not become a required task-time read. Inspect `python3 scripts/build_skills.py --check --show-reference-report` for contextual chapter/range citations. The advertised runtime concern index has mandatory range closure; other reported citations require a scoped decision about whether they are operational prerequisites or optional navigation.

Generated source excerpts are attributed, noncanonical views. They do not form another Core document set, define independent rules, or replace the canonical entrypoint and manifest. Deterministic regeneration keeps those views tied to their source. Duplication across independently installable directories is a packaging cost, not a reason to load every reference into context.

## Preserve practical boundaries

Apply always-applicable rules and the additional obligations activated by the actual paths, risks, or claims. Reuse the application's accepted binding and exact policy references at their valid scope. Do not create unused protocol categories, empty overlays, duplicate policy, or a claim dossier for an ordinary business-rule change.

Distinguish a request to implement behavior from a request to review it. A review workflow must not grant itself permission to edit, publish comments, or broaden a bounded review. A coding workflow must not silently choose a new project contract when a missing decision changes semantics.

Keep download, installation, updates, freshness checks, Git provenance procedures, and skill maintenance in [SKILLS.md](SKILLS.md) or this guide. Do not put them in `SKILL.md`, working-contract instructions, or task references. Skills use their bundled sources to perform the code task, without contacting GitHub or checking for a newer skill on every run.

## Validate the changed workflow

First inspect the diff and check frontmatter, local link destinations, section anchors, and generated source coverage. From the Pokeball checkout, run the checks used by [CI](../.github/workflows/skills.yml):

```sh
python3 scripts/build_skills.py --check
python3 -m unittest discover -s tests -p 'test_skill_tools.py' -v
python3 scripts/check_skills.py
```

`--check` verifies that generated bytes match the maintained inputs. The skill checker verifies standalone local closure. Run relevant generator/check tests when their inputs or implementation change. Use `skill-creator`'s format validator when available; passing it does not establish useful agent behavior.

Test each skill copied alone into an isolated discovery directory, with the source checkout and other skills unavailable. Verify that every required local reference remains accessible. For a substantial workflow change, also run a bounded forward test with an independent agent using that single copied directory, a realistic user request, and only the raw application artifacts needed to do the work. The test must complete without network access or access to the source repository, generator, or private files. Give no intended answer, suspected defect, or preferred implementation. Inspect the execution trace and produced artifacts as well as the final response.

Choose cases that distinguish decisions, including applicable ones from this set:

| Scenario | Observe |
|---|---|
| Local business-rule change | Reuses the binding, reads affected sources, and avoids unrelated async machinery. |
| Out-of-order detached results | Follows the existing correlation/status contract and checks the relevant arrival orderings. |
| Cross-authority read or workflow | Distinguishes read dependencies from commands and derives ownership from the actual coordination need. |
| Changed acceptance or durability mechanism | Keeps State and present outputs consistent and tests the claimed failure boundary. |
| Bounded implementation review | Produces concrete source-backed findings within scope and leaves implementation unchanged. |
| Unrelated utility or documentation edit | Does not activate a Pokeball workflow solely because the repository mentions Pokeball. |

Check both missing reads and wasteful reads. A shorter trace is not a success if it omits a necessary clause; an exhaustive read is not a substitute for correct routing. Revise only from demonstrated problems, then repeat the affected case. Avoid tests that merely assert a particular heading or reproduce the implementation's text.

## Change sources and skills together

When a Core section, Agent Pack procedure, source path, or relevant example changes, inspect its dependent templates, selections, and skill routes in the same pull request. Update the affected instructions and behavioral cases, regenerate the published directories, and review their diff together. Do not defer that work to a consumer's next coding task.

CI can verify reproducible generation, format, local paths, anchors, and single-directory closure. It cannot prove that the chosen sections are semantically sufficient or that an agent made the right architectural decision. Review the affected source-to-workflow meaning and record the bounded execution evidence in the pull request.

Before handing off, verify the installed layout with each skill copied alone and its original source unavailable. State any untested hosts or scenarios. Keep source licensing and attribution intact.
