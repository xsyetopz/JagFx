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
        var cowDeathPatch = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(cowDeathPatch);
        _ = Assert.Single(cowDeathPatch.ActiveVoices);
        Assert.Equal(0, cowDeathPatch.Loop.BeginMs);
        Assert.Equal(0, cowDeathPatch.Loop.EndMs);
    }

    [Fact]
    public void ReadsWardOfArceuusCastCorrectly()
    {
        var wardCastPatch = SynthFileReader.Read(TestResources.WardOfArceuusCast);
        Assert.NotNull(wardCastPatch);
        Assert.True(wardCastPatch.ActiveVoices.Count >= 1);
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
    public void CowDeathAmplitudeEnvelopeHasSegments()
    {
        var cowDeathPatch = SynthFileReader.Read(TestResources.CowDeath);
        var (_, voice) = cowDeathPatch.ActiveVoices.First();
        Assert.True(
            voice.AmplitudeEnvelope.Segments.Count > 0,
            "cow_death amplitude envelope must have at least one segment"
        );
    }

    #endregion

    #region Partial Tests

    [Fact]
    public void ParsesPartialsCorrectly()
    {
        var cowDeathPatch = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(cowDeathPatch);
        var (_, voice) = cowDeathPatch.ActiveVoices.First();
        Assert.Equal(2, voice.Partials.Count);
        Assert.Equal(100, voice.Partials[0].Amplitude.Value);
    }

    [Fact]
    public void CowDeathPartialsHaveExpectedWaveformOnFrequencyEnvelope()
    {
        var cowDeathPatch = SynthFileReader.Read(TestResources.CowDeath);
        var (_, voice) = cowDeathPatch.ActiveVoices.First();
        Assert.Equal(Waveform.Sine, voice.FrequencyEnvelope.Waveform);
    }

    #endregion

    #region Filter Tests

    [Fact]
    public void FilterFileContainsVoiceWithFilter()
    {
        var zebakRoarPatch = SynthFileReader.Read(TestResources.ToaZebakAttackMeleeRoar01);
        var voiceWithFilter = zebakRoarPatch.ActiveVoices.FirstOrDefault(v =>
            v.Voice.Filter != null
        );
        Assert.True(
            voiceWithFilter != default,
            "toa_zebak_attack_melee_roar_01 must contain a voice with a filter"
        );

        var filter = voiceWithFilter.Voice.Filter!;
        Assert.Equal(2, filter.PoleCounts.Length);
        Assert.True(
            filter.PoleCounts[0] > 0 || filter.PoleCounts[1] > 0,
            "Filter must have at least one pole in one channel"
        );
    }

    #endregion

    #region Format Validation Tests

    [Theory]
    [InlineData("cow_death")]
    [InlineData("noa_melee_attack_movement")]
    [InlineData("ward_of_arceuus_cast")]
    public void AllResourcesAreValidSynthFiles(string resourceName)
    {
        var bytes = TestResources.GetBytes(resourceName);
        var resourcePatch = SynthFileReader.Read(bytes);
        Assert.NotNull(resourcePatch);
        Assert.NotNull(resourcePatch.Voices);
    }

    #endregion
}
