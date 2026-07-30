<h1 align="center">Pokeball</h1>

<p align="center">
  <a href="spec/pokeball-architecture-core.md">
    <img src="assets/pokeball-architecture-hero.svg" alt="Pokeball Architecture" width="100%" />
  </a>
</p>

<p align="center">
  <strong>An architecture specification for applications built from explicitly composed, state-owning functional modules with pure bounded decisions, closed typed protocols, and evidence-qualified guarantees.</strong>
</p>

<p align="center">
  <a href="spec/pokeball-architecture-core.md"><img alt="Core specification" src="https://img.shields.io/badge/CORE-SPECIFICATION-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="docs/agents/README.md"><img alt="Agent Pack" src="https://img.shields.io/badge/AGENT-PACK-0969DA?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="docs/ADOPTION.md"><img alt="Adoption guide" src="https://img.shields.io/badge/ADOPTION-GUIDE-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a><br />
  <a href="docs/ru/README.md"><img alt="Russian overview" src="https://img.shields.io/badge/RU-OVERVIEW-0969DA?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="#documentation"><img alt="Documentation" src="https://img.shields.io/badge/READ-DOCUMENTATION-1F6FEB?style=flat-square&amp;labelColor=0D1117" /></a>
  <a href="#license-and-authorship"><img alt="CC BY 4.0 license" src="https://img.shields.io/badge/CC_BY_4.0-LICENSE-58A6FF?style=flat-square&amp;labelColor=0D1117" /></a>
</p>

<table>
  <tr>
    <td width="33%" align="center">
      <strong>Explicit ownership</strong><br />
      <sub>One explicit canonical state scope per Ball.</sub>
    </td>
    <td width="33%" align="center">
      <strong>Pure bounded decisions</strong><br />
      <sub>No I/O or ambient authority in the Nucleus.</sub>
    </td>
    <td width="33%" align="center">
      <strong>Closed typed protocols</strong><br />
      <sub>Only used paths, each with an effective finite bound.</sub>
    </td>
  </tr>
</table>

> [!IMPORTANT]
> For each law, the marked `Source clause for PBA-xx` in the numbered body of the [Core specification](spec/pokeball-architecture-core.md) is the sole normative authority. The law list, applicability matrix, tests, checklist, examples, glossary, this README, and the [Agent Pack](docs/agents/README.md) are projections. If a projection differs from its source clause, the source clause controls.

This repository contains the Core specification and practical adoption guidance. It does not contain a Pokeball runtime, framework, library, or reference implementation.

## What problem does Pokeball address?

Stateful applications become difficult to reason about when state ownership, side effects, asynchronous results, authorization, retries, and delivery guarantees are implicit. Pokeball makes those decisions visible in the architecture:

- Who owns each mutable fact?
- Which exact inputs may change it?
- Which typed outputs may leave the module, and only after what acceptance point?
- How does an asynchronous result prove which operation caused it?
- Which queues, retries, fan-out, state, and work limits are finite?
- Which guarantees come from Core semantics, and which require a concrete runtime profile and evidence?

Pokeball does this without requiring a mediator, broker, queue, reflection, serialization, runtime DI container, or a particular database for a local `Inline` binding.

## The architecture in one sentence

A `Ball` is the smallest operationally feasible scope in which one authority can enforce the required invariants without reading another Ball's mutable state.

Each Ball:

1. owns one explicit canonical state scope;
2. separates external interaction, pure decision logic, and resource execution;
3. exposes only the closed typed protocol paths it actually uses;
4. atomically accepts its snapshot state mutation or `EventDecision` together with the complete semantic-output batch;
5. dispatches no `SemanticOutput` before that acceptance;
6. gives detached or independently observable work stable causal identity and verified provenance;
7. keeps every present variable dimension within one finite effective bound.

Pokeball has one sparse Core, not separate “Lite” and “full” architectures. Its always-applicable invariants remain universal. A reachable path or named risk activates only its guardrail; a concrete claim activates its evidence. Each activated guardrail resolves once through construction, a local declaration, or an exact immutable project-, profile-, Assembly-, or binding-scoped policy, with only permitted Ball-specific deltas. A proven-absent trigger needs no empty table, zero, `N/A`, or evidence placeholder.

`TriggerAbsenceProof` is materialized only when a Pokeball conformance or release claim, or an accepted ambiguity-resolution decision, relies on trigger absence. Routine design, implementation, adoption, or piloting creates no proof merely because a category is absent. An `always` obligation or present trigger cannot use such a proof; contrary present evidence invalidates it and requires the guardrail to resolve.

Core constrains valid authority and dependency graphs; it does not promise one unique decomposition graph. Boundary choices are falsifiable:

- combine parts when one strict invariant requires one state key, writer, lifecycle, and recovery unit, unless a stronger trust, ownership, partition, durability, or containment boundary forbids it;
- separate parts when they need independent owners, state keys, lifecycles, terminal outcomes, trust/durability boundaries, or material load/containment;
- keep stateless mechanics as a Ball-local utility owned by one logical role, or as shared mechanical Foundation; shared domain/business semantics stay as explicit local implementations or acquire one Ball/Flow owner and a declared Application Surface/protocol;
- use a `Feature Ball` for one local capability's state and decisions, a `Flow Ball` for material cross-authority coordination, and a `Read Model Ball` only for derived query state, source positions, freshness, and rebuild policy without command authority.

