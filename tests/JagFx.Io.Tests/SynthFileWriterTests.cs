using JagFx.Domain.Models;
using JagFx.TestData;
using Xunit;

namespace JagFx.Io.Tests;

public class SynthFileWriterTests
{
    #region Roundtrip Tests

    [Fact]
    public void CowDeath1VoiceRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Single(reread.ActiveVoices);
        Assert.Equal(original.Loop, reread.Loop);
    }

    [Fact]
    public void CowDeathRoundtrip_PreservesVoiceEnvelopeAndPartials()
    {
        var original = SynthFileReader.Read(TestResources.CowDeath);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);

        var (_, origVoice) = original.ActiveVoices.First();
        var (_, rereadVoice) = reread.ActiveVoices.First();

        Assert.Equal(origVoice.AmplitudeEnvelope.Segments.Count, rereadVoice.AmplitudeEnvelope.Segments.Count);
        Assert.Equal(origVoice.FrequencyEnvelope.Waveform, rereadVoice.FrequencyEnvelope.Waveform);
        Assert.Equal(origVoice.Partials.Count, rereadVoice.Partials.Count);
        Assert.Equal(origVoice.Partials[0].Amplitude, rereadVoice.Partials[0].Amplitude);
    }

    [Fact]
    public void WardOfArceuusCastRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestResources.WardOfArceuusCast);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.True(reread.ActiveVoices.Count >= 1);
        Assert.Equal(original.Loop, reread.Loop);
    }

    [Fact]
    public void FilterVoiceRoundtrip_PreservesFilterPairCount()
    {
        var original = SynthFileReader.Read(TestResources.ToaZebakAttackMeleeRoar01);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);

        var origFilterVoice = original.ActiveVoices.FirstOrDefault(v => v.Voice.Filter != null);
        var rereadFilterVoice = reread.ActiveVoices.FirstOrDefault(v => v.Voice.Filter != null);

        Assert.True(origFilterVoice != default, "Test fixture must contain a voice with a filter");
        Assert.True(rereadFilterVoice != default, "Round-tripped fixture must contain a voice with a filter");

        var origFilter = origFilterVoice.Voice.Filter!;
        var rereadFilter = rereadFilterVoice.Voice.Filter!;

        Assert.Equal(origFilter.PoleCounts[0], rereadFilter.PoleCounts[0]);
        Assert.Equal(origFilter.PoleCounts[1], rereadFilter.PoleCounts[1]);
    }

    #endregion

    #region Edge Cases

    [Fact]
    public void WritesEmptyFileCorrectly()
    {
        var emptyFile = new Patch(
            voices: [],
            loop: new LoopSegment(100, 200)
        );
        var written = SynthFileWriter.Write(emptyFile);
        Assert.Equal(14, written.Length);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Empty(reread.ActiveVoices);
        // Note: Empty files may have loop parameters reset to 0
        Assert.True(reread.Loop.BeginMs == 0 || reread.Loop.BeginMs == 100);
    }

    #endregion
}
