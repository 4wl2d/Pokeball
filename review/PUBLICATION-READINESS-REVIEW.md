# Pokeball: publication-readiness review

**Reviewed snapshot:** `fd624cb` (Core `1.5.0-draft`, 2026-09-26)
**Question asked:** Is Pokeball ready to publish on arXiv and in journals? Is it unique? Can it live? What does it need first?
**Method:** I read the Core chapters on the model, acceptance, async, composition, profiles, examples and adoption, plus the guides and skills. I measured the repository mechanically. Three independent literature searches covered prior art, publication venues, and LLM-agent research.
**Evidence limits:** the proxy blocked arXiv full texts, USPTO and several publisher sites. Recent (2026) arXiv citations and the trademark record were therefore checked only through search-engine extracts or GitHub mirrors, and should be re-verified before anyone cites them. The arXiv policy text was read from arXiv's own documentation source.

---

## 1. Verdict

| Question | Answer |
|---|---|
| Ready to publish as a research paper today? | **No.** There is no paper-shaped text, no related-work section, no implementation, no evaluation and no formal model. Posting it to arXiv now risks a decline as a "position paper" under the CS rule of 31 Oct 2025, and a decline brings closer scrutiny of every later submission (§5.1). |
| Is it a *new architecture*? | **No.** Every mechanism already exists, usually in the same form. The core combination (pure decide, atomic state and outbox, single writer, dispatch after commit, no re-entrancy) already ships in Akka Persistence, Wolverine+Marten, fmodel/Decider stacks, Durable Entities and Reliable State Machines. |
| Is anything genuinely distinctive? | **Yes, at the level of specification rather than mechanism.** See §4. The four applicability triggers ("absent path, absent ceremony"), scoped claim records, and one law set covering UI state machines through durable backends, security and limits are not found together in any single prior work. How well a coding agent can follow the spec is also an open research question. |
| Can it live (be adopted)? | **Not yet.** The hardest part (the acceptor, outbox, status ledger and dispatcher, which the spec calls the "binding") is left to every adopter as "integration work". Successful architectures spread through a runnable library or a short, memorable text. Pokeball currently has neither. |
| Is it worth continuing? | **Yes, if it is reframed.** The engineering thinking is careful and the claims discipline is unusually honest. The ideas deserve a 15-page paper, a small runtime and one strong experiment, not 150,000 words. |

---

## 2. What Pokeball is, in standard vocabulary

This is how an informed reviewer will read it. The mapping comes from §5 and the prior-art table in §4.

| Pokeball term | What a reviewer will call it |
|---|---|
| Ball | Single-writer, state-owning module (an aggregate or actor) |
| Protocol Nucleus + `decide` | Elm/MVU `update`, a Mobius/TCA reducer, Chassaing's Decider, or an Erlang `gen_server` callback |
| Interaction Hemisphere / Resource Hemisphere | Driving and driven adapters (Hexagonal), or the imperative shell (FCIS, Sans-IO) |
| Pulse / SemanticOutput | Msg/Event and Cmd/Effect |
| Atomic acceptance + commit-before-dispatch | Transactional outbox, Akka `persist(...).thenRun`, Elm's runtime ordering |
| No reentrant transition | Run-to-completion / turn-based actor concurrency |
| SnapshotOutbox / EventJournal | State-stored vs event-sourced aggregate (fmodel, Akka DurableState vs EventSourced) |
| Flow Ball | Saga / Process Manager |
| Assembly | Composition root |
| Stale result by handle + generation | Fencing tokens, generation counters, TCA `cancelInFlight` |
| ACK/result, operation status | Asynchronous Request-Reply, Google AIP-151 long-running operations |
| `OutcomeUnknown` | Jepsen `:info`, gRPC `DEADLINE_EXCEEDED` semantics, CockroachDB ambiguous commit |
| Idempotency scope + key + fingerprint | Stripe/Brandur idempotency keys, AIP-155 |
| Capability-sealed effect, no ambient authority | Object capabilities (Miller 2006), WASI |
| Bounded everything | NASA/JPL Power of Ten (Holzmann 2006), TigerStyle |
| Profile-proportional mechanism | Risk-driven architecture (Fairbanks 2010), integrity levels (SIL/DAL/ASVS) |
| Claim Record | Assurance cases (GSN, Claims-Arguments-Evidence) at module granularity |

