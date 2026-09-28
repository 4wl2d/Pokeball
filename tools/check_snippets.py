#!/usr/bin/env python3
"""Checks that code snippets in the documentation match the source files they
come from. A fenced code block directly preceded by a line
`<!-- snippet: path/from/repo/root -->` must appear verbatim (ignoring trailing
whitespace) in that file. Exits 1 on a mismatch.

Usage: python3 tools/check_snippets.py [files...]   (default: docs/*.md)
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BLOCK = re.compile(r"<!-- snippet: (\S+) -->\s*\n```[a-z]*\n(.*?)\n```", re.S)


def norm(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.splitlines())


def main() -> None:
    files = [Path(f) for f in sys.argv[1:]] or sorted((ROOT / "docs").glob("*.md"))
    checked = failed = 0
    for doc in files:
        for m in BLOCK.finditer(doc.read_text(encoding="utf-8")):
            checked += 1
            source = ROOT / m.group(1)
            if not source.exists() or norm(m.group(2)) not in norm(source.read_text(encoding="utf-8")):
                failed += 1
                print(f"{doc.relative_to(ROOT)}: snippet not found verbatim in {m.group(1)}:\n{m.group(2)[:200]}\n")
    print(f"{checked} snippets checked, {failed} mismatched")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
