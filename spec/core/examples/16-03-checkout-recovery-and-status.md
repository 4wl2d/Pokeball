# Core part — Checkout Flow: recovery and status

[Core contents](../../pokeball-architecture-core.md) · [← Checkout Flow: execution](16-02-checkout-execution.md) · [Verification: transition and property tests →](../verification/17-01-transition-and-property-tests.md)

> Canonical part 14 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

### 16.10. Compensation

If order confirmation is definitively rejected after payment capture, the Flow creates new operations:

```text
SemanticHandle(operationId, Payment.Refund, "payment-refund")
SemanticHandle(operationId, Inventory.Release, "inventory-release")
SemanticHandle(operationId, Cart.Unlock, "cart-unlock")
```

Target values are derived only from committed Checkout state:

```text
Payment.Refund target
    = {
        paymentRef = state.retained.paymentCapture.value,
        originalPaymentHandle = state.steps.paymentCapture.handle,
        operationId = state.operationId
      }

Inventory.Release target
    = {
        reservationRef = state.retained.inventoryReservation.value,
        originalReservationHandle = state.steps.inventoryReservation.handle,
        operationId = state.operationId
      }

Cart.Unlock target
    = {
        cartId = state.cartSnapshot.value.cartId,
        originalLockHandle = state.steps.cartLock.handle,
        operationId = state.operationId
      }
```

Every compensation command receives a `semanticHandle` from the closed slot shown, an `idempotencyKey` from that handle/operation, a deadline from the retained workflow deadline plus trusted current context, and `CurrentActionAuthorization` for the exact compensation handle. The participant Execution Gate repeats the action/target/operation/expiry checks. If a concrete target contract permits compensation only by original semantic handle instead of P/I, that handle-based target form must be versioned and declared by the participant contract itself; the Flow does not assume it silently. The Capture grant is not used for Refund/Release/Unlock.

Every compensation has:

- a new complete `SemanticHandle` and its own `ModuleCommandRequest`/zero-based `sourceOrdinal`;
- its own idempotency key;
- deadline;
- authorization/capability;
- an outcome and `OutcomeUnknown` path.

The Order rejection/result and retained P/I/cart values are accepted even if compensation authorization is absent; the compensation steps created and the complete available output batch are changed by the same accepted frame. A crash cannot leave a dependent output without its source value or clear a value before its last consumer. Missing/expired compensation authorization does not trigger ambient credential fallback: no action is created, the residual target remains in state, and the terminal outcome becomes `NeedsManualReconciliation`. Compensation is not rollback. It may partially fail.

Terminal outcomes may be:

```text
Completed(orderId)
RejectedBeforeExternalCommitment
FailedCompensated
FailedPartiallyCompensated
NeedsManualReconciliation
CancelledBeforeCommitment
CancelledWithResidualEffects
```

### 16.11. Cancellation race

Customer cancellation passes parsing/authentication/validation in Interaction and arrives during payment as an owned `Intent`:

```text
CancellationRequested(generation = 1)
```

The Flow records the request and sends a declared target command:

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

If the target contract treats cancellation as a privileged action, its output uses a separate current grant for `paymentCancellationHandle`; the capture grant is not reused or stored in live state.

One possible sequence:

```text
PaymentCaptured result
CancellationTooLate
```

`PaymentCaptured` contains accepted-command proof, so under the same §16.9 rule the Flow atomically retains provenance-bound `retained.paymentCapture`, records `payment.acceptance = Accepted`, `payment.outcome = Succeeded`, and cancellation `CancellationTooLate`; the result is not discarded because of a local cancel flag. The declared policy then applies: continue the order or create a new `Payment.Refund` handle whose target comes from retained P and whose current grant is keyed by the refund handle. The cancellation branch does not keep P only in the current Pulse.

Another sequence is also possible:

```text
CancellationAcceptedBeforeStart
```

Then payment is not performed, and inventory/cart may be safely released.

### 16.12. Commit and runtime identity

Flow state stores:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
```

The runtime ledger stores:

```text
SemanticHandle(operationId, Payment.Capture, "payment-capture")
    -> OutputId(out-payment-...)
SemanticHandle(operationId, Inventory.Reserve, "inventory-reservation")
    -> OutputId(out-inventory-...)
