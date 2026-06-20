using System.Collections.Immutable;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using JagFx.Core.Types;
using JagFx.Domain.Models;
using JagFx.Io;
using JagFx.Synthesis.Core;
using JagFx.TestData;
using Xunit;

namespace JagFx.Synthesis.Tests.Core;

public class SynthesisBitPerfectTests
{
    [Theory]
    [InlineData(
        "cow_death",
        19845,
        "6A77E49D67D21F86B9CE45137280A716623863F1BCA7D0196DADF8266ACF999F"
    )]
    [InlineData(
        "noa_melee_attack_movement",
        26460,
        "62982B68C71FC2BDC7B604518737BC3FF57BAA141216B7A6F1E01CD3891C1348"
    )]
    [InlineData(
        "noa_melee_scratch_floor_7",
        11025,
        "BFCE5DA346A3FCEBC39DA90E45289517D9D01396F93161CD74923D0DC7FD9BC7"
    )]
    [InlineData(
        "noa_screech_melee_3",
        17640,
        "CB2045AEA745F99B32997E71BC3AAB62077B2641E3BE876D93A9BD181F62CF33"
    )]
    [InlineData(
        "relic_unlock_pulsing",
        22050,
        "0C811A3E242C4C8D54E7B5E7509D2BB95A003C084F76EB4E1843B259251B2B49"
    )]
    [InlineData(
        "relic_unlock_spinning_hover",
        33075,
        "3CDA589E81DD8B6524DDFAEA2C8370DB5A61601859FE725D60A6E8C63782E0FE"
    )]
    [InlineData(
        "spawn_bone_crack_2",
        5512,
        "495D2F2AE0BA535A7A0032D1F5BE10AD27946C2B10422B7627074F96D67D0252"
    )]
    [InlineData(
        "toa_zebak_attack_jaw_shut_02",
        8820,
        "5C98BA3ABEAF6A3682CF658FDBB9451625A681BCCF2AB2CCBBD78A4BC76D4690"
    )]
    [InlineData(
        "toa_zebak_attack_melee_enranged_jaw_snap_01",
        9922,
        "6783476760B5C348712F7A7A3166D6E37798CCE0BC89BDBCC43D3218FDEBE5B0"
    )]
    [InlineData(
        "toa_zebak_attack_melee_high_roar_02",
        27562,
        "25919BC7B1F90110AFB772CC746BF3F73DC3232C7EBC86BED82766FBCE484457"
    )]
    [InlineData(
        "toa_zebak_attack_melee_jaw_snap_shut_01",
        7717,
        "18FFC67AAAD62055EC4A39B91E6C3062D5612A9FE7A663CFA62F07833412F254"
    )]
    [InlineData(
        "toa_zebak_attack_melee_roar_01",
        26460,
        "A4D453E2E6300F45680DCF9662D4D81A3CF3B302037C739538937D2235362DE9"
    )]
    [InlineData(
        "toa_zebak_attack_roar_05",
        114660,
        "B312ACC5630135B685A31A71DF6E46D9856946CDB8EC993DA8E484B360A23536"
    )]
    [InlineData(
        "toa_zebak_ranged_enranged_gulp_02",
        7717,
        "920D6FEFAC30F1766E660543E5B091FAEC4E7E437D081EC318B713CDDA62F673"
    )]
    [InlineData(
        "ward_of_arceuus_cast",
        46305,
        "B681EAEDE14EAAC25DD0DCF7891DAF0D90E76FAED84107F2256530D3C471C42A"
    )]
    public void ReferenceSynthsRenderBitPerfectSamples(
        string resourceName,
        int expectedLength,
        string expectedSha256
    )
    {
        var patch = SynthFileReader.Read(TestResources.GetBytes(resourceName));
        var audio = PatchRenderer.Synthesize(patch, 1);

        Assert.Equal(expectedLength, audio.Length);
        Assert.Equal(expectedSha256, ComputeSha256(audio.Samples));
    }

    [Fact]
    public void PooledPatchRenderMatchesOwnedRenderAndSupportsReuse()
    {
        var patch = SynthFileReader.Read(TestResources.GetBytes("cow_death"));
        var owned = PatchRenderer.Synthesize(patch, 1);

        using (var pooled = PatchRenderer.SynthesizePooled(patch, 1))
        {
            Assert.Equal(owned.Length, pooled.Length);
            Assert.Equal(ComputeSha256(owned.Samples), ComputeSha256(pooled.Span));
        }

        using (var pooled = PatchRenderer.SynthesizePooled(patch, 1))
        {
            Assert.Equal(owned.Length, pooled.Length);
            Assert.Equal(ComputeSha256(owned.Samples), ComputeSha256(pooled.Span));
        }
    }

    [Fact]
    public void PooledVoiceRenderMatchesOwnedRender()
    {
        var patch = SynthFileReader.Read(TestResources.GetBytes("relic_unlock_pulsing"));
        var voice = patch.ActiveVoices[0].Voice;
        var owned = VoiceSynthesizer.Synthesize(voice);

        using var pooled = VoiceSynthesizer.SynthesizePooled(voice);

        Assert.Equal(owned.Length, pooled.Length);
        Assert.Equal(ComputeSha256(owned.Samples), ComputeSha256(pooled.Span));
    }

