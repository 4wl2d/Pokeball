<!-- pkb:translation source="spec/core/14-manifest-and-organization.md" -->

[Документация на русском](../../README.md) · [Содержание Core](../pokeball-architecture-core.md) · [Оригинал на английском](../../../../spec/core/14-manifest-and-organization.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../spec/pokeball-architecture-core.md).

<a id="core-part--manifest-and-source-organization"></a>

# Часть Core — Манифест и организация исходников

[Содержание Core](../pokeball-architecture-core.md) · [← Профили, пределы и производительность](12-profiles-and-limits.md) · [Пример поиска в каталоге →](examples/15-catalog-search.md)

> Перевод канонической части 10 из 24. [Точка входа в Core](../pokeball-architecture-core.md) определяет версию, статус, полный набор файлов и порядок чтения.

---

<a id="14-minimal-manifest-and-source-code-organization"></a>

## 14. Минимальный манифест и организация исходного кода

<a id="141-role-of-the-manifest"></a>

### 14.1. Роль манифеста

`BallManifest` — одно из возможных материализованных представлений архитектурного контракта `Ball`. Оно помогает развёртыванию, ревью, генерации документации, статическим проверкам и заявлениям о соответствии, но не является ни обязательным исходным файлом, ни реестром runtime. Авторитетным может быть замкнутое типизированное объявление в исходниках; инструментам СЛЕДУЕТ выводить представление манифеста, а не заставлять разработчиков повторять тот же протокол и политику.

Наличие поля манифеста не доказывает, что реализация ему соответствует. Например:

- `singleWriter: true` требует реального контроля планировщиком или хранилищем;
- `maxStructuralAllocations: 0` требует замера;
- `capability: Catalog.Search` требует конкретного ограниченного клиента, учётных данных или брокера;
- `noCrossStateReads: true` требует проверки графа зависимостей и кода.

На раннем этапе контракт МОЖЕТ быть небольшим YAML- или JSON-файлом либо типизированным объявлением в исходном коде. Полный пакет схем не входит в Core. Авторитетный источник каждого факта должен быть однозначным:

```text
owned protocol/state       -> typed Ball source or one manifest
inter-Ball routes          -> Assembly
Flow participation         -> Flow source + participant Application Surface + existing dependency references
shared profiles/limits     -> exact project/binding policy
Ball-specific differences  -> local policy delta
claim evidence             -> claim record
```

Обычный локальный Ball не нуждается в отдельном манифесте, если его используемый замкнутый протокол, владелец состояния, точка входа решения, ресурсы и действующее разрешение через конструирование/локальную область/политику статически восстанавливаются из исходников и охватывающей области. Инструмент выпуска или проверки соответствия может сгенерировать полностью разрешённое представление.

`spec.protocols` перечисляет только варианты протокола, которыми владеет сам `Ball`, если манифест служит авторитетным перечнем. Для независимо версионируемого протокола каждая непустая принадлежащая ему категория ДОЛЖНА быть объявлена прямо или разрешена ровно через одну явную авторитетную ссылку с закреплённой версией. В типизированных исходниках одной сборки перечнем служат объявления замкнутых объединений/типов; копировать их не нужно. Отсутствующая категория означает пустое замкнутое множество; отсутствие не требует пустого списка или нулевого предела. Правило одинаково для `FeatureBall` и `FlowBall`.

Каждая присутствующая собственная запись `queries` ДОЛЖНА определять одну пару `query -> result`, где оба типа полезной нагрузки принадлежат одной действующей идентичности протокола. Тип результата — полезная нагрузка канонического `ReadResult<ResultPayload>` из §6.3, когда применимо условие чтения с отметкой, а не зафиксированный `ReplyOutput` или `ProjectionOutput`. Запрос без сопоставления результата или с несколькими сопоставлениями недопустим.

