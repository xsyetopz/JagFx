# Contributing

This guide defines the contribution workflow and review expectations.

## Before Starting

- Check open issues before starting work.

## Standard Workflow

1. Fork the repository.
2. Clone your fork: `git clone https://github.com/<your-user>/JagFx.git`.
3. Restore tools: `dotnet tool restore` (installs Husky.Net + CommitLint.Net git hooks automatically).
4. Create a topic branch: `git checkout -b feat/<short-description>`.
5. Make focused changes.
6. Run `dotnet build && dotnet test && dotnet format`.
7. Commit using [Conventional Commits](#commit-style). The commit-msg hook validates automatically.
8. Push and open a pull request that describes behaviour changes and test coverage.

## Coding Guidelines

### Core Principles

- KISS: prefer the simplest solution that works now.
- DRY: extract shared behaviour when duplication obscures intent.
- YAGNI: do not build features before the roadmap needs them.

### Naming

Domain model field names follow DSP conventions. Use `specs/synth-format-spec.md` as the reference:

- Envelope boundary fields are `StartValue` / `EndValue` (not "samples").
- Segment fields are `Duration` / `TargetLevel`.
- Voice timing fields are `DurationMs` / `OffsetMs`.
- Loop region fields are `BeginMs` / `EndMs`.
- Gating envelopes are `GapOffEnvelope` / `GapOnEnvelope`.
- LFO carrier is `RateEnvelope`; filter modulation is `ModulationEnvelope`.
- Echo wet level is `FeedbackPercent`.
- The patch synthesis entry point is `PatchRenderer`.

### Comments and Docs

- Comment to explain *why* a choice was made, not what the code already states.
- Update documentation and examples whenever behaviour changes.

## Commit Style

This repository uses Conventional Commits, enforced by [CommitLint.Net](https://github.com/tomwis/CommitLint.Net) via [Husky.Net](https://github.com/alirezanet/Husky.Net) git hooks.

Format: `type(scope): description`

Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

Examples:

- `feat: add loop region editing`
- `fix(io): correct filter coefficient round-trip`
- `docs: update CLI usage in README`
- `refactor(synthesis): extract envelope generator`

The `commit-msg` hook runs after `dotnet tool restore`. CI validates commit messages on pull requests.

## Testing Expectations

- Run `dotnet test` before every push.
- Add or update targeted tests for every bug fix or new feature.
- Mention skipped or flaky tests in the pull request.

## Using AI Assistants

AI tooling is allowed. Contributors own the code they submit:

- Understand surrounding code before accepting AI suggestions.
- Review and test generated code.
- Do not merge generated output you do not understand.

## Pull Request Checklist

- [ ] Tests pass locally with `dotnet test`.
- [ ] Code formatted with `dotnet format`.
- [ ] Commit messages follow [Conventional Commits](#commit-style).
- [ ] Docs and comments updated if behaviour changed.
- [ ] PR description covers motivation, approach, and test coverage.

## Reporting Issues

When filing a bug, include:

- Steps to reproduce.
- Expected versus actual behaviour.
- Output snippets or logs when helpful.
- Commit hash, tooling version, and platform.

Feature requests should describe the use case and timing.

## Questions and Support

Open an issue for direction, architecture, or roadmap questions. Keep discussions public so future contributors can find the context.

## Code of Conduct

Contributors must follow [Code of Conduct](CODE_OF_CONDUCT.md).
