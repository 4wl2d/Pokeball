# Core part — Laws PBA-31–44

[Core contents](../../pokeball-architecture-core.md) · [← Laws PBA-19–30](20-02-laws-delivery-composition.md) · [Law applicability and navigation index →](20-04-law-index.md)

> Canonical part 21 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

<!-- pkb:generated:start id="core-laws-31-44" -->
### PBA-31 — Double Quarantine

Source: §11.1.

- **Rule:** Every raw external-input or raw resource-output edge that exists passes through the applicable parsing, validation, finite bounds, and provenance checks before its value reaches the Nucleus.
- **Applicability:** `P`: raw input or resource output crosses a trust/representation edge.
- **Declaration owner:** Interaction/Resource adapter.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Parser/provenance/fuzz tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact validator evidence reusable; absent side needs no placeholder.
- **Primary verification route:** `§17.4`

### PBA-32 — Capability-Sealed Effect

Source: §11.4.

- **Rule:** Every external effect uses the minimum explicit technical authority for its bounded operation class, enforced at a real capability boundary.
- **Applicability:** `P`: an `EffectRequest` reaches the Ball's external resource/authority.
- **Declaration owner:** Ball capability semantics and binding realization.
- **Scope:** The exact external effect path and its bounded operation class.
- **Enforcement / evidence owner:** Binding/resource owner at the scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolation boundary; positive restriction and unrestricted-authority negative tests.
- **Resolution, failure, and conformance:** Authority wider than the declared operation class, or a local check behind unrestricted authority, fails the rule at that effect path.
- **Reuse and absent-trigger behavior:** Capability policy reusable; omit when no external Effect path exists.
- **Primary verification route:** `§17.3`

### PBA-33 — Dual Gate

Source: §11.3.

- **Rule:**
  - When a privileged action requires business and current technical authorization, the Nucleus Policy Gate alone makes the business permission decision from committed State, the current cause, and trusted semantic context.
  - Immediately before authoritative execution, the target/resource Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding.
  - It does so without making a new business decision.
  - Failure after target acceptance is a declared Resource/result/status path.
  - It neither rolls back nor downgrades accepted work.
  - It never becomes a pre-acceptance command carrier.

  #### Policy Gate

  The `Nucleus` alone decides whether an action or actor-dependent read is permitted and which business-visible result follows from the current State, cause or Query, and trusted context. After an actor-dependent read is admitted, the pure read Policy Gate selects one variant from the Query's total target-owned `ResultPayload`: permitted, denied, redacted, or deliberately non-disclosing as that exact protocol declares. It never turns that semantic outcome into `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary-invented `NotFound`. A pure read remains non-mutating and creates no `Decision`; this does not move its semantic permission or result selection into Interaction, Assembly, runtime, or Resource.

  #### Execution Gate

  Immediately before an authoritative action, the target or resource enforces the applicable subset of this catalog without revisiting business permission:

  - accepted-action identity and immutable payload binding when execution follows an accepted output;
  - minimum technical capability when an external resource is used;
  - authenticity and integrity of actor context and, for a privileged action, any required grant from an approved issuer for the relevant realm, audience, and action;
  - binding of actor, context, audience, action, object, operation, and each constrained field when such constraints authorize the actual target or payload;
  - proof/grant version and expiry, freshness, or revocation when those policies exist;
  - expected target/object version when concurrency or a current-version constraint exists;
  - idempotency identity when duplicate execution is possible;
  - endpoint identity for a network endpoint;
  - local quota or budget when quota enforcement is present;
  - safe-sink constraints at an interpreter edge.

  Resource does not reinterpret business intent or apply a second business policy. It verifies only the technical authorization needed to execute the already accepted action. Fields and checks whose trigger is absent are omitted rather than populated with defaults.

  The Execution Gate fails closed for every triggered proof: required evidence that is missing, unverifiable, expired, stale, revoked, wrong-version, or mismatched does not authorize an authoritative action and is not replaced by an ambient credential, transport authentication, or a trusted string-valued `issuer`. Rejection returns as a declared typed Resource outcome bound to the accepted action. If target acceptance already occurred, the owning Nucleus may accept the corresponding target-owned result/status transition, but neither the gate nor its failure rewrites that history as `CommandRejectedBeforeAcceptance`. Authentication of an IPC peer alone does not make that peer an approved authorization issuer.
