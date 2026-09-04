# Is Pokeball worth using here?

> [!NOTE]
> This is non-normative adoption guidance. The ordered [Core document set](../spec/pokeball-architecture-core.md) is the sole normative authority. This page proposes a project comparison; it adds no certification procedure or universal release checklist.

[← Overview](../README.md) · [First implementation](QUICKSTART.md) · [Adoption](ADOPTION.md)

Pokeball is useful when a shared contract for state ownership, decisions, effects, and failure handling saves more work than its implementation and learning cost. Evaluate that proposition on one real slice. A project that already handles these concerns consistently may gain little from adopting another architectural vocabulary.

## What is different from good Clean Architecture?

Clean Architecture already separates business policy from UI, storage, and frameworks through inward dependencies; isolated tests and replaceable adapters are shared benefits. Its original description permits functions and data structures and does not prescribe a class for every operation. See Robert C. Martin's [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html), accessed 2026-09-05.

Pure logic with external I/O performed around it is also established by Gary Bernhardt's [Functional Core, Imperative Shell](https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell), accessed 2026-09-05. Purity is not a Pokeball invention.

The comparison below is an interpretation of those sources and Core. A competent Clean implementation can incorporate the same mechanisms. Pokeball's additional specificity is their shared definition, applicability, ownership, and verification routes.

| Concern | Pokeball makes explicit | Candidate benefit | Cost to count |
|---|---|---|---|
| Mutable state | One semantic authority and one logical writer per Ball scope; replicas remain replicas. | Fewer competing writers and ownership disputes. | Boundary design; serialization and contention where applicable. |
| Decisions and effects | Pure bounded decisions; state and present outputs accepted together; dispatch follows acceptance. | Testable ordering between recorded cause and external consequence. | Acceptance implementation and fault tests. |
| Asynchronous work | Triggered causal identity, result provenance, retries, cancellation, and unknown outcomes. | Consistent handling of late, repeated, or ambiguous results. | Protocol variants, correlation state, and failure tests. |
| Operational claims | Finite effective limits and evidence for the exact claimed scope. | Assumptions and unsupported guarantees become inspectable. | Limit selection, measurements, and evidence maintenance. |

These are mechanisms and candidate benefits, not measured productivity or reliability improvements. The corresponding sources are [state authority](../spec/core/07-state-and-authority.md#72-one-mutable-factone-authority), [single writer](../spec/core/07-state-and-authority.md#76-single-writer), [acceptance](../spec/core/08-decision-and-acceptance.md#85-atomic-decision-acceptance), [asynchrony](../spec/core/09-asynchrony-and-delivery.md#9-asynchrony-causality-and-delivery-semantics), and [claim scope](../spec/core/12-profiles-and-limits.md#134-claim-contract).

## Run a comparison that can say no

Choose a stateful vertical slice with an observable outcome and a manageable external interaction. Use the [first implementation guide](QUICKSTART.md) to establish the implementation shape. Follow [Core's pilot route](../spec/core/reference/21-adoption.md#211-start-with-one-vertical-slice) for its applicable rules.

1. **Freeze the comparison.** Record the same feature requirements, guarantees, failure assumptions, workload, toolchain, and acceptance tests for both implementations. Use the project's competent existing approach as the baseline. Include any protection already supplied by its libraries or runtime.
2. **Choose success before measuring.** The project selects required improvements and acceptable costs. Examples are less maintenance time, fewer ownership mistakes, or simpler recovery within a latency budget. Core supplies no universal numeric threshold.
3. **Implement and change both.** Include initial setup and subsequent changes. Report shared binding/policy work separately, then include it in total cost; amortize it only over an explicit number of real covered features. Distinguish handwritten domain code, handwritten support code, generated code, tests, and documentation.
4. **Test a human's first change.** Give a developer unfamiliar with Pokeball the starting guide, working slice, and a request to change one business rule and add one input. Allow documentation, without an AI agent or an expert supplying the implementation. Record time to passing tests, lookups, assistance, and wrong-owner edits. Use comparable tasks and experience for the baseline; report task-order and learning effects.
5. **Run the same relevant failure scenarios.** Select the rows below that the slice actually exercises; do not add durability to a local slice to populate a table.

| Slice | Example shared acceptance scenarios |
|---|---|
| Local state | A valid change preserves the invariant; a rejected change leaves state unchanged; the same inputs produce the same decision; applicable limits reject excess work before acceptance. |
| Asynchronous effect | A late result for an older operation follows the declared stale-result policy; duplicates do not repeat the logical action; timeout and cancellation races follow the declared outcome policy; failed admission publishes neither state nor outputs. |
| Durable work | Inject crashes before acceptance, after durable acceptance but before dispatch, and after the external action but before its result is recorded. Check the selected contract's recovered state, retained outputs, retry identity, and reconciliation behavior. |

Durable source acceptance alone does not establish target receipt or business success. A timeout after an external action may require an unknown outcome and reconciliation. Expected results come from the chosen binding and project protocol; see [live and durable outputs](../spec/core/09-asynchrony-and-delivery.md#913-live-and-durable-outputs) and [recovery handoffs](../spec/core/reference/21-adoption.md#216-documentation-evolution).

Record measurements with their method and scope:

| Question | Useful observation |
|---|---|
| Can people maintain it? | Time to passing changes, expert interventions, documentation lookups, review time, and rework. |
| Is ceremony paying for itself? | Support artifacts, repeated declarations, shared setup hours, and policy/evidence upkeep. |
| Are changes more contained? | Files and public contracts changed for the same business-rule, input, and adapter changes. |
| Does it handle required failures? | Shared scenarios passed, failures found, and implementation/debugging effort. |
| Does it fit the workload? | Applicable latency, allocations, retained state, contention, and queue occupancy on identical workloads. |

Continue when the selected benefit meets the agreed budget. Reshape when a boundary or repeated declaration dominates cost. Stop when the baseline satisfies the requirements more simply or the human task still depends on expert intervention beyond the project's budget. Preserve an unfavorable result; renaming components is not an improvement.

## Decide production use for a named scope

Use this table to assign the evidence activated by the selected slice and claims. It does not require every row in every project or replace the controlling Core sources.

| Owner | Decision and relevant evidence | Stop condition |
|---|---|---|
| Feature owner | State/protocol ownership, invariants, pure transition tests, and the human maintenance exercise for this feature. | Required behavior fails, ownership is unresolved, or maintenance cost exceeds the selected budget. |
| Binding owner | Selected execution/state profile; acceptance ordering, fault atomicity, single-writer and applicable capacity tests on the concrete implementation. | The binding cannot enforce an applicable invariant or has untested required failure behavior. |
| Binding and resource owners, when durability or external recovery is required | Exact storage/executor contract, crash evidence, idempotency/reconciliation behavior, retention, and recovery limits. | Required recovery exceeds the demonstrated guarantee or ambiguous external outcomes have no workable handling. |
| Deployment and security owners, for relevant workload and threats | Workload measurements, enforced limits, access/isolation evidence, monitoring, and an operational response for the actual environment. | Required load or threat assumptions are unmet; a stronger claim lacks matching evidence. |
| Project owner | Recorded comparison, accepted costs, supported guarantees, remaining limitations, and criteria for revisiting the choice. | No incremental benefit is demonstrated, or an unmet requirement blocks the intended deployment. |

The repository supplies a specification, worked examples, verification routes, and adoption guidance. It supplies no runtime, reference implementation, comparative benchmark, or evidence for your deployment. Documentation checks can establish consistency; they cannot establish production reliability or human usability on an untested project. A successful pilot supports its measured feature, team, binding, and environment. It does not establish suitability for every project.
