namespace JagFx.Desktop.Controls;

/// <summary>
/// Shared pan and zoom state for canvas controls that support both interactions.
/// </summary>
public sealed class CanvasPanZoomController
{
    public static readonly int[] ZoomLevels = [1, 2, 4];

    private double _panStartX;
    private double _panStartOffset;

    public bool IsPanning { get; private set; }

    public void BeginPan(double startX, double currentOffset)
    {
        IsPanning = true;
        _panStartX = startX;
        _panStartOffset = currentOffset;
    }

    public double ComputePanOffset(double currentX, double maxOffset)
    {
        var dx = _panStartX - currentX;
        return Math.Clamp(_panStartOffset + dx, 0, maxOffset);
    }

    public void EndPan() => IsPanning = false;

    public static int StepZoom(int currentZoom, double wheelDelta)
    {
        var index = Array.IndexOf(ZoomLevels, currentZoom);
        if (index < 0)
        {
            index = 0;
        }

        return wheelDelta > 0 && index < ZoomLevels.Length - 1 ? ZoomLevels[index + 1]
            : wheelDelta < 0 && index > 0 ? ZoomLevels[index - 1]
            : currentZoom;
    }
}
