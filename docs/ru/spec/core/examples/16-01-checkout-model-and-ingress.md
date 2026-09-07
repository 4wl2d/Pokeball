<!-- pkb:translation source="spec/core/examples/16-01-checkout-model-and-ingress.md" -->

[Документация на русском](../../../README.md) · [Содержание Core](../../pokeball-architecture-core.md) · [Оригинал на английском](../../../../../spec/core/examples/16-01-checkout-model-and-ingress.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../../spec/pokeball-architecture-core.md).

<a id="core-part--checkout-flow-model-and-ingress"></a>

# Часть Core — Checkout Flow: модель и входящие запросы

[Содержание Core](../../pokeball-architecture-core.md) · [← Пример поиска по каталогу](15-catalog-search.md) · [Checkout Flow: исполнение →](16-02-checkout-execution.md)

> Перевод канонической части 12 из 24. [Входной документ Core](../../pokeball-architecture-core.md) задаёт версию, статус, полный состав файлов и порядок чтения.

---

<a id="16-end-to-end-example-ii-checkout-flow"></a>

## 16. Сквозной пример II: Checkout Flow

Для Checkout нужен отдельный `FlowBall`: у него несколько владельцев полномочий, независимый конечный исход, отмена, компенсация и сверка.

<a id="161-roles-and-flow-participants"></a>

### 16.1. Роли и участники Flow

```text
Flow authority:
CheckoutFlowBall

Supporting context authority (not a Flow participant):
AuthBall

Flow participants:
CartBall
InventoryBall
PaymentBall
OrderBall
```

В примере шесть владельцев полномочий или ролей, но ровно четыре участника Flow. `CheckoutFlowBall` владеет координацией, а `AuthBall` поставляет доверенные данные субъекта и контекста; ни один из них не участвует в собственном или необъявленном отношении Flow. Четыре разрешённых владельца полномочий участников `FlowParticipation` — ровно `Cart`, `Inventory`, `Payment` и `Order`, с девятью зависимостями маршрутов, перечисленными ниже.

Локальные контракты:

```text
Cart.LockForCheckout
Cart.Unlock

Inventory.Reserve
Inventory.Release

Payment.Capture
Payment.CancelCapture
Payment.Refund
Payment.GetOperationStatus

Order.Confirm
```

Каждая запись разрешается ровно через одно принадлежащее цели отображение команды и результата версии 1. Любой обычный бизнес-отказ в этой версии Checkout — принятый результат цели, а не носитель отказа до принятия:

| Команда | Путь принятого результата |
|---|---|
| `Cart.LockForCheckout` | Успех либо доменный отказ или конфликт блокировки. |
| `Cart.Unlock` | Успех, отказ, сбой или неизвестный исход. |
| `Inventory.Reserve` | `InventoryReserved` или `InventoryReservationRejected`. |
| `Inventory.Release` | Успех, отказ, сбой или неизвестный исход. |
| `Payment.Capture` | Платёж проведён, отклонён, завершился сбоем или имеет неизвестный исход. |
| `Payment.CancelCapture` | Отмена принята до запуска или во время исполнения, слишком поздняя отмена, отказ или неизвестный исход. |
| `Payment.Refund` | Успех, отказ, сбой или неизвестный исход. |
| `Payment.GetOperationStatus` | `Captured`, `DefinitelyNotCaptured` или `StillUnknown`. |
| `Order.Confirm` | Подтверждение либо окончательный отказ, сбой или неизвестный исход. |

