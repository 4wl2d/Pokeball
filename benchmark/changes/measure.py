#!/usr/bin/env python3
"""Measures the change-request patches in benchmark/changes.

For each patch `<change request>/<design>.patch` (a unified diff relative to
the repository root, against the scenario-A code of the design in this
checkout) it reports:

- the files touched: changed, added or deleted;
- the top-level units touched: a top-level class, interface or object with
  everything nested in it, or the top-level functions and properties of a
  file, which count as one unit (the class the Kotlin compiler generates for
  them, `<File>Kt`); a unit is touched when a code line inside it is added,
  removed or changed;
- code lines (logical lines) added, removed and changed. A logical line is a
  statement, a declaration header or a `when` branch, however it is wrapped:
  a physical line is joined to the one before it while a `(` or `[` is open,
  when it starts with `.`, `?.`, `?:`, `&&`, `||`, `)`, `]`, `as` or an
  `else` that is not a `when` branch, when the line before ends with a binary
  operator, `=`, `,`, `:`, an infix `to`/`and`/`or`/... or a `->` that does
  not end a lambda's parameters, when it holds only closing brackets, when
  it is the `get()`/`set()` of the property declared on the line before, and
  when the line before holds only annotations. Logical lines are compared as
  token sequences (comments, whitespace and a trailing comma before a closing
  bracket ignored), so the count does not depend on line wrapping;
- physical code lines added, removed and changed: lines that hold code,
  compared without indentation, as in RESULTS.md section 9;
- tokens added and removed: the code of each file as a sequence of tokens
  (identifiers, keywords, literals, operators and brackets; every string
  literal one token), which depends on no formatting at all;
- import lines added or removed.

In all three line and token counts, `package` and `import` lines are ignored,
files are compared one by one, and `difflib` pairs old and new; in a replaced
block of lines, the first min(old, new) lines count as changed and the rest as
added or removed. A physical line holds code when it is not blank and not only
a comment; report.py's scanner decides this, so string literals and nested
comments are handled as for the SLOC metric.

`git apply --numstat` (physical lines added and deleted per file) is recorded
for each patch.

Formatting rule of the patches, checked by --check: no line a patch adds is
longer than LIMIT characters, the longest line in the nine designs' existing
code (scenario-B and toolkit packages included; the check fails if that
maximum changes), and a statement that was on one line and that the patch
changes stays on one line if it fits within LIMIT. New code follows the
formatting of the parallel existing code. Only the physical line count depends
on this rule.

Writes the "patches" part of benchmark/results/changes.json and keeps the
"verification" part, which verify.py writes. With --check it writes nothing
and fails if a patch does not apply to this checkout (`git apply --check`), if
its `git apply --numstat` or its numbers differ from the recorded ones, or if
it breaks the formatting rule.

Needs only the Python standard library (and git for --check).
Usage: python3 benchmark/changes/measure.py [--check]
"""
from __future__ import annotations

import difflib
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
BENCH = HERE.parent
REPO = BENCH.parent
RESULTS = BENCH / "results" / "changes.json"

sys.path.insert(0, str(BENCH))
from report import scan  # noqa: E402  (the SLOC scanner of report.py)

CHANGE_REQUESTS = [
    ("cr1-refund", "Cancel a completed order within a window, and refund it"),
    ("cr2-loyalty", "Award loyalty points once per completed order"),
    ("cr3-failover", "A second payment provider with failover"),
    ("cr4-no-inventory", "Remove the inventory"),
]
DESIGNS = ["layered", "hardened", "actors", "actors-careful", "mvu", "mvu-careful", "saga", "saga-careful", "ballarch"]

HUNK = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")
DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|protected|sealed|data|enum|abstract|open|annotation|value|inline|inner|expect|actual|const|"
    r"operator|infix|tailrec|suspend|external|lateinit)\s+)*(?:fun\s+interface\s+(\w+)|(?:class|interface|object)\s+(\w+)|(fun|val|var|typealias)\b)"
)


class FilePatch:
    def __init__(self, old: str | None, new: str | None):
        self.old = old
        self.new = new
        self.hunks: list[tuple[int, int, int, int, list[str]]] = []

    @property
    def path(self) -> str:
        return self.new or self.old or ""


