# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed

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
