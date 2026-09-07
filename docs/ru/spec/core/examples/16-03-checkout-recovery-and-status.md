<!-- pkb:translation source="spec/core/examples/16-03-checkout-recovery-and-status.md" -->

[Документация на русском](../../../README.md) · [Содержание Core](../../pokeball-architecture-core.md) · [Оригинал на английском](../../../../../spec/core/examples/16-03-checkout-recovery-and-status.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../../spec/pokeball-architecture-core.md).

<a id="core-part--checkout-flow-recovery-and-status"></a>

# Часть Core — Checkout Flow: восстановление и статус

[Содержание Core](../../pokeball-architecture-core.md) · [← Checkout Flow: исполнение](16-02-checkout-execution.md) · [Проверка: тесты переходов и свойств →](../verification/17-01-transition-and-property-tests.md)

> Перевод канонической части 14 из 24. [Входной документ Core](../../pokeball-architecture-core.md) задаёт версию, статус, полный состав файлов и порядок чтения.

---

<a id="1610-compensation"></a>

### 16.10. Компенсация

Если после проведения платежа подтверждение заказа окончательно отклонено, Flow создаёт новые операции:

```text
SemanticHandle(operationId, Payment.Refund, "payment-refund")
SemanticHandle(operationId, Inventory.Release, "inventory-release")
SemanticHandle(operationId, Cart.Unlock, "cart-unlock")
```

Значения целей выводятся только из зафиксированного состояния Checkout:

```text
цель Payment.Refund
    = {
        paymentRef = state.retained.paymentCapture.value,
        originalPaymentHandle = state.steps.paymentCapture.handle,
        operationId = state.operationId
      }

цель Inventory.Release
    = {
        reservationRef = state.retained.inventoryReservation.value,
        originalReservationHandle = state.steps.inventoryReservation.handle,
        operationId = state.operationId
      }

цель Cart.Unlock
    = {
        cartId = state.cartSnapshot.value.cartId,
        originalLockHandle = state.steps.cartLock.handle,
        operationId = state.operationId
      }
```

Каждая команда компенсации получает `semanticHandle` из показанного закрытого слота, `idempotencyKey` из этого дескриптора и операции, срок из сохранённого срока рабочего процесса и доверенного текущего контекста, а также `CurrentActionAuthorization` для точного дескриптора компенсации. Execution Gate участника повторяет проверки действия, цели, операции и срока действия. Если конкретный контракт цели допускает компенсацию только по исходному семантическому дескриптору вместо P/I, такая форма цели на основе дескриптора должна быть версионирована и объявлена самим контрактом участника; Flow не предполагает её молча. Разрешение Capture не используется для Refund, Release или Unlock.

Каждая компенсация имеет:

- новый полный `SemanticHandle` и собственный `ModuleCommandRequest` с `sourceOrdinal`, отсчитываемым от нуля;
- собственный ключ идемпотентности;
- срок;
- авторизацию или возможность;
- исход и путь `OutcomeUnknown`.

Отказ или результат Order и сохранённые значения P/I/корзины принимаются, даже если нет авторизации компенсации; созданные шаги компенсации и весь доступный пакет выходов меняются в том же принятом кадре. Сбой процесса не может оставить зависимый выход без исходного значения или очистить значение до последнего потребителя. Отсутствующая или просроченная авторизация компенсации не запускает резервное использование неявных учётных данных: действие не создаётся, остаточная цель остаётся в состоянии, а конечный исход становится `NeedsManualReconciliation`. Компенсация не является откатом. Она может частично завершиться сбоем.

Конечные исходы могут быть следующими:

```text
Completed(orderId)
RejectedBeforeExternalCommitment
FailedCompensated
FailedPartiallyCompensated
NeedsManualReconciliation
CancelledBeforeCommitment
CancelledWithResidualEffects
```

<a id="1611-cancellation-race"></a>

### 16.11. Гонка при отмене

Отмена клиентом проходит разбор, аутентификацию и валидацию в Interaction и поступает во время платежа как собственный `Intent`:

```text
CancellationRequested(generation = 1)
```

Flow записывает запрос и отправляет объявленную команду цели:

