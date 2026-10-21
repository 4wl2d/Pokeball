#!/usr/bin/env python3
"""Renders benchmark/results/RESULTS.md from the runner's results.json.

Everything except two source-level counts comes from results.json. This
script adds, from the Kotlin sources of each implementation's feature code:

- SLOC: lines that hold code, i.e. anything other than whitespace and
  comments (`//` and nested `/* */` comments are recognised outside string
  and character literals; a line holding only part of a string literal
  counts).
- Exception handlers: `catch` clauses and `runCatching` calls.
- Calls whose failure or timeout a library returns as a value: calls of
  toolkit functions that catch exceptions themselves (found in the `kit`
  sources: functions that contain `catch (` or `runCatching`, or call one
  that does), and calls of the Ballarch library's `Call(...)` and
  `request(...)`, whose failures and timeouts the runtime returns as
  `Outcome` values (listed in LIBRARY_VALUE_CALLS, because the runtime is
  not a `kit` package and catches where it executes a call, not in these
  entry points).
- Checks of failure values: `Reply.Failure`, the value through which the
  provider's callback API reports a failure instead of throwing it.
- Lines changed for scenario B: each file of a scenario-B package compared
  with the file at the same path in the design it adapts (lines that hold
  code, without indentation, ignoring `package` and `import` lines).

Two more files next to results.json are rendered when they exist:
changes.json (the change requests: patch metrics from changes/measure.py,
verification from changes/verify.py) and overhead.json (the runtime overhead,
from the runner's `overhead` task; machine-dependent, so not part of the
byte-for-byte check of results.json).

Comments and string literals are not searched.

Needs only the Python standard library.
Usage: python3 benchmark/report.py results.json benchmark/results
"""
from __future__ import annotations

import difflib
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def kotlin_root(module: str) -> Path:
    return ROOT / module / "src" / "main" / "kotlin"


def package_dir(module: str, package: str) -> Path:
    return kotlin_root(module) / Path(*package.split("."))


def sources(arch: dict) -> list[Path]:
    """The scenario-A feature code: the design's root package without toolkits and scenario-B packages."""
    excluded = [package_dir(arch["module"], p) for p in arch["excluded"]]
    return sorted(f for f in package_dir(arch["module"], arch["root"]).rglob("*.kt") if not any(e in f.parents for e in excluded))


def toolkit_sources(arch: dict) -> list[Path]:
    return sorted(f for p in arch["toolkit"] for f in package_dir(arch["toolkitModule"], p).rglob("*.kt"))


def scan(text: str) -> tuple[set[int], str]:
    """Returns the 0-based numbers of the lines that hold code, and the text with
    comments removed and string and character literals blanked out."""
    code_lines: set[int] = set()
    out: list[str] = []
    line = 0
    i = 0
    n = len(text)
    # Each frame is a literal ('"', '"""' or "'") or a template expression ('{' with its brace depth).
    stack: list[list] = []
    while i < n:
        c = text[i]
        top = stack[-1] if stack else None
        in_literal = top is not None and top[0] in ('"', '"""', "'")
        if c == "\n":
            line += 1
            out.append(c)
            i += 1
            continue
        if in_literal:
            code_lines.add(line)
            kind = top[0]
            if c == "\\" and kind != '"""':
                out.append("  ")
                i += 2
                continue
            if kind != "'" and text.startswith("${", i):
                stack.append(["{", 1])
                out.append("  ")
                i += 2
                continue
            if text.startswith(kind, i):
                stack.pop()
                out.append(" " * len(kind))
                i += len(kind)
                continue
            out.append(" ")
            i += 1
            continue
        # Code, possibly inside a template expression.
        if text.startswith("//", i):
            while i < n and text[i] != "\n":
                i += 1
            continue
        if text.startswith("/*", i):
            depth = 0
            while i < n:
                if text.startswith("/*", i):
                    depth += 1
                    i += 2
                elif text.startswith("*/", i):
                    depth -= 1
                    i += 2
                    if depth == 0:
                        break
                else:
                    if text[i] == "\n":
                        line += 1
                        out.append("\n")
                    i += 1
            continue
        if not c.isspace():
            code_lines.add(line)
        if text.startswith('"""', i):
            stack.append(['"""'])
            out.append("   ")
            i += 3
            continue
        if c in ('"', "'"):
            stack.append([c])
            out.append(" ")
            i += 1
            continue
        if top is not None and top[0] == "{":
            if c == "{":
                top[1] += 1
            elif c == "}":
                top[1] -= 1
                if top[1] == 0:
                    stack.pop()
                    out.append(" ")
                    i += 1
                    continue
        out.append(c)
        i += 1
    return code_lines, "".join(out)


def sloc(files: list[Path]) -> int:
    return sum(len(scan(f.read_text(encoding="utf-8"))[0]) for f in files)


def code(files: list[Path]) -> str:
    return "\n".join(scan(f.read_text(encoding="utf-8"))[1] for f in files)


CATCHING = r"\bcatch\s*\(|\brunCatching\b"

