<h1 align="center">Ballarch</h1>

<p align="center">
  <a href="spec/ballarch-core-2.0.md">
    <img src="assets/ballarch-hero.svg" alt="Ballarch: owners, obligations, honest outcomes" width="100%" />
  </a>
</p>

<p align="center">
  <strong>Owners, obligations and honest outcomes: one architecture from registers to services.</strong>
</p>

<p align="center">
  <a href="docs/tutorial.md">Write your first feature</a> ·
  <a href="docs/interpretations.md">Ballarch at every level</a> ·
  <a href="docs/problems.md">Problems it addresses</a> ·
  <a href="benchmark/results/RESULTS.md">Benchmark</a> ·
  <a href="spec/ballarch-core-2.0.md">Core specification</a> ·
  <a href="paper/">Paper</a> ·
  <a href="docs/ru/README.md">По-русски</a>
</p>

**Ballarch Core 2.0** is an architecture for components that own state and talk to systems that can fail: services, user interfaces, firmware and hardware blocks.

- Each piece of mutable state has one owner, a *Ball*. A Ball decides in pure functions, and a runtime commits its decisions atomically.
- Every outbound request is an *obligation* that ends in exactly one honest outcome: `Done`, `NotDone` or `Unknown`.
- Which outcome is possible follows mechanically from two boundaries in time, the **commit horizon** and the **send horizon**, and from the port's effect class.

The [specification](spec/ballarch-core-2.0.md) is a release candidate (`2.0.0-rc.1`). Ballarch was called Pokeball up to Core 1.5; see the [migration guide](docs/migration-from-1.5.md).

## What is new

Ballarch keeps the established parts of earlier styles:

- single owners (actors, aggregates);
- pure steps (Elm, Decider);
- commit then dispatch (transactional outbox);
- adapters at the edge (Hexagonal and Clean Architecture).

