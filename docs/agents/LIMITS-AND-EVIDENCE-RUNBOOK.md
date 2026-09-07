# Limits and Evidence Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation when a variable dimension, admission/economic authority, finite enforcement mechanism, or reusable evidence scope is triggered.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Security and Limits Runbook](SECURITY-LIMITS-RUNBOOK.md) · [Agent Pack index](README.md)

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

Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without a static type-and-representation proof resolves one immutable `BoundedByteMeasure`: `dimension`, `measureIdentity`, `measureVersion`, `representationDefinition`, `limitName`, and `maxBytes`. Input selects exactly raw-boundary or normalized-trusted stage and the complete boundary-metadata/Context inclusion set; exact `N` may proceed and `N+1` fails before trusted semantic acceptance/`decide`. State measures the complete candidate `nextState` semantic representation with declared semantic metadata, excluding heap/allocator/storage/transport mechanics; `N+1` rejects the whole Decision before State, revision, or output acceptance. DecisionOutputs measures the complete ordered `Decision.outputs` sequence: include sequence structure, every field and payload required by the actual representation; correlation tokens and `sourceOrdinal` appear only under their triggers; exclude `nextState`, non-envelope accepted-frame fields, and later transport/framing/compression/encryption/retry/attempt data. Alternate representations map deterministically to the selected sequence, representation erasure preserves equal-value counts, and different tuples are incomparable. No branch truncates a value or partially accepts a Decision.

Static dependencies, participants, and routes are inspectable from source types and wiring; Core imposes no mandatory numeric count on their static finite structure. Check a synchronous workflow's complete work, including every result/refusal/failure handler and any outputs they can generate. A closed finite execution needs no carried causal scope/depth, level-by-level reservation, or separate geometric fan-out total. An import DAG is insufficient when a completion handler can issue new work indefinitely. Queues, external requests, retained outputs/completions, dynamic consumers, and other growing work keep real finite limits and pre-acceptance capacity checks.

When numerically needed or explicitly selected, `maxCumulativeFanout` uses one distinct accepted output branch from its source to one effective route and consumer/executor within one accepted root operation or explicitly named causal scope. A single-destination output counts once; a Signal with `k` consumers counts `k`; terminal, co-reachable, and converging route traversals count, so a diamond counts traversals rather than unique authorities. Mutually exclusive alternatives share their maximum reservation and only the selected accepted branch consumes it. Retry/redelivery of the same accepted output through the same effective route to the same consumer adds nothing; a new accepted output adds one. Async handoff preserves scope and remaining budget, while only a separately declared independent root starts fresh. Per-Decision output, per-Signal consumer, and causal-depth bounds remain separate.

For numeric causal accounting, the binding uses admission state, never `DecisionContext` or a new protocol. It checks the complete candidate output batch and the capacity to preserve its required completions before acceptance. Exact `N` may accept; the first required unit `N+1` returns typed `AdmissionFailure(CausalBudgetExceeded)` and accepts no State, revision, partial output batch, or dispatch. Structure-bound immediate execution needs no counter or reservation artifact.

Only dimensions present in the closed protocol/type/control-flow inventory are declared. Optional delivery, mailbox, worker, in-flight, response, decompression, IPC, storage, potentially growing routed work, fan-out, compensation, timer, status, CPU/wall/memory/network/file, and economic dimensions appear when their paths exist.

A policy reference includes revision/digest, scope, effective values, enforcement owner, and override allowlist. It selects only mechanisms or values Core leaves open; it cannot suppress an inferred trigger, weaken a law, or authorize a direct-control cycle. Resolution rejects missing fields on present dimensions, conflict, cycle, stale or mutable references, scope/profile/binding/environment mismatch, and unauthorized overrides. Overflow never truncates accepted semantic data or dispatches a subset.

A `ReadDependency` uses the already effective input, read-work, response, route, and buffering bounds. Query existence alone does not require a new canonical per-Ball read-limit field.

## 5. Admission and economic authority

When the candidate frame can exceed an effective bound or capacity can race acceptance, preflight or reserve the complete State/output/required-continuation frame before acceptance. Input-byte admission applies at the selected raw-boundary or normalized-trusted stage before semantic acceptance or `decide`; State-byte admission measures the complete candidate `nextState`; output-byte admission measures the complete ordered accepted sequence. When numeric fan-out accounting is selected, its admission includes each new output/route/consumer unit in the candidate batch under the remaining budget. A static bounded construction needs no reservation object. Capacity failure before acceptance returns typed `BoundaryResponse(AdmissionFailure(reason))`; `maxCumulativeFanout` overflow uses `CausalBudgetExceeded`. An immediate trusted command call may return its declared typed refusal through the serialized source handler; separately delivered refusals retain the required verified provenance. It is not `BusinessRejection` and creates no accepted state, commit identity, handle, partial output, or dispatch. A post-acceptance `DispatchStopped` observation is not retroactively converted into admission failure. Neither case may rewrite the Decision or forget accepted work.

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
- for static synchronous composition: actual target binding, acceptance/return order, and full execution termination including completion handlers; adding an allowed consumer changes wiring without new identity/carrier/issuer types or a global connection budget; real growing storage/work retains overflow and accepted-preservation tests;
- when `maxTransitionSteps` exists: immutable meter identity/version, transition artifact version, and unit selection; deterministic monotonic counting; helper/phase/retry/yield/erasure no-reset cases; exact `N/N+1` acceptance behavior; and rejection of cross-binding comparison unless meter identity/version, transition artifact version, and unit all match;
- when numeric `maxCumulativeFanout` is selected: exact causal scope and source-output/route/consumer branch identity; tree, diamond, terminal, converging, co-reachable, and mutually exclusive cases; retry/redelivery versus a new accepted output; async handoff versus an independent root; static proof or exact runtime `N/N+1`; and no partial State/revision/output/dispatch;
- mailbox/backpressure/fairness for concurrent selection;
- retention/fault recovery for durable selection; and
- the negative profile fixture that keeps an ordinary scoped credential in trusted `InProcess + Hardened` without a hostile-containment/separate-principal claim, and positive isolation escape/resource/credential-separation fixtures when `Isolated` is selected.

For immediate same-build calls, inspect actual typed targets, acceptance, serialized completion order, and the existing trusted boundary. Do not reconstruct absent source/result/issuer fields or test exact function text. For other same-stack/generated layouts, inspect the applicable logical roles, calls, and provenance with Interaction duties distinguished from the Trusted Boundary even when co-located. The existing source and enclosing binding may carry this map under Core §5/PBA-01: inspect actual typed entrypoints, calls, verification and accepted-write sites, and add annotations or references only for unclear facts. A separate proof file and repeated unchanged binding suite are unnecessary; labels alone do not prove enforcement. Prove that the Runtime/acceptor performs only applicable validation handoff, admission/reservation, selected-frame publication, retained-output scheduling, and declared mechanical-observation routing. Reject Runtime-owned business schema/policy/permission/read-result selection/retry choice, direct runtime State writes, Assembly-created semantic values or route-dependent business choices, hidden service locators/global state, and shared-foundation route selection or mutable business communication.

When a waiver exists, validate its exact eight-field shape, pinned evidence, review/remediation, and literal conformance effect; also prove that it neither suppresses the trigger nor makes a `MUST`/`MUST NOT` violation conforming.

A performance, security, isolation, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, or conformance claim adds the complete evidence record for its exact named boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees. Source durability or retained pending work alone never establishes a stronger downstream guarantee. Missing claim evidence removes the claim; it does not deactivate a reachable guardrail.