`InventoryReservationRejected` всегда является принятым результатом `Inventory.Reserve` и никогда — `CommandRejectedBeforeAcceptance`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` — принятый результат новой команды `Payment.GetOperationStatus`, который описывает аспект принятия исходной `Payment.Capture`; это не отказ в команде статуса до принятия. Окончательный отказ `Order.Confirm` после проведения платежа — принятый результат Order. Позже Flow может принять собственный `RejectedBeforeExternalCommitment`, но этот исход рабочего процесса не копирует причину из носителя. Сбои валидации или допуска и любые отдельно перечисленные `DecisionRejected(BusinessRejection)` цели остаются отказами до принятия только согласно статическому версионированному отображению цели в §§6.9 и 14.1.

`CheckoutFlowBall` знает последовательность и смысл конечного исхода. Участники знают только собственные инварианты и операции.

Перечень принадлежащих Checkout поверхностей для `protocolVersion: 1.0.0` задан в `spec.protocols` манифеста §14.3. `CheckoutStarted` и `CancellationRequested` разрешаются как `Intent`, `CheckoutCommandDeliveryObserved` — как доверенный `ControlPulse` после фиксации, `GetCheckoutStatus -> CheckoutStatus` — как собственное отображение Query и результата, а `RequestAccepted` — как содержимое зафиксированного `Reply`. Именованные результаты участников, включая `CartLocked`, `InventoryReserved`, `PaymentCaptured`, исходы статуса и отмены, — импортированные варианты содержимого `ModuleResult`, принадлежащие цели и доставляемые внутри канонического `ModuleResultPulse`; это не самостоятельные варианты Pulse Checkout и не типы, принадлежащие Checkout.

<a id="162-ingress-and-idempotency"></a>

### 16.2. Входящие запросы и идемпотентность

Внешний запрос на изменение содержит:

```text
CheckoutStarted {
    cartId
    paymentMethodRef
    expectedCartVersion
    idempotencyKey
}
```

Для `CheckoutStarted start` и доверенного `AuthenticatedActorContext A` применяется одна каноническая семантическая функция:

```text
CheckoutStartFingerprintV1(start, A) =
    fingerprintV1(
        stableSubject = ScopedStableSubject(
            issuer = A.issuer,
            namespaceOrRealm = A.namespaceOrRealm,
            stableSubjectId = A.stableSubjectId
        ),
        namespace = A.namespaceOrRealm,
        operationKind = Checkout.Start,
        cartId = start.cartId,
        paymentMethodRef = start.paymentMethodRef,
        expectedCartVersion = start.expectedCartVersion
    )
```

`ScopedStableSubject` делает строковый ID субъекта однозначным в области издателя и realm согласно §11.2. `fingerprintV1` получает один помеченный типизированный кортеж в указанном порядке; конкретная привязка к среде исполнения определяет детерминированное каноническое кодирование или дайджест и версию артефакта, но не меняет набор полей или их источники. Interaction и переход Checkout используют одну и ту же `CheckoutStartFingerprintV1`, выбранную по совместимым версиям артефактов.

`start.idempotencyKey`, `RequestId`, ID трассировки, данные повторов и попыток и канал ответа не являются операндами отпечатка. Они не добавляются через хеширование всего Pulse или неявные метаданные. Interaction хранит `IdempotencyScope + start.idempotencyKey + F`, где `F = CheckoutStartFingerprintV1(start, A)`; принятый кадр Checkout должен атомарно сохранить тот же `F` в `CapturedCheckoutInput.source.ingressFingerprint`. Nucleus получает `start` как текущий `Pulse`, `A` — как доверенный `DecisionContext.actorContext`, а проверенный `IV` — как `DecisionContext.artifactVersion`; `IV` обозначает выбранный артефакт отпечатка Interaction и отличается от артефакта переходов Nucleus. Реестр или история принятых входов не читаются как скрытый вход.

После принятия корневой операции Checkout тот же ключ и тот же отпечаток в пределах объявленного допустимого срока повторов повторно доставляют доказательство исходного принятого кадра `ReplyOutput(RequestAccepted(operationId))`. Повтор сохраняет точные `BallInstanceId` Checkout, принятую ревизию, материализованный `OutputId`, `semanticHandle` ответа, `sourceOrdinal = 1`, содержимое и `OperationId`, сохранённый отпечаток входящего запроса и версию артефакта Interaction; меняется только механический `AttemptId` доставки. Второй `decide` Checkout не запускается, не создаются ревизия, принятый кадр, команда, семантический выход или источник статуса, и исходная принятая привязка субъекта, платежа и артефакта не может быть заменена. Принятый кадр ответа и запись идемпотентности хранятся не меньше этого срока повторов.

Отказ валидации, допуска или Decision корневого запроса не фиксирует соответствие операции и `OperationId`, поэтому последующий повтор не может наблюдать или вернуть отклонённую корневую операцию и может предпринять новую попытку принятия по тому же контракту идемпотентности. Тот же ключ с другим отпечатком обнаруживает только объявленный владелец полномочий входящих запросов и идемпотентности, пока хранится его конфликтующая покрытая запись и до создания любого Intent `CheckoutStarted`. Он возвращает ровно `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, сохраняет прежнюю принятую операцию и не создаёт операцию Checkout, Decision, ревизию, дескриптор, Reply, выход или источник статуса. `IdempotencyConflict` — не `DecisionRejected` и не отдельный собственный вариант протокола в манифесте.

