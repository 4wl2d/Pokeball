# Decision guide: when is Pokeball Core 2 worth it?

This guide reflects the historical prototype measurements summarized in [Core §10](../spec/pokeball-core-2.0.md#10-trade-offs-and-known-limitations), including their negative results. The comparison's full data and harness are outside this distribution. It gives no universal recommendation; it states which trade-offs were measured, and which were not.

## What you pay

The following was measured on three slices, all written by one author:

- **More feature code.** A local feature took 27 lines against 10 for a plain class. Payment capture took 151 lines against 123 for an expert's hand-rolled version with the same fault tolerance. Checkout took 255 against 112.
- **More concepts.** Feature code used 18–25 framework symbols, against 6–7 coroutine symbols.
- **Wider changes.** Adding or removing a component touched 8–11 files, against 2 in single-module code.
- **A thread hand-off per request** with the reference scheduler: about 39 µs round trip on a shared 4-vCPU VM. The runtime's own work is about 0.5 µs and 1 KB per request.
- **A runtime to depend on.** The reference runtime is about 1,000 lines. It is a research prototype without a production durable store.

## What you get

- **Fault handling that does not depend on each feature's author.**
  - The rules: no blind re-send of non-idempotent calls, commit before dispatch, one honest outcome per call, and recovery by call phase.
  - Where they live: in shared code that was model-checked (bounded) and explored under every fault schedule of the experiment.
  - How they compare: in the experiment, idiomatic coroutine code charged customers up to six times and reported failures for charged orders. The Core 2 versions had no such violation. Neither did the expert's hand-rolled versions, which needed expert knowledge applied per feature.
- **Explicit states for ambiguity.** `Unknown` is a type, not a timeout exception that each call site maps differently.
- **Testability.** A decider is a pure function. The same test kit runs a whole feature under simulated faults and crashes against ground truth (see the [tutorial](tutorial.md)).
- **Mechanical checks.** The architecture rules (ArchUnit), the module-boundary check, the runtime oracles and the adapter conformance kit.

## Use it when…

- features call external systems with effects (payments, bookings, emails, provisioning) whose ambiguous outcomes matter;
- several people or agents will write such features, and you cannot rely on each of them to hand-roll write-ahead records, reconciliation and recovery correctly;
- state must survive crashes and restarts, and you are willing to invest in a durable store, which the reference runtime lacks;
- you want failure behaviour to be reviewable and testable as data.

## Prefer something else when…

- **Features are local and synchronous.** A plain class or a reducer is shorter; the kernel adds ceremony and nothing else.
- **One expert owns a small number of effectful flows.** In the evaluation, the expert's hand-rolled code was shorter and equally correct.
- **You already run a durable execution engine** (Temporal, Restate, Durable Functions) or an actor system with persistence (Akka, Orleans). Compare its actual guarantees with the requirements of your application before adding another runtime contract.
- **Latency per request must stay in the low microseconds** on the reference scheduler.
- **Your changes mostly add or remove components.** The module structure makes those changes wider.

## Not yet known

These were not measured. Do not assume them either way:

- whether the explicit structure speeds up review, onboarding or defect finding in real teams;
- how the architecture behaves with a real durable store, over a network, or under production load;
- whether an independent implementer of the baselines would reach the same results.
