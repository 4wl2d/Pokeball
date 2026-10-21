# Problems Ballarch addresses, and problems it does not

Ballarch was designed against concrete, recurring failures of today's architectures. This page lists each problem with three things:

- the mechanism that addresses it;
- the rule that makes the mechanism mandatory;
- the evidence in this repository.

Several mechanisms are established practice, and the table says so. The last section lists what Ballarch does not solve.

## Problems addressed

| # | Problem in today's architectures | Typical symptom | Ballarch mechanism | Rule | Evidence here | New in Ballarch? |
|---|---|---|---|---|---|---|
| 1 | Error handling is two-valued: a timeout is mapped to "failed" or "succeeded" at each call site | A charged customer is told the payment failed; a retry charges twice | Three outcomes. The runtime chooses `Done`, `NotDone` or `Unknown` from the call's phase relative to the send horizon and from the port's effect class. `NotDone` is never produced without evidence. | R7 | [Benchmark](../benchmark/results/RESULTS.md): idiomatic layered, actor, MVU and saga versions violate a requirement in 12–73% of runs; Ballarch in none of its 8,000 | The mechanical classification by phase and effect class is new; the third outcome itself is old (RPC "maybe", Jepsen `:info`) |
| 2 | Retries are configured at several layers, without regard to what a repeat does | Retry storms; duplicated side effects | Effect class on every port. Only the runtime retries, and only Safe and Idempotent ports, within a finite budget. Adapters never retry. | R8 | Alloy property P4 and its seeded `ResendAfterCrash` bug; the C mutant `BA_MUTANT_RESEND_AFTER_CRASH`; the RTL mutant `MUTANT_RETRY_FIRE` | Using the effect class as the interface's primary type, with mechanical consequences, is new; the safe/idempotent vocabulary is HTTP's |
| 3 | Dual writes: state goes to the database, messages to a broker, and a crash in between loses one of them | Lost or phantom events | The decision's state and all its outputs are committed in one atomic operation, and nothing leaves before that commit | R4, R5 | Alloy P1 and P6; the C exploration over torn NVM writes; the Kotlin `FileStore` test that kills a writing process | Not new: the transactional outbox and output commit. Ballarch makes it a rule for every component at every level. |
| 4 | Recovery is an afterthought: restart code re-runs whatever was in progress | Double effects after a deploy or crash | Restart rules per call phase. A call that was never sent is sent. A possibly-sent non-idempotent call ends `Unknown`. Idempotent calls are retried. | §3.7, R8 | In the benchmark, every idiomatic design's recovery path produced double charges; Ballarch's produced none | The phase-based restart table is part of contribution 2 |
| 5 | Shared mutable state, races, re-entrant callbacks | Heisenbugs; lost updates | One owner per fact; one step at a time per instance; no synchronous waiting between components | R1, R3, §3.4 | Architecture rules; the runtime guard and its oracle; Yosys single-driver check in RTL | Not new: actors, aggregates, single-driver discipline in hardware |
| 6 | Workflows and sagas report a composite operation as succeeded or failed after a partial or uncertain effect | "Booking failed" while the flight is booked | Knowledge algebra over Kleene's three-valued logic; absorbing compensations; R14 makes every claim about a composite derive from it | R14, §4.2, §7.4 | The trip example: its claims always admit the real bookings, while a two-valued status is wrong in 254 of 1,339 runs | New: the (all, any) algebra and composition closure. XA's heuristic outcomes are the closest precedent. |
| 7 | Boundaries in space only: nothing says *when* an effect may leave or what may be claimed after it left | Each team invents its own timeout semantics | Two boundaries in time, the commit horizon and the send horizon, which every implementation must make observable | §2.5 | Observable as store and write-ahead records (Kotlin), NVM records (C), clock edge and handshake (RTL) | New as architectural boundaries; the underlying mechanisms are known (output commit, write-ahead logging, precise interrupts) |
| 8 | Architecture erodes: diagrams and code drift apart | Rules exist only in wikis | Every rule names its enforcement. Bytecode rules, trace oracles, the Alloy model, object-file checks and netlist checks fail the build. | §5, §9 | Seeded violations at every level are detected | New in breadth: one rule set with level-specific checks |
| 9 | Business logic is entangled with I/O and needs mocks to test | Brittle tests; untested failure paths | Pure deciders; deterministic simulation with ground truth; exhaustive or seeded fault exploration | R2 | 88% of Ballarch's benchmark feature code is in pure classes, against 26–59% for the other designs; exploration tests at every level | Not new: functional core with an imperative shell, and Elm. The fault exploration against ground truth is packaged as a test kit. |
| 10 | Each layer of the stack has its own mental model: backend, firmware, hardware | Knowledge does not transfer; seams between levels are where failures hide | One contract; a composite of honest parts is one honest part one level up | §7.4, §11 | Kotlin, C and SystemVerilog references with level-specific checks | New: Bluespec and Lingua Franca unify execution semantics; Ballarch unifies effect outcomes |
| 11 | Time, identity and randomness are ambient globals | Nondeterministic tests; replays that diverge | Time, call keys and identity come from the context | R2 | `deciderPurity`; the C purity check (no external symbols) | Not new: the Decider pattern and Elm |
| 12 | Unbounded queues and fan-out | Memory blow-ups; silent drops under load | Finite bounds; new work is refused before it is accepted | R11 | Runtime preflight and admission | Not new: bounded queues |

## Problems Ballarch does not solve

- **Distributed consensus and replication** of one owner across nodes. Use a consensus system or a database for that.
- **Invariants that span owners.** They need a coordinating Ball, a reservation or a storage constraint (Core §10).
- **Hot keys.** One owner is one sequential writer.
- **Exactly-once external effects.** Ballarch guarantees at most one send of a non-idempotent call and an honest `Unknown`. Effectively-once behaviour needs an idempotent destination or reconciliation.
- **Choosing good ownership boundaries.** This is guidance and review (Core §8.1), not a check.
- **A production durable store.** The reference runtime's `FileStore` commits by atomic rename and is tested against killed processes, not power loss. It has no replication and serves one runtime per directory.
- **Performance.** The reference runtime adds a thread hand-off per request and at least three durable writes per effectful call.
- **Team productivity and the cost of the extra concepts.** These are unmeasured; the benchmark shows more code and more framework types.
