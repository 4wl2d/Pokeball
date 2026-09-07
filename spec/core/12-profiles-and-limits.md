# Core part — Profiles, limits, and performance

[Core contents](../pokeball-architecture-core.md) · [← Security and privacy](11-security-and-privacy.md) · [Manifest and source organization →](14-manifest-and-organization.md)

> Canonical part 9 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 12. Execution profiles

Profiles are independent dimensions. A single overloaded label such as `DurableSecureFast` is not used.

```text
execution:   Inline | BoundedConcurrent
state:       Transient | SnapshotOutbox | EventJournal
isolation:   InProcess | Isolated
security:    Standard | Hardened
composition: Static
```

`Secure`, as a strong adversarial claim, is reserved for a separate isolation and security specification.

A project or binding may select an exact default profile once. A Ball that uses it records no duplicate profile row; it records only an explicit permitted override. The effective profile is the statically resolved project selection plus that delta. Selecting a stronger profile activates its mechanisms and tests; merely listing all profiles does not.

### 12.1. Semantics common to all profiles

Every profile preserves the always-applicable semantics:

- the three logical zones;
- explicit ownership;
- pure bounded decision;
- no reentrant transition;
- atomic Decision acceptance;
- no ambient authority;
- finite bounds on every present variable dimension.

When an output, detached-work, duplicate, retry, cancellation, ambiguity, concurrency, durability, isolation, or security trigger exists, the selected profile also preserves the corresponding §8–§11 contract. A profile name does not require machinery for an absent path, and it cannot waive machinery for a present one.

### 12.2. Inline

```text
Interaction -> decide -> atomically publish AcceptedSnapshotDecisionFrame | AcceptedEventCommit -> dispatch retained outputs
```

Properties:

- one mutating transition at a time;
- no mandatory mailbox;
- no mandatory parallel effects;
- no imposed synchronization, queue, coroutine, serialization, or thread hop;
- synchronous completion processed in the next iteration after acceptance;
- a caller-owned or fixed output buffer is permitted.

Suitable for mobile, desktop, embedded, a local backend use case, and a unit-test binding.

### 12.3. BoundedConcurrent

```text
bounded mailbox
  -> single-writer decision loop
  -> bounded effect/command workers
```

Required:

- finite mailbox capacity;
- typed `BoundaryResponse(AdmissionFailure(reason))` for pre-acceptance backpressure/admission;
- finite worker counts when workers exist;
- a fairness policy when separately queued control inputs could starve;
- out-of-order Fact/Result handling when completion can reorder;
- cancellation/deadline race tests when those paths exist;
- no concurrent mutation of one state instance.

### 12.4. Transient state

- state lives only in process memory;
- a crash may lose state, live replies, and pending operations;
- the source claims no durable delivery;
- an external side effect may have an unknown outcome after a crash;
- recovery uses reinitialization or resynchronization according to the application contract.

### 12.5. SnapshotOutbox

The always-present durable-state contract records the state snapshot and commit revision in one authoritative transaction and recovers from that committed snapshot. The outbox part is empty and may be representation-erased when no output exists.

Additional clauses materialize independently with their triggers:

- when a Decision has durable source outputs, the same transaction records the complete output batch and dispatch begins only after commit;
- a present durable source record remains `Pending` only while its declared delivery policy permits work and is retained through that policy's terminal horizon;
- when retry exists, attempts are finite under the effective time/attempt budget and preserve the same `OutputId`;
- target or executor deduplication is mandatory only for duplicate-permitting irreversible execution;
- when a retrying/retained delivery policy can exhaust, the source atomically records `DispatchStopped` with the available reason/evidence and does not silently discard the accepted output;
- ingress/inbox or idempotency records and their retention horizon exist only when duplicate acceptance/delivery is possible;
- expected revision or a storage-enforced ownership epoch is verified only when concurrency or movable ownership activates it;
- an ambiguous storage commit of an irreversible input does not permit blind re-evaluation;
- operation status exists only under the §9.11 trigger and contains only its reachable facets.

