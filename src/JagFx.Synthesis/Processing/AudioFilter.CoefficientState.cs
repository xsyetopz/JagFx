using JagFx.Core.Constants;
using JagFx.Domain.Models;

namespace JagFx.Synthesis.Processing;

public static partial class AudioFilter
{
    private ref struct AudioFilterCoefficientState(
        Filter filter,
        Span<float> sosCoefficients,
        Span<int> feedforward,
        Span<int> feedback
    )
    {
        private readonly Filter _filterModel = filter;
        private readonly Span<float> _sosCoefficients = sosCoefficients;

        public Span<int> Feedforward { get; } = feedforward;
        public Span<int> Feedback { get; } = feedback;
        public int InverseA0 { get; set; }

        public int ComputeCoefficients(int dir, float envelopeFactor)
        {
            _sosCoefficients.Clear();

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

        private readonly int ComputeInverseA0Q16(float envelopeFactor)
        {
            var unityGainStart = _filterModel.UnityGain[0];
            var unityGainEnd = _filterModel.UnityGain[1];
            var interpGain = Interpolate(unityGainStart, unityGainEnd, envelopeFactor);
            var gainDb = interpGain * GainScaleFactor;
            var floatInvA0 = (float)Math.Pow(0.1, gainDb / 20.0);
            return (int)(floatInvA0 * AudioConstants.FixedPoint.Scale);
        }

        private readonly void CreateFirstSection(int dir, float envelopeFactor)
        {
            var amplitude = GetAmplitude(_filterModel, dir, 0, envelopeFactor);
            var phase = CalculatePhase(_filterModel, dir, 0, envelopeFactor);
            var cosPhase = (float)Math.Cos(phase);
            var offset = dir * MaxCoefficients;
            _sosCoefficients[offset] = -2.0f * amplitude * cosPhase;
            _sosCoefficients[offset + 1] = amplitude * amplitude;
        }

        private readonly void CascadeSections(int dir, int poleCount, float envelopeFactor)
        {
            for (var section = 1; section < poleCount; section++)
            {
                var ampP = GetAmplitude(_filterModel, dir, section, envelopeFactor);
                var phaseP = CalculatePhase(_filterModel, dir, section, envelopeFactor);
                var cosPhaseP = (float)Math.Cos(phaseP);
                var term1 = -2.0f * ampP * cosPhaseP;
                var term2 = ampP * ampP;

                var offset = dir * MaxCoefficients;
                _sosCoefficients[offset + section * 2 + 1] =
                    _sosCoefficients[offset + section * 2 - 1] * term2;
                _sosCoefficients[offset + section * 2] =
                    _sosCoefficients[offset + section * 2 - 1] * term1
                    + _sosCoefficients[offset + section * 2 - 2] * term2;

                for (var coeffIndex = section * 2 - 1; coeffIndex >= 2; coeffIndex--)
                {
                    _sosCoefficients[offset + coeffIndex] +=
                        _sosCoefficients[offset + coeffIndex - 1] * term1
                        + _sosCoefficients[offset + coeffIndex - 2] * term2;
                }

                _sosCoefficients[offset + 1] += _sosCoefficients[offset] * term1 + term2;
                _sosCoefficients[offset] += term1;
            }
        }

        private readonly int ScaleAndConvertCoefficients(
            int dir,
            int poleCount,
            float floatInverseA0
        )
        {
            var targetCoeffs = dir == 0 ? Feedforward : Feedback;
            var coefficientCount = poleCount * 2;

            if (dir == 0)
            {
                for (var i = 0; i < coefficientCount; i++)
                {
                    _sosCoefficients[i] *= floatInverseA0;
                }
            }

            for (var i = 0; i < coefficientCount; i++)
            {
                targetCoeffs[i] = (int)(
                    _sosCoefficients[(dir * MaxCoefficients) + i] * AudioConstants.FixedPoint.Scale
                );
            }

            return coefficientCount;
        }
    }
}
