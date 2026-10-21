#!/usr/bin/env python3
"""Checks that every relative link in tracked Markdown resolves to a tracked
file or directory. Local-only files cannot satisfy links. Anchors are not checked.

Usage: python3 tools/check_links.py [root]
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)|!\[[^\]]*\]\(([^)\s]+)\)|<(?:a|img)\s[^>]*(?:href|src)=\"([^\"]+)\"")
REFDEF = re.compile(r"^\s*\[[^\]]+\]:\s*(\S+)", re.M)


def main() -> None:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    tracked = set(subprocess.run(["git", "ls-files", "-z"], cwd=root, capture_output=True, text=True, check=True).stdout.rstrip("\0").split("\0"))
    files = sorted(path for path in tracked if path.endswith(".md"))
    directories = {str(parent) for path in tracked for parent in Path(path).parents}
    broken = []
    for rel in files:
        path = root / rel
        text = path.read_text(encoding="utf-8")
        text = re.sub(r"```.*?```", "", text, flags=re.S)  # ignore code blocks
        targets = [t for m in LINK.finditer(text) for t in m.groups() if t] + REFDEF.findall(text)
        for target in targets:
            if re.match(r"^[a-z][a-z0-9+.-]*:", target) or target.startswith("#"):
                continue  # external URL, mailto, or in-page anchor
            file_part = unquote(target.split("#", 1)[0])
            if not file_part:
                continue
            resolved = (path.parent / file_part).resolve()
            try:
                relative = str(resolved.relative_to(root))
            except ValueError:
                relative = None
            if not resolved.exists() or relative not in tracked | directories:
                broken.append(f"{rel}: {target}")
    for b in broken:
        print(b)
    print(f"{len(files)} Markdown files checked, {len(broken)} broken relative links")
    sys.exit(1 if broken else 0)


if __name__ == "__main__":
    main()
