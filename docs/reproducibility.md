# Verifying the reference implementation

This checkout includes the reference implementation, its tests and examples, the Alloy model, and documentation checks. These checks support the conformance criteria in [Core §9](../spec/pokeball-core-2.0.md#9-conformance). They do not certify a deployment or reproduce the historical comparative evaluation discussed in Core §10.

## Prerequisites

- JDK 21. The Gradle wrapper downloads Gradle 8.14.3; Gradle fetches Kotlin 2.3.21 and dependencies from Maven Central.
- Python 3.10 or later (standard library only).
- For model checking, Java and network access to download the pinned Alloy 6.2.0 distribution.

## Runtime, examples, and architecture rules

From `reference/kotlin`:

```sh
./gradlew build checkModuleBoundaries
```

This runs runtime unit and fault-exploration tests, architecture-rule fixtures, HTTP adapter conformance, and the tutorial's decider and fault-exploration tests. Inspect the test reports for failures and skipped tests. Reports are written under each module's ignored `build/` directory, including adapter and architecture-rule summaries under `build/reports/conformance/`.

Optional mutation testing:

```sh
./gradlew :pokeball-runtime:pitest
```

Inspect `pokeball-runtime/build/reports/pitest/`; a mutation score describes the tested runtime and chosen mutators, not overall correctness.

## Alloy model

From the repository root:

```sh
bash formal/alloy/fetch-alloy.sh
python3 formal/alloy/run.py
```

The download helper verifies the pinned jar's checksum. The runner executes each model command and compares its result with the command's `expect` annotation, including the seeded bugs. It exits nonzero if a command fails or disagrees with its expectation. Its report is written to the ignored `formal/alloy/build/RESULTS.md`; use `--out` to select another output path.

Inspect the scope and trace bounds in each result. Absence of a counterexample within those bounds is not a proof for all system sizes or schedules.

## Documentation

From the repository root:

```sh
python3 tools/check_links.py
python3 tools/check_snippets.py
```

The link check requires relative destinations to belong to the tracked public tree; an ignored local file cannot satisfy a link. It does not check URL availability or anchors. The snippet check compares marked tutorial blocks with their Kotlin source.

## Limits

The runtime's durable store is an in-memory stand-in for simulated restarts. A real store, deployment, workload, and failure environment need their own evidence. The historical comparison's raw runs, baselines, benchmark harness, and change-request patches are outside this distribution; the checks above do not independently reproduce those measurements.
