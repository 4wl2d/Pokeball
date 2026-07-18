# Async and Status Runbook

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Open only the sections activated by reachable detached work, late or duplicate result, separate ACK, ambiguous execution, retry, cancellation/deadline/timer, delivery observation, or operation status. A synchronous call-scope completion does not acquire the whole async model.

## 1. Detached causal identity

When work can detach, retain, retry, reorder, cancel, recover, cross a Ball boundary, or enter status, resolve:

```text
logical OperationId when an independent operation lifecycle exists
SemanticHandle for the addressable work
accepted source ordinal or equivalent sequence identity
source Ball/revision when that identity crosses call scope or is observed
OutputId and AttemptId only after materialization
result issuer and accepted-operation proof only across the corresponding trust/acceptance boundary
generation and stale policy only for late/reorderable generations
duplicate identity and retention only for duplicate/redelivery risk
causal depth/budget only when the work can cause a later Decision
```

A retry changes attempt identity, not the logical operation or handle. A same-stack synchronous completion may use accepted call-scope position and needs no fabricated operation/transport lineage.

For a value used by a later Decision, retain its field-minimized value and add metadata only by subtrigger: source correlation when matching crosses call scope; version when rollout/persistence is observed; provenance across a trust boundary; consumer/deletion data when lifetime is not statically fixed. Inbox, outbox, participant ledger, and ambient history are not Decision inputs.

## 2. Operation facets

Materialize only facets that have reachable observations:

| Facet | Meanings when triggered |
|---|---|
| Dispatch | not sent, sent/might be sent, finite policy stopped |
| Acceptance | not accepted, accepted, rejected before acceptance, unknown |
| Business outcome | pending, succeeded, rejected, failed, cancelled, unknown |
| Cancellation | requested and accepted-before-start/in-progress/too-late/rejected/unknown races |

Do not flatten independent meanings. A source commit does not imply dispatch; a result with accepted-operation proof implies acceptance; delivery stop proves neither failure nor non-execution; cancellation does not erase a legitimate result.

## 3. Canonical ingress

| Observation that exists | Input category |
|---|---|
| External start/cancel request | validated `Intent` |
| Private resource outcome | provenance-bound `Fact` |
| Target Ball outcome | provenance-bound `ModuleResult` |
| Committed publication | `ObservedSignal` over one declared route |
| Runtime/route dispatch or ACK observation affecting state | typed trusted `ControlPulse` |

Runtime never writes operation facets directly into Sovereign State. Duplicate identity, source tuple, bounded/redacted evidence, and conflict policy are required only for observation paths that exist.

## 4. ACK, ambiguity, and retry

When ACK is separate, test the reachable orderings among source acceptance, dispatch, ACK, and result. A result may prove acceptance before an ACK. A timeout creates `AcceptanceUnknown` or `OutcomeUnknown` only after the declared possible-send/execution boundary; otherwise it is an ordinary attempt failure.

For each active retry layer, record one row in the authoritative route or operation policy:

| Failure mode | Primary owner | Stable logical key | Finite attempt/time budget | Idempotency horizon, if required | Other active layers |
|---|---|---|---|---|---|

Check cumulative SDK × adapter × runtime × Flow attempts. A blind retry after possible irreversible execution requires preserved identity and a target/provider idempotency horizon; otherwise use status, reconciliation, or manual policy.

## 5. Cancellation, deadline, and timer

Cancellation is a new typed operation with its own handle when it can race execution. Its executor or target returns only the accepted-before-start, accepted-in-progress, too-late, rejected, or unknown outcomes that can actually occur. A timer path has stable timer identity and an explicit late policy; generation appears only for cancellation/rescheduling/replacement, while firing identity and deduplication appear only for duplicate/redelivery risk.

Test only reachable races, but include both orders. For the canonical Catalog semantics:

- accepted-in-progress before/after a legitimate result converges without lifecycle regression;
- terminal cancellation itself proves acceptance, and a later weaker accepted-in-progress observation is a no-op;
- terminal result versus mutually exclusive terminal cancellation preserves the first accepted frame, while the contradictory second proof fails before acceptance with no state/output; and
- the conflict rule is symmetric in both arrival orders.

## 6. Operation status

Add a status authority only when the Core §9.11 trigger exists. Authenticate it only when status access crosses a trust/access-control boundary. Record the base authority and then only triggered facets in authoritative source or an exact reusable policy:

```text
authority identity and operation key
revision/writer and ConsistencyStamp construction
reachable lifecycle sources

optional by subtrigger:
  authenticated namespace/access policy
  acceptance, workflow, cancellation, ambiguity, or delivery source
  NotFound proof
  expired-marker proof/horizon
  freshness/lag claim and mechanism
  retention mechanism
  ownership fence when movable
  pending/out-of-order capacity and backpressure when buffering/reordering exists
```

The authority does not become command authority for source facts. Its stamp proves only its own snapshot.

If several independently delivered outputs can stop, retain one bounded canonically ordered record per complete `SemanticHandle`. An identical duplicate is a no-op; conflicting evidence for one handle fails closed; another handle never evicts an existing record; lifecycle and cancellation remain independent. A stop before its causal workflow record is bounded-pending or causally ordered, never dropped.

## 7. Triggered tests

Select the rows whose paths exist:

| Trigger | Minimum test |
|---|---|
| synchronous causal completion | pre-reserve total budget; at N+1 the Decision fails before acceptance or the already accepted completion is retained in one bounded `RetainedContinuation`; never drop, truncate, or reset budget |
| detached result | exact source/provenance; stale, duplicate, and conflict cases only when reachable |
| possible send without proof | possible-send boundary, unknown, one reconcile identity |
| retry | stable logical identity, finite budget, cumulative owners, plus idempotency horizon only when blind duplicate execution is possible |
| cancellation | reachable result/cancel orders and terminal conflict |
| timer | stable identity/late policy; old/new generation only for replacement and dedup only for duplicate/redelivery risk |
| delivery observation | commit-before-observation, no direct runtime state write, plus monotonic duplicate handling only for duplicate observations |
| status | exact stamped authority and base lifecycle; absence, expiry, and bounded stopped handles only when those facets exist |
| durable retained value | crash at each assignment/output boundary and exact recovery lineage |

For Checkout-specific `M→S→I→P`, reply-plus-command stop capacity, grant, and migration fixtures, use Core §16 and [EXAMPLE-CROSSWALK.md](EXAMPLE-CROSSWALK.md). They are not inherited by a Ball whose closed paths do not contain those risks.
