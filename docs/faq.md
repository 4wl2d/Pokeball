# FAQ

**Is this new?**
No single mechanism is. Core 2 combines known mechanisms in a small contract with executable checks. This distribution makes no claim of scientific novelty or general superiority.

**Is it a framework?**
The kernel is about 170 lines of types with no dependencies. The reference runtime is about 1,000 lines. Other runtimes may implement the [specification](../spec/pokeball-core-2.0.md); conformance is defined in §9.

**Does it give exactly-once effects?**
No. It guarantees that a non-idempotent call is sent at most once, and that the application is told honestly when the runtime cannot know whether the call took effect (`Unknown`). Idempotent destinations, keyed by the call identity, make most `Unknown` outcomes disappear after retries.

**Why is `Unknown` not an exception?**
Because it is not exceptional. After a timeout, a crash, or an error after the request was written, not knowing is the correct state. Making it a case of `Outcome` forces the code to decide what to do with it.

**What happens when the process crashes?**
It depends on the profile.
- **Transient:** everything is lost together, and no half-applied decision remains.
- **Durable:** the runtime restarts from committed records:
  - calls that were never sent are sent;
  - repeatable calls are retried within their budget;
  - non-idempotent calls that may have been sent end `Unknown(RecoveredAfterCrash)`;
  - timers are re-armed to their due time;
  - notices are redelivered.

The reference runtime's durable store is an in-memory stand-in used to simulate restarts. A production durable store is not implemented.

**Why not use Temporal, Restate, Durable Functions, Akka or Orleans?**
If you already do, you may not need this. Those systems provide durable execution or persistent actors with more engineering behind them. Core 2 is a smaller contract that can be implemented inside an ordinary application, and its rules are stated so that they can be checked. See the [decision guide](decision-guide.md).

**Why did the evaluation find it costs more code than an expert's version?**
Because the protocol is explicit in the types: every state and every outcome is a case. An expert who knows the failure modes can write a shorter, equally correct version by hand. The architecture's bet is that such knowledge should not have to be re-applied for each feature. Whether that bet pays off in a team was not measured.

**Where did the Interaction/Nucleus/Resource roles of 1.5 go?**
They became the decider and the adapters. See the [migration guide](migration-from-1.5.md).

**Can a Ball call another Ball and wait for the answer?**
It can call. It cannot wait inside a decision: the answer arrives later as a completion. This rules out deadlocks and re-entrant decisions (Core §3.4).

**Is there a Russian version?**
The [previous published 1.5 documentation has a Russian edition](https://github.com/4wl2d/Pokeball/tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e/docs/ru). It describes Core 1.5; this Core 2 distribution currently has no Russian edition.

**Can I use the code?**
The text is CC BY 4.0. The executable code has no software license yet; see [NOTICE.md](../NOTICE.md).
