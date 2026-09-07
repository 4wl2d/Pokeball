# Core part — State and authority

[Core contents](../pokeball-architecture-core.md) · [← Protocol algebra](06-protocol-algebra.md) · [Decision and acceptance →](08-decision-and-acceptance.md)

> Canonical part 4 of 24. The [Core entrypoint](../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

## 7. State and authority

### 7.1. State kinds

<!-- pkb:pba-source:start id="PBA-14" title="Explicit State Kind" -->
**Source clause for PBA-14 — Explicit State Kind.**

- **Rule:**
  - The state kinds in this subsection are distinct authority and meaning categories and are not interchangeable.
  - `EphemeralState` contains only UI/transport mechanics that cannot change a business Decision.
  - A decision-relevant UI/transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext` input, never hidden ephemeral authority.

  | Kind | Owner | Meaning |
  |---|---|---|
  | `SovereignState` | Nucleus of a specific Ball | Canonical mutable semantic facts |
  | `EphemeralState` | Interaction/transport | UI and transport mechanics that cannot change a business Decision |
  | `ReplicaState` | Resource/read adapter | Cache or copy of an external source with provenance |
  | `CapturedInput` | Flow/operation owner | Immutable versioned snapshot for decision and recovery |
  | `ReadModelState` | Read Model Ball | Derived query-oriented state |
  | `Projection` | No one | Immutable output, not an authority |
  | `RuntimeState` | Runtime | Mailbox, claims, attempts, breaker, tracing |
- **Applicability:** `A`: every state-like value, including UI/transport values that may affect a Decision.
- **Declaration owner:** State/protocol owner; Interaction owns only non-decision mechanics.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Type/state mapping plus decision-relevant UI current-input/retention tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Reuse vocabulary; non-decision mechanics may be ephemeral, absent state kinds need no row.
- **Primary verification route:** `§17.1`
<!-- pkb:pba-source:end -->
### 7.2. One mutable fact—one authority

<!-- pkb:pba-source:start id="PBA-11" title="Single Semantic Authority" -->
**Source clause for PBA-11 — Single Semantic Authority.**

- **Rule:**
  > **Within one overlapping semantic scope, there cannot be two independent writers that each consider their own value canonical.**

  The scope may include:

  ```text
  semanticId
  namespace / tenant / realm
  stateKey or key range
  region or jurisdiction
  validity interval
  ```

  An external provider or database may be the source of record. In that case, the local `Ball` owns decision state, operation state, or a replica, but does not pretend to own the external canonical fact.
- **Applicability:** `A`: every mutable semantic fact.
- **Declaration owner:** Project ownership map and owning Ball.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Access boundaries/ownership tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** One State Belt may be referenced; no copied ownership prose.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->
### 7.3. State Belt

<!-- pkb:term:start name="State Belt" -->
**State Belt** — a logical map of isolated state authorities; not a global mutable-store API.
<!-- pkb:term:end -->

```text
StateBelt {
    AuthBall owns auth.session
    CatalogBall owns catalog.searchSession
    CartBall owns cart.contents
    CheckoutFlow owns checkout.operation
}
```

The State Belt IS NOT a mandatory global store, singleton API, or object available to every module. It is an architectural ownership model and, when needed, a generated registry.

### 7.4. Prohibition on direct cross-cell reads

<!-- pkb:pba-source:start id="PBA-13" title="State Isolation" -->
**Source clause for PBA-13 — State Isolation.**

- **Rule:**
  One Ball's `Nucleus` MUST NOT read another Ball's mutable state through a memory reference, global-store selector, or shared ORM session.

  Permitted ways to obtain another Ball's data:

  - public read contract;
  - versioned immutable snapshot;
  - target-owned result through an immediate trusted typed return or a verified `ModuleResultPulse` under §6.9;
  - observed signal/event;
  - Read Model;
  - target-side validation/reservation.

  The view layer may combine multiple projections for presentation, provided that the combination makes no cross-authority business decision and is not claimed to be a consistent snapshot without a separate mechanism.
- **Applicability:** `A`: every Ball graph.
- **Declaration owner:** Ball and Assembly.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Imports/storage boundaries.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Shared graph check; no `noCrossStateReads` flag.
- **Primary verification route:** `§17.5`
<!-- pkb:pba-source:end -->
Observable isolation does not prescribe physical copying. For example, one Document Ball may own immutable `DocumentState(metadata, paragraphs)`. Renaming its title creates new metadata and shares unchanged immutable paragraphs; those parts need no separate Balls. A mutable text builder belongs only to the current candidate and never escapes to readers or mutates a previously accepted document. Reads see accepted values; rejecting or failing the candidate leaves the previously published document unchanged. The implementation chooses storage, structural sharing, and copying techniques under the existing acceptance rules.

### 7.5. Captured input

A Flow or long-running operation may retain only the required foreign or point-in-time fields. The following is an applicability catalog, not a mandatory wrapper shape:

```text
CapturedInput {
    sourceCorrelation?
    sourceProtocolOrSchemaVersion?
    verifiedProvenance?
    capturedAt?
    purpose?
    fields
    expiresAtOrDeletionTrigger?
}
```

The same rule applies to a value from an earlier `Pulse` or `DecisionContext` that a later Decision needs: it becomes a bounded typed value in committed State or is explicitly reintroduced by a new trusted input. Correlation is added when several sources/operations can be confused; an explicit version when an independent protocol/schema can change or the value survives rollout/recovery; portable provenance when its source cannot be established by the trusted local boundary and call scope; and a retention/deletion field when its lifetime differs from the containing state. A simple locally authoritative value in one build may be stored directly without that metadata. Inbox, outbox, runtime, or participant history is never a decision-readable substitute. Captured input does not become a second mutable authority. Before a strict action, the target may require an expected version, reservation, or current revalidation.

### 7.6. Single writer

<!-- pkb:pba-source:start id="PBA-12" title="Single Writer" -->
**Source clause for PBA-12 — Single Writer.**

- **Rule:**
  At any moment, one `BallInstance` has one logical writer. The Inline profile provides this through call discipline; a concurrent profile through a single-writer loop; and a movable durable profile through a storage-enforced ownership epoch or fence.

  A lease or process-local mutex is insufficient if two runtimes can independently consider themselves the owner and write to the same authoritative store.
- **Applicability:** `A`: sequential acceptance; `R`: owner can move.
- **Declaration owner:** Execution/state binding.
- **Scope:** The exact scope stated by the Rule and Applicability fields.
- **Enforcement / evidence owner:** Call loop or storage fence and concurrency tests.
- **Resolution, failure, and conformance:** Resolve under §0.2; a violation is non-conforming in the stated scope unless the Rule states a stricter local failure.
- **Reuse and absent-trigger behavior:** Binding contract reusable in exact store scope; omit fencing for fixed owner.
- **Primary verification route:** `§17.6`
<!-- pkb:pba-source:end -->
### 7.7. Commutative updates under single-writer acceptance

A Ball may declare a commutative or CRDT-like merge contract for concurrent ingress or independently prepared update values with proven properties:

```text
associative
commutative
idempotent or duplicate-aware
deterministic conflict handling
bounded metadata
```

Such a contract changes conflict and merge semantics but does not remove single-writer acceptance: one logical writer serializes the merge and assigns the `CommitRevision`. Two independent writers concurrently accepting a mutation of the same `BallInstance` are outside Core even when the algebraic properties hold. A genuine multi-writer profile requires a separate specification; without one, §7.6 and PBA-12 always apply.

### Definition source records for §7

These marked definitions are the sole glossary inputs for the terms owned in this section.

<!-- pkb:term:start name="Captured Input" -->
**Captured Input** — an immutable bounded field-minimized value from a prior ingress/result/context/authority snapshot, retained for a later decision/recovery. It adds source correlation, version, provenance, and deletion metadata only when the actual source, rollout, trust, ambiguity, or lifetime trigger requires them; it does not become a second mutable authority.
<!-- pkb:term:end -->


<!-- pkb:term:start name="EphemeralState" -->
**EphemeralState** — Interaction/transport mechanics such as focus, scroll, animation, parser, or socket state that cannot change a business Decision. A decision-relevant UI/transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext`, not ephemeral authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Field-Minimized" -->
**Field-Minimized** — containing exactly the semantic, correlation, version, provenance, validity, and lifetime fields activated by the value's actual decision and boundary triggers; fields with absent triggers are omitted rather than filled with defaults or placeholders.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Replica State" -->
**Replica State** — an external/cache copy with provenance, revision, and freshness; it is not decision authority.
<!-- pkb:term:end -->

<!-- pkb:term:start name="Sovereign State" -->
**Sovereign State** — canonical mutable state with one decision authority.
<!-- pkb:term:end -->


<!-- pkb:term:start name="StateKey" -->
**StateKey** — the identity of a BallInstance's local state/consistency/partition boundary.
<!-- pkb:term:end -->


---
