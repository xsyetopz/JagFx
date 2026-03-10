using JagFx.Domain.Models;
using JagFx.Io;
using JagFx.TestData;
using Xunit;

namespace JagFx.Io.Tests;

public class SynthFileReaderTests
{
    private static readonly int[] expected = [0, 1];

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
