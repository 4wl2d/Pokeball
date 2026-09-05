<!-- pkb:translation source="spec/core/examples/16-02-checkout-execution.md" -->

[Документация на русском](../../../README.md) · [Содержание Core](../../pokeball-architecture-core.md) · [Оригинал на английском](../../../../../spec/core/examples/16-02-checkout-execution.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../../spec/pokeball-architecture-core.md).

<a id="core-part--checkout-flow-execution"></a>

# Часть Core — Checkout Flow: исполнение

[Содержание Core](../../pokeball-architecture-core.md) · [← Checkout Flow: модель и входящие запросы](16-01-checkout-model-and-ingress.md) · [Checkout Flow: восстановление и статус →](16-03-checkout-recovery-and-status.md)

> Перевод канонической части 13 из 24. [Входной документ Core](../../pokeball-architecture-core.md) задаёт версию, статус, полный состав файлов и порядок чтения.

---

<a id="165-cart-lock-and-captured-snapshot"></a>

### 16.5. Блокировка корзины и захваченный снимок

Первое решение:

```text
Idle + currentPulse: CheckoutStarted(
    cartId,
    paymentMethodRef = M,
    expectedCartVersion,
    idempotencyKey
)
with DecisionContext(actorContext = A, artifactVersion = IV)

startFingerprint = CheckoutStartFingerprintV1(currentPulse, A)

cartLockHandle = SemanticHandle {
    operationId
    outputKind = Cart.LockForCheckout
    localOrdinalOrName = "cart-lock"
}

-> LockingCart(
       retained.checkoutInput = CapturedCheckoutInput(
           paymentMethodRef = M,
           source = {
               operationId,
               ingressFingerprint = startFingerprint,
               sourceProtocolVersion = Checkout.protocolVersion,
               interactionArtifactVersion = IV
           },
           actorBinding = stableSubjectBinding(A)
       )
   )
+ ModuleCommandRequest {
      semanticHandle = cartLockHandle
      sourceOrdinal = 0
      payload = Cart.LockForCheckout(
          cartId,
          expectedCartVersion
      )
  }
+ ReplyOutput {
      semanticHandle = SemanticHandle {
          operationId
          outputKind = Checkout.RequestAccepted
          localOrdinalOrName = "initial-acceptance"
      }
      sourceOrdinal = 1
      payload = RequestAccepted(operationId)
}
```

Здесь полный начальный Context — `DecisionContext(actorContext = A, artifactVersion = IV)`. Доверенная привязка к среде исполнения проверяет и ограничивает `IV` как точную версию артефакта отпечатка Interaction, выбранного для этого входящего запроса. `IV` отличается от `transitionArtifactVersion` Nucleus; это единственный источник сохраняемой `interactionArtifactVersion`, а отсутствующее, устаревшее, несовпадающее или недоверенное значение приводит к отказу до принятия. Interaction/Policy Gate требует доверенный действительный `A`; явные операнды `currentPulse + A` определяют `startFingerprint` без неявного захвата Context или чтения реестра. Запись идемпотентности с точным `startFingerprint`, `IV`, захваченным входом и обоими начальными выходами входит в одну авторитетную транзакцию принятия. Повтор того же ключа и отпечатка после этого принятия повторно доставляет доказательство именно этого принятого кадра `ReplyOutput(RequestAccepted(operationId))` с порядковым номером 1 и новым `AttemptId`; он не выполняет переход, не создаёт ещё один выход или ревизию и не заменяет захваченное значение другой привязкой субъекта, платежа или артефакта Interaction. Сбой до принятия создаёт только свой ответ границы и не создаёт соответствие принятой операции.

Затем маршрут Cart выполняет один канонический цикл запроса и ответа. Из принятого кадра Checkout маршрут выводит `cartLockCommandSource`, используя зафиксированные экземпляр и ревизию Checkout, `cartLockHandle` и `sourceOrdinal = 0`; Assembly не создаёт этот токен. Доверенная граница Cart проверяет принятый кадр и отображение цели и создаёт:

