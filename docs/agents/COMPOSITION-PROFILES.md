# Composition and Profiles

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Use this runbook only when an inter-Ball edge, Flow, shared foundation, profile delta, or guarantee claim exists. Assembly is authoritative for routes; an exact project policy is authoritative only when one is referenced. Do not repeat either source per Ball.

This runbook is a projection of the marked Core source clauses. It constrains valid ownership and authority graphs but does not promise one unique decomposition graph for a system.

**Task routing:** edge classification, reads, command edges, Flow selection, and graph resolution remain below; effective profiles, claims, Foundation, and change output continue in [COMPOSITION-PROFILES-AND-CLAIMS.md](COMPOSITION-PROFILES-AND-CLAIMS.md).

## 1. Classify existing edges

Classify physical helper imports before applying the four-kind semantic inter-authority taxonomy below:

- a Ball-local utility is owned by exactly one Ball and logical role, is called only inside that authority/role, and creates no Assembly route or protocol-dependency row;
- a utility shared across Balls is mechanical Foundation and must satisfy `PKB-AR-CMP-004`; and
- shared domain/business semantics cannot remain ownerless: retain explicit Ball-local implementations or assign one Ball/Flow owner and consume its declared Application Surface/protocol.

Every physical import remains visible in the acyclic compile-time graph. A Ball-local or mechanical-Foundation import is not a `Direct Control Dependency` unless it exposes another Ball's Application Surface or synchronously transfers control across Ball authorities.

| Kind | Trigger | Required authoritative facts |
|---|---|---|
| `ReadDependency` | read from another authority | caller, target authority, target-owned total `Query -> ResultPayload` effective protocol identity covering every reachable post-admission semantic outcome, target read/status authority, caller freshness/consistency requirement, Assembly route/binding; optional fields only by existing triggers |
| `DeclaredCommandDependency` | one addressed command hop | target-owned typed operation and acceptance/refusal meaning, permitted target interface and return binding; explicit versions/provenance/idempotency/deadline/status only when triggered |
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

Actual dependencies, participants, and routes remain visible through types and Assembly wiring. Static finite composition requires no numeric maxima on these counts. A project may select a concrete maintenance/resource limit, but Core does not make a new permitted consumer revise a global connection budget.

`Direct Control Dependency` is an orthogonal graph classification: a compile-time import of another Ball's Application Surface or a synchronous cross-Ball call that transfers control before an asynchronous handoff/yield. Generated inline dispatch is direct control when it invokes the target on that current control path. A declared asynchronous route does not add this edge merely because code is generated, but its binding does add the edge if target execution occurs synchronously before handoff/yield.

## 2. Read and command edges

A `ReadDependency` imports the target-owned read meaning and Application Surface needed by the caller Nucleus's closed Query contract; it does not redeclare, mirror, re-own, or re-export them. Protocol version, stamp form, actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields materialize only under their existing triggers. A same-build fact may resolve statically. Absent fields create no placeholders.

A trusted boundary may return an existing declared validation/admission `BoundaryResponse` before invoking the target read. Once admitted, canonical `read(...) -> ReadResult` returns only its successfully evaluated target-owned result and creates no Decision, accepted-input marker, revision, `SemanticHandle`, or `SemanticOutput`. Its `ResultPayload` is total over the Query's reachable post-admission semantic outcomes and contains only the target-declared permitted, denied, redacted, or deliberately non-disclosing variants that apply. Wrong target/version/read authority/result mapping/stamp fails at the boundary and is not semantic `NotFound`; boundary, caller, Assembly, runtime, and generated code cannot substitute `BusinessRejection`, exception, post-admission `BoundaryResponse`, undeclared `NotFound`, or another payload. They transport and verify without redefining, synthesizing, re-exporting, or altering the target payload, status fact, snapshot identity, stamp, or business meaning. Multiple dependencies remain independent target snapshots and promise no atomic multi-source snapshot without a separate mechanism.

A read-like operation remains a command only when the caller requires accepted provenance, stable command/step identity, idempotent replay, accepted-operation status, or reconciliation. It then pays target acceptance and revision through the command bridge. An ordinary non-recording lookup uses `Query`/`ReadDependency` and creates no target Decision, revision, or output. Query alone does not introduce a new mandatory per-Ball read-limit field; already effective input/read-work/response/route/buffering bounds still apply.

