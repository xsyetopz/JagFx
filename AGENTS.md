# Agent brief

JagFx is a Kotlin Multiplatform toolkit for Old School RuneScape `.synth` sound-effect files, built with Kotlin 2.4.20, Compose Multiplatform 1.12.1, and a JDK 21 toolchain. The repository contains:

- `:cli`: Clikt command-line conversion, inspection, and rendering.
- `:sharedUI`: Compose editor UI shared by the desktop and web apps.
- `:core:format`: binary `.synth` codec and JSON serialization.
- `:core:engine`: DSP rendering and WAV writing.
- `:core:vorbis`: `.jagvorbis` sample codec and Vorbis decoder.
- JavaScript packages under `packages/` backed by the Kotlin/JS and Kotlin/Wasm builds of `:bindings`.

## Commands

Use Bun for JavaScript package work.

```bash
./gradlew check                     # Tests, spotlessCheck, and detekt; must pass before reporting work complete
./gradlew spotlessApply             # Auto-fix formatting; CI checks formatting
./gradlew detekt                    # Static analysis only
./gradlew :cli:run --args='<command> [args]'
./gradlew :desktopApp:run
./gradlew :bindings:syncNpmPackages # Copy Kotlin/JS and Kotlin/Wasm output into packages/
bun run build:js && bun run typecheck && bun run test:js
just release-all                    # Build release artifacts for the host OS; see PUBLISHING.md
```

Compiler warnings are errors (`allWarningsAsErrors`), and library modules use explicit API mode.

## Project layout

| Module | Role |
| --- | --- |
| `:core:model` | Immutable domain types: `Patch`, `Voice`, `Envelope`, `Filter`, value classes `Milliseconds` and `Percent`, and `SynthLimits`. |
| `:core:format` | `SynthCodec` binary codec, `SynthJson` interchange, and `TracedField` decode tracing. |
| `:core:engine` | `Renderer`, `VoiceSynthesizer`, `VoiceFilter`, `WaveTables` (with the Java-compatible `JavaLcg` noise RNG), `EnvelopeGenerator`, and the `Wav` writer. |
| `:core:vorbis` | `JagVorbis` and `JagVorbisCodec` for `.jagvorbis` samples (the Vorbis audio stored with some sound effects), and a floor-1 Vorbis decoder that matches the game client's PCM. |
| `:sharedUI` | Compose editor: state, reducers, canvases, widgets, theme, and i18n strings. |
| `:desktopApp` | JVM desktop app with jpackage installers. |
| `:webApp` | Kotlin/Wasm browser app. |
| `:cli` | Clikt CLI, installed as `jagfx`. |
| `:benchmark` | kotlinx-benchmark render benchmarks with a checked baseline. |
| `:bindings` | Kotlin/JS and Kotlin/Wasm exports for the npm packages. |
| `build-logic/` | Convention plugins and `GenerateHexFixturesTask` (embeds `references/` files as hex for common tests). |
| `packages/jagfx` | JS/TS JSON patch helpers backed by Kotlin/JS. |
| `packages/jagfx-node` | Node rendering bindings backed by Kotlin/JS. |
| `packages/jagfx-wasm` | Kotlin/Wasm ESM package and browser playground. |

## Code patterns

- Package root is `io.github.xsyetopz.jagfx`. Shared code lives in `commonMain`; platform code in `jvmMain` and `wasmJsMain`.
- Domain models are immutable `data class` types with `List` fields that validate ranges in `init` through `SynthLimits`; units are `@JvmInline value class` types.
- `SynthCodec.decode` and `SynthCodec.encode` are the binary entry points over the internal `SynthDecoder` and `SynthEncoder`.
- JSON conversion flows through `SynthJson` -> `SynthJsonMapper` -> `SynthJsonModels` (kotlinx.serialization).
- CLI commands extend Clikt `CliktCommand`; `ConvertCommand` is the root and `jagFx()` in `cli/.../Main.kt` registers the subcommands.
- Tests use `kotlin.test` in `commonTest` with Arrange-Act-Assert. Reference synths live under `references/synths/` and reach common tests as generated hex fixtures.
- The editor uses unidirectional state: `EditorIntent` -> `reduce` in `EditorReducer.kt` -> `EditorState`, driven by `EditorViewModel`. Platform services (`AudioOutput`, patch files) are interfaces in `EditorServices.kt` with one implementation per platform.
- Canvas controls live under `sharedUI/src/commonMain/kotlin/io/github/xsyetopz/jagfx/ui/canvas/` and use DAW-style interactions: double-click insert, right-click menu, Delete key, and hover cursor feedback.
- Editor colors come from the Material 3 schemes in `JagFxColorSchemes` and scope colors from `SynthPalette` (`ui/theme/`); sizes belong in `JagFxDimens`; user-facing text belongs in `ui/i18n/` (`UiString` plus one `Strings*.kt` per language).
- DSP field names follow `docs/synth-format-spec.md`: `startValue`, `endValue`, `duration`, `targetLevel`, and related binary-format names.

## Required behavior

- Do not revert user changes unless the user explicitly asks.
- Do not push. The user pushes.
- Do not commit unless the user explicitly asks.
- Before committing, stage only intended files and use a Conventional Commit message.
- Keep binary round-trip fidelity for files that currently round-trip cleanly.
- Preserve known JSON test behavior: files with existing binary asymmetries compare against `SynthCodec.encode(SynthCodec.decode(bytes).patch)`, not raw original bytes.
- Do not commit secrets, `.env` files, or local credential material.
- Ask before adding Gradle or npm dependencies. Explain why existing APIs are not enough. After a Gradle dependency change, run `./gradlew --write-verification-metadata sha256 help`; never use lenient verification.
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
- Variable-length integers use `USmart16` and `Smart16`, read and written in `SynthDecoder` and `SynthEncoder` (constants in `FormatConstants.kt`).
- The filter field always follows `Offset`, as in the game client (spec §6.7); `0` means no filter.
- Partial amplitude `0` terminates the partial list in binary.

## References

- `docs/synth-format-spec.md`: binary `.synth` format and synthesis semantics.
- `schemas/synth.schema.json`: JSON interchange schema.
- `references/synths/`: real `.synth` files used for tests.
- `references/json/`: JSON generated from every reference `.synth` file.
- `llms.txt`: project overview for LLM readers.
- `PUBLISHING.md`: release and signing steps.

## Tooling

- The version lives in `gradle.properties` (`version=`). `scripts/bump-version.sh <version>` updates it and `packages/*/package.json` once `CHANGELOG.md` has the entry.
- Dependency versions live in `gradle/libs.versions.toml`; checksums live in `gradle/verification-metadata.xml`.
- CI lives in `.github/workflows/ci.yml`.
- Release packaging lives in `.github/workflows/release.yml` and `justfile`.
- CodeQL lives in `.github/workflows/codeql.yml`.
- Dependabot tracks Gradle, Bun, and GitHub Actions dependencies.
- Spotless (ktlint) and detekt (`config/detekt/detekt.yml`) enforce Kotlin style; Biome checks JavaScript and JSON.
- `@commitlint/cli` and `husky` install the commit-msg hook through `bun install`.