```text
cartCommandPulse = ModuleCommandPulse {
    commandSource = cartLockCommandSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    command = Cart.LockForCheckout(cartId, expectedCartVersion)
    issuerProvenance = verifiedCheckoutRoute
}
```

Cart принимает команду только через собственный канонический Decision. Успешный кадр цели содержит результат, принадлежащий цели:

```text
Accepted(SnapshotDecision {
    nextState = cartStateAfterLock
    outputs = [
        ModuleResultOutput {
            semanticHandle = cartLockCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = cartLockCommandSource
            payload = CartLocked(snapshot)
        }
    ]
})
```

Лишь после принятия целью маршрут результата выводит `cartLockResultSource` из этого кадра Cart и создаёт:

```text
cartResultPulse = ModuleResultPulse {
    commandSource = cartLockCommandSource
    resultSource = cartLockResultSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    result = CartLocked(snapshot)
    issuerProvenance = verifiedCartRoute
}
```

Следующий Decision Checkout потребляет этот Pulse. При успешном завершении в том же стеке Checkout резервирует слот альтернативного завершения уровня 1, принятый `decide` Cart расходует его, Cart резервирует уровень 2, а Decision результата Checkout расходует уровень 2; получаются принятые уровни `0/1/2`. Если Cart не принят, он не расходует принятый уровень, а Decision носителя Checkout расходует ту же альтернативу уровня 1. Только прямой вызов создаёт ребро `Checkout -> Cart`; ни одна ветвь возврата не создаёт ребро `Cart -> Checkout`. Сгенерированный код может устранять транспортные объекты, но должен доказывать те же принятые токены, идентичности, владение содержимым, резервирования и точки принятия.

После принятия и результата цели Flow получает снимок с минимальным набором полей:

```text
CheckoutCartSnapshot {
    cartId
    cartVersion
    lineItems: productRef + quantity
    quoteId
    quoteVersion
    total
    currency
    expiresAt
}
```

Снимок не содержит историю профиля, черновик UI или посторонние поля корзины. Он неизменен и имеет происхождение и версию. Cart остаётся владельцем полномочий для своего состояния.

<a id="166-inventory-reservation"></a>

### 16.6. Резервирование товара

```text
LockingCart(
    retained.checkoutInput = C
) + currentPulse: ModuleResultPulse {
      commandSource = cartLockCommandSource,
      resultSource = cartLockResultSource,
      effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0,
      result = CartLocked(snapshot = S),
      issuerProvenance = R_C
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

verifiedCart = VerifiedStepValue(
    value = S,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

inventoryHandle = SemanticHandle {
    operationId
    outputKind = Inventory.Reserve
    localOrdinalOrName = "inventory-reservation"
}

-> ReservingInventory(
       cartSnapshot = verifiedCart,
       retained.checkoutInput = C
   )
+ ModuleCommandRequest {
      semanticHandle = inventoryHandle
      sourceOrdinal = 0
      payload = Inventory.Reserve(
          items = S.lineItems,
          operationId
      )
  }
```

Проверенный Pulse результата `CartLocked` должен иметь `commandSource.semanticHandle = cartLockHandle`, выведенный из цели `resultSource` и закреплённую по версии идентичность Cart; привязанный к происхождению снимок, сохранённый вход Checkout и выход Inventory принимаются в одном кадре. Псевдонимы выше выводятся только из этих канонических полей и не создают альтернативного источника полномочий для дескриптора или версии. Повторная доставка использует ту же идентичность команды и идемпотентности. Flow не создаёт новый логический резерв для каждого транспортного повтора.

<a id="167-payment-capture"></a>

### 16.7. Проведение платежа

После успешного резервирования:

