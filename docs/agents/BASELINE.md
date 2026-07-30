# Agent Pack Integrity Manifest

> **Status:** derived noncanonical package metadata. This file does not define or extend Pokeball Architecture. If it conflicts with the canonical specification, Core prevails.

This manifest identifies one exact ordered Core-set candidate and its matching Agent Pack. It is the only file in `docs/agents/` that records the exact Core-set SHA-256. Digests cover exact paths and bytes without line-ending, Unicode, or whitespace normalization.

```yaml
schema: pokeball-agent-pack/v3
packRevision: 12
status: derived-noncanonical
canonicalCore:
  entrypointFromRepositoryRoot: spec/pokeball-architecture-core.md
  declaredVersion: 1.4.0-draft
  declaredStatus: canonical draft
  fileCountIncludingEntrypoint: 25
  digestScope: manifest order + repository-relative path + NUL + exact bytes + NUL
  sha256: d7792cb6adfaf9d7e3cf0c59bcc40b1158200bfcd0496661d3293035917f352c
  bytes: 725281
packageIntegrity:
  fileCountIncludingBaseline: 25
  digestScope: lexicographic filename + NUL + exact bytes + NUL for every sibling Markdown file except BASELINE.md
  sha256: 1332fbc4ccbc55112ea87fa902437e6bb27f043a67663ebbe6e11f3e4239089d
readinessRequirements:
  sameImmutablePublishedSnapshot: true
  exactIntegrityMatch: true
hashOccurrencePolicy: BASELINE-only
```

## Verification

From the repository root, verify the manifest order and Core-set digest:

```sh
python3 - <<'PY'
from pathlib import Path
import hashlib
import re

entrypoint = Path('spec/pokeball-architecture-core.md')
manifest = entrypoint.read_text(encoding='utf-8').split(
    '## Canonical document set', 1
)[1].split('## Section index', 1)[0]
targets = re.findall(r'^- \[[^]]+\]\(([^)]+\.md)\)$', manifest, re.MULTILINE)

digest = hashlib.sha256()
total = 0
for target in targets:
    path = Path('spec') / target
    data = path.read_bytes()
    relative = path.as_posix().encode('utf-8')
    digest.update(relative)
    digest.update(b'\0')
    digest.update(data)
    digest.update(b'\0')
    total += len(data)

print(f'files={len(targets)}')
print(f'bytes={total}')
print(f'sha256={digest.hexdigest()}')
PY
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

The declared Core entrypoint version/status, manifest file count, set digest/bytes, package file count, and package digest must all match. The root `LICENSE` and `NOTICE.md` are outside the package digest. Portable copies retain the digest-covered `LICENSING.md` without replacing the consuming project's software license.

## Readiness rule

Matching metadata proves byte identity, not architectural quality and not publication provenance. The package is ready for application only when both conditions hold:

1. the exact Core and Agent Pack bytes came from the same immutable published snapshot; and
2. every integrity value above matches those delivered bytes.

If immutable publication provenance is absent, the files are a candidate rather than a ready package. If any value differs, treat the package as stale. If the Core entrypoint, any manifest-listed Core file, or this manifest is missing, stop Pokeball-specific application and obtain the canonical artifacts.

Do not edit an installed manifest or update only its digest. Replace the complete Core set and the entire Agent Pack from one later immutable published snapshot. An absent project overlay does not make the package stale: an overlay is needed only when a task depends on a shared policy, permitted delta, waiver, or claim that is not otherwise resolved.

## Consumer integrity report

```text
Immutable snapshot provenance: present | absent
Core entrypoint and manifest paths: ...
Declared version/status: ...
Exact Core file count/set digest/bytes match: yes | no
Exact package count/digest match: yes | no
Project policy or overlay used by this task: exact reference | absent
Task scope: ...
Applicable PKB-AR rules: ...
```
