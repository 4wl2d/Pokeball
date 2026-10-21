# Architecture benchmark

The same checkout feature, written in six architectural styles, runs against one simulated environment with an unreliable payment provider and crashes. The benchmark measures two things:

- **correctness under faults**, checked against the provider's ground truth;
- **structure** of the feature code, using standard coupling, cohesion, complexity and size metrics.

Results: [`results/RESULTS.md`](results/RESULTS.md); raw data: [`results/results.json`](results/results.json).

## Scenario

Every implementation receives the same specification:

- **Stock.** One SKU with 2 units in stock.
- **Orders.** Three orders (`o1`, `o2`, `o3`) of 1 unit each, all sent at time 0. A client sends each order once.
- **Checkout.** Reserve the unit, charge 100 through the payment provider, then complete the order. If the payment is not made, release the reservation and fail the order. Out of stock also fails the order.
- **Terminal states.** `Completed`, `Failed`, or `NeedsAttention`, which hands an unresolved case to an operator.
- **Persistence.** A database with atomic transactions survives crashes; everything else is lost. Ballarch keeps its store in the same simulated database.
- **The provider** charges every request that reaches it: it has **no idempotency keys**. A Safe status endpoint reports whether an order was charged. A request reaches the provider within 20 ms of being sent or never, so a request older than 1 s can no longer take effect (the delivery horizon).

### Requirements (the oracles)

| Violation | Definition |
|---|---|
| DoubleCharge | the provider charged an order more than once |
| FalseFailure | an order reported `Failed` was charged |
| UnpaidCompletion | an order reported `Completed` was not charged |
| Stranded | an accepted order has no terminal state when the run ends |
| StockLeak | an order reported `Failed` still holds stock |
| Oversell | more orders completed than units in stock |
| LostCharge | an order was charged that the system never accepted |

`NeedsAttention` is not a violation: it is the honest answer when the provider cannot be asked. Its frequency is reported as a cost.

### Fault model

- **Provider fates.** Each provider request independently gets one fate:
  - answered in time;
  - answered after 5 s;
  - lost before arrival;
  - processed, answer lost;
  - processed, then connection reset;
  - connection refused (provably not sent).
- **Profiles.** *Stress* makes every fate equally likely. *Mild* answers 90% of requests in time and gives each fault 2%.
- **Crashes.** In crash cells, the process crashes once and restarts 50 ms later. The crash comes before a scheduler task chosen uniformly among the first *N* tasks, where *N* is the median task count of crash-free runs of that architecture and profile. Tasks, not times, are used so that a crash can fall between a database write and the request that follows it in the same instant.
- **Recovery.** Each implementation recovers in its own idiomatic way.
- **Run length.** A run ends when no work is left, or after 120 s of simulated time.
- **Seeds.** The simulation is deterministic: the same seed gives the same run, and the published `results.json` is reproduced byte for byte.

## Implementations

All six are in Kotlin, written by the same author against the specification above. Each follows its style's canonical guidance and nothing more. Code that stands in for a framework sits in a `kit` package (an actor toolkit, an Elm-style runtime, transactional messaging). It is excluded from the feature metrics and its size is reported separately; Ballarch's runtime library is excluded in the same way.

