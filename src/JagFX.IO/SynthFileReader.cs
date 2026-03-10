using JagFx.Core.Constants;
using JagFx.Core.Types;
using JagFx.Domain.Models;
using JagFx.Io.Buffers;
using System.Collections.Immutable;

namespace JagFx.Io;

public static class SynthFileReader
{
    private const int MinWaveformId = 1;
    private const int MaxWaveformId = 4;
    private const int EnvelopeStartThreshold = 10_000_000;
    private const int SegCountOffset = 9;
    private const int MaxReasonableSegCount = 15;
    private const int MinVoiceSize = 30;

    public static Patch Read(byte[] data)
    {
        var parser = new SynthParser(new BinaryBuffer(data));
        return parser.Parse();
    }

    public static Patch ReadFromPath(string path)
    {
        var data = File.ReadAllBytes(path);
        return Read(data);
    }

    private class SynthParser(BinaryBuffer buf)
    {
        private readonly BinaryBuffer _buf = buf;
        private readonly List<string> _warnings = [];

        public Patch Parse()
        {
            var voices = ReadVoices();

            var loopParams = _buf.Remaining >= 4
                ? new LoopSegment(_buf.ReadUInt16BigEndian(), _buf.ReadUInt16BigEndian())
                : _buf.IsTruncated
                    ? new LoopSegment(0, 0)
                    : new LoopSegment(0, 0);

            if (_buf.IsTruncated)
                _warnings.Add($"File truncated at byte offset {_buf.Position}; loop parameters may be incomplete or invalid");

            return new Patch(voices, loopParams, [.. _warnings]);
        }

        private ImmutableList<Voice?> ReadVoices()
        {
            var voices = new List<Voice?>(AudioConstants.MaxVoices);

            for (var i = 0; i < AudioConstants.MaxVoices; i++)
            {
                if (_buf.Remaining >= MinVoiceSize)
                {
                    var marker = _buf.Peek();
                    if (marker != 0 && IsValidWaveform(marker))
                    {
                        var voice = ReadVoice();
                        voices.Add(voice);
                    }
                    else
                    {
                        if (marker != 0)
                        {
                            voices.Add(null);
                            for (var j = i + 1; j < AudioConstants.MaxVoices; j++)
                                voices.Add(null);
                            break;
                        }
                        _buf.Skip(1);
                        voices.Add(null);
                    }
                }
                else
                {
                    voices.Add(null);
                }
            }

            return [.. voices];
        }

        private Voice ReadVoice()
        {
            var pitchEnvelope = ReadEnvelope();
            var volumeEnvelope = ReadEnvelope();

            var (vibratoRate, vibratoDepth) = ReadOptionalEnvelopePair();
            var (tremoloRate, tremoloDepth) = ReadOptionalEnvelopePair();
            var (gapOff, gapOn) = ReadOptionalEnvelopePair();

            var partials = ReadPartials();

            var echoDelay = _buf.ReadUSmart();
            var echoFeedback = _buf.ReadUSmart();

            var duration = _buf.ReadUInt16BigEndian();
            var startTime = _buf.ReadUInt16BigEndian();

            var filter = ReadFilter();

            return new Voice(
                FrequencyEnvelope: CreateEnvelopeWithDuration(pitchEnvelope, duration),
                AmplitudeEnvelope: CreateEnvelopeWithDuration(volumeEnvelope, duration),
                PitchLfo: vibratoRate != null && vibratoDepth != null ? new LowFrequencyOscillator(vibratoRate, vibratoDepth) : null,
                AmplitudeLfo: tremoloRate != null && tremoloDepth != null ? new LowFrequencyOscillator(tremoloRate, tremoloDepth) : null,
                GapOffEnvelope: gapOff,
                GapOnEnvelope: gapOn,
                Partials: partials,
                Echo: new Echo(echoDelay, echoFeedback),
                DurationMs: duration,
                OffsetMs: startTime,
                Filter: filter);
        }

        private Envelope ReadEnvelope()
        {
            var waveformId = _buf.ReadUInt8();
            var startValue = _buf.ReadInt32BigEndian();
            var endValue = _buf.ReadInt32BigEndian();
            var waveform = (waveformId >= 0 && waveformId <= 4) ? (Waveform)waveformId : Waveform.Off;
            var segmentLength = _buf.ReadUInt8();

            var segments = new List<Segment>(segmentLength);
            for (var i = 0; i < segmentLength; i++)
            {
                segments.Add(new Segment(
                    _buf.ReadUInt16BigEndian(),
                    _buf.ReadUInt16BigEndian()));
            }

            return new Envelope(waveform, startValue, endValue, [.. segments]);
        }

