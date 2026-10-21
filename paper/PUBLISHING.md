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
   - the current issues of the Arm and PCIe specifications, and the statement that reads of a prefetchable PCIe memory range have no side effects;
   - the year of the Chassaing blog post;
   - Gray 1981: the passage on real actions deferred until commit, and its page, against the primary text (only the proceedings index and secondary sources could be reached);
   - the DOIs of Maeng and Lucia (PLDI 2019) and of Mukherjee et al. (ECOOP 2019, LIPIcs 134), and the article number of Surbatovich et al. (PACMPL 3, OOPSLA), which could not be confirmed and are left out;
   - Pardon and Pautasso (WWW 2014): the statement that a participant cancels a reservation not confirmed within its timeout and refuses a later confirmation was checked only against search-engine summaries of Pautasso's slides on the same design (inf.usi.ch, `rest-tcc`) and of a related paper, and against the description of a reviewer; the full text could not be reached from here;
   - the web sources (gRFC A6, the Temporal documentation, Leach's article, the protobuf and Smithy specifications, the Seata blog, the Narayana source): each `note` says what was checked on 2026-10-21; pin a commit or a date where the source has one;
   - the OMG Transaction Service specification is not cited, because its document number could not be verified; the paper cites the Narayana source for heuristic merging and interposition instead.

   Open each of these entries and check it against the source before submitting.
4. **Re-run every check** on the commit you submit ([verification guide](../docs/reproducibility.md)):
   - `./gradlew build checkModuleBoundaries`;
   - from the repository root, `python3 -m unittest discover -s tools -p 'test_*.py'`, then `python3 tools/oracles.py` on the conformance traces the build writes to `reference/kotlin/ballarch-runtime/build/conformance-traces/` (every top-level trace must pass, and with `--expect-violations` every trace in `seeded/` must fail);
   - `make` in `reference/c` and `reference/rtl`, and `make sanitize` and `make deep` in `reference/c` (the paper quotes the deep counts);
   - the Alloy runner;
   - the benchmark;
   - the documentation checks;
   - `make arxiv` here;
   - the character count of the abstract in [`arxiv-metadata.txt`](arxiv-metadata.txt), after the benchmark's results replace its `[NUMBERS PENDING]` marker. The results sentences of the LaTeX abstract, from "In a benchmark" to its end, run to 600 characters as plain text, but only 456 fit: cut them in `ballarch.tex` to that length (or shorten the rest of both abstracts by the difference) before copying them into `arxiv-metadata.txt`.
5. **Software license.** The code has no software license. arXiv does not need one, but artifact evaluation, reuse and a software DOI do. Choose one before tagging any release:
   - set `license` in `CITATION.cff` to its SPDX id (replacing `license-url`), and `"license"` in [`.zenodo.json`](../.zenodo.json) to its Zenodo id;
   - keep the CC BY 4.0 grant for text and diagrams in `message`, `notes` and `NOTICE.md`;
   - until this is done, do not enable the Zenodo GitHub integration: its record carries a single license id and cannot be withdrawn. A DOI for the paper alone can come from a manual Zenodo upload of the PDF and the specification as a `publication` under CC BY 4.0.
6. **Repository name (blocking, before tagging or submitting).** The paper's author block and *Artifacts* paragraph name the project Ballarch but still print the repository URL `https://github.com/4wl2d/Pokeball`, and say that the repository keeps the project's earlier name. An arXiv version and a Zenodo record keep whatever URL they carry, so rename first:
   - rename the GitHub repository to `Ballarch` (Settings, General, Repository name) and run `git remote set-url origin https://github.com/4wl2d/Ballarch`;
   - replace `https://github.com/4wl2d/Pokeball` with `https://github.com/4wl2d/Ballarch` in `ballarch.tex` (author block and *Artifacts* paragraph; drop the remark about the earlier name there and the comment under `\author`), `references.bib` (`BallarchRepo`, including its note), `arxiv-metadata.txt` (Comments), `CITATION.cff` (`repository-code`, `license-url`), `.zenodo.json`, `NOTICE.md`, `skills/ballarch/NOTICE.md` and `docs/ru/habr.md`;
   - keep the historical links to `tree/1bb9e0ecca4d4d1c8b39327c93988c1b490f303e` as they are; GitHub redirects them;
   - do not replace the URLs before the rename, or they will not resolve; rebuild `ballarch.pdf` afterwards.
7. **Freeze a version.**
   - Tag the release, for example `v2.0.0-rc.1`, and create a GitHub release; add `date-released` to `CITATION.cff` on that day.
   - With the Zenodo GitHub integration enabled, the release receives a DOI. [`.zenodo.json`](../.zenodo.json) supplies its metadata.
   - Add the DOI to the paper's *Artifacts* paragraph and to `CITATION.cff`.

## arXiv

| Item | Value |
|---|---|
| Primary category | `cs.SE` (Software Engineering) |
| Cross-lists | `cs.DC` (Distributed, Parallel, and Cluster Computing), `cs.AR` (Hardware Architecture) |
| License | CC BY 4.0 (the same as the repository's text) |
| Source | `make arxiv` writes `build/ballarch-arxiv.tar.gz` (`ballarch.tex`, `references.bib`, `ballarch.bbl`). Upload the source, not the PDF: arXiv usually rejects PDFs produced from TeX. The archive was checked to build with plain `pdflatex`. |
| Title, authors, abstract, comments, classes | Paste from [`arxiv-metadata.txt`](arxiv-metadata.txt). The abstract field takes plain ASCII text of at most 1,920 characters, so do not paste the LaTeX abstract; the file's text says the same in plain words. Fill in the page and table counts of the Comments field from the submitted PDF. |
| ACM classes | `D.2.11; C.2.4; D.4.5; D.2.4` (1998 ACM Computing Classification System), as in `arxiv-metadata.txt`. |

**Policies to be aware of** (checked in October 2026; verify again before submitting):

- **Position papers.** Since 31 October 2025, arXiv's CS section accepts review articles and position papers only after peer review. Research papers are not affected. This paper is written as research: definitions and proved propositions, a bounded formal model, artifacts, and an evaluation with stated method and threats to validity. Keep that framing; avoid "vision" or "manifesto" language in the abstract and the comments.
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
| **Onward! Papers** (SPLASH) | new ideas with a substantial worked example and an implementation | ≤ 17 pages + references, double-blind; anonymize the repository link; reformat to the `acmart` class with CCS concepts (`\ccsdesc`) and `\keywords`. The arXiv version's main text runs to page 19, so check the length after reformatting and move detail to the repository if needed |
| **The Art, Science, and Engineering of Programming** (*Programming* journal) | programming models and styles, with scholarly placement | ≤ 22 pages; three deadlines per year |
| **ICSA** (Software Architecture) research track or New and Emerging Ideas | architecture styles with evaluation | research track: empirical evaluation (the benchmark); NEMI: ≤ 5 pages; reformat to the IEEE conference template with Index Terms |
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
| Habr (habr.com) | Russian-speaking engineers | the draft article [`docs/ru/habr.md`](../docs/ru/habr.md); insert the arXiv and Zenodo links and recheck its numbers against the published commit; hubs: "Архитектура", "Анализ и проектирование систем", "Распределённые системы", "Программирование микроконтроллеров", "FPGA" |
| Hacker News (Show HN) | general engineering | lead with the benchmark and the runnable references, not the name |
| Lobsters | PL and systems | invitation required |
| Reddit r/programming, r/softwarearchitecture, r/FPGA, r/embedded | by level | one post per community, adapted to its level |
| The authors of the closest work | researchers | an e-mail with the paper; this is also the honest route to an arXiv endorsement |

Answer criticism with evidence: a failing check, a counterexample, or a change to the benchmark. The specification's claims are bounded, and the announcement should be too.
