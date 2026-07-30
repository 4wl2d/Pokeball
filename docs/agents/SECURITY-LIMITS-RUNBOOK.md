# Security and Limits Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use only the sections triggered by an actual trust/representation edge, external authority, interpreter, privilege, secret, unsafe path, variable resource dimension, concurrency/delivery path, or security/performance claim. Shared mechanisms and evidence are declared once at exact project/binding scope.

This runbook is a projection of the marked Core source clauses. Core remains the sole authority for modal force, trigger, owner, scope, failure behavior, reuse, and absence behavior.

## 1. Trigger resolution

| Reachable condition | Guardrail |
|---|---|
| raw ingress | bounded parse, normalize, validate |
| actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status authorization/result selection | field-minimized trusted actor context; authenticity/integrity relative to a valid approved issuer or equivalent fixed trusted same-stack issuer/realm proof; forged, tampered, wrong, stale, or missing evidence fails closed |
| raw resource/target output | schema/size/provenance/correlation validation; redaction only when sensitive/secret data can appear |
| external consequence | minimum technical authority for the bounded operation class, enforced at a real capability boundary, plus target-owned execution constraints |
| privileged action depends on both business permission and current technical authorization | Nucleus Policy Gate followed by current fail-closed Execution Gate; actor-dependent read outcome uses Policy Gate only |
| interpreter/dialect | applicable parameterized, structured, capability-rooted, or context-encoded safe sink |
| secret/PII flow | explicit policy for the exact State, output, persistence, serialization, log, or telemetry path and scope |
| raw authority escape | named owner, scope, controls, evidence, expiry/revocation |
| credential must be isolated from less-trusted co-resident code, a high-privilege credential requires its own principal, or another hostile/resource-containment condition or claim exists | isolated principal, bounded IPC/resources, separate credentials, crash policy |

Unknown trust or behavior activates the conservative guardrail. Metadata cannot declare a reachable risk absent.

## 2. Shared quarantine and capability

A project may declare and test exact parser, provenance verifier, restricted client, sink, redaction, and isolation bindings once. A Ball records only its semantic wiring and local allowlisted delta.

The trusted binding boundary verifies bounded observations and constructs `DecisionContext` or actor-dependent `ReadContext`. Interaction is the logical adapter role; Trusted Boundary is the separately inspectable authorized verification-and-construction edge, commonly realized at Interaction or route ingress, not another zone or a synonym. Co-location transfers no business-policy or State authority. The Ball/Nucleus owns context semantic schema and interpretation; it neither verifies raw provenance nor performs I/O. When Inline work queues more than one cause, the boundary preserves a separately verified context for each Pulse; root actor/grant/reserved-ID fields are not inherited by a later cause, and remaining runtime causal capacity is passed only to reservation/admission.

When a privileged action depends on both business permission and current technical authorization, keep the two gates distinct. The Nucleus Policy Gate alone decides business permission from committed State, the current cause, and trusted context. Immediately before execution, the target/Resource Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding without inventing another business decision. An actor-dependent read outcome activates only the pure Policy Gate and no Execution Gate. A Grant appears only when action proof crosses an authority. A delayed or recovered action retains only bounded stable-subject/value binding; current action authorization arrives through declared trusted context keyed to the exact action handle. Raw bearer/session/principal is not live state, and authorization for another action is not reused.

For a Grant, bind every action-contract constrained field when such constraints authorize the actual target or payload, and check an idempotency identity only when duplicate execution is possible. Accepted-output immutable-payload binding remains a separate triggered check. When a trigger is absent, omit its field and check rather than constructing a default or placeholder.

