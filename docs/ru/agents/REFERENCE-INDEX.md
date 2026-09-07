<!-- pkb:translation source="docs/agents/REFERENCE-INDEX.md" -->

[Документация на русском](../README.md) · [Содержание Agent Pack](README.md) · [Оригинал на английском](../../agents/REFERENCE-INDEX.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../spec/pokeball-architecture-core.md). Пояснения, промпты и формы отчётов переведены для людей. Пути установки, схемы и контрольные суммы относятся к [исходному английскому Agent Pack](../../agents/README.md); устанавливайте и проверяйте его точные артефакты.

<a id="core-reference-index"></a>

# Указатель источников Core

> **Статус:** производный неканонический пакет. Он не определяет архитектуру и не расширяет Core. Перед использованием пройдите проверку исходного набора в [BASELINE.md](BASELINE.md). При расхождении действует каноническая спецификация.

Все определения и нормативный текст находятся в упорядоченном [каноническом наборе документов Core](../spec/pokeball-architecture-core.md); его точка входа задаёт полный перечень путей и порядок чтения. Этот файл — только указатель. Каждая ячейка `Primary source clause` ведёт к единственной исходной записи Core для данного закона. Раздел 20 — её полное представление для аудита с сохранением каждого поля; §20.1 — лишь ограниченный индекс применимости, владения, источников и путей тестирования. Проверка производных представлений сравнивает полную запись §20 и затронутые правила пакета с полной исходной записью, а §20.1 проверяет только в пределах объявленных столбцов. Полный семантический аудит один раз читает каждый уникальный нормативный источник и применимый путь тестов/списка проверок, затем механически проверяет равенство сгенерированных вариантов §§20/22, не перечитывая их смысл заново. Пути обычных задач меньше; ни один путь не утверждает, что исчерпывающая проверка уникального содержания Core проста. Покрытие указателя не означает применимость к каждому Ball: по §§0.2/20.1 Core и [AGENT-CONTRACT.md](AGENT-CONTRACT.md) определите правила `always` и правила, включаемые путём, риском или заявлением о гарантиях.

<a id="sections-023"></a>

## Разделы 0–23

