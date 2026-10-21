# Decision guide: when is Ballarch Core 2 worth it?

This guide summarises what the reproducible [benchmark](../benchmark/) of this distribution measured, including the negative results. It gives no universal recommendation. It states which trade-offs were measured, and which were not.

The benchmark implements one checkout nine times: in four styles (layered, actors, MVU, outbox plus saga), each in a *naive* version and in a *careful* one written with the style's own tools, and in Ballarch. One author wrote all nine ([results](../benchmark/results/RESULTS.md), [method and threats to validity](../benchmark/README.md)). The benchmark is one small scenario family; its fault mix and the provider's delivery horizon are assumptions; it was not pre-registered. The careful versions are as correct as Ballarch while the provider's delivery horizon holds, so the comparisons below are with them.

## What you pay

- **More feature code.** Ballarch took 279 SLOC against 153 (MVU) to 231 (outbox + saga) for the careful versions, 21% more than the largest. The naive versions took 96–184.
- **More concepts.** The feature code used 40 framework types against 5–8.
- **More classes, with lower cohesion.** 35 named classes against 20–24. Cohesion was lower than in three of the four careful versions by both measures: mean CAMC 0.40 against 0.37–0.67, and mean LCOM4 1.60 (2 of 5 measured classes at 1) against 1.00–1.67. LCOM4 counts a class as split when a method uses none of its fields, as a decider that keeps its state in a parameter does. That does not explain the CAMC result, which is based on parameter types.
- **Higher total complexity.** Cyclomatic complexity, counted on bytecode as JaCoCo defines it, was 80 in total against 49–62. Per function it was lower than in three of the four (mean 2.50 against 2.23–4.77, maximum 8 against 7–31).
- **Larger changes.** Four change requests were applied to every design: refund a cancelled order, award loyalty points through a second service without idempotency keys, fail over to a second payment provider, and remove the inventory. Over all four, Ballarch's patches were the largest of the five reconciling designs (the careful versions and Ballarch) by every measure: 179 code lines against 122–173, and 3,008 tokens against 1,527–1,954.
  - Refund: 73 code lines against 34–43, the largest. Ballarch's checkout and payment Balls each had to accept a second kind of request.
  - Loyalty points: 50 code lines against 64–68, the smallest by lines, but fourth of five with import lines (84 against 71–89) and by tokens (899 against 633–974).
  - Failover: 18 code lines against 11–16, the largest.
  - Remove the inventory: 38 code lines against 8–48, within the range; by tokens the largest (720 against 109–584).

  Ballarch touched 2–4 files per change request, within the careful versions' 2–5, and more top-level units than any of them in three of the four. After every change, all five still violated no requirement in any Stress or Mild run or at any crash point swept under those profiles. Beyond the horizon, each violated a requirement in 153–335 of 4,000 runs.
- **A thread hand-off per request.** A request through the reference runtime on its production thread scheduler, with an in-memory store, took a median of 46 µs (99th percentile 121 µs), against 0.04 µs for a direct call of the same decider. The runtime's own work, run on the caller's thread, took a median of 0.68 µs (an upper bound, because the JIT compiler had not settled) and allocated about 1,144 bytes per request. With the durable `FileStore`, which forces every commit to the device, the median was 1.25 ms. These figures are from one machine, a 4-processor Linux VM with OpenJDK 21 (three rounds of 100,000 requests each, 2,000 with the `FileStore`, after a warm-up), and will differ on others.

An earlier, unpublished prototype comparison also measured change width and overhead. Its data is not distributed, so it cannot be checked; the change-impact and overhead measurements above replace it.

Also:

