---
name: pokeball-binding
description: Implement or debug a Pokeball application's writer, acceptance, admission, dispatch, scheduling, boundary adapters or durable recovery. Use when binding mechanics change; keep ordinary business-rule edits in the feature.
---

# Pokeball binding implementation

## Establish the mechanism

Locate the application's selected profiles, acceptance site, scheduler/transaction, adapters and tests. Reuse accepted contracts and mechanisms. For a new binding, resolve actual visibility, workload and failure requirements. Omit unused categories and adapters. Reproduce the changed failure trace.

## Implement acceptance

- Enforce one logical writer; prevent concurrent mutation and reentrant decisions. Use expected revisions or ownership fencing when concurrent or movable ownership requires them.
- Keep business schemas, permission, read selection and retry/fallback policy in owners. The binding verifies inputs/context, admits work, publishes accepted frames and schedules execution. State-relevant mechanical observations enter serialized decisions.
- Snapshot atomically publishes isolated `nextState` and all ordered outputs, including revision when required. Share immutable structure; keep mutable builders candidate-private. Rejected candidates cannot mutate published values; deep copying is not mandatory.
- EventJournal atomically accepts ordered events, outputs and the accepted-input marker, including accepted `NoDomainChange`; reconstruct through `evolve`. Never choose an independent Event `nextState` or force both mutation forms into one runtime union.
- Join required idempotency, status and durable-output records in the acceptance transaction. Reserve separately materialized status capacity before source acceptance. Dispatch afterward. Rejection or pre-acceptance faults publish no partial State, revision or output. Post-acceptance failure preserves the accepted frame and obligations.
- Reads evaluate target-owned results with valid context; they cause no decision, acceptance, revision or output. Keep admitted denial/redaction in the result and ingress/admission failures before invocation.

## Bound actual work

Resolve present dimensions by construction or effective scoped limits. Runtime capacity belongs in admission, outside semantic context. Admit complete candidates; bound retained outputs before another frame. Never truncate State or accept an output subset. Byte limits name representation, metadata inclusion, stage and unit; measure complete candidate State/outputs. Work meters remain deterministic and monotonic across helpers and yields.

Reuse one serialized accept-and-dispatch mechanism across owners. A statically terminating synchronous chain can use call structure for its work bound and target/return correlation. No depth field, source/target token, carrier or level-1/level-2 transfer is required merely for crossing a Ball. Verify source acceptance before calling the target, target-owned acceptance and serialized source return.

For growing or retained work, reserve real capacity before acceptance. Preserve retained items' trusted context and applicable remaining budgets across handoff/yield. Never lose accepted outputs or reset cumulative budgets. An import DAG alone does not prove that result-triggered command chains terminate.

Bound concurrent mailboxes/workers; report admission failure before acceptance. Add fairness/reordering mechanisms when needed. Draining rejects new logical mutations while preserving declared completion, cancellation and available Query/status paths; Stopped rejects mutations.

## Apply triggered guarantees

- Durable acceptance uses the authoritative transaction and recovers its committed records. Retain pending outputs through the declared horizon with stable identity and delivery-stop outcomes. Never regenerate accepted outputs using current decision code or blindly replay an ambiguously committed irreversible input.
- Upcast or quarantine persisted semantic fields. Preserve required status through reserved capacity, causal-order application or bounded pending, and source-coverage retention barriers. Detached callbacks alone do not require durability.
- Trusted static calls can use supplied capabilities and the existing boundary as provenance. Verify source evidence for untrusted/independent delivery and required actor/grant evidence before trusted construction. Apply current technical authorization at execution when required. Use restricted capabilities, safe sinks and exact secret policies. Wrappers do not contain hostile code or isolate credentials; real hostile containment requires process/sandbox enforcement.
- Keep programming faults, business rejection, admission failure and post-execution uncertainty distinct. Preserve unknown external outcomes.

## Verify

Use controlled scheduling and fault injection at changed boundaries: before/after acceptance, dispatch order, exact-limit/overflow cases, candidate isolation and context preservation. Durable changes need real crash/restart tests. Reuse unaffected binding evidence; renaming/helpers must not break property checks. Report results and exact unverified failure boundaries. Outbox or mock success does not prove downstream delivery or exactly-once execution.