| § | Название | Основной путь пакета |
|---:|---|---|
| 0 | Статус и область документа | `BASELINE`, применимость/приоритет в `AGENT-CONTRACT` |
| 1 | Определение Pokeball | `DESIGN-RUNBOOK` |
| 2 | Цели и то, что не входит в задачи | `AGENT-CONTRACT`, путь проектирования без лишних артефактов |
| 3 | Каноническая модель | `DESIGN-RUNBOOK` |
| 4 | Выбор границы Ball | `DESIGN-RUNBOOK` |
| 5 | Три логические зоны | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK` |
| 6 | Алгебра протокола | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 7 | Состояние и полномочия | `DESIGN-RUNBOOK` |
| 8 | Семантика решения и фиксации | `DESIGN-RUNBOOK`, разрешение действующих правил; пути §8.12 ниже |
| 9 | Асинхронность, причинность и семантика доставки | `ASYNC-STATUS-RUNBOOK` |
| 10 | Композиция системы | `COMPOSITION-PROFILES` |
| 11 | Безопасность и приватность | `SECURITY-LIMITS-RUNBOOK` |
| 12 | Профили исполнения | `COMPOSITION-PROFILES` |
| 13 | Лимиты, бюджеты, производительность и затраты при проектировании | `SECURITY-LIMITS-RUNBOOK`, проверки заявлений о гарантиях |
| 14 | Минимальный манифест и организация исходного кода | `MANIFEST-AND-ASSEMBLY`, правила/отличия проекта |
| 15 | Сквозной пример I: поиск в каталоге | `EXAMPLE-CROSSWALK` |
| 16 | Сквозной пример II: Checkout Flow | `EXAMPLE-CROSSWALK`, `ASYNC-STATUS-RUNBOOK` |
| 17 | Тесты, ревью и проверка в эксплуатации | обычные наборы тестов и наборы по условиям/заявлениям в `TEST-AND-REVIEW-GATES` |
| 18 | Практический список проверок | путь применимости в `TEST-AND-REVIEW-GATES` |
| 19 | Антипаттерны | Все руководства как список отрицательных проверок |
| 20 | Канонические законы Pokeball и матрица применимости | Этот указатель, `AGENT-CONTRACT`, `TRACEABILITY` |
| 21 | Стратегия внедрения | `INSTALL`, `DESIGN-RUNBOOK` без лишних артефактов |
| 22 | Глоссарий | Этот указатель |
| 23 | Каноническая формулировка | обзор `AGENT-CONTRACT` |

<a id="core-812-runtime-concern-index"></a>

## Указатель задач среды исполнения из §8.12 Core

Это лишь справочная таблица путей. Core один раз определяет `Runtime / acceptor` как техническую роль привязки к среде исполнения: применимая передача после проверки, допуск/резервирование, публикация выбранного кадра, планирование сохранённых выходов и объявленная маршрутизация проверенных технических наблюдений. Роль не владеет бизнес-схемой, правилами, разрешением, выбором результата чтения, повтора/запасного пути или прямой записью Sovereign State вне публикации принятого снимка/событий и последующего последовательного `decide`. Указатель не добавляет среду исполнения, не требует всех задач в каждом Ball и не переносит владение из точных разделов Core. Перед переходом к руководству пакета определите применимость по Core.

| # | Задача среды исполнения | Точные разделы Core | Основной путь пакета |
|---:|---|---|---|
| 1 | Причина и контекст с минимальным набором полей | §§3.3–3.4, 6.11, 8.1 | `DESIGN-RUNBOOK`; `SECURITY-LIMITS-RUNBOOK` для доверенного контекста решения/чтения; `ASYNC-STATUS-RUNBOOK` для сохранённой связи причины/контекста каждого Pulse |
| 2 | Конечные семантические и runtime-ограничения | §§8.3–8.4, 10.9, 13.1–13.2; PBA-38 | `SECURITY-LIMITS-RUNBOOK` для активированных мер; `COMPOSITION-PROFILES` для структурной конечности; `ASYNC-STATUS-RUNBOOK` для реальной ёмкости растущей работы и статуса |
| 3 | Семантические, причинные и технические идентичности | §§3.5–3.6, 9.1–9.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`; `MANIFEST-AND-ASSEMBLY` для действующей идентичности протокола |
| 4 | Предварительная проверка, резервирование и допуск | §§8.4, 8.7, 13.2 | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK`; `ASYNC-STATUS-RUNBOOK` для необходимой ёмкости выходов и завершений без потери принятого |
| 5 | Атомарное принятие Decision | §§8.5, 8.9; PBA-07 | `DESIGN-RUNBOOK`; плоский `AcceptedSnapshotDecisionFrame` и `AcceptedEventCommit` без `nextState` у Event |
| 6 | Фиксация до отправки | §8.6; PBA-08 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, включая принятый выход результата цели |
| 7 | Сохранение принятой работы | §§8.4, 8.8, 9.13, 12.4–12.6 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, `COMPOSITION-PROFILES`; остановка результата цели никогда не переписывает исход источника |
| 8 | Вызов команды, принятый возврат и отказ до принятия | §§6.8–6.13, 8.4–8.9, 9.1–9.4, 10.2/10.7/10.11; PBA-18/PBA-19 | `ASYNC-STATUS-RUNBOOK`, `MANIFEST-AND-ASSEMBLY`, `DESIGN-RUNBOOK`; локальные цель, принятие и завершение, переносимые идентичность и происхождение по триггеру, стадии ошибки и реальная ёмкость |
| 9 | Результаты, ACK, доставка и доверенные наблюдения | §§6.5, 6.9–6.11, 9.3–9.5, 9.11–9.13 | `ASYNC-STATUS-RUNBOOK`; `SECURITY-LIMITS-RUNBOOK` на границах доверия |
| 10 | Отклонения, отказы допуска и сбои среды исполнения | §§6.7, 6.13, 8.7–8.8, 13.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`; точное распределение по этапам носителя, результата, Resources, остановки и программной ошибки |
| 11 | Хранение, восстановление и миграция | §§8.9, 10.11, 12.5–12.6, 17.7 | `COMPOSITION-PROFILES`, `TEST-AND-REVIEW-GATES` |
| 12 | Локальные чтения и чтения статуса операций | §§6.3, 8.10, 9.11; PBA-30 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, `COMPOSITION-PROFILES`, `MANIFEST-AND-ASSEMBLY`; выбор команды или чтения, полный `ReadDependency` и принадлежащие цели данные для всех исходов после допуска |
| 13 | Жизненный цикл, владение и отсечение старых владельцев | §§7.6, 8.11, 12.3–12.6 | `DESIGN-RUNBOOK`, `COMPOSITION-PROFILES`, `ASYNC-STATUS-RUNBOOK`, `TEST-AND-REVIEW-GATES`; доступность чтения в `Draining` и один владелец статуса |

