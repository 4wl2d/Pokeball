---
name: pokeball
description: Implement and refactor stateful features with Pokeball Architecture. Use for ordinary feature code, business rules, local state and reads in a project using or explicitly adopting Pokeball.
---

# Pokeball feature development

Work in the consuming project's language, layout and existing binding. Resolve this file's physical directory through any installation symlink before following relative links; keep the consuming project as the working directory.

Read [working contract](../references/working-contract.md) once for this task. Use [feature routes](../references/feature.md) to read only the source sections needed for the change.

## Make the change

1. Find the mutable fact's owner, decision or read function, input types, accepted-write site and nearest behavior tests. Identify the requested behavior change and the binding/policy already covering it. For a new feature, start from one real invariant and inspect an adjacent implementation before introducing abstractions.
2. Keep the business choice in the owner's pure decision/read. Use explicit current input and only decision-relevant trusted context. Preserve the selected Snapshot or Event mutation form and the binding's single writer and atomic acceptance. Ordinary feature work reuses that binding.
3. Implement the smallest source and test delta. A quantity-limit change usually changes a decision and boundary-value tests. A stateless formatter remains a utility; logical roles do not mandate folders, interfaces, manifests or a new Ball.
4. Check the actual changed paths: external action, detached execution, foreign authority, durable state, trust, variable bound, or guarantee claim. Open the matching route below only when it exists or changes. Do not treat a newly activated obligation as covered by an unrelated existing test.
5. Run the affected behavioral checks. Report the owner and behavior changed, tests/results, any changed trigger, and an unresolved decision only if it prevents this implementation.

## Continue only for the affected work

| Changed work | Workflow |
|---|---|
| Detached effects, result ordering, retry, cancellation or operation status | [Async](../pokeball-async/SKILL.md) |
| Ownership split, cross-authority read/command/signal or workflow coordination | [Composition](../pokeball-composition/SKILL.md) |
| Writer, acceptance, scheduling, admission, trust-boundary or recovery mechanics | [Binding](../pokeball-binding/SKILL.md) |
| A requested implementation review or explicit guarantee assessment | [Review](../pokeball-review/SKILL.md) |

Use the focused workflow for that portion, then finish the original task. Reading it does not authorize a broader redesign, audit, or deployment.
