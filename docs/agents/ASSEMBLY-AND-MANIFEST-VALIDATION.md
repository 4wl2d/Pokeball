# Assembly and Manifest Validation

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open this continuation when an imported Application Surface, Assembly route, resolved-view validation, or manifest change discipline is in scope.

Every `PKB-AR-*` rule is defined only in [AGENT-CONTRACT.md](AGENT-CONTRACT.md). This document is task guidance projected from the named Core source clauses.

[← Manifest and Assembly Runbook](MANIFEST-AND-ASSEMBLY.md) · [Agent Pack index](README.md)

## 4. Imported edge and Assembly

For each edge that exists, the target/producer owns its Application Surface, the caller Nucleus owns only its use of that imported surface in a closed contract, and Assembly owns:

```text
route identity and dependency kind
producer/source and exact protocol identity
consumer/target and exact protocol identity
delivery point and semantics
identity/deduplication/ordering policy when triggered
retry failure mode, exact primary owner, secondary disabled-or-finite-transparent proof, cumulative cap, and unknown/status policy when triggered
queue/backpressure/retention and security binding when triggered
effective route/fan-out bounds
```

For `maxCumulativeFanout`, the resolved view additionally identifies the one causal scope, the complete accepted source-tuple/effective-route/consumer branch identity, mutually exclusive reservation rule, duplicate/redelivery treatment, async-handoff continuation, independent-root boundary, and static-proof or existing-admission enforcement owner. It does not replace the separate per-Decision, per-Signal, or causal-depth bounds.

For a command edge, the resolved view additionally proves:

```text
target-owned ModuleCommand -> ModuleResult mapping
static refusal classification and target protocol version
accepted source ModuleCommandRequest -> verified ModuleCommandPulse ingress
accepted target ModuleResultOutput -> verified ModuleResultPulse return
effective protocol/version pair
commandSource/resultSource/issuer provenance transport without synthesis
target output-count and the `DecisionOutputs` dimension of its resolved `BoundedByteMeasure` plus conditional result-delivery/status slot
same-stack source level-1 alternative-completion reservation
target level-2 result reservation on the accepted branch
atomic level-1 transfer to source carrier Decision on pre-acceptance branch
```

The result-delivery key is the unnamed tuple `(effectiveProtocolIdentity, commandSource, resultSource)`. A same-stack representation may erase envelopes only while proving identical accepted tuples. The source reserves level 1 before acceptance; target acceptance consumes it only after reserving level 2, while verified target validation/admission/`decide` rejection consumes no target level/frame/revision/output and transfers level 1 to the source `decide(ControlPulse)` carrier path. Missing level 1 prevents source acceptance/dispatch; missing level 2 prevents target acceptance and returns `AdmissionFailure(CausalBudgetExceeded)` through the carrier. Further synchronous outputs from carrier handling reserve from the remaining total budget. Reclassification requires a new target protocol version and Assembly pair. A result return is not a reverse dependency edge; async transport preserves the causal scope/depth/budget but does not infer the same-stack transfer rule.

Within the command idempotency horizon, the binding returns only verified ACK proof of the existing accepted target frame while its result is pending; it does not wait, re-invoke `decide`, revise, fabricate a result, or repeat Resource execution. Once a result frame exists, it redelivers that exact accepted frame with unchanged effective identity, `commandSource`, and `resultSource`, changing only mechanical attempt identity. Same identity with a different command fingerprint or conflicting evidence fails closed.

For a read edge, pre-read validation/admission may return only an already declared `BoundaryResponse`. Once admitted, target `read(...) -> ReadResult` returns only the successfully evaluated target-owned result, whose payload closes every reachable post-admission permitted, denied, redacted, or deliberately non-disclosing outcome, and creates no accepted marker, Decision, revision, handle, or output. Wrong target/version/authority/mapping/stamp fails at the boundary and is not converted into semantic `NotFound`; boundary, caller, Assembly, runtime, and generated code cannot add `BusinessRejection`, exception, post-admission `BoundaryResponse`, undeclared `NotFound`, or another payload. Multiple read dependencies do not imply an atomic multi-source snapshot. Query alone adds no new canonical per-Ball read-limit field.

A local route may compile to a direct call. A process boundary may use IPC. Neither changes domain semantics, and neither requires a universal mediator.

Classify the binding separately for graph review. A compile-time import or synchronous cross-Ball invocation before asynchronous handoff/yield—including generated inline dispatch—is a `Direct Control Dependency`. Compile-time-import and direct-control graphs are independently and unconditionally acyclic. Bounded asynchronous feedback begins after handoff/yield, creates no direct-control edge, and retains its owner/identity/budget/escape/fan-out contract.

## 5. Static validation

For routine work, validate the affected source/delta/edge:

