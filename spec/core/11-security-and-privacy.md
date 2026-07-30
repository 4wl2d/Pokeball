# Core part — Security and privacy

[Core contents](../pokeball-architecture-core.md) · [← System composition](10-system-composition.md) · [Profiles, limits, and performance →](12-profiles-and-limits.md)

> Canonical part 8 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 11. Security and privacy

`No Ambient Authority` (§11.5) is always applicable. The remaining guardrails are activated independently:

| Guardrail | Trigger |
|---|---|
| Double Quarantine | untrusted bytes/platform input or raw resource/SDK output crosses into semantic code; |
| actor context | actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization or result selection; |
| Policy/Execution Gate and Grant | a privileged action depends on both current business and technical authorization and activates both gates; an actor-dependent read that can select more than one semantic permission/result outcome activates the Policy Gate only; a cross-authority action proof activates the Grant contract; |
| capability | a consequence accesses I/O or another external authority/resource; an already scoped constructor dependency may be the capability; |
| safe sink | data reaches SQL, HTML/JS, shell, paths, URLs, deserialization, or another interpreter/dialect; |
| secret containment | a secret can enter state, output, persistence, serialization, logs, or telemetry; |
| unsafe escape hatch | the closed effect algebra deliberately admits raw interpreter authority; if it does not, no repeated forbidden-capability list is required; |
| isolation/hardening evidence | the corresponding profile or security claim is selected. |

Shared ingress, capability, sink, redaction, and isolation mechanisms may be declared and tested once at exact project/binding scope. A Ball records only its semantic wiring, triggered local policy, and permitted delta. Unknown trust or external behavior activates the conservative guardrail; omission of local boilerplate never suppresses an inferred trigger.

### 11.1. Double Quarantine

<!-- pkb:pba-source:start id="PBA-31" title="Double Quarantine" -->
**Source clause for PBA-31 — Double Quarantine.**

- **Rule:** Every raw external-input or raw resource-output edge that exists passes through the applicable parsing, validation, finite bounds, and provenance checks before its value reaches the Nucleus.
- **Applicability:** `P`: raw input or resource output crosses a trust/representation edge.
- **Declaration owner:** Interaction/Resource adapter.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Parser/provenance/fuzz tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Exact validator evidence reusable; absent side needs no placeholder.
- **Primary verification route:** `§17.4`
<!-- pkb:pba-source:end -->

Both external sides of the Nucleus are treated as untrusted:

```text
User / HTTP / UI / OS        -> untrusted input
Network / DB / File / SDK    -> untrusted resource output
```

Data path:

```text
Untrusted bytes
  -> parse
  -> normalize by field contract
  -> validate and bound
  -> validated semantic value + trusted DecisionContext/ReadContext when required
  -> Nucleus Policy Gate or pure semantic read
```

A Resource response passes through an analogous parse, validation, and provenance path before a `Fact` is created.

When actor identity affects semantics, the trusted binding boundary before the `Nucleus` authenticates the actor and constructs the verified, bounded, field-minimized `DecisionContext` or actor-dependent `ReadContext`, but does not make the business authorization or result-selection decision. A value becomes an authorized cause of a privileged action only after the `Nucleus` Policy Gate; authoritative execution then passes separately through the Execution Gate. A Decision or read path with no actor-context trigger creates no actor record or authentication row.

### 11.2. Actor context

<!-- pkb:term:start name="AuthenticatedActorContext" -->
**AuthenticatedActorContext** — a verified, bounded, field-minimized trusted description of a stable subject used only when actor data changes a Decision or Query/status-read authorization/result selection. Its approved issuer/realm is materialized or proven by fixed trusted same-stack binding scope; method, assurance, time, expiry, and delegation appear only when their policy/lifetime paths exist, and the context grants no authority by itself.
<!-- pkb:term:end -->

<!-- pkb:pba-source:start id="PBA-44" title="Trusted Actor Context" -->
**Source clause for PBA-44 — Trusted Actor Context.**

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
<!-- pkb:pba-source:end -->
### 11.3. Policy Gate and Execution Gate

<!-- pkb:pba-source:start id="PBA-33" title="Dual Gate" -->
**Source clause for PBA-33 — Dual Gate.**

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
<!-- pkb:pba-source:end -->
### 11.4. Capability

<!-- pkb:pba-source:start id="PBA-32" title="Capability-Sealed Effect" -->
**Source clause for PBA-32 — Capability-Sealed Effect.**

- **Rule:** Every external effect uses the minimum explicit technical authority for its bounded operation class, enforced at a real capability boundary.
- **Applicability:** `P`: an `EffectRequest` reaches the Ball's external resource/authority.
- **Declaration owner:** Ball capability semantics and binding realization.
- **Scope:** The exact external effect path and its bounded operation class.
- **Enforcement / evidence owner:** Binding/resource owner at the scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolation boundary; positive restriction and unrestricted-authority negative tests.
- **Resolution, failure, and conformance:** Authority wider than the declared operation class, or a local check behind unrestricted authority, fails the rule at that effect path.
- **Reuse and absent-trigger behavior:** Capability policy reusable; omit when no external Effect path exists.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->

