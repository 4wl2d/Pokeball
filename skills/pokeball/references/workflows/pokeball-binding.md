# Pokeball binding implementation

Work in the consuming project. Resolve supporting references within this skill's directory. Read [working contract](../working-contract.md) once, then use [binding routes](../binding.md) for the mechanism being changed.

## Make the mechanism testable

1. Locate the selected mutation form, single accepted-write site, execution profile, storage profile, trust boundary and relevant binding tests. Resolve the exact contract already covering the feature. If creating a binding, explicitly choose the required mechanisms from the real workload and failure requirements; Core supplies no runtime implementation.
2. Write the failure trace for the changed mechanism before coding it. Distinguish before acceptance, atomic acceptance, after acceptance before dispatch, external execution and recovery. Name the exact visibility boundary the implementation must protect.
3. Keep the binding mechanical. It verifies/converts permitted observations, reserves/admit resources where triggered, accepts the selected complete frame, publishes and schedules retained outputs. Business policy, read-result selection, retry/fallback decisions and Sovereign State changes remain with the owner and its serialized decision route.
4. Preserve State plus all present outputs as one accepted unit, with no early dispatch or reentrant mutation. EventJournal accepts its event-based frame and evolves State; it does not add a separately chosen `nextState`. Use the source-specific atomicity requirements for the selected durability profile.
5. Resolve finite bounds for present dimensions by construction or one effective scoped declaration. Runtime capacity and causal reservation belong to admission/scheduling, not semantic `DecisionContext`. For a materialized byte limit or work meter, read the exact measurement contract before implementing overflow behavior.
6. Test the changed boundary with controlled scheduling and fault injection. No acceptance means no partial State/revision/output publication; successful acceptance retains the whole selected frame. For durable work, crash/restart tests must exercise the actual persistence transaction and recovery path, including pending output and unknown external outcome handling.

Select security, lifecycle, migration and profile fixtures only when those paths change. A green mock test or the presence of an outbox cannot establish downstream delivery, exactly-once effects or production readiness. Report the implemented mechanism, tests actually run and the precise remaining guarantee boundary.