        private (Envelope? first, Envelope? second) ReadOptionalEnvelopePair()
        {
            var marker = _buf.Peek();
            if (marker != 0)
            {
                var env1 = ReadEnvelope();
                var env2 = ReadEnvelope();
                return (env1, env2);
            }

            _buf.Skip(1);
            return (null, null);
        }

        private ImmutableList<Partial> ReadPartials()
        {
            var partials = new List<Partial>(AudioConstants.MaxOscillators);

            while (partials.Count < AudioConstants.MaxOscillators)
            {
                if (_buf.Remaining == 0) break;

                var volume = _buf.ReadUSmart();
                if (volume == 0) break;

                var pitchOffset = _buf.ReadSmart();
                var startDelay = _buf.ReadUSmart();

                partials.Add(new Partial(
                    new Percent(volume),
                    pitchOffset,
                    new Milliseconds(startDelay)));
            }

            return [.. partials];
        }

        private Filter? ReadFilter()
        {
            if (_buf.Remaining == 0) return null;
            if (!DetectFilterPresent()) return null;

            var (poleCount0, poleCount1) = ReadFilterHeader();
            var unityGain0 = _buf.ReadUInt16BigEndian();
            var unityGain1 = _buf.ReadUInt16BigEndian();
            var modulationMask = _buf.ReadUInt8();

            var (frequencies, magnitudes) = ReadFilterCoefficients(
                poleCount0, poleCount1, modulationMask);

            Envelope? modulationEnvelope = null;
            if (modulationMask != 0 || unityGain1 != unityGain0)
            {
                modulationEnvelope = ReadEnvelopeSegments();
            }

            if (_buf.IsTruncated)
            {
                _warnings.Add("Filter truncated (discarding partial data)");
                return null;
            }

            return BuildFilter(
                poleCount0,
                poleCount1,
                unityGain0,
                unityGain1,
                frequencies,
                magnitudes,
                modulationEnvelope);
        }

        private (int poleCount0, int poleCount1) ReadFilterHeader()
        {
            var packedPoles = _buf.ReadUInt8();
            var poleCount0 = packedPoles >> 4;
            var poleCount1 = packedPoles & 0xF;
            return (poleCount0, poleCount1);
        }

        private (int[,,] frequencies, int[,,] magnitudes) ReadFilterCoefficients(
            int poleCount0, int poleCount1, int modulationMask)
        {
            var frequencies = new int[2, 2, AudioConstants.MaxFilterPairs];
            var magnitudes = new int[2, 2, AudioConstants.MaxFilterPairs];

            ReadFilterPhase0Coefficients(frequencies, magnitudes, poleCount0, poleCount1);
            ReadFilterPhase1Coefficients(frequencies, magnitudes, poleCount0, poleCount1, modulationMask);

            return (frequencies, magnitudes);
        }

        private void ReadFilterPhase0Coefficients(
            int[,,] frequencies, int[,,] magnitudes, int poleCount0, int poleCount1)
        {
            for (var channel = 0; channel < 2; channel++)
            {
                var poles = channel == 0 ? poleCount0 : poleCount1;
                var maxPoles = Math.Min(poles, AudioConstants.MaxFilterPairs);
                for (var p = 0; p < maxPoles; p++)
                {
                    if (_buf.Remaining < 2) return;
                    frequencies[channel, 0, p] = _buf.ReadUInt16BigEndian();
                    if (_buf.Remaining < 2) return;
                    magnitudes[channel, 0, p] = _buf.ReadUInt16BigEndian();
                }
            }
        }

        private void ReadFilterPhase1Coefficients(
            int[,,] frequencies, int[,,] magnitudes, int poleCount0, int poleCount1, int modulationMask)
        {
            for (var channel = 0; channel < 2; channel++)
            {
                var poles = channel == 0 ? poleCount0 : poleCount1;
                var maxPoles = Math.Min(poles, AudioConstants.MaxFilterPairs);
                for (var p = 0; p < maxPoles; p++)
                {
                    if ((modulationMask & (1 << (channel * 4 + p))) != 0)
                    {
                        if (_buf.Remaining < 2) return;
                        frequencies[channel, 1, p] = _buf.ReadUInt16BigEndian();
                        if (_buf.Remaining < 2) return;
                        magnitudes[channel, 1, p] = _buf.ReadUInt16BigEndian();
                    }
                    else
                    {
                        CopyPhase0ToPhase1(frequencies, magnitudes, channel, p);
                    }
                }
            }
        }