| Key | Style | What it follows | Handling of provider failures | Recovery |
|---|---|---|---|---|
| `layered` | Layered / Clean Architecture | Entities, use-case interactor, output ports, adapters, main component (Martin 2017) | One attempt with a timeout; any exception means the payment failed: release and fail | On start, resume every unfinished order from its persisted status |
| `hardened` | The same layers, hardened by an expert by hand | As `layered`, plus write-ahead status, evidence-based retry and status reconciliation (as described in Section 8.4 of the Ballarch specification, but written into the feature) | `CHARGING` is saved before each request; a request is repeated only after connection refused; timeout or reset → wait past the horizon, then query the status | `CHARGING` orders are reconciled, never charged again |
| `actors` | Actors with durable state | Typed actors, `ask` with a timeout, `pipeToSelf`, state persisted before acting (Hewitt et al. 1973; Akka Typed durable state) | A failed `ask` means the payment failed: release (`tell`) and fail | Re-create checkout actors from durable state and re-drive their current stage (at-least-once) |
| `mvu` | Model–View–Update (Elm architecture) on the server | One store; pure `update(model, msg) → (model, commands)`; the model is persisted before commands run (Czaplicki 2012) | A command's result is a two-valued `Result`; failure releases and fails the order | `init` re-issues the commands of orders still in the paying stage |
| `saga` | Hexagonal + transactional outbox + orchestrated saga | Saga orchestrator; outbox written with each state change; at-least-once relay; idempotent consumers keyed by message id (Richardson 2018; Garcia-Molina & Salem 1987) | The payment participant replies `PaymentFailed` on timeout or error, and the saga compensates | The relay redelivers unacknowledged messages |
| `ballarch` | Ballarch Core 2 | Inventory, payment and checkout Balls; ports with effect classes; composition root ([specification](../spec/ballarch-core-2.0.md)) | The runtime classifies outcomes; the payment Ball repeats a charge only after `NotDone` and reconciles `Unknown` | Durable profile of the reference runtime |

`hardened` exists to show that correctness does not require Ballarch: an expert can reach it by hand in any style. Comparing it with `ballarch` shows where the fault-handling knowledge lives and what each choice costs.

### Seeded bugs

To check that zero violations is not a blind spot, two plausible bugs are injected from outside the correct implementations:

- a Ballarch payment Ball that treats an `Unknown` charge as not charged;
- a hardened gateway that reports an uncertain charge as not sent.

Both must be detected.

## Metrics

