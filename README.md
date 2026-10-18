# KotlinAssay

**Status:** v0.1.0 local prototype, qualified on 4 October 2026 for the named `strict-syntax/0.1` profile and synthetic Nalan specimens. This is the bounded checker requested for Nalan; IDE integration, distribution and general semantic effect analysis are deferred.

KotlinAssay checks an explicitly declared functional domain with Kotlin compiler PSI, then compiles the actual scoped Kotlin files and executes configured behavior commands. Immutable records/ADTs and top-level functions are admitted. Behavioral OOP, mutation, imperative loops, exception-based control and recognized hidden effects are rejected by 18 rules. File/compiler/process/test effects live in declared adapters and are listed separately.

**Result:** all 17 end-to-end qualification cases met their expected verdict; the checker passes 48 policy assertions. Nalan passes 36 behavior checks, including 1,001 generated wake-window cases. See [qualification and limitations](QUALIFICATION.md) and the [contract](CONTRACT.md). The synthetic Nalan specimen belongs to the separate sibling workspace and is not included in this repository.

## Use on this machine

Requires the existing user-local pinned Kotlin 2.4.20/dependencies in [toolchain.lock.json](toolchain.lock.json), Java 21 and Python 3. The launcher checks pinned toolchain bytes and builds a cached JVM tool. No sudo is needed. Paths/configured classpaths currently target this workspace; this is not a portable packaged installer.

```bash
cd /home/arun/Developer/CanonFlow-Assay/KotlinAssay
./kotlin-assay doctor /home/arun/Developer/Nalan/assay/dogfood
./kotlin-assay --self-test
./kotlin-assay verify /home/arun/Developer/Nalan/assay/dogfood
```

`doctor` reports readiness information, not verification. `scan` observes syntax without running compilation/tests, so its normal result is **Inconclusive**. `verify` writes a canonical JSON receipt to stdout and the target's `.kotlinassay/verify.json`; command logs are beside it. Exit codes are 0 Pass, 1 Fail, 2 Inconclusive, 3 ToolFailure and 64 invalid CLI. The JSON includes literal command vectors, so it can be long.

Each target has `.kotlinassay.json` with the exact versioned schema/profile, explicit nonoverlapping `domainRoots`/`boundaryRoots` and `requiredCommands` argument arrays. Commands run without shell expansion. The tool discovers Kotlin files outside generated/cache directories; unclassified files, missing roots, unsupported scripts, parse gaps, missing behavior commands and source drift cannot yield Pass. Configuration commands are trusted local execution instructions, not sandboxed input.

For a justified change, rerun the bounded qualification:

```bash
python3 eng/qualify.py
```

For machine or CI runs, use `--nalan /path/to/Nalan/assay/dogfood` to select the synthetic specimen and `--evidence-dir /path/to/fresh-evidence` to retain the historical receipts. The default specimen is resolved relative to the sibling workspace; a missing policy fails before execution.

Negative cases are created under `build/qualification`; the real Nalan specimen is not changed. Retained receipts and their hashes are in [evidence/qualification.json](evidence/qualification.json).

## Scope of a pass

A pass means these named syntax rules and actually executed commands passed over the recorded source snapshot. This prototype does not resolve arbitrary symbols/aliases, infer all effects, prove deep immutability or certify Android alarm/background/Bluetooth behavior. Known call-name checks are conservative and can reject a harmless name. Untested syntax or dependencies must receive review rather than an assumed purity guarantee. The compiler PSI adapter uses a localized opt-in to the pinned compiler's internal/deprecation-marked API.

Reference clones are pinned in [reference-lock.json](reference-lock.json). No upstream repository was changed or published. Zenbridge/Haskell was not found in the searched local material; no project-specific recollection is assumed.

## USB feature selection

The USB Kotlin prototype contributed functional authoring guidance in
[docs/functional-kotlin.md](docs/functional-kotlin.md) and the order-domain
example in `examples/order`. The example uses persistent collections and
top-level sealed outcome cases to fit the existing PSI profile. Run:

```sh
./kotlin-assay verify examples/order
```

Its eight behavior checks cover each validation branch and bounded-price
totals. Money arithmetic is illustrative and does not guard against Long
overflow. The lexical scanner and its weaker two-state Pass/Fail receipt were
not imported. Maven/JDK 25 packaging remains separate from this Java 21 tool.