<a id="163-flow-state"></a>

### 16.3. Состояние Flow

```text
CheckoutState {
    operationId
    phase
    cartSnapshot?: VerifiedStepValue<CheckoutCartSnapshot>
    retained {
        checkoutInput?: CapturedCheckoutInput
        inventoryReservation?: VerifiedStepValue<InventoryReservationRef>
        paymentCapture?: VerifiedStepValue<PaymentRef>
    }
    steps {
        cartLock
        inventoryReservation
        paymentCapture
        paymentCancellation?
        paymentReconciliation?
        orderConfirmation
        inventoryRelease?
        paymentRefund?
        cartUnlock?
    }
    cancellation
    deadline
    terminalOutcome?
}
```

Сохраняемые значения имеют следующие закрытые ограниченные формы:

```text
CapturedCheckoutInput {
    paymentMethodRef: PaymentMethodRef
    source {
        operationId
        ingressFingerprint
        sourceProtocolVersion
        interactionArtifactVersion
    }
    actorBinding {
        stableSubjectBindingDigest
        issuer
        namespaceOrRealm
        actorContextId?
    }
}

VerifiedStepValue<T> {
    value: T
    authorityActionHandle: SemanticHandle
    observedViaStepHandle: SemanticHandle
    authorityProtocolVersion
    observedViaProtocolVersion
    resultProvenance
}
```

`CapturedCheckoutInput.source` привязывает M к текущему `operationId`, точному выходу `CheckoutStartFingerprintV1(start, A)`, версии протокола Checkout и точной проверенной версии артефакта отпечатка Interaction `IV`, переданной текущим Context; `ingressFingerprint` побайтно равен отпечатку в атомарно принятой записи идемпотентности Interaction. `interactionArtifactVersion` не является `transitionArtifactVersion`. Сам типизированный Intent уже создан проверенной границей Interaction, а происхождение субъекта сохраняется в приведённой ниже привязке с минимальным набором полей. Ни одно поле не требует чтения реестра принятых входов после фиксации. `actorBinding` — дайджест стабильного субъекта из проверенного исходного `AuthenticatedActorContext`, а не учётные данные, сессия или неограниченный principal. Необязательный `actorContextId` допустим только как ссылка на запись издателя, которая сама не даёт полномочий и остаётся стабильной не меньше срока рабочего процесса; если издатель меняет её или её срок истекает, свежий контекст или разрешение доказывают того же субъекта через дайджест, издателя и realm. Само владение привязкой ничего не разрешает. `resultProvenance` — ограниченный дескриптор или дайджест с удалёнными закрытыми данными об уже проверенных издателе и причинных доказательствах авторитетного действия результата; это не ссылка, которую `Nucleus` позже разрешает через историю среды исполнения. Входной `sourceProtocolVersion` равен выбранной версии Checkout; версии авторитетного действия и наблюдения результата берутся из зависимостей с закреплёнными версиями. Эти поля — захваченные значения рабочего процесса, но они не делают Flow владельцем полномочий для корзины, способа оплаты, резерва или платежа: участник по-прежнему проверяет собственную цель и инварианты.

