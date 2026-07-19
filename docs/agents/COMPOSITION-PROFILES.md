# Composition and Profiles

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook only when an inter-Ball edge, Flow, shared foundation, profile delta, or guarantee claim exists. Assembly is authoritative for routes; an exact project policy is authoritative only when one is referenced. Do not repeat either source per Ball.

This runbook is a projection of the marked Core source clauses. It constrains valid ownership and authority graphs but does not promise one unique decomposition graph for a system.

## 1. Classify existing edges

| Kind | Trigger | Required authoritative facts |
|---|---|---|
| `ReadDependency` | read from another authority | caller, target authority, target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, Assembly route/binding; optional fields only by existing triggers |
| `DeclaredCommandDependency` | one addressed command hop | target-owned versioned `ModuleCommand -> ModuleResult` mapping and static refusal classification, exact effective protocol identity, verified ingress and accepted-frame return binding; idempotency/deadline/status only when triggered |
| `DeclaredSignalDependency` | bounded observation of publication | producer/consumer, effective protocol identity, delivery, source identity/provenance, fan-out/size; version/dedup/ordering/buffering/depth/attempts only when triggered |
| `FlowParticipation` | material coordination needs one owner | Flow owner, participants, steps and at least one owned lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome; other fields only when triggered |

No edge is implied by package visibility, service locator, event-bus label, or runtime discovery. Each edge resolves once in Assembly.

`Direct Control Dependency` is an orthogonal graph classification: a compile-time import of another Ball's application surface or a synchronous cross-Ball call that transfers control before an asynchronous handoff/yield. Generated inline dispatch is direct control when it invokes the target on that current control path. A declared asynchronous route does not add this edge merely because code is generated, but its binding does add the edge if target execution occurs synchronously before handoff/yield.

## 2. Read and command edges

A `ReadDependency` imports target-owned read meaning; it does not redeclare or re-export it. Protocol version, stamp form, actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. A same-build fact may resolve statically. Absent fields create no placeholders.

A trusted boundary may return an existing declared validation/admission `BoundaryResponse` before invoking the target read. Once admitted, canonical `read(...) -> ReadResult` returns only its declared successful result and creates no Decision, accepted-input marker, revision, `SemanticHandle`, or `SemanticOutput`. Wrong target/version/read authority/result mapping/stamp fails at the boundary and is not semantic `NotFound`. Caller, Assembly, and generated code transport and verify without redefining, synthesizing, re-exporting, or altering the target payload, status fact, snapshot identity, stamp, or business meaning. Multiple dependencies remain independent target snapshots and promise no atomic multi-source snapshot without a separate mechanism.

A read-like operation remains a command only when the caller requires accepted provenance, stable command/step identity, idempotent replay, accepted-operation status, or reconciliation. It then pays target acceptance and revision through the command bridge. An ordinary non-recording lookup uses `Query`/`ReadDependency` and creates no target Decision, revision, or output. Query alone does not introduce a new mandatory per-Ball read-limit field; already effective input/read-work/response/route/buffering bounds still apply.

For a command route, the target owns one exact mapping and refusal classification. Assembly binds verified `ModuleCommandRequest -> ModuleCommandPulse` ingress and accepted `ModuleResultOutput -> ModuleResultPulse` return, but cannot synthesize or modify `commandSource`, `resultSource`, target payload, issuer provenance, or refusal meaning. Same-stack erasure proves the identical accepted tuples.

The same-stack source/root, target, and source-result Decisions consume causal levels `0/1/2`. Before acceptance, source and target reserve the target-completion and result-completion slots respectively. Missing result capacity returns verified carrier `AdmissionFailure(CausalBudgetExceeded)` and creates no accepted target Decision/result. This produces one direct-control edge `source -> target`; the result return is not a reverse edge. An asynchronous handoff removes the edge but preserves causal scope, depth, and remaining budget.

## 3. One hop or Flow

Keep one hop when there is one target and no independently owned lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, manual queue, or terminal outcome.

Use a Flow when one material coordination property makes the one-hop conditions false. Mere call count, a sequence with no owned lifecycle, or one valid command hop is insufficient. The Flow owns only orchestration state and field-minimized retained values needed by later Decisions. It never copies participant truth or reads runtime/participant ledgers as hidden Decision input.

Compensation is new fallible work. When triggered it has its own identity and outcome; duplicate execution adds idempotency, a privileged consequence adds current action-specific authorization, a deadline adds its time contract, and an ambiguous result adds reconciliation. It targets retained authoritative values/handles, not inferred history.

## 4. Graph resolution

Inspect only graph kinds that exist:

1. compile-time imports;
2. `Direct Control Dependency` edges;
3. async signal/command feedback; and
4. data/read provenance.

The first two are independently and unconditionally acyclic. A project policy, overlay, compensating control, or `WaiverRecord` cannot make either cycle conforming; a waiver records deliberate non-conformance for its exact scope. Async feedback begins only after an explicit bounded handoff/yield, creates no direct-control edge, and resolves an owner, stable identity, finite causal budget, escape condition, and fan-out protection; deduplication and retry budgets appear only when duplicate delivery or retry exists.

Each present ceiling resolves through a static proof, local declaration, or optional exact project policy. Assembly records endpoints, effective protocol identity, delivery semantics, and triggered route fields/deltas, but it never defines or synthesizes a producer/target-owned protocol type or business meaning. Verify no wildcard target/consumer, duplicate resolution, protocol re-export, hidden feature-internal import, unsupported multi-source atomicity claim, or routed type missing from its owner. For an independently versioned Signal route, verify the exact producer/consumer pair selected by Assembly; Catalog's canonical v2 fixture uses `2.0.0/2.0.0` for `ProductSelectionConfirmed`.

## 5. Effective profile

Resolve the four dimensions independently, even when one named policy supplies them together:

| Dimension | Choices | Triggered mechanism |
|---|---|---|
| Execution | `Inline` / `BoundedConcurrent` | mailbox/workers/backpressure only for concurrent selection |
| State | `Transient` / `SnapshotOutbox` / `EventJournal` | durable records/recovery/status only for durable selection |
| Isolation | `InProcess` / `Isolated` | IPC/principal/resource containment only for isolation selection |
| Security | `Standard` / `Hardened` | stronger actor/grant/audit/abuse controls only for hardening selection |

A project/binding may select an exact default policy once. A Ball needs no repeated policy row when that binding selection covers it; it records only an explicit local selection or allowlisted difference. Resolution must yield one effective profile; a mutable “latest” alias or runtime registry is invalid.

## 6. Claims

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

## 7. Foundation

When a shared foundation exists or changes, one project-scoped authoritative source and scan classify exports as stable mechanical primitives or forbidden domain/policy/route authority. Record the export/dependency/call-graph scan, mutable-state/protocol/resource-authority/route-selection/hidden-communication result, accepted exceptions, owner, and exact source or policy digest once.

Shared foundation contains no mutable business meaning, business-policy decision, domain authority, route selection, service locator, runtime registry, or hidden communication state. The project/binding scope covers its Balls without repeating the scan in each one; a Ball records only a local exception or delta. If no shared foundation exists, no foundation artifact is created.

## 8. Change output

Routine composition work reports only changed edges, effective declaration or policy/delta, newly activated risks, and tests. Update Assembly, target contracts, Flow state, or shared policy only where authoritative. A full graph/claim dossier is produced only for a conformance or release claim under [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md).
