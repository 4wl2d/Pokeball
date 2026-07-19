# Test and Review Gates

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Testing follows applicability:

```text
base invariant tests
+ reachable path/risk tests
+ local policy-delta tests
+ evidence suites for claims actually made
```

Shared mechanisms and their evidence are tested once at exact revision/digest/scope. A Ball tests semantic wiring and its delta.

## 1. Routine change

Routine implementation does not create a full gate dossier. Before editing, resolve:

```text
exact baseline and affected authority
authoritative source and exact policy reference, if used
semantic delta
new, removed, and unchanged triggers
explicit exclusions
```

After editing, run base tests and the tests selected by changed triggers. Report the diff, policy delta, commands/results, and any unresolved triggered decision. Do not record absent paths individually, create `TriggerAbsenceProof` placeholders, or make a conformance or stronger guarantee claim. The proof is materialized only when a conformance/release claim or accepted ambiguity-resolution decision relies on absence.

## 2. Trigger-to-test matrix

| Trigger | Minimum evidence at the owning scope |
|---|---|
| every Ball | ownership/writer, zone imports, pure deterministic Decision, atomic acceptance, present-bound overflow |
| output | complete batch, commit-before-dispatch, fault before/after acceptance |
| synchronous causal completion | pre-reserve total budget; at N+1 reject before acceptance or retain the accepted completion in one bounded `RetainedContinuation`; no drop/truncate/budget reset |
| detached/late result | stable source/provenance; duplicate, stale, and reordering cases only when reachable |
| retry | stable logical key, primary owner, finite/cumulative attempts; idempotency horizon only for duplicate-permitting irreversible execution |
| possible execution without proof | possible-send boundary, typed unknown, reconciliation/manual terminal policy |
| cancellation | reachable race orders and terminal conflict |
| timer | stable identity/late policy; generation only for replacement and dedup only for duplicate/redelivery risk |
| Query / `ReadDependency` | exact caller/target authority, target-owned `Query -> ResultPayload` effective identity and read/status authority, caller freshness/consistency requirement, and Assembly route; sparse version/stamp/actor/cache/order/buffer/retry/status fields; boundary failure before `read`; successful purity and no Decision/accepted marker/revision/handle/output; no payload/stamp/status re-export or multi-source atomicity claim; no new Query-only mandatory limit field |
| status | one committed revisioned single-writer authority; causal-order apply or bounded pending; idempotent duplicate and monotonic conflict behavior; lossless independent facets; pre-acceptance reservation/no eviction; covered-source/empty-pending marker before absence and no resurrection; target accepted-result crash/stop with source remaining Pending/Unknown |
| inter-Ball command | exact target-owned versioned mapping and static refusal class; accepted-source `ModuleCommandPulse`, target `decide`, accepted-target `ModuleResultOutput`, verified-source `ModuleResultPulse`; exact source/target fields and result-delivery tuple; no Assembly/generated synthesis; three carrier responses; accepted-result/carrier/version conflict; command-vs-read; same-stack/async causal reservations |
| other inter-Ball edge | target/producer-owned contract, sole Assembly edge, effective identity, finite graph/fan-out bounds; independently acyclic compile-time-import and `Direct Control Dependency` graphs, including generated inline dispatch before handoff/yield |
| Flow/dispatcher | closed routes; coordinator only for stateful multi-authority workflow; compensation only when present |
| concurrent | mailbox/backpressure and finite workers; fairness only for starvation risk and out-of-order tests only when completion can reorder |
| durable | atomic state/event transaction and recovery; output/value/ambiguity/fencing/status cases only when those paths exist |
| raw trust/representation edge | bounded parsing/validation/provenance quarantine for the exact edge |
| actor context changes a Decision or Query/status-read authorization/result selection | trusted-boundary construction under Ball/Nucleus-owned schema; approved-issuer or fixed trusted same-stack issuer/realm proof; forged, tampered, stale, wrong-issuer/realm, and missing evidence fail closed; actor-independent Decision/read creates no actor artifact |
| logical-role or shared-foundation path | role map, permitted call graph, accepted-write sites, and cause/context/result provenance across Interaction, Nucleus, Resource/route, Assembly, runtime, status/read authority, and foundation; reject runtime state writes, Assembly-created meaning, mutable foundation business state/policy/route selection, globals, and service locators |
| external/privileged consequence | minimum restricted capability; Policy Gate, current fail-closed Execution Gate, and Grant only under their separate triggers |
| interpreter/secret/unsafe path | exact safe-sink, containment, or eight-field `WaiverRecord` tests selected by each subtrigger |
| policy reference/delta | when referenced: digest/scope resolution, cycle/conflict/mismatch, override allowlist, effective bound, and proof that policy cannot suppress a trigger, weaken a law, or authorize a direct-control cycle |
| waiver | exact eight-field shape, pinned tests/evidence, review/remediation, literal conformance effect, and proof that the waiver is neither policy/precedent nor guardrail satisfaction |
| absence-dependent conformance/release claim or accepted ambiguity decision | exact `TriggerAbsenceProof`; present/absent path/risk/claim predicates, `always` misuse, present-claim contradiction, accepted/unaccepted ambiguity, wrong scope/profile/revision/digest, invalidation and reevaluation |
| claim | exact mechanism/assumptions/non-guarantees plus benchmark, fault, security, or conformance evidence |

