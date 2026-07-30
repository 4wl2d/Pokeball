# Core Reference Index

> **Status:** derived noncanonical package. It neither defines the architecture nor extends the Core. Before using it, pass the baseline gate in [BASELINE.md](BASELINE.md). If the two conflict, the canonical specification prevails.

All definitions and normative text are in the ordered [canonical Core document set](../../spec/pokeball-architecture-core.md); its entrypoint owns the complete path inventory and reading order. This file is only an index. Each `Primary source clause` cell routes to the sole Core source record for that law. Section 20 is its complete field-for-field audit projection; §20.1 is only the limited applicability, ownership, source, and test-route index. Projection checks compare the full §20 entry and affected package rules with the complete source record, while §20.1 is checked only for its declared columns. A full semantic audit reads every unique authoritative source and applicable test/checklist route once, then mechanically checks generated §§20/22 alternatives for equality instead of semantically rereading them. Ordinary task routes are smaller; neither route claims that exhaustive review of unique Core content is trivial. Index coverage does not imply per-Ball applicability: use Core §§0.2/20.1 and [AGENT-CONTRACT.md](AGENT-CONTRACT.md) to resolve `always`, path-, risk-, and claim-triggered rules.

## Sections 0–23

| § | Title | Primary package route |
|---:|---|---|
| 0 | Document status and scope | `BASELINE`, `AGENT-CONTRACT` applicability/precedence |
| 1 | Definition of Pokeball | `DESIGN-RUNBOOK` |
| 2 | Goals and non-goals | `AGENT-CONTRACT`, sparse-design route |
| 3 | Canonical model | `DESIGN-RUNBOOK` |
| 4 | Choosing a Ball boundary | `DESIGN-RUNBOOK` |
| 5 | Three logical zones | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK` |
| 6 | Protocol algebra | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 7 | State and authority | `DESIGN-RUNBOOK` |
| 8 | Decision and commit semantics | `DESIGN-RUNBOOK`, effective-policy resolution; §8.12 routes below |
| 9 | Asynchrony, causality, and delivery semantics | `ASYNC-STATUS-RUNBOOK` |
| 10 | System composition | `COMPOSITION-PROFILES` |
| 11 | Security and privacy | `SECURITY-LIMITS-RUNBOOK` |
| 12 | Execution profiles | `COMPOSITION-PROFILES` |
| 13 | Limits, budgets, performance, and design-time tax | `SECURITY-LIMITS-RUNBOOK`, claim gates |
| 14 | Minimal manifest and source-code organization | `MANIFEST-AND-ASSEMBLY`, project policy/delta |
| 15 | End-to-end example I: catalog search | `EXAMPLE-CROSSWALK` |
| 16 | End-to-end example II: Checkout Flow | `EXAMPLE-CROSSWALK`, `ASYNC-STATUS-RUNBOOK` |
| 17 | Testing, review, and operational verification | routine/trigger/claim suites in `TEST-AND-REVIEW-GATES` |
| 18 | Practical checklist | applicability route in `TEST-AND-REVIEW-GATES` |
| 19 | Anti-patterns | All runbooks as a negative review list |
| 20 | Canonical Pokeball laws and applicability matrix | This index, `AGENT-CONTRACT`, `TRACEABILITY` |
| 21 | Adoption strategy | `INSTALL`, sparse `DESIGN-RUNBOOK` |
| 22 | Glossary | This index |
| 23 | Canonical statement | `AGENT-CONTRACT` overview |

## Core §8.12 runtime concern index

This is a reference-only routing table. Core defines `Runtime / acceptor` once as the mechanical binding role for applicable validation handoff, admission/reservation, selected-frame publication, retained-output scheduling, and declared routing of verified mechanical observations. It owns no business schema, policy, permission, read-result selection, retry/fallback choice, or direct Sovereign-State write outside accepted snapshot/event publication and a later serialized `decide`. This index does not add a runtime, require every concern in every Ball, or move ownership from the exact Core anchors. Determine applicability from Core before following a package route.

| # | Runtime concern | Exact Core anchors | Primary package route |
|---:|---|---|---|
| 1 | Cause and field-minimized context | §§3.3–3.4, 6.11, 8.1 | `DESIGN-RUNBOOK`; `SECURITY-LIMITS-RUNBOOK` for trusted Decision/read context; `ASYNC-STATUS-RUNBOOK` for retained per-Pulse cause/context association |
| 2 | Finite semantic and runtime bounds | §§8.3–8.4, 10.9, 13.1–13.2; PBA-38 | `SECURITY-LIMITS-RUNBOOK` for the Decision Work Meter when triggered; `COMPOSITION-PROFILES` for graph ceilings; `ASYNC-STATUS-RUNBOOK` for capacity-only admission, alternative-completion, and status reservations |
| 3 | Semantic, causal, and mechanical identities | §§3.5–3.6, 9.1–9.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`; `MANIFEST-AND-ASSEMBLY` for effective protocol identity |
| 4 | Preflight, reservation, and admission | §§8.4, 8.7, 13.2 | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK`; `ASYNC-STATUS-RUNBOOK` for level-1 alternative-completion transfer, level-2 result, causal, and status reservations |
| 5 | Atomic Decision acceptance | §§8.5, 8.9; PBA-07 | `DESIGN-RUNBOOK`; flattened `AcceptedSnapshotDecisionFrame` versus `AcceptedEventCommit`, with no Event `nextState` |
| 6 | Commit-before-dispatch | §8.6; PBA-08 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, including accepted target result output |
| 7 | Preservation of accepted work | §§8.4, 8.8, 9.13, 12.4–12.6 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, `COMPOSITION-PROFILES`; target result stop never rewrites source outcome |
| 8 | Command ingress, accepted result return, and pre-acceptance refusal | §§6.8–6.13, 8.4–8.9, 9.1–9.4, 10.2/10.7/10.11; PBA-18/PBA-19 | `ASYNC-STATUS-RUNBOOK`, `MANIFEST-AND-ASSEMBLY`, `DESIGN-RUNBOOK`; exact bridge, refusal class, result tuple, command-vs-read, level-1 alternative-completion transfer, level-2 result reservation, and async budget preservation |
| 9 | Results, ACKs, delivery, and trusted observations | §§6.5, 6.9–6.11, 9.3–9.5, 9.11–9.13 | `ASYNC-STATUS-RUNBOOK`; `SECURITY-LIMITS-RUNBOOK` at trust edges |
| 10 | Rejections, admission failures, and runtime faults | §§6.7, 6.13, 8.7–8.8, 13.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`; exact carrier/result/Resource/stop/programming-fault stage mapping |
| 11 | Persistence, recovery, and migration | §§8.9, 10.11, 12.5–12.6, 17.7 | `COMPOSITION-PROFILES`, `TEST-AND-REVIEW-GATES` |
| 12 | Local reads and operation-status reads | §§6.3, 8.10, 9.11; PBA-30 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, `COMPOSITION-PROFILES`, `MANIFEST-AND-ASSEMBLY`; command-vs-read selection, complete `ReadDependency`, and total target-owned post-admission payload |
| 13 | Lifecycle, ownership, and fencing | §§7.6, 8.11, 12.3–12.6 | `DESIGN-RUNBOOK`, `COMPOSITION-PROFILES`, `ASYNC-STATUS-RUNBOOK`, `TEST-AND-REVIEW-GATES`; `Draining` read availability and one status authority |

