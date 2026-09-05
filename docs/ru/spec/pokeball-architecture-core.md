<!-- pkb:translation source="spec/pokeball-architecture-core.md" -->

[Документация на русском](../README.md) · [Содержание Core](pokeball-architecture-core.md) · [Оригинал на английском](../../../spec/pokeball-architecture-core.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md).

<a id="pokeball-architecture--core-specification"></a>

# Pokeball Architecture — спецификация Core

**Версия оригинала:** `1.4.0-draft`<br>
**Статус оригинала:** canonical draft<br>
**Дата оригинала:** 2026-09-05<br>
**Язык:** русский перевод; точные имена типов и полей протокола записаны латиницей

---

<a id="canonical-document-set"></a>

## Канонический набор документов

Pokeball Core — один упорядоченный канонический набор документов. Английский оригинал этой страницы — его стабильная точка входа; он задаёт версию, статус, полный перечень путей и порядок чтения. Каждая часть манифеста принадлежит тому же Core; ни одна не является отдельной спецификацией или версией, и второй собранной нормативной копии нет.

Для каждого закона единственным нормативным источником остаётся отмеченная запись `Source clause for PBA-xx` в нумерованном основном тексте. Сгенерированные законы, индексы, тесты, списки проверок, примеры, статьи глоссария, публичные обзоры и Agent Pack остаются производными представлениями. Расхождение исправляют в пользу исходной записи.

<!-- pkb-translation:core-manifest:start -->
- [Точка входа Core](pokeball-architecture-core.md)
- [Статус, область и цели](core/00-status-scope-goals.md)
- [Модель, границы и зоны](core/03-model-boundaries-zones.md)
- [Алгебра протокола](core/06-protocol-algebra.md)
- [Состояние и полномочия](core/07-state-and-authority.md)
- [Решение и принятие](core/08-decision-and-acceptance.md)
- [Асинхронность и доставка](core/09-asynchrony-and-delivery.md)
- [Композиция системы](core/10-system-composition.md)
- [Безопасность и приватность](core/11-security-and-privacy.md)
- [Профили, лимиты и производительность](core/12-profiles-and-limits.md)
- [Манифест и организация исходного кода](core/14-manifest-and-organization.md)
- [Пример поиска в каталоге](core/examples/15-catalog-search.md)
- [Checkout Flow: модель и вход](core/examples/16-01-checkout-model-and-ingress.md)
- [Checkout Flow: исполнение](core/examples/16-02-checkout-execution.md)
- [Checkout Flow: восстановление и статус](core/examples/16-03-checkout-recovery-and-status.md)
- [Проверка: тесты переходов и свойств](core/verification/17-01-transition-and-property-tests.md)
- [Проверка: тесты границ и архитектуры](core/verification/17-02-boundary-and-architecture-tests.md)
- [Проверка: тесты профилей, безопасности и заявлений о гарантиях](core/verification/17-03-profile-security-and-claim-tests.md)
- [Практический список проверок и антипаттерны](core/verification/18-checklist-and-antipatterns.md)
- [Законы PBA-01–18](core/reference/20-01-laws-boundary-decision-state.md)
- [Законы PBA-19–30](core/reference/20-02-laws-delivery-composition.md)
- [Законы PBA-31–44](core/reference/20-03-laws-security-bounds-claims.md)
- [Применимость законов и навигационный индекс](core/reference/20-04-law-index.md)
- [Стратегия внедрения](core/reference/21-adoption.md)
- [Глоссарий и каноническая формулировка](core/reference/22-glossary-and-statement.md)
<!-- pkb-translation:core-manifest:end -->

<a id="section-index"></a>

## Указатель разделов

