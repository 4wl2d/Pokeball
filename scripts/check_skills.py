#!/usr/bin/env python3
"""Check the skills' shape, reading budget, local links and source byte integrity.

This bounded mechanical check does not establish semantic fidelity, runtime
conformance, publication provenance, or complete Markdown/GitHub compatibility.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlsplit

from skill_context import ContextError, ROOT, local_path, markdown_index, visible_lines


SKILL_WORD_LIMIT = 800
REFERENCE_WORD_LIMIT = 1600
REFERENCE_TOTAL_LIMIT = 8000


def scalar(value: str) -> str:
    value = value.strip()
    if value.startswith('"'):
        result = json.loads(value)
        if not isinstance(result, str):
            raise ValueError("expected a string")
        return result
    if value.startswith("'"):
        if not value.endswith("'") or len(value) < 2:
            raise ValueError("unclosed quoted string")
        return value[1:-1].replace("''", "'")
    if not value or value[0] in "|>[{&*!" or re.search(r":\s|\s#", value):
        raise ValueError("use a plain single-line string or a quoted string")
    return value


def frontmatter(source: str) -> dict[str, str]:
    lines = source.splitlines()
    if not lines or lines[0] != "---":
        raise ValueError("missing YAML frontmatter")
    try:
        end = lines.index("---", 1)
    except ValueError as error:
        raise ValueError("unclosed YAML frontmatter") from error
    result: dict[str, str] = {}
    for line in lines[1:end]:
        if not line.strip():
            continue
        match = re.fullmatch(r"([a-z][a-z-]*):\s*(.*)", line)
        if not match or match[1] not in {"name", "description", "license", "compatibility", "allowed-tools"}:
            raise ValueError("use supported single-line frontmatter fields; UI metadata belongs in agents/openai.yaml")
        if match[1] in result:
            raise ValueError(f"duplicate frontmatter field: {match[1]}")
        result[match[1]] = scalar(match[2])
    return result


def link_targets(source: str) -> list[str]:
    visible = "".join(visible_lines(source))
    visible = re.sub(r"(`+).*?\1", "", visible)
    targets = re.findall(r"!?\[[^]\n]*\]\(\s*(<[^>\n]+>|[^\s)]+)(?:\s+[\"'][^\n]*?[\"'])?\s*\)", visible)
    targets += re.findall(r"^ {0,3}\[[^]\n]+\]:\s*(<[^>\n]+>|\S+)", visible, re.M)
    targets += [match[1] for match in re.findall(r"\b(?:href|src)\s*=\s*([\"'])(.*?)\1", visible, re.I)]
    return [target[1:-1] if target.startswith("<") else target for target in targets]


def check_links(root: Path, path: Path) -> list[str]:
    errors: list[str] = []
    relative = path.relative_to(root)
    for target in link_targets(path.read_text(encoding="utf-8")):
        parsed = urlsplit(target)
        if parsed.scheme or parsed.netloc:
            continue
        try:
            destination = local_path(root, parsed.path, path.parent) if parsed.path else path
            if parsed.fragment:
                if destination.suffix.lower() != ".md":
                    raise ContextError("local fragments must target Markdown")
                _, anchors = markdown_index(destination.read_text(encoding="utf-8"))
                if unquote(parsed.fragment) not in anchors:
                    raise ContextError(f"unknown fragment: {parsed.fragment}")
        except (ContextError, OSError, UnicodeError) as error:
            errors.append(f"{relative}: broken local link {target!r}: {error}")
    return errors


def baseline_block(source: str, name: str) -> dict[str, str]:
    match = re.search(rf"^{re.escape(name)}:\n((?:[ \t]+[^\n]*\n)+)", source, re.M)
    if not match:
        raise ValueError(f"BASELINE lacks {name}")
    return {key: scalar(value) for key, value in re.findall(r"^  ([\w]+):\s*(.+)$", match[1], re.M)}


def verify_sources(root: Path) -> list[str]:
    root = root.resolve()
    errors: list[str] = []
    try:
        baseline_path = local_path(root, "docs/agents/BASELINE.md")
        baseline = baseline_path.read_text(encoding="utf-8")
        core = baseline_block(baseline, "canonicalCore")
        package = baseline_block(baseline, "packageIntegrity")
        entrypoint = local_path(root, core["entrypointFromRepositoryRoot"])
        source = entrypoint.read_text(encoding="utf-8")
        manifest = source.split("## Canonical document set", 1)[1].split("## Section index", 1)[0]
        targets = re.findall(r"^- \[[^]]+\]\(([^)]+\.md)\)$", manifest, re.M)
        paths = [local_path(root, target, entrypoint.parent) for target in targets]
        if not paths or paths[0] != entrypoint or len(paths) != len(set(paths)):
            errors.append("Core manifest must begin with its entrypoint and contain no duplicate files")
        digest = hashlib.sha256()
        total = 0
        for path in paths:
            data = path.read_bytes()
            digest.update(path.relative_to(root).as_posix().encode() + b"\0" + data + b"\0")
            total += len(data)
        for label, actual, expected in (
            ("Core file count", str(len(paths)), core["fileCountIncludingEntrypoint"]),
            ("Core byte count", str(total), core["bytes"]),
            ("Core digest", digest.hexdigest(), core["sha256"]),
        ):
            if actual != expected:
                errors.append(f"{label} differs from BASELINE.md")
        for label, key in (("Version", "declaredVersion"), ("Status", "declaredStatus")):
            header = re.search(rf"^\*\*{label}:\*\*\s*`?([^`<\n]+)", source, re.M)
            if not header or header[1].strip() != core[key]:
                errors.append(f"Core {label.lower()} differs from BASELINE.md")
        paths = sorted(baseline_path.parent.glob("*.md"))
        digest = hashlib.sha256()
        for path in paths:
            if path.name != "BASELINE.md":
                digest.update(path.name.encode() + b"\0" + path.read_bytes() + b"\0")
        if str(len(paths)) != package["fileCountIncludingBaseline"]:
            errors.append("Agent Pack file count differs from BASELINE.md")
        if digest.hexdigest() != package["sha256"]:
            errors.append("Agent Pack digest differs from BASELINE.md")
    except (ContextError, OSError, UnicodeError, ValueError, KeyError, IndexError) as error:
        errors.append(f"source integrity: {error}")
    return errors


def long_paragraphs(source: str) -> set[str]:
    """Catch copied prose blocks; this is intentionally not a semantic detector."""
    paragraphs = {" ".join(paragraph.split()) for paragraph in re.split(r"\n\s*\n", source)}
    return {paragraph for paragraph in paragraphs if len(paragraph.split()) >= 40}


def check(root: Path) -> tuple[list[str], dict[str, int]]:
    root = root.resolve()
    errors = verify_sources(root)
    skills_root = root / "skills"
    skill_paths = sorted(skills_root.rglob("SKILL.md"))
    if not skill_paths:
        errors.append("no skills/*/SKILL.md files found")
    for path in skill_paths:
        if path.parent.parent != skills_root:
            errors.append(f"{path.relative_to(root)}: place the entrypoint at skills/<name>/SKILL.md")
        source = path.read_text(encoding="utf-8")
        try:
            fields = frontmatter(source)
            name = fields.get("name", "")
            if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", name) or len(name) > 64 or name != path.parent.name:
                errors.append(f"{path.relative_to(root)}: name must match its lowercase hyphenated folder (maximum 64 characters)")
            if not 30 <= len(fields.get("description", "")) <= 600:
                errors.append(f"{path.relative_to(root)}: description must be a discriminating 30–600 character string")
        except (ValueError, json.JSONDecodeError) as error:
            errors.append(f"{path.relative_to(root)}: {error}")
        if len(source.split()) > SKILL_WORD_LIMIT:
            errors.append(f"{path.relative_to(root)}: exceeds {SKILL_WORD_LIMIT} words")
    references = sorted(path for path in skills_root.rglob("*.md") if path.name != "SKILL.md")
    total = 0
    for path in references:
        words = len(path.read_text(encoding="utf-8").split())
        total += words
        if words > REFERENCE_WORD_LIMIT:
            errors.append(f"{path.relative_to(root)}: exceeds {REFERENCE_WORD_LIMIT} reference words")
    if total > REFERENCE_TOTAL_LIMIT:
        errors.append(f"skill references exceed {REFERENCE_TOTAL_LIMIT} total words")
    documents = sorted(skills_root.rglob("*.md"))
    for relative in ("docs/SKILLS.md", "docs/SKILL-AUTHORING.md"):
        path = root / relative
        if path.is_file():
            documents.append(path)
        else:
            errors.append(f"missing public skill document: {relative}")
    original_prose: set[str] = set()
    for path in sorted((root / "spec").rglob("*.md")) + [root / "docs/agents/AGENT-CONTRACT.md"]:
        if path.is_file():
            original_prose.update(long_paragraphs(path.read_text(encoding="utf-8")))
    for path in documents:
        source = path.read_text(encoding="utf-8")
        errors.extend(check_links(root, path))
        if path.is_relative_to(skills_root):
            if long_paragraphs(source) & original_prose:
                errors.append(f"{path.relative_to(root)}: copied source paragraph (40+ words); link to the original section")
            if re.search(r"<!--\s*pkb:(?:pba-source|term|generated):", source):
                errors.append(f"{path.relative_to(root)}: copied normative/generated source record; link to its owner")
            if re.search(r"^(?:\|\s*`?PKB-AR-[A-Z]+-\d+`?\s*\||#{1,6}\s+.*PKB-AR-[A-Z]+-\d+)", source, re.M):
                errors.append(f"{path.relative_to(root)}: PKB-AR definition row/heading outside AGENT-CONTRACT.md")
    return errors, {"skills": len(skill_paths), "documents": len(documents), "reference_words": total}


def main() -> int:
    try:
        errors, counts = check(ROOT)
    except (OSError, UnicodeError, ValueError) as error:
        print(f"check_skills: {error}", file=sys.stderr)
        return 1
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    if errors:
        return 1
    print(f"Skill checks passed: {counts['skills']} skills, {counts['documents']} linked documents, {counts['reference_words']} reference words; Core/Agent Pack integrity matches BASELINE.md.")
    print("Mechanical checks only; semantic fidelity and agent behavior require scoped review.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
