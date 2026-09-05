<!-- pkb:translation source="docs/agents/INSTALL.md" -->

[Документация на русском](../README.md) · [Содержание Agent Pack](README.md) · [Оригинал на английском](../../agents/INSTALL.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md). Инструкции, команды, шаблоны и контрольные суммы относятся к [исходному английскому Agent Pack](../../agents/README.md); для установки и проверки целостности используйте его.

<a id="installing-and-updating-the-agent-pack"></a>

# Установка и обновление Agent Pack

> **Статус:** производный неканонический пакет. Он не определяет и не расширяет Pokeball Core. Проверьте [BASELINE.md](BASELINE.md); приоритет имеет Core.

Установка переносит проекцию Core, а не второй нормативный источник. Если установленные указания расходятся с отмеченным исходным положением Core, приоритет имеет Core, а пакет нужно заменить синхронизированным снимком.

<a id="target-layout"></a>

## Целевая структура

```text
target-repo/
├── spec/pokeball-architecture-core.md # stable Core entrypoint and ordered manifest
├── spec/core/**                       # every manifest-listed Core chapter
├── docs/agents/                    # exact portable package
├── docs/pokeball-project-overlay.md # optional accepted shared project policy
└── AGENTS.md
```

Контракты Ball остаются в обычном типизированном исходном коде или локальных манифестах. Assembly остаётся в корне композиции проекта. Общая политика не содержит перечня Ball.

<a id="initial-installation"></a>

## Первая установка

1. Скопируйте стабильную точку входа Core, каждый точный путь `spec/core/**` из её упорядоченного манифеста и каждый файл `docs/agents/` из одного опубликованного неизменяемого снимка, сохранив пути относительно корня репозитория, точные байты и имена файлов. Сохраните `LICENSING.md`, не заменяя `LICENSE` целевого программного обеспечения.
2. Проверьте манифест целостности упорядоченного набора Core и пакета в [BASELINE.md](BASELINE.md). Для готовности нужны и подтверждённое происхождение из неизменяемой публикации, и точное совпадение метаданных; сам манифест не содержит вывода проверки.
3. Изучите один реальный Ball и обеспечьте его действующие ограничения устройством системы, локальным объявлением или повторно используемой политикой; не создавайте overlay лишь ради завершения установки.
4. Только если общие политики, привязки к среде исполнения, потолки, отступления или заявления о гарантиях полезны, скопируйте [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) в `docs/pokeball-project-overlay.md`, удалите неиспользуемые разделы, разрешите пути источников и получите принятие владельцем плюс неизменяемую ревизию/дайджест.
5. Закрепите точную политику один раз в её авторитетной области проекта/привязки к среде исполнения; Ball записывает ссылку, только если ещё не охвачен ею, и добавляет лишь разрешённое локальное отличие. Полностью локальный/статический Ball не содержит строки политики.
6. Добавьте блок маршрутизации ниже в корневой `AGENTS.md`; сохраните существующие указания по репозиторию, сборке и тестам.
7. Проверьте скопированные файлы, перенесённые ссылки и маршрутизацию, затем выполните один подходящий обычный пробный прогон из [PORTABILITY-VALIDATION.md](PORTABILITY-VALIDATION.md). Полный каталог для чистой структуры проверяет сам Agent Pack по `AP-GATE-10`; принимающий проект не реализует отсутствующие возможности, чтобы повторить его. Перед заявлением о соответствии или выпуске пройдите все применимые проверки `RG-*`.

Обычная установка, проектирование и внедрение не создают `TriggerAbsenceProof`. Только вывод о соответствии/выпуске, зависящий от отсутствия, или принятое решение по неоднозначности записывает одно точное доказательство в области владельца доказательств; для `always` или присутствующего триггера нельзя доказать отсутствие.

Точная политика выбирает только механизмы или значения, которые Core оставляет открытыми; она не может подавить триггер, ослабить закон или разрешить цикл прямого управления. `WaiverRecord` записывает осознанное отступление и его влияние на соответствие, но никогда не обеспечивает само ограничение. При нарушении `MUST`/`MUST NOT` точная область остаётся несоответствующей.

