using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Io.Buffers;
using System.Collections.Immutable;

namespace JagFx.Io;

public static class SynthFileWriter
{
    private static readonly int BufferSize = 4096;

    public static byte[] Write(Patch patch)
    {
        var buffer = new BinaryBuffer(BufferSize);

        WriteVoices(buffer, patch.Voices);
        WriteLoop(buffer, patch.Loop);

        return buffer.Data[..buffer.Position];
    }

    public static void WriteToPath(Patch patch, string path)
    {
        var data = Write(patch);
        File.WriteAllBytes(path, data);
    }

    private static void WriteVoices(BinaryBuffer buffer, ImmutableList<Voice?> voices)
    {
        for (var i = 0; i < AudioConstants.MaxVoices; i++)
        {
            var voice = i < voices.Count ? voices[i] : null;
            if (voice == null)
            {
                buffer.WriteUInt8(0);
            }
            else
            {
                WriteVoice(buffer, voice);
            }
        }
    }

    private static void WriteVoice(BinaryBuffer buffer, Voice voice)
    {
        WriteEnvelope(buffer, voice.FrequencyEnvelope);
        WriteEnvelope(buffer, voice.AmplitudeEnvelope);

        WriteOptionalEnvelopePair(buffer, voice.PitchLfo);
        WriteOptionalEnvelopePair(buffer, voice.AmplitudeLfo);
        WriteOptionalEnvelopePair(buffer, voice.GapOffEnvelope, voice.GapOnEnvelope);

        WritePartials(buffer, voice.Partials);

        buffer.WriteUSmart16((ushort)voice.Echo.DelayMilliseconds);
        buffer.WriteUSmart16((ushort)voice.Echo.FeedbackPercent);

        buffer.WriteUInt16BigEndian(voice.DurationMs);
        buffer.WriteUInt16BigEndian(voice.OffsetMs);

        if (voice.Filter != null)
        {
            WriteFilter(buffer, voice.Filter);
        }
        else
        {
            buffer.WriteUInt8(0);
        }
    }

    private static void WriteEnvelope(BinaryBuffer buffer, Envelope envelope)
    {
        buffer.WriteUInt8((int)envelope.Waveform);
        buffer.WriteInt32BigEndian(envelope.StartValue);
        buffer.WriteInt32BigEndian(envelope.EndValue);
        buffer.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buffer.WriteUInt16BigEndian(segment.Duration);
            buffer.WriteUInt16BigEndian(segment.TargetLevel);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buffer, LowFrequencyOscillator? lfo)
    {
        if (lfo == null)
        {
            buffer.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buffer, lfo.RateEnvelope);
            WriteEnvelope(buffer, lfo.ModulationDepth);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buffer, Envelope? env1, Envelope? env2)
    {
        if (env1 == null || env2 == null)
        {
            buffer.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buffer, env1);
            WriteEnvelope(buffer, env2);
        }
    }

    private static void WritePartials(BinaryBuffer buffer, ImmutableList<Partial> partials)
    {
        foreach (var partial in partials)
        {
            buffer.WriteUSmart16((ushort)partial.Amplitude.Value);
            buffer.WriteSmart16((short)partial.PitchOffsetSemitones);
            buffer.WriteUSmart16((ushort)partial.Delay.Value);
        }
        buffer.WriteUSmart16(0);
    }

    private static void WriteFilter(BinaryBuffer buffer, Filter filter)
    {
        var poleCounts = filter.PoleCounts;
        var unityGain = filter.UnityGain;

        var packedPoles = (poleCounts[0] << 4) | poleCounts[1];
        buffer.WriteUInt8(packedPoles);

        buffer.WriteUInt16BigEndian(unityGain[0]);
        buffer.WriteUInt16BigEndian(unityGain[1]);

        var modulationMask = CalculateModulationMask(filter);
        buffer.WriteUInt8(modulationMask);

        WriteFilterCoefficients(buffer, filter, phase: 0, modulationMask: 0);
        WriteFilterCoefficients(buffer, filter, phase: 1, modulationMask);

        if (filter.ModulationEnvelope != null)
        {
            WriteFilterEnvelope(buffer, filter.ModulationEnvelope);
        }
    }

    private static void WriteFilterCoefficients(BinaryBuffer buffer, Filter filter, int phase, int modulationMask)
    {
        for (var channel = 0; channel < 2; channel++)
        {
            var poles = filter.PoleCounts[channel];
            for (var pole = 0; pole < poles; pole++)
            {
                if (modulationMask != 0 && (modulationMask & (1 << (channel * 4 + pole))) == 0)
                    continue;

                buffer.WriteUInt16BigEndian(filter.PolePhase[channel][phase][pole]);
                buffer.WriteUInt16BigEndian(filter.PoleMagnitude[channel][phase][pole]);
            }
        }
    }

    private static void WriteFilterEnvelope(BinaryBuffer buffer, Envelope envelope)
    {
        buffer.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buffer.WriteUInt16BigEndian(segment.Duration);
            buffer.WriteUInt16BigEndian(segment.TargetLevel);
        }
    }

    private static void WriteLoop(BinaryBuffer buffer, LoopSegment loop)
    {
        buffer.WriteUInt16BigEndian(loop.BeginMs);
        buffer.WriteUInt16BigEndian(loop.EndMs);
    }

    private static int CalculateModulationMask(Filter filter)
    {
        var mask = 0;
        var poleCounts = filter.PoleCounts;
        var polePhase = filter.PolePhase;

        for (var channel = 0; channel < 2; channel++)
        {
            var poles = poleCounts[channel];
            for (var pole = 0; pole < poles; pole++)
            {
                var phase0 = polePhase[channel][0][pole];
                var phase1 = polePhase[channel][1][pole];
                if (phase0 != phase1)
                {
                    mask |= 1 << (channel * 4 + pole);
                }
            }
        }

        return mask;
    }
}
