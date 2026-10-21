# FAQ

**Is this new?**
The mechanisms are not: single owners, pure steps, commit then dispatch, idempotency keys and sagas are established practice, and so are most of the ideas Ballarch builds on. As far as the author could establish, four things are new in where they are placed ([Core §12](../spec/ballarch-core-2.0.md#12-lineage-and-what-is-new-informative) cites the prior art for each):
- the commit horizon and the send horizon as boundaries that every implementation, at every level, must make observable. The instants themselves are known from transaction processing, outboxes, HTTP and durable-execution engines.
- one runtime rule, not per-transport or per-feature code, that chooses each call's outcome from its durable phase and its port's effect class. HTTP/2, gRPC and HTTP clients apply similar rules within one transport; protobuf and Smithy already annotate operations as safe or idempotent, to choose HTTP methods or decide retries; and some durable-execution engines can be configured, per step or scope, not to run a step again after a crash (see "Why not use Temporal …?" below). Ballarch derives the outcome from a class declared on the port and delivers it as a typed value.
- a knowledge algebra for composite operations that refines the heuristic-outcome merge of XA transaction managers, with composition closure for a composite that replies with its settled knowledge, given honest adapters, absorbing compensations and a durable call ledger. Its soundness, and its coordinate-wise exactness for independent calls, follow from known properties of Kleene's logic.
- one rule set whose call-protocol rules and knowledge algebra are checked at three levels (RTL, firmware, services). Nested composition is checked at one level, by an itinerary Flow that books two trip Flows; no reference composes different levels, such as firmware behind a service port, so composition across levels is not tested.

The claim covers these elements, not general superiority. See the [benchmark](../benchmark/) for what they cost and gain in one scenario.

**Why "Ballarch"?**
"Ball" is the unit of ownership; "arch" stands for architecture. The project was called Pokeball up to Core 1.5 and was renamed because "Poké Ball" is a Nintendo trademark. Ballarch is not affiliated with Nintendo.

**Does it really apply to hardware?**
The rules describe owned state, atomic commit and outcomes of requests, and synchronous circuits have all three. [`reference/rtl/`](../reference/rtl/) has two SystemVerilog blocks whose registers, next-state logic and bus transactions follow the rules: a command issuer, and a composite that keeps the knowledge of its two-step operation in a status register. The composite is Transient, so its register covers only operations since its last reset. Lint, Yosys structural checks and fault-injecting benches check the call-protocol rules and the algebra there. [`reference/c/`](../reference/c/) does the same for firmware with power loss. [Ballarch at every level](interpretations.md) also reads the rules for device drivers, GPUs, PLCs and automotive ECUs, without reference code.

**How is it different from Clean or Hexagonal Architecture?**
Those draw boundaries in space: which code may depend on which. Ballarch keeps that discipline (adapters at the edge, a composition root) and adds boundaries in time, plus rules about what a component may claim about effects. In the [benchmark](../benchmark/), a careful layered checkout, hardened by hand, was as correct as Ballarch while the provider's delivery horizon held, but its feature code holds the fault handling: a write-ahead status, five exception handlers and the reconciliation. A naive layered checkout that treats a timeout as a failed payment double-charged or reported charged orders as failed; that failure policy was the author's choice, shared by the naive versions of all four styles, so it measures the policy, not the layers.

**Is it a framework?**
The kernel is about 280 lines of code (types and the knowledge algebra) with no dependencies. The reference runtime is about 1,100 lines, plus a file store of about 300. Other runtimes may implement the [specification](../spec/ballarch-core-2.0.md); conformance is defined in §9.

**Does it give exactly-once effects?**
No. It guarantees that a non-idempotent call is sent at most once, and that the application is told honestly when the runtime cannot know whether the call took effect (`Unknown`). Idempotent destinations, keyed by the call identity, make most `Unknown` outcomes disappear after retries, provided the adapter ends each attempt before the call's deadline ([Core §8.4](../spec/ballarch-core-2.0.md#8-guidance-non-normative)).

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

The reference runtime has two stores. `MemoryStore` simulates restarts in tests. `FileStore` keeps one file per instance and commits by writing a temporary file, forcing it to the device and renaming it over the previous one; a test kills a writing process repeatedly and checks that every record reads back whole. It has not been tested against power loss, has no replication and serves one runtime per directory, so it is not a production database.

**What happens when I deploy a new version while calls are open?**
Under the Durable profile the new code starts from records the old code committed ([Core §6.1](../spec/ballarch-core-2.0.md#61-evolution)). The Kotlin reference runtime implements the following; the firmware reference does not ([Core §10](../spec/ballarch-core-2.0.md#10-trade-offs-and-known-limitations)).
- Each record carries the state version of the code that committed it. A Ball type that changes the shape of its state raises `stateVersion` and converts old states in `migrate`, a pure function.
- Each call keeps the effect class its port had when the call was committed. A call committed as Idempotent that may have been sent, on a port since rebound as NonIdempotent, is not sent again: it ends `Unknown(RetriesExhausted)`.
- Before it writes anything, the runtime refuses to start, with an `IncompatibleStoreException` that names each offending record, if the store holds a record it cannot read, a Ball type the composition no longer registers, a state version that cannot be migrated, an open call on a port that is no longer bound, or a notice owed to a removed subscription. To remove a port, a Ball type or a subscription, drain it first under a composition that still has it. A drained Ball type's records are then deleted with `Store.delete`.

Two things are not checked: a new decider that no longer handles completions of a port that is still bound (the instance is quarantined when such a completion arrives), and two runtimes owning one instance during a rolling deploy.

**Can a runtime in another language be checked against the rules?**
Yes, for the runtime rules that a trace can show. [Core §9.6](../spec/ballarch-core-2.0.md#96-conformance-trace) defines a conformance trace in JSON Lines ([format](../tools/TRACE-FORMAT.md)): the runtime's events, and the ground truth that a simulated environment records for each call. [`tools/oracles.py`](../tools/oracles.py), with Python's standard library only, checks the trace properties T3 and T5–T9, which correspond to R3 and R5–R9. The Kotlin build writes 46 traces from explorations and clean runs, which must pass, and 11 with a seeded violation, which must fail. The Kotlin reference's own oracles implement the same properties and report the same messages, with `R` in place of `T`.

**Why not use Temporal, Restate, Durable Functions, Akka or Orleans?**
If you already do, you may not need this. Those systems provide durable execution or persistent actors with more engineering behind them. Core 2 is a smaller contract that can be implemented inside an ordinary application, and its rules are stated so that they can be checked. See the [decision guide](decision-guide.md).

The difference shows in effects that cannot be repeated. Take a Temporal activity that charges a card through a provider without idempotency keys. The worker sends the charge, and then the activity's Start-To-Close timeout expires, for example because the worker crashed before it reported. Temporal retries activities by default, with unlimited attempts, so the charge may run again. With Maximum Attempts set to 1, the workflow receives a timeout failure instead, which does not say whether the card was charged; the workflow code has to treat it as unknown by itself. Temporal's documentation recommends making activities idempotent for this reason. Temporal does record which timeout ended an attempt, so a workflow can tell an attempt that never reached a worker (Schedule-To-Start) from one that may have run (Start-To-Close); mapping that to an outcome, and not retrying a non-idempotent activity, are left to workflow code and retry-policy configuration. In Ballarch the port is declared `NonIdempotent`, the composition refuses a retry policy for it, and the caller's `complete` receives `Unknown(DeadlineAfterSend)`, or `Unknown(RecoveredAfterCrash)` after a restart. Any claim about the order that contains the charge is derived from that value (R14), and the [payment example](../reference/kotlin/examples/shop/payments/src/main/kotlin/ballarch/examples/shop/payments/PaymentBall.kt) reconciles through a Safe status port.

Newer engines persist the send horizon itself. In AWS Lambda durable functions, a step declared `AT_MOST_ONCE_PER_RETRY` is checkpointed before its code runs; if it is interrupted after that checkpoint, it runs again only on a new attempt of its retry strategy, and otherwise ends in an interruption error ([SDK documentation](https://github.com/aws/aws-durable-execution-sdk-java/blob/main/docs/core/steps.md)). Golem's [documentation](https://learn.golem.cloud/develop/durability) says that, with its idempotence mode switched off, it fails the worker instead of repeating a request whose delivery it cannot determine; Golem's main branch removed that global mode in October 2026. Both are chosen per step or scope by configuration, and both report an error. Ballarch derives the outcome from the class declared on the port, refuses a retry policy for a `NonIdempotent` port when the composition is built, and delivers `Unknown` as a typed completion that composes. Other engines have their own defaults; check them in the same way.

**Is Ballarch more correct than careful hand-written code?**
No. In the benchmark, careful versions in four styles and Ballarch violated no requirement in any run or swept crash point while the provider's delivery horizon held, and all five failed the same way beyond it without idempotency keys. With keys, Ballarch had no violating run beyond the horizon, where the careful versions had 3.3% of runs with a crash, because its runtime sends again with the key after a restart and they reconcile by status; careful versions that send again with the key were not measured. That holds for the sampled runs: in the sweep beyond the horizon, Ballarch had a violating crash point in 1 of 1,000 seeds (5 of 28,838 points), each careful version in 37 or 38. It also needs an adapter that ends each attempt. With one shaped like the reference HTTP adapter, Ballarch reported charged orders as failed in 4.2% of crash-free runs beyond the horizon. With keys, a naive layered design whose same-key retries end in `NeedsAttention` instead of `Failed` also had no violation in any cell, at 0.394 operator hand-offs per order under Stress against 0.221. What Ballarch changes is where the care lives: the careful versions write the classification of outcomes, the write-ahead status and the recovery into each feature, and Ballarch's runtime provides them. In both, the feature writes the reconciliation.

**Why did the evaluation find it costs more code than careful hand-written versions?**
Because the protocol is explicit in the types: every state and every outcome is a case, and the runtime's concepts appear as framework types. The benchmark's careful versions took 153–231 SLOC against Ballarch's 279, used 5–8 framework types against 40, and needed smaller changes in total for four change requests (122–173 code lines against 179). They follow the reconciliation procedure of [Core §8.4](../spec/ballarch-core-2.0.md#8-guidance-non-normative) and were written by Ballarch's author after the first results were known, so they show that correctness is reachable without the runtime, not how an independent expert would write it. The architecture's bet is that such knowledge should not have to be re-applied for each feature. Whether that bet pays off in a team was not measured.

**Where did the Interaction/Nucleus/Resource roles of 1.5 go?**
They became the decider and the adapters. See the [migration guide](migration-from-1.5.md).

**Can a Ball call another Ball and wait for the answer?**
It can call. It cannot wait inside a decision: the answer arrives later as a completion. This rules out deadlocks and re-entrant decisions (Core §3.4).

**Is there a Russian version?**
[Обзор на русском](ru/README.md) covers Core 2 in brief. The [previous published 1.5 documentation has a full Russian edition](https://github.com/4wl2d/Pokeball/tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e/docs/ru), under the former name.

**Can I use the code?**
The text is CC BY 4.0. The executable code has no software license yet; see [NOTICE.md](../NOTICE.md).