Beyond them it adds four elements ([Core §12](spec/ballarch-core-2.0.md#12-lineage-and-what-is-new-informative) gives the prior art for each):

1. **Boundaries in time.** Layers and hexagons constrain *where* code may depend; the commit horizon and the send horizon constrain *when* effects and knowledge may cross. Before the send horizon, non-occurrence is provable. After it, only evidence about every attempt (an answer, a refusal, or an adapter's proof of non-application) or reconciliation can settle what happened to an effectful call.
2. **Outcomes from phase and effect class.** A runtime, not each feature, chooses `Done`, `NotDone` or `Unknown`, and never reports `NotDone` without evidence. A `NonIdempotent` call is never re-sent.
3. **Knowledge of composites, with closure.** A small algebra over Kleene's three-valued logic says what an operation made of several calls may claim (Done, NotDone, Partial, Unknown, …). Honest parts compose into an honest whole, which can be used one level up as a single component.
4. **One rule set, checked at every level.** The same rules are checked by bytecode rules and trace oracles in a [JVM service](reference/kotlin/), by power-loss exploration and object-file checks in [firmware](reference/c/), and by lint, netlist checks and a fault-injecting bench in [RTL](reference/rtl/).

## Evidence

| What | Result |
|---|---|
| [Benchmark](benchmark/results/RESULTS.md): one checkout in six styles, 2,000 runs per cell | Idiomatic layered, actor, MVU and outbox-plus-saga versions double-charged or reported charged orders as failed in 12–73% of runs. Ballarch and a layered version hardened by hand violated no requirement. Ballarch cost 29% more code, 45 framework types against 6, lower class cohesion, higher cyclomatic complexity (68 against 49 in total) and a larger distance from the main sequence (0.44 against 0.35). It had the lowest propagation cost and 88% of its code in pure classes. |
| [Alloy model](formal/alloy/obligations.als) of the call protocol | 6 safety properties and 1 liveness property hold within bounds; 6 seeded runtime bugs each yield a counterexample |
| [Knowledge algebra](reference/kotlin/ballarch-kernel/src/test/kotlin/ballarch/kernel/KnowledgeTest.kt) | Soundness and coordinate-wise exactness checked exhaustively for up to four constituents (69,905 worlds) |
| [Trip example](reference/kotlin/examples/trip/) | Under every explored fate and crash, the composite's claims admit the real bookings; a two-valued status is wrong in 19% of the same runs |
| [Firmware reference](reference/c/) | About 220,000 runs over link fates and power-loss points, including torn writes: no violation; 3 seeded bugs caught |
| [RTL reference](reference/rtl/) | 75 fault scenarios pass; 3 seeded bugs caught; structural checks of single drivers, latch-free deciders and registered dispatch |

None of this establishes production reliability or team productivity. The runtime's durable store is a single-node file store, tested against killed processes but not power loss, and the benchmark is small and was written by one author. [Core §10](spec/ballarch-core-2.0.md#10-trade-offs-and-known-limitations) and the [decision guide](docs/decision-guide.md) list the costs and what was not measured. [Problems Ballarch addresses](docs/problems.md) maps twelve recurring failures of today's architectures to the mechanism, rule and evidence for each, and lists what Ballarch does not solve.

## The idea in one example

```kotlin
object OrderDraftBall : Ball<Draft, DraftRequest, DraftReply> {
    override val type = "order-draft"
    override fun initial(key: String) = Draft(quantity = 1)

    override fun decide(state: Draft, request: DraftRequest, ctx: RequestContext) = when (request) {
        is DraftRequest.SetQuantity ->
            if (request.value in 1..20) accept(Draft(request.value), DraftReply.Quantity(request.value))
            else reject(DraftReply.OutOfRange)
        DraftRequest.GetQuantity -> accept(state, DraftReply.Quantity(state.quantity))
    }
}
```

A decision may also contain *outputs*: calls to ports, replies, notices and cancellations. The runtime commits the new state and the outputs atomically, sends nothing before that commit, and later delivers each call's single completion to `complete`. A read (`Safe`) never ends `Unknown`. A card charge (`NonIdempotent`) is never re-sent, and after a timeout or crash it ends `Unknown` rather than a false `NotDone`. The [payment example](reference/kotlin/examples/shop/payments/src/main/kotlin/ballarch/examples/shop/payments/PaymentBall.kt) reconciles after `Unknown`.

## Repository map

| Path | Contents |
|---|---|
| [`spec/ballarch-core-2.0.md`](spec/ballarch-core-2.0.md) | The normative specification: model, boundaries in time, execution semantics, knowledge algebra, rules R1–R14, profiles, conformance, interpretation across levels, lineage |
| [`reference/kotlin/`](reference/kotlin/) | Kernel, reference runtime, test kit, architecture rules, examples, and conformance tests (services) |
| [`reference/c/`](reference/c/) | Firmware runtime and a therapy-controller example with power-loss exploration (C11) |
| [`reference/rtl/`](reference/rtl/) | A hardware Ball with lint, structural checks and a fault-injecting bench (SystemVerilog) |
| [`formal/alloy/`](formal/alloy/) | Alloy 6 model of the call protocol |
| [`benchmark/`](benchmark/) | One checkout in six architectural styles, fault injection, and structural metrics |
| [`paper/`](paper/) | The paper (LaTeX) and the publication kit |
| [`docs/`](docs/) | Tutorial, levels guide, problems addressed, decision guide, FAQ, anti-patterns, migration, verification, Russian overview and article draft |
| [`skills/ballarch/`](skills/ballarch/) | An agent skill for feature work (unevaluated) |
| [`tools/`](tools/) | Link and snippet checks for the documentation |

## Try it

Requires JDK 21. From `reference/kotlin`:

```sh
./gradlew build checkModuleBoundaries   # tests, rules, examples, knowledge algebra, benchmark regression
```

Then `make` in [`reference/c`](reference/c/) and [`reference/rtl`](reference/rtl/), and see the [verification guide](docs/reproducibility.md) for the model, the benchmark and the paper. Start with the [tutorial](docs/tutorial.md).

## License and authorship

Copyright © 2026 **Vladislav Tomilov (4wl2d)**.

Text and diagrams are licensed under [CC BY 4.0](LICENSE); see [NOTICE.md](NOTICE.md). **The code has no software license yet.** A software license remains the copyright holder's decision. Ballarch is not affiliated with Nintendo; the project's former name is not used as a trademark.

Ballarch Architecture by Vladislav Tomilov (4wl2d).
