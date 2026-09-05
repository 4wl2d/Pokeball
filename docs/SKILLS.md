# Pokeball skills

Install the official Pokeball skills to give a coding agent a practical workflow for your project. Each skill selects the relevant source sections, applies them to the affected code, and checks the resulting behavior. You can install only the workflows you need.

The skills are derived guidance. The [ordered Core set](../spec/pokeball-architecture-core.md) remains authoritative. Installation supplies neither a runtime nor evidence that your application conforms to Pokeball.

## Choose a workflow

| Skill | Use it for |
|---|---|
| [`pokeball`](../skills/pokeball/SKILL.md) | Implementing or changing a Ball, its owned State, pure decisions, inputs, and ordinary tests. Start here for everyday feature work. |
| [`pokeball-async`](../skills/pokeball-async/SKILL.md) | Detached work, result correlation, retries, cancellation, unknown outcomes, and operation status. |
| [`pokeball-composition`](../skills/pokeball-composition/SKILL.md) | Boundaries between authorities, dependencies, reads, Application Surfaces, Assembly, and Flow ownership. |
| [`pokeball-binding`](../skills/pokeball-binding/SKILL.md) | Implementing or changing the shared writer, acceptance, dispatch, execution, durability, security, and bounds mechanisms. |
| [`pokeball-review`](../skills/pokeball-review/SKILL.md) | Reviewing a specified implementation or change against the applicable Pokeball sources and evidence. |

These are independently selectable workflows sharing one source checkout. A skill reads its short entrypoint and shared working contract, then follows only the routes activated by the task. Files stored on disk do not all enter the agent's context.

## Download once

Choose an absolute source directory separate from the application you are developing. The following POSIX-shell commands use an example location; change it before running them. Keep this directory for as long as your installed links use it.

```sh
pokeball_source="$HOME/.local/share/pokeball"
mkdir -p "$(dirname "$pokeball_source")"
git clone --branch master --single-branch \
  https://github.com/4wl2d/Pokeball.git "$pokeball_source"
git -C "$pokeball_source" rev-parse HEAD
```