# Library entry points outside the `kit` packages whose failure or timeout comes back to the caller as a
# value, per implementation: (name shown in RESULTS.md, pattern). Ballarch's runtime catches what an adapter
# throws (Runtime.kt, around `adapter.execute`) and what a decider throws (`decideGuarded`), and resolves a
# `Call` that times out or fails as an `Outcome`; `Runtime.request` reports its timeout the same way.
LIBRARY_VALUE_CALLS = {
    "ballarch": [("Call", r"\bCall\s*\("), ("Runtime.request", r"\.request\s*(?:<[^<>]*>)?\s*\(")],
}


def catching_functions(files: list[Path]) -> list[str]:
    """Toolkit functions that catch exceptions, directly or through another such function."""
    bodies: dict[str, str] = {}
    for f in files:
        parts = re.split(r"\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>]+\.)?(\w+)\s*\(", code([f]))
        for name, body in zip(parts[1::2], parts[2::2]):
            bodies[name] = bodies.get(name, "") + body
    found: set[str] = set()
    while True:
        more = {name for name, body in bodies.items() if name not in found and (
            re.search(CATCHING, body) or any(re.search(rf"\b{g}\s*\(", body) for g in found))}
        if not more:
            return sorted(found)
        found |= more


def diff_lines(path: Path) -> list[str]:
    """The lines of [path] that hold code, without indentation, `package` and `import` lines."""
    text = path.read_text(encoding="utf-8")
    lines = text.split("\n")
    kept = [lines[i].strip() for i in sorted(scan(text)[0])]
    return [l for l in kept if not l.startswith(("package ", "import "))]


def change(design: dict) -> dict:
    """What a scenario-B design changed: each file of its package against the file at the same path under the
    design it adapts. Lines are compared without indentation; `package` and `import` lines are ignored."""
    base = package_dir(design["module"], design["root"])
    keyed = package_dir(design["module"], design["package"])
    files, added, removed = [], 0, 0
    for f in sorted(keyed.rglob("*.kt")):
        rel = f.relative_to(keyed)
        original = base / rel
        old = diff_lines(original) if original.exists() else []
        new = diff_lines(f)
        a = r = 0
        for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, old, new, autojunk=False).get_opcodes():
            if tag != "equal":
                r += i2 - i1
                a += j2 - j1
        if a or r:
            files.append(("" if original.exists() else "new: ") + rel.as_posix())
            added += a
            removed += r
    return {"files": files, "added": added, "removed": removed}


def occurrences(text: str, pattern: str) -> int:
    return len(re.findall(pattern, text))


def pct(rate: dict) -> str:
    return f"{100 * rate['rate']:.1f} [{100 * rate['low']:.1f}, {100 * rate['high']:.1f}]"


PROFILE_LABEL = {"Stress": "Stress", "Mild": "Mild", "BeyondHorizon": "Beyond horizon"}

NAIVE = ("layered", "actors", "mvu", "saga")
RECONCILING = ("hardened", "actors-careful", "mvu-careful", "saga-careful")


