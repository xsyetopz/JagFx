using Avalonia;
using Avalonia.Controls;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public partial class EnvelopeCanvas
{
    private void ShowContextMenu(Point pos, EnvelopeGeometry geo, EnvelopeViewModel env)
    {
        var menu = new MenuFlyout();

        var pointIndex = geo.HitTest(pos);
        if (pointIndex >= 0)
        {
            _selectedIndex = pointIndex;
            InvalidateVisual();

            if (env.Segments.Count > 1)
            {
                var deleteItem = new MenuItem { Header = Loc.Get("MenuDeletePoint") };
                var idx = pointIndex;
                deleteItem.Click += (_, _) =>
                {
                    env.RemoveSegmentAt(idx);
                    _selectedIndex = -1;
                    InvalidateVisual();
                    RequestPreviewUpdate(immediate: true);
                };
                _ = menu.Items.Add(deleteItem);
            }
        }
        else
        {
            var lineIndex = geo.LineHitTest(pos);
            if (lineIndex >= 0)
            {
                var addItem = new MenuItem { Header = Loc.Get("MenuAddPointHere") };
                var li = lineIndex;
                addItem.Click += (_, _) =>
                {
                    InsertPointOnLine(li, pos, geo, env);
                    RequestPreviewUpdate(immediate: true);
                };
                _ = menu.Items.Add(addItem);
            }
            else
            {
                var addEndItem = new MenuItem { Header = Loc.Get("MenuAddPointEnd") };
                addEndItem.Click += (_, _) =>
                {
                    env.AddSegment(100, 0);
                    _selectedIndex = env.Segments.Count - 1;
                    InvalidateVisual();
                    RequestPreviewUpdate(immediate: true);
                };
                _ = menu.Items.Add(addEndItem);
            }
        }

        if (menu.Items.Count > 0)
        {
            menu.ShowAt(this, true);
        }
    }

    private void InsertPointOnLine(
        int lineIndex,
        Point pos,
        EnvelopeGeometry geo,
        EnvelopeViewModel env
    )
    {
        var totalDuration = env.Segments.Sum(s => s.Duration);
        if (totalDuration <= 0)
        {
            return;
        }

        var clickTime = geo.XToTime(pos.X, totalDuration, ScrollOffset);

        // Calculate cumulative time up to the start of the segment at lineIndex
        double segStartTime = 0;
        for (var i = 0; i < lineIndex; i++)
        {
            segStartTime += env.Segments[i].Duration;
        }

        var seg = env.Segments[lineIndex];
        var segEndTime = segStartTime + seg.Duration;

        // Clamp click time within the segment
        clickTime = Math.Clamp(clickTime, segStartTime + 1, segEndTime - 1);

        var firstDuration = (int)Math.Max(1, clickTime - segStartTime);
        var secondDuration = Math.Max(1, seg.Duration - firstDuration);

        var interpolatedLevel = EnvelopeGeometry.YToPeakLevel(
            pos.Y,
            Bounds.Height,
            env,
            DisplayMode
        );

        // Shorten existing segment to the first portion
        env.Segments[lineIndex].Duration = firstDuration;

        // Insert new segment with remainder duration and original target level
        env.InsertSegment(lineIndex + 1, secondDuration, seg.TargetLevel);

        // Set the shortened segment's target to the interpolated level
        env.Segments[lineIndex].TargetLevel = interpolatedLevel;

        _selectedIndex = lineIndex;
        InvalidateVisual();
        RequestPreviewUpdate(immediate: true);
    }
}
