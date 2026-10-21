# Verifying Ballarch

This checkout includes the following, each with its checks:
- the Kotlin reference implementation, its tests and examples;
- the firmware (C) and hardware (SystemVerilog) references;
- the Alloy model;
- the architecture benchmark;
- the paper;
- documentation checks.

These checks support the conformance criteria in [Core §9](../spec/ballarch-core-2.0.md#9-conformance). They do not certify a deployment.

## Prerequisites

- JDK 21. The Gradle wrapper downloads Gradle 8.14.3; Gradle fetches Kotlin 2.3.21 and dependencies from Maven Central.
- Python 3.10 or later (standard library only; the benchmark report also needs `lizard`).
- For model checking, Java and network access to download the pinned Alloy 6.2.0 distribution.
- For the firmware reference, a C11 compiler and binutils (`nm`). For the hardware reference, Icarus Verilog 12, Verilator 5 and Yosys 0.33 or later. For the paper, TeX Live 2023 or later with `latexmk`.

## Runtime, examples, and architecture rules

From `reference/kotlin`:

```sh
./gradlew build checkModuleBoundaries
```

This runs:
- runtime unit and fault-exploration tests;
- architecture-rule fixtures;
- HTTP adapter conformance;
- the tutorial's decider and fault-exploration tests;
- the exhaustive knowledge-algebra check;
- the trip example's exploration;
- the benchmark's regression test. Inspect the test reports for failures and skipped tests. Reports are written under each module's ignored `build/` directory, including adapter and architecture-rule summaries under `build/reports/conformance/`.

Optional mutation testing:

```sh
./gradlew :ballarch-runtime:pitest
```

Inspect `ballarch-runtime/build/reports/pitest/`; a mutation score describes the tested runtime and chosen mutators, not overall correctness.

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

This runs the therapy controller's exhaustive exploration (about 220,000 runs over link fates and power-loss points, including torn writes), the object-file purity checks with their negative fixture, and three seeded bugs that must each produce a counterexample.

## Hardware reference (SystemVerilog)

From `reference/rtl`, with Icarus Verilog 12, Verilator 5 and Yosys 0.33 or later:

```sh
make
```

This runs Verilator lint, Yosys structural rule checks (single driver, no latch, registered dispatch) with their negative fixtures, the fault-injecting bench (75 scenarios), and three seeded bugs that must each fail.

## Architecture benchmark

From `reference/kotlin`, with Python 3.10 or later and `pip install lizard==1.24.1`:

```sh
./gradlew :benchmark:runner:run --args="2000 ../../benchmark/results"
python3 ../../benchmark/report.py ../../benchmark/results/results.json ../../benchmark/results
```

The simulation is deterministic: the published [`results.json`](../benchmark/results/results.json) is reproduced byte for byte. Method and threats to validity: [benchmark/README.md](../benchmark/README.md).

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

The runtime's file store has been tested against killed processes, not power loss, and has no replication. The firmware and RTL references run in simulation. A real store, device, deployment, workload and failure environment need their own evidence. The benchmark is one scenario, written by one author; see its threats to validity. The earlier prototype comparison's raw runs, baselines and change-request patches are outside this distribution; the checks above do not reproduce those measurements.
