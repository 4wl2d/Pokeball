# Core part — Manifest and source organization

[Core contents](../pokeball-architecture-core.md) · [← Profiles, limits, and performance](12-profiles-and-limits.md) · [Catalog search example →](examples/15-catalog-search.md)

> Canonical part 10 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 14. Minimal manifest and source-code organization

### 14.1. Role of the manifest

`BallManifest` is one possible materialized view of a `Ball`'s architectural contract. It supports deployment, review, documentation generation, static checks, and conformance claims, but is neither a mandatory source file nor a runtime registry. A closed typed source declaration may be authoritative; tools SHOULD derive a manifest view instead of making developers repeat the same protocol and policy.

The presence of a manifest field does not prove that the implementation complies with it. For example:

- `singleWriter: true` requires actual scheduler or storage enforcement;
- `maxStructuralAllocations: 0` requires a benchmark;
- `capability: Catalog.Search` requires a concrete restricted client, credential, or broker;
- `noCrossStateReads: true` requires inspection of the dependency graph and code.

At an early stage, the contract MAY be a small YAML or JSON file or a typed declaration in source code. A complete schema package is not part of Core. The authoritative source for each fact must be unambiguous:

```text
owned protocol/state       -> typed Ball source or one manifest
inter-Ball routes          -> Assembly
Flow participation         -> Flow source + participant Application Surface + existing dependency references
shared profiles/limits     -> exact project/binding policy
Ball-specific differences  -> local policy delta
claim evidence             -> claim record
```

An ordinary local Ball needs no standalone manifest when its used closed protocol, state authority, decision entry point, resources, and effective construction/local/policy resolution are statically recoverable from source and enclosing scope. A release or conformance tool may generate the fully resolved view.

`spec.protocols` lists only protocol variants owned by the `Ball` itself when the manifest is the authoritative inventory. For an independently versioned protocol, every non-empty owned category MUST be declared inline or resolved through exactly one explicit, version-pinned authoritative reference. In a same-build typed source, the closed union/type declarations are the inventory and need not be copied. An absent category denotes an empty closed set; absence requires no empty list or zero-valued limit. This rule is the same for `FeatureBall` and `FlowBall`.

Every owned `queries` entry that exists MUST define one `query -> result` pair in which both payload types belong to the same effective protocol identity. The result type is the payload of canonical `ReadResult<ResultPayload>` in §6.3 when the stamped-read trigger applies, not a committed `ReplyOutput` or `ProjectionOutput`. A query with zero or multiple result mappings is invalid.

When a manifest is the authoritative dependency inventory, each non-empty `dependencies.reads` entry imports exactly one target-owned query/result mapping and names the target authority, target read/status authority, effective protocol identity, and caller freshness/consistency requirement. The Assembly route supplies the binding. The caller does not repeat the target types under `spec.protocols`, and optional route fields are present only when triggered. For example:

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

If Pricing and Catalog can version independently, the dependency replaces the same-build identity with the exact target protocol version selected by Assembly. The caller's declared requirement cannot strengthen the target stamp into a multi-source or later-freshness promise.

When a manifest is the authoritative Flow composition view, each Flow/participant pair appears exactly once as the §10.2 `FlowParticipation` tuple. The participant Application Surface contains only participant-owned public types/entrypoints. Every referenced read, command, or signal dependency remains declared in its existing dependency section and bound by Assembly; the participation view neither duplicates its protocol identity nor becomes a runtime message. In the Checkout example below, enclosing `metadata.id: Checkout` is the manifest identifier for the §16 `CheckoutFlowBall` and resolves `flowAuthority`; `flowOwnedCoordinationRef` resolves the non-empty bounded `ownedCoordination` set; and each row supplies the participant authority, exact Application Surface, and dependency references without repeating those common values.

Imported `ModuleCommand` and `ModuleResult` types belong to the target. The authoritative target source, or its manifest projection, declares exactly one command-to-result mapping for each operation and statically classifies every reachable refusal. For example, the Payment-owned mapping for its accepted status command may project as:

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

