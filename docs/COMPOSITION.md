# Pokeball Composition Guide

> [!NOTE]
> This is a non-normative reading guide. The ordered Core document set rooted at [`spec/pokeball-architecture-core.md`](../spec/pokeball-architecture-core.md) is the sole normative authority; if this guide differs from a marked Core source clause, Core controls.

[← Pokeball overview](../README.md) · [Architecture](ARCHITECTURE.md) · **Composition** · [Adoption](ADOPTION.md) · [Agent Pack](agents/README.md)

Use this guide when another state owner is actually needed. A local composition can use ordinary types, calls and one shared acceptance mechanism. A helper, folder or command round trip alone does not create a Flow.

## A Counter with two capabilities

Counter owns one integer and permits increments up to 3. It exposes two target-owned views; Assembly supplies each consumer the appropriate one:

```text
CounterRead.value(): Int
CounterCommands.increment(): IncrementResult
IncrementResult = Changed(value: Int) | NotAccepted(reason: LimitReached)

pure counterDecide(value, Increment):
    if value >= 3: return NotAccepted(LimitReached)
    return Accepted(state = value + 1, result = Changed(value + 1))
```

`NotAccepted` means no Counter mutation or output was accepted. `Changed` is returned only after Counter accepts the change. The direct target call and its return establish correlation and provenance within this trusted same-build call scope. No command/result tokens, issuer enum, protocol-ID field or materialized envelope is needed solely because the call crosses a Ball. For an untrusted or independently delivered message, verify the actual source and carry the causal information its delivery needs.

Counter describes operations, not consumer names. Reading and incrementing remain capabilities of one owner. Adding an allowed consumer is another binding to `CounterCommands`; it does not change Counter's result type or business logic. If permission depends on the caller, Counter still makes that business decision from trusted input/context. Narrow interfaces express the available API; ordinary language visibility is not hostile-code containment.

## Share the acceptance mechanism

The following pseudocode uses one `SerialOwner` for Counter and a source. An implementation supplies the owning-thread check and acceptance mechanism:

```text
handle(input):
    requireOwningThreadAndNoActiveDecision()
    candidate = runPureDecisionWithoutReentry(committed.state, input)
    if candidate is NotAccepted: return candidate
    requireExecutorForPresentOutputs(candidate)
    committed = candidate                 // State and all outputs together.
    try:
        for output in candidate.outputs:  // Retained by this immediate call.
            execute(output)               // After acceptance, outside decide.
    catch UnhandledExecutorFault:
        retainFrameAndStopNewMutations(candidate)
        throw
    return candidate.result
```

State and outputs are immutable here. The owning thread is checked at entry; a decision cannot reenter its owner. The accepted call retains its complete frame while dispatch executes. An unhandled executor fault retains that frame and stops new mutations; supervision/recovery is outside this example. Counter returns immediately, so its accepted result needs no queue. These closed paths have no growing batch, detached work or retry. This small mechanism illustrates an `Inline + Transient + InProcess + Standard` binding; it is not a general runtime or a required library.

The source owns its current display. Its decision emits one increment request, and its executor supplies the returned value as a new source input:

```text
pure sourceDecide(state, Click):
    return Accepted(state = Waiting(state.displayed), outputs = [Increment])
pure sourceDecide(state, Returned(Changed(value))):
    return Accepted(state = Showing(value))
pure sourceDecide(state, Returned(NotAccepted(reason))):
    return Accepted(state = Refused(state.displayed, reason))
pure sourceDecide(state, ExecutionFailed):
    return Accepted(state = ExecutorFailed(state.displayed))

execute(Increment):
    try:
        result = counterCommands.increment()
    catch DeclaredExecutorFailure:
        source.handle(ExecutionFailed)
        return
    source.handle(Returned(result))

counter = SerialOwner(0, counterDecide)
reader = CounterRead(counter.read)
commands = CounterCommands(() => counter.handle(Increment))
source = makeSource(commands)
additionalConsumer = makeSource(commands)  // Assembly-only addition.
```

The executor runs only for an accepted source output. Counter accepts its own mutation, then the source accepts its display update through the same serialized mechanism. The current call associates the immediate return with the request; no missing tuple fields must be reconstructed to prove this order. The source's displayed number is a captured value, not another writable Counter authority.

Check these three paths in the consuming implementation:

| Path | Counter | Source |
|---|---|---|
| Increment below the limit | Accepts the increased value; returns `Changed(value)` | Accepts the returned display value |
| Increment at the limit | Returns `NotAccepted(LimitReached)` without acceptance | Accepts its refusal display; keeps the previous value |
| Executor fails after Counter acceptance | Accepted increment remains | Accepts `ExecutionFailed`; never records `NotAccepted` |

For the last path, inject a fault while handing back an already accepted result and verify that Counter's accepted change remains. This checks failure-stage separation within the local example. An external action that may have happened remains unknown until the contract has proof; neither a timeout nor an executor error means nonacceptance. Such a contract adds its own required closed outcomes rather than adopting one mandatory carrier shape.

## Check the work that can grow

This source emits once; Counter returns once; the result handler emits nothing. That complete structure bounds one invocation. It needs no numeric dependency/participant/route quota, causal depth field, level-1/level-2 reservations or separate geometric fan-out calculation. An import DAG alone is insufficient: emitting another increment from every result handler could create an unending chain even with the same imports.

