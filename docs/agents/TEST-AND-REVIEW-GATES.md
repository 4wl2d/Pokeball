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

**Task routing:** routine test selection continues in [ROUTINE-TEST-MATRIX.md](ROUTINE-TEST-MATRIX.md); profile suites and the complete additive fixture catalogue continue in [TEST-FIXTURES-AND-PROFILES.md](TEST-FIXTURES-AND-PROFILES.md). The ten `RG-*` and ten `AP-GATE-*` definitions remain uniquely owned here.

## 1. Routine change

Moved to [Routine change](ROUTINE-TEST-MATRIX.md#1-routine-change).

## 2. Trigger-to-test matrix

Moved to [Trigger-to-test matrix](ROUTINE-TEST-MATRIX.md#2-trigger-to-test-matrix).

## 3. Conformance or release review

Only an explicit conformance/release claim runs the full review workflow. Freeze Core, package, source, environment, evidence, and any project policy or Assembly in the claimed scope. Resolve the whole claimed scope. When the claim relies on absence of a non-`always` trigger, record the exact Core `TriggerAbsenceProof` once rather than creating empty guardrail artifacts. Verify its trigger class/anchor, exact scope/effective profile, one closed inventory-evidence choice, inventory revisions/digests, evaluated predicate, `Absent` conclusion, evidence owner, and invalidation conditions. A present trigger or claim, `always`, unaccepted ambiguity, wrong/stale/unresolved/conflicting evidence, or an invalidation condition blocks reliance until reevaluation.

Every release gate is `pass`, `fail`, or `partial`; `partial` is not a claim pass.

| Gate ID | Name | Question |
|---|---|---|
| `RG-01` | Scope | Are claimed scope, authority, policy references, goals/non-goals, extensions, every exact WaiverRecord plus its conformance effect, and every absence proof actually relied upon explicit? |
| `RG-02` | Protocol | Are every used protocol/read/output category, command ingress/result egress, refusal carrier, read dependency, and boundary identity closed without copied, hidden, open-string, or compatibility-alias variants; and is each `Query` bound to one target-owned payload total over all reachable post-admission outcomes without a non-owner-invented carrier? |
| `RG-03` | Authority | Does each mutable fact/workflow have one authority/writer, with retained references honestly classified? |
| `RG-04` | Decision | Are explicit trusted inputs, purity, serial acceptance, the selected Snapshot/Event semantics, observable candidate isolation, complete accepted output retention and acceptance-before-dispatch proven? Do local command completions reenter only through the serialized source handler, keep refusal distinct from post-acceptance failure, and avoid mandatory absent carrier fields? Are actual numeric measures and capacity checks applied only where their triggers exist? |
| `RG-05` | Async | Are all reachable root Reply replay/pre-Intent conflict, command ACK-before-result/exact-result replay, provenance, identity, carrier/result/refusal meaning, retry, unknown/manual terminal, cancellation, delivery, and lossless status/materializer races total, including accepted-only root `OperationId`/known status, namespace-proven absence, and downstream target nonacceptance only as an already accepted source facet? |
| `RG-06` | Composition | Are real dependency kinds, owner-authored operation/result types, limited interfaces, Assembly wiring, Flow responsibilities and independent import/direct-control acyclicity inspectable? Can an extra allowed local consumer be wired without changing target caller names or result/domain semantics? Does complete execution structure bound local work including result handlers, while asynchronous feedback retains identity, finite budget and escape conditions? Are optional static graph counts kept as project policy? |
| `RG-07` | Security | Are every reachable trust, actor-origin for Decision and read semantics, protocol/type validation versus State/Context business-stage ownership, total target-owned actor-dependent read result and Policy-Gate selection, Policy/Execution Gate, bounded-operation real capability boundary, privilege, applicable safe-sink form, exact-path/scope secret flow, foundation, and unsafe path closed under its own trigger? |
| `RG-08` | Profiles | Does each effective profile and claimed guarantee name the exact boundary/scope, mechanism, assumptions, retention, evidence, and non-guarantees without inferring a stronger downstream guarantee from source durability or retained pending work? |
| `RG-09` | Limits | Does every actually growing input, state, output, queue, external-request, retry, retained or fan-out dimension have a finite effective bound and no accepted loss? Does a complete finite execution need no extra geometric calculation or carried budget? When a numeric byte/work/fan-out measure is selected, does its exact scope and unit support meaningful limit/overflow tests without partial acceptance or hidden reset? Are variable renaming and helper extraction free of checks tied to source wording? |
| `RG-10` | Integrity | Do all 44 Core-embedded source records, complete §20 audit entries, limited §20.1 applicability/ownership/navigation rows, explicit primary §17 test routes, policy refs, examples, tests, indexes, traceability, package integrity, bilingual public projections, and claim/absence evidence agree without orphan modal wording; and does exhaustive semantic review cover each unique authority/test/checklist route once while treating generated §§20/22 alternatives as mechanically equality-checked views rather than duplicate semantic reading? |


## 4. Profile suites

Moved to [Profile suites](TEST-FIXTURES-AND-PROFILES.md#4-profile-suites).

## 5. Agent Pack gates

These gates validate the package itself. They do not certify a consuming project.

| Gate ID | Name | Pass condition |
|---|---|---|
| `AP-GATE-01` | Package closure | Every declared file exists and every relative link resolves. |
| `AP-GATE-02` | Snapshot integrity metadata | The candidate Core entrypoint version/status, manifest file count, path-and-byte set digest/bytes, and package file count/digest match `BASELINE.md`; the Core-set digest occurs only there, and no internal review verdict is exported as package authority. Consumer readiness additionally requires that this exact file set and these exact bytes come from one immutable published snapshot. |
| `AP-GATE-03` | Noncanonical precedence | Every file is marked derived; no guidance overrides Core or exact policy precedence; every repeated projection modal names and remains set-equal to its marked body source clause, and an orphan `MUST`/`MUST NOT` scan is empty. |
| `AP-GATE-04` | Rule integrity | Exactly 37 `PKB-AR-*` definitions are unique and defined only in `AGENT-CONTRACT.md`: the prior 35 IDs in order plus only `PKB-AR-GOV-005` and `PKB-AR-PRT-004`; contract and traceability sets are equal. |
| `AP-GATE-05` | Core coverage | §§0–23 and all 13 Core §8.12 rows are indexed set- and order-equal by concern and anchor; the 44-row reference block explicitly resolves each Core-embedded source record to its complete §20 audit entry, limited §20.1 applicability/ownership/navigation row, and primary §17 test route, with exact unique ordered `PBA-01–44` sets in every projection; all 94 Core glossary terms are indexed; ordered union checks also reject bare `ModuleResult`, top-level `TimerFired`, `SignalOutput`, and compatibility aliases. |
| `AP-GATE-06` | Trigger traceability | Every agent rule has a Core source and complete source tuple: modal, class/exact trigger, declaration/semantic owner, scope, enforcement/failure behavior, reuse, absent-trigger behavior, and gate. Anchor equality alone does not pass. Exact §6.13 failure-stage, total admitted-read result, and closed-admission-reason mappings are traced. |
| `AP-GATE-07` | Policy and waiver closure | The compact project-policy template contains exact metadata/source/policy resolution, permits only local deltas, omits Ball inventories/empty optional sections, and is statically resolvable; any waiver has exactly the eight Core fields and neither weakens nor suppresses a guardrail. |
| `AP-GATE-08` | Applicability and fixture closure | The affected fixture catalogue passes for the exact candidate: finite local Counter calls and capability wiring, source/target acceptance and completion stages, added owner with shared mechanics, refactoring, Document isolation, and all still-triggered portable protocol, status, security, capacity and profile behaviors. No inferred trigger is suppressed; absent local tokens, carriers, static graph ceilings or reservation levels require no shape checks. Other unchanged fixtures retain their existing evidence scope. |
| `AP-GATE-09` | Claim boundary | Routine work requires no evidence dossier or absence-proof placeholder; every explicit claim contains its exact named boundary and scope, mechanism, assumptions, retention, evidence, non-guarantees, applicable waiver effects, and every relied-upon exact `TriggerAbsenceProof`, and claims no stronger downstream guarantee from source durability or retained pending work alone. Present/`always` contradictions or any `MUST`/`MUST NOT` violation block conformance in their exact scope. |
| `AP-GATE-10` | Governance-free portability | A clean standalone target with no source-repository governance files resolves a sparse singleton, triggered local Balls, and one shared declaration for two covered Balls plus an allowed delta; wrong/stale/cyclic/conflicting/version/profile/environment refs fail. The overlay is optional, no Query-specific limit field is invented, and package licensing, links, Core-set/package digests, and target software license remain intact. |

The complete additive revision fixture catalogue moved to [TEST-FIXTURES-AND-PROFILES.md](TEST-FIXTURES-AND-PROFILES.md#complete-revision-fixture-catalogue).

## 6. Claim record

```text
Review/claim ID and owner
Exact Core/package/source/environment baselines plus project policy/Assembly when in scope
Claimed scope and exact wording
Exact named guarantee boundary
Resolved policy references/deltas, exact WaiverRecords and conformance effects if any, trigger inventory, and every exact TriggerAbsenceProof actually relied upon
Questions, coverage units, previous evidence, exclusions
Applicable rules and RG-01..10 results
Mechanisms, assumptions, retention, evidence artifacts, observed results
For any cross-binding numeric maxTransitionSteps comparison: equal meterIdentity, meterVersion, transitionArtifactVersion, and unitDefinition; otherwise separate incomparable observations
Explicit non-guarantees
Proof that source durability or retained pending work alone establishes no stronger downstream guarantee
Failed/partial units and follow-up trigger
```

Valid final wording is bounded: “On exact baseline X and claimed scope Y, gates A–J passed; no failed or partial unit remains in that scope.” Do not say “perfect,” “secure in general,” “production-ready,” or use an unqualified delivery/once-only claim.