Рекомендуемый блок маршрутизации:

```markdown
## Pokeball Architecture

For Pokeball-scoped work, verify `docs/agents/BASELINE.md`, then read
`docs/agents/AGENT-CONTRACT.md` and only the runbooks activated by the closed
source/profile/route/risk/claim inventory. If the affected source references
`docs/pokeball-project-overlay.md`, read that exact accepted policy too.

Use `ASYNC-STATUS-RUNBOOK.md` for root idempotency, an inter-Ball command/result
round trip, retry, or cancellation, and continue to `STATUS-AND-ASYNC-TESTS.md`
only for operation status or its async test catalogue. Use
`COMPOSITION-PROFILES.md` for an ordinary cross-authority ReadDependency,
utility ownership, a Nucleus import of an owner-authored Application Surface,
FlowParticipation, dependency/Flow-route counting, or cumulative fan-out;
continue to `COMPOSITION-PROFILES-AND-CLAIMS.md` only for profile, claim, or
Foundation work.

Use `DESIGN-RUNBOOK.md` for boundary and State, then
`PROTOCOL-DESIGN-RUNBOOK.md` for the selected Snapshot/Event mutation,
accepted frame, protocol, output, lifecycle, or read. Use
`SECURITY-LIMITS-RUNBOOK.md` for protocol-validation versus State/Context
business-stage ownership, capabilities, sinks, secrets, or unsafe paths; use
`LIMITS-AND-EVIDENCE-RUNBOOK.md` for any numeric input/State/output byte limit,
a triggered Decision Work Meter, runtime-enforced fan-out ceiling, admission,
or evidence reuse. Use the absence-proof gate only when a claim or accepted
ambiguity decision relies on absence.

Canonical Core set: `spec/pokeball-architecture-core.md` plus every exact
ordered manifest-listed path under `spec/core/**`.
Core prevails. Shared policies are exact static references; Balls record only
allowed deltas. Absent paths create no placeholder artifacts.
```

<a id="routine-dry-run"></a>

## Обычный пробный прогон

Перенесён в [PORTABILITY-VALIDATION.md](PORTABILITY-VALIDATION.md#routine-dry-run), где определены форма обычного отчёта и каталог проверочных сценариев для чистой структуры.

<a id="updating"></a>

## Обновление

1. Зафиксируйте старые базовые версии упорядоченного набора Core, пакета, исходного кода Ball и заявлений о гарантиях, а также каждой политики проекта, затронутой обновлением.
2. Замените точку входа Core, каждую главу из манифеста и весь Agent Pack данными одного нового опубликованного неизменяемого снимка; никогда не превращайте установленный снимок в другую ревизию правками на месте.
3. Если общая политика меняется, опубликуйте её как новую неизменяемую ревизию/дайджест и перечислите Ball, которые на неё ссылаются, и доказательства, потерявшие силу.
4. Обновите только затронутые ссылки/отличия Ball и активированные тесты. Не копируйте неизменную политику.
5. После семантических проверок и проверки переносимости сверьте в новом `BASELINE.md` версию/статус точки входа Core, число файлов манифеста, дайджест/байты набора Core и число файлов/дайджест пакета; потребители не обновляют этот манифест сами.
6. Повторяйте полные проверки проекта только для существующего или предлагаемого заявления о гарантиях, чья область изменилась.

Запрещены частичные обновления: изменение только дайджеста; сохранение изменяемой ссылки на политику «latest»; копирование одной инструкции без её базовой версии/контракта/индекса/трассировки/проверок; замена `LICENSING.md`; трактовка шаблона как принятой политики, а исключения — как источника приоритета; сохранение заявления о гарантиях или доказательства отсутствия после изменения дайджеста его профиля/политики/маршрута/перечня/доказательств.

<a id="discontinuing-pokeball"></a>

## Отказ от Pokeball

Удаление маршрутизации и артефактов — решение владельца проекта. Сохраните историю заявлений о гарантиях/отступлений и прекратите использовать формулировки о соответствии. Не оставляйте маршруты к отсутствующим или устаревшим артефактам Core/пакета/политики.