| Раздел | Глава Core |
|---:|---|
| <a id="0-document-status-and-scope"></a>§0 | [Статус и область документа](core/00-status-scope-goals.md#0-document-status-and-scope) |
| <a id="1-definition-of-pokeball"></a>§1 | [Определение Pokeball](core/00-status-scope-goals.md#1-definition-of-pokeball) |
| <a id="2-goals-and-non-goals"></a>§2 | [Цели и то, что не входит в задачи](core/00-status-scope-goals.md#2-goals-and-non-goals) |
| <a id="3-canonical-model"></a>§3 | [Каноническая модель](core/03-model-boundaries-zones.md#3-canonical-model) |
| <a id="4-choosing-a-ball-boundary"></a>§4 | [Выбор границы Ball](core/03-model-boundaries-zones.md#4-choosing-a-ball-boundary) |
| <a id="5-three-logical-zones"></a>§5 | [Три логические зоны](core/03-model-boundaries-zones.md#5-three-logical-zones) |
| <a id="6-protocol-algebra"></a>§6 | [Алгебра протокола](core/06-protocol-algebra.md#6-protocol-algebra) |
| <a id="7-state-and-authority"></a>§7 | [Состояние и полномочия](core/07-state-and-authority.md#7-state-and-authority) |
| <a id="8-decision-and-commit-semantics"></a>§8 | [Семантика решения и фиксации](core/08-decision-and-acceptance.md#8-decision-and-commit-semantics) |
| <a id="9-asynchrony-causality-and-delivery-semantics"></a>§9 | [Асинхронность, причинность и семантика доставки](core/09-asynchrony-and-delivery.md#9-asynchrony-causality-and-delivery-semantics) |
| <a id="10-system-composition"></a>§10 | [Композиция системы](core/10-system-composition.md#10-system-composition) |
| <a id="11-security-and-privacy"></a>§11 | [Безопасность и приватность](core/11-security-and-privacy.md#11-security-and-privacy) |
| <a id="12-execution-profiles"></a>§12 | [Профили исполнения](core/12-profiles-and-limits.md#12-execution-profiles) |
| <a id="13-limits-budgets-and-performance"></a>§13 | [Лимиты, бюджеты и производительность](core/12-profiles-and-limits.md#13-limits-budgets-and-performance) |
| <a id="14-minimal-manifest-and-source-code-organization"></a>§14 | [Минимальный манифест и организация исходного кода](core/14-manifest-and-organization.md#14-minimal-manifest-and-source-code-organization) |
| <a id="15-end-to-end-example-i-catalog-search"></a>§15 | [Сквозной пример I: поиск в каталоге](core/examples/15-catalog-search.md#15-end-to-end-example-i-catalog-search) |
| <a id="16-end-to-end-example-ii-checkout-flow"></a>§16 | [Сквозной пример II: Checkout Flow](core/examples/16-01-checkout-model-and-ingress.md#16-end-to-end-example-ii-checkout-flow) |
| <a id="17-testing-review-and-operational-verification"></a>§17 | [Тесты, ревью и проверка в эксплуатации](core/verification/17-01-transition-and-property-tests.md#17-testing-review-and-operational-verification) |
| <a id="18-practical-checklist"></a>§18 | [Практический список проверок](core/verification/18-checklist-and-antipatterns.md#18-practical-checklist) |
| <a id="19-anti-patterns"></a>§19 | [Антипаттерны](core/verification/18-checklist-and-antipatterns.md#19-anti-patterns) |
| <a id="20-canonical-pokeball-laws"></a>§20 | [Канонические законы Pokeball](core/reference/20-01-laws-boundary-decision-state.md#20-canonical-pokeball-laws) |
| <a id="21-adoption-strategy"></a>§21 | [Стратегия внедрения](core/reference/21-adoption.md#21-adoption-strategy) |
| <a id="22-glossary"></a>§22 | [Глоссарий](core/reference/22-glossary-and-statement.md#22-glossary) |
| <a id="23-canonical-statement"></a>§23 | [Каноническая формулировка](core/reference/22-glossary-and-statement.md#23-canonical-statement) |

Заголовок `Core part`, цепочка навигации и уведомление о части канонического набора в начале каждого файла — навигационные метаданные. Нумерованные заголовки, исходные записи и их порядок сохраняют структуру архитектуры.

<a id="suggested-reading-paths"></a>

## Рекомендуемые пути чтения

- **Первая реализация и обычные изменения:** §§14.1–14.2 — минимальный контракт в исходном коде; §21.7 — ответственность и выбор пути изменения; затем только применимые нормативные пути.
- **Смысл и семантическая основа:** §§0–3.
- **Проектирование Ball, состояния, протокола и композиции:** §§4–10.
- **Выбор профиля среды исполнения:** §12.
- **Полные примеры работы:** §§15–16.
- **Реализация и ревью:** §§11, 13–14, 17–19, 21–22.
- **Полное представление законов для аудита:** §20; §20.1 даёт краткую навигацию по применимости, владельцам, источникам и тестам.

Полный аудит архитектуры идёт в порядке манифеста и охватывает каждый уникальный нормативный источник. Обычная работа следует только путям задачи из §0.5 и применимым источникам, которые они называют.
