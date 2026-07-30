# Composition and Profiles

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook only when an inter-Ball edge, Flow, shared foundation, profile delta, or guarantee claim exists. Assembly is authoritative for routes; an exact project policy is authoritative only when one is referenced. Do not repeat either source per Ball.

This runbook is a projection of the marked Core source clauses. It constrains valid ownership and authority graphs but does not promise one unique decomposition graph for a system.

## 1. Classify existing edges

Classify physical helper imports before applying the four-kind semantic inter-authority taxonomy below:

- a Ball-local utility is owned by exactly one Ball and logical role, is called only inside that authority/role, and creates no Assembly route or protocol-dependency row;
- a utility shared across Balls is mechanical Foundation and must satisfy `PKB-AR-CMP-004`; and
- shared domain/business semantics cannot remain ownerless: retain explicit Ball-local implementations or assign one Ball/Flow owner and consume its declared Application Surface/protocol.

Every physical import remains visible in the acyclic compile-time graph. A Ball-local or mechanical-Foundation import is not a `Direct Control Dependency` unless it exposes another Ball's Application Surface or synchronously transfers control across Ball authorities.

| Kind | Trigger | Required authoritative facts |
|---|---|---|
| `ReadDependency` | read from another authority | caller, target authority, target-owned total `Query -> ResultPayload` effective protocol identity covering every reachable post-admission semantic outcome, target read/status authority, caller freshness/consistency requirement, Assembly route/binding; optional fields only by existing triggers |
| `DeclaredCommandDependency` | one addressed command hop | target-owned versioned `ModuleCommand -> ModuleResult` mapping and static refusal classification, exact effective protocol identity, verified ingress and accepted-frame return binding; idempotency/deadline/status only when triggered |
| `DeclaredSignalDependency` | bounded observation of publication | producer/consumer, effective protocol identity, delivery, source identity/provenance, fan-out/size; version/dedup/ordering/buffering/depth/attempts only when triggered |
| `FlowParticipation` | material coordination needs one owner | one Flow/participant pair, Flow authority, participant authority, exact participant `Application Surface`, non-empty bounded Flow-owned coordination responsibilities, and bounded refs to existing read/command/signal dependencies |

No semantic inter-authority edge is implied by package visibility, a helper-package boundary, service locator, event-bus label, or runtime discovery. Each routed edge resolves once in Assembly.

An `Application Surface` is the exact set of owner-authored public semantic types and entrypoints deliberately exposed to another Ball or Assembly. A Nucleus may import the exact declared target- or producer-owned Application Surfaces required by its closed Query, Pulse, and Decision contracts. The import remains a dependency on public semantics and transfers no ownership: foreign mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, caller-owned mirrors or redeclarations, and re-exported foreign contracts remain excluded. Interaction and Assembly may verify, bind, and transport but cannot synthesize the imported semantic contract. The minimum resolved `FlowParticipation` view is:

```text
FlowParticipation {
  flowAuthority
  participantAuthority
  participantApplicationSurface
  flowOwnedCoordination: NonEmptyBoundedSet<
    lifecycle | orderingOrBranchJoin | compensationOrRecovery
    | cancellation | reconciliation | terminalOutcome
  >
  dependencyRefs: BoundedSet<
    ReadDependencyRef | DeclaredCommandDependencyRef | DeclaredSignalDependencyRef
  >
}
```

This is a declaration view, not a runtime envelope or fifth route protocol. Each referenced dependency remains authoritative for its payload ownership, effective identity, route, return binding, limit, and triggered field. Review semantic ownership, Application Surface import, synchronous direct control, asynchronous data path, and Assembly route/version/binding separately; the Application Surface import is recorded in both the compile-time-import and `Direct Control Dependency` graphs.

When `maxDeclaredDependenciesPerBall` is present, count each distinct resolved `ReadDependency`, `DeclaredCommandDependency`, `DeclaredSignalDependency`, and `FlowParticipation` declaration owned by the Ball once. A dependency referenced by `FlowParticipation` remains counted in its own declaration kind in addition to the FlowParticipation row; multiple operations to the same target remain distinct, and an exact duplicate or alias/equivalent row resolving to an existing identity is invalid rather than free or deduplicated. Static resolution accepts exactly `N` declarations and rejects the Ball contract when a first distinct declaration would make `N+1`, before execution.

`Direct Control Dependency` is an orthogonal graph classification: a compile-time import of another Ball's Application Surface or a synchronous cross-Ball call that transfers control before an asynchronous handoff/yield. Generated inline dispatch is direct control when it invokes the target on that current control path. A declared asynchronous route does not add this edge merely because code is generated, but its binding does add the edge if target execution occurs synchronously before handoff/yield.

