# AGENTS.md

## Context

JagFx is a .NET 8 solution for parsing, synthesizing, and editing OSRS `.synth` instrument files. The binary format spec lives in `specs/synth-format-spec.md`. A JSON interchange format enables programmatic patch creation.

## Build & Test

```bash
dotnet build        # Must pass with 0 errors, 0 warnings
dotnet test         # Must pass all tests before any PR
```

## Project Layout

| Project           | Role                                                                                |
| ----------------- | ----------------------------------------------------------------------------------- |
| `JagFx.Core`      | Value types (`Percent`, `Milliseconds`, `Samples`), `AudioConstants`                |
| `JagFx.Domain`    | Immutable domain records (`Patch`, `Voice`, `Envelope`, `Filter`, etc.)             |
| `JagFx.Io`        | Binary reader/writer, JSON serialization (`Json/`), WAV writer                      |
| `JagFx.Synthesis` | DSP engine: `PatchRenderer`, `VoiceSynthesizer`, `AudioFilter`                      |
| `JagFx.Cli`       | CLI commands via `System.CommandLine`: `convert`, `inspect`, `to-json`, `from-json` |
| `JagFx.Desktop`   | Avalonia 11 MVVM desktop editor with custom canvas controls                         |
| `JagFx.TestData`  | Embedded hex resources for test fixtures                                            |

## Key Patterns

- **Domain models**: immutable `record` types with `ImmutableList`/`ImmutableArray`
- **I/O layer**: `SynthFileReader`/`SynthFileWriter` are static classes operating on `BinaryBuffer`
- **JSON layer**: `SynthJsonSerializer` → `SynthJsonMapper` (domain ↔ DTO) → `SynthJsonModels` (DTOs with `[JsonPropertyName]`)
- **CLI commands**: class extending `Command`, registered in `JagFxCli.BuildRootCommand()`
- **Tests**: xUnit with `TestResources.GetBytes("name")` for embedded hex fixtures
- **Desktop**: MVVM with `CommunityToolkit.Mvvm`, service-based architecture
- **Controls**: custom Avalonia canvas controls (`EnvelopeCanvas`, `WaveformCanvas`, `PoleZeroCanvas`, `KnobControl`) in `Controls/`
- **DAW-style interactions**: double-click to insert breakpoints, right-click context menu, Delete key removal, cursor feedback
- **Theme**: `ThemeColors.cs` centralizes rendering color constants; `Colors.axaml` defines XAML-side resource brushes

## Rules

- Run `dotnet test` and verify all tests pass before declaring work complete
- Follow existing code style in each file (nullable enabled, implicit usings)
- Use DSP field names from `specs/synth-format-spec.md`
- Don't add NuGet packages without justification
- Don't break binary round-trip fidelity for files that currently round-trip cleanly
- Commit messages: `[scope:] verb lowercase text` (e.g. `synth reader: fix filter detection`)

## Specs & References

- `specs/synth-format-spec.md` — authoritative binary format documentation
- `specs/synth.schema.json` — JSON interchange format schema
- `specs/examples/` — example JSON patches
- `references/synths/` — real `.synth` files for testing
- `llms.txt` — project overview for LLM consumption
