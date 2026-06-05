using System.Globalization;
using JagFx.Core.Constants;

namespace JagFx.Domain.Utilities;

public static class AudioMath
{
    public enum UnitType
    {
        Raw16,
        Percent,
        Normalized,
        Decicents,
    }

    public const double TwoPi = /* 2.0 * Math.PI */
        6.283185307179586;
    public const double HalfPi = /* Math.PI / 2.0 */
        1.5707963267948966;

    private const double DecibelDivisor = 20.0;
    private const double PercentScale = 100.0;
    private const double DecicentsScale = 1200.0;

    private static readonly double Log10 = Math.Log(10.0);

    public static double Clamp(double value, double min, double max) => Math.Clamp(value, min, max);

    public static int Clamp(int value, int min, int max) => Math.Clamp(value, min, max);

    public static void ClipInt16(int[] buffer, int length = -1)
    {
        var bufferEnd = length < 0 ? buffer.Length : length;
        for (var i = 0; i < bufferEnd; i++)
        {
            if (buffer[i] < short.MinValue)
            {
                buffer[i] = short.MinValue;
            }
            else if (buffer[i] > short.MaxValue)
            {
                buffer[i] = short.MaxValue;
            }
        }
    }

    public static void ClipToByte(int[] buffer, int length = -1)
    {
        var bufferEnd = length < 0 ? buffer.Length : length;
        for (var i = 0; i < bufferEnd; i++)
        {
            var sample = buffer[i];
            buffer[i] =
                (sample + byte.MinValue & -byte.MaxValue - 1) != 0 ? (sample >> 31) ^ 127 : sample;
        }
    }

    public static double Lerp(double a, double b, double t) => a + (b - a) * t;

    public static double MapRange(
        double value,
        double inMin,
        double inMax,
        double outMin,
        double outMax
    ) => outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin);

    public static double DecibelToLinear(double decibel) =>
        Math.Exp(decibel / DecibelDivisor * Log10);

    public static double LinearToDecibel(double linear) => DecibelDivisor * Math.Log10(linear);

    public static double Convert(double value, UnitType sourceUnit, UnitType targetUnit)
    {
        if (sourceUnit == targetUnit)
        {
            return value;
        }

        var normalized = sourceUnit switch
        {
            UnitType.Raw16 => value / AudioConstants.FixedPoint.Scale,
            UnitType.Percent => value / PercentScale,
            UnitType.Normalized => value,
            UnitType.Decicents => value / DecicentsScale,
            _ => value,
        };

        return targetUnit switch
        {
            UnitType.Raw16 => normalized * AudioConstants.FixedPoint.Scale,
            UnitType.Percent => normalized * PercentScale,
            UnitType.Normalized => normalized,
            UnitType.Decicents => normalized * DecicentsScale,
            _ => normalized,
        };
    }

    public static string Format(double value, UnitType unit, int decimals = 1)
    {
        var formatString = $"F{decimals}";
        return unit switch
        {
            UnitType.Raw16 => ((int)value).ToString(CultureInfo.InvariantCulture),
            UnitType.Percent => $"{value.ToString(formatString, CultureInfo.InvariantCulture)}%",
            UnitType.Normalized => value.ToString(formatString, CultureInfo.InvariantCulture),
            UnitType.Decicents =>
                $"{(value / 10.0).ToString(formatString, CultureInfo.InvariantCulture)} st",
            _ => value.ToString(CultureInfo.InvariantCulture),
        };
    }
}