Если манифест — авторитетный перечень зависимостей, каждая непустая запись `dependencies.reads` импортирует ровно одно принадлежащее получателю сопоставление запроса/результата и называет владельца полномочий получателя, владельца чтения/статуса, действующую идентичность протокола и требование вызывающей стороны к свежести/согласованности. Маршрут Assembly задаёт привязку. Вызывающая сторона не повторяет типы получателя в `spec.protocols`; необязательные поля маршрута присутствуют только при выполнении условий. Например:

```yaml
target:
  authority: Pricing
  ownedQueryMapping: { query: Pricing.GetCurrentQuote, result: Pricing.CurrentQuote }
  readAuthority: PricingQuoteAuthority
caller:
  authority: Catalog
  dependencies:
    reads:
      - targetAuthority: Pricing
        query: Pricing.GetCurrentQuote
        result: Pricing.CurrentQuote
        effectiveProtocolIdentity: "same-build:PricingQuoteProtocol"
        readAuthority: PricingQuoteAuthority
        freshnessAndConsistency: selected-target-snapshot-only
```

Если Pricing и Catalog могут версионироваться независимо, зависимость заменяет идентичность одной сборки точной версией протокола получателя, выбранной Assembly. Объявленное требование вызывающей стороны не может усилить отметку получателя до обещания согласованности нескольких источников или свежести на более поздний момент.

Если манифест — авторитетное представление композиции Flow, каждая пара Flow/участник появляется ровно один раз как кортеж `FlowParticipation` из §10.2. Application Surface участника содержит только публичные типы/точки входа, принадлежащие участнику. Каждая упомянутая зависимость чтения, команды или сигнала остаётся объявленной в своём существующем разделе зависимостей и связанной Assembly; представление участия не дублирует её идентичность протокола и не становится сообщением runtime. В примере Checkout ниже охватывающий `metadata.id: Checkout` — идентификатор манифеста для `CheckoutFlowBall` из §16, разрешающий `flowAuthority`; `flowOwnedCoordinationRef` разрешает непустое ограниченное множество `ownedCoordination`; каждая строка задаёт владельца участника, точный Application Surface и ссылки на зависимости без повторения этих общих значений.

Импортированные типы `ModuleCommand` и `ModuleResult` принадлежат получателю. Авторитетный исходник получателя или его проекция в манифест объявляет ровно одно сопоставление команды с результатом для каждой операции и статически классифицирует каждый достижимый отказ. Например, принадлежащее Payment сопоставление для принятой команды статуса может выглядеть так:

```yaml
protocols:
  commands:
    - command: Payment.GetOperationStatus
      result: Payment.ModuleResult
      refusalClassification:
        ValidationFailure: preAcceptance
        AdmissionFailure: preAcceptance
        Captured: acceptedResult
        DefinitelyNotCaptured: acceptedResult
        StillUnknown: acceptedResult
```

Конкретное сопоставление перечисляет только достижимые ответы/результаты; пустые категории не добавляются. В `dependencies.commands` полное имя `operation` вместе с явной версией протокола получателя или точной идентичностью типа одной сборки ДОЛЖНО однозначно импортировать это единственное принадлежащее получателю сопоставление и НЕ ДОЛЖНО повторно объявлять любую из полезных нагрузок как собственный тип вызывающей стороны или Flow. На независимо версионируемой границе выбранная версия ДОЛЖНА совпадать с `Assembly.consumerProtocolVersion`. Assembly связывает вход команды и возврат результата, но не имеет полномочий менять сопоставление или классификацию отказов. Внутренние варианты состояния и аспекты операции не становятся отдельными вариантами манифеста лишь потому, что хранятся в `State`.

Применимое общее защитное правило выбирается один раз своей авторитетной областью проекта/профиля/Assembly/привязки с одной точной неизменяемой ссылкой, например:

```yaml
policy:
  ref: policy:ShopApplication/local-standard@3#sha256:4f923e8b1dbfa6a628f180be90218e9ff9ed5e4a86ad6ff08b3a1c1ad5fe8a77
```