PBA-44 applies independently of privilege whenever actor context can change a Decision or Query/status authorization or result selection, including typed unprivileged inter-Ball input and protected reads. The Trusted Boundary verifies the selected profile's authenticity and integrity mechanism against a valid approved issuer before context reaches the Ball. A fixed trusted same-stack scope may prove issuer/realm statically without materialized fields. `AuthenticatedActorContext` identifies an actor; it is not itself a Grant or arbitrary action authority. After valid read admission, the pure Nucleus/read Policy Gate selects one variant from the Query's total target-owned `ResultPayload`, including its declared denied, redacted, or deliberately non-disclosing result when ordinary disclosure is not permitted. It never uses `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary-invented `NotFound`. An actor-independent Decision or read creates no actor-context value, issuer row, authentication artifact, or actor-specific evidence.

Validation stage follows declared ownership and inputs:

| Predicate | Owner and result |
|---|---|
| representation or declared closed-protocol/type invariant independent of committed State, semantic Context, and business policy | Interaction before semantic input; `BoundaryResponse(ValidationFailure)` and no `Intent`/`Query` |
| valid typed rule depending on committed State or semantic Context, or deliberately owned as a business choice | Nucleus; mutation `Rejected(BusinessRejection)` or admitted-read target-owned `ResultPayload` variant |
| fixed constraint deliberately promoted into the closed protocol/type invariant | protocol owner changes the effective versioned contract; Interaction may then return `ValidationFailure`, but one identity cannot switch stages dynamically |

For a Payment-like command path, preserve stage ownership: invalid source/protocol/provenance/context, validation, or admission before target `decide` uses only the statically declared `CommandRejectedBeforeAcceptance` carrier; a Nucleus business refusal inside an accepted target Decision is an accepted `ModuleResultOutput(Rejected(...))`; an Execution-Gate/provider failure after target acceptance remains the declared Resource/result/status path. Neither gate, Assembly, nor a later failure may roll back or downgrade target acceptance.

A capability is the minimum explicit technical authority for one bounded operation class, enforced by a real restricted client, scoped credential, broker, ACL/DB role, OS handle, network policy, sandbox, or isolated process. A wrapper or local `if (allowed)` check around an unrestricted ambient client is only code discipline, not capability enforcement.

Do not conflate credential scope with credential containment. A trusted `InProcess + Hardened` binding may use an ordinary scoped external credential when it makes no hostile-component-containment claim and does not require a separate principal for that credential. Select `Isolated` when the credential must be protected from less-trusted co-resident code or a high-privilege credential requires its own security principal. The InProcess case still requires real least privilege, the applicable Policy/Execution Gates, and honest non-claims about hostile containment.

## 3. Safe sinks, secrets, and unsafe paths

For every interpreter or dialect boundary that exists, use the applicable parameterized, structured, capability-rooted, or context-encoded safe sink. Endpoint identity applies to network targets, capability-rooted path resolution to filesystem paths, path constraints to URL paths, query/argument encoding to the corresponding interpreter, and response bounds only to a result path. A raw traversal string is not a capability-rooted sink.

For every reachable secret flow, enumerate the exact paths and scopes in which the secret can enter State, any `SemanticOutput` or other output, persistence, serialization, logs, or telemetry, and resolve an explicit policy for each. A policy for logs or Projection alone does not authorize the same secret in State, a Reply/Signal/status value, storage, replay material, metrics, or traces.

An unsafe site that deliberately deviates has one authoritative `WaiverRecord` with exactly eight top-level fields:

```yaml
owner: <accountable owner>
approvedBy: <authority permitted to accept the project risk>
governingAnchor: <exact Core law, clause, extension, or project rule>
exactScope: <exact nonconforming scope>
reason: <deliberate rationale>
constraintsAndCompensatingControls: [<constraint or control>]
testsAndEvidence: [<immutable or version-pinned evidence>]
review:
  expiryOrReviewAt: <date, event, or review condition>
  remediation: <required remediation>
  conformanceEffect: <literal effect on claims and conformance>
