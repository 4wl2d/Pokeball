# Decision guide: when is Ballarch Core 2 worth it?

This guide summarises what has been measured, including the negative results, in the reproducible [benchmark](../benchmark/) of this distribution. It also repeats, marked as unverified, two figures from an earlier prototype comparison whose data is not published ([Core §10](../spec/ballarch-core-2.0.md#10-trade-offs-and-known-limitations)).

It gives no universal recommendation. It states which trade-offs were measured, and which were not.

## What you pay

From the benchmark: one checkout, six styles, all written by one author ([results](../benchmark/results/RESULTS.md)).

- **More feature code.** Ballarch took 279 SLOC against 216 for a layered version hardened by hand to the same correctness. The idiomatic but faulty versions took 96 (MVU) to 184 (outbox + saga).
- **More concepts.** The feature code used 45 framework types against 3–6.
- **More, smaller classes, with lower class cohesion.** LCOM4 and CAMC count stateless deciders over a state type as less cohesive than classes that share fields.
- **Higher complexity and a less balanced package structure.** Cyclomatic complexity was 2.19 per function against 1.81 (68 against 49 in total). The mean distance from the main sequence was 0.44 against 0.35. The most coupled class had a CBO of 18 against 16.

Unverified, from an earlier prototype comparison whose data and harness are not part of this distribution. These figures cannot be checked from this checkout and support no claim:

- **Wider changes.** The module structure of Core §7.1 made cross-feature changes touch more files than single-module baselines in 4 of 5 change requests.
- **A thread hand-off per request.** A local request through the reference runtime took a median of 39 µs more than a direct call on the production thread scheduler, almost all of it thread hand-off. The runtime's own work is about 0.5 µs and 1 KB per request.

Also:

- **A runtime to depend on.** The reference runtime is about 1,000 lines. It is a research prototype; its file store is single-node and has no replication.

## What you get

- **Fault handling that does not depend on each feature's author.** In the benchmark:
  - idiomatic layered, actor, MVU and outbox-plus-saga checkouts double-charged, or reported charged orders as failed, in 12–73% of runs;
  - Ballarch, and the layered version hardened by hand with the specification's reconciliation procedure, did so in none of 2,000 runs per cell;
  - in the hardened version, the knowledge lives in the feature: a write-ahead status, 5 `catch` clauses, and reconciliation by hand. In Ballarch, it lives in the runtime and in `Outcome` branches.
- **Explicit states for ambiguity.** `Unknown` is a type, not a timeout exception that each call site maps differently.
- **Honest composites.** The knowledge algebra (Core §4.2) keeps a workflow from reporting "failed" after a partial or uncertain effect, provided it counts every call it may still make as unknown and replies once it is settled (R14, §7.4).
- **Pure code and lower static coupling.** 88% of Ballarch's feature code was in pure classes, against 26–59% for the others. It had the lowest propagation cost of the six designs. Propagation cost is a static proxy computed from dependencies; it is not a measurement of how far real changes spread.
- **Testability.** A decider is a pure function. The same test kit runs a whole feature under simulated faults and crashes against ground truth (see the [tutorial](tutorial.md)).
- **Mechanical checks.** At each level:
  - services: architecture rules (ArchUnit), a module-boundary check, runtime oracles, an adapter conformance kit;
  - firmware and RTL: object-file and netlist checks, power-loss exploration and a fault-injecting bench. These check the call-protocol rules; the knowledge algebra is checked only for services.

## Use it when…

- features call external systems with effects (payments, bookings, emails, provisioning) whose ambiguous outcomes matter;
- several people or agents will write such features, and you cannot rely on each of them to hand-roll write-ahead records, reconciliation and recovery correctly;
- state must survive crashes and restarts, and you are willing to invest in a production durable store, which the reference runtime lacks (its file store is single-node);
- you want failure behaviour to be reviewable and testable as data;
- firmware or a hardware block issues commands whose effects must not repeat (actuators, doorbells, FIFO pushes): the same rules apply, with level-specific checks ([Ballarch at every level](interpretations.md)).

## Prefer something else when…

- **Features are local and synchronous.** A plain class or a reducer is shorter; the kernel adds ceremony and nothing else.
- **A few effectful flows, owned by someone who knows their failure modes.** In the benchmark, the layered version hardened by hand with the specification's write-ahead status and reconciliation procedure was 23% shorter than Ballarch's and equally correct. It shows that correctness is reachable without the runtime, not how an independent expert would harden the code.
- **You already run a durable execution engine** (Temporal, Restate, Durable Functions) or an actor system with persistence (Akka, Orleans). Compare its actual guarantees with the requirements of your application before adding another runtime contract. The [FAQ](faq.md) works through one case: a non-idempotent activity that timed out after it started.
- **Latency per request must stay in the low microseconds** on the reference scheduler.
- **Your changes mostly add or remove components.** In the earlier, unverified prototype comparison above, the module structure made such changes wider.

## Not yet known

These were not measured. Do not assume them either way:

- whether the explicit structure speeds up review, onboarding or defect finding in real teams;
- how the architecture behaves with a production durable store, over a network, or under production load;
- whether an independent implementer of the baselines would reach the same results;
- the change impact of real change requests on the benchmark's six implementations.
