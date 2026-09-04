# Pokeball Architecture — Core Specification

**Version:** `1.4.0-draft`<br>
**Status:** canonical draft<br>
**Date:** 2026-09-05<br>
**Language:** English; exact protocol type and field names use Latin identifiers

---

## Canonical document set

Pokeball Core is one ordered canonical document set. This file is its stable entrypoint and owns the version, status, complete path inventory, and reading order. Every manifest-listed part belongs to the same Core; no part is an independent specification or version, and there is no second assembled normative copy.

For each law, the marked `Source clause for PBA-xx` record in the numbered body remains the sole normative authority. Generated laws, indexes, tests, checklists, examples, glossary entries, public overviews, and the Agent Pack remain projections. A projection conflict is repaired in favor of its source record.

<!-- pkb:core-manifest:start -->
- [Core entrypoint](pokeball-architecture-core.md)
- [Status, scope, and goals](core/00-status-scope-goals.md)
- [Model, boundaries, and zones](core/03-model-boundaries-zones.md)
- [Protocol algebra](core/06-protocol-algebra.md)
- [State and authority](core/07-state-and-authority.md)
- [Decision and acceptance](core/08-decision-and-acceptance.md)
- [Asynchrony and delivery](core/09-asynchrony-and-delivery.md)
- [System composition](core/10-system-composition.md)
- [Security and privacy](core/11-security-and-privacy.md)
- [Profiles, limits, and performance](core/12-profiles-and-limits.md)
- [Manifest and source organization](core/14-manifest-and-organization.md)
- [Catalog search example](core/examples/15-catalog-search.md)
- [Checkout Flow: model and ingress](core/examples/16-01-checkout-model-and-ingress.md)
- [Checkout Flow: execution](core/examples/16-02-checkout-execution.md)
- [Checkout Flow: recovery and status](core/examples/16-03-checkout-recovery-and-status.md)
- [Verification: transition and property tests](core/verification/17-01-transition-and-property-tests.md)
- [Verification: boundary and architecture tests](core/verification/17-02-boundary-and-architecture-tests.md)
- [Verification: profile, security, and claim tests](core/verification/17-03-profile-security-and-claim-tests.md)
- [Practical checklist and anti-patterns](core/verification/18-checklist-and-antipatterns.md)
- [Laws PBA-01–18](core/reference/20-01-laws-boundary-decision-state.md)
- [Laws PBA-19–30](core/reference/20-02-laws-delivery-composition.md)
- [Laws PBA-31–44](core/reference/20-03-laws-security-bounds-claims.md)
- [Law applicability and navigation index](core/reference/20-04-law-index.md)
- [Adoption strategy](core/reference/21-adoption.md)
- [Glossary and canonical statement](core/reference/22-glossary-and-statement.md)
<!-- pkb:core-manifest:end -->

## Section index

| Section | Canonical chapter |
|---:|---|
| <a id="0-document-status-and-scope"></a>§0 | [Document status and scope](core/00-status-scope-goals.md#0-document-status-and-scope) |
| <a id="1-definition-of-pokeball"></a>§1 | [Definition of Pokeball](core/00-status-scope-goals.md#1-definition-of-pokeball) |
| <a id="2-goals-and-non-goals"></a>§2 | [Goals and non-goals](core/00-status-scope-goals.md#2-goals-and-non-goals) |
| <a id="3-canonical-model"></a>§3 | [Canonical model](core/03-model-boundaries-zones.md#3-canonical-model) |
| <a id="4-choosing-a-ball-boundary"></a>§4 | [Choosing a Ball boundary](core/03-model-boundaries-zones.md#4-choosing-a-ball-boundary) |
| <a id="5-three-logical-zones"></a>§5 | [Three logical zones](core/03-model-boundaries-zones.md#5-three-logical-zones) |
| <a id="6-protocol-algebra"></a>§6 | [Protocol algebra](core/06-protocol-algebra.md#6-protocol-algebra) |
| <a id="7-state-and-authority"></a>§7 | [State and authority](core/07-state-and-authority.md#7-state-and-authority) |
| <a id="8-decision-and-commit-semantics"></a>§8 | [Decision and commit semantics](core/08-decision-and-acceptance.md#8-decision-and-commit-semantics) |
| <a id="9-asynchrony-causality-and-delivery-semantics"></a>§9 | [Asynchrony, causality, and delivery semantics](core/09-asynchrony-and-delivery.md#9-asynchrony-causality-and-delivery-semantics) |
| <a id="10-system-composition"></a>§10 | [System composition](core/10-system-composition.md#10-system-composition) |
| <a id="11-security-and-privacy"></a>§11 | [Security and privacy](core/11-security-and-privacy.md#11-security-and-privacy) |
| <a id="12-execution-profiles"></a>§12 | [Execution profiles](core/12-profiles-and-limits.md#12-execution-profiles) |
| <a id="13-limits-budgets-and-performance"></a>§13 | [Limits, budgets, and performance](core/12-profiles-and-limits.md#13-limits-budgets-and-performance) |
| <a id="14-minimal-manifest-and-source-code-organization"></a>§14 | [Minimal manifest and source-code organization](core/14-manifest-and-organization.md#14-minimal-manifest-and-source-code-organization) |
| <a id="15-end-to-end-example-i-catalog-search"></a>§15 | [End-to-end example I: catalog search](core/examples/15-catalog-search.md#15-end-to-end-example-i-catalog-search) |
| <a id="16-end-to-end-example-ii-checkout-flow"></a>§16 | [End-to-end example II: Checkout Flow](core/examples/16-01-checkout-model-and-ingress.md#16-end-to-end-example-ii-checkout-flow) |
| <a id="17-testing-review-and-operational-verification"></a>§17 | [Testing, review, and operational verification](core/verification/17-01-transition-and-property-tests.md#17-testing-review-and-operational-verification) |
| <a id="18-practical-checklist"></a>§18 | [Practical checklist](core/verification/18-checklist-and-antipatterns.md#18-practical-checklist) |
| <a id="19-anti-patterns"></a>§19 | [Anti-patterns](core/verification/18-checklist-and-antipatterns.md#19-anti-patterns) |
| <a id="20-canonical-pokeball-laws"></a>§20 | [Canonical Pokeball laws](core/reference/20-01-laws-boundary-decision-state.md#20-canonical-pokeball-laws) |
| <a id="21-adoption-strategy"></a>§21 | [Adoption strategy](core/reference/21-adoption.md#21-adoption-strategy) |
| <a id="22-glossary"></a>§22 | [Glossary](core/reference/22-glossary-and-statement.md#22-glossary) |
| <a id="23-canonical-statement"></a>§23 | [Canonical statement](core/reference/22-glossary-and-statement.md#23-canonical-statement) |

The `Core part` heading, breadcrumb, and canonical-part banner at the top of each file are navigation metadata. Numbered headings, source records, and their existing order retain the architectural structure.

## Suggested reading paths

- **First implementation and everyday changes:** §§14.1–14.2 for the smallest source contract, §21.7 for responsibility and change routing, then only the triggered authoritative routes.
- **Meaning and semantic foundation:** §§0–3.
- **Ball, state, protocol, and composition design:** §§4–10.
- **Runtime profile selection:** §12.
- **Complete operational examples:** §§15–16.
- **Implementation and review:** §§11, 13–14, 17–19, 21–22.
- **Complete law audit projection:** §20; use §20.1 for compact applicability, ownership, source, and test navigation.

A full architectural audit follows the manifest order and covers every unique authoritative source. Ordinary work follows only the task-specific routes in §0.5 and the triggered sources they name.
