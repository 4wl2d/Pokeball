# Composition source routes

| Changed decision | Read |
|---|---|
| Combine, split or introduce a Ball | [§4.4 boundary decision/falsifiers](core/4.4.md#44-decision-tree-and-falsifiers), [§4.6 non-Balls](core/4.6.md#46-what-is-not-a-ball), [§10.1 component kinds](core/10.1.md#101-application-component-kinds). |
| Cross-authority Query or aggregation | [§10.2 dependencies and ReadDependency](core/10.2.md#102-permitted-inter-module-dependencies), [§10.6 read aggregation](core/10.6.md#106-read-aggregation), [§8.10 local read](core/8.10.md#810-local-read). Target owns the admitted result algebra; a read adds no target mutation/commit/output. |
| A workflow may need an independent owner | [§10.3 Flow criterion](core/10.3.md#103-when-a-flow-is-needed), [§10.4 sovereignty](core/10.4.md#104-workflow-sovereignty), [§14.3 actual Flow contract](core/14.3.md#143-manifest-for-a-flow). Read [§10.5 compensation](core/10.5.md#105-compensation) only for that failure path. |
| Command/signal wiring or owner-authored surface import | [§10.2 dependencies](core/10.2.md#102-permitted-inter-module-dependencies), [§10.7 Assembly](core/10.7.md#107-assembly), [§10.8 no re-export](core/10.8.md#108-no-protocol-re-export), [§14.4 Assembly declaration](core/14.4.md#144-assembly-declaration). Use [§6.10](core/6.10.md#610-signal) for a Signal; commands additionally need the async protocol route. |
| Dependency count, routes, fan-out or cycle | [§10.9 bounded composition](core/10.9.md#109-bounded-composition), [§10.10 cycles](core/10.10.md#1010-cycles), and the affected edge's classification in §10.2. |
| Extract or share helper code | [§4.6 non-Balls](core/4.6.md#46-what-is-not-a-ball), [§5.5 imports](core/5.5.md#55-permitted-dependency-graph), [§14.7 Foundation](core/14.7.md#147-foundation-quarantine) only for shared Foundation. |
| Protocol compatibility changes | [§10.11 versions](core/10.11.md#1011-versioning-and-compatibility), the affected category in §6 and its owner/consumer tests. |

Check the changed graph/contracts through the relevant [§17.5 architecture tests](core/17.5.md#175-architecture-tests) and [§18.2 composition checklist](core/18.2.md#182-composition). An unchanged graph does not require a new whole-system dossier.
