using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Data;

namespace JagFx.Synthesis.Processing;

public static class AudioFilter
{
    private const int MaxCoefficients = 8;
    private const int ChunkSize = 128;

    public static void Apply(int[] buffer, Filter filter, EnvelopeGenerator? envelopeEval, int sampleCount)
    {
        if (filter.PoleCounts[0] == 0 && filter.PoleCounts[1] == 0)
        {
            return;
        }

        envelopeEval?.Reset();

        var tempBuffer = AudioBufferPool.Acquire(sampleCount);
        var inputCopySpan = tempBuffer.AsSpan();
        var outSpan = buffer.AsSpan();
        outSpan.CopyTo(inputCopySpan);

        var state = new FilterState(filter);
        var envelopeValue = envelopeEval?.Evaluate(sampleCount) ?? AudioConstants.FixedPoint.Scale;
        var envelopeFactor = envelopeValue / (float)AudioConstants.FixedPoint.Scale;

        var ffCount = state.ComputeCoefficients(0, envelopeFactor);
        var savedInverseA0 = state.InverseA0;
        var fbCount = state.ComputeCoefficients(1, envelopeFactor);
        state.InverseA0 = savedInverseA0;

        if (sampleCount < ffCount + fbCount)
        {
            AudioBufferPool.Release(tempBuffer);
            return;
        }

        ProcessInitialBlock(inputCopySpan, outSpan, state, envelopeEval, ffCount, fbCount, sampleCount);
        ProcessMainBlocks(inputCopySpan, outSpan, state, envelopeEval, sampleCount, ref ffCount, ref fbCount);
        ProcessFinalBlock(inputCopySpan, outSpan, state, envelopeEval, sampleCount, ffCount, fbCount);

        AudioBufferPool.Release(tempBuffer);
    }

    private static void ProcessInitialBlock(Span<int> inputCopySpan, Span<int> outSpan, FilterState state, EnvelopeGenerator? envelopeEval, int ffCount, int fbCount, int sampleCount)
    {
        ProcessSampleRange(inputCopySpan, outSpan, state, envelopeEval, 0, fbCount, ffCount, fbCount, sampleCount);
    }

    private static void ProcessMainBlocks(Span<int> inputCopySpan, Span<int> outSpan, FilterState state, EnvelopeGenerator? envelopeEval, int sampleCount, ref int ffCount, ref int fbCount)
    {
        int pos = fbCount;

        while (pos < sampleCount - ffCount)
        {
            int chunkEnd = Math.Min(pos + ChunkSize, sampleCount - ffCount);
            ProcessSampleRange(inputCopySpan, outSpan, state, envelopeEval, pos, chunkEnd, ffCount, fbCount, sampleCount);
            pos = chunkEnd;
        }
    }

    private static void ProcessFinalBlock(Span<int> inputCopySpan, Span<int> outSpan, FilterState state, EnvelopeGenerator? envelopeEval, int sampleCount, int ffCount, int fbCount)
    {
        ProcessSampleRange(inputCopySpan, outSpan, state, envelopeEval, sampleCount - ffCount, sampleCount, ffCount, fbCount, sampleCount, isFinal: true);
    }

    private static int ProcessSampleRange(Span<int> inputCopySpan, Span<int> outSpan, FilterState state,
        EnvelopeGenerator? envelopeEval, int start, int end, int ffCount, int fbCount, int sampleCount, bool isFinal = false)
    {
        int lastEnvelopeValue = AudioConstants.FixedPoint.Scale;
        for (var n = start; n < end; n++)
        {
            if (isFinal)
            {
                ApplyFilterToFinalSample(inputCopySpan, outSpan, state, n, ffCount, fbCount, sampleCount);
            }
            else
            {
                ApplyFilterToSample(inputCopySpan, outSpan, state, n, ffCount, fbCount);
            }
            lastEnvelopeValue = envelopeEval?.Evaluate(sampleCount) ?? AudioConstants.FixedPoint.Scale;

            if (n + 1 < sampleCount)
            {
                var envelopeFactor = lastEnvelopeValue / (float)AudioConstants.FixedPoint.Scale;
                ffCount = state.ComputeCoefficients(0, envelopeFactor);
                fbCount = state.ComputeCoefficients(1, envelopeFactor);
            }
        }
        return lastEnvelopeValue;
    }

