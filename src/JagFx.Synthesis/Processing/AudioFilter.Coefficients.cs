using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;

namespace JagFx.Synthesis.Processing;

public static partial class AudioFilter
{
    private static void ApplyCachedCoefficients(
        ref AudioFilterProcessingFrame filterFrame,
        int envelopeValue,
        ref int ffCount,
        ref int fbCount
    )
    {
        var slot = envelopeValue;
        if (filterFrame.CoefficientCache.Keys[slot] == envelopeValue)
        {
            ffCount = filterFrame.CoefficientCache.FfCounts[slot];
            fbCount = filterFrame.CoefficientCache.FbCounts[slot];
            filterFrame.Coefficients.InverseA0 = filterFrame.CoefficientCache.InverseA0[slot];
            var coefficientOffset = slot * MaxCoefficients;
            for (var i = 0; i < MaxCoefficients; i++)
            {
                filterFrame.Coefficients.Feedforward[i] = filterFrame.CoefficientCache.Feedforward[
                    coefficientOffset + i
                ];
                filterFrame.Coefficients.Feedback[i] = filterFrame.CoefficientCache.Feedback[
                    coefficientOffset + i
                ];
            }

            return;
        }

        var envelopeFactor = envelopeValue / (float)AudioConstants.FixedPoint.Scale;
        ffCount = filterFrame.Coefficients.ComputeCoefficients(0, envelopeFactor);
        var savedInverseA0 = filterFrame.Coefficients.InverseA0;
        fbCount = filterFrame.Coefficients.ComputeCoefficients(1, envelopeFactor);
        filterFrame.Coefficients.InverseA0 = savedInverseA0;

        filterFrame.CoefficientCache.Keys[slot] = envelopeValue;
        filterFrame.CoefficientCache.InverseA0[slot] = savedInverseA0;
        filterFrame.CoefficientCache.FfCounts[slot] = ffCount;
        filterFrame.CoefficientCache.FbCounts[slot] = fbCount;
        var writeOffset = slot * MaxCoefficients;
        for (var i = 0; i < MaxCoefficients; i++)
        {
            filterFrame.CoefficientCache.Feedforward[writeOffset + i] = filterFrame
                .Coefficients
                .Feedforward[i];
            filterFrame.CoefficientCache.Feedback[writeOffset + i] = filterFrame
                .Coefficients
                .Feedback[i];
        }
    }

    private static float Interpolate(float value0, float value1, float factor) =>
        value0 + factor * (value1 - value0);

    private static float GetAmplitude(Filter filter, int direction, int pole, float factor)
    {
        var mag0 = filter.GetPoleMagnitude(direction, 0, pole);
        var mag1 = filter.GetPoleMagnitude(direction, 1, pole);
        var interpolatedMag = Interpolate(mag0, mag1, factor);
        var dbValue = interpolatedMag * AmplitudeDbScaleFactor;
        return 1.0f - (float)AudioMath.DecibelToLinear(-dbValue);
    }

    private static float CalculatePhase(Filter filter, int dir, int pole, float factor)
    {
        var phase0 = filter.GetPolePhase(dir, 0, pole);
        var phase1 = filter.GetPolePhase(dir, 1, pole);
        var interpolatedPhase = Interpolate(phase0, phase1, factor);
        var scaledPhase = interpolatedPhase * PhaseScaleFactor;
        return GetOctavePhase(scaledPhase);
    }

    private static float GetOctavePhase(float pow2Value)
    {
        var frequencyHz = Math.Pow(2.0, pow2Value) * C1BaseFrequencyHz;
        return (float)(frequencyHz * AudioMath.TwoPi / AudioConstants.SampleRate);
    }

    private readonly ref struct AudioFilterCoefficientCache(
        Span<int> keys,
        Span<int> inverseA0,
        Span<int> ffCounts,
        Span<int> fbCounts,
        Span<int> feedforward,
        Span<int> feedback
    )
    {
        public Span<int> Keys { get; } = keys;
        public Span<int> InverseA0 { get; } = inverseA0;
        public Span<int> FfCounts { get; } = ffCounts;
        public Span<int> FbCounts { get; } = fbCounts;
        public Span<int> Feedforward { get; } = feedforward;
        public Span<int> Feedback { get; } = feedback;
    }
}