Этот URI и дайджест показывают синтаксис, а не артефакт политики, поставляемый Core. Реальное условие остаётся неразрешённым, пока использующий проект не заменит пример существующим неизменяемым артефактом в нужной области, чьи точные байты/дайджест и полномочия можно проверить. Политика выбирает только механизмы, значения, доказательства или разрешённые отличия, которые Core оставляет этой области; она не может подавить условие применения, ослабить закон или служить освобождением от требований.

Артефакт по ссылке содержит полные метаданные владения, области, механизма/значения, контроля и доказательств из §0.2. Покрываемый Ball не повторяет ссылку уровня области; он записывает только иной явный выбор или разрешённое локальное отличие. Статическое разрешение ДОЛЖНО дать один полный действующий контракт для каждого активного защитного правила; ссылка не является поиском во время исполнения.

<a id="142-minimal-core-manifest"></a>

### 14.2. Минимальный манифест Core

Этот singleton Ball одной сборки с профилем `Inline + Transient + InProcess + Standard` имеет один Intent фиксированного размера, состояние фиксированного размера, не имеет коллекций, смысловых выходов, внешних ресурсов, повторов, пути жизненного цикла/статуса и заявлений о гарантиях. Типизированные исходники/поток управления замыкают протокол и доказывают фиксированные измерения; охватывающая привязка один раз выбирает профиль по умолчанию, поэтому Ball не нужны ни `StateKey`, ни строка политики.

```yaml
apiVersion: pokeball.dev/core/v1alpha1
kind: BallManifest
metadata:
  id: Preferences
  ballKind: FeatureBall

spec:
  owns: [preferences]

  protocols:
    intents: [ThemeSelected]
```

Отсутствие `effects`, `commands`, выходов, повторов, профилей и доказательств значимо, потому что замкнутый перечень исходников доказывает отсутствие этих путей. `0`, пустой необязательный раздел, список запрещённых возможностей, перечисление жизненного цикла или объяснение `N/A` не добавляются. Действующий контракт всё равно имеет одного владельца полномочий, одного писателя, ограниченные вход/состояние/работу решения, чистое решение, атомарное принятие и отсутствие неявных полномочий.

Поскольку исходники и охватывающая привязка уже содержат авторитетные факты, этот отдельный YAML необязателен и может генерироваться.

<a id="143-manifest-for-a-flow"></a>

### 14.3. Манифест Flow

```yaml
apiVersion: pokeball.dev/core/v1alpha1
kind: BallManifest
metadata:
  id: Checkout
  ballKind: FlowBall
  protocolVersion: 1.0.0
  stateSchemaVersion: 2

spec:
  instance:
    stateKey: CheckoutOperationId
    singleWriter: true

  owns:
    - checkout.workflow

  protocols:
    intents: [CheckoutStarted, CancellationRequested]
    controlPulses: [CheckoutCommandDeliveryObserved]
    queries:
      - { query: GetCheckoutStatus, result: CheckoutStatus }
    replies: [RequestAccepted]

  flowParticipations:
    - participantAuthority: Cart
      participantApplicationSurface: [Cart.LockForCheckout, Cart.Unlock]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Cart.LockForCheckout, Cart.Unlock]
    - participantAuthority: Inventory
      participantApplicationSurface: [Inventory.Reserve, Inventory.Release]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Inventory.Reserve, Inventory.Release]
    - participantAuthority: Payment
      participantApplicationSurface: [Payment.Capture, Payment.CancelCapture, Payment.Refund, Payment.GetOperationStatus]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Payment.Capture, Payment.CancelCapture, Payment.Refund, Payment.GetOperationStatus]
    - participantAuthority: Order
      participantApplicationSurface: [Order.Confirm]
      flowOwnedCoordinationRef: Checkout.ownedCoordination
      dependencyRefs: [Order.Confirm]

  ownedCoordination:
    - lifecycle
    - orderingOrBranchJoin
    - compensationOrRecovery
    - cancellation
    - reconciliation
    - terminalOutcome

  dependencies:
    commands:
      - { operation: Cart.LockForCheckout, targetProtocolVersion: 1.0.0 }
      - { operation: Cart.Unlock, targetProtocolVersion: 1.0.0 }
      - { operation: Inventory.Reserve, targetProtocolVersion: 1.0.0 }
      - { operation: Inventory.Release, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.Capture, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.CancelCapture, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.Refund, targetProtocolVersion: 1.0.0 }
      - { operation: Payment.GetOperationStatus, targetProtocolVersion: 1.0.0 }
      - { operation: Order.Confirm, targetProtocolVersion: 1.0.0 }

  policy:
    ref: policy:ShopApplication/workflow-durable@7#sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
    overrides:
      limits:
        maxStateBytes: 262144
        maxCollectionItems: 256
        maxCommandsPerDecision: 8
        maxParallelBranches: 2
        maxOutputsPerDecision: 8
        maxCausalDepth: 16
        maxTransitionSteps: 4096
        maxCompensations: 4
        maxCapturedInputBytes: 65536
        maxRetainedDispatchStops: 10
```

