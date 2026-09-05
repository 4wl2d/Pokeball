# Feature source routes

Choose the row matching the source delta. Read complete linked sections, not every row. This is routing, not a second definition of Core.

| Task | Sources and useful checks |
|---|---|
| Business-rule change in an established feature | [§21.7 responsibility](core/21.7.md#217-everyday-development-and-production-responsibility), owning decision and its tests. If decision inputs change: [§8.1 context](core/8.1.md#81-decisioncontext), [§8.2 determinism](core/8.2.md#82-determinism). |
| First feature or new mutation form | [§3.3 operation forms](core/3.3.md#33-canonical-mutation-and-read-forms), [§14.1 source contract](core/14.1.md#141-role-of-the-manifest), [§14.2 present fields](core/14.2.md#142-minimal-core-manifest). Resolve the writer/acceptance through the binding workflow if none exists. |
| State ownership or data surviving to a later decision | [§7.1 state kinds](core/7.1.md#71-state-kinds), [§7.2 authority](core/7.2.md#72-one-mutable-factone-authority), [§7.5 captured input](core/7.5.md#75-captured-input), [§7.6 writer](core/7.6.md#76-single-writer). Preserve the later consumer's required lineage. |
| Input representation versus a business constraint | [§5.1 Interaction](core/5.1.md#51-interaction-hemisphere), [§5.2 Nucleus](core/5.2.md#52-protocol-nucleus), [§6.13 error stages](core/6.13.md#613-error-classes). State/context-dependent rejection belongs to the decision. |
| New or changed protocol variant | [§6.14 closure](core/6.14.md#614-protocol-closure), then the relevant category in [protocol routes](protocol.md); select that category, not the whole table. |
| Local query or view policy | [§6.3 Query](core/6.3.md#63-query), [§8.10 read](core/8.10.md#810-local-read). Read results do not initiate a target decision/commit or effects. |
| Present variable input, State, outputs or decision work | [§8.3 bounded decision](core/8.3.md#83-bounded-decision), [§13.1 limit classes](core/13.1.md#131-three-classes-of-limits). Reuse construction proofs or the exact scoped bound. |
| Actor-dependent decision/read, new I/O or trust boundary | [§11.1 quarantine](core/11.1.md#111-double-quarantine), [§11.2 actor](core/11.2.md#112-actor-context), [§11.3 gates](core/11.3.md#113-policy-gate-and-execution-gate); continue to the changed capability, grant or sink heading only when present. |
| Verify the changed transition | Relevant [§17.1 transition](core/17.1.md#171-transition-tests) or [§17.2 property](core/17.2.md#172-property-based-tests) cases. Reuse still-applicable binding evidence. |