```

Every source Decision atomically accepts:

```text
next workflow state
new EffectRequest/ModuleCommandRequest outputs
idempotency markers
operation status changes
```

If the current Pulse introduces a decision-relevant M/S/I/P value for the first time, its `CapturedCheckoutInput`/`VerifiedStepValue` and every new output that depends on it belong to the same accepted frame. No crash point can publish an output without the retained source value or retain the value without the complete output batch. The protected output record retains the exact immutable scoped grant with which the output was accepted; live state stores only the actor/value lineage from §§16.3–16.4.

Runtime then dispatches the outputs. This removes the dependency of domain state on commit-time infrastructure IDs.

The dispatch/ACK result is not written back into this commit retroactively. Runtime stores mechanical attempt evidence and returns a typed `CheckoutCommandDeliveryObserved` with the original `SemanticHandle`; only the next serialized Decision may change the corresponding `Step` and create a reconciliation output if needed.

Participant result dispatch is a separate accepted-frame path. After a participant accepts `ModuleResultOutput`, its result route derives `resultSource` from that target frame and attempts delivery of `ModuleResultPulse` to Checkout. A crash before result dispatch does not erase the accepted target result under the selected participant state/output profile. When that result route is retained, retried, or independently observable, the output occupies one participant-owned stop-eligible slot keyed by `(effectiveProtocolIdentity, commandSource, resultSource)`. Exhaustion records target `DispatchStopped`; it neither fabricates Checkout receipt nor sets a Checkout `Step` acceptance/outcome/cancellation/status facet. Checkout remains at its previously proven `Pending`, `AcceptanceUnknown`, or `OutcomeUnknown` state until a verified result Pulse or declared reconciliation proof arrives. A later verified result may refine those facets without erasing the target's retained delivery-stop fact.

### 16.13. Operation status and reply

Initial reply:

```text
ReplyOutput(
    semanticHandle = SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance"),
    sourceOrdinal = 1,
    payload = RequestAccepted(operationId)
)
```

is not a terminal business result. The client obtains terminal status through:

```text
GetCheckoutStatus(operationId)
```

The owned result payload is closed:

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

`CheckoutKnownStatus` retains independent lifecycle, cancellation, and dispatch facets. `Accepted` means an accepted Flow operation before entry into the first participant phase. `InProgress` and `OutcomeUnknown` retain the current `CheckoutProgress`. Terminal mapping is total: `Completed(orderId)` maps to `Completed`; `RejectedBeforeExternalCommitment` to `Rejected`; the three failure outcomes to `Failed`; and the two cancellation outcomes to `Cancelled`. The §16.8 accepted-`StillUnknown` transition therefore materializes as `Failed(NeedsManualReconciliation)`, not an indefinitely reconciling row. A root validation, admission, or Checkout Decision rejection returns only its typed `BoundaryResponse` and creates no Checkout operation/status record. Nonacceptance of a participant command remains in the matching `Step.acceptance` of an already accepted Flow operation and does not become a root lifecycle.

`NoDispatchStopped` means the empty set. `OneOrMoreDispatchStopped.stops` is nonempty, contains no more than the manifest's `maxRetainedDispatchStops` records, and has a unique complete `semanticHandle`; this tighter bound also satisfies the general `maxCollectionItems`. For the current protocol version, serialization follows the fixed order of ten stop-eligible source-output slots: `cartLock`, `initialAcceptanceReply`, `inventoryReservation`, `paymentCapture`, `paymentCancellation`, `paymentReconciliation`, `orderConfirmation`, `inventoryRelease`, `paymentRefund`, `cartUnlock`; arrival order does not affect the result. `initialAcceptanceReply` means the exact handle `SemanticHandle(operationId, Checkout.RequestAccepted, "initial-acceptance")` from §16.5; the remaining slots correspond to the `steps` fields in §16.3.

These ten slots belong to Checkout source-output delivery. A participant's accepted `ModuleResultOutput` and any target `DispatchStopped` for its return route belong to that participant's separate target delivery/status bounds and do not add an eleventh Checkout source slot or directly change `CheckoutStatus`.

As the exact ten-slot Checkout instantiation of §9.11's pre-acceptance capacity rule, before acceptance of any Decision that creates a stop-eligible `SemanticOutput` for the first time, the source/status capacity plan reserves a record slot for every new unique handle; the initial Decision in §16.5 reserves two slots—`cartLock` and `initialAcceptanceReply`. The number of exposed handles over the operation's lifetime cannot exceed either declared bound; `N+1` rejects the entire Decision before source acceptance and dispatch. After acceptance, a valid terminal observation cannot be lost because of materialization pressure: it remains retained/pending until applied by the status authority, which may honestly lag with its own stamp but may not evict/truncate the record. The status materializer handles a capacity/byte failure as typed backpressure/operational fault with retry within the profile policy, not as grounds to change an already accepted semantic fact.

Checkout instantiates §9.11's monotonic conflict rule as follows. Merging the same terminal observation is idempotent. For the current delivery policy, a nonequivalent second stop record with the same `semanticHandle` is an invariant fault and does not overwrite the first by arrival order; there is no implicit reopening of this policy. A record for another handle is added without deleting existing records. Stopped records are retained until the declared status-retention transition and may coexist with any compatible lifecycle/cancellation state; they do not turn it into `Failed` or `OutcomeUnknown`.

For this query, the declared operation status authority is committed `CheckoutStatusAuthority` with its own revision and one logical writer in an authenticated namespace. It is the Checkout projection of the canonical §9.11 authority; its physical storage/process form remains project/binding-owned. It materializes Flow status changes and trusted runtime delivery observations without becoming the workflow's command authority. Its records transition as follows:

```text
absent + accepted Checkout operation record -> CheckoutKnownStatus
CheckoutKnownStatus + workflow commit -> updated CheckoutKnownStatus
CheckoutKnownStatus + trusted terminal delivery observation
    -> same lifecycle/cancellation + idempotent merge by semanticHandle