- **Applicability:** `R`: a privileged action needs business and current technical authorization, or an actor-dependent read can select more than one semantic permission/result outcome. Only the gate obligations present on that path materialize.
- **Declaration owner:** Ball/Nucleus owns action/read business permission and read result selection; target/resource owns the immediate technical Execution Gate for an action.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Sole-policy-site, gate-order, proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink, and post-acceptance failure tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Gate mechanics/evidence reusable; action/read meaning remains local; omit the Execution Gate for a pure read and omit the whole rule when neither trigger exists.
- **Primary verification route:** `§17.8`

### PBA-34 — No Ambient Authority

Source: §11.5.

- **Rule:** Every Ball and binding keeps application authority and mutable business communication out of ambient globals, credentials, service locators, runtime registries, and shared foundation state; dependencies and communication paths are explicit in construction, protocol, or static Assembly.
- **Applicability:** `A`: every Ball/binding.
- **Declaration owner:** Project dependency and explicit-communication contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports, call graph, global-state/service-locator/runtime-registry/foundation scans.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared injection convention may be reused; actual graph proves absence of ambient authority and hidden communication.
- **Primary verification route:** `§17.5`

### PBA-35 — Safe Sink

Source: §11.6.

- **Rule:** Every interpreter or dialect boundary that exists uses the applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- **Applicability:** `P`: data reaches an interpreter/dialect.
- **Declaration owner:** Resource/sink owner.
- **Scope:** The exact interpreter or dialect edge and its data/context interpretation.
- **Enforcement / evidence owner:** Owning Interaction/Resource sink implementation; parameterized, structured, capability-rooted, and context-encoded positive fixtures plus raw-injection/traversal negatives as applicable.
- **Resolution, failure, and conformance:** A typed wrapper without an applicable safe sink, or raw data interpreted as code/path/dialect syntax, fails that edge.
- **Reuse and absent-trigger behavior:** Adapter/evidence reusable; omit with no interpreter edge.
- **Primary verification route:** `§17.3`

### PBA-36 — Secret Containment

Source: §11.9.

- **Rule:** A secret does not enter state, output, persistence, serialization, logs, or telemetry without an explicit policy for that exact path and scope.
- **Applicability:** `R`: a secret can enter State, any output, persistence, serialization, logs, or telemetry.
- **Declaration owner:** Project classification and affected owner.
- **Scope:** Each exact reachable secret path and its Ball/project/binding scope.
- **Enforcement / evidence owner:** Affected State/output owner plus persistence, serializer, log, and telemetry owners; full data-flow and non-log leakage tests.
- **Resolution, failure, and conformance:** Any reachable path without an explicit policy for that exact path and scope fails closed; a policy for one sink does not cover another.
- **Reuse and absent-trigger behavior:** Exact secret policy reusable; omit when closed data flow proves no secret.
- **Primary verification route:** `§17.8`

### PBA-37 — Explicit Unsafe Escape Hatch

Source: §11.10.

- **Rule:**
  Raw SQL, shell, an arbitrary URL, unsafe deserialization, or unrestricted filesystem access is permitted only as an explicitly named unsafe operation with:

  ```text
  owner
  reason
  scope
  capability
  isolation decision
  security review
  tests
  expiry/remediation
  ```

  An escape hatch must not masquerade as an ordinary `Effect`.
- **Applicability:** `R`: raw authority/unsafe escape exists.
- **Declaration owner:** Named security owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Capability gate/scan/audit tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Controls reusable, but each site resolves; omit register when scan proves none.
- **Primary verification route:** `§17.8`

