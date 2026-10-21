# Anti-patterns

Each entry names a mistake, the rule it breaks, and what catches it. "Caught by" lists only checks that exist in this repository.

| Anti-pattern | Why it is wrong | Rule | Caught by |
|---|---|---|---|
| **Reading the clock or a random generator in a decider** (`System.currentTimeMillis()`, `Instant.now()`, `Random`) | Decisions stop being reproducible, and tests and recovery diverge. | R2 | `deciderPurity` (architecture rule) |
| **I/O, sleeping or threads in a decider** | The runtime cannot commit what happened, so the effect escapes commit-before-dispatch. | R2, R5 | `deciderPurity` |
| **Mutable state in state or messages** (`var` properties, `MutableList`) | Another holder can change committed state behind the runtime's back. | R1 | `immutableValues` |
| **Top-level mutable variables shared by features** | A hidden writer and a hidden channel between features. | R1, R10 | `noGlobalMutableState` |
| **A feature importing another feature's implementation** | Couples to private types and bypasses the port. | R10 | `featuresUseOnlyPublicApis`, `checkModuleBoundaries` |
| **Marking a port `Idempotent` because retries "usually" work, or because the request only sets a value** (a PUT, a register write) | The runtime will retry, and the destination will apply the effect twice, or apply a late attempt after a later request. Idempotent requires that the destination deduplicate by the call's identity (Core §2). | R7, R8 | Nothing mechanical: effect classes are declarations. The adapter conformance kit checks that an Idempotent adapter sends the call identity as its key. |
| **Retrying inside an adapter** | Retries become invisible to the runtime and multiply its own. A non-idempotent call can be applied several times. | R8 | Adapter conformance kit (C2) |
| **Reporting `NotApplied` for a timeout or a 5xx** | Creates a false `NotDone`: the business believes nothing happened when it may have. | R7 | Adapter conformance kit (C3); oracle R7 in simulation |
| **Treating a transport acknowledgment as `Done`** (a returned `write()`, a TCP acknowledgment, a NIC transmit completion, a broker's publish confirm) | It shows that bytes left or reached the next hop, not that the destination processed the request, which is what `Done` asserts. | §2.2, §9.3 | Review |
| **Treating `Unknown` as failure** (refund, retry, "try again" to the user) | May produce a duplicate charge or claim failure after an effect occurred. | R14 for composites; §8.4 for a single call | Your own exploration test (see the [tutorial](tutorial.md), §7) |
| **Ignoring the completion key** (acting on any completion) | A late or superseded completion changes the wrong state. | §8.3 | Your own tests; the runtime delivers each key once but cannot know which key matters |
| **A Ball that never replies to a request it accepted** | The caller waits until its deadline and gets `Unknown`. | R9 | `ReplyOverdue` trace event; `Harness.assertNoReplyLeaks()` (the oracles check only the at-most-one-reply half of R9) |
| **Waiting for another Ball inside a decision** | Deadlock, or a decision that depends on uncommitted state. | R3, §3.4 | Impossible through the kernel API (no synchronous call exists) |
| **One Ball per table row with a cross-Ball invariant** | An invariant spread over several owners cannot be enforced in one decision. | R1, §8.1 | Review only |
| **One giant Ball for the whole application** | One sequential writer becomes the throughput limit. | §8.1, §10 | Review; load tests |
| **Using a timer completion as "time has come" without checking** | After a restart the timer can fire late; with a custom timer adapter, possibly early. | §2.4 | Review; exploration tests |
| **Publishing notices to a subscriber that can fail** | Quarantine blocks the notices, and the publisher's outbox fills until it is refused with `Overloaded`. | §10 | `Fault` trace; the outbox bound (R11) |
| **Claiming exactly-once delivery** | No profile provides it for external effects. | R13, §9.5 | Review |
| **Reporting a workflow as "failed" after one of its steps ended `Unknown` or took effect** | The customer is told nothing happened while a charge or booking exists. | R14 | Your own exploration test against ground truth; the trip example shows one |
| **Counting a compensation that does not fence the original request** | A late delivery of the original request can take effect after the "undo". | R14, §4.2 | Review: name the fence (for example a Try-Confirm/Cancel framework's record that blocks a try arriving after its cancel) or the horizon in a comment |
| **Answering for a composite before it is settled, with calls not yet made counted as `No`** | The answer says an effect did not happen although the Ball may still cause it; a caller one level up keeps that false outcome, because a completion never changes. | R14, §7.4 | Review; your own exploration test that compares each reply with the ground truth once the operation is settled, not at the time of the reply |
| **Exposing a composite's partial effect as a plain `NotDone` or `Done`** | The caller one level up loses the fact that something, but not everything, happened. | §7.4 | Review; return `Knowledge` in the reply |
| **Inferring a latch in RTL next-state logic, or driving one register from two processes** | Hidden or contested state: the clock edge no longer commits one owner's decision. | R1, R2 | Yosys `check -assert` in [`reference/rtl`](../reference/rtl/) |
| **A global or static variable in a firmware decider** | State that no NVM commit covers; it diverges after power loss. | R1, R2 | `tools/check_purity.sh` in [`reference/c`](../reference/c/) |