Манифест прямо объявляет принадлежащие Checkout Intent `CheckoutStarted` и `CancellationRequested`, доверенное управление доставкой `CheckoutCommandDeliveryObserved`, `GetCheckoutStatus -> CheckoutStatus` и ответ `RequestAccepted`. `CheckoutStatus` остаётся единственным замкнутым содержимым §16.13; отмена клиентом проходит через Interaction. Наблюдения отделённой доставки проверяют дескриптор и доверенное происхождение. Четыре `flowParticipations` связывают Cart, Inventory, Payment и Order с Flow; Auth предоставляет контекст, а Checkout владеет координацией. Девять версионированных `dependencies.commands` импортируют принадлежащие целям отображения и смысл отказов без копирования в `spec.protocols`. Из них выводятся девять маршрутов циклов команд в §14.4. Эти конечные структурные числа описывают пример и не являются обязательными пределами ёмкости Core. Типы операций и wiring могут порождать оба представления.

Точная политика `workflow-durable` задаёт неизменные общие контракты профиля, входа, повторов, доставки, возможностей и базовых пределов. Checkout записывает только разрешённые отличия своего процесса. Показанное `maxTransitionSteps: 4096` полно только потому, что та же точная политика разрешает одну неизменяемую идентичность/версию `DecisionWorkMeter`, версию артефакта перехода, определение единицы и политику превышения по §8.3; числовое отличие не может молча заменить счётчик. Та же политика задаёт действующие числовые `maxStateBytes` и `maxOutputBytesPerDecision` Checkout через неизменяемые кортежи `BoundedByteMeasure` для каждого измерения либо замыкает любое из этих измерений статическим доказательством ограниченного типа и представления; локальный манифест не копирует общие контракты. Конечная структура процесса Checkout и его терминальные обработчики могут ограничивать всю нормальную работу без отдельного геометрического расчёта разветвления. Реальные пути повторов, сохраняемых выходов, внешних запросов и восстановления всё равно разрешают конечную ёмкость и гарантии завершения по §10.9; одного графа маршрутов недостаточно. Замкнутый протокол не содержит частного `EffectRequest`, поэтому нулевой предел эффектов или строка `not-applicable` не нужны: вся работа выполняется через объявленные команды участников. Доменный `maxRetainedDispatchStops: 10` покрывает начальный `ReplyOutput` `RequestAccepted` и девять замкнутых слотов шагов команд Checkout из §16.3, не превышает действующий `maxCollectionItems` и не становится новым обязательным пределом Core. `stateSchemaVersion: 2` фиксирует сохранённые значения процесса, добавленные в §16.3; собственные и импортированные варианты протокола, версии зависимостей и маршруты Assembly не меняются. Миграция уже хранимого состояния остаётся отдельным контрактом развёртывания или расширения по §§8.9/10.11: активное состояние v1 нельзя молча дополнять null или значениями M/S/I/P по умолчанию; привязка либо восстанавливает их из объявленных авторитетных доказательств миграции, либо запрещает обычный `decide` v2 и переводит операцию на объявленный путь карантина или ручной обработки.

