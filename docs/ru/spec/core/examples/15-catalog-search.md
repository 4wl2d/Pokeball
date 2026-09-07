<!-- pkb:translation source="spec/core/examples/15-catalog-search.md" -->

[Документация на русском](../../../README.md) · [Содержание Core](../../pokeball-architecture-core.md) · [Оригинал на английском](../../../../../spec/core/examples/15-catalog-search.md)

> Русский перевод для чтения. Нормативный источник — [английский Core](../../../../../spec/pokeball-architecture-core.md).

<a id="core-part--catalog-search-example"></a>

# Часть Core — Пример поиска по каталогу

[Содержание Core](../../pokeball-architecture-core.md) · [← Манифест и организация исходных файлов](../14-manifest-and-organization.md) · [Checkout Flow: модель и входящие запросы →](16-01-checkout-model-and-ingress.md)

> Перевод канонической части 11 из 24. [Входной документ Core](../../pokeball-architecture-core.md) задаёт версию, статус, полный состав файлов и порядок чтения.

---

<a id="15-end-to-end-example-i-catalog-search"></a>

## 15. Сквозной пример I: поиск по каталогу

Этот пример показывает небольшой `FeatureBall` с путём принятия решений `Inline` и асинхронным результатом ресурса. Для него не нужны Flow, брокер или движок долговечных рабочих процессов.

<a id="151-protocol"></a>

### 15.1. Протокол

Авторитетные идентификаторы контракта Catalog:

```text
protocolVersion = 2.0.0
stateSchemaVersion = 2
transitionArtifactVersion = 2.0.1
```

```text
CatalogIntent =
    SearchRequested(query, pageSize)
  | SearchCancelled(operationId)
  | ProductSelected(productId)

CatalogQuery =
    GetCatalogView

CatalogSignal =
    ProductSelectionConfirmed(productId)

CatalogFact =
    ProductsFound(searchHandle, generation, products, provenance)
  | ProductSearchFailed(searchHandle, generation, failure, provenance)
  | ProductSearchOutcomeUnknown(searchHandle, generation, provenance)
  | ProductSearchCancelledBeforeStart(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationAcceptedInProgress(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancelled(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationTooLate(cancellationHandle, targetSearchHandle, generation, provenance)
  | ProductSearchCancellationRejected(cancellationHandle, targetSearchHandle, generation, reason, provenance)
  | ProductSearchCancellationOutcomeUnknown(cancellationHandle, targetSearchHandle, generation, provenance)

CatalogSearchCancellation =
    NotRequested
  | Requested
  | CancellationAcceptedBeforeStart
  | CancellationAcceptedInProgress
  | CancellationTooLate
  | CancellationRejected(reason)
  | CancellationUnknown

CatalogSearchLifecycle =
    Searching(generation, query, pageSize)
  | Ready(generation, products)
  | Failed(generation, reason)
  | OutcomeUnknown(generation, query, pageSize)
  | Cancelled(generation)

CatalogView =
    CatalogIdle
  | CatalogSearchStatus(operationId, lifecycle, cancellation)

CatalogEffect =
    FindProducts(searchHandle, query, pageSize, mode, deadline)
  | CancelProductSearch(targetSearchHandle)
```

`ProductSelectionConfirmed` принадлежит Catalog. Он может выйти за границу Ball только как содержимое принятого `SignalPublication`; маршрут Assembly из §14.4 не может определять или синтезировать его.

Собственное отображение запросов — `GetCatalogView -> CatalogView`. Успешное локальное чтение отображает точное зафиксированное `CatalogState` с помощью приведённой ниже тотальной функции и возвращает `ReadResult<CatalogView>` с меткой этого снимка; оно не создаёт ни `ProjectionOutput`, ни новую фиксацию. `ProjectionOutput` состояния поиска использует то же содержимое `CatalogView`, поэтому текущее представление и представление по запросу не могут выбирать разные аспекты жизненного цикла.

<a id="152-state"></a>

### 15.2. Состояние

```text
CatalogState =
    Idle(revision)
  | Searching {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Ready {
        operationId
        generation
        query
        products
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Failed {
        operationId
        generation
        reason
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | OutcomeUnknown {
        operationId
        generation
        query
        pageSize
        pendingSearch: SemanticHandle
        pendingCancellation: SemanticHandle?
        cancellation: CatalogSearchCancellation
        revision
    }
  | Cancelled {
        operationId
        generation
        cancellation: CancellationAcceptedBeforeStart | CancellationAcceptedInProgress
        revision
}
```

