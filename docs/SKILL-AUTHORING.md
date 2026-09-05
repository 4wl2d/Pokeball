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

Use the shared [working contract](../skills/references/working-contract.md) for common project-context and precedence instructions. Keep conditional routes close to the step that activates them. A route must say what causes the read and where the agent should go; a directory listing with no selection rule is insufficient.

Link to the original Core sections instead of copying chapters into skill folders. Link to a relevant Agent Pack procedure only when it helps the task. Stable `PKB-AR-*` definitions remain solely in [AGENT-CONTRACT.md](agents/AGENT-CONTRACT.md); references to their IDs do not create another definition.

## Read exact source sections

The repository's [section reader](../scripts/skill_context.py) uses Python 3.9 or later to extract existing Markdown sections without generating new architectural text. Set `pokeball_source` to the absolute source-checkout path. From the consuming project's working directory, invoke the reader through that path:

```sh
python3 "$pokeball_source/scripts/skill_context.py" \
  --list spec/core/reference/21-adoption.md
python3 "$pokeball_source/scripts/skill_context.py" \
  'spec/core/reference/21-adoption.md#217-everyday-development-and-production-responsibility'
```

Use its `--help` before relying on an unfamiliar option. A host without Python can read the same file and heading directly. The reader is a repository helper, not a required application dependency.

Choose enough source to retain the rule's trigger, authority, inputs, output algebra, failure behavior, and relevant exceptions. A narrow excerpt is useful only when it preserves the facts necessary for the decision. Follow an explicit dependency when a clause relies on another section; do not expand every read into a full Core review.

Resolve source paths from the physical skill directory after following any installation symlink. Preserve the consuming application as the working directory and editing target. Verify that each route works when the source checkout lives elsewhere and has no private project files.

## Preserve practical boundaries

Apply always-applicable rules and the additional obligations activated by the actual paths, risks, or claims. Reuse the application's accepted binding and exact policy references at their valid scope. Do not create unused protocol categories, empty overlays, duplicate policy, or a claim dossier for an ordinary business-rule change.

Distinguish a request to implement behavior from a request to review it. A review workflow must not grant itself permission to edit, publish comments, or broaden a bounded review. A coding workflow must not silently choose a new project contract when a missing decision changes semantics.

Keep download, installation, updates, freshness checks, Git provenance, and skill maintenance in [SKILLS.md](SKILLS.md) or this guide. Do not put them in `SKILL.md`, the shared working contract, or task references. Skills should use their available sources to perform the code task, without contacting GitHub or checking for a newer skill on every run.

## Validate the changed workflow

First inspect the diff and check frontmatter, link destinations, section anchors, and source-reading behavior. From the Pokeball checkout, run the same commands as [CI](../.github/workflows/skills.yml):

```sh
python3 -m unittest discover -s tests -p 'test_skill_tools.py' -v
python3 scripts/check_skills.py
```

Run the relevant reader/check tests when their inputs or implementation change. Use `skill-creator`'s format validator when available; passing it does not establish useful agent behavior.

Then run a bounded forward test with an independent agent when the workflow changed substantially. Supply the actual skill, a realistic user request, and only the raw application artifacts needed to do the work. Use an isolated project and give no intended answer, suspected defect, or preferred implementation. Inspect the execution trace and produced artifacts as well as the final response.

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

When a Core section, Agent Pack procedure, source path, or relevant example changes, inspect the skills and routes that depend on it in the same pull request. Update the affected instructions, routes, and behavioral cases together. Do not defer that work to a consumer's next coding task.

CI can verify format, paths, anchors, and the reader's behavior. It cannot prove that a skill preserves Core semantics or that an agent made the right architectural decision. Review the affected source-to-workflow meaning and record the bounded execution evidence in the pull request.

Before handing off, verify a clean whole-checkout layout, the selected-skill symlink path, and direct source-file access. State any untested hosts or scenarios. Keep source licensing and attribution intact.