## PBA-01–44

<!-- pkb:generated:start id="agent-pba-index" -->
| Law | Title | Core source | Law projection | Navigation index | Core test | Agent rules |
|---|---|---|---|---|---|---|
| `PBA-01` | Three-Zone Boundary | §5 | §20 PBA-01 | §20.1 PBA-01 | `§17.5` | `PKB-AR-BND-001`, `PKB-AR-BND-002`, `PKB-AR-CMP-004` |
| `PBA-02` | Polar Isolation | §5.4 | §20 PBA-02 | §20.1 PBA-02 | `§17.5` | `PKB-AR-BND-002`, `PKB-AR-CMP-004` |
| `PBA-03` | Pure Bounded Nucleus | §5.2 | §20 PBA-03 | §20.1 PBA-03 | `§17.1` | `PKB-AR-BND-002`, `PKB-AR-PRT-002`, `PKB-AR-DEC-001`, `PKB-AR-CMP-004`, `PKB-AR-SEC-002` |
| `PBA-04` | Closed Protocol | §6.14 | §20 PBA-04 | §20.1 PBA-04 | `§17.1` | `PKB-AR-PRT-001`, `PKB-AR-PRT-002`, `PKB-AR-DEC-001`, `PKB-AR-DEC-003`, `PKB-AR-SEC-001`, `PKB-AR-MAN-001` |
| `PBA-05` | Explicit Decision Inputs | §8.1 | §20 PBA-05 | §20.1 PBA-05 | `§17.1` | `PKB-AR-PRT-002`, `PKB-AR-STA-002`, `PKB-AR-ASY-005`, `PKB-AR-SEC-002` |
| `PBA-06` | Controlled Causality | §5.2 | §20 PBA-06 | §20.1 PBA-06 | `§17.1` | `PKB-AR-DEC-001`, `PKB-AR-CMP-001` |
| `PBA-07` | Atomic Decision | §8.5 | §20 PBA-07 | §20.1 PBA-07 | `§17.1` | `PKB-AR-DEC-002` |
| `PBA-08` | Commit Before Dispatch | §8.6 | §20 PBA-08 | §20.1 PBA-08 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-002` |
| `PBA-09` | No Reentrant Transition | §8.4 | §20 PBA-09 | §20.1 PBA-09 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-003`, `PKB-AR-CMP-003`, `PKB-AR-LIM-002` |
| `PBA-10` | Fault Atomicity | §8.8 | §20 PBA-10 | §20.1 PBA-10 | `§17.1` | `PKB-AR-DEC-003`, `PKB-AR-ASY-005` |
| `PBA-11` | Single Semantic Authority | §7.2 | §20 PBA-11 | §20.1 PBA-11 | `§17.5` | `PKB-AR-BND-001`, `PKB-AR-STA-001` |
| `PBA-12` | Single Writer | §7.6 | §20 PBA-12 | §20.1 PBA-12 | `§17.6` | `PKB-AR-STA-001` |
| `PBA-13` | State Isolation | §7.4 | §20 PBA-13 | §20.1 PBA-13 | `§17.5` | `PKB-AR-STA-001` |
| `PBA-14` | Explicit State Kind | §7.1 | §20 PBA-14 | §20.1 PBA-14 | `§17.1` | `PKB-AR-STA-002` |
| `PBA-15` | Semantic Handle | §3.5 | §20 PBA-15 | §20.1 PBA-15 | `§17.1` | `PKB-AR-ID-001` |
| `PBA-16` | Trusted Identifier Allocation | §3.5 | §20 PBA-16 | §20.1 PBA-16 | `§17.4` | `PKB-AR-PRT-003`, `PKB-AR-ID-001`, `PKB-AR-ASY-005` |
| `PBA-17` | Revisioned Causality | §9.2 | §20 PBA-17 | §20.1 PBA-17 | `§17.1` | `PKB-AR-ID-001`, `PKB-AR-ASY-005` |
| `PBA-18` | Provenance-Bound Result | §9.1 | §20 PBA-18 | §20.1 PBA-18 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-ASY-001` |
| `PBA-19` | ACK/Result Separation | §9.4 | §20 PBA-19 | §20.1 PBA-19 | `§17.1` | `PKB-AR-PRT-004`, `PKB-AR-DEC-003`, `PKB-AR-ASY-002` |
| `PBA-20` | First-Class Unknown | §9.5 | §20 PBA-20 | §20.1 PBA-20 | `§17.3` | `PKB-AR-DEC-003`, `PKB-AR-ASY-002` |
| `PBA-21` | Explicit Idempotency | §9.6 | §20 PBA-21 | §20.1 PBA-21 | `§17.1` | `PKB-AR-ASY-003` |
| `PBA-22` | Stable Logical Retry | §9.6 | §20 PBA-22 | §20.1 PBA-22 | `§17.3` | `PKB-AR-ASY-003` |
| `PBA-23` | Cancellation Is a Protocol | §9.7 | §20 PBA-23 | §20.1 PBA-23 | `§17.1` | `PKB-AR-ASY-004` |
| `PBA-24` | Owned Retry Policy | §9.9 | §20 PBA-24 | §20.1 PBA-24 | `§17.3` | `PKB-AR-ASY-003` |
| `PBA-25` | Declared Dependency | §10.2 | §20 PBA-25 | §20.1 PBA-25 | `§17.5` | `PKB-AR-PRT-001`, `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-CMP-001`, `PKB-AR-CMP-002`, `PKB-AR-CMP-004`, `PKB-AR-MAN-001`, `PKB-AR-MAN-002` |
| `PBA-26` | Workflow Sovereignty | §10.4 | §20 PBA-26 | §20.1 PBA-26 | `§17.5` | `PKB-AR-STA-002`, `PKB-AR-CMP-002` |
| `PBA-27` | No Wildcard Mediator | §10.4 | §20 PBA-27 | §20.1 PBA-27 | `§17.5` | `PKB-AR-CMP-002` |
| `PBA-28` | No Protocol Re-export | §10.8 | §20 PBA-28 | §20.1 PBA-28 | `§17.5` | `PKB-AR-PRT-001`, `PKB-AR-CMP-001`, `PKB-AR-MAN-002` |
| `PBA-29` | Bounded Composition | §10.9 | §20 PBA-29 | §20.1 PBA-29 | `§17.5` | `PKB-AR-CMP-003`, `PKB-AR-LIM-002`, `PKB-AR-MAN-001` |
| `PBA-30` | Honest Read Consistency | §8.10 | §20 PBA-30 | §20.1 PBA-30 | `§17.1` | `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-ASY-005`, `PKB-AR-CMP-003`, `PKB-AR-SEC-002` |
| `PBA-31` | Double Quarantine | §11.1 | §20 PBA-31 | §20.1 PBA-31 | `§17.4` | `PKB-AR-SEC-001` |
| `PBA-32` | Capability-Sealed Effect | §11.4 | §20 PBA-32 | §20.1 PBA-32 | `§17.3` | `PKB-AR-SEC-002` |
| `PBA-33` | Dual Gate | §11.3 | §20 PBA-33 | §20.1 PBA-33 | `§17.8` | `PKB-AR-PRT-002`, `PKB-AR-SEC-002` |
| `PBA-34` | No Ambient Authority | §11.5 | §20 PBA-34 | §20.1 PBA-34 | `§17.5` | `PKB-AR-CMP-004`, `PKB-AR-SEC-002` |
| `PBA-35` | Safe Sink | §11.6 | §20 PBA-35 | §20.1 PBA-35 | `§17.3` | `PKB-AR-SEC-003` |
| `PBA-36` | Secret Containment | §11.9 | §20 PBA-36 | §20.1 PBA-36 | `§17.8` | `PKB-AR-SEC-003` |
| `PBA-37` | Explicit Unsafe Escape Hatch | §11.10 | §20 PBA-37 | §20.1 PBA-37 | `§17.8` | `PKB-AR-SEC-003` |
| `PBA-38` | Bounded Execution | §8.3 | §20 PBA-38 | §20.1 PBA-38 | `§17.1` | `PKB-AR-PRT-003`, `PKB-AR-PRT-004`, `PKB-AR-DEC-001`, `PKB-AR-ASY-005`, `PKB-AR-CMP-003`, `PKB-AR-LIM-001`, `PKB-AR-LIM-002`, `PKB-AR-MAN-001` |
| `PBA-39` | Profile-Proportional Mechanism | §0.2 | §20 PBA-39 | §20.1 PBA-39 | `§17.5` | `PKB-AR-GOV-003`, `PKB-AR-GOV-005`, `PKB-AR-PRT-003`, `PKB-AR-PRF-001`, `PKB-AR-PRF-002`, `PKB-AR-LIM-001`, `PKB-AR-MAN-001`, `PKB-AR-TST-001` |
| `PBA-40` | Zero Mandatory Runtime Tax | §13.3 | §20 PBA-40 | §20.1 PBA-40 | `§17.5` | `PKB-AR-PRF-001` |
| `PBA-41` | Measured Claim | §13.4 | §20 PBA-41 | §20.1 PBA-41 | `§17.9` | `PKB-AR-GOV-003`, `PKB-AR-PRF-002`, `PKB-AR-TST-001` |
| `PBA-42` | Honest Guarantee Scope | §9.13 | §20 PBA-42 | §20.1 PBA-42 | `§17.7` | `PKB-AR-GOV-003`, `PKB-AR-ASY-005`, `PKB-AR-PRF-002` |
| `PBA-43` | Foundation Quarantine | §14.7 | §20 PBA-43 | §20.1 PBA-43 | `§17.5` | `PKB-AR-CMP-004`, `PKB-AR-MAN-001` |
| `PBA-44` | Trusted Actor Context | §11.2 | §20 PBA-44 | §20.1 PBA-44 | `§17.8` | `PKB-AR-PRT-002`, `PKB-AR-SEC-002` |
<!-- pkb:generated:end -->

## Glossary index

Definitions are read only from Core §22.

<!-- pkb:generated:start id="agent-glossary-index" -->
| Term | Core definition source | Glossary projection |
|---|---|---|
| AdmissionFailure | §6 | §22 `AdmissionFailure` |
| Applicability Trigger | §0 | §22 `Applicability Trigger` |
| Application Surface | §10 | §22 `Application Surface` |
| Assembly | §10 | §22 `Assembly` |
| AttemptId | §3 | §22 `AttemptId` |
| AuthenticatedActorContext | §11 | §22 `AuthenticatedActorContext` |
| Ball | §1 | §22 `Ball` |
| BallInstance | §1 | §22 `BallInstance` |
| BallType | §1 | §22 `BallType` |
| BoundaryResponse | §6 | §22 `BoundaryResponse` |
| BusinessRejection | §6 | §22 `BusinessRejection` |
| Capability | §11 | §22 `Capability` |
| Captured Input | §7 | §22 `Captured Input` |
| CausalToken | §9 | §22 `CausalToken` |
| Claim Record | §13 | §22 `Claim Record` |
| CommandRejectedBeforeAcceptance | §6 | §22 `CommandRejectedBeforeAcceptance` |
| Commit-before-dispatch | §8 | §22 `Commit-before-dispatch` |
| CommitId | §3 | §22 `CommitId` |
| CommitRevision | §3 | §22 `CommitRevision` |
| CommittedStateSnapshot | §6 | §22 `CommittedStateSnapshot` |
| ConsistencyStamp | §6 | §22 `ConsistencyStamp` |
| ControlPulse | §6 | §22 `ControlPulse` |
| Decision | §6 | §22 `Decision` |
| Decision Work Meter | §8 | §22 `Decision Work Meter` |
| DecisionContext | §8 | §22 `DecisionContext` |
| DeclaredCommandDependency | §10 | §22 `DeclaredCommandDependency` |
| DeclaredSignalDependency | §10 | §22 `DeclaredSignalDependency` |
| Delivery Observation | §9 | §22 `Delivery Observation` |
| Direct Control Dependency | §10 | §22 `Direct Control Dependency` |
| DispatchStopped | §9 | §22 `DispatchStopped` |
| Draining | §8 | §22 `Draining` |
| Effect | §6 | §22 `Effect` |
| Effective Guardrail | §0 | §22 `Effective Guardrail` |
| Effective Protocol Identity | §10 | §22 `Effective Protocol Identity` |
| EffectRequest | §6 | §22 `EffectRequest` |
| EphemeralState | §7 | §22 `EphemeralState` |
| EventJournal | §12 | §22 `EventJournal` |
| Execution Gate | §11 | §22 `Execution Gate` |
| Fact | §6 | §22 `Fact` |
| Feature Ball | §4 | §22 `Feature Ball` |
| Field-Minimized | §7 | §22 `Field-Minimized` |
| Flow Ball | §4 | §22 `Flow Ball` |
| FlowParticipation | §10 | §22 `FlowParticipation` |
| Foundation Quarantine | §14 | §22 `Foundation Quarantine` |
| Grant | §11 | §22 `Grant` |
| Guardrail Policy Reference | §0 | §22 `Guardrail Policy Reference` |
| Intent | §6 | §22 `Intent` |
| Interaction Hemisphere | §5 | §22 `Interaction Hemisphere` |
| Material Coordination | §4 | §22 `Material Coordination` |
| ModuleCommand | §6 | §22 `ModuleCommand` |
| ModuleCommandPulse | §6 | §22 `ModuleCommandPulse` |
| ModuleCommandRequest | §6 | §22 `ModuleCommandRequest` |
| ModuleResult | §6 | §22 `ModuleResult` |
| ModuleResultOutput | §6 | §22 `ModuleResultOutput` |
| ModuleResultPulse | §6 | §22 `ModuleResultPulse` |
| Nucleus | §5 | §22 `Nucleus` |
| ObservedSignal | §6 | §22 `ObservedSignal` |
| Operation Status Authority | §9 | §22 `Operation Status Authority` |
| OperationId | §3 | §22 `OperationId` |
| OutcomeUnknown | §9 | §22 `OutcomeUnknown` |
| OutputId | §3 | §22 `OutputId` |
| Policy Gate | §11 | §22 `Policy Gate` |
| Projection | §6 | §22 `Projection` |
| ProjectionOutput | §6 | §22 `ProjectionOutput` |
| Pulse | §6 | §22 `Pulse` |
| Query | §6 | §22 `Query` |
| Read Model Ball | §4 | §22 `Read Model Ball` |
| ReadContext | §6 | §22 `ReadContext` |
| ReadDependency | §10 | §22 `ReadDependency` |
| ReadResult | §6 | §22 `ReadResult` |
| Replica State | §7 | §22 `Replica State` |
| Reply | §6 | §22 `Reply` |
| ReplyOutput | §6 | §22 `ReplyOutput` |
| Representation Erasure | §5 | §22 `Representation Erasure` |
| RequestId | §3 | §22 `RequestId` |
| Resource Hemisphere | §5 | §22 `Resource Hemisphere` |
| RetainedContinuation | §8 | §22 `RetainedContinuation` |
| Safe Sink | §11 | §22 `Safe Sink` |
| SemanticHandle | §3 | §22 `SemanticHandle` |
| SemanticOutput | §6 | §22 `SemanticOutput` |
| Set-Equal | §0 | §22 `Set-Equal` |
| Signal | §6 | §22 `Signal` |
| SignalPublication | §6 | §22 `SignalPublication` |
| SnapshotOutbox | §12 | §22 `SnapshotOutbox` |
| Sovereign State | §7 | §22 `Sovereign State` |
| State Belt | §7 | §22 `State Belt` |
| StateKey | §7 | §22 `StateKey` |
| StorageTransactionId | §3 | §22 `StorageTransactionId` |
| Structural Allocation | §13 | §22 `Structural Allocation` |
| TimerRequest | §6 | §22 `TimerRequest` |
| TriggerAbsenceProof | §0 | §22 `TriggerAbsenceProof` |
| Trusted Boundary | §11 | §22 `Trusted Boundary` |
| Workflow Sovereignty | §10 | §22 `Workflow Sovereignty` |
| Zero Mandatory Runtime Tax | §13 | §22 `Zero Mandatory Runtime Tax` |
<!-- pkb:generated:end -->
