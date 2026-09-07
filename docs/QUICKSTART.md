# Write your first Pokeball feature

> [!NOTE]
> This is a non-normative teaching guide. The ordered [Core document set](../spec/pokeball-architecture-core.md) defines Pokeball. The examples explain selected paths; they are not a runtime implementation or a conformance verdict.

[Overview](../README.md) · **Quickstart** · [Adoption](ADOPTION.md) · [Evaluate a project](EVALUATION.md)

Start with one owned fact and one rule. A Ball can be one file. The useful separation is between adapting input, deciding what changes, accepting that change, and executing any resulting actions.

## A complete local example

An order draft owns the quantity the customer intends to buy. Its business rule allows quantities from 1 through 20. It does not own inventory, accept an order, reserve stock, calculate payment, or perform I/O.

The host gives this draft one private, serial call scope on one owning thread. Calls neither yield nor invoke callbacks, and another thread cannot access its state. That confinement must actually be enforced by the chosen language and host; a comment is insufficient. The immutable, fixed-size state is published by one assignment. Losing the process loses the draft.

These conditions select `Inline + Transient + InProcess + Standard`, with static composition. The language-neutral pseudocode below shows the whole feature in one file:

```text
record State { quantity: UInt8 }
enum Intent { QuantitySelected(value: UInt8) }
enum Query { GetQuantity }
enum BusinessRejection { QuantityOutsideRange }
record QuantityNotAccepted { reason: BusinessRejection }

pure decide(state: State, intent: Intent):
    match intent:
        QuantitySelected(value):
            if value < 1 or value > 20:
                return Rejected(QuantityOutsideRange)
            return Accepted(SnapshotDecision(State(quantity = value)))

pure read(state: State, query: Query):
    match query:
        GetQuantity: return state.quantity

scope OrderDraft on its enforced owning thread:
    private committed = State(quantity = 1)

    selectQuantity(value: UInt8):
        // Interaction: this trusted typed entry point accepts a UInt8.
        candidate = decide(committed, QuantitySelected(value))
        match candidate:
            Rejected(reason):
                return QuantityNotAccepted(reason)
            Accepted(decision):
                // Acceptor: the only state publication, with no suspension.
                committed = decision.nextState
                return normally

    quantity():
        return read(committed, GetQuantity)
```

