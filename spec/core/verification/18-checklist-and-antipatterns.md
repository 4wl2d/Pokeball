# Core part — Practical checklist and anti-patterns

[Core contents](../../pokeball-architecture-core.md) · [← Verification: profile, security, and claim tests](17-03-profile-security-and-claim-tests.md) · [Laws PBA-01–18 →](../reference/20-01-laws-boundary-decision-state.md)

> Canonical part 18 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 18. Practical checklist

### 18.1. Core Ball and trigger resolution

Always-applicable checks:

- [ ] The `Ball` boundary passes the §4.4 decision tree and its combine/separate/utility/Feature/Flow/Read Model falsifier.
- [ ] Core validity does not imply one unique decomposition graph.
- [ ] A materialized `StateKey` exists only when the instance-identity trigger requires it.
- [ ] Interaction, Nucleus, Resource/route, Assembly, runtime/acceptor, status/read authority, and shared-foundation roles—and every present Trusted Boundary edge—have an inspectable role/edge map, permitted call graph, accepted-write sites, and provenance trace; Interaction is a role, Trusted Boundary is an authorized verification/construction edge, and same-file/stack/generated co-location merges neither authority nor evidence.
- [ ] Used protocol categories are closed and typed; an absent category is genuinely empty and has no placeholder.
- [ ] The canonical ordered unions are exactly `Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse` and `ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest`; no payload-only alias is treated as an envelope.
- [ ] The current cause is one typed `Pulse`.
- [ ] The trusted binding boundary constructs every non-empty verified/bounded `DecisionContext` under a Ball/Nucleus-owned semantic schema and interpretation.
- [ ] The context contains only observations that affect the Decision.
- [ ] State has one writer.
- [ ] Every mutable semantic fact has one authority.
- [ ] No Ball reads another authority's mutable state directly.
- [ ] `EphemeralState` contains only non-decision UI/transport mechanics. Any UI/transport value that can change a business Decision is committed State or an explicit trusted current `Pulse`/`DecisionContext` input.
- [ ] `decide` is pure and terminating.
- [ ] Present variable dimensions have one finite effective bound.
- [ ] If `maxCumulativeFanout` is present, its one causal scope and accepted source-output-to-effective-route/consumer branch unit are explicit: terminal/converging branches count, co-reachable branches sum, mutually exclusive alternatives share the maximum reservation, duplicate/redelivery does not increment, async handoff preserves scope, and exact `N/N+1` rejects the whole over-limit Decision without partial dispatch.
- [ ] A numeric `maxTransitionSteps` resolves one immutable versioned Decision Work Meter; for equal canonical inputs under the same binding, transition artifact version, and meter identity/version it charges equal non-negative integral totals, starts once at zero and never resets within one `decide`, passes at exact `N`, accepts no frame/State/revision/output/dispatch at `N+1`, and is compared across bindings only when meter identity/version, transition artifact version, and unit definition are all equal.
- [ ] Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without static type-and-representation proof resolves one immutable `BoundedByteMeasure` dimension/identity/version/representation/limit tuple. Input fixes raw-or-normalized stage and metadata/Context inclusion; State fixes the complete candidate-next-State semantic representation; output fixes the complete ordered `Decision.outputs` sequence and envelopes. Alternate representations map exactly, erasure preserves counts, unequal tuples are incomparable, exact `N` passes, and stage-specific `N+1` accepts no prohibited semantic artifact.
- [ ] Overflow accepts neither partial state nor partial output.
- [ ] A Decision is accepted in full or not accepted.
- [ ] The selected state profile exposes exactly one §3.3 mutation algebra and accepted frame: Snapshot uses `SnapshotDecisionResult`/`AcceptedSnapshotDecisionFrame`, EventJournal uses `EventDecisionResult`/`AcceptedEventCommit`, and Event mode has no independent `nextState` or mandatory cross-profile runtime union.
- [ ] Representation and declared closed protocol/type invariants fail before semantic admission as `ValidationFailure`; State- or semantic-Context-owned rules reach the Nucleus and reject as `BusinessRejection`. Promoting a fixed rule into the type changes the versioned protocol identity rather than switching stages under the same version.
- [ ] Reentrant mutation is prohibited.
- [ ] No ambient resource authority, service locator, runtime registry, mutable business global, or foundation-mediated hidden communication is available to application code.

