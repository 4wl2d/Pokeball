# Core part — Verification: boundary and architecture tests

[Core contents](../../pokeball-architecture-core.md) · [← Verification: transition and property tests](17-01-transition-and-property-tests.md) · [Verification: profile, security, and claim tests →](17-03-profile-security-and-claim-tests.md)

> Canonical part 16 of 24. The [Core entrypoint](../../pokeball-architecture-core.md) owns the version, status, complete file set, and reading order.

---

### 17.3. Resource contract tests

Every adapter that exists is tested separately. The cases below are selected by its reachable resource/risk paths; for example, an adapter with no retry, cancellation, secret, or ambiguous execution path receives none of those fixtures:

- positive capability fixture: a scoped credential, restricted client/broker/role, or capability-rooted OS handle enforces the exact bounded operation class at the real execution boundary;
- negative capability fixture: an administrator credential plus `if (allowed)` or a restricted-looking wrapper over unrestricted authority fails;
- each applicable safe-sink form—parameterized, structured, capability-rooted, and context-encoded—is exercised at its interpreter/dialect edge; a capability-rooted filesystem resolution succeeds while raw traversal fails;
- Execution-Gate verification immediately before authoritative execution covers every triggered proof, capability, constrained payload field, version, freshness/revocation, endpoint, quota, and safe-sink binding without a second business decision;
- an Execution-Gate refusal after accepted work maps to a bound typed `Fact`/target result or status and never to a pre-acceptance carrier;
- response schema validation;
- timeout/cancellation behavior;
- idempotency key propagation;
- retry ownership;
- retry-owner composition: each active failure mode has exactly one primary retry owner; every other retry layer is disabled or has a finite bound and proof of semantic transparency; cumulative SDK × adapter × runtime × Flow attempts are computed, and a `2 × 3 × 4` configuration with three primary-like owners fails;
- response-size and decompression bounds;
- mapping external errors into typed Facts;
- mapping every cancellation executor outcome into a closed `Fact`, target-owned `ModuleResult` carried by `ModuleResultPulse`, or declared trusted `ControlPulse` variant according to origin;
- `OutcomeUnknown` at ambiguous boundaries;
- secret redaction.

### 17.4. Interaction tests

For each present Interaction path, select the reachable cases below. Shared parser/authentication policy is tested once at its declared scope; the Ball tests its typed mapping and any delta:

