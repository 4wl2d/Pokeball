---
name: pokeball-binding
description: Implement or debug a Pokeball application's writer, acceptance, admission, dispatch, scheduling, boundary adapters or durable recovery. Use when binding mechanics change; keep ordinary business-rule edits in the feature.
---

# Pokeball binding implementation

## Establish the mechanism

Work in the application. Locate the selected mutation/execution/storage profiles, accepted-write site, scheduler or transaction, adapters and tests. Reuse accepted project contracts and scoped mechanisms. For a new binding, resolve the actual visibility, failure and workload requirements; omit unused protocol categories, profile records and adapters. Record one concrete failure trace for the mechanism being changed.

## Implement acceptance

- Enforce one logical writer per state instance. Prevent reentrant decisions and concurrent mutation. Apply expected-revision or ownership fencing when concurrency or movable ownership requires it.
- Keep business schema, permission, read-result selection and retry/fallback policy in the owner. Limit the binding to verification, trusted-input construction, admission/reservation, accepted-frame publication and scheduling. Route state-relevant mechanical observations through declared inputs and serialized decisions.
- For Snapshot mutation, atomically publish the complete `nextState` and ordered outputs, including revision when present. For EventJournal, atomically accept the commit envelope, ordered events, all outputs and accepted-input marker, including accepted `NoDomainChange`; reconstruct State through `evolve`. Do not add an independently selected Event `nextState` or force both mutation forms into a runtime union.
- Join any idempotency, status and durable-output records required by the selected acceptance transaction. For separately materialized status, reserve capacity before source acceptance. Dispatch no output before acceptance. On rejection or pre-acceptance fault, publish no partial State, revision or output. After acceptance, preserve the accepted frame; do not rewrite it into rejection.
- Preserve pure local reads: evaluate the target-owned closed result with valid context, without a decision, acceptance marker, revision advance or output. Encode admitted denial/redaction within the declared result; keep validation/admission failures before read invocation.

## Bound scheduling and admission

- Resolve finite limits for present dimensions through construction or scoped declarations. Keep runtime capacity outside `DecisionContext`; changing capacity may reject admission but must not change the candidate business decision.
- Admit complete candidates. Bound retained outputs before admitting another frame. Reject overflow before publication; never truncate State or accept an output subset. For numeric byte bounds, fix the measured representation, metadata inclusion, stage and unit. Measure the complete candidate State or ordered output envelopes/payloads, not a convenient partial buffer. Keep numeric work-meter counts deterministic and monotonic across helpers, phases and yields, starting once per decision.
- When synchronous completions or multi-decision causal chains exist, reserve their total depth, completion slots and present fan-out capacity before acceptance. Preserve each retained item's own trusted input/context and remaining budget across yield/resume. Do not drop accepted work or reset budgets.
- For a same-stack command, reserve source level 1 before dispatch. Target acceptance consumes it only after reserving the level-2 source-result completion. A verified pre-acceptance target refusal creates no target frame and atomically transfers level 1 to the source carrier-handling decision. Prevent double consumption; reserve any further carrier outputs normally.
- For concurrent execution, bound mailboxes/workers and return typed admission failures before acceptance. Add fairness and reordering mechanisms only where those risks exist. Reject new logical mutations during Draining and after Stopped. While draining, preserve only the declared completion, cancellation and available Query/status-read paths.

## Apply the selected boundaries

- For durable acceptance, use the actual authoritative transaction and recover its committed records. Retain pending outputs through the selected terminal horizon; preserve stable delivery identity and recorded delivery-stop outcomes. Do not regenerate accepted outputs with current decision code or blindly reevaluate an ambiguously committed irreversible input.
- Migrate persisted semantic fields through authoritative upcasts or quarantine. Preserve required operation status with pre-acceptance capacity, causal-order application or bounded pending, and covered-source retention barriers. Do not add durability solely because a callback is detached.
- Verify provenance and required actor/grant evidence before trusted construction. Enforce current technical authorization at execution when required. Use restricted capabilities, structured/context-safe sinks and exact secret-path policies. Do not treat wrappers or private visibility as credential isolation. Use a process/sandbox for hostile-component containment; an ordinary scoped credential alone does not require it.
- Keep programming faults distinct from business rejection, pre-acceptance admission failure and post-execution unknown outcomes. Preserve declared external uncertainty rather than reporting an unproven failure or success.

## Verify and report

Exercise changed boundaries with controlled scheduling and fault injection. Test just-before/after acceptance, dispatch ordering, applicable exact-limit/overflow cases, and real crash/restart recovery for durable changes. Check ownership, context preservation and no partial publication. Reuse unaffected scoped evidence. Report the implemented mechanism, tests/results, remaining assumptions and exact failure boundary; do not infer downstream delivery or exactly-once execution from an outbox or successful mock test.
