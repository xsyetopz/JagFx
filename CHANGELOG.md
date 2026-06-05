# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.4.0]

### Added

- **Localized desktop UI** -- added localized UI string resources for English, German, Spanish, French, Italian, and Brazilian Portuguese, plus translation markup extensions for normal and uppercase text.
- **Analysis graph renderer** -- added shared analysis-grid rendering for Pole/Zero and Frequency Response graphs, with a separate transition-curve grid for the Filter graph.
- **Pole/Zero graph controls** -- added 1X/2X/4X zoom and snap-to-grid support to the Pole/Zero graph.
- **Filter frequency scale** -- added a shared logarithmic musical-frequency scale for filter analysis views.
- **Loop count display conversion** -- added converter support for showing infinite loop count as `0`.
- **Justfile workflow** -- added `just` recipes for run, build, test, CSharpier format/check, desktop publish, desktop installer packaging, CLI publish, and compatibility aliases.

### Changed

- **Version** -- bumped JagFx packages and assemblies to 2.4.0.
- **Desktop dependencies** -- updated Avalonia packages to 12.0.4 and added Optris Material Design icon packages.
- **Editor visual system** -- refreshed the desktop editor with Okabe-Ito-derived color tokens, consistent square toolbar buttons, larger icon rendering, stronger disabled-state treatment, and shared theme resources.
- **Signal-chain matrix** -- changed matrix cell labels to short descriptive names, uppercase zoom labels (`1X`, `2X`, `4X`), pixel-snapped canvas drawing, consistent selection borders, and removed unused mode-dropdown controls.
- **Filter inspection** -- replaced raw angle/magnitude editing fields with knob controls, shortened common labels (`DUR`, `OFS`, `LVL`, `PIT`, `VOL`, `DEL`, `FBK`), and tightened sidebar layout.
- **Partials and Echo footer** -- aligned Echo with the OSC card style, kept OSC cards visually inactive when toggled off, and centered footer knob layouts by knob count.
- **Header and loop controls** -- unified text-field sizing and changed loop input labelling to use icon-based loop-in/loop-out affordances.
- **True Wave preview** -- changed preview rendering to queue immediate updates during interactive edits and cache per-voice preview renders for faster live waveform feedback.
- **Pole/Zero representation** -- changed Pole/Zero from a unit-circle view to the same logarithmic frequency ruler used by Frequency Response, with pole/zero positions derived from filter phase and magnitude data.
- **Frequency Response rendering** -- moved the graph onto the shared analysis grid and pixel-snapped response traces.
- **Graph drawing** -- disabled anti-aliasing on matrix envelope/analysis canvases for pixel-perfect graph lines.
- **Native rendering** -- updated native rendering output to include the current version metadata and synthesis/rendering adjustments.
- **Project layout** -- moved JavaRandom and SmartInt source files out of nested `src` folders and adjusted project references/tests.
- **Documentation** -- revised README, CONTRIBUTING, PUBLISHING, synth-format spec, and agent guidance text.

### Fixed

- **macOS file picker focus** -- serialized native file dialogs and reactivated the owner window before opening pickers to avoid greyed-out, unclickable dialogs.
- **Analyzer compliance** -- resolved analyzer, formatting, async, switch exhaustiveness, unused member, and naming issues without suppressions.
- **CI formatting** -- switched CI formatting verification from `dotnet format` to the repo's CSharpier tool workflow.
- **Whitespace and schema formatting** -- normalized formatting across JSON examples, schema files, CLI commands, tests, and helper libraries.
- **Audio rendering** -- tightened clipping, buffer pooling, waveform table, voice synthesis, and filter processing paths used by preview/playback.

### Removed

- **Status bar** -- removed the desktop status bar and knob hint plumbing from the main window.
- **Legacy Makefile** -- removed the Makefile in favor of the new `justfile`.
- **Old custom icon theme file** -- removed the local `Icons.axaml` resource in favor of Optris icons.
- **Old screenshot asset** -- replaced `370_cow_death.png` with `relic_unlock_spinning_hover.png`.

## [2.2.2]

### Added