The concrete mapping lists only reachable responses/results; it does not add empty categories. In `dependencies.commands`, a qualified `operation` together with an explicit target protocol version or exact same-build type identity MUST unambiguously import that one target-owned mapping and MUST NOT redeclare either payload as an owned type of the caller or Flow. At an independently versioned boundary, the selected version MUST match `Assembly.consumerProtocolVersion`. Assembly binds the command ingress and result return but has no authority to change the mapping or refusal classification. Internal state variants and operation facets do not become separate manifest variants merely because they are stored in `State`.

An applicable shared guardrail is selected once by its authoritative project/profile/Assembly/binding scope with one exact immutable reference, for example:

```yaml
policy:
  ref: policy:ShopApplication/local-standard@3#sha256:4f923e8b1dbfa6a628f180be90218e9ff9ed5e4a86ad6ff08b3a1c1ad5fe8a77
```

This URI and digest are illustrative syntax, not a policy artifact shipped by Core. A real trigger is unresolved until the consuming project replaces the example with an existing immutable in-scope artifact whose exact bytes/digest and authority can be verified. A policy selects only mechanisms, values, evidence, or permitted deltas that Core leaves to that scope; it cannot suppress a trigger, weaken a law, or act as a waiver.

The referenced artifact contains the complete §0.2 ownership, scope, mechanism/value, enforcement, and evidence metadata. A covered Ball repeats no scope-level reference; it records only a different explicit selection or allowed local delta. Static resolution MUST produce one complete effective contract for every triggered guardrail; the reference is not a runtime lookup.

### 14.2. Minimal Core manifest

This same-build singleton `Inline + Transient + InProcess + Standard` Ball has one fixed-size Intent, fixed-size state, no collection, no semantic output, no external resource, no retry, no lifecycle/status path, and no claim. Typed source/control flow closes the protocol and proves the fixed dimensions; the enclosing binding selects the default profile once, so the Ball needs neither `StateKey` nor a policy row.

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

The absence of `effects`, `commands`, outputs, retry, profiles, and evidence is meaningful because the closed source inventory proves those paths absent. No `0`, empty optional section, forbidden-capability list, lifecycle enum, or `N/A` explanation is added. The effective contract still has one authority, one writer, bounded input/state/decision work, pure decision, atomic acceptance, and no ambient authority.

Because source and the enclosing binding already carry the authoritative facts, this standalone YAML is optional and may be generated.

### 14.3. Manifest for a Flow

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
        maxFlowParticipants: 4
        maxParallelBranches: 2
        maxOutputsPerDecision: 8
        maxCausalDepth: 16
        maxTransitionSteps: 4096
        maxCompensations: 4
        maxCapturedInputBytes: 65536
        maxRoutesPerFlow: 9
        maxRetainedDispatchStops: 10
