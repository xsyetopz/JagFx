using JagFx.Domain.Models;
using JagFx.Io.Json;
using JagFx.TestData;
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
    public void RoundTripBinaryJsonBinaryProducesIdenticalBytes(string resourceName)
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
    public void RoundTripJsonPatchJsonPreservesJsonStructure()
    {
        var patch = SynthFileReader.Read(TestResources.CowDeath);
        var json1 = SynthJsonSerializer.Serialize(patch);
        var roundTripped = SynthJsonSerializer.Deserialize(json1);
        var json2 = SynthJsonSerializer.Serialize(roundTripped);

        Assert.Equal(json1, json2);
    }

    [Fact]
    public void ReferenceJsonFilesMatchSynthSerializerOutput()
    {
        var repoRoot = FindRepositoryRoot();
        var synthDir = Path.Combine(repoRoot, "references", "synths");
        var jsonDir = Path.Combine(repoRoot, "references", "json");

        foreach (var synthPath in Directory.EnumerateFiles(synthDir, "*.synth").Order())
        {
            var name = Path.GetFileNameWithoutExtension(synthPath);
            var jsonPath = Path.Combine(jsonDir, name + ".json");

            Assert.True(File.Exists(jsonPath), $"Missing JSON fixture for {name}");

            var patch = SynthFileReader.ReadFromPath(synthPath);
            var expectedJson = SynthJsonSerializer.Serialize(patch);
            var actualJson = TrimFinalLineEnding(File.ReadAllText(jsonPath));

            Assert.Equal(expectedJson, actualJson);
        }
    }

    [Fact]
    public void ReferenceJsonFilesRoundTripToBinaryWriterBaseline()
    {
        var repoRoot = FindRepositoryRoot();
        var synthDir = Path.Combine(repoRoot, "references", "synths");
        var jsonDir = Path.Combine(repoRoot, "references", "json");

        foreach (var synthPath in Directory.EnumerateFiles(synthDir, "*.synth").Order())
        {
            var name = Path.GetFileNameWithoutExtension(synthPath);
            var jsonPath = Path.Combine(jsonDir, name + ".json");

            Assert.True(File.Exists(jsonPath), $"Missing JSON fixture for {name}");

            var baselinePatch = SynthFileReader.ReadFromPath(synthPath);
            var baselineBytes = SynthFileWriter.Write(baselinePatch);
            var jsonPatch = SynthJsonSerializer.DeserializeFromPath(jsonPath);
            var jsonBytes = SynthFileWriter.Write(jsonPatch);

            Assert.Equal(baselineBytes, jsonBytes);
        }
    }

    #endregion

    #region Minimal JSON with Defaults

    [Fact]
    public void DeserializeMinimalJsonAppliesDefaults()
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

        _ = Assert.Single(patch.ActiveVoices);
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
    public void FilterCoefficientsMapCorrectly()
    {
        var originalBytes = TestResources.GetBytes("toa_zebak_attack_melee_roar_01");
        var patch = SynthFileReader.Read(originalBytes);

        var voiceWithFilter = patch.ActiveVoices.FirstOrDefault(v => v.Voice.Filter != null);

        Assert.True(voiceWithFilter != default, "Test fixture must contain a voice with a filter");

        var filter = voiceWithFilter.Voice.Filter!;
        var json = SynthJsonSerializer.Serialize(patch);
        var roundTripped = SynthJsonSerializer.Deserialize(json);

        var rtVoice = roundTripped.ActiveVoices.First(v => v.Voice.Filter != null);
        var rtFilter = rtVoice.Voice.Filter!;

        Assert.Equal(filter.PoleCounts[0], rtFilter.PoleCounts[0]);
        Assert.Equal(filter.PoleCounts[1], rtFilter.PoleCounts[1]);
        Assert.Equal(filter.UnityGain[0], rtFilter.UnityGain[0]);
        Assert.Equal(filter.UnityGain[1], rtFilter.UnityGain[1]);

        for (var ch = 0; ch < 2; ch++)
        {
            for (var ph = 0; ph < 2; ph++)
            {
                Assert.Equal([.. filter.PolePhase[ch][ph]], [.. rtFilter.PolePhase[ch][ph]]);
                Assert.Equal(
                    [.. filter.PoleMagnitude[ch][ph]],
                    [.. rtFilter.PoleMagnitude[ch][ph]]
                );
            }
        }
    }

    #endregion

    #region Waveform String Mapping

    [Fact]
    public void SerializeUsesStringWaveforms()
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
    public void RoundTripEmptyPatch()
    {
        var emptyPatch = new Patch(voices: [], loop: new LoopSegment(0, 0));

        var json = SynthJsonSerializer.Serialize(emptyPatch);
        var roundTripped = SynthJsonSerializer.Deserialize(json);

        Assert.Empty(roundTripped.ActiveVoices);
    }

    #endregion
    private static string FindRepositoryRoot()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory != null && !File.Exists(Path.Combine(directory.FullName, "JagFx.sln")))
        {
            directory = directory.Parent;
        }

        return directory?.FullName
            ?? throw new DirectoryNotFoundException("Could not find JagFx.sln");
    }

    private static string TrimFinalLineEnding(string text)
    {
        return text.EndsWith("\r\n", StringComparison.Ordinal) ? text[..^2]
            : text.EndsWith('\n') ? text[..^1]
            : text;
    }
}
