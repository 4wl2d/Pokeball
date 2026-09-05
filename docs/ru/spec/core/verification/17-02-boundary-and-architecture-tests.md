<!-- pkb:translation source="spec/core/verification/17-02-boundary-and-architecture-tests.md" -->

[Документация на русском](../../../README.md) · [Содержание Core](../../pokeball-architecture-core.md) · [Оригинал на английском](../../../../../spec/core/verification/17-02-boundary-and-architecture-tests.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../../spec/pokeball-architecture-core.md).

<a id="core-part--verification-boundary-and-architecture-tests"></a>

# Часть Core — Проверка: тесты границ и архитектуры

[Содержание Core](../../pokeball-architecture-core.md) · [← Проверка: тесты переходов и свойств](17-01-transition-and-property-tests.md) · [Проверка: тесты профилей, безопасности и заявлений о гарантиях →](17-03-profile-security-and-claim-tests.md)

> Перевод канонической части 16 из 24. [Входной документ Core](../../pokeball-architecture-core.md) задаёт версию, статус, полный состав файлов и порядок чтения.

---

<a id="173-resource-contract-tests"></a>

### 17.3. Тесты контрактов ресурсов

Каждый существующий адаптер тестируется отдельно. Случаи ниже выбираются по достижимым путям ресурсов и рисков; например, адаптер без пути повторов, отмены, секретов или неоднозначного исполнения не получает соответствующих тестовых примеров:

- положительный пример возможностей: учётные данные с ограниченной областью, ограниченный клиент, брокер или роль либо дескриптор ОС с корнем в возможности обеспечивают точный ограниченный класс операций на реальной границе исполнения;
- отрицательный пример возможностей: учётные данные администратора вместе с `if (allowed)` либо внешне ограниченная обёртка над неограниченными полномочиями не проходят проверку;
- каждая применимая форма безопасной точки исполнения — параметризованная, структурированная, с корнем в возможности и кодированная по контексту — проверяется на границе интерпретатора или диалекта; разрешение файлового пути от корня возможности успешно, а прямой обход пути — нет;
- проверка Execution Gate непосредственно перед авторитетным исполнением покрывает каждое применимое доказательство, возможность, ограниченное поле содержимого, версию, свежесть и отзыв, конечную точку, квоту и привязку безопасной точки исполнения без второго бизнес-решения;
- отказ Execution Gate после принятой работы отображается в привязанный типизированный `Fact`, результат цели или статус, но никогда — в носитель отказа до принятия;
- проверка схемы ответа;
- поведение тайм-аута и отмены;
- передача ключа идемпотентности;
- владение повторами;
- композиция владельцев повторов: у каждого активного вида сбоя ровно один основной владелец повторов; каждый другой слой повторов отключён либо имеет конечную границу и доказательство семантической прозрачности; совокупные попытки SDK × адаптер × среда исполнения × Flow вычисляются, а конфигурация `2 × 3 × 4` с тремя владельцами, действующими как основные, не проходит проверку;
- ограничения размера ответа и распаковки;
- отображение внешних ошибок в типизированные Fact;
- отображение каждого исхода исполнителя отмены в закрытый `Fact`, принадлежащий цели `ModuleResult`, переносимый `ModuleResultPulse`, либо объявленный доверенный вариант `ControlPulse` согласно источнику;
- `OutcomeUnknown` на неоднозначных границах;
- скрытие секретов.

<a id="174-interaction-tests"></a>

### 17.4. Тесты Interaction

Для каждого присутствующего пути Interaction выберите достижимые случаи ниже. Общая политика парсера и аутентификации тестируется один раз в объявленной области; Ball тестирует своё типизированное отображение и любые отличия:

- отказ парсера;
- недопустимый вход границы возвращает `BoundaryResponse`, но не создаёт Pulse, Decision, CommitRevision, SemanticHandle или зафиксированный ReplyOutput;
- неверно сформированное значение или нарушение выбранного инварианта закрытого протокола либо типа возвращает `ValidationFailure` до Intent; то же представление, удовлетворяющее типу, но превышающее принадлежащее State или семантическому Context ограничение, достигает `decide` и возвращает `DecisionRejected(BusinessRejection)`; например, `items = 101` при принадлежащем State `maxItemsPerOrder = 100` следует последнему пути;
- намеренный перенос этого фиксированного ограничения в закрытый тип входа требует новой идентичности протокола и переводит тестовый пример к валидации до Intent только для этой версии; изменение стадии при старой идентичности не проходит проверку свидетельств совместимости;
- политика нормализации;
- поведение повторных или неизвестных полей, где это уместно;
- создание доверенной привязкой ограниченных `DecisionContext` и зависимого от субъекта `ReadContext` с минимальным набором полей, со свидетельствами утверждённого издателя либо фиксированных доверенных издателя и realm в том же стеке только при наличии условия контекста субъекта; Ball владеет семантическими схемой и толкованием, а не зависящее от субъекта решение или чтение не создаёт артефакт субъекта;
- ID поиска и выбора продукта Catalog поступают только из проверенных `reservedSemanticIds`; выбор получает один ID для своего единственного маршрутизируемого дескриптора Signal и не получает неиспользуемые поля субъекта, конфигурации или времени;
- Checkout использует одну выбранную `CheckoutStartFingerprintV1` в Interaction и переходе: доверенный начальный Context несёт точную проверенную версию артефакта Interaction `IV`; после принятия тот же ключ и тот же отпечаток повторно доставляют точный исходный принятый кадр `ReplyOutput(RequestAccepted(operationId))` и исходный `IV`, меняя только `AttemptId`; отказ корня до принятия не возвращает соответствие операции; тот же ключ при несовпадении семантики или области субъекта в покрытой записи возвращает `BoundaryResponse(ValidationFailure(IdempotencyConflict))` до Intent без семантического артефакта, а изменения ключа, RequestId, trace или ответа сами по себе не меняют отпечаток;
- запрос ID кандидата до принятия корня или после корневого отказа до принятия может вернуть только `NotFound`, доказанный пространством имён и отсутствием; после принятия тот же ID выбирает одну известную запись операции, а повтор никогда не создаёт одновременно запись отклонённого корня и принятую операцию;
- внешнее изменение или отмена после валидации создаёт ровно один объявленный `Intent`, а не `ControlPulse`; затем Catalog отклоняет `SearchCancelled(A)` до принятия, когда текущие State и ожидающий поиск принадлежат B, а точная идентичность достигает существующего перехода отмены;
- проверенный вход команды создаёт ровно один `ModuleCommandPulse` из принятого кадра источника и принадлежащего цели отображения; неверно сформированное содержимое либо непроверяемые источник или происхождение возвращают статически классифицированный ответ до принятия и никогда не создают принятый Decision цели;
- проверенный выход результата создаёт `ModuleResultPulse` только из принятого `ModuleResultOutput` цели; Assembly или сгенерированный код не могут создавать или менять любой из причинных токенов либо содержимое результата;
- успешно вычисленный ответ Query — включая любой объявленный вариант содержимого отказа, скрытия закрытых данных или нераскрытия — соответствует принадлежащему цели отображению результата, использует `ReadResult` и `ConsistencyStamp`, когда применяется условие чтения с меткой, либо идентичность области вызова в остальных случаях и не создаёт идентичность фиксации;
- статус операции возвращает случаи отсутствия, истечения хранения и известной операции внутри объявленного закрытого содержимого результата, а не через транспортный 404, `BoundaryResponse` или необъявленный второй тип результата;
- кодирование и экранирование выхода;
- корреляция запроса и ответа;
- отсутствие сырых объектов фреймворка внутри протокола Nucleus.

<a id="175-architecture-tests"></a>

