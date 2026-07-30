# Composition Profiles and Claims

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation only after the composition graph is known and an effective profile, explicit claim, shared Foundation classification, or composition change report is needed.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Composition and Profiles](COMPOSITION-PROFILES.md) · [Agent Pack index](README.md)

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