Applicability checks:

- [ ] The closed protocol/type/profile/route/risk/claim inventory has been evaluated against §20.1; inferred triggers cannot be disabled by missing metadata.
- [ ] Every triggered guardrail resolves to one effective static proof, local declaration, or exact in-scope reusable policy plus permitted delta; stale, cyclic, conflicting, wrong-version/profile/binding/environment references fail.
- [ ] Absent triggers create no mechanism, empty table, zero field, evidence dossier, or `N/A` row. Ambiguous absence is resolved explicitly or treated as present.
- [ ] If a conformance/release claim or accepted ambiguity-resolution decision relies on absence, one exact `TriggerAbsenceProof` binds the non-`always` class, anchor, scope/profile, inventory/digests, predicate, owner, and invalidation conditions.
- [ ] A present trigger or claim invalidates contrary proof.
- [ ] Wrong/stale/unresolved/conflicting evidence or any invalidation condition blocks reliance until reevaluation.
- [ ] Ordinary design/adoption creates no proof placeholder.
- [ ] A boundary worksheet is project-owned `SHOULD` guidance only for active adoption/pilot work; relevant workload, method, baseline, and continue/reshape/stop thresholds are project-selected, Core supplies no universal number, and negative-adoption cases or established non-pilot work create no worksheet, empty Ball, or conformance placeholder.
- [ ] If outputs exist, commit-before-dispatch and complete-batch retention are proven.
- [ ] If an Inline deque or continuation carries multiple causes, each item retains or resolves its own trusted per-Pulse `DecisionContext`; a later cause does not inherit root actor/grant/reserved-ID/time/validity fields by variable reuse.
- [ ] Every version consumed by a Decision or retained as lineage is a named verified current Context field or an exact enclosing-binding proof; Checkout explicitly supplies `artifactVersion = IV`, retains `interactionArtifactVersion = IV`, and keeps it distinct from `transitionArtifactVersion` across retry/recovery/migration.
- [ ] Remaining causal budget/depth/slots, execution quantum, and current runtime capacity reach only reservation/admission/continuation logic; they are absent from `DecisionContext` and cannot change the candidate Decision.
- [ ] Detached/addressable work has stable semantic identity, while immediate local output need not materialize it.
- [ ] A local command uses typed target access and call scope; independently delivered commands/results verify the required source/target causal identity and provenance.
- [ ] Every target refusal follows its static versioned carrier-or-accepted-result classification. Carrier `BusinessRejection` projects `RejectedBeforeAcceptance + NotExpected`; accepted result rejection projects `Accepted + Rejected`; conflicting evidence fails closed.
- [ ] Local pre-acceptance refusal creates no accepted target state/output and reaches the source through its serialized handler; a later executor failure preserves acceptance.
- [ ] If a Query/stamped read/status path exists, its exact result/stamp/authority contract is closed.
- [ ] All reads remain non-mutating.
- [ ] Each status namespace has one committed revisioned single-writer authority.
- [ ] Candidate `OperationId` reservation alone creates no operation/status authority. Root validation, admission, or Decision rejection creates only its typed `BoundaryResponse`; a covered later lookup may return `NotFound`, never a known rejected root row. Participant nonacceptance remains a facet of an already accepted source operation.
- [ ] When root idempotency is triggered, same key/fingerprint within the horizon redelivers the exact original accepted `ReplyOutput` frame and changes only `AttemptId`; different fingerprint is pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))`, creates no semantic artifact, and leaves the prior operation unchanged.
- [ ] Its materializer applies causal order or bounded pending.
- [ ] Its materializer merges idempotently and monotonically without facet loss.
- [ ] Its materializer reserves finite capacity before source acceptance.
- [ ] Its materializer evicts/truncates nothing after acceptance.
- [ ] Its materializer commits a covered-source/empty-pending marker before absence with no resurrection.
- [ ] `Draining` rejects new logical mutations.
- [ ] `Draining` serves available declared Query/status Query paths.
- [ ] `Draining` serves already-accepted completion/cancellation/status inputs.
- [ ] An unavailable read fails before `read`.
- [ ] An admitted read returns only the successfully evaluated `ReadResult`; its target-owned payload closes every reachable permitted/denied/redacted/non-disclosing semantic outcome, the pure Policy Gate selects the variant, no boundary/caller/Assembly invents one, and the read creates no Decision.
- [ ] Status has exactly one query authority per namespace, whether co-located or separate, and never acquires command/business-fact authority; materialization lag is represented by its own stamp.
- [ ] For Catalog v2, all six `CatalogState` variants map exactly once to the composite `CatalogView`, and all six `ProductSelected` transitions preserve state-specific facts and emit only one ordinal-0 `ProductSelectionConfirmed` Signal publication.
- [ ] Catalog's illustrative Ball-owned domain `revision` remains distinct from acceptor-owned `CommitRevision` unless a declared binding proves exact equality while preserving both ownership meanings.
- [ ] Catalog cancellation verifies `Intent.operationId = State.operationId = pendingSearch.operationId` before any handle/Decision/Effect; mismatch is a bounded business rejection with no accepted artifact, and Catalog retains protocol `2.0.0`, schema `2`, and transition artifact `2.0.1` as distinct identities.
- [ ] If a value crosses Decisions, it is retained or reintroduced with only the correlation/version/provenance/retention required by that path; runtime history is not a hidden decision input.
- [ ] If late results, ACK, ambiguity, retry, cancellation, deadline, timer, status, or durable delivery paths exist, only their reachable §9 contracts and race tests are present.
- [ ] A target-command duplicate before accepted result returns only verified ACK proof of the original accepted target frame/pending result and neither waits nor re-decides/re-executes; after result it redelivers the exact accepted result frame with only a new `AttemptId`; conflicting evidence fails closed.
- [ ] Checkout v1 consumes accepted `StillUnknown` on its single status slot into terminal `NeedsManualReconciliation` with `outputs = []`; duplicate delivery adds no Decision/handle, no automatic generation exists, and later proof requires a separately declared manual/recovery artifact.
- [ ] For every active retrying failure mode, exactly one layer is the primary retry owner; every other layer is disabled or finite and proven semantically transparent, and cumulative attempts are checked.
- [ ] If a raw, external-resource, privileged, interpreter, secret, or unsafe path exists, only its reachable §11 guardrails resolve; no forbidden-capability boilerplate substitutes for a closed type graph.
- [ ] Every external Effect uses minimum explicit authority for its bounded operation class at a real capability boundary; a wrapper or local check behind unrestricted authority does not qualify.
- [ ] Every interpreter/dialect edge uses its applicable parameterized, structured, capability-rooted, or context-encoded safe sink.
- [ ] Every reachable secret path through State, any output, persistence, serialization, logs, or telemetry has an explicit policy for that exact path and scope; policy for one path does not cover another.
- [ ] If a privileged action exists, the Nucleus Policy Gate alone decides business permission.
- [ ] Immediately before execution, the Resource/target Execution Gate verifies the triggered proof/capability/constraints/version/freshness/revocation/endpoint/quota/sink without a new business decision.
- [ ] A post-acceptance gate failure remains a typed Resource/result/status path, never a carrier or downgrade.
- [ ] If actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection, the trusted binding constructs verified, bounded, field-minimized context relative to a valid approved issuer or fixed trusted same-stack issuer/realm proof under PBA-44.
- [ ] Forged, wrong-issuer/realm, missing, stale, or unverifiable evidence fails closed.
- [ ] An actor-independent Decision/read creates no actor artifacts.
- [ ] If a shared foundation exists, it contains mechanical primitives only and owns no mutable business meaning, policy decision, domain authority, route selection, service locator, or hidden communication state.
- [ ] A policy reference or `WaiverRecord` never suppresses a trigger or weakens a law; any violated `MUST`/`MUST NOT` keeps its exact scope non-conforming.
- [ ] Performance, durability, delivery, recovery, receipt, acceptance, once-only, RPO, RTO, isolation, security, or conformance language is used only with an exact claim record naming the guarantee boundary, scope, mechanism, assumptions, retention, evidence, and non-guarantees; source durability or retained pending work alone implies no stronger downstream outcome.
- [ ] Each error/fault follows its exact §6.13 stage carrier/result/status mapping.
- [ ] No later failure rewrites prior acceptance.
- [ ] Every concrete fallible-admission profile/binding has one finite closed `AdmissionFailure.reason` union.
- [ ] Unknown/open-string reasons fail.
- [ ] A non-fallible path has no empty union.
- [ ] The §0.1 Core-documentation rule is projected exactly: every new or changed Mermaid diagram in this Core includes a local legend distinguishing semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows; omission blocks that documentation change and does not affect a consuming binding unless project policy adopts the rule.

### 18.2. Composition

Apply this subsection only when an inter-Ball edge or Flow exists.

- [ ] A public dependency is declared as `ReadDependency`, `DeclaredCommandDependency`, `DeclaredSignalDependency`, or `FlowParticipation`.
- [ ] Static dependencies are visible in types and wiring without mandatory numeric dependency/participant/route ceilings; any optional project limit has its own purpose and contract.
- [ ] A physical helper import is classified before that four-kind semantic taxonomy: Ball-local utility owned by exactly one logical role, or shared mechanical Foundation. An ownerless shared domain/business utility fails; shared domain semantics remain local copies or acquire one Ball/Flow owner and a declared Application Surface/protocol.
- [ ] Every utility import remains visible in the acyclic compile-time graph; it adds no Assembly route or dependency row and no `Direct Control Dependency` unless it exposes another Ball's Application Surface or synchronously crosses authority.
- [ ] Every `FlowParticipation` resolves one Flow authority, one participant authority, one exact participant `Application Surface`, a non-empty bounded Flow-owned coordination set, and bounded references to existing read/command/signal dependencies and their Assembly bindings.
- [ ] `FlowParticipation` is a design-time relation view, not a protocol, runtime envelope, fifth route, copied dependency, or transfer of participant facts/authority.
- [ ] Every referenced `Application Surface` remains participant-owned and exposes only deliberate public semantic types/entrypoints—not mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, or re-exported foreign contracts.
- [ ] A caller Nucleus imports only the exact declared target- or producer-owned Application Surfaces required by its closed `Query`, `Pulse`, and `Decision` contracts; import transfers no ownership.
- [ ] Checkout's positive fixture imports the four Cart/Inventory/Payment/Order participant-owned surfaces and resolves the nine target-owned command/result mappings.
- [ ] Checkout has four participation relations and nine command routes. These are inspectable facts of the example, not mandatory Core capacity limits.
- [ ] Caller redeclaration or a structurally identical mirror, foreign State/internal/private-adapter import, protocol re-export, and Interaction/Assembly synthesis all fail.
- [ ] Every `ReadDependency` resolves exactly one target authority, target-owned Query/result mapping and effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding.
- [ ] Caller and Assembly cannot redefine the target payload, stamp, status fact, or read meaning.
- [ ] `ReadDependency` versions, actor/authentication, cache/comparison, source positions, ordering, buffering, timeout, retry, and status fields appear only under existing triggers.
- [ ] Same-build/static wiring may erase wrappers while proving the same contract.
- [ ] Independent reads make no atomic multi-source promise.
- [ ] Query alone creates no new per-Ball read-limit field.
- [ ] A read boundary may return an existing validation/admission response before `read`.
- [ ] Once admitted, only the declared successfully evaluated `ReadResult` returns; policy denial/redaction/non-disclosure is a target-owned payload variant, never `BusinessRejection`, exception, post-admission `BoundaryResponse`, or boundary/Assembly-invented `NotFound`.
- [ ] An admitted read creates no accepted marker, Decision, revision, handle, or semantic output.
- [ ] Wrong version/authority/mapping/stamp is rejected rather than mapped to `NotFound`.
- [ ] Every imported `ModuleCommand`/`ModuleResult` resolves through exactly one target-owned mapping and dependency whose effective protocol identity matches both Assembly ingress and return routes.
- [ ] Explicit versions appear only across independent versioning/deployment.
- [ ] Imported target types are not redeclared as caller-owned.
- [ ] Assembly selects route/version/binding and transports verified `ModuleCommandPulse`/`ModuleResultPulse` values.
- [ ] Assembly does not create or alter `commandSource`, `resultSource`, target payloads, or refusal meaning.
- [ ] An immediate same-build call is checked by its actual target, acceptance order and return; missing tuple fields need not be reconstructed.
- [ ] Assembly/generated wiring does not construct semantic context, select business permission/read results, or move mutable business communication through foundation/runtime state.
- [ ] A read-like operation uses the command bridge only when accepted provenance, stable step identity, idempotent replay, status, or reconciliation is required; an ordinary non-recording read remains `ReadDependency`/`Query` with no target Decision/revision/output.
- [ ] A same-stack command round trip has one `source -> target` direct-control edge and no reverse return edge.
- [ ] The complete finite local execution includes result handling and cannot regenerate work; an import DAG alone is insufficient.
- [ ] Finite local completion needs no carried causal scope/depth or reservation levels.
- [ ] Real queue, external-request and retained-output capacity is secured before accepting work that can otherwise be lost.
- [ ] Completion cannot lose accepted evidence or reset an applicable growing-work budget; further outputs remain subject to their actual bounds.
- [ ] Async handoff removes only the synchronous-invocation contribution to direct control, retains any separately present compile-time-import edge, and does not reset scope/depth/budget.
- [ ] A Feature does not import another Feature's internals.
- [ ] A one-hop command is not artificially turned into a micro-Flow.
- [ ] A Flow owns at least one material coordination property—lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome. Call count or one hop alone is not material coordination; one real property is sufficient when the one-hop conditions fail.
- [ ] A stateful multi-participant workflow has one coordinator owner.
- [ ] A Flow does not copy mutable participant truth.
- [ ] A Flow stores only field-minimized workflow values needed by later decisions/recovery; a participant/runtime ledger, outbox, and history do not become hidden decision input.
- [ ] A public contract does not re-export another authority's owned types.
- [ ] Full structurally finite execution needs no separate geometric fan-out calculation. Growing fan-out has an effective bound, with no acceptance or partial dispatch beyond capacity.
- [ ] Compile-time import and `Direct Control Dependency` graphs are unconditionally acyclic, including generated inline dispatch before handoff/yield. A waiver records deliberate non-conformance and cannot make this check pass.
- [ ] Async feedback, if present, has an owner, stable identity, a finite causal budget, an escape condition, and fan-out protection; idempotency/deduplication and a retry budget appear only when duplicate or retry paths exist.
- [ ] A multi-source read is not called an atomic snapshot without a corresponding mechanism.

### 18.3. Concurrent

Apply this subsection only for `BoundedConcurrent` or another explicitly claimed concurrent binding.

- [ ] The mailbox is bounded.
- [ ] Pre-acceptance backpressure is a typed `BoundaryResponse(AdmissionFailure(reason))` and creates no accepted state/output.
- [ ] Worker concurrency is bounded when workers exist.
- [ ] Out-of-order result tests exist when completion can reorder.
- [ ] Causal depth and cumulative fan-out are bounded when causal feedback/fan-out exists; the complete causal scope preserves the same exact branch count across concurrency and handoff rather than resetting per worker, queue, or hop.
- [ ] Cancellation/deadline/late-result races are represented only when those paths exist.

### 18.4. Durable state

Apply this subsection only for `SnapshotOutbox`, `EventJournal`, or another explicit durability claim.

- [ ] State/events and every present durable output are accepted atomically.
- [ ] Commit-before-dispatch is proven when outputs exist.
- [ ] Ingress/inbox duplicate identity and retention are defined when duplicates are possible.
- [ ] Output identity remains stable when redelivery exists.
- [ ] ACK and business result are distinct when ACK is separately observed.
- [ ] An accepted `ModuleResultOutput` survives a post-acceptance crash according to the target profile; retained/retried/observable result delivery is finitely bounded and target `DispatchStopped` uses `(effectiveProtocolIdentity, commandSource, resultSource)` without setting source facets.
- [ ] Snapshot same-state accepted rejection increments target revision; EventJournal records accepted `NoDomainChange`; duplicate command identity within the horizon reuses the accepted result without another target Decision/revision.
- [ ] A durable target duplicate before result returns only accepted-frame/pending-result ACK proof and does not repeat target work; after result, recovery/redelivery uses the exact accepted result frame.
- [ ] A lost accepted root response is recovered by redelivery of the original accepted Reply frame rather than a new Decision/status lookup; conflicting fingerprint remains pre-Intent validation failure.
- [ ] `OutcomeUnknown` has reconciliation when ambiguous external execution exists.
- [ ] Checkout's accepted `StillUnknown` recovery restores the same terminal manual-reconciliation frame with no automatic output or silent late-proof rewrite.
- [ ] Only the reachable crash/ACK/result/exhaustion cases from §17.7 are tested.
- [ ] Recovery uses committed output records, not rerun `decide`, when dispatchable outputs exist.
- [ ] Recovery restores every retained cross-transition value with only its triggered metadata.
- [ ] A movable owner has a fence; a fixed owner needs none.
- [ ] Operation status is independent of a live reply when the status trigger exists; its physical representation is binding-owned, while causal pending, conflict, capacity, marker, and single-writer semantics remain §9.11-equivalent.
- [ ] Crash after accepted target `ModuleResultOutput` preserves the target result source; target route exhaustion records target `DispatchStopped`, and source status remains Pending/Unknown until verified result or reconciliation.
- [ ] RPO/RTO and downstream durability are not assumed automatically.

### 18.5. Hardened / Isolated

Apply this subsection only for the selected `Hardened`/`Isolated` profile or corresponding security/isolation claim.

PBA-44 already applies in every profile when actor context can change a Decision or Query/status-read authorization/result selection. This subsection checks only the stronger profile-specific realization and evidence.

- [ ] Actor-dependent `DecisionContext`/`ReadContext` comes from a trusted binding boundary when actor identity affects semantics; fixed trusted same-stack scope may prove issuer/realm statically, and actor-independent paths create no actor artifacts.
- [ ] A privileged output is bound to a scoped grant when proof crosses an authority.
- [ ] The Nucleus Policy Gate owns business permission; the Execution Gate checks every triggered technical proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink immediately before each privileged execution without a new business decision.
- [ ] A present grant is authentic and fail-closed bound to every constrained payload field.
- [ ] A delayed privileged action receives a current action-scoped grant; raw principals are not retained as live authority.
- [ ] An external-resource capability uses a restricted credential/client/broker/ACL.
- [ ] Interpreter authority is absent or controlled by an explicit unsafe contract.
- [ ] Present secrets do not enter ordinary projections/logs/metrics.
- [ ] Hostile code is placed in a process/sandbox if containment is claimed.
- [ ] `Isolated` is selected when a credential must be isolated from less-trusted co-resident code or a high-privilege credential requires its own security principal; an ordinary scoped external credential may remain `InProcess + Hardened` when the trusted in-process threat model makes no hostile-containment or separate-principal claim.
- [ ] Effective limits are enforced only for resources/exposures present at the boundary; absent network or file authority needs no row, while denial by construction is valid proof.

---

## 19. Anti-patterns

| Anti-pattern | Why it violates the model | Preferred construction |
|---|---|---|
| `UI -> Repository -> Network` as a business path | The decision and effect bypass the Nucleus | `Intent -> Decision -> Effect` |
| Global mutable store | Multiple implicit owners and a broad blast radius | Isolated state authorities |
| Cross-cell selector over multiple mutable stores | Hidden coordination authority | Read Model or Flow-owned operation |
| Universal mediator/event bus | Runtime discovery, wildcard coupling, hidden fan-out | Typed explicit routes |
| `Map<String, Any>` / string message names | No closed algebra or exhaustiveness | Tagged unions / sealed types |
| `ExecuteSql(String)` | Raw interpreter authority | `FindUserById(UserId)` + safe adapter |
| `FetchUrl(String)` | SSRF and arbitrary endpoint authority | Endpoint-scoped typed capability |
| `RunShell(String)` | Command injection and ambient OS authority | Structured OS operation; if raw shell is unavoidable, use the explicit unsafe escape-hatch contract in §11.10. Add a `WaiverRecord` only for a separate unresolved `MUST`/`MUST NOT` deviation; that deviation remains non-conforming in its exact scope. |
| Unbounded queue/retry/fan-out | Unpredictable memory/latency/cost | Declared finite limits |
| Retry in SDK + adapter + runtime + Flow for one failure mode | Multiplicative attempts and unclear ownership | One primary retry owner per failure mode; other layers disabled or bounded and proven semantically transparent |
| Timeout = failed action | The external action might have occurred | `OutcomeUnknown` + reconciliation |
| Cancel flag = physical stop | Cancellation may be too late | Cancellation protocol/state |
| Shared `common` domain model | Transitive coupling and foreign ownership | Local semantic types + opaque refs |
| Protocol re-export | An owner change propagates transitively | A dedicated consumer-facing contract |
| Feature calls the target Nucleus directly | Bypasses route, authority, and delivery semantics | Declared command/read contract |
| Flow for every one-hop call | Ceremony and coordination explosion | `DeclaredCommandDependency` |
| One God Flow for the entire system | Hot key, enormous state graph, change radius | Multiple workflow authorities |
| One Ball per DTO/table | Micromodularity without an invariant boundary | Boundary by authority/invariants/lifecycle |
| Repository interface only for a mock | Artificial abstraction without a real boundary | Pure transition tests + adapter contract tests |
| Service locator/global SDK client | Ambient authority and invisible dependency | Explicit scoped capability injection |
| Adapter "checks itself" while holding an admin client | No independent enforcement | Restricted credential/ACL/broker |
| Roll back state after an emitted external action | The consequence does not disappear | Forward recovery/reconciliation |
| Rebuild the outbox by rerunning the Nucleus | A new version may produce different outputs | Preserve the committed output ledger |
| One `Pending/Done/Failed` status for a distributed step | Does not express acceptance/cancel/unknown races | Orthogonal facets |
| Ownerless shared domain utility | Hides business meaning outside any Ball/Flow and bypasses the four semantic dependency kinds | Ball-local implementations, or one Ball/Flow owner exposed through a declared Application Surface/protocol |
| "Durable" without saying what is durable | Conflates source, target, executor, and reply guarantees | Qualified guarantee per boundary |

---
