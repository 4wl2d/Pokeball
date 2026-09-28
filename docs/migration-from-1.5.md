# Migrating from Pokeball Core 1.5 to Core 2.0

The [previous published Core 1.5.0-draft](https://github.com/4wl2d/Pokeball/tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e) remains in Git history. Core 2 replaces the project-defined binding with a smaller runtime contract and executable conformance checks. This guide maps the 1.5 vocabulary and each of its 44 laws to [Core 2.0](../spec/pokeball-core-2.0.md).

## What changes for a developer

1. **One decider replaces three roles.** The Interaction/Nucleus/Resource split becomes a pure *decider* (the old Nucleus) plus *adapters* at the edges (the old Interaction and Resource code). The runtime does what 1.5 left to each project's "binding": it serialises inputs, commits atomically, dispatches, tracks deadlines and classifies outcomes.
2. **Three inputs, four outputs.** The six Pulse kinds and seven SemanticOutput kinds collapse into Request, Completion and Notice (inputs) and Call, Reply, Publish and Cancel (outputs).
3. **Outcomes come from the port's effect class.** In 1.5, a law's trigger (for example "may have executed") decided whether `OutcomeUnknown` applied. In Core 2, the runtime chooses `Done`, `NotDone` or `Unknown` from the call's phase and the port's effect class (`Safe`, `Idempotent` or `NonIdempotent`). A Safe port never produces `Unknown`.
4. **No applicability triggers.** A Ball that makes no calls uses no asynchronous machinery, so there is nothing to declare absent.
5. **Fewer rules, each with a named check.** Core 2 has 13 rules, R1–R13 (§5 of the specification). Security, read-model and protocol-design material becomes guidance (§8).

## Vocabulary

| Core 1.5 | Core 2.0 |
|---|---|
| Ball, BallInstance | Ball type, Ball instance (`BallId` = type + key) |
| Nucleus (`decide`, pure reads) | decider: `decide`, `complete`, `observe` |
| Interaction | inbound adapter (outside the kernel) plus the request message type |
| Resource / route | adapter port (`AdapterPort`) and its adapter |
| Assembly, manifest | composition root (`Composition`) |
| Pulse: `Intent` | Request |
| Pulse: `ModuleCommandPulse` | Request from another Ball (a call on a `BallPort`) |
| Pulse: `ModuleResultPulse`, `Fact` (resource result) | Completion |
| Pulse: `ObservedSignal` | Notice |
| Pulse: `ControlPulse` (timer, cancellation, control) | Completion of a timer call; a cancellation request is an ordinary Request |
| `ReplyOutput` | the reply plan of `Accept`, or a later `Reply` output |
| `EffectRequest` | `Call` on an adapter port |
| `ModuleCommandRequest`, `ModuleResultOutput` | `Call` on a Ball port, and the target's reply |
| `SignalPublication` | `Publish` |
| `TimerRequest` | `Timer.after` (a call on the built-in timer port) |
| `ProjectionOutput` | none: views read committed state (R12) |
| `OutcomeUnknown` | `Outcome.Unknown(reason)` |
| `SemanticHandle` | call key (`CallKey`) and call identity (`CallId`) |
| `OperationStatus` materializer | a query answered from the owner's state (§8.5) |
| Flow | Flow: a Ball whose state is the workflow (§7.2) |
| Profiles (Inline, SnapshotOutbox, EventJournal, …) | Transient and Durable; event sourcing is an optional representation of Durable (§6) |
| `AdmissionFailure.reason` | `NotDoneReason`, a closed type in the kernel |

## Law-by-law disposition

*Kept* means the obligation is still normative, possibly reworded. *By construction* means the kernel API or the runtime makes a violation impossible or reports it, so no author rule is needed. *Guidance* means the text moved to non-normative §8. *Dropped* means Core 2 no longer makes the claim; the reason is given.

| 1.5 law | Disposition | Core 2 |
|---|---|---|
| PBA-01 Three-Zone Boundary | Replaced | Decider plus adapters (§2); R2, R10 |
| PBA-02 Polar Isolation | By construction | Adapters communicate only through the runtime (R10); module structure §7.1 |
| PBA-03 Pure Bounded Nucleus | Kept | R2; termination is checked by review; output bounds are in R11 |
| PBA-04 Closed Protocol | Guidance / by construction | Message types are the application's own (sealed types recommended); the kernel's outcome and reason types are closed |
| PBA-05 Explicit Decision Inputs | Kept | R2: state, input and context only |
| PBA-06 Controlled Causality | By construction | Outputs exist only in decisions; the runtime executes only committed outputs (R5) |
| PBA-07 Atomic Decision | Kept | R4 |
| PBA-08 Commit Before Dispatch | Kept | R5 |
| PBA-09 No Reentrant Transition | Kept | R3; no synchronous calls between Balls (§3.4) |
| PBA-10 Fault Atomicity | Kept | R4 (a fault commits nothing); quarantine (§3.5) |
| PBA-11 Single Semantic Authority | Kept | R1 |
| PBA-12 Single Writer | Kept | R1, R3; fencing of a Ball across machines is out of scope of the reference runtime (§10) |
| PBA-13 State Isolation | Kept | R10, R12 |
| PBA-14 Explicit State Kind | Dropped | One state per Ball. UI and transport state is outside the architecture: a value that can change a decision must be in a message or in state |
| PBA-15 Semantic Handle | By construction | Call keys from `Context.newKey`; `CallId` identifies a call globally |
| PBA-16 Trusted Identifier Allocation | Kept | R2 and deterministic `Context.newKey`; business identifiers come from messages or are derived from state |
| PBA-17 Revisioned Causality | By construction / guidance | Exactly one completion per call key (R6); stale results are handled by key (§8.3) |
| PBA-18 Provenance-Bound Result | By construction | A completion is delivered only to the instance that committed the call, under its key (R6) |
| PBA-19 ACK/Result Separation | Replaced | `Done` / `NotDone(reason)` / `Unknown(reason)`, whose reasons separate refusal, overload, fault, deadline and ambiguity (§2.2, §3.3) |
| PBA-20 First-Class Unknown | Kept | R7: `Unknown` only for effectful ports that may have sent; a Safe port ends `NotDone(NoAnswer)` |
| PBA-21 Explicit Idempotency | Kept | Effect classes; idempotent ingress by idempotency key with a message-equality check (`IdempotencyConflict`) (§3.3) |
| PBA-22 Stable Logical Retry | Kept | R8: retries keep the call identity; adapters see only the attempt number |
| PBA-23 Cancellation Is a Protocol | Kept | `Cancel` output; the completion still arrives and a cancellation never erases a result (§3.2) |
| PBA-24 Owned Retry Policy | Kept | R8: the runtime is the only retrier; adapters must not retry |
| PBA-25 Declared Dependency | Kept | R10: composition root, API modules, `checkModuleBoundaries` |
| PBA-26 Workflow Sovereignty | Kept (SHOULD) | Flow (§7.2) |
| PBA-27 No Wildcard Mediator | Kept | R10: no wildcard routing or handler discovery |
| PBA-28 No Protocol Re-export | Guidance | API modules own their types (§7.1); not a separate rule |
| PBA-29 Bounded Composition | Kept | R10: the composition root lists every Ball, binding and subscription |
| PBA-30 Honest Read Consistency | Kept / guidance | R12 read-only queries; consistency claims fall under §9.4 |
| PBA-31 Double Quarantine | Guidance | §8.2 and §8.6: validate at adapters and treat inputs as untrusted |
| PBA-32 Capability-Sealed Effect | Guidance | §8.6: adapters hold capabilities, deciders hold none |
| PBA-33 Dual Gate | Guidance | §8.2: the decider decides permissions; the adapter enforces technical authorization |
| PBA-34 No Ambient Authority | Kept | R2 (no globals in deciders; `noGlobalMutableState`), R10 |
| PBA-35 Safe Sink | Guidance | §8.6 |
| PBA-36 Secret Containment | Guidance | §8.6 |
| PBA-37 Explicit Unsafe Escape Hatch | Dropped | General secure-coding practice, not specific to this architecture |
| PBA-38 Bounded Execution | Kept | R11 and the bounds of §3.5 |
| PBA-39 Profile-Proportional Mechanism | Replaced | Proportionality by construction: a Ball that makes no calls needs no asynchronous machinery |
| PBA-40 Zero Mandatory Runtime Tax | Dropped | Core 2 is a runtime contract with a measured cost; see Core §10 |
| PBA-41 Measured Claim | Kept | §9.4 Claims |
| PBA-42 Honest Guarantee Scope | Kept | R13, §9.4 |
| PBA-43 Foundation Quarantine | By construction | Shared code holds no mutable state (`noGlobalMutableState`); features share only API modules (R10) |
| PBA-44 Trusted Actor Context | Kept (reduced) | `RequestContext.caller`, a principal authenticated by the inbound adapter (§2.3, §8.2) |

**Summary.** Of the 44 laws, 25 are kept or kept in reduced form, 6 are enforced by construction, 7 became guidance, 3 were replaced by a different mechanism, and 3 were dropped (PBA-14, PBA-37, PBA-40). The counts assign each law to its first-listed disposition.

## Porting a feature

1. Keep the old Nucleus logic as the body of `decide`. Replace pulse-kind branching with one `when` over the request type, plus `complete` for completions and `observe` for notices.
2. Replace each `EffectRequest` with a `Call` on an `AdapterPort`, and choose its effect class honestly. Move the transport code into an `Adapter` and check it with the adapter conformance kit.
3. Replace `ModuleCommandRequest` with a `Call` on the target's `BallPort`. Replace `SignalPublication` with `Publish` plus a subscription in the composition root.
4. Delete handle, status-materializer and trigger-absence code. Answer status requests from state, and keep a call's key in state when a completion must match it.
5. Handle each call's single completion in `complete`: `Done`, `NotDone` and, for effectful ports, `Unknown` (§8.4 describes reconciliation).
