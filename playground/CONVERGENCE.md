# Playground convergence — phase 1

The ExpenseGuard specimen and strict-syntax contract already preserve the useful
portable boundary: pure sealed outcomes, explicit adapters, named evidence and
no exception-based expected validation. KotlinAssay itself remains an explicitly
syntactic prototype.

The next candidate is a configuration firewall in the application specimen,
using immutable Kotlin data and sealed rejection cases. Compile only with the
locked local compiler and retain fresh evidence. Do not add a serialization
framework, claim Android behavior, replace reviewed policy with a small DSL, or
call a syntactic Pass a semantic purity proof.

## Phase 2 specimen (`playground/configuration-firewall`)

Compile and run the sealed Kotlin 2.4.20 configuration firewall with the locked compiler:

```sh
kotlinc playground/configuration-firewall/domain/Firewall.kt playground/configuration-firewall/tests/Main.kt -jvm-target 21 -Werror -include-runtime -d /tmp/kotlin-firewall.jar
java -jar /tmp/kotlin-firewall.jar
```