        private Envelope ReadEnvelopeSegments()
        {
            var length = _buf.ReadUInt8();
            var segments = new List<Segment>(length);

            for (var i = 0; i < length; i++)
            {
                segments.Add(new Segment(
                    _buf.ReadUInt16BigEndian(),
                    _buf.ReadUInt16BigEndian()));
            }

            return new Envelope(Waveform.Off, 0, 0, [.. segments]);
        }

        private bool DetectFilterPresent()
        {
            var peeked = _buf.Peek();
            if (peeked == 0)
            {
                _buf.Skip(1);
                return false;
            }
            if (IsAmbiguousFilterByte(peeked) && LooksLikeEnvelope()) return false;
            return true;
        }

        private bool IsAmbiguousFilterByte(int b)
        {
            return b >= MinWaveformId && b <= MaxWaveformId &&
                   _buf.Remaining >= AudioConstants.MaxVoices;
        }

        private bool LooksLikeEnvelope()
        {
            var b1 = _buf.PeekAt(1);
            var b2 = _buf.PeekAt(2);
            var b3 = _buf.PeekAt(3);
            var b4 = _buf.PeekAt(4);

            var possibleStart = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            if (possibleStart < -EnvelopeStartThreshold ||
                possibleStart > EnvelopeStartThreshold)
                return false;

            var possibleSegCount = _buf.PeekAt(SegCountOffset);
            return possibleSegCount <= MaxReasonableSegCount;
        }

        private static bool IsValidWaveform(int waveformId)
        {
            return waveformId >= MinWaveformId && waveformId <= MaxWaveformId;
        }

        private static void CopyPhase0ToPhase1(int[,,] frequencies, int[,,] magnitudes, int channel, int p)
        {
            frequencies[channel, 1, p] = frequencies[channel, 0, p];
            magnitudes[channel, 1, p] = magnitudes[channel, 0, p];
        }

        private static Filter BuildFilter(
            int poleCount0,
            int poleCount1,
            int unityGain0,
            int unityGain1,
            int[,,] frequencies,
            int[,,] magnitudes,
            Envelope? modulationEnvelope)
        {
            var poleCounts = ImmutableArray.Create(poleCount0, poleCount1);
            var unityGain = ImmutableArray.Create(unityGain0, unityGain1);

            var (polePhase, poleMagnitude) = BuildFilterCoeffientArrays(
                frequencies, magnitudes, poleCount0, poleCount1);

            return new Filter(poleCounts, unityGain, polePhase, poleMagnitude, modulationEnvelope);
        }

        private static (ImmutableArray<ImmutableArray<ImmutableArray<int>>> polePhase, ImmutableArray<ImmutableArray<ImmutableArray<int>>> poleMagnitude) BuildFilterCoeffientArrays(
            int[,,] frequencies, int[,,] magnitudes, int poleCount0, int poleCount1)
        {
            var freqArray = new ImmutableArray<int>[2][];
            var magArray = new ImmutableArray<int>[2][];

            for (var channel = 0; channel < 2; channel++)
            {
                var poles = channel == 0 ? poleCount0 : poleCount1;
                var maxPoles = Math.Min(poles, AudioConstants.MaxFilterPairs);
                freqArray[channel] = new ImmutableArray<int>[2];
                magArray[channel] = new ImmutableArray<int>[2];

                for (var phase = 0; phase < 2; phase++)
                {
                    freqArray[channel][phase] = BuildCoeffientArray(frequencies, channel, phase, maxPoles);
                    magArray[channel][phase] = BuildCoeffientArray(magnitudes, channel, phase, maxPoles);
                }
            }

            var polePhase = ImmutableArray.Create(
                [freqArray[0][0], freqArray[0][1]],
                ImmutableArray.Create(freqArray[1][0], freqArray[1][1])
            );
            var poleMagnitude = ImmutableArray.Create(
                [magArray[0][0], magArray[0][1]],
                ImmutableArray.Create(magArray[1][0], magArray[1][1])
            );

            return (polePhase, poleMagnitude);
        }

        private static ImmutableArray<int> BuildCoeffientArray(
            int[,,] coefficients, int channel, int phase, int poles)
        {
            var builder = ImmutableArray.CreateBuilder<int>(poles);
            for (var pole = 0; pole < poles; pole++)
            {
                builder.Add(coefficients[channel, phase, pole]);
            }
            return builder.ToImmutable();
        }

        private static Envelope CreateEnvelopeWithDuration(Envelope env, int duration)
        {
            if (env.Segments.Count == 0 && env.StartValue != env.EndValue)
            {
                return new Envelope(
                    env.Waveform,
                    env.StartValue,
                    env.EndValue,
                    [new Segment(duration, env.EndValue)]);
            }
            return env;
        }
    }
}