Поле `revision` внутри `CatalogState` в этом примере — **доменная ревизия**, которая принадлежит Ball и используется в переходах и проекциях Catalog. Она отличается от `CommitRevision` из §3.2, которой владеет компонент принятия решений. Конкретная привязка к среде исполнения может выводить или отображать одну из другой, только если её объявленный контракт доказывает их точное равенство и сохраняет оба смысла владения; иначе это отдельные значения, и ни одно не служит доказательством другого.

`CancellationRejected(reason)` сохраняет в зафиксированном состоянии ограниченную типизированную причину отказа. Она не восстанавливается из журналов или временного обратного вызова. `OutcomeUnknown` сохраняет идентичность текущего поиска и поля запроса, чтобы поздний совпадающий результат, наблюдение отмены или объявленное решение о сверке могли уточнить ту же операцию без чтения истории среды исполнения.

Зафиксированное `CatalogState` — единственный авторитетный источник. Отображение версии 2 в `CatalogView` состоит ровно из этих шести случаев: без резервной ветви или ветви по умолчанию и без нескольких подходящих случаев:

| Исходный вариант `CatalogState` | Точный результат `CatalogView` |
|---|---|
| `Idle(revision)` | `CatalogIdle` |
| `Searching(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Searching(generation, query, pageSize), cancellation)` |
| `Ready(operationId, generation, ..., products, cancellation, revision)` | `CatalogSearchStatus(operationId, Ready(generation, products), cancellation)` |
| `Failed(operationId, generation, reason, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, Failed(generation, reason), cancellation)` |
| `OutcomeUnknown(operationId, generation, query, pageSize, ..., cancellation, revision)` | `CatalogSearchStatus(operationId, OutcomeUnknown(generation, query, pageSize), cancellation)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `CatalogSearchStatus(operationId, Cancelled(generation), cancellation)` |

Назовём это чистое тотальное отображение `toCatalogView`. Каждое решение Catalog о поиске или отмене, которое меняет состояние, публикует `toCatalogView(nextState)`, если его переход ниже указывает проекцию. Отображение вместе раскрывает жизненный цикл, доказанный результат или сбой, статус отмены и причину отказа в отмене; закрытые дескрипторы ожидания остаются состоянием, а не публичным статусом.

В этом примере положения о миграции и восстановлении из §§8.9 и 10.11 применяются так: сохранённое состояние схемы v1 поступает в обычный `decide` версии 2 только после авторитетного преобразования в новую схему. Авторитетный преобразователь до исполнения отображает каждую запись v1 `Idle`, `Searching`, `Ready`, `Failed` или `Cancelled` и её точные поля в соответствующий вариант v2. В записи v1 `CancellationRejected` нет обязательной причины: её можно преобразовать, только если ограниченное авторитетное принятое свидетельство задаёт эту точную причину; иначе запись помещают в карантин или направляют по объявленному пути ручного исправления. Запрещено синтезировать null, пустую, общую или выведенную косвенно причину. Уже сохранённые выходы протокола v1 сохраняют смысл v1 и не переосмысливаются как содержимое `CatalogView` или `CatalogSignal` v2.

Состояние хранит `SemanticHandle`, а не созданный хранилищем `OutputId`. Реестр среды исполнения может задавать соответствие:

```text
SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
} -> OutputId(out-8f...)
```

Это механическое соответствие не меняет бизнес-состояние.

<a id="153-interaction-parsing-and-validation"></a>

### 15.3. Interaction: разбор и валидация

Сырой запрос:

```text
query = "50%_off' OR 1=1 --"
pageSize = "100"
```

Interaction выполняет:

```text
проверка UTF-8
нормализация NFC по контракту поля
SearchText.parse(maxUtf8Bytes = 128)
PageSize.parse(range = 1..100)
построение срока выполнения запроса
```

Строка допустима как буквальный пользовательский текст. Специальные символы не удаляются ради «безопасности».

Запрос:

```text
pageSize = "2000000000"
```

отклоняется с `BoundaryResponse(ValidationFailure.InvalidPageSize)`. Interaction не создаёт `SearchRequested`, `Decision`, `CommitRevision` или `SemanticHandle`: ответ границы не является зафиксированным `ReplyOutput`. Ограничение значения сверху допустимо лишь как отдельная, явно объявленная продуктовая политика.

Доверенная граница передаёт:

```text
DecisionContext {
    trustedTimeObservation
    reservedSemanticIds = [search-88]
    expiresAt
}
```

`operationId` не считывается из произвольного клиентского поля и не создаётся внутри Nucleus из неявного источника случайности.

Для `ProductSelected(productId)` Interaction проверяет идентификатор продукта и передаёт один зарезервированный ID операции выбора, например `reservedSemanticIds = [selection-501]`. Показанный здесь путь выбора не зависит от субъекта и не использует конфигурацию или доверенное время, поэтому он не создаёт полей контекста субъекта, конфигурации или времени. Проект добавляет контекст субъекта, только если субъект, арендатор, издатель, область доверия, уровень подтверждения или делегирование могут изменить это решение о выборе; тогда PBA-44 применяется независимо от привилегий.

<a id="154-first-decision"></a>

### 15.4. Первое решение

```text
Idle
+ SearchRequested(query, pageSize)
+ DecisionContext(reservedSemanticIds = [search-88])