```

The record is neither policy nor precedent and does not satisfy the guardrail. A `SHOULD` deviation may remain conforming only when Core permits the recorded deliberate rationale; a violated `MUST` or `MUST NOT` blocks a Pokeball conformance claim for `exactScope` until remediation. A direct-control or compile-time import cycle remains deliberate nonconformance even with a waiver.

One project scan proves the empty unsafe set when making a conformance claim; routine Balls do not carry empty registries.

## 4. Effective bounds

For each present variable dimension, resolve exactly one finite bound through a bounded type/static proof, immutable in-scope policy reference, local declaration, or allowed delta. Canonical numeric names retain Core units when used:

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

A numeric `maxTransitionSteps` declaration appears only when the binding selects numeric metering to supply the decision-work bound instead of or in addition to a static proof. Its resolved contract names `meterIdentity`, `meterVersion`, `transitionArtifactVersion`, one immutable `unitDefinition`, `maxTransitionSteps`, and a finite typed pre-acceptance or programming-fault `overflowPolicy`. For equal binding, transition artifact version, meter identity/version, canonical State, Pulse, and valid Context, the count is deterministic; units are non-negative and consumption is monotonic. Counting starts once at zero for one `decide` and cannot reset, split, or restart through helpers, phases, loops, retries, yields, or representation erasure. Completion at `N` is legal; attempted unit `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and cannot truncate work into a different business result. Numeric counts compare across bindings only when meter identity/version, transition artifact version, and unit definition all match; Core defines no universal step unit or cross-language replay guarantee. A static bound alone requires no meter artifact.

Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without a static type-and-representation proof resolves one immutable `BoundedByteMeasure`: `dimension`, `measureIdentity`, `measureVersion`, `representationDefinition`, `limitName`, and `maxBytes`. Input selects exactly raw-boundary or normalized-trusted stage and the complete boundary-metadata/Context inclusion set; exact `N` may proceed and `N+1` fails before trusted semantic acceptance/`decide`. State measures the complete candidate `nextState` semantic representation with declared semantic metadata, excluding heap/allocator/storage/transport mechanics; `N+1` rejects the whole Decision before State, revision, or output acceptance. DecisionOutputs measures the complete ordered `Decision.outputs` sequence: include sequence structure, every required output-envelope field, correlation token, `sourceOrdinal`, and payload; exclude `nextState`, non-envelope accepted-frame fields, and later transport/framing/compression/encryption/retry/attempt data. Alternate representations map deterministically to the selected sequence, representation erasure preserves equal-value counts, and different tuples are incomparable. No branch truncates a value or partially accepts a Decision.

`maxDeclaredDependenciesPerBall` counts each distinct resolved `ReadDependency`, `DeclaredCommandDependency`, `DeclaredSignalDependency`, and `FlowParticipation` declaration owned by the Ball once. A referenced dependency remains counted in its own kind, multiple operations to one target remain distinct, and exact duplicate declarations or aliases/equivalent rows resolving to an existing identity are invalid. Static resolution accepts exactly `N` declarations and rejects the Ball contract when a first distinct declaration would make `N+1`, before execution. `maxRoutesPerFlow` counts only distinct effective Assembly command/result round-trip mappings used by one Flow; command ingress plus its bound result return is one unit and different operations remain distinct. Read and signal dependencies retain their own bounds but consume zero command-route units; `FlowParticipation` and `dependencyRefs` also consume zero. Any exact duplicate, spelling alias, equivalent repeated route row, or split-leg alias is invalid; static resolution accepts exactly `N` mappings and rejects the Flow/Assembly contract at `N+1`, before execution.

`maxCumulativeFanout` uses a different canonical unit: one distinct accepted `SemanticOutput` branch from its complete source tuple to one effective route and consumer/executor within one accepted root operation or explicitly named causal scope. A single-destination output counts once; a Signal with `k` consumers counts `k`; terminal, co-reachable, and converging route traversals count, so a diamond counts traversals rather than unique authorities. Mutually exclusive alternatives share their maximum reservation and only the selected accepted branch consumes it. Retry/redelivery of the same source tuple through the same effective route to the same consumer adds nothing; a new accepted output source tuple adds one. Async handoff preserves scope and remaining budget, while only a separately declared independent root starts fresh. Per-Decision output, per-Signal consumer, and causal-depth bounds remain separate.

