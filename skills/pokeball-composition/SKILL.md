---
name: pokeball-composition
description: Implement Pokeball ownership boundaries, cross-authority reads, commands, signals and Flow coordination. Use for a real dependency or ownership change, including shared helper extraction.
---

# Pokeball composition

## Assign ownership

Identify changed facts, invariants, authority, lifecycle/recovery and trust boundaries. Keep one semantic owner and logical writer per mutable fact. Combine behavior when one owner can enforce its invariant; separate independent authorities or lifecycle/recovery boundaries. Do not derive Ball ownership from screens, endpoints, tables or call counts.

Use a Feature Ball for an owned local capability. Use a Read Model Ball for owned derived query State with source positions, freshness and rebuild policy; give it no command authority over source facts. Introduce a Flow only for material coordination: lifecycle, ordering/branch/join, compensation/recovery, cancellation, reconciliation or an independent terminal outcome. A lookup or command hop alone needs no Flow.

## Select the interaction

- **Read:** bind the exact target-owned Query/result surface. Keep the target read pure: no decision, acceptance, State/revision change or output. Put admitted denial/redaction in its closed result. Resolve actual authorization context, consistency/freshness requirements and mismatch behavior. Add stamps or retained read State only when the selected consistency/status path requires them.
- **Command:** the source decision emits an accepted `ModuleCommandRequest`; verify and deliver a `ModuleCommandPulse` to the target. The target accepts its `ModuleResultOutput`; verify and return a `ModuleResultPulse` to the source. Keep schemas target-owned and preserve accepted source/target correlation and issuer provenance. Use the declared pre-acceptance rejection carrier, not a fabricated result for an unaccepted command.
- **Signal:** publish the producer-owned closed signal after source acceptance. Bind exact consumers with required provenance, ordering/deduplication and bounded fan-out. Do not substitute a signal for an acknowledged command outcome or transfer ownership of source facts.

Capture foreign results as explicit inputs before decisions. Retain required values and lineage in owned State when later decisions consume them. Perform no cross-authority I/O or mutable reads inside the pure Nucleus.

## Wire the contracts

Participants own their Application Surfaces and protocol schemas. Callers may import declared owner-authored public surfaces, never foreign internals/State. Do not redeclare or re-export another participant's protocol.

Keep Assembly to static route/version selection and transport. Business branches, output choices, permissions and result interpretation stay in the responsible Nucleus. Bind one effective command/result round trip per declared operation; do not split its legs into separate business routes or add wildcard dispatch.

For a Flow, retain required participant references and cross-step values in State. Make compensation, cancellation and uncertain-outcome reconciliation explicit decisions. Give each retryable failure mode one primary retry owner; preserve semantic identity across transport attempts. Do not promise a distributed transaction or infer known failure from timeout.

## Check graph and bounds

Keep compile-time imports and Direct Control Dependencies acyclic. Async handoff removes synchronous invocation coupling only; independently present imports remain dependencies.

Count each distinct resolved read, command, signal and FlowParticipation declaration once. Count a command/result round-trip mapping as one Flow route; reject duplicate or split-leg aliases. Enforce dependency, route and participant limits before execution. For cumulative fan-out, count accepted-output-to-consumer branches across the causal scope; sum co-reachable branches, share reservations only for mutually exclusive alternatives and exclude retry/redelivery of the same branch. Reject over-limit candidates as a whole before acceptance.

Keep helpers owned by one Ball and logical role unless sharing purely mechanical Foundation code. Shared business policy gets an explicit Ball/Flow owner and public contract. Do not hide business communication in Foundation, globals or service locators.

## Verify and finish

Test the actual bound target and result, repeated non-mutating reads, schema ownership, both dependency graphs and changed count boundaries. For commands, test correlation, pre-acceptance refusal and accepted result return. For changed workflow failures, test the declared terminal/recovery path.

Report changed ownership, contracts and edges, tests/results and unresolved policy selections. Preserve unaffected contracts and features.