```text
paymentCancellationHandle = SemanticHandle {
    operationId
    outputKind = Payment.CancelCapture
    localOrdinalOrName = "payment-cancellation"
}

ModuleCommandRequest {
    semanticHandle = paymentCancellationHandle
    sourceOrdinal = 0
    payload = Payment.CancelCapture(targetHandle = paymentHandle, generation = 1)
}
```

Если контракт цели считает отмену привилегированным действием, её выход использует отдельное текущее разрешение для `paymentCancellationHandle`; разрешение на проведение платежа не используется повторно и не хранится в текущем состоянии.

Одна из возможных последовательностей:

```text
результат PaymentCaptured
CancellationTooLate
```

`PaymentCaptured` содержит доказательство принятой команды, поэтому по тому же правилу §16.9 Flow атомарно сохраняет привязанный к происхождению `retained.paymentCapture`, записывает `payment.acceptance = Accepted`, `payment.outcome = Succeeded` и отмену `CancellationTooLate`; результат не отбрасывается из-за локального флага отмены. Затем применяется объявленная политика: продолжить заказ или создать новый дескриптор `Payment.Refund`, чья цель берётся из сохранённого P, а текущее разрешение имеет ключ дескриптора возврата. Ветвь отмены не оставляет P только в текущем Pulse.

Возможна и другая последовательность:

```text
CancellationAcceptedBeforeStart
```

Тогда платёж не проводится, а резерв товара и корзину можно безопасно освободить.

<a id="1612-commit-and-runtime-identity"></a>

### 16.12. Фиксация и идентичность среды исполнения

Состояние Flow хранит:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
```

Реестр среды исполнения хранит:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
    -> OutputId(out-payment-...)
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
    -> OutputId(out-inventory-...)
```

Каждый Decision источника атомарно принимает:

```text
следующее состояние рабочего процесса
новые выходы EffectRequest/ModuleCommandRequest
маркеры идемпотентности
изменения статуса операции
```

Если текущий Pulse впервые вводит значимое для решения значение M/S/I/P, его `CapturedCheckoutInput` или `VerifiedStepValue` и каждый новый выход, который от него зависит, входят в один принятый кадр. Ни одна точка сбоя не может опубликовать выход без сохранённого исходного значения или сохранить значение без полного пакета выходов. Защищённая запись выхода хранит точное неизменное разрешение с ограниченной областью, с которым выход был принят; текущее состояние хранит только цепочку происхождения субъекта и значения из §§16.3–16.4.

Затем среда исполнения отправляет выходы. Это устраняет зависимость доменного состояния от инфраструктурных ID, создаваемых при фиксации.

Результат отправки или ACK не записывается в эту фиксацию задним числом. Среда исполнения хранит механические свидетельства попытки и возвращает типизированный `CheckoutCommandDeliveryObserved` с исходным `SemanticHandle`; только следующий сериализованный Decision может изменить соответствующий `Step` и при необходимости создать выход сверки.

Отправка результата участника — отдельный путь принятого кадра. После того как участник принял `ModuleResultOutput`, его маршрут результата выводит `resultSource` из этого кадра цели и пытается доставить `ModuleResultPulse` в Checkout. Сбой до отправки результата не стирает принятый результат цели согласно выбранному профилю состояния и выходов участника. Когда этот маршрут результата сохраняется, повторяется или наблюдаем независимо, выход занимает один принадлежащий участнику слот, в котором возможна остановка доставки, с ключом `(effectiveProtocolIdentity, commandSource, resultSource)`. Исчерпание попыток записывает `DispatchStopped` цели; оно не выдумывает получение в Checkout и не задаёт аспекты принятия, исхода, отмены или статуса `Step` Checkout. Checkout остаётся в своём ранее доказанном состоянии `Pending`, `AcceptanceUnknown` или `OutcomeUnknown`, пока не придёт проверенный Pulse результата или объявленное доказательство сверки. Поздний проверенный результат может уточнить эти аспекты, не стирая сохранённый у цели факт остановки доставки.

<a id="1613-operation-status-and-reply"></a>

### 16.13. Статус операции и ответ

Начальный ответ:

```text
ReplyOutput(
    semanticHandle = SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance"),
    sourceOrdinal = 1,
    payload = RequestAccepted(operationId)
)
```

