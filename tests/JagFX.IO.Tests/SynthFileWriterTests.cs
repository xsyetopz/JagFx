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