```text
ReservingInventory(
    cartSnapshot = RS,
    retained.checkoutInput = C
) + currentPulse: ModuleResultPulse {
      commandSource = inventoryCommandSource,
      resultSource = inventoryResultSource,
      effectiveProtocolIdentity = Inventory.Reserve@1.0.0,
      result = InventoryReserved(reservationRef = I),
      issuerProvenance = R_I
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

S = RS.value

verifiedInventory = VerifiedStepValue(
    value = I,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

paymentHandle = SemanticHandle {
    operationId
    outputKind = Payment.Capture
    localOrdinalOrName = "payment-capture"
}

captureAuthorization =
    context.authorizationSnapshot.actionGrants[paymentHandle]

-> CapturingPayment(
       cartSnapshot = RS,
       retained.checkoutInput = C,
       retained.inventoryReservation = verifiedInventory
   )
+ ModuleCommandRequest {
      semanticHandle = paymentHandle
      sourceOrdinal = 0
      payload = Payment.Capture(
          amount = S.total,
          currency = S.currency,
          paymentMethodRef = C.paymentMethodRef,
          idempotencyKey = operationId / "capture",
          grant = captureAuthorization.grant
      )
  }
```

До принятия Nucleus проверяет точные дескриптор, версии и происхождение результата, равенство ограничений субъекта, способа оплаты, суммы, валюты и версии из §16.4 и действительность разрешения на доверенный момент `captureAuthorization.observedAt`; Execution Gate повторяет проверку срока действия и отзыва при фактическом исполнении. `verifiedInventory`, следующее состояние и неизменный выход проведения платежа принимаются в одном кадре. Ни M, ни I, ни разрешение не считываются из прежнего Intent, реестра участника или среды исполнения либо исходящей очереди.

Если `captureAuthorization` отсутствует, просрочена, устарела или не совпадает, текущая версия Checkout не ждёт неявного обновления и не повторяет тот же результат как новую причину. Тот же Decision сохраняет `verifiedInventory`, не создаёт `Payment.Capture` и переходит в `Compensating`. Действительные текущие разрешения Release и Unlock из того же контекста могут атомарно создать выходы из §16.10; отсутствующее резервное разрешение не заменяется учётными данными, а оставшееся действие фиксируется как `NeedsManualReconciliation`. После доказанно успешных Release и Unlock конечный исход — `RejectedBeforeExternalCommitment`; неизвестный или неудачный остаточный исход следует обычной политике компенсации и сверки. Дубликат того же `InventoryReserved` после этого не возвращает Flow на путь проведения платежа.

<a id="target-side-payment-authorization-and-result-trace"></a>

#### Авторизация Payment на стороне цели и трасса результата

Когда Checkout принял показанный выше выход `Payment.Capture`, маршрут выводит `paymentCommandSource` из этого точного кадра источника. Доверенная граница привязки Payment проверяет принятый кортеж источника, эффективную идентичность протокола, принадлежащее цели содержимое команды, источник разрешения и происхождения, ограничения полей и байтов и каждое применимое правило действительности контекста, прежде чем создать:

```text
paymentCommandPulse = ModuleCommandPulse {
    commandSource = paymentCommandSource
    effectiveProtocolIdentity = Payment.Capture@1.0.0
    command = Payment.Capture(
        amount = S.total,
        currency = S.currency,
        paymentMethodRef = C.paymentMethodRef,
        idempotencyKey = operationId / "capture",
        grant = captureAuthorization.grant
    )
    issuerProvenance = verifiedCheckoutRoute
}

paymentContext = DecisionContext {
    actorContext = verifiedFieldMinimizedActorContext
    authorizationSnapshot = VerifiedCaptureAuthorization {
        actionHandle = paymentHandle
        actorBinding = C.actorBinding
        grantBinding = captureAuthorization.grant
        issuerProvenance = captureAuthorization.issuerProvenance
        contextVersion = captureAuthorization.contextVersion
    }
    trustedTimeObservation = captureAuthorization.observedAt
}
```

