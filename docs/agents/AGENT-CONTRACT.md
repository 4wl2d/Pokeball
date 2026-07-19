# Pokeball Agent Contract

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Before using it, pass [BASELINE.md](BASELINE.md). Core prevails over this package.

Every `PKB-AR-*` rule is defined exactly once in this file. `A`, `P`, `R`, and `C` mean `always`, `path-triggered`, `risk-triggered`, and `claim-triggered` under Core §§0.2 and 20.1.

## Workflow

Before changing or reviewing Pokeball scope, the agent:

1. verifies the exact baseline and source precedence;
2. reads the affected authoritative source plus only the accepted policy reference, local delta, Assembly edges, and waiver record that exist;
3. derives applicability from the closed type/protocol/profile/route inventory plus reachable risks and explicit claims;
4. resolves each triggered guardrail to one construction proof, local declaration, or immutable in-scope policy reference plus an allowed delta;
5. runs base tests and only the path/risk tests that are reachable; and
6. creates a full evidence/gate record only for a release, conformance, or other explicit guarantee claim.

Metadata may add a trigger but cannot suppress one inferred from source or risk. In ordinary design, implementation, and adoption work, a proven-absent trigger has no mechanism, empty table, zero placeholder, explanation row, or proof artifact. Ambiguous absence is treated as present until an accepted decision closes it. When a conformance/release claim or an accepted ambiguity-resolution decision relies on absence, `PKB-AR-GOV-005` instead requires the exact static proof owned by Core §0.2.

Core §8.12 is a reference-only runtime concern index. It routes an agent to the sole normative anchors for cause/context, bounds, identity, admission, acceptance, dispatch, accepted-work preservation, observations, faults, recovery, reads, and lifecycle/fencing; it creates no additional component, field, mechanism, or trigger.

