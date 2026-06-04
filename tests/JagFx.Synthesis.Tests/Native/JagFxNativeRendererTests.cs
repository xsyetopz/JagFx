using JagFx.Native;
using JagFx.TestData;
using Xunit;

namespace JagFx.Synthesis.Tests.Native;

public class JagFxNativeRendererTests
{
    [Fact]
    public void RenderPcm16LeReportsRequiredBufferSize()
    {
        var status = JagFxNativeRenderer.RenderPcm16Le(
            TestResources.CowDeath,
            [],
            out var bytesWritten
        );

        Assert.Equal(JagFxNativeStatus.BufferTooSmall, status);
        Assert.True(bytesWritten > 0);
        Assert.Equal(0, bytesWritten % 2);
    }

    [Fact]
    public void RenderPcm16LeWritesCowDeathPcm()
    {
        var probe = JagFxNativeRenderer.RenderPcm16Le(
            TestResources.CowDeath,
            [],
            out var requiredBytes
        );

        Assert.Equal(JagFxNativeStatus.BufferTooSmall, probe);

        var destination = new byte[requiredBytes];
        var status = JagFxNativeRenderer.RenderPcm16Le(
            TestResources.CowDeath,
            destination,
            out var bytesWritten
        );

        Assert.Equal(JagFxNativeStatus.Success, status);
        Assert.Equal(requiredBytes, bytesWritten);
        Assert.Contains(destination, b => b != 0);
    }

    [Fact]
    public void RenderPcm16LeRejectsEmptyInput()
    {
        var status = JagFxNativeRenderer.RenderPcm16Le([], [], out var bytesWritten);

        Assert.Equal(JagFxNativeStatus.InvalidArgument, status);
        Assert.Equal(0, bytesWritten);
    }
}