    private static void ApplyFilterToSample(Span<int> inputCopySpan, Span<int> outSpan, FilterState state, int sampleIndex, int ffCount, int fbCount)
    {
        var acc = 0L;
        AddFeedforward(inputCopySpan, state, sampleIndex, ffCount, ref acc);
        AddFeedback(outSpan, state, sampleIndex, fbCount, ref acc);
        outSpan[sampleIndex] = (int)acc;
    }

    private static void ApplyFilterToFinalSample(Span<int> inputCopySpan, Span<int> outSpan, FilterState state, int sampleIndex, int ffCount, int fbCount, int sampleCount)
    {
        var acc = 0L;
        AddFeedforwardFinal(inputCopySpan, state, sampleIndex, ffCount, sampleCount, ref acc);
        AddFeedback(outSpan, state, sampleIndex, fbCount, ref acc);
        outSpan[sampleIndex] = (int)acc;
    }

    private static void AddFeedforward(Span<int> inputCopySpan, FilterState state, int sampleIndex, int ffCount, ref long acc)
    {
        acc += ((long)inputCopySpan[sampleIndex + ffCount] * state.InverseA0) >> 16;
        for (var k = 0; k < ffCount; k++)
        {
            acc += ((long)inputCopySpan[sampleIndex + ffCount - 1 - k] * state.Feedforward[k]) >> 16;
        }
    }

    private static void AddFeedforwardFinal(Span<int> inputCopySpan, FilterState state, int sampleIndex, int ffCount, int sampleCount, ref long acc)
    {
        var startK = sampleIndex + ffCount - sampleCount;
        for (var k = startK; k < ffCount; k++)
        {
            var inputIndex = sampleIndex + ffCount - 1 - k;
            acc += ((long)inputCopySpan[inputIndex] * state.Feedforward[k]) >> 16;
        }
    }

    private static void AddFeedback(Span<int> outSpan, FilterState state, int sampleIndex, int fbCount, ref long acc)
    {
        var actualFb = Math.Min(sampleIndex, fbCount);
        for (var k = 0; k < actualFb; k++)
        {
            var bufferIndex = sampleIndex - 1 - k;
            var fbTerm = ((long)outSpan[bufferIndex] * state.Feedback[k]) >> 16;
            acc -= fbTerm;
        }
    }

    private static float Interpolate(float value0, float value1, float factor)
    {
        return value0 + factor * (value1 - value0);
    }

    private static float GetAmplitude(Filter filter, int direction, int pole, float factor)
    {
        var mag0 = filter.PoleMagnitude[direction][0][pole];
        var mag1 = filter.PoleMagnitude[direction][1][pole];
        var interpolatedMag = Interpolate(mag0, mag1, factor);
        var dbValue = interpolatedMag * 0.0015258789f;
        return 1.0f - (float)AudioMath.DecibelToLinear(-dbValue);
    }

    private static float CalculatePhase(Filter filter, int dir, int pole, float factor)
    {
        var phase0 = filter.PolePhase[dir][0][pole];
        var phase1 = filter.PolePhase[dir][1][pole];
        var interpolatedPhase = Interpolate(phase0, phase1, factor);
        var scaledPhase = interpolatedPhase * 1.2207031e-4f;
        return GetOctavePhase(scaledPhase);
    }

    private static float GetOctavePhase(float pow2Value)
    {
        var frequencyHz = Math.Pow(2.0, pow2Value) * 32.703197;
        return (float)(frequencyHz * AudioMath.TwoPi / AudioConstants.SampleRate);
    }

