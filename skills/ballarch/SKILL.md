---
name: ballarch
description: Write, change or review a component on Ballarch Core 2 (Balls, deciders, ports, effect classes, composition, knowledge of composite operations, changes to deployed Balls), in Kotlin services, C firmware or RTL. Use when code imports ballarch.kernel or ballarch.runtime, includes ballarch.h, follows reference/rtl, or when asked to add a stateful feature that calls external systems or devices in this repository's style.
---

# Ballarch Core 2 component work

## Before writing code

1. Work in the consuming project and use its existing kernel, runtime and tests. A review request authorizes findings only; edit or publish only when requested.
2. Check the fit. Ballarch moves fault handling out of each feature into the runtime and the rules; it does not make a feature more correct than careful hand-written handling of the same policy. It costs more code, more framework types, larger changes in total and a thread hand-off per request (Core §10). For stateless code, or CRUD without external effects, use plain code.
3. Name the owner of each fact: one Ball type per consistency boundary.
4. Locate the two horizons of every interaction:
   - the **commit horizon**: the atomic commit of the decision that requests the effect;
   - the **send horizon**: the first moment the request may have left.

   Nothing may leave before the commit horizon. Before the send horizon, `NotDone` is provable; after it, only `Done`, `NotDone` with evidence of non-application (for a Safe port, the absence of any effect), or `Unknown`.
5. For every external call, choose the port's effect class honestly:
   - `Safe`: no effect;
   - `Idempotent`: the destination deduplicates by the call identity;
   - `NonIdempotent`: otherwise.

   If unsure, choose `NonIdempotent`.

## Writing the Ball

- `decide`, `complete` and `observe` must be pure. Take time only from `ctx.now` and keys only from `ctx.newKey()`. No I/O, clocks, randomness, globals or threads (R2).
- Keep every stage and every outcome as an explicit state, including "may have happened" (`Unknown`).
- Store the key of each call whose completion matters, and ignore completions with other keys.
- Never re-send after `Unknown` on a NonIdempotent port. Reconcile instead: wait past the destination's horizon, then query a Safe status port. That status is final only if the destination keeps its horizon; prefer an idempotency key where the destination offers one. A new call after `NotDone` is allowed, because `NotDone` proves nothing happened.
- Reply to every accepted request, now or later (R9). Wait with `Timer.after`, and treat a timer completion as a wake-up.
- Make state and messages immutable data (R1).

## Composite operations (R14)

When a Ball reports whether an operation made of several calls took effect, derive the claim with `ballarch.kernel.Knowledge`. Count every call for the operation's final effect:
- a call counts as `No` only once it completed `NotDone`, or once the Ball has not made it and can no longer make it for this operation (for example, after the operation is settled);
- a requested call without a completion counts as `Maybe`, even if it may not have been sent yet, and so does a call the Ball may still make, including a compensation;
- `Done` with a refusal inside counts as `No`; pass an `effective` predicate to `effect` or `knowledge`.

Count a compensation only if it is absorbing: the destination fences the reference, or the original request's delivery horizon has passed. Combine repeated attempts at one effect with `or`, and repeated compensation attempts too.

Answer the request that starts the operation only once the operation is settled (no call open, none still to be made): accept it with `acceptLater`, and send a `Reply` carrying the settled knowledge. Give callers a deadline that covers the operation's worst case. A status given earlier that counts calls not yet made as `No` is a snapshot: mark it as one (for example, with the stage) and never offer it as the outcome.

When calling such a composite, take the knowledge in its settled reply as one constituent with `outcome.reportedKnowledge(onlyRequest, settled)`. It counts `Unknown`, a reply that does not describe the settled operation, and a call still open as `Unknown`. Pass `onlyRequest = true` only if no other request, from you or anyone else, could start the same operation; only then does `NotDone` count as `NotDone`. Never add two replies about the same operation, such as the replies to a request and to its repeat with the same key: keep one, a settled one if you have one.

Run the Ball under the Durable profile unless a crash also clears the effects of its calls.

Report `Partial` or `NotAllDone` as such; do not collapse them into "failed". See `examples/trip` for a composite, and `examples/itinerary` for a composite of composites: it books two trips through the trip's port and adds their settled replies with `+`.

## Writing an adapter

Report once. Never retry. Report `NotApplied` only with evidence that nothing was applied (for example, connection refused before the request was written); otherwise report `Failed`. For Idempotent ports, send `invocation.callId` as the idempotency key. The Kotlin reference runtime has no timeout per attempt, so on a port with a retry policy end each attempt yourself: report `Failed` after an attempt timeout shorter than the call's deadline, and ignore a later answer. Otherwise a lost answer holds the first attempt until the deadline, the call ends `Unknown`, and the retry policy never acts. Check the adapter with `ballarch.testkit.AdapterConformance`.