### PBA-38 — Bounded Execution

Source: §8.3.

- **Rule:**
  - Every present variable dimension is finitely bounded as specified below.
  - A triggered operation-status materializer reserves its operation, pending, facet, and delivery-stop capacity before source acceptance.
  - It never evicts or truncates an accepted fact because of later pressure.
  - A `ReadDependency` remains subject to the already effective input, read-work, response, route, and buffering bounds.
  - The mere existence of a `Query` creates no new mandatory per-Ball read-limit field.
  - When admission can fail, the concrete finite closed `AdmissionFailure.reason` union bounds its outcome space.
  - No open reason string is permitted.
  - Each same-stack command uses §8.4's one next-level alternative-completion reservation: exactly one accepted target Decision or source carrier Decision consumes it, never both; successful target acceptance separately reserves the following result completion, and unavailable capacity prevents acceptance rather than losing a carrier or resetting the causal budget.
  - When `maxCumulativeFanout` is present, §10.9/PBA-29 defines its one causal-scope branch unit, aggregation, duplicate treatment, and exact boundary. A static proof may establish the effective ceiling; otherwise the existing total causal reservation admits the whole candidate output batch before acceptance. The first branch at `N+1` rejects that entire candidate Decision with `AdmissionFailure(CausalBudgetExceeded)` and dispatches no subset.

  Every variable dimension that exists on a reachable path has a finite effective bound. The proof for one dimension is exactly one of:

  1. a closed bounded type or static control-flow proof;
  2. an exact reusable policy reference resolved under §0.2;
  3. a local declaration, or an allowed local delta over that reference.

  The canonical dimensions are:

  ```text
  maxInputBytes
  maxStateBytes
  maxCollectionItems
  maxOutputsPerDecision
  maxOutputBytesPerDecision
  maxEffectsPerDecision
  maxCommandsPerDecision
  maxCausalDepth
  maxCumulativeFanout
  maxRetriesPerOperation
  maxTransitionSteps
  ```

  The names and units are canonical when a numeric declaration is used:

  | Limit | Trigger and scope |
  |---|---|
  | `maxInputBytes` | Raw or normalized input has variable byte size: maximum bytes for one candidate input at the exact selected boundary stage under the binding-owned byte-measure contract below. The representation definition names the included boundary metadata and `DecisionContext` fields. A bounded input type may prove it statically. |
  | `maxStateBytes` | Retained State has variable byte size: maximum bytes for the complete candidate `nextState` under the binding-owned byte-measure contract below. A fixed-size State type and representation may prove it statically. |
  | `maxCollectionItems` | A retained or processed collection has variable length: maximum items for each named collection; cumulative bytes still apply. |
  | `maxOutputsPerDecision` | A Decision can contain outputs: maximum complete output sequence length. A fixed output algebra/control flow may prove it statically. |
  | `maxOutputBytesPerDecision` | A Decision can contain a variable-size output sequence: maximum bytes for the complete ordered accepted output sequence under the binding-owned byte-measure contract below. A fixed bounded output algebra and representation may prove it statically. |
  | `maxEffectsPerDecision` | `EffectRequest` exists: maximum such outputs in one Decision. |
  | `maxCommandsPerDecision` | `ModuleCommandRequest` exists: maximum such outputs in one Decision. |
  | `maxCausalDepth` | An accepted consequence can cause another mutating Decision or a route spans multiple Decision hops: maximum total hops including the root. |
  | `maxCumulativeFanout` | Accepted outputs traverse declared routes within one causal scope: maximum distinct accepted output-to-effective-route/consumer branches under the exact §10.9/PBA-29 counting contract. |
  | `maxRetriesPerOperation` | A retry path exists: maximum repeated attempts beyond the initial attempt for the named retry owner and failure mode. |
  | `maxTransitionSteps` | A numeric declaration is used to bound decision work instead of or in addition to a closed bounded type or static control-flow proof: maximum units under one exact versioned binding-owned Decision Work Meter. |

  When a numeric `maxTransitionSteps` declaration is present, its resolved declaration view is:

  ```text
  DecisionWorkMeter {
      meterIdentity
      meterVersion
      transitionArtifactVersion
      unitDefinition
      maxTransitionSteps
      overflowPolicy
  }
  ```

  This is an effective contract view, not a mandatory runtime envelope. The identity and version select one immutable unit definition. For the same binding, transition artifact version, meter identity/version, canonical State, Pulse, and valid `DecisionContext`, the total count is deterministic. Units are non-negative integers and consumption is monotonic. The meter starts once at zero for one `decide` invocation and cannot be reset, split, or restarted by a nested helper, internal phase, loop, retry, yield, or representation erasure to evade the limit. Its scope ends with that invocation; a later `decide` starts a new scope, while causal depth, retry attempts, wall time, execution quantum, dispatch, and route work remain separate dimensions.

  At exactly `N = maxTransitionSteps`, the Decision may complete normally. Attempting unit `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and follows the binding's declared finite typed pre-acceptance or programming-fault policy; it cannot truncate work into a different business result. Numeric counts are comparable across bindings only when meter identity, meter version, transition artifact version, and unit definition are all equal. Core defines no universal step unit and still makes no bit-exact cross-language replay guarantee. When static bounded control flow already proves the transition-work limit, no meter artifact or runtime counter is required.

  When a numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` declaration is used without a static bounded-type-and-representation proof, the binding resolves one immutable measurement contract for that active dimension:

  ```text
  BoundedByteMeasure {
      dimension: Input | State | DecisionOutputs
      measureIdentity
      measureVersion
      representationDefinition
      limitName: maxInputBytes | maxStateBytes | maxOutputBytesPerDecision
      maxBytes
  }
  ```

  `BoundedByteMeasure` is the local name of this PBA-38 effective declaration/conformance view, not a new protocol, runtime envelope, or glossary-inventory term. `dimension + measureIdentity + measureVersion + representationDefinition + limitName` selects one immutable equality domain. Under one tuple, equal canonical values produce equal non-negative byte counts; counts from different tuples are incomparable. `maxBytes` is exactly the effective numeric value of `limitName`, not a second limit. If another representation is retained or transported, `representationDefinition` maps it deterministically to the selected measured byte sequence; representation erasure cannot change the count for equal canonical values under the same tuple.

  For `dimension = Input`, `representationDefinition` selects exactly one measurement stage—`RawBoundaryInput` or `NormalizedTrustedInput`—and one complete candidate-input sequence. It names every included raw or normalized input field, boundary metadata field, and constructed `DecisionContext` field; an omitted field is explicitly outside that input limit rather than ambiently counted. Later transport buffering, allocator layout, compression, encryption, and retry/attempt framing are excluded. The selected count is checked before the trusted semantic input is accepted or supplied to `decide`. At exact `N = maxInputBytes`, the input may proceed; `N+1` returns the binding's declared finite pre-acceptance response and invokes no `decide`.

  For `dimension = State`, the measured value is the complete candidate `nextState` semantic representation before acceptance. `representationDefinition` includes the State schema discriminator/version and every retained State field plus any other semantic metadata it names. It excludes heap/object headers, allocator padding, indexes, storage-record framing, compression, encryption, replication metadata, and transport mechanics. A binding that persists another representation maps it deterministically to the selected sequence. At exact `N = maxStateBytes`, the complete Decision may be accepted; `N+1` rejects the whole Decision before State, revision, or output acceptance and performs no truncation or dispatch.

  For `dimension = DecisionOutputs`, the measured value is one deterministic semantic representation of the complete ordered `Decision.outputs` sequence before dispatch. The bytes include sequence structure plus every required output-envelope field, correlation token, `sourceOrdinal`, and payload. They exclude `nextState`, accepted-frame fields that are not part of an output envelope, and later transport-only framing, compression, encryption, retry headers, or `AttemptId`. At exact `N = maxOutputBytesPerDecision`, the complete Decision may be accepted; `N+1` rejects the whole Decision before State, revision, or output acceptance, and no individual output is truncated or omitted.

  A closed bounded type plus static representation proof may establish any of these ceilings without a `BoundedByteMeasure` artifact or runtime serialization/counting. The proof still fixes the same dimension, complete value scope, and `N/N+1` result; it cannot rely on an unspecified implementation representation.

  An absent output kind, retry path, causal chain, collection, queue, or profile path requires no zero-valued field. Absence is proved by the closed protocol/type/profile/route inventory. A present dimension with no static proof, no applicable reusable policy, and no local declaration is invalid; `unbounded` is never an effective value. Delivery attempts, retention, concurrency, IPC, resource responses, and other profile-specific dimensions resolve by the same rule when their paths exist.

  Every `ModuleResultOutput` counts as one target output under `maxOutputsPerDecision` and contributes its complete target-owned envelope/payload bytes to the target's `maxOutputBytesPerDecision`. A retained, retried, or independently observable result route also counts against the target's finite delivery/status bounds and one stop-eligible target slot; source-side input, command, or output bounds do not substitute for these target bounds.

  When operation status is triggered, the source/status capacity plan has finite effective bounds for every reachable operation record, pending observation, lifecycle/cancellation/result facet, retention marker, and unique delivery-stop record. It reserves the newly reachable capacity before the acceptance point that can create the source fact. Capacity `N+1` prevents that source acceptance and dispatch under the declared typed admission/fault policy; after acceptance, pressure cannot justify eviction, truncation, or loss of the accepted source or observation.

  A shared policy update does not mutate existing effective contracts: it creates a new revision/digest, and a Ball or Assembly adopts that revision explicitly. Two referenced policies that provide the same effective key without an explicit single override relation conflict and fail resolution.

  Overflow MUST NOT cause truncation, partial state, or dispatch of a subset of non-drop-eligible outputs. The Decision is accepted in full or rejected in full.
