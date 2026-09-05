---
name: pokeball-async
description: Implement or debug Pokeball asynchronous effects, late results, command/result processing, retry, cancellation, timers and operation status. Use when a Pokeball feature's work or result crosses an execution boundary.
---

# Pokeball asynchronous behavior

Operate on the consuming project. Resolve supporting references within this skill's directory. Read [working contract](../references/working-contract.md) once per task and select the relevant [async routes](../references/async.md).

## Trace the operation

1. Reproduce one concrete trace: accepted cause, retained output, dispatch, external observation, verified input, subsequent decision. Locate the existing correlation types, status owner, retry owner and selected execution/durability profiles.
2. Preserve the distinction between semantic operation identity, accepted-output identity and execution attempts. Decide the stale-result policy in the owning Nucleus; verify origin and correlation at the trusted boundary. A result arriving later gets its own verified current context.
3. Change only the activated protocol. Resource outcomes use the Effect/Fact route; inter-Ball commands use the target-owned command/result route. Do not substitute a bare callback, unverified payload or Assembly-created business result for the required boundary.
4. If work outlives its initiating call, read the operation-status source as well as the causal source. Resolve committed status, pre-acceptance capacity and retention, including late or conflicting observations. Suppressing an obsolete UI result does not erase required operation evidence.
5. Implement the business response to timeout, cancellation and retries from the selected routes. A timeout after a possible external action can mean `OutcomeUnknown`; cancellation is not proof that the action never ran. Keep one primary retry owner and preserve required operation/idempotency identity across attempts.
6. Test the traces this change can affect: A then B then late A; duplicate result; wrong correlation; cancellation racing completion; timeout after possible success; exhausted capacity or retry budget. Select relevant cases, not a universal fixture suite.

Return the code/test delta and what the observed trace now does. State any unresolved external-outcome or binding assumption. Detached execution alone does not select durable storage; crash survival additionally needs the actual [binding workflow](../pokeball-binding/SKILL.md).