### 17.5. Тесты архитектуры

Как проекция свидетельств соответствия и выпуска из отмеченного исходного положения `PBA-39`, CI проверяет применимое подмножество, выведенное из закрытого перечня. Строка о Query, статусе, асинхронности, доставке, Flow, сохранении или небезопасных полномочиях отсутствует, когда соответствующий путь отсутствует.

У сопровождения документов Core есть отдельный структурный шлюз: он разбирает ровно одну исходную запись с упорядоченными полями для каждого `PBA-01–44`, ровно один отмеченный источник для каждого из 94 терминов глоссария, разрешает каждый источник и основной маршрут §17, детерминированно отображает §§20, 20.1 и 22, сравнивает сгенерированные байты и доказывает, что вторая генерация ничего не меняет. Изменения временных копий удаляют, дублируют и переставляют записи, поля и термины; делают устаревшим источник, якорь, тест, маршрут Agent или сгенерированный блок; вводят второй реестр источников или ручное определение; воспроизводят регрессии PBA-32/35/36/42, импорта Nucleus, статуса корня только после принятия, воспроизведения корня и стадии конфликта, объёма источника PBA-10, единиц композиции, байтов выходов, дубликатов цели, устаревшей отмены, `StillUnknown`, совокупного разветвления, владения утилитами, версии Interaction Checkout, режима изменения и кадра, тотальности допущенного чтения, стадии валидации, маршрута Runtime и чтения и доменной ревизии Catalog. Каждое изменение должно провалить проверку. Эти свидетельства сопровождения публикации предотвращают расхождение проекций, но не являются ни средой исполнения приложения, ни доказательством соответствия потребляющего проекта Core.

Если вердикт соответствия или выпуска либо принятое решение об устранении неоднозначности опирается на отсутствие, CI сначала проверяет точную форму `TriggerAbsenceProof` и привязанные перечень и дайджесты. Примеры покрывают присутствующие и отсутствующие предикаты для `path-triggered`, `risk-triggered` и `claim-triggered`; попытку применения для `always`; противоречие с присутствующим заявлением о гарантии; принятое и непринятое устранение неоднозначности; неверные область, профиль, версию, ревизию или дайджест; отсутствующие, устаревшие, неразрешённые или противоречащие свидетельства; каждое перечисленное условие утраты действительности с последующей переоценкой; зависимые и не зависящие от субъекта пути Query. Обычные сборки без вердикта, зависящего от отсутствия, не создают заглушку доказательства.

Для каждой конкретной привязки к среде исполнения набор свидетельств включает карту логических ролей и рёбер границ. Физические пакеты необязательны; владение, рёбра проверки и допустимые вызовы обязательны:

| Роль или ребро | Принадлежащий смысл | Обязательная граница свидетельств |
|---|---|---|
| Роль Interaction | адаптация сырого входа и выхода, нормализация, валидация и безопасное для контекста кодирование | места адаптеров и допустимые вызовы; без политики, записи State или прямого бизнес-вызова Resource |
| Ребро Trusted Boundary | проверка источника, подлинности, целостности, версии, действительности и ограничений и создание доверенного семантического входа и контекста | места проверок и конструкторов до толкования Ball; могут размещаться вместе с кодом Interaction или маршрута, но не имеют бизнес-полномочий |
| Nucleus / Policy Gate | State, схемы протокола и контекста, бизнес-толкование, разрешение, упорядоченный Decision или результат | чистые точки входа и единственные места принятых бизнес-решений; без I/O или неявных полномочий |
| Resource / маршрут / Execution Gate | исполнение принятых действий; применимая техническая проверка; маршрутизация проверенных Fact и результатов | места возможностей, точек исполнения и проверок и причинное происхождение; без нового бизнес-выбора или прямой записи состояния |
| Assembly | выбор маршрута, пары протокола и версии, привязки и сгенерированных связей | статическая карта маршрутов; без синтеза причинного токена, содержимого, отказа, контекста или политики |
| Runtime / компонент принятия | допуск, атомарное принятие, планирование, наблюдение доставки и механический перенос статуса | места принятого кадра и записи состояния и типизированные маршруты ControlPulse; без доменного изменения вне `decide` |
| Владелец полномочий статуса или чтения | зафиксированный снимок запроса, закрытое отображение чтения, авторизация пространства имён и выбор результата | источник снимка с меткой или в области вызова и чистая точка входа чтения; без полномочий команд или скрытого I/O |
| Локальная утилита Ball / общий foundation | локальный вспомогательный код принадлежит ровно одной роли Ball; общий код только механический | проверка владения, импортов и состояния, доказывающая сохранение внутри локальной роли или механику Foundation и отвергающая общий доменный или бизнес-смысл без владельца, политику, выбор маршрута, локатор сервисов или скрытое состояние коммуникаций |

