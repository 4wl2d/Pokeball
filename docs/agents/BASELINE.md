# Agent Pack Integrity Manifest

> **Status:** derived noncanonical package metadata. This file does not define or extend Pokeball Architecture. If it conflicts with the canonical specification, Core prevails.

This manifest identifies one exact Core candidate and its matching Agent Pack. It is the only file in `docs/agents/` that records the exact Core SHA-256. Hashes cover exact bytes without line-ending, Unicode, or whitespace normalization.

```yaml
schema: pokeball-agent-pack/v2
packRevision: 2
status: derived-noncanonical
canonicalSpec:
  pathFromRepositoryRoot: spec/pokeball-architecture-core.md
  declaredVersion: 1.2.0-draft
  declaredStatus: canonical draft
  sha256: 001dbcf45acb49e298b474bd78c5fc842a80a7cabc17e1d57179edb64f020d83
  bytes: 277970
packageIntegrity:
  fileCountIncludingBaseline: 15
  digestScope: lexicographic filename + NUL + exact bytes + NUL for every sibling Markdown file except BASELINE.md
  sha256: 3c5370a203ba7c98b56c5a2bc0ae53c5e023cca8c02cf7655e7f93821cd19cbd
readinessRequirements:
  sameImmutablePublishedSnapshot: true
  exactIntegrityMatch: true
hashOccurrencePolicy: BASELINE-only
```

## Verification

From the repository root:

```sh
shasum -a 256 spec/pokeball-architecture-core.md
wc -c spec/pokeball-architecture-core.md
```

For the package digest:

```sh
python3 - <<'PY'
from pathlib import Path
import hashlib

root = Path('docs/agents')
h = hashlib.sha256()
for path in sorted(p for p in root.glob('*.md') if p.name != 'BASELINE.md'):
    h.update(path.name.encode('utf-8'))
    h.update(b'\0')
    h.update(path.read_bytes())
    h.update(b'\0')
print(h.hexdigest())
PY
```

The declared Core version/status/hash/bytes, package file count, and package digest must all match. The root `LICENSE` and `NOTICE.md` are outside the package digest. Portable copies retain the digest-covered `LICENSING.md` without replacing the consuming project's software license.

## Readiness rule

Matching metadata proves byte identity, not architectural quality and not publication provenance. The package is ready for application only when both conditions hold:

1. the exact Core and Agent Pack bytes came from the same immutable published snapshot; and
2. every integrity value above matches those delivered bytes.

If immutable publication provenance is absent, the files are a candidate rather than a ready package. If any value differs, treat the package as stale. If Core or this manifest is missing, stop Pokeball-specific application and obtain the canonical artifacts.

Do not edit an installed manifest or update only its hash. Replace Core and the entire Agent Pack from one later immutable published snapshot. An absent project overlay does not make the package stale: an overlay is needed only when a task depends on a shared policy, permitted delta, waiver, or claim that is not otherwise resolved.

## Consumer integrity report

```text
Immutable snapshot provenance: present | absent
Core path: ...
Declared version/status: ...
Exact Core hash/bytes match: yes | no
Exact package count/digest match: yes | no
Project policy or overlay used by this task: exact reference | absent
Task scope: ...
Applicable PKB-AR rules: ...
```
