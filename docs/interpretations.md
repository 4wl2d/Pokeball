# Ballarch at every level

Ballarch's rules mention no language, transport or storage. This guide shows how to read them at each level where state is owned and interactions can fail, from a hardware block to a workflow across organisations. The mapping is in [Core §11](../spec/ballarch-core-2.0.md#11-interpretation-across-levels-informative), which is informative; the rules it interprets (§2–§7) are normative, and the examples below are informative.

The first five sections cover levels that have a reference or an example in this repository. [Levels without reference code](#levels-without-reference-code) applies the same questions to device drivers, accelerators, network stacks, RTOS tasks, PLCs, automotive ECUs, databases, data pipelines, offline-first mobile apps and smart contracts.

At every level, ask the same five questions:

1. **Who owns this state?** Name the one Ball that changes it (R1).
2. **Where is the commit horizon?** Find the instant after which a decision and the effects it requests exist together (R4, R5).
3. **Where is the send horizon?** Find the first instant a request may have left the component, after which it can no longer be withdrawn without the destination's word (R7, R8).
4. **What is the effect class of each interface?** Decide whether repeating a request is harmless (Safe), deduplicated by the destination (Idempotent), or possibly effective on each delivery (NonIdempotent, the safe choice when unsure).
5. **What does `Unknown` mean here, and who reconciles it?** Name the evidence that would later settle it, and the component that collects it.

Idempotent has the narrow meaning of Core §2: the destination recognises a repeat by an identity that every attempt carries unchanged, such as an idempotency key, a sequence number or a command identifier, and applies it once. A request that only sets a value is not Idempotent in this sense, although HTTP calls a PUT idempotent: the destination applies every delivery, and a late attempt can arrive after a later request and undo it. Declare such a port NonIdempotent unless the destination deduplicates. Some rows below name the conditions under which sending the same value again is still harmless. Under them, a decider that received `Unknown` may decide to send the value again as a new call; the runtime never re-sends it on its own (R8).

## Synchronous hardware (RTL)

| Question | Answer in a clocked design |
|---|---|
| Owner | the one process that writes a register; two writers are a multi-driver error |
| Decider | the combinational next-state logic; a latch inferred in it is hidden state |
| Commit horizon | the clock edge: every register takes its next value at once |
| Send horizon | the first cycle from which the request can no longer be withdrawn from the interconnect. On a bus that lets a master withdraw an unaccepted request, it is the `valid && ready` handshake. On AXI, VALID may not be withdrawn once asserted, so it is the assertion of VALID, not the handshake. |
| Effect classes | reading a plain status register is Safe. Writing a configuration register is Idempotent only if every attempt carries the same command identifier and the device applies each identifier at most once. Otherwise it is NonIdempotent: writing the same value again is harmless only while the interconnect delivers this master's writes in issue order (on AXI, only if every write to that device uses the same ID), since a late earlier attempt can overwrite a later command. Pushing into a FIFO, writing a doorbell, or reading a read-to-clear register is NonIdempotent. Arm's Device memory type forbids speculative and repeated accesses for exactly this reason. |
| `Unknown` | an accepted transaction without a response by its timeout, for example a PCIe Completion Timeout on a non-posted request (a configuration or I/O write, or an AtomicOp). Ordinary memory writes are posted and get no completion, so they give no evidence until a later read from the same device completes. |
| Reconciliation | read a status or counter register that records whether the command took effect |
| Profile | Transient: registers do not survive reset |

The reference blocks are [`ballarch_issuer.sv`](../reference/rtl/rtl/ballarch_issuer.sv), which issues commands, and [`ballarch_composite.sv`](../reference/rtl/rtl/ballarch_composite.sv), which performs a two-step operation through it and keeps the operation's knowledge (R14) in a status register. The block is Transient, so the register covers only operations since its last reset. Each truth value *a* takes two bits, *a1* (may be Yes) and *a0* (may be No): Yes is `10`, No is `01` and Maybe is `11`. Kleene's AND is then `{a1 & b1, a0 | b0}` and OR is `{a1 | b1, a0 & b0}`. Yosys checks one driver per register (R1), no latch and no storage other than flip-flops (R2), one rising clock edge for all state with no input reaching an asynchronous pin (R4), and no path from an input to an output except through a register (R5). Fault-injecting benches check R5–R8 against the device model, and R14 for the composite's status register. Writing the issuer, the first draft reported `NOT_DONE` for a withdrawn retry after an earlier attempt had already been applied, and the bench caught it; the bug is kept as a seeded bug that the bench must catch. The rules are subtle enough to need checks at this level too.

## Embedded firmware

| Question | Answer on a microcontroller |
|---|---|
| Owner | the task that owns a device protocol; its state lives in one committed record |
| Decider | a pure `step()` function in its own translation unit |
| Commit horizon | the NVM write that makes a new record valid: two slots, sequence number, CRC. A write torn by power loss leaves the previous record valid. |
| Send horizon | the write-ahead record that marks a command *possibly sent*, written before the bytes leave the UART, SPI or CAN controller |
| Effect classes | querying a sensor is Safe. Setting a set-point is Idempotent if every attempt carries the same sequence number and the device ignores one it has already applied; otherwise it is NonIdempotent, and sending the same set-point again is harmless only over a link that delivers commands in order. Dosing, firing an actuator or dispensing is NonIdempotent. |
| `Unknown` | a timeout or garbled answer after transmission; power loss while a command was *possibly sent* |
| Reconciliation | read the device's counter or log after its delivery horizon; never repeat a NonIdempotent command blindly |
| Profile | Durable, if state and the call ledger are kept in NVM and time comes from a clock that keeps running |

The reference is [`reference/c/`](../reference/c/), with two examples: an infusion-pump controller, and a two-valve transfer whose controller keeps its knowledge (R14) in the committed record and returns it to a status request. Both are explored by a bounded exhaustive search with one fault at every power-loss point and NVM write, including torn writes, and over the link fates of the first transmissions (6 for dosing, 7 for the transfer; more in `make deep`). A purity lint reads the decider's object file (no external symbol except compiler helpers, no writable section), its preprocessed source (no `volatile`, assembly, pointer casts or hosted headers) and, with clang, its syntax tree. It is necessary, not sufficient: the reference's README lists what it does not detect, such as pointer arithmetic from an argument into other memory.

Intermittent-computing research meets the same hazard on energy-harvesting devices. Code re-executed after a power failure can corrupt memory through I/O (Surbatovich, Jia and Lucia 2019), peripheral operations need their own checkpointing (Maeng and Lucia 2019), and EaseIO (Yildiz et al. 2023) lets the programmer annotate each I/O call with its re-execution semantics, the closest firmware analogue of effect classes. Those systems aim to make re-execution correct. Ballarch instead never re-executes a NonIdempotent command whose fate is unknown: it reports `Unknown` and leaves the follow-up to a Safe status query.

## Backend services

| Question | Answer in a service |
|---|---|
| Owner | an aggregate or entity instance; one Ball instance per key |
| Decider | the Ball type's `decide`, `complete` and `observe` |
| Commit horizon | the database transaction that stores the new state with its outbox and call ledger |
| Send horizon | the write-ahead `Sending` record before the HTTP or gRPC request |
| Effect classes | GET is Safe. A request with an idempotency key is Idempotent if the provider deduplicates by it. A PUT without a key is NonIdempotent here: a late attempt can overwrite a later request to the same resource. Sending it again is harmless only if no later request to that resource is sent before every earlier attempt is answered or past its delivery horizon. Most POSTs to third parties are NonIdempotent. |
| `Unknown` | a timeout, a reset after the request was written, or a restart while `Sending` |
| Reconciliation | a status endpoint after the provider's delivery horizon, or a provider that accepts idempotency keys, which makes the port Idempotent. Runtime retries then cover a lost answer only if the adapter ends each attempt before the call's deadline (Core §8.4) |
| Profile | Durable when state, outbox and call ledger are committed in one store transaction; Transient with an in-memory store |

See the [tutorial](tutorial.md), the [shop example](../reference/kotlin/examples/shop/), and the [benchmark](../benchmark/), which compares this with naive and careful layered, actor, MVU and saga designs.

## User interfaces and mobile clients

| Question | Answer in a client |
|---|---|
| Owner | a screen or feature state machine; one Ball per feature instance |
| Commit horizon | the state update that also records the request it starts. In a client without persistence (Transient), a crash loses both. |
| Send horizon | the network call |
| Effect classes | a search or status query is Safe. A submit that carries an idempotency key, generated by the decider from the context and stored with the request, is Idempotent if the server deduplicates by it. A submit without a key is NonIdempotent. |
| `Unknown` | the request timed out, or the app was suspended or killed with the request in flight |
| Reconciliation | when the app resumes, ask a Safe status endpoint about each request that may have been sent |
| What to show | a third state, "we don't know yet; checking", rather than an error that invites the user to submit again |
| Profile | Transient in memory; Durable if state and queued requests are kept in an on-device database (see [offline sync](#mobile-apps-with-offline-sync)) |

The [catalog example](../reference/kotlin/examples/catalog/) is a read-only (Safe) search: no `Unknown` arises, so none of the reconciliation machinery appears.

## Workflows across services and organisations

| Question | Answer in a workflow |
|---|---|
| Owner | the Flow Ball whose state is the workflow (Core §7.2) |
| Effect class of its port | Idempotent: a Flow's port is a Ball port, and the runtime deduplicates requests by their identity (Core §3.4) |
| Composite claims | derive every statement about the whole from the knowledge algebra (Core §4.2, rule R14), and count each call the Flow may still make, including a compensation, as `Maybe`. A booking whose flight leg is `Unknown` and whose hotel leg is `NotDone` is `NotAllDone`, not "failed". |
| Compensation | count it as undoing an effect only if it is absorbing: the partner fences the reference, as the fence of Try-Confirm/Cancel frameworks does when a cancel arrives before the try, or the original request's horizon has passed |
| Reconciliation | per leg, through each provider's Safe status port |
| Profile | Durable: the Flow keeps its state and call ledger as long as its legs' effects persist (Core §7.4) |
| Closure | a Flow that obeys R14 and answers with its settled knowledge, in a deferred reply, is one honest port for its caller, which may itself be a Flow (Core §7.4). A status given before the workflow settles either counts every call the Flow may still make as `Maybe` or is a snapshot that says so. A caller whose deadline is shorter than the time the Flow needs to settle gets `Unknown`, which is honest but loses information. |

See the [trip example](../reference/kotlin/examples/trip/). Under every fault and crash it explores, including booking requests that reach a provider after their compensation, neither the status it holds at any step nor its settled knowledge contradicts the providers' bookings. The usual two-valued status is wrong in 1,190 of the 6,435 settled runs, all of them runs in which the trip gave up compensating. The [itinerary example](../reference/kotlin/examples/itinerary/) nests the trip in another Flow: it books two trips through the trip's port, adds their settled replies, and stays honest in every run it explores.

Hardware readers can read `Maybe` as Verilog's `x`. *all* and *any* are an AND and an OR over the legs' net effects, and each leg enters once, so `x` propagates through them exactly, without the pessimism of reconvergent fanout (Core §4.2).

## Levels without reference code

The sections below apply the five questions to levels for which this repository has no reference code and no checks. They show how to answer the questions, not that any product conforms. The last row of each table names checks that would apply by analogy with the references; none of them exists here.

### Operating-system device drivers

NVMe and virtio queues, and DMA rings in general.

| Question | Answer in a driver |
|---|---|
| Owner | the driver's state for one submission and completion queue pair, including its table of outstanding commands. The interrupt handler is an inbound adapter: it hands completion entries to the owner and decides nothing. |
| Commit horizon | the update that records the command's identifier with its request in the outstanding table. The submission entry or DMA descriptor written after it can still be withdrawn, because the device cannot see it yet. |
| Send horizon | the write that publishes the entry to the device: the NVMe submission-queue tail doorbell, the virtio available-ring index (a device that polls the ring can see it before any notification), or the tail register or ownership bit of a DMA ring |
| Effect classes | a read or an Identify command is Safe. The device does not recognise a resubmitted command, so a write is NonIdempotent: a resubmitted write can land after a later write to the same blocks. Writing the same data to the same block range again is harmless only if no other write to that range can come between. NVMe Zone Append, where the device chooses the address, writes the data a second time if repeated. |
| `Unknown` | a command that timed out after the doorbell and was still outstanding when the driver reset the controller |
| Reconciliation | the queue's owner, after the reset: read back the block range, the zone's write pointer or a device log. A NonIdempotent command outstanding at the reset is not resubmitted; its submitter receives `Unknown`. |
| Profile | Transient: queue state does not survive a controller reset |
| Checks that would apply | the C reference's purity lint on a decider compiled on its own (R1, R2); fault injection that drops completions and resets a device model at every point between doorbell and completion (R5–R8) |

### GPU and accelerator command queues

| Question | Answer for a GPU or accelerator |
|---|---|
| Owner | the driver's or user-mode runtime's state for one hardware queue or context: submitted jobs and their fence values |
| Commit horizon | recording the job and the fence value that will signal its completion, before the commands are written to the ring buffer |
| Send horizon | the write-pointer update (doorbell) that lets the command processor fetch the commands |
| Effect classes | reading a fence or a timestamp is Safe. The command processor does not recognise a resubmitted job, so every job is NonIdempotent. A kernel that computes its outputs only from inputs it does not modify gives the same result if it runs again, provided nothing else writes those outputs in between; a kernel that accumulates with atomics, appends to a buffer or updates data in place does not. |
| `Unknown` | a hang detected by a watchdog (TDR on Windows, the DRM scheduler's job timeout on Linux), followed by a reset with jobs in flight. Vulkan reports it as `VK_ERROR_DEVICE_LOST`: pending work may have failed, and the memory it used becomes undefined. |
| Reconciliation | the application that submitted the job, after the reset: it may run a kernel of the first kind again from inputs it still holds; for one of the second kind it first restores or recomputes the buffers |
| Profile | Transient |
| Checks that would apply | a trace oracle that the runtime resubmits no job after a reset and reports each job in flight at the reset as `Unknown` (R7, R8) |

### Network stacks and NICs

| Question | Answer with a network stack |
|---|---|
| Owner | the state of one connection (a TCP control block, a QUIC connection), processed by one context at a time; a NIC transmit ring belongs to one driver queue |
| Commit horizon | for an application, the commit of the decision that requests the call, as in a service |
| Send horizon | for an application, the `send()` or `write()` that copies the request into the socket buffer; for a driver, the tail-register write that hands a transmit descriptor to the NIC |
| Effect classes | a TCP retransmission is an Idempotent retry: the receiver deduplicates by sequence number, but only within one connection. A request sent again on a new connection is NonIdempotent unless the application protocol carries a key. |
| `Unknown` | a reset or timeout after the request left the socket buffer. A returned `send()`, a NIC transmit completion or a TCP acknowledgment shows that bytes left or reached the peer's stack, not that the peer's application applied the request: that knowledge needs an answer from the endpoint (the end-to-end argument of Saltzer, Reed and Clark). |
| Evidence for `NotDone` | HTTP/2's REFUSED_STREAM, or a GOAWAY whose last stream identifier is below the request's stream, proves that the server did not process the request (R7) |
| Reconciliation | the application that owns the request, through a Safe status query keyed by the request's identifier, on a new connection |
| Profile | Transient: connection state does not survive a restart |
| Checks that would apply | fault injection that drops, delays and duplicates segments and resets connections at every point, against the peer's ground truth (R6–R8) |

### RTOS tasks and embedded Linux services

This extends the [firmware](#embedded-firmware) section to preemptive systems.

| Question | Answer with an RTOS or embedded Linux |
|---|---|
| Owner | one task, or one single-threaded service, per device protocol. Interrupt handlers and other tasks only post messages to its queue, for example with FreeRTOS `xQueueSendFromISR` or Zephyr `k_msgq_put`. |
| Decider | the task's step from (state, message) to (state, commands), with no blocking call and no shared variable |
| Commit horizon | in RAM, the state update before the task issues a command; for Durable state, an NVM record as in firmware, or on Linux a file written, forced to the device and renamed over the old one, as the reference `FileStore` does |
| Send horizon | the write that starts a transfer: a DMA descriptor handed to the controller, a transmit register, a CAN mailbox's transmission request, or a `write()` to a device file |
| Effect classes | as in firmware |
| `Unknown` | a watchdog reset or brown-out while a command was possibly sent; a started DMA transfer whose completion never came. A CAN controller's confirmation of a transmit abort shows only that it will not send the frame again, so the command stays `Unknown`. It is evidence for `NotDone` only if the controller also reports that transmission never started, for example because the frame was still waiting for the bus: receivers accept a frame at the last-but-one bit of end-of-frame, one bit before the transmitter counts it as sent, so an error in the last bit can make the transmitter give up on a frame that receivers have accepted. |
| Reconciliation | the owning task after a restart, from the device's status or counters |
| Profile | Transient, unless the task's state and call ledger are kept in NVM or a file; a watchdog reset is then a crash under the Durable profile |
| Checks that would apply | the C reference's checks, for a decider built as its own translation unit: the purity lint (R1, R2) and power-loss exploration, which also covers a watchdog reset at every point (R4–R8) |

### Programmable logic controllers

| Question | Answer in a PLC |
|---|---|
| Owner | the program or task that writes an output or variable; two tasks writing the same output are the multi-driver error of RTL |
| Decider | the scan-cycle logic, which computes the output image from the input image read at the start of the scan |
| Commit horizon | the end of the scan, when the output image is complete |
| Send horizon | the copy of the output image to local outputs, or the fieldbus frame that carries it |
| Effect classes | reading an input is Safe. Cyclic process data that holds a level or a set-point is state that the output image publishes every cycle, not a call: each frame carries the whole value, and a lost frame is corrected by the next one as long as the fieldbus delivers frames in cycle order. Nothing deduplicates it, so it is not Idempotent in the sense of Core §2. A toggle bit, a relative move or a counted dose is NonIdempotent. |
| `Unknown` | a command such as a relative move whose acknowledgment did not arrive; a PLC stop or power loss after it was sent |
| Reconciliation | the program, after a warm restart, from the drive's actual position or the device's counter |
| Profile | Durable only if the platform saves retentive (`RETAIN`) variables consistently at a scan boundary, together with the record of commands sent; otherwise Transient. Retentive memory that is updated in place during the scan, or saved whenever power fails, can hold a half-applied decision after a power loss, which R4 forbids. |
| Checks that would apply | a cross-reference check that every output has one writer (R1), the analogue of the RTL single-driver check |

### Automotive ECUs

AUTOSAR Classic-style communication between software components.

| Question | Answer in an ECU |
|---|---|
| Owner | a software component's data, changed only by its own runnables; under R1 a sender-receiver data element has one sender (1:n), not several (n:1) |
| Decider | a runnable that reads its inputs and writes its outputs through the RTE and has no other side effect |
| Commit horizon | with implicit communication (`Rte_IRead`, `Rte_IWrite`), the end of the runnable, when its writes become visible together; for data that must survive a reset, the completed NvM block write |
| Send horizon | the communication stack handing the PDU to the bus interface (CAN, FlexRay or Ethernet) |
| Effect classes | a periodic signal that carries a state (a requested torque, a lamp status) is state published every period, not a call: the receiver keeps the latest value, and a lost frame is corrected by the next one. Nothing deduplicates it, so it is not Idempotent in the sense of Core §2. An event-triggered command or a client-server operation (unlock the doors, start a routine) is NonIdempotent, unless every attempt carries the same command identifier and the receiver ignores one it has already applied. An E2E counter does not serve: the sender increments it on every send request, so a command sent again carries a new counter, and the check's `E2E_P_REPEATED` status detects only a frame repeated on the way. |
| `Unknown` | a client-server call over the bus that ends with `RTE_E_TIMEOUT`; an ECU reset or bus-off with a command possibly sent |
| Reconciliation | the calling component, from the server's status signal (for example the lock state) |
| Profile | Transient in RAM; Durable for state kept in NvM blocks written before the command is sent |
| Checks that would apply | a check that every NonIdempotent client-server call treats `RTE_E_TIMEOUT` as `Unknown`, not as failure (R7 at the application level) |

### Databases and storage engines

| Question | Answer with a database |
|---|---|
| Owner | inside an engine, the transaction that holds a row's lock or writes its version, and the log writer that appends to the write-ahead log; outside it, the application Ball that runs the transaction |
| Commit horizon | the commit record forced to stable storage (`fsync` or `fdatasync` of the log). With group commit, one flush carries the commit records of several transactions, and each crosses the horizon when that flush completes. |
| Send horizon | for the engine, the commit acknowledgment to the client; for a client, `COMMIT` written to the socket |
| Effect classes | a read-only transaction is Safe; an insert keyed by a client-chosen identifier that does nothing on conflict is Idempotent; an insert with a server-generated key, or an increment, is NonIdempotent |
| `Unknown` | the connection drops after `COMMIT` was sent and before the reply. PostgreSQL defines SQLSTATE 08007 (`transaction_resolution_unknown`), though a client usually sees only a broken connection; CockroachDB reports 40003 ("result is ambiguous"); MongoDB labels the error `UnknownTransactionCommitResult`. Inside the engine, a failed `fsync` of data files leaves their state unknown: PostgreSQL's documentation warns that a retried flush "may be reported as successful, when in fact the data has been lost", so by default the server stops and recovers from the log. |
| Reconciliation | the client, after reconnecting, through a Safe query for a token written in the same transaction. An in-doubt two-phase-commit branch is resolved by its transaction manager. |
| Rule to watch | an engine that acknowledges a commit before the flush sends before its commit horizon (R5). PostgreSQL's `synchronous_commit = off` does this on purpose: a crash can lose recently acknowledged transactions. |
| Profile | Durable |
| Checks that would apply | kill and power-loss tests of the commit path, like the reference `FileStore` test (which covers killed processes only); a history checker such as Elle over the client's outcomes, with `Unknown` recorded as indeterminate |

### Data pipelines

| Question | Answer in a stream pipeline |
|---|---|
| Owner | the keyed operator state for one key (Flink keyed state, a Kafka Streams state store) |
| Commit horizon | the completed checkpoint that stores operator state together with the input offsets |
| Send horizon | for a transactional sink, the write into an open transaction, which `read_committed` readers see only after the commit that follows the checkpoint; for a non-transactional sink, the call |
| Effect classes | an upsert that carries a version, such as the input offset, and that the sink ignores unless the version is newer than the stored one, is Idempotent. A plain upsert by primary key, replayed in order after recovery, ends with the same rows, but readers can see older values again during the replay; nothing deduplicates it, so it is not Idempotent in the sense of Core §2. A Kafka producer with a stable `transactional.id` writes atomically with the offsets and fences older instances. The idempotent producer deduplicates retries only within one producer session, so a restart can still duplicate. An e-mail, webhook or payment sink is NonIdempotent. |
| `Unknown` | a failure between a NonIdempotent sink call and the next completed checkpoint; a sink commit whose acknowledgment was lost (Flink commits pre-committed transactions again after recovery, so that commit must be idempotent) |
| What "exactly once" covers | operator state and transactional sinks. A side effect in a function, or a non-transactional sink, is replayed after recovery, because it sends before the commit horizon (R5). Write the request to the checkpointed output instead, and let a separate sender with its own call ledger own the calls. |
| Reconciliation | that sender, through the destination's status endpoint |
| Profile | Durable |
| Checks that would apply | crash exploration at every point between a sink call and the completion of the checkpoint, against the sink's ground truth (R5–R8) |

### Mobile apps with offline sync

| Question | Answer in an offline-first app |
|---|---|
| Owner | the device owns drafts and its queue of pending requests; the server's Ball owns the shared record. An edit made offline is a request that the server decides, not a second writer. Replicated data types that merge concurrent edits (CRDTs) give up the single owner and are outside R1. |
| Commit horizon | the on-device database transaction (SQLite, Room, Core Data) that stores the screen state together with the queued request and its idempotency key |
| Send horizon | the sync worker handing the request to the HTTP client |
| Effect classes | a status query is Safe; a request that carries its stored idempotency key is Idempotent if the server deduplicates by it; without a key it is NonIdempotent and is not re-sent automatically |
| `Unknown` | a timeout, a lost connection, or the app suspended or terminated while a request was in flight |
| Reconciliation | the sync worker, when the app resumes or the network returns: it re-sends with the same key or asks a Safe status endpoint. Meanwhile the screen shows the request as pending, not failed. |
| Profile | Durable |
| Checks that would apply | the Kotlin test kit's fault exploration against a simulated server, if the client's logic is written as Balls on the JVM (R5–R8, R14) |

### Smart contracts

| Question | Answer with a smart contract |
|---|---|
| Owner | two Balls: the contract, whose storage only its own code changes, and the off-chain sender that signs and broadcasts transactions to it |
| Decider | the contract's function: deterministic, with the block number and timestamp supplied as context; it cannot perform off-chain I/O |
| Commit horizon, off-chain sender | the commit of its own store that records the signed transaction together with its nonce |
| Send horizon, off-chain sender | broadcasting the signed transaction |
| Commit horizon, contract | finality of the block that includes the transaction. Inclusion alone can be undone by a chain reorganisation until the block is final. |
| Send horizon, contract | a relayer or oracle acting off chain on the contract's event |
| Effect classes | a read-only call is Safe. On an account-based chain such as Ethereum, broadcasting the same signed transaction again is Idempotent, because the account nonce lets it execute at most once. Signing a new transaction for the same intent is NonIdempotent. |
| `Unknown` | a broadcast transaction that is neither included nor replaced; a transaction included in a block that is not yet final |
| Evidence for `NotDone` | another transaction from the same account with the same nonce, included in a final block: the original can then never execute. A same-nonce replacement is therefore an absorbing compensation. |
| Rule to watch | a relayer or oracle that acts on an event before finality sends before the contract's commit horizon (R5); a reorganisation can remove the event after the effect happened |
| Reconciliation | the off-chain sender, from the transaction receipt and the account's nonce at a final block |
| Profile | Durable on chain; the off-chain sender needs its own durable call ledger |
| Checks that would apply | crash and reorganisation exploration of the off-chain sender against a simulated chain (R5–R8) |

## What does not change between levels

- The rules R1–R14, and the meaning of `Done`, `NotDone` and `Unknown`. A level without some element, such as deferred replies, has nothing for the corresponding rule to check (Core §9.4).
- The question a reviewer asks: *can this component claim something its evidence does not establish?*

What changes is the mechanism that checks the rules: bytecode rules and trace oracles in a JVM service; a purity lint and power-loss exploration in firmware; lint, structural netlist checks and fault-injecting benches in RTL. All three references check the call-protocol rules and the knowledge algebra: the JVM reference with its trip and itinerary examples, the firmware with its C algebra and transfer example, the RTL with its composite block. The itinerary nests one Flow inside another, two depths of composition at one level, but no reference composes different levels, for example firmware that drives the RTL block as a Ball behind a port ([Core §11](../spec/ballarch-core-2.0.md#11-interpretation-across-levels-informative)).
