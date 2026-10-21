# Verifying Ballarch

This checkout includes the following, each with its checks:
- the Kotlin reference implementation, its tests and examples;
- the conformance-trace oracles;
- the firmware (C) and hardware (SystemVerilog) references;
- the Alloy model;
- the architecture benchmark;
- the paper;
- documentation checks.

These checks support the conformance criteria in [Core §9](../spec/ballarch-core-2.0.md#9-conformance). They do not certify a deployment.

## Prerequisites

- JDK 21. The Gradle wrapper downloads Gradle 8.14.3; Gradle fetches Kotlin 2.3.21 and dependencies from Maven Central.
- Python 3.10 or later, standard library only (the trace oracles need 3.8 or later), and git for the benchmark's change requests.
- For model checking, Java and network access to download the pinned Alloy 6.2.0 distribution.
- For the firmware reference, a C11 compiler and binutils (`nm`, `objdump`). Clang, if installed, adds the purity lint's syntax-tree layer and its Cortex-M0 checks; without it they are skipped with a note. For the hardware reference, Icarus Verilog 12, Verilator 5 and Yosys 0.33 or later. For the paper, TeX Live 2023 or later with `latexmk`.

## Runtime, examples, and architecture rules

From `reference/kotlin`:

```sh
./gradlew build checkModuleBoundaries
```

This runs:
- runtime unit and fault-exploration tests, which also write conformance traces (below);
- the evolution tests (`EvolutionTest`): state versions, effect classes recorded at commit, and the start-up refusal of [Core §6.1](../spec/ballarch-core-2.0.md#61-evolution);
- architecture-rule fixtures;
- HTTP adapter conformance;
- the tutorial's decider and fault-exploration tests;
- the exhaustive knowledge-algebra check;
- the trip example's explorations, including the Transient profile and a control without the providers' fence;
- the itinerary example's explorations of two trips composed through the trip's port;
- the benchmark's regression test.

Inspect the test reports for failures and skipped tests. Reports are written under each module's ignored `build/` directory, including adapter and architecture-rule summaries under `build/reports/conformance/`. The trip and itinerary explorations print their counts (runs, settled knowledge, false claims, controls) to standard output, which Gradle keeps in the `system-out` element of each test result, under `examples/trip/build/test-results/test/` and `examples/itinerary/build/test-results/test/`. To run the two examples alone:

```sh
./gradlew :examples:trip:test :examples:itinerary:test
```

Optional mutation testing:

```sh
./gradlew :ballarch-runtime:pitest
```

Inspect `ballarch-runtime/build/reports/pitest/`; a mutation score describes the tested runtime and chosen mutators, not overall correctness.

## Conformance traces

The Kotlin build writes traces in the language-neutral format of [Core §9.6](../spec/ballarch-core-2.0.md#96-conformance-trace) ([`tools/TRACE-FORMAT.md`](../tools/TRACE-FORMAT.md)) to `reference/kotlin/ballarch-runtime/build/conformance-traces/`. From the repository root, after the build:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/oracles.py reference/kotlin/ballarch-runtime/build/conformance-traces/*.jsonl
python3 tools/oracles.py --expect-violations reference/kotlin/ballarch-runtime/build/conformance-traces/seeded/*.jsonl
```

The first command runs the oracles' own tests (26 tests). The second checks the 46 traces from explorations and clean runs, all of which must pass: it prints `oracles.py: 46 trace(s), 0 violation(s), 0 invalid`. The third checks the 10 traces in `seeded/`, each with one seeded violation, all of which must fail. Each command exits with 0 on success. A runtime in another language can be checked the same way once it writes this format.

## Alloy model

From the repository root:

```sh
bash formal/alloy/fetch-alloy.sh
python3 formal/alloy/run.py
```

The download helper verifies the pinned jar's checksum. The runner executes each model command and compares its result with the command's `expect` annotation, including the seeded bugs. It exits nonzero if a command fails or disagrees with its expectation. Its report is written to the ignored `formal/alloy/build/RESULTS.md`; use `--out` to select another output path.

Inspect the scope and trace bounds in each result. Absence of a counterexample within those bounds is not a proof for all system sizes or schedules.

## Firmware reference (C)

From `reference/c`, with a C11 compiler and binutils:

```sh
make
```

This runs, in about two minutes:
- the exhaustive check of the C knowledge algebra (69,905 worlds for completed outcomes, as in Kotlin, and 5,421,361 for claims made before the end);
- the runtime's deterministic tests (preflight, bounds, retries, quarantine, NVM write errors, 64-bit time);
- the bounded exhaustive explorations of the dosing controller (937,229 runs) and the two-valve transfer (982,599 runs, in which every claim and status reply must admit the valves' final positions), with one fault at every power-loss point and NVM write, including torn and failed writes, and the link fates of the first 6 (dosing) or 7 (transfer) transmissions;
- the purity lint of each decider (object file, preprocessed source and, with clang, syntax tree), with negative controls that must each be refused with their expected message;
- 47 seeded bugs in 52 checks, each of which must produce a counterexample from its named oracle.

```sh
make sanitize   # the tests under AddressSanitizer and UndefinedBehaviorSanitizer
make deep       # larger bounds, about six minutes
```

`make deep` explores the first 8 dosing transmissions (10,006,169 runs), every transfer transmission (3,212,051 runs), and two faults per run (1,350,947 dosing and 1,616,315 transfer runs, over fewer transmissions). The explorations are deterministic, so these counts reproduce exactly. [`reference/c/README.md`](../reference/c/README.md) lists every oracle, fixture and seeded bug.

## Hardware reference (SystemVerilog)

From `reference/rtl`, with Icarus Verilog 12, Verilator 5 and Yosys 0.33 or later:

```sh
make
```

This runs, in five to six minutes on four cores:
- Verilator lint of both blocks and their hierarchy;
- the parameter-range check: ten parameter sets that Verilator, Yosys and Icarus must each refuse, and four they must accept;
- Yosys structural checks of R1, R2, R4 and R5 on the netlist (single drivers, flip-flops on the rising edge of `clk` as the only storage, no input reaching an asynchronous pin, registered outputs), with 15 fixtures that must each be rejected for their named reason and one that must pass;
- the fault-injecting benches: 8,764 issuer scenarios at each of three parameter sets, and 1,458 scenarios for the composite block, whose status register must admit the device's ground truth and end at the knowledge computed by supervaluation;
- 28 seeded bugs, and three runs with the issuer's parameter check removed, each of which must make a bench report a failed check.

The seeded bugs run in parallel, one bench per core; `make JOBS=1` runs them one at a time. [`reference/rtl/README.md`](../reference/rtl/README.md) lists the fates, checks and seeded bugs.

## Architecture benchmark

From `reference/kotlin`:

```sh
./gradlew :benchmark:runner:run --args="2000 ../../benchmark/results"
python3 ../../benchmark/report.py ../../benchmark/results/results.json ../../benchmark/results
```

The runner runs every cell 2,000 times and the single-crash sweep on 1,000 seeds, in about two minutes, and writes `results.json`; a third argument sets the number of sweep seeds. `report.py` renders [`RESULTS.md`](../benchmark/results/RESULTS.md) from `results.json`, `changes.json` and `overhead.json`. The simulation is deterministic: the published [`results.json`](../benchmark/results/results.json) is reproduced byte for byte, and `RESULTS.md` is rendered unchanged. `./gradlew build` also runs the benchmark's regression tests on fewer runs.

The change requests and the runtime overhead, from the repository root:

```sh
python3 benchmark/changes/measure.py --check
python3 benchmark/changes/verify.py
(cd reference/kotlin && ./gradlew :benchmark:runner:overhead)
python3 benchmark/report.py benchmark/results/results.json benchmark/results
```

- `measure.py --check` writes nothing. It fails if a change-request patch no longer applies to this checkout, if a patch's metrics differ from those recorded in [`changes.json`](../benchmark/results/changes.json), or if a patch breaks the formatting rule of [`changes/README.md`](../benchmark/changes/README.md). Without `--check` it recomputes the patch metrics.
- `verify.py` applies the patches of each change request in a temporary copy outside the work tree, builds it, and runs every design under that change request's scenario: 2,000 runs per profile with and without a crash, and the sweep on 500 seeds. It takes about five minutes on 4 processors, writes the verification part of `changes.json`, and exits with 1 if a careful version or Ballarch violates a requirement while the delivery horizon holds. Extra Gradle arguments go after `--gradle-arg`.
- The `overhead` task times one request through the reference runtime in four ways and writes [`overhead.json`](../benchmark/results/overhead.json). Its numbers depend on the machine and its load, so CI does not regenerate the file and it is not part of the byte-for-byte check.
- The last command renders `RESULTS.md` again with the new files.

Method, change requests and threats to validity: [benchmark/README.md](../benchmark/README.md).

## Paper

From `paper`, with TeX Live 2023 or later:

```sh
latexmk -pdf ballarch.tex
```

## Documentation

From the repository root:

```sh
python3 tools/check_links.py
python3 tools/check_snippets.py
```

The link check requires relative destinations to belong to the tracked public tree; an ignored local file cannot satisfy a link. It does not check URL availability or anchors. The snippet check compares marked tutorial blocks with their Kotlin source.

## Limits

The runtime's file store has been tested against killed processes, not power loss, and has no replication. The firmware and RTL references run in simulation, within the bounds stated above, and emit no conformance trace. A real store, device, deployment, workload and failure environment need their own evidence. The benchmark is one scenario family, written by one author, and its runtime overhead was measured on one machine; see its threats to validity. The data of an earlier prototype comparison, which the specification once cited, is outside this distribution; the benchmark's change requests and overhead measurement replace its figures.
