# Paper

*Ballarch: Boundaries in Time and Honest Outcomes, from Register-Transfer Logic to Services* ([PDF](ballarch.pdf), [LaTeX source](ballarch.tex), [references](references.bib)).

The paper presents Ballarch Core 2.0:
- the boundaries in time;
- the outcome classification;
- the knowledge algebra, its soundness and exactness propositions and the composition-closure corollary, with explorations of a composite (the trip) and of a composite of composites (the itinerary);
- the rules, with the mechanisms that check them at each of three levels, including the knowledge algebra in firmware and hardware;
- the language-neutral conformance trace and the reference runtime's checks for code changes under open obligations;
- the Alloy model;
- the benchmark: one checkout feature in nine implementations (four styles, each in a naive and a careful version, and Ballarch) against providers without and with idempotency keys, with an exhaustive single-crash sweep, seeded bugs, four change requests and a measurement of runtime overhead.

Every number in it comes from a check, a data file or a source file in this repository, and the [verification guide](../docs/reproducibility.md) shows how to reproduce them. The runtime overhead depends on the machine: the paper quotes the measurement recorded in [`benchmark/results/overhead.json`](../benchmark/results/overhead.json).

Build with TeX Live 2023 or later:

```sh
make          # ballarch.pdf
make arxiv    # build/ballarch-arxiv.tar.gz, the source archive for arXiv
```

The source fixes the PDF's date and trailer ID, so a rebuild with the same TeX installation reproduces `ballarch.pdf` byte for byte.

[arxiv-metadata.txt](arxiv-metadata.txt) holds the plain-text title, abstract, comments and classes for the arXiv form. [PUBLISHING.md](PUBLISHING.md) lists what to check before submitting, the arXiv policies that apply, candidate venues, and where to announce the work. Nothing has been submitted yet.
