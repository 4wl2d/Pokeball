# Core part — Checkout Flow: execution

[Core contents](../../pokeball-architecture-core.md) · [← Checkout Flow: model and ingress](16-01-checkout-model-and-ingress.md) · [Checkout Flow: recovery and status →](16-03-checkout-recovery-and-status.md)

> Canonical part 13 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

### 16.5. Cart lock and captured snapshot

First decision:

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

Here the complete initial Context is `DecisionContext(actorContext = A, artifactVersion = IV)`. The trusted binding verifies and bounds `IV` as the exact version of the Interaction fingerprint artifact selected for this ingress. `IV` is distinct from the Nucleus `transitionArtifactVersion`; it is the only source for retained `interactionArtifactVersion`, and a missing, stale, mismatched, or untrusted value rejects before acceptance. The Interaction/Policy Gate requires trusted, valid `A`; the explicit operands `currentPulse + A` define `startFingerprint` without implicit Context capture or a ledger read. The idempotency record with the exact `startFingerprint`, `IV`, captured input, and both initial outputs belongs to one authoritative acceptance transaction. Repeating the same key/fingerprint after that acceptance redelivers proof of this exact accepted ordinal-1 `ReplyOutput(RequestAccepted(operationId))` frame with a new `AttemptId`; it does not run the transition, create another output/revision, or replace the captured value with another actor, payment, or Interaction-artifact binding. A pre-acceptance failure creates only its boundary response and no accepted-operation mapping.

The Cart route then follows one canonical round trip. From the accepted Checkout frame, the route derives `cartLockCommandSource` using the committed Checkout instance/revision, `cartLockHandle`, and `sourceOrdinal = 0`; Assembly does not construct that token. The trusted Cart boundary verifies the accepted frame and target mapping and constructs:

```text
cartCommandPulse = ModuleCommandPulse {
    commandSource = cartLockCommandSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    command = Cart.LockForCheckout(cartId, expectedCartVersion)
    issuerProvenance = verifiedCheckoutRoute
}
```

Cart accepts the command only through its canonical Decision. A successful target frame contains its target-owned result:

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

Only after target acceptance does the result route derive `cartLockResultSource` from that Cart frame and construct:

```text
cartResultPulse = ModuleResultPulse {
    commandSource = cartLockCommandSource
    resultSource = cartLockResultSource
    effectiveProtocolIdentity = Cart.LockForCheckout@1.0.0
    result = CartLocked(snapshot)
    issuerProvenance = verifiedCartRoute
}
```

The next Checkout Decision consumes this Pulse. On successful same-stack completion, Checkout reserves the level-1 alternative-completion slot, accepted Cart `decide` consumes it, Cart reserves level 2, and the Checkout result Decision consumes level 2, producing accepted levels `0/1/2`. If Cart is not accepted, Cart consumes no accepted level and the Checkout carrier Decision consumes the same level-1 alternative instead. Only the direct invocation creates the edge `Checkout -> Cart`; neither return branch creates a `Cart -> Checkout` edge. Generated code may erase the transport objects but must prove these same accepted tokens, identities, payload ownership, reservations, and acceptance points.

After target acceptance/result, the Flow receives a field-minimized snapshot:

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

The snapshot contains no profile history, UI draft, or unrelated cart fields. It is immutable and has provenance/version. Cart remains the authority for its state.

### 16.6. Inventory reservation

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

The verified `CartLocked` result Pulse must have `commandSource.semanticHandle = cartLockHandle`, a target-derived `resultSource`, and the version-pinned Cart identity; the provenance-bound snapshot, preserved checkout input, and Inventory output are accepted in one frame. The aliases above derive only from those canonical fields and do not create alternate handle/version authority. Duplicate delivery uses the same command identity/idempotency identity. The Flow does not create a new logical reservation for every transport retry.

### 16.7. Payment capture

After inventory succeeds:

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

Before acceptance, the Nucleus verifies the exact result handle/versions/provenance, equality of the actor/method/amount/currency/version constraints in §16.4, and grant validity at trusted `captureAuthorization.observedAt`; the Execution Gate repeats the expiry/revocation check during actual execution. `verifiedInventory`, next state, and the immutable capture output are accepted in one frame. Neither M, I, nor the grant is read from a prior Intent, participant/runtime ledger, or outbox.