Доказательство графа вызовов привязки прослеживает конкретные или сгенерированные рёбра, соответствующие:

```text
verified origin -> Trusted Boundary verifier/constructor -> Interaction adaptation or route ingress -> Nucleus decide/read
Nucleus Accepted Decision -> acceptor -> Resource/route
accepted EffectRequest -> Execution Gate -> validated Fact -> Nucleus decide
accepted ModuleCommandRequest -> target boundary -> target decide
accepted ModuleResultOutput -> verified result route -> source decide
committed status snapshot -> pure read authority -> typed ReadResult/payload
Assembly/runtime/foundation -X-> business policy or direct Sovereign State mutation
```

Размещение в одном файле, одном стеке или сгенерированном коде может устранять представления адаптеров, только если карта ролей, граф вызовов, владение принятыми кадрами и трасса происхождения остаются механически проверяемыми. Свидетельства прослеживают каждую доверенную причину, `DecisionContext`, зависимый от субъекта `ReadContext`, `Fact`, `ModuleCommandPulse`, `ModuleResultPulse` и `ControlPulse` доставки до проверенного источника и отвергают необъявленные конструкторы, скрытые входы локатора сервисов или глобального состояния, прямые записи среды исполнения, смысл, созданный Assembly, или бизнес-коммуникации через foundation.