def change_impact(L: list[str], data: dict, label: dict) -> None:
    crs = [(c["key"], c["title"]) for c in data["changeRequests"]]
    short = {k: k.split("-")[0].upper() for k, _ in crs}
    rows = {(p["changeRequest"], p["design"]): p for p in data["patches"]}
    designs = data["designs"]

    def code(p: dict) -> int:
        return sum(p["codeLines"].values())

    def with_imports(p: dict) -> int:
        return code(p) + p["importLinesChanged"]

    def physical(p: dict) -> int:
        return sum(p["physicalLines"].values())

    def tokens(p: dict) -> int:
        return p["tokens"]["added"] + p["tokens"]["removed"]

    def span(values: list[int]) -> str:
        return f"{min(values):,}–{max(values):,}" if min(values) != max(values) else f"{min(values):,}"

    def rank(value: int, others: list[int]) -> str:
        r = 1 + sum(1 for o in others if o < value)
        return f"{r}{'=' if value in others else ''}"

    L.append("\n## 13. Change impact: four change requests\n")
    L.append("Four change requests, specified in [`changes/README.md`](../changes/README.md) before they were implemented, were applied to the "
             "scenario-A code of every design, each as one patch per design (`benchmark/changes/<change request>/<design>.patch`). CR1 refunds a "
             "completed order that the customer cancels within 30 s; CR2 awards loyalty points through a second external service without "
             "idempotency keys; CR3 adds a second payment provider and fails over to it when the first provably did not charge (the naive versions: "
             "when the first failed); CR4 removes the inventory. The same author wrote every patch, after the base results were known.\n")
    L.append("Three sizes are given for each patch, all computed per file by `difflib` on the code without `package` and `import` lines; in a "
             "replaced block, the first min(old, new) lines count as changed and the rest as added or removed. *Code lines* are logical lines: a "
             "statement, a declaration header or a `when` branch, however it is wrapped, compared as a sequence of tokens, so they do not depend "
             "on line wrapping. *Physical lines* are lines that hold code, compared without indentation, as in section 9; they depend on how "
             "each statement is wrapped (the patches follow one formatting rule, given in changes/README.md). *Tokens* (identifiers, keywords, "
             "literals, operators and brackets) added and removed depend on no formatting, but count a long qualified name or an explicit type "
             "argument as several tokens. The import lines added or removed are counted separately; *with imports* adds them to the code lines. "
             "A unit is a top-level class, interface or object with everything nested in it, or the top-level functions and properties of one "
             "file; it is touched when one of its code lines is added, removed or changed. Unit counts depend on how a design splits its "
             "declarations: Ballarch's `Api.kt` declares each request type as a top-level class, so a change to its messages touches several "
             "small units.\n")
    L.append("### Size of each change\n")
    L.append("| Change request | Design | Files touched (new, deleted) | Units touched (new, removed) | Code lines added | removed | changed | "
             "total | Import lines | Code lines with imports | Physical lines | Tokens |")
    L.append("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for cr, title in crs:
        for d in designs:
            p = rows.get((cr, d))
            if p is None:
                continue
            s = p["codeLines"]
            L.append(f"| {title} | {label[d]} | {p['filesTouched']} ({p['filesAdded']}, {p['filesDeleted']}) | "
                     f"{p['unitsTouched']} ({p['unitsAdded']}, {p['unitsRemoved']}) | {s['added']} | {s['removed']} | {s['changed']} | "
                     f"{code(p)} | {p['importLinesChanged']} | {with_imports(p)} | {physical(p)} | {tokens(p):,} |")

    measures = [("Code lines", code), ("With imports", with_imports), ("Physical lines", physical), ("Tokens", tokens)]
    groups = [(cr, title, [cr]) for cr, title in crs] + [("all", "All four change requests", [cr for cr, _ in crs])]

    def total(f, design: str, keys: list[str]) -> int | None:
        ps = [rows.get((k, design)) for k in keys]
        return None if any(p is None for p in ps) else sum(f(p) for p in ps)

    L.append("\n### Ballarch against the other designs\n")
    L.append("Ballarch's value, and the four careful versions and the four naive versions as ranges. The last row adds up the four change "
             "requests per design.\n")
    head = " | ".join(f"{m}: Ballarch | careful | naive" for m, _ in measures)
    L.append(f"| Change request | {head} |")
    L.append("|---|" + "---|" * (3 * len(measures)))
    for key, title, keys in groups:
        cells = []
        for _, f in measures:
            b = total(f, "ballarch", keys)
            careful = [v for v in (total(f, d, keys) for d in RECONCILING) if v is not None]
            naive = [v for v in (total(f, d, keys) for d in NAIVE) if v is not None]
            cells += [f"{b:,}" if b is not None else "—", span(careful) if careful else "—", span(naive) if naive else "—"]
        L.append(f"| {title} | " + " | ".join(cells) + " |")

    L.append("\nRank of Ballarch's change among the five designs that reconcile (Ballarch and the four careful versions): 1 is the smallest "
             "change, 5 the largest; `=` marks a tie with a careful version. Files and units touched are given for Ballarch and as the careful "
             "versions' range.\n")
    L.append("| Change request | " + " | ".join(m for m, _ in measures) + " | Files: Ballarch | careful | Units: Ballarch | careful |")
    L.append("|---|" + "---|" * (len(measures) + 4))
    for key, title, keys in groups:
        ranks = []
        for _, f in measures:
            b = total(f, "ballarch", keys)
            careful = [v for v in (total(f, d, keys) for d in RECONCILING) if v is not None]
            ranks.append(rank(b, careful) if b is not None and careful else "—")
        if key == "all":
            extra = ["—", "—", "—", "—"]
        else:
            b = rows.get((key, "ballarch"))
            careful = [rows[(key, d)] for d in RECONCILING if (key, d) in rows]
            extra = [str(b["filesTouched"]), span([p["filesTouched"] for p in careful]), str(b["unitsTouched"]),
                     span([p["unitsTouched"] for p in careful])] if b and careful else ["—"] * 4
        L.append(f"| {title} | " + " | ".join(ranks + extra) + " |")

    v = data.get("verification", {}).get("changeRequests")
    if not v:
        return
    settings = {(e["runsPerCell"], e["sweepSeeds"]) for e in v.values()}
    uniform = len(settings) == 1
    n_all, seeds_all = next(iter(settings))

    def of(count: int, runs: int) -> str:
        return f"{count:,}" if uniform else f"{count:,} of {runs:,}"

    L.append("\n### Correctness after each change\n")
    runs_text = (f"{n_all:,} runs per profile with and without a crash, and the single-crash sweep on {seeds_all:,} seeds under Stress and Mild"
                 if uniform else "the runs per profile and crash setting and the sweep seeds recorded for each change request in `changes.json`")
    L.append(f"[`changes/verify.py`](../changes/verify.py) applies the patches of a change request in a copy of the sources, builds it, and runs every "
             f"design under the change request's scenario: {runs_text}. It checks the base requirements and the change request's own. *Without "
             "the change* is the unpatched design in the same scenario: its runs that violate one of the change request's requirements show that "
             "the new requirements catch a design that lacks the change. Stress and Mild respect the delivery horizon; Beyond horizon does not. "
             "The last column counts, for Beyond horizon with the change, the runs with each violation (a run can have several).\n")
    held_of = f" (of {4 * n_all:,})" if uniform else ""
    beyond_of = f" (of {2 * n_all:,})" if uniform else ""
    L.append(f"| Change request | Design | Without the change: runs violating the CR's requirements, Stress and Mild{held_of} | With the change: "
             f"runs with any violation, Stress and Mild{held_of} | With the change: violating sweep points | With the change: runs with any "
             f"violation, Beyond horizon{beyond_of} | Violations seen with the change | Beyond horizon with the change: runs per violation |")
    L.append("|---|---|---|---|---|---|---|---|")
    for cr, title in crs:
        e = v.get(cr)
        if e is None:
            continue
        n = e["runsPerCell"]
        for d in designs:
            after = e["after"].get(d)
            before = e["before"].get(d)
            if after is None or before is None:
                continue
            held = [c for c in after["cells"] if c["profile"] != "BeyondHorizon"]
            beyond = [c for c in after["cells"] if c["profile"] == "BeyondHorizon"]
            before_held = [c for c in before["cells"] if c["profile"] != "BeyondHorizon"]
            points = sum(s["points"] for s in after["sweeps"])
            violating = sum(s["violating"] for s in after["sweeps"])
            seen = sorted({k for c in after["cells"] for k in list(c["violations"]) + list(c["changeViolations"])}
                          | {k for s in after["sweeps"] for k in list(s["violations"]) + list(s["changeViolations"])})
            per: dict[str, int] = {}
            for c in beyond:
                for k, x in list(c["violations"].items()) + list(c["changeViolations"].items()):
                    per[k] = per.get(k, 0) + x
            per_text = ", ".join(f"{k} {x:,}" for k, x in sorted(per.items())) or "—"
            L.append(f"| {title} | {label[d]} | {of(sum(c['changeViolating'] for c in before_held), 4 * n)} | "
                     f"{of(sum(c['violating'] for c in held), 4 * n)} | {violating:,} of {points:,} | "
                     f"{of(sum(c['violating'] for c in beyond), 2 * n)} | {', '.join(seen) or '—'} | {per_text} |")

    L.append("\n### Scenario-B variants that the patches leave out\n")
    L.append("The scenario-B variants of section 3 reuse each design's scenario-A code: they call its constructors and use its message types. "
             "A patch changes only the scenario-A code, so where it changes a constructor or a type, the variant no longer compiles; "
             "[`changes/verify.py`](../changes/verify.py) compiles each patched design with its scenario-B packages and records the files that "
             "fail, then removes those packages before it runs the change request. Each cell gives the files that no longer compile and, in "
             "parentheses, the compiler's errors in them. The edits these files need are not in the patches and not in the counts above, and a "
             "variant would also need the change request itself, which this suite does not implement.\n")
    L.append("| Design | " + " | ".join(short[cr] for cr, _ in crs) + " |")
    L.append("|---|" + "---|" * len(crs))
    for d in designs:
        cells = []
        for cr, _ in crs:
            broken = v.get(cr, {}).get("scenarioBCompileErrors", {}).get(d)
            cells.append("—" if broken is None else "0" if not broken else f"{len(broken)} ({sum(broken.values())})")
        L.append(f"| {label[d]} | " + " | ".join(cells) + " |")

    L.append("\n### What each change does at run time\n")
    L.append("Means per run, with the change, without crashes. CR1: orders refunded, and orders that ended `NeedsAttention` for any reason "
             "(an unresolved charge or an unconfirmed refund); CR2: awards made, and awards handed to an operator; CR3: orders completed, and "
             "orders charged at the secondary provider; CR4: orders completed (there is no stock limit any more), and orders that ended "
             "`NeedsAttention`. The five reconciling designs apply the same policy, so without crashes they reach the same outcomes; so do the "
             "four naive ones.\n")
    L.append("| Change request | Design | Stress | Mild |")
    L.append("|---|---|---|---|")

    def effect(cr: str, c: dict) -> str:
        k = c["countsPerRun"]
        if cr == "cr1-refund":
            return f"{k['refunded']:.3f} refunded, {3 * c['needsAttentionPerOrder']:.3f} needing attention"
        if cr == "cr2-loyalty":
            return f"{k['awarded']:.3f} awarded, {k['awardNeedsAttention']:.3f} needing attention"
        if cr == "cr3-failover":
            return f"{c['completedPerRun']:.3f} completed, {k['chargedAtSecondary']:.3f} charged at the secondary"
        return f"{c['completedPerRun']:.3f} completed, {3 * c['needsAttentionPerOrder']:.3f} needing attention"

    for cr, title in crs:
        for d in designs:
            after = v.get(cr, {}).get("after", {}).get(d)
            if after is None:
                continue
            cells = {(c["profile"], c["crashes"]): c for c in after["cells"]}
            L.append(f"| {title} | {label[d]} | {effect(cr, cells[('Stress', False)])} | {effect(cr, cells[('Mild', False)])} |")


def overhead(L: list[str], data: dict) -> None:
    rounds = data.get("rounds", 1)
    L.append("\n## 14. Runtime overhead (machine-dependent)\n")
    L.append(f"Measured by `./gradlew :benchmark:runner:overhead`, which writes `benchmark/results/overhead.json`, on {data['jvm']}, {data['os']}, "
             f"{data['availableProcessors']} processors ({data['measuredAt'][:10]}). These numbers depend on the machine and its load, so "
             "`overhead.json` is not regenerated by CI and is not part of the byte-for-byte check of `results.json`. Each variant performs the same "
             "request, a counter increment, and is timed per request after a warm-up; *bytes* are those allocated on the caller's thread, which "
             f"for the same-thread variant is all the work of a request. Every variant was measured in {rounds} rounds in one JVM, each with a "
             "fresh runtime and its own warm-up, the rounds taking the variants in turn; each statistic is the median of the rounds, with the "
             "lowest and highest round in parentheses.\n")
    L.append("| Variant | What runs | Requests per round (warm-up) | Median | 99th percentile | Mean | Bytes per request, caller's thread |")
    L.append("|---|---|---|---|---|---|---|")

    def us(nanos: int) -> str:
        return f"{nanos / 1000:,.2f} µs"

    def stat(r: dict, key: str, fmt) -> str:
        values = [x[key] for x in r.get("rounds", [])]
        return f"{fmt(r[key])} ({fmt(min(values))}–{fmt(max(values))})" if len(values) > 1 else fmt(r[key])

    for key, r in data["variants"].items():
        L.append(f"| `{key}` | {r['description']} | {r['iterations']:,} ({r['warmupIterations']:,}) | {stat(r, 'medianNanos', us)} | "
                 f"{stat(r, 'p99Nanos', us)} | {stat(r, 'meanNanos', us)} | {stat(r, 'callerThreadBytesPerRequest', lambda b: f'{b:,}')} |")


def main() -> None:
    results = Path(sys.argv[1])
    out = Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    data = json.loads(results.read_text())
    if results.resolve() != (out / "results.json").resolve():
        shutil.copy(results, out / "results.json")
    archs = data["architectures"]
    keyed = data["keyedDesigns"]
    keys = [a["key"] for a in archs]
    label = {a["key"]: a["label"] for a in archs}
    keyed_label = {d["key"]: d["label"] for d in keyed}
    cells = {(c["architecture"], c["keys"], c["profile"], c["crashes"]): c for c in data["dynamic"]}
    profiles = list(data["config"]["profiles"].keys())
    runs = data["config"]["runsPerCell"]
    st = data["structure"]
    src = {}
    for a in archs:
        files = sources(a)
        text = code(files)
        kit = toolkit_sources(a)
        entry_points = [(g, rf"\b{g}\s*\(") for g in catching_functions(kit)] + LIBRARY_VALUE_CALLS.get(a["key"], [])
        src[a["key"]] = {
            "sloc": sloc(files),
            "toolkit_sloc": sloc(kit) if kit else None,
            "handlers": occurrences(text, CATCHING),
            "value_calls": sum(occurrences(text, pattern) for _, pattern in entry_points),
            "entry_points": [name for name, _ in entry_points],
            "failure_values": occurrences(text, r"\bReply\.Failure\b"),
        }
    scenarios = [(False, keys, label), (True, [d["key"] for d in keyed], keyed_label)]
    scenario_name = {False: "A", True: "B"}

    L = []
    L.append("# Architecture benchmark: results\n")
    L.append(f"Generated by `benchmark/report.py` from `results.json` ({runs} runs per cell). "
             "Method, definitions and threats to validity: [README](../README.md). "
             "Rates are shares of runs, with Wilson 95% intervals in brackets.\n")
    L.append("Scenario A: the provider has no idempotency keys. Scenario B: the provider honours idempotency keys, and every design is "
             "adapted to send them. In both, Stress and Mild respect the provider's declared 1 s delivery horizon; under Beyond horizon about 2% of "
             "requests take effect 5 s after they were sent. In crash cells every run crashes exactly once. The *naive* designs share one "
             "provider-failure policy chosen by the author (a timeout or I/O error means the payment failed); the *careful* designs handle "
             "uncertainty with their style's own tools.\n")
    cols = [(p, c) for p in profiles for c in (False, True)]
    kinds = list(data["dynamic"][0]["violations"].keys())

    def correctness(keyed_scenario: bool, rows: list[str], names: dict) -> None:
        L.append("| Design | " + " | ".join(f"{PROFILE_LABEL[p]}, {'one crash' if c else 'no crash'}" for p, c in cols) + " |")
        L.append("|---|" + "---|" * len(cols))
        for k in rows:
            L.append(f"| {names[k]} | " + " | ".join(pct(cells[(k, keyed_scenario, p, c)]["anyViolation"]) for p, c in cols) + " |")

    def kinds_table(keyed_scenario: bool, rows: list[str], names: dict) -> None:
        L.append("| Design | Profile | Crash | " + " | ".join(kinds) + " |")
        L.append("|---|---|---|" + "---|" * len(kinds))
        for k in rows:
            for p, c in cols:
                cell = cells[(k, keyed_scenario, p, c)]
                L.append(f"| {names[k]} | {PROFILE_LABEL[p]} | {'one' if c else 'none'} | " + " | ".join(str(cell["violations"][v]["hits"]) for v in kinds) + " |")

    L.append("## 1. Correctness under faults, scenario A (no idempotency keys): runs with at least one violated requirement (%)\n")
    correctness(False, keys, label)
    L.append(f"\n## 2. Which requirement was violated, scenario A (runs, of {runs})\n")
    kinds_table(False, keys, label)

    L.append("\n## 3. Scenario B: a provider that honours idempotency keys\n")
    L.append("Every charge request carries an idempotency key, and the provider does not charge a key twice. The naive designs send a key "
             "derived from the order and repeat a request that got no answer with the same key, up to three requests per incarnation, then "
             "treat the payment as failed. *Layered, same-key retries, then NeedsAttention* exists only in this scenario: the naive layered "
             "design whose retries end in `NeedsAttention` instead of `Failed`. The careful designs repeat an unanswered or refused request "
             "with the same key within the same budget of three requests, then reconcile as before. Ballarch declares the charge port "
             "Idempotent and sends the call identity as the key; its adapter ends an attempt after 1 s, and its runtime repeats a failed "
             "attempt with the same key, up to three requests within the call's 3 s deadline, then the payment Ball reconciles as before. "
             "*Ballarch, reference adapter* exists only in this scenario: the same adaptation with an adapter whose attempt ends at the "
             "call's 1 s deadline, as in the reference HTTP adapter, so that the runtime never repeats a request whose answer was lost.\n")
    L.append("### Runs with at least one violated requirement (%)\n")
    correctness(True, scenarios[1][1], keyed_label)
    L.append(f"\n### Which requirement was violated (runs, of {runs})\n")
    kinds_table(True, scenarios[1][1], keyed_label)

    L.append("\n## 4. Crash points\n")
    L.append("A crash time is drawn uniformly from the whole milliseconds of the busy period of the same seed's crash-free run: "
             "from 0 until every accepted order has its terminal state. The process crashes at that time, before every scheduler "
             "task due at it, and restarts 50 ms later; the client then sends again every order the system has not durably accepted.\n")
    L.append("| Scenario | Design | Profile | Runs that crashed | Busy period, mean (ms) | Crash time, mean (ms) | Crashes before any order was accepted | Orders accepted / run |")
    L.append("|---|---|---|---|---|---|---|---|")
    for ks, rows, names in scenarios:
        for k in rows:
            for p in profiles:
                c = cells[(k, ks, p, True)]
                L.append(f"| {scenario_name[ks]} | {names[k]} | {PROFILE_LABEL[p]} | {c['crashedRuns']} of {c['runs']} | {c['busyMillisMean']:.0f} | "
                         f"{c['crashMillisMean']:.0f} | {c['crashesBeforeAcceptance']} | {c['acceptedPerRun']:.2f} |")

    sweep_seeds = data["config"]["sweepSeeds"]
    L.append("\n## 5. Exhaustive single-crash sweep\n")
    L.append(f"A separate check of every point at which a single crash can hit: for each of the first {sweep_seeds} crash-cell seeds, "
             "one run per scheduler task of its crash-free run, crashing immediately before that task. It includes the points between "
             "two tasks of the same instant, which a crash time never selects. The points are each design's own scheduler tasks, so "
             "their number, and the share of them that violate, depend on how finely a design splits its work into tasks: the share "
             "is not comparable across designs.\n")
    L.append("| Scenario | Design | Profile | Crash points | Violating points (%) | Seeds with a violating point | Violations seen |")
    L.append("|---|---|---|---|---|---|---|")
    for sw in data["crashSweep"]:
        names = keyed_label if sw["keys"] else label
        seen = ", ".join(v for v, x in sw["violations"].items() if x)
        share = 100 * sw["violating"] / sw["points"] if sw["points"] else 0
        L.append(f"| {scenario_name[sw['keys']]} | {names[sw['architecture']]} | {PROFILE_LABEL[sw['profile']]} | {sw['points']} | {sw['violating']} ({share:.1f}) | "
                 f"{sw['seedsWithViolation']} of {sw['seeds']} | {seen or '—'} |")

    L.append("\n## 6. Seeded bugs\n")
    L.append("Each correct implementation with one plausible mistake injected from outside its code, run in scenario A. "
             "A detected bug shows that a zero rate above is not a blind spot of the oracles. The recovery bugs change only what a "
             "restarted process finds, so they can be detected only in runs that crash.\n")
    L.append("| Seeded bug | Kind | Profile | Runs with a violation, no crash (%) | Runs with a violation, one crash (%) | Violations seen |")
    L.append("|---|---|---|---|---|---|")
    seeded = {(c["name"], c["profile"], c["crashes"]): c for c in data["seeded"]}
    for name in dict.fromkeys(c["name"] for c in data["seeded"]):
        for p in dict.fromkeys(c["profile"] for c in data["seeded"] if c["name"] == name):
            free, crash = seeded[(name, p, False)], seeded[(name, p, True)]
            seen = ", ".join(k for k in kinds if free["violations"][k]["hits"] or crash["violations"][k]["hits"])
            kind = "recovery" if crash["recovery"] else "classification"
            L.append(f"| {name} | {kind} | {PROFILE_LABEL[p]} | {pct(free['anyViolation'])} | {pct(crash['anyViolation'])} | {seen or '—'} |")

    L.append("\n## 7. What each design costs at run time\n")
    L.append("Outcomes and provider requests, per order or per run, for each scenario, profile and crash setting.\n")
    L.append("| Scenario | Design | Profile | Crash | Completed / accepted order | Needs attention / order | Charge requests / order | Status queries / order | Busy period, mean (ms) |")
    L.append("|---|---|---|---|---|---|---|---|---|")
    for ks, rows, names in scenarios:
        for k in rows:
            for p in profiles:
                for c_ in (False, True):
                    c = cells[(k, ks, p, c_)]
                    done = c["completedPerRun"] / c["acceptedPerRun"] if c["acceptedPerRun"] else 0
                    busy = f"{c['busyMillisMean']:.0f}" if c_ else "—"
                    L.append(f"| {scenario_name[ks]} | {names[k]} | {PROFILE_LABEL[p]} | {'one' if c_ else 'none'} | {done:.3f} | {c['needsAttentionPerOrder']:.3f} | "
                             f"{c['chargeRequestsPerOrder']:.2f} | {c['statusRequestsPerOrder']:.2f} | {busy} |")

    L.append("\n## 8. Structure of the feature code (scenario A)\n")
    L.append("Feature code excludes the shared simulated environment, the scenario-B packages and the `kit` packages that stand in for an actor, MVU or messaging framework "
             "(their size is listed separately; a naive design and its careful variant share one kit). Ballarch's runtime is a library and is likewise excluded; its kernel and runtime types count as framework types, "
             "as the kit types do. Propagation cost is given at three granularities because it depends on how finely the code is cut into nodes; none of them is the reference.\n")

    def opt(v, fmt):
        return "n/a" if v is None else fmt.format(v)

    rows = [
        ("Source lines (SLOC)", lambda k: str(src[k]["sloc"])),
        ("Toolkit SLOC excluded", lambda k: "—" if src[k]["toolkit_sloc"] is None else str(src[k]["toolkit_sloc"])),
        ("Bytecode instructions (JVM opcodes)", lambda k: str(st[k]["instructions"])),
        ("Named classes (units)", lambda k: str(st[k]["units"])),
        ("Top-level classes", lambda k: str(st[k]["topLevelClasses"])),
        ("Source files", lambda k: str(st[k]["sourceFiles"])),
        ("Packages", lambda k: str(st[k]["packages"])),
        ("CBO, mean (max)", lambda k: f"{st[k]['cboMean']:.2f} ({st[k]['cboMax']})"),
        ("LCOM4, mean (classes with LCOM4 = 1, of classes measured)",
         lambda k: f"{st[k]['lcom4Mean']:.2f} ({st[k]['lcom4CohesiveClasses']} of {st[k]['lcom4Measured']})"),
        ("CAMC, mean (classes measured)", lambda k: f"{st[k]['camcMean']:.2f} ({st[k]['camcMeasured']})"),
        ("Distance from the main sequence D, mean: feature packages only", lambda k: opt(st[k]["distanceMean"], "{:.2f}")),
        ("Distance from the main sequence D, mean: all dependencies but the JDK and Kotlin's standard library", lambda k: opt(st[k]["distanceMeanAll"], "{:.2f}")),
        ("Propagation cost, per named class", lambda k: f"{100 * st[k]['propagationCost']:.1f}%"),
        ("Propagation cost, per top-level class", lambda k: f"{100 * st[k]['propagationCostTopLevel']:.1f}%"),
        ("Propagation cost, per source file", lambda k: f"{100 * st[k]['propagationCostFile']:.1f}%"),
        ("Dependency cycles (units in cycles)", lambda k: f"{st[k]['cyclicGroups']} ({st[k]['unitsInCycles']})"),
        ("Functions", lambda k: str(st[k]["functions"])),
        ("Cyclomatic complexity per function, mean (max)", lambda k: f"{st[k]['ccnMean']:.2f} ({st[k]['ccnMax']})"),
        ("Cyclomatic complexity, total", lambda k: str(st[k]["ccnTotal"])),
        ("Framework types used", lambda k: str(st[k]["frameworkTypes"])),
        ("Exception handlers (`catch`, `runCatching`)", lambda k: str(src[k]["handlers"])),
        ("Calls whose failure or timeout a library returns as a value", lambda k: str(src[k]["value_calls"])),
        ("Checks of provider failure values (`Reply.Failure`)", lambda k: str(src[k]["failure_values"])),
        ("Share of code in pure units", lambda k: f"{100 * st[k]['pureShare']:.0f}%"),
    ]
    L.append("| Metric | " + " | ".join(label[k] for k in keys) + " |")
    L.append("|---|" + "---|" * len(keys))
    for name, f in rows:
        L.append(f"| {name} | " + " | ".join(f(k) for k in keys) + " |")
    entry = "; ".join(f"{k}: {', '.join(f'`{g}`' for g in src[k]['entry_points'])}" for k in keys if src[k]["entry_points"])
    L.append(f"\nLibrary functions whose failure or timeout comes back as a value, counted where the feature code calls them: {entry or 'none'}. "
             "The actor kit's functions catch exceptions and deliver them as `Result` messages; Ballarch's runtime catches adapter "
             "exceptions and resolves calls that fail or time out as `Outcome` values. "
             "Ballarch's adapters call the provider's callback API, which reports failures as `Reply.Failure` values; "
             "the other implementations call the throwing `ProviderClient`.")

    L.append("\n## 9. Scenario B: what each design changed for idempotency keys\n")
    L.append("Each scenario-B design lives in its own package, whose files replace the files at the same paths in the design it adapts. "
             "Lines that hold code are compared without indentation; `package` and `import` lines are ignored, so a file that was copied "
             "only to import a changed class counts as unchanged. Wiring outside the module (the runner) is not counted.\n")
    L.append("| Scenario-B design | Adapts | Files changed | SLOC added or changed | SLOC removed or changed |")
    L.append("|---|---|---|---|---|")
    for d in keyed:
        ch = change(d)
        files = ", ".join(f"`{f}`" for f in ch["files"]) or "—"
        L.append(f"| {d['label']} | {label[d['base']]} | {files} | {ch['added']} | {ch['removed']} |")

    L.append("\n## 10. Cohesion per class\n")
    L.append("Every class measured for LCOM4 and CAMC (classes whose only methods are generated or are accessors are not measured).\n")
    L.append("| Design | Class | Methods | LCOM4 | CAMC |")
    L.append("|---|---|---|---|---|")
    for k in keys:
        for c in st[k]["cohesionDetail"]:
            L.append(f"| {label[k]} | `{c['class']}` | {c['methods']} | {c['lcom4']} | {opt(c['camc'], '{:.2f}')} |")

    L.append("\n## 11. Cyclomatic complexity per function\n")
    L.append("<details><summary>Every hand-written function</summary>\n")
    L.append("| Design | Function | Complexity |")
    L.append("|---|---|---|")
    for k in keys:
        for f in st[k]["complexityDetail"]:
            L.append(f"| {label[k]} | `{f['function']}` | {f['ccn']} |")
    L.append("\n</details>")

    L.append("\n## 12. Package metrics (Martin)\n")
    L.append("Ce counts the classes of a package that depend on a class outside it. *Ce* and *D* count only dependencies on other feature packages; "
             "*Ce all* and *D all* also count dependencies on libraries, toolkit packages and the shared simulation, but not on the JDK or Kotlin's standard library.\n")
    L.append("| Design | Package | Ca | Ce | Ce all | A | I | D | I all | D all |")
    L.append("|---|---|---|---|---|---|---|---|---|---|")
    for k in keys:
        for p in st[k]["packagesDetail"]:
            L.append(f"| {label[k]} | `{p['name']}` | {p['ca']} | {p['ce']} | {p['ceAll']} | {p['A']:.2f} | {p['I']:.2f} | {p['D']:.2f} | {p['IAll']:.2f} | {p['DAll']:.2f} |")

    changes = out / "changes.json"
    if changes.exists():
        change_impact(L, json.loads(changes.read_text(encoding="utf-8")), label)
    overhead_file = out / "overhead.json"
    if overhead_file.exists():
        overhead(L, json.loads(overhead_file.read_text(encoding="utf-8")))

    L.append("\nSLOC, the three counts of failure handling and the scenario-B changes: `benchmark/report.py` (Kotlin sources). Change impact: "
             "`benchmark/changes/measure.py` (patches) and `benchmark/changes/verify.py` (runs). Runtime overhead: the runner's `overhead` task. "
             "Every other metric: `benchmark/runner` (ASM bytecode analysis).\n")
    (out / "RESULTS.md").write_text("\n".join(L) + "\n", encoding="utf-8")
    print(f"wrote {out / 'RESULTS.md'}")


if __name__ == "__main__":
    main()