- parser rejection;
- invalid boundary input returns `BoundaryResponse` but creates no Pulse, Decision, CommitRevision, SemanticHandle, or committed ReplyOutput;
- a malformed value or violation of the selected closed protocol/type invariant returns pre-Intent `ValidationFailure`; the same representation satisfying that type but exceeding a State- or semantic-Context-owned limit reaches `decide` and returns `DecisionRejected(BusinessRejection)`; for example, `items = 101` against a State-owned `maxItemsPerOrder = 100` follows the latter path;
- deliberately moving that fixed constraint into the closed input type requires a new protocol identity and moves the fixture to pre-Intent validation only for that version; changing the stage under the old identity fails compatibility evidence;
- normalization policy;
- duplicate/unknown field behavior where relevant;
- trusted binding construction of bounded, field-minimized `DecisionContext` and actor-dependent `ReadContext`, with approved-issuer or fixed trusted same-stack issuer/realm evidence only when the actor-context trigger exists; the Ball owns semantic schema/interpretation and an actor-independent Decision/read produces no actor artifact;
- Catalog search and product-selection IDs come only from validated `reservedSemanticIds`; selection receives one ID for its single routed Signal handle and no unused actor/configuration/time fields;
- Checkout uses one selected `CheckoutStartFingerprintV1` in Interaction and transition: the trusted initial Context carries its exact verified Interaction artifact version `IV`; after acceptance, same key + same fingerprint redelivers the exact original accepted `ReplyOutput(RequestAccepted(operationId))` frame and original `IV` with only a new `AttemptId`; root rejection before acceptance returns no operation mapping; same key + semantic/actor-scope mismatch under the covered record returns pre-Intent `BoundaryResponse(ValidationFailure(IdempotencyConflict))` with no semantic artifact, while key/RequestId/trace/reply changes do not themselves change the fingerprint;
- querying the candidate ID before root acceptance or after root pre-acceptance rejection can return only namespace/absence-proven `NotFound`; after acceptance the same ID selects the one known operation record, and retry never creates both a rejected-root record and an accepted operation;
- external mutation/cancellation after validation creates exactly one declared `Intent`, not a `ControlPulse`; Catalog then rejects `SearchCancelled(A)` before acceptance when current State/pending search belongs to B, while exact identity reaches the existing cancellation transition;
- verified command ingress constructs exactly one `ModuleCommandPulse` from an accepted source frame and the target-owned mapping; malformed payload or unverifiable source/provenance returns the statically classified pre-acceptance response and never creates an accepted target Decision;
- verified result egress constructs `ModuleResultPulse` only from an accepted target `ModuleResultOutput`; Assembly/generated code cannot create or alter either causal token or result payload;
- a successfully evaluated Query response—including any declared denial, redaction, or non-disclosing payload variant—matches the target-owned result mapping, uses `ReadResult`/`ConsistencyStamp` when the stamped-read trigger applies or call-scope identity otherwise, and creates no commit identity;
- operation status returns absent/expired/known cases within the declared closed result payload, not through transport 404, `BoundaryResponse`, or an undeclared second result type;
- output encoding/escaping;
- request and reply correlation;
- absence of raw framework objects inside Nucleus protocol.

### 17.5. Architecture tests

As a conformance and release-evidence projection of the marked source clause for `PBA-39`, CI verifies the applicable subset derived from the closed inventory. A line about Query, status, async, delivery, Flow, persistence, or unsafe authority is absent when that path is absent.

Core-document maintenance has a separate structural gate: it parses exactly one ordered-field source record for each `PBA-01–44`, exactly one marked source for each of the 94 glossary terms, resolves every source and primary §17 route, renders §§20, 20.1, and 22 deterministically, compares generated bytes, and proves a second generation is a no-op. Temporary-copy mutations delete/duplicate/reorder records, fields, and terms; stale a source, anchor, test, Agent route, or generated block; introduce a second source registry or manual definition; and replay the PBA-32/35/36/42, Nucleus-import, accepted-only root-status, root replay/conflict-stage, PBA-10 source-extent, composition-unit, output-byte, target-duplicate, stale-cancellation, `StillUnknown`, cumulative-fan-out, utility-ownership, Checkout Interaction-version, mutation-mode/frame, admitted-read-totality, validation-stage, Runtime/reader-route, and Catalog-domain-revision regressions. Every mutation must fail. This publication-maintenance evidence prevents projection drift but is neither an application runtime nor proof that a consuming project conforms to Core.

If the conformance/release verdict or an accepted ambiguity-resolution decision relies on absence, CI first checks the exact `TriggerAbsenceProof` shape and its bound inventory/digests. Fixtures cover present and absent predicates for `path-triggered`, `risk-triggered`, and `claim-triggered`; attempted use for `always`; contradiction by a present claim; accepted and unaccepted ambiguity resolution; wrong scope/profile/version/revision/digest; missing/stale/unresolved/conflicting evidence; every listed invalidation condition followed by reevaluation; and actor-dependent versus actor-independent Query paths. Ordinary builds with no absence-dependent verdict create no proof placeholder.

For every concrete binding, the evidence set includes a logical-role and boundary-edge map. Physical packages are optional; ownership, verification edges, and permitted calls are not:

| Role or edge | Owned meaning | Required evidence boundary |
|---|---|---|
| Interaction role | raw ingress/egress adaptation, normalization, validation, and context-safe encoding | adapter sites and permitted calls; no policy, State write, or direct Resource business call |
| Trusted Boundary edge | origin/authenticity/integrity/version/validity/bound verification and trusted semantic input/context construction | verifier and constructor sites before Ball interpretation; may be co-located with Interaction/route code but has no business authority |
| Nucleus / Policy Gate | State, protocol/context schemas, business interpretation, permission, ordered Decision/result | pure entrypoints and sole accepted business-decision sites; no I/O or ambient authority |
| Resource / route / Execution Gate | execution of accepted actions; triggered technical verification; validated Fact/result routing | capability/sink/check sites and causal provenance; no new business choice or direct state write |
| Assembly | route, protocol/version pair, binding, and generated wiring selection | static route map; no causal token, payload, refusal, context, or policy synthesis |
| Runtime / acceptor | admission, atomic acceptance, scheduling, delivery observation, and mechanical status transport | accepted-frame/state-write sites and typed ControlPulse routes; no domain mutation outside `decide` |
| Status/read authority | committed query snapshot, closed read mapping, namespace authorization/result selection | stamped/call-scope snapshot source and pure read entrypoint; no command authority or hidden I/O |
| Ball-local utility / shared foundation | local helper belongs to exactly one Ball role; shared code is mechanical only | ownership/import/state scan proving local-role containment or Foundation mechanics and rejecting ownerless shared domain/business meaning, policy, route selection, service locator, or hidden communication state |

The binding's call-graph proof traces the concrete or generated edges corresponding to:

```text
verified origin -> Trusted Boundary verifier/constructor -> Interaction adaptation or route ingress -> Nucleus decide/read
Nucleus Accepted Decision -> acceptor -> Resource/route
accepted EffectRequest -> Execution Gate -> validated Fact -> Nucleus decide
accepted ModuleCommandRequest -> target boundary -> target decide
accepted ModuleResultOutput -> verified result route -> source decide
committed status snapshot -> pure read authority -> typed ReadResult/payload
Assembly/runtime/foundation -X-> business policy or direct Sovereign State mutation
```

A same-file, same-stack, or generated layout may representation-erase adapters only when this role map, call graph, accepted-frame ownership, and provenance trace remain mechanically inspectable. Evidence traces every trusted cause, `DecisionContext`, actor-dependent `ReadContext`, `Fact`, `ModuleCommandPulse`, `ModuleResultPulse`, and delivery `ControlPulse` to its verified origin and rejects undeclared constructors, hidden service-locator/global-state inputs, direct runtime writes, Assembly-created meaning, or foundation-mediated business communication.

