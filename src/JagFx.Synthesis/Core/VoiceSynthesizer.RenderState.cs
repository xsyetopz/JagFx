using JagFx.Synthesis.Processing;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private readonly struct VoiceRenderState
    {
        public required EnvelopeGenerator FrequencyBaseEval { get; init; }
        public required EnvelopeGenerator AmplitudeBaseEval { get; init; }
        public EnvelopeGenerator? FrequencyModulationRateEval { get; init; }
        public EnvelopeGenerator? FrequencyModulationRangeEval { get; init; }
        public EnvelopeGenerator? AmplitudeModulationRateEval { get; init; }
        public EnvelopeGenerator? AmplitudeModulationRangeEval { get; init; }
        public int FrequencyStep { get; init; }
        public int FrequencyBase { get; init; }
        public int AmplitudeStep { get; init; }
        public int AmplitudeBase { get; init; }
        public int PartialCount { get; init; }
        public EnvelopeGenerator? FilterEnvelopeEval { get; init; }
    }
}
