using System.Collections.Immutable;
using System.Text;
using JagFx.Core.Constants;
using JagFx.Core.Types;
using JagFx.Domain.Models;
using JagFx.Io.Buffers;

namespace JagFx.Io;

public static class SynthFileReader
{
    private const string GitLfsPointerPrefix = "version https://git-lfs.github.com/spec/";
    private const int MinWaveformId = 1;
    private const int MaxWaveformId = 4;
    private const int EnvelopeStartThreshold = 10_000_000;
    private const int SegCountOffset = 9;
    private const int MaxReasonableSegCount = 15;
    private const int MinVoiceSize = 30;

    public static Patch Read(byte[] synthBytes)
    {
        ValidateInput(synthBytes);
        var parser = new SynthParser(new BinaryBuffer(synthBytes));
        return parser.Parse();
    }

    public static Patch ReadFromPath(string path)
    {
        var synthBytes = File.ReadAllBytes(path);
        return Read(synthBytes);
    }

    private static void ValidateInput(byte[] synthBytes)
    {
        if (synthBytes.Length == 0)
        {
            return;
        }

        if (IsGitLfsPointer(synthBytes))
        {
            throw new SynthFileException(
                "File is a Git LFS pointer, not synth file bytes. Run `git lfs pull` in the archive repository to download the real .synth contents."
            );
        }
    }

    private static bool IsGitLfsPointer(byte[] synthBytes)
    {
        if (synthBytes.Length < GitLfsPointerPrefix.Length)
        {
            return false;
        }

        var prefix = Encoding.ASCII.GetString(synthBytes, 0, GitLfsPointerPrefix.Length);
        return prefix == GitLfsPointerPrefix;
    }

    private sealed class SynthParser(BinaryBuffer synthBuffer)
    {
        private readonly BinaryBuffer _synthBuffer = synthBuffer;
        private readonly List<string> _warnings = [];

        public Patch Parse()
        {
            var voices = ReadVoices();

            var loopParams =
                _synthBuffer.Remaining >= 4
                    ? new LoopSegment(
                        _synthBuffer.ReadUInt16BigEndian(),
                        _synthBuffer.ReadUInt16BigEndian()
                    )
                : _synthBuffer.IsTruncated ? new LoopSegment(0, 0)
                : new LoopSegment(0, 0);

            if (_synthBuffer.IsTruncated)
            {
                _warnings.Add(
                    $"File truncated at byte offset {_synthBuffer.Position}; loop parameters may be incomplete or invalid"
                );
            }

            return new Patch(voices, loopParams, [.. _warnings]);
        }

