# KotlinAssay — strict functional profile and evidence contract

**Version:** 0.1.0 local prototype · 4 October 2026.
**Original owner request (4 October 2026):** create a stricter Kotlin assay, inspired by the cloned CanonFlow-Assay tools and ai-skills, and dogfood it against Nalan after functional requirements are recorded. That request did not include publication, push, release or Android-app deployment. A later owner request authorizes GitHub repository setup and one consolidated initial commit; package releases and Android-app deployment remain outside that scope.

## 1. Authority

`authority ≤ evidence ≤ observed scope`

Four verdicts only: **Pass, Fail, Inconclusive, ToolFailure**. Pass concerns a named mechanical profile over exact files plus actually executed compilation/tests. It is not proof of universal purity, business correctness, deep immutability, alarm delivery, health validity or a complete Android app.

Use Kotlin compiler PSI for source structure, not a regular-expression word blacklist. Compilation is separate required evidence. Semantic symbol/alias/type resolution and full effect analysis require a later qualified implementation; unsupported/unobserved claims cannot become Pass.

## 2. Functional-first profile

| Domain requirement | Strict observation target |
| --- | --- |
| Immutable bindings | Reject `var`, mutable constructor properties, assignments, increment/decrement. |
| Expression-oriented algorithms | Reject `for`, `while`, `do`, jumps and explicit imperative iteration APIs. |
| Data and functions | Admit immutable data/value records, empty sealed tags and empty data-object cases; reject behavioral classes/interfaces, companion objects, custom member behavior/init/accessors/delegation. |
| Explicit expected outcomes | Reject `throw`, `try`, `!!`, unchecked casts, exception-based validation helpers. |
| Transparent effects | Reject recognized IO/clock/random/thread/coroutine effects and infrastructure imports in declared domain scope. |
| Persistent data vocabulary | Reject mutable/read-only container exposure and arrays in domain declarations; persistent containers are the preferred reviewed vocabulary. No claim of arbitrary deep immutability. |
| Closed matching | Reject default/else arms in subject-based `when`; compiler exhaustiveness remains required. Guarded condition-only `when` can have an else. |
| No escape hatches | Reject suppression/compiler-interop escape annotations and wildcard imports; aliases do not hide prohibited imported effects. |
| Scope completeness | Discover every `.kt` file under the target, bind classified source and exact bytes; unclassified source, empty domain or unexpected `.kts` makes evidence incomplete. |
| Honest verification | Scan cannot substitute for required compilation/behavior tests. Missing evidence, unsupported compiler or failed parsing cannot silently pass. |

The profile permits ADT representations, not behavioral OOP. Kotlin and the JVM use class constructs internally; do not market a literal absence of every JVM object. System/platform/CLI effects are declared adapters and reported separately, not called pure. No broad suppression baseline or silent legacy carve-out.

## 3. First implementation boundary

The initial tool enforces named PSI/syntax observations and known import/call hazards. Every blocking rule needs executable good/bad/adversarial specimens. Use function-oriented Kotlin for the tool's decision logic; contain compiler/file/process interactions in named adapters.

Do not promote incomplete interprocedural effect inference or full type/alias verification into an admitted rule. Receipts must label this first profile **syntactic**, list the exact rule set and state that compiler success does not prove purity. An explicit required command vector is owner-authored execution configuration; run it without shell expansion and capture exit/output evidence.

## 4. Receipt and execution requirements

Bind tool/profile version, source paths and SHA-256 digests, policy bytes, toolchain version/digest, observed rules/findings, complete/incomplete scope, command argument vectors/exit/output digests and verdict. Deterministic finding order and analysis with no changing clock. Detect source/policy changes between inspection and command completion; the old snapshot cannot authorize changed code.

Exit codes: 0 Pass, 1 Fail, 2 Inconclusive, 3 ToolFailure, 64 invalid CLI. A scan with required commands not run is Inconclusive. Compiler syntax errors, absent commands or empty scope never Pass. Boundary files are visible in the receipt; an absent Nalan Android implementation cannot be certified via an example directory.

## 5. Acceptance

The tool must distinguish source constructs from comments/strings; detect loops/mutation hidden inside lambdas; reject behavioral OOP and aliased known effects; keep invalid or unclassified input incomplete; capture a deliberately failed test as Fail; produce stable repeated receipts; and inspect an isolated Nalan functional specimen with actual compiler/behavior evidence.

Nalan-specific cases cover bounded standing, exceptions, dismissal versus completion, missing sleep/history, unknown cycle stage, chosen wake-window fallback, partial rest and independent NSDR/nap accounting. All are synthetic test inputs. The specimen and its functional requirements are in the separate sibling Nalan workspace, not this repository.

## 6. Inspiration and exclusions

Reuse CSharpAssay/TypeScriptAssay's explicit verification evidence, FSharpAssay's distinction between prototype/admitted rules, STEAssay's command vectors/digests and playground-style positive/negative specimens. ai-skills supplies Kotlin records/ADTs/functions; its permissive mutation/loop/exception examples do not override this owner policy. DartAssay is an M0 contract reference only; its human gate is not approval to modify it or build Dart here. eShop is a comparison specimen, not an architecture to copy. HTML5Assay's cloned main currently contains a qualification workflow only; do not claim its implementation was inspected on that branch.

Zenbridge/Haskell was not found in the searched local material. Preserve general functional principles without inventing project-specific recollection. Existing reference clones remain unchanged.
