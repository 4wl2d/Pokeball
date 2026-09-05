---
name: pokeball-review
description: Review a supplied Pokeball implementation or diff for concrete behavior and architecture regressions. Use for bounded code review, not general architecture certification.
---

# Pokeball code review

Review the supplied commit, diff or fixed file set. Identify affected owners and behavior; follow only necessary callers, binding code, project contracts and tests. Stay read-only unless fixes are requested. Do not publish comments without authorization.

## Trace the changed path

Follow input validation → trusted input/context → pure decision or read → atomic acceptance → dispatch → verified completion → subsequent decision. Check actual code and a concrete success/failure trace for each changed step.

### State and decisions

- One authority and logical writer per mutable fact. No foreign mutable-State access, callback writes or reentrant mutation.
- Decisions consume explicit State, current typed Pulse and minimal trusted context. No I/O, ambient clocks, randomness, services, mutable globals or hidden decision-relevant inputs.
- Later decisions' values retain required correlation/version lineage in owned State. UI/transport mechanics do not secretly own business State.
- Preserve the selected Snapshot/Event mutation form. Event State comes from `evolve`, with no independently chosen Event `nextState`.
- Accept State and the complete ordered output batch together before dispatch. Pre-acceptance failure publishes nothing partial. Post-acceptance failure preserves the accepted frame and undelivered obligations.

### Boundaries and reads

Representation/type validation belongs at ingress; State/context-dependent business policy belongs in the Nucleus. Distinguish validation, admission, business rejection, programming faults and post-execution uncertainty.

A Query returns the target-owned closed result without a decision, acceptance record, revision change or output. Admitted denial/redaction stays in that result algebra. Verify required actor, grant and result-origin evidence at the appropriate boundary; enforce current technical authorization at execution when required.

### Asynchronous work — when present

- Completions match accepted causes and effective protocol identity, carry verified provenance and enter serialized decisions with their own context. Stale display results do not erase required operation observations.
- Keep Effect/Fact and command/result routes distinct. Commands use an accepted source request, verified target Pulse, accepted target result output and verified source result Pulse. No Assembly-created business result or unaccepted target frame.
- Retries preserve logical operation and provider/target idempotency identity; only attempt identity changes. Reject same-key fingerprint conflicts. Pending duplicate commands acknowledge existing acceptance; completed duplicates replay the accepted result.
- Timeout after possible execution preserves `OutcomeUnknown`; cancellation does not prove non-execution. Contradictory terminal proof fails closed. Use one primary retry owner, disabled-or-bounded transparent secondary retries and finite cumulative budgets per active failure mode.
- Detached work has required committed status, reserved capacity and retention. Apply causal prerequisites first or retain observations in bounded pending State. Merge equivalent status observations idempotently, reject same-causal-key conflicts and never regress proven facets. Enforce absence/expiry barriers against duplicate resurrection.

### Composition and binding — when changed

Check target/producer-owned public schemas, exact route bindings and pure cross-authority reads. Assembly owns wiring/transport, not policy. A Flow owns actual coordination; utilities do not hide shared business authority. Both import and Direct Control Dependency graphs remain acyclic, including imports retained across async handoff.

Resolve finite limits for present variable dimensions. Overflow rejects the whole candidate before acceptance. Keep capacity outside semantic context; preserve causal budgets and completion reservations. Check dependency/round-trip/fan-out counting where changed.

For durable changes, inspect the actual transaction, retained outputs, recovery and schema migration/quarantine. For trust changes, check restricted capabilities, safe sinks, secrets and required real containment. Do not equate storage, wrappers or mocks with external delivery, credential isolation or crash correctness.

## Report actionable findings

Select checks for changed paths; do not require unused adapters, folders, overlays or evidence copies. Reuse still-valid scoped binding tests. Distinguish a demonstrated bug, missing evidence and a design preference.

For each finding, provide its code location, violated operational invariant, reproducible failure trace, practical effect and testable fix criterion. Order by impact. Include scope, checks actually run and material unverified assumptions. Zero findings is valid. Do not turn this review into a conformance, release or production-readiness verdict.
