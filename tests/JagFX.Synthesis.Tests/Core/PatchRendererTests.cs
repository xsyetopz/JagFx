using JagFX.Core.Constants;
using JagFX.Domain.Models;
using JagFX.Io;
using JagFX.Synthesis.Core;
using JagFX.TestData;
using Xunit;

namespace JagFX.Synthesis.Tests;

public class PatchRendererTests
{
    #region Single Voice Tests

    [Fact]
    public void SynthesizesCowDeath1Voice()
    {
        var file = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(file);
        var audio = PatchRenderer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(AudioConstants.SampleRate, audio.SampleRate);
        Assert.Equal(19889 - 44, audio.Length);
    }

    [Fact]
    public void SynthesizesWardOfArceuusCast()
    {
        var file = SynthFileReader.Read(TestResources.WardOfArceuusCast);
        Assert.NotNull(file);
        var audio = PatchRenderer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(AudioConstants.SampleRate, audio.SampleRate);
    }

    #endregion

    #region Edge Cases

    [Fact]
    public void EmptyFileProducesEmptyBuffer()
    {
        var emptyFile = new Patch(
            voices: [],
            loop: new LoopSegment(0, 0)
        );
        var audio = PatchRenderer.Synthesize(emptyFile, 1);
        Assert.Equal(0, audio.Length);
    }

    #endregion

    #region Multiple File Tests

    [Theory]
    [InlineData("cow_death")]
    [InlineData("noa_melee_attack_movement")]
    [InlineData("ward_of_arceuus_cast")]
    public void VariousFiles_ProduceValidAudio(string resourceName)
    {
        var bytes = TestResources.GetBytes(resourceName);
        var file = SynthFileReader.Read(bytes);
        Assert.NotNull(file);

        var audio = PatchRenderer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(AudioConstants.SampleRate, audio.SampleRate);
        Assert.True(audio.Samples.Length > 0);
    }

    #endregion
}