`PaymentBall` владеет схемой и семантическим толкованием `paymentContext`; граница привязки лишь проверяет и ограничивает объявленные поля. Nucleus Payment получает `paymentCommandPulse` как текущую причину, применяет Policy Gate на основе State Payment и этого проверенного контекста и единолично решает, разрешено ли действие бизнес-правилами.

Если разрешение и инварианты проверены успешно, Payment принимает принадлежащую цели операцию и её неизменное действие провайдера в одном кадре цели:

```text
Accepted(SnapshotDecision {
    nextState = paymentState.recordCaptureAccepted(
        commandSource = paymentCommandSource,
        operationId,
        amount = S.total,
        currency = S.currency
    )
    outputs = [
        EffectRequest {
            semanticHandle = paymentHandle
            sourceOrdinal = 0
            payload = CaptureProviderPayment(
                amount = S.total,
                currency = S.currency,
                paymentMethodRef = C.paymentMethodRef,
                providerIdempotencyKey = operationId / "capture",
                authorization = VerifiedCaptureAuthorization(...)
            )
        }
    ]
})
```

Между принятием этого кадра цели и принятием позднейшего кадра результата провайдера повторная доставка с теми же эффективной идентичностью протокола, `paymentCommandSource` и отпечатком команды получает только проверенное ACK-доказательство исходного принятого кадра Payment и его состояния ожидания результата. Payment не ждёт провайдера, не запускает `decide` повторно, не увеличивает ревизию, не создаёт промежуточный `ModuleResultOutput` и не повторяет `CaptureProviderPayment`. Сбой процесса в этом интервале восстанавливает принятый кадр и ожидающее действие согласно выбранному профилю и сохраняет тот же исход дубликата. После того как Payment принял кадр результата, тот же дубликат вместо этого повторно доставляет доказательство именно этого кадра с прежними `commandSource`, `resultSource`, ревизией цели, дескриптором и порядковым номером результата и содержимым; меняется только `AttemptId` доставки. Другой отпечаток или противоречащие свидетельства принятого кадра и результата приводят к отказу по умолчанию.

Этот Decision и ревизия цели уже приняты до исполнения у провайдера. Непосредственно перед исполнением `CaptureProviderPayment` Payment Execution Gate проверяет принятую идентичность Effect и неизменное содержимое, утверждённых издателя и целостность доказательства, минимальную возможность проведения платежа, субъекта, аудиторию, действие, объект, операцию и каждое ограниченное поле, версии доказательства, контекста и объекта, текущие свежесть, срок действия и отзыв, конечную точку провайдера, квоту и привязку к безопасной точке исполнения. Он не принимает бизнес-решений и не может заменить принятое действие другим содержимым или неявными учётными данными.

Успех провайдера, известный сбой провайдера, неопределённость или отказ Execution Gate возвращаются как закрытый `Fact`, привязанный к этому принятому `EffectRequest`. Затем Nucleus Payment принимает соответствующий кадр состояния и результата, принадлежащий цели, например:

```text
Accepted(SnapshotDecision {
    nextState = paymentState.recordCaptureFailed(
        commandSource = paymentCommandSource,
        reason = ExecutionAuthorizationFailed(reason)
    )
    outputs = [
        ModuleResultOutput {
            semanticHandle = paymentCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = paymentCommandSource
            payload = PaymentCaptureFailed(ExecutionAuthorizationFailed(reason))
        }
    ]
})
```

Та же принадлежащая цели форма результата используется для успешного `PaymentCaptured`, отказа или сбоя провайдера либо `PaymentCaptureOutcomeUnknown` согласно закрытому отображению из §16.1. Маршрут результата выводит `resultSource` из этого принятого кадра Payment и создаёт проверенный `ModuleResultPulse`; ни Execution Gate, ни Assembly не создают результат цели.

Отказ бизнес-политики Payment отличается от этого технического сбоя. Checkout v1 статически классифицирует обычный отказ в проведении платежа как принятый результат цели, поэтому Nucleus Payment принимает:

