using Avalonia;
using Avalonia.Controls;
using JagFx.Desktop.Controls;
using JagFx.Desktop.ViewModels;
using JagFx.Domain.Models;

namespace JagFx.Desktop.Views.SignalChain;

public partial class SignalChainPanel
{
    private static void UpdateDimming(EnvelopeSlot slot, EnvelopeViewModel envelope) =>
        slot.Container.Opacity = envelope.IsEmpty ? 0.22 : 1.0;

    private void UpdateSelection(SignalChainSlot selectedSlot)
    {
        foreach (var slot in _slots)
        {
            var selected = slot.Slot == selectedSlot;
            slot.Container.BorderBrush = selected
                ? ThemeColors.AccentBrush
                : ThemeColors.CellBorderBrush;
            slot.Container.BorderThickness = new Thickness(1);
        }
    }

    // Column 3 cells (filter-related)
    private static readonly HashSet<SignalChainSlot> FilterCells =
    [
        SignalChainSlot.PoleZero,
        SignalChainSlot.Filter,
        SignalChainSlot.Bode,
    ];

    private void UpdateGridMode(GridMode mode)
    {
        var colDefs = MatrixGrid.ColumnDefinitions;
        var rowDefs = MatrixGrid.RowDefinitions;

        switch (mode)
        {
            case GridMode.Main:
                // 3×3: cols 0-2 equal, col 3 = 0
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                    {
                        slot.Container.Width = double.NaN;
                    }
                }
                for (var i = 0; i < 3; i++)
                {
                    colDefs[i].Width = new GridLength(1, GridUnitType.Star);
                }

                colDefs[3].Width = new GridLength(0);
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                {
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                }

                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch;
                break;

            case GridMode.Filter:
                // Only col 3 visible (stacked vertically in 3 rows)
                for (var i = 0; i < 3; i++)
                {
                    colDefs[i].Width = new GridLength(0);
                }

                colDefs[3].Width = GridLength.Auto;
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                {
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                }

                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Center;
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                    {
                        slot.Container.Width = 300;
                    }
                }
                break;

            case GridMode.Both:
                // Full 3×4 — equal columns
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                    {
                        slot.Container.Width = double.NaN;
                    }
                }
                colDefs[0].Width = new GridLength(1, GridUnitType.Star);
                colDefs[1].Width = new GridLength(1, GridUnitType.Star);
                colDefs[2].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                {
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                }

                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch;
                break;

            default:
                break;
        }
    }
}