<!-- pkb:term:start name="Capability" -->
**Capability** — the minimum explicit technical authority to perform a bounded class of resource operations, enforced at a real capability boundary. A wrapper or local check over unrestricted authority is not that boundary.
<!-- pkb:term:end -->

Prefer:

```text
CatalogSearchCapability(index=products, maxPageSize=100)
PaymentCaptureCapability(merchant=42, currency=EUR, maxAmount=500)
SandboxFileCapability(root=/app/cache, operations=[read, write])
```

Instead of:

```text
DatabaseAdmin
ArbitraryHttp
FileSystemAll
ShellExecute
```

A Capability must be enforced by a real boundary: a scoped credential, restricted client, broker, DB role, OS handle, network policy, or isolated process. `if (allowed)` inside an adapter that already holds an unrestricted credential is only code discipline.

### 11.5. No ambient authority

<!-- pkb:pba-source:start id="PBA-34" title="No Ambient Authority" -->
**Source clause for PBA-34 — No Ambient Authority.**

- **Rule:** Every Ball and binding keeps application authority and mutable business communication out of ambient globals, credentials, service locators, runtime registries, and shared foundation state; dependencies and communication paths are explicit in construction, protocol, or static Assembly.
- **Applicability:** `A`: every Ball/binding.
- **Declaration owner:** Project dependency and explicit-communication contract.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports, call graph, global-state/service-locator/runtime-registry/foundation scans.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared injection convention may be reused; actual graph proves absence of ambient authority and hidden communication.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->

Application code does not obtain resources through:

```text
Database.global
HttpClient.default
ServiceLocator.get(...)
ApplicationContext
GlobalScope
```

Dependencies are supplied through explicit construction or static assembly. The `Nucleus` receives semantic context and values, not service objects.

### 11.6. Safe sinks

<!-- pkb:pba-source:start id="PBA-35" title="Safe Sink" -->
**Source clause for PBA-35 — Safe Sink.**

- **Rule:** Every interpreter or dialect boundary that exists uses the applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- **Applicability:** `P`: data reaches an interpreter/dialect.
- **Declaration owner:** Resource/sink owner.
- **Scope:** The exact interpreter or dialect edge and its data/context interpretation.
- **Enforcement / evidence owner:** Owning Interaction/Resource sink implementation; parameterized, structured, capability-rooted, and context-encoded positive fixtures plus raw-injection/traversal negatives as applicable.
- **Resolution, failure, and conformance:** A typed wrapper without an applicable safe sink, or raw data interpreted as code/path/dialect syntax, fails that edge.
- **Reuse and absent-trigger behavior:** Adapter/evidence reusable; omit with no interpreter edge.
- **Primary verification route:** `§17.3`
<!-- pkb:pba-source:end -->

A typed operation is necessary but insufficient.

| Resource | Safe sink requirements |
|---|---|
| SQL | Parameterization, timeout, row/byte limit, least-privilege DB role |
| Pattern search | Explicit literal/wildcard semantics and dialect escaping |
| Filesystem | Capability-rooted path resolution; no raw traversal |
| HTTP | Allowed service identity, DNS/redirect/response bounds |
| OS | Structured API; shell only as an explicit unsafe exception |
| HTML/JS | Context-specific encoding in Interaction |
| Serialization | Schema, size/depth bounds, unknown-field policy |

### 11.7. Authorization grant

A privileged cross-Ball command or effect may carry attenuated proof:

```text
AuthorizationGrant {
    actorContextId
    issuer
    audience
    action
    objectRef
    operationId
    constraints?
    expectedObjectVersion?
    issuedAt
    expiresAt
}
```

<!-- pkb:term:start name="Grant" -->
**Grant** — scoped business-authorization proof from an approved issuer with verified authenticity/integrity and complete binding to actor, action, object, audience/target, operation, and validity period. Its optional `constraints` bind only fields made authorization-relevant by the action contract for the actual target or payload. Immutable binding to an accepted output and duplicate-execution idempotency are separate Execution-Gate checks activated by their §11.3 triggers; an absent subtrigger adds no field, check, default, or placeholder.
<!-- pkb:term:end -->

A Grant is effective only when the Execution Gate has verified the approved issuer, authenticity and integrity, and binding to trusted actor context and the actual operation. The action contract defines mandatory constrained fields; each such field in the actual payload, including amount and currency or equivalent restrictions, must match or fall within the grant. An unverified or absent constraint grants no additional authority.

A Flow passes only the grant required by the next participant, not the entire principal or session object. A Flow may forward or attenuate a grant within the original authority, but does not expand constraints and is not considered an issuer unless a trusted issuance policy separately permits it.

