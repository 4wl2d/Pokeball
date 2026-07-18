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

After editing, run base tests and the tests selected by changed triggers. Report the diff, policy delta, commands/results, and any unresolved triggered decision. Do not record absent paths individually and do not make a conformance or stronger guarantee claim.

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
| Query | single result and purity; stamp only for its consistency/status/cross-time subtrigger |
| status | exact authority and only reachable lifecycle/access/stop/retention variants |
| inter-Ball edge | target-owned contract, sole Assembly edge, effective identity, graph/fan-out bounds |
| Flow/dispatcher | closed routes; coordinator only for stateful multi-authority workflow; compensation only when present |
| concurrent | mailbox/backpressure and finite workers; fairness only for starvation risk and out-of-order tests only when completion can reorder |
| durable | atomic state/event transaction and recovery; output/value/ambiguity/fencing/status cases only when those paths exist |
| trust/privilege/unsafe/secret | only the quarantine, capability, gate, sink, containment, or waiver tests selected by each subtrigger |
| policy reference/delta | when referenced: digest/scope resolution, cycle/conflict/mismatch, override allowlist, effective bound |
| claim | exact mechanism/assumptions/non-guarantees plus benchmark, fault, security, or conformance evidence |

Mocks of framework objects inside the Nucleus do not replace transition tests. A declaration does not replace enforcement. Reused evidence is valid only for the exact artifact digest and scope named by the policy or claim.

## 3. Conformance or release review

Only an explicit conformance/release claim runs the full review workflow. Freeze Core, package, source, environment, evidence, and any project policy or Assembly in the claimed scope. Resolve the whole claimed scope. For a trigger proven absent, record the closed inventory proof once rather than creating empty artifacts.

Every release gate is `pass`, `fail`, or `partial`; `partial` is not a claim pass.

| Gate ID | Name | Question |
|---|---|---|
| `RG-01` | Scope | Are claimed scope, authority, any policy references, goals/non-goals, extensions, and deviations exact? |
| `RG-02` | Protocol | Are every used protocol/read/output category and boundary identity closed without copied or hidden variants? |
| `RG-03` | Authority | Does each mutable fact/workflow have one authority/writer, with retained references honestly classified? |
| `RG-04` | Decision | Are explicit inputs, purity, present bounds, atomic acceptance, output ordering, and fault behavior proven? |
| `RG-05` | Async | Are all reachable identity, result, retry, unknown, cancellation, delivery, and status races total? |
| `RG-06` | Composition | Are all existing edges, effective protocol identities, triggered versions, Flow ownership, graph ceilings, and feedback escape conditions resolved? |
| `RG-07` | Security | Are every reachable trust, authority, interpreter, secret, and unsafe path closed? |
| `RG-08` | Profiles | Does each effective profile and claimed guarantee match mechanism, recovery/failure assumptions, and non-guarantees? |
| `RG-09` | Limits | Does every present variable dimension resolve to one finite enforced bound without unauthorized delta? |
| `RG-10` | Integrity | Do Core, policy refs, examples, tests, indexes, traceability, package integrity, and claim evidence agree? |

## 4. Profile suites

Run only suites selected by the effective profile or claim.

### Inline / Transient

- acceptance publication faults and complete in-process retained frame when outputs exist;
- exact N/N+1 causal-budget boundary when synchronous completion can cause another Decision, including `RetainedContinuation` fate;
- no hidden queue/thread/serialization tax;
- explicit process-loss boundary when external ambiguity can exist; and
- benchmark only when making a performance claim.

### BoundedConcurrent

- mailbox full/backpressure and finite workers; fairness only when separately queued inputs could starve;
- out-of-order input/result, plus duplicates only when duplicate delivery is possible;
- cancellation/deadline/result races only when those paths are reachable; and
- finite causal/fan-out budgets only for present causal or routed dimensions.

### SnapshotOutbox / EventJournal

- before/inside/after-transaction crashes; unknown commit only when ambiguity is possible;
- recovery from accepted records without rerunning current transition code;
- redelivery, ACK loss, attempt exhaustion, status, retention, and fencing only when reachable; and
- exact retained cross-Decision values and migrations only when later work uses them.

### Hardened / Isolated

- wrong issuer/audience/payload/expiry/revocation and current action proof only for the corresponding privileged controls;
- restricted credential/capability for external authority and safe-sink injection only for an interpreter edge;
- secret/redaction behavior only for a secret path; and
- isolated crash/escape/resource caps when containment is selected or claimed.

## 5. Agent Pack gates

These gates validate the package itself. They do not certify a consuming project.

| Gate ID | Name | Pass condition |
|---|---|---|
| `AP-GATE-01` | Package closure | Every declared file exists and every relative link resolves. |
| `AP-GATE-02` | Baseline integrity | Core hash/bytes/version and package digest match; the Core hash occurs only in `BASELINE.md`. |
| `AP-GATE-03` | Noncanonical precedence | Every file is marked derived and no guidance overrides Core or exact policy precedence. |
| `AP-GATE-04` | Rule integrity | All 35 `PKB-AR-*` definitions are unique and defined only in `AGENT-CONTRACT.md`. |
| `AP-GATE-05` | Core coverage | §§0–23, PBA-01–43, §20.1 applicability, and every glossary term are indexed. |
| `AP-GATE-06` | Trigger traceability | Every agent rule has a Core source, class/trigger, authoritative source type, enforcement/evidence scope, and gate. |
| `AP-GATE-07` | Policy-template closure | The compact project-policy template contains exact metadata/source/policy resolution, permits only local deltas, omits Ball inventories and empty optional sections, and is statically resolvable. |
| `AP-GATE-08` | Applicability closure | Positive and negative fixtures prove that inferred triggers activate guardrails and cannot be suppressed, while closed absent paths add no artifacts. |
| `AP-GATE-09` | Claim boundary | Routine work requires no evidence dossier; every explicit claim contains exact scope, mechanism, assumptions, evidence, and non-guarantees. |
| `AP-GATE-10` | Portability | A standalone target resolves both sparse and triggered fully local Balls without an overlay and, separately, relocates one shared policy for two covered Balls plus an allowed delta; it retains licensing, links, hashes, and target software license. |

## 6. Claim record

```text
Review/claim ID and owner
Exact Core/package/source/environment baselines plus project policy/Assembly when in scope
Claimed scope and exact wording
Resolved policy references/deltas, if any, and trigger inventory
Questions, coverage units, previous evidence, exclusions
Applicable rules and RG-01..10 results
Mechanisms, assumptions, evidence artifacts, observed results
Explicit non-guarantees
Failed/partial units and follow-up trigger
```

Valid final wording is bounded: “On exact baseline X and claimed scope Y, gates A–J passed; no failed or partial unit remains in that scope.” Do not say “perfect,” “secure in general,” “production-ready,” or use an unqualified delivery/once-only claim.