```

The inline manifest declares every non-empty Checkout-owned surface used by the example: mutation intents `CheckoutStarted` and `CancellationRequested`, the trusted post-commit control input `CheckoutCommandDeliveryObserved`, the query and result mapping `GetCheckoutStatus -> CheckoutStatus`, and the committed reply payload `RequestAccepted`. `CheckoutStatus` is the sole closed payload in §16.13 for a known, absent, or retention-expired operation; neither a transport 404 nor a second result type replaces it. Customer cancellation passes through Interaction and is not declared as a `ControlPulse`; `CheckoutCommandDeliveryObserved` is accepted only from a declared trusted runtime or route origin after handle and provenance verification. The four `flowParticipations` rows resolve Cart, Inventory, Payment, and Order exactly once against `Checkout.ownedCoordination`; Auth is a supporting context authority and Checkout is the Flow owner, so neither is a participant row. Participant command/result mappings and their static refusal classifications remain imported through exactly nine versioned `dependencies.commands` entries rather than copied into `spec.protocols` or the participation rows; this preserves target ownership and the prohibition on protocol re-export. Under §10.9, Checkout therefore owns thirteen counted declarations: four distinct `FlowParticipation` rows plus nine distinct `DeclaredCommandDependency` rows. A participation reference does not erase the referenced command declaration or create a third copy. The corresponding §14.4 route inventory contains exactly nine effective command round-trip rows; each row binds verified `ModuleCommandPulse` ingress and accepted-frame `ModuleResultPulse` return for one target-owned mapping and counts once, not twice.

The exact `workflow-durable` policy supplies the unchanged shared profile, ingress, retry, delivery, capability, and base limit contracts. Checkout records only its permitted workflow-specific deltas. The displayed `maxTransitionSteps: 4096` is complete only because that exact policy also resolves one immutable `DecisionWorkMeter` identity/version, transition artifact version, unit definition, and overflow policy under §8.3; the numeric delta cannot silently replace the meter. The same policy supplies Checkout's effective numeric `maxStateBytes` and `maxOutputBytesPerDecision` through their dimension-specific immutable `BoundedByteMeasure` tuples, or closes either dimension by a static bounded-type-and-representation proof; the local manifest does not copy those shared contracts. Checkout's closed output algebra, nine-route graph, per-Decision output bound, and causal-depth bound provide the static §10.9 proof for its effective `maxCumulativeFanout`, so no local counter or duplicate numeric row is shown; a binding that cannot retain that exact static proof must instead resolve and enforce the same cumulative branch ceiling through its existing causal reservation. Its closed protocol contains no private `EffectRequest`, so no zero-valued effect limit or `not-applicable` row is required: all work is performed through declared participant commands. Domain-specific `maxRetainedDispatchStops: 10` covers the initial `RequestAccepted` `ReplyOutput` and the nine closed Checkout command-step slots in §16.3, does not exceed the effective `maxCollectionItems`, and does not become a new mandatory Core limit. `stateSchemaVersion: 2` records the retained workflow values added in §16.3; owned and imported protocol variants, dependency versions, and Assembly routes do not change. Migration of already stored state remains a separate rollout or extension contract under §§8.9/10.11: active v1 state must not be silently supplemented with null or default M/S/I/P values; the binding either reconstructs them from declared authoritative migration evidence or disallows normal v2 `decide` and moves the operation to a declared quarantine or manual path.

### 14.4. Assembly declaration

The relationship between a producer and consumer belongs to the composition root—`Assembly`—rather than being hidden in a global locator.

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

Assembly is responsible for concrete wiring. The `ReadDependency` row binds Catalog to the exact Pricing-owned mapping and `PricingQuoteAuthority`; it neither makes those types Catalog-owned nor changes `selected-target-snapshot-only` into a multi-source promise. Each command `to` selects the target-owned command/result mapping; the common `resultTo: CheckoutFlowBall.ModuleResultPulse` is a receiving endpoint, not a Checkout-owned result alias. The Pulse's verified `commandSource` and effective protocol identity select the matching Checkout step and imported target payload. A `Ball` is responsible for the required semantic contract. The runtime is responsible for the invocation or delivery mechanism. Neither Assembly nor generated route code may construct causal tokens, result payloads, stamps, status facts, read meaning, or refusal meaning outside the accepted target/read or command frames in §6.

For the Catalog route, Catalog's version-2 owned protocol is the sole authority for `ProductSelectionConfirmed`; Assembly owns only the producer/consumer version pair and delivery binding. The consumer's `2.0.0` route contract accepts that exact producer payload as `ObservedSignal` and does not redefine it. Its one declared consumer counts as one cumulative branch for the accepted Signal source tuple; redelivery under the same route/consumer identity adds no branch, and the preserved causal scope/depth plus closed graph supplies or selects the remaining §10.9 bound.

For a local monolith, a route may compile to a direct function call. For another process, the same route may use IPC. This must not change domain semantics.

### 14.5. Recommended directory structure

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

Physical directories are not normative. Dependency direction is normative:

```text
interaction -> own public interaction protocol
resources   -> own private resource protocol
nucleus     -> own state + own protocols + Ball-local Nucleus utilities + exact declared target/producer-owned Application Surfaces required by closed Query/Pulse/Decision contracts + mechanical foundation
assembly    -> public protocols + explicit routes + resolved contract views
```

The Nucleus row permits its own Ball-local utility artifacts and owner-authored public semantic types/entrypoints only. Ball-local and mechanical-Foundation imports remain ordinary compile-time graph edges, not a fifth semantic dependency kind. The row transfers no ownership and excludes foreign mutable State, internals, private Resource adapters, platform/I/O implementations, ownerless shared domain utilities, caller-owned mirrors or redeclarations, and protocol re-export. Interaction and Assembly cannot synthesize the imported contract or business meaning.

An optional helper package is recorded under its owning Ball and logical role even if the physical layout places it elsewhere. Only a shared mechanical helper belongs under `foundation/`; physical reuse or directory placement never erases semantic ownership.

Prohibited directions:

```text
interaction -> resources
resources   -> interaction
nucleus     -> UI / HTTP / SQL / filesystem / platform SDK
feature A   -> feature B internals
feature A   -> mutable state of feature B
```

### 14.6. Interfaces, DI, and code generation

Pokeball does not require an interface for every class, a repository abstraction merely for mocking, or a runtime DI container.

An interface is justified when a real boundary exists:

- multiple production implementations;
- platform boundary;
- process/plugin boundary;
- public library contract;
- independently deployable adapter;
- a test double for an external nondeterministic resource, not for a pure Nucleus.

Constructor injection and compile-time wiring are permitted. A service locator and ambient global client are prohibited for application authority.

Code generation MAY create:

- tagged unions;
- exhaustive dispatch;
- bounded output frames;
- manifest consistency checks;
- state diagrams;
- route maps;
- test fixtures.

The generator must not become a hidden runtime framework. Stabilize the semantic model first, then automate repeatable mechanics.

### 14.7. Foundation Quarantine

<!-- pkb:pba-source:start id="PBA-43" title="Foundation Quarantine" -->
**Source clause for PBA-43 — Foundation Quarantine.**

- **Rule:** When a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, business-policy decision, domain authority, route selection, or hidden communication state. A utility shared across Balls is valid Foundation only under that mechanical-only rule. Domain- or business-semantic helper code remains Ball-local or acquires one Ball/Flow owner and is consumed through its declared Application Surface and protocol; it cannot remain an ownerless shared utility or be relabelled as Foundation.
- **Applicability:** `P`: shared foundation exists or a helper/utility is proposed for use by more than one Ball.
- **Declaration owner:** Project foundation/shared-code owner defines its mechanical-only surface; each Ball/Flow retains its domain semantics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency/ownership/state/call-graph scan for local-versus-shared utilities, policy, authority, routes, service locators, and hidden communication.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One project policy serves all Balls for mechanical Foundation only; domain semantics stay local or Ball/Flow-owned, and the Foundation artifact is omitted when no shared mechanical code exists.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

The shared foundation contains only stable mechanical primitives:

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

Domain authorities and universal business models do not belong in the foundation:

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

The foundation may implement pure verification or bounded-container mechanics selected by a trusted binding, but it does not select an approved issuer, interpret permission, choose an Assembly route, retain a domain/status record, or communicate mutable facts between logical roles. Foundation-owned global caches, registries, callbacks, and service locators are invalid when application behavior can observe them as business meaning or a hidden path.

Shared **mechanical** code is extracted when its mechanics, invariants, and change cadence match, not merely when field structure matches. Domain or business semantics do not enter Foundation: they remain as Ball-local implementation artifacts or acquire one Ball/Flow owner and a declared Application Surface/protocol. Small deliberate local duplication is cheaper than an ownerless domain abstraction with high fan-in.

### Definition source records for §14

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Foundation Quarantine" -->
**Foundation Quarantine** — the rule that limits shared foundation to mechanical primitives and prohibits mutable business meaning, policy decisions, domain authority, route selection, service locators, hidden communication state, or an ownerless shared domain/business utility. Domain semantics remain Ball-local or acquire one Ball/Flow owner and a declared Application Surface/protocol.
<!-- pkb:term:end -->


---
