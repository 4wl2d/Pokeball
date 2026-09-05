---
name: pokeball-composition
description: Design or change Pokeball ownership boundaries, inter-Ball reads, commands, signals and Flow coordination. Use for a real authority or dependency change, including extracting shared behavior.
---

# Pokeball composition

Work on the consuming project. Resolve supporting references within this skill's directory. Read [working contract](../references/working-contract.md) once and select the applicable [composition routes](../references/composition.md).

## Resolve ownership before wiring

1. Identify the affected facts, invariants, authority, lifecycle/recovery and trust boundaries. Inspect existing owner-authored surfaces and source dependencies. Use those facts to justify a boundary; screens, tables and call counts do not determine Ball ownership.
2. Classify the needed interaction before implementing it: a non-mutating target-owned Query/result, an accepted command/result round trip, or a producer-owned Signal. A read must not initiate target acceptance, a State revision or outputs. Actor-dependent admitted read meanings belong to the target's pure policy and closed result contract.
3. Introduce a Flow only when it owns material coordination: lifecycle, order/branch/join, cancellation, compensation/recovery, reconciliation or an independent terminal outcome. A single foreign lookup or command hop is insufficient by itself.
4. Keep protocol and Application Surface ownership at the participant. The caller's Nucleus may use declared imported owner-authored surfaces; it cannot access foreign internals/State or re-export another owner's protocol. Assembly binds declared routes and transport; it does not invent business choices.
5. When an edge changes, check both compile-time imports and Direct Control Dependencies. Async handoff can remove synchronous-invocation coupling while an independently present import still contributes its dependency. Resolve only present dependency, route and fan-out bounds using their source counting rules.
6. Implement the source/wiring change and relevant positive/negative contract tests. For shared helpers, distinguish one Ball/role's utility, shared mechanical Foundation, and business policy requiring explicit ownership; do not hide domain communication in a utility package.

Report the ownership decision, changed contracts/edges and evidence. Add a diagram only when it clarifies the changed graph; source may already carry the map. Command execution details use [async](../pokeball-async/SKILL.md); changing the enforcement machinery uses [binding](../pokeball-binding/SKILL.md).