<a id="144-assembly-declaration"></a>

### 14.4. Объявление Assembly

Связь производителя и потребителя принадлежит корню композиции — `Assembly`, — а не скрывается в глобальном локаторе.

```yaml
assembly: ShopApplication
routes:
  - kind: ReadDependency
    from: Catalog.PricingQuoteRead
    to: Pricing.GetCurrentQuote
    result: Pricing.CurrentQuote
    targetAuthority: Pricing
    readAuthority: PricingQuoteAuthority
    effectiveProtocolIdentity: "same-build:PricingQuoteProtocol"
    callerFreshnessAndConsistency: selected-target-snapshot-only
    binding: InProcess

  - kind: DeclaredCommandDependency
    from: Checkout.CartLockCommand
    to: Cart.LockForCheckout
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.InventoryReservationCommand
    to: Inventory.Reserve
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentCaptureCommand
    to: Payment.Capture
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.OrderConfirmationCommand
    to: Order.Confirm
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentStatusCommand
    to: Payment.GetOperationStatus
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentRefundCommand
    to: Payment.Refund
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.InventoryReleaseCommand
    to: Inventory.Release
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.CartUnlockCommand
    to: Cart.Unlock
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredCommandDependency
    from: Checkout.PaymentCancellationCommand
    to: Payment.CancelCapture
    resultTo: CheckoutFlowBall.ModuleResultPulse
    producerProtocolVersion: 1.0.0
    consumerProtocolVersion: 1.0.0

  - kind: DeclaredSignalDependency
    producer: Catalog
    signalType: CatalogSignal.ProductSelectionConfirmed
    consumer: RecommendationsReadModel
    producerProtocolVersion: 2.0.0
    consumerProtocolVersion: 2.0.0
    deliverySemantics: live-duplicate-permitting
    identityPolicy: sourceBallInstanceId+sourceCommitRevision+semanticHandle+sourceOrdinal
    idempotencyOrDedupPolicy: deduplicate-by-identity-within-retention
    orderingScope: CatalogSessionId
    limits:
      maxConsumersPerSignal: 1
      maxObservationBytes: 16384
      maxBufferedOrInFlightObservations: 64
      maxCausalDepth: 4
      maxDeliveryAttempts: 2
```

Assembly отвечает за конкретные связи. Строка `ReadDependency` связывает Catalog с точным принадлежащим Pricing сопоставлением и `PricingQuoteAuthority`; она не делает эти типы собственностью Catalog и не превращает `selected-target-snapshot-only` в обещание для нескольких источников. Каждый командный `to` выбирает принадлежащее получателю сопоставление команды/результата; общий `resultTo: CheckoutFlowBall.ModuleResultPulse` — принимающая точка входа, а не принадлежащий Checkout псевдоним результата. Проверенный `commandSource` Pulse и действующая идентичность протокола выбирают соответствующий шаг Checkout и импортированную полезную нагрузку получателя. `Ball` отвечает за нужный смысловой контракт. Runtime отвечает за механизм вызова или доставки. Ни Assembly, ни сгенерированный код маршрутов не могут создавать причинные токены, полезные нагрузки результатов, отметки, факты статуса, смысл чтения или отказа вне принятых фреймов получателя/чтения или команды из §6.

Для маршрута Catalog собственный протокол Catalog версии 2 — единственный авторитетный источник `ProductSelectionConfirmed`; Assembly владеет только парой версий производителя/потребителя и привязкой доставки. Контракт маршрута потребителя `2.0.0` принимает точную полезную нагрузку производителя как `ObservedSignal` и не переопределяет её. Один объявленный потребитель считается одной совокупной ветвью для принятого кортежа источника Signal; повторная доставка с той же идентичностью маршрута/потребителя не добавляет ветвь, а сохранённые причинная область/глубина и замкнутый граф задают или выбирают оставшийся предел §10.9.