A Ball is not automatically a screen, endpoint, table, repository, service, aggregate, or use case. `EphemeralState` is limited to focus, scroll, animation, layout, transport, and equivalent mechanics that cannot change a business `Decision`; a decision-relevant UI or transport value is committed State or an explicit trusted current `Pulse`/`DecisionContext` input.

## One Ball at a glance

A Ball owns one explicit canonical state scope and keeps Interaction, pure Nucleus decisions, and Resources logically separate. [Read the complete Ball and logical-zone guide](docs/ARCHITECTURE.md#one-ball-at-a-glance).

## The decision and acceptance model

Pokeball accepts State or an Event decision together with the complete semantic-output batch, then dispatches only after acceptance. [Read the complete decision, command/result, and law guide](docs/ARCHITECTURE.md#the-decision-and-acceptance-model).

## How an application is composed

Applications combine Feature, Flow, and Read Model Balls through explicit bounded dependencies; utility packages remain stateless mechanics. [Read the complete composition guide](docs/COMPOSITION.md#how-an-application-is-composed).

## Core rules developers must preserve

The 44 stable `PBA-*` laws remain projections of their unique marked Core source clauses. [Use the developer-facing law map](docs/ARCHITECTURE.md#core-rules-developers-must-preserve).

## Profiles: pay only for the guarantees you use

Profiles are independent dimensions and cannot waive an activated guardrail. [Compare the profiles and their costs](docs/ADOPTION.md#profiles-pay-only-for-the-guarantees-you-use).

## Guarantee boundaries

Every guarantee ends at its named boundary and requires the mechanism and evidence claimed for that scope. [Review the explicit non-guarantees](docs/ADOPTION.md#guarantee-boundaries).

## A typical project layout

Folders are non-normative; authority and dependency direction are what matter. [See the suggested project shape and its constraints](docs/ADOPTION.md#a-typical-project-layout).

## Adopt Pokeball

Start with one suitable vertical slice, add only triggered machinery, and stop or reshape the pilot when its measured cost exceeds its benefit. [Follow the complete adoption workflow](docs/ADOPTION.md#adopt-pokeball).

## Documentation

### Where to start

- **Learn the architecture:** read this overview, then the [architecture](docs/ARCHITECTURE.md) and [composition](docs/COMPOSITION.md) guides; use Core §§0–3 for scope and semantics and §§4–10 for boundaries, state, protocols, and composition.
- **See complete flows:** study the Catalog and Checkout walkthroughs in Core §§15–16.
- **Apply Pokeball:** follow the [adoption guide](docs/ADOPTION.md), use the [Agent Pack](docs/agents/README.md) for design and review guidance, and follow its [installation guide](docs/agents/INSTALL.md) when bringing it into another repository.

### Minimal Core reading path

1. §§0.1–0.2 — source-clause authority, applicability, absence, and exact reuse.
2. §§3–5 — canonical model, falsifiable Ball boundaries, and logical roles.
3. §§6–8 — closed protocols, state authority, pure decisions, reads, and acceptance.
4. Read only the triggered parts of §§9–13 for async delivery, composition, security, profiles, and bounds.
5. Use §14 only when materializing a manifest or Assembly view; use §§15–16 as worked examples, §§17–18 for evidence/checks, §20 as the complete generated audit projection, §20.1 as the limited applicability/ownership/navigation index, and §22 for glossary lookup.

### Main documents

| Document | What it contains |
|---|---|
| [Core specification](spec/pokeball-architecture-core.md) | Stable entrypoint and reading map for the complete ordered Core document set |
| [Architecture guide](docs/ARCHITECTURE.md) | Non-normative Ball, decision, acceptance, and law orientation |
| [Composition guide](docs/COMPOSITION.md) | Non-normative application roles, dependencies, reads, status, security, and Foundation orientation |
| [Adoption guide](docs/ADOPTION.md) | Non-normative profiles, guarantee boundaries, project shape, and pilot workflow |
| [Agent Pack](docs/agents/README.md) | Practical guidance and runbooks derived from the Core for applying Pokeball in another project |
| [Russian overview](docs/ru/README.md) | Russian-language introduction to the architecture |
| [License](LICENSE) and [notice](NOTICE.md) | License terms, authorship, scope, and reusable attribution |

## License and authorship

Copyright © 2026 **Vladislav Tomilov (4wl2d)**. `4wl2d` is his public
pseudonym. The original specification, documentation,
diagrams, examples, and Agent Pack are licensed under
[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/)
(`CC-BY-4.0`). Anyone may share and adapt those materials for any purpose,
including commercial use, subject to CC BY 4.0: retain the supplied creator,
copyright, license, and warranty-disclaimer notices; include the license text
or URL; link the source to the extent reasonably practicable; and indicate
changes while retaining prior change notices. No ShareAlike condition applies.

This licenses the copyrightable expression of Pokeball Architecture; it does
not create exclusive copyright ownership in abstract ideas, methods, systems,
or functional concepts. CC BY 4.0 does not grant patent or trademark rights.
See [`NOTICE.md`](NOTICE.md) for the exact scope and recommended attribution,
and [`LICENSE`](LICENSE) for the complete legal code.