Exact cryptographic format, wire canonicalization, and key distribution belong to the Secure extension. Core requires a semantic verification contract but does not choose a signature, MAC, protected channel, unforgeable handle, or other concrete mechanism.

### 11.8. In-process limitations

In-process visibility, a private modifier, and a wrapper type do not isolate credentials from malicious native code. `InProcess + Hardened` may reduce accidental misuse but does not claim hostile-component containment.

An ordinary in-process data object with an `issuer` field is not proof. A Hardened binding must have a profile-defined trusted issuance and verification boundary even when cryptographic encoding is unnecessary within one trusted process.

A hostile plugin, parser, or SDK requires an `Isolated` boundary: a process or sandbox, bounded IPC, separate credentials, resource quotas, and crash containment.

### 11.9. Secrets

<!-- pkb:pba-source:start id="PBA-36" title="Secret Containment" -->
**Source clause for PBA-36 — Secret Containment.**

- **Rule:** A secret does not enter state, output, persistence, serialization, logs, or telemetry without an explicit policy for that exact path and scope.
- **Applicability:** `R`: a secret can enter State, any output, persistence, serialization, logs, or telemetry.
- **Declaration owner:** Project classification and affected owner.
- **Scope:** Each exact reachable secret path and its Ball/project/binding scope.
- **Enforcement / evidence owner:** Affected State/output owner plus persistence, serializer, log, and telemetry owners; full data-flow and non-log leakage tests.
- **Resolution, failure, and conformance:** Any reachable path without an explicit policy for that exact path and scope fails closed; a policy for one sink does not cover another.
- **Reuse and absent-trigger behavior:** Exact secret policy reusable; omit when closed data flow proves no secret.
- **Primary verification route:** `§17.8`
<!-- pkb:pba-source:end -->

```text
Secret<T>
CredentialHandle
PrivateKeyHandle
AccessToken
```

Prohibited by default:

- ordinary `toString`/debug rendering;
- Projection, Signal, and generic Reply;
- logs/metric labels;
- unencrypted persistence;
- arbitrary serialization;
- generic read model/replica;
- plugin transfer.

A wrapper does not promise physical zeroization in a garbage-collected runtime. The implementation honestly documents copies, swap, crash dumps, and key lifecycle.

### 11.10. Unsafe escape hatch

<!-- pkb:pba-source:start id="PBA-37" title="Explicit Unsafe Escape Hatch" -->
**Source clause for PBA-37 — Explicit Unsafe Escape Hatch.**

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
<!-- pkb:pba-source:end -->
### 11.11. Observability and privacy

Tracing is path-triggered by actual observability use and claim-triggered when trace evidence supports a guarantee. A present operational trace contains only the causal identifiers, type names, revisions, timing, and outcome fields materialized by the observed path; it adds no placeholder ID or revision. It contains no secrets or optional raw payloads.

A security audit, replay bundle, metrics, and debug trace are distinct data products. None substitutes for another.

### Definition source records for §11

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="Execution Gate" -->
**Execution Gate** — the target/resource check immediately before authoritative execution that enforces every triggered proof, capability, constrained-field, version, freshness/revocation, idempotency, endpoint, quota, and safe-sink binding for an already accepted action without inventing a new business decision. Post-acceptance failure is a typed Resource/result/status path, never a pre-acceptance carrier or downgrade.
<!-- pkb:term:end -->


<!-- pkb:term:start name="Policy Gate" -->
**Policy Gate** — the Nucleus semantic rule that alone determines business permission and, for an actor-dependent pure read, selects one permitted, denied, redacted, or deliberately non-disclosing variant from the Query's total target-owned `ResultPayload` using State, Query, and trusted context. It is distinct from the target/resource Execution Gate; a pure read creates no `Decision` and uses no `BusinessRejection` or post-admission `BoundaryResponse`.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Safe Sink" -->
**Safe Sink** — the applicable parameterized, structured, capability-rooted, or context-encoded API at an interpreter or dialect boundary; it prevents untrusted data from being interpreted as code, traversal, or dialect syntax.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Trusted Boundary" -->
**Trusted Boundary** — an explicitly authorized binding edge that verifies representation, finite bounds, provenance, accepted-frame correspondence, protocol identity, and every triggered authenticity/integrity/version/validity rule before constructing trusted semantic input, non-empty `DecisionContext`, or actor-dependent `ReadContext`. It is an edge, not the Interaction role; physical co-location transfers neither semantic interpretation nor business authority. The Ball/Nucleus owns semantic schema and interpretation. For a command/result bridge it constructs `ModuleCommandPulse`/`ModuleResultPulse` only from the corresponding accepted frame; it does not replace target `decide`, select policy/read results, synthesize business meaning, or grant authority merely by authenticating origin.
<!-- pkb:term:end -->


---
