#!/usr/bin/env python3
"""Read exact Markdown sections from this checkout without loading whole chapters."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import html
from pathlib import Path
import re
import sys
import unicodedata
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parents[1]


class ContextError(ValueError):
    """An invalid local source or section request."""


@dataclass(frozen=True)
class Heading:
    level: int
    line: int
    title: str
    fragment: str


def local_path(root: Path, relative: str, base: Path | None = None) -> Path:
    """Resolve local paths, including symlinks, without escaping the checkout."""
    relative = unquote(relative)
    if not relative or Path(relative).is_absolute() or "\\" in relative:
        raise ContextError(f"expected a relative checkout path: {relative!r}")
    root = root.resolve()
    try:
        path = ((base or root) / relative).resolve()
    except (OSError, RuntimeError) as error:
        raise ContextError(f"cannot resolve path {relative!r}: {error}") from error
    if not path.is_relative_to(root):
        raise ContextError(f"path leaves the checkout: {relative}")
    if not path.is_file():
        raise ContextError(f"file not found: {relative}")
    return path


def visible_lines(source: str) -> list[str]:
    """Mask fenced code and frontmatter while retaining source line positions."""
    lines = source.splitlines(keepends=True)
    result = list(lines)
    fence: tuple[str, int] | None = None
    frontmatter = bool(lines and lines[0].strip() == "---")
    for index, line in enumerate(lines):
        if frontmatter:
            result[index] = "\n"
            if index and line.strip() in ("---", "..."):
                frontmatter = False
            continue
        marker = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line.rstrip("\r\n"))
        if fence:
            result[index] = "\n"
            if marker and marker[1][0] == fence[0] and len(marker[1]) >= fence[1] and not marker[2].strip():
                fence = None
        elif marker and not (marker[1][0] == "`" and "`" in marker[2]):
            fence = (marker[1][0], len(marker[1]))
            result[index] = "\n"
    return result


def heading_slug(title: str) -> str:
    """GitHub-style slugs for the ATX/Setext headings used by these sources."""
    title = re.sub(r"!?\[([^]]*)\]\([^)]*\)", r"\1", title)
    title = html.unescape(re.sub(r"<[^>]*>", "", title)).lower()
    title = "".join(char for char in title if char in "-_" or not unicodedata.category(char).startswith(("P", "S")))
    return re.sub(r"\s", "-", title)


def markdown_index(source: str) -> tuple[list[Heading], dict[str, int]]:
    """Return headings and fragment-to-heading indexes, including explicit IDs."""
    lines = visible_lines(source)
    headings: list[Heading] = []
    used: set[str] = set()
    explicit: list[tuple[str, int]] = []
    for index, line in enumerate(lines):
        for match in re.finditer(r"<a\b[^>]*\b(?:id|name)\s*=\s*(['\"])(.*?)\1[^>]*>", line, re.I):
            explicit.append((html.unescape(match[2]), index))
        match = re.match(r"^ {0,3}(#{1,6})(?:[ \t]+(.*?)|[ \t]*)$", line.rstrip("\r\n"))
        if match:
            level, start = len(match[1]), index
            title = re.sub(r"[ \t]+#+[ \t]*$", "", match[2] or "").strip()
        elif index and lines[index - 1].strip() and re.match(r"^ {0,3}(=+|-+)[ \t]*$", line.rstrip("\r\n")):
            if re.match(r"^ {0,3}(?:#{1,6}\s|>|[-*+]\s)", lines[index - 1]):
                continue
            level, start, title = (1 if line.lstrip().startswith("=") else 2), index - 1, lines[index - 1].strip()
        else:
            continue
        slug = heading_slug(title)
        fragment, suffix = slug, 0
        while fragment in used:
            suffix += 1
            fragment = f"{slug}-{suffix}"
        used.add(fragment)
        headings.append(Heading(level, start, title, fragment))
    anchors = {heading.fragment: index for index, heading in enumerate(headings)}
    for fragment, line in explicit:
        # A standalone anchor directly preceding a heading belongs to that heading.
        following = next((i for i, heading in enumerate(headings) if heading.line > line), None)
        only_anchor = not re.sub(r"<a\b[^>]*>\s*</a>", "", lines[line], flags=re.I).strip()
        if following is not None and only_anchor and not "".join(lines[line + 1:headings[following].line]).strip():
            owner = following
        else:
            owner = next((i for i in range(len(headings) - 1, -1, -1) if headings[i].line <= line), None)
        if owner is not None:
            if fragment in anchors and anchors[fragment] != owner:
                raise ContextError(f"ambiguous Markdown anchor: {fragment}")
            anchors[fragment] = owner
    return headings, anchors


def section(root: Path, request: str) -> tuple[Path, str]:
    relative, separator, fragment = request.partition("#")
    if not separator or not fragment:
        raise ContextError("a section requires path.md#fragment; use --list path.md to find fragments")
    path = local_path(root, relative)
    if path.suffix.lower() != ".md":
        raise ContextError(f"section source must be Markdown: {relative}")
    source = path.read_bytes().decode("utf-8")
    headings, anchors = markdown_index(source)
    fragment = unquote(fragment)
    if fragment not in anchors:
        raise ContextError(f"unknown fragment {fragment!r} in {relative}; use --list {relative}")
    selected = headings[anchors[fragment]]
    lines = source.splitlines(keepends=True)
    end = next((heading.line for heading in headings if heading.line > selected.line and heading.level <= selected.level), len(lines))
    return path, "".join(lines[selected.line:end])


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sections", nargs="*", metavar="PATH#FRAGMENT")
    parser.add_argument("--list", dest="list_path", metavar="PATH", help="list section fragments in a Markdown file")
    args = parser.parse_args(argv)
    try:
        if args.list_path:
            if args.sections:
                raise ContextError("choose section requests or --list, not both")
            path = local_path(ROOT, args.list_path)
            if path.suffix.lower() != ".md":
                raise ContextError("--list requires a Markdown file")
            headings, anchors = markdown_index(path.read_bytes().decode("utf-8"))
            relative = path.relative_to(ROOT)
            for index, heading in enumerate(headings):
                aliases = [fragment for fragment, owner in anchors.items() if owner == index and fragment != heading.fragment]
                print(f"{relative}#{heading.fragment}  {heading.title}" + (f"  [aliases: {', '.join(aliases)}]" if aliases else ""))
        else:
            if not args.sections:
                raise ContextError("provide path.md#fragment or --list path.md")
            # Validate the whole request before emitting partial context.
            results = [section(ROOT, request) for request in args.sections]
            for index, (_, content) in enumerate(results):
                if index:
                    sys.stdout.write("\n")
                sys.stdout.write(content)
        return 0
    except (ContextError, OSError, UnicodeError) as error:
        print(f"skill_context: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
