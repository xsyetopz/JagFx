using Avalonia;
using Avalonia.Media;

namespace JagFx.Desktop.Controls.Canvases;

public partial class EnvelopeCanvas
{
    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        if (IsSelected)
        {
            context.DrawRectangle(null, ThemeColors.AccentPen1, new Rect(0.5, 0.5, w - 1, h - 1));
        }

        using var clip = context.PushClip(new Rect(0, 0, w, h));

        if (UseAnalysisGrid)
        {
            AnalysisGridRenderer.Draw(context, new Rect(0, 0, w, h), includeVertical: false);
        }
        else
        {
            DrawGrid(context, w, h, ZoomLevel, ScrollOffset);
        }

        var env = Envelope;
        if (env is null || env.Segments.Count == 0)
        {
            return;
        }

        var geometry = EnvelopeGeometry.Compute(env, w, h, ZoomLevel, ScrollOffset, DisplayMode);
        DrawEnvelope(context, geometry);
    }

    private static void DrawGrid(
        DrawingContext context,
        double w,
        double h,
        int zoomLevel,
        double scrollOffset
    )
    {
        // Horizontal grid: scale subdivisions with zoom
        // 1x -> 4 divisions (25% steps, 3 lines), 2x -> 8 (12.5%, 7 lines), 4x -> 16 (6.25%, 15 lines)
        var hDivisions = 4 * zoomLevel;
        for (var i = 1; i < hDivisions; i++)
        {
            var y = ThemeColors.Snap(h * i / (double)hDivisions);
            var pen = i == hDivisions / 2 ? ThemeColors.MidPen : ThemeColors.GridFaintPen;
            context.DrawLine(pen, new Point(0, y), new Point(w, y));
        }

        // Vertical grid: subdivide visible width, offset by scroll for alignment
        var visibleW = w - EnvelopeGeometry.Padding * 2;
        var vDivisions = 8 * zoomLevel;
        var step = visibleW / vDivisions;
        var offsetMod = scrollOffset % step;
        for (var i = 0; i <= vDivisions; i++)
        {
            var x = ThemeColors.Snap(EnvelopeGeometry.Padding + i * step - offsetMod);
            if (x < EnvelopeGeometry.Padding || x > w - EnvelopeGeometry.Padding)
            {
                continue;
            }

            context.DrawLine(ThemeColors.GridFaintPen, new Point(x, 0), new Point(x, h));
        }
    }

    private void DrawEnvelope(DrawingContext context, EnvelopeGeometry geo)
    {
        var lineBrush = LineColor ?? ThemeColors.AccentBrush;
        var linePen = new Pen(lineBrush, 1);
        var points = new Point[geo.Points.Length];
        for (var i = 0; i < geo.Points.Length; i++)
        {
            points[i] = ThemeColors.Snap(geo.Points[i]);
        }

        for (var i = 1; i < points.Length - 1; i++)
        {
            context.DrawLine(linePen, points[i], points[i + 1]);
        }

        const double s = 3;
        const double selR = 5;
        for (
            var i =
                1 /* hides StartValue */
            ;
            i < points.Length;
            i++
        )
        {
            var pt = points[i];

            // Selection highlight for breakpoints (index i corresponds to segment i-1)
            if (_selectedIndex >= 0 && i == _selectedIndex + 1)
            {
                context.DrawEllipse(SelectionBrush, SelectionPen, pt, selR, selR);
            }

            context.DrawLine(
                ThemeColors.MarkerPen,
                new Point(pt.X - s, pt.Y - s),
                new Point(pt.X + s, pt.Y + s)
            );
            context.DrawLine(
                ThemeColors.MarkerPen,
                new Point(pt.X - s, pt.Y + s),
                new Point(pt.X + s, pt.Y - s)
            );
        }
    }
}