## PBA-01–44

<!-- pkb-translation:generated:start id="agent-pba-index" -->
| Закон | Название | Источник Core | Представление закона | Навигационный индекс | Тест Core | Правила агента |
|---|---|---|---|---|---|---|
| `PBA-01` | Граница трёх зон | §5 | §20 PBA-01 | §20.1 PBA-01 | `§17.5` | `PKB-AR-BND-001`, `PKB-AR-BND-002`, `PKB-AR-CMP-004` |
| `PBA-02` | Полярная изоляция | §5.4 | §20 PBA-02 | §20.1 PBA-02 | `§17.5` | `PKB-AR-BND-002`, `PKB-AR-CMP-004` |
| `PBA-03` | Чистый ограниченный Nucleus | §5.2 | §20 PBA-03 | §20.1 PBA-03 | `§17.1` | `PKB-AR-BND-002`, `PKB-AR-PRT-002`, `PKB-AR-DEC-001`, `PKB-AR-CMP-004`, `PKB-AR-SEC-002` |
| `PBA-04` | Замкнутый протокол | §6.14 | §20 PBA-04 | §20.1 PBA-04 | `§17.1` | `PKB-AR-PRT-001`, `PKB-AR-PRT-002`, `PKB-AR-DEC-001`, `PKB-AR-DEC-003`, `PKB-AR-SEC-001`, `PKB-AR-MAN-001` |
| `PBA-05` | Явные входы Decision | §8.1 | §20 PBA-05 | §20.1 PBA-05 | `§17.1` | `PKB-AR-PRT-002`, `PKB-AR-STA-002`, `PKB-AR-ASY-005`, `PKB-AR-SEC-002` |
| `PBA-06` | Управляемая причинность | §5.2 | §20 PBA-06 | §20.1 PBA-06 | `§17.1` | `PKB-AR-DEC-001`, `PKB-AR-CMP-001` |
| `PBA-07` | Атомарный Decision | §8.5 | §20 PBA-07 | §20.1 PBA-07 | `§17.1` | `PKB-AR-DEC-002` |
| `PBA-08` | Фиксация до отправки | §8.6 | §20 PBA-08 | §20.1 PBA-08 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-002` |
| `PBA-09` | Запрет повторного входа в переход | §8.4 | §20 PBA-09 | §20.1 PBA-09 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-003`, `PKB-AR-CMP-003`, `PKB-AR-LIM-002` |
| `PBA-10` | Атомарность при сбое | §8.8 | §20 PBA-10 | §20.1 PBA-10 | `§17.1` | `PKB-AR-DEC-003`, `PKB-AR-ASY-005` |
| `PBA-11` | Единый владелец смысловых полномочий | §7.2 | §20 PBA-11 | §20.1 PBA-11 | `§17.5` | `PKB-AR-BND-001`, `PKB-AR-STA-001` |
| `PBA-12` | Единственный писатель | §7.6 | §20 PBA-12 | §20.1 PBA-12 | `§17.6` | `PKB-AR-STA-001` |
| `PBA-13` | Изоляция State | §7.4 | §20 PBA-13 | §20.1 PBA-13 | `§17.5` | `PKB-AR-STA-001` |
| `PBA-14` | Явный вид State | §7.1 | §20 PBA-14 | §20.1 PBA-14 | `§17.1` | `PKB-AR-STA-002` |
| `PBA-15` | Смысловой дескриптор | §3.5 | §20 PBA-15 | §20.1 PBA-15 | `§17.1` | `PKB-AR-ID-001` |
| `PBA-16` | Доверенное выделение идентификаторов | §3.5 | §20 PBA-16 | §20.1 PBA-16 | `§17.4` | `PKB-AR-PRT-003`, `PKB-AR-ID-001`, `PKB-AR-ASY-005` |
| `PBA-17` | Причинность с ревизиями | §9.2 | §20 PBA-17 | §20.1 PBA-17 | `§17.1` | `PKB-AR-ID-001`, `PKB-AR-ASY-005` |
| `PBA-18` | Результат с подтверждённым происхождением | §9.1 | §20 PBA-18 | §20.1 PBA-18 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-ASY-001` |
| `PBA-19` | Разделение ACK и результата | §9.4 | §20 PBA-19 | §20.1 PBA-19 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-003`, `PKB-AR-ASY-002` |
| `PBA-20` | Неизвестный исход как самостоятельное состояние | §9.5 | §20 PBA-20 | §20.1 PBA-20 | `§17.3` | `PKB-AR-DEC-003`, `PKB-AR-ASY-002` |
| `PBA-21` | Явная идемпотентность | §9.6 | §20 PBA-21 | §20.1 PBA-21 | `§17.1` | `PKB-AR-ASY-003` |
| `PBA-22` | Стабильный логический повтор | §9.6 | §20 PBA-22 | §20.1 PBA-22 | `§17.3` | `PKB-AR-ASY-003` |
| `PBA-23` | Отмена как протокол | §9.7 | §20 PBA-23 | §20.1 PBA-23 | `§17.1` | `PKB-AR-ASY-004` |
| `PBA-24` | Владелец политики повторов | §9.9 | §20 PBA-24 | §20.1 PBA-24 | `§17.3` | `PKB-AR-ASY-003` |
| `PBA-25` | Объявленная зависимость | §10.2 | §20 PBA-25 | §20.1 PBA-25 | `§17.5` | `PKB-AR-PRT-001`, `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-CMP-001`, `PKB-AR-CMP-002`, `PKB-AR-CMP-004`, `PKB-AR-MAN-001`, `PKB-AR-MAN-002` |
| `PBA-26` | Суверенитет рабочего процесса | §10.4 | §20 PBA-26 | §20.1 PBA-26 | `§17.5` | `PKB-AR-STA-002`, `PKB-AR-CMP-002` |
| `PBA-27` | Без универсального посредника | §10.4 | §20 PBA-27 | §20.1 PBA-27 | `§17.5` | `PKB-AR-CMP-002` |
| `PBA-28` | Без реэкспорта протокола | §10.8 | §20 PBA-28 | §20.1 PBA-28 | `§17.5` | `PKB-AR-PRT-001`, `PKB-AR-CMP-001`, `PKB-AR-MAN-002` |
| `PBA-29` | Ограниченная композиция | §10.9 | §20 PBA-29 | §20.1 PBA-29 | `§17.5` | `PKB-AR-CMP-003`, `PKB-AR-LIM-002`, `PKB-AR-MAN-001` |
| `PBA-30` | Честная согласованность чтения | §8.10 | §20 PBA-30 | §20.1 PBA-30 | `§17.1` | `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-ASY-005`, `PKB-AR-CMP-003`, `PKB-AR-SEC-002` |
| `PBA-31` | Двойной карантин | §11.1 | §20 PBA-31 | §20.1 PBA-31 | `§17.4` | `PKB-AR-SEC-001` |
| `PBA-32` | Эффект, ограниченный Capability | §11.4 | §20 PBA-32 | §20.1 PBA-32 | `§17.3` | `PKB-AR-SEC-002` |
| `PBA-33` | Двойная проверка | §11.3 | §20 PBA-33 | §20.1 PBA-33 | `§17.8` | `PKB-AR-PRT-002`, `PKB-AR-SEC-002` |
| `PBA-34` | Отсутствие неявных полномочий | §11.5 | §20 PBA-34 | §20.1 PBA-34 | `§17.5` | `PKB-AR-CMP-004`, `PKB-AR-SEC-002` |
| `PBA-35` | Безопасный приёмник | §11.6 | §20 PBA-35 | §20.1 PBA-35 | `§17.3` | `PKB-AR-SEC-003` |
| `PBA-36` | Удержание секретов в заданных границах | §11.9 | §20 PBA-36 | §20.1 PBA-36 | `§17.8` | `PKB-AR-SEC-003` |
| `PBA-37` | Явный небезопасный обходной путь | §11.10 | §20 PBA-37 | §20.1 PBA-37 | `§17.8` | `PKB-AR-SEC-003` |
| `PBA-38` | Ограниченное исполнение | §8.3 | §20 PBA-38 | §20.1 PBA-38 | `§17.1` | `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-DEC-001`, `PKB-AR-ASY-005`, `PKB-AR-CMP-003`, `PKB-AR-LIM-001`, `PKB-AR-LIM-002`, `PKB-AR-MAN-001` |
| `PBA-39` | Механизм, соразмерный профилю | §0.2 | §20 PBA-39 | §20.1 PBA-39 | `§17.5` | `PKB-AR-GOV-003`, `PKB-AR-GOV-005`, `PKB-AR-PRT-003`, `PKB-AR-PRF-001`, `PKB-AR-PRF-002`, `PKB-AR-LIM-001`, `PKB-AR-MAN-001`, `PKB-AR-TST-001` |
| `PBA-40` | Отсутствие обязательных накладных расходов runtime | §13.3 | §20 PBA-40 | §20.1 PBA-40 | `§17.5` | `PKB-AR-PRF-001` |
| `PBA-41` | Подтверждённое измерением заявление | §13.4 | §20 PBA-41 | §20.1 PBA-41 | `§17.9` | `PKB-AR-GOV-003`, `PKB-AR-PRF-002`, `PKB-AR-TST-001` |
| `PBA-42` | Точные границы гарантии | §9.13 | §20 PBA-42 | §20.1 PBA-42 | `§17.7` | `PKB-AR-GOV-003`, `PKB-AR-ASY-005`, `PKB-AR-PRF-002` |
| `PBA-43` | Карантин Foundation | §14.7 | §20 PBA-43 | §20.1 PBA-43 | `§17.5` | `PKB-AR-CMP-004`, `PKB-AR-MAN-001` |
| `PBA-44` | Доверенный контекст актора | §11.2 | §20 PBA-44 | §20.1 PBA-44 | `§17.8` | `PKB-AR-PRT-002`, `PKB-AR-SEC-002` |
<!-- pkb-translation:generated:end -->

