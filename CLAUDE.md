# CLAUDE.md

## Context

JagFx is a .NET 8 toolkit for OSRS synthesizer instrument files (`.synth`). It includes a CLI (`JagFx.Cli`), a desktop editor (`JagFx.Desktop`, Avalonia 11 MVVM), and a JSON interchange format for programmatic patch creation.

## Commands

```bash
dotnet build          # Must pass with 0 warnings (TreatWarningsAsErrors enabled)
dotnet test           # Must pass before committing
dotnet format         # Auto-fix formatting; CI checks --verify-no-changes
dotnet run --project src/JagFx.Cli -- <command> [args]
dotnet run --project src/JagFx.Desktop
make release-all      # Cross-platform distributable archives (see Makefile)
```

## Project Layout

| Project | Role |
| --- | --- |
| `JagFx.Core` | Value types (`Percent`, `Milliseconds`, `Samples`), `AudioConstants` |
| `JagFx.Domain` | Immutable domain records (`Patch`, `Voice`, `Envelope`, `Filter`, etc.) |
| `JagFx.Io` | Binary reader/writer, JSON serialization (`Json/`), WAV writer |
| `JagFx.Synthesis` | DSP engine: `PatchRenderer`, `VoiceSynthesizer`, `AudioFilter` |
| `JagFx.Cli` | CLI commands via `System.CommandLine`: `convert`, `inspect`, `to-json`, `from-json` |
| `JagFx.Desktop` | Avalonia 11 MVVM desktop editor with custom canvas controls |
| `JagFx.TestData` | Embedded hex resources for test fixtures |
| `JavaRandom` | Java-compatible RNG (for OSRS noise waveform) |
| `SmartInt` | Variable-length integer codec |

## Key Patterns

- **Domain models**: immutable `record` types with `ImmutableList`/`ImmutableArray`
- **I/O layer**: `SynthFileReader`/`SynthFileWriter` are static classes operating on `BinaryBuffer`
- **JSON layer**: `SynthJsonSerializer` → `SynthJsonMapper` (domain ↔ DTO) → `SynthJsonModels` (DTOs with `[JsonPropertyName]`)
- **CLI commands**: class extending `Command`, registered in `JagFxCli.BuildRootCommand()`
- **Tests**: xUnit, Arrange-Act-Assert, test data via `JagFx.TestData.TestResources`
- **Desktop**: MVVM with `CommunityToolkit.Mvvm`, service-based architecture
- **Controls**: custom Avalonia canvas controls in `Controls/` — DAW-style interactions (double-click insert, right-click menu, Delete key, cursor feedback)
- **Theme**: `ThemeColors.cs` centralizes rendering constants; `Colors.axaml` for XAML resource brushes
- **DSP field names** follow `specs/synth-format-spec.md` (e.g. StartValue/EndValue, Duration/TargetLevel)

## Rules

### Always do
- Run `dotnet test` before declaring work complete
- Run `dotnet format` before committing
- Follow existing code style in each file (nullable enabled, implicit usings, file-scoped namespaces)
- Use conventional commits: `type(scope): description` — types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

### Ask first
- Adding NuGet packages — justify why built-in alternatives won't work
- Modifying `specs/synth-format-spec.md` — requires understanding of OSRS client behavior

### Never do
- Break binary round-trip fidelity for files that currently round-trip cleanly
- Commit secrets or `.env` files
- Skip tests or bypass CI checks

## Binary Format Notes

- Big-endian throughout, variable-length integers (USmart16, Smart16)
- 10 voice slots + 4-byte loop footer per patch
- Filter detection uses heuristics (see `SynthFileReader.DetectFilterPresent`)
- 4 reference files have pre-existing binary round-trip asymmetries — JSON tests compare against `SynthFileWriter.Write(SynthFileReader.Read(bytes))` baseline, not raw original bytes

## Specs & References

- `specs/synth-format-spec.md` — authoritative binary format documentation
- `specs/synth.schema.json` — JSON interchange format schema
- `specs/examples/` — example JSON patches
- `references/synths/` — real `.synth` files for testing
- `llms.txt` — project overview for LLM consumption

## Tooling

- **Version**: managed in `Directory.Build.props`
- **CI**: GitHub Actions — build, test, format check, conventional commits validation (`.github/workflows/ci.yml`)
- **Security**: CodeQL analysis (`.github/workflows/codeql.yml`)
- **Dependencies**: Dependabot for NuGet + GitHub Actions updates
- **Linting**: `.editorconfig` + Roslyn analyzers (`AnalysisLevel: 8.0-recommended`)
- **Commit hooks**: Husky.Net + CommitLint.Net (auto-installed via `dotnet restore`)