        private ImmutableList<Voice?> ReadVoices()
        {
            var voices = new List<Voice?>(AudioConstants.MaxVoices);

            for (var i = 0; i < AudioConstants.MaxVoices; i++)
            {
                if (_synthBuffer.Remaining >= MinVoiceSize)
                {
                    var marker = _synthBuffer.Peek();
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
                            {
                                voices.Add(null);
                            }

                            break;
                        }
                        _synthBuffer.Skip(1);
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

            var echoDelay = _synthBuffer.ReadUSmart();
            var echoFeedback = _synthBuffer.ReadUSmart();

            var duration = _synthBuffer.ReadUInt16BigEndian();
            var startTime = _synthBuffer.ReadUInt16BigEndian();

            var filter = ReadFilter();

            return new Voice(
                FrequencyEnvelope: CreateEnvelopeWithDuration(pitchEnvelope, duration),
                AmplitudeEnvelope: CreateEnvelopeWithDuration(volumeEnvelope, duration),
                PitchLfo: vibratoRate != null && vibratoDepth != null
                    ? new LowFrequencyOscillator(vibratoRate, vibratoDepth)
                    : null,
                AmplitudeLfo: tremoloRate != null && tremoloDepth != null
                    ? new LowFrequencyOscillator(tremoloRate, tremoloDepth)
                    : null,
                GapOffEnvelope: gapOff,
                GapOnEnvelope: gapOn,
                Partials: partials,
                Echo: new Echo(echoDelay, echoFeedback),
                DurationMs: duration,
                OffsetMs: startTime,
                Filter: filter
            );
        }

        private Envelope ReadEnvelope()
        {
            var waveformId = _synthBuffer.ReadUInt8();
            var startValue = _synthBuffer.ReadInt32BigEndian();
            var endValue = _synthBuffer.ReadInt32BigEndian();
            var waveform = (waveformId is >= 0 and <= 4) ? (Waveform)waveformId : Waveform.Off;
            var segmentLength = _synthBuffer.ReadUInt8();
            return new Envelope(waveform, startValue, endValue, ReadSegmentPairs(segmentLength));
        }

        private ImmutableList<Segment> ReadSegmentPairs(int count)
        {
            var segments = new List<Segment>(count);
            for (var i = 0; i < count; i++)
            {
                segments.Add(
                    new Segment(
                        _synthBuffer.ReadUInt16BigEndian(),
                        _synthBuffer.ReadUInt16BigEndian()
                    )
                );
            }
            return [.. segments];
        }

        private (Envelope? first, Envelope? second) ReadOptionalEnvelopePair()
        {
            var marker = _synthBuffer.Peek();
            if (marker != 0)
            {
                var env1 = ReadEnvelope();
                var env2 = ReadEnvelope();
                return (env1, env2);
            }

            _synthBuffer.Skip(1);
            return (null, null);
        }

        private ImmutableList<VoicePartial> ReadPartials()
        {
            var partials = new List<VoicePartial>(AudioConstants.MaxOscillators);

            while (partials.Count < AudioConstants.MaxOscillators)
            {
                if (_synthBuffer.Remaining == 0)
                {
                    break;
                }

                var volume = _synthBuffer.ReadUSmart();
                if (volume == 0)
                {
                    break;
                }

                var pitchOffset = _synthBuffer.ReadSmart();
                var startDelay = _synthBuffer.ReadUSmart();

                partials.Add(
                    new VoicePartial(new Percent(volume), pitchOffset, new Milliseconds(startDelay))
                );
            }

            return [.. partials];
        }

        private Filter? ReadFilter()
        {
            if (_synthBuffer.Remaining == 0)
            {
                return null;
            }

            if (!DetectFilterPresent())
            {
                return null;
            }

            var (poleCount0, poleCount1) = ReadFilterHeader();
            var unityGain0 = _synthBuffer.ReadUInt16BigEndian();
            var unityGain1 = _synthBuffer.ReadUInt16BigEndian();
            var modulationMask = _synthBuffer.ReadUInt8();

            var (frequencies, magnitudes) = ReadFilterCoefficients(
                poleCount0,
                poleCount1,
                modulationMask
            );

            Envelope? modulationEnvelope = null;
            if (modulationMask != 0 || unityGain1 != unityGain0)
            {
                modulationEnvelope = ReadEnvelopeSegments();
            }

            if (_synthBuffer.IsTruncated)
            {
                _warnings.Add("Filter truncated (discarding partial filter payload)");
                return null;
            }

            return BuildFilter(
                poleCount0,
                poleCount1,
                unityGain0,
                unityGain1,
                frequencies,
                magnitudes,
                modulationEnvelope
            );
        }

        private (int poleCount0, int poleCount1) ReadFilterHeader()
        {
            var packedPoles = _synthBuffer.ReadUInt8();
            var poleCount0 = packedPoles >> 4;
            var poleCount1 = packedPoles & 0xF;
            return (poleCount0, poleCount1);
        }

        private (int[] frequencies, int[] magnitudes) ReadFilterCoefficients(
            int poleCount0,
            int poleCount1,
            int modulationMask
        )
        {
            var frequencies = new int[2 * 2 * AudioConstants.MaxFilterPairs];
            var magnitudes = new int[2 * 2 * AudioConstants.MaxFilterPairs];

            for (var phase = 0; phase < 2; phase++)
            {
                for (var channel = 0; channel < 2; channel++)
                {
                    var poles = channel == 0 ? poleCount0 : poleCount1;
                    var maxPoles =
                        poles < AudioConstants.MaxFilterPairs
                            ? poles
                            : AudioConstants.MaxFilterPairs;
                    for (var p = 0; p < maxPoles; p++)
                    {
                        var index = GetFilterCoefficientIndex(channel, phase, p);
                        if (phase == 1 && (modulationMask & (1 << (channel * 4 + p))) == 0)
                        {
                            var baseIndex = GetFilterCoefficientIndex(channel, 0, p);
                            frequencies[index] = frequencies[baseIndex];
                            magnitudes[index] = magnitudes[baseIndex];
                            continue;
                        }

                        if (_synthBuffer.Remaining < 2)
                        {
                            return (frequencies, magnitudes);
                        }

                        frequencies[index] = _synthBuffer.ReadUInt16BigEndian();
                        if (_synthBuffer.Remaining < 2)
                        {
                            return (frequencies, magnitudes);
                        }

                        magnitudes[index] = _synthBuffer.ReadUInt16BigEndian();
                    }
                }
            }

            return (frequencies, magnitudes);
        }

        private Envelope ReadEnvelopeSegments()
        {
            var length = _synthBuffer.ReadUInt8();
            return new Envelope(Waveform.Off, 0, 0, ReadSegmentPairs(length));
        }

        private bool DetectFilterPresent()
        {
            var peeked = _synthBuffer.Peek();
            if (peeked == 0)
            {
                _synthBuffer.Skip(1);
                return false;
            }
            return !IsAmbiguousFilterByte(peeked) || !LooksLikeEnvelope();
        }

        private bool IsAmbiguousFilterByte(int b)
        {
            return b >= MinWaveformId
                && b <= MaxWaveformId
                && _synthBuffer.Remaining >= AudioConstants.MaxVoices;
        }

        private bool LooksLikeEnvelope()
        {
            var b1 = _synthBuffer.PeekAt(1);
            var b2 = _synthBuffer.PeekAt(2);
            var b3 = _synthBuffer.PeekAt(3);
            var b4 = _synthBuffer.PeekAt(4);

            var possibleStart = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            if (possibleStart is < (-EnvelopeStartThreshold) or > EnvelopeStartThreshold)
            {
                return false;
            }

            var possibleSegCount = _synthBuffer.PeekAt(SegCountOffset);
            return possibleSegCount <= MaxReasonableSegCount;
        }

        private static bool IsValidWaveform(int waveformId) =>
            waveformId is >= MinWaveformId and <= MaxWaveformId;

        private static Filter BuildFilter(
            int poleCount0,
            int poleCount1,
            int unityGain0,
            int unityGain1,
            int[] frequencies,
            int[] magnitudes,
            Envelope? modulationEnvelope
        )
        {
            var poleCounts = ImmutableArray.Create(poleCount0, poleCount1);
            var unityGain = ImmutableArray.Create(unityGain0, unityGain1);

            var (polePhase, poleMagnitude) = BuildFilterCoefficientArrays(
                frequencies,
                magnitudes,
                poleCount0,
                poleCount1
            );

            return new Filter(poleCounts, unityGain, polePhase, poleMagnitude, modulationEnvelope);
        }

        private static (
            ImmutableArray<ImmutableArray<ImmutableArray<int>>> polePhase,
            ImmutableArray<ImmutableArray<ImmutableArray<int>>> poleMagnitude
        ) BuildFilterCoefficientArrays(
            int[] frequencies,
            int[] magnitudes,
            int poleCount0,
            int poleCount1
        )
        {
            var freqArray = new ImmutableArray<int>[2][];
            var magArray = new ImmutableArray<int>[2][];

            for (var channel = 0; channel < 2; channel++)
            {
                var poles = channel == 0 ? poleCount0 : poleCount1;
                var maxPoles =
                    poles < AudioConstants.MaxFilterPairs ? poles : AudioConstants.MaxFilterPairs;
                freqArray[channel] = new ImmutableArray<int>[2];
                magArray[channel] = new ImmutableArray<int>[2];

                for (var phase = 0; phase < 2; phase++)
                {
                    freqArray[channel][phase] = BuildCoefficientArray(
                        frequencies,
                        channel,
                        phase,
                        maxPoles
                    );
                    magArray[channel][phase] = BuildCoefficientArray(
                        magnitudes,
                        channel,
                        phase,
                        maxPoles
                    );
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

        private static ImmutableArray<int> BuildCoefficientArray(
            int[] coefficients,
            int channel,
            int phase,
            int poles
        )
        {
            var builder = ImmutableArray.CreateBuilder<int>(poles);
            for (var pole = 0; pole < poles; pole++)
            {
                builder.Add(coefficients[GetFilterCoefficientIndex(channel, phase, pole)]);
            }
            return builder.ToImmutable();
        }

        private static int GetFilterCoefficientIndex(int channel, int phase, int pole) =>
            ((channel * 2) + phase) * AudioConstants.MaxFilterPairs + pole;

        private static Envelope CreateEnvelopeWithDuration(Envelope env, int duration)
        {
            return env.Segments.Count == 0 && env.StartValue != env.EndValue
                ? new Envelope(
                    env.Waveform,
                    env.StartValue,
                    env.EndValue,
                    [new Segment(duration, env.EndValue)]
                )
                : env;
        }
    }
}