```text
Accepted(SnapshotDecision {
    nextState = paymentState
    outputs = [
        ModuleResultOutput {
            semanticHandle = paymentCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = paymentCommandSource
            payload = PaymentCaptureRejected(businessReason)
        }
    ]
})
```

Таким образом, три стадии не пересекаются:

| Стадия | Владелец полномочий и результат |
|---|---|
| До `decide` Payment | Доверенная граница цели может вернуть статически объявленный носитель `CommandRejectedBeforeAcceptance` при недопустимых источнике, протоколе, происхождении или контексте, ошибке валидации или допуска. Она не создаёт Decision, ревизию, Effect или результат Payment. |
| Policy Gate Nucleus Payment | Обычный бизнес-отказ — принятый Decision цели с принадлежащим цели `ModuleResultOutput(Rejected(...))`; аспекты источника становятся `Accepted + Rejected`. |
| После принятия целью | Сбой Execution Gate или провайдера идёт по пути Resource `Fact` или статуса, за которым следует принятый результат цели. Он не может стать носителем отказа до принятия, откатить принятую операцию Payment или ослабить свидетельство принятого результата. |

Результат платежа может поступить:

- после ACK;
- до отдельного ACK, но с происхождением принятой команды;
- как `OutcomeUnknown` после тайм-аута провайдера.

Flow не считает отсутствие ACK доказательством недоставки, если команда уже могла быть принята.

<a id="168-unknown-payment-outcome"></a>

### 16.8. Неизвестный исход платежа

Два случая неизвестности не смешиваются.

Если доставка могла выйти за границу источника, но принятие целью ещё не доказано:

```text
payment.dispatch = Dispatched
payment.acceptance = AcceptanceUnknown
payment.outcome = Pending
phase = ReconcilingPaymentAcceptance
```

Причина этого перехода — `CheckoutCommandDeliveryObserved(CommandDeliveryAmbiguous)` для совпадающего `paymentHandle`, а не фиксация источника или прямая запись среды исполнения. `CommandDispatchStopped`, чей сохранённый `lastObservation` также доказывает «могло выйти за границу источника» без доказательства принятия, сохраняет аспект остановки и начинает тот же путь сверки. Поле `steps.paymentReconciliation` гарантирует, что дубликаты или несколько неоднозначных попыток не создадут вторую команду статуса с другой семантической идентичностью.

Если цель приняла команду и вернула доказательство принятой команды, но исход провайдера потерян:

```text
payment.dispatch = Dispatched
payment.acceptance = Accepted
payment.outcome = OutcomeUnknown
phase = ReconcilingPaymentOutcome
```

Вторая форма не может вернуться к `AcceptanceUnknown`: принятие целью уже доказано. В обеих фазах Flow создаёт объявленную команду с новой идентичностью для шага статуса и сохраняет идентичность исходного проведения платежа:

```text
statusHandle = SemanticHandle {
    operationId
    outputKind = Payment.GetOperationStatus
    localOrdinalOrName = "payment-status"
}

ModuleCommandRequest {
    semanticHandle = statusHandle
    sourceOrdinal = 0
    payload = Payment.GetOperationStatus(
        originalPaymentHandle = paymentHandle,
        originalProviderIdempotencyKey = operationId / "capture"
    )
}
```

`Payment.GetOperationStatus` намеренно является командой, а не обычным чтением: Checkout нужны привязанный к происхождению принятый результат, стабильная идентичность шага сверки, идемпотентное повторное воспроизведение и свидетельство статуса. Для каждого нового `statusHandle` Payment принимает Decision цели с прежним состоянием и увеличивает свою ревизию:

```text
Accepted(SnapshotDecision {
    nextState = paymentState
    outputs = [
        ModuleResultOutput {
            semanticHandle = statusCommandSource.semanticHandle
            sourceOrdinal = 0
            commandSource = statusCommandSource
            payload = Captured | DefinitelyNotCaptured | StillUnknown
        }
    ]
})
```