- **Modern app icon** -- added a waveform-first JagFx icon in SVG, PNG, ICO, and ICNS formats, then wired it into the desktop window, Windows installer, macOS app bundle, and Linux AppImage metadata.
- **Publishing guide** -- added `PUBLISHING.md` with host-OS-specific release steps, GitHub secret setup, signing/notarization notes, and troubleshooting for macOS, Windows, and Linux.

### Changed

- **Cross-platform release packaging** -- replaced archive-only desktop release targets with installer targets for macOS DMG, Windows Inno Setup EXE, Linux AppImage/tar fallback, plus separate CLI artifacts.
- **Self-contained single-file publishing** -- desktop and CLI RID publishes now embed runtime, content, and native dependencies into self-contained single-file outputs.
- **macOS app bundle metadata** -- macOS bundles now include the app icon and `CFBundleIconFile` metadata.

### Fixed

- **macOS DMG creation** -- fixed false `No space left on device` failures by using deterministic APFS DMG sizing, controlled local temporary output, stale mount cleanup, and retry headroom.
- **Icon raster generation** -- added `tools/release/render-icon.sh` so raster icons match the SVG source and preserve transparent corners instead of rendering with a white background.

## [2.2.0]

### Added

- **DAW-style envelope editing** -- double-click to insert breakpoints, right-click context menu (Delete Point / Add Point Here / Add Point at End), Delete/Backspace key to remove selected point, cursor feedback (Hand over breakpoints, Cross over lines), visual selection indicator
- **Knob double-click-to-reset** -- `DefaultValue` property on `KnobControl`; double-click resets to default (or minimum if unset)
- **Partial active toggle** -- checkbox in partials footer header to enable/disable individual partials

### Changed

