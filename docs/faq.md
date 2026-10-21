# FAQ

**Is this new?**
The mechanisms are not: single owners, pure steps, commit then dispatch, idempotency keys and sagas are established practice, and so are most of the ideas Ballarch builds on. As far as the author could establish, four things are new in where they are placed ([Core §12](../spec/ballarch-core-2.0.md#12-lineage-and-what-is-new-informative) cites the prior art for each):
- the commit horizon and the send horizon as boundaries that every implementation, at every level, must make observable. The instants themselves are known from transaction processing, outboxes, HTTP and durable-execution engines.
- one runtime rule, not per-transport or per-feature code, that chooses each call's outcome from its durable phase and its port's effect class. HTTP/2, gRPC and HTTP clients apply similar rules within one transport, and protobuf and Smithy already annotate operations as safe or idempotent; they decide retries, while Ballarch also derives the outcome and delivers it as a typed value.
- a knowledge algebra for composite operations that refines the heuristic-outcome merge of XA transaction managers, with composition closure for a composite that replies with its settled knowledge. Its soundness, and its exactness for independent calls, follow from known properties of Kleene's logic.
- one rule set whose call-protocol rules and knowledge algebra are checked at three levels (RTL, firmware, services). Nested composition is checked at one level, by an itinerary Flow that books two trip Flows; no reference composes different levels, such as firmware behind a service port, so composition across levels is not tested.

The claim covers these elements, not general superiority. See the [benchmark](../benchmark/) for what they cost and gain in one scenario.

**Why "Ballarch"?**
"Ball" is the unit of ownership; "arch" stands for architecture. The project was called Pokeball up to Core 1.5 and was renamed because "Poké Ball" is a Nintendo trademark. Ballarch is not affiliated with Nintendo.

**Does it really apply to hardware?**
The rules describe owned state, atomic commit and outcomes of requests, and synchronous circuits have all three. [`reference/rtl/`](../reference/rtl/) has two SystemVerilog blocks whose registers, next-state logic and bus transactions follow the rules: a command issuer, and a composite that keeps the knowledge of its two-step operation in a status register. Lint, Yosys structural checks and fault-injecting benches check the call-protocol rules and the algebra there. [`reference/c/`](../reference/c/) does the same for firmware with power loss. [Ballarch at every level](interpretations.md) also reads the rules for device drivers, GPUs, PLCs and automotive ECUs, without reference code.

**How is it different from Clean or Hexagonal Architecture?**
Those draw boundaries in space: which code may depend on which. Ballarch keeps that discipline (adapters at the edge, a composition root) and adds boundaries in time, plus rules about what a component may claim about effects. In the [benchmark](../benchmark/), an idiomatic Clean Architecture checkout double-charged or reported charged orders as failed under faults. The same layers hardened by hand were correct, but needed the fault handling written into the feature.

**Is it a framework?**
The kernel is about 280 lines of code (types and the knowledge algebra) with no dependencies. The reference runtime is about 1,100 lines, plus a file store of about 300. Other runtimes may implement the [specification](../spec/ballarch-core-2.0.md); conformance is defined in §9.

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

The reference runtime has two stores. `MemoryStore` simulates restarts in tests. `FileStore` keeps one file per instance and commits by writing a temporary file, forcing it to the device and renaming it over the previous one; a test kills a writing process repeatedly and checks that every record reads back whole. It has not been tested against power loss, has no replication and serves one runtime per directory, so it is not a production database.

**What happens when I deploy a new version while calls are open?**
Under the Durable profile the new code starts from records the old code committed ([Core §6.1](../spec/ballarch-core-2.0.md#61-evolution)).
- Each record carries the state version of the code that committed it. A Ball type that changes the shape of its state raises `stateVersion` and converts old states in `migrate`, a pure function.
- Each call keeps the effect class its port had when the call was committed. A call committed as Idempotent that may have been sent, on a port since rebound as NonIdempotent, is not sent again: it ends `Unknown(RetriesExhausted)`.
- Before it writes anything, the runtime refuses to start, with an `IncompatibleStoreException` that names each offending record, if the store holds a record it cannot read, a Ball type the composition no longer registers, a state version that cannot be migrated, an open call on a port that is no longer bound, or a notice owed to a removed subscription. To remove a port, a Ball type or a subscription, drain it first under a composition that still has it. A drained Ball type's records are then deleted with `Store.delete`.

Two things are not checked: a new decider that no longer handles completions of a port that is still bound (the instance is quarantined when such a completion arrives), and two runtimes owning one instance during a rolling deploy.

**Can a runtime in another language be checked against the rules?**
Yes, for the runtime rules that a trace can show. [Core §9.6](../spec/ballarch-core-2.0.md#96-conformance-trace) defines a conformance trace in JSON Lines ([format](../tools/TRACE-FORMAT.md)): the runtime's events, and the ground truth that a simulated environment records for each call. [`tools/oracles.py`](../tools/oracles.py), with Python's standard library only, checks the trace properties T3 and T5–T9, which correspond to R3 and R5–R9. The Kotlin reference writes 46 traces that must pass and 10 with a seeded violation that must fail, and its own oracles report the same messages.

**Why not use Temporal, Restate, Durable Functions, Akka or Orleans?**
If you already do, you may not need this. Those systems provide durable execution or persistent actors with more engineering behind them. Core 2 is a smaller contract that can be implemented inside an ordinary application, and its rules are stated so that they can be checked. See the [decision guide](decision-guide.md).

The difference shows in effects that cannot be repeated. Take a Temporal activity that charges a card through a provider without idempotency keys. The worker sends the charge, and then the activity's Start-To-Close timeout expires, for example because the worker crashed before it reported. Temporal retries activities by default, with unlimited attempts, so the charge may run again. With Maximum Attempts set to 1, the workflow receives a timeout failure instead, which does not say whether the card was charged; the workflow code has to treat it as unknown by itself. Temporal's documentation recommends making activities idempotent for this reason. Temporal does record which timeout ended an attempt, so a workflow can tell an attempt that never reached a worker (Schedule-To-Start) from one that may have run (Start-To-Close); mapping that to an outcome, and not retrying a non-idempotent activity, are left to workflow code and retry-policy configuration. In Ballarch the port is declared `NonIdempotent`, the composition refuses a retry policy for it, and the caller's `complete` receives `Unknown(DeadlineAfterSend)`, or `Unknown(RecoveredAfterCrash)` after a restart. Any claim about the order that contains the charge is derived from that value (R14), and the [payment example](../reference/kotlin/examples/shop/payments/src/main/kotlin/ballarch/examples/shop/payments/PaymentBall.kt) reconciles through a Safe status port. Other engines have their own defaults; check them in the same way.

**Why did the evaluation find it costs more code than a version hardened by hand?**
Because the protocol is explicit in the types: every state and every outcome is a case. A hand-written version that applies the specification's write-ahead status and reconciliation can be shorter and equally correct, as the benchmark's hardened version is. That version follows the procedure of [Core §8.4](../spec/ballarch-core-2.0.md#8-guidance-non-normative), so it shows that correctness is reachable without the runtime, not how an independent expert would harden the code. The architecture's bet is that such knowledge should not have to be re-applied for each feature. Whether that bet pays off in a team was not measured.

**Where did the Interaction/Nucleus/Resource roles of 1.5 go?**
They became the decider and the adapters. See the [migration guide](migration-from-1.5.md).

**Can a Ball call another Ball and wait for the answer?**
It can call. It cannot wait inside a decision: the answer arrives later as a completion. This rules out deadlocks and re-entrant decisions (Core §3.4).

**Is there a Russian version?**
[Обзор на русском](ru/README.md) covers Core 2 in brief. The [previous published 1.5 documentation has a full Russian edition](https://github.com/4wl2d/Pokeball/tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e/docs/ru), under the former name.

**Can I use the code?**
The text is CC BY 4.0. The executable code has no software license yet; see [NOTICE.md](../NOTICE.md).
