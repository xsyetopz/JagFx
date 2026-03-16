using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Data;
using JagFx.Synthesis.Processing;

namespace JagFx.Synthesis.Core;

public static class VoiceSynthesizer
{
    public static AudioBuffer Synthesize(Voice voice)
    {
        var sampleCount = (int)(voice.DurationMs * AudioConstants.SampleRatePerMillisecond);
        if (sampleCount <= 0 || voice.DurationMs < AudioConstants.MinDurationMs)
        {
            return AudioBuffer.Empty(0);
        }

        var samplesPerStep = sampleCount / (double)voice.DurationMs;
        var buffer = AudioBufferPool.Acquire(sampleCount);

        var state = CreateSynthesisState(voice, samplesPerStep);
        RenderSamples(buffer, voice, state, sampleCount);

        ApplyGating(buffer, voice, sampleCount);
        ApplyEcho(buffer, voice, samplesPerStep, sampleCount);

        if (voice.Filter != null)
        {
            AudioFilter.Apply(buffer, voice.Filter, state.FilterEnvelopeEval, sampleCount);
        }

        AudioMath.ClipInt16(buffer, sampleCount);

        var output = new int[sampleCount];
        Array.Copy(buffer, 0, output, 0, sampleCount);

        AudioBufferPool.Release(buffer);
        return new AudioBuffer(output, AudioConstants.SampleRate);
    }

    private static SynthesisState CreateSynthesisState(Voice voice, double samplesPerStep)
    {
        var freqBaseEval = new EnvelopeGenerator(voice.FrequencyEnvelope);
        var ampBaseEval = new EnvelopeGenerator(voice.AmplitudeEnvelope);
        freqBaseEval.Reset();
        ampBaseEval.Reset();

        var (freqModRateEval, freqModRangeEval, vibratoStep, vibratoBase) =
            CreateFrequencyEnvelope(voice, samplesPerStep);
        var (ampModRateEval, amplModRangeEval, tremoloStep, tremoloBase) =
            CreateAmplitudeEnvelope(voice, samplesPerStep);

        var (delays, volumes, semitones, starts) =
            CreatePartials(voice, samplesPerStep);

        EnvelopeGenerator? filterEnvelopeEval = null;
        if (voice.Filter != null && voice.Filter.ModulationEnvelope != null)
        {
            filterEnvelopeEval = new EnvelopeGenerator(voice.Filter.ModulationEnvelope);
            filterEnvelopeEval.Reset();
        }

        return new SynthesisState
        {
            FrequencyBaseEval = freqBaseEval,
            AmplitudeBaseEval = ampBaseEval,
            FrequencyModulationRateEval = freqModRateEval,
            FrequencyModulationRangeEval = freqModRangeEval,
            AmplitudeModulationRateEval = ampModRateEval,
            AmplitudeModulationRangeEval = amplModRangeEval,
            FrequencyStep = vibratoStep,
            FrequencyBase = vibratoBase,
            AmplitudeStep = tremoloStep,
            AmplitudeBase = tremoloBase,
            PartialDelays = delays,
            PartialVolumes = volumes,
            PartialSemitones = semitones,
            PartialStarts = starts,
            FilterEnvelopeEval = filterEnvelopeEval
        };
    }

    private static (EnvelopeGenerator? rateEval, EnvelopeGenerator? rangeEval, int step, int baseValue)
        CreateFrequencyEnvelope(Voice voice, double samplesPerStep)
        => CreateModulationEnvelope(voice.PitchLfo, samplesPerStep);

    private static (EnvelopeGenerator? rateEval, EnvelopeGenerator? rangeEval, int step, int baseValue)
        CreateAmplitudeEnvelope(Voice voice, double samplesPerStep)
        => CreateModulationEnvelope(voice.AmplitudeLfo, samplesPerStep);

    private static (EnvelopeGenerator? rateEval, EnvelopeGenerator? rangeEval, int step, int baseValue)
        CreateModulationEnvelope(LowFrequencyOscillator? lfo, double samplesPerStep)
    {
        if (lfo != null)
        {
            var rateEval = new EnvelopeGenerator(lfo.RateEnvelope);
            var rangeEval = new EnvelopeGenerator(lfo.ModulationDepth);
            rateEval.Reset();
            rangeEval.Reset();

            var step = (int)((lfo.RateEnvelope.EndValue - lfo.RateEnvelope.StartValue) * AudioConstants.PhaseScale / samplesPerStep);
            var baseValue = (int)(lfo.RateEnvelope.StartValue * AudioConstants.PhaseScale / samplesPerStep);

            return (rateEval, rangeEval, step, baseValue);
        }

        return (null, null, 0, 0);
    }

