# CLAUDE.md

## Project

JagFx is a .NET 8 toolkit for OSRS synthesizer instrument files (`.synth`). It includes a CLI (`JagFx.Cli`), a desktop editor (`JagFx.Desktop`, Avalonia 11 MVVM), and a JSON interchange format for programmatic patch creation.

## Commands

```bash
# Build
dotnet build

# Test (always run before committing)
dotnet test

# Run CLI
dotnet run --project src/JagFx.Cli -- <command> [args]

# Run desktop app
dotnet run --project src/JagFx.Desktop

# Publish (see Makefile for platform-specific targets)
make publish-macos-arm64
make publish-macos-x64
make publish-windows
make publish-linux
```

## Architecture

```text
src/
  JagFx.Core/        # Value types (Percent, Milliseconds, Samples), AudioConstants
  JagFx.Domain/      # Immutable records: Patch, Voice, Envelope, Filter, Partial, Echo, LFO
  JagFx.Io/          # SynthFileReader/Writer (binary), Json/ (SynthJsonSerializer), WaveFileWriter
  JagFx.Synthesis/   # PatchRenderer, VoiceSynthesizer, AudioFilter, EnvelopeGenerator
  JagFx.Cli/         # System.CommandLine CLI: convert, inspect, to-json, from-json
  JagFx.Desktop/     # Avalonia MVVM app
    Controls/        # EnvelopeCanvas, WaveformCanvas, PoleZeroCanvas, KnobControl
    ViewModels/      # MVVM view models (CommunityToolkit.Mvvm)
    Views/           # AXAML views: Inspector, SignalChain, Footer, Header
libs/
  JavaRandom/         # Java-compatible RNG (for OSRS noise waveform)
  SmartInt/           # Variable-length integer codec
tests/
  JagFx.Io.Tests/     # Reader/writer/JSON round-trip tests
  JagFx.Synthesis.Tests/
  JagFx.TestData/     # Embedded hex resources for test fixtures
specs/
  synth-format-spec.md   # Authoritative binary format spec (use for field naming)
  synth.schema.json      # JSON interchange schema
```

## Conventions

- **Domain models are immutable records** using `System.Collections.Immutable`
- **Nullable reference types enabled** across all projects
- **DSP field names** follow `specs/synth-format-spec.md` (e.g. StartValue/EndValue, Duration/TargetLevel)
- **CLI command pattern**: class extending `Command`, register in `JagFxCli.BuildRootCommand()`
- **Tests**: xUnit, Arrange-Act-Assert, test data via `JagFx.TestData.TestResources`
- **Commit style**: `[scope:] verb lowercase` (e.g. `synth reader: consolidate filter reading`)
- **No .editorconfig** — follow existing code style in each file
- **Version**: managed in `Directory.Build.props`
- **Canvas controls**: DAW-style interactions (double-click insert, right-click menu, Delete key, cursor feedback)
- **Theme colors**: `ThemeColors.cs` centralizes rendering constants; `Colors.axaml` for XAML resource brushes

## Binary format notes

- Big-endian throughout, variable-length integers (USmart16, Smart16)
- 10 voice slots + 4-byte loop footer per patch
- Filter detection uses heuristics (see `SynthFileReader.DetectFilterPresent`)
- 4 reference files have pre-existing binary round-trip asymmetries — JSON tests compare against `SynthFileWriter.Write(SynthFileReader.Read(bytes))` baseline, not raw original bytes

## Don'ts

- Don't add packages without justification — `System.Text.Json` is built into .NET 8
- Don't break binary round-trip fidelity for files that currently round-trip cleanly
- Don't modify `specs/synth-format-spec.md` without understanding the OSRS client behavior it documents
