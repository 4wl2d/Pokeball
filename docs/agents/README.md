# Pokeball Agent Pack

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Before using it, verify the integrity manifest and published immutable ordered Core set recorded in [BASELINE.md](BASELINE.md). If package guidance conflicts with Core, Core prevails.

This package turns Core into a sparse workflow for project agents. It preserves every applicable guardrail while avoiding duplicate policy, empty sections, and claim evidence for properties the project does not claim.

Every contract row, runbook, gate, example, index, and summary here is a projection of one or more marked Core source clauses. It has no independent normative authority and cannot strengthen or weaken Core.

## Start

1. Verify the package files and published immutable ordered Core set against [BASELINE.md](BASELINE.md); this is an integrity check, not an architectural-quality verdict.
2. Read the authoritative typed source or manifest for the affected Ball; read an exact accepted project policy only when referenced, and Assembly only when an inter-Ball edge exists.
3. Classify each relevant law as `always`, `path-triggered`, `risk-triggered`, or `claim-triggered` using Core §20.1.
4. Resolve every triggered guardrail through construction, one local declaration, or one exact immutable policy reference plus a permitted delta.
5. Read only the runbooks selected by the triggers below.
6. For routine work, test the affected semantics and deltas. For a conformance or other guarantee claim, run the full gates in [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md).

A missing optional path produces no placeholder artifact. An inferred trigger cannot be disabled by omitting metadata. If absence is ambiguous, treat the trigger as present or obtain an accepted decision. Routine work creates no `TriggerAbsenceProof`; one is materialized only when a conformance/release verdict or accepted ambiguity-resolution decision relies on absence.

## Helping a human make an ordinary change

Start from the affected feature source and its existing tests, then use Core §21.7. Explain the owned fact, the requested business change, the acceptance point, and only the paths this change activates. A small local feature can use one source file, a pure function, a serial owner, and a direct local getter under Core §§3.3, 5, 8.5 and 8.10. Show those concrete source locations before presenting a catalog of terminology or scaffolding folders.

The feature author owns decisions and affected tests. The binding owner supplies and verifies reusable writer, acceptance, dispatch and triggered resource mechanics; the same person may hold both responsibilities. Reuse is justified by exact scope and assumptions, not by a label. A production claimant resolves actual project evidence. No human or agent needs to repeat unchanged binding design for every business-rule edit, and the short path does not erase a new trigger.

## Task routing

| Trigger or task | Read first | Continue only when triggered |
|---|---|---|
| Ball boundary, state kind, or always-applicable design | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) | [PROTOCOL-DESIGN-RUNBOOK.md](PROTOCOL-DESIGN-RUNBOOK.md) for a protocol, accepted frame, output, lifecycle, or read; [BOUNDS-AND-CHANGE-DESIGN.md](BOUNDS-AND-CHANGE-DESIGN.md) for bounds, profiles, impact, or review output |
| Inter-Ball command ingress/result return, refusal, root idempotency, retry, or cancellation | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) | [STATUS-AND-ASYNC-TESTS.md](STATUS-AND-ASYNC-TESTS.md) for status, materialization, delivery-stop evidence, or the async test catalogue |
| Cross-authority Query/read, command-vs-read choice, utility ownership, Application Surface, FlowParticipation, graph, or cumulative fan-out | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) | [COMPOSITION-PROFILES-AND-CLAIMS.md](COMPOSITION-PROFILES-AND-CLAIMS.md) for the effective profile, explicit claim, Foundation, or change output |
| Trust/Trusted Boundary edge, protocol-validation versus State/Context business stage, capability, safe sink, unsafe path, or secret | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) | [LIMITS-AND-EVIDENCE-RUNBOOK.md](LIMITS-AND-EVIDENCE-RUNBOOK.md) for an effective finite bound, Decision Work Meter, admission/economic authority, or evidence reuse |
| Authority map, policy resolution, or sparse resolved view | [MANIFEST-AND-ASSEMBLY.md](MANIFEST-AND-ASSEMBLY.md) | [ASSEMBLY-AND-MANIFEST-VALIDATION.md](ASSEMBLY-AND-MANIFEST-VALIDATION.md) for imported edges, Assembly, static validation, or change discipline |
| Runtime/acceptor mechanics, concern placement, or ownership | [REFERENCE-INDEX.md](REFERENCE-INDEX.md) §8.12 runtime concern index | the exact Core anchors named there; Runtime owns no business policy or direct State write |
| Ordinary change tests | [ROUTINE-TEST-MATRIX.md](ROUTINE-TEST-MATRIX.md) | shared evidence at its accepted scope; no claim record |
| Conformance/release claim, profile suite, or full package fixture catalogue | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) | [TEST-FIXTURES-AND-PROFILES.md](TEST-FIXTURES-AND-PROFILES.md) and [TRACEABILITY.md](TRACEABILITY.md) |
| Verdict or accepted ambiguity decision relying on trigger absence | [MANIFEST-AND-ASSEMBLY.md](MANIFEST-AND-ASSEMBLY.md) proof contract | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) |
| Catalog or Checkout analogy | [EXAMPLE-CROSSWALK.md](EXAMPLE-CROSSWALK.md) | [EXAMPLE-CHECKOUT.md](EXAMPLE-CHECKOUT.md) only for an analogous Checkout property; canonical Core §§15–16 control |
| Install or update the package | [INSTALL.md](INSTALL.md) | [PORTABILITY-VALIDATION.md](PORTABILITY-VALIDATION.md) and optional [PROJECT-OVERLAY.template.md](PROJECT-OVERLAY.template.md) |