delivery observation before its causal accepted/workflow record
    -> bounded pending by operationId/semanticHandle; no lossy status commit
causal record + pending observations
    -> one CheckoutKnownStatus update with canonical stop merge
CheckoutKnownStatus + covered sources + no pending + retention expiry
    -> CheckoutExpiredFromStatusRetention marker
expired marker + observation at/before covered source position
    -> unchanged expired marker
expired marker + marker-horizon expiry -> absent / CheckoutNotFound
```

A delivery observation causally references a previously committed stop-eligible source output. For a command Step, the same runtime fact enters the Flow through `CheckoutCommandDeliveryObserved(CommandDispatchStopped)` and the status authority through its declared typed delivery-observation input. For `initialAcceptanceReply`, only the status-authority path applies: its trusted `ControlPulse` under §6.11 is bound to the exact committed `ReplyOutput` source tuple and does not masquerade as a command Pulse or business result because reply delivery does not change `CheckoutState`. The concrete status-authority protocol/origin binding is recorded in the project overlay.

This transition table is the set-equal Checkout instantiation of §9.11: the materializer either applies sources in causal order or retains an out-of-order observation in bounded pending state; the source record/observation remains retained under the selected profile policy until application. For `Transient`, this promise ends with the process lifetime; durable retention requires a `SnapshotOutbox`/`EventJournal` binding and declared horizon. Per-operation pending stop keys are bounded by `maxRetainedDispatchStops`; the global queue/backpressure cap is defined by the concrete binding. A retention marker may be committed only after declared source positions/horizons covering the operation and with an empty pending set, and it is committed before the known row can become absent. Therefore, an observation at or before covered positions leaves the marker unchanged, a late duplicate does not resurrect an expired operation, and an accepted observation is not lost in a race with retention within the stated profile guarantee.

`GetCheckoutStatus -> CheckoutStatus` returns as `ReadResult<CheckoutStatus>` with the stamp of the exact committed `CheckoutStatusAuthority` snapshot for all three outer variants. Therefore, `NotFound` and expiry do not require an invented snapshot of an absent `CheckoutFlowBall` instance. The stamp does not promise that materialized status has already caught up with every source. A Query is not a `ReplyOutput`/`ProjectionOutput` and creates no new commit. A live/durable reply channel for the concrete profile remains an alternative. Loss of an HTTP connection does not delete an accepted operation under a durable profile.

### 16.14. What the example demonstrates

- A Flow appears because of independent coordination authority, not because of every inter-module call.
- Feature Balls preserve local invariants.
- Each participant target owns one command/result mapping; verified routes preserve accepted source and target causal tuples, while Assembly owns only transport/version binding.
- Captured input is minimal and versioned.
- The Checkout ingress fingerprint has one explicit actor/namespace-scoped derivation from the current Pulse + trusted Context, excludes key/transport metadata, and is retained equal to the atomic Interaction record together with the exact verified Interaction artifact version `IV`, distinct from `transitionArtifactVersion`.
- Values from prior ingress/results needed by a later Decision are retained as bounded provenance-bound workflow values until the last consumer; runtime/participant history is not decision input.
- A privileged output receives a current action-scoped grant through a declared trusted versioned context; a raw grant/principal does not live in Flow state, while the exact committed grant is protected inside the output record.
- ACK, acceptance, result, cancellation, and unknown are not conflated.
- Pre-acceptance carrier reasons remain `RejectedBeforeAcceptance + NotExpected`; all nine normal Checkout business refusals are accepted target results, and accepted rejection remains `Accepted + Rejected`.
- Compensation is new fallible work, not rewind.
- Root idempotency redelivers the original accepted Reply frame for an equivalent retry and classifies a conflicting fingerprint before Intent construction; target idempotency returns accepted-frame ACK while a result is pending and exact accepted-result proof afterward.
- The sole Checkout v1 status reconciliation consumes accepted `StillUnknown` into terminal `NeedsManualReconciliation`; no automatic generation or silent late-proof rewrite is implied.
- Runtime delivery identity is separate from semantic workflow state.
- Post-commit dispatch/ACK/ambiguity changes Flow state only through a declared trusted `ControlPulse`.
- The status authority honestly distinguishes a known, absent, and retention-expired operation, retains all bounded stopped output/step handles, and does not conflate delivery exhaustion with business outcome.
- A reserved root candidate ID that fails validation, admission, or the Checkout Decision creates only its typed `BoundaryResponse`, never a known Checkout status row; participant command nonacceptance remains an accepted operation's Step facet.

---