В локальном монолите маршрут может компилироваться в прямой вызов функции. Для другого процесса тот же маршрут может использовать IPC. Это не должно менять доменную семантику.

<a id="145-recommended-directory-structure"></a>

### 14.5. Рекомендуемая структура каталогов

Начните с самой малой структуры исходников, которая показывает владение полномочиями и разрешённые вызовы. Для локальной функции, работающей только с состоянием, это может быть:

```text
features/
  order_draft.ext        # owned types, pure decision/read, serial binding
  order_draft_test.ext   # behavior and the binding's actual acceptance boundary
```

Расширение приведено для примера. Имена исходников и физические файлы выбирает проект; замкнутый смысл протокола и логические роли остаются доступными для проверки по §5. Отдельный файл Resource, интерфейс, манифест или пустой каталог не нужны, если соответствующего пути нет. Разделяйте файл, когда этого требуют реальные адаптеры, зависимости от платформы, владение или читаемость. Более крупный проект может использовать следующую структуру; это не каркас, который надо заполнить до первой функции:

```text
features/
  catalog/
    interaction/
      mobile/
      http/
      test/

    nucleus/
      protocol/
      state/
      transition/
      policy/

    resources/
      search/

    ball.yaml            # optional/generated resolved view

flows/
  checkout/
    interaction/
    nucleus/
      protocol/
      state/
      transition/
    ball.yaml            # optional/generated resolved view

application/
  assembly/
  runtime/
  observability/

foundation/
  bounded/
  security/
  time/
  tracing/
```

Физические каталоги не нормативны. Направление зависимостей нормативно:

```text
interaction -> own public interaction protocol
resources   -> own private resource protocol
nucleus     -> own state + own protocols + Ball-local Nucleus utilities + exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts + mechanical foundation
assembly    -> public protocols + explicit routes + resolved contract views
```

Строка Nucleus разрешает только собственные локальные утилиты Ball и публичные смысловые типы/точки входа, созданные владельцем. Импорты локальных утилит Ball и механического Foundation остаются обычными рёбрами графа времени компиляции, а не пятым смысловым видом зависимостей. Строка не передаёт владение и исключает чужой изменяемый State, внутренности, частные адаптеры Resource, реализации платформы/I/O, общие доменные утилиты без владельца, зеркала или повторные объявления вызывающей стороны и реэкспорт протокола. Interaction и Assembly не могут создавать импортированный контракт или бизнес-смысл.

Необязательный пакет помощников записывается за своим Ball-владельцем и логической ролью, даже если физически лежит в другом месте. Только общий механический помощник относится к `foundation/`; физическое повторное использование или размещение в каталоге никогда не стирает смысловое владение.

Запрещённые направления:

```text
interaction -> resources
resources   -> interaction
nucleus     -> UI / HTTP / SQL / filesystem / platform SDK
feature A   -> feature B internals
feature A   -> mutable state of feature B
```

<a id="146-interfaces-di-and-code-generation"></a>

### 14.6. Интерфейсы, DI и генерация кода

Pokeball не требует интерфейса для каждого класса, абстракции репозитория только ради моков или DI-контейнера времени исполнения.

Интерфейс оправдан, когда есть реальная граница:

- несколько рабочих реализаций;
- граница платформы;
- граница процесса/плагина;
- контракт публичной библиотеки;
- независимо развёртываемый адаптер;
- тестовая замена внешнего недетерминированного ресурса, а не чистого Nucleus.

Внедрение через конструктор и связывание во время компиляции разрешены. Локатор сервисов и неявный глобальный клиент запрещены для полномочий приложения.

Генерация кода МОЖЕТ создавать:

- размеченные объединения;
- полный диспетчинг;
- ограниченные фреймы выходов;
- проверки согласованности манифеста;
- диаграммы состояния;
- карты маршрутов;
- тестовые данные.