**One sentence:** Pokeball is a normative, trigger-scoped specification that combines Elm/Decider-style pure transitions, actor-style single writers, outbox-style commit-before-dispatch, explicit distributed-failure semantics, object-capability effects and bounded resources into one contract for stateful application modules.

---

## 3. Evaluation: strengths and gaps

### Strengths (keep these)

1. **Honest claims.** It lists non-goals (§2.2), states that "Pokeball does not promise an advantage on every project", and includes a pilot that is allowed to conclude "stop" (`docs/EVALUATION.md`). It admits there is no benchmark. Reviewers rarely see this, and it is the right scientific attitude.
2. **Failure semantics most app architectures ignore.** It separates ACK from result, treats timeout as "unknown" rather than failure, and handles cancellation that arrives too late. It covers stale results, retry ownership (one primary retry layer per failure mode), idempotency conflicts, and orthogonal status facets (§9.3). Clean, Hexagonal and Elm-style architectures say nothing here. This is the most practically valuable content.
3. **Proportionality as a rule rather than a slogan.** The `always / path / risk / claim` classes (§0.2, PBA-39) are the most original device found. They address the real reason rigorous architectures fail, which is ceremony on paths that don't need it.
4. **One model from UI to backend.** The same Ball works as an in-memory UI state machine (`Inline + Transient`) and as a durable backend aggregate (`SnapshotOutbox`/`EventJournal`).
5. **Clean documentation mechanics.** There are 129 Markdown files with 0 broken internal links. Each of the 44 laws has one source record plus generated projections, and there are 94 defined terms.
6. **Packaged for agents.** Installable `SKILL.md` skills and an Agent Pack arrive at the right time (see §5.3).

### Gaps (in order of severity for publication)

1. **No executable artifact.** There is no runtime, no reference binding and no example application. The only runnable example (`examples/local_composition.py` plus tests) was removed in `cc68409`. The generator and linter that maintain the `pkb:generated` regions and SHA-256 inventories are not in the repository either (removed in `c9b06f8`/`b4a8219`). A reader therefore cannot run anything or reproduce the consistency gate.
2. **No public evidence.** The public tree contains no pilot, case study, benchmark, fault-injection result, user study or formal model. `docs/EVALUATION.md` describes a good pilot that has not been run.

   The PR #9 description does report private validation:
   - 50 projection regression tests, 11 skill-tool tests and 13 local-composition tests;
   - a separate research harness covering 30 workflow cases, 27 boundary cases, 311 mapped concrete transitions and 32 negative checks.

   That harness and the accompanying manuscript are not published, so outside reviewers cannot inspect or reproduce them. Publishing the harness is the fastest way to turn this gap into evidence (see E2), though it would still be *finite* checking rather than proof. For every refereed venue, evidence is the deciding factor.
3. **Size and readability.** Measured on spec prose, excluding code and comments:
   - Size: about 80.5k words, and about 150k words of English Markdown overall.
   - Sentences: 21.9 words on average. The longest sentences are 544 to 660 words (bulleted run-ons).
   - Flesch-Kincaid grade is **18.0** and Flesch Reading Ease is **5**, which is the range of legal or regulatory text.
   - Vocabulary: 94 defined terms and 190 distinct CamelCase identifiers.
   - The explanatory example is heavy. The Catalog search needs about 2.9k words and about 32 type variants: 3 intents, 9 facts, 7 cancellation states, 5 lifecycle states, 6 state shapes and 2 effects.

   The core ideas fit in about 15 pages. At this density, reviewers and practitioners will not get past the first chapter.
