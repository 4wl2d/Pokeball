# Adopting Pokeball

> [!NOTE]
> This is a non-normative reading guide. The ordered Core document set rooted at [`spec/pokeball-architecture-core.md`](../spec/pokeball-architecture-core.md) is the sole normative authority; if this guide differs from a marked Core source clause, Core controls.

[← Pokeball overview](../README.md) · [Architecture](ARCHITECTURE.md) · [Composition](COMPOSITION.md) · **Adoption** · [Agent Pack](agents/README.md)

Use this guide to select only the profiles and guarantees a project needs, choose a readable project shape, and pilot one bounded vertical slice.

For a first implementation, use the [human quickstart](QUICKSTART.md). If the project has no binding yet, implement that quickstart owner (or the shared serial accept/dispatch pattern in [Composition](COMPOSITION.md)) once; then Core's everyday workflow in §21.7 applies. For the adoption decision, use the [equal-scope comparison](EVALUATION.md) to decide whether the extra contracts pay for themselves. The tables below are reference choices for actual paths, not a checklist to populate before writing a feature. After the first `decide` compiles, inventory paths/profiles (steps 5–6) only for triggers that are present.

## Adopt Pokeball

Pokeball is not automatically the right choice. Use a Ball-local utility, an adapter, shared mechanical Foundation, or stop the pilot when the candidate is only stateless mechanics, presentation-only focus/scroll/animation state, a passive adapter with no owned semantic Decision, a project unwilling to establish one writer/authority/closed bounded paths, or a slice whose measured design/runtime cost exceeds its authority, invariant, recovery, or change-radius benefit. This simplification does not permit ownerless shared domain/business semantics; keep them local or give them one Ball/Flow owner and a declared Application Surface/protocol.

During active adoption or pilot work, the project architecture owner `SHOULD` record or otherwise prove the boundary's semantic facts, state key, strict invariants, dependencies, trust/lifecycle/recovery owners, expected size/load, and public protocol surface. This is project-owned guidance only—not a universal Core artifact, runtime record, or conformance placeholder. Outside adoption/pilot work, no worksheet or empty fields are required.

For a suitable candidate, start with one vertical slice rather than redesigning the whole application:

1. Assign one authority to each mutable semantic fact.
2. Choose the Ball boundary; materialize `StateKey` only when there can be multiple instances or identity crosses call scope.
3. Define only the used closed inputs, state, and outputs.
4. Implement pure bounded `decide` before introducing a framework or DSL.
5. Inventory reachable paths, risks, selected profiles, and intended claims.
6. Resolve each applicable guardrail once through construction, a local declaration, or an exact project/binding policy plus permitted delta.
7. Add adapters, revisions, handles, stale-result, retry, cancellation, status, security, and durability only when first triggered.
8. Test base semantics, triggered paths, and local deltas; test shared mechanisms once at their scope.
9. Add dependencies and Assembly routes only for actual inter-Ball edges.
10. Create evidence only for claims actually made; generate a fully resolved contract view only when deployment or conformance review needs it.

For agent-assisted adoption in another repository, start with the [Agent Pack](agents/README.md). It can guide ordinary design without a fully populated overlay. Its [installation guide](agents/INSTALL.md) explains how to select exact reusable project policies and Ball deltas; an accepted resolved project contract and claim evidence are required before making a Pokeball conformance claim.


## Updating an earlier 1.5.0-draft adoption

The draft version label alone does not identify exact bytes. Compare your adopted snapshot with the complete replacement Core and Agent Pack using [BASELINE](agents/BASELINE.md); keep the original snapshot identity with your project contract. The following clarifications may require re-resolving an existing binding or evidence claim. Apply only rows whose paths or claims exist in your project, and reuse evidence only within its recorded scope.