- **A runtime to depend on.** The reference runtime is about 1,100 lines. It is a research prototype; its file store is single-node and has no replication.
- **Adapters that end their attempts.** The reference runtime has no timeout per attempt, so an adapter must end each attempt itself for runtime retries to cover a lost answer ([Core §8.4](../spec/ballarch-core-2.0.md#8-guidance-non-normative)). In the benchmark's scenario B, Ballarch with an adapter shaped like the reference HTTP adapter, whose attempt ends at the call's deadline, sent 0.98 charge requests per order under Stress against 1.69, and reported charged orders as failed in 4.2% of crash-free runs beyond the horizon, where the adapter with a 1 s attempt timeout had none.
- **Deploys that respect open obligations.** Under the Durable profile, a port, a Ball type or a subscription that still has open calls, records or undelivered notices must be drained before a deploy removes it; otherwise the Kotlin reference runtime refuses to start and names each record ([Core §6.1](../spec/ballarch-core-2.0.md#61-evolution)). A Ball type that changes its state's shape needs a version and a migration. A rolling deploy needs a handover that keeps two runtimes from owning one instance, which the runtime does not provide. The firmware reference does not implement these checks ([Core §10](../spec/ballarch-core-2.0.md#10-trade-offs-and-known-limitations)).

## What you get

- **The correctness of careful code, with most of the care moved out of each feature.** In the benchmark:
  - the four naive versions share one policy chosen by the author: a timeout or I/O error means the payment failed. They violated a requirement in 73.4% (Stress) and 11.7% (Mild) of crash-free runs, as that policy and the fault mix predict, and in 91.9% and 94.3% of runs with one crash, identically run for run. These rates measure the policy, not the styles;
  - the careful versions and Ballarch violated no requirement in any Stress or Mild run, with or without a crash, in either scenario, or at any crash point swept under those two profiles. Without crashes all five reached the same outcomes, including 0.307 `NeedsAttention` per order under Stress. Ballarch is as correct as the careful versions, not more;
  - beyond the provider's declared delivery horizon, all five reported charged orders as failed in 4.2% of crash-free runs and 3.5% of runs with a crash, without idempotency keys. With keys and the same retry budget, Ballarch had no violation in any cell, and the careful versions had violations in 3.3% of runs with a crash, because after a restart they reconcile by status where Ballarch's Durable runtime sends again with the same key. In the sweep beyond the horizon, Ballarch had a violating crash point in 1 of 1,000 seeds, each careful version in 37 or 38. Ballarch's result needs an adapter that ends each attempt (see "Adapters that end their attempts" above). Careful versions that also send again with the key were not measured. A naive layered design whose same-key retries end in `NeedsAttention` instead of `Failed` also had no violation in any cell or swept crash point, at 0.394 operator hand-offs per order under Stress without a crash, against 0.221 for the careful versions and Ballarch;
  - the careful versions write the classification of outcomes, a write-ahead status, the recovery and the reconciliation into the feature, with 3–5 exception handlers each. In Ballarch the runtime provides the first three, and the feature writes the reconciliation as `Outcome` branches, with no exception handler. A seeded bug in what the runtime reads back after a restart was detected only in runs with a crash, as were the same kind of bugs in the careful versions' own recovery code.
- **Explicit states for ambiguity.** `Unknown` is a type, not a timeout exception that each call site maps differently.
- **Honest composites.** The knowledge algebra (Core §4.2) keeps a workflow from reporting "failed" after a partial or uncertain effect, provided it counts every call it may still make as unknown and replies once it is settled (R14, §7.4). In the [trip example](../reference/kotlin/examples/trip/), a two-valued status was wrong in 1,190 of 6,435 settled runs and a claim derived with the algebra in none. A status with a third value, "needs attention", was never wrong either; the algebra refines that value, and its claims compose. The [itinerary example](../reference/kotlin/examples/itinerary/) nests two such workflows in a third and stays honest. The benchmark's checkout has no composite operation, so it does not measure this.
- **Pure code and low static coupling.** 89% of Ballarch's feature code was in pure classes, against 28–66% for the careful versions. Its propagation cost was the lowest of the nine designs per named class (13.1% against 14.8–31.0%) and per top-level class (23.4% against 26.4–41.7%); per source file it tied with both saga versions (48.0%). Propagation cost is a static proxy computed from dependencies. The change requests above measure real changes, and Ballarch's were larger.
- **Testability.** A decider is a pure function. The same test kit runs a whole feature under simulated faults and crashes against ground truth (see the [tutorial](tutorial.md)).
- **Mechanical checks at each level.** All three levels check the call-protocol rules and the knowledge algebra:
  - services: architecture rules (ArchUnit), a module-boundary check, runtime oracles that also run on language-neutral conformance traces, an adapter conformance kit;
  - firmware: a purity lint of the decider (object file, preprocessed source and, with clang, syntax tree) and power-loss exploration;
  - RTL: netlist checks and fault-injecting benches.

## Use it when…

- features call external systems with effects (payments, bookings, emails, provisioning) whose ambiguous outcomes matter;
- several people or agents will write such features, and you cannot rely on each of them to hand-roll outcome classification, write-ahead records and recovery correctly (each feature still writes its reconciliation);
- state must survive crashes and restarts, and you are willing to invest in a production durable store, which the reference runtime lacks (its file store is single-node);
- you want failure behaviour to be reviewable and testable as data;
- firmware or a hardware block issues commands whose effects must not repeat (actuators, doorbells, FIFO pushes): the same rules apply, with level-specific checks ([Ballarch at every level](interpretations.md)).

## Prefer something else when…

- **Features are local and synchronous.** A plain class or a reducer is shorter; the kernel adds ceremony and nothing else.
- **A few effectful flows, owned by someone who knows their failure modes.** In the benchmark, careful versions in all four styles were as correct as Ballarch while the horizon held, 17–45% shorter, and needed smaller changes in total. They follow the reconciliation procedure of [Core §8.4](../spec/ballarch-core-2.0.md#8-guidance-non-normative) and were written by Ballarch's author after the first results were known. They show that correctness is reachable without the runtime, not how an independent expert would write it. With a provider that honours idempotency keys, even a naive layered design whose retries end in `NeedsAttention` violated no requirement in any cell, at the cost of more operator hand-offs under Stress.
- **You already run a durable execution engine** (Temporal, Restate, Azure Durable Functions, AWS Lambda durable functions) or an actor system with persistence (Akka, Orleans). Compare its actual guarantees with the requirements of your application before adding another runtime contract. The benchmark does not include such an engine. The [FAQ](faq.md) works through one case: a non-idempotent activity that timed out after it started.
- **Latency per request must stay in the low microseconds** on the reference scheduler: the measured median was 46 µs.
- **Your changes often add new kinds of request to existing components.** In the benchmark's refund change, Ballarch's patch was about twice the size of the careful versions' (73 code lines against 34–43, 16 top-level units against 5–6). This is one change request, written by one author.

## Not yet known

These were not measured. Do not assume them either way:

- whether the explicit structure speeds up review, onboarding or defect finding in real teams;
- how the architecture behaves with a production durable store, over a network, or under production load;
- whether independent authors of the baselines and of the change requests would reach the same results;
- how the size of changes compares for changes to business rules or data alone: three of the four change requests concern an unreliable external service;
- how Ballarch compares with a durable-execution engine;
- the runtime's overhead on other machines, and its throughput.
