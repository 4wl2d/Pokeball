# Build the paper and the arXiv source archive. Requires TeX Live 2023 or later with latexmk.
.PHONY: pdf arxiv clean
pdf:
	latexmk -pdf -interaction=nonstopmode -halt-on-error ballarch.tex

# arXiv accepts .bib since November 2025; the .bbl is included as well so that
# the build does not depend on arXiv's BibTeX version.
arxiv: pdf
	mkdir -p build
	tar -czf build/ballarch-arxiv.tar.gz ballarch.tex references.bib ballarch.bbl
	@echo "wrote build/ballarch-arxiv.tar.gz"

clean:
	latexmk -C ballarch.tex
	rm -rf build