## Authority and precedence

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-GOV-001` | `A` | The agent confirms that Core and package come from one published immutable snapshot and that the exact integrity metadata matches before applying derived guidance or making a claim. | Core §0; package `BASELINE.md` |
| `PKB-AR-GOV-002` | `A` | Precedence is Core, accepted extension in scope, accepted exact project policy within choices Core leaves open, then derived guidance and examples. A `WaiverRecord` is not precedence, does not satisfy a guardrail, and cannot make a violated `MUST`/`MUST NOT` conforming. | Core §§0, 0.3, 21.6 |
| `PKB-AR-GOV-003` | `A/C` | Routine work resolves scope, authority, effective profile, triggers, and delta from authoritative sources; a concrete claim additionally records mechanism, assumptions, evidence, and non-guarantees. | Core §§0.2, 12–13, 20.1; PBA-39/41/42 |
| `PKB-AR-GOV-004` | `P/C` | The agent neither invents an extension nor attributes an excluded or unevidenced guarantee to Core. | Core §§0, 2.2, 21.6 |
| `PKB-AR-GOV-005` | `P/R/C` | When a Pokeball conformance/release claim or accepted ambiguity-resolution decision relies on absence of a non-`always` trigger, the agent records exactly one `TriggerAbsenceProof` for the trigger anchor, exact scope/effective profile, closed inventory evidence and its revisions/digests, evaluated `Absent` predicate, evidence owner, and invalidation conditions. An `always` obligation, present trigger or claim, unaccepted ambiguity interpretation, stale or mismatched evidence, unresolved reference, conflict, or listed invalidation makes the proof unusable until reevaluated. Ordinary design/adoption creates no proof placeholder; the evidence owner owns proof and invalidation review, and the proof is never a runtime registry, ambient default, waiver, hidden inheritance, or scope-broadening authority. | Core §§0.1–0.2, 17.5, 18.1, 20.1, 22; PBA-39 |

The exact closed evidence shape used by `PKB-AR-GOV-005` is:

```text
TriggerAbsenceProof {
    triggerClass:
        path-triggered
      | risk-triggered
      | claim-triggered
    triggerAnchor
    exactScopeAndEffectiveProfile
    inventoryEvidence:
        PathInventoryRef
      | RiskInventoryRef
      | DenialByConstructionRef
      | ClaimPublicationInventoryRef
    inventoryRevisionsOrDigests
    evaluatedPredicate
    conclusion: Absent
    evidenceOwner
    invalidationConditions
}
```

## Boundary, state, and protocol

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-BND-001` | `A` | A Ball boundary follows invariants, lifecycle, authority, and semantic scope; a materialized `StateKey` is required only when identity crosses the trigger boundaries named by Core. | Core §§1, 3.1, 4; PBA-01/11 |
| `PKB-AR-BND-002` | `A` | Interaction, Nucleus, and Resources are logically separated; roles may share a file or call stack, but the Nucleus receives no platform or I/O authority. | Core §5; PBA-01–03 |
| `PKB-AR-PRT-001` | `A` | Every used protocol category is closed and typed for one effective identity, and every public protocol preserves type ownership without re-exporting a foreign authority's contract. A route references but never defines or synthesizes a producer-owned type; independently versioned boundaries pin exact versions, while absent categories have no declaration. Every concrete profile/binding exposes one finite closed `AdmissionFailure.reason` union and rejects unknown or open-string reasons. | Core §§6, 10.7–10.8, 13.2, 14.1/14.4; PBA-04/25/28 |
| `PKB-AR-PRT-002` | `A` | The current mutating cause appears once as `Pulse`; field-minimized context contains only trusted observations that affect the Decision, with reserved IDs, actor/configuration/time/validity/version fields only under their own trigger, and raw external mutation becomes an `Intent` after validation. A trusted binding boundary verifies, bounds, and constructs `DecisionContext`, while the Ball/Nucleus owns its semantic schema, interpretation, and Policy Gate. | Core §§5.1–5.2, 6.11, 8.1, 11.1–11.3; PBA-03/05/33/44 |
| `PKB-AR-PRT-003` | `P` | Every cross-authority `ReadDependency` names the caller, one target authority, the target-owned `Query -> ResultPayload` effective protocol identity, target read/status authority, caller freshness/consistency requirement, and Assembly route/binding. Version, stamp, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry, and status fields appear only under their existing triggers. Boundary validation/admission can fail before `read`; once admitted, canonical non-mutating `read(...) -> ReadResult` returns only the declared success and creates no Decision, accepted-input marker, revision, `SemanticHandle`, or `SemanticOutput`. Caller/Assembly cannot redefine, re-export, synthesize, or alter target payload, status fact, stamp, or meaning; independent reads imply no atomic multi-source snapshot, and Query alone adds no new mandatory per-Ball read-limit field. | Core §§6.3, 8.3, 8.10, 9.11, 10.2/10.6–10.7, 14.1/14.4; PBA-25/30/38/39 |
| `PKB-AR-PRT-004` | `P` | For every declared command path, the target owns one exact versioned `ModuleCommand -> ModuleResult` mapping and static refusal classification. A trusted target boundary maps only a verified accepted source `ModuleCommandRequest` frame to `ModuleCommandPulse`; target `decide` is the sole acceptance point; only an accepted target Decision creates `ModuleResultOutput`; and a verified return route maps only that accepted target frame to `ModuleResultPulse`. Assembly/generated same-stack code selects and transports the route, effective identity, version pair, and verified tuples but cannot synthesize or alter causal tokens, target payloads, issuer provenance, or refusal meaning. Pre-acceptance carrier and accepted result cannot substitute or conflict, and reclassification requires a new target protocol and Assembly version pair. A read-like operation uses this accepted/revisioned command path only for accepted provenance, stable command identity, idempotent replay, status, or reconciliation; otherwise it uses `ReadDependency`/`Query`. | Core §§3.4, 6.8–6.13, 8.4–8.9, 9.1–9.13, 10.2/10.7/10.11, 14.1/14.4; PBA-08/18/19/25/30 |
| `PKB-AR-STA-001` | `A/R` | One mutable semantic fact has one authority, sequential acceptance has one writer, and no Ball directly reads another authority's mutable state; movable ownership additionally requires a storage-enforced fence. | Core §§7.2, 7.4, 7.6; PBA-11–13 |
| `PKB-AR-STA-002` | `A/P` | Every present state-like value has one explicit state kind; a value used by a later Decision is retained or trustworthily reintroduced with only the correlation, rollout identity, provenance, retention, and recovery data activated by that path. Hidden ledger/history reads are prohibited. | Core §§7.1, 7.3, 7.5, 8.1, 10.4; PBA-05/14/26 |

