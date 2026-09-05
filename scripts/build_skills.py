#!/usr/bin/env python3
"""Build independent skill folders from maintained workflows and Core sections.

The default and --check are read-only. --write replaces generated artifacts only
under skills/<template-name>. This repository helper is not installed in a skill.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import os
from pathlib import Path, PurePosixPath
import re
import sys
from urllib.parse import unquote, urlsplit

from skill_context import ContextError, ROOT, local_path, markdown_index, section, visible_lines


NUMBER = re.compile(r"^(\d+(?:\.\d+)*)\.\s+")
INLINE_LINK = re.compile(r"(!?\[[^]\n]*\]\(\s*)(<[^>\n]+>|[^\s)]+)((?:\s+[\"'][^\n]*?[\"'])?\s*\))")
REFERENCE_LINK = re.compile(r"(^ {0,3}\[[^]\n]+\]:\s*)(<[^>\n]+>|\S+)", re.M)
HTML_LINK = re.compile(r"(\b(?:href|src)\s*=\s*)([\"'])(.*?)\2", re.I)
SECTION_REFERENCE = re.compile(r"§{1,2}\s*(\d+(?:\.\d+)*(?:\s*(?:[,/]|[–−-])\s*\d+(?:\.\d+)*)*)")
# These sections explicitly declare themselves routing/index material. Their
# Markdown links still resolve; their broad textual inventories do not force
# every indexed section into every installed workflow.
NAVIGATION_ONLY = {"0.4", "0.5"}
# The runtime concern index is an advertised operational router, not an optional
# reading overview. Its section ranges are required local destinations.
HARD_RANGE_SOURCES = {"8.12"}


class BuildError(ValueError):
    """A source cannot be packaged without silently losing a dependency."""


@dataclass(frozen=True)
class CoreSection:
    number: str
    path: Path
    fragment: str
    title: str
    content: str


def strip_frontmatter(source: str) -> str:
    if not source.startswith("---\n"):
        return source
    end = source.find("\n---\n", 4)
    if end < 0:
        raise BuildError("unclosed workflow frontmatter")
    return source[end + 5:].lstrip("\n")


def relative_link(origin: PurePosixPath, target: PurePosixPath, fragment: str = "") -> str:
    result = Path(os.path.relpath(str(target), str(origin.parent))).as_posix()
    return result + ("#" + fragment if fragment else "")


def core_index(root: Path) -> tuple[dict[str, CoreSection], dict[Path, tuple[list, dict]]]:
    root = root.resolve()
    entrypoint = root / "spec/pokeball-architecture-core.md"
    source = entrypoint.read_text(encoding="utf-8")
    try:
        manifest = source.split("## Canonical document set", 1)[1].split("## Section index", 1)[0]
    except IndexError as error:
        raise BuildError("Core entrypoint lacks its ordered manifest") from error
    targets = re.findall(r"^- \[[^]]+\]\(([^)]+\.md)\)$", manifest, re.M)
    if not targets:
        raise BuildError("empty Core manifest")
    result: dict[str, CoreSection] = {}
    indexes: dict[Path, tuple[list, dict]] = {}
    for target in targets:
        path = local_path(root, target, entrypoint.parent)
        text = path.read_bytes().decode("utf-8")
        headings, anchors = markdown_index(text)
        indexes[path] = (headings, anchors)
        for heading in headings:
            match = NUMBER.match(heading.title)
            if not match:
                continue
            number = match[1]
            if number.split(".")[0] in {"20", "22"}:
                continue
            if number in result:
                raise BuildError(f"duplicate Core section §{number}")
            _, content = section(root, path.relative_to(root).as_posix() + "#" + heading.fragment)
            result[number] = CoreSection(number, path, heading.fragment, heading.title, content)
    return result, indexes


class SkillBuilder:
    def __init__(self, root: Path, name: str, sections: dict[str, CoreSection], indexes: dict):
        self.root = root
        self.name = name
        self.template_root = root / "skill-src"
        self.source_skill = self.template_root / name / "SKILL.md"
        self.sections = sections
        self.indexes = indexes
        self.files: dict[PurePosixPath, bytes] = {}
        self.pending: list[tuple[Path, PurePosixPath, str | None]] = []
        self.queued: set[PurePosixPath] = set()
        self.origins: dict[PurePosixPath, tuple[Path, str | None]] = {}
        self.reports: set[str] = set()
        self.core_numbers: set[str] = set()

    def enqueue(self, source: Path, output: PurePosixPath, number: str | None = None):
        origin = (source, number)
        if output in self.origins and self.origins[output] != origin:
            raise BuildError(f"{self.name}: two source artifacts map to {output}")
        self.origins[output] = origin
        if output not in self.queued:
            self.queued.add(output)
            self.pending.append((source, output, number))

    def add_core(self, number: str) -> PurePosixPath:
        if number not in self.sections:
            raise BuildError(f"{self.name}: required Core section §{number} is unavailable")
        output = PurePosixPath("references/core") / (number + ".md")
        self.core_numbers.add(number)
        self.enqueue(self.sections[number].path, output, number)
        return output

    def add_template(self, path: Path) -> PurePosixPath:
        relative = path.relative_to(self.template_root)
        if path == self.source_skill:
            output = PurePosixPath("SKILL.md")
        elif path.name == "SKILL.md" and len(relative.parts) == 2:
            output = PurePosixPath("references/workflows") / (path.parent.name + ".md")
        elif relative.parts[0] == "references":
            output = PurePosixPath(relative.as_posix())
        else:
            # Skill-specific supporting files keep their relative structure.
            output = PurePosixPath("references/templates") / PurePosixPath(relative.as_posix())
        self.enqueue(path, output)
        return output

    def source_section(self, path: Path, fragment: str) -> tuple[str, str]:
        if not fragment:
            raise BuildError(f"{self.name}: choose a numbered Core section instead of the whole file {path.relative_to(self.root)}")
        if path not in self.indexes:
            raise BuildError(f"{self.name}: source outside the canonical Core manifest: {path}")
        headings, anchors = self.indexes[path]
        if fragment not in anchors:
            raise BuildError(f"{self.name}: unknown Core fragment {fragment!r} in {path.relative_to(self.root)}")
        owner = anchors[fragment]
        # A linked unnumbered subheading is covered by its complete numbered parent.
        for index in range(owner, -1, -1):
            candidate = headings[index]
            if index != owner and candidate.level >= headings[owner].level:
                continue
            match = NUMBER.match(candidate.title)
            if match:
                number = match[1]
                if number.split(".")[0] in {"20", "22"}:
                    raise BuildError(f"{self.name}: link §{number} to its owning body source instead of a generated law/glossary projection")
                if number not in self.sections:
                    raise BuildError(f"{self.name}: no packageable source section for {fragment}")
                excerpt_headings, excerpt_anchors = markdown_index(self.sections[number].content)
                if fragment in excerpt_anchors:
                    return number, fragment
                # Duplicate heading suffixes can differ after extraction. Resolve
                # the same heading position, never silently substitute a parent.
                relative_line = headings[owner].line - candidate.line
                local_heading = next((item for item in excerpt_headings if item.line == relative_line), None)
                if local_heading is None:
                    raise BuildError(f"{self.name}: fragment {fragment!r} is outside the selected section")
                return number, local_heading.fragment
        raise BuildError(f"{self.name}: {fragment!r} has no numbered Core section owner")

    def link(self, target: str, source_path: Path, output: PurePosixPath) -> str:
        parsed = urlsplit(target)
        if parsed.scheme or parsed.netloc:
            # External links in normative text and legal attribution are citations,
            # not resource retrieval instructions. No generated operation fetches them.
            return target
        if parsed.query:
            raise BuildError(f"{source_path.relative_to(self.root)}: local link query is unsupported: {target}")
        path = local_path(self.root, parsed.path, source_path.parent) if parsed.path else source_path
        fragment = unquote(parsed.fragment)
        if path.is_relative_to(self.root / "spec"):
            number, fragment = self.source_section(path, fragment)
            destination = self.add_core(number)
        elif path.is_relative_to(self.template_root):
            if path.suffix.lower() != ".md":
                raise BuildError(f"{self.name}: unsupported template resource {path}; provide a Markdown task reference")
            if fragment:
                _, anchors = markdown_index(path.read_bytes().decode("utf-8"))
                if fragment not in anchors:
                    raise BuildError(f"{self.name}: unknown template fragment {target!r}")
            destination = self.add_template(path)
        elif path in {self.root / "LICENSE", self.root / "NOTICE.md"}:
            destination = PurePosixPath(path.name)
        else:
            raise BuildError(f"{self.name}: operational dependency outside skill templates/Core: {path.relative_to(self.root)}; author a local reference")
        return relative_link(output, destination, fragment)

    def rewrite_links(self, text: str, source_path: Path, output: PurePosixPath) -> str:
        lines = text.splitlines(keepends=True)
        visible = visible_lines(text)
        result: list[str] = []
        for line, shown in zip(lines, visible):
            if shown == "\n" and line != "\n":
                result.append(line)
                continue
            # Protect inline code: an illustrative Markdown link is not a resource.
            parts = re.split(r"(`+[^`\n]*`+)", line)
            for index in range(0, len(parts), 2):
                value = parts[index]

                def inline(match):
                    raw = match[2]
                    target = raw[1:-1] if raw.startswith("<") else raw
                    rewritten = self.link(target, source_path, output)
                    return match[1] + ("<" + rewritten + ">" if raw.startswith("<") else rewritten) + match[3]

                def reference(match):
                    raw = match[2]
                    target = raw[1:-1] if raw.startswith("<") else raw
                    rewritten = self.link(target, source_path, output)
                    return match[1] + ("<" + rewritten + ">" if raw.startswith("<") else rewritten)

                def html_link(match):
                    return match[1] + match[2] + self.link(match[3], source_path, output) + match[2]

                value = INLINE_LINK.sub(inline, value)
                value = REFERENCE_LINK.sub(reference, value)
                value = HTML_LINK.sub(html_link, value)
                parts[index] = value
            result.append("".join(parts))
        return "".join(result)

    def dependencies(self, number: str, text: str) -> None:
        for line in visible_lines(text):
            if "**Primary verification route:**" in line:
                continue
            for match in SECTION_REFERENCE.finditer(line):
                expression = re.sub(r"\s+", "", match[1])
                if number in NAVIGATION_ONLY:
                    self.reports.add(f"§{number}: navigation-only reference §§{expression}")
                    continue
                for item in re.split(r"[,/]", expression):
                    if re.search(r"[–−-]", item):
                        if number in HARD_RANGE_SOURCES:
                            start, end = re.split(r"[–−-]", item)
                            low, high = tuple(map(int, start.split("."))), tuple(map(int, end.split(".")))
                            selected = [key for key in self.sections if low <= tuple(map(int, key.split("."))) <= high]
                            if start not in self.sections or end not in self.sections or not selected:
                                raise BuildError(f"§{number}: unresolved required section range §§{item}")
                            for key in selected:
                                self.add_core(key)
                        else:
                            self.reports.add(f"§{number}: range §§{item} needs route-specific review")
                    elif "." not in item:
                        self.reports.add(f"§{number}: chapter §{item} is contextual/route-specific")
                    elif item.split(".")[0] in {"20", "22"}:
                        self.reports.add(f"§{number}: generated lookup §{item} omitted in favor of body sources")
                    elif item != number:
                        self.add_core(item)

    def render_core(self, number: str, output: PurePosixPath) -> str:
        source = self.sections[number]
        self.dependencies(number, source.content)
        text = re.sub(r"^\s*<!--\s*pkb:[^\n]*-->[ \t]*\n?", "", source.content, flags=re.M)
        text = self.rewrite_links(text, source.path, output)
        # Keep the exact original heading first, preserving all fragment identities.
        first, separator, remainder = text.partition("\n")
        banner = (
            f"> Derived excerpt of Pokeball Core §{number}. This packaged reference adds no "
            "architectural authority or rules; the original Core remains normative. "
            "Source section: " + source.title + ".\n"
        )
        return (first + separator + "\n" + banner + "\n" + remainder.lstrip("\n")).rstrip("\r\n") + "\n"

    def build(self) -> tuple[dict[Path, bytes], list[str]]:
        self.add_template(self.source_skill)
        while self.pending:
            path, output, number = self.pending.pop(0)
            if number is not None:
                text = self.render_core(number, output)
            else:
                text = path.read_bytes().decode("utf-8")
                if path.name == "SKILL.md" and path != self.source_skill:
                    text = strip_frontmatter(text)
                text = self.rewrite_links(text, path, output)
            self.files[output] = text.encode("utf-8")
        for name in ("LICENSE", "NOTICE.md"):
            data = (self.root / name).read_bytes()
            # Normalize only terminal newlines in the exported license; legal text is unchanged.
            self.files[PurePosixPath(name)] = data.rstrip(b"\r\n") + b"\n" if name == "LICENSE" else data
        expected = {Path("skills") / self.name / str(path): data for path, data in self.files.items()}
        reports = [f"{self.name}: {len(self.files)} files, {len(self.core_numbers)} Core sections, {sum(map(len, self.files.values()))} bytes"]
        reports.extend(f"{self.name}: REVIEW {item}" for item in sorted(self.reports))
        return expected, reports


def generate(root: Path = ROOT) -> tuple[dict[Path, bytes], list[str]]:
    """Compute expected repository-relative generated files without mutating disk."""
    root = root.resolve()
    templates = sorted((root / "skill-src").glob("*/SKILL.md"))
    if not templates:
        raise BuildError("no skill-src/<name>/SKILL.md templates found")
    sections, indexes = core_index(root)
    files: dict[Path, bytes] = {}
    reports: list[str] = []
    for template in templates:
        name = template.parent.name
        if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", name):
            raise BuildError(f"invalid skill directory name: {name}")
        built, messages = SkillBuilder(root, name, sections, indexes).build()
        files.update(built)
        reports.extend(messages)
    return files, reports


def differences(root: Path, expected: dict[Path, bytes]) -> list[str]:
    """Compare generated file identity and inventory within current skill roots."""
    problems: list[str] = []
    names = {path.parts[1] for path in expected}
    actual: set[Path] = set()
    for name in names:
        directory = root / "skills" / name
        if directory.is_symlink():
            problems.append(f"generated skill directory must not be a symlink: skills/{name}")
            continue
        if directory.exists():
            actual.update(path.relative_to(root) for path in directory.rglob("*") if path.is_file() or path.is_symlink())
    for relative, content in sorted(expected.items()):
        path = root / relative
        if path.is_symlink():
            problems.append(f"generated file must not be a symlink: {relative}")
        elif not path.is_file():
            problems.append(f"missing generated file: {relative}")
        elif path.read_bytes() != content:
            problems.append(f"generated content differs: {relative}")
    problems.extend(f"unexpected generated file: {path}" for path in sorted(actual - set(expected)))
    return problems


def write_generated(root: Path, expected: dict[Path, bytes]) -> None:
    names = {path.parts[1] for path in expected}
    # Check every output parent before any mutation, including pre-existing links.
    for relative in expected:
        target = root / relative
        if target.is_symlink() or any(parent.is_symlink() for parent in target.parents if parent != root and parent.is_relative_to(root)):
            raise BuildError(f"refusing to write through a generated-path symlink: {relative}")
    for name in sorted(names):
        directory = root / "skills" / name
        if directory.exists():
            for path in sorted(directory.rglob("*"), reverse=True):
                if (path.is_file() or path.is_symlink()) and path.relative_to(root) not in expected:
                    path.unlink()
                elif path.is_dir() and not any(path.iterdir()):
                    path.rmdir()
    for relative, data in sorted(expected.items()):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.is_file() or path.read_bytes() != data:
            path.write_bytes(data)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true", help="replace generated skill files")
    mode.add_argument("--check", action="store_true", help="check generated bytes and inventory (default)")
    parser.add_argument("--show-reference-report", action="store_true", help="list contextual/range references requiring semantic route review")
    args = parser.parse_args(argv)
    try:
        expected, reports = generate(ROOT)
        if args.write:
            write_generated(ROOT, expected)
        problems = differences(ROOT, expected)
        for report in reports:
            if args.show_reference_report or ": REVIEW " not in report:
                print(report)
        review_count = sum(": REVIEW " in report for report in reports)
        if review_count:
            print(f"{review_count} contextual/range references recorded; --show-reference-report prints the complete route-review inventory.")
        for problem in problems[:30]:
            print(f"ERROR: {problem}", file=sys.stderr)
        if len(problems) > 30:
            print(f"ERROR: {len(problems) - 30} further generated differences", file=sys.stderr)
        if problems:
            return 1
        print("Standalone skill generation matches maintained templates and selected Core sections.")
        return 0
    except (BuildError, ContextError, OSError, UnicodeError, ValueError) as error:
        print(f"build_skills: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
