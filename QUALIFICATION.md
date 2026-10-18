# KotlinAssay qualification snapshot

**Recorded:** 4 October 2026. **Profile:** `strict-syntax/0.1`; tool v0.1.0; Kotlin 2.4.20; Java 21.0.12.1. **Observed scope:** local checker core and isolated synthetic Nalan domain. All 17 qualification cases met their expected outcome. A deliberately bad specimen receiving Fail is a successful rejection test, not a failure of the qualification run.

The [machine-readable summary](evidence/qualification.json) binds each retained receipt by SHA-256. Receipts record file digests, policy/toolchain/tool identity, rule IDs, classified boundaries, compilation and behavior command results. Their hashes were checked after the run. The two positive targets have complete parsed/classified scope, unchanged source snapshots and zero findings; real compilation and configured behavior commands exited 0.

## Positive feature evidence

| Target | Evidence | Result |
| --- | --- | --- |
| Checker core | [tool-core receipt](evidence/tool-core.json), [test output](evidence/tool-core-tests.log) | Pass; 48 assertions, including good/bad shapes for all 18 rules and adversarial/verdict cases. |
| Nalan domain | [nalan-domain receipt](evidence/nalan-domain.json), [test output](evidence/nalan-domain-tests.log) | Pass; 36 behavior checks, including 1,001 generated wake-window cases. |
| Repeated analysis | [first scan](evidence/scan-not-verification.json), [repeat scan](evidence/repeatable-scan.json) | Byte-identical receipts; both Inconclusive because compilation/tests were not run. |

Nalan's tests exercise bounded standing/follow-up, rest/exception suppression, dismissal versus a standing report, unknown sleep/history, approximate wake guides within chosen limits, latest fallback, partial/paused/unknown NSDR and local event replay/ordering/conflict preservation. Inputs are synthetic. Test loops/assertions/output are declared boundary code, not certified functional domain logic.

## End-to-end rejection and incomplete-evidence cases

| Case | Expected and observed verdict | Required observation |
| --- | --- | --- |
| [Mutation](evidence/reject-mutation.json) | Fail | KFA001 present. |
| [Behavioral OOP](evidence/reject-oop.json) | Fail | KFA004 present. |
| [Imperative loop](evidence/reject-loop.json) | Fail | KFA003 present. |
| [Hidden effects](evidence/reject-hidden-effects.json) | Fail | KFA008 present. |
| [Direct boundary call](evidence/reject-boundary-call.json) | Fail | KFA015 present. |
| [Invalid source](evidence/invalid-source-not-pass.json) | Inconclusive | Parse gap cannot pass. |
| [Unclassified source](evidence/unclassified-not-pass.json) | Inconclusive | Incomplete scope cannot pass. |
| [Missing behavior command](evidence/missing-test-not-pass.json) | Inconclusive | Compilation alone cannot pass. |
| [Failed command](evidence/failed-command-fails.json) | Fail | Deliberate test command exits 7. |
| [Unavailable command](evidence/unavailable-command-not-pass.json) | Inconclusive | Missing executable cannot pass. |
| [Source drift](evidence/source-drift-not-pass.json) | Inconclusive | Changed fixture cannot use the earlier snapshot. |
| [Invalid policy](evidence/invalid-policy-tool-failure.json) | ToolFailure | Unsupported profile is surfaced. |
| [Missing policy](evidence/missing-policy-not-pass.json) | Inconclusive | Undeclared scope cannot pass. |

These thirteen cases plus the four positive/repeat-scan cases total 17. Qualification uses the recorded command `python3 eng/qualify.py`; generated bad specimens stay under `build/qualification`. Retained test-output copies match their receipt output digests at this snapshot. No universal syntax/semantic coverage or filesystem-security proof is claimed.

## Limits and stopping point

The result qualifies a **local syntactic prototype**, not a general Kotlin purity theorem or a completed Nalan app. Compiler/file/process/test adapters are explicitly outside the certified domain. No Android app, real alarm, Bluetooth authentication/radio/reconnect, device background delivery, private history import, model benchmark or appliance control has been tested here.

The current stopping point is the working checker plus these Nalan specimens. Add a rule or behavior test when a concrete implemented Nalan feature needs it. Semantic effect analysis, IDE plugins, distribution, release automation and a wider assay ecosystem are deferred. Future Android behavior needs device evidence, separately from a clean domain receipt.