The command bridge in `PKB-AR-PRT-004` preserves these exact target-owned frames and delivery identity:

```text
accepted source ModuleCommandRequest
  -> verified ModuleCommandPulse(commandSource, effectiveProtocolIdentity, command, issuerProvenance)
  -> target decide / accepted target Decision
  -> ModuleResultOutput(semanticHandle = commandSource.semanticHandle,
                        target-frame sourceOrdinal, commandSource, payload)
  -> verified ModuleResultPulse(commandSource, resultSource,
                                effectiveProtocolIdentity, result, issuerProvenance)

result-delivery key = (effectiveProtocolIdentity, commandSource, resultSource)

CommandRejectedBeforeAcceptance {
    commandSource
    effectiveProtocolIdentity
    boundaryResponse:
        ValidationFailure
      | AdmissionFailure
      | DecisionRejected(BusinessRejection)
    targetBoundaryProvenance
}
```

`ModuleResultOutput.semanticHandle` is correlation only. The output always consumes target output-count and byte bounds and consumes one target delivery/status slot when retained, retried, or observable. Target `DispatchStopped` uses the result-delivery tuple and establishes no source business/status facet. A forged carrier creates no trusted source input. An accepted same-state result advances the snapshot revision, or uses accepted `NoDomainChange` in EventJournal; duplicate replay within the idempotency horizon returns the prior result without another Decision/revision.

## Identity and decision

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-ID-001` | `P` | Detached/addressable work has stable semantic identity and exact causal matching; when a Decision needs a new semantic ID, it receives a trusted validated/deterministic/reserved value rather than reading ambient randomness. Immediate call-scope output uses its accepted sequence position. | Core §§3.1–3.6, 9.1–9.2; PBA-15–17 |
| `PKB-AR-DEC-001` | `A/P` | Every Decision is pure, deterministic, and bounded, with `DecisionResult = Accepted(Decision) \| Rejected(BusinessRejection)`; when an external-action output exists, only the Nucleus creates its semantic intent. | Core §§3.3–3.4, 6.7, 8.2–8.4; PBA-03/04/06 |
| `PKB-AR-DEC-002` | `A/P` | Every accepted mutation is atomic; when capacity can fail, the complete candidate frame is preflighted or reserved and overload returns a pre-acceptance `BoundaryResponse(AdmissionFailure)`; when outputs exist, the complete ordered batch is retained with state and none is dispatched before acceptance. | Core §§6.13, 8.5–8.7, 13.2; PBA-07/08 |
| `PKB-AR-DEC-003` | `A/P/R` | Reentrant mutation is prohibited. Validation, admission, pre-acceptance Decision rejection, accepted target result, post-acceptance Resource failure/timeout/unknown, delivery stop, and programming fault keep their exact legal carrier/status stage: pre-acceptance faults expose nothing, and no later failure rolls back, downgrades, or rewrites an accepted fact. | Core §§6.13, 8.4, 8.8, 9.4–9.5; PBA-09/10/19/20 |

## Async and status

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-ASY-001` | `P` | A reachable `Fact`, verified `ModuleResultPulse` carrying its target-owned `ModuleResult` payload, or observation is provenance-bound to its accepted source and uses the complete causal identity required by its delivery shape. | Core §§6.4–6.11, 9.1–9.2; PBA-18 |
| `PKB-AR-ASY-002` | `P/R` | When ACK, business result, or possible execution without proof exists, those meanings remain independent and uncertainty is first class. | Core §§9.3–9.5; PBA-19/20 |
| `PKB-AR-ASY-003` | `P` | A duplicate-capable or retried operation preserves logical identity, declares mismatch and horizon semantics, and has one primary retry owner per failure mode. | Core §§9.6, 9.9; PBA-21/22/24 |
| `PKB-AR-ASY-004` | `P` | Reachable cancellation is a typed race, a timer has stable identity and an explicit late policy, and a deadline declares its semantic/resource kind plus trusted time source; only a detached deadline-crossing execution adds race identity. | Core §§9.7–9.10; PBA-23 |
| `PKB-AR-ASY-005` | `P` | A reachable delivery/status path changes Sovereign State only through declared trusted ingress and has one committed revisioned single-writer query authority. Its materializer applies causal sources in order or retains bounded pending evidence, merges duplicates idempotently, fails closed on non-equivalent same-key evidence, losslessly preserves every reachable lifecycle/acceptance/cancellation/result/unknown/stop facet, reserves finite operation/pending/facet/stop capacity before source acceptance without eviction or truncation, and creates a retention marker only after covered source positions and empty pending; absence follows the marker and late covered evidence cannot resurrect status. Physical form remains project/binding-owned and status authority acquires no underlying command/business-fact authority. | Core §§6.11, 8.3/8.8, 9.11/9.13, 12.5, 16.3/16.13; PBA-05/10/17/30/38/42 |

