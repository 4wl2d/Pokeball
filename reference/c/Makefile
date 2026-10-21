# Ballarch firmware reference: build and check from this directory with `make`.
CC ?= cc
CFLAGS ?= -std=c11 -O2 -Wall -Wextra -Wpedantic -Wshadow -Wstrict-prototypes -Werror
INC := -Iinclude -Iexamples/dosing
SRC := src/ballarch_runtime.c examples/dosing/dosing.c examples/dosing/dosing_ball.c test/test_dosing.c
HDR := include/ballarch.h examples/dosing/dosing.h
MUTANTS := BA_MUTANT_NO_WRITE_AHEAD BA_MUTANT_RESEND_AFTER_CRASH TH_MUTANT_RETRY_UNKNOWN

.PHONY: all test purity mutants clean
all: test purity mutants

build/test_dosing: $(SRC) $(HDR)
	@mkdir -p build
	$(CC) $(CFLAGS) $(INC) $(SRC) -o $@

test: build/test_dosing
	./build/test_dosing

purity:
	sh tools/check_purity.sh examples/dosing/dosing.c
	@if sh tools/check_purity.sh tools/fixtures/impure_decider.c; then echo "impure fixture not detected"; exit 1; else echo "impure fixture detected (expected)"; fi

# Each seeded bug must be caught by the exploration (the test must fail).
mutants: $(SRC) $(HDR)
	@mkdir -p build
	@for m in $(MUTANTS); do \
	  $(CC) $(CFLAGS) -D$$m $(INC) $(SRC) -o build/mutant_$$m || exit 1; \
	  if ./build/mutant_$$m > build/mutant_$$m.log; then echo "mutant $$m survived"; cat build/mutant_$$m.log; exit 1; \
	  else echo "mutant $$m killed: $$(tail -1 build/mutant_$$m.log)"; fi; \
	done

clean:
	rm -rf build