Для прямого результата участника `authorityActionHandle`, `observedViaStepHandle`, оба псевдонима версий протокола и `resultProvenance` выводятся только из проверенных `ModuleResultPulse.commandSource`, `resultSource`, `effectiveProtocolIdentity` и `issuerProvenance`. При сверке псевдонимы наблюдения выводятся из текущего Pulse результата статуса, а исходные псевдонимы авторитетного действия — из вложенного в него проверенного доказательства принятой команды. Checkout не принимает отдельно переданный конверт метаданных дескриптора и версии и не позволяет Assembly синтезировать эти псевдонимы.

Это исправление вывода меняет `transitionArtifactVersion`, но не публичную `protocolVersion: 1.0.0`, объявленную форму состояния, `stateSchemaVersion: 2` или маршруты. Сохранённую запись схемы v2, созданную прежним артефактом, нельзя считать корректной только по её форме: до обычного восстановления или `decide` развёртывание проверяет равенство сохранённого значения авторитетным принятым свидетельствам идемпотентности или выполняет объявленную детерминированную миграцию. Отсутствие свидетельств или несовпадение ведёт в карантин или на ручной путь; обычный Nucleus не получает чтение реестра. Если конкретная привязка к среде исполнения не может различить артефакты или безопасно мигрировать запись в её сохранённой форме, она увеличивает собственную версию схемы состояния согласно §10.11.

Пока зависимый переход остаётся достижимым, действуют следующие инварианты:

- `retained.checkoutInput` появляется атомарно с принятием `CheckoutStarted`; M хранится, пока достижимо решение о проведении платежа, а привязка субъекта — до последнего достижимого привилегированного решения о заказе, отмене или компенсации. Закрытая форма этого примера сохраняет всю ограниченную запись до более позднего из двух условий;
- `cartSnapshot` появляется только из проверенного результата, совпадающего с `steps.cartLock.handle`, и хранится до конечного заказа либо конечной разблокировки или сверки;
- `retained.inventoryReservation` появляется только из проверенного результата, совпадающего с `steps.inventoryReservation.handle`, и хранится до конечного заказа либо конечного освобождения резерва или сверки;
- `retained.paymentCapture` появляется только из проверенного прямого или полученного при сверке результата проведения платежа, совпадающего с исходным дескриптором платежа, и хранится до конечного заказа и конечного возврата или сверки;
- точный дубликат одного наблюдения идемпотентен; другое объявленное прямое доказательство или доказательство сверки для тех же `authorityActionHandle`, версии авторитетного действия и канонического значения служит подтверждением и не создаёт выход или перезапись; другое значение или версия авторитетного действия либо противоречащее доказательство того же авторитетного действия — нарушение инварианта или происхождения;
- значение нельзя удалять лишь потому, что зависимая команда уже выпущена: очистка допустима после последнего достижимого потребителя и принятия всех обязательных конечных или компенсационных выходов согласно объявленной политике хранения.

Все сохраняемые записи проходят выбранную меру `maxInputBytes`, `maxCapturedInputBytes`, где она применима, и выбранную меру `maxStateBytes` для всего кандидата следующего кадра. Каждое числовое измерение байтов Core разрешает свой точный кортеж из §8.3, если его не закрывает статическое доказательство для типа и представления. Значения не обрезаются: вход `N+1` отклоняется до доверенного семантического принятия или `decide`, а State `N+1` отклоняет весь Decision до принятия. Ссылки на платёж, привязка субъекта и происхождение считаются чувствительными данными и не попадают в Reply, Projection, статус или обычный журнал, метрику или трассу; статус раскрывает только типизированный жизненный цикл с удалёнными закрытыми данными.

