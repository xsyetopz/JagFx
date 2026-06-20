using JagFx.Desktop.Controls;
using JagFx.Desktop.ViewModels;
using Xunit;

namespace JagFx.Synthesis.Tests.Controls;

public class EnvelopeGeometryTests
{
    [Fact]
    public void FullScaleGraphPlotsFileTargetLevelDirectly()
    {
        var envelope = new EnvelopeViewModel { StartValue = 0, EndValue = 100 };
        envelope.AddSegment(32768, 0);

        var geometry = EnvelopeGeometry.Compute(envelope, canvasWidth: 400, canvasHeight: 240);

        Assert.Equal(236, geometry.Points[1].Y);
    }
}