- used owned variants and every target-owned `Query -> ResultPayload` mapping resolve exactly once; each payload is total over its reachable post-admission semantic outcomes, an intentional non-disclosure meaning is explicit and tested, and no non-owner invents a denial/redaction/absence carrier;
- every physical helper import resolves to one Ball/logical-role-local implementation or shared mechanical Foundation and remains in the acyclic compile-time graph; reject a fifth semantic dependency row, ownerless shared domain/business utility, and relabelling domain semantics as Foundation; Ball-owned shared semantics use a declared Application Surface/protocol;
- every `ReadDependency` resolves its complete target-owned identity/authority/requirement/binding contract, triggered-only fields, and successfully evaluated total-result codomain; target-declared permitted/denied/redacted/non-disclosing variants pass, while wrong version/authority/mapping/stamp, undeclared `NotFound`, `BusinessRejection`, post-admission `BoundaryResponse`, exception, payload invention, and command/read substitution fail;
- representation and declared closed-protocol/type invariants independent of committed State, semantic Context, and business policy fail at Interaction before semantic input; valid typed State/Context/business rules remain Nucleus-owned; promoting a fixed constraint into the type invariant changes the effective versioned protocol and is never a dynamic binding-stage choice;
- imported command/result types resolve to one target-owned mapping and static refusal classification; verified ingress/accepted-frame return preserve exact tuples and target bounds; duplicate-before-result returns verified ACK only, duplicate-after-result replays the exact accepted frame, and command-fingerprint/evidence conflict fails closed; explicit versions agree when independent versioning/deployment triggers them;
- every imported `Application Surface` is exact, declared, target/producer-owned, and required by a closed caller Nucleus contract; import transfers no ownership, exposes no foreign State/internal/private adapter, and creates no caller mirror/redeclaration, protocol re-export, or Interaction/Assembly-synthesized semantics; every Flow/participant pair has one resolved `FlowParticipation` with a non-empty bounded coordination set and only bounded refs to existing dependencies;
- same-stack command resolution proves source level-1 reservation, accepted-target level-2 reservation, target-pre-acceptance no-frame behavior, atomic level-1 carrier transfer, carrier-output reservation, and async budget preservation;
- inferred applicability cannot be disabled by missing metadata;
- every triggered guardrail has one construction proof, local declaration, or exact policy reference plus allowed delta;
- every present variable dimension has a finite effective bound;
- each numeric input/State/output byte limit is either closed by a static type-and-representation proof or resolves one exact dimension-specific `BoundedByteMeasure`: input fixes the raw-boundary or normalized-trusted stage and complete metadata/Context inclusion before `decide`; State covers the complete candidate `nextState` semantic representation before State/revision/output acceptance; DecisionOutputs covers the complete ordered output sequence and required envelope semantics while excluding later mechanics; every dimension proves exact alternate-representation mapping, erasure invariance, tuple incomparability, and exact `N/N+1` behavior without truncation or partial acceptance;
- each present `maxDeclaredDependenciesPerBall` counts distinct resolved read/command/signal/FlowParticipation declarations once with referenced rows retained, multiple operations distinct, duplicate/equivalent-alias declarations rejected, and static exact-`N`/reject-contract-at-`N+1`; each `maxRoutesPerFlow` counts only distinct effective Assembly command/result round-trip mappings with command ingress/result return unified, read/signal/participation/reference units at zero, exact duplicate/spelling-alias/equivalent-row/split-leg aliases rejected, and static exact-`N`/reject-Flow-Assembly-at-`N+1`;
- each present `maxCumulativeFanout` resolves one causal scope, exact accepted output/route/consumer branch identity, all-level tree/diamond/terminal/converging and co-reachable aggregation, mutually exclusive maximum reservation, duplicate/redelivery and new-tuple behavior, handoff/independent-root behavior, and static proof or exact `N/N+1` admission with no partial Decision;
- each triggered `DecisionWorkMeter` has one complete immutable meter identity/version, transition artifact version, unit/cap/overflow contract, deterministic monotonic per-`decide` counting, `N/N+1` behavior, and no illicit reset or cross-binding comparison;
- output paths have acceptance-before-dispatch and route/capability binding;
- a concrete fallible-admission profile/binding exposes one finite closed `AdmissionFailure.reason` union and rejects unknown/open strings; a non-fallible profile has no empty union;
- state-shape persistence changes have authoritative migration or quarantine, with no invented defaults for newly required semantic fields;
- every routed type resolves once from its owner and independently versioned endpoint pairs match Assembly; command reclassification changes the target/Assembly version pair; and
- compile-time/direct-control graphs, async feedback, fan-out, foundation, and unsafe checks run only when those paths exist, while policy and waiver records never weaken their result.

For a claim, generate the complete resolved view with exact named boundary and scope, mechanism, assumptions, retention, evidence, and explicit non-guarantees; do not infer a stronger downstream guarantee from source durability or retained pending work. Record `TriggerAbsenceProof` only for absence on which the verdict relies, and run the `RG-*` gates. Manifest text alone never proves enforcement.

## 6. Change discipline

Change only the authority that owns the fact. A protocol meaning, ownership, route, effective policy, allowed delta, persistent schema, or guarantee change is semantic. Shared-policy updates create a new immutable revision/digest and an explicit affected-reference set; they never mutate an existing reference silently.