The unconditional Core guarantee ends at durable state acceptance. For present source outputs or status, it additionally includes only the retention and terminal semantics resolved for those paths. It does not promise target receipt, target acceptance, or external execution under every failure schedule.

A concrete binding may claim `at-least-once` only for a present output path, relative to an explicitly named delivery point and with recorded liveness/failure assumptions, retry and retention horizon, and terminal policy. Without such a contract, a present outbox provides only its declared bounded duplicate-permitting dispatch semantics, not unconditional eventual delivery or exactly-once external execution.

### 12.6. EventJournal

- authoritative mutation consists of ordered domain events;
- every Accepted result records an `AcceptedEventCommit`, including `NoDomainChange` with an empty event batch;
- the commit envelope, event batch, and accepted-input marker are accepted atomically; idempotency metadata, status changes, and source output records join the transaction only when their independent triggers exist;
- state is reconstructed through `evolve`;
- present durable outputs are not regenerated by rerunning current transition code;
- exact replay requires pinned artifacts and context and belongs to a separate extension specification.

### 12.7. InProcess

- boundaries are logical and enforced by compile-time or runtime discipline;
- direct calls and a shared allocator are permitted, but every synchronous cross-Ball call before handoff/yield is a `Direct Control Dependency` and the direct-control graph remains acyclic;
- crash containment and hostile credential isolation are not claimed;
- least privilege requires scoped external credentials and adapters, not only visibility modifiers.

### 12.8. Isolated

- process/sandbox boundary;
- IPC is not automatically asynchronous: a synchronous cross-Ball IPC call before handoff/yield is still a `Direct Control Dependency` and remains subject to the acyclic graph;
- versioned authenticated IPC; peer authentication does not replace authorization-issuer verification when actor/grant paths exist;
- profile-defined authenticity and integrity for present actor context and grants under §11.3;
- bounded frame size/depth;
- no shared mutable business memory;
- separate/scoped credentials for present external-resource paths;
- CPU/memory/wall limits for the isolated execution; network/file authority is denied by construction or receives its own finite limits only when exposed;
- crash/restart/quarantine policy;
- anti-replay/idempotency when the operation class permits duplicates or retries.

### 12.9. Hardened

`Hardened` strengthens every present security path in the selected execution/isolation profile; it does not create unused actor, resource, interpreter, secret, or grant paths. Apply only the triggered items:

- actor context from an approved issuer or fixed trusted same-stack issuer/realm proof when actor identity affects a Decision or Query/status-read authorization/result selection;
- least-privilege capabilities for present external resources;
- safe sinks at present interpreter boundaries;
- secret handling for present secret flows;
- explicit grants for privileged proof crossing an authority;
- bounded resource abuse controls;
- security tests for the triggered paths and evidence for the claimed threat assumptions.

It does not mean absolute security.

### 12.10. Profile selection

| Scenario | Recommended starting profile |
|---|---|
| Local UI state machine | `Inline + Transient + InProcess + Standard` |
| Async mobile feature | `BoundedConcurrent + Transient/SnapshotOutbox + InProcess` |
| Durable backend aggregate | `BoundedConcurrent + SnapshotOutbox + InProcess + Hardened` |
| Hostile plugin/parser | `BoundedConcurrent + Transient/SnapshotOutbox + Isolated + Hardened` |
| Distributed irreversible effect | Durable source + separate executor/provider contract; not only a Core profile |

### Definition source records for §12

These marked definitions are the sole glossary inputs for the terms owned in this section.


<!-- pkb:term:start name="EventJournal" -->
**EventJournal** — a durability profile in which authoritative state is reconstructed from committed domain events and every accepted input, including `NoDomainChange`, receives a durable `AcceptedEventCommit`; idempotency, status, and source-output records join that transaction only when their paths exist.
<!-- pkb:term:end -->

<!-- pkb:term:start name="SnapshotOutbox" -->
**SnapshotOutbox** — a durability profile in which a state snapshot is accepted durably and any present durable source outputs join the same atomic transaction; retry, duplicate handling, terminal `DispatchStopped`, and status/retention facets appear only when their delivery paths exist, and eventual delivery is not guaranteed.
<!-- pkb:term:end -->