- **Applicability:** `A`: present input/state/decision/output dimensions; `P`: numeric decision metering, synchronous command completion, collections, reads/routes/buffers, queues, retries, fan-out, concurrency, IPC, delivery, retention, status, or fallible admission.
- **Declaration owner:** Ball/project; binding owns meter identity/unit, each byte-measure identity/version/representation, and source/target reservations; Assembly/runtime/status/profile owner owns other introduced dimensions/reasons.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Meter identity/version/unit resolution; input/State/output byte-measure dimension/identity/version/representation/limit resolution, mapping/erasure invariance, tuple incomparability and exact stage-specific `N/N+1`; equal-input determinism, monotonic no-reset per-`decide` scope, depth/fan-out exact `N/N+1`, alternative-completion transfer, branch identity/aggregation, read bounds, closed reasons, pressure, and overflow tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Static/local/shared proof; static work, byte-size, or fan-out needs no runtime counter or measure artifact, Query alone adds no limit field, absent dimensions/reasons are omitted, unlike meters/byte-measure tuples are not compared, retries/redelivery do not double-count one accepted route branch, and accepted status facts are never evicted/truncated.
- **Primary verification route:** `§17.1`

### PBA-39 — Profile-Proportional Mechanism

Source: §0.2.

- **Rule:**
  - Every guardrail uses exactly one of the four closed applicability classes below.
  - A present trigger resolves statically to one effective guardrail.
  - An absent `path-triggered`, `risk-triggered`, or `claim-triggered` predicate removes its mechanism and ceremony only under the inventory and evidence rules in this subsection.
  - An `always` obligation is never absent.
  - A boundary/adoption worksheet is project-owned `SHOULD` guidance only while adoption or pilot work is active.
  - It is not a universal Core artifact or conformance placeholder.

  Core uses four closed applicability classes:

  | Class | A guardrail applies when |
  |---|---|
  | `always` | the rule is an invariant of every conforming Ball or binding in its stated scope; |
  | `path-triggered` | the closed protocol, type graph, selected profile, Assembly, or reachable control flow contains the named path; |
  | `risk-triggered` | the architecture contains the named trust, failure, concurrency, irreversibility, secrecy, abuse, or recovery condition; |
  | `claim-triggered` | a concrete binding or distribution makes the named performance, durability, delivery, isolation, security, or conformance claim. |

  Applicability and declaration location are different questions. An applicable guardrail MUST resolve statically to exactly one effective mechanism, value, or evidence contract through one of these forms:

  1. construction or a bounded type proves the invariant directly;
  2. the Ball declares the guardrail locally;
  3. an accepted project, profile, Assembly, or binding scope selects one exact reusable declaration covering the Ball and, where that declaration permits it, the Ball supplies only a field-specific local delta or a different explicit selection.

  The enclosing scope owns that single reference; a covered Ball does not repeat it merely to acknowledge the default. A Ball-local reference is needed only when selection is local rather than already fixed by the enclosing scope.

  A reusable declaration is immutable at its referenced revision and identifies:

  ```text
  owner
  policyId + revision + content digest
  covered guardrail IDs
  exact project/Ball/profile/Assembly/binding/environment scope
  effective values or mechanisms
  allowed override fields, if any
  enforcement owner
  evidence references and their scope, if evidence is required
  review or expiry condition, if one exists
  ```

  Resolution follows the closed reference graph before implementation or review. A missing, mutable, stale, cyclic, conflicting, wrong-version, wrong-profile, wrong-binding, or wrong-environment reference is invalid. A local delta is valid only for a field explicitly made overridable by the referenced declaration and produces one unique effective value. Reuse never creates a runtime registry, service locator, ambient default, or hidden inheritance.

  An existing complete local declaration remains valid. Replacing it with a reference is semantics-preserving only when the resolved mechanism, values, failure behavior, guarantee scope, and evidence scope are equal; migration may be incremental Ball by Ball.

  When the closed inventory proves a trigger absent, the corresponding mechanism, evidence, empty table, zero-valued placeholder, and `not applicable` row are omitted. If absence cannot be proved, the trigger is treated as present unless an explicit accepted decision closes the ambiguity. Missing evidence for a claim prohibits the claim; it does not permit an unprotected triggered path.

  When a Pokeball conformance or release claim, or an accepted ambiguity-resolution decision, relies on trigger absence, that conclusion is materialized in exactly this closed static evidence shape:

  ```text
  TriggerAbsenceProof {
      triggerClass:
          path-triggered
        | risk-triggered
        | claim-triggered
      triggerAnchor
      exactScopeAndEffectiveProfile
      inventoryEvidence:
          PathInventoryRef
        | RiskInventoryRef
        | DenialByConstructionRef
        | ClaimPublicationInventoryRef
      inventoryRevisionsOrDigests
      evaluatedPredicate
      conclusion: Absent
      evidenceOwner
      invalidationConditions
  }
  ```

  An `always` obligation cannot use `TriggerAbsenceProof`. A present trigger predicate invalidates a contrary proof and requires its guardrail to resolve; in particular, a published present claim cannot be negated by a claim-trigger absence proof. An unaccepted ambiguity interpretation cannot establish absence. Ordinary design, implementation, and adoption work materializes no proof or placeholder merely because a category is absent.

  The proof is bound to its exact trigger anchor, scope, effective profile, referenced inventory, and inventory revisions or digests. A listed invalidation condition, scope/profile/version/inventory change, missing or stale digest, unresolved reference, or conflicting evidence invalidates it and requires reevaluation before the dependent conformance/release claim or accepted decision may continue to rely on absence. `evidenceOwner` owns both the proof and its invalidation review. This is static review evidence, not a runtime registry, ambient default, hidden inheritance path, waiver, or authority to broaden a reusable declaration's scope.
