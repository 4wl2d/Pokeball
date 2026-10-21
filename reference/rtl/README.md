# Ballarch in register-transfer logic (SystemVerilog)

[`rtl/ballarch_issuer.sv`](rtl/ballarch_issuer.sv) is a hardware block that is a Ball. It accepts commands from upstream, issues each as a bus transaction to a device, and reports exactly one completion per command: `DONE`, `NOT_DONE` or `UNKNOWN`.

| Ballarch concept | Here |
|---|---|
| State owned by one writer (R1) | registers written by a single `always_ff` |
| Pure decider (R2) | one `always_comb` with a default for every next-state signal: no latch, so no state that a clock edge does not commit |
| Atomic commit (R4) | every register updates on the same edge |
| Commit before dispatch (R5) | every output is a function of registered state only |
| Send horizon | the `m_valid && m_ready` handshake on this bus, where a master may withdraw a request not yet accepted; on AXI, where VALID may not be withdrawn, it is the assertion of VALID |
| Effect classes | READ is Safe, CONFIG is Idempotent, FIRE is NonIdempotent |
| Obligation and deadline (R6) | one command in flight, a timeout counter per phase, a fresh tag per attempt; late responses carry an old tag and are dropped |
| Honest outcomes, no blind re-send (R7, R8) | withdrawal of a first attempt is `NOT_DONE`; no answer is `UNKNOWN` (FIRE, CONFIG after its retries) or `NOT_DONE` (READ); FIRE is never re-issued |
| Profile (R13) | Transient: registers do not survive reset |

## Checks

With Icarus Verilog 12, Verilator 5 and Yosys 0.33 or later, from this directory:

```sh
make            # = make lint rules sim mutants
```

- `lint`: Verilator `--lint-only -Wall` with no warnings.
- `rules`: Yosys structural checks. R1: no register has two drivers. R2: no latch is inferred. R5: no combinational path leads from any input to a dispatched output. The fixtures in [`lint/`](lint/) (two writers of one register, a decider that infers a latch) must fail.
- `sim`: [`tb/tb_issuer.sv`](tb/tb_issuer.sv) runs every operation under every combination of fates of its first two bus attempts (75 scenarios): answered in time, answered late, answer lost, refused, never accepted. The device is the ground truth, and the bench checks R5, R6, R7 and R8.
- `mutants`: three seeded bugs must each fail the bench: re-issuing FIRE, reporting `NOT_DONE` after an accepted request times out, and driving the bus request combinationally. The last one must also fail the structural R5 check.

The first draft of this block reported `NOT_DONE` when a CONFIG retry was withdrawn before acceptance, although the first attempt had already been applied. The bench found it in the scenario where the first answer is late or lost and the retry is never accepted. The rule it broke, R7, is the same one the Kotlin runtime enforces with its `Pending`/`Sending` phases.

These are bounded checks of one block in simulation, not formal verification of the RTL, and the code has no software license yet (see [`NOTICE.md`](../../NOTICE.md)).
