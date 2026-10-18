# Functional Kotlin

Use `val` properties in data classes and sealed interfaces or sealed classes for alternatives. Return a domain `Accepted` or `Rejected` value for expected validation failure rather than throw or use `!!`. Keep domain transformations total where possible and use folds to make accumulation explicit.

Parse and validate untrusted input at the boundary. File, network, clock, logging, and process effects belong in a named adapter or CLI shell. Tests must cover each alternative and avoid relying on ambient mutable state.

For this repository, use persistent collections and top-level ADT cases as required by `strict-syntax/0.1`. Authoring guidance does not confer a verification verdict.
