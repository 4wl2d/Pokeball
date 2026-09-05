---
name: pokeball-async
description: Implement or debug Pokeball effects, late results, command/result handling, retry, cancellation, timers and operation status in application code.
---

# Pokeball asynchronous behavior

## Establish the change

Work in the application. Locate the affected operation, closed input/output types, accepted-write site, correlation verifier, status authority and tests. Reproduce the failing order from acceptance through dispatch to completion. Reuse the project's accepted protocol, binding and policies; identify any unresolved business outcome before choosing its behavior. Change only the requested paths.

## Preserve accepted causes

- Accept State and all present outputs atomically; dispatch afterward. Route completions through the owner's serialized decision. Do not write business State from callbacks, workers, timers or status materializers.
- Return private-resource outcomes as provenance-bound `Fact` values matching accepted `EffectRequest` work. Verify origin, effective protocol identity and correlation before trusted input construction. Let the owning decision apply its stale-result policy.
- Preserve logical operation, handle and accepted-output identity across attempts. Materialize only identities, generations and revisions needed by the actual path. Supply each completion with its own verified current context; retain values and lineage needed by later decisions in owned State.
- Preserve remaining causal depth and fan-out budgets across handoff, yield and retry. Keep capacity in admission/scheduling, outside semantic decision context. Preserve accepted completions when an execution quantum ends.

## Implement only present protocols

For an inter-Ball command, preserve this sequence:

`accepted ModuleCommandRequest → verified ModuleCommandPulse → target decide → accepted ModuleResultOutput → verified ModuleResultPulse → source decide`

Keep the mapping and payload target-owned. Preserve `commandSource`, accepted-target `resultSource`, effective protocol identity and issuer provenance. Do not synthesize results in Assembly or adapters. Use a non-mutating target-owned Query for a lookup requiring no accepted operation record.

Return target validation, admission or decision rejection before acceptance only through verified `CommandRejectedBeforeAcceptance`. Create no target accepted frame, revision, output or operation. Route a failure after acceptance through the accepted result path. Reject conflicting carrier/result evidence.

Within the retained idempotency horizon:

- At root ingress, replay the original accepted reply for the same key/fingerprint. Reject a different fingerprint before constructing an Intent. Create no new decision or operation.
- At the target, return verified ACK of the existing acceptance while its result is pending; do not wait, decide again or fabricate a pending result. After result acceptance, redeliver the exact accepted result frame. Preserve semantic/frame fields; change only attempt identity.

For work outliving its initiating call, independently queried/reconciled work or an explicit status requirement, provide an independent committed status query with one revisioned writer. Reserve all newly reachable status/pending/retention capacity before source acceptance. Retain accepted observations until applied; apply prerequisites first or keep bounded pending observations. Preserve independent acceptance, outcome, cancellation and delivery facets. Merge equivalent duplicate observations without a new status revision; reject conflicting observations for the same causal key; never regress a proven facet. Create no known operation from a merely reserved ID or rejected root request. Expose absence/expiry only after the declared namespace, source-coverage and empty-pending barriers; prevent resurrection by covered duplicates.

## Handle uncertainty and termination

- After possible external execution without proof, retain `OutcomeUnknown`; implement the accepted reconciliation or manual terminal policy. Do not infer failure from timeout or delivery exhaustion.
- Assign one primary retry owner per failure mode. Disable other layers or bound them with demonstrated semantic transparency. Bound cumulative attempts/time. For blind retries after possible execution, require explicit policy permission, stable identity and provider/target idempotency whose deduplication horizon covers the entire retry window.
- Treat racing cancellation as a typed operation with its own identity. Preserve legitimate results and the first accepted terminal outcome; fail closed on contradictory terminal proof in either arrival order. Add timer generation or deduplication only for replacement or duplicate-delivery paths.
- Keep detached work transient unless accepted requirements demand process-loss survival. Preserve the selected durability and retention boundaries.

## Verify and report

Test affected cases: late A after newer B, duplicates, wrong correlation, cancellation/completion orders, timeout after possible success, retry exhaustion and capacity overflow. Reuse unchanged binding evidence. Report the code change, observed behavior, checks actually run and unresolved operation-policy or external-outcome decisions. Do not claim delivery or exactly-once execution beyond the implemented evidence.