def parse(text: str) -> list[FilePatch]:
    """The files of a `git diff` patch, with their hunks."""
    files: list[FilePatch] = []
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("--- "):
            old = line[4:]
            new = lines[i + 1][4:]
            files.append(FilePatch(None if old == "/dev/null" else old[2:], None if new == "/dev/null" else new[2:]))
            i += 2
            continue
        m = HUNK.match(line)
        if m:
            os_, ol, ns, nl = int(m[1]), int(m[2] or 1), int(m[3]), int(m[4] or 1)
            body: list[str] = []
            i += 1
            o = n = 0
            while i < len(lines) and (o < ol or n < nl):
                b = lines[i]
                if b.startswith("\\"):
                    i += 1
                    continue
                body.append(b)
                if b.startswith("-"):
                    o += 1
                elif b.startswith("+"):
                    n += 1
                else:
                    o += 1
                    n += 1
                i += 1
            files[-1].hunks.append((os_, ol, ns, nl, body))
            continue
        i += 1
    return files


def apply(old_text: str, fp: FilePatch) -> str:
    """Applies the hunks of [fp] to [old_text] at their stated positions; they must match exactly."""
    old = old_text.split("\n") if old_text else []
    out: list[str] = []
    pos = 0
    for os_, ol, _, _, body in fp.hunks:
        start = os_ - 1 if ol > 0 else os_
        out.extend(old[pos:start])
        pos = start
        for b in body:
            if b.startswith("+"):
                out.append(b[1:])
            else:
                expected = b[1:]
                if pos >= len(old) or old[pos] != expected:
                    raise ValueError(f"{fp.path}: hunk does not match line {pos + 1}")
                if b.startswith(" "):
                    out.append(expected)
                pos += 1
    out.extend(old[pos:])
    return "\n".join(out)


def units(text: str, file_name: str) -> list[str | None]:
    """The top-level unit of every line: a top-level type's name, the file facade `<File>Kt`, or None before the first declaration."""
    facade = file_name.removesuffix(".kt") + "Kt"
    lines = text.split("\n")
    code, _ = scan(text)
    result: list[str | None] = [None] * len(lines)
    current: str | None = None
    pending_annotation: list[int] = []
    for i, line in enumerate(lines):
        if line and not line[0].isspace() and i in code:
            if line.startswith("@"):
                pending_annotation.append(i)
                continue
            m = DECLARATION.match(line)
            if m:
                current = m[1] or m[2] or facade
                for a in pending_annotation:
                    result[a] = current
            pending_annotation = []
        result[i] = current if result[i] is None else result[i]
    return result


def directive_lines(text: str) -> set[int]:
    """The 0-based numbers of the `package` and `import` lines."""
    return {i for i, l in enumerate(text.split("\n")) if l.strip().startswith(("package ", "import "))}


def code_lines(text: str) -> list[tuple[int, str]]:
    """(index, stripped line) for the physical lines that hold code, without `package` and `import` lines."""
    lines = text.split("\n")
    skip = directive_lines(text)
    return [(i, lines[i].strip()) for i in sorted(scan(text)[0]) if i not in skip]


def import_lines(text: str) -> list[str]:
    return [l.strip() for l in text.split("\n") if l.strip().startswith("import ")]


# Kotlin tokens. Longer operators first, so that `?.` is not read as `?` and `.`.
OPERATORS = sorted("""?. ?: :: ..< .. -> && || === !== == != <= >= ++ -- += -= *= /= %= !! as?
- + * / % = < > ! ? : . , ; @ # & | ( ) { } [ ] ^ ~ $""".split(), key=len, reverse=True)
IDENTIFIER = re.compile(r"`[^`\n]*`|[A-Za-z_][A-Za-z_0-9]*")
NUMBER = re.compile(r"0[xX][0-9a-fA-F_]+[uUL]*|0[bB][01_]+[uUL]*|(?:\d[\d_]*)?\.\d[\d_]*(?:[eE][+-]?\d+)?[fF]?|\d[\d_]*(?:[eE][+-]?\d+)?[fFuUL]*")