A static graph/control-flow proof may establish this ceiling without a runtime artifact. Otherwise the binding uses the existing total causal reservation, never `DecisionContext` or a new protocol. It reserves the complete candidate output batch and any owned mutually exclusive reservation before acceptance. Exact `N` may accept; the first required unit `N+1` returns typed `AdmissionFailure(CausalBudgetExceeded)` and accepts no State, revision, partial output batch, or dispatch.

Only dimensions present in the closed protocol/type/control-flow inventory are declared. Optional delivery, mailbox, worker, in-flight, response, decompression, IPC, storage, participant, route, fan-out, compensation, timer, status, CPU/wall/memory/network/file, and economic dimensions appear when their paths exist.

A policy reference includes revision/digest, scope, effective values, enforcement owner, and override allowlist. It selects only mechanisms or values Core leaves open; it cannot suppress an inferred trigger, weaken a law, or authorize a direct-control cycle. Resolution rejects missing fields on present dimensions, conflict, cycle, stale or mutable references, scope/profile/binding/environment mismatch, and unauthorized overrides. Overflow never truncates accepted semantic data or dispatches a subset.

A `ReadDependency` uses the already effective input, read-work, response, route, and buffering bounds. Query existence alone does not require a new canonical per-Ball read-limit field.

## 5. Admission and economic authority

When the candidate frame can exceed an effective bound or capacity can race acceptance, preflight or reserve the complete State/output/required-continuation frame before acceptance. Input-byte admission applies at the selected raw-boundary or normalized-trusted stage before semantic acceptance or `decide`; State-byte admission measures the complete candidate `nextState`; output-byte admission measures the complete ordered accepted sequence. Fan-out admission includes every new source-output/route/consumer unit in that candidate batch under the scope's remaining reservation. A static bounded construction needs no reservation object. Capacity failure before acceptance returns typed `BoundaryResponse(AdmissionFailure(reason))`; `maxCumulativeFanout` overflow uses `CausalBudgetExceeded`. On a command route the response travels only through the verified pre-acceptance carrier. It is not `BusinessRejection` and creates no accepted state, commit identity, handle, partial output, or dispatch. A post-acceptance `DispatchStopped` observation is not retroactively converted into admission failure. Neither case may rewrite the Decision or forget accepted work.

Every concrete fallible-admission profile/binding exposes one finite closed `AdmissionFailure.reason` union. Core §13.2 is an illustrative applicability catalog, not permission for an open string. Test every declared member and reject an unknown discriminator or free-form reason before construction of a trusted `AdmissionFailure`; a profile whose admission cannot fail has no empty union.

When an economic/shared quota exists, it has an authority and reservation/commit/release lifecycle. A copied balance or non-atomic counter check is not enforcement.

## 6. Evidence reuse

Test shared mechanisms once at their exact digest/scope. A Ball tests its semantic wiring and local delta. Select only reachable cases:

