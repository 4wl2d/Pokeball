---
name: pokeball
description: Implement or change local Pokeball features, owned State, business decisions and pure reads. Use for ordinary feature work in an existing binding or a small first feature.
---

# Pokeball feature development

## Locate the change

Work in the application's language and layout. Find the fact's owner, decision/read function, typed input, acceptance site and tests. When a binding already exists, reuse it and accepted project policies. If none exists yet, implement the Inline Transient accept loop below once, then reuse it.

For a first feature, define one owned fact, invariant, closed inputs, pure decision and serialized acceptance. Keep them in one file when practical. Add Resources only for real external actions. Stateless mechanical helpers remain utilities.

Always-applicable accept skeleton (omit empty outputs; EventJournal/`evolve` only if that form is already selected): require no reentry → validate ingress → `decide` → on reject return without publish → atomically publish state and any outputs → dispatch outputs after publish → return the accepted observation (flat `Accepted(nextState)` with omitted empty outputs equals nested Snapshot framing).

## Implement owned behavior

- Give each mutable semantic fact one authority and logical writer. Read foreign facts through the owner's public Query or trusted input. Never expose foreign mutable State.
- Publish isolated accepted values. Share immutable substructure; keep mutable builders candidate-private. Rejected candidates must not alter earlier published values. Parts of one structure need no separate Balls or deep copies.
- Keep business choices in the Nucleus, representation validation in Interaction and accepted action execution in Resources. Show roles in types, calls and write sites; role names require no folders or interface hierarchy.
- Decisions are pure and terminating: explicit State, current typed input and minimal trusted context produce candidate State/outputs. Supply relevant time, actor, configuration and reserved semantic IDs as verified values. No ambient clocks, randomness, services, globals or mutable closures.
- Retain values needed by later decisions in owned State with required lineage. UI/transport values affecting business choices must be committed State or current explicit input/context.
- Preserve the mutation form. Snapshot proposes complete next State and ordered outputs. EventJournal proposes events/outputs and reconstructs through `evolve`; never choose an independent Event `nextState` or require both forms.
- Reuse binding acceptance of State/all outputs together, followed by dispatch. No dispatch inside `decide`, callback State writes or reentrant decisions. Rejection/pre-acceptance faults publish no partial State, revision or output.

## Keep boundaries precise

Validate malformed representations at ingress. Evaluate State-dependent limits and business permission in the decision; do not move a business rule into parsing during a refactor. Distinguish boundary, admission, business rejection and later execution failure.

Reads map owned State, Query and required trusted context to target-owned results, including denial/redaction where applicable. A Query creates no decision, revision, acceptance record or output.

For immediate same-build commands, execute the typed target call after source acceptance and return through the source's serialized handler. The call scope and supplied capability can carry correlation/provenance. Crossing a Ball alone requires no tokens, envelopes, caller registry or protocol-ID field. An error after acceptance never becomes `NotAccepted`. A callee programming fault after that source acceptance surfaces only declared operational/quarantine evidence or `OutcomeUnknown` when execution may have occurred.

Preserve identity for results that may arrive late, reorder, repeat, recover or be observed independently. Detached work needs required status, bounded execution and unknown-outcome handling. Do not silently change an accepted project policy or guarantee.

## Bound and verify the change

Bound present variable inputs, State, outputs and execution by construction or effective scoped limits. Static terminating calls need no depth/reservation protocol or numeric dependency quota. An import DAG alone does not establish termination. Bound actual queues, retained outputs, requests and growing work; reject overflow before acceptance and never accept a partial batch.

Source types/wiring can carry the contract; derive optional tables. Reuse valid binding evidence. Add only present triggers and allowed local deltas, without empty overlays or absence paperwork.

Test transitions, allowed boundary and first rejected value, candidate isolation, rejection preserving State and affected reads. Assertions should survive renaming and helper extraction. Test ordering/failure only for changed paths. Report changed behavior/owner, checks actually run and unresolved decisions. Local tests establish neither production nor general conformance guarantees.