Queues, external requests, retained outputs, retries and dynamic fan-out still need real limits. Reject work before acceptance if overflow could lose it afterward. When a cumulative causal budget applies, preserve it across handoff and retry. A project may choose numeric limits on static composition for a concrete reason, but adding a statically bound consumer does not automatically require a new global quota.

## Try three small changes

Use your project's binding and test framework for these exercises. Verify increments through 3, refusal beyond 3, source acceptance before target invocation, serialized source completion, and the post-acceptance fault path above. Keep the same behavioral assertions while changing the implementation.

The expected scope of each change is:

| Change | Domain code | Composition code | Repeated binding mechanics | Manually synchronized derived descriptions |
|---|---|---|---|---|
| Add a small indicator Ball | One toggle decision | One `SerialOwner` construction | None; existing class reused | None |
| Add an allowed Counter consumer | None | One `makeSource(commands)` connection | None | None |
| Rename a variable and extract the limit check | One renamed decision and one pure helper | None | None | None |

This table counts expected changed functions and connections, not measured performance or usability. Verify that boundary, refusal, acceptance-order and return assertions survive the refactor in your implementation. The example's types and wiring supply the inspectable source contract without a second manifest or route table; a tool can derive those views when needed. These documentation exercises do not establish ease of use for every system or for novice humans. [Project evaluation](EVALUATION.md) describes a broader measured comparison.

## How an application is composed

Pokeball distinguishes four application component roles:

| Role | Owns | Does not own |
|---|---|---|
| `Feature Ball` | A local business capability and its state authority | Another feature's internals or mutable state |
| `Flow Ball` | A specific multi-participant coordination lifecycle and terminal outcome | Participant domain facts or a global workflow engine |
| `Read Model Ball` | Derived query state, source positions, freshness, and rebuild policy | Command authority for source facts |
| Utility package | Pure stateless mechanics | State, protocol, lifecycle, or resource authority |

Real dependencies remain visible through target-owned interfaces and wiring. Physical helper imports remain in the acyclic compile-time graph; they are not another semantic dependency kind. An immediate trusted call can use its typed target as the effective operation identity. Explicit versions are needed when endpoints version or deploy independently. Assembly selects routes, versions and bindings; business choices and result meanings stay in owners.

**Material coordination** exists when one authority must own any independent workflow lifecycle, semantic ordering or branch/join, compensation or recovery, cancellation, reconciliation, or terminal outcome across participant authorities. One such property is sufficient when it makes the simple one-hop conditions false. Call count, sequential syntax, or one command round trip alone is not material coordination and does not require a `Flow Ball`.

### Reads, Draining, and status authority

A cross-authority `ReadDependency` resolves exactly one target authority, target-owned `Query -> ResultPayload` mapping and effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Caller and Assembly do not redefine the target result, stamp, status fact, or read meaning. Multiple reads remain independent and do not promise one atomic multi-source snapshot without a separate declared mechanism.

`Draining` rejects new logical mutations but serves every available declared `Query` and status Query from its committed authority. If the read authority is unavailable, the trusted boundary can return an existing validation/admission response before `read`; once admitted, `read(...) -> ReadResult` remains successful and creates no Decision.

Each status namespace has exactly one committed, revisioned, single-writer query authority without taking command authority over the underlying business facts. `OperationId` identifies an accepted root operation: reserving a candidate before `decide`, or rejecting root validation/admission/Decision before acceptance, creates no operation, handle, output, known status row, or retention marker. A covered committed snapshot may return `NotFound`; downstream target nonacceptance remains a Step facet of an already accepted source operation. Co-location can reduce lag and transaction boundaries; a separate status/Read Model authority can isolate query load and combine declared sources but pays materialization lag and source-position evidence. Neither choice permits two independently writable status answers.

### Security, context, and foundation

When actor, tenant, issuer, realm, assurance, or delegation can change a `Decision` or `Query`/status-read authorization or result selection, the trusted binding boundary constructs verified, bounded, field-minimized context against an approved issuer or equivalent fixed trusted same-stack proof. Forged, tampered, wrong-issuer/realm, missing, stale, or otherwise unverifiable required evidence fails closed. Actor-independent Decisions and reads create no actor-context, issuer, authentication, or actor-evidence artifacts.

The Nucleus Policy Gate alone decides business permission from committed State, the current cause or Query, and trusted context. Immediately before authoritative execution, the Resource/target Execution Gate verifies every triggered proof, capability, constraint, version, freshness/revocation, endpoint, quota, and safe-sink binding without making a new business decision. A post-acceptance gate failure remains a declared Resource/result/status path; it cannot roll back or downgrade accepted work or become a pre-acceptance carrier.

Shared foundation code contains mechanical primitives only. It owns no mutable business meaning, business-policy decision, domain or status authority, route selection, service locator, or hidden communication state. An ownerless shared domain/business utility is prohibited: keep the semantics Ball-local, including deliberate small duplication, or assign one Ball/Flow owner and expose its declared Application Surface/protocol.


---

[← Architecture](ARCHITECTURE.md) · [Pokeball overview](../README.md) · [Next: Adoption →](ADOPTION.md)