- **Applicability:** `A`: every applicability decision; `P/R/C`: a verdict/decision relies on absence; `P`: adoption/pilot work requests boundary guidance.
- **Declaration owner:** Ball/project architecture owner; proof names evidence owner; project owns any worksheet.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Resolver/proof contradiction/invalidation plus worksheet-present/absent and negative-adoption fixtures.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** `always`/present predicates cannot use proof; exact-scope reuse only; worksheet is `SHOULD` during adoption/pilot and absent otherwise.
- **Primary verification route:** `§17.5`

### PBA-40 — Zero Mandatory Runtime Tax

Source: §13.3.

- **Rule:**
  Core semantics do not require:

  ```text
  runtime handler lookup
  reflection discovery
  in-process serialization
  mandatory queue
  mandatory thread hop
  object message hierarchy
  service locator
  ```

  Only a concrete Inline binding with a benchmark on the exact toolchain may claim `maxStructuralAllocationsPerDecision = 0`.

  Distinguish:

  - **structural allocation**—created solely by the architectural runtime mechanism;
  - **payload allocation**—useful domain or result data;
  - **payload copy**—copying useful payload across a boundary.
- **Applicability:** `A`: Core design; implementation check when Inline selected.
- **Declaration owner:** Core/profile and Inline binding owner.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Build inspection; benchmark only for a claim.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One binding serves many Balls; no per-Ball wrapper.
- **Primary verification route:** `§17.5`