If `captureAuthorization` is missing/expired/stale/mismatched, the current Checkout version does not wait for an ambient refresh or repeat the same result as a new cause. The same Decision retains `verifiedInventory`, creates no `Payment.Capture`, and advances to `Compensating`. Valid current Release/Unlock authorizations from the same context may atomically create the outputs in §16.10; an absent fallback grant is not replaced with a credential and records the residual action as `NeedsManualReconciliation`. After demonstrably successful Release/Unlock, the terminal outcome is `RejectedBeforeExternalCommitment`; an unknown/failed residual follows the ordinary compensation/reconciliation policy. A duplicate of the same `InventoryReserved` does not return the Flow to the capture path afterward.

#### Target-side Payment authorization and result trace

When Checkout has accepted the `Payment.Capture` output above, the route derives `paymentCommandSource` from that exact source frame. The trusted Payment binding boundary verifies the accepted source tuple, effective protocol identity, target-owned command payload, grant/provenance origin, field and byte bounds, and every triggered context validity rule before it constructs:

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

`PaymentBall` owns the schema and semantic interpretation of `paymentContext`; the binding boundary only verifies and bounds its declared fields. The Payment Nucleus receives `paymentCommandPulse` as the current cause, applies the Policy Gate from Payment State plus that verified context, and is the only component that decides business permission.

On permission and invariant success, Payment accepts the target-owned operation and its immutable provider action in one target frame:

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

Between acceptance of that target frame and acceptance of a later provider-result frame, a redelivery with the same effective protocol identity, `paymentCommandSource`, and command fingerprint receives only verified ACK proof of the original accepted Payment frame and its pending-result state. Payment does not wait for the provider, rerun `decide`, increment revision, create a provisional `ModuleResultOutput`, or repeat `CaptureProviderPayment`. A crash in this interval recovers the accepted frame/pending action according to the selected profile and preserves the same duplicate outcome. Once Payment has accepted a result frame, the same duplicate instead redelivers proof of that exact frame with unchanged `commandSource`, `resultSource`, target revision, result handle/ordinal, and payload; only its delivery `AttemptId` changes. A different fingerprint or conflicting accepted-frame/result evidence fails closed.

This target Decision and revision are already accepted before provider execution. Immediately before executing `CaptureProviderPayment`, the Payment Execution Gate verifies the accepted Effect identity and immutable payload, approved proof issuer/integrity, minimum capture capability, actor/audience/action/object/operation and every constrained field, proof/context/object versions, current freshness/expiry/revocation, provider endpoint, quota, and safe-sink binding. It makes no business choice and cannot replace the accepted action with another payload or ambient credential.

Provider success, known provider failure, ambiguity, or Execution-Gate rejection returns as a closed `Fact` bound to that accepted `EffectRequest`. The Payment Nucleus then accepts the corresponding target-owned state/result frame, for example:

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

The same target-owned result shape is used for a successful `PaymentCaptured`, a provider rejection/failure, or `PaymentCaptureOutcomeUnknown` according to the closed mapping in §16.1. The result route derives `resultSource` from this accepted Payment frame and constructs the verified `ModuleResultPulse`; neither Execution Gate nor Assembly constructs a target result.

A Payment business-policy refusal is different from that technical failure. Checkout v1 statically classifies its normal capture refusal as an accepted target result, so the Payment Nucleus accepts:

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

The three stages are therefore disjoint:

| Stage | Authority and result |
|---|---|
| Before Payment `decide` | The trusted target boundary may return the statically declared `CommandRejectedBeforeAcceptance` carrier for invalid source/protocol/provenance/context, validation, or admission. It creates no Payment Decision, revision, Effect, or result. |
| Payment Nucleus Policy Gate | A normal business refusal is an accepted target Decision with target-owned `ModuleResultOutput(Rejected(...))`; source facets become `Accepted + Rejected`. |
| After target acceptance | Execution-Gate or provider failure is a Resource `Fact`/status path followed by an accepted target result. It cannot become the pre-acceptance carrier, roll back the accepted Payment operation, or downgrade accepted-result evidence. |

A payment result may arrive:

- after the ACK;
- before the separate ACK, but with accepted-command provenance;
- as `OutcomeUnknown` after a provider timeout.

The Flow does not treat the absence of an ACK as proof of nondelivery if the command might already have been accepted.

### 16.8. Unknown payment outcome

The two unknown cases are not conflated.

If delivery might have left the source but target acceptance has not yet been proven:

```text
payment.dispatch = Dispatched
payment.acceptance = AcceptanceUnknown
payment.outcome = Pending
phase = ReconcilingPaymentAcceptance
```