For a command route, the target owns the typed operation, Application Surface, and acceptance/refusal meaning. The caller imports these exact types without mirroring or re-owning them. Assembly provides the permitted target interface and binds execution/return without changing domain policy. An immediate trusted same-build call uses its target and call scope as source/return evidence; it needs no added command/result/issuer token or carrier type. Separately delivered or untrusted messages keep the required correlation and source verification.

Execute the command only after source acceptance and completion of its transition. The target serializes its own decision/acceptance, and its result or refusal goes through the source's serialized handler. Failure after target acceptance cannot become `NotAccepted`. A finite complete synchronous execution, including every completion handler, needs no carried causal scope/depth or numbered reservation. Actual storage capacity and potentially growing work remain bounded; preserve accepted outputs/completions. One reusable binding can enforce this sequence for multiple owners. The invocation contributes `source -> target` to direct control; its return creates no reverse edge. An async handoff removes only the synchronous-invocation contribution, preserves separate imports, and retains every active causal bound.

For example, Assembly gives read consumers `CounterRead` and allowed increment consumers `CounterCommands`. A further allowed consumer uses the existing interface through wiring; Counter's result type and decision logic do not acquire consumer names. If the business operation depends on caller identity, Counter still checks that policy using trusted input.

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

Check the full execution structure before adding numeric causal accounting. A source that issues one command, accepts its result, and stops is finite. A result handler that issues the same command again is not bounded merely because imports form a DAG. Static finite structure needs no separate geometric fan-out total, causal fields, or level-1/level-2 reservations. Real queues, external calls, retained outputs, dynamic consumer sets, and other growing work retain finite limits and pre-acceptance protection against accepted loss.

When numeric `maxCumulativeFanout` is needed or explicitly selected, its counting contract is exact. The scope is one accepted root operation or another explicitly named causal scope. One unit is one distinct accepted `SemanticOutput` branch from its accepted source to one effective route and consumer/executor: a single-destination output counts once and a Signal with `k` consumers counts `k`. Terminal branches count; co-reachable branches across all levels sum; separate branches converging on one authority still count separately. Mutually exclusive alternatives share one reservation sized to their maximum and only the selected accepted branch consumes it. Retry/redelivery of the same accepted output through the same route to the same consumer adds no unit, while a new accepted output does. Async handoff preserves scope and remaining budget; only a separately declared independent root starts fresh.

Actual output, consumer, and causal-work bounds keep their distinct meanings. A complete finite execution structure needs no counter. When numeric accounting is selected, admission checks the complete candidate batch and required completion capacity: exact `N` may accept, while the first `N+1` branch returns `AdmissionFailure(CausalBudgetExceeded)` and accepts no State, revision, partial batch, or dispatch. Accounting is runtime admission state, not `DecisionContext` or a new protocol.

Each present ceiling resolves through a static proof, local declaration, or optional exact project policy. Assembly records endpoints, effective protocol identity, delivery semantics, and triggered route fields/deltas, but it never defines or synthesizes a producer/target-owned protocol type or business meaning. Verify no wildcard target/consumer, duplicate resolution, caller-owned mirror or redeclaration, protocol re-export, foreign State/internal/private-adapter import, Interaction/Assembly synthesis, unsupported multi-source atomicity claim, or routed type missing from its owner. For an independently versioned Signal route, verify the exact producer/consumer pair selected by Assembly; Catalog's canonical v2 fixture uses `2.0.0/2.0.0` for `ProductSelectionConfirmed`.


## 5. Effective profile

Moved to [Effective profile](COMPOSITION-PROFILES-AND-CLAIMS.md#5-effective-profile).

## 6. Claims

Moved to [Claims](COMPOSITION-PROFILES-AND-CLAIMS.md#6-claims).

## 7. Foundation

Moved to [Foundation](COMPOSITION-PROFILES-AND-CLAIMS.md#7-foundation).

## 8. Change output

Moved to [Change output](COMPOSITION-PROFILES-AND-CLAIMS.md#8-change-output).