Mocks of framework objects inside the Nucleus do not replace transition tests. A declaration does not replace enforcement. Reused evidence is valid only for the exact artifact digest and scope named by the policy or claim.

## 3. Conformance or release review

Only an explicit conformance/release claim runs the full review workflow. Freeze Core, package, source, environment, evidence, and any project policy or Assembly in the claimed scope. Resolve the whole claimed scope. When the claim relies on absence of a non-`always` trigger, record the exact Core `TriggerAbsenceProof` once rather than creating empty guardrail artifacts. Verify its trigger class/anchor, exact scope/effective profile, one closed inventory-evidence choice, inventory revisions/digests, evaluated predicate, `Absent` conclusion, evidence owner, and invalidation conditions. A present trigger or claim, `always`, unaccepted ambiguity, wrong/stale/unresolved/conflicting evidence, or an invalidation condition blocks reliance until reevaluation.

Every release gate is `pass`, `fail`, or `partial`; `partial` is not a claim pass.

| Gate ID | Name | Question |
|---|---|---|
| `RG-01` | Scope | Are claimed scope, authority, policy references, goals/non-goals, extensions, every exact WaiverRecord plus its conformance effect, and every absence proof actually relied upon explicit? |
| `RG-02` | Protocol | Are every used protocol/read/output category, command ingress/result egress, refusal carrier, read dependency, and boundary identity closed without copied, hidden, open-string, or compatibility-alias variants? |
| `RG-03` | Authority | Does each mutable fact/workflow have one authority/writer, with retained references honestly classified? |
| `RG-04` | Decision | Are explicit inputs, trusted context construction/semantic ownership, purity, present bounds, atomic acceptance, output ordering, same-state accepted result, and exact failure-stage behavior proven? |
| `RG-05` | Async | Are all reachable command/result provenance, identity, carrier/result/refusal meaning, retry, unknown, cancellation, delivery, and lossless status/materializer races total? |
| `RG-06` | Composition | Are all existing read/command/signal/Flow edges, target ownership, effective protocol identities, triggered versions, Flow ownership, graph ceilings, independently acyclic import/direct-control graphs, and post-handoff asynchronous-feedback owner/identity/budget/escape/fan-out conditions resolved without Assembly business authority? |
| `RG-07` | Security | Are every reachable trust, actor-origin for Decision and read semantics, Policy/Execution Gate, external authority, privilege, interpreter, secret, foundation, and unsafe path closed under its own trigger? |
| `RG-08` | Profiles | Does each effective profile and claimed guarantee match mechanism, recovery/failure assumptions, and non-guarantees? |
| `RG-09` | Limits | Does every present variable dimension resolve to one finite enforced bound without unauthorized delta? |
| `RG-10` | Integrity | Do all 44 marked source tuples, law/matrix projections, policy refs, examples, tests, indexes, traceability, package integrity, bilingual public projections, and claim/absence evidence agree without orphan modal wording? |

