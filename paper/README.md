# Paper

*Ballarch: Boundaries in Time and Honest Outcomes, from Register-Transfer Logic to Services* ([PDF](ballarch.pdf), [LaTeX source](ballarch.tex), [references](references.bib)).

The paper presents Ballarch Core 2.0:
- the boundaries in time;
- the outcome classification;
- the knowledge algebra and its closure theorems;
- the rules checked at three levels;
- the Alloy model;
- the benchmark.

Every number in it comes from a check in this repository, except the earlier prototype measurements named under Limitations, whose data is not published; the [verification guide](../docs/reproducibility.md) shows how to reproduce the rest.

Build with TeX Live 2023 or later:

```sh
make          # ballarch.pdf
make arxiv    # build/ballarch-arxiv.tar.gz, the source archive for arXiv
```

[PUBLISHING.md](PUBLISHING.md) lists what to check before submitting, the arXiv policies that apply, candidate venues, and where to announce the work. Nothing has been submitted yet.
