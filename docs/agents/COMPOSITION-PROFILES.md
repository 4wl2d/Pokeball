# Composition and Profiles

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook only when an inter-Ball edge, Flow, shared foundation, profile delta, or guarantee claim exists. Assembly is authoritative for routes; an exact project policy is authoritative only when one is referenced. Do not repeat either source per Ball.

## 1. Classify existing edges

| Kind | Trigger | Required authoritative facts |
|---|---|---|
| `ReadDependency` | read from another authority | source, result identity/stamp/freshness, honest consistency |
| `DeclaredCommandDependency` | one addressed command hop | target operation, exact protocol identity, route, result, delivery; version/idempotency/deadline only when triggered |
| `DeclaredSignalDependency` | bounded observation of publication | producer/consumer, effective protocol identity, delivery, source identity/provenance, fan-out/size; version/dedup/ordering/buffering/depth/attempts only when triggered |
| `FlowParticipation` | coordination owns state/lifecycle/outcome | Flow owner, participants, steps; deadlines, compensation, and reconciliation only when triggered |

No edge is implied by package visibility, service locator, event-bus label, or runtime discovery. Each edge resolves once in Assembly.

## 2. One hop or Flow

Keep one hop when there is one target and no independent coordination lifecycle, branching/join, compensation, reconciliation, manual queue, shared deadline/cancellation, or separate terminal outcome.

Use a Flow when one substantial coordination property makes the one-hop conditions false. The Flow owns only orchestration state and field-minimized retained values needed by later Decisions. It never copies participant truth or reads runtime/participant ledgers as hidden Decision input.

Compensation is new fallible work. When triggered it has its own identity and outcome; duplicate execution adds idempotency, a privileged consequence adds current action-specific authorization, a deadline adds its time contract, and an ambiguous result adds reconciliation. It targets retained authoritative values/handles, not inferred history.

## 3. Graph resolution

Inspect only graph kinds that exist:

1. compile-time imports;
2. direct synchronous/control edges;
3. async signal/command feedback; and
4. data/read provenance.

The first two are acyclic unless an accepted deviation says otherwise. Async feedback resolves an owner, stable identity, finite causal budget, escape condition, and fan-out protection; deduplication and retry budgets appear only when duplicate delivery or retry exists.

Each present ceiling resolves through a static proof, local declaration, or optional exact project policy. Assembly records endpoints, effective protocol identity, delivery semantics, and triggered route fields/deltas. Verify no wildcard target/consumer, duplicate resolution, protocol re-export, hidden feature-internal import, or unsupported multi-source atomicity claim.

## 4. Effective profile

Resolve the four dimensions independently, even when one named policy supplies them together:

| Dimension | Choices | Triggered mechanism |
|---|---|---|
| Execution | `Inline` / `BoundedConcurrent` | mailbox/workers/backpressure only for concurrent selection |
| State | `Transient` / `SnapshotOutbox` / `EventJournal` | durable records/recovery/status only for durable selection |
| Isolation | `InProcess` / `Isolated` | IPC/principal/resource containment only for isolation selection |
| Security | `Standard` / `Hardened` | stronger actor/grant/audit/abuse controls only for hardening selection |

A project/binding may select an exact default policy once. A Ball needs no repeated policy row when that binding selection covers it; it records only an explicit local selection or allowlisted difference. Resolution must yield one effective profile; a mutable “latest” alias or runtime registry is invalid.

## 5. Claims

Profile selection is not a claim. When a project says `durable`, `at-least-once`, `isolated`, `secure`, `atomic`, `zero overhead`, or similar, create a claim record:

```text
claim and exact boundary
effective profile/policy digest
mechanism and failure assumptions
ordering/delivery point and retention horizon
evidence artifact and observed result
explicit non-guarantees
owner/review date
```

Without the record, remove the claim rather than adding speculative mechanisms.

## 6. Foundation

When a shared foundation exists or changes, one project-scoped authoritative source and scan classify exports as stable mechanical primitives or domain/policy authority. Record the export/dependency scan, mutable-state/protocol/resource-authority result, accepted exceptions, owner, and exact source or policy digest once.

The project/binding scope covers its Balls without repeating the scan in each one; a Ball records only a local exception or delta. If no shared foundation exists, no foundation artifact is created.

## 7. Change output

Routine composition work reports only changed edges, effective declaration or policy/delta, newly activated risks, and tests. Update Assembly, target contracts, Flow state, or shared policy only where authoritative. A full graph/claim dossier is produced only for a conformance or release claim under [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md).