---

## 13. Limits, budgets, and performance

### 13.1. Three classes of limits

#### Semantic limits

Affect the business or orchestration decision:

```text
maxItemsPerOrder
maxWorkflowSteps
maxCommands
maxRetriesByPolicy
businessDeadline
```

They belong in state or context and are visible to the `Nucleus`.

#### Runtime caps

Constrain implementation resources on paths where those resources exist:

```text
CPU
wall time
memory
mailbox
IPC bytes
storage bytes
parallel workers
delivery attempts per output
outbox and operation-status retention
```

They are enforced by the runtime and may cause an admission failure or fault, but do not rewrite the business decision. The project/binding may declare and verify a cap once; a Ball repeats it only when it overrides the shared value or needs different semantic failure behavior.

`maxCumulativeFanout`, when numerically selected under §10.9, is a causal-composition cap rather than a business choice. A synchronous workflow whose complete execution structure bounds all generated work needs no separate geometric fan-out calculation or carried budget. Where potentially growing work needs numeric accounting, the binding enforces its declared branch count before acceptance; this runtime admission state cannot enter `DecisionContext` or be reset by an async worker/handoff.

`maxInputBytes`, `maxStateBytes`, and `maxOutputBytesPerDecision` are pre-acceptance resource caps, not business choices. Section 8.3 fixes each active numeric dimension through an immutable binding-owned `BoundedByteMeasure`: exact boundary stage and included input metadata/Context, complete candidate-next-State representation, or complete ordered output-sequence representation. A static bounded type and representation need no runtime byte counter; otherwise the boundary or acceptor applies the selected measure at the exact stage and cannot substitute an incomparable wire/heap/storage representation, move the value into `DecisionContext`, or truncate a value.

#### Economic/shared quotas

Money, provider calls, API credits, and a shared tenant balance require an authoritative ledger or reservation. Core requires only an explicit contract and prohibits copying the entire parent balance into parallel branches. A complete distributed budget protocol is an extension.

### 13.2. Backpressure

When admission can fail because a bounded runtime/shared resource is saturated, overload returns a typed outcome. The following is an illustrative Core applicability catalog, not an open or mandatory concrete union:

```text
TemporarilyUnavailable(retryAfter?)
CapacityExceeded(scope)
QueueFull
StoragePressure
```

Each concrete profile/binding with fallible admission declares one finite closed `AdmissionFailure.reason` union containing only its reachable cases. Decoding, construction, and exhaustiveness checks reject an unknown discriminator or free-form/open string; they do not map it to `TemporarilyUnavailable`, `BusinessRejection`, or another stage. A direct Inline path with no fallible admission declares no empty reason union.

Before Decision acceptance, such overload is returned as `BoundaryResponse(AdmissionFailure(<typed overload outcome>))`. It creates no accepted state, `CommitRevision`, `SemanticHandle`, or `SemanticOutput`. A post-acceptance delivery observation is not retroactively converted into admission failure.

An admission failure does not masquerade as a permanent business rejection. An unbounded queue is prohibited. A direct Inline path with no queue or fallible admission needs no backpressure type or empty policy.

`DispatchStopped` after an already accepted durable output is not an admission failure or business rejection. It is a terminal delivery observation: the source retains the output and status according to the declared retention policy and delegates the next decision to reconciliation or manual policy.

### 13.3. Zero Mandatory Runtime Tax

<!-- pkb:pba-source:start id="PBA-40" title="Zero Mandatory Runtime Tax" -->
**Source clause for PBA-40 — Zero Mandatory Runtime Tax.**

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
<!-- pkb:pba-source:end -->
### 13.4. Claim contract

<!-- pkb:pba-source:start id="PBA-41" title="Measured Claim" -->
**Source clause for PBA-41 — Measured Claim.**