### PBA-41 — Measured Claim

Source: §13.4.

- **Rule:** A performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim names its exact guarantee boundary, binding and scope, mechanism, assumptions, retention, evidence, and non-guarantees; without that complete in-scope evidence, the claim is not made. A claim that compares `maxTransitionSteps` counts across bindings additionally proves equality of Decision Work Meter identity/version, transition artifact version, and unit definition; otherwise the counts are intentionally incomparable.
- **Applicability:** `C`: any named performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim is made; cross-binding transition-step comparison additionally requires equal meter/artifact/unit identity.
- **Declaration owner:** Claimant names the guarantee boundary, exact binding/scope, mechanism, assumptions, retention, evidence/non-guarantees, and any compared meter/artifact/unit identity.
- **Scope:** The exact named boundary, binding, environment, workload, retention horizon, and audience of the claim.
- **Enforcement / evidence owner:** Claimant plus benchmark/security/fault/recovery/delivery/conformance evidence owner; meter-identity equality owner when counts are compared.
- **Resolution, failure, and conformance:** Missing or out-of-scope claim fields/evidence remove the claim rather than creating an implied guarantee; unsupported strengthening is non-conforming.
- **Reuse and absent-trigger behavior:** Reuse only on identical digest/scope; unlike meters remain incomparable, and without evidence omit the claim.
- **Primary verification route:** `§17.9`