Каждый шаг хранит семантические данные:

```text
Step {
    handle: SemanticHandle
    dispatch: NotDispatched | Dispatched | DispatchStopped
    acceptance: NotAccepted | Accepted | RejectedBeforeAcceptance | AcceptanceUnknown
    outcome: NotExpected | Pending | Succeeded | Rejected | Failed | Cancelled | OutcomeUnknown
    cancellation: NotRequested | Requested | CancellationAcceptedBeforeStart | CancellationAcceptedInProgress | CancellationTooLate | CancellationRejected | CancellationUnknown
}
```

Аспекты независимы и сохраняют следующие инварианты:

- `NotDispatched` требует `NotAccepted` и `NotExpected`;
- `AcceptanceUnknown` допустим лишь пока нет проверяемого ACK, результата или доказательства статуса;
- проверенный `ModuleResultPulse` с происхождением, подтверждающим принятие целью, сам доказывает принятие целью, переводит `NotDispatched -> Dispatched`, если политика доставки ещё не остановлена, и затем переводит принятие в `Accepted`, даже если отдельный ACK потерян;
- конечный `Succeeded | Rejected | Failed | Cancelled` требует `acceptance = Accepted`;
- `outcome = OutcomeUnknown` не означает `acceptance = AcceptanceUnknown`: эти случаи хранятся отдельно;
- наблюдение отмены не удаляет уже принятый бизнес-результат.

Наблюдения доставки и принятия после фиксации поступают во Flow только как собственный `ControlPulse`:

```text
CheckoutCommandDeliveryObserved {
    source: CausalToken   # Checkout projection of commandSource
    observationId
    observation:
        CommandDispatched(attemptId, deliveryEvidence)
      | CommandAccepted(acceptanceEvidence)
      | CommandRejectedBeforeAcceptance(reason, acceptanceEvidence)
      | CommandDeliveryAmbiguous(attemptId, ambiguityEvidence)
      | CommandDispatchStopped(reason, attempts, lastObservation)
    issuerProvenance
}
```

`CheckoutCommandDeliveryObserved.observation.CommandRejectedBeforeAcceptance` — проекция Checkout, равная по множеству полей каноническому носителю из §6.13:

- внешний `source` проецирует полный `commandSource` с минимальным набором полей; его `semanticHandle` выбирает Step Checkout;
- разрешённая операция `dependencies.commands` и пара версий Assembly проецируют `effectiveProtocolIdentity`;
- `reason` проецирует `boundaryResponse` без переклассификации;
- `acceptanceEvidence` проецирует `targetBoundaryProvenance`.

На стороне источника Checkout проверенный носитель задаёт ровно `acceptance = RejectedBeforeAcceptance` и `outcome = NotExpected`. Перенесённый `DecisionRejected(BusinessRejection)` — лишь информационная причина отказа до принятия; он никогда не задаёт `outcome = Rejected`. Напротив, проверенный `ModuleResultPulse`, чей принятый результат цели равен `Rejected(...)`, задаёт ровно `acceptance = Accepted` и `outcome = Rejected`. Свидетельства носителя и результата для одной эффективной идентичности протокола и одного кортежа источника противоречат друг другу и приводят к отказу по умолчанию.

На маршруте участника в том же стеке попытка до принятия не создаёт принятого фрейма получателя. Checkout применяет отказ через свой сериализованный Decision `ControlPulse`. Этот сохраняемый, восстанавливаемый workflow сохраняет реальные пределы причинной работы и вместимости завершений по §8.4; синхронный маршрут не добавляет обязательную схему резервирования уровней 1/2 и никогда не сбрасывает применимый бюджет.

Если `acceptanceEvidence` подделано, изменено, отсутствует, устарело или привязано к неверному кортежу цели, версии и источника, маршрут не создаёт доверенный `CheckoutCommandDeliveryObserved`, не меняет ни один аспект Step и не начинает компенсацию. Эти псевдонимы не добавляют и не опускают ни одного семантического поля, которое канонический носитель требует в этой области Checkout.