- **Rule:** A performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim names its exact guarantee boundary, binding and scope, mechanism, assumptions, retention, evidence, and non-guarantees; without that complete in-scope evidence, the claim is not made. A claim that compares `maxTransitionSteps` counts across bindings additionally proves equality of Decision Work Meter identity/version, transition artifact version, and unit definition; otherwise the counts are intentionally incomparable.
- **Applicability:** `C`: any named performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance claim is made; cross-binding transition-step comparison additionally requires equal meter/artifact/unit identity.
- **Declaration owner:** Claimant names the guarantee boundary, exact binding/scope, mechanism, assumptions, retention, evidence/non-guarantees, and any compared meter/artifact/unit identity.
- **Scope:** The exact named boundary, binding, environment, workload, retention horizon, and audience of the claim.
- **Enforcement / evidence owner:** Claimant plus benchmark/security/fault/recovery/delivery/conformance evidence owner; meter-identity equality owner when counts are compared.
- **Resolution, failure, and conformance:** Missing or out-of-scope claim fields/evidence remove the claim rather than creating an implied guarantee; unsupported strengthening is non-conforming.
- **Reuse and absent-trigger behavior:** Reuse only on identical digest/scope; unlike meters remain incomparable, and without evidence omit the claim.
- **Primary verification route:** `§17.9`
<!-- pkb:pba-source:end -->

<!-- pkb:term:start name="Claim Record" -->
**Claim Record** — a binding-specific statement of a performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance guarantee together with its exact named boundary, scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees. Source durability or retained pending work alone proves no stronger downstream outcome. When no claim is made, no evidence dossier is required.
<!-- pkb:term:end -->

A Ball, project, or binding materializes a Claim Record only for a claim it actually makes:

```text
ClaimRecord {
    exactNamedBoundary
    exactBindingAndScope
    mechanism
    assumptions
    retention
    evidence
    explicitNonGuarantees
}
```

Its metrics and evidence may include:

```text
transition p50/p95/p99
structural allocations
payload copies
state bytes
mailbox occupancy
outbox age
max effects/commands
external response bytes
CPU/wall limits
binary contribution
```

An absolute number without a workload, hardware, compiler and runtime, payload distribution, and measurement method is not a substantiated claim. In the absence of evidence the claim is prohibited; ordinary implementation work does not require an empty evidence dossier.

### 13.5. Design-time tax

Architectural cost includes more than runtime:

```text
manifest size
number of artifacts
routes per operation
manual review hours
waiver count
schema/evidence maintenance
flow-to-feature ratio
```

A simple Ball should not pay ceremony for unused distributed guarantees.

Human first-use cost is part of that tax: time to the first correct change, reference lookups, expert interventions, wrong-owner edits, and rework. Compare the same behavior and required guarantees with the project's competent existing approach. Count shared binding setup and evidence maintenance separately from per-feature work, then include both in the adoption decision; moving cost into a shared component or generated file does not erase it. A smaller file count alone proves neither easier maintenance nor correctness.

For an adoption pilot, the project selects the relevant measure, workload, measurement method, baseline, and continue/reshape/stop threshold. Candidate measures include state size, peak operations per key, transition cost, contention, change radius, recovery cost, and the design-time items above. Core supplies no universal number. A pilot measurement becomes a performance claim only when the project makes that claim under §13.4.

Shared mechanisms, policies, and evidence are reviewed once at their exact scope and referenced by every covered Ball. A local change records only a newly triggered guardrail, semantic delta, override, or invalidated evidence item. Copying an unchanged project policy or repeating `N/A` for absent paths is itself avoidable design-time tax.

### Definition source records for §13

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Structural Allocation" -->
**Structural Allocation** — an allocation that exists only because of an architecture runtime mechanism, not because of useful payload.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Zero Mandatory Runtime Tax" -->
**Zero Mandatory Runtime Tax** — the absence of mandatory mediator/reflection/serialization/queue/thread-hop/object-hierarchy overhead in Core Inline semantics; concrete zero-allocation claims require a benchmark.
<!-- pkb:term:end -->


---