    private static (int[] delays, int[] volumes, int[] semitones, int[] starts)
        CreatePartials(Voice voice, double samplesPerStep)
    {
        var delays = new int[AudioConstants.MaxOscillators];
        var volumes = new int[AudioConstants.MaxOscillators];
        var semitones = new int[AudioConstants.MaxOscillators];
        var starts = new int[AudioConstants.MaxOscillators];

        var partialCount = Math.Min(AudioConstants.MaxOscillators, voice.Partials.Count);
        for (var partial = 0; partial < partialCount; partial++)
        {
            var height = voice.Partials[partial];
            if (height.Amplitude.Value != 0)
            {
                delays[partial] = (int)(height.Delay.Value * samplesPerStep);
                volumes[partial] = (height.Amplitude.Value << 14) / 100;
                semitones[partial] = (int)(
                    (voice.FrequencyEnvelope.EndValue - voice.FrequencyEnvelope.StartValue) * AudioConstants.PhaseScale *
                    WaveformTables.GetPitchMultiplier(height.PitchOffsetSemitones) / samplesPerStep);
                starts[partial] = (int)(voice.FrequencyEnvelope.StartValue * AudioConstants.PhaseScale / samplesPerStep);

                if (partial == 0)
                {
                    _ = WaveformTables.GetPitchMultiplier(height.PitchOffsetSemitones);
                }
                else if (partial == 1)
                {
                    _ = WaveformTables.GetPitchMultiplier(height.PitchOffsetSemitones);
                }
            }
        }

        return (delays, volumes, semitones, starts);
    }

    private static void RenderSamples(
        int[] buffer,
        Voice voice,
        SynthesisState state,
        int sampleCount)
    {
        var phases = new int[AudioConstants.MaxOscillators];
        var frequencyPhase = 0;
        var amplitudePhase = 0;

        for (var sample = 0; sample < sampleCount; sample++)
        {
            var frequency = state.FrequencyBaseEval.Evaluate(sampleCount);
            var amplitude = state.AmplitudeBaseEval.Evaluate(sampleCount);

            (frequency, frequencyPhase) = ApplyVibrato(frequency, frequencyPhase, sampleCount, state, voice);
            (amplitude, amplitudePhase) = ApplyTremolo(amplitude, amplitudePhase, sampleCount, state, voice);

            RenderPartials(
                buffer,
                voice,
                state,
                sample,
                sampleCount,
                frequency,
                amplitude,
                phases);
        }
    }

    private static void RenderPartials(
        int[] buffer,
        Voice voice,
        SynthesisState state,
        int sample,
        int sampleCount,
        int frequency,
        int amplitude,
        int[] phases)
    {
        var partialCount = Math.Min(AudioConstants.MaxOscillators, voice.Partials.Count);
        for (var partial = 0; partial < partialCount; partial++)
        {
            if (voice.Partials[partial].Amplitude.Value != 0)
            {
                var position = sample + state.PartialDelays[partial];
                if (position >= 0 && position < sampleCount)
                {
                    var sampleValue = GenerateSample(
                        amplitude * state.PartialVolumes[partial] >> 15,
                        phases[partial],
                        voice.FrequencyEnvelope.Waveform);

                    buffer[position] += sampleValue;
                    phases[partial] += (frequency * state.PartialSemitones[partial] >> 16) +
                        state.PartialStarts[partial];
                }
            }
        }
    }

    private static (int frequency, int phase) ApplyVibrato(
        int frequency,
        int phase,
        int sampleCount,
        SynthesisState state,
        Voice voice)
    {
        if (state.FrequencyModulationRateEval == null || state.FrequencyModulationRangeEval == null)
            return (frequency, phase);

        var (mod, nextPhase) = EvaluateModulation(
            state.FrequencyModulationRateEval, state.FrequencyModulationRangeEval,
            state.FrequencyBase, state.FrequencyStep,
            phase, sampleCount, voice.PitchLfo!.RateEnvelope.Waveform);

        return (frequency + mod, nextPhase);
    }