4. **Renamed established concepts, no related work.** The spec invents metaphors for known ideas: Ball, Hemisphere, Nucleus, Pulse, State Belt, Polar Isolation, Double Quarantine. Only Clean Architecture and FCIS are cited, and only in `docs/EVALUATION.md`. Reviewers read renaming without attribution as reinvention, which is the fastest route to rejection. At minimum the paper must cite Elm/MVU, Decider, Akka Persistence, the transactional outbox, sagas, Helland (CIDR 2007), Durable Functions (OOPSLA 2021), Ambrosia (PVLDB 2020), Reliable State Machines (ECOOP 2019), Miller (2006), Holzmann (2006) and Fairbanks (2010).
5. **The laws are prose, not formal.** None of the 44 laws is machine-checkable. Some are meta-rules about governance rather than architecture (PBA-39 applicability, PBA-41 measured claim, PBA-42 guarantee scope), and a documentation-maintenance rule (Mermaid legends, §0.1) sits inside the normative Core. A paper needs a small formal core with checked safety properties (see §6, E2).
6. **Examples over-apply the machinery.**
   - The Catalog search is a *read-only* search with a "read-only credential" (§15.5), and §15.9 itself says "repetition is usually safe". It still models `OutcomeUnknown` and a 7-state cancellation facet. The root cause is that PBA-20 is triggered by *"may have executed"*, not *"execution has a consequence"*, so trigger design does not consider idempotency or reversibility.
   - §15.7 gives three alternative stale-result behaviours ("ignores… or records… or uses…"), not one normative trace.

   A reviewer will use the flagship example against the proportionality claim.
7. **The hardest guarantees are deferred.** Durable runtime and recovery, distributed delivery, event sourcing and replay, ownership transfer, and conformance tooling are all "separate future specifications" (§0). These are also the areas where competitors (Akka, Temporal, Durable Functions, Restate) already provide working runtimes with evaluations.
8. **Name and emblem.** "POKÉ BALL" is a registered Nintendo of America mark (US Reg. 6037776, Class 9 software and games; found via search snippets of uspto.report; the USPTO site itself was not reachable). `assets/pokeball-architecture-hero.svg` describes itself as "a rotated blue Pokeball emblem". Nintendo enforces its marks actively. Separately from the legal risk, the name makes the work hard to find and hard to take seriously in academic venues. This is not legal advice. Rename before formal publication, or at least drop the emblem and add a disclaimer that the project is not affiliated with Nintendo. An opinion from a trademark lawyer would settle it.
9. **Provenance.** The project is about 10 weeks old, with 24 commits from one author, and branch names show agent-driven development (`agent/core-1.3-…`). The spec tripled from 32.7k to 89.3k words in 12 days (2026-07-18 to 2026-07-30). Venues require the human author to take full responsibility, and most require disclosure of AI assistance. Growth driven by generation is also the most likely cause of the readability problem.

---

## 4. Uniqueness: what is and isn't new

**Not new (mechanisms with clear prior art):** pure transition returning state plus effects, snapshot vs event-sourced modes, outbox and commit-before-dispatch, single writer and no re-entrancy, sagas and process managers, idempotency keys, long-running-operation status, ambiguous outcomes, fencing and generation checks, capabilities, safe sinks, and bounding everything.

**Closest overall competitors:**

- **Akka Persistence Typed:** command handler returns an `Effect`, persist then `thenRun`, a single writer per persistence ID, stashing until persisted, and both state-based and event-sourced variants. It lacks enforced purity, capabilities, limits, and unknown/stale protocols.
- **Azure Durable Functions / Durable Entities** (OOPSLA 2021, Netherite VLDB 2022): deterministic orchestrations, single-threaded entities, and machine-checked "observably exactly-once" semantics.
- **Reliable State Machines** (ECOOP 2019) and **Ambrosia** (MSR-TR 2018, PVLDB 2020): state and outbox committed together, and non-determinism captured as logged "impulses" before commit.
- **The Decider family:** Chassaing's decide/evolve, fmodel (Decider/Saga/View, state-stored vs event-sourced, even the words "intent" and "facts"), and Wolverine+Marten (pure handlers with the outbox in the same transaction).
- **Elm/MVU, redux-loop, Mobius, TCA, Square Workflow:** the Transient profile, already mainstream on the client.
- **Helland, "Life beyond Distributed Transactions"** (CIDR 2007): the conceptual ancestor of Ball, Flow and the async laws.

**Plausibly original contributions (specification level):**

1. **Four applicability triggers as a normative conformance rule:** mechanisms exist only when a path, risk or claim is present, and ambiguity counts as present.
2. **Scoped claim records:** assurance-case discipline applied to module-level architectural guarantees, with explicit non-guarantees.
3. **One law set for UI-local and durable distributed stateful modules**, including security and resource bounds.
4. **Failure semantics as mandatory protocol variants** (`OutcomeUnknown`, ACK≠result, cancellation facets) inside the domain type, rather than transport error codes.
5. **Agent-followability:** architecture delivered as machine-consumable skills. This is *only* a contribution if it is measured; skill packs for Clean/DDD already exist.

