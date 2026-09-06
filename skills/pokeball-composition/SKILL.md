---
name: pokeball-composition
description: Implement Pokeball ownership boundaries, cross-authority reads, commands, signals and Flow coordination. Use for a real dependency or ownership change, including shared helper extraction.
---

# Pokeball composition

## Assign ownership

Find the changed facts, invariants and lifecycle/recovery/trust boundaries in the application. Keep one authority and logical writer per mutable fact. Combine behavior when one owner can enforce its invariant; separate independent authorities or lifecycles. Screens, folders, tables and call counts do not determine Ball boundaries.

Use a Feature Ball for a local capability and a Read Model Ball for derived query State, source positions, freshness and rebuild policy. A Read Model gets no command authority over source facts. Add a Flow for actual coordination: independent lifecycle, ordering/branch/join, compensation/recovery, cancellation, reconciliation or terminal outcome. One command round trip needs no Flow.

## Connect the owners

- **Read:** use the target-owned Query/result interface. Reading causes no decision, acceptance, revision or output. Resolve actual authorization and consistency/freshness requirements; keep admitted denial/redaction in the result. Add stamps only where their use requires them.
- **Immediate command:** run the typed target call from an accepted source output, outside pure `decide`. The target accepts its own change; the return enters the source's serialized handler. `increment() -> Changed(value) | NotAccepted(reason)` can express the contract. Supplied capability and call scope carry target, provenance and correlation. Crossing a Ball alone requires no token, envelope, issuer or protocol-ID fields. Failure after acceptance never means `NotAccepted`.
- **Independently delivered command:** preserve accepted source/target identity, verified provenance, exact protocol mapping and serialized delivery. Carry causal envelopes where information must survive the call. Distinguish pre-acceptance refusal, accepted results and unknown external outcomes.
- **Signal:** dispatch producer-owned accepted signals to exact consumers. Apply required provenance, ordering/deduplication and actual fan-out bounds. Signals do not acknowledge command outcomes or transfer source authority.

Supply foreign results as explicit decision inputs. Retain values and required lineage in owned State when later decisions need them. Do no cross-authority I/O or mutable read inside the pure Nucleus.

## Wire and bound execution

Participants own operation/read schemas. Supply narrow capabilities through Assembly, such as read-only and increment-only Counter views. Consumer names belong in wiring: an additional allowed consumer of an existing operation changes no Counter result type or caller registry. Caller-dependent business permission stays in the owner. Never access foreign mutable State or redeclare foreign protocols.

Assembly selects routes, versions and bindings. Keep business branches, payload choices and result interpretation in owners. One command/result round trip is one mapping; avoid split-leg aliases and wildcard dispatch. Derive optional manifests and route tables from source types and wiring.

Keep import and Direct Control Dependency graphs acyclic; async handoff does not remove remaining imports. Static terminating execution—one command, return, source update, then completion—needs no numeric dependency/participant/route quota, depth field, geometric fan-out calculation or reservation protocol. An import DAG alone does not prevent a result handler from issuing the command again.

Bound actual growing work: queues, retained outputs, external requests, retries and dynamic fan-out. Preserve applicable budgets across handoff; reject overflow before accepting work that could be lost. Give each retryable failure mode one primary retry owner. Preserve identity across attempts and uncertainty after possible external execution.

Share purely mechanical Foundation code. Keep business policy owned by a Ball/Flow and expose its public contract; do not hide communication in globals or service locators.

## Verify

Test target selection, reads, acceptance order, return, refusal and post-acceptance failure. Add an allowed consumer through wiring and reuse the binding. Test changed growing-work limits and failure/recovery behavior. Report ownership/contract changes, checks run and unresolved choices; preserve unaffected project contracts.
