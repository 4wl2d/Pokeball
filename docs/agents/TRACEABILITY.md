# Agent Rule Traceability

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Sole rule definitions are in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This matrix routes each rule to Core, applicability, its authoritative source, and validation. `A/P/R/C` are the four classes in Core §20.1.

| AgentRuleId | Core / law | Class and trigger | Authoritative source | Enforcement or evidence | Gate |
|---|---|---|---|---|---|
| `PKB-AR-GOV-001` | §0 | `A`: package used or claim made | `BASELINE` | exact hash/bytes/version/digest | `AP-GATE-02`, `RG-10` |
| `PKB-AR-GOV-002` | §§0, 21.6 | `A`: every decision | Core, extension, accepted project policy/deviation | precedence check | `AP-GATE-03`, `RG-01` |
| `PKB-AR-GOV-003` | §§0.2, 12–13, 20.1; PBA-39/41/42 | `A`: resolve applicability; `C`: concrete claim | Ball source/policy; claim record for `C` | resolver; scoped claim evidence | `RG-01`, `RG-08`, `RG-10` |
| `PKB-AR-GOV-004` | §§0, 2.2, 21.6 | `P`: extension need; `C`: excluded guarantee claim | accepted extension or claim/non-claim record | scope check | `RG-01`, `RG-08` |
| `PKB-AR-BND-001` | §§1, 3.1, 4; PBA-01/11 | `A`: every Ball | typed scope/authority source | ownership and identity-boundary tests | `RG-01`, `RG-03` |
| `PKB-AR-BND-002` | §5; PBA-01–03 | `A`: every Ball | source/type graph | import/no-I/O checks | `RG-01`, `RG-04` |
| `PKB-AR-PRT-001` | §§6, 10.8, 14.1; PBA-04/28 | `A`: every used/public protocol category | owned closed types or one manifest/reference | exhaustiveness, ownership, no-re-export | `RG-02`, `RG-06`, `AP-GATE-08` |
| `PKB-AR-PRT-002` | §§6.11, 8.1; PBA-05 | `A`: every Decision | Pulse/sparse Context types | origin/context tests | `RG-02`, `RG-04` |
| `PKB-AR-PRT-003` | §§6.3, 8.10, 9.11; PBA-30 | `P`: Query; stamped wrapper/status clauses by subtrigger | read types/source; status authority if present | purity/result/stamp tests | `RG-02`, `RG-03`, `RG-05` |
| `PKB-AR-STA-001` | §§7.2, 7.4, 7.6; PBA-11–13 | `A`: sequential acceptance/authority; `R`: owner can move | state/type/access graph and binding | writer/authority/access; storage fence only if movable | `RG-03` |
| `PKB-AR-STA-002` | §§7.1, 7.3, 7.5, 8.1, 10.4; PBA-05/14/26 | `A`: every present state-like value; `P`: value crosses Decisions | state types and transition source | state-kind mapping; triggered lineage/retention/recovery | `RG-03`, `RG-04`, `RG-05`, `RG-08` |
| `PKB-AR-ID-001` | §§3.1–3.6, 9.1–9.2; PBA-15–17 | `P`: detached/addressable work, late/reordered result, or new/trust-validated semantic ID | state/protocol identity and trusted issuer source | causal/stale tests; allocation/validation/collision only when new ID exists | `RG-04`, `RG-05` |
| `PKB-AR-DEC-001` | §§3.3–3.4, 6.7, 8.2–8.4; PBA-03/04/06 | `A`: every Decision; `P`: external-action output exists | transition and output types/source | purity/determinism/bounds; output authority only when present | `RG-02`, `RG-04`, `RG-09` |
| `PKB-AR-DEC-002` | §§8.5–8.7; PBA-07/08 | `A`: accepted mutation; `P`: output exists | acceptance binding and output source | publication/dispatch fault tests | `RG-04` |
| `PKB-AR-DEC-003` | §§8.4, 8.8; PBA-09/10 | `A`: every mutating transition; `P/R`: delivery/ambiguity | execution/failure binding | reentrancy and reachable fault tests | `RG-04`, `RG-08` |
| `PKB-AR-ASY-001` | §§6.4–6.11, 9.1–9.2; PBA-18 | `P`: result/observation path | source/result protocol and verifier policy | provenance/causal tests | `RG-05` |
| `PKB-AR-ASY-002` | §§9.3–9.5; PBA-19/20 | `P`: separate ACK; `R`: possible execution without proof | operation/failure policy | ACK/result/unknown traces | `RG-05` |
| `PKB-AR-ASY-003` | §§9.6, 9.9; PBA-21/22/24 | `P`: duplicate or retry possible | operation/route retry policy | key/owner/finite budget; mismatch/horizon only for duplicate/idempotency path | `RG-05`, `RG-09` |
| `PKB-AR-ASY-004` | §§9.7–9.10; PBA-23 | `P`: cancellation, deadline, or timer path | operation protocol/trusted time source | cancellation races; timer identity/late policy; detached deadline race only when reachable | `RG-05` |
| `PKB-AR-ASY-005` | §§6.11, 8.8, 9.11, 12.5, 16.3/16.13; PBA-05/10/30/42 | `P`: delivery observation or status | typed ingress/status authority | only reachable lifecycle/stop/retention/stamp facets | `RG-03`, `RG-05`, `RG-08` |
| `PKB-AR-CMP-001` | §§10.2, 10.7; PBA-25 | `P`: inter-Ball edge | Assembly and target contract | route identity and triggered-version tests | `RG-06` |
| `PKB-AR-CMP-002` | §§10.3–10.5; PBA-26/27 | `P`: coordination lifecycle/outcome or multi-operation dispatcher | Flow/dispatcher protocol and Assembly | coordinator when stateful; compensation tests only if present; closed routes | `RG-03`, `RG-05`, `RG-06` |
| `PKB-AR-CMP-003` | §§10.6, 10.9–10.11; PBA-29/30 | `P`: composition/read aggregation/multi-hop causality | Assembly plus static/local ceilings or optional project policy/read contract | graph/fan-out/causal-depth/consistency tests | `RG-06`, `RG-09` |
| `PKB-AR-CMP-004` | §14.7; PBA-43 | `P`: shared foundation exists | project-scoped foundation source or exact policy | export/dependency/authority scan | `RG-06`, `RG-10` |
| `PKB-AR-PRF-001` | §§0.2, 12; PBA-39/40 | `A`: resolve every binding/Ball profile; `P`: Inline implementation selected | direct construction/declaration or optional exact project/binding policy plus delta | static resolver; Inline build-tax inspection, benchmark only for claim | `RG-08` |
| `PKB-AR-PRF-002` | §§12.4–12.9, 13.4; PBA-41/42 | `C`: guarantee or conformance claim | claim record | scoped mechanism/assumption/evidence/non-guarantee | `RG-08`, `AP-GATE-09` |
| `PKB-AR-SEC-001` | §§11.1–11.2; PBA-31 | `P`: raw trust/representation edge | Interaction/Resource binding source or optional policy | parser/provenance tests | `RG-07` |
| `PKB-AR-SEC-002` | §§11.3–11.5, 11.7; PBA-32–34 | `A`: no ambient authority; `P`: external Effect; `R`: privilege, with Grant only for cross-authority proof | type/dependency graph, capability/action source, triggered grant | graph scan; restricted client; applicable gate/grant proof | `RG-04`, `RG-07`, `RG-08` |
| `PKB-AR-SEC-003` | §§11.6, 11.9–11.11; PBA-35–37 | `P`: interpreter; `R`: secret/unsafe | sink/secret policy or unsafe record | injection/leakage/expiry tests | `RG-07`, `AP-GATE-09` |
| `PKB-AR-LIM-001` | §8.3; PBA-38/39 | `A`: present base dimensions; `P`: optional dimensions | bounded type, exact policy, or local declaration/delta | resolver and overflow tests | `RG-09` |
| `PKB-AR-LIM-002` | §§10.9, 13.1–13.2; PBA-29/38 | `P`: delivery/concurrency/retention/response/fan-out/multi-hop causality/quota | owning declaration or policy/Assembly/runtime | cap/backpressure/causal-depth/quota tests | `RG-09`, `AP-GATE-08` |
| `PKB-AR-MAN-001` | §§14.1–14.3; PBA-04/39 | `A`: authoritative inventory/resolved view | typed source or one manifest; exact policy ref only when reused | source/view equality and trigger resolution | `RG-02`, `AP-GATE-07`, `AP-GATE-08` |
| `PKB-AR-MAN-002` | §§10.7–10.8, 14.4; PBA-25/28 | `P`: imported contract | target source and Assembly | identity/triggered-version/ownership equality | `RG-06`, `AP-GATE-08` |
| `PKB-AR-TST-001` | §§17.1–17.9, 18; PBA-39/41 | `A`: base; `P/R`: triggered tests; `C`: evidence matrix | test source; claim record for `C` | base + trigger + delta + claim suites | `RG-01–10`, `AP-GATE-09` |
| `PKB-AR-TST-002` | §§15.10, 16.14, 20 | `P`: example used as evidence | canonical example and local analogous source | analogy/trigger crosswalk | `RG-10`, `AP-GATE-03` |

## Matrix invariants

- The contract and this matrix contain the same 35-rule set.
- Every rule has one Core source and explicit class/trigger.
- Shared policy/mechanism/evidence may close many covered Balls once at its exact digest and scope.
- A Ball records only its authoritative semantics plus any local declaration, different explicit selection, or allowed delta; the enclosing accepted scope owns a shared policy reference once.
- A proven-absent trigger requires no artifact; a conformance review records the closed absence proof once.
- A change to Core source, rule meaning, trigger, policy schema, or gate requires synchronized package validation.