```text
no interaction -> resources import
no resources -> interaction import
no nucleus -> platform/I/O import
every concrete/generated implementation symbol maps to Interaction, Nucleus, Resource/route, Assembly, runtime/acceptor, status/read authority or shared-foundation role, with all permitted call edges explicit
Interaction is classified as a logical adapter role and every present Trusted Boundary as an authorized verification/construction edge; physical overlap neither merges the classifications nor transfers Nucleus policy, interpretation, or State authority
same-file and same-stack layouts preserve the role call graph and provenance trace; physical co-location is not accepted as separation evidence
no direct foreign state access
trusted binding boundary is the only constructor of non-empty DecisionContext and actor-dependent ReadContext; every field is verified, bounded and declared by the Ball-owned semantic schema
Inline deque/continuation preserves one trusted per-item Pulse-to-DecisionContext association; root context is not reused, and remaining causal budget/execution quantum/current capacity has no call edge into decide
Nucleus/Policy Gate is the only business-permission and business-result-selection authority; Resource/Execution Gate, Assembly, runtime and foundation cannot choose policy
every downstream output value is present in committed State, current Pulse or a declared field-minimized DecisionContext; no free value or decision read from inbox/outbox/runtime/participant history
every prior-Pulse value needed after a crash is retained with a bounded type and only the correlation, rollout identity, provenance and last-consumer retention required by its actual source/trust/lifetime triggers
every retained ingress fingerprint equals the atomically accepted idempotency-record fingerprint from one declared versioned function over explicit Pulse/Context operands; key and transport metadata are excluded
persisted state-shape change increments stateSchemaVersion without silently changing owned/imported protocolVersion
no feature internals import
no protocol re-export
caller Nucleus may import the exact declared target- or producer-owned Application Surface required by its closed Query/Pulse/Decision contracts without acquiring ownership
Checkout Nucleus imports exactly the Cart, Inventory, Payment, and Order participant-owned Application Surfaces and resolves all nine target-owned command/result mappings
caller-owned redeclaration or structurally identical mirror of an imported type fails
foreign mutable State, Nucleus internals, private Resource adapters, platform/I/O implementations, and protocol re-export fail
Interaction or Assembly synthesis of an imported payload, mapping, refusal, or business meaning fails
no raw external request -> ControlPulse path; every external mutation/cancellation enters through one declared Intent after Interaction validation
every post-commit runtime/route observation that changes Sovereign State resolves to one declared typed ControlPulse, exact previously committed source tuple and trusted provenance, then passes through single-writer decide; source commit and direct runtime state write cannot materialize Dispatched/ACK/ambiguity/DispatchStopped facets
closed protocol exhaustiveness
selected state profile resolves exactly one mutation function and frame: SnapshotDecisionResult + AcceptedSnapshotDecisionFrame or EventDecisionResult + AcceptedEventCommit; Event has no independent nextState and no binding implements a mandatory cross-profile union
representation or declared closed protocol/type invariant violation = pre-Intent ValidationFailure; valid typed value violating a State- or semantic-Context-owned rule = Nucleus BusinessRejection; promoting the rule into the type requires a new protocol identity
Pulse union order = Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse
SemanticOutput union order = ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest
ModuleCommandPulse fields = commandSource + effectiveProtocolIdentity + command + issuerProvenance
ModuleResultOutput fields/invariant = semanticHandle equals commandSource.semanticHandle + target sourceOrdinal + commandSource + payload
ModuleResultPulse fields = commandSource + resultSource + effectiveProtocolIdentity + result + issuerProvenance
the result-delivery key is exactly the unnamed tuple (effectiveProtocolIdentity, commandSource, resultSource)
the canonical pre-acceptance carrier has exactly commandSource + effectiveProtocolIdentity + one closed BoundaryResponse + targetBoundaryProvenance; on a same-stack target-not-accepted branch it creates no accepted target level and its source Decision consumes the transferred level-1 alternative-completion slot
no bare ModuleResult is a Pulse variant; timer firing remains a declared ControlPulse and Catalog SignalPublication/ObservedSignal remain unchanged
every non-empty Ball-owned protocol category resolves each used variant exactly once through an inline declaration or one version-pinned authoritative reference; omitted categories are empty for FeatureBall and FlowBall alike
every routed Signal resolves exactly once in the producer-owned protocol; Assembly owns route/version/delivery binding and cannot define the producer payload
every owned Query resolves exactly one result payload for the same effective protocol identity; triggered stamped reads use canonical ReadResult/ConsistencyStamp, same-stack getters use call-scope identity, and neither creates a Decision, revision, SemanticHandle or SemanticOutput
every ReadDependency resolves exactly one caller + target authority + target-owned Query/result mapping + effective protocol identity + target read/status authority + caller freshness/consistency requirement + Assembly route/binding; caller/Assembly cannot redefine payload, stamp, status fact or read meaning
wrong ReadDependency version/authority/mapping/stamp fails before semantic read and is not NotFound; pre-read failure uses an existing BoundaryResponse, while admitted read returns only the declared ReadResult, whose target-owned payload exhausts every reachable permitted/denied/redacted/non-disclosing outcome selected by the pure Policy Gate, and creates no accepted marker/Decision/revision/handle/output
ReadDependency optional fields follow existing protocol-version, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry and status triggers; same-build/static erasure proves the same contract and Query alone creates no new per-Ball read-limit field
multiple ReadDependency results are independent target snapshots and do not imply one atomic multi-source snapshot without a separate mechanism
every operation-status result payload exhaustively distinguishes only its reachable lifecycle, acceptance, cancellation, ambiguity, delivery-stop and retention variants; any present absence/expiry result is derived from a stamped declared status-authority snapshot
every status namespace resolves one committed revisioned single-writer authority; physical representation is project/binding-owned and creates neither a second command authority nor an unsupported freshness/atomicity claim
Draining rejects new logical mutations but serves every available declared Query/status Query and already-accepted completion/cancellation/status input; unavailable read failure precedes read, admitted read returns only the successfully evaluated ReadResult with a total target-owned payload, and neither creates a Decision
co-located and separate status-authority layouts each have exactly one query writer and preserve underlying command/business-fact ownership; materialization lag is reflected only by the status stamp
every status source/observation applies in declared causal order or bounded pending; equivalent duplicate is idempotent, compatible facets merge losslessly, weaker evidence does not regress, and nonequivalent same-key evidence fails closed
every reachable operation/pending/lifecycle/cancellation/result/marker/stop capacity is finite and reserved before its source acceptance; N+1 prevents acceptance, while accepted evidence is never evicted, truncated, silently dropped or rolled back
every retention marker commits only after declared source positions/horizons cover the operation and pending is empty; marker precedes absence, covered late evidence cannot resurrect the operation, and marker-horizon expiry alone permits later NotFound
accepted target ModuleResultOutput remains a target status fact across crash-before-result-dispatch; target DispatchStopped preserves the exact result-delivery tuple while source status stays Pending/Unknown until verified result/reconciliation
Catalog protocolVersion = 2.0.0, stateSchemaVersion = 2, transitionArtifactVersion = 2.0.1, and the ProductSelectionConfirmed producer/consumer route pair = 2.0.0/2.0.0
CatalogState revision is a Ball-owned domain revision distinct from acceptor-owned CommitRevision unless the binding proves exact equality while preserving both ownership meanings
Catalog ProductSelected transitions are set-equal to {Idle, Searching, Ready, Failed, OutcomeUnknown, Cancelled}; each preserves its state-specific fields/facets and emits exactly one sourceOrdinal-0 ProductSelectionConfirmed SignalPublication
CatalogState-to-CatalogView mappings are set-equal to the same six states, one case each, with no fallback/default or multiple case; GetCatalogView and every search-state Projection use that mapping
Catalog v1 persisted state enters v2 decide only through authoritative upcast; missing rejection-reason or other required evidence routes to quarantine/manual remediation and never to an invented default
for Checkout, the stopped-handle universe includes the initial RequestAccepted ReplyOutput and all nine command Step handles; cap/order/reservation/materialization and 10/11 tests cover the same exact set
every imported ModuleCommand/ModuleResult resolves through exactly one target-owned command-to-result mapping and declared dependency whose effective protocol identity matches Assembly; refusal classification is static for that version, explicit producer/consumer versions are required only across independent versioning/deployment, and imported target types are not redeclared as caller-owned
every command Assembly route binds verified target ingress and accepted-frame result return while synthesizing/modifying no commandSource, resultSource, target-owned payload, or refusal meaning; same-stack erasure proves the same accepted tuples
every read-like operation requiring accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation uses the command bridge; an ordinary non-recording read uses Query/ReadDependency and creates no target Decision/revision/output
same-stack command success has one source-to-target Direct Control Dependency, no reverse return edge, accepted levels 0/1/2, one level-1 alternative-completion reservation, and a separate level-2 result reservation; every pre-acceptance target-not-accepted branch consumes the transferred level-1 alternative in the source carrier Decision, never in an accepted target, while async handoff preserves causal scope/depth/budget without inferring the transfer
CheckoutCommandDeliveryObserved carrier aliases are set-equal to commandSource, effectiveProtocolIdentity, boundaryResponse, and targetBoundaryProvenance; all nine normal Checkout business refusals are accepted results
every example output maps to the canonical envelope/payload algebra; no orphan EffectIntent/ModuleCommandIntent/CommandId
compile-time import and Direct Control Dependency graphs are independently and unconditionally acyclic; generated inline dispatch before async handoff/yield is included
a WaiverRecord on either direct cycle records deliberate non-conformance and cannot make the architecture test pass
async feedback after a bounded handoff/yield creates no direct-control edge and is tested separately for owner, identity, finite budget, escape condition and fan-out protection
every inter-Ball edge has ReadDependency, DeclaredCommandDependency, DeclaredSignalDependency or FlowParticipation
every physical helper import has one owner/classification: Ball-local to exactly one logical role or shared mechanical Foundation; ownerless shared domain/business utilities fail, and Ball-owned shared semantics use a declared Application Surface/protocol rather than a fifth dependency kind
every physical import remains in the acyclic compile-time graph; a utility edge enters Direct Control Dependency only through another Ball's Application Surface or synchronous cross-authority control
every FlowParticipation resolves exactly one Flow authority + participant authority + participant Application Surface + non-empty bounded Flow-owned coordination set + bounded references to existing read/command/signal dependencies; each reference preserves its existing owner, effective identity, route, return binding, limits, and Assembly binding
FlowParticipation creates no protocol variant, runtime envelope, fifth route, target-type copy, participant-State authority, or duplicate dependency; Checkout has exactly four Flow participants and nine existing command routes although six authorities/roles appear in the example
every Application Surface contains only the owning Ball's deliberate public semantic types/entrypoints and excludes mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, and re-exported foreign contracts
every produced command/read/effect has a declared route or private capability binding
every boundary choice passes its §4.4 positive evidence and falsifier; Core accepts multiple decompositions only when each independently proves the same authority/invariant/lifecycle/trust/dependency rules
decision-relevant UI/transport value is committed State or explicit trusted current Pulse/DecisionContext; EphemeralState contains only non-decision mechanics
Flow exists exactly when it owns material lifecycle/ordering/branch-join/compensation-recovery-cancellation/reconciliation/terminal-outcome coordination; call count and one hop alone do not qualify
every present variable dimension resolves to one finite effective bound through static proof, an exact reusable policy, or a local declaration/delta; absent dimensions need no zero or N/A row
when maxCumulativeFanout is present, one root causal scope sums distinct accepted source-output-to-effective-route/consumer traversals across levels; terminal and converging route branches count, co-reachable branches sum, mutually exclusive alternatives share their maximum reservation, duplicate/redelivery does not increment, async handoff preserves scope, and exact N/N+1 rejects the whole over-limit Decision
every numeric maxTransitionSteps resolves one immutable versioned Decision Work Meter; equal canonical inputs under the same binding/transition artifact version/meter identity/version consume the same non-negative integral total, one decide scope is monotonic and cannot reset, exact N may complete, N+1 accepts no frame/state/revision/output/dispatch, and unlike meter identity/version, transition artifact version, or unit-definition tuples are not compared
every concrete fallible-admission profile/binding exposes one finite closed AdmissionFailure.reason union; unknown discriminator/open string fails before trusted construction, and a non-fallible profile has no empty union
every §6.13 error maps from its exact stage to only its legal carrier/result/status effect; later failure cannot rewrite validation/admission/pre-acceptance rejection/accepted result/post-acceptance Resource evidence/delivery stop/programming fault into another stage
candidate OperationId reservation followed by root validation/admission/Decision rejection creates only BoundaryResponse and no accepted operation, known status row, marker, handle, output, or reply; covered lookup may return only NotFound, while participant refusal remains an accepted source operation's step facet
root same-key/same-fingerprint retry redelivers the exact accepted ReplyOutput(RequestAccepted) source frame with only a new AttemptId; same-key/different-fingerprint returns pre-Intent ValidationFailure(IdempotencyConflict) and creates no semantic artifact
target duplicate before accepted result returns only verified ACK proof of the original target frame/pending result; after result it redelivers the exact accepted result frame, and neither path re-decides or re-executes
maxDeclaredDependenciesPerBall unit = one distinct owner-declared ReadDependency | DeclaredCommandDependency | DeclaredSignalDependency | FlowParticipation row; dependencyRefs preserve both counts and aliases/duplicates fail
maxRoutesPerFlow unit = one distinct effective Flow command/result round-trip mapping; ingress+return = one, participation/reference = zero, and Checkout = nine
maxInputBytes unit = one exact raw-or-normalized candidate-input representation with declared boundary-metadata/Context inclusion; maxStateBytes unit = complete candidate nextState semantic representation; maxOutputBytesPerDecision unit = complete ordered Decision.outputs semantic representation; one measure tuple fixes each N/N+1 and later storage/transport mechanics are excluded
Catalog stale SearchCancelled operationId mismatch accepts no Decision/revision/handle/projection/effect; protocolVersion 2.0.0 + stateSchemaVersion 2 + transitionArtifactVersion 2.0.1 remain distinct
Checkout StillUnknown on its sole status slot terminates normal v1 at NeedsManualReconciliation with outputs = [] and no implicit reopening
every policy reference is exact, acyclic, in scope, current for its selected revision, and conflict-free; wrong-version/profile/binding/environment and unauthorized overrides fail
policy references and WaiverRecords cannot suppress an inferred trigger, weaken a law, or convert a MUST/MUST NOT violation into conformance
every absence-dependent conformance/release verdict or accepted ambiguity-resolution decision has one exact TriggerAbsenceProof bound to class/anchor/scope/profile/inventory/digests/predicate/owner/invalidation; always and present triggers reject the proof, and invalidation blocks reliance until reevaluation
ordinary design/adoption and a verdict not relying on absence materialize no TriggerAbsenceProof or placeholder
adoption/pilot worksheet and selected workload/method/baseline/continue-reshape-stop thresholds are project-owned guidance only while that work exists; Core contributes no universal threshold, and negative-adoption fixtures create neither empty Balls nor conformance placeholders
the authoritative §0.1 Core-documentation rule is projected exactly: every new or changed Mermaid diagram in Core carries a local legend for semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows; omission blocks that documentation change, not an unadopted consuming-project binding
when actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection, PBA-44 resolves approved-issuer authenticity/integrity or fixed trusted same-stack issuer/realm proof even for typed inter-Ball input; actor-independent Decision/read paths resolve no actor artifact
Payment trace separates pre-decide carrier, accepted Nucleus business refusal, and post-acceptance Execution-Gate/Resource failure; the last two remain accepted target result paths and cannot be downgraded
Checkout initial Context explicitly supplies verified Interaction fingerprint artifact version IV, retained interactionArtifactVersion equals IV across retry/recovery, missing or mismatched IV fails, and transitionArtifactVersion remains distinct
every accepted action reaches its Resource/target Execution Gate immediately before execution with the triggered proof + capability + constraints + version + freshness/revocation + endpoint + quota + sink checks and no second business decision
no hidden cause/context/result constructor, direct runtime State write, Assembly-created semantic meaning, global service locator, mutable foundation communication, or route-selected foundation policy
the §14.2 minimal Inline fixture resolves only always-applicable obligations and contains no absent-path placeholders
positive and negative fixtures for path-, risk-, and claim-triggered rules respectively activate the guardrail and reject the same reachable trigger when no effective guardrail/evidence resolves
two Balls can resolve one exact shared policy without copying it; a local delta changes only an explicitly overridable field and leaves all other effective values equal
unsafe effect registry
foundation domain-type quarantine
```

Результат линтера импортов не доказывает семантическое владение или изоляцию безопасности. Он доказывает только проверяемое структурное свойство.
