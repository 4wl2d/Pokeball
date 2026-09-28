# Anti-patterns

Each entry names a mistake, the rule it breaks, and what catches it. "Caught by" lists only checks that exist in this repository.

| Anti-pattern | Why it is wrong | Rule | Caught by |
|---|---|---|---|
| **Reading the clock or a random generator in a decider** (`System.currentTimeMillis()`, `Instant.now()`, `Random`) | Decisions stop being reproducible, and tests and recovery diverge. | R2 | `deciderPurity` (architecture rule) |
| **I/O, sleeping or threads in a decider** | The runtime cannot commit what happened, so the effect escapes commit-before-dispatch. | R2, R5 | `deciderPurity` |
| **Mutable state in state or messages** (`var` properties, `MutableList`) | Another holder can change committed state behind the runtime's back. | R1 | `immutableValues` |
| **Top-level mutable variables shared by features** | A hidden writer and a hidden channel between features. | R1, R10 | `noGlobalMutableState` |
| **A feature importing another feature's implementation** | Couples to private types and bypasses the port. | R10 | `featuresUseOnlyPublicApis`, `checkModuleBoundaries` |
| **Marking a port `Idempotent` because retries "usually" work** | The runtime will retry, and the destination will apply the effect twice. | R7, R8 | Nothing mechanical: effect classes are declarations. The adapter conformance kit checks that an Idempotent adapter sends the call identity as its key. |
| **Retrying inside an adapter** | Retries become invisible to the runtime and multiply its own. A non-idempotent call can be applied several times. | R8 | Adapter conformance kit (C2) |
| **Reporting `NotApplied` for a timeout or a 5xx** | Creates a false `NotDone`: the business believes nothing happened when it may have. | R7 | Adapter conformance kit (C3); oracle R7 in simulation |
| **Treating `Unknown` as failure** (refund, retry, "try again" to the user) | May produce a duplicate charge or claim failure after an effect occurred. | R7 (application) | Your own exploration test (see the [tutorial](tutorial.md), §7) |
| **Ignoring the completion key** (acting on any completion) | A late or superseded completion changes the wrong state. | §8.3 | Your own tests; the runtime delivers each key once but cannot know which key matters |
| **A Ball that never replies to a request it accepted** | The caller waits until its deadline and gets `Unknown`. | R9 | `ReplyOverdue` trace event; oracle R9 |
| **Waiting for another Ball inside a decision** | Deadlock, or a decision that depends on uncommitted state. | R3, §3.4 | Impossible through the kernel API (no synchronous call exists) |
| **One Ball per table row with a cross-Ball invariant** | An invariant spread over several owners cannot be enforced in one decision. | R1, §8.1 | Review only |
| **One giant Ball for the whole application** | One sequential writer becomes the throughput limit. | §8.1, §10 | Review; load tests |
| **Using a timer completion as "time has come" without checking** | After a restart the timer can fire late; with a custom timer adapter, possibly early. | §2.4 | Review; exploration tests |
| **Publishing notices to a subscriber that can fail** | Quarantine blocks the notices, and the publisher's outbox fills until it is refused with `Overloaded`. | §10 | `Fault` trace; the outbox bound (R11) |
| **Claiming exactly-once delivery** | No profile provides it for external effects. | R13, §9.4 | Review |
