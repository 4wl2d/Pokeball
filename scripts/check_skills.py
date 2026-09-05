#!/usr/bin/env python3
"""Check compact skill structure and source integrity, not instruction semantics."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SKILL_NAMES = {"pokeball", "pokeball-async", "pokeball-binding", "pokeball-composition", "pokeball-review"}
PACKAGE_FILES = {"SKILL.md", "LICENSE", "NOTICE.md"}
WORD_LIMIT = 800
READING_REFERENCE = re.compile(
    r"\]\s*[\[(]|^\s*\[[^]\n]+\]:|\b(?:href|src)\s*=|\b[a-z][a-z0-9+.-]*://"
    r"|\b(?:references|scripts|spec|skill-src)[/\\]|\.\.[/\\]|\bSKILL\.md\b"
    r"|§\s*\d|\bPBA-\d|\bPKB-AR-|<!--\s*pkb:|skill_context\.py|build_skills\.py",
    re.I | re.M,
)


def scalar(value: str) -> str:
    value = value.strip()
    if value.startswith('"'):
        value = json.loads(value)
        if not isinstance(value, str):
            raise ValueError("frontmatter values must be strings")
    elif value.startswith("'"):
        if not value.endswith("'") or len(value) < 2:
            raise ValueError("unclosed frontmatter string")
        value = value[1:-1].replace("''", "'")
    elif not value or value[0] in "|>[{&*!" or re.search(r":\s|\s#", value):
        raise ValueError("use single-line frontmatter strings")
    return value


def frontmatter(source: str) -> dict[str, str]:
    lines = source.splitlines()
    if not lines or lines[0] != "---" or "---" not in lines[1:]:
        raise ValueError("missing or unclosed frontmatter")
    fields = {}
    for line in lines[1:lines.index("---", 1)]:
        if not line.strip():
            continue
        key, separator, value = line.partition(":")
        if not separator or key not in {"name", "description", "license", "compatibility"} or key in fields:
            raise ValueError("unsupported or duplicate frontmatter field")
        fields[key] = scalar(value)
    return fields


def check_package(path: Path) -> list[str]:
    """Inspect one copied directory without opening any file outside it."""
    try:
        if path.is_symlink() or not path.is_dir():
            return ["skill must be an ordinary directory"]
        if {item.name for item in path.iterdir()} != PACKAGE_FILES:
            return ["skill must contain only SKILL.md, LICENSE, and NOTICE.md"]
        if any((path / name).is_symlink() or not (path / name).is_file() for name in PACKAGE_FILES):
            return ["package files must be ordinary files, not links or directories"]
        source = (path / "SKILL.md").read_text(encoding="utf-8")
        fields = frontmatter(source)
        errors = []
        if fields.get("name") != path.name or not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", path.name):
            errors.append("name must match its lowercase hyphenated directory")
        if len(path.name) > 64 or not 30 <= len(fields.get("description", "")) <= 600:
            errors.append("use a name of at most 64 characters and a description of 30–600 characters")
        if len(source.split()) > WORD_LIMIT:
            errors.append(f"SKILL.md exceeds {WORD_LIMIT} words")
        if READING_REFERENCE.search(source):
            errors.append("SKILL.md must contain direct instructions without source-reading references")
        return errors
    except (OSError, UnicodeError, ValueError) as error:
        return [str(error)]


def verify_sources(root: Path) -> list[str]:
    """Verify the existing ordered Core and Agent Pack byte declarations."""
    root = root.resolve()
    try:
        def local(relative: str) -> Path:
            path = (root / relative).resolve()
            if not path.is_relative_to(root):
                raise ValueError("source path leaves the repository")
            return path
        baseline = local("docs/agents/BASELINE.md").read_text(encoding="utf-8")
        def block(name: str) -> dict[str, str]:
            text = re.search(rf"^{name}:\n((?:  [^\n]*\n)+)", baseline, re.M)
            if text is None:
                raise ValueError(f"missing {name} metadata")
            return dict(re.findall(r"^  (\w+): (.+)$", text[1], re.M))
        core, pack = block("canonicalCore"), block("packageIntegrity")
        entrypoint = local(core["entrypointFromRepositoryRoot"])
        source = entrypoint.read_text(encoding="utf-8")
        manifest = source.split("## Canonical document set", 1)[1].split("## Section index", 1)[0]
        targets = re.findall(r"^- \[[^]]+\]\(([^)]+\.md)\)$", manifest, re.M)
        paths = [local(str(entrypoint.parent / target)) for target in targets]
        if not paths or paths[0] != entrypoint or len(paths) != len(set(paths)):
            raise ValueError("Core manifest must start with its entrypoint and contain unique files")
        def digest(files, names):
            return hashlib.sha256(b"".join(name.encode() + b"\0" + file.read_bytes() + b"\0"
                                          for file, name in zip(files, names))).hexdigest()
        actual = (str(len(paths)), str(sum(path.stat().st_size for path in paths)),
                  digest(paths, [p.relative_to(root).as_posix() for p in paths]))
        if actual != (core["fileCountIncludingEntrypoint"], core["bytes"], core["sha256"]):
            raise ValueError("Core count, bytes, or digest differs from BASELINE.md")
        for label, key in (("Version", "declaredVersion"), ("Status", "declaredStatus")):
            header = re.search(rf"^\*\*{label}:\*\*\s*`?([^`<\n]+)", source, re.M)
            if not header or header[1].strip() != core[key]:
                raise ValueError(f"Core {label.lower()} differs from BASELINE.md")
        files = sorted(local("docs/agents").glob("*.md"))
        payload = [p for p in files if p.name != "BASELINE.md"]
        if str(len(files)) != pack["fileCountIncludingBaseline"] or digest(payload, [p.name for p in payload]) != pack["sha256"]:
            raise ValueError("Agent Pack count or digest differs from BASELINE.md")
        return []
    except (OSError, UnicodeError, ValueError, KeyError, IndexError) as error:
        return [f"source integrity: {error}"]


def check(root: Path) -> tuple[list[str], dict[str, int]]:
    errors = verify_sources(root)
    directory = root / "skills"
    if directory.is_symlink() or not directory.is_dir():
        return errors + ["missing ordinary skills directory"], {"skills": 0}
    names = {item.name for item in directory.iterdir()}
    if names != SKILL_NAMES:
        errors.append("skills must contain exactly the five maintained skill directories")
    for name in sorted(SKILL_NAMES):
        errors.extend(f"{name}: {error}" for error in check_package(directory / name))
    return errors, {"skills": len(names & SKILL_NAMES)}


if __name__ == "__main__":
    failures, counts = check(ROOT)
    for failure in failures:
        print(f"ERROR: {failure}", file=sys.stderr)
    if not failures:
        print(f"Passed: {counts['skills']} compact skills; Core/Agent integrity. Semantics require review.")
    raise SystemExit(bool(failures))
