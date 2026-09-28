---
name: pokeball-core-2
description: Write, change or review a feature on Pokeball Core 2 (Balls, deciders, ports, adapters, composition). Use when code imports pokeball.kernel or pokeball.runtime, or when asked to add a stateful feature that calls external systems in this repository's style.
---

# Pokeball Core 2 feature work

## Before writing code

1. Work in the consuming project and use its existing kernel, runtime, and tests. A review request authorizes findings only; edit or publish only when requested.
2. Name the owner of each fact: one Ball type per consistency boundary.
3. For every external call, choose the port's effect class honestly:
   - `Safe`: no effect;
   - `Idempotent`: the destination deduplicates by the call identity;
   - `NonIdempotent`: otherwise.
   If unsure, choose `NonIdempotent`.

## Writing the Ball

- `decide`, `complete` and `observe` must be pure. Take time only from `ctx.now` and keys only from `ctx.newKey()`; no I/O, clocks, randomness, globals or threads (R2).
- Keep every stage and every outcome as an explicit state, including "may have happened" (`Unknown`).
- Store the key of each call whose completion matters, and ignore completions with other keys.
- Never re-send after `Unknown` on a NonIdempotent port. Reconcile instead: wait past the destination's horizon, then query a Safe status port.
- Reply to every accepted request, now or later (R9). Wait with `Timer.after`, and treat a timer completion as a wake-up.
- Make state and messages immutable data (R1).

## Writing an adapter

Report once. Never retry. Report `NotApplied` only with evidence that nothing was applied; otherwise report `Failed`. For Idempotent ports, send `invocation.callId` as the idempotency key. Check it with `pokeball.testkit.AdapterConformance`.

## Wiring

Register Balls, adapters and subscriptions in one composition root. Features depend on each other only through `-api` modules (R10).

## Checks to run before finishing

Use the consuming project's build and tests. In the reference implementation, run from its Gradle root:

```sh
./gradlew build checkModuleBoundaries
```

This runs the architecture rules, runtime tests, and examples. For a changed effectful feature, use `Harness` + `SimPort` + `explore` when available: inject network faults and a crash, assert `h.assertInvariants()`, and check the feature's state against `h.truth`. Reuse the project's equivalent tests when it uses another conforming runtime. State what was actually checked.

## Review checklist

- Effect class of each port justified in a comment or ADR.
- No `Unknown` treated as failure or retried.
- Every completion branch handles `Done`, `NotDone` and `Unknown`.
- No decider reads anything but its arguments.
- Tests explore faults and crashes, not only the happy path.
