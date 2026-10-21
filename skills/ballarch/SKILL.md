---
name: ballarch
description: Write, change or review a component on Ballarch Core 2 (Balls, deciders, ports, effect classes, composition, knowledge of composite operations), in Kotlin services, C firmware or RTL. Use when code imports ballarch.kernel or ballarch.runtime, includes ballarch.h, follows reference/rtl, or when asked to add a stateful feature that calls external systems or devices in this repository's style.
---

# Ballarch Core 2 component work

## Before writing code

1. Work in the consuming project and use its existing kernel, runtime and tests. A review request authorizes findings only; edit or publish only when requested.
2. Name the owner of each fact: one Ball type per consistency boundary.
3. Locate the two horizons of every interaction:
   - the **commit horizon**: the atomic commit of the decision that requests the effect;
   - the **send horizon**: the first moment the request may have left.

   Nothing may leave before the commit horizon. Before the send horizon, `NotDone` is provable; after it, only `Done`, `NotDone` with evidence of non-application (for a Safe port, the absence of any effect), or `Unknown`.
4. For every external call, choose the port's effect class honestly:
   - `Safe`: no effect;
   - `Idempotent`: the destination deduplicates by the call identity;
   - `NonIdempotent`: otherwise.

   If unsure, choose `NonIdempotent`.

## Writing the Ball

- `decide`, `complete` and `observe` must be pure. Take time only from `ctx.now` and keys only from `ctx.newKey()`. No I/O, clocks, randomness, globals or threads (R2).
- Keep every stage and every outcome as an explicit state, including "may have happened" (`Unknown`).
- Store the key of each call whose completion matters, and ignore completions with other keys.
- Never re-send after `Unknown` on a NonIdempotent port. Reconcile instead: wait past the destination's horizon, then query a Safe status port. A new call after `NotDone` is allowed, because `NotDone` proves nothing happened.
- Reply to every accepted request, now or later (R9). Wait with `Timer.after`, and treat a timer completion as a wake-up.
- Make state and messages immutable data (R1).

## Composite operations (R14)

When a Ball reports whether an operation made of several calls took effect, derive the claim with `ballarch.kernel.Knowledge`. Count every call for the operation's final effect:
- a call counts as `No` only once it completed `NotDone`, or once the Ball can no longer make it for this operation (for example, after the operation is settled);
- a requested call without a completion counts as `Maybe`, even if it may not have been sent yet, and so does a call the Ball may still make, including a compensation;
- `Done` with a refusal inside counts as `No`; pass an `effective` predicate to `effect` or `knowledge`.

Count a compensation only if it is absorbing: the destination fences the reference, or the original request's delivery horizon has passed. Combine repeated attempts at one effect with `or`, and repeated compensation attempts too.

Answer the request that starts the operation only once the operation is settled (no call open, none still to be made): accept it with `acceptLater`, and send a `Reply` carrying the settled knowledge. Give callers a deadline that covers the operation's worst case. A status given earlier that counts calls not yet made as `No` is a snapshot: mark it as one (for example, with the stage) and never offer it as the outcome.

When calling such a composite, take the knowledge in its settled reply as one constituent. Count `NotDone` as `NotDone`, and `Unknown`, a snapshot or a call still open as `Unknown`.

Run the Ball under the Durable profile unless a crash also clears the effects of its calls.

Report `Partial` or `NotAllDone` as such; do not collapse them into "failed". See `examples/trip`.

## Writing an adapter

Report once. Never retry. Report `NotApplied` only with evidence that nothing was applied (for example, connection refused before the request was written); otherwise report `Failed`. For Idempotent ports, send `invocation.callId` as the idempotency key. Check it with `ballarch.testkit.AdapterConformance`.

## Wiring

Register Balls, adapters and subscriptions in one composition root. Features depend on each other only through `-api` modules (R10).

## Other levels

- **Firmware (C).**
  - Write the decider as a pure `step()` in its own translation unit.
  - Keep state and the call ledger in one CRC-checked NVM record.
  - Record "possibly sent" before transmitting.
  - Check purity with `reference/c/tools/check_purity.sh`.
- **RTL.**
  - Each register has one writing process.
  - The next-state logic is one `always_comb` with defaults: no latch.
  - Outputs come from registered state only.
  - The send horizon is the bus handshake. On AXI, it is VALID.
  - Check with `make rules` in `reference/rtl`.

## Checks to run before finishing

Use the consuming project's build and tests. In the reference implementation, run from its Gradle root:

```sh
./gradlew build checkModuleBoundaries
```

This runs the architecture rules, runtime tests, examples and the knowledge-algebra check. For a changed effectful feature, use `Harness` + `SimPort` + `explore` when available:
- inject network faults and a crash;
- assert `h.assertInvariants()`, and `h.assertNoReplyLeaks()` where every accepted request should be answered before its deadline;
- check the feature's state, or its `Knowledge`, against the ground truth.

Reuse the project's equivalent tests when it uses another conforming runtime. State what was actually checked.

## Review checklist

- Effect class of each port justified in a comment or ADR.
- No `Unknown` treated as failure or retried.
- Every completion branch handles `Done`, `NotDone` and `Unknown`.
- No decider reads anything but its arguments.
- Claims about composite operations come from `Knowledge`, count calls still to be made as `Maybe`, and use only absorbing compensations; the operation's outcome is sent only once it is settled.
- Tests explore faults and crashes, not only the happy path.
