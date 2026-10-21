# FAQ

**Is this new?**
The mechanisms are not: single owners, pure steps, commit then dispatch, idempotency keys and sagas are established practice. Four things are new as far as the author could establish ([Core §12](../spec/ballarch-core-2.0.md#12-lineage-and-what-is-new-informative)):
- architectural boundaries in time, the commit horizon and the send horizon;
- outcomes chosen mechanically from a call's phase and its port's effect class;
- a knowledge algebra for composite operations, with composition closure;
- one rule set with checks at every level, from RTL to services.

The claim covers these elements, not general superiority. See the [benchmark](../benchmark/) for what they cost and gain in one scenario.

**Why "Ballarch"?**
"Ball" is the unit of ownership; "arch" stands for architecture. The project was called Pokeball up to Core 1.5 and was renamed because "Poké Ball" is a Nintendo trademark. Ballarch is not affiliated with Nintendo.

**Does it really apply to hardware?**
The rules describe owned state, atomic commit and outcomes of requests, and synchronous circuits have all three. [`reference/rtl/`](../reference/rtl/) is a SystemVerilog block whose registers, next-state logic and bus transactions follow the rules. Lint, Yosys structural checks and a fault-injecting bench check them. [`reference/c/`](../reference/c/) does the same for firmware with power loss. See [Ballarch at every level](interpretations.md).

**How is it different from Clean or Hexagonal Architecture?**
Those draw boundaries in space: which code may depend on which. Ballarch keeps that discipline (adapters at the edge, a composition root) and adds boundaries in time, plus rules about what a component may claim about effects. In the [benchmark](../benchmark/), an idiomatic Clean Architecture checkout double-charged or reported charged orders as failed under faults. The same layers hardened by hand were correct, but needed the fault handling written into the feature.

**Is it a framework?**
The kernel is about 170 lines of types with no dependencies. The reference runtime is about 1,000 lines. Other runtimes may implement the [specification](../spec/ballarch-core-2.0.md); conformance is defined in §9.

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
[Обзор на русском](ru/README.md) covers Core 2 in brief. The [previous published 1.5 documentation has a full Russian edition](https://github.com/4wl2d/Pokeball/tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e/docs/ru), under the former name.

**Can I use the code?**
The text is CC BY 4.0. The executable code has no software license yet; see [NOTICE.md](../NOTICE.md).
