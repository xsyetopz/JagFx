using JagFx.Domain.Models;
using JagFx.Io.Json;
using JagFx.TestData;
using System.Collections.Immutable;
using Xunit;

namespace JagFx.Io.Tests.Json;

public class SynthJsonSerializerTests
{
    #region Round-trip Tests

    [Theory]
    [InlineData("cow_death")]
    [InlineData("noa_melee_attack_movement")]
    [InlineData("noa_melee_scratch_floor_7")]
    [InlineData("noa_screech_melee_3")]
    [InlineData("relic_unlock_pulsing")]
    [InlineData("relic_unlock_spinning_hover")]
    [InlineData("spawn_bone_crack_2")]
    [InlineData("toa_zebak_attack_jaw_shut_02")]
    [InlineData("toa_zebak_attack_melee_enranged_jaw_snap_01")]
    [InlineData("toa_zebak_attack_melee_high_roar_02")]
    [InlineData("toa_zebak_attack_melee_jaw_snap_shut_01")]
    [InlineData("toa_zebak_attack_melee_roar_01")]
    [InlineData("toa_zebak_attack_roar_05")]
    [InlineData("toa_zebak_ranged_enranged_gulp_02")]
    [InlineData("ward_of_arceuus_cast")]
    public void RoundTrip_Binary_Json_Binary_ProducesIdenticalBytes(string resourceName)
    {
        var originalBytes = TestResources.GetBytes(resourceName);
        var patch = SynthFileReader.Read(originalBytes);

        // Use binary round-trip as baseline (some files have pre-existing
        // reader/writer asymmetries unrelated to JSON serialization)
        var baselineBytes = SynthFileWriter.Write(patch);

        var json = SynthJsonSerializer.Serialize(patch);
        var roundTripped = SynthJsonSerializer.Deserialize(json);
        var roundTrippedBytes = SynthFileWriter.Write(roundTripped);

        Assert.Equal(baselineBytes, roundTrippedBytes);
    }

    [Fact]
    public void RoundTrip_Json_Patch_Json_PreservesJsonStructure()
    {
        var patch = SynthFileReader.Read(TestResources.CowDeath);
        var json1 = SynthJsonSerializer.Serialize(patch);
        var roundTripped = SynthJsonSerializer.Deserialize(json1);
        var json2 = SynthJsonSerializer.Serialize(roundTripped);

        Assert.Equal(json1, json2);
    }

    #endregion

    #region Minimal JSON with Defaults

    [Fact]
    public void Deserialize_MinimalJson_AppliesDefaults()
    {
        const string minimalJson = """
        {
            "voices": [
                {
                    "frequencyEnvelope": {
                        "waveform": "sine",
                        "startValue": 32768,
                        "endValue": 32768,
                        "segments": []
                    },
                    "amplitudeEnvelope": {
                        "waveform": "off",
                        "startValue": 0,
                        "endValue": 65535,
                        "segments": [
                            { "duration": 16384, "targetLevel": 65535 },
                            { "duration": 49152, "targetLevel": 0 }
                        ]
                    },
                    "durationMs": 500
                }
            ]
        }
        """;

        var patch = SynthJsonSerializer.Deserialize(minimalJson);

        Assert.Single(patch.ActiveVoices);
        var voice = patch.ActiveVoices[0].Voice;

        // Defaults applied
        Assert.Null(voice.PitchLfo);
        Assert.Null(voice.AmplitudeLfo);
        Assert.Null(voice.GapOffEnvelope);
        Assert.Null(voice.GapOnEnvelope);
        Assert.Empty(voice.Partials);
        Assert.Equal(0, voice.Echo.DelayMilliseconds);
        Assert.Equal(0, voice.Echo.FeedbackPercent);
        Assert.Equal(0, voice.OffsetMs);
        Assert.Null(voice.Filter);
        Assert.Equal(new LoopSegment(0, 0), patch.Loop);

        // Explicit values preserved
        Assert.Equal(Waveform.Sine, voice.FrequencyEnvelope.Waveform);
        Assert.Equal(500, voice.DurationMs);
        Assert.Equal(2, voice.AmplitudeEnvelope.Segments.Count);
    }

    #endregion

    #region Filter Coefficient Mapping

    [Fact]
    public void FilterCoefficients_MapCorrectly()
    {
        var originalBytes = TestResources.GetBytes("toa_zebak_attack_melee_roar_01");
        var patch = SynthFileReader.Read(originalBytes);

        var voiceWithFilter = patch.ActiveVoices
            .FirstOrDefault(v => v.Voice.Filter != null);

        Assert.True(voiceWithFilter != default, "Test fixture must contain a voice with a filter");

        var filter = voiceWithFilter.Voice.Filter!;
        var json = SynthJsonSerializer.Serialize(patch);
        var roundTripped = SynthJsonSerializer.Deserialize(json);

        var rtVoice = roundTripped.ActiveVoices
            .First(v => v.Voice.Filter != null);
        var rtFilter = rtVoice.Voice.Filter!;

        Assert.Equal(filter.PoleCounts[0], rtFilter.PoleCounts[0]);
        Assert.Equal(filter.PoleCounts[1], rtFilter.PoleCounts[1]);
        Assert.Equal(filter.UnityGain[0], rtFilter.UnityGain[0]);
        Assert.Equal(filter.UnityGain[1], rtFilter.UnityGain[1]);

        for (var ch = 0; ch < 2; ch++)
        {
            for (var ph = 0; ph < 2; ph++)
            {
                Assert.Equal(
                    filter.PolePhase[ch][ph].ToArray(),
                    rtFilter.PolePhase[ch][ph].ToArray());
                Assert.Equal(
                    filter.PoleMagnitude[ch][ph].ToArray(),
                    rtFilter.PoleMagnitude[ch][ph].ToArray());
            }
        }
    }

    #endregion

    #region Waveform String Mapping

    [Fact]
    public void Serialize_UsesStringWaveforms()
    {
        var patch = SynthFileReader.Read(TestResources.CowDeath);
        var json = SynthJsonSerializer.Serialize(patch);

        Assert.DoesNotContain("\"waveform\": 0", json);
        Assert.DoesNotContain("\"waveform\": 1", json);
        Assert.DoesNotContain("\"waveform\": 2", json);

        Assert.Contains("\"waveform\":", json);
    }

    #endregion

    #region Empty Patch

    [Fact]
    public void RoundTrip_EmptyPatch()
    {
        var emptyPatch = new Patch(
            voices: ImmutableList<Voice?>.Empty,
            loop: new LoopSegment(0, 0));

        var json = SynthJsonSerializer.Serialize(emptyPatch);
        var roundTripped = SynthJsonSerializer.Deserialize(json);

        Assert.Empty(roundTripped.ActiveVoices);
    }

    #endregion
}