`UInt8` means an unsigned integer from 0 through 255. Its representation is bounded; the narrower 1–20 range is deliberately a business rule in `decide`. If a UI or HTTP adapter receives text instead, it parses and checks the `UInt8` representation before invoking this entry point. It does not move the 1–20 policy into parsing. See [validation versus business policy](../spec/core/03-model-boundaries-zones.md#51-interaction-hemisphere).

`Intent` is this Ball's only `Pulse` variant. Its `DecisionContext` is semantically `Unit`: no clock, actor, configuration, or other contextual field changes the decision. The closed semantic-output set is empty, so `SnapshotDecision(nextState)` omits the empty output collection. These are permitted [representational omissions](../spec/core/03-model-boundaries-zones.md#33-canonical-mutation-and-read-forms), not alternative decision semantics.

`Accepted` from `decide` is a candidate until the owner publishes it. A rejected candidate leaves committed state unchanged. Returning normally after publication is ordinary completion of this local call; there is no addressed semantic `Reply`. The contract-specific `QuantityNotAccepted` return expresses [pre-acceptance rejection](../spec/core/06-protocol-algebra.md#613-error-classes); no common carrier wrapper is required for this immediate call. The getter reads only the current committed state in that same scope and returns the closed `UInt8` payload directly.

There is no Resource adapter because the feature has no external action. Its Resource role is empty. The [three logical roles](../spec/core/03-model-boundaries-zones.md#5-three-logical-zones) do not require three classes or folders.

## Check the behavior

Use the actual implementation's test framework to verify these assertions. Use one fresh `draft` owner initialized to quantity 1 for the entire sequence below:

```text
original = State(quantity = 1)
assert decide(original, QuantitySelected(20))
       == Accepted(SnapshotDecision(State(quantity = 20)))
assert original == State(quantity = 1)   // decide did not mutate its input

draft.selectQuantity(20)
assert draft.quantity() == 20

assert draft.selectQuantity(21)
       == QuantityNotAccepted(QuantityOutsideRange)
assert draft.quantity() == 20          // rejection published nothing

assert draft.selectQuantity(0)
       == QuantityNotAccepted(QuantityOutsideRange)
assert draft.quantity() == 20

draft.selectQuantity(3)
assert draft.quantity() == 3
```

Also inspect the implementation: only the owner writes `committed`; no mutable state reference escapes; both entry points obey confinement; every branch terminates without a loop, recursion, allocation-dependent batch, or I/O. The fixed-size input/state and finite control flow give a static bound for this example. Its small file size does not prove those properties. Use [transition and property verification](../spec/core/verification/17-01-transition-and-property-tests.md#17-testing-review-and-operational-verification) for the corresponding checks.

## Make an ordinary business change

Suppose the maximum quantity increases to 30. Change the upper comparison in `decide` and add assertions that 30 succeeds and 31 rejects without changing the last accepted quantity. Existing valid drafts remain valid. The `UInt8` protocol still represents both values, and the getter, owner, and input adaptation keep their meaning.

This is the intended local reasoning benefit: the policy and the tests that explain it are together. A policy change does not automatically require a new interface, manifest field, route, Resource adapter, or Flow.

For an everyday change, check:

- Is the changed fact still owned and written by this Ball alone?
- Do the existing closed inputs express the new request?
- Can the rule be decided from State and explicit current input/context?
- Does acceptance still publish the complete decision atomically?
- Did this change introduce an external action, asynchronous lifetime, another authority, variable dimension, trust boundary, or stronger guarantee?

The final question determines the next reading route. It is not a request to fill out every possible category.

## What this example deliberately does not materialize

These omissions follow from its stated behavior:

| Artifact | Why it is absent here |
|---|---|
| Instance ID and revision fields | One local scope proves identity and order; no persistence, asynchronous ordering, or exposed snapshot identity. [§§3.1–3.2](../spec/core/03-model-boundaries-zones.md#31-ball-type-and-instance) |
| Output frame, queue, Resource adapter | No semantic outputs or I/O; state-only acceptance is one atomic publication. [§8.5](../spec/core/08-decision-and-acceptance.md#85-atomic-decision-acceptance) |
| Stamped read or lifecycle enum | Same-stack getter and host-owned lifetime; no cache, status, or independent lifecycle. [§§8.10–8.11](../spec/core/08-decision-and-acceptance.md#810-local-read) |
| Assembly routes and Flow | There is no other Ball authority to call or coordinate. [§10](../spec/core/10-system-composition.md#10-system-composition) |
| Manifest, interface hierarchy, DI container | Source and host already expose the contract; these artifacts add no needed boundary. [§14.1](../spec/core/14-manifest-and-organization.md#141-role-of-the-manifest), [§14.6](../spec/core/14-manifest-and-organization.md#146-interfaces-di-and-code-generation) |

Ordinary implementation creates no absence dossier merely because a category is unused. A conformance or release claim that relies on absence activates the separate [applicability and evidence rules](../spec/core/00-status-scope-goals.md#02-proportionality-principle).

## Share immutable parts of one State

One Document Ball can own immutable `DocumentState(metadata, paragraphs)`. Changing a title creates new metadata and shares unchanged immutable paragraphs. It does not require a Ball per paragraph or a physical deep copy:

```text
next = DocumentState(Metadata(title = newTitle), current.paragraphs)
assert next.paragraphs is current.paragraphs
assert current.metadata.title == previousTitle

builder = mutableList(current.paragraphs)  // Private to this candidate.
builder.append(newText)
if exceedsDocumentLimit(builder): return NotAccepted(DocumentSize)
next = DocumentState(current.metadata, immutableTuple(builder))
```

Only immutable values reach readers. The builder never escapes to accepted State; rejecting after its mutation leaves every published document unchanged. In your implementation, test title sharing, candidate isolation and rejection after the permitted document-size boundary. The storage/copy strategy remains an implementation choice.

For a next **local** dependency, follow the [Counter composition](COMPOSITION.md#a-counter-with-two-capabilities). It shows one serialized binding shared across owners, read/increment capabilities, and acceptance, refusal and post-acceptance failure with ordinary calls. Adding a consumer does not introduce command/result tokens, a caller enum or another dispatcher.

## Next change: asynchronous search

An external search adds a real problem: callbacks can arrive out of order. Keep one Ball owning the current search, and make the accepted request identity part of its decision state.

1. Accept search A's State and complete `EffectRequest(FindProducts(...))` batch together. Dispatch afterward.
2. Accept search B with a new stable search handle and generation, again together with its request.
3. Verify B's completion against its accepted request. A matching `Fact` reaches `decide`, which accepts B's results.
4. Verify late completion A against A's request. It is authentic but stale: `decide` compares the complete handle and generation with the current search and leaves B's results intact.

```text
accepted A → accepted B → result B → result A
current view:                 B          B
```

The host or binding must now supply real mechanisms for atomic state/output acceptance, retained post-acceptance dispatch, serialized completions, stable identity and provenance verification, and finite request/response/work/in-flight bounds. It must construct each completion's own trusted Context rather than inherit the initiating call's Context. The selected adapter's reachable failures and ambiguous outcomes need closed result paths. Choose explicit retry, deadline, or cancellation behavior only where those paths exist; changing those choices can activate further obligations.

These searches outlive their initiating calls, so [operation status](../spec/core/09-asynchrony-and-delivery.md#911-operation-status) is already required. The binding supplies a query over one committed, revisioned status authority, independent of a live reply channel. It can co-locate that authority with the search owner; no additional service is required. Before accepting a source operation or output, reserve all newly reachable bounded status capacity, including pending observations, facets, and retention markers when applicable. If capacity is unavailable, reject before acceptance and dispatch. Declare the retention horizon and process-loss scope: a `Transient` binding retains status only within its stated process lifetime; survival after process loss requires a durable mechanism and evidence. A's stale result must not replace B's current view, but staleness is not permission to discard status evidence required for accepted operation A.

The trace explains the state rule, not a complete async implementation. Follow [Catalog's accepted request](../spec/core/examples/15-catalog-search.md#154-first-decision), [Resource adapter](../spec/core/examples/15-catalog-search.md#155-resource-adapter-and-safe-sink), and [stale-result trace](../spec/core/examples/15-catalog-search.md#157-stale-result). Its full example also includes cancellation and signal behavior; those are additional scenarios. Use the [causality source](../spec/core/09-asynchrony-and-delivery.md#91-causal-identity) for identity requirements.

## Take a slice toward production

Use the [evaluation guide](EVALUATION.md) to compare the same feature's implementation cost, failure behavior, and change effort against the project's current approach. Supply the concrete binding evidence for the guarantees the project needs. If accepted work must survive process loss, follow [durability adoption](../spec/core/reference/21-adoption.md#214-when-to-add-durability); a transient teaching example cannot establish that guarantee.

The next feature should reuse established binding mechanisms and exact applicable project policies. Its author writes the new domain decisions and affected tests, then revisits only new triggers, local differences, and evidence invalidated by the change.
