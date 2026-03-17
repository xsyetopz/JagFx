using JagFx.Domain.Models;
using JagFx.TestData;
using Xunit;

namespace JagFx.Io.Tests;

public class SynthFileReaderTests
{
    #region Voice Count Tests

    [Fact]
    public void ReadsCowDeath1VoiceCorrectly()
    {
        var result = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(result);
        Assert.Single(result.ActiveVoices);
        Assert.Equal(0, result.Loop.BeginMs);
        Assert.Equal(0, result.Loop.EndMs);
    }

    [Fact]
    public void ReadsWardOfArceuusCastCorrectly()
    {
        var result = SynthFileReader.Read(TestResources.WardOfArceuusCast);
        Assert.NotNull(result);
        Assert.True(result.ActiveVoices.Count >= 1);
    }

    #endregion

    #region Envelope Tests

    [Fact]
    public void ParsesEnvelopeFormsCorrectly()
    {
        var cow = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(cow);
        var (_, cowVoice) = cow.ActiveVoices.First();
        Assert.Equal(Waveform.Sine, cowVoice.FrequencyEnvelope.Waveform);
    }

    [Fact]
    public void CowDeath_AmplitudeEnvelope_HasSegments()
    {
        var result = SynthFileReader.Read(TestResources.CowDeath);
        var (_, voice) = result.ActiveVoices.First();
        Assert.True(voice.AmplitudeEnvelope.Segments.Count > 0,
            "cow_death amplitude envelope must have at least one segment");
    }

    #endregion

    #region Partial Tests

    [Fact]
    public void ParsesPartialsCorrectly()
    {
        var result = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(result);
        var (_, voice) = result.ActiveVoices.First();
        Assert.Equal(2, voice.Partials.Count);
        Assert.Equal(100, voice.Partials[0].Amplitude.Value);
    }

    [Fact]
    public void CowDeath_Partials_HaveExpectedWaveformOnFrequencyEnvelope()
    {
        var result = SynthFileReader.Read(TestResources.CowDeath);
        var (_, voice) = result.ActiveVoices.First();
        Assert.Equal(Waveform.Sine, voice.FrequencyEnvelope.Waveform);
    }

    #endregion

    #region Filter Tests

    [Fact]
    public void FilterFile_ContainsVoiceWithFilter()
    {
        var result = SynthFileReader.Read(TestResources.ToaZebakAttackMeleeRoar01);
        var voiceWithFilter = result.ActiveVoices.FirstOrDefault(v => v.Voice.Filter != null);
        Assert.True(voiceWithFilter != default, "toa_zebak_attack_melee_roar_01 must contain a voice with a filter");

        var filter = voiceWithFilter.Voice.Filter!;
        Assert.Equal(2, filter.PoleCounts.Length);
        Assert.True(filter.PoleCounts[0] > 0 || filter.PoleCounts[1] > 0,
            "Filter must have at least one pole in one channel");
    }

    #endregion

    #region Format Validation Tests

    [Theory]
    [InlineData("cow_death")]
    [InlineData("noa_melee_attack_movement")]
    [InlineData("ward_of_arceuus_cast")]
    public void AllResources_AreValidSynthFiles(string resourceName)
    {
        var bytes = TestResources.GetBytes(resourceName);
        var result = SynthFileReader.Read(bytes);
        Assert.NotNull(result);
        Assert.NotNull(result.Voices);
    }

    #endregion
}