searchHandle = SemanticHandle {
    operationId = search-88
    outputKind = Catalog.FindProducts
    localOrdinalOrName = "generation-1"
}

-> Searching {
     operationId = search-88
     generation = 1
     query
     pageSize
     pendingSearch = searchHandle
     pendingCancellation = none
     cancellation = NotRequested
   }

+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId = search-88
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-searching-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         search-88,
         Searching(generation = 1, query, pageSize),
         NotRequested
     ) # в точности toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = searchHandle
     sourceOrdinal = 1
     payload = FindProducts(
         searchHandle,
         query,
         pageSize,
         mode = LiteralContains,
         deadline = context.expiresAt
     )
   }
```

Среда исполнения проверяет ограничения выходов и привязку возможностей, принимает `Decision`, затем отправляет Effect на исполнение. Если состояние сохраняется, оно записывается атомарно вместе с исходящей очередью эффектов.

Для `ProductSelected` задан один явный переход из каждого из шести вариантов состояния. Это шесть закрытых случаев, а не универсальная резервная ветвь:

| Исходное состояние + `ProductSelected(productId)` | Следующее состояние |
|---|---|
| `Idle(revision)` | `Idle(revision + 1)` |
| `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `Searching(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision)` | `Ready(operationId, generation, query, products, pendingCancellation, cancellation, revision + 1)` |
| `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision)` | `Failed(operationId, generation, reason, pendingCancellation, cancellation, revision + 1)`; аспект `CancellationRejected(reason)` сохраняет собственную типизированную причину отмены независимо от причины сбоя поиска. |
| `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision)` | `OutcomeUnknown(operationId, generation, query, pageSize, pendingSearch, pendingCancellation, cancellation, revision + 1)` |
| `Cancelled(operationId, generation, cancellation, revision)` | `Cancelled(operationId, generation, cancellation, revision + 1)` |

Каждое из этих шести решений содержит ровно этот один выход и не содержит Projection, Reply, Effect, Command или Timer:

```text
SignalPublication {
    semanticHandle = SemanticHandle {
        operationId = context.reservedSemanticIds.single # selection-501
        outputKind = Catalog.ProductSelectionConfirmed
        localOrdinalOrName = "selected"
    }
    sourceOrdinal = 0
    payload = ProductSelectionConfirmed(productId)
}
```

Принятое увеличение ревизии даёт направляемой публикации одну точную зафиксированную ревизию источника и сохраняет все факты жизненного цикла и отмены, свойственные состоянию. Signal отправляется только после принятия этого кадра состояния и выходов. Assembly владеет привязкой маршрута; Catalog владеет содержимым и решением о публикации.

<a id="155-resource-adapter-and-safe-sink"></a>

### 15.5. Адаптер ресурса и безопасная точка исполнения

Адаптер получает только возможность `Catalog.Search`. У него нет неограниченного клиента базы данных, оболочки или произвольного HTTP-клиента.

Для SQL `LIKE` с буквальной семантикой:

```text
escaped = escapeLikeLiteral(query, '\\')
pattern = "%" + escaped + "%"

SELECT product_id, title, price
FROM product_search
WHERE searchable_text LIKE ? ESCAPE '\\'
ORDER BY rank DESC, product_id ASC
LIMIT ?
```

Обязательны:

- привязка параметров;
- явная семантика буквального поиска или шаблонных символов;
- учётные данные только для чтения;
- утверждённая политика индекса и плана запроса;
- тайм-аут;
- ограничения числа строк и размера ответа в байтах;
- проверка схемы внешнего результата.

