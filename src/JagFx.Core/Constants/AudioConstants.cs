namespace JagFx.Core.Constants;

public static class AudioConstants
{
    public const int SampleRate = 22050;
    public const int BitsPerSample = 8;
    public const int AudioChannelCount = 1;
    public const double MillisecondsPerSample = 1000.0 / SampleRate;
    public const double SampleRatePerMillisecond = SampleRate / 1000.0;

    public const int MaxVoices = 10;
    public const int MinDurationMs = 10;
    public const int MaxOscillators = 10;
    public const int MaxFilterPairs = 15;
    public const int FilterUpdateRate = byte.MaxValue + 1;

    public const int PhaseMask = 0x7fff;
    public const double PhaseScale = 32.768;
    public const int NoisePhaseDiv = 2607;
    public const int MaxBufferSize = 1048576;

    public static class FixedPoint
    {
        /// <summary>65536 - Full 16-bit range for fixed-point scaling.</summary>
        public const int Scale = 1 << 16;

        /// <summary>32768 - Signed/unsigned conversion offset.</summary>
        public const int Offset = 1 << 15;

        /// <summary>16384 - Quarter scale for waveform calculations.</summary>
        public const int Quarter = 1 << 14;
    }
}
