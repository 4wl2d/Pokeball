# Ballarch RTL reference: lint, structural rule checks, simulation and seeded bugs.
# Requires Icarus Verilog (12), Verilator (5) and Yosys (0.33 or later).
IVERILOG ?= iverilog
VVP ?= vvp
VERILATOR ?= verilator
YOSYS ?= yosys
RTL := rtl/ballarch_issuer.sv
MUTANTS := MUTANT_RETRY_FIRE MUTANT_TIMEOUT_NOT_DONE MUTANT_COMB_DISPATCH
# R1: one driver per register; R2: no latch in the decider (yosys check); R5: no
# combinational path from an input to a dispatched output (outputs come from committed state).
RULES = proc; opt; flatten; check -assert; \
	select -set inputs i:*; select -set reach @inputs %co*:-\$$adff,\$$dff,\$$sdff,\$$dffe,\$$adffe; \
	select -assert-none @reach o:m_* o:cmp_* o:req_ready %u %u %i

.PHONY: all lint rules sim mutants clean
all: lint rules sim mutants

lint:
	$(VERILATOR) --lint-only -Wall $(RTL)

rules:
	@mkdir -p build
	$(YOSYS) -q -p "read_verilog -sv $(RTL); $(RULES)"
	@echo "R1/R2/R5 structural checks passed for $(RTL)"
	@for f in lint/*.sv; do \
	  if $(YOSYS) -q -p "read_verilog -sv $$f; proc; check -assert" > build/$$(basename $$f).log 2>&1; then echo "$$f not detected"; exit 1; \
	  else printf "%s detected: %s\n" "$$f" "$$(grep -m1 -oE 'conflicting drivers|Latch inferred' build/$$(basename $$f).log)"; fi; \
	done

sim:
	@mkdir -p build
	$(IVERILOG) -g2012 -o build/tb $(RTL) tb/tb_issuer.sv
	$(VVP) -n build/tb | tee build/sim.log | grep RESULT
	@grep -q 'RESULT: PASS' build/sim.log

mutants:
	@mkdir -p build
	@for m in $(MUTANTS); do \
	  $(IVERILOG) -g2012 -D$$m -o build/tb_$$m $(RTL) tb/tb_issuer.sv || exit 1; \
	  $(VVP) -n build/tb_$$m > build/$$m.log; \
	  if grep -q 'RESULT: PASS' build/$$m.log; then echo "mutant $$m survived"; exit 1; \
	  else echo "mutant $$m killed: $$(grep -m1 FAIL build/$$m.log)"; fi; \
	done
	@if $(YOSYS) -q -p "read_verilog -sv -DMUTANT_COMB_DISPATCH $(RTL); $(RULES)" > build/rules_mutant.log 2>&1; then \
	  echo "MUTANT_COMB_DISPATCH passed the structural R5 check"; exit 1; \
	else echo "mutant MUTANT_COMB_DISPATCH also rejected by the structural R5 check"; fi

clean:
	rm -rf build
