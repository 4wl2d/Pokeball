---
name: pokeball-async
description: Implement or debug Pokeball detached effects, late or independently delivered results, retry, cancellation, timers and operation status. Immediate local calls alone do not need this workflow.
---

# Pokeball asynchronous behavior

## Establish the change

Locate the application's affected operation, types, acceptance site, verifier, status authority and tests. Reproduce the failing order. Reuse accepted protocols/bindings/policies; resolve unknown business choices before implementing them. Keep the requested scope.

## Preserve accepted causes

- Accept State and all present outputs atomically; dispatch afterward. Route completions through the owner's serialized decision. Do not write business State from callbacks, workers, timers or status materializers.
- Return private-resource outcomes as provenance-bound `Fact` values matching accepted `EffectRequest` work. Verify origin, effective protocol identity and correlation before trusted input construction. Let the owning decision apply its stale-result policy.
- Preserve logical operation, handle and accepted-output identity across attempts. Materialize only identities, generations and revisions needed by the actual path. Supply each completion with its own verified current context; retain values and lineage needed by later decisions in owned State.
- Preserve applicable remaining causal and fan-out budgets across handoff, yield and retry. Bound actual queues, retained outputs, requests and cumulative work. Keep capacity outside semantic decision context. A statically terminating immediate workflow needs no numeric depth or reservation protocol; an import DAG alone does not establish termination.

## Implement only present protocols

For an independently delivered inter-Ball command, preserve this sequence:

`accepted ModuleCommandRequest → verified ModuleCommandPulse → target decide → accepted ModuleResultOutput → verified ModuleResultPulse → source decide`

Keep the mapping and payload target-owned. Preserve `commandSource`, accepted-target `resultSource`, effective protocol identity and issuer provenance. Do not synthesize results in Assembly or adapters. Use a non-mutating target-owned Query for a lookup requiring no accepted operation record.

For independently delivered work, convey target validation, admission or decision rejection through verified `CommandRejectedBeforeAcceptance`; create no target accepted frame or operation. An immediate typed call may instead return a contract-specific `NotAccepted(reason)` correlated by call scope. No extra token or carrier type is needed solely for crossing a Ball. Post-acceptance failure never becomes nonacceptance; preserve accepted results and unknown external outcomes. Reject contradictory evidence.

Within the retained idempotency horizon:

- At root ingress, replay the original accepted reply for the same key/fingerprint. Reject a different fingerprint before constructing an Intent. Create no new decision or operation.
- At the target, return verified ACK of the existing acceptance while its result is pending; do not wait, decide again or fabricate a pending result. After result acceptance, redeliver the exact accepted result frame. Preserve semantic/frame fields; change only attempt identity.

Work outliving its call, independent query/reconciliation or explicit status requirements activate one committed, revisioned status authority. Reserve reachable status/pending/retention capacity before acceptance. Retain observations until applied, prerequisites first or in bounded pending. Preserve acceptance, outcome, cancellation and delivery facets. Equivalent duplicates cause no new revision; conflicts fail closed and proven facets never regress. Reserved IDs/rejected roots create no known operation. Absence/expiry requires namespace, source-coverage and empty-pending barriers; covered duplicates cannot resurrect it.

## Handle uncertainty and termination

- After possible external execution without proof, retain `OutcomeUnknown`; implement the accepted reconciliation or manual terminal policy. Do not infer failure from timeout or delivery exhaustion.
- Assign one primary retry owner per failure mode. Disable other layers or bound them with demonstrated semantic transparency. Bound cumulative attempts/time. For blind retries after possible execution, require explicit policy permission, stable identity and provider/target idempotency whose deduplication horizon covers the entire retry window.
- Treat racing cancellation as a typed operation with its own identity. Preserve legitimate results and the first accepted terminal outcome; fail closed on contradictory terminal proof in either arrival order. Add timer generation or deduplication only for replacement or duplicate-delivery paths.
- Keep detached work transient unless accepted requirements demand process-loss survival. Preserve the selected durability and retention boundaries.

## Verify and report

Test changed late-result, duplicate, correlation, cancellation, unknown-outcome, retry and capacity paths. Reuse unaffected binding tests. Report behavior, checks actually run and unresolved policies/outcomes. Do not claim delivery or exactly-once execution beyond the evidence.
