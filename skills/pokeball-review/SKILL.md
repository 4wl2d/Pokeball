---
name: pokeball-review
description: Review Pokeball implementation code or a supplied diff for architecture and behavior regressions, or assess an explicitly requested conformance or guarantee claim. Use for review requests; ordinary implementation testing stays with the implementation workflow.
---

# Pokeball implementation review

Review the consuming project. Resolve supporting references within this skill's directory. Read [working contract](references/working-contract.md) once and select [review routes](references/review.md).

## Review the supplied scope

1. Freeze the supplied commit/diff or exact changed-file bytes. Name the affected owners and behavior, requested review questions and exclusions. Follow the diff into the necessary caller, binding, policy and tests; do not turn a local review into a repository-wide audit.
2. Resolve the relevant source clauses and effective project contracts. Check a concrete transition or failure trace against the actual implementation. Classify present paths and changed triggers before selecting tests. Logical roles in one file, absent adapters and reused in-scope evidence are not defects by themselves.
3. Trace the affected invariants: authority/writer, pure decision inputs, selected mutation/read result, atomic acceptance and dispatch; then only changed async, composition, trust, bound or recovery paths. Test or inspect actual enforcement rather than accepting names, annotations or diagrams as proof.
4. For each actionable finding, give a stable code location, violated source requirement, reproducible counterexample, practical effect and bounded fix criterion. Separate missing evidence from proven incorrect behavior. Do not invent findings to fill categories or describe a preferred design as a defect.
5. Report findings by impact, followed by the reviewed scope, checks run and unresolved evidence. Zero findings is valid for that exact scope. Remain read-only unless the request also authorizes fixes; do not publish comments merely because a review was requested.

## Claims are a separate requested scope

For an explicit conformance, release or stronger guarantee assessment, read the claim route before giving a verdict. Resolve the exact boundary, implemented mechanism, accepted project contract, effective profiles, assumptions, retention and applicable evidence. An absence-dependent verdict needs the Core-owned absence evidence; routine review creates no placeholder proof.

A waiver records its conformance effect; it does not override Core. If required evidence is missing, state that the named claim is unestablished. Documentation consistency or successful lint is not production, delivery or conformance evidence.