не является конечным бизнес-результатом. Клиент получает конечный статус через:

```text
GetCheckoutStatus(operationId)
```

Собственное содержимое результата замкнуто:

```text
CheckoutProgress =
    LockingCart
  | ReservingInventory
  | CapturingPayment
  | ReconcilingPaymentAcceptance
  | ReconcilingPaymentOutcome
  | ConfirmingOrder
  | Compensating

CheckoutCancellationFacet =
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected
  | CancellationUnknown

CheckoutTerminalOutcome =
    Completed(orderId)
  | RejectedBeforeExternalCommitment
  | FailedCompensated
  | FailedPartiallyCompensated
  | NeedsManualReconciliation
  | CancelledBeforeCommitment
  | CancelledWithResidualEffects

CheckoutLifecycle =
    Accepted
  | InProgress(progress: CheckoutProgress)
  | Completed(orderId)
  | Rejected(outcome: RejectedBeforeExternalCommitment)
  | Failed(outcome: FailedCompensated | FailedPartiallyCompensated | NeedsManualReconciliation)
  | Cancelled(outcome: CancelledBeforeCommitment | CancelledWithResidualEffects)
  | OutcomeUnknown(progress: CheckoutProgress)

CheckoutDispatchStop {
    semanticHandle: SemanticHandle
    reason
    attempts
    lastObservation
}

CheckoutDispatchFacet =
    NoDispatchStopped
  | OneOrMoreDispatchStopped(
        stops: BoundedSequence<CheckoutDispatchStop>
    )

CheckoutStatus =
    CheckoutNotFound(operationId)
  | CheckoutExpiredFromStatusRetention(operationId)
  | CheckoutKnownStatus(
        operationId,
        lifecycle: CheckoutLifecycle,
        cancellation: CheckoutCancellationFacet,
        dispatch: CheckoutDispatchFacet
    )
```

`CheckoutKnownStatus` сохраняет независимые аспекты жизненного цикла, отмены и отправки. `Accepted` означает принятую операцию Flow до входа в первую фазу участника. `InProgress` и `OutcomeUnknown` сохраняют текущий `CheckoutProgress`. Отображение конечных исходов тотально: `Completed(orderId)` отображается в `Completed`; `RejectedBeforeExternalCommitment` — в `Rejected`; три исхода сбоя — в `Failed`; два исхода отмены — в `Cancelled`. Поэтому переход принятого `StillUnknown` из §16.8 материализуется как `Failed(NeedsManualReconciliation)`, а не как бесконечно сверяемая запись. Отказ валидации, допуска или Decision Checkout корневого запроса возвращает только свой типизированный `BoundaryResponse` и не создаёт запись операции или статуса Checkout. Непринятие команды участника остаётся в соответствующем `Step.acceptance` уже принятой операции Flow и не становится корневым жизненным циклом.

`NoDispatchStopped` означает пустое множество. `OneOrMoreDispatchStopped.stops` непусто, содержит не больше записей, чем `maxRetainedDispatchStops` манифеста, и имеет уникальный полный `semanticHandle`; эта более строгая граница также удовлетворяет общему `maxCollectionItems`. Для текущей версии протокола сериализация следует фиксированному порядку десяти слотов выходов источника, в которых возможна остановка доставки: `cartLock`, `initialAcceptanceReply`, `inventoryReservation`, `paymentCapture`, `paymentCancellation`, `paymentReconciliation`, `orderConfirmation`, `inventoryRelease`, `paymentRefund`, `cartUnlock`; порядок поступления не влияет на результат. `initialAcceptanceReply` означает точный дескриптор `SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance")` из §16.5; остальные слоты соответствуют полям `steps` из §16.3.

Эти десять слотов относятся к доставке выходов источника Checkout. Принятый `ModuleResultOutput` участника и любой `DispatchStopped` цели для его обратного маршрута относятся к отдельным ограничениям доставки и статуса цели этого участника; они не добавляют одиннадцатый слот источника Checkout и не меняют `CheckoutStatus` напрямую.