## 4. Profile suites

Run only suites selected by the effective profile or claim.

### Inline / Transient

- acceptance publication faults and complete in-process retained frame when outputs exist;
- exact N/N+1 causal-budget boundary when synchronous completion can cause another Decision, including `RetainedContinuation` fate;
- same-stack command levels `0/1/2`, one source-to-target direct edge, two completion reservations, result-return non-edge, and `CausalBudgetExceeded` through the carrier;
- no hidden queue/thread/serialization tax;
- explicit process-loss boundary when external ambiguity can exist; and
- benchmark only when making a performance claim.

### BoundedConcurrent

- mailbox full/backpressure and finite workers; fairness only when separately queued inputs could starve;
- out-of-order input/result, plus duplicates only when duplicate delivery is possible;
- cancellation/deadline/result races only when those paths are reachable; and
- finite causal/fan-out budgets only for present causal or routed dimensions, with async handoff preserving causal scope/depth/budget.

### SnapshotOutbox / EventJournal

- before/inside/after-transaction crashes; unknown commit only when ambiguity is possible;
- recovery from accepted records without rerunning current transition code;
- redelivery, ACK loss, attempt exhaustion, status, retention, and fencing only when reachable; and
- accepted target result survival across crash-before-dispatch, target tuple-keyed `DispatchStopped`, and source Pending/Unknown until verified result/reconciliation; and
- exact retained cross-Decision values and migrations only when later work uses them.

### Hardened / Isolated

- actor-context authenticity/integrity and wrong/unapproved issuer/realm evidence whenever actor context can change a Decision or Query/status-read authorization/result selection, independently of privilege;
- audience/payload/expiry/revocation and current action proof only for the corresponding privileged controls;
- restricted credential/capability for external authority and safe-sink injection only for an interpreter edge;
- secret/redaction behavior only for a secret path; and
- isolated crash/escape/resource caps when containment is selected or claimed.

## 5. Agent Pack gates

These gates validate the package itself. They do not certify a consuming project.

| Gate ID | Name | Pass condition |
|---|---|---|
| `AP-GATE-01` | Package closure | Every declared file exists and every relative link resolves. |
| `AP-GATE-02` | Snapshot integrity metadata | The candidate Core version/status/hash/bytes and package file count/digest match `BASELINE.md`; the Core hash occurs only there, and no internal review verdict is exported as package authority. Consumer readiness additionally requires that these exact bytes come from one immutable published snapshot. |
| `AP-GATE-03` | Noncanonical precedence | Every file is marked derived; no guidance overrides Core or exact policy precedence; every repeated projection modal names and remains set-equal to its marked body source clause, and an orphan `MUST`/`MUST NOT` scan is empty. |
| `AP-GATE-04` | Rule integrity | Exactly 37 `PKB-AR-*` definitions are unique and defined only in `AGENT-CONTRACT.md`: the prior 35 IDs in order plus only `PKB-AR-GOV-005` and `PKB-AR-PRT-004`; contract and traceability sets are equal. |
| `AP-GATE-05` | Core coverage | §§0–23 and all 13 Core §8.12 rows are indexed set- and order-equal by concern and anchor; the exact `PBA-01–44` ID sets for marked body sources, §20 laws, and §20.1 matrix are each unique and equal at 44; all 89 Core glossary terms are indexed; ordered union checks also reject bare `ModuleResult`, top-level `TimerFired`, `SignalOutput`, and compatibility aliases. |
| `AP-GATE-06` | Trigger traceability | Every agent rule has a Core source and complete source tuple: modal, class/exact trigger, declaration/semantic owner, scope, enforcement/failure behavior, reuse, absent-trigger behavior, and gate. Anchor equality alone does not pass. Exact §6.13 failure-stage and closed-admission-reason mappings are traced. |
| `AP-GATE-07` | Policy and waiver closure | The compact project-policy template contains exact metadata/source/policy resolution, permits only local deltas, omits Ball inventories/empty optional sections, and is statically resolvable; any waiver has exactly the eight Core fields and neither weakens nor suppresses a guardrail. |
| `AP-GATE-08` | Applicability and fixture closure | The complete H fixture catalog below passes: trigger absence, actor/context/Dual Gate/foundation, read dependency, command/result/refusal, causal/status materializer, Checkout and Catalog, failure stage, admission reason, `Draining`, and negative-adoption cases. Inferred triggers cannot be suppressed and closed absent paths add no artifacts. |
| `AP-GATE-09` | Claim boundary | Routine work requires no evidence dossier or absence-proof placeholder; every explicit claim contains exact scope, mechanism, assumptions, evidence, non-guarantees, applicable waiver effects, and every relied-upon exact `TriggerAbsenceProof`. Present/`always` contradictions or any `MUST`/`MUST NOT` violation block conformance in their exact scope. |
| `AP-GATE-10` | Governance-free portability | A clean standalone target with no source-repository governance files resolves a sparse singleton, triggered local Balls, and one shared declaration for two covered Balls plus an allowed delta; wrong/stale/cyclic/conflicting/version/profile/environment refs fail. The overlay is optional, no Query-specific limit field is invented, and package licensing, links, hashes, and target software license remain intact. |

