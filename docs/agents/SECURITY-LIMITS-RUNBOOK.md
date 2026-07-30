# Security and Limits Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use only the sections triggered by an actual trust/representation edge, external authority, interpreter, privilege, secret, unsafe path, variable resource dimension, concurrency/delivery path, or security/performance claim. Shared mechanisms and evidence are declared once at exact project/binding scope.

This runbook is a projection of the marked Core source clauses. Core remains the sole authority for modal force, trigger, owner, scope, failure behavior, reuse, and absence behavior.

**Task routing:** trigger resolution, quarantine, capabilities, safe sinks, secrets, and unsafe paths remain below; effective bounds, admission/economic authority, and evidence reuse continue in [LIMITS-AND-EVIDENCE-RUNBOOK.md](LIMITS-AND-EVIDENCE-RUNBOOK.md).

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

Moved to [Effective bounds](LIMITS-AND-EVIDENCE-RUNBOOK.md#4-effective-bounds).

## 5. Admission and economic authority

Moved to [Admission and economic authority](LIMITS-AND-EVIDENCE-RUNBOOK.md#5-admission-and-economic-authority).

## 6. Evidence reuse

Moved to [Evidence reuse](LIMITS-AND-EVIDENCE-RUNBOOK.md#6-evidence-reuse).