## Composition and profiles

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-CMP-001` | `P` | Every existing inter-Ball edge has one declared dependency kind, exact Assembly endpoints, and one effective protocol identity; each routed type resolves from its owning protocol rather than Assembly, and explicit versions appear only across independent versioning/deployment. | Core §§10.2, 10.7, 14.4; PBA-25 |
| `PKB-AR-CMP-002` | `P` | A stateful multi-authority workflow has one coordinator; a one-hop edge remains one hop, and every Flow or multi-operation dispatcher has a closed non-wildcard route graph. | Core §§10.3–10.5; PBA-26/27 |
| `PKB-AR-CMP-003` | `P` | Existing composition prohibits wildcard routing, resolves finite graph/fan-out/multi-hop causal-depth limits, and makes no unsupported multi-source atomicity claim. Compile-time imports and `Direct Control Dependency` edges—including generated inline dispatch before handoff/yield—are unconditionally acyclic; bounded asynchronous feedback after handoff is not a direct edge and retains its owner/identity/budget/escape/fan-out contract. | Core §§10.2, 10.6, 10.8–10.11; PBA-29/30 |
| `PKB-AR-CMP-004` | `P` | When a shared foundation exists or changes, it contains mechanical primitives only and owns no mutable business meaning, business-policy decision, domain authority, route selection, service locator, or hidden communication state. Logical-role/call-graph/provenance evidence must still distinguish Interaction, Nucleus, Resource/route, Assembly, runtime, status/read authority, and foundation when one file, stack, or generator representation-erases adapters; one exact project scan may cover all Balls. | Core §§5, 11.5, 14.6–14.7, 17.5; PBA-01–03/34/43 |
| `PKB-AR-PRF-001` | `A/P` | Each Ball has one statically resolved execution/state/isolation/security profile, selected directly or through an exact project/binding policy. For Inline, absent triggers add no mediator, reflection, serialization, queue, thread hop, or object hierarchy; build inspection proves zero mandatory runtime tax, while a benchmark is claim-triggered. | Core §§0.2, 12; PBA-39/40 |
| `PKB-AR-PRF-002` | `C` | A performance, durability, delivery, isolation, security, or conformance claim names its exact boundary, profile, mechanism, assumptions, evidence, and non-guarantees. | Core §§12.4–12.9, 13.4; PBA-41/42 |

## Security and limits

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-SEC-001` | `P` | Each raw input or resource-output trust/representation edge that exists performs bounded parsing, validation, and provenance quarantine before the Nucleus. | Core §§11.1–11.2; PBA-31 |
| `PKB-AR-SEC-002` | `A/P/R` | Ambient application authority is always prohibited. When actor context can change a Decision or Query/status-read authorization/result selection, a trusted binding boundary verifies, bounds, and constructs field-minimized context relative to a valid approved issuer or equivalent fixed trusted same-stack issuer/realm proof; forged, tampered, stale, wrong-issuer/realm, or missing evidence fails closed, while actor-independent paths create no actor artifact. The Ball/Nucleus owns context schema and semantic Policy-Gate interpretation. An external-Effect path uses the minimum explicit capability; a privileged path separately resolves Policy and immediate Execution Gates and adds a Grant only when proof crosses an authority. The Execution Gate verifies triggered proof/capability/constraint/version/freshness/revocation/endpoint/quota/sink binding without making a business decision, and a post-acceptance failure remains a Resource/result/status path rather than a pre-acceptance carrier. | Core §§5.1–5.2, 6.3, 8.1/8.10, 11.1–11.7; PBA-03/05/30/32–34/44 |
| `PKB-AR-SEC-003` | `P/R` | Interpreter, secret, or unsafe paths resolve the applicable safe-sink, containment, or owned unsafe-site contract. A waiver uses the exact eight-field `WaiverRecord`, never cancels the trigger or law, and records the exact conformance effect; absent paths create no registry row. | Core §§0.3, 11.6, 11.9–11.11; PBA-35–37 |
| `PKB-AR-LIM-001` | `A/P` | Every present variable dimension has one finite effective bound proven by construction, exact reusable policy, local declaration, or allowed delta; absent dimensions have no placeholder. | Core §8.3; PBA-38/39 |
| `PKB-AR-LIM-002` | `P` | Reachable delivery, concurrency, retention, response, fan-out, multi-hop causal-depth, and economic dimensions resolve their own finite caps, backpressure, and authority. | Core §§10.9, 13.1–13.2; PBA-29/38 |

