# Core Reference Index

> **Status:** derived noncanonical package. It neither defines the architecture nor extends the Core. Before using it, pass the baseline gate in [BASELINE.md](BASELINE.md). If the two conflict, the canonical specification prevails.

All definitions and normative text are in the [canonical Core](../../spec/pokeball-architecture-core.md). This file is only an index. Index coverage does not imply per-Ball applicability: use Core §§0.2/20.1 and [AGENT-CONTRACT.md](AGENT-CONTRACT.md) to resolve `always`, path-, risk-, and claim-triggered rules.

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

This is a reference-only routing table. It does not define a runtime, require every concern in every Ball, or move ownership from the exact Core anchors. Determine applicability from Core before following a package route.

| # | Runtime concern | Exact Core anchors | Primary package route |
|---:|---|---|---|
| 1 | Cause and field-minimized context | §§3.3–3.4, 6.11, 8.1 | `DESIGN-RUNBOOK`; `ASYNC-STATUS-RUNBOOK` for retained cause |
| 2 | Finite semantic and runtime bounds | §§8.3–8.4, 10.9, 13.1–13.2; PBA-38 | `SECURITY-LIMITS-RUNBOOK`; `COMPOSITION-PROFILES` for graph ceilings |
| 3 | Semantic, causal, and mechanical identities | §§3.5–3.6, 9.1–9.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 4 | Preflight, reservation, and admission | §§8.4, 8.7, 13.2 | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK` |
| 5 | Atomic Decision acceptance | §§8.5, 8.9; PBA-07 | `DESIGN-RUNBOOK` |
| 6 | Commit-before-dispatch | §8.6; PBA-08 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 7 | Preservation of accepted work | §§8.4, 8.8, 9.13, 12.4–12.6 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK`, `COMPOSITION-PROFILES` |
| 8 | Results, ACKs, delivery, and trusted observations | §§6.5, 6.9–6.11, 9.3–9.5, 9.11–9.13 | `ASYNC-STATUS-RUNBOOK`; `SECURITY-LIMITS-RUNBOOK` at trust edges |
| 9 | Rejections, admission failures, and runtime faults | §§6.7, 6.13, 8.7–8.8, 13.2 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 10 | Persistence, recovery, and migration | §§8.9, 10.11, 12.5–12.6, 17.7 | `COMPOSITION-PROFILES`, `TEST-AND-REVIEW-GATES` |
| 11 | Local reads and operation-status reads | §§6.3, 8.10, 9.11; PBA-30 | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| 12 | Lifecycle, ownership, and fencing | §§7.6, 8.11, 12.3–12.6 | `DESIGN-RUNBOOK`, `COMPOSITION-PROFILES`, `TEST-AND-REVIEW-GATES` |

## PBA-01–44

