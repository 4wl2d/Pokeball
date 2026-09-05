<!-- pkb:translation source="docs/agents/PROJECT-OVERLAY.template.md" -->

[Документация на русском](../README.md) · [Содержание Agent Pack](README.md) · [Оригинал на английском](../../agents/PROJECT-OVERLAY.template.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md). Инструкции, команды, шаблоны и контрольные суммы относятся к [исходному английскому Agent Pack](../../agents/README.md); для установки и проверки целостности используйте его.

<a id="pokeball-project-policy--template"></a>

# Политика проекта Pokeball — шаблон

> **Статус:** производный неканонический шаблон. Это не принятое решение проекта; он не определяет и не расширяет Core. Проверьте `docs/agents/BASELINE.md`; приоритет имеет Core.

Используйте этот шаблон, только если проект выбирает повторно используемые политики, привязки к среде исполнения, исключения или заявления о гарантиях. Проекту, в котором все применимые ограничения обеспечены устройством системы или локальными объявлениями, overlay не нужен. При использовании скопируйте шаблон в `docs/pokeball-project-overlay.md`, замените заполнители в угловых скобках, удалите неиспользуемые необязательные ключи/разделы и получите принятие владельцем проекта. Эта политика объявляет общие факты один раз; это не перечень Ball.

<a id="project-policy"></a>

## Политика проекта

```yaml
schema: pokeball-project-policy/v2

metadata:
  project: <project name>
  revision: <immutable revision>
  contentDigest: <sha256>
  owner: <accountable owner>
  acceptedBy: <project owner or review body>
  acceptedAt: <date>
  coreBaseline: docs/agents/BASELINE.md

sources:
  ballContracts: [<typed-source path or manifest glob>]
  # Include only when an inter-Ball edge exists.
  assembly: <sole route/composition source>

policies:
  <policy id>:
    revision: <immutable revision>
    digest: <sha256>
    owner: <policy owner>
    scope:
      ballsOrBindings: [<exact scope>]
      bindingsProfilesOrEnvironments: [<exact applicable scope>]
    coveredGuardrails: [<PBA or PKB-AR ids>]
    effective:
      # Keep only non-empty categories and present dimensions.
      profiles:
        <present profile dimension>: <exact choice>
      limits:
        <present dimension>: <finite value and unit>
      mechanisms:
        <triggered guardrail>: <exact mechanism or binding reference>
    overridableFields: [<exact allowlist>]
    enforcement:
      owner: <enforcement owner>
      # Include only concrete enforcement/evidence artifacts.
      artifacts: [<path or exact artifact reference>]
    # Include only when review or expiry exists for this declaration/evidence.
    reviewOrExpiry:
      condition: <exact date, event, or invalidation condition>
      owner: <review owner>

# Add only when the project graph contains them.
compositionCeilings:
  <present graph dimension>:
    value: <finite value and unit>
    policyRef: <exact revision and digest>

sharedBindings:
  <binding id>:
    policyRef: <exact revision and digest>
    source: <path>

# Add only when a deliberate deviation exists. Keep exactly the eight Core fields.
waivers:
  - owner: <accountable owner>
    approvedBy: <accepting authority>
    governingAnchor: <exact Core, extension, or project-policy anchor>
    exactScope: <exact nonconforming scope>
    reason: <why the deviation is accepted>
    constraintsAndCompensatingControls: [<constraint or control>]
    testsAndEvidence: [<test or evidence reference>]
    review:
      expiryOrReviewAt: <date, event, or review condition>
      remediation: <required remediation>
      conformanceEffect: <exact effect on claims and conformance>

# Add only as a non-empty reference list.
claims: [<claim record path>]

acceptance:
  decision: <accepted or rejected>
  revision: <policy revision>
  acceptedBy: <owner or review body>
  acceptedAt: <date>
  review: <accepted project record or path>
```

Оставьте только действующие измерения и механизмы в области каждой политики. Добавляйте `evidence: {scope, artifacts}` в политику, только если её активированный механизм или конкретное заявление о гарантиях требует доказательств. Общие доказательства записываются один раз с точными дайджестом/областью артефакта.

<a id="scope-selection-and-ball-delta"></a>

## Выбор области и отличие Ball

Принятая область проекта/привязки к среде исполнения выбирает точную политику один раз. Охваченный Ball ничего не содержит, если у него нет разрешённого отличия; тогда авторитетный типизированный исходный код или сгенерированный манифест содержит только отличие:

```yaml
policyDelta:
  overrides:
    <allowlisted field>: <local effective value>
```

Опускайте пустой `policyDelta`. Точный локальный `policySelection` Ball добавляют, только если он намеренно выбирает другую политику, а не наследует охватывающую принятую область. Статическое разрешение отвергает изменяемые/устаревшие ссылки, циклы, конфликты, несовпадение области/профиля/привязки к среде исполнения/окружения, непокрытые триггеры и поля вне `overridableFields`.

Политика может выбирать только механизм или значение, которые Core оставляет открытыми. Она не может подавлять триггер применимости, ослаблять закон или разрешать цикл прямого управления.

Исключение использует ровно восемь полей выше. Оно фиксирует осознанное несоответствие и не становится политикой, прецедентом или доказательством обеспечения ограничения, от которого отступили. Нарушение `MUST` или `MUST NOT` блокирует заявление о соответствии для `exactScope` исключения, как указано в `review.conformanceEffect`; разрешённый исключением цикл прямого управления остаётся осознанным несоответствием. Запись о заявлении владеет областью действия, механизмом, допущениями, доказательствами, негарантиями и результатами проверок; эта политика только ссылается на неё.

Для принятия нужны точные дайджесты базовой версии/политики, разрешимые пути источников, ациклический бесконфликтный граф ссылок, положительные и отрицательные тесты разрешения триггеров, проверка точной формы WaiverRecord и влияния на соответствие при наличии исключения и чистый перенос по `docs/agents/INSTALL.md`. Перечень для каждого Ball, копия маршрутов, пустой необязательный раздел или таблица доказательств не нужны.
