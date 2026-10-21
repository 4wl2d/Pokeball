# Alloy model of the call protocol

[`obligations.als`](obligations.als) models one caller Ball under the Durable profile, a network that loses and duplicates messages, and one destination. It abstracts business state away: a call is created by a committed decision and must receive exactly one terminal outcome. It checks properties P1–P6 and L1 within bounds. It also has one command per seeded runtime bug, and each of those commands is expected to find a counterexample.

Run it with:

```
bash formal/alloy/fetch-alloy.sh
python3 formal/alloy/run.py
```

`run.py` compares each command's result with its `expect` annotation and exits with a non-zero status if any differs.

## Correspondence with the reference runtime

Core §9.1 asks a conforming runtime to relate its call lifecycle to this model. For the Kotlin reference ([`Runtime.kt`](../../reference/kotlin/ballarch-runtime/src/main/kotlin/ballarch/runtime/Runtime.kt), [`Store.kt`](../../reference/kotlin/ballarch-runtime/src/main/kotlin/ballarch/runtime/Store.kt)) the correspondence is:

| Model | Reference runtime |
|---|---|
| `Phase`: `Pending`, `Sending` | `ballarch.runtime.Phase` in the call ledger of `InstanceRecord` |
| `Created`, `Awaiting` | the call ledger and the business state of one `InstanceRecord`, which is saved as a whole |
| `commit[c]` | `commit`: one `store.save` of the new state, the new ledger entries in phase `Pending`, replies and notices |
| `markSending[c]` | `startSend`: `saveLedger` with phase `Sending` and the next attempt number, before any transport runs |
| `send[c]` | `startSend`: `adapter.execute`, or delivery to a Ball port, after the write-ahead record |
| `retry[c]` | `failAttempt`: a new attempt for Safe and Idempotent ports within the retry budget; composition refuses retry policies on NonIdempotent ports. Each new attempt saves the ledger again with phase `Sending` and a higher attempt number. In the model the phase does not change, so that save is a stutter. |
| `receive[c]` | `onAdapterResult` with `AdapterResult.Ok`, or a Ball reply, then `resolve` with `Done` |
| `timeout[c]` | `onDeadline`: `Pending` gives `NotDone(DeadlineBeforeSend)`; `Sending` gives `NotDone(NoAnswer)` for a Safe port and `Unknown(DeadlineAfterSend)` otherwise |
| `crash` | `Runtime.crash()`: the instance stops processing, and callbacks from that incarnation are ignored. Only what `Store` holds survives. |
| `recover` | `Runtime.start()` on the same store calls `recover`. A `Pending` call is sent. A `Sending` NonIdempotent call ends `Unknown(RecoveredAfterCrash)`. A `Sending` repeatable call is sent again within its budget. A call past its deadline is classified as by `timeout`. |
| `deliver`, `dropRequest`, `dropResponse` | the destination and the network; in tests, the simulated providers and `ScriptedAdapter` |
| `stutter` | any step that changes no modelled variable, for example a decision without calls |

The model does not cover the following. Its properties say nothing about them; the trace oracles and `RuntimeRulesTest` check them instead.

- **Evidence of non-application.** `AdapterResult.Refused` and `AdapterResult.NotApplied` give `NotDone` only when the evidence covers every attempt that may have been delivered (`onAdapterResult`).
- **Transport failure classified before the deadline.** A failure reported by an adapter ends the call through `failAttempt`.
- **Cancellation.** `Cancel` withdraws a `Pending` call with `NotDone(Cancelled)` and is otherwise a hint to the adapter.
- **Several destinations and several instances.**
- **Inbound requests, replies and notices.**

The model is scoped to the Durable profile. For a Transient runtime the correspondence holds for executions without a crash: a Transient crash ends the instance together with its calls (Core §6).
