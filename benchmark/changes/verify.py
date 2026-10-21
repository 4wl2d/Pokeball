#!/usr/bin/env python3
"""Checks that every design still meets every requirement after each change request.

For each change request, in a temporary copy of reference/kotlin and
benchmark (outside any git work tree):

1. apply every patch of the change request (`git apply`), one per design;
2. compile the patched designs with their scenario-B packages, and record
   which files of those packages no longer compile. The scenario-B variants
   (results.json, "keyedDesigns") reuse scenario-A code: they call its
   constructors and use its types, so a patch that changes a constructor or
   a message type breaks them. The patches do not include the edits these
   call sites would need, and they do not give the variants the change
   request itself; the list shows what is left out. A module whose scenario-B
   package fails stops the modules that depend on it from compiling, so the
   designs are compiled in two passes: first every design except the three
   naive modules that careful designs depend on (actors, mvu, saga), with
   those three modules' scenario-B packages removed; then those three alone;
3. remove the scenario-B packages of every design, and build and run the
   change requests' runner, `:benchmark:changes:run`, which runs each design
   under the change request's scenario and checks the base requirements and
   the change request's own.

The unpatched designs are run the same way, as a baseline that shows the new
requirements catch a design without the change. Only the designs and the
change requests' runner are built in the copies: neither the benchmark runner
(which uses the scenario-B packages) nor any test is built with the patches.

The results go to the "verification" part of benchmark/results/changes.json,
one entry per change request with the run counts it was verified with, so a
run with --only replaces the entries of the change requests it verifies and
keeps the others; the "patches" part, written by measure.py, is kept.

The simulation is deterministic, so the results do not depend on the machine.

Needs Python 3.10+, git, and what the Gradle build needs (JDK 21).
Usage: python3 benchmark/changes/verify.py [--runs N] [--sweep-seeds N] [--work DIR] [--keep]
       [--only CR ...] [--gradle-arg ARG ...]
Example (as used for the published file):
  python3 benchmark/changes/verify.py --gradle-arg=-I --gradle-arg=/path/to/mirror.init.gradle
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
BENCH = HERE.parent
REPO = BENCH.parent
RESULTS = BENCH / "results" / "changes.json"

sys.path.insert(0, str(HERE))
from measure import CHANGE_REQUESTS, DESIGNS  # noqa: E402

IGNORED = shutil.ignore_patterns("build", ".gradle", ".kotlin", "__pycache__", "results")
NAIVE = ("layered", "actors", "mvu", "saga")
# Naive modules that a careful module depends on (for its toolkit); see each careful module's build file.
DEPENDED_ON = ("actors", "mvu", "saga")
COMPILE_ERROR = re.compile(r"^e: (?:file://)?(/[^:]+\.kt):(\d+):\d+ (.*)$")


def scenario_b_packages() -> dict[str, list[Path]]:
    """The scenario-B package directories of every design, relative to the copy's root, from results.json."""
    data = json.loads((BENCH / "results" / "results.json").read_text(encoding="utf-8"))
    module = {a["key"]: a["module"] for a in data["architectures"]}
    out: dict[str, list[Path]] = {}
    for d in data["keyedDesigns"]:
        out.setdefault(d["base"], []).append(Path("benchmark", module[d["base"]], "src", "main", "kotlin", *d["package"].split(".")))
    return out


def copy_sources(dest: Path) -> None:
    shutil.copytree(REPO / "reference" / "kotlin", dest / "reference" / "kotlin", ignore=IGNORED)
    shutil.copytree(BENCH, dest / "benchmark", ignore=IGNORED)


def gradle(root: Path, gradle_args: list[str], *tasks: str, check: bool = True) -> subprocess.CompletedProcess:
    cmd = ["./gradlew", *gradle_args, "-q", *tasks]
    print("$", " ".join(cmd), f"(in {root / 'reference' / 'kotlin'})", flush=True)
    return subprocess.run(cmd, cwd=root / "reference" / "kotlin", check=check, capture_output=not check, text=True)


def run_changes(root: Path, out: Path, runs: int, sweep: int, crs: list[str], gradle_args: list[str]) -> dict:
    gradle(root, gradle_args, ":benchmark:changes:run", f"--args={out} {runs} {sweep} {' '.join(crs)}")
    return json.loads(out.read_text(encoding="utf-8"))


