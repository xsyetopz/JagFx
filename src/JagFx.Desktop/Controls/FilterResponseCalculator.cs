using System.Numerics;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls;

public static class FilterResponseCalculator
{
    private const int NumPoints = 200;
    private const double MinFreq = FilterFrequencyScale.MinimumFrequencyHz;
    private const double MaxFreq = FilterFrequencyScale.MaximumFrequencyHz;
    private const double SampleRate = FilterFrequencyScale.SampleRate;
    private const int MaxCoefficients = 8;

    public static double[] ComputeMagnitudeResponse(
        FilterViewModel filter,
        int numPoints = NumPoints
    ) => ComputeMagnitudeResponse(filter, 0, numPoints);

    public static double[] ComputeMagnitudeResponse(
        FilterViewModel filter,
        int phaseIndex,
        int numPoints = NumPoints
    ) => ComputeMagnitudeResponse(filter, (double)phaseIndex, numPoints);

    /// <summary>
    /// Computes the combined magnitude response at an envelope interpolation factor (0.0–1.0).
    /// Interpolation is performed on raw integer values before conversion, matching AudioFilter.GetAmplitude.
    /// </summary>
    public static double[] ComputeMagnitudeResponse(
        FilterViewModel filter,
        double envelopeFactor,
        int numPoints = NumPoints
    )
    {
        var decibelResponse = new double[numPoints];

        if (!filter.HasFilter || filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault)
        {
            Array.Fill(decibelResponse, 0.0);
            return decibelResponse;
        }

        if (filter.PoleCount0 == 0 && filter.PoleCount1 == 0)
        {
            Array.Fill(decibelResponse, 0.0);
            return decibelResponse;
        }

        var ffCoeffs = BuildSosCoefficientsInterp(filter, 0, envelopeFactor);
        var ffOrder = filter.PoleCount0 * 2;

        var fbCoeffs = BuildSosCoefficientsInterp(filter, 1, envelopeFactor);
        var fbOrder = filter.PoleCount1 * 2;

        var inverseA0 = ComputeInverseA0Interp(filter, envelopeFactor);

        var logMin = Math.Log10(MinFreq);
        var logMax = Math.Log10(MaxFreq);

        for (var i = 0; i < numPoints; i++)
        {
            var freq = Math.Pow(10, logMin + (logMax - logMin) * i / (numPoints - 1));
            var omega = 2.0 * Math.PI * freq / SampleRate;
            var zInv = Complex.FromPolarCoordinates(1.0, -omega);

            var numerator = Complex.One;
            var zPow = zInv;
            for (var k = 0; k < ffOrder; k++)
            {
                numerator += ffCoeffs[k] * zPow;
                zPow *= zInv;
            }

            var denominator = Complex.One;
            zPow = zInv;
            for (var k = 0; k < fbOrder; k++)
            {
                denominator += fbCoeffs[k] * zPow;
                zPow *= zInv;
            }

            var h =
                denominator.Magnitude > 1e-10 ? inverseA0 * numerator / denominator : Complex.Zero;

            decibelResponse[i] = 20.0 * Math.Log10(Math.Max(h.Magnitude, 1e-10));
        }

        return decibelResponse;
    }

    /// <summary>
    /// Converts raw pole magnitude to z-plane radius, matching AudioFilter.GetAmplitude().
    /// </summary>
    internal static double RawMagnitudeToRadius(int raw)
    {
        var dbValue = raw * 0.0015258789;
        return 1.0 - Math.Pow(10, -dbValue / 20.0);
    }

    /// <summary>
    /// Converts raw pole phase to z-plane angle in radians, matching AudioFilter.CalculatePhase().
    /// </summary>
    internal static double RawPhaseToAngle(int raw)
    {
        var scaled = raw * 1.2207031e-4;
        var freqHz = Math.Pow(2.0, scaled) * 32.703197;
        return freqHz * 2.0 * Math.PI / SampleRate;
    }

    /// <summary>
    /// Inverse of RawMagnitudeToRadius: converts z-plane radius back to raw magnitude.
    /// </summary>
    internal static int RadiusToRawMagnitude(double r)
    {
        r = Math.Clamp(r, 0, 0.9999);
        if (r <= 0)
        {
            return 0;
        }

        var raw = (int)(-20.0 * Math.Log10(1.0 - r) / 0.0015258789);
        return Math.Clamp(raw, 0, 65535);
    }

