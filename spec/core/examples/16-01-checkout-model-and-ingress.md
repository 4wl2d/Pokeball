# Core part — Checkout Flow: model and ingress

[Core contents](../../pokeball-architecture-core.md) · [← Catalog search example](15-catalog-search.md) · [Checkout Flow: execution →](16-02-checkout-execution.md)

> Canonical part 12 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 16. End-to-end example II: Checkout Flow

Checkout requires a separate `FlowBall` because it has multiple authorities, an independent terminal outcome, cancellation, compensation, and reconciliation.

### 16.1. Roles and Flow participants

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

These are six example authorities/roles, but exactly four Flow participants. `CheckoutFlowBall` is the coordination owner and `AuthBall` supplies trusted actor/context support; neither participates in its own or an undeclared Flow relation. The four resolved `FlowParticipation` participant authorities are exactly `Cart`, `Inventory`, `Payment`, and `Order`, with the nine route dependencies listed below.

Local contracts:

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

Each entry resolves through exactly one target-owned version-1 command/result mapping. Every normal business refusal in this Checkout version is an accepted target result, never a pre-acceptance carrier:

| Command | Accepted result path |
|---|---|
| `Cart.LockForCheckout` | Success or domain lock refusal/conflict. |
| `Cart.Unlock` | Success, rejection, failure, or unknown. |
| `Inventory.Reserve` | `InventoryReserved` or `InventoryReservationRejected`. |
| `Inventory.Release` | Success, rejection, failure, or unknown. |
| `Payment.Capture` | Captured, rejected, failed, or outcome-unknown. |
| `Payment.CancelCapture` | Accepted-before-start/in-progress, too-late, rejected, or unknown. |
| `Payment.Refund` | Success, rejection, failure, or unknown. |
| `Payment.GetOperationStatus` | `Captured`, `DefinitelyNotCaptured`, or `StillUnknown`. |
| `Order.Confirm` | Confirmed or definitive rejected, failed, or unknown. |

`InventoryReservationRejected` is always an accepted `Inventory.Reserve` result and never `CommandRejectedBeforeAcceptance`. `DefinitelyNotCaptured(RejectedBeforeAcceptance, evidence)` is an accepted result of the new `Payment.GetOperationStatus` command that describes the original `Payment.Capture` acceptance facet; it is not pre-acceptance refusal of the status command. A definitive `Order.Confirm` refusal after capture is an accepted Order result. The Flow may later accept its own `RejectedBeforeExternalCommitment`, but that workflow outcome does not copy a carrier reason. Validation/admission failures and any separately enumerated target `DecisionRejected(BusinessRejection)` remain pre-acceptance only under the static versioned target mapping in §§6.9 and 14.1.

`CheckoutFlowBall` knows the sequence and terminal meaning. Participants know only their own invariants and operations.

The owned surface inventory for Checkout `protocolVersion: 1.0.0` is defined by `spec.protocols` in the §14.3 manifest. `CheckoutStarted` and `CancellationRequested` resolve as `Intent`, `CheckoutCommandDeliveryObserved` as a trusted post-commit `ControlPulse`, `GetCheckoutStatus -> CheckoutStatus` as an owned Query/result mapping, and `RequestAccepted` as a committed `Reply` payload. Named participant results, including `CartLocked`, `InventoryReserved`, `PaymentCaptured`, status, and cancellation outcomes, are imported target-owned `ModuleResult` payload variants delivered inside canonical `ModuleResultPulse`; they are not bare Checkout Pulse variants or owned Checkout types.

### 16.2. Ingress and idempotency

The external mutation contains:

```text
CheckoutStarted {
    cartId
    paymentMethodRef
    expectedCartVersion
    idempotencyKey
}
```

For `CheckoutStarted start` and trusted `AuthenticatedActorContext A`, one canonical semantic function applies:

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