- parser/schema/size/decompression and provenance;
- trusted `DecisionContext` and actor-dependent `ReadContext` constructor provenance, bounds, field minimization, and Ball-owned semantic interpretation;
- a restricted capability for the exact bounded external-resource operation class, plus a negative wrapper/admin-credential fixture proving that local checks over unrestricted authority do not pass;
- every applicable safe-sink form for an interpreter/dialect edge, including a capability-rooted filesystem positive and raw-traversal negative when that path exists;
- valid-approved-issuer or fixed trusted-scope authenticity/integrity plus forged, tampered, wrong-issuer, wrong-realm, stale, and missing-required-evidence cases whenever actor context can change a Decision or read authorization/result selection; include an actor-independent sparse fixture;
- for an actor- or policy-dependent Query, target-declared permitted, denied/redacted, and deliberately non-disclosing variants that are actually reachable; wrong provenance remains a boundary failure, and no caller/Assembly/runtime invention, `BusinessRejection`, exception, post-admission `BoundaryResponse`, or undeclared `NotFound` passes;
- the three stage-classification fixtures: malformed representation or out-of-type value produces `ValidationFailure` before an `Intent`/`Query`; a valid typed value exceeding a State/Context business limit reaches the Nucleus and produces mutation `BusinessRejection` or an admitted-read payload variant; promoting the fixed limit to a type invariant requires a new compatible protocol identity/version and cannot vary by binding;
- one Grant fixture with action-contract constraints over the actual target or payload and possible duplicate execution proves matching constrained-field and idempotency-identity binding, while substitution or mismatch fails closed;
- an otherwise equivalent Grant fixture with neither subtrigger adds no corresponding field, check, default, or placeholder; other wrong-target, audience, current-version, expiry, revocation, Policy Gate, Execution Gate, capability, or Grant tests appear only when separately triggered;
- delayed/recovered same-subject and action-specific authorization for a delayed privileged consequence;
- exact-path secret policy across each reachable State, output, persistence, serialization, log, and telemetry path, plus a negative fixture in a non-log path;
- each present effective bound and overflow, including a local-delta edge when one exists;
- for every numeric input/State/output byte dimension without static proof: exact immutable `BoundedByteMeasure` dimension/identity/version/representation/limit tuple; input stage and metadata/Context inclusion with pre-`decide` `N/N+1`; complete candidate-next-State semantic representation with pre-State/revision/output `N/N+1`; complete ordered output sequence/envelope inclusion and later-mechanics exclusion; exact alternate-representation mapping, erasure invariance, tuple incomparability, and no partial acceptance;
- when either composition count exists: all four distinct declaration kinds with references retained, duplicate/equivalent-alias identities invalid, and static exact-`N`/reject-Ball-contract-at-`N+1`; only distinct effective Flow command/result round-trip mappings consume `maxRoutesPerFlow`, with command ingress/result return unified, read/signal/participation/reference units at zero, exact duplicate/spelling-alias/equivalent-row/split-leg aliases invalid, multiple command operations distinct, and static exact-`N`/reject-Flow-Assembly-at-`N+1`;
- when `maxTransitionSteps` exists: immutable meter identity/version, transition artifact version, and unit selection; deterministic monotonic counting; helper/phase/retry/yield/erasure no-reset cases; exact `N/N+1` acceptance behavior; and rejection of cross-binding comparison unless meter identity/version, transition artifact version, and unit all match;
- when `maxCumulativeFanout` exists: exact causal scope and source-output/route/consumer branch identity; tree, diamond, terminal, converging, co-reachable, and mutually exclusive cases; retry/redelivery versus a new accepted source tuple; async handoff versus an independent root; static proof or exact runtime `N/N+1`; and no partial State/revision/output/dispatch;
- mailbox/backpressure/fairness for concurrent selection;
- retention/fault recovery for durable selection; and
- the negative profile fixture that keeps an ordinary scoped credential in trusted `InProcess + Hardened` without a hostile-containment/separate-principal claim, and positive isolation escape/resource/credential-separation fixtures when `Isolated` is selected.

For same-stack/generated layouts, also prove the complete logical-role/call graph and origin trace with Interaction duties and Trusted Boundary edge evidence distinguished even when co-located. Prove that the Runtime/acceptor performs only applicable validation handoff, admission/reservation, selected-frame publication, retained-output scheduling, and declared mechanical-observation routing. Reject Runtime-owned business schema/policy/permission/read-result selection/retry choice, direct runtime State writes, Assembly-created semantic values or route-dependent business choices, hidden service locators/global state, and shared-foundation route selection or mutable business communication.

When a waiver exists, validate its exact eight-field shape, pinned evidence, review/remediation, and literal conformance effect; also prove that it neither suppresses the trigger nor makes a `MUST`/`MUST NOT` violation conforming.

A performance, security, isolation, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, or conformance claim adds the complete evidence record for its exact named boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees. Source durability or retained pending work alone never establishes a stronger downstream guarantee. Missing claim evidence removes the claim; it does not deactivate a reachable guardrail.