    [Fact]
    public void LoopExpansionPreservesPooledAndOwnedSampleHashes()
    {
        var patch = new Patch(
            Enumerable
                .Range(0, 10)
                .Select(index => index == 0 ? CreateBasicVoice(30) : null)
                .ToImmutableList(),
            new LoopSegment(5, 15),
            (IEnumerable<string>?)null
        );

        var owned = PatchRenderer.Synthesize(patch, 3);

        using var pooled = PatchRenderer.SynthesizePooled(patch, 3);

        Assert.Equal(1101, owned.Length);
        Assert.Equal(owned.Length, pooled.Length);
        Assert.Equal(ComputeSha256(owned.Samples), ComputeSha256(pooled.Span));
    }

    [Fact]
    public void LoopExpansionRepeatsLogicalRegionInsideActiveLength()
    {
        var patch = new Patch(
            Enumerable
                .Range(0, 10)
                .Select(index => index == 0 ? CreateBasicVoice(30) : null)
                .ToImmutableList(),
            new LoopSegment(5, 15),
            (IEnumerable<string>?)null
        );

        var single = PatchRenderer.Synthesize(patch, 1);
        var looped = PatchRenderer.Synthesize(patch, 3);
        var loopStart = new Milliseconds(5).ToSamples().Value;
        var loopStop = new Milliseconds(15).ToSamples().Value;
        var loopLength = loopStop - loopStart;
        var endOffset = loopLength * 2;

        Assert.Equal(single.Length + endOffset, looped.Length);
        for (var i = loopStart; i < loopStop; i++)
        {
            Assert.Equal(single.Samples[i], looped.Samples[i]);
            Assert.Equal(single.Samples[i], looped.Samples[i + loopLength]);
            Assert.Equal(single.Samples[i], looped.Samples[i + (loopLength * 2)]);
        }

        for (var i = loopStop; i < single.Length; i++)
        {
            Assert.Equal(single.Samples[i], looped.Samples[i + endOffset]);
        }
    }

    [Fact]
    public void FilterModulationRenderPreservesPooledAndOwnedSampleHashes()
    {
        var voice = CreateBasicVoice(40) with { Filter = CreateModulatedFilter() };
        var owned = VoiceSynthesizer.Synthesize(voice);

        using var pooled = VoiceSynthesizer.SynthesizePooled(voice);

        Assert.Equal(882, owned.Length);
        Assert.Equal(owned.Length, pooled.Length);
        Assert.Equal(ComputeSha256(owned.Samples), ComputeSha256(pooled.Span));
        Assert.Contains(owned.Samples, sample => sample != 0);
    }

    [Fact]
    public void GatedRenderSuppressesDelayedPartialAtMutedDestinationSamples()
    {
        var voice = new Voice(
            new Envelope(Waveform.Square, 1000, 1000, []),
            new Envelope(Waveform.Square, 1000, 1000, []),
            null,
            null,
            new Envelope(Waveform.Square, 512, 512, []),
            new Envelope(Waveform.Square, 512, 512, []),
            [new VoicePartial(new Percent(100), 0, new Milliseconds(1))],
            new Echo(0, 0),
            20,
            0
        );

        var audio = VoiceSynthesizer.Synthesize(voice);
        var delaySamples = new Milliseconds(1).ToSamples().Value;

        for (var sample = delaySamples; sample < 80; sample++)
        {
            if (sample % 4 is 0 or 3)
            {
                Assert.Equal(0, audio.Samples[sample]);
            }
            else
            {
                Assert.NotEqual(0, audio.Samples[sample]);
            }
        }
    }

    private static Voice CreateBasicVoice(int durationMs) =>
        new(
            new Envelope(Waveform.Square, 1000, 1000, []),
            new Envelope(Waveform.Square, 1000, 1000, []),
            null,
            null,
            null,
            null,
            [new VoicePartial(new Percent(80), 0, new Milliseconds(0))],
            new Echo(0, 0),
            durationMs,
            0
        );

    private static Filter CreateModulatedFilter()
    {
        ImmutableArray<ImmutableArray<ImmutableArray<int>>> phase =
        [
            [
                [200, 400, 0, 0],
                [500, 700, 0, 0],
            ],
            [
                [300, 600, 0, 0],
                [800, 900, 0, 0],
            ],
        ];
        ImmutableArray<ImmutableArray<ImmutableArray<int>>> magnitude =
        [
            [
                [1200, 800, 0, 0],
                [400, 200, 0, 0],
            ],
            [
                [600, 300, 0, 0],
                [200, 100, 0, 0],
            ],
        ];

        return new Filter(
            [2, 2],
            [128, 512],
            phase,
            magnitude,
            new Envelope(Waveform.Sine, 0, 65535, [new Segment(20, 32768), new Segment(20, 65535)])
        );
    }

    private static string ComputeSha256(int[] samples) => ComputeSha256(samples.AsSpan());

    private static string ComputeSha256(ReadOnlySpan<int> samples)
    {
        var bytes = new byte[samples.Length * sizeof(int)];
        MemoryMarshal.AsBytes(samples).CopyTo(bytes);
        return Convert.ToHexString(SHA256.HashData(bytes));
    }
}
