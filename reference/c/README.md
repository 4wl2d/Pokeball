# Ballarch in firmware (C11)

The same contract as the Kotlin reference, for a microcontroller that owns a device protocol. This is a reference for the rules, not a production RTOS component.

| Ballarch concept | Here |
|---|---|
| Ball, decider (R2) | `th_step` in [`examples/dosing/dosing.c`](examples/dosing/dosing.c): a pure function from (state, input, context) to (next state, calls, reply) |
| Atomic commit (R4) | one record (state + call ledger) written to one of two NVM slots with a sequence number and CRC-32 ([`src/ballarch_runtime.c`](src/ballarch_runtime.c)); a torn write leaves the previous record valid |
| Commit before dispatch (R5) | `transmit` is called only after the record that holds the call has been written |
| Write-ahead, no blind re-send (R8) | each attempt first records phase *Sending*; after a power loss a *Sending* call on a NonIdempotent port ends `Unknown(RecoveredAfterCrash)` |
| Honest outcomes (R7) | NACK before the command was latched is `NotDone`; a timeout or garbled answer after sending is `Unknown` (NonIdempotent) or a retry (Safe, Idempotent) |
| Profile (R13) | Durable: state, call ledger and timers survive power loss; time comes from a clock that keeps running |

The example is a therapy controller that gives a prescribed number of doses through an infusion pump whose dose command has no idempotency key. A dose with an unknown fate is never repeated: the controller waits past the link's delivery horizon and reads the pump's dose counter (a Safe port).

## Checks

From this directory, with a C11 compiler and binutils:

```sh
make            # = make test purity mutants
```

- `test` runs the decider unit tests and an exhaustive exploration of every link fate for every transmission and one power loss at any point: between loop iterations, before or in the middle of an NVM write, and after the write-ahead record but before the bytes leave. The pump is the ground truth. The oracles check R5, R6, R7 and R8, no overdose, and that the final state matches the pump. With the default bounds this is about 220,000 runs.
- `purity` checks the decider's object file. R2: it references no external symbol except `memcpy`, `memset` and `memmove`. R1: it defines no writable data. The impure fixture in [`tools/fixtures/`](tools/fixtures/) must fail both checks.
- `mutants` builds three seeded bugs and requires each to produce a counterexample: transmitting before the write-ahead record, re-sending a possibly-sent dose after a power loss, and a decider that doses again after `Unknown`.

These are bounded checks of one example on a simulated platform. They are not a proof for all programs, schedules or hardware, and the code has no software license yet (see [`NOTICE.md`](../../NOTICE.md)).