## 2. Read and command edges

A `ReadDependency` imports the target-owned read meaning and Application Surface needed by the caller Nucleus's closed Query contract; it does not redeclare, mirror, re-own, or re-export them. Protocol version, stamp form, actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. A same-build fact may resolve statically. Absent fields create no placeholders.

A trusted boundary may return an existing declared validation/admission `BoundaryResponse` before invoking the target read. Once admitted, canonical `read(...) -> ReadResult` returns only its successfully evaluated target-owned result and creates no Decision, accepted-input marker, revision, `SemanticHandle`, or `SemanticOutput`. Its `ResultPayload` is total over the Query's reachable post-admission semantic outcomes and contains only the target-declared permitted, denied, redacted, or deliberately non-disclosing variants that apply. Wrong target/version/read authority/result mapping/stamp fails at the boundary and is not semantic `NotFound`; boundary, caller, Assembly, runtime, and generated code cannot substitute `BusinessRejection`, exception, post-admission `BoundaryResponse`, undeclared `NotFound`, or another payload. They transport and verify without redefining, synthesizing, re-exporting, or altering the target payload, status fact, snapshot identity, stamp, or business meaning. Multiple dependencies remain independent target snapshots and promise no atomic multi-source snapshot without a separate mechanism.

A read-like operation remains a command only when the caller requires accepted provenance, stable command/step identity, idempotent replay, accepted-operation status, or reconciliation. It then pays target acceptance and revision through the command bridge. An ordinary non-recording lookup uses `Query`/`ReadDependency` and creates no target Decision, revision, or output. Query alone does not introduce a new mandatory per-Ball read-limit field; already effective input/read-work/response/route/buffering bounds still apply.

For a command route, the target owns one exact mapping, Application Surface, and refusal classification. The caller Nucleus may import those exact target-owned command/result types for its closed Pulse/Decision contract but cannot mirror or redeclare them. Assembly binds verified `ModuleCommandRequest -> ModuleCommandPulse` ingress and accepted `ModuleResultOutput -> ModuleResultPulse` return, but cannot synthesize or modify `commandSource`, `resultSource`, target payload, issuer provenance, refusal meaning, or any replacement contract. Same-stack erasure proves the identical accepted tuples.

The same-stack source/root, target, and source-result Decisions consume causal levels `0/1/2`. Before source acceptance, the source reserves one level-1 alternative-completion slot. Target acceptance consumes it after separately reserving level 2; any verified target validation/admission/`decide` rejection before acceptance consumes no target level/frame/revision/output and atomically transfers level 1 to the source carrier Decision. Missing level-1 capacity prevents source acceptance/dispatch; missing level-2 capacity prevents target acceptance and returns verified carrier `AdmissionFailure(CausalBudgetExceeded)`. A carrier-handling Decision reserves further synchronous outputs from the remaining budget. This invocation contributes `source -> target` to direct control; the result return is not a reverse edge. An asynchronous handoff removes only the synchronous-invocation contribution, retains any separately present compile-time-import edge, and preserves causal scope, depth, and remaining budget without inheriting the same-stack transfer rule merely from transport.

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

When `maxRoutesPerFlow` is present, count only distinct effective Assembly command/result round-trip mappings used by that Flow. The canonical command ingress plus its bound result return is one unit. Read and signal dependencies remain under their own bounds and consume zero units of this command-route limit; `FlowParticipation` and `dependencyRefs` also consume zero. Different command operations remain distinct even when they share a target or physical binding, and any exact duplicate, spelling alias, equivalent repeated row, or split-leg alias is invalid rather than free capacity. Static resolution accepts exactly `N` command mappings and rejects the Flow/Assembly contract at `N+1`, before execution. Checkout therefore has nine such route rows, one for each of its nine command operations.

When `maxCumulativeFanout` is present, its counting contract is exact. The scope is one accepted root operation or another explicitly named causal scope. One unit is one distinct accepted `SemanticOutput` branch from its complete accepted source tuple to one effective route and consumer/executor: a single-destination output counts once and a Signal with `k` consumers counts `k`. Terminal branches count; co-reachable branches across all levels sum; separate branches converging on one authority still count separately. Mutually exclusive alternatives share one reservation sized to their maximum and only the selected accepted branch consumes it. Retry/redelivery of the same source tuple through the same route to the same consumer adds no unit, while a new accepted output source tuple does. Async handoff preserves scope and remaining budget; only a separately declared independent root starts fresh.