## Manifest and evidence

| Rule ID | Class | Sole definition | Core source |
|---|---|---|---|
| `PKB-AR-MAN-001` | `A` | Authoritative typed source or a manifest resolves every used owned category and Query result once; a generated resolved view must not duplicate authority or invent absent rows. | Core §§14.1–14.3; PBA-04/39 |
| `PKB-AR-MAN-002` | `P` | An imported contract resolves through the target-owned identity and sole Assembly route; the caller neither copies nor re-exports it. | Core §§10.7–10.8, 14.4; PBA-25/28 |
| `PKB-AR-TST-001` | `A/P/R/C` | Routine work runs base tests plus reachable trigger and local-delta tests, reusing shared evidence once; a claim additionally runs and records the complete in-scope evidence matrix. | Core §§17.1–17.9, 18; PBA-39/41 |
| `PKB-AR-TST-002` | `P` | Catalog v2 and Checkout are illustrative fixtures used only for analogous activated properties; Catalog's six selection transitions, six total view mappings, versioned route and migration boundary provide no project default, value, or guarantee. | Core §§14.4, 15.1–15.10, 16.14, 20 |

## Fail-closed conditions

Stop the affected change or claim when an applicable policy reference is missing, mutable, stale, cyclic, conflicting, wrong-version/profile/binding/environment, or overridden outside its allowlist. A policy reference, `WaiverRecord`, or `TriggerAbsenceProof` cannot suppress a present trigger, weaken a law, or convert a `MUST`/`MUST NOT` violation into conformance; a direct-cycle waiver is deliberate non-conformance. Stop on carrier/result conflict, dynamic refusal reclassification, unknown/open-string admission reason, or a stage mapping that rewrites an accepted fact. Stop when a trigger is ambiguous and no accepted decision resolves it. Do not stop merely because an absent path has no document in ordinary work.