    /// <summary>
    /// Inverse of RawPhaseToAngle: converts z-plane angle back to raw phase.
    /// </summary>
    internal static int AngleToRawPhase(double theta)
    {
        var freqHz = theta * SampleRate / (2.0 * Math.PI);
        freqHz = Math.Max(freqHz, 1.0);
        var scaled = Math.Log2(freqHz / 32.703197);
        var raw = (int)(scaled / 1.2207031e-4);
        return Math.Clamp(raw, 0, 65535);
    }

    private static int GetRawMagnitude(
        FilterViewModel filter,
        int direction,
        int pole,
        int phaseIndex
    ) =>
        direction >= filter.PoleMagnitude.Length
        || filter.PoleMagnitude[direction].IsDefault
        || phaseIndex >= filter.PoleMagnitude[direction].Length
        || filter.PoleMagnitude[direction][phaseIndex].IsDefault
            ? 0
        : pole >= filter.PoleMagnitude[direction][phaseIndex].Length ? 0
        : filter.PoleMagnitude[direction][phaseIndex][pole];

    private static int GetRawPhase(
        FilterViewModel filter,
        int direction,
        int pole,
        int phaseIndex
    ) =>
        direction >= filter.PolePhase.Length
        || filter.PolePhase[direction].IsDefault
        || phaseIndex >= filter.PolePhase[direction].Length
        || filter.PolePhase[direction][phaseIndex].IsDefault
            ? 0
        : pole >= filter.PolePhase[direction][phaseIndex].Length ? 0
        : filter.PolePhase[direction][phaseIndex][pole];

    private static double GetAmplitudeInterp(
        FilterViewModel filter,
        int direction,
        int pole,
        double factor
    )
    {
        var raw0 = GetRawMagnitude(filter, direction, pole, 0);
        var raw1 = GetRawMagnitude(filter, direction, pole, 1);
        var rawInterp = raw0 + (raw1 - raw0) * factor;
        return RawMagnitudeToRadius((int)rawInterp);
    }

    private static double GetPhaseInterp(
        FilterViewModel filter,
        int direction,
        int pole,
        double factor
    )
    {
        var raw0 = GetRawPhase(filter, direction, pole, 0);
        var raw1 = GetRawPhase(filter, direction, pole, 1);
        var rawInterp = raw0 + (raw1 - raw0) * factor;
        return RawPhaseToAngle((int)rawInterp);
    }

    private static double[] BuildSosCoefficientsInterp(
        FilterViewModel filter,
        int direction,
        double factor
    )
    {
        var coeffs = new double[MaxCoefficients];
        var poleCount = direction == 0 ? filter.PoleCount0 : filter.PoleCount1;

        if (poleCount == 0)
        {
            return coeffs;
        }

        var amp = GetAmplitudeInterp(filter, direction, 0, factor);
        var phase = GetPhaseInterp(filter, direction, 0, factor);
        coeffs[0] = -2.0 * amp * Math.Cos(phase);
        coeffs[1] = amp * amp;

        for (var section = 1; section < poleCount; section++)
        {
            var ampP = GetAmplitudeInterp(filter, direction, section, factor);
            var phaseP = GetPhaseInterp(filter, direction, section, factor);
            var term1 = -2.0 * ampP * Math.Cos(phaseP);
            var term2 = ampP * ampP;

            coeffs[section * 2 + 1] = coeffs[section * 2 - 1] * term2;
            coeffs[section * 2] = coeffs[section * 2 - 1] * term1 + coeffs[section * 2 - 2] * term2;

            for (var k = section * 2 - 1; k >= 2; k--)
            {
                coeffs[k] += coeffs[k - 1] * term1 + coeffs[k - 2] * term2;
            }

            coeffs[1] += coeffs[0] * term1 + term2;
            coeffs[0] += term1;
        }

        return coeffs;
    }

    private static double ComputeInverseA0Interp(FilterViewModel filter, double factor)
    {
        var gain0 = filter.UnityGain0;
        var gain1 = filter.UnityGain1;
        var gain = gain0 + (gain1 - gain0) * factor;
        var gainDb = gain * 0.0030517578;
        return Math.Pow(0.1, gainDb / 20.0);
    }
}