Как точное применение правила ёмкости до принятия из §9.11 к десяти слотам Checkout, до принятия любого Decision, который впервые создаёт `SemanticOutput` с возможностью остановки доставки, план ёмкости источника и статуса резервирует слот записи для каждого нового уникального дескриптора; начальный Decision из §16.5 резервирует два слота — `cartLock` и `initialAcceptanceReply`. Число раскрытых дескрипторов за срок жизни операции не может превышать ни одно из объявленных ограничений; `N+1` отклоняет весь Decision до принятия источника и отправки. После принятия допустимое конечное наблюдение не может потеряться из-за нехватки ресурсов материализации: оно остаётся сохранённым и ожидающим до применения владельцем полномочий статуса, который может честно отставать со своей меткой, но не может вытеснить или обрезать запись. Материализатор статуса обрабатывает сбой ёмкости или размера в байтах как типизированное обратное давление или эксплуатационный сбой с повтором по политике профиля, а не как основание изменить уже принятый семантический факт.

Checkout применяет монотонное правило конфликтов §9.11 следующим образом. Слияние того же конечного наблюдения идемпотентно. Для текущей политики доставки вторая неэквивалентная запись остановки с тем же `semanticHandle` — нарушение инварианта; она не перезаписывает первую по порядку поступления, и эта политика не возобновляется неявно. Запись для другого дескриптора добавляется без удаления существующих записей. Записи остановки хранятся до объявленного перехода срока хранения статуса и могут сосуществовать с любым совместимым состоянием жизненного цикла и отмены; они не превращают его в `Failed` или `OutcomeUnknown`.

Для этого запроса объявленным владельцем полномочий статуса операции является зафиксированный `CheckoutStatusAuthority` с собственной ревизией и одним логическим писателем в аутентифицированном пространстве имён. Это проекция Checkout канонического владельца полномочий из §9.11; физическая форма хранилища и процесса остаётся в ведении проекта или привязки к среде исполнения. Он материализует изменения статуса Flow и доверенные наблюдения доставки среды исполнения, не становясь владельцем полномочий команд рабочего процесса. Его записи переходят следующим образом:

```text
отсутствие + принятая запись операции Checkout -> CheckoutKnownStatus
CheckoutKnownStatus + фиксация рабочего процесса -> обновлённый CheckoutKnownStatus
CheckoutKnownStatus + доверенное наблюдение конечного исхода доставки
    -> те же жизненный цикл/отмена + идемпотентное объединение по semanticHandle
наблюдение доставки до причинно предшествующей принятой записи/записи рабочего процесса
    -> ограниченное ожидание по operationId/semanticHandle; нет фиксации статуса с потерями
причинная запись + ожидающие наблюдения
    -> одно обновление CheckoutKnownStatus с каноническим объединением остановок
CheckoutKnownStatus + покрытые источники + пустое ожидание + истечение срока хранения
    -> маркер CheckoutExpiredFromStatusRetention
маркер истечения срока + наблюдение на покрытой позиции источника или до неё
    -> неизменённый маркер истечения срока
маркер истечения срока + истечение горизонта маркера -> отсутствие / CheckoutNotFound
```

Наблюдение доставки причинно ссылается на ранее зафиксированный выход источника, для которого возможна остановка доставки. Для командного Step тот же факт среды исполнения поступает во Flow через `CheckoutCommandDeliveryObserved(CommandDispatchStopped)`, а владельцу полномочий статуса — через его объявленный типизированный вход наблюдения доставки. Для `initialAcceptanceReply` применяется только путь владельца полномочий статуса: его доверенный `ControlPulse` согласно §6.11 привязан к точному зафиксированному кортежу источника `ReplyOutput` и не выдаёт себя за Pulse команды или бизнес-результат, поскольку доставка ответа не меняет `CheckoutState`. Конкретная привязка протокола и происхождения владельца полномочий статуса записывается в проектном наложении.

