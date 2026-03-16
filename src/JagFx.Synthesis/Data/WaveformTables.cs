using JagFx.Core.Constants;
using JagFx.Domain;
using JagFx.Domain.Utilities;
using JavaRng = JagFx.JavaRandom.JavaRandom;

namespace JagFx.Synthesis.Data;

public static class WaveformTables
{
    public const double DecicentRatio = 1.0057929410678534;

    public static int[] NoiseTable { get; }
    public static int[] SineWaveTable { get; }
    public static double[] UnitCircleXArray { get; }
    public static double[] UnitCircleYArray { get; }

    private static readonly double[] SemitoneCache;

    private const double SinTableDivisor = 5215.1903;
    private const int CircleSegments = 64;
    private const int SemitoneRange = 120;

    static WaveformTables()
    {
        NoiseTable = GenerateNoiseTable();
        SineWaveTable = GenerateSineWaveTable();
        UnitCircleXArray = CreateUnitCircleXArray();
        UnitCircleYArray = CreateUnitCircleYArray();
        SemitoneCache = CreateSemitoneCache();
    }

    private static int[] GenerateNoiseTable()
    {
        var rng = new JavaRng(0);
        var table = new int[AudioConstants.FixedPoint.Offset];
        for (var i = 0; i < AudioConstants.FixedPoint.Offset; i++)
        {
            table[i] = (rng.NextInt() & 2) - 1;
        }
        return table;
    }

    private static int[] GenerateSineWaveTable()
    {
        var table = new int[AudioConstants.FixedPoint.Offset];
        for (var i = 0; i < AudioConstants.FixedPoint.Offset; i++)
        {
            table[i] = (int)(Math.Sin(i / SinTableDivisor) * AudioConstants.FixedPoint.Quarter);
        }
        return table;
    }

    private static double[] CreateUnitCircleXArray() => CreateUnitCircleArray(Math.Cos);

    private static double[] CreateUnitCircleYArray() => CreateUnitCircleArray(Math.Sin);

    private static double[] CreateUnitCircleArray(Func<double, double> trigFunc)
    {
        var table = new double[CircleSegments + 1];
        for (var i = 0; i <= CircleSegments; i++)
        {
            table[i] = trigFunc(i * AudioMath.TwoPi / CircleSegments);
        }
        return table;
    }

    private static double[] CreateSemitoneCache()
    {
        const int cacheSize = SemitoneRange * 2 + 1;
        var table = new double[cacheSize];
        for (var i = 0; i < cacheSize; i++)
        {
            table[i] = Math.Pow(DecicentRatio, i - SemitoneRange);
        }
        return table;
    }

    public static double GetPitchMultiplier(int decicents)
    {
        if (decicents >= -SemitoneRange && decicents <= SemitoneRange)
        {
            return SemitoneCache[decicents + SemitoneRange];
        }
        return Math.Pow(DecicentRatio, decicents);
    }
}