def variant_compile_errors(root: Path, designs: list[str], packages: dict[str, list[Path]], gradle_args: list[str]) -> dict:
    """For each design, the files of its scenario-B packages that do not compile, with their error count."""
    errors: dict[str, dict[str, int]] = {d: {} for d in designs}
    holding = root / "held-scenario-b"
    for group in ([d for d in designs if d not in DEPENDED_ON], [d for d in designs if d in DEPENDED_ON]):
        if not group:
            continue
        held = [p for d in designs if d not in group for p in packages.get(d, [])]
        for p in held:
            (holding / p).parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(root / p), str(holding / p))
        try:
            r = gradle(root, gradle_args, "--continue", *[f":benchmark:{d}:compileKotlin" for d in group], check=False)
        finally:
            for p in held:
                shutil.move(str(holding / p), str(root / p))
        output = r.stdout + r.stderr
        unexplained = []
        for line in output.splitlines():
            m = COMPILE_ERROR.match(line.strip())
            if not m:
                continue
            path = Path(m[1]).resolve().relative_to(root.resolve())
            owner = next((d for d in group for p in packages.get(d, []) if p in path.parents), None)
            if owner is None:
                unexplained.append(line)
            else:
                errors[owner][path.as_posix()] = errors[owner].get(path.as_posix(), 0) + 1
        if unexplained or (r.returncode != 0 and not any(errors[d] for d in group)):
            print(output, file=sys.stderr)
            raise SystemExit(f"compilation failed outside the scenario-B packages of {group}")
    return {d: dict(sorted(e.items())) for d, e in errors.items()}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--runs", type=int, default=2000, help="runs per profile and crash setting (default 2000)")
    ap.add_argument("--sweep-seeds", type=int, default=500, help="seeds of the single-crash sweep (default 500)")
    ap.add_argument("--work", type=Path, default=None, help="directory for the temporary copies (default: the system's)")
    ap.add_argument("--keep", action="store_true", help="keep the temporary copies")
    ap.add_argument("--only", nargs="*", default=None, help="change requests to verify (default: all)")
    ap.add_argument("--gradle-arg", action="append", default=[], help="extra argument for gradlew, repeatable")
    opts = ap.parse_args()

    crs = [k for k, _ in CHANGE_REQUESTS if opts.only is None or k in opts.only]
    work = Path(tempfile.mkdtemp(prefix="ballarch-changes-", dir=opts.work))
    inside = subprocess.run(["git", "rev-parse", "--is-inside-work-tree"], cwd=work, capture_output=True, text=True)
    if inside.returncode == 0 and inside.stdout.strip() == "true":
        print(f"{work} is inside a git work tree; git apply would resolve paths against it. Use --work elsewhere.", file=sys.stderr)
        return 2
    packages = scenario_b_packages()
    entries: dict[str, dict] = {}
    try:
        base = work / "unpatched"
        copy_sources(base)
        before = run_changes(base, work / "before.json", opts.runs, opts.sweep_seeds, crs, opts.gradle_arg)["results"]
        for cr in crs:
            root = work / cr
            copy_sources(root)
            applied = []
            for design in DESIGNS:
                patch = HERE / cr / f"{design}.patch"
                if not patch.exists():
                    continue
                subprocess.run(["git", "apply", "--check", str(patch)], cwd=root, check=True)
                subprocess.run(["git", "apply", str(patch)], cwd=root, check=True)
                applied.append(design)
            broken = variant_compile_errors(root, applied, packages, opts.gradle_arg)
            for d in applied:
                for pkg in packages.get(d, []):
                    shutil.rmtree(root / pkg, ignore_errors=True)
            after = run_changes(root, work / f"{cr}.json", opts.runs, opts.sweep_seeds, [cr], opts.gradle_arg)["results"][cr]
            entries[cr] = {
                "runsPerCell": opts.runs,
                "sweepSeeds": opts.sweep_seeds,
                "before": before[cr],
                "after": {d: r for d, r in after.items() if d in applied},
                "scenarioBCompileErrors": broken,
            }
    finally:
        if not opts.keep:
            shutil.rmtree(work, ignore_errors=True)
        else:
            print(f"kept {work}")

    data = json.loads(RESULTS.read_text(encoding="utf-8")) if RESULTS.exists() else {}
    previous = data.get("verification", {}).get("changeRequests", {})
    merged = {**previous, **entries}
    data["verification"] = {"changeRequests": {k: merged[k] for k, _ in CHANGE_REQUESTS if k in merged}}
    RESULTS.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
    failed = [
        f"{cr} {design} {c['profile']} crash={c['crashes']}"
        for cr, e in entries.items() for design, r in e["after"].items() if design not in NAIVE
        for c in r["cells"] if c["profile"] != "BeyondHorizon" and c["violating"]
    ] + [
        f"{cr} {design} sweep {s['profile']}"
        for cr, e in entries.items() for design, r in e["after"].items() if design not in NAIVE
        for s in r["sweeps"] if s["violating"]
    ]
    for f in failed:
        print("violation:", f, file=sys.stderr)
    print(f"wrote {RESULTS.relative_to(REPO)}; reconciling designs with a violation while the horizon holds: {len(failed)}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