### PBA-42 — Honest Guarantee Scope

Source: §9.13.

- **Rule:** Any delivery, durability, recovery, receipt, acceptance, once-only, RPO, or RTO claim is limited to its exact named boundary, scope, assumptions, retention, and evidence; source durability or retained pending work alone does not imply a stronger downstream guarantee.
- **Applicability:** `C`: delivery/durability/recovery/RPO/RTO/receipt/acceptance/once-only claim is made.
- **Declaration owner:** Claimant and binding owner.
- **Scope:** The exact named guarantee boundary, claim scope, assumptions, and retention horizon stated by the claim.
- **Enforcement / evidence owner:** Claimant and binding evidence owner for the named boundary, crash/recovery/delivery behavior, retention, and every downstream non-guarantee.
- **Resolution, failure, and conformance:** Missing boundary, scope, assumptions, retention, or evidence prohibits the claim; source durability or retained pending work alone cannot satisfy or strengthen it.
- **Reuse and absent-trigger behavior:** Failure model/evidence reusable only in identical scope; weaker/no claim needs no table.
- **Primary verification route:** `§17.7`

### PBA-43 — Foundation Quarantine

Source: §14.7.

- **Rule:** When a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, business-policy decision, domain authority, route selection, or hidden communication state. A utility shared across Balls is valid Foundation only under that mechanical-only rule. Domain- or business-semantic helper code remains Ball-local or acquires one Ball/Flow owner and is consumed through its declared Application Surface and protocol; it cannot remain an ownerless shared utility or be relabelled as Foundation.
- **Applicability:** `P`: shared foundation exists or a helper/utility is proposed for use by more than one Ball.
- **Declaration owner:** Project foundation/shared-code owner defines its mechanical-only surface; each Ball/Flow retains its domain semantics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Dependency/ownership/state/call-graph scan for local-versus-shared utilities, policy, authority, routes, service locators, and hidden communication.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One project policy serves all Balls for mechanical Foundation only; domain semantics stay local or Ball/Flow-owned, and the Foundation artifact is omitted when no shared mechanical code exists.
- **Primary verification route:** `§17.5`