**Honest framing for any paper:** *"A proportional specification that unifies and makes checkable known techniques for stateful application modules."* Do not use "a new architecture". Present the related-work mapping table (§2) first, not last.

---

## 5. Publication paths

Sources are official policy pages and calls for papers. Items taken only from search extracts are marked "(extract)"; the rest are "[judgment]".

### 5.1 arXiv: do not submit the current material

- **Content-type risk.** Since 31 Oct 2025, arXiv CS rejects review articles and position papers unless they have already passed journal or conference peer review; workshop review is "generally insufficient". The rule was introduced because LLMs make "papers not introducing new research results" easy to write ([arXiv blog](https://blog.arxiv.org/2025/10/31/attention-authors-updated-practice-for-review-articles-and-position-papers-in-arxiv-cs-category/)).
  - Moderators also decline "research proposals" and non-research books.
  - A prescriptive spec with no artifact, formal analysis or evaluation will very likely be classed as a position paper or book.
  - A decline has a lasting cost: arXiv warns that previously declined submitters "should anticipate closer scrutiny".
- **Endorsement.** Since 21 Jan 2026, an institutional email alone no longer grants automatic endorsement. An independent first-time author in cs.SE needs a personal endorser: someone with recent papers in the area, ideally an author you cite who has read your draft. Mass-emailing endorsers is explicitly discouraged.
- **Format and identity requirements:**
  - LaTeX source, not Markdown (pandoc conversion is fine).
  - English first; a Russian version may follow in the same PDF.
  - CC BY 4.0 is available as a license.
  - Real, complete author information is required: publish as *Vladislav Tomilov (Independent)*, with "4wl2d" only as a handle.
  - Significant generative-AI use must be disclosed, and the author takes full responsibility for the content.
- **What to do instead:** put the full spec on GitHub and Zenodo (DOI, no moderation). Post to arXiv (cs.SE with a cs.PL cross-list) only once the paper contains a research contribution: an implementation plus a checked model and/or evaluation, or after acceptance at a peer-reviewed venue.

### 5.2 Peer-reviewed venues that fit

| Venue | Fit | Needs | Timing (verify) |
|---|---|---|---|
| **Onward! Papers (SPLASH)** | Best conceptual fit: accepts "ideas that are well-argued but not yet proven", "exploratory implementations, and substantial examples" | ≤17 pp + refs, double-blind; small implementation + substantial worked example + prior-art positioning | 2027 deadline likely ~April 2027 (extract) |
| **\<Programming\> journal** | Strong: its "Art" category covers "programming models and styles… essays" | ≤22 pp; scholarly placement in prior work; "Engineering" papers need measurements | Deadlines 1 Feb / 1 Jun / 1 Oct |
| **EuroPLoP / PLoP** | Good and needs no code: present the laws as a pattern language with known uses (Elm, Akka, Decider, outbox) | Pattern form, shepherding, writers' workshop | EuroPLoP submission ~Feb; PLoP 2026 has closed |
| **ICSA NEMI (New and Emerging Ideas)** | Feasible: 5-page visions with "concrete plans" are welcome, but not "disguised advertisements" | ≤5 pp double-anonymous; research plan (use §6) | 2027 date unconfirmed |
| **JSS "New Ideas and Trends"** | Plausible: "not required to be fully validated" | Trend survey + outline of solution + preliminary results | Rolling |
| ICSA/ECSA research tracks, TOSEM, EMSE | Not yet: they require empirical evaluation | E1–E3 or E5–E6 | ICSA 2027 papers due 30 Oct 2026 (too soon) |
| IEEE Software, InfoQ, ICSA SA-in-Practice | After real use: they want practitioner experience | ≤4,200 words, 3 actionable insights (IEEE Software) | Months |

**Closest precedent to copy:** Meng & Jackson, *"What You See Is What It Does: A Structural Pattern for Legible Software"*. It is a new structural pattern for software, posted as an arXiv cs.SE preprint (2508.14511) and then published at Onward! 2025, and evaluated on the RealWorld ("Conduit") benchmark app. That is the right-sized template: a short paper, an implementation, and one realistic app.

**Reviewer checklist to satisfy.** Reviewers will use the ACM SIGSOFT Empirical Standards, "Engineering Research" standard. It requires:
- the artifact described in enough detail, and the need for it justified;
- a conceptual evaluation of strengths and limits;
- an *empirical* evaluation (case study, experiment, simulation or benchmark);
- a comparison with state-of-the-art alternatives.

Listed antipatterns include overstated novelty and "toy examples misrepresented as case studies". Design-science framing (Hevner et al. 2004; Wieringa 2014) lets a formal model and a worked application count as treatment validation.

**How comparable ideas spread.** No comparable architecture gained traction from a large specification alone:
- Elm/TEA, Redux and TCA spread through working libraries.
- Hexagonal and Clean Architecture spread through short texts by authors who already had an audience.
- The Decider spread through a short blog post with code.
- Lingua Franca earned academic credibility with a compiler and benchmarks.

The 150k-word spec should become the reference manual *behind* a short paper and a small library.

**Recommended order:**
1. Onward! 2027 or \<Programming\> with E1 + E2 + a RealWorld-style app.
2. In parallel, a EuroPLoP pattern-language paper or an ICSA NEMI vision paper.
3. Then the E6 agent study as a separate empirical paper.
4. Practitioner articles after real adoption.

### 5.3 The most distinctive research angle: agents

A literature scan (mostly abstract-level, because arXiv full texts were blocked by the proxy) points to a promising gap:

- **Repository context files have neutral-to-negative effects.** Studies of AGENTS.md/CLAUDE.md report no correctness gain and higher inference cost (e.g. arXiv 2602.11988, 2607.27250).
- **Skills help least in software engineering.** SkillsBench (2602.12670) reports only about +4.5 percentage points for SE tasks.
- **Architecture guidance is under-tested.** Evidence that it improves agent output comes from tiny baselines (2608.21747, n=3 unguided runs) or a single case study (Cervantes, Kazman & Cai, ICSE 2026 workshop).
- **Async and failure bugs are measurable.** Concurrency and distributed-bug benchmarks (CONCUR 2603.03683, DDBench 2608.14863) and an ambiguous-write study for tool-using agents (LIMBO 2609.29095) show these defects can be measured behaviourally.

**No study found tests whether a normative design discipline delivered as skills reduces state/async/failure-semantics defects, measured by hidden fault-injection oracles.** Either a positive or a null result would be publishable. Pokeball is well placed to be the treatment in that experiment (design in §6, E6).

---

## 6. What it needs before publication (ordered)

**Phase 0: hygiene (days)**
1. Decide the name. Remove or replace the ball emblem, and add a non-affiliation note if the name stays.
2. Freeze a `1.5.0` snapshot and publish the generator and linter, so the consistency claims can be reproduced.
3. Add `CITATION.cff`. Archive the snapshot on Zenodo to get a DOI and timestamp. This establishes priority without peer review.

**Phase 1: the paper text (2–4 weeks)**

4. Write a **12–15 page paper** in LaTeX, separate from the spec. Guidelines:
   - About 12 core concepts, with the standard term next to each Pokeball term.
   - A small core calculus: types for `decide`, acceptance, dispatch, and completion.
   - Every law grouped under 6–8 properties.
   - One worked example that really has side effects. The payment timeout-after-capture case is ideal; do not use the read-only catalog search.
   - Related work up front, using the table in §2.
   - A limitations section.
5. Fix the trigger design so that `OutcomeUnknown`, cancellation and retry machinery are triggered by **consequence** (non-idempotent or irreversible effect), not by mere possibility of execution. Re-do the Catalog example to show the *smaller* model this produces. This is also the best demonstration of the proportionality claim.

**Phase 2: evidence (1–3 months, pick at least E1+E2, ideally E3 or E6)**

- **E1. Reference binding.** A small library in 1–2 languages (for example TypeScript and Kotlin, Rust or Go) containing:
  - the Inline/Transient acceptor;
  - `SnapshotOutbox` on SQLite/Postgres;
  - a status ledger;
  - generation and correlation helpers.

  Without it, nobody can adopt Pokeball and nothing can be benchmarked.
- **E2. Formal model.** A TLA+ (or P/Alloy) model of acceptance, dispatch and crash points, outbox redelivery with an idempotent target, stale results, `OutcomeUnknown` and reconciliation, and status-facet merge. Check these safety properties:
  - `NoEffectWithoutCommittedCause`
  - `NoLostAcceptedOutput` (durable profile)
  - `StaleNeverOverwrites`
  - `StatusMonotonic`
  - `NoAcceptanceUnknownWithProvenTerminal`

  This turns the laws into checked claims and is the cheapest strong evidence for a specification paper.
- **E3. Fault-injection comparison.** Three slices: async search with cancel, payment with provider timeout, and a checkout saga. Implement each in (a) the Pokeball binding, (b) an idiomatic baseline, and (c) the closest competitor (Akka Persistence, Temporal, or TCA on the client). Run deterministic simulation: reorder, duplicate, drop, delay, crash at every step, and provider timeout after execution. Measure:
  - invariant violations (double charge, lost order, stale overwrite);
  - LOC by category (domain, support, tests);
  - change impact for three pre-registered change requests.

  Pre-register the scenarios, and have someone other than the author write the baseline.
- **E4. Microbenchmarks.** Use these only to back claims actually made:
  - Inline binding vs hand-written code (ns/op, allocations/op) to substantiate PBA-40 on a named toolchain;
  - outbox commit latency vs a plain transaction with an outbox table;
  - hot-key throughput under single-writer contention.
- **E5. Small human study.** 8–16 developers, within-subjects: one rule change, one new input and one async bug fix, Pokeball vs baseline. Measure time to green, wrong-owner edits and NASA-TLX. `docs/EVALUATION.md` step 4 already specifies this.
- **E6. Agent study, the most novel.**
  - Tasks: 10–12 feature tasks with async external effects, plus 2 pure-CRUD negative controls.
  - Conditions: C0 no guidance; C1 a generic reliability prompt matched to C2 by token count; C2 Pokeball skills; C3 the same content as always-on AGENTS.md.
  - Agents: 2 agent CLIs × 2 model sizes.
  - Oracles: hidden black-box oracles with effect ledgers and a fault-injecting proxy over 100–200 seeds.
  - Primary metric: "fault-robust pass".
  - Analysis: about 175–190 runs per condition gives 80% power to detect 40%→55%. Use mixed-effects logistic regression.
  - Controls: pre-register, and have tasks and oracles written independently of the spec author.

**Phase 3: submit.** See §5.

---

## 7. Can it live? Adoption view

- **Who benefits most:** teams building async-heavy clients (mobile/desktop with offline sync) and backends with irreversible external effects (payments, provisioning), and teams directing coding agents on such code.
- **Who should not use it:** CRUD services and stateless transforms. The spec says this itself (§21.1 negative adoption cases).
- **Main adoption barrier:** there is no library, so every team must build and verify its own acceptor, outbox and status ledger. Competitors hand these over ready-made.
- **Second barrier:** vocabulary and reading cost. Even the Quickstart is written in spec register.
- **What would make it live:**
  - a small runtime per ecosystem;
  - a two-page "Pokeball in one picture" with the standard vocabulary;
  - three runnable example apps;
  - the E6 result. If skills measurably reduce async defects in agent-written code, that result sells itself.

---

## 8. Measurements used in this review

| Metric | Value |
|---|---|
| English Markdown words (all) | 149,626 |
| Spec words (all) / prose only | 93,095 / 80,466 |
| Growth, 1.1.0 → now | 32,672 → 93,095 spec words (1.1.0 on 2026-07-18; 89,335 already by 2026-07-30) |
| Markdown files / broken internal links | 129 / 0 |
| Law source records / defined terms | 44 / 94 |
| Distinct backticked CamelCase identifiers | 190 |
| Mean sentence length / FK grade / Flesch ease | 21.9 words / 18.0 / 5 |
| Uppercase modals (MUST / MUST NOT / SHOULD / MAY; most normative force sits in the law `Rule:` fields instead) | 23 / 22 / 10 / 3 |
| Agent rules / review gates | 37 unique `PKB-AR-*` / 10 `AP-GATE-*` |
| Executable code, tests, generator in public tree | none (PR #9 reports private tests and a research harness; not published) |
| Commits / authors / age | 24 / 1 / about 10 weeks |
