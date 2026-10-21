# Contributing

Ballarch publishes an architecture specification, reference implementations at three levels (services, firmware, hardware), a benchmark, and the artifacts for checking them. Useful contributions include:

1. **Independent checks.** Reproduce a specification rule with a small example or a failing conformance test. State the exact version, assumptions, and observed outcome.
2. **Another runtime.** An implementation of [`spec/ballarch-core-2.0.md`](spec/ballarch-core-2.0.md) in another language or on a durable execution engine, checked against the conformance criteria in §9.
3. **A durable store**, and measurements of its commit and recovery behavior.
4. **Counterexamples.** Schedules, specification ambiguities or prior art that contradict a claim. Open an issue with the evidence.
5. **Independent benchmark implementations.** A version of the [benchmark](benchmark/) checkout in any style, written without seeing Ballarch's version, or a new scenario with its requirements stated in advance.
6. **Other levels.** A Ball on a real microcontroller or FPGA, or in another hardware description language, with checks of the rules at that level.

## Ground rules

- **Evidence first.** Explain the reason for a proposed normative change and, where possible, include a check that fails without it. Describe behavior separately from preferences.
- **Keep claims bounded.** Name the profile, scenario, implementation, and evidence for a claim. Generated reports belong in ignored build directories; do not treat a passing bounded check as a production guarantee.
- **Tests and checks.** Run `./gradlew build checkModuleBoundaries` from `reference/kotlin`, `make` in `reference/c` and `reference/rtl`, then the documentation checks in the [verification guide](docs/reproducibility.md).
- **Style.** Kotlin official code style; KDoc on public API; plain, precise English in documents.

## Licensing of contributions

Text contributions are accepted under CC BY 4.0 (see [`NOTICE.md`](NOTICE.md)). Code contributions cannot be accepted until the copyright holder chooses a software license.