- **Darker UI theme** -- all surface colors shifted darker (`SurfaceBase` #333->#1a1a, `SurfaceCell` #404040->#2a2a2a, etc.), subtler borders, brighter text (#f0f0f0->#ffffff)
- **Knob visuals** -- darker background arc (#505050->#333333), darker fill (#4a4a4a->#1a1a1a), brighter labels (#888->#aaa); re-renders on `IsEnabled` change
- **Waveform center line** -- drawn before clip region so it's visible even with no samples
- **Partials footer** -- knobs gain `DefaultValue` bindings (PIT=0, VOL=100, DLY=0)

### Removed

- `PartialsMatrix.axaml` / `.axaml.cs` (unused inspector view)

## [2.1.0]

### Added

- **JSON interchange format** -- a human-readable JSON representation of `.synth` patches for programmatic creation and inspection. Includes `SynthJsonModels` (DTOs), `SynthJsonMapper` (domain ↔ DTO), and `SynthJsonSerializer` (public API) in `JagFx.Io.Json`. Uses string waveform names (`"sine"`, `"square"`, etc.), raw Q16 integers for envelope values (round-trip safe), and structured filter coefficients (`feedforward`/`feedback` × `baseline`/`modulated`). Optional fields default to sensible values when omitted.

- **CLI `to-json` command** -- `jagfx to-json <input.synth> [output.json]` converts a binary `.synth` file to the JSON interchange format. Outputs to stdout when no output path is given.

- **CLI `from-json` command** -- `jagfx from-json <input.json> <output.synth>` converts a JSON patch back to binary `.synth` format.

- **JSON Schema** -- `specs/synth.schema.json` (JSON Schema draft 2020-12) documents the interchange format with type constraints, required fields, and descriptions.

- **Example JSON patches** -- `specs/examples/minimal.json` (hand-written single-voice, required fields only) and `specs/examples/cow_death.json` (generated via `to-json`).

- **`llms.txt`** -- project description following the [llms.txt standard](https://llmstxt.org/) for LLM consumption, covering the project, `.synth` format, JSON interchange, CLI usage, and project structure.

- **`CLAUDE.md`** -- project-specific instructions for Claude Code with architecture, conventions, build commands, and constraints.

- **`AGENTS.md`** -- agent-oriented project context for AI coding agents with build/test commands, project layout, key patterns, and rules.

- **JSON round-trip tests** -- parametrized xUnit tests covering all 15 reference `.synth` files (binary -> JSON -> binary), minimal JSON with defaults, filter coefficient mapping, waveform string serialization, and empty patch handling.

- **Status bar** -- a thin bar at the bottom of the main window shows live knob hints (label + current value + unit) as the user hovers or drags any `KnobControl`, and clears when the pointer leaves. `KnobControl.HintChanged` is a static event wired into `MainViewModel.StatusHint`.

- **Envelope canvas scroll/pan** -- `EnvelopeCanvas` and `WaveformCanvas` both support scroll-offset panning when zoomed in. Drag on the canvas background (not a breakpoint) to pan; scroll offset resets to 0 when returning to 1× zoom. Grid lines tile correctly with the scroll offset so vertical lines stay aligned.

- **Mouse-wheel zoom** -- scrolling the mouse wheel over any `EnvelopeCanvas` or `WaveformCanvas` cycles through zoom levels 1×/2×/4×. Zoom-toggle buttons in the signal chain header stay in sync via a `PropertyChanged` handler.

- **Loop count control** -- a `CNT` numeric field (0 = infinite) and `BGN`/`END` loop-point fields have been added to the header bar next to the `LOOP` button. Synthesis passes `EffectiveLoopCount` (1 when not looping, `LoopCount` or 50 when looping with 0) to `SynthesisService.RenderAsync`.

- **Seamless loop replay** -- `AudioPlaybackService.ReplayFromExistingFile()` restarts the existing temp WAV without re-encoding, enabling low-latency looping for `LoopCount == 0`.

- **Bank navigation arrows** -- the partials footer replaces the bank toggle buttons with `◀`/`▶` arrow buttons that cycle the `PartialBankOffset` by 5 per click (clamped to 0–5).

- **`ReplayFromExistingFile` on `AudioPlaybackService`** -- kills any running process and restarts from the cached temp WAV path, reattaching the `PlaybackFinished` callback.

### Changed

- **Colorblind-friendly palette** -- all accent colors updated from #44BB77 (green) to #009E73 (Okabe-Ito teal) and from #8888d4/`#ffaa44` (purple/orange) to #0072B2/#E69F00 (Okabe-Ito blue/yellow). Updated across `ThemeColors.cs`, `Colors.axaml`, `Controls.axaml`, and all hard-coded hex literals in `SignalChainPanel`, `MainViewModel`, and `PartialsFooter`.

- **KnobControl redesign** -- reduced default measure size from 52×60 to 44×32. Start angle changed from 225° to 135° (knob opens upward). Value text removed; only the label abbreviation is drawn in the dead zone. Dimmed arc uses `#222222` instead of `#1a3344`.

- **Knob heights normalised** -- all `KnobControl` elements in `EnvelopeInspector`, `FilterSection`, `InspectorPanel`, and `PartialsFooter` changed from `Height="56"` / `Height="50"` to `Height="32"`.

- **LOOP / TRUE buttons moved** -- `LOOP` and `TRUE` buttons removed from the partials footer; `LOOP` is now in the header bar with its associated `CNT`/`BGN`/`END` fields. `TrueWaveEnabled` toggle was removed from the footer entirely (button was non-functional placeholder).

- **Loop section removed from inspector** -- the `LOOP BEGIN`/`END` numeric fields in `InspectorPanel` have been removed; they are now accessible via the header bar.

- **Partial cell titles** -- changed from `P{0}` to `PARTIAL {0}` (e.g. "PARTIAL 1" through "PARTIAL 5"). Cell header now uses a `SurfaceCellHeaderBrush` background `Border` matching the ECHO cell style.

- **PARTIALS center label removed** -- the redundant "PARTIALS" `TextBlock` in the footer header bar has been removed.

- **Filter mode layout** -- filter column cells now have a fixed `Width=300` and the grid is centered horizontally in filter-only mode; NaN width is restored when switching to Main or Both modes.

- **Signal chain: zoom buttons hidden for PoleZero/Bode** -- `CreateZoomGroup` is now skipped for `SlotType.PoleZero` and `SlotType.Bode` canvases (those controls had no zoom support).

- **NumericUpDown sizing** -- global styles in `JagFxTheme.axaml` set `Height=20`, `MinHeight=0`, `Padding=4,0` on `NumericUpDown` and strip the Fluent inner `Border` chrome to eliminate excess vertical padding. `InspectorPanel` numeric widths narrowed from 80 to 64.

- **Notarize script hardening** -- `notarize-macos.sh` now: extracts the certificate SHA1 hash and passes it to all `codesign` calls (avoids ambiguous identity name matching); adds an error trap printing the failing line number; checks for the Apple intermediate "Developer ID Certification Authority" certificate; removes the interactive keychain-unlock block (was incompatible with CI); moves the `STAGING_DIR` cleanup trap to immediately after it is created; removes the `_sign` wrapper that suppressed identity output.

- **JagVoiceButton visual tiers** -- inactive voices (no data) are now dimmed at 50% opacity; active voices (have data, not selected) show default button colors with no green tint; selected voice keeps accent border + green text. All button themes (`JagVoiceButton`, `JagToolButton`, `JagCellButton`, `JagCellToggle`) now share the same `{DynamicResource ...}` color tokens.

- **EnvelopeInspector waveform buttons removed** -- the --/⊓/∿/⟋/⁂ waveform selector row was redundant with the signal chain MODE dropdown and has been removed. Waveform selection is done exclusively via the MODE dropdown on each signal chain cell. Inspector padding updated to `6,6` for consistent spacing.

- **TRUE wave render reliability** -- the debounce timer's fire-and-forget async callback now catches and logs unhandled exceptions, preventing silent render failures when editing envelope knobs.

- **DSP-accurate naming refactor** -- renamed identifiers across the full solution to match what the code actually does. No behaviour changes.

  | Old name                               | New name                              | Reason                                                    |
  | -------------------------------------- | ------------------------------------- | --------------------------------------------------------- |
  | `Segment.DurationSamples`              | `Segment.Duration`                    | Value is relative ticks, not samples                      |
  | `Segment.PeakLevel`                    | `Segment.TargetLevel`                 | It is a breakpoint target, not a peak                     |
  | `Envelope.StartSample`                 | `Envelope.StartValue`                 | Value is generic (not always samples)                     |
  | `Envelope.EndSample`                   | `Envelope.EndValue`                   | Value is generic (not always samples)                     |
  | `Voice.DurationSamples`                | `Voice.DurationMs`                    | Value is milliseconds                                     |
  | `Voice.StartSample`                    | `Voice.OffsetMs`                      | Value is a time offset in milliseconds                    |
  | `Voice.GateSilenceEnvelope`            | `Voice.GapOffEnvelope`                | Controls gate-off period                                  |
  | `Voice.GateDurationEnvelope`           | `Voice.GapOnEnvelope`                 | Controls gate-on period                                   |
  | `LoopSegment.BeginSample`              | `LoopSegment.BeginMs`                 | Value is milliseconds                                     |
  | `LoopSegment.EndSample`                | `LoopSegment.EndMs`                   | Value is milliseconds                                     |
  | `LowFrequencyOscillator.FrequencyRate` | `LowFrequencyOscillator.RateEnvelope` | Carries waveform + rate; "Frequency" was redundant        |
  | `Filter.CutoffEnvelope`                | `Filter.ModulationEnvelope`           | Modulates coefficients, not just cutoff                   |
  | `Echo.MixPercent`                      | `Echo.FeedbackPercent`                | Controls feedback/wet amount                              |
  | `PatchMixer`                           | `PatchRenderer`                       | Class synthesises and sums voices; "Mixer" was misleading |

  UI labels updated to match:

  | Old label                 | New label             |
  | ------------------------- | --------------------- |
  | `G.SIL` / `Gate Silence`  | `GAP OFF` / `Gap Off` |
  | `G.DUR` / `Gate Duration` | `GAP ON` / `Gap On`   |
  | `STO` (header bar)        | `OFS`                 |
  | `MIX` (echo section)      | `FDBK`                |
