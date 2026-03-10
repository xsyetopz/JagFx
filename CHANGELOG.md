# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

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

  | Old name | New name | Reason |
  |----------|----------|--------|
  | `Segment.DurationSamples` | `Segment.Duration` | Value is relative ticks, not samples |
  | `Segment.PeakLevel` | `Segment.TargetLevel` | It is a breakpoint target, not a peak |
  | `Envelope.StartSample` | `Envelope.StartValue` | Value is generic (not always samples) |
  | `Envelope.EndSample` | `Envelope.EndValue` | Value is generic (not always samples) |
  | `Voice.DurationSamples` | `Voice.DurationMs` | Value is milliseconds |
  | `Voice.StartSample` | `Voice.OffsetMs` | Value is a time offset in milliseconds |
  | `Voice.GateSilenceEnvelope` | `Voice.GapOffEnvelope` | Controls gate-off period |
  | `Voice.GateDurationEnvelope` | `Voice.GapOnEnvelope` | Controls gate-on period |
  | `LoopSegment.BeginSample` | `LoopSegment.BeginMs` | Value is milliseconds |
  | `LoopSegment.EndSample` | `LoopSegment.EndMs` | Value is milliseconds |
  | `LowFrequencyOscillator.FrequencyRate` | `LowFrequencyOscillator.RateEnvelope` | Carries waveform + rate; "Frequency" was redundant |
  | `Filter.CutoffEnvelope` | `Filter.ModulationEnvelope` | Modulates coefficients, not just cutoff |
  | `Echo.MixPercent` | `Echo.FeedbackPercent` | Controls feedback/wet amount |
  | `PatchMixer` | `PatchRenderer` | Class synthesises and sums voices; "Mixer" was misleading |

  UI labels updated to match:

  | Old label | New label |
  |-----------|-----------|
  | `G.SIL` / `Gate Silence` | `GAP OFF` / `Gap Off` |
  | `G.DUR` / `Gate Duration` | `GAP ON` / `Gap On` |
  | `STO` (header bar) | `OFS` |
  | `MIX` (echo section) | `FDBK` |
