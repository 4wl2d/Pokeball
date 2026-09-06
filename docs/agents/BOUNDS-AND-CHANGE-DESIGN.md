# Bounds and Change Design

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation when a design resolves an effective bound or profile, evaluates change impact, or prepares the routine review output.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Design Runbook](DESIGN-RUNBOOK.md) · [Agent Pack index](README.md)

## 7. Effective bounds and profiles

Resolve each present dimension through exactly one source:

1. bounded type or static control-flow proof;
2. immutable project/profile/binding policy reference; or
3. local declaration or an allowlisted local delta.

When a numeric `maxTransitionSteps` declaration supplies the decision-work bound rather than a static proof alone, resolve exactly:

```text
DecisionWorkMeter {
  meterIdentity
  meterVersion
  transitionArtifactVersion
  unitDefinition
  maxTransitionSteps
  overflowPolicy
}
```

The identity/version selects one immutable unit definition. For equal binding, `transitionArtifactVersion`, meter identity/version, canonical State, Pulse, and valid Context, the total is deterministic; units are non-negative and consumption monotonic. Counting starts once at zero for each `decide` and cannot reset, split, or restart in helpers, phases, loops, retries, yields, or representation erasure. Completion at `N` is legal; attempting `N+1` accepts no Decision frame, State, revision, output batch, or dispatch and follows the binding's finite typed pre-acceptance or programming-fault policy without truncating work into another business result. Compare numeric counts across bindings only when meter identity, meter version, transition artifact version, and unit definition all match; Core defines neither a universal unit nor bit-exact cross-language replay.

Each numeric `maxInputBytes`, `maxStateBytes`, or `maxOutputBytesPerDecision` without a static type-and-representation proof resolves one exact immutable binding-owned `BoundedByteMeasure`:

```text
BoundedByteMeasure {
  dimension: Input | State | DecisionOutputs
  measureIdentity
  measureVersion
  representationDefinition
  limitName: maxInputBytes | maxStateBytes | maxOutputBytesPerDecision
  maxBytes
}
```

`Input` selects exactly `RawBoundaryInput` or `NormalizedTrustedInput`, including the declared boundary metadata and Context fields, and rejects `N+1` before trusted semantic acceptance or `decide`. `State` measures the complete candidate `nextState` semantic representation, including declared semantic metadata and excluding heap, allocator, index, storage, compression, encryption, and transport mechanics; `N+1` rejects the whole Decision before State, revision, or output acceptance. `DecisionOutputs` measures the complete ordered output sequence, including its structure, every field and payload required by the actual representation; include correlation tokens and `sourceOrdinal` only when their triggers require them, and excludes `nextState`, non-envelope accepted-frame fields, and later transport, framing, compression, encryption, retry, and attempt data. Any alternate retained representation maps exactly to the selected semantic representation, representation erasure preserves counts for equal values, and unequal dimension/identity/version/representation/limit tuples are incomparable. Exact `N` may pass; no value is truncated and no Decision is partially accepted. A static proof needs no measure runtime artifact.

Keep actual static dependencies, participants, and routes visible in types and wiring. Core does not require numeric maxima for their counts. For a synchronous workflow, inspect its complete execution, including result, refusal, and failure handlers: a finite structure can bound all work without carried causal scope/depth, numbered reservations, or a separate geometric fan-out total. An import DAG alone does not prove this if a handler can emit another command indefinitely. Preserve finite bounds and pre-acceptance capacity checks for real queues, external requests, retained outputs/completions, dynamic consumers, and other growing work.

When a numeric `maxCumulativeFanout` is needed or explicitly selected, use Core §10.9 and [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md): count distinct accepted source-output-to-effective-route/consumer branches across one causal scope, sum co-reachable/terminal/converging traversals, share the maximum reservation for mutually exclusive alternatives, ignore retry/redelivery of the same accepted output and route, preserve scope across async handoff, and reject the complete over-limit Decision at exact `N+1` without partial dispatch. This reservation is admission state, never Context.

Resolve one effective execution/state/isolation/security profile in the same way. A stronger selected profile activates its runbook and tests. A claim activates a separate record naming the exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees; profile selection or source durability alone does not establish a stronger downstream guarantee.

## 8. Change impact

Update only authoritative artifacts affected by the delta:

| Trigger | Possible affected source |
|---|---|
| owned protocol/state/Decision | typed Ball source and transition tests |
| policy selection or override | exact policy ref/local delta and resolver tests |
| inter-Ball edge | owner-authored Application Surface, caller Nucleus closed contract, Assembly, and target/producer contract tests |
| helper extraction or sharing | owning Ball/logical role or mechanical Foundation classification, compile-time graph, and shared-domain ownership test |
| cross-Decision value | state lineage and recovery test if durable |
| persistent schema | migration/quarantine artifact |
| async/security/profile path | corresponding focused runbook tests |
| explicit claim | named-boundary/scope/assumptions/retention/evidence record and full gate evidence |

Shared mechanisms and evidence remain referenced at their accepted digest. Do not copy them into the Ball or overlay.

For a persistent schema change, every old state shape enters ordinary new-version `decide` only after authoritative upcast. Missing evidence or an unsafe mapping routes the record to quarantine/manual remediation; nulls, empty values, generic reasons, logs, or runtime history do not fill newly required semantic fields. Retained old-version outputs keep their original protocol meaning.

## 9. Routine review output

For ordinary work, report:

```text
baseline and affected authority
semantic delta
new/removed triggers
effective declaration or policy ref/delta, if present
tests run
remaining decision, if one blocks the triggered path
```

Use the full review record in [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) only for a conformance or release claim.