Привязка Payment `EventJournal` записывает то же принятие как `Accepted(NoDomainChange { outputs = [...] })`. Повторная доставка той же эффективной идентичности протокола и `commandSource` в пределах срока идемпотентности возвращает прежний принятый результат и доказательство кадра цели без ещё одного Decision или ревизии Payment. Запрос статуса, которому не нужны эти свидетельства принятой операции, был бы `ReadDependency` или `Query` и не создавал бы Decision, ревизию или выход цели.

В этом примере сверка применяет исходные положения `PBA-20`, `PBA-21`, `PBA-22` и `PBA-26` вместе с поддерживающим контрактом суверенитета рабочего процесса из §10.4. Этот переход:

- сохраняет идентичность исходного проведения платежа;
- оставляет тайм-аут неразрешённым исходом, не считая его постоянным сбоем;
- сохраняет резерв товара и пока не допускает другое оформление заказа; и
- устанавливает, был ли проведён платёж, до начала возврата, если только контракт провайдера не делает более раннее начало возврата безопасным.

Возможные результаты сверки:

```text
Captured(acceptedCommandProof, paymentRef)
DefinitelyNotCaptured(acceptanceEvidence)
StillUnknown(acceptanceFacet, evidence)
```

Все три значения — принятые результаты `Payment.GetOperationStatus`. `Captured` сразу записывает исходное проведение платежа как `acceptance = Accepted, outcome = Succeeded`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` описывает исходную `Payment.Capture`, тогда как сама команда статуса имеет `acceptance = Accepted`; другая его форма различает принятый исходный платёж с `Rejected/Failed` на уровне провайдера. `StillUnknown` сохраняет переданный аспект принятия исходного платежа и не сводит `AcceptanceUnknown + Pending` к `Accepted + OutcomeUnknown` без доказательства.

В протоколе Checkout v1 принятый `StillUnknown` исчерпывает единственное автоматическое поколение сверки платежа. После проверки точных `statusHandle`, `commandSource`, `resultSource`, эффективной идентичности протокола Payment, переданного аспекта исходного проведения платежа и ограниченных свидетельств Checkout принимает этот конечный Decision источника:

```text
ReconcilingPaymentAcceptance | ReconcilingPaymentOutcome
+ ModuleResultPulse(
      commandSource = statusCommandSource,
      resultSource = statusResultSource,
      result = StillUnknown(originalAcceptanceFacet, evidence)
  )

-> preserve cart snapshot, inventory reservation, original payment Step facets,
            original capture unknown evidence, and accepted status-result source
   steps.paymentReconciliation = Accepted + Succeeded
   terminalOutcome = NeedsManualReconciliation
   outputs = []
```

Пустая последовательность выходов намеренна: эта версия не создаёт второй дескриптор статуса или поколение, таймер, команду, Effect, возврат, освобождение резерва, разблокировку или автоматическое возобновление. Принятый результат статуса и его ограниченные свидетельства остаются доступны согласно выбранному контракту хранения принятого источника и статуса на объявленный срок аудита и восстановления; долговечность источника не заявляет исход провайдера. Точная повторная доставка того же результата статуса идемпотентна и не создаёт новый Decision, ревизию, дескриптор или конечный кадр Checkout. Позднее более сильное доказательство не может молча переписать `NeedsManualReconciliation`; его потребление требует отдельно объявленного артефакта ручной обработки или восстановления с собственными полномочиями, входом, переходом и свидетельствами. Такого артефакта в Checkout v1 нет.

`Captured(..., paymentRef)` не оставляет ссылку только внутри Pulse сверки: после проверки происхождения результата статуса он следует тому же переходу, который сохраняет `VerifiedStepValue<PaymentRef>` и создаёт выход Order, что и прямой `PaymentCaptured` ниже.

<a id="169-order-confirmation"></a>

### 16.9. Подтверждение заказа

После доказанного проведения платежа:

```text
CapturingPayment(
    cartSnapshot = RS,
    retained.checkoutInput = C,
    retained.inventoryReservation = RI
) + currentPulse: ModuleResultPulse {
      commandSource = paymentCommandSource,
      resultSource = paymentResultSource,
      effectiveProtocolIdentity = Payment.Capture@1.0.0,
      result = PaymentCaptured(paymentRef = P),
      issuerProvenance = R_P
    }