The complete revision fixture catalog is additive; it does not replace the Core acceptance set:

- **Source authority and closure:** verify one unique primary marker, law projection, and matrix row for each `PBA-01–44`; compare every source/projection modal, trigger, owner, scope, failure, reuse, and absence tuple; reject orphan projection `MUST`/`MUST NOT`. Verify exact ordered `Pulse` and `SemanticOutput` unions and negative membership for bare `ModuleResult`, top-level `TimerFired`, `SignalOutput`, and every compatibility alias. Verify validation/admission/pre-acceptance Decision rejection, accepted target result, post-acceptance Resource failure/timeout/unknown, delivery stop, and programming-fault carriers/status without later-history rewrite. Every fallible concrete profile exposes a finite closed `AdmissionFailure.reason` union, rejects unknown/open strings, and a nonfallible path exposes no empty union.
- **Command/result bridge:** verify every `ModuleCommandPulse`, `ModuleResultOutput`, and `ModuleResultPulse` field, issuer provenance, target-frame `sourceOrdinal`, correlation-only `semanticHandle`, and delivery key `(effectiveProtocolIdentity, commandSource, resultSource)`, with no replacement identity type. Cover routed/same-stack success, all three pre-acceptance `BoundaryResponse` choices, forged/tampered/missing/wrong provenance, accepted same-state Snapshot/EventJournal refusal, duplicate replay without another Decision/revision, carrier/result conflict, dynamic reclassification and wrong version, output/byte and conditional target status bounds, crash before result dispatch, target `DispatchStopped`, late result, handle collision, target `N/N+1`, ACK/result orderings, and exact-one/live-route sparsity. Same-stack covers levels `0/1/2`, exactly two completion reservations, result return as no reverse edge, `CausalBudgetExceeded` through the carrier, and async preservation of causal scope/depth/budget.
- **Checkout command/refusal projection:** cover all nine target mappings. `InventoryReservationRejected` is never a carrier; `DefinitelyNotCaptured(RejectedBeforeAcceptance)` describes the original Capture; definitive Order refusal is an accepted result; Flow rejection copies no carrier reason. Carrier maps to `RejectedBeforeAcceptance + NotExpected`, accepted rejection to `Accepted + Rejected`, and carrier `BusinessRejection` is informational only.
- **Reads, actor context, gates, and foundation:** verify complete `ReadDependency` resolution, triggered-only fields, boundary failure before `read`, successful `ReadResult` codomain, no Decision/accepted marker/revision/handle/output, no target payload/stamp/status synthesis, no atomic multi-source promise, and no new Query-only limit field. Compare `Payment.GetOperationStatus` command semantics with an ordinary status Query. Cover actor-dependent Decision and read paths, actor-independent sparsity, fixed trusted same-stack issuer/realm proof, forged/tampered/missing/stale/wrong issuer/realm/target/field, and the Payment path from accepted command through action-scoped context/grant, Nucleus business result, and immediate Execution Gate. The seven-role map/call graph/provenance proof rejects direct runtime writes, Assembly-created meaning, mutable foundation business communication/policy/route selection, hidden globals, and service locators.
- **Status materialization:** generic fixtures cover observation-before-source, causal apply/bounded pending, duplicate and non-equivalent conflict, every reachable independent facet, pre-acceptance `N/N+1`, no eviction/truncation, covered-source/empty-pending retention marker, marker-before-absence, expiry, invalidation/conflict, late duplicate no-resurrection, and accepted-target-result crash/stop with source Pending/Unknown. Checkout covers the fixed canonical ten-handle order `cartLock`, `initialAcceptanceReply`, `inventoryReservation`, `paymentCapture`, `paymentCancellation`, `paymentReconciliation`, `orderConfirmation`, `inventoryRelease`, `paymentRefund`, `cartUnlock`; reply-only, reply-plus-nine, both/permuted arrivals, duplicate/conflict, reason/attempt/last-observation retention, reservation/no eviction, and capacity `10/11`. Catalog covers all 6/6 state-to-view mappings and `ProductSelected` transitions, the total lifecycle × cancellation × unknown × rejection-reason model and compatible late orderings, one ordinal-0 Signal, exact `2.0.0/2.0.0` route, actor-independent reserved ID, and authoritative v1 upcast or quarantine without invented reason.
- **Boundaries and adoption:** run the §4 decision tree and concrete combine/separate/utility/Feature/Flow/Read Model falsifiers; verify Core constrains valid authority graphs without promising one unique decomposition. Prove that decision-relevant UI/transport values are committed State or explicit trusted current `Pulse`/`DecisionContext`, while only non-decision mechanics are `EphemeralState`. Distinguish material coordination—owned lifecycle, ordering/branch/join, compensation/recovery/cancellation, reconciliation, or independent terminal outcome—from call count or one hop. The project-owned worksheet is `SHOULD` guidance only during active adoption/pilot work. Negative-adoption fixtures cover a stateless library/value/algorithm, presentation-only mechanical state, a passive adapter, refusal to establish one writer/authority/closed bounded contract, and pilot cost exceeding benefit; the minimal reading route follows §§0.1–0.2, §§3–5, §§6–8, only triggered §§9–13, conditional §14, examples §§15–16, checks §§17–18, and projection index §20.
- **Absence, applicability, lifecycle, and portability:** cover present and absent path/risk/claim predicates, `always` misuse, present-claim contradiction, accepted/unaccepted ambiguity, wrong scope/profile/revision/digest, every invalidation condition and reevaluation, plus actor-dependent/independent Query cases. Routine and negative-adoption work creates no proof/worksheet placeholder. `Draining` rejects new mutations, admits available Query/status reads with canonical success, rejects an unavailable read before `read`, and continues declared accepted-operation inputs. A clean relocated package with no governance files proves the sparse singleton, one shared declaration for two Balls, allowed delta, and stale/cyclic/conflicting/wrong-version/profile/environment rejection; the optional overlay and licensing baselines remain intact.

## 6. Claim record

```text
Review/claim ID and owner
Exact Core/package/source/environment baselines plus project policy/Assembly when in scope
Claimed scope and exact wording
Resolved policy references/deltas, exact WaiverRecords and conformance effects if any, trigger inventory, and every exact TriggerAbsenceProof actually relied upon
Questions, coverage units, previous evidence, exclusions
Applicable rules and RG-01..10 results
Mechanisms, assumptions, evidence artifacts, observed results
Explicit non-guarantees
Failed/partial units and follow-up trigger
```

Valid final wording is bounded: “On exact baseline X and claimed scope Y, gates A–J passed; no failed or partial unit remains in that scope.” Do not say “perfect,” “secure in general,” “production-ready,” or use an unqualified delivery/once-only claim.