Use a new destination. If it already contains files, inspect it and choose another location rather than replacing it. Record the displayed commit SHA with your installation notes; it identifies the exact GitHub snapshot you downloaded. The official published branch is [`master`](https://github.com/4wl2d/Pokeball/tree/master).

Keep the complete checkout. Skills refer to shared files under `skills/references/`, the original Core under `spec/`, and relevant Agent Pack sources under `docs/agents/`. **Copying an individual skill folder or using a generic single-folder skill installer is unsupported:** those methods omit its repository-relative dependencies.

## Make selected skills discoverable

Choose one scope and set `pokeball_skills` in the same shell as above. For project scope, run these commands from the consuming project's root.

| Agent and scope | Set the destination |
|---|---|
| Codex, all your projects | `pokeball_skills="$HOME/.agents/skills"` |
| Codex, this project | `pokeball_skills="$PWD/.agents/skills"` |
| Claude Code, all your projects | `pokeball_skills="$HOME/.claude/skills"` |
| Claude Code, this project | `pokeball_skills="$PWD/.claude/skills"` |

Both [Codex](https://developers.openai.com/codex/skills) and [Claude Code](https://code.claude.com/docs/en/skills#where-skills-live) support symlinked local skill directories. Other Agent Skills hosts may use different discovery locations; check that host's documentation. The [Agent Skills format](https://agentskills.io/specification) does not mandate a universal installation directory. Documentation checked on 2026-09-05.

The example installs only `pokeball`. Add other names from the table to the `for` line when you want those workflows. It refuses an existing destination, including a dangling link.

```sh
: "${pokeball_source:?Set the absolute source checkout path first}"
: "${pokeball_skills:?Choose a discovery destination from the table first}"
mkdir -p "$pokeball_skills"
for pokeball_skill in pokeball; do
  pokeball_target="$pokeball_skills/$pokeball_skill"
  if [ -e "$pokeball_target" ] || [ -L "$pokeball_target" ]; then
    printf 'Already exists; inspect before changing: %s\n' "$pokeball_target" >&2
    break
  fi
  if [ ! -f "$pokeball_source/skills/$pokeball_skill/SKILL.md" ]; then
    printf 'Skill source missing: %s\n' "$pokeball_skill" >&2
    break
  fi
  ln -s "$pokeball_source/skills/$pokeball_skill" "$pokeball_target"
done
```

Keep project-specific links local unless your team intentionally shares that exact source layout: an absolute path on your computer will not work on someone else's. Teammates can repeat the installation with their own checkout location.

Check your agent's skill selector and open a selected skill to confirm that its shared references are accessible. If discovery has not refreshed, start a new session. Codex supports explicit `$skill-name` invocation; Claude Code uses `/skill-name`. Automatic selection depends on the host and each skill's description.

The commands above are for POSIX shells. On Windows, directory symlinks depend on the host's support and your system's permissions. If you cannot create them, keep the complete checkout and explicitly give your agent the absolute path to the chosen `SKILL.md`; that is manual file-based use, not automatic discovery. Windows host execution is not verified here.

## Use it in your application

Keep the agent's working directory in your application. Skill links lead to the source checkout; they do not make that checkout the project to edit.

For example, in Codex:

```text
Use $pokeball to add a maximum item count to this existing cart Ball.
Reuse its binding and test the changed business rule.

Use $pokeball-async to fix stale search results overwriting newer results.
Inspect the existing request and status contracts before changing code.

Use $pokeball-composition to decide who owns this checkout workflow and
implement the requested connection between the existing authorities.

Use $pokeball-binding to implement the acceptance boundary for this binding.
The application's accepted profile and guarantee requirements are in ./docs/binding.md.

Use $pokeball-review to review this diff for Pokeball regressions.
Report the concrete failure traces and relevant source clauses.
```

For Claude Code, replace `$pokeball…` with the corresponding `/pokeball…` command. With manual file-based use, provide the complete skill path and the same task.

If the application already uses an installed [Agent Pack](agents/README.md), retain its accepted Core, project policies, binding, and instructions. Adding skills does not migrate that contract. Resolve a source-version conflict explicitly before changing contract-dependent behavior; follow the [Agent Pack installation guide](agents/INSTALL.md) when an actual package migration is requested. An overlay template is optional and does not become an accepted policy merely by being copied.

## Update when requested

GitHub is the distribution source. There is no background updater, package registry, or separately released Pokeball command-line tool. Source and workflow changes are maintained together in this repository; local installations change when you update their checkout.

First inspect your installation from any working directory:

```sh
git -C "$pokeball_source" status --short --branch
git -C "$pokeball_source" remote get-url origin
git -C "$pokeball_source" rev-parse HEAD
```

Proceed only for the intended official checkout on `master`, with no local modifications or local commits to preserve. Record the previous SHA, then:

```sh
git -C "$pokeball_source" pull --ff-only origin master
git -C "$pokeball_source" rev-parse HEAD
```

[`--ff-only`](https://git-scm.com/docs/git-pull) refuses divergent history. If the checkout is modified, on another branch, or divergent, inspect the difference before deciding how to preserve it. Do not reset, clean, overwrite, or force-update it as an installation step.

Record the new SHA with your installation notes. Existing links now point to the updated files. Start a new agent session before the next task so it uses the new instructions. An existing application's pinned contract still requires its own explicit migration decision.

You can delegate this maintenance separately:

```text
Update my official Pokeball skill checkout at <absolute source directory>
from GitHub master. Verify the remote, branch, and local changes first.
Preserve local work; use only a fast-forward update. Report the old and new
commit SHAs. Keep my application's accepted Pokeball contract unchanged.
```

Installation and maintenance instructions live on this page. The skills themselves contain only workflows for implementing and reviewing Pokeball-based code. For contributing those workflows, see [skill authoring](SKILL-AUTHORING.md).
