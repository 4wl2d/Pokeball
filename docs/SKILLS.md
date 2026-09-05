# Pokeball skills

Install the official Pokeball skills to give a coding agent practical instructions for your project. Each skill contains direct implementation or review steps and task-specific checks. You can install only the workflows you need.

The skills are derived guidance. The [ordered Core set](../spec/pokeball-architecture-core.md) remains authoritative. Installation supplies neither a runtime nor evidence that your application conforms to Pokeball.

## Choose a workflow

| Skill | Use it for |
|---|---|
| [`pokeball`](../skills/pokeball/SKILL.md) | Implementing or changing a Ball, its owned State, pure decisions, inputs, and ordinary tests. Start here for everyday feature work. |
| [`pokeball-async`](../skills/pokeball-async/SKILL.md) | Detached work, result correlation, retries, cancellation, unknown outcomes, and operation status. |
| [`pokeball-composition`](../skills/pokeball-composition/SKILL.md) | Boundaries between authorities, dependencies, reads, Application Surfaces, Assembly, and Flow ownership. |
| [`pokeball-binding`](../skills/pokeball-binding/SKILL.md) | Implementing or changing the shared writer, acceptance, dispatch, execution, durability, security, and bounds mechanisms. |
| [`pokeball-review`](../skills/pokeball-review/SKILL.md) | Reviewing a specified implementation or change for state ownership, acceptance, boundary, asynchronous, and testing regressions. |

Each skill contains one concise `SKILL.md` with the complete work instructions, plus `LICENSE` and `NOTICE.md`. The notices are legal material, not task context. Install any skill by itself: using it requires no Pokeball checkout, sibling skill, documentation-reading step, or network access.

## Download once