| Change | What to check in your implementation | Required confirmation |
|---|---|---|
| [Callee programming fault after source acceptance, §6.9](../spec/core/06-protocol-algebra.md#69-moduleresult) | Retain the accepted source command and any accepted target facts. The caller sees declared operational/quarantine evidence, or `OutcomeUnknown` only when execution may have occurred. | A fault-injection trace after source acceptance preserves the frames and returns neither `NotAccepted` nor a fabricated business result. |
| [Direct control and instance grain, §10.2](../spec/core/10-system-composition.md#102-permitted-inter-module-dependencies) | Graph nodes are `BallInstance` authorities. An awaited call stays a direct-control edge until return or an explicit Assembly handoff; an independent import edge remains. | Classify awaited, enqueued, and same-type instance calls using the actual wiring; check both required graphs for cycles. |
| [Compensation-risk acceptance, §10.5](../spec/core/10-system-composition.md#105-compensation) | If conflicting compensation can start under `OutcomeUnknown`, identify its Flow/project-policy owner and immutable record using the eight-field `WaiverRecord` discipline. | The exact scope, approval, controls, evidence, review and conformance effect are inspectable. Risk acceptance never cancels a Core requirement. |
| [Migration and rollout, §10.11](../spec/core/10-system-composition.md#1011-versioning-and-compatibility) and [§21.6](../spec/core/reference/21-adoption.md#216-documentation-evolution) | Re-resolve producer/consumer pairs, retained-output meaning and the coexistence interval wherever independent versions or persistence exist. | The handoff names its owner, migration/rollout artifact, retained guarantees and unsupported stronger claims; compatibility tests cover the declared interval. |
| [Merge evidence, §7.7](../spec/core/07-state-and-authority.md#77-commutative-updates-under-single-writer-acceptance) and [finite checks, §17](../spec/core/verification/17-01-transition-and-property-tests.md#17-testing-review-and-operational-verification) | Name an algebraic argument, fixture suite, or both for each merge property; bound growing metadata and retain one writer. Review the scope of verification and scheduling claims. | Evidence records assumptions, bounds and cases. Finite/sample checks do not establish an unbounded theorem; starvation or priority claims follow the performance claim contract. |
| [Examples, §17.1](../spec/core/verification/17-01-transition-and-property-tests.md#171-transition-tests), [first slice, §21.1](../spec/core/reference/21-adoption.md#211-start-with-one-vertical-slice) and [quickstart](QUICKSTART.md) | Keep unwalked Checkout deadline transitions and the abbreviated Catalog restart outside mandatory example coverage. A first local slice may have no effects; implement or reuse its serial binding once. Catalog SQL parameterization depends on the driver/API binding. | The first slice demonstrates atomic publication and applicable accept-before-dispatch behavior; example tests use explicit Core oracles and label omissions; adapter tests substantiate any parameterization claim. |

Changing an operation's refusal from a pre-acceptance return to an accepted result changes protocol meaning: follow §10.11 rather than relabeling retained evidence. These clarifications introduce no new Core profile or public protocol. Matching documentation digests and passing research fixtures do not establish production readiness for a consuming system.

## Profiles: pay only for the guarantees you use

Profiles are independent dimensions. Every selection preserves the always-applicable Core semantics and cannot waive a guardrail activated by its actual paths, risks, or claims.

A project or binding may select an exact default profile policy once; a Ball records only a permitted override.

| Dimension | Choices | What changes |
|---|---|---|
| Execution | `Inline` / `BoundedConcurrent` | Direct run-to-completion versus bounded mailbox and workers, while retaining a single writer |
| State | `Transient` / `SnapshotOutbox` / `EventJournal` | Process-memory lifetime versus durable source state/output records or event commits |
| Isolation | `InProcess` / `Isolated` | Logical in-process boundary versus process or sandbox containment with bounded IPC |
| Security | `Standard` / `Hardened` | Base discipline versus stronger actor, grant, capability, secret, and abuse-control requirements |
| Composition | `Static` | Explicit compile-time or generated wiring; no mandatory runtime registry |

Typical starting points from the Core:

- Local UI state machine: `Inline + Transient + InProcess + Standard`.
- Asynchronous mobile feature: `BoundedConcurrent + Transient/SnapshotOutbox + InProcess`.
- Durable backend aggregate: `BoundedConcurrent + SnapshotOutbox + InProcess + Hardened`.
- Hostile plugin or parser: `BoundedConcurrent + Transient/SnapshotOutbox + Isolated + Hardened`.


## Guarantee boundaries

Pokeball makes guarantees explicit, but it does not turn a label into proof:

- A timeout does not prove that an external action failed or never happened; use `OutcomeUnknown` and reconciliation where required.
- Cancellation is a protocol and a race, not an instant physical stop.
- For a conforming, evidenced `SnapshotOutbox` binding, the guarantee ends at durable source acceptance and retained source output/status within its stated contract. It does not by itself prove target receipt, target acceptance, business success, exactly-once execution, or unconditional eventual delivery.
- A `Flow Ball` coordinates participants; it does not create a distributed ACID transaction. Compensation is new fallible work, not time reversal.
- A multi-source read is not an atomic snapshot without a mechanism that provides one.
- `Hardened`, performance, zero-allocation, durability, and security claims require evidence from a concrete binding and threat/failure model.

Stronger durable runtime, distributed delivery, full replay, secure isolation, subscription, conformance, and dynamic-extension protocols are deliberately left to future extension specifications.


## A typical project layout

Start with ordinary source files. A local feature can keep its owned types, pure decision/read, and serial binding in one file, with behavior tests alongside it. Add Resource adapters or split packages when real dependencies and readability require them; see the [complete small example](QUICKSTART.md).

Physical folders are not normative. The larger layout below is useful when those responsibilities exist; it is not a mandatory starter scaffold:

```text
features/
  catalog/
    interaction/
    nucleus/
      protocol/
      state/
      transition/
      policy/
    resources/
    ball.yaml            # optional/generated resolved view

flows/
  checkout/
    interaction/
    nucleus/
      protocol/
      state/
      transition/
    ball.yaml            # optional/generated resolved view

application/
  assembly/
  runtime/
  observability/

foundation/
  bounded/
  security/
  time/
  tracing/
```

`ball.yaml` is optional: typed source may be authoritative, while tools may generate a fully resolved view for deployment, review, or a conformance claim.

The important part is the direction of authority and dependencies: Interaction adapts external channels, Resources adapt external systems, and the Nucleus owns its State and protocols while importing only its Ball-local Nucleus utilities, the exact declared target- or producer-owned Application Surfaces required by its closed Query, Pulse, and Decision contracts, and mechanical foundation. Local/mechanical helper imports remain ordinary compile-time edges; they create no inter-authority route or `Direct Control Dependency` absent an actual cross-authority condition. An Application Surface import transfers no ownership. Foreign State, internals, private Resource adapters, ownerless shared domain utilities, caller-owned mirrors or redeclarations, protocol re-export, and semantic synthesis by Interaction or Assembly remain prohibited; Assembly selects only public routes, versions, and bindings.




---

[← Composition](COMPOSITION.md) · [Pokeball overview](../README.md) · [Agent Pack →](agents/README.md)