with derived currentResultAliases {
    authorityActionHandle = currentPulse.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.resultSource.semanticHandle,
    authorityProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    resultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    )
}

or

ReconcilingPaymentAcceptance | ReconcilingPaymentOutcome(
    cartSnapshot = RS,
    retained.checkoutInput = C,
    retained.inventoryReservation = RI
) + currentPulse: ModuleResultPulse {
      commandSource = statusCommandSource,
      resultSource = statusResultSource,
      effectiveProtocolIdentity = Payment.GetOperationStatus@1.0.0,
      result = Captured(acceptedCommandProof, paymentRef = P),
      issuerProvenance = R_STATUS
    }

with derived currentResultAliases {
    authorityActionHandle = acceptedCommandProof.commandSource.semanticHandle,
    observedViaStepHandle = currentPulse.commandSource.semanticHandle,
    authorityProtocolVersion = acceptedCommandProof.effectiveProtocolIdentity,
    observedViaProtocolVersion = currentPulse.effectiveProtocolIdentity,
    observedResultProvenance = verifiedDigest(
        currentPulse.commandSource,
        currentPulse.resultSource,
        currentPulse.issuerProvenance
    ),
    resultProvenance = normalizeOriginalActionProof(
        acceptedCommandProof,
        observedResultProvenance
    )
}

S = RS.value

verifiedPayment = VerifiedStepValue(
    value = P,
    authorityActionHandle = currentResultAliases.authorityActionHandle,
    observedViaStepHandle = currentResultAliases.observedViaStepHandle,
    authorityProtocolVersion = currentResultAliases.authorityProtocolVersion,
    observedViaProtocolVersion = currentResultAliases.observedViaProtocolVersion,
    resultProvenance = currentResultAliases.resultProvenance
)

orderHandle = SemanticHandle {
    operationId
    outputKind = Order.Confirm
    localOrdinalOrName = "order-confirmation"
}

-> ConfirmingOrder(
       cartSnapshot = RS,
       retained.checkoutInput = C,
       retained.inventoryReservation = RI,
       retained.paymentCapture = verifiedPayment
   )
+ ModuleCommandRequest {
      semanticHandle = orderHandle
      sourceOrdinal = 0
      payload = Order.Confirm(
          cartSnapshot = S,
          inventoryReservationRef = RI.value,
          paymentRef = verifiedPayment.value
      )
  }
```

Прямой результат должен иметь `commandSource.semanticHandle = paymentHandle`; результат сверки должен иметь `commandSource.semanticHandle = statusHandle` и доказанно описывать то же исходное авторитетное действие и ключ идемпотентности провайдера через `acceptedCommandProof`. Оба пути выводят дескриптор, версию и происхождение наблюдения из канонического `ModuleResultPulse`; ни один псевдоним Checkout не заменяет `commandSource`, `resultSource` или эффективную идентичность протокола. Независимое действительное доказательство того же P через другой объявленный маршрут служит подтверждением: оно не создаёт второй `Order.Confirm` и не перезаписывает принятое значение; другое P или версия авторитетного действия либо противоречащее доказательство исходного действия приводят к отказу по умолчанию. `verifiedPayment`, следующее состояние и выход Order принимаются в одном кадре. Цель Order заново проверяет собственные инварианты и ожидаемые ссылки. Flow не может заставить Order принять недопустимое состояние лишь потому, что предыдущие шаги прошли успешно.
