<!-- pkb:translation source="docs/agents/BASELINE.md" -->

[Документация на русском](../README.md) · [Содержание Agent Pack](README.md) · [Оригинал на английском](../../agents/BASELINE.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md). Инструкции, команды, шаблоны и контрольные суммы относятся к [исходному английскому Agent Pack](../../agents/README.md); для установки и проверки целостности используйте его.

<a id="agent-pack-integrity-manifest"></a>

# Манифест целостности Agent Pack

> **Статус:** метаданные производного неканонического пакета. Этот файл не определяет и не расширяет Pokeball Architecture. При расхождении с канонической спецификацией приоритет имеет Core.

Этот манифест определяет один точный кандидат упорядоченного набора Core и соответствующий ему Agent Pack. Это единственный файл в `docs/agents/`, который записывает точный SHA-256 набора Core. Дайджесты охватывают точные пути и байты без нормализации переводов строк, Unicode или пробельных символов.

```yaml
schema: pokeball-agent-pack/v3
packRevision: 13
status: derived-noncanonical
canonicalCore:
  entrypointFromRepositoryRoot: spec/pokeball-architecture-core.md
  declaredVersion: 1.4.0-draft
  declaredStatus: canonical draft
  fileCountIncludingEntrypoint: 25
  digestScope: manifest order + repository-relative path + NUL + exact bytes + NUL
  sha256: cd1322a8fe58ad429587120e32d04281b5d9d4df6c68b51a2a0ce1cd3e090637
  bytes: 732089
packageIntegrity:
  fileCountIncludingBaseline: 25
  digestScope: lexicographic filename + NUL + exact bytes + NUL for every sibling Markdown file except BASELINE.md
  sha256: c40b5cd1b4608cf97f75111f238440c7b4577b72e472032a6d581ee2bcfb7ced
readinessRequirements:
  sameImmutablePublishedSnapshot: true
  exactIntegrityMatch: true
hashOccurrencePolicy: BASELINE-only
```

<a id="verification"></a>

## Проверка

Из корня репозитория проверьте порядок манифеста и дайджест набора Core:

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

Для дайджеста пакета:

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

Объявленные версия/статус точки входа Core, число файлов манифеста, дайджест/байты набора, число файлов пакета и дайджест пакета должны совпасть. Корневые `LICENSE` и `NOTICE.md` не входят в дайджест пакета. Переносимые копии сохраняют входящий в дайджест `LICENSING.md`, не заменяя лицензию программного обеспечения принимающего проекта.

<a id="readiness-rule"></a>

## Правило готовности

Совпадение метаданных доказывает тождество байтов, но не качество архитектуры и не происхождение публикации. Пакет готов к применению, только если выполнены оба условия:

1. точные байты Core и Agent Pack получены из одного неизменяемого опубликованного снимка; и
2. каждое значение целостности выше совпадает с полученными байтами.

Если происхождение из неизменяемой публикации не подтверждено, файлы остаются кандидатом, а не готовым пакетом. Если любое значение отличается, считайте пакет устаревшим. Если нет точки входа Core, любого файла Core из манифеста или самого манифеста, прекратите применение правил Pokeball и получите канонические артефакты.

Не редактируйте установленный манифест и не обновляйте только его дайджест. Замените полный набор Core и весь Agent Pack данными одного более позднего неизменяемого опубликованного снимка. Отсутствие overlay проекта не делает пакет устаревшим: overlay нужен, только если задача зависит от общей политики, разрешённого отличия, исключения или заявления о гарантиях, которые не разрешены другим способом.

<a id="consumer-integrity-report"></a>

## Отчёт потребителя о целостности

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
