# Asynchronous source routes

Start with the actual trace's row; combine rows only when their paths are present.

| Path | Complete source sections |
|---|---|
| Effect and external result | [§6.4 Effect](core/6.4.md#64-effect), [§6.5 Fact](core/6.5.md#65-fact), [§9.1 identity](core/9.1.md#91-causal-identity), [§9.2 revisioned causality](core/9.2.md#92-revisioned-causality). |
| Work outlives initiating call | [§9.11 operation status](core/9.11.md#911-operation-status), [§9.3 independent facets](core/9.3.md#93-independent-dimensions-of-operation-state), [§9.13 live/durable outputs](core/9.13.md#913-live-and-durable-outputs), [§12.3 detached execution](core/12.3.md#123-boundedconcurrent). |
| Inter-Ball command/result | [§6.7 boundary/reply](core/6.7.md#67-boundaryresponse-and-reply), [§10.7 Assembly](core/10.7.md#107-assembly), [§6.8 command](core/6.8.md#68-modulecommand), [§6.9 result](core/6.9.md#69-moduleresult), [§6.11 Pulse bridges](core/6.11.md#611-pulse), [§6.12 outputs](core/6.12.md#612-semanticoutput), [§9.4 ACK/result/carrier](core/9.4.md#94-ack-and-business-result), [§8.4 reservation](core/8.4.md#84-run-to-completion). This is a larger coherent protocol; retain its refusal and alternative-completion branches. |
| Timeout after a possible external action | [§9.5 OutcomeUnknown](core/9.5.md#95-outcomeunknown), [§9.8 deadlines](core/9.8.md#98-deadlines-and-timeout). |
| Duplicates or retry | [§9.6 idempotency](core/9.6.md#96-idempotency), [§9.9 retry owner](core/9.9.md#99-retry-ownership). Distinguish replay, pending and fingerprint conflict; do not create a new semantic operation for a transport retry. |
| Cancellation or timer | [§9.7 cancellation](core/9.7.md#97-cancellation), [§9.10 timers](core/9.10.md#910-timers), plus status when the work outlives its call. |
| Reordering or loss of correlated observations | [§9.12 ordering](core/9.12.md#912-ordering), [§9.11 status](core/9.11.md#911-operation-status). |
| Result ingress or execution authorization changes | [§11.1 verified boundaries](core/11.1.md#111-double-quarantine), [§11.3 gates](core/11.3.md#113-policy-gate-and-execution-gate), [§11.7 grants](core/11.7.md#117-authorization-grant) when required. |

Select the applicable checks from [§17.3 Resource contracts](core/17.3.md#173-resource-contract-tests) and [§17.6 concurrency](core/17.6.md#176-concurrent-profile-tests).
