# Managed by typescript-bedrock. Put project recipes in project.just.

set shell := ["bash", "-euo", "pipefail", "-c"]
set working-directory := ".."

# Install the dependencies. CI must not change bun.lock.
setup:
    bun install {{ if env("CI", "") == "" { "" } else { "--frozen-lockfile" } }}

# Format the code.
format:
    bun fmt

# Fail if the code is not formatted.
format-check:
    bun fmt --check

# Type-check without emitting anything.
typecheck:
    bun check

# Run oxlint, one type-aware run at a time on the machine. The base justfile lints Markdown.
lint:
    bun run typescript-bedrock lint

# Apply oxlint's fixes.
lint-fix:
    bun run typescript-bedrock lint --fix

# Run the tests.
test:
    bun test --pass-with-no-tests

# Report suppressions, other package managers, undocumented package API, TypeScript drift, and adopt drift.
guard:
    bun run typescript-bedrock guard

# Run every Bun check CI runs.
verify: format-check typecheck lint guard test

# The Bun checks the pre-commit hook runs.
pre-commit: format-staged typecheck lint guard

[private]
format-staged:
    bun fmt --staged
