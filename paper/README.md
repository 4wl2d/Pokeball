# Paper

*Ballarch: Boundaries in Time and Honest Outcomes, from Register-Transfer Logic to Services* ([PDF](ballarch.pdf), [LaTeX source](ballarch.tex), [references](references.bib)).

The paper presents Ballarch Core 2.0:
- the boundaries in time;
- the outcome classification;
- the knowledge algebra, its soundness and exactness propositions and the composition-closure corollary, with explorations of a composite (the trip) and of a composite of composites (the itinerary);
- the rules, with the mechanisms that check them at each of three levels, including the knowledge algebra in firmware and hardware;
- the language-neutral conformance trace and the reference runtime's checks for code changes under open obligations;
- the Alloy model;
- the benchmark.

Every number in it comes from a check or a source file in this repository, except the earlier prototype measurements named under Limitations, whose data is not published; the [verification guide](../docs/reproducibility.md) shows how to reproduce the rest.

Build with TeX Live 2023 or later:

```sh
make          # ballarch.pdf
make arxiv    # build/ballarch-arxiv.tar.gz, the source archive for arXiv
```

[arxiv-metadata.txt](arxiv-metadata.txt) holds the plain-text title, abstract, comments and classes for the arXiv form. [PUBLISHING.md](PUBLISHING.md) lists what to check before submitting, the arXiv policies that apply, candidate venues, and where to announce the work. Nothing has been submitted yet.