| Law | Title | Agent rules |
|---|---|---|
| `PBA-01` | Three-Zone Boundary | `PKB-AR-BND-001/002` |
| `PBA-02` | Polar Isolation | `PKB-AR-BND-002` |
| `PBA-03` | Pure Bounded Nucleus | `PKB-AR-BND-002`, `PKB-AR-DEC-001` |
| `PBA-04` | Closed Protocol | `PKB-AR-PRT-001`, `PKB-AR-DEC-001`, `PKB-AR-MAN-001` |
| `PBA-05` | Explicit Decision Inputs | `PKB-AR-PRT-002`, `PKB-AR-STA-002`, `PKB-AR-ASY-005` |
| `PBA-06` | Controlled Causality | `PKB-AR-DEC-001` |
| `PBA-07` | Atomic Decision | `PKB-AR-DEC-002` |
| `PBA-08` | Commit Before Dispatch | `PKB-AR-DEC-002` |
| `PBA-09` | No Reentrant Transition | `PKB-AR-DEC-003` |
| `PBA-10` | Fault Atomicity | `PKB-AR-DEC-003`, `PKB-AR-ASY-005` |
| `PBA-11` | Single Semantic Authority | `PKB-AR-BND-001`, `PKB-AR-STA-001` |
| `PBA-12` | Single Writer | `PKB-AR-STA-001` |
| `PBA-13` | State Isolation | `PKB-AR-STA-001` |
| `PBA-14` | Explicit State Kind | `PKB-AR-STA-002` |
| `PBA-15` | Semantic Handle | `PKB-AR-ID-001` |
| `PBA-16` | Trusted Identifier Allocation | `PKB-AR-ID-001` |
| `PBA-17` | Revisioned Causality | `PKB-AR-ID-001`, `PKB-AR-ASY-001` |
| `PBA-18` | Provenance-Bound Result | `PKB-AR-ASY-001` |
| `PBA-19` | ACK/Result Separation | `PKB-AR-ASY-002/005` |
| `PBA-20` | First-Class Unknown | `PKB-AR-ASY-002` |
| `PBA-21` | Explicit Idempotency | `PKB-AR-ASY-003` |
| `PBA-22` | Stable Logical Retry | `PKB-AR-ASY-003` |
| `PBA-23` | Cancellation Is a Protocol | `PKB-AR-ASY-004` |
| `PBA-24` | Owned Retry Policy | `PKB-AR-ASY-003` |
| `PBA-25` | Declared Dependency | `PKB-AR-CMP-001`, `PKB-AR-MAN-001/002` |
| `PBA-26` | Workflow Sovereignty | `PKB-AR-STA-002`, `PKB-AR-CMP-002` |
| `PBA-27` | No Wildcard Mediator | `PKB-AR-CMP-002` |
| `PBA-28` | No Protocol Re-export | `PKB-AR-PRT-001`, `PKB-AR-MAN-002` |
| `PBA-29` | Bounded Composition | `PKB-AR-CMP-003`, `PKB-AR-LIM-002` |
| `PBA-30` | Honest Read Consistency | `PKB-AR-PRT-003`, `PKB-AR-CMP-003`, `PKB-AR-ASY-005` |
| `PBA-31` | Double Quarantine | `PKB-AR-SEC-001` |
| `PBA-32` | Capability-Sealed Effect | `PKB-AR-SEC-002` |
| `PBA-33` | Dual Gate | `PKB-AR-SEC-002` |
| `PBA-34` | No Ambient Authority | `PKB-AR-SEC-002` |
| `PBA-35` | Safe Sink | `PKB-AR-SEC-003` |
| `PBA-36` | Secret Containment | `PKB-AR-SEC-003` |
| `PBA-37` | Explicit Unsafe Escape Hatch | `PKB-AR-SEC-003` |
| `PBA-38` | Bounded Execution | `PKB-AR-LIM-001/002` |
| `PBA-39` | Profile-Proportional Mechanism | `PKB-AR-GOV-003`, `PKB-AR-PRF-001` |
| `PBA-40` | Zero Mandatory Runtime Tax | `PKB-AR-PRF-001` |
| `PBA-41` | Measured Claim | `PKB-AR-GOV-003`, `PKB-AR-PRF-002`, `PKB-AR-TST-001` |
| `PBA-42` | Honest Guarantee Scope | `PKB-AR-GOV-003`, `PKB-AR-PRF-002`, `PKB-AR-ASY-005` |
| `PBA-43` | Foundation Quarantine | `PKB-AR-CMP-004` |
| `PBA-44` | Trusted Actor Context | `PKB-AR-SEC-002` |

## Glossary index

Definitions are read only from Core §22.

