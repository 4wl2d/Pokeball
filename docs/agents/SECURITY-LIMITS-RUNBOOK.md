# Security and Limits Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use only the sections triggered by an actual trust/representation edge, external authority, interpreter, privilege, secret, unsafe path, variable resource dimension, concurrency/delivery path, or security/performance claim. Shared mechanisms and evidence are declared once at exact project/binding scope.

## 1. Trigger resolution

| Reachable condition | Guardrail |
|---|---|
| raw ingress | bounded parse, normalize, validate |
| actor, tenant, issuer, realm, assurance, or delegation can change a Decision | field-minimized `AuthenticatedActorContext`; authenticity and integrity relative to a valid approved issuer; forged, wrong-issuer, or missing required evidence fails closed |
| raw resource/target output | schema/size/provenance/correlation validation; redaction only when sensitive/secret data can appear |
| external consequence | minimum technical capability and target-owned execution constraints |
| privileged consequence | Nucleus Policy Gate plus current fail-closed Execution Gate |
| interpreter/dialect | structured, parameterized, context-encoded safe sink |
| secret/PII flow | containment, serialization/storage/telemetry policy |
| raw authority escape | named owner, scope, controls, evidence, expiry/revocation |
| hostile or containment claim | isolated principal, bounded IPC/resources, crash policy |

Unknown trust or behavior activates the conservative guardrail. Metadata cannot declare a reachable risk absent.

## 2. Shared quarantine and capability

A project may declare and test exact parser, provenance verifier, restricted client, sink, redaction, and isolation bindings once. A Ball records only its semantic wiring and local allowlisted delta.

For a privileged path, resolve actor/action/object/state policy and the current execution proof. A delayed or recovered action retains only bounded stable-subject/value binding; current action authorization arrives through declared trusted context keyed to the exact action handle. Raw bearer/session/principal is not live state, and authorization for another action is not reused.

PBA-44 applies independently of privilege whenever actor context can change any Decision, including a typed unprivileged inter-Ball input. The Trusted Boundary verifies the selected profile's authenticity and integrity mechanism against a valid approved issuer before context reaches the Nucleus. `AuthenticatedActorContext` identifies an actor; it is not itself a Grant or arbitrary action authority. An actor-independent path creates no actor-context value, issuer row, authentication artifact, or actor-specific evidence.

A capability is a real restricted client, credential, broker, ACL, OS handle, or sandbox. A wrapper around an unrestricted ambient client is not enforcement.

## 3. Safe sinks, secrets, and unsafe paths

For every interpreter edge that exists, use the structured/parameterized/context-encoded checks applicable to that dialect. Endpoint identity applies to network targets, path constraints to filesystem/URL paths, query/argument encoding to the corresponding interpreter, and response bounds only to a result path. For every reachable secret flow, state where it may appear and prove it cannot leak through ordinary Projection, Reply, status, logs, metrics, traces, or replay artifacts.

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
maxEffectsPerDecision
maxCommandsPerDecision
maxCausalDepth
maxRetriesPerOperation
maxTransitionSteps
```

Only dimensions present in the closed protocol/type/control-flow inventory are declared. Optional delivery, mailbox, worker, in-flight, response, decompression, IPC, storage, participant, route, fan-out, compensation, timer, status, CPU/wall/memory/network/file, and economic dimensions appear when their paths exist.

A policy reference includes revision/digest, scope, effective values, enforcement owner, and override allowlist. It selects only mechanisms or values Core leaves open; it cannot suppress an inferred trigger, weaken a law, or authorize a direct-control cycle. Resolution rejects missing fields on present dimensions, conflict, cycle, stale or mutable references, scope/profile/binding/environment mismatch, and unauthorized overrides. Overflow never truncates accepted semantic data or dispatches a subset.

## 5. Admission and economic authority

When the candidate frame can exceed an effective bound or capacity can race acceptance, preflight or reserve the complete state/output/required-continuation frame before acceptance. A static bounded construction needs no reservation object. Capacity failure before acceptance returns typed `BoundaryResponse(AdmissionFailure(reason))`; it is not `BusinessRejection` and creates no accepted state, commit identity, handle, or output. A post-acceptance `DispatchStopped` observation is not retroactively converted into admission failure. Neither case may rewrite the Decision or forget accepted work.

When an economic/shared quota exists, it has an authority and reservation/commit/release lifecycle. A copied balance or non-atomic counter check is not enforcement.

## 6. Evidence reuse

Test shared mechanisms once at their exact digest/scope. A Ball tests its semantic wiring and local delta. Select only reachable cases:

- parser/schema/size/decompression and provenance;
- restricted capability for an external-resource path;
- safe-sink behavior for an interpreter edge;
- valid-approved-issuer authenticity/integrity plus forged, tampered, wrong-issuer, and missing-required-evidence cases whenever actor context can change a Decision;
- only the audience, payload, current-version, expiry, revocation, Policy Gate, Execution Gate, or Grant checks separately triggered by a privileged path;
- delayed/recovered same-subject and action-specific authorization for a delayed privileged consequence;
- secret/redaction leakage;
- each present effective bound and overflow, including a local-delta edge when one exists;
- mailbox/backpressure/fairness for concurrent selection;
- retention/fault recovery for durable selection; and
- isolation escape/resource limits for isolated selection.

When a waiver exists, validate its exact eight-field shape, pinned evidence, review/remediation, and literal conformance effect; also prove that it neither suppresses the trigger nor makes a `MUST`/`MUST NOT` violation conforming.

A performance, security, isolation, durability, delivery, or conformance claim adds the complete scoped evidence record. Missing claim evidence removes the claim; it does not deactivate a reachable guardrail.