Доверенная граница среды исполнения и маршрута создаёт этот Pulse только после принятия источника и проверяет, что `source.semanticHandle` совпадает с ранее зафиксированным шагом Checkout, `observationId` и происхождение подлинны, а размер и число попыток удовлетворяют `maxInputBytes` и `maxDeliveryAttempts`. Сама фиксация источника оставляет новый шаг в `NotDispatched | NotAccepted | NotExpected`; среда исполнения не записывает аспекты `CheckoutState` напрямую.

Полный кортеж источника должен разрешаться ровно в один ранее зафиксированный `ModuleCommandRequest`; одного дескриптора без фиксации источника и порядкового номера недостаточно. `observationId` ограничен доверенным издателем и кортежем источника, остаётся тем же при повторной доставке одного наблюдения и отличается у нового; это механический идентификатор дедупликации, а не `SemanticHandle` или `AttemptId`. Запись дедупликации хранится не меньше объявленного срока повторной доставки. Каждый `reason`, свидетельство и `lastObservation` — версионированный протоколом ограниченный объект-значение с удалёнными закрытыми данными: сырое транспортное исключение, секрет или неограниченное содержимое провайдера не попадают в Pulse или статус.

Сериализованный переход применяет наблюдение монотонно:

| Наблюдение | Изменение совпадающего `Step` |
|---|---|
| `CommandDispatched` | `NotDispatched -> Dispatched`; принятие и исход не выводятся лишь из свидетельства отправки. |
| `CommandAccepted` | `NotDispatched -> Dispatched`, если отправка ещё не остановлена; `NotAccepted \| AcceptanceUnknown -> Accepted`; `NotExpected -> Pending`. |
| `CommandRejectedBeforeAcceptance` | `NotDispatched -> Dispatched`, если отправка ещё не остановлена; `NotAccepted \| AcceptanceUnknown -> RejectedBeforeAcceptance`; `NotExpected \| Pending \| OutcomeUnknown -> NotExpected`. Доказательство принятия или конечного исхода противоречит такому наблюдению и приводит к отказу по умолчанию. |
| `CommandDeliveryAmbiguous` | Допустимо только после свидетельства, что попытка пересекла объявленную точку отправки источника; `NotDispatched -> Dispatched`, а при отсутствии доказательства принятия — `NotAccepted -> AcceptanceUnknown` и `NotExpected -> Pending`. |
| `CommandDispatchStopped` | Любой незавершённый аспект отправки переходит в `DispatchStopped`; принятие, исход и отмена сохраняются, кроме случая проверенного `lastObservation = may-have-left` без доказательства принятия: тогда `NotAccepted -> AcceptanceUnknown` и `NotExpected -> Pending`. |

Тот же `observationId` с тем же каноническим содержимым идемпотентен и не создаёт новый выход. Тот же ID с другим содержимым, неизвестный или устаревший дескриптор либо несовместимые доказательства принятия и отказа — недопустимый доверенный вход или нарушение инварианта; они не применяются путём перезаписи в порядке поступления. Более слабое позднее наблюдение не отменяет доказанное принятие или результат. Поздний ACK или привязанный к происхождению `ModuleResultPulse` может уточнить принятие и исход после `DispatchStopped`, но конечный факт исчерпания текущей политики доставки сохраняется. Текущая версия протокола Checkout не возобновляет остановленную политику неявно.

Например, результат, пришедший до отдельного ACK, хранится как:

```text
dispatch = Dispatched
acceptance = Accepted
outcome = Succeeded
cancellation = CancellationTooLate
```

Результат с происхождением принятой команды доказывает принятие; состояние `acceptance = AcceptanceUnknown, outcome = Succeeded` запрещено.

<a id="164-authorization"></a>

### 16.4. Авторизация

`AuthenticatedActorContext` выдаёт доверенная граница. Nucleus проверяет политику в текущем состоянии и создаёт требования, ограниченные конкретной целью.