## Source precedence

1. the exact ordered Core set pinned in `BASELINE.md`;
2. an accepted extension, only in its declared scope;
3. an accepted exact project policy, only for mechanisms or values Core leaves open;
4. `AGENT-CONTRACT.md`, runbooks, traceability, and indexes;
5. examples.

Policy reuse is static. A reference includes owner, ID, revision, digest, scope, covered guardrails, effective mechanisms or values, allowed override fields, enforcement ownership, and scoped evidence when evidence is required. It is not a runtime lookup, ambient default, or permission to weaken Core.

A policy cannot suppress an applicability trigger, weaken a law, or authorize a direct-control cycle. An exact Core WaiverRecord may document deliberate nonconformance, but it is neither policy nor precedent and does not satisfy the waived guardrail. A violated `MUST` or `MUST NOT` blocks a conformance claim for the waiver's exact scope according to its recorded conformance effect; a waived direct-control cycle remains deliberate nonconformance.

## Two workflows

### Routine application

Resolve the affected Ball and any policy it references, infer triggers from the closed source/profile/route/risk inventory, change only the semantic delta, and run base plus triggered tests. Reuse shared mechanism and evidence at its exact scope. No review dossier or absence-proof placeholder is required.

### Claim or release

Freeze exact baselines, resolve the whole claimed scope, and run all applicable profile and `RG-*` gates. A claim record names its exact boundary and scope, mechanism, assumptions, retention, evidence, and non-guarantees; source durability or retained pending work alone supplies no stronger downstream guarantee. When the verdict actually relies on a non-`always` trigger being absent, record one exact scope/profile/inventory/digest-bound `TriggerAbsenceProof`, reject contradiction or invalidation, and assign evidence ownership; otherwise create no proof. Evaluate every applicable waiver's conformance effect and attach evidence to the claim record. Without that record, omit the claim.

## Package contents

`AGENT-CONTRACT.md` defines all stable `PKB-AR-*` rules once. The task-split runbooks provide procedures, with their original filenames retained as stable entry routes; `TRACEABILITY.md` maps rules to Core, triggers, authoritative sources, and gates. `PROJECT-OVERLAY.template.md` is an optional compact shared project-policy template, not a Ball inventory or installation requirement. `REFERENCE-INDEX.md` supplies the explicit 44-row source-record → complete §20 audit projection → limited §20.1 applicability/ownership/navigation index → primary §17 test route, complete glossary lookup, and the reference-only §8.12 runtime concern routes. An exhaustive semantic audit reads each unique authoritative source once and checks generated alternatives mechanically; ordinary task routes are smaller, but neither route implies that all unique Core content is trivial to review. The index is not a statement that every indexed rule applies to every Ball.

The package does not select project identities, profiles, policies, routes, grants, or claims; create a runtime or extension; or turn Catalog and Checkout into mandatory templates.

## License

Retain the scoped [LICENSING.md](LICENSING.md) when transferring the exact package. It does not replace or alter the consuming project's software license.