Эта таблица переходов — равное по множеству применение §9.11 в Checkout: материализатор либо применяет источники в причинном порядке, либо сохраняет пришедшее не по порядку наблюдение в ограниченном ожидающем состоянии; запись источника или наблюдение остаётся сохранённой по выбранной политике профиля до применения. Для `Transient` это обещание заканчивается со сроком жизни процесса; долговечное хранение требует привязки `SnapshotOutbox` или `EventJournal` и объявленного срока. Ключи ожидающих остановок для каждой операции ограничены `maxRetainedDispatchStops`; общую границу очереди и обратного давления задаёт конкретная привязка к среде исполнения. Маркер истечения хранения может фиксироваться только после объявленных позиций и сроков источников, покрывающих операцию, и при пустом множестве ожидания; он фиксируется до того, как известная запись может стать отсутствующей. Поэтому наблюдение на покрытых позициях или до них оставляет маркер прежним, поздний дубликат не воскрешает операцию с истёкшим сроком хранения, а принятое наблюдение не теряется в гонке с хранением в пределах заявленной гарантии профиля.

`GetCheckoutStatus -> CheckoutStatus` возвращается как `ReadResult<CheckoutStatus>` с меткой точного зафиксированного снимка `CheckoutStatusAuthority` для всех трёх внешних вариантов. Поэтому `NotFound` и истечение хранения не требуют выдуманного снимка отсутствующего экземпляра `CheckoutFlowBall`. Метка не обещает, что материализованный статус уже догнал каждый источник. Query не является `ReplyOutput` или `ProjectionOutput` и не создаёт новую фиксацию. Альтернативой остаётся текущий или долговечный канал ответа для конкретного профиля. Потеря HTTP-соединения не удаляет принятую операцию при профиле долговечности.

<a id="1614-what-the-example-demonstrates"></a>

### 16.14. Что показывает пример

- Flow появляется из-за независимых полномочий координации, а не из-за каждого межмодульного вызова.
- Feature Balls сохраняют локальные инварианты.
- Каждая цель-участник владеет одним отображением команды и результата; проверенные маршруты сохраняют принятые причинные кортежи источника и цели, а Assembly владеет только привязкой транспорта и версий.
- Захваченный вход минимален и версионирован.
- Отпечаток входящего запроса Checkout имеет один явный вывод из текущего Pulse и доверенного Context в области субъекта и пространства имён; он исключает ключ и транспортные метаданные и хранится равным атомарной записи Interaction вместе с точной проверенной версией артефакта Interaction `IV`, отличной от `transitionArtifactVersion`.
- Значения из прежних входящих запросов и результатов, нужные позднейшему Decision, хранятся как ограниченные, привязанные к происхождению значения рабочего процесса до последнего потребителя; история среды исполнения или участника не является входом решения.
- Привилегированный выход получает текущее разрешение в области действия через объявленный доверенный версионированный контекст; сырое разрешение или principal не живут в состоянии Flow, а точное зафиксированное разрешение защищено внутри записи выхода.
- ACK, принятие, результат, отмена и неизвестный исход не смешиваются.
- Причины носителя отказа до принятия остаются `RejectedBeforeAcceptance + NotExpected`; все девять обычных бизнес-отказов Checkout — принятые результаты цели, а принятый отказ остаётся `Accepted + Rejected`.
- Компенсация — новая работа, которая может завершиться сбоем, а не перемотка назад.
- Идемпотентность корня повторно доставляет исходный принятый кадр Reply для эквивалентного повтора и классифицирует конфликтующий отпечаток до создания Intent; идемпотентность цели возвращает ACK принятого кадра, пока результат ожидается, и точное доказательство принятого результата после его появления.
- Единственная сверка статуса Checkout v1 потребляет принятый `StillUnknown` в конечный `NeedsManualReconciliation`; автоматическое поколение или молчаливая перезапись поздним доказательством не подразумеваются.
- Идентичность доставки среды исполнения отделена от семантического состояния рабочего процесса.
- Отправка, ACK и неопределённость после фиксации меняют состояние Flow только через объявленный доверенный `ControlPulse`.
- Владелец полномочий статуса честно различает известную, отсутствующую и вышедшую за срок хранения операцию, хранит все ограниченные дескрипторы остановленных выходов и шагов и не смешивает исчерпание доставки с бизнес-исходом.
- Зарезервированный ID кандидата корневой операции, который не прошёл валидацию, допуск или Decision Checkout, создаёт только свой типизированный `BoundaryResponse`, но никогда — запись известного статуса Checkout; непринятие команды участника остаётся аспектом Step принятой операции.

---