def tokenize(text: str) -> list[tuple[str, int]]:
    """(token, 0-based line) for the code of [text]: comments are dropped and every string or character literal,
    templates included, is one token."""
    toks: list[tuple[str, int]] = []
    n = len(text)

    def literal_end(j: int) -> int:
        if text.startswith('"""', j):
            k = text.find('"""', j + 3)
            k = n if k < 0 else k + 3
            while k < n and text[k] == '"':
                k += 1
            return k
        quote = text[j]
        k = j + 1
        while k < n:
            if text[k] == "\\":
                k += 2
            elif quote == '"' and text.startswith("${", k):
                k = template_end(k + 2)
            elif text[k] == quote:
                return k + 1
            else:
                k += 1
        return k

    def template_end(j: int) -> int:
        depth = 1
        while j < n:
            c = text[j]
            if c in "\"'":
                j = literal_end(j)
                continue
            depth += 1 if c == "{" else -1 if c == "}" else 0
            j += 1
            if depth == 0:
                return j
        return j

    i, line = 0, 0
    while i < n:
        c = text[i]
        if c.isspace():
            line += c == "\n"
            i += 1
        elif text.startswith("//", i):
            while i < n and text[i] != "\n":
                i += 1
        elif text.startswith("/*", i):
            depth = 0
            while i < n:
                if text.startswith("/*", i):
                    depth, i = depth + 1, i + 2
                elif text.startswith("*/", i):
                    depth, i = depth - 1, i + 2
                    if depth == 0:
                        break
                else:
                    line += text[i] == "\n"
                    i += 1
        elif c in "\"'":
            j = literal_end(i)
            toks.append((text[i:j], line))
            line += text.count("\n", i, j)
            i = j
        else:
            m = NUMBER.match(text, i) if c.isdigit() or (c == "." and text[i + 1:i + 2].isdigit()) else IDENTIFIER.match(text, i)
            tok = m.group() if m and m.end() > i else next((op for op in OPERATORS if text.startswith(op, i)), c)
            toks.append((tok, line))
            i += len(tok)
    return toks


def code_tokens(text: str) -> list[tuple[str, int]]:
    skip = directive_lines(text)
    return [(t, l) for t, l in tokenize(text) if l not in skip]


# Rules that join a physical line to the logical line before it (see the module's docstring).
JOIN_AFTER = {"=", "+=", "-=", "*=", "/=", "%=", "==", "!=", "===", "!==", "<=", ">=", "&&", "||", "?:", "+", "-", "*", "/", "%",
              "..", "..<", ".", "?.", ",", "->", ":", "::"}
INFIX = {"to", "and", "or", "xor", "until", "downTo", "step", "shl", "shr", "ushr", "in", "is", "as", "zip"}
JOIN_BEFORE = {".", "?.", "?:", "&&", "||", ")", "]", "else", "as", "as?", "::"}
OPENING = {"(", "[", "{"}
CLOSING = {")", "]", "}"}
MODIFIERS = {"private", "public", "internal", "protected", "override", "open", "abstract", "final", "lateinit", "const", "inline"}


def only_annotations(ts: list[str]) -> bool:
    k = 0
    while k < len(ts) and ts[k] == "@":
        k += 1
        while k < len(ts) and IDENTIFIER.fullmatch(ts[k]):
            k += 1
            if k < len(ts) and ts[k] == ".":
                k += 1
            else:
                break
        if k < len(ts) and ts[k] == "(":
            depth = 0
            while k < len(ts):
                depth += 1 if ts[k] == "(" else -1 if ts[k] == ")" else 0
                k += 1
                if depth == 0:
                    break
    return k > 0 and k == len(ts)


def declares_property(ts: list[str]) -> bool:
    k = 0
    while k < len(ts) and ts[k] in MODIFIERS:
        k += 1
    return k < len(ts) and ts[k] in ("val", "var")


def logical_lines(text: str) -> list[tuple[list[int], str]]:
    """The logical lines of [text], without `package` and `import` lines: the physical lines each spans (closing-bracket
    lines left out) and its normalized text (tokens joined by spaces, a trailing comma before a closing bracket dropped)."""
    toks = code_tokens(text)
    by_line: dict[int, list[str]] = {}
    for t, l in toks:
        by_line.setdefault(l, []).append(t)
    out: list[tuple[list[int], list[str]]] = []
    stack: list[tuple[str, int]] = []  # open brackets and the line each was opened on
    previous_line = -1
    for t, l in toks:
        if l != previous_line:
            here = by_line[l]
            join = False
            if out:
                prev = out[-1][1]
                last = prev[-1]
                if stack and stack[-1][0] in "([":
                    join = True
                elif t in JOIN_BEFORE and not (t == "else" and here[1:2] == ["->"]):
                    join = True
                elif all(x in CLOSING or x in (",", ";") for x in here):
                    join = True
                elif t in ("get", "set") and here[1:2] == ["("] and declares_property(prev):
                    join = True
                elif last in JOIN_AFTER:
                    # `->` after a `{` opened on the same line ends a lambda's parameters.
                    join = not (last == "->" and stack and stack[-1] == ("{", previous_line))
                elif last in INFIX and len(prev) > 1 and prev[-2] not in ("fun", ".", "?.", "val", "var"):
                    join = True
                elif only_annotations(prev):
                    join = True
            if join:
                if not all(x in CLOSING or x in (",", ";") for x in here):
                    out[-1][0].append(l)
            else:
                out.append(([l], []))
            previous_line = l
        out[-1][1].append(t)
        if t in OPENING:
            stack.append((t, l))
        elif t in CLOSING and stack:
            stack.pop()
    result = []
    for lines, ts in out:
        kept = [x for k, x in enumerate(ts) if not (x == "," and k + 1 < len(ts) and ts[k + 1] in (")", "]"))]
        result.append((lines, " ".join(kept)))
    return result