Для платежа:

```text
AuthorizationGrant {
    actorContextId
    issuer
    audience = PaymentBall
    action = Payment.Capture
    objectRef = checkout operation / payment method reference
    operationId
    constraints {
        amount = exact total
        currency = snapshot.currency
        paymentMethodRef
    }
    expectedObjectVersion = snapshot.quoteVersion
    issuedAt
    expiresAt
}
```

Для исходного контекста субъекта доверенная граница определяет ограниченный `actorContextId`, однозначно привязанный к проверенным `stableSubjectId + issuer + namespaceOrRealm`; Flow сохраняет только эту привязку, а не исходный principal или сессию. Каждое позднейшее решение, которое создаёт привилегированный выход, получает текущую авторизацию как версионированную часть `DecisionContext.authorizationSnapshot`:

```text
CheckoutAuthorizationSnapshot {
    actionGrants: BoundedMap<SemanticHandle, CurrentActionAuthorization>
}

CurrentActionAuthorization {
    grant: AuthorizationGrant
    subjectBindingProof
    issuerProvenance
    contextVersion
    observedAt
}
```

Карта ограничена `maxOutputsPerDecision`, её ключ — точный семантический дескриптор; она не является текущей причиной перехода. `subjectBindingProof` ограничено и доказывает, что субъект текущего разрешения соответствует сохранённым стабильному субъекту, издателю и realm; само по себе оно не является ни учётными данными, ни полномочием. Выбранный профиль безопасности и принятое проектное наложение объявляют доверенного издателя и границу выдачи или повторного введения, механизм подлинности и целостности, политику свежести и отзыва и срок действия для каждого действия. Для проведения платежа Nucleus сопоставляет разрешение с сохранёнными `actorBinding`, `paymentMethodRef`, текущими `cartSnapshot.value.total/currency/quoteVersion`, `operationId`, `paymentHandle`, аудиторией, действием, объектом и доверенным `CurrentActionAuthorization.observedAt`; цель повторяет проверку срока действия и отзыва в момент фактического исполнения. Refund, Release, Unlock и каждая другая привилегированная компенсация используют отдельную запись для собственного дескриптора действия; разрешение на проведение платежа не используется повторно и не расширяется.

Payment Execution Gate принимает разрешение только от доверенного `issuer`, объявленного профилем, проверяет подлинность и целостность, контекст субъекта, аудиторию, действие, объект, операцию и срок действия, а затем сопоставляет каждое ограниченное поле с фактическим содержимым `Payment.Capture`, отклоняя запрос при несовпадении. Изменённые `amount`, `currency`, `paymentMethodRef` или ожидаемая версия не наследуют исходную авторизацию. Отсутствующая, просроченная, устаревшая по версии или несовпадающая текущая авторизация до фиксации не создаёт привилегированный выход и следует точному переходу проведения платежа или компенсации в §§16.7/16.10; для компенсации, которую нельзя завершить, конечный исход — `NeedsManualReconciliation`. При принятии источником разрешение должно быть действительным на доверенный момент `observedAt`; цель проверяет его снова во время фактического исполнения. После фиксации выхода команды её содержимое и разрешение неизменны: позднейшее истечение срока или отказ обрабатываются через существующий типизированный путь отказа, неизвестного исхода или сверки, а не через неявное обновление или скрытую подмену разрешения.

Сырое разрешение не является полем текущего `CheckoutState`. Точное разрешение внутри принятого выхода команды хранится только в защищённой зафиксированной записи выхода на срок отправки, повторной доставки и аудита; профиль долговечности хранит ту же запись согласно своей гарантии состояния. Разрешение скрывается в статусе, журналах и трассах и удаляется по объявленной политике безопасного хранения после этого срока. Точное криптографическое кодирование остаётся деталью профиля или расширения; семантическая привязка обязательна. Flow не передаёт общий неограниченный объект principal.