Параметризация исключает интерпретацию текста как SQL. Экранирование `%` и `_` задаёт именно семантику буквального поиска. Это разные обязанности.

<a id="156-successful-result"></a>

### 15.6. Успешный результат

Resource создаёт проверенный `CatalogFact`, привязанный к происхождению:

```text
ProductsFound {
    searchHandle = SemanticHandle {
        operationId = search-88
        outputKind = Catalog.FindProducts
        localOrdinalOrName = "generation-1"
    }
    generation = 1
    products = BoundedList(max = 100)
    provenance = CatalogSearchExecutor / attempt-1
}
```

Решение:

```text
Searching(generation = 1, pendingSearch = searchHandle, cancellation)
or OutcomeUnknown(generation = 1, pendingSearch = searchHandle, cancellation)
+ ProductsFound(searchHandle, generation = 1, products, provenance)
+ DecisionContext = Unit

-> Ready(operationId, generation = 1, products, pendingCancellation, cancellation)
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-ready-generation-1"
     }
     sourceOrdinal = 0
     payload = CatalogSearchStatus(
         operationId,
         Ready(generation = 1, products),
         cancellation
     ) # в точности toCatalogView(nextState)
  }
```

В показанном примере, не зависящем от субъекта, это решение по результату имеет собственный контекст `Unit` для данного Pulse. Оно не наследует зарезервированный ID, доверенное время или поля срока действия из контекста исходного `SearchRequested` лишь потому, что привязка Inline может исполнить обе причины в одной причинной области.

`ProductSearchFailed` следует тому же уточнению `Searching|OutcomeUnknown -> Failed` и публикует полное `CatalogSearchStatus(..., Failed(generation, reason), cancellation)`. Доказанный результат или сбой уточняет прежний `OutcomeUnknown`, но сохраняет текущий аспект отмены, включая `CancellationRejected(reason)`.

<a id="157-stale-result"></a>

### 15.7. Устаревший результат

Пользователь начинает второй поиск до завершения первого:

```text
first  -> generation 1 / SemanticHandle(search-88, Catalog.FindProducts, "generation-1")
second -> generation 2 / SemanticHandle(search-89, Catalog.FindProducts, "generation-2")
```

Вторая строка сокращённо обозначает отдельное принятое решение о перезапуске; его полный блок перехода намеренно опущен. Трасса устаревшего результата начинается лишь после того, как этот кадр зафиксировал поколение 2 и его новый дескриптор и пакет выходов, поэтому ни обратный вызов, ни компонент среды исполнения не меняют состояние Catalog неявно.

Поздний `ProductsFound` с полным дескриптором первой операции и `generation = 1` не заменяет поколение 2. Nucleus:

- игнорирует устаревший результат;
- либо записывает диагностический исход;
- либо использует только безопасные данные кеша по явно объявленному правилу слияния.

Он не применяет результат по правилу «побеждает последний обратный вызов».

<a id="158-cancellation-race"></a>

### 15.8. Гонка при отмене

`SearchCancelled(receivedOperationId)` — допустимый Intent Catalog, но он может адресовать только текущую операцию. До создания дескриптора отмены или принятия любого решения об отмене Nucleus Catalog требует точного равенства:

```text
receivedOperationId
    = state.operationId
    = state.pendingSearch.operationId
```

Эта проверка применяется только к `Searching` или `OutcomeUnknown` с присутствующим `pendingSearch`; другое исходное состояние следует собственному закрытому контракту отказа и не может заимствовать дескриптор. Если три значения различаются, `decide` возвращает ровно `Rejected(BusinessRejection.StaleSearchOperation(expectedOperationId = state.operationId, receivedOperationId))`. Interaction кодирует этот фактический отказ Nucleus как `BoundaryResponse(DecisionRejected(...))`. Ограниченная причина содержит только ожидаемый и полученный семантические ID. При отказе не принимаются Decision, State, ревизия, дескриптор отмены, Projection, Effect или выход, и текущий поиск не может быть отменён. Это не отсутствие изменений при устаревшем результате; идентичность Intent операции A никогда не объединяется с `pendingSearch` операции B.

