---
name: pokeball
description: Implement or change local Pokeball features, owned State, business decisions and pure reads. Use for ordinary feature work in an existing binding or a small first feature.
---

# Pokeball feature development

## Locate the change

Work in the application's language and layout. Find the mutable fact's owner, decision/read function, typed input, accepted-write site and nearest tests. Reuse the existing binding and accepted project policies. Limit changes to the requested behavior.

For a first feature, choose one owned fact and invariant, define closed input types, implement a pure decision and one serialized acceptance site, then add behavior tests. Keep these in one file when practical. Add a Resource adapter only for actual external work. Keep a stateless formatter or mechanical helper as an ordinary utility.

## Implement the owned behavior

- Give every mutable semantic fact one authority and logical writer. Read foreign facts only through the owner's public Query or verified input; never access another feature's mutable State.
- Keep business choices in the Nucleus. Interaction validates representation and adapts inputs; Resources execute accepted requested work. Make role boundaries visible in types, calls and write sites. Do not create folders or interfaces merely to name the roles.
- Make the decision pure and terminating: explicit State, one current typed Pulse and minimal trusted context in; candidate change and outputs out. Supply decision-relevant time, actor, configuration and reserved semantic IDs as verified values. No clocks, randomness, globals, services or mutable closures inside the decision.
- Retain a value needed by a later decision in owned State with its required correlation/version lineage. UI or transport values affecting business choices must be committed State or explicit current input/context.
- Preserve the selected mutation form. Snapshot proposes complete next State and ordered outputs. EventJournal proposes ordered domain events and outputs; reconstruct State through `evolve`. Do not choose an independent Event `nextState` or require both forms.
- Let the binding accept State and all present outputs atomically, then dispatch. No dispatch from the decision or State writes from callbacks. Preserve one writer and prevent reentrant decisions. Rejection or pre-acceptance faults publish no partial State, revision or output.

## Place validation and reads

Reject malformed or invalid closed-type inputs at ingress. Evaluate State-dependent limits, permissions and business policy in the decision. Preserve the distinction between boundary failure, business rejection, admission failure and later execution failure. Do not move a business restriction into the input type as an incidental refactor.

Implement reads as pure mappings from owned State, typed Query and required trusted read context to the target-owned closed result. Include declared denial/redaction outcomes where relevant. A Query starts no decision, advances no State/revision and creates no acceptance record or output.

## Keep the implementation proportional

- Bound each present variable input, State, output collection and decision path by construction or one effective scoped limit. Reject overflow at its required stage before acceptance; never truncate a candidate or accept an output subset.
- Reuse inspectable source contracts and still-valid shared binding evidence. Add only actual policy selections or allowed deltas. Omit empty overlays, unused protocol categories, placeholder manifests and routine absence-proof paperwork.
- For a new external action, detached completion, foreign authority, persistent field or trust boundary, resolve its concrete protocol and binding behavior before using that path. Keep operation identity across retries, verify result provenance, retain required detached-operation status and preserve unknown external outcomes. Do not silently choose a new project policy or guarantee.

## Verify and finish

Test the changed transition, allowed boundary value, first rejected value, rejection preserving State, determinism and affected read behavior. Add ordering/failure cases only for changed paths. Reuse unaffected binding tests.

Report the changed behavior and owner, code/tests touched, checks actually run and any unresolved decision blocking the requested path. Do not infer production or conformance guarantees from local tests.
