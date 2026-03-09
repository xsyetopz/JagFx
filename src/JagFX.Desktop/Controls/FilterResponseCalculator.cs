using System.Numerics;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

public static class FilterResponseCalculator
{
    private const int NumPoints = 200;
    private const double MinFreq = 20.0;
    private const double MaxFreq = 11025.0;
    private const int SampleRate = 22050;

    public static double[] ComputeMagnitudeResponse(FilterViewModel filter, int numPoints = NumPoints)
    {
        var result = new double[numPoints];

        var poleCount = filter.PoleCount0;
        if (poleCount == 0 || filter.PoleMagnitude.IsDefault || filter.PolePhase.IsDefault)
        {
            Array.Fill(result, 0.0);
            return result;
        }

        // Extract poles from channel 0, bank 0
        var poles = new Complex[poleCount];
        for (var i = 0; i < poleCount; i++)
        {
            var phase = filter.PolePhase[0][0][i] / 65535.0 * 2.0 * Math.PI;
            var magnitude = filter.PoleMagnitude[0][0][i] / 65535.0;
            poles[i] = Complex.FromPolarCoordinates(magnitude, phase);
        }

        // Evaluate H(z) at log-spaced frequencies
        var logMin = Math.Log10(MinFreq);
        var logMax = Math.Log10(MaxFreq);

        for (var i = 0; i < numPoints; i++)
        {
            var freq = Math.Pow(10, logMin + (logMax - logMin) * i / (numPoints - 1));
            var omega = 2.0 * Math.PI * freq / SampleRate;
            var z = Complex.FromPolarCoordinates(1.0, omega);

            var h = Complex.One;
            foreach (var pole in poles)
            {
                var denom = z - pole;
                if (denom.Magnitude > 1e-10)
                    h /= denom;
            }

            result[i] = 20.0 * Math.Log10(Math.Max(h.Magnitude, 1e-10));
        }

        return result;
    }
}