| Term | Primary route |
|---|---|
| Applicability Trigger | `AGENT-CONTRACT`, `TRACEABILITY` |
| Assembly | `MANIFEST-AND-ASSEMBLY` |
| AuthenticatedActorContext | `SECURITY-LIMITS-RUNBOOK` |
| Ball | `DESIGN-RUNBOOK` |
| BallInstance | `DESIGN-RUNBOOK`, overlay |
| BallType | `DESIGN-RUNBOOK` |
| BusinessRejection | `DESIGN-RUNBOOK` |
| BoundaryResponse | `DESIGN-RUNBOOK` |
| Capability | `SECURITY-LIMITS-RUNBOOK` |
| Captured Input | `DESIGN-RUNBOOK`, `COMPOSITION-PROFILES` |
| Claim Record | `TEST-AND-REVIEW-GATES` |
| CausalToken | `ASYNC-STATUS-RUNBOOK` |
| CommitRevision | `DESIGN-RUNBOOK` |
| CommitId | `DESIGN-RUNBOOK` |
| Commit-before-dispatch | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| CommittedStateSnapshot | `DESIGN-RUNBOOK` |
| ConsistencyStamp | `ASYNC-STATUS-RUNBOOK` |
| Decision | `DESIGN-RUNBOOK` |
| DecisionContext | `DESIGN-RUNBOOK` |
| ControlPulse | `ASYNC-STATUS-RUNBOOK` |
| DeclaredCommandDependency | `COMPOSITION-PROFILES` |
| DeclaredSignalDependency | `COMPOSITION-PROFILES` |
| Direct Control Dependency | `COMPOSITION-PROFILES` |
| Delivery Observation | `ASYNC-STATUS-RUNBOOK` |
| DispatchStopped | `ASYNC-STATUS-RUNBOOK` |
| Effective Guardrail | `AGENT-CONTRACT`, `MANIFEST-AND-ASSEMBLY` |
| Effect | `DESIGN-RUNBOOK` |
| EffectRequest | `DESIGN-RUNBOOK` |
| EventJournal | `COMPOSITION-PROFILES` |
| Execution Gate | `SECURITY-LIMITS-RUNBOOK` |
| Fact | `ASYNC-STATUS-RUNBOOK` |
| Field-Minimized | `DESIGN-RUNBOOK`, `ASYNC-STATUS-RUNBOOK` |
| Feature Ball | `DESIGN-RUNBOOK` |
| Flow Ball | `COMPOSITION-PROFILES` |
| Foundation Quarantine | `COMPOSITION-PROFILES` |
| Grant | `SECURITY-LIMITS-RUNBOOK` |
| Guardrail Policy Reference | `PROJECT-OVERLAY.template`, `MANIFEST-AND-ASSEMBLY` |
| Interaction Hemisphere | `DESIGN-RUNBOOK` |
| Intent | `DESIGN-RUNBOOK` |
| ModuleCommand | `MANIFEST-AND-ASSEMBLY` |
| ModuleCommandRequest | `MANIFEST-AND-ASSEMBLY` |
| ModuleResult | `ASYNC-STATUS-RUNBOOK` |
| Nucleus | `DESIGN-RUNBOOK` |
| OperationId | `ASYNC-STATUS-RUNBOOK` |
| Operation Status Authority | `ASYNC-STATUS-RUNBOOK`, overlay |
| AttemptId | `ASYNC-STATUS-RUNBOOK` |
| ObservedSignal | `COMPOSITION-PROFILES` |
| OutcomeUnknown | `ASYNC-STATUS-RUNBOOK` |
| OutputId | `DESIGN-RUNBOOK` |
| Policy Gate | `SECURITY-LIMITS-RUNBOOK` |
| Projection | `DESIGN-RUNBOOK` |
| ProjectionOutput | `DESIGN-RUNBOOK` |
| Pulse | `DESIGN-RUNBOOK` |
| Query | `DESIGN-RUNBOOK`, `MANIFEST-AND-ASSEMBLY` |
| RequestId | `ASYNC-STATUS-RUNBOOK` |
| ReadContext | `DESIGN-RUNBOOK` |
| ReadDependency | `COMPOSITION-PROFILES` |
| ReadResult | `DESIGN-RUNBOOK` |
| Read Model Ball | `COMPOSITION-PROFILES` |
| Reply | `DESIGN-RUNBOOK` |
| ReplyOutput | `DESIGN-RUNBOOK` |
| Replica State | `DESIGN-RUNBOOK` |
| RetainedContinuation | `TEST-AND-REVIEW-GATES` |
| Resource Hemisphere | `DESIGN-RUNBOOK`, `SECURITY-LIMITS-RUNBOOK` |
| Safe Sink | `SECURITY-LIMITS-RUNBOOK` |
| SemanticHandle | `ASYNC-STATUS-RUNBOOK` |
| SemanticOutput | `DESIGN-RUNBOOK` |
| Signal | `COMPOSITION-PROFILES` |
| SignalPublication | `COMPOSITION-PROFILES` |
| SnapshotOutbox | `COMPOSITION-PROFILES`, `TEST-AND-REVIEW-GATES` |
| StorageTransactionId | `DESIGN-RUNBOOK` |
| Sovereign State | `DESIGN-RUNBOOK` |
| State Belt | `DESIGN-RUNBOOK` |
| StateKey | `DESIGN-RUNBOOK`, overlay |
| Structural Allocation | `COMPOSITION-PROFILES` |
| TimerRequest | `ASYNC-STATUS-RUNBOOK` |
| Trusted Boundary | `SECURITY-LIMITS-RUNBOOK`, `DESIGN-RUNBOOK` |
| Workflow Sovereignty | `COMPOSITION-PROFILES` |
| Zero Mandatory Runtime Tax | `COMPOSITION-PROFILES` |