Download the complete directory of each selected skill from [GitHub](https://github.com/4wl2d/Pokeball/tree/master/skills), including its `SKILL.md` and notices. A GitHub skill installer that preserves the whole selected directory is supported. For example, ask your agent:

```text
Install skills/pokeball from https://github.com/4wl2d/Pokeball at master.
Keep the entire skill directory and its legal notices. Report the installed
path and source commit. Preserve any existing installation or local changes.
```

For manual installation, clone once into a new staging directory, then copy only the skills you want. The following POSIX-shell commands use an example staging location; choose an absent destination before running them:

```sh
pokeball_source="$HOME/Downloads/pokeball-skills-source"
mkdir -p "$(dirname "$pokeball_source")"
git clone --branch master --single-branch \
  https://github.com/4wl2d/Pokeball.git "$pokeball_source"
git -C "$pokeball_source" rev-parse HEAD
```

If the destination already contains files, inspect it and choose another location rather than replacing it. Record the displayed commit SHA with your installation notes; it identifies the exact GitHub snapshot you downloaded. The official published branch is [`master`](https://github.com/4wl2d/Pokeball/tree/master).

The staging checkout is only a download source. Installed copies continue to work after it is moved or removed. Do not copy `SKILL.md` alone: copy its complete containing directory.

## Make selected skills discoverable

Choose one scope and set `pokeball_skills` in the same shell as above. For project scope, run these commands from the consuming project's root.

| Agent and scope | Set the destination |
|---|---|
| Codex, all your projects | `pokeball_skills="$HOME/.agents/skills"` |
| Codex, this project | `pokeball_skills="$PWD/.agents/skills"` |
| Claude Code, all your projects | `pokeball_skills="$HOME/.claude/skills"` |
| Claude Code, this project | `pokeball_skills="$PWD/.claude/skills"` |

These local discovery locations are documented by [Codex](https://developers.openai.com/codex/skills) and [Claude Code](https://code.claude.com/docs/en/skills#where-skills-live). Other Agent Skills hosts may use different locations; check that host's documentation. The [Agent Skills format](https://agentskills.io/specification) does not mandate a universal installation directory. Documentation checked on 2026-09-05.

The example installs only `pokeball`. Add other names from the table to the `for` line when you want those workflows. It refuses an existing destination, including a dangling link, and creates ordinary directories.

```sh
: "${pokeball_source:?Set the staging checkout path first}"
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
  cp -R "$pokeball_source/skills/$pokeball_skill" "$pokeball_target"
done
```

For project scope, teams can version the installed directories with their application so each checkout receives the same skill files. For personal scope, each developer installs their preferred selection. Keep the bundled notices with the files.

Check your agent's skill selector and open a selected skill to confirm that its instructions are available. If discovery has not refreshed, start a new session. Codex supports explicit `$skill-name` invocation; Claude Code uses `/skill-name`. Automatic selection depends on the host and each skill's description.

The commands above are for POSIX shells. On Windows, copy the entire selected directory with your file manager or the host's installer into its documented discovery location; symlink privileges are unnecessary. Windows host execution is not verified here.

## Use it in your application

Keep the agent's working directory in your application. It applies the installed instructions to your source code, accepted project contracts, and tests.

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
Report concrete failure traces and the affected implementation rules.
```

For Claude Code, replace `$pokeball…` with the corresponding `/pokeball…` command. With manual file-based use, provide the complete skill path and the same task.

If the application already uses an installed [Agent Pack](agents/README.md), retain its accepted Core, project policies, binding, and instructions. Adding skills does not migrate that contract. Resolve a source-version conflict explicitly before changing contract-dependent behavior; follow the [Agent Pack installation guide](agents/INSTALL.md) when an actual package migration is requested. An overlay template is optional and does not become an accepted policy merely by being copied.

## Update when requested

GitHub is the distribution source. There is no background updater, package registry, or separately released Pokeball command-line tool. Source and workflow changes are maintained together in this repository. Installed directories are snapshots; downloading a newer checkout does not change them.

Download a new snapshot into a fresh staging directory using the clone instructions above. If you kept the previous staging checkout, first inspect it:

```sh
git -C "$pokeball_source" status --short --branch
git -C "$pokeball_source" remote get-url origin
git -C "$pokeball_source" rev-parse HEAD
```

For the intended official staging checkout on `master`, with no local modifications or local commits to preserve, record the previous SHA and update it:

```sh
git -C "$pokeball_source" pull --ff-only origin master
git -C "$pokeball_source" rev-parse HEAD
```

[`--ff-only`](https://git-scm.com/docs/git-pull) refuses divergent history. If the checkout is modified, on another branch, or divergent, inspect the difference before deciding how to preserve it. Do not reset, clean, overwrite, or force-update it as an installation step.

Replace selected installed skills as complete directories:

1. Record the new source SHA and the exact skills you intend to replace. Compare each current directory with its recorded source snapshot and inspect any local customizations.
2. Copy each complete new skill into a temporary location outside the agent's discovery directories. Check that `SKILL.md`, `LICENSE`, and `NOTICE.md` are present.
3. Choose a uniquely named backup destination outside discovery and verify that it does not exist. Decide explicitly which customizations to reapply to the new copy; retain the originals in the backup.
4. Between agent sessions, move the old directory to its backup location, then place the prepared new directory at the original installed path. If replacement fails, restore the backup before using the skill. Replace whole directories rather than overlaying files, which can retain obsolete files.
5. Record the new SHA and any reapplied local changes. Start a new session and confirm that the installed instructions are available.

For an earlier symlink installation, first preserve any customized source files as ordinary backup files. Replace the link itself with a complete copied skill directory; do not write through it or alter its shared source. The resulting installation has no dependency on that checkout.

An application's pinned Pokeball contract requires its own explicit migration decision. Updating a coding skill does not change that contract.

You can delegate this maintenance separately:

```text
Update only <selected skill names> installed at <absolute skills directory>
from https://github.com/4wl2d/Pokeball master. Stage one exact Git snapshot,
inspect local customizations, and back up each existing installation outside
skill discovery before replacing its complete directory. Preserve customized
files and report source commits and any local changes reapplied. Keep my
application's accepted Pokeball contract unchanged.
```

Installation and maintenance instructions live on this page. The skills themselves contain only workflows for implementing and reviewing Pokeball-based code. For contributing those workflows, see [skill authoring](SKILL-AUTHORING.md).