<a id="glossary-index"></a>

## Указатель глоссария

Определения читают только в §22 Core.

<!-- pkb-translation:generated:start id="agent-glossary-index" -->
| Термин | Источник определения в Core | Представление в глоссарии |
|---|---|---|
| AdmissionFailure | §6 | §22 `AdmissionFailure` |
| Applicability Trigger | §0 | §22 `Applicability Trigger` |
| Application Surface | §10 | §22 `Application Surface` |
| Assembly | §10 | §22 `Assembly` |
| AttemptId | §3 | §22 `AttemptId` |
| AuthenticatedActorContext | §11 | §22 `AuthenticatedActorContext` |
| Ball | §1 | §22 `Ball` |
| BallInstance | §1 | §22 `BallInstance` |
| BallType | §1 | §22 `BallType` |
| BoundaryResponse | §6 | §22 `BoundaryResponse` |
| BusinessRejection | §6 | §22 `BusinessRejection` |
| Capability | §11 | §22 `Capability` |
| Captured Input | §7 | §22 `Captured Input` |
| CausalToken | §9 | §22 `CausalToken` |
| Claim Record | §13 | §22 `Claim Record` |
| CommandRejectedBeforeAcceptance | §6 | §22 `CommandRejectedBeforeAcceptance` |
| Commit-before-dispatch | §8 | §22 `Commit-before-dispatch` |
| CommitId | §3 | §22 `CommitId` |
| CommitRevision | §3 | §22 `CommitRevision` |
| CommittedStateSnapshot | §6 | §22 `CommittedStateSnapshot` |
| ConsistencyStamp | §6 | §22 `ConsistencyStamp` |
| ControlPulse | §6 | §22 `ControlPulse` |
| Decision | §6 | §22 `Decision` |
| Decision Work Meter | §8 | §22 `Decision Work Meter` |
| DecisionContext | §8 | §22 `DecisionContext` |
| DeclaredCommandDependency | §10 | §22 `DeclaredCommandDependency` |
| DeclaredSignalDependency | §10 | §22 `DeclaredSignalDependency` |
| Delivery Observation | §9 | §22 `Delivery Observation` |
| Direct Control Dependency | §10 | §22 `Direct Control Dependency` |
| DispatchStopped | §9 | §22 `DispatchStopped` |
| Draining | §8 | §22 `Draining` |
| Effect | §6 | §22 `Effect` |
| Effective Guardrail | §0 | §22 `Effective Guardrail` |
| Effective Protocol Identity | §10 | §22 `Effective Protocol Identity` |
| EffectRequest | §6 | §22 `EffectRequest` |
| EphemeralState | §7 | §22 `EphemeralState` |
| EventJournal | §12 | §22 `EventJournal` |
| Execution Gate | §11 | §22 `Execution Gate` |
| Fact | §6 | §22 `Fact` |
| Feature Ball | §4 | §22 `Feature Ball` |
| Field-Minimized | §7 | §22 `Field-Minimized` |
| Flow Ball | §4 | §22 `Flow Ball` |
| FlowParticipation | §10 | §22 `FlowParticipation` |
| Foundation Quarantine | §14 | §22 `Foundation Quarantine` |
| Grant | §11 | §22 `Grant` |
| Guardrail Policy Reference | §0 | §22 `Guardrail Policy Reference` |
| Intent | §6 | §22 `Intent` |
| Interaction Hemisphere | §5 | §22 `Interaction Hemisphere` |
| Material Coordination | §4 | §22 `Material Coordination` |
| ModuleCommand | §6 | §22 `ModuleCommand` |
| ModuleCommandPulse | §6 | §22 `ModuleCommandPulse` |
| ModuleCommandRequest | §6 | §22 `ModuleCommandRequest` |
| ModuleResult | §6 | §22 `ModuleResult` |
| ModuleResultOutput | §6 | §22 `ModuleResultOutput` |
| ModuleResultPulse | §6 | §22 `ModuleResultPulse` |
| Nucleus | §5 | §22 `Nucleus` |
| ObservedSignal | §6 | §22 `ObservedSignal` |
| Operation Status Authority | §9 | §22 `Operation Status Authority` |
| OperationId | §3 | §22 `OperationId` |
| OutcomeUnknown | §9 | §22 `OutcomeUnknown` |
| OutputId | §3 | §22 `OutputId` |
| Policy Gate | §11 | §22 `Policy Gate` |
| Projection | §6 | §22 `Projection` |
| ProjectionOutput | §6 | §22 `ProjectionOutput` |
| Pulse | §6 | §22 `Pulse` |
| Query | §6 | §22 `Query` |
| Read Model Ball | §4 | §22 `Read Model Ball` |
| ReadContext | §6 | §22 `ReadContext` |
| ReadDependency | §10 | §22 `ReadDependency` |
| ReadResult | §6 | §22 `ReadResult` |
| Replica State | §7 | §22 `Replica State` |
| Reply | §6 | §22 `Reply` |
| ReplyOutput | §6 | §22 `ReplyOutput` |
| Representation Erasure | §5 | §22 `Representation Erasure` |
| RequestId | §3 | §22 `RequestId` |
| Resource Hemisphere | §5 | §22 `Resource Hemisphere` |
| RetainedContinuation | §8 | §22 `RetainedContinuation` |
| Safe Sink | §11 | §22 `Safe Sink` |
| SemanticHandle | §3 | §22 `SemanticHandle` |
| SemanticOutput | §6 | §22 `SemanticOutput` |
| Set-Equal | §0 | §22 `Set-Equal` |
| Signal | §6 | §22 `Signal` |
| SignalPublication | §6 | §22 `SignalPublication` |
| SnapshotOutbox | §12 | §22 `SnapshotOutbox` |
| Sovereign State | §7 | §22 `Sovereign State` |
| State Belt | §7 | §22 `State Belt` |
| StateKey | §7 | §22 `StateKey` |
| StorageTransactionId | §3 | §22 `StorageTransactionId` |
| Structural Allocation | §13 | §22 `Structural Allocation` |
| TimerRequest | §6 | §22 `TimerRequest` |
| TriggerAbsenceProof | §0 | §22 `TriggerAbsenceProof` |
| Trusted Boundary | §11 | §22 `Trusted Boundary` |
| Workflow Sovereignty | §10 | §22 `Workflow Sovereignty` |
| Zero Mandatory Runtime Tax | §13 | §22 `Zero Mandatory Runtime Tax` |
<!-- pkb-translation:generated:end -->

<a id="everyday-development-route"></a>

## Путь обычной разработки

§21.7 Core разделяет изменения функции приложения, работу над привязкой к среде исполнения и заявления об эксплуатации, не добавляя архитектурных ролей или обязательных артефактов. Используйте §§0.4–0.5 для изменённых путей, §5/PBA-01 — для карты ролей/связей в исходном коде, §§14.1–14.2 — для минимального контракта в исходном коде, §13.5 — для первого использования человеком и полной стоимости внедрения. Это пути задач, а не дополнительные определения PBA.
