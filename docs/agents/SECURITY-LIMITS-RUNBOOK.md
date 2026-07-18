# Security and Limits Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use only the sections triggered by an actual trust/representation edge, external authority, interpreter, privilege, secret, unsafe path, variable resource dimension, concurrency/delivery path, or security/performance claim. Shared mechanisms and evidence are declared once at exact project/binding scope.

## 1. Trigger resolution

| Reachable condition | Guardrail |
|---|---|
| raw ingress | bounded parse, normalize, validate; authenticate actor/origin only when that identity affects the Decision |
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

A capability is a real restricted client, credential, broker, ACL, OS handle, or sandbox. A wrapper around an unrestricted ambient client is not enforcement.

## 3. Safe sinks, secrets, and unsafe paths

For every interpreter edge that exists, use the structured/parameterized/context-encoded checks applicable to that dialect. Endpoint identity applies to network targets, path constraints to filesystem/URL paths, query/argument encoding to the corresponding interpreter, and response bounds only to a result path. For every reachable secret flow, state where it may appear and prove it cannot leak through ordinary Projection, Reply, status, logs, metrics, traces, or replay artifacts.

An unsafe site has one authoritative record:

```text
name and owner
exact capability/scope and business reason
threat controls and tests
reviewer and expiry/review condition
revocation/remediation path
```

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

A policy reference includes revision/digest, scope, effective values, enforcement owner, and override allowlist. Resolution rejects missing fields on present dimensions, conflict, cycle, stale or mutable references, scope/profile/binding/environment mismatch, and unauthorized overrides. Overflow never truncates accepted semantic data or dispatches a subset.

## 5. Admission and economic authority

When the candidate frame can exceed an effective bound or capacity can race acceptance, preflight or reserve the complete state/output/required-continuation frame before acceptance. A static bounded construction needs no reservation object. Any capacity failure is a typed admission/backpressure outcome and cannot rewrite the Decision or forget an already accepted fact.

When an economic/shared quota exists, it has an authority and reservation/commit/release lifecycle. A copied balance or non-atomic counter check is not enforcement.

## 6. Evidence reuse

Test shared mechanisms once at their exact digest/scope. A Ball tests its semantic wiring and local delta. Select only reachable cases:

- parser/schema/size/decompression and provenance;
- restricted capability for an external-resource path;
- safe-sink behavior for an interpreter edge;
- only the issuer, audience, payload, current-version, expiry, or revocation checks triggered by the privileged path;
- delayed/recovered same-subject and action-specific authorization for a delayed privileged consequence;
- secret/redaction leakage;
- each present effective bound and overflow, including a local-delta edge when one exists;
- mailbox/backpressure/fairness for concurrent selection;
- retention/fault recovery for durable selection; and
- isolation escape/resource limits for isolated selection.

A performance, security, isolation, durability, delivery, or conformance claim adds the complete scoped evidence record. Missing claim evidence removes the claim; it does not deactivate a reachable guardrail.
