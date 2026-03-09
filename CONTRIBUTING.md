# Contributing

This guide keeps expectations clear and simple so you can move fast without guessing.

## Before Starting

- Check open issues to avoid duplicating work.

## Standard Workflow

1. Fork repository.
2. Clone fork: `git clone https://github.com/<your-user>/jagfx-scala.git`.
3. Create topic branch: `git checkout -b feat/<short-description>`.
4. Make focused changes.
5. Run `dotnet build && dotnet test` to verify nothing is broken.
6. Commit with clear message explaining *why* the change exists.
7. Push and open a pull request describing behaviour changes and tests.

## Coding Guidelines

### Core Principles

- **KISS** -- prefer simplest solution that works today.
- **DRY** -- extract shared behaviour to keep one source of truth.
- **YAGNI** -- do not build future features until roadmap calls for them.

### Naming

Domain model field names follow DSP conventions — see `specs/synth-format-spec.md` for the authoritative reference. In particular:

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

## Testing Expectations

- Run `dotnet test` before every push.
- Add or update targeted tests for every bug fix or new feature.
- Mention any skipped or flaky tests in the pull request so reviewers know the risk.

## Using AI Assistants

You may use AI tooling but you remain responsible for code quality:

- Understand surrounding code before accepting AI suggestions.
- Review and test generated code carefully.
- Never merge output you do not fully understand.

## Pull Request Checklist

- [ ] Tests pass locally with `dotnet test`.
- [ ] Docs and comments updated if behaviour changed.
- [ ] Commit messages explain intent.
- [ ] PR description covers motivation, approach, and testing.

## Reporting Issues

When filing a bug, include:

- Steps to reproduce.
- Expected versus actual behaviour.
- Output snippets or logs when helpful.
- Commit hash, tooling version, and platform.

Feature requests should describe the use case and why it is needed now.

## Questions and Support

Open an issue if you need clarification on direction, architecture, or roadmap priorities. Discussions stay public so future contributors benefit from context.

## Code of Conduct

All contributors must follow [Code of Conduct](CODE_OF_CONDUCT.md).