    private static (int amplitude, int phase) ApplyTremolo(
        int amplitude,
        int phase,
        int sampleCount,
        SynthesisState state,
        Voice voice)
    {
        if (state.AmplitudeModulationRateEval == null || state.AmplitudeModulationRangeEval == null)
            return (amplitude, phase);

        var (mod, nextPhase) = EvaluateModulation(
            state.AmplitudeModulationRateEval, state.AmplitudeModulationRangeEval,
            state.AmplitudeBase, state.AmplitudeStep,
            phase, sampleCount, voice.AmplitudeLfo!.RateEnvelope.Waveform);

        return (amplitude * (mod + AudioConstants.FixedPoint.Offset) >> 15, nextPhase);
    }

    private static (int mod, int nextPhase) EvaluateModulation(
        EnvelopeGenerator rateEval,
        EnvelopeGenerator rangeEval,
        int baseValue,
        int step,
        int phase,
        int sampleCount,
        Waveform waveform)
    {
        var rate = rateEval.Evaluate(sampleCount);
        var range = rangeEval.Evaluate(sampleCount);
        var mod = GenerateSample(range, phase, waveform) >> 1;
        var nextPhase = phase + baseValue + (rate * step >> 16);
        return (mod, nextPhase);
    }

    private static int GenerateSample(int amplitude, int phase, Waveform waveform)
    {
        return waveform switch
        {
            Waveform.Square => (phase & AudioConstants.PhaseMask) < AudioConstants.FixedPoint.Quarter ? amplitude : -amplitude,
            Waveform.Sine => (WaveformTables.SineWaveTable[phase & AudioConstants.PhaseMask] * amplitude) >> 14,
            Waveform.Saw => (((phase & AudioConstants.PhaseMask) * amplitude) >> 14) - amplitude,
            Waveform.Noise => WaveformTables.NoiseTable[(phase / AudioConstants.NoisePhaseDiv) & AudioConstants.PhaseMask] * amplitude,
            Waveform.Off => 0,
            _ => 0
        };
    }

    private static void ApplyGating(int[] buffer, Voice voice, int sampleCount)
    {
        if (voice.GapOffEnvelope != null && voice.GapOnEnvelope != null)
        {
            var silenceEval = new EnvelopeGenerator(voice.GapOffEnvelope);
            var durationEval = new EnvelopeGenerator(voice.GapOnEnvelope);
            silenceEval.Reset();
            durationEval.Reset();

            var counter = 0;
            var muted = true;

            for (var sample = 0; sample < sampleCount; sample++)
            {
                var stepOn = silenceEval.Evaluate(sampleCount);
                var stepOff = durationEval.Evaluate(sampleCount);
                var threshold = muted
                    ? voice.GapOffEnvelope.StartValue + ((voice.GapOffEnvelope.EndValue - voice.GapOffEnvelope.StartValue) * stepOn >> 8)
                    : voice.GapOffEnvelope.StartValue + ((voice.GapOffEnvelope.EndValue - voice.GapOffEnvelope.StartValue) * stepOff >> 8);

                counter += 256;
                if (counter >= threshold)
                {
                    counter = 0;
                    muted = !muted;
                }

                if (muted)
                {
                    buffer[sample] = 0;
                }
            }
        }
    }

    private static void ApplyEcho(int[] buffer, Voice voice, double samplesPerStep, int sampleCount)
    {
        if (voice.Echo.DelayMilliseconds > 0 && voice.Echo.FeedbackPercent > 0)
        {
            var start = (int)(voice.Echo.DelayMilliseconds * samplesPerStep);
            for (var sample = start; sample < sampleCount; sample++)
            {
                buffer[sample] += buffer[sample - start] * voice.Echo.FeedbackPercent / 100;
            }
        }
    }

    private class SynthesisState
    {
        public required EnvelopeGenerator FrequencyBaseEval { get; init; }
        public required EnvelopeGenerator AmplitudeBaseEval { get; init; }
        public EnvelopeGenerator? FrequencyModulationRateEval { get; init; }
        public EnvelopeGenerator? FrequencyModulationRangeEval { get; init; }
        public EnvelopeGenerator? AmplitudeModulationRateEval { get; init; }
        public EnvelopeGenerator? AmplitudeModulationRangeEval { get; init; }
        public int FrequencyStep { get; init; }
        public int FrequencyBase { get; init; }
        public int AmplitudeStep { get; init; }
        public int AmplitudeBase { get; init; }
        public int[] PartialDelays { get; init; } = null!;
        public int[] PartialVolumes { get; init; } = null!;
        public int[] PartialSemitones { get; init; } = null!;
        public int[] PartialStarts { get; init; } = null!;
        public EnvelopeGenerator? FilterEnvelopeEval { get; init; }
    }
}