def diff(old: list[str], new: list[str]) -> list[tuple[str, int, int, int, int]]:
    return [op for op in difflib.SequenceMatcher(None, old, new, autojunk=False).get_opcodes() if op[0] != "equal"]


def counts(ops: list[tuple[str, int, int, int, int]]) -> dict:
    added = removed = changed = 0
    for _, i1, i2, j1, j2 in ops:
        r, a = i2 - i1, j2 - j1
        c = min(r, a)
        changed, added, removed = changed + c, added + a - c, removed + r - c
    return {"added": added, "removed": removed, "changed": changed}


def measure(patch: Path) -> dict:
    text = patch.read_text(encoding="utf-8")
    files = parse(text)
    logical = {"added": 0, "removed": 0, "changed": 0}
    physical = {"added": 0, "removed": 0, "changed": 0}
    tokens = {"added": 0, "removed": 0}
    imports = 0
    touched: set[str] = set()
    old_units_all: set[str] = set()
    new_units_all: set[str] = set()
    file_rows = []
    for fp in files:
        old_text = (REPO / fp.old).read_text(encoding="utf-8") if fp.old else ""
        new_text = apply(old_text, fp)
        name = Path(fp.path).name
        status = "added" if fp.old is None else "deleted" if fp.new is None else "changed"
        plus = sum(1 for _, _, _, _, b in fp.hunks for l in b if l.startswith("+"))
        minus = sum(1 for _, _, _, _, b in fp.hunks for l in b if l.startswith("-"))
        file_rows.append({"path": fp.path, "status": status, "added": plus, "deleted": minus})
        old_u = units(old_text, name) if fp.old else []
        new_u = units(new_text, name) if fp.new else []
        old_units_all |= {f"{name}: {u}" for u in old_u if u}
        new_units_all |= {f"{name}: {u}" for u in new_u if u}

        old_l = logical_lines(old_text) if fp.old else []
        new_l = logical_lines(new_text) if fp.new else []
        ops = diff([s for _, s in old_l], [s for _, s in new_l])
        for k, v in counts(ops).items():
            logical[k] += v
        for _, i1, i2, j1, j2 in ops:
            touched |= {f"{name}: {old_u[old_l[k][0][0]]}" for k in range(i1, i2) if old_u[old_l[k][0][0]]}
            touched |= {f"{name}: {new_u[new_l[k][0][0]]}" for k in range(j1, j2) if new_u[new_l[k][0][0]]}

        old_c = code_lines(old_text) if fp.old else []
        new_c = code_lines(new_text) if fp.new else []
        for k, v in counts(diff([l for _, l in old_c], [l for _, l in new_c])).items():
            physical[k] += v

        old_t = [t for t, _ in code_tokens(old_text)] if fp.old else []
        new_t = [t for t, _ in code_tokens(new_text)] if fp.new else []
        for _, i1, i2, j1, j2 in diff(old_t, new_t):
            tokens["removed"] += i2 - i1
            tokens["added"] += j2 - j1

        ia = import_lines(old_text) if fp.old else []
        ib = import_lines(new_text) if fp.new else []
        imports += len(set(ia) ^ set(ib))
    return {
        "files": file_rows,
        "filesTouched": len(files),
        "filesAdded": sum(1 for f in file_rows if f["status"] == "added"),
        "filesDeleted": sum(1 for f in file_rows if f["status"] == "deleted"),
        "units": sorted(touched),
        "unitsTouched": len(touched),
        "unitsAdded": len(touched & (new_units_all - old_units_all)),
        "unitsRemoved": len(touched & (old_units_all - new_units_all)),
        "codeLines": logical,
        "physicalLines": physical,
        "tokens": tokens,
        "importLinesChanged": imports,
    }


# The formatting rule (see the module's docstring).
LIMIT = 168


def longest_existing_line() -> tuple[int, str]:
    best = (0, "")
    for design in DESIGNS:
        for f in sorted((BENCH / design / "src" / "main" / "kotlin").rglob("*.kt")):
            for i, line in enumerate(f.read_text(encoding="utf-8").split("\n")):
                if len(line) > best[0]:
                    best = (len(line), f"{f.relative_to(REPO).as_posix()}:{i + 1}")
    return best


