---
name: pokeball-review
description: Review a supplied Pokeball implementation or diff for concrete behavior and architecture regressions. Use for bounded code review, not general architecture certification.
---

# Pokeball code review

Review the supplied fixed commit, diff or file set. Follow affected owners, necessary callers, binding and tests. Stay read-only unless fixes are requested; publishing comments requires authorization.

Trace input validation → trusted input/context → pure decision/read → atomic acceptance → dispatch → completion → serialized decision. Check actual success/failure behavior.

## State and decisions

- Each mutable fact has one authority and logical writer. No foreign mutable-State access, callback writes or reentrant mutation.
- Decisions use explicit State, current typed input and minimal trusted context. No I/O, ambient clocks/randomness, globals, services or hidden decision inputs. Later decisions' required values/lineage stay in owned State.
- Readers see accepted values; rejected candidates cannot alter them. Immutable structure may be shared, mutable builders remain private. Do not demand deep copies or separate Balls for structure fields.
- Preserve Snapshot/Event mutation semantics; Event State comes from `evolve`, never an independently selected `nextState`.
- Accept complete State/outputs together before dispatch. Pre-acceptance failure publishes nothing partial. Later failure preserves accepted work and undelivered obligations.

Representation validation belongs at ingress; State/context-dependent business policy stays in the Nucleus. Distinguish validation, admission, business rejection, programming faults and external uncertainty. Queries produce target-owned closed results without decision, acceptance, revision or output; admitted denial/redaction stays in that result.

## Composition and binding

Verify target-owned schemas, bound capabilities and pure reads. Immediate commands execute after source acceptance, accept at the target and return through serialized source handling. Trusted capability and call scope can carry provenance/correlation without tokens, envelopes, issuer fields or protocol IDs. Post-acceptance failure never means `NotAccepted`.

Assembly owns wiring/transport; business choices stay in owners. An allowed additional consumer of an existing operation should need only wiring. A Flow owns actual coordination; shared utilities contain mechanical behavior, not hidden business authority. Imports and Direct Control Dependencies remain acyclic, including imports retained across async handoff.

Bound actual growing work: queues, external requests, retained outputs, retries and dynamic fan-out. Reject overflow before acceptance and preserve applicable causal budgets; capacity stays outside semantic context. Static terminating execution needs no numeric dependency/route/participant quota, depth field or reservation protocol. An import DAG alone cannot rule out commands repeatedly issued by result handlers.

## Independent delivery — when present

- Completions match accepted causes and protocol identity with verified provenance; they enter serialized decisions with their own context. Stale display results do not erase required operation observations.
- Keep Effect/Fact and command/result routes distinct. Commands preserve source acceptance, target ingress/acceptance and verified accepted result return. Assembly never creates business results or accepted target frames.
- Retries preserve operation/idempotency identity; only attempt identity changes. Reject same-key fingerprint conflicts. Pending duplicate commands acknowledge existing acceptance; completed duplicates replay its result.
- Timeout/cancellation after possible execution preserves `OutcomeUnknown`. Contradictory terminal proof fails closed. Use one primary retry owner and bounded transparent secondary retries, with cumulative attempts/time bounds.
- Detached work has required committed status, reserved capacity and retention. Apply prerequisites first or retain bounded pending observations. Merge equivalent duplicates without revision changes; reject conflicts, preserve proven facets and prevent expiry/duplicate resurrection through source-coverage barriers.

For changed durability, inspect real transactions, output retention, recovery and schema upcast/quarantine. For trust changes, verify actor/grant evidence, restricted capabilities, safe sinks and required containment. Wrappers or mocks do not prove isolation, delivery or crash safety.

## Report

Use property checks that survive local renaming/helper extraction. Reuse valid scoped binding tests. Source types and wiring may own the contract; optional tables can be derived. Do not demand unused adapters, overlays or duplicate evidence.

For each finding give location, violated invariant, concrete trace, practical effect and testable fix criterion. Separate defects, missing evidence and preferences; zero findings is valid. State scope, actual checks and material unknowns. This review supplies no general conformance or production verdict.
