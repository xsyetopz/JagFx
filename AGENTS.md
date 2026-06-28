# AGENTS.md

JagFx is a .NET 8 toolkit for Old School RuneScape `.synth` sound-effect files. The repository contains:

- `JagFx.Cli`: command-line conversion, inspection, and rendering.
- `JagFx.Desktop`: Avalonia 11 MVVM desktop editor.
- `JagFx.Io`: binary `.synth` reader/writer, JSON serialization, and WAV writing.
- `JagFx.Synthesis`: DSP rendering for patches and voices.
- JavaScript packages under `packages/` for CLI-backed bindings and WASM work.

## Commands

Use Bun for JavaScript package work.

```bash
dotnet build          # Must pass with 0 warnings; TreatWarningsAsErrors is enabled
dotnet test           # Must pass before reporting backend work complete
dotnet format         # Auto-fix formatting; CI checks formatting
bun test              # Use inside JS package directories that define Bun tests
dotnet run --project src/JagFx.Cli -- <command> [args]
dotnet run --project src/JagFx.Desktop
just release-all      # Build release artifacts; see PUBLISHING.md
```

On machines where `dotnet` resolves to a newer SDK, use the .NET 8 install path if needed:

```bash
/usr/local/share/dotnet/dotnet test --nologo
```

## Project layout

| Project | Role |
| --- | --- |
| `JagFx.Core` | Value types (`Percent`, `Milliseconds`, `Samples`) and `AudioConstants`. |
| `JagFx.Domain` | Immutable records for `Patch`, `Voice`, `Envelope`, `Filter`, and related models. |
| `JagFx.Io` | Binary reader/writer, JSON mapper/serializer, and WAV writer. |
| `JagFx.Synthesis` | `PatchRenderer`, `VoiceSynthesizer`, `AudioFilter`, waveform tables, and envelope generation. |
| `JagFx.Cli` | `System.CommandLine` commands: `convert`, `inspect`, `to-json`, `from-json`. |
| `JagFx.Desktop` | Avalonia editor, MVVM view models, services, and custom canvas controls. |
| `JagFx.TestData` | Embedded hex fixtures for tests. |
| `JavaRandom` | Java-compatible RNG for the OSRS noise waveform. |
| `SmartInt` | Variable-length integer codec. |
| `packages/jagfx` | JS/TS helpers around the CLI and JSON patch format. |
| `packages/jagfx-wasm` | WASM package and browser playground. |

## Code patterns

- Domain models are immutable `record` types with `ImmutableList` or `ImmutableArray` fields.
- `SynthFileReader` and `SynthFileWriter` are static I/O entry points over `BinaryBuffer`.
- JSON conversion flows through `SynthJsonSerializer` -> `SynthJsonMapper` -> `SynthJsonModels`.
- CLI commands extend `Command` and are registered in `JagFxCli.BuildRootCommand()`.
- Tests use xUnit and Arrange-Act-Assert. Reference synths live under `references/synths/`.
- Desktop code uses `CommunityToolkit.Mvvm` and service-based view models.
- Canvas controls live under `src/JagFx.Desktop/Controls/` and use DAW-style interactions: double-click insert, right-click menu, Delete key, and cursor feedback.
- Rendering colors and pens belong in `ThemeColors.cs`; XAML brushes belong in `Colors.axaml`.
- DSP field names follow `docs/synth-format-spec.md`: `StartValue`, `EndValue`, `Duration`, `TargetLevel`, and related binary-format names.

## Required behavior

- Do not revert user changes unless the user explicitly asks.
- Do not push. The user pushes.
- Do not commit unless the user explicitly asks.
- Before committing, stage only intended files and use a Conventional Commit message.
- Keep binary round-trip fidelity for files that currently round-trip cleanly.
- Preserve known JSON test behavior: files with existing binary asymmetries compare against `SynthFileWriter.Write(SynthFileReader.Read(bytes))`, not raw original bytes.
- Do not commit secrets, `.env` files, or local credential material.
- Ask before adding NuGet packages. Explain why built-in APIs are not enough.
- Ask before changing `docs/synth-format-spec.md` unless the user is already asking for format-doc work.

## Formatting and commits

Commit messages must use Conventional Commits:

```text
type(scope): description
```

Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

Examples:

- `feat(io): add synth json fixtures`
- `fix(synthesis): preserve filter coefficient phase`
- `docs(format): clarify partial encoding`

## Format notes

- `.synth` files are big-endian.
- A patch has ten voice slots followed by a four-byte loop footer.
- Variable-length integers use `USmart16` and `Smart16` from `SmartInt`.
- Filter detection is heuristic; check `SynthFileReader.DetectFilterPresent` before changing filter parsing.
- Partial amplitude `0` terminates the partial list in binary.

## References

- `docs/synth-format-spec.md`: binary `.synth` format and synthesis semantics.
- `schemas/synth.schema.json`: JSON interchange schema.
- `references/synths/`: real `.synth` files used for tests.
- `references/json/`: JSON generated from every reference `.synth` file.
- `llms.txt`: project overview for LLM readers.
- `PUBLISHING.md`: release and signing steps.

## Tooling

- Version fields live in `Directory.Build.props`.
- CI lives in `.github/workflows/ci.yml`.
- Release packaging lives in `.github/workflows/release.yml` and `justfile`.
- CodeQL lives in `.github/workflows/codeql.yml`.
- Dependabot tracks NuGet and GitHub Actions dependencies.
- `.editorconfig` and Roslyn analyzers enforce C# style.
- Husky.Net and CommitLint.Net install through `dotnet restore`.