`ScopedStableSubject` makes a lexical subject ID unambiguous within issuer/realm scope under §11.2. `fingerprintV1` receives one labeled typed tuple in the order shown; the concrete binding defines deterministic canonical encoding/digest and an artifact version, but does not change the field set or its sources. Interaction and the Checkout transition use the same `CheckoutStartFingerprintV1`, selected by compatible artifact versions.

`start.idempotencyKey`, `RequestId`, trace ID, retry/attempt data, and the reply channel are not fingerprint operands. They are not added through whole-Pulse hashing or ambient metadata. Interaction stores `IdempotencyScope + start.idempotencyKey + F`, where `F = CheckoutStartFingerprintV1(start, A)`; the accepted Checkout frame must atomically store the same `F` in `CapturedCheckoutInput.source.ingressFingerprint`. The Nucleus receives `start` as the current `Pulse`, `A` as trusted `DecisionContext.actorContext`, and verified `IV` as `DecisionContext.artifactVersion`; `IV` identifies the selected Interaction fingerprint artifact and is distinct from the Nucleus transition artifact. The accepted-input ledger/history is not read as hidden input.

After a Checkout root operation is accepted, same key + same fingerprint within the declared legal retry horizon redelivers proof of the original accepted `ReplyOutput(RequestAccepted(operationId))` frame. The replay preserves the exact Checkout `BallInstanceId`, accepted revision, materialized `OutputId`, reply `semanticHandle`, `sourceOrdinal = 1`, payload/`OperationId`, retained ingress fingerprint, and Interaction artifact version; only the mechanical delivery `AttemptId` changes. It runs no second Checkout `decide`, creates no revision, accepted frame, command, semantic output, or status source, and cannot replace the original accepted actor/payment/artifact binding. The accepted reply frame and idempotency record are retained for at least that retry horizon.