```text
Searching or OutcomeUnknown(
    pendingSearch = searchHandle,
    cancellation = NotRequested
)
+ SearchCancelled(receivedOperationId = operationId)

cancelHandle = SemanticHandle {
    operationId
    outputKind = Catalog.CancelProductSearch
    localOrdinalOrName = "cancel-generation-2"
}

-> те же жизненный цикл и поля поиска,
   pendingCancellation = cancelHandle,
   cancellation = Requested
+ ProjectionOutput {
     semanticHandle = SemanticHandle {
         operationId
         outputKind = Catalog.CatalogView
         localOrdinalOrName = "view-cancel-requested-generation-2"
     }
     sourceOrdinal = 0
     payload = toCatalogView(nextState)
  }
+ EffectRequest {
     semanticHandle = cancelHandle
     sourceOrdinal = 1
     payload = CancelProductSearch(targetSearchHandle = searchHandle)
  }
```

Каждый Fact отмены должен одновременно совпадать по `cancellationHandle`, `targetSearchHandle`, `generation` и доверенному происхождению. После совпадения применяются следующие исчерпывающие переходы:

| Fact | Переход состояния | Дальнейшее поведение |
|---|---|---|
| `ProductSearchCancelledBeforeStart` | `Searching\|OutcomeUnknown -> Cancelled(cancellation = CancellationAcceptedBeforeStart)`; из `Ready\|Failed` с принятым конечным результатом поиска — нарушение инварианта или происхождения без нового Decision | Конечный Fact доказывает отмену до запуска. Взаимоисключающее конечное доказательство при любом порядке не перезаписывает первое принятое состояние. |
| `ProductSearchCancellationAcceptedInProgress` | Из `Searching\|OutcomeUnknown\|Ready\|Failed(cancellation = Requested)` сохранить точные поля жизненного цикла и результата и изменить только аспект на `CancellationAcceptedInProgress`; повтор в этом аспекте или позднее более слабое доказательство из `Cancelled(CancellationAcceptedInProgress)` — подтверждение без изменений | Принятие не доказывает физическую остановку; совпадающий результат до или после этого Fact остаётся авторитетным. |
| `ProductSearchCancelled` | `Searching\|OutcomeUnknown(cancellation = Requested\|CancellationAcceptedInProgress) -> Cancelled(cancellation = CancellationAcceptedInProgress)`; из `Ready\|Failed` — нарушение инварианта или происхождения без нового Decision | Сам конечный Fact доказывает принятую отмену, даже если отдельный Fact принятия отмены во время исполнения задержан или потерян. |
| `ProductSearchCancellationTooLate` | Сохранить точные поля жизненного цикла и результата `Searching\|OutcomeUnknown\|Ready\|Failed` и задать `cancellation = CancellationTooLate` | Уже полученный или последующий совпадающий результат остаётся авторитетным. |
| `ProductSearchCancellationRejected(reason)` | Сохранить точные поля жизненного цикла и результата `Searching\|OutcomeUnknown\|Ready\|Failed` и задать `cancellation = CancellationRejected(reason)` | Ограниченная причина фиксируется и остаётся в каждом последующем `CatalogView`. |
| `ProductSearchCancellationOutcomeUnknown` | Сохранить точные поля жизненного цикла и результата `Searching\|OutcomeUnknown\|Ready\|Failed` и задать `cancellation = CancellationUnknown` | Совпадающий результат не отбрасывается; сверка остаётся явной. |

Каждый принятый переход в таблице, который меняет состояние, создаёт один `ProjectionOutput` с полным дескриптором представления, `sourceOrdinal = 0` и содержимым `toCatalogView(nextState)`. Поэтому он публикует жизненный цикл и результат вместе с отменой. Точный дубликат или указанное подтверждение без изменений не создаёт повторное семантическое состояние или выход. Путь конфликта не является переходом: он не принимает Decision, состояние или выход и следует §8.8.

Для одной точной цепочки происхождения отмены и поиска порядок наблюдений не является скрытым предусловием:

```text
ProductSearchCancellationAcceptedInProgress
ProductsFound
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductsFound
-> Ready(cancellation = Requested)
ProductSearchCancellationAcceptedInProgress
-> Ready(cancellation = CancellationAcceptedInProgress)

ProductSearchCancellationAcceptedInProgress
ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)

ProductSearchCancelled
-> Cancelled(cancellation = CancellationAcceptedInProgress)
поздний ProductSearchCancellationAcceptedInProgress
-> подтверждение; нет нового семантического состояния/выхода
```

`ProductSearchFailed` следует тем же правилам совместимого порядка. Из них не следует ни доставка AIP первой, ни неограниченная буферизация.

Наблюдения слишком поздней отмены, отказа и неизвестного исхода отмены коммутируют с допустимым результатом, поскольку каждое меняет только аспект отмены:

```text
CancellationRejected(reason-R)
ProductsFound(products-P)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))

ProductsFound(products-P)
CancellationRejected(reason-R)
-> Ready(products-P, cancellation = CancellationRejected(reason-R))
```

`CancellationTooLate` и `CancellationUnknown` следуют тому же правилу двух порядков. Предшествующий `OutcomeUnknown` поиска может быть уточнён поздним совпадающим `ProductsFound` или `ProductSearchFailed`; этот результат меняет только жизненный цикл и сохраняет последний принятый аспект отмены и причину отказа.

Взаимоисключающие конечные доказательства имеют симметричный контракт:

```text
ProductSearchCancelled -> Cancelled
ProductsFound -> нарушение инварианта/происхождения; нет Decision; Cancelled остаётся принятым

ProductsFound          -> Ready
ProductSearchCancelled -> нарушение инварианта/происхождения; нет Decision; Ready остаётся принятым
```

`ProductSearchFailed` следует тому же правилу конфликта конечных исходов. Это не правило «побеждает последнее поступление» и не неявное преобразование типизированного `ProductSearchCancelled` в `CancellationTooLate`. Catalog v2 закрепляет это закрытое семейство переходов, включая допуск устаревшей отмены, за `transitionArtifactVersion: 2.0.1`; `protocolVersion = 2.0.0`, `stateSchemaVersion = 2`, версии маршрутов и граница миграции версии 2 в §§14.4/15.1–15.2 остаются прежними.

Обе допустимые трассы порядка имеют одинаковый смысл:

```text
CancellationTooLate -> Searching(cancellation = CancellationTooLate)
ProductsFound       -> Ready(cancellation = CancellationTooLate)

ProductsFound       -> Ready(cancellation = Requested)
CancellationTooLate -> Ready(cancellation = CancellationTooLate)
```

Аналогично, совпадающий `ProductsFound` после `CancellationAcceptedInProgress`, `CancellationRejected(reason)` или `CancellationUnknown` принимается и сохраняет соответствующий аспект отмены. Линейного перечисления `Loading | Cancelled | Ready` без независимого аспекта недостаточно.

<a id="159-timeout-and-unknown"></a>

### 15.9. Тайм-аут и неизвестный исход

Истечение локального срока означает, что вызывающая сторона перестала ждать. Если внешний запрос мог быть принят, результат:

```text
ProductSearchOutcomeUnknown
```

не преобразуется в `ProductSearchFailed`. Для текущего совпадающего поиска:

```text
Searching(все поля поиска, cancellation)
+ ProductSearchOutcomeUnknown(searchHandle, generation, provenance)

-> OutcomeUnknown(те же поля операции/поиска/ожидания, cancellation)
+ ProjectionOutput(
     sourceOrdinal = 0,
     payload = toCatalogView(nextState)
   )
```

Поздний совпадающий `ProductsFound` или `ProductSearchFailed` уточняет `OutcomeUnknown` до `Ready` или `Failed` и сохраняет аспект отмены. Более поздний и слабый `ProductSearchOutcomeUnknown`, полученный после `Ready`, `Failed` или доказанного `Cancelled`, — подтверждение без изменения состояния и выходов; он никогда не отменяет более сильное доказательство. Точный дубликат в `OutcomeUnknown` также ничего не меняет.

Повтор обычно безопасен для поиска только на чтение, но это свойство данного точного контракта Effect, а не общее правило тайм-аутов. Любой повтор сохраняет дескриптор операции и поиска, остаётся конечным и удовлетворяет полному правилу владения повторами из §9.9; этот пример не выбирает политику повторов.

<a id="1510-what-the-example-demonstrates"></a>

### 15.10. Что показывает пример

- Interaction не знает SQL.
- Resource не знает состояние UI.
- Nucleus не знает драйвер базы данных.
- Пользовательский текст не становится кодом запроса.
- Состояние защищено причинностью поколений и дескрипторов.
- Зафиксированное состояние отображается через одно тотальное `CatalogView` с шестью случаями и для Query, и для Projection.
- Выбор продукта публикует один принадлежащий производителю `CatalogSignal` из каждого состояния без потери фактов жизненного цикла поиска или отмены.
- Неизвестный исход можно уточнить доказательством, но он не может отменить доказанный конечный результат.
- ID среды исполнения отделён от семантического дескриптора.
- Путь Inline не требует посредника или очереди.
- Асинхронный адаптер можно заменить без изменения семантики решений.

---