`maxOutputsPerDecision`, `maxConsumersPerSignal`, and `maxCausalDepth` keep their separate meanings. A static graph/control-flow proof may establish cumulative fan-out without a runtime artifact. Otherwise the existing total causal reservation admits the complete candidate batch before acceptance: exact `N` may accept, while the first `N+1` branch returns `AdmissionFailure(CausalBudgetExceeded)` and accepts no State, revision, partial batch, or dispatch. The reservation is runtime admission state, not `DecisionContext` or a new protocol.

Each present ceiling resolves through a static proof, local declaration, or optional exact project policy. Assembly records endpoints, effective protocol identity, delivery semantics, and triggered route fields/deltas, but it never defines or synthesizes a producer/target-owned protocol type or business meaning. Verify no wildcard target/consumer, duplicate resolution, caller-owned mirror or redeclaration, protocol re-export, foreign State/internal/private-adapter import, Interaction/Assembly synthesis, unsupported multi-source atomicity claim, or routed type missing from its owner. For an independently versioned Signal route, verify the exact producer/consumer pair selected by Assembly; Catalog's canonical v2 fixture uses `2.0.0/2.0.0` for `ProductSelectionConfirmed`.

## 5. Effective profile

Resolve the four dimensions independently, even when one named policy supplies them together:

| Dimension | Choices | Triggered mechanism |
|---|---|---|
| Execution | `Inline` / `BoundedConcurrent` | mailbox/workers/backpressure only for concurrent selection |
| State | `Transient` / `SnapshotOutbox` / `EventJournal` | durable records/recovery/status only for durable selection |
| Isolation | `InProcess` / `Isolated` | IPC/principal/resource containment only for isolation selection |
| Security | `Standard` / `Hardened` | stronger actor/grant/audit/abuse controls only for hardening selection |

State-profile resolution also selects exactly one mutation and acceptance form. `Transient` and `SnapshotOutbox` use the canonical `decide(...) -> SnapshotDecisionResult<State>` signature and publish the flattened `AcceptedSnapshotDecisionFrame<State> { commitRevision?, nextState, outputs }`. `EventJournal` uses the canonical `decide(...) -> EventDecisionResult` signature and records `AcceptedEventCommit` with `decision: EventDecision`; it reconstructs State through `evolve` and never adds an independent Event `nextState`. These are mutually exclusive selected contracts, not a runtime union.

A project/binding may select an exact default policy once. A Ball needs no repeated policy row when that binding selection covers it; it records only an explicit local selection or allowlisted difference. Resolution must yield one effective profile; a mutable “latest” alias or runtime registry is invalid.

Credential scope and credential containment are separate triggers. An ordinary scoped external credential may remain in a trusted `InProcess + Hardened` binding when the threat model makes no hostile-component-containment claim and requires no separate security principal for that credential. Select `Isolated` when the credential must be inaccessible to less-trusted co-resident code or a high-privilege credential requires protection by its own principal; least privilege and all triggered Policy/Execution Gate rules apply in either profile.

## 6. Claims

Profile selection is not a claim. When a project says `durable`, `at-least-once`, `isolated`, `secure`, `atomic`, `zero overhead`, or similar, create a claim record:

```text
claim and exact boundary
exact scope
effective profile/policy digest
mechanism and failure assumptions
ordering/delivery point and retention horizon
evidence artifact and observed result
explicit non-guarantees
owner/review date
```

The record never derives target receipt/acceptance, executor safety, durable reply, eventual delivery, once-only execution, or recovery beyond its named boundary merely from source durability or retained pending work. Without the record, remove the claim rather than adding speculative mechanisms.

## 7. Foundation

When a helper is proposed for more than one Ball, one project-scoped authoritative source and scan classify it as stable mechanical Foundation or reject it as ownerless domain/business semantics. Record the export/dependency/call-graph scan, mutable-state/protocol/resource-authority/route-selection/hidden-communication result, accepted exceptions, owner, and exact source or policy digest once.

Shared foundation contains no mutable business meaning, business-policy decision, domain authority, route selection, service locator, runtime registry, or hidden communication state. Domain/business helper semantics remain Ball-local, including deliberate small duplication, or acquire one Ball/Flow owner and a declared Application Surface/protocol; relabelling them as Foundation is invalid. The project/binding scope covers its Balls without repeating the scan in each one; a Ball records only a local exception or delta. If no shared mechanical foundation exists, no foundation artifact is created.

## 8. Change output

Routine composition work reports only changed edges, effective declaration or policy/delta, newly activated risks, and tests. Update Assembly, target contracts, Flow state, or shared policy only where authoritative. A full graph/claim dossier is produced only for a conformance or release claim under [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md).