| Metric | Definition | Source |
|---|---|---|
| Runs with a violation | Share of runs in which any requirement above fails, with a Wilson 95% interval | — |
| SLOC | Non-blank, non-comment lines of feature code | — |
| CBO | Coupling Between Objects: the number of other feature classes a class uses or is used by | Chidamber & Kemerer 1994 |
| LCOM4 | Connected components of a class's methods, joined by shared fields or calls; 1 is cohesive | Hitz & Montazeri 1995 |
| CAMC | Cohesion Among Methods of a Class: overlap of the methods' parameter types (0–1, higher is more cohesive) | Bansiya et al. 1999 |
| Ca, Ce, A, I, D | Afferent and efferent coupling of a package, abstractness, instability Ce/(Ca+Ce), distance from the main sequence \|A+I−1\| | Martin 1994 |
| Propagation cost | Density of the transitive closure of the class dependency graph: the average share of the system a change can reach | MacCormack, Rusnak & Baldwin 2006 |
| Dependency cycles | Strongly connected components of the class graph with more than one class | — |
| McCabe CCN | Cyclomatic complexity per function, from [lizard](https://github.com/terryyin/lizard) | McCabe 1976 |
| Framework types | Distinct library types and function files the feature code references, outside the JDK, the Kotlin standard library and the shared simulation | — |
| `catch` clauses | Places where the feature author handles exceptions | — |
| Pure share | Share of bytecode in classes with no mutable field and no reference to concurrency, I/O, clocks, randomness, the database, the provider or a runtime | — |

The structural metrics are computed from bytecode by [`runner/src/main/kotlin/bench/runner/Structure.kt`](runner/src/main/kotlin/bench/runner/Structure.kt). A *class* is a named class. Compiler-generated classes, such as lambdas, coroutine state machines and `$WhenMappings`, count as part of the class that declares them. Cohesion metrics ignore constructors, compiler-generated members, data-class members and property accessors.

## Reading the results

Correctness under faults:

- Ballarch and the hand-hardened layered implementation violated no requirement in any cell (0 of 2,000 runs per cell).
- The four idiomatic implementations violated a requirement in 12% of crash-free runs under Mild faults, and in 25–73% of runs under Stress faults or with a crash.
- Without crashes, those four behave identically: each turns a timeout or a connection error into a failure, with one attempt per order. Only their crash recovery differs:
  - actors and MVU re-send a charge that may already have happened;
  - the saga's relay redelivers the charge command;
  - the layered version resumes from the last persisted status.
- Both seeded bugs were detected.

Structure, Ballarch against the hand-hardened version (the one that is equally correct):

- **Costs:**
  - about 29% more SLOC (279 against 216) and 28% more bytecode;
  - 45 framework types against 6;
  - more, smaller classes, with higher cyclomatic complexity (2.19 against 1.81 per function; 68 against 49 in total);
  - a larger mean distance from the main sequence (0.44 against 0.35) and the highest single-class CBO of all six (18 against 16);
  - fewer cohesive classes by LCOM4 (8% with LCOM4 = 1, against 71%; mean LCOM4 1.92 against 2.14) and lower mean CAMC (0.33 against 0.52).
- **Gains:**
  - a lower mean CBO (3.94 against 4.70);
  - 88% of the code in pure classes against 27%;
  - the lowest propagation cost of all six (13% against 20–31%);
  - no `catch` clause in feature code.

The fault-handling knowledge sits in the runtime and in `Outcome` branches rather than in exception handlers. The hardened version needed 5 `catch` clauses and a hand-written write-ahead status.

## Threats to validity

- **One author wrote all six implementations**, and that author also designed Ballarch. Each idiomatic baseline is one reading of its style. The idiomatic reading deliberately does not add the fault handling that `hardened` adds; the `hardened` column shows that an expert can add it by hand. Independent implementations, ideally written before the results are seen, would make the comparison stronger.
- **One small scenario.** The scenario has three orders, one SKU, one provider without idempotency keys, at most one crash, and a simulated provider and database. A provider with idempotency keys would let simple retries become correct and would narrow the gap.
- **The fault distributions are assumptions.** Stress is adversarial by design.
- **Metrics have known biases.**
  - LCOM4 assumes that classes share fields, so it penalizes stateless functions over a state type: Ballarch deciders, and MVU's `update`.
  - Propagation cost and CBO depend on how finely code is split into classes; Ballarch's sealed state hierarchies add many small classes.
  - SLOC and framework-type counts depend on the excluded toolkits.
- **Not measured:** change impact (no change requests were applied in this round), developer effort, review speed, runtime performance, and behaviour on a real network or durable store.
- **The benchmark was not pre-registered.**

## Reproduce

Requires JDK 21, Python 3.10 or later, and `pip install lizard==1.24.1`. From `reference/kotlin`:

```sh
./gradlew :benchmark:runner:run --args="2000 ../../benchmark/results"
python3 ../../benchmark/report.py ../../benchmark/results/results.json ../../benchmark/results
```

`./gradlew build` also runs [`BenchmarkTest`](runner/src/test/kotlin/bench/runner/BenchmarkTest.kt), which repeats the headline observations on 300 runs per cell.

## References

- Bansiya, J., Etzkorn, L., Davis, C., Li, W. (1999). A class cohesion metric for object-oriented designs. *Journal of Object-Oriented Programming* 11(8), 47–52.
- Chidamber, S. R., Kemerer, C. F. (1994). A metrics suite for object oriented design. *IEEE TSE* 20(6), 476–493.
- Czaplicki, E. (2012). *Elm: Concurrent FRP for Functional GUIs*. Senior thesis, Harvard University.
- Garcia-Molina, H., Salem, K. (1987). Sagas. *SIGMOD '87*, 249–259.
- Hewitt, C., Bishop, P., Steiger, R. (1973). A universal modular ACTOR formalism for artificial intelligence. *IJCAI '73*, 235–245.
- Hitz, M., Montazeri, B. (1995). Measuring coupling and cohesion in object-oriented systems. *Proc. International Symposium on Applied Corporate Computing*.
- MacCormack, A., Rusnak, J., Baldwin, C. Y. (2006). Exploring the structure of complex software designs. *Management Science* 52(7), 1015–1030.
- Martin, R. C. (1994). OO design quality metrics: an analysis of dependencies. White paper.
- Martin, R. C. (2017). *Clean Architecture*. Prentice Hall.
- McCabe, T. J. (1976). A complexity measure. *IEEE TSE* SE-2(4), 308–320.
- Richardson, C. (2018). *Microservices Patterns*. Manning.
