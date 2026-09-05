# Implementation review routes

For an ordinary code or diff review, start with the affected source clauses. These route tables identify them without loading another skill's full workflow:

- [Feature](feature.md): State, decision, input validation, read, ordinary bounds.
- [Async](async.md): accepted causes, result provenance, status, retry/cancellation and unknown outcome.
- [Composition](composition.md): authorities, owned surfaces, reads, Flow and dependency graphs.
- [Binding](binding.md): writer, acceptance, admission, dispatch, profiles and recovery.

Read only the matching table and source row, then the relevant §17 test section. Use the affected portion of [§18](../../spec/core/verification/18-checklist-and-antipatterns.md#18-practical-checklist) to check omissions; do not turn every checklist category into a finding or a mandatory artifact.

## Explicit conformance or guarantee assessment

This route applies only when the request asks for that verdict.

1. Read [§13.4 claim contract](../../spec/core/12-profiles-and-limits.md#134-claim-contract) and [§17.9 evidence](../../spec/core/verification/17-03-profile-security-and-claim-tests.md#179-claim-records-and-benchmarks). Establish the exact claimed scope and boundary before selecting the evidence.
2. Resolve the accepted project contract, present source/profile/route/risk/claim inventory, and the complete applicable requirements in the [law applicability index](../../spec/core/reference/20-04-law-index.md#201-applicability-ownership-and-navigation-index). The index locates controlling source clauses; it is not the full ruleset.
3. For an absence-dependent verdict, read the exact evidence shape in [§0.2](../../spec/core/00-status-scope-goals.md#02-proportionality-principle); for a deliberate deviation, read [§0.3](../../spec/core/00-status-scope-goals.md#03-waivers-and-conformance-effect).
4. If the project already applies the Agent Pack, retain its accepted review contract and applicable gates. Its [review gates](../../docs/agents/TEST-AND-REVIEW-GATES.md) remain a derived workflow within that project contract. A coding skill alone does not establish an accepted conformance contract.

Missing evidence leaves the named claim unestablished. Keep that verdict separate from any concrete code defect, and from operational guarantees outside the supplied implementation and evidence.