Генератор не должен становиться скрытым runtime-фреймворком. Сначала стабилизируйте смысловую модель, затем автоматизируйте повторяемую механику.

### 14.7. Foundation Quarantine

<!-- pkb-translation:pba-source:start id="PBA-43" title="Foundation Quarantine" -->
**Исходное положение PBA-43 — Карантин Foundation.**

- **Правило:** если общий foundation есть, он содержит только механические примитивы и не владеет изменяемым бизнес-смыслом, решением бизнес-политики, доменными полномочиями, выбором маршрута или скрытым состоянием обмена. Общая для нескольких Ball утилита допустима как Foundation только по этому правилу чистой механики. Код помощников с доменной или бизнес-семантикой остаётся локальным для Ball либо получает одного владельца Ball/Flow и используется через его объявленный Application Surface и протокол; он не может оставаться общей утилитой без владельца или переименовываться в Foundation.
- **Применимость:** `P`: общий foundation существует или помощник/утилита предлагается для использования более чем одним Ball.
- **Владелец объявления:** владелец foundation/общего кода проекта определяет его чисто механическую поверхность; каждый Ball/Flow сохраняет свою доменную семантику.
- **Область действия:** точная область, заданная полями «Правило» и «Применимость».
- **Владелец контроля / доказательств:** проверка зависимостей/владения/состояния/графа вызовов для локальных и общих утилит, политики, полномочий, маршрутов, локаторов сервисов и скрытого обмена.
- **Разрешение, сбой и соответствие:** разрешается по §0.2; нарушение означает несоответствие в указанной области, если Правило не задаёт более строгий локальный сбой.
- **Повторное использование и поведение без условия применения:** одна политика проекта обслуживает все Ball только для механического Foundation; доменная семантика остаётся локальной или принадлежит Ball/Flow; без общего механического кода артефакт Foundation опускается.
- **Основной путь проверки:** `§17.5`
<!-- pkb-translation:pba-source:end -->

Общий foundation содержит только стабильные механические примитивы:

```text
BoundedString / BoundedList
checked numeric operations
Revision / Deadline / Cancellation
SemanticHandle / TraceId
Secret wrapper
validation helpers
pure proof/encoding primitives
small result/error primitives
```

Доменные владельцы полномочий и универсальные бизнес-модели не относятся к foundation:

```text
User
Order
Payment
Cart
Session
Product
CommonResponse
BaseEntity
UniversalDto
```

Foundation может реализовать чистую проверку или механику ограниченных контейнеров, выбранную доверенной привязкой, но не выбирает одобренного издателя, не толкует разрешение, не выбирает маршрут Assembly, не хранит доменную запись/статус и не передаёт изменяемые факты между логическими ролями. Глобальные кеши, реестры, обратные вызовы и локаторы сервисов Foundation недопустимы, когда поведение приложения может наблюдать их как бизнес-смысл или скрытый путь.

Общий **механический** код выделяют, когда совпадают его механика, инварианты и темп изменений, а не просто структура полей. Доменная или бизнес-семантика не входит в Foundation: она остаётся локальным артефактом реализации Ball либо получает одного владельца Ball/Flow и объявленный Application Surface/протокол. Небольшое осознанное локальное дублирование дешевле доменной абстракции без владельца, на которую зависит много кода.

<a id="definition-source-records-for-14"></a>

### Исходные записи определений для §14

Эти помеченные определения — единственные исходные данные глоссария для терминов, которыми владеет этот раздел.

<!-- pkb-translation:term:start name="Foundation Quarantine" -->
**Foundation Quarantine** — правило, ограничивающее общий foundation механическими примитивами и запрещающее изменяемый бизнес-смысл, решения политики, доменные полномочия, выбор маршрутов, локаторы сервисов, скрытое состояние обмена и общую доменную/бизнес-утилиту без владельца. Доменная семантика остаётся локальной для Ball либо получает одного владельца Ball/Flow и объявленный Application Surface/протокол.
<!-- pkb-translation:term:end -->


---
