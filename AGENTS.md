# KotlinAssay contributor instructions

Read `README.md`, `CONTRACT.md`, and `QUALIFICATION.md` before changing the checker.
This is a local `strict-syntax/0.1` prototype. Do not describe syntax checks as a
proof of purity, correctness, or Android behavior.

## GitHub account protection

The owner reports a previous GitHub ban and reinstatement associated with bulk
activity. Keep remote work limited to the explicitly requested task. Prepare and
review changes locally, consolidate related changes into one meaningful commit,
and avoid repetitive pushes, mass issues, comments, pull requests, or repository
creation. Check existing remote state before creating content. Respect rate
limits and retry instructions; stop on an abuse, suspension, or restriction
warning. Never rotate identities or credentials to evade a restriction.

On the owner's workstation, also read `/home/arun/Developer/AGENTS.md`.

## Implementation and validation

- Keep decision logic in `src/domain`; compiler, filesystem, and process effects
  belong in `src/adapters`.
- Preserve Pass, Fail, Inconclusive, and ToolFailure. Missing evidence cannot pass.
- Treat policy command vectors as trusted execution instructions; inspect them
  before running verification on an unfamiliar target.
- Run `./kotlin-assay --self-test` for checker changes.
- Run `python3 eng/qualify.py --evidence-dir /tmp/kotlinassay-qualification`
  for end-to-end qualification. It requires the sibling synthetic Nalan specimen;
  use `--nalan /path/to/specimen` when necessary.
- Keep historical `evidence/` receipts intact. Put fresh receipts in a separate
  directory and report exactly what was run.
- Do not commit generated builds, local caches, credentials, or private Nalan
  application data. Distribution and semantic effect analysis remain deferred.
