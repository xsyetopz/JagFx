namespace JagFx.Desktop.Controls;

/// <summary>
/// Shared pan and zoom state for canvas controls that support both interactions.
/// </summary>
public sealed class CanvasInteractionHelper
{
    public static readonly int[] ZoomLevels = [1, 2, 4];

    private bool _isPanning;
    private double _panStartX;
    private double _panStartOffset;

    public bool IsPanning => _isPanning;

    public void BeginPan(double startX, double currentOffset)
    {
        _isPanning = true;
        _panStartX = startX;
        _panStartOffset = currentOffset;
    }

    public double ComputePanOffset(double currentX, double maxOffset)
    {
        var dx = _panStartX - currentX;
        return Math.Clamp(_panStartOffset + dx, 0, maxOffset);
    }

    public void EndPan()
    {
        _isPanning = false;
    }

    public int StepZoom(int currentZoom, double wheelDelta)
    {
        var index = Array.IndexOf(ZoomLevels, currentZoom);
        if (index < 0) index = 0;

        if (wheelDelta > 0 && index < ZoomLevels.Length - 1)
            return ZoomLevels[index + 1];
        if (wheelDelta < 0 && index > 0)
            return ZoomLevels[index - 1];
        return currentZoom;
    }
}