The cause of this transition is `CheckoutCommandDeliveryObserved(CommandDeliveryAmbiguous)` for the matching `paymentHandle`, not a source commit or direct runtime write. `CommandDispatchStopped` whose retained `lastObservation` also proves "might have left the source" without acceptance proof retains the stop facet and starts the same reconciliation path. The `steps.paymentReconciliation` field ensures that duplicates or multiple ambiguous attempts do not create a second status command with a different semantic identity.

If the target accepted the command and returned accepted-command proof, but the provider outcome was lost:

```text
payment.dispatch = Dispatched
payment.acceptance = Accepted
payment.outcome = OutcomeUnknown
phase = ReconcilingPaymentOutcome
```

The second form cannot degrade back to `AcceptanceUnknown`: target acceptance has already been proven. In both phases, the Flow creates a declared command with a new identity for the status step while preserving the identity of the original capture:

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

`Payment.GetOperationStatus` is deliberately a command rather than an ordinary read because Checkout requires a provenance-bound accepted result, stable reconciliation-step identity, idempotent replay, and status evidence. For every new `statusHandle`, Payment accepts a same-state target Decision and increments its target revision:

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

An `EventJournal` Payment binding records the same acceptance as `Accepted(NoDomainChange { outputs = [...] })`. Redelivery of the same effective protocol identity and `commandSource` within the idempotency horizon returns the prior accepted result and target-frame proof without another Payment Decision or revision. A status lookup that needs none of this accepted-operation evidence would instead be a `ReadDependency`/`Query` and would create no target Decision, revision, or output.

As a worked-example reconciliation projection of the source clauses for `PBA-20`, `PBA-21`, `PBA-22`, and `PBA-26`, together with the supporting workflow-sovereignty contract in §10.4, this transition:

- preserves the original capture identity;
- keeps a timeout as an unresolved outcome rather than treating it as permanent failure;
- retains inventory and keeps another checkout unavailable at this point; and
- establishes whether capture occurred before starting a refund, unless the provider contract makes earlier refund initiation safe.

Possible reconciliation results:

```text
Captured(acceptedCommandProof, paymentRef)
DefinitelyNotCaptured(acceptanceEvidence)
StillUnknown(acceptanceFacet, evidence)
```

All three values are accepted `Payment.GetOperationStatus` results. `Captured` immediately records the original capture as `acceptance = Accepted, outcome = Succeeded`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` describes the original `Payment.Capture`, whereas the status command itself has `acceptance = Accepted`; its other form distinguishes an accepted original capture with provider-level `Rejected/Failed`. `StillUnknown` preserves the supplied original-capture acceptance facet and does not collapse `AcceptanceUnknown + Pending` into `Accepted + OutcomeUnknown` without proof.

For Checkout protocol v1, accepted `StillUnknown` exhausts the only automatic payment-reconciliation generation. After verifying the exact `statusHandle`, `commandSource`, `resultSource`, effective Payment protocol identity, supplied original-capture facet, and bounded evidence, Checkout accepts this terminal source Decision:

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

The empty output sequence is deliberate: this version creates no second status handle or generation, timer, command, Effect, refund, release, unlock, or automatic reopening. The accepted status result and its bounded evidence remain available under the selected accepted-source/status retention contract for the declared audit/recovery horizon; source durability does not claim provider outcome. Exact redelivery of the same status result is idempotent and creates no new Checkout Decision, revision, handle, or terminal frame. Later stronger proof cannot silently rewrite `NeedsManualReconciliation`; consuming it requires a separately declared manual/recovery artifact with its own authority, input, transition, and evidence. No such artifact is part of Checkout v1.

`Captured(..., paymentRef)` does not leave the reference only inside the reconciliation Pulse: after status-result provenance is verified, it follows the same transition that retains `VerifiedStepValue<PaymentRef>` and creates the Order output as the direct `PaymentCaptured` below.

### 16.9. Order confirmation

After proven capture:

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

The direct result must have `commandSource.semanticHandle = paymentHandle`; the reconciliation result must have `commandSource.semanticHandle = statusHandle` and demonstrably describe the same original authority action/provider idempotency key through `acceptedCommandProof`. Both paths derive observation handle/version/provenance from the canonical `ModuleResultPulse`; no Checkout alias replaces `commandSource`, `resultSource`, or effective protocol identity. Independent valid proof of the same P through another declared route is corroboration: it creates no second `Order.Confirm` and does not overwrite the accepted value; another P/authority version or a conflicting original-action proof fails closed. `verifiedPayment`, next state, and the Order output are accepted in one frame. The Order target rechecks its own invariants and expected references. The Flow cannot force Order to accept invalid state merely because the preceding steps succeeded.
