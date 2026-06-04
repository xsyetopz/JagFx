namespace JagFx.Desktop.Controls;

internal static class FilterFrequencyScale
{
    internal const double SampleRate = 22050.0;
    internal const double MinimumFrequencyHz = 20.0;
    internal const double MaximumFrequencyHz = SampleRate / 2.0;
    internal const double BaseFrequencyHz = 32.703197;
    internal const double RawPhaseToOctaves = 1.2207031e-4;
    internal const int SemitonesPerOctave = 12;

    private static readonly double LogFrequencyRange = Math.Log(
        MaximumFrequencyHz / MinimumFrequencyHz
    );

    internal static double FrequencyToNormalizedX(double frequencyHz)
    {
        var clamped = Math.Clamp(frequencyHz, MinimumFrequencyHz, MaximumFrequencyHz);
        return Math.Clamp(Math.Log(clamped / MinimumFrequencyHz) / LogFrequencyRange, 0.0, 1.0);
    }

    internal static double NormalizedXToFrequency(double normalizedX) =>
        MinimumFrequencyHz * Math.Exp(Math.Clamp(normalizedX, 0.0, 1.0) * LogFrequencyRange);

    internal static double RawPhaseToFrequency(int rawPhase) =>
        BaseFrequencyHz
        * Math.Pow(2.0, Math.Clamp(rawPhase, 0, ushort.MaxValue) * RawPhaseToOctaves);

    internal static double RawPhaseToNormalizedX(int rawPhase) =>
        FrequencyToNormalizedX(RawPhaseToFrequency(rawPhase));

    internal static int NormalizedXToRawPhase(double normalizedX)
    {
        var frequencyHz = NormalizedXToFrequency(normalizedX);
        var octaves = Math.Log2(frequencyHz / BaseFrequencyHz);
        return Math.Clamp((int)Math.Round(octaves / RawPhaseToOctaves), 0, ushort.MaxValue);
    }

    internal static double SemitoneToFrequency(int semitone) =>
        BaseFrequencyHz * Math.Pow(2.0, semitone / (double)SemitonesPerOctave);

    internal static int FirstVisibleSemitone() =>
        (int)Math.Floor(SemitonesPerOctave * Math.Log2(MinimumFrequencyHz / BaseFrequencyHz));

    internal static int LastVisibleSemitone() =>
        (int)Math.Ceiling(SemitonesPerOctave * Math.Log2(MaximumFrequencyHz / BaseFrequencyHz));

    internal static bool IsOctaveSemitone(int semitone) =>
        PositiveModulo(semitone, SemitonesPerOctave) == 0;

    internal static bool IsHarmonicAnchorSemitone(int semitone)
    {
        var chroma = PositiveModulo(semitone, SemitonesPerOctave);
        return chroma is 4 or 7;
    }

    internal static bool IsAccidentalSemitone(int semitone)
    {
        var chroma = PositiveModulo(semitone, SemitonesPerOctave);
        return chroma is 1 or 3 or 6 or 8 or 10;
    }

    private static int PositiveModulo(int value, int divisor) =>
        ((value % divisor) + divisor) % divisor;
}
