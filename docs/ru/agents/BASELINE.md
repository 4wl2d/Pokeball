<!-- pkb:translation source="docs/agents/BASELINE.md" -->

[Документация на русском](../README.md) · [Содержание Agent Pack](README.md) · [Оригинал на английском](../../agents/BASELINE.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md). Пояснения, промпты и формы отчётов переведены для людей. Пути установки, схемы и контрольные суммы относятся к [исходному английскому Agent Pack](../../agents/README.md); устанавливайте и проверяйте его точные артефакты.

<a id="agent-pack-integrity-manifest"></a>

# Манифест целостности Agent Pack

> **Статус:** метаданные производного неканонического пакета. Этот файл не определяет и не расширяет Pokeball Architecture. При расхождении с канонической спецификацией приоритет имеет Core.

Этот манифест определяет один точный кандидат упорядоченного набора Core и соответствующий ему Agent Pack. Это единственный файл в `docs/agents/`, который записывает точный SHA-256 набора Core. Дайджесты охватывают точные пути и байты без нормализации переводов строк, Unicode или пробельных символов.

```yaml
schema: pokeball-agent-pack/v3
packRevision: 15
status: derived-noncanonical
canonicalCore:
  entrypointFromRepositoryRoot: spec/pokeball-architecture-core.md
  declaredVersion: 1.5.0-draft
  declaredStatus: canonical draft
  fileCountIncludingEntrypoint: 25
  digestScope: manifest order + repository-relative path + NUL + exact bytes + NUL
  sha256: 2ac605e4ff4db406b661356ea9c15a1b2d1683f68e515cd7cfc136c41c28daad
  bytes: 733764
packageIntegrity:
  fileCountIncludingBaseline: 25
  digestScope: lexicographic filename + NUL + exact bytes + NUL for every sibling Markdown file except BASELINE.md
  sha256: 736220908debbb93a84dd971ce5943efb79b957cb3c6d7c04ad6eba97ae1aa97
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
Происхождение из неизменяемого снимка: подтверждено | отсутствует
Точка входа Core и пути из манифеста: ...
Объявленные версия и статус: ...
Точное совпадение числа файлов, дайджеста и байтов Core: да | нет
Точное совпадение числа файлов и дайджеста пакета: да | нет
Политика проекта или overlay, используемые задачей: точная ссылка | отсутствуют
Область задачи: ...
Применимые правила PKB-AR: ...
```
