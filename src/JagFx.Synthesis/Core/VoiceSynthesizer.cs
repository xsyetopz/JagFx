using System.Buffers;
using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Audio;
using JagFx.Synthesis.Processing;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    public static AudioBuffer Synthesize(Voice voice, CancellationToken ct = default)
    {
        using var pooled = SynthesizePooled(voice, ct);
        return pooled.ToAudioBuffer();
    }

    public static PooledAudioBuffer SynthesizePooled(Voice voice, CancellationToken ct = default)
    {
        var pooled = SynthesizePooledCore(voice, ct);
        return new PooledAudioBuffer(pooled.Samples, pooled.Length);
    }

    internal static PooledVoiceBuffer SynthesizePooledCore(
        Voice voice,
        CancellationToken ct = default
    )
    {
        ct.ThrowIfCancellationRequested();
        var sampleCount = (int)(voice.DurationMs * AudioConstants.SampleRatePerMillisecond);
        if (sampleCount <= 0 || voice.DurationMs < AudioConstants.MinDurationMs)
        {
            return new PooledVoiceBuffer([], 0);
        }

        var samplesPerStep = sampleCount / (double)voice.DurationMs;
        var buffer = AudioBufferPool.Acquire(sampleCount);
        Span<int> partialDelays = stackalloc int[AudioConstants.MaxOscillators];
        Span<int> partialVolumes = stackalloc int[AudioConstants.MaxOscillators];
        Span<int> partialSemitones = stackalloc int[AudioConstants.MaxOscillators];
        Span<int> partialStarts = stackalloc int[AudioConstants.MaxOscillators];

        var state = CreateVoiceRenderState(
            voice,
            samplesPerStep,
            partialDelays,
            partialVolumes,
            partialSemitones,
            partialStarts
        );

        try
        {
            byte[]? gateMaskBuffer = null;
            try
            {
                if (voice.GapOffEnvelope != null && voice.GapOnEnvelope != null)
                {
                    gateMaskBuffer = ArrayPool<byte>.Shared.Rent(sampleCount);
                    FillGateMask(gateMaskBuffer, voice, sampleCount, ct);
                    RenderGatedSamples(
                        buffer,
                        voice,
                        state,
                        partialDelays,
                        partialVolumes,
                        partialSemitones,
                        partialStarts,
                        gateMaskBuffer.AsSpan(0, sampleCount),
                        sampleCount,
                        ct
                    );
                }
                else
                {
                    RenderSamples(
                        buffer,
                        voice,
                        state,
                        partialDelays,
                        partialVolumes,
                        partialSemitones,
                        partialStarts,
                        sampleCount,
                        ct
                    );
                }
            }
            finally
            {
                if (gateMaskBuffer != null)
                {
                    ArrayPool<byte>.Shared.Return(gateMaskBuffer);
                }
            }

            ct.ThrowIfCancellationRequested();
            ApplyEcho(buffer, voice, samplesPerStep, sampleCount, ct);

            if (voice.Filter != null)
            {
                ct.ThrowIfCancellationRequested();
                AudioFilter.Apply(buffer, voice.Filter, state.FilterEnvelopeEval, sampleCount, ct);
            }

            ct.ThrowIfCancellationRequested();
            AudioMath.ClipInt16(buffer, sampleCount);

            return new PooledVoiceBuffer(buffer, sampleCount);
        }
        finally
        {
            ReleaseVoiceRenderState(state);
        }
    }

    private static VoiceRenderState CreateVoiceRenderState(
        Voice voice,
        double samplesPerStep,
        Span<int> partialDelays,
        Span<int> partialVolumes,
        Span<int> partialSemitones,
        Span<int> partialStarts
    )
    {
        var freqBaseEval = EnvelopeGenerator.Rent(voice.FrequencyEnvelope);
        var ampBaseEval = EnvelopeGenerator.Rent(voice.AmplitudeEnvelope);

        var (freqModRateEval, freqModRangeEval, vibratoStep, vibratoBase) = CreateFrequencyEnvelope(
            voice,
            samplesPerStep
        );
        var (ampModRateEval, amplModRangeEval, tremoloStep, tremoloBase) = CreateAmplitudeEnvelope(
            voice,
            samplesPerStep
        );

        var partialCount = CreatePartials(
            voice,
            samplesPerStep,
            partialDelays,
            partialVolumes,
            partialSemitones,
            partialStarts
        );

        EnvelopeGenerator? filterEnvelopeEval = null;
        if (voice.Filter != null && voice.Filter.ModulationEnvelope != null)
        {
            filterEnvelopeEval = EnvelopeGenerator.Rent(voice.Filter.ModulationEnvelope);
        }

        return new VoiceRenderState
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
            PartialCount = partialCount,
            FilterEnvelopeEval = filterEnvelopeEval,
        };
    }

    private static void ReleaseVoiceRenderState(VoiceRenderState state)
    {
        EnvelopeGenerator.Return(state.FrequencyBaseEval);
        EnvelopeGenerator.Return(state.AmplitudeBaseEval);
        EnvelopeGenerator.Return(state.FrequencyModulationRateEval);
        EnvelopeGenerator.Return(state.FrequencyModulationRangeEval);
        EnvelopeGenerator.Return(state.AmplitudeModulationRateEval);
        EnvelopeGenerator.Return(state.AmplitudeModulationRangeEval);
        EnvelopeGenerator.Return(state.FilterEnvelopeEval);
    }

    private static (
        EnvelopeGenerator? rateEval,
        EnvelopeGenerator? rangeEval,
        int step,
        int baseValue
    ) CreateFrequencyEnvelope(Voice voice, double samplesPerStep) =>
        CreateModulationEnvelope(voice.PitchLfo, samplesPerStep);

    private static (
        EnvelopeGenerator? rateEval,
        EnvelopeGenerator? rangeEval,
        int step,
        int baseValue
    ) CreateAmplitudeEnvelope(Voice voice, double samplesPerStep) =>
        CreateModulationEnvelope(voice.AmplitudeLfo, samplesPerStep);

    private static (
        EnvelopeGenerator? rateEval,
        EnvelopeGenerator? rangeEval,
        int step,
        int baseValue
    ) CreateModulationEnvelope(LowFrequencyOscillator? lfo, double samplesPerStep)
    {
        if (lfo != null)
        {
            var rateEval = EnvelopeGenerator.Rent(lfo.RateEnvelope);
            var rangeEval = EnvelopeGenerator.Rent(lfo.ModulationDepth);

            var step = (int)(
                (lfo.RateEnvelope.EndValue - lfo.RateEnvelope.StartValue)
                * AudioConstants.PhaseScale
                / samplesPerStep
            );
            var baseValue = (int)(
                lfo.RateEnvelope.StartValue * AudioConstants.PhaseScale / samplesPerStep
            );

            return (rateEval, rangeEval, step, baseValue);
        }

        return (null, null, 0, 0);
    }

    private static int CreatePartials(
        Voice voice,
        double samplesPerStep,
        Span<int> delays,
        Span<int> volumes,
        Span<int> semitones,
        Span<int> starts
    )
    {
        var partialCount =
            voice.Partials.Count < AudioConstants.MaxOscillators
                ? voice.Partials.Count
                : AudioConstants.MaxOscillators;
        var activePartial = 0;
        var frequencySpan = voice.FrequencyEnvelope.EndValue - voice.FrequencyEnvelope.StartValue;
        var frequencyStart = (int)(
            voice.FrequencyEnvelope.StartValue * AudioConstants.PhaseScale / samplesPerStep
        );

        for (var partial = 0; partial < partialCount; partial++)
        {
            var height = voice.Partials[partial];
            if (height.Amplitude.Value == 0)
            {
                continue;
            }

            delays[activePartial] = (int)(height.Delay.Value * samplesPerStep);
            volumes[activePartial] = (height.Amplitude.Value << 14) / 100;
            semitones[activePartial] = (int)(
                frequencySpan
                * AudioConstants.PhaseScale
                * WaveformTables.GetPitchMultiplier(height.PitchOffsetSemitones)
                / samplesPerStep
            );
            starts[activePartial] = frequencyStart;
            activePartial++;
        }

        SortPartialsByDelay(activePartial, delays, volumes, semitones, starts);
        return activePartial;
    }

    private static void SortPartialsByDelay(
        int partialCount,
        Span<int> delays,
        Span<int> volumes,
        Span<int> semitones,
        Span<int> starts
    )
    {
        for (var i = 1; i < partialCount; i++)
        {
            var delay = delays[i];
            var volume = volumes[i];
            var semitone = semitones[i];
            var start = starts[i];
            var j = i - 1;

            while (j >= 0 && delays[j] > delay)
            {
                delays[j + 1] = delays[j];
                volumes[j + 1] = volumes[j];
                semitones[j + 1] = semitones[j];
                starts[j + 1] = starts[j];
                j--;
            }

            delays[j + 1] = delay;
            volumes[j + 1] = volume;
            semitones[j + 1] = semitone;
            starts[j + 1] = start;
        }
    }

    private static void FillGateMask(
        byte[] gateMask,
        Voice voice,
        int sampleCount,
        CancellationToken ct
    )
    {
        var silenceEval = EnvelopeGenerator.Rent(voice.GapOffEnvelope!);
        var durationEval = EnvelopeGenerator.Rent(voice.GapOnEnvelope!);

        try
        {
            var counter = 0;
            var muted = true;

            for (var sample = 0; sample < sampleCount; sample++)
            {
                if ((sample & 0xFF) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                var stepOn = silenceEval.Evaluate(sampleCount);
                var stepOff = durationEval.Evaluate(sampleCount);
                var threshold = muted
                    ? voice.GapOffEnvelope!.StartValue
                        + (
                            (voice.GapOffEnvelope.EndValue - voice.GapOffEnvelope.StartValue)
                                * stepOn
                            >> 8
                        )
                    : voice.GapOffEnvelope!.StartValue
                        + (
                            (voice.GapOffEnvelope.EndValue - voice.GapOffEnvelope.StartValue)
                                * stepOff
                            >> 8
                        );

                counter += 256;
                if (counter >= threshold)
                {
                    counter = 0;
                    muted = !muted;
                }

                gateMask[sample] = muted ? (byte)0 : (byte)1;
            }
        }
        finally
        {
            EnvelopeGenerator.Return(durationEval);
            EnvelopeGenerator.Return(silenceEval);
        }
    }
}