```text
no interaction -> resources import
no resources -> interaction import
no nucleus -> platform/I/O import
every concrete/generated implementation symbol maps to Interaction, Nucleus, Resource/route, Assembly, runtime/acceptor, status/read authority or shared-foundation role, with all permitted call edges explicit
Interaction is classified as a logical adapter role and every present Trusted Boundary as an authorized verification/construction edge; physical overlap neither merges the classifications nor transfers Nucleus policy, interpretation, or State authority
same-file and same-stack layouts preserve the role call graph and provenance trace; physical co-location is not accepted as separation evidence
no direct foreign state access
trusted binding boundary is the only constructor of non-empty DecisionContext and actor-dependent ReadContext; every field is verified, bounded and declared by the Ball-owned semantic schema
Inline deque/continuation preserves one trusted per-item Pulse-to-DecisionContext association; root context is not reused, and remaining causal budget/execution quantum/current capacity has no call edge into decide
Nucleus/Policy Gate is the only business-permission and business-result-selection authority; Resource/Execution Gate, Assembly, runtime and foundation cannot choose policy
every downstream output value is present in committed State, current Pulse or a declared field-minimized DecisionContext; no free value or decision read from inbox/outbox/runtime/participant history
every prior-Pulse value needed after a crash is retained with a bounded type and only the correlation, rollout identity, provenance and last-consumer retention required by its actual source/trust/lifetime triggers
every retained ingress fingerprint equals the atomically accepted idempotency-record fingerprint from one declared versioned function over explicit Pulse/Context operands; key and transport metadata are excluded
persisted state-shape change increments stateSchemaVersion without silently changing owned/imported protocolVersion
no feature internals import
no protocol re-export
caller Nucleus may import the exact declared target- or producer-owned Application Surface required by its closed Query/Pulse/Decision contracts without acquiring ownership
Checkout Nucleus imports exactly the Cart, Inventory, Payment, and Order participant-owned Application Surfaces and resolves all nine target-owned command/result mappings
caller-owned redeclaration or structurally identical mirror of an imported type fails
foreign mutable State, Nucleus internals, private Resource adapters, platform/I/O implementations, and protocol re-export fail
Interaction or Assembly synthesis of an imported payload, mapping, refusal, or business meaning fails
no raw external request -> ControlPulse path; every external mutation/cancellation enters through one declared Intent after Interaction validation
every post-commit runtime/route observation that changes Sovereign State resolves to one declared typed ControlPulse, exact previously committed source tuple and trusted provenance, then passes through single-writer decide; source commit and direct runtime state write cannot materialize Dispatched/ACK/ambiguity/DispatchStopped facets
closed protocol exhaustiveness
selected state profile resolves exactly one mutation function and frame: SnapshotDecisionResult + AcceptedSnapshotDecisionFrame or EventDecisionResult + AcceptedEventCommit; Event has no independent nextState and no binding implements a mandatory cross-profile union
representation or declared closed protocol/type invariant violation = pre-Intent ValidationFailure; valid typed value violating a State- or semantic-Context-owned rule = Nucleus BusinessRejection; promoting the rule into the type requires a new protocol identity
Pulse union order = Intent | Fact | ModuleCommandPulse | ModuleResultPulse | ObservedSignal | ControlPulse
SemanticOutput union order = ProjectionOutput | ReplyOutput | EffectRequest | ModuleCommandRequest | ModuleResultOutput | SignalPublication | TimerRequest
portable ModuleCommandPulse fields = commandSource + effectiveProtocolIdentity + command + issuerProvenance
portable ModuleResultOutput fields/invariant = semanticHandle equals commandSource.semanticHandle + target sourceOrdinal + commandSource + payload
portable ModuleResultPulse fields = commandSource + resultSource + effectiveProtocolIdentity + result + issuerProvenance
independent result-delivery key = (effectiveProtocolIdentity, commandSource, resultSource); an immediate typed return needs no delivery key
portable pre-acceptance carrier preserves commandSource + effectiveProtocolIdentity + closed BoundaryResponse + targetBoundaryProvenance; immediate local refusal uses the target contract type and call scope without a mandatory carrier shape
no bare ModuleResult is a Pulse variant; timer firing remains a declared ControlPulse and Catalog SignalPublication/ObservedSignal remain unchanged
every non-empty Ball-owned protocol category resolves each used variant exactly once through an inline declaration or one version-pinned authoritative reference; omitted categories are empty for FeatureBall and FlowBall alike
every routed Signal resolves exactly once in the producer-owned protocol; Assembly owns route/version/delivery binding and cannot define the producer payload
every owned Query resolves exactly one result payload for the same effective protocol identity; triggered stamped reads use canonical ReadResult/ConsistencyStamp, same-stack getters use call-scope identity, and neither creates a Decision, revision, SemanticHandle or SemanticOutput
every ReadDependency resolves exactly one caller + target authority + target-owned Query/result mapping + effective protocol identity + target read/status authority + caller freshness/consistency requirement + Assembly route/binding; caller/Assembly cannot redefine payload, stamp, status fact or read meaning
wrong ReadDependency version/authority/mapping/stamp fails before semantic read and is not NotFound; pre-read failure uses an existing BoundaryResponse, while admitted read returns only the declared ReadResult, whose target-owned payload exhausts every reachable permitted/denied/redacted/non-disclosing outcome selected by the pure Policy Gate, and creates no accepted marker/Decision/revision/handle/output
ReadDependency optional fields follow existing protocol-version, actor/authentication, cache/comparison, source-position, ordering, buffering, timeout, retry and status triggers; same-build/static erasure proves the same contract and Query alone creates no new per-Ball read-limit field
multiple ReadDependency results are independent target snapshots and do not imply one atomic multi-source snapshot without a separate mechanism
every operation-status result payload exhaustively distinguishes only its reachable lifecycle, acceptance, cancellation, ambiguity, delivery-stop and retention variants; any present absence/expiry result is derived from a stamped declared status-authority snapshot
every status namespace resolves one committed revisioned single-writer authority; physical representation is project/binding-owned and creates neither a second command authority nor an unsupported freshness/atomicity claim
Draining rejects new logical mutations but serves every available declared Query/status Query and already-accepted completion/cancellation/status input; unavailable read failure precedes read, admitted read returns only the successfully evaluated ReadResult with a total target-owned payload, and neither creates a Decision
co-located and separate status-authority layouts each have exactly one query writer and preserve underlying command/business-fact ownership; materialization lag is reflected only by the status stamp
every status source/observation applies in declared causal order or bounded pending; equivalent duplicate is idempotent, compatible facets merge losslessly, weaker evidence does not regress, and nonequivalent same-key evidence fails closed
every reachable operation/pending/lifecycle/cancellation/result/marker/stop capacity is finite and reserved before its source acceptance; N+1 prevents acceptance, while accepted evidence is never evicted, truncated, silently dropped or rolled back
every retention marker commits only after declared source positions/horizons cover the operation and pending is empty; marker precedes absence, covered late evidence cannot resurrect the operation, and marker-horizon expiry alone permits later NotFound
accepted target ModuleResultOutput remains a target status fact across crash-before-result-dispatch; target DispatchStopped preserves the exact result-delivery tuple while source status stays Pending/Unknown until verified result/reconciliation
Catalog protocolVersion = 2.0.0, stateSchemaVersion = 2, transitionArtifactVersion = 2.0.1, and the ProductSelectionConfirmed producer/consumer route pair = 2.0.0/2.0.0
CatalogState revision is a Ball-owned domain revision distinct from acceptor-owned CommitRevision unless the binding proves exact equality while preserving both ownership meanings
Catalog ProductSelected transitions are set-equal to {Idle, Searching, Ready, Failed, OutcomeUnknown, Cancelled}; each preserves its state-specific fields/facets and emits exactly one sourceOrdinal-0 ProductSelectionConfirmed SignalPublication
CatalogState-to-CatalogView mappings are set-equal to the same six states, one case each, with no fallback/default or multiple case; GetCatalogView and every search-state Projection use that mapping
Catalog v1 persisted state enters v2 decide only through authoritative upcast; missing rejection-reason or other required evidence routes to quarantine/manual remediation and never to an invented default
for Checkout, the stopped-handle universe includes the initial RequestAccepted ReplyOutput and all nine command Step handles; cap/order/reservation/materialization and 10/11 tests cover the same exact set
every imported ModuleCommand/ModuleResult resolves through exactly one target-owned command-to-result mapping and declared dependency whose effective protocol identity matches Assembly; refusal classification is static for that version, explicit producer/consumer versions are required only across independent versioning/deployment, and imported target types are not redeclared as caller-owned
every command Assembly binding preserves target ownership, source acceptance before invocation, target acceptance before accepted result, and serialized source completion; local target access and call scope suffice, while portable routes preserve their required verified tuples
every read-like operation requiring accepted provenance, stable command/step identity, idempotent replay, status, or reconciliation uses the command bridge; an ordinary non-recording read uses Query/ReadDependency and creates no target Decision/revision/output
local command success has one source-to-target Direct Control Dependency and no reverse return edge; complete finite execution needs no carried scope/depth or level reservations; regenerating work and queued/external/retained work require their actual bounds and no accepted loss
CheckoutCommandDeliveryObserved carrier aliases are set-equal to commandSource, effectiveProtocolIdentity, boundaryResponse, and targetBoundaryProvenance; all nine normal Checkout business refusals are accepted results
every example output preserves its canonical semantic role; typed local calls need no envelope fields invented only for representation equivalence
compile-time import and Direct Control Dependency graphs are independently and unconditionally acyclic; generated inline dispatch before async handoff/yield is included
a WaiverRecord on either direct cycle records deliberate non-conformance and cannot make the architecture test pass
async feedback after a bounded handoff/yield creates no direct-control edge and is tested separately for owner, identity, finite budget, escape condition and fan-out protection
every inter-Ball edge has ReadDependency, DeclaredCommandDependency, DeclaredSignalDependency or FlowParticipation
every physical helper import has one owner/classification: Ball-local to exactly one logical role or shared mechanical Foundation; ownerless shared domain/business utilities fail, and Ball-owned shared semantics use a declared Application Surface/protocol rather than a fifth dependency kind
every physical import remains in the acyclic compile-time graph; a utility edge enters Direct Control Dependency only through another Ball's Application Surface or synchronous cross-authority control
every FlowParticipation resolves exactly one Flow authority + participant authority + participant Application Surface + non-empty bounded Flow-owned coordination set + bounded references to existing read/command/signal dependencies; each reference preserves its existing owner, effective identity, route, return binding, limits, and Assembly binding
FlowParticipation creates no protocol variant, runtime envelope, fifth route, target-type copy, participant-State authority, or duplicate dependency; Checkout has exactly four Flow participants and nine existing command routes although six authorities/roles appear in the example
every Application Surface contains only the owning Ball's deliberate public semantic types/entrypoints and excludes mutable State, Nucleus internals, runtime/transport mechanics, private Resource adapters, and re-exported foreign contracts
every produced command/read/effect has a declared route or private capability binding
every boundary choice passes its §4.4 positive evidence and falsifier; Core accepts multiple decompositions only when each independently proves the same authority/invariant/lifecycle/trust/dependency rules
decision-relevant UI/transport value is committed State or explicit trusted current Pulse/DecisionContext; EphemeralState contains only non-decision mechanics
Flow exists exactly when it owns material lifecycle/ordering/branch-join/compensation-recovery-cancellation/reconciliation/terminal-outcome coordination; call count and one hop alone do not qualify
every present variable dimension resolves to one finite effective bound through static proof, an exact reusable policy, or a local declaration/delta; absent dimensions need no zero or N/A row
when maxCumulativeFanout is present, one root causal scope sums distinct accepted source-output-to-effective-route/consumer traversals across levels; terminal and converging route branches count, co-reachable branches sum, mutually exclusive alternatives share their maximum reservation, duplicate/redelivery does not increment, async handoff preserves scope, and exact N/N+1 rejects the whole over-limit Decision
every numeric maxTransitionSteps resolves one immutable versioned Decision Work Meter; equal canonical inputs under the same binding/transition artifact version/meter identity/version consume the same non-negative integral total, one decide scope is monotonic and cannot reset, exact N may complete, N+1 accepts no frame/state/revision/output/dispatch, and unlike meter identity/version, transition artifact version, or unit-definition tuples are not compared
every concrete fallible-admission profile/binding exposes one finite closed AdmissionFailure.reason union; unknown discriminator/open string fails before trusted construction, and a non-fallible profile has no empty union
every §6.13 error maps from its exact stage to only its legal carrier/result/status effect; later failure cannot rewrite validation/admission/pre-acceptance rejection/accepted result/post-acceptance Resource evidence/delivery stop/programming fault into another stage
candidate OperationId reservation followed by root validation/admission/Decision rejection creates only BoundaryResponse and no accepted operation, known status row, marker, handle, output, or reply; covered lookup may return only NotFound, while participant refusal remains an accepted source operation's step facet
root same-key/same-fingerprint retry redelivers the exact accepted ReplyOutput(RequestAccepted) source frame with only a new AttemptId; same-key/different-fingerprint returns pre-Intent ValidationFailure(IdempotencyConflict) and creates no semantic artifact
target duplicate before accepted result returns only verified ACK proof of the original target frame/pending result; after result it redelivers the exact accepted result frame, and neither path re-decides or re-executes
static dependencies remain visible in types and wiring; no mandatory maxDeclaredDependenciesPerBall or maxFlowParticipants ceiling
static routes remain visible in types and wiring; no mandatory maxRoutesPerFlow ceiling; optional project ceilings follow that project contract
maxInputBytes unit = one exact raw-or-normalized candidate-input representation with declared boundary-metadata/Context inclusion; maxStateBytes unit = complete candidate nextState semantic representation; maxOutputBytesPerDecision unit = complete ordered Decision.outputs semantic representation; one measure tuple fixes each N/N+1 and later storage/transport mechanics are excluded
Catalog stale SearchCancelled operationId mismatch accepts no Decision/revision/handle/projection/effect; protocolVersion 2.0.0 + stateSchemaVersion 2 + transitionArtifactVersion 2.0.1 remain distinct
Checkout StillUnknown on its sole status slot terminates normal v1 at NeedsManualReconciliation with outputs = [] and no implicit reopening
every policy reference is exact, acyclic, in scope, current for its selected revision, and conflict-free; wrong-version/profile/binding/environment and unauthorized overrides fail
policy references and WaiverRecords cannot suppress an inferred trigger, weaken a law, or convert a MUST/MUST NOT violation into conformance
every absence-dependent conformance/release verdict or accepted ambiguity-resolution decision has one exact TriggerAbsenceProof bound to class/anchor/scope/profile/inventory/digests/predicate/owner/invalidation; always and present triggers reject the proof, and invalidation blocks reliance until reevaluation
ordinary design/adoption and a verdict not relying on absence materialize no TriggerAbsenceProof or placeholder
adoption/pilot worksheet and selected workload/method/baseline/continue-reshape-stop thresholds are project-owned guidance only while that work exists; Core contributes no universal threshold, and negative-adoption fixtures create neither empty Balls nor conformance placeholders
the authoritative §0.1 Core-documentation rule is projected exactly: every new or changed Mermaid diagram in Core carries a local legend for semantic cause/output, committed acceptance, route binding, and non-authoritative dependency/wiring arrows; omission blocks that documentation change, not an unadopted consuming-project binding
when actor, tenant, issuer, realm, assurance, or delegation can change a Decision or Query/status-read authorization/result selection, PBA-44 resolves approved-issuer authenticity/integrity or fixed trusted same-stack issuer/realm proof even for typed inter-Ball input; actor-independent Decision/read paths resolve no actor artifact
Payment trace separates pre-decide carrier, accepted Nucleus business refusal, and post-acceptance Execution-Gate/Resource failure; the last two remain accepted target result paths and cannot be downgraded
Checkout initial Context explicitly supplies verified Interaction fingerprint artifact version IV, retained interactionArtifactVersion equals IV across retry/recovery, missing or mismatched IV fails, and transitionArtifactVersion remains distinct
every accepted action reaches its Resource/target Execution Gate immediately before execution with the triggered proof + capability + constraints + version + freshness/revocation + endpoint + quota + sink checks and no second business decision
no hidden cause/context/result constructor, direct runtime State write, Assembly-created semantic meaning, global service locator, mutable foundation communication, or route-selected foundation policy
the §14.2 minimal Inline fixture resolves only always-applicable obligations and contains no absent-path placeholders
positive and negative fixtures for path-, risk-, and claim-triggered rules respectively activate the guardrail and reject the same reachable trigger when no effective guardrail/evidence resolves
two Balls can resolve one exact shared policy without copying it; a local delta changes only an explicitly overridable field and leaves all other effective values equal
unsafe effect registry
foundation domain-type quarantine
```

An import linter result does not prove semantic ownership or security isolation. It proves only the verifiable structural property.
