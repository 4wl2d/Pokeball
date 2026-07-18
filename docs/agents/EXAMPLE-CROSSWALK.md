# Example Crosswalk

> **Status:** derived noncanonical package. It neither defines nor extends Pokeball Core. Verify [BASELINE.md](BASELINE.md); Core prevails.

Catalog (§15) and Checkout (§16) are advanced fixtures. Use only the property whose trigger matches the project. Do not copy their profiles, identities, limits, routes, participants, grants, status authority, or artifact count.

## Catalog Feature Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| local authority and logical zones | §§15.2–15.5 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| external resource and safe sink | §§15.3, 15.5 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| detached result identity/stale policy | §§15.2, 15.4, 15.6–15.7 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| cancellation/result races | §§15.1–15.2, 15.8 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| possible-send unknown | §15.9 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |

Canonical causal trace:

```text
SearchRequested generation N
-> accepted state + detached FindProducts handle N
-> provenance-bound result
-> apply current / ignore stale / typed failure or unknown
-> publish output only after acceptance
```

For its cancellation trigger, compatible accepted-in-progress/result orderings converge; terminal cancellation itself proves acceptance; a later weaker observation is a no-op. Mutually exclusive terminal result/cancellation proof preserves the first accepted terminal frame and rejects the contradictory second proof before acceptance, symmetrically in both orders.

A Ball with no external operation, detached result, or cancellation path inherits none of this machinery.

## Checkout Flow Ball

| Trigger demonstrated | Core anchor | Agent route |
|---|---|---|
| stateful multi-authority coordination | §§16.1, 16.14 | [COMPOSITION-PROFILES.md](COMPOSITION-PROFILES.md) |
| durable ingress/idempotency | §16.2 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| cross-Decision retained values | §§16.3, 16.5–16.9 | [DESIGN-RUNBOOK.md](DESIGN-RUNBOOK.md) |
| delayed privileged actions | §16.4 | [SECURITY-LIMITS-RUNBOOK.md](SECURITY-LIMITS-RUNBOOK.md) |
| unknown/reconciliation/compensation | §§16.8, 16.10 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| post-commit delivery observation | §16.12 | [ASYNC-STATUS-RUNBOOK.md](ASYNC-STATUS-RUNBOOK.md) |
| durable operation status/multi-stop | §16.13 | [TEST-AND-REVIEW-GATES.md](TEST-AND-REVIEW-GATES.md) |

Canonical retained-value trace:

```text
CheckoutStarted retains M + ingress fingerprint + actor binding
CartLocked retains S
InventoryReserved retains I -> Capture(S, M, current grant)
PaymentCaptured retains P -> Confirm(S, I, P)
Order rejection -> Refund(P) + Release(I) + Unlock(original cart/lock)
```

Each retained value carries the authority/observation correlation, versions, provenance, and last-consumer retention required by the durable path. Direct and reconciled proof of the same P corroborate; conflicting proof fails closed. Recovery uses committed values, never a participant/runtime ledger or rerun of current transition code.

The delivery/status fixture includes the initial Reply plus all command handles, bounded pending/retention, duplicate/conflict, and capacity-edge tests. Those are Checkout's reachable paths, not universal Ball requirements.

## Review use

1. Identify one analogous path or risk, not a similar domain name.
2. Cite Core and the matching `PKB-AR-*` rule, not the example alone.
3. Compare authoritative project source and effective policy with the invariant.
4. Use only the tests activated by that path.
5. Return to Core when the example does not cover the property.
