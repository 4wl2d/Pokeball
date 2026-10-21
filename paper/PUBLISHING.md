# Publishing Ballarch

The [paper](ballarch.tex) presents Ballarch Core 2.0 together with its references, its formal model and its benchmark. This file is the publication kit: what to check before submitting, where to submit, and what each venue requires.

Nothing has been submitted or published. Every step below is the author's decision.

## Before any submission

1. **Author information.** Add a contact e-mail address to `\author` in `ballarch.tex`. arXiv requires real, complete author information: *Vladislav Tomilov (Independent researcher)*, with `4wl2d` used only as a handle.
2. **The AI-use statement.** The paper says that an AI assistant drafted parts of the code, the benchmark and the text under the author's direction. Edit it until it matches what actually happened. Authors are fully responsible for AI-assisted content, and since 2026 arXiv sanctions clear evidence of unchecked AI output, such as invented references.
3. **References.** [`references.bib`](references.bib) was assembled from dblp and publisher records, but some fields could not be verified from here:
   - the Hitz & Montazeri venue city and pages;
   - the X/Open XA document details;
   - the Elle issue and pages;
   - the current issues of the Arm and PCIe specifications;
   - the year of the Chassaing blog post.

   Open each of these entries and check it against the source before submitting.
4. **Re-run every check** on the commit you submit ([verification guide](../docs/reproducibility.md)):
   - `./gradlew build checkModuleBoundaries`;
   - `make` in `reference/c` and `reference/rtl`;
   - the Alloy runner;
   - the benchmark;
   - the documentation checks;
   - `make arxiv` here.
5. **Freeze a version.**
   - Tag the release, for example `v2.0.0-rc.1`, and create a GitHub release.
   - With the Zenodo GitHub integration enabled, the release receives a DOI. [`.zenodo.json`](../.zenodo.json) supplies its metadata.
   - Add the DOI to the paper's *Artifacts* paragraph and to `CITATION.cff`.
6. **Repository name.** The repository is still called `Pokeball`. If it is renamed to `Ballarch`, GitHub redirects old URLs. Update the URLs in the paper, `CITATION.cff`, `.zenodo.json` and `NOTICE.md` afterwards.
7. **Software license.** The code has no software license. arXiv does not need one, but artifact evaluation and reuse do. Choose one before inviting independent implementations.

## arXiv

| Item | Value |
|---|---|
| Primary category | `cs.SE` (Software Engineering) |
| Cross-lists | `cs.DC` (Distributed, Parallel, and Cluster Computing), `cs.AR` (Hardware Architecture) |
| License | CC BY 4.0 (the same as the repository's text) |
| Source | `make arxiv` writes `build/ballarch-arxiv.tar.gz` (`ballarch.tex`, `references.bib`, `ballarch.bbl`). Upload the source, not the PDF: arXiv usually rejects PDFs produced from TeX. The archive was checked to build with plain `pdflatex`. |
| Comments field | `16 pages, 5 tables, 1 figure. Artifacts: https://github.com/4wl2d/Pokeball (v2.0.0-rc.1)` |

**Policies to be aware of** (checked in October 2026; verify again before submitting):

- **Position papers.** Since 31 October 2025, arXiv's CS section accepts review articles and position papers only after peer review. Research papers are not affected. This paper is written as research: definitions and theorems, a bounded formal model, artifacts, and an evaluation with stated method and threats to validity. Keep that framing; avoid "vision" or "manifesto" language in the abstract and the comments.
  - Source: https://blog.arxiv.org/2025/10/31/attention-authors-updated-practice-for-review-articles-and-position-papers-in-arxiv-cs-category/
- **Endorsement.** Since 21 January 2026, an institutional e-mail address alone no longer earns endorsement. A first-time submitter in `cs.SE` needs either an institutional address together with an accepted paper in the area, or a personal endorsement from an established author in the area. Ask one or two researchers whose work the paper cites and who have read it; arXiv discourages mass requests.
  - Sources: https://blog.arxiv.org/2026/01/21/attention-authors-updated-endorsement-policy/ and https://info.arxiv.org/help/endorsement.html
- **English.** A full English text is required. A Russian version may follow as a separate document; [`docs/ru/README.md`](../docs/ru/README.md) can serve as its basis.
- **Rate limits.** Since 1 October 2026: at most 2 new submissions per submitter per month, and 3 active at once.
- **BibTeX.** Since November 2025 arXiv accepts `.bib` files. The archive also contains the `.bbl`.

## Peer-reviewed venues

arXiv is a preprint server. A peer-reviewed venue gives the work standing, and some venues expect a preprint. Candidates, with what each expects:

| Venue | Fit | What it needs |
|---|---|---|
| **Onward! Papers** (SPLASH) | new ideas with a substantial worked example and an implementation | ≤ 17 pages + references, double-blind; anonymize the repository link |
| **The Art, Science, and Engineering of Programming** (*Programming* journal) | programming models and styles, with scholarly placement | ≤ 22 pages; three deadlines per year |
| **ICSA** (Software Architecture) research track or New and Emerging Ideas | architecture styles with evaluation | research track: empirical evaluation (the benchmark); NEMI: ≤ 5 pages |
| **EuroPLoP / PLoP** | the rules as a pattern language with known uses | pattern form, shepherding, writers' workshop |
| **Journal of Systems and Software**, New Ideas and Trends | ideas not yet fully validated | trend analysis, solution outline, preliminary results |
| **DATE, DAC or FPGA workshops** on HW/SW co-design | the cross-level part | a hardware-focused case study |

The [ACM SIGSOFT Empirical Standards](https://acmsigsoft.github.io/EmpiricalStandards/) (Engineering Research) are what reviewers will apply. Before a refereed submission, the evaluation most needs:
- independent implementations of the baselines;
- a second scenario, including one with an idempotent provider;
- measured change impact.

## Archives and indexes

- **Zenodo**: a DOI for each GitHub release. It is not moderated and gives the work a timestamped citation.
- **Software Heritage**: archives the repository permanently; request a save at https://archive.softwareheritage.org/save/
- **ORCID**: register an ORCID iD and add it to the paper and to `.zenodo.json`.
- **TechRxiv** (IEEE) can host a version aimed at hardware readers. **HAL** suits authors with a French affiliation.

## Announcing it

Announce once there is something people can run and a stable link:
- the arXiv abstract page;
- the Zenodo DOI;
- the repository.

| Where | Audience | Notes |
|---|---|---|
| Habr (habr.com) | Russian-speaking engineers | adapt [`docs/ru/README.md`](../docs/ru/README.md) and add the benchmark tables; hubs: "Архитектура", "Анализ и проектирование систем", "Программирование микроконтроллеров" |
| Hacker News (Show HN) | general engineering | lead with the benchmark and the runnable references, not the name |
| Lobsters | PL and systems | invitation required |
| Reddit r/programming, r/softwarearchitecture, r/FPGA, r/embedded | by level | one post per community, adapted to its level |
| The authors of the closest work | researchers | an e-mail with the paper; this is also the honest route to an arXiv endorsement |

Answer criticism with evidence: a failing check, a counterexample, or a change to the benchmark. The specification's claims are bounded, and the announcement should be too.