## Wiring

Register Balls, adapters and subscriptions in one composition root. Features depend on each other only through `-api` modules (R10).

## Changing a deployed Ball (Durable profile, Core §6.1)

A Durable runtime restarts from records that the old code committed, with obligations still open.

- When committed states can no longer be read as the new state type, raise the Ball's `stateVersion` and override `migrate(fromVersion, state)`, which must be pure. Keep the old classes readable by the store's codec; with the default `JavaSerializationCodec`, give every stored class a fixed `serialVersionUID`.
- Changing a port's effect class does not change its open calls: each call keeps the class recorded when it was committed. After a restart, a call recorded as Idempotent that may have been sent is not sent again if its port is now NonIdempotent; it ends `Unknown`.
- Before removing a port, a Ball type or a subscription, drain its open calls, records or notices under a composition that still has it. Otherwise the runtime refuses to start with `IncompatibleStoreException`, which names each offending record. Retire a drained Ball type by deleting its records with `Store.delete`.
- Keep handling completions of every port the old code may have open calls on. A completion the new decider does not handle quarantines the instance, and the start-up check does not catch it.
- Never let two runtimes own the same instances at once. A rolling deploy needs a handover.

## Other levels

- **Firmware (C).**
  - Write the decider as a pure `step()` in its own translation unit.
  - Keep state and the call ledger in one CRC-checked NVM record.
  - Record "possibly sent" before transmitting.
  - Check purity with `reference/c/tools/check_purity.sh`; it is necessary, not sufficient.
  - For a composite, derive claims with `ballarch_knowledge.h` (`ba_claim`, `ba_net`, `ba_k_plus`); see `examples/transfer`.
  - The firmware runtime does not implement Core §6.1: it stores no effect class with a call and classifies open calls by the current port table. Apply an update that changes a port's effect class or the port table with an empty call ledger, or bump the Ball's `schema` so that an old record quarantines the device instead of being misread. An update that changes the record layout must migrate the record or start with an empty call ledger and erased slots.
  - Run `make` in `reference/c`.
- **RTL.**
  - Each register has one writing process.
  - The next-state logic is one `always_comb` with defaults: no latch.
  - Outputs come from registered state only.
  - The send horizon is the bus handshake. On AXI, it is VALID.
  - For a composite, keep the knowledge in a status register, two bits per coordinate; see `rtl/ballarch_composite.sv`. Registers are Transient: after a reset the status covers only operations since that reset.
  - Run `make` in `reference/rtl`; `make rules` runs the structural checks alone.

## Checks to run before finishing

Use the consuming project's build and tests. In the reference implementation, run from its Gradle root:

```sh
./gradlew build checkModuleBoundaries
```

This runs the architecture rules, runtime tests (including the evolution tests), examples and the knowledge-algebra check. Then check the conformance traces the build writes, from the repository root:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/oracles.py reference/kotlin/ballarch-runtime/build/conformance-traces/*.jsonl
python3 tools/oracles.py --expect-violations reference/kotlin/ballarch-runtime/build/conformance-traces/seeded/*.jsonl
```

For a changed effectful feature, use `Harness` + `SimPort` + `explore` when available:
- inject network faults and a crash;
- assert `h.assertInvariants()` (the trace properties T3 and T5–T9 of Core §9.6), and `h.assertNoReplyLeaks()` where every accepted request should be answered before its deadline;
- check the feature's state, or its `Knowledge`, against the ground truth;
- write a run with `h.writeTrace(path)` when a check outside the JVM should judge it.

For a changed state type or composition, restart a Durable runtime on a store written by the old code, as `EvolutionTest` does.

When the project uses another runtime, have it write traces in the format of `tools/TRACE-FORMAT.md` and check them with `tools/oracles.py`, or reuse its equivalent tests. State what was actually checked.

## Review checklist

- Effect class of each port justified in a comment or ADR.
- No `Unknown` treated as failure or retried.
- Every completion branch handles `Done`, `NotDone` and `Unknown`.
- No decider reads anything but its arguments.
- Claims about composite operations come from `Knowledge`, count calls still to be made as `Maybe`, and use only absorbing compensations; the operation's outcome is sent only once it is settled.
- A change to a deployed Ball raises `stateVersion` with a `migrate` when the state shape changes, and drains a port, Ball type or subscription before removing it.
- Tests explore faults and crashes, not only the happy path.
