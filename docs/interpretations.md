# Ballarch at every level

Ballarch's rules mention no language, transport or storage. This guide shows how to read them at each level where state is owned and interactions can fail, from a hardware block to a workflow across organisations. The mapping is in [Core §11](../spec/ballarch-core-2.0.md#11-interpretation-across-levels-informative), which is informative; the rules it interprets (§2–§7) are normative, and the examples below are informative.

At every level, ask the same five questions:

1. **Who owns this state?** Name the one Ball that changes it (R1).
2. **Where is the commit horizon?** Find the instant after which a decision and the effects it requests exist together (R4, R5).
3. **Where is the send horizon?** Find the first instant a request may have left the component, after which it can no longer be withdrawn without the destination's word (R7, R8).
4. **What is the effect class of each interface?** Decide whether repeating a request is harmless (Safe), deduplicated by the destination (Idempotent), or possibly effective on each delivery (NonIdempotent, the safe choice when unsure).
5. **What does `Unknown` mean here, and who reconciles it?** Name the evidence that would later settle it, and the component that collects it.

## Synchronous hardware (RTL)

| Question | Answer in a clocked design |
|---|---|
| Owner | the one process that writes a register; two writers are a multi-driver error |
| Decider | the combinational next-state logic; a latch inferred in it is hidden state |
| Commit horizon | the clock edge: every register takes its next value at once |
| Send horizon | the handshake that hands a request to the interconnect. On a bus that lets a master withdraw an unaccepted request, it is `valid && ready`. On AXI, VALID may not be withdrawn, so it is the assertion of VALID. |
| Effect classes | reading a plain status register is Safe; writing a configuration register with a value is Idempotent; pushing into a FIFO, writing a doorbell, or reading a read-to-clear register is NonIdempotent. Arm's Device memory type forbids speculative and repeated accesses for exactly this reason. |
| `Unknown` | an accepted transaction without a response by its timeout. A PCIe completion timeout on a non-posted write is the textbook case. |
| Reconciliation | read a status or counter register that records whether the command took effect |
| Profile | Transient: registers do not survive reset |

The reference block is [`reference/rtl/rtl/ballarch_issuer.sv`](../reference/rtl/rtl/ballarch_issuer.sv). Yosys checks one driver per register (R1), no latch in the decider (R2), and no combinational path from an input to a dispatched output (R5). A fault-injecting bench checks R5–R8 against the device model. Writing this block, the first draft reported `NOT_DONE` for a withdrawn retry after an earlier attempt had already been applied, and the bench caught it. The rules are subtle enough to need checks at this level too.

## Embedded firmware

| Question | Answer on a microcontroller |
|---|---|
| Owner | the task that owns a device protocol; its state lives in one committed record |
| Decider | a pure `step()` function in its own translation unit |
| Commit horizon | the NVM write that makes a new record valid: two slots, sequence number, CRC. A write torn by power loss leaves the previous record valid. |
| Send horizon | the write-ahead record that marks a command *possibly sent*, written before the bytes leave the UART, SPI or CAN controller |
| Effect classes | querying a sensor is Safe; setting a set-point is Idempotent; dosing, firing an actuator or dispensing is NonIdempotent |
| `Unknown` | a timeout or garbled answer after transmission; power loss while a command was *possibly sent* |
| Reconciliation | read the device's counter or log after its delivery horizon; never repeat a NonIdempotent command blindly |
| Profile | Durable, if state and the call ledger are kept in NVM and time comes from a clock that keeps running |

The reference is [`reference/c/`](../reference/c/): an infusion-pump controller explored over every link fate and every power-loss point, including torn writes. An `nm`-based check confirms that the decider object references no external symbol (R2) and defines no writable data (R1).

## Backend services

| Question | Answer in a service |
|---|---|
| Owner | an aggregate or entity instance; one Ball instance per key |
| Commit horizon | the database transaction that stores the new state with its outbox and call ledger |
| Send horizon | the write-ahead `Sending` record before the HTTP or gRPC request |
| Effect classes | GET is Safe; PUT and requests with an idempotency key are Idempotent; most POSTs to third parties are NonIdempotent |
| `Unknown` | a timeout, a reset after the request was written, or a restart while `Sending` |
| Reconciliation | a status endpoint after the provider's delivery horizon, or a provider that accepts idempotency keys, which makes the port Idempotent |

See the [tutorial](tutorial.md), the [shop example](../reference/kotlin/examples/shop/), and the [benchmark](../benchmark/), which compares this with layered, actor, MVU and saga designs.

## User interfaces and mobile clients

| Question | Answer in a client |
|---|---|
| Owner | a screen or feature state machine; one Ball per feature instance |
| Commit horizon | the state update that also records the request it starts. In a client without persistence (Transient), a crash loses both. |
| Send horizon | the network call |
| `Unknown` | the request timed out, or the app was suspended or killed with the request in flight |
| What to show | a third state, "we don't know yet; checking", rather than an error that invites the user to submit again. Re-query when the app resumes. |

The [catalog example](../reference/kotlin/examples/catalog/) is a read-only (Safe) search: no `Unknown` arises, so none of the reconciliation machinery appears.

## Workflows across services and organisations

| Question | Answer in a workflow |
|---|---|
| Owner | the Flow Ball whose state is the workflow (Core §7.2) |
| Composite claims | derive every statement about the whole from the knowledge algebra (Core §4.2, rule R14). A booking whose flight leg is `Unknown` and whose hotel leg is `NotDone` is `NotAllDone`, not "failed". |
| Compensation | count it as undoing an effect only if it is absorbing: the partner fences the reference, or the original request's horizon has passed |
| Closure | a Flow that obeys R14 is one honest port for the level above (Core §7.4) |

See the [trip example](../reference/kotlin/examples/trip/). Under every fault and crash it explores, its status never contradicts the providers' bookings; the usual two-valued status is wrong in 19% of the same runs.

## What does not change between levels

- The rules R1–R14, and the meaning of `Done`, `NotDone` and `Unknown`.
- The question a reviewer asks: *can this component claim something its evidence does not establish?*

What changes is the mechanism that checks the rules: bytecode rules and trace oracles in a JVM service, object-file checks and power-loss exploration in firmware, lint and structural netlist checks in RTL.