def joined(lines: list[str]) -> str:
    """[lines] as one line, with the indentation of the first."""
    s = " ".join(l.strip() for l in lines)
    for a, b in (("( ", "("), ("[ ", "["), (" )", ")"), (" ]", "]"), (" .", "."), (" ?.", "?."), (",)", ")"), (",]", "]")):
        s = s.replace(a, b)
    return lines[0][: len(lines[0]) - len(lines[0].lstrip())] + s


def formatting_problems(patch: Path) -> list[str]:
    problems = []
    for fp in parse(patch.read_text(encoding="utf-8")):
        if fp.new is None:
            continue
        old_text = (REPO / fp.old).read_text(encoding="utf-8") if fp.old else ""
        new_text = apply(old_text, fp)
        new_lines = new_text.split("\n")
        for _, _, ns, nl, body in fp.hunks:
            n = ns - 1 if nl > 0 else ns
            for b in body:
                if b.startswith("+") and len(b) - 1 > LIMIT:
                    problems.append(f"{fp.path}:{n + 1}: {len(b) - 1} characters, more than {LIMIT}")
                n += not b.startswith("-")
        if fp.old is None:
            continue
        old_l, new_l = logical_lines(old_text), logical_lines(new_text)
        for tag, i1, i2, j1, j2 in diff([s for _, s in old_l], [s for _, s in new_l]):
            for j in range(j1, j2) if tag == "replace" else ():
                span = new_l[j][0]
                if len(span) < 2 or len(joined([new_lines[l] for l in span])) > LIMIT:
                    continue
                if any(len(old_l[i][0]) == 1 and difflib.SequenceMatcher(None, old_l[i][1], new_l[j][1]).ratio() >= 0.6 for i in range(i1, i2)):
                    problems.append(f"{fp.path}:{span[0] + 1}: a one-line statement is wrapped although it fits within {LIMIT} characters")
    return problems


def git_numstat(patch: Path) -> list[dict]:
    out = subprocess.run(["git", "apply", "--numstat", str(patch)], cwd=REPO, check=True, capture_output=True, text=True).stdout
    rows = []
    for line in out.strip().split("\n"):
        a, d, path = line.split("\t")
        rows.append({"path": path, "added": int(a), "deleted": int(d)})
    return rows


def patches() -> list[tuple[str, str, Path]]:
    found = []
    for cr, _ in CHANGE_REQUESTS:
        for design in DESIGNS:
            p = HERE / cr / f"{design}.patch"
            if p.exists():
                found.append((cr, design, p))
    return found


def compute() -> list[dict]:
    rows = []
    for cr, design, p in patches():
        rows.append({"changeRequest": cr, "design": design, "patch": p.relative_to(BENCH).as_posix(), **measure(p)})
    return rows


def main() -> int:
    check = "--check" in sys.argv[1:]
    data = json.loads(RESULTS.read_text(encoding="utf-8")) if RESULTS.exists() else {}
    rows = compute()
    if check:
        failures = []
        for cr, design, p in patches():
            r = subprocess.run(["git", "apply", "--check", str(p)], cwd=REPO, capture_output=True, text=True)
            if r.returncode != 0:
                failures.append(f"{p.relative_to(REPO)} does not apply: {r.stderr.strip()}")
                continue
            ours = next(x for x in rows if x["changeRequest"] == cr and x["design"] == design)
            numstat = [{"path": f["path"], "added": f["added"], "deleted": f["deleted"]} for f in ours["files"]]
            if git_numstat(p) != numstat:
                failures.append(f"{p.relative_to(REPO)}: git apply --numstat differs from the patch's own count")
            failures.extend(f"{p.relative_to(REPO)}: {problem}" for problem in formatting_problems(p))
        longest = longest_existing_line()
        if longest[0] != LIMIT:
            failures.append(f"the longest line in the designs' code is {longest[0]} characters ({longest[1]}), not LIMIT = {LIMIT}")
        if data.get("patches") != rows:
            failures.append("benchmark/results/changes.json is out of date: run python3 benchmark/changes/measure.py")
        for f in failures:
            print(f, file=sys.stderr)
        print(f"checked {len(rows)} patches: {'FAILED' if failures else 'OK'}")
        return 1 if failures else 0
    out = {
        "changeRequests": [{"key": k, "title": t} for k, t in CHANGE_REQUESTS],
        "designs": DESIGNS,
        "patches": rows,
    }
    if "verification" in data:
        out["verification"] = data["verification"]
    RESULTS.write_text(json.dumps(out, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {RESULTS.relative_to(REPO)} ({len(rows)} patches)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