### PBA-44 — Trusted Actor Context

Source: §11.2.

- **Rule:**
  - When actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization or result selection, a trusted binding boundary constructs a verified, bounded, field-minimized actor context.
  - Its authenticity and integrity resolve relative to a valid approved issuer or an equivalent fixed trusted same-stack issuer/realm proof before the Ball interprets it.
  - Forged, tampered, wrong-issuer/realm, missing, stale, or otherwise unverifiable required evidence fails closed.
  - Actor context grants no authority by itself.
  - After valid evidence and read admission, the Nucleus/read Policy Gate selects one variant of the Query's total target-owned `ResultPayload`, including the declared denied, redacted, or non-disclosing outcome when policy does not permit ordinary disclosure; it does not return `BusinessRejection` or a post-admission `BoundaryResponse`.
  - An actor-independent Decision or read materializes no actor-context, issuer, authentication, or actor-evidence artifact.

  When the actor-context trigger exists, authentication and raw provenance verification are performed by the trusted binding boundary, not by the request payload or Nucleus. The Ball/Nucleus owns the semantic context schema and interpretation. Fixed issuer/realm facts may be proven by the enclosing trusted type or binding rather than copied into every value.

  Applicability catalog:

  ```text
  AuthenticatedActorContext {
      stableSubjectId
      issuer?                # issuer is not fixed by trusted enclosing scope
      namespaceOrRealm?      # more than one realm/namespace is possible
      authenticationMethod? # method affects policy or evidence
      assuranceLevel?        # assurance affects policy
      authenticatedAt?       # authentication time affects validity/audit
      expiresAt?             # context can expire
      delegation?            # delegation exists
  }
  ```

  The effective `stableSubjectId` must be scoped by issuer and realm, whether those identities are materialized or statically fixed. Credential rotation does not automatically create a new subject.

  The presence of an `issuer` field is not proof of origin. The selected security or isolation profile MUST define verifiable authenticity and integrity of actor context relative to a valid approved issuer or the equivalent fixed trusted same-stack proof. Forged or tampered evidence, evidence from a wrong or unapproved issuer/realm, and missing, stale, or otherwise unverifiable required evidence fail closed before the actor context can authorize or otherwise change a Decision or Query/status-read result. `AuthenticatedActorContext` itself describes the actor but grants no authority to perform an arbitrary action. A path whose Decision and read authorization/result are actor-independent creates no actor-context value, issuer row, authentication artifact, or actor-specific evidence.
- **Applicability:** `R`: actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection.
- **Declaration owner:** Ball semantic owner defines context schema/interpretation; Trusted Boundary/security-binding owner defines construction, approved issuer/static proof, and evidence contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Trusted Boundary verifier plus Decision/read authorization, permitted/denied/redacted/non-disclosing result selection, actor-origin, wrong-provenance, and actor-independent sparsity tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact issuer/verifier policy may be reused only in scope; fixed trusted same-stack issuer/realm may be static; actor-independent Decision/read paths omit actor context, issuer data, authentication, and actor-evidence artifacts.
- **Primary verification route:** `§17.8`
<!-- pkb:generated:end -->
