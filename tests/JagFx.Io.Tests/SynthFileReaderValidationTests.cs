using Xunit;

namespace JagFx.Io.Tests;

public class SynthFileReaderValidationTests
{
    [Fact]
    public void ReadRejectsGitLfsPointerFiles()
    {
        var pointer =
            """
            version https://git-lfs.github.com/spec/v1
            oid sha256:ba0494ff046823e2e11143beff9c0c705189afb8c810742f2e798b68f0f7baed
            size 130
            """u8.ToArray();

        var ex = Assert.Throws<SynthFileException>(() => SynthFileReader.Read(pointer));

        Assert.Contains("Git LFS pointer", ex.Message);
    }
}