    public class FilterState(Filter filter)
    {
        private readonly Filter _filterModel = filter;
        private readonly float[,] _sosCoefficients = new float[2, MaxCoefficients];

        public int[] Feedforward { get; } = new int[MaxCoefficients];
        public int[] Feedback { get; } = new int[MaxCoefficients];
        public int InverseA0 { get; set; }

        public int ComputeCoefficients(int dir, float envelopeFactor)
        {
            Array.Clear(_sosCoefficients, 0, _sosCoefficients.Length);

            var inverseA0Q16 = ComputeInverseA0Q16(envelopeFactor);
            InverseA0 = inverseA0Q16;
            var floatInverseA0 = inverseA0Q16 / (float)AudioConstants.FixedPoint.Scale;
            var poleCount = _filterModel.PoleCounts[dir];
            if (poleCount == 0)
            {
                return 0;
            }

            CreateFirstSection(dir, envelopeFactor);
            CascadeSections(dir, poleCount, envelopeFactor);
            return ScaleAndConvertCoefficients(dir, poleCount, floatInverseA0);
        }

        private int ComputeInverseA0Q16(float envelopeFactor)
        {
            var unityGainStart = _filterModel.UnityGain[0];
            var unityGainEnd = _filterModel.UnityGain[1];
            var interpGain = Interpolate(unityGainStart, unityGainEnd, envelopeFactor);
            var gainDb = interpGain * 0.0030517578f;
            var floatInvA0 = (float)Math.Pow(0.1, gainDb / 20.0);
            return (int)(floatInvA0 * AudioConstants.FixedPoint.Scale);
        }

        private void CreateFirstSection(int dir, float envelopeFactor)
        {
            var amplitude = GetAmplitude(_filterModel, dir, 0, envelopeFactor);
            var phase = CalculatePhase(_filterModel, dir, 0, envelopeFactor);
            var cosPhase = (float)Math.Cos(phase);
            _sosCoefficients[dir, 0] = -2.0f * amplitude * cosPhase;
            _sosCoefficients[dir, 1] = amplitude * amplitude;
        }

        private void CascadeSections(int dir, int poleCount, float envelopeFactor)
        {
            for (var section = 1; section < poleCount; section++)
            {
                var ampP = GetAmplitude(_filterModel, dir, section, envelopeFactor);
                var phaseP = CalculatePhase(_filterModel, dir, section, envelopeFactor);
                var cosPhaseP = (float)Math.Cos(phaseP);
                var term1 = -2.0f * ampP * cosPhaseP;
                var term2 = ampP * ampP;

                _sosCoefficients[dir, section * 2 + 1] = _sosCoefficients[dir, section * 2 - 1] * term2;
                _sosCoefficients[dir, section * 2] = _sosCoefficients[dir, section * 2 - 1] * term1 + _sosCoefficients[dir, section * 2 - 2] * term2;

                for (var coeffIndex = section * 2 - 1; coeffIndex >= 2; coeffIndex--)
                {
                    _sosCoefficients[dir, coeffIndex] += _sosCoefficients[dir, coeffIndex - 1] * term1 + _sosCoefficients[dir, coeffIndex - 2] * term2;
                }

                _sosCoefficients[dir, 1] += _sosCoefficients[dir, 0] * term1 + term2;
                _sosCoefficients[dir, 0] += term1;
            }
        }

        private int ScaleAndConvertCoefficients(int dir, int poleCount, float floatInverseA0)
        {
            var targetCoeffs = dir == 0 ? Feedforward : Feedback;
            var coefficientCount = poleCount * 2;

            if (dir == 0)
            {
                for (var i = 0; i < coefficientCount; i++)
                {
                    _sosCoefficients[0, i] *= floatInverseA0;
                }
            }

            for (var i = 0; i < coefficientCount; i++)
            {
                targetCoeffs[i] = (int)(_sosCoefficients[dir, i] * AudioConstants.FixedPoint.Scale);
            }

            return coefficientCount;
        }
    }
}