Root validation, admission, or Decision rejection commits no operation/`OperationId` mapping, so a later retry cannot observe or return a rejected root operation and may make a new acceptance attempt under the same idempotency contract. Same key + different fingerprint is detected only by the declared ingress/idempotency authority while its conflicting covered record is retained and before any `CheckoutStarted` Intent is constructed. It returns exactly `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, leaves the prior accepted operation unchanged, and creates no Checkout operation, Decision, revision, handle, Reply, output, or status source. `IdempotencyConflict` is neither `DecisionRejected` nor a separate owned manifest protocol variant.

### 16.3. Flow state

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

Retained values have the following closed, bounded forms:

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

`CapturedCheckoutInput.source` binds M to the current `operationId`, exact output of `CheckoutStartFingerprintV1(start, A)`, Checkout protocol version, and exact verified Interaction fingerprint artifact version `IV` supplied by the current Context; `ingressFingerprint` is byte-for-byte equal to the fingerprint in the atomically accepted Interaction idempotency record. `interactionArtifactVersion` is not `transitionArtifactVersion`. The typed Intent itself has already been created by the verified Interaction boundary, while actor provenance is retained in the field-minimized binding below. No field requires reading the accepted-input ledger after commit. `actorBinding` is a stable-subject digest from the verified initial `AuthenticatedActorContext`, not a credential, session, or unrestricted principal. Optional `actorContextId` is permitted only as a non-authorizing issuer record reference that remains stable for at least the workflow horizon; if the issuer rotates/expires it, a fresh context/grant proves the same subject through digest + issuer + realm. Possession of the binding by itself authorizes nothing. `resultProvenance` is a bounded redacted descriptor/digest of already verified issuer and causal evidence for the result's authority action, not a reference that the `Nucleus` later dereferences in runtime history. Input `sourceProtocolVersion` equals the selected Checkout version; result authority/observation versions come from version-pinned dependencies. These fields are captured workflow values, but they do not make the Flow the authority for the cart, payment method, reservation, or payment: the participant still checks its own target and invariants.

For a direct participant result, `authorityActionHandle`, `observedViaStepHandle`, both protocol-version aliases, and `resultProvenance` derive only from the verified `ModuleResultPulse.commandSource`, `resultSource`, `effectiveProtocolIdentity`, and `issuerProvenance`. For reconciliation, the observation aliases derive from the current status-result Pulse, while the original authority aliases derive from its nested verified accepted-command proof. Checkout does not accept an independently supplied handle/version metadata envelope or let Assembly synthesize these aliases.

This derivation correction changes `transitionArtifactVersion`, but not the public `protocolVersion: 1.0.0`, declared state shape, `stateSchemaVersion: 2`, or routes. A persisted schema-v2 record created by a prior artifact cannot be considered correct based on shape alone: before normal recovery/`decide`, rollout verifies equality of the retained value with authoritative accepted idempotency evidence or performs a declared deterministic migration. Missing evidence or a mismatch leads to a quarantine/manual path; the normal Nucleus receives no ledger read. If a concrete binding cannot distinguish artifacts or safely migrate a record in its persisted form, it increments its own state schema version under §10.11.

While a dependent transition remains reachable, these invariants apply:

- `retained.checkoutInput` appears atomically with acceptance of `CheckoutStarted`; M is retained while a capture decision is reachable, and the actor binding until the last reachable privileged order/cancellation/compensation decision. This closed example shape retains the entire bounded record until the later of the two triggers;
- `cartSnapshot` appears only from a verified result matching `steps.cartLock.handle` and is retained until a terminal order or terminal unlock/reconciliation;
- `retained.inventoryReservation` appears only from a verified result matching `steps.inventoryReservation.handle` and is retained until a terminal order or terminal release/reconciliation;
- `retained.paymentCapture` appears only from a verified direct/reconciled capture result matching the original payment handle and is retained until a terminal order and terminal refund/reconciliation;
- an exact duplicate of one observation is idempotent; another declared direct/reconciliation proof for the same `authorityActionHandle`, authority version, and canonical value is corroboration and creates no output/overwrite, while another value/authority version or a conflicting proof for the same authority action is an invariant/provenance fault;
- a value cannot be deleted merely because a dependent command was emitted: cleanup is permitted after the last reachable consumer and acceptance of all mandatory terminal/compensation outputs under the declared retention policy.

All retained records pass the selected `maxInputBytes` measure, `maxCapturedInputBytes` where applicable, and the selected `maxStateBytes` measure for the complete candidate next frame. Each numeric Core byte dimension resolves its exact §8.3 tuple unless a static type-and-representation proof closes it. Values are not truncated: input `N+1` fails before trusted semantic acceptance/`decide`, and State `N+1` rejects the entire Decision before acceptance. Payment references, actor binding, and provenance are classified as sensitive and do not enter Reply, Projection, status, or an ordinary log/metric/trace; status exposes only a redacted typed lifecycle.

Every step stores semantic data:

```text
Step {
    handle: SemanticHandle
    dispatch: NotDispatched | Dispatched | DispatchStopped
    acceptance: NotAccepted | Accepted | RejectedBeforeAcceptance | AcceptanceUnknown
    outcome: NotExpected | Pending | Succeeded | Rejected | Failed | Cancelled | OutcomeUnknown
    cancellation: NotRequested | Requested | CancellationAcceptedBeforeStart | CancellationAcceptedInProgress | CancellationTooLate | CancellationRejected | CancellationUnknown
}
```

The facets are independent and preserve these invariants:

- `NotDispatched` requires `NotAccepted` and `NotExpected`;
- `AcceptanceUnknown` is permitted only while no verifiable ACK/result/status proof exists;
- a verified `ModuleResultPulse` with accepted-target provenance itself proves target acceptance, advances `NotDispatched -> Dispatched` if the delivery policy has not already stopped, and then advances acceptance to `Accepted`, even if the separate ACK was lost;
- terminal `Succeeded | Rejected | Failed | Cancelled` requires `acceptance = Accepted`;
- `outcome = OutcomeUnknown` does not imply `acceptance = AcceptanceUnknown`: these cases are stored separately;
- a cancellation observation does not delete an already accepted business result.

Post-commit delivery/acceptance observations enter the Flow only as an owned `ControlPulse`:

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

`CheckoutCommandDeliveryObserved.observation.CommandRejectedBeforeAcceptance` is a set-equal Checkout projection of the canonical carrier in §6.13:

- the enclosing `source` projects the complete field-minimized `commandSource`; its `semanticHandle` selects the Checkout Step;
- the resolved `dependencies.commands` operation and Assembly version pair project `effectiveProtocolIdentity`;
- `reason` projects `boundaryResponse` without reclassification;
- `acceptanceEvidence` projects `targetBoundaryProvenance`.

At the Checkout source, a verified carrier sets exactly `acceptance = RejectedBeforeAcceptance` and `outcome = NotExpected`. A carried `DecisionRejected(BusinessRejection)` is only the informational pre-acceptance reason and never sets `outcome = Rejected`. By contrast, a verified `ModuleResultPulse` whose accepted target result is `Rejected(...)` sets exactly `acceptance = Accepted` and `outcome = Rejected`. Carrier/result evidence for the same effective protocol identity and source tuple is conflicting evidence and fails closed.

For a same-stack participant route, a pre-acceptance attempt creates no accepted target frame. Checkout applies the refusal through its serialized `ControlPulse` Decision. This retained, recoverable workflow preserves its actual causal-work and completion-capacity bounds under §8.4; a synchronous route adds no mandatory level-1/level-2 reservation scheme and never resets an applicable budget.

If `acceptanceEvidence` is forged, tampered, missing, stale, or bound to the wrong target/version/source tuple, the route constructs no trusted `CheckoutCommandDeliveryObserved`, changes no Step facet, and initiates no compensation. These aliases add or omit no semantic field required by the canonical carrier in this Checkout scope.

The trusted runtime/route boundary creates this Pulse only after source acceptance and verifies that `source.semanticHandle` matches a previously committed Checkout step, that `observationId`/provenance are authentic, and that size and attempts satisfy `maxInputBytes`/`maxDeliveryAttempts`. A source commit by itself leaves a new step in `NotDispatched | NotAccepted | NotExpected`; runtime does not write `CheckoutState` facets directly.

The complete source tuple must resolve to exactly one previously committed `ModuleCommandRequest`; a handle without source commit/ordinal is insufficient. `observationId` is scoped to trusted issuer + source tuple, remains stable when one observation is redelivered, and differs for a new observation; it is a mechanical dedup identity, not a `SemanticHandle` or `AttemptId`. The dedup record is retained for at least the declared duplicate-delivery horizon. Every `reason`/evidence/`lastObservation` is a protocol-versioned bounded redacted value object: a raw transport exception, secret, or unbounded provider payload does not enter the Pulse/status.

The serialized transition applies an observation monotonically:

| Observation | Change to the matching `Step` |
|---|---|
| `CommandDispatched` | `NotDispatched -> Dispatched`; acceptance/outcome are not inferred from send evidence alone. |
| `CommandAccepted` | `NotDispatched -> Dispatched` if dispatch has not already stopped; `NotAccepted \| AcceptanceUnknown -> Accepted`; `NotExpected -> Pending`. |
| `CommandRejectedBeforeAcceptance` | `NotDispatched -> Dispatched` if dispatch has not already stopped; `NotAccepted \| AcceptanceUnknown -> RejectedBeforeAcceptance`; `NotExpected \| Pending \| OutcomeUnknown -> NotExpected`. Accepted/terminal proof conflicts with such an observation and fails closed. |
| `CommandDeliveryAmbiguous` | Permitted only after evidence that the attempt crossed the declared source dispatch point; `NotDispatched -> Dispatched`, and in the absence of acceptance proof, `NotAccepted -> AcceptanceUnknown` and `NotExpected -> Pending`. |
| `CommandDispatchStopped` | Any unfinished dispatch facet advances to `DispatchStopped`; acceptance, outcome, and cancellation are retained, except when verified `lastObservation = may-have-left` without acceptance proof: then `NotAccepted -> AcceptanceUnknown` and `NotExpected -> Pending`. |

The same `observationId` with the same canonical payload is idempotent and creates no new output. The same ID with a different payload, an unknown/stale handle, or incompatible accepted/rejected proofs are invalid trusted input/an invariant fault and are not applied through arrival-order overwrite. A weaker late observation does not regress proven acceptance/result. A late ACK or provenance-bound `ModuleResultPulse` may refine acceptance/outcome after `DispatchStopped`, but the terminal fact that the current delivery policy is exhausted is retained. The current Checkout protocol version does not implicitly reopen a stopped policy.

For example, a result that arrives before the separate ACK is stored as:

```text
dispatch = Dispatched
acceptance = Accepted
outcome = Succeeded
cancellation = CancellationTooLate
```

A result with accepted-command provenance is proof of acceptance; the state `acceptance = AcceptanceUnknown, outcome = Succeeded` is prohibited.

### 16.4. Authorization

`AuthenticatedActorContext` is issued by the trusted boundary. The Nucleus checks policy in the current state and creates target-scoped requirements.

For payment:

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

For the initial actor context, the trusted boundary defines a bounded `actorContextId` unambiguously bound to verified `stableSubjectId + issuer + namespaceOrRealm`; the Flow retains only this binding, not the original principal/session. Every later decision that creates a privileged output receives current authorization as a versioned part of `DecisionContext.authorizationSnapshot`:

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

The map is bounded by `maxOutputsPerDecision`, keyed by the exact semantic handle, and is not the current cause of the transition. `subjectBindingProof` is bounded and proves that the current grant actor corresponds to the retained stable subject/issuer/realm; it is neither a credential nor authority by itself. The selected security profile and accepted project overlay declare the trusted issuer/issuance-or-reintroduction boundary, authenticity/integrity mechanism, freshness/revocation policy, and action-specific expiry. For capture, the Nucleus matches the grant against retained `actorBinding`, `paymentMethodRef`, current `cartSnapshot.value.total/currency/quoteVersion`, `operationId`, `paymentHandle`, audience/action/object, and trusted `CurrentActionAuthorization.observedAt`; the target repeats the expiry/revocation check at the actual execution time. Refund/Release/Unlock and other privileged compensation each use a separate entry for their own action handle; the capture grant is neither reused nor broadened.

The Payment Execution Gate accepts a grant only from the profile-declared trusted `issuer`, verifies authenticity/integrity, actor context, audience, action, object, operation, and expiry, and then compares every constrained field with the actual `Payment.Capture` payload fail-closed. Changed `amount`, `currency`, `paymentMethodRef`, or expected version does not inherit the original authorization. Missing, expired, stale-version, or mismatched current authorization before commit creates no privileged output and follows the exact capture/compensation transition in §§16.7/16.10; for compensation that cannot be completed, the terminal outcome is `NeedsManualReconciliation`. At source acceptance, the grant must be valid at trusted `observedAt`; the target checks it again during actual execution. After the command output is committed, its payload and grant are immutable: later expiry/rejection is handled through the existing typed rejected/unknown/reconciliation path, not by ambient refresh or hidden grant substitution.

A raw grant is not a field of live `CheckoutState`. The exact grant inside an accepted command output is retained only in the protected committed output record for the dispatch/redelivery and audit horizon; a durable profile retains the same record under its state guarantee. The grant is redacted from status/log/trace and deleted under the declared security-retention policy after that horizon. Exact cryptographic encoding remains a profile/extension detail; semantic binding is mandatory. The Flow does not pass a general unrestricted principal object.
