using Avalonia.Controls;
using Avalonia.Data.Converters;
using JagFX.Domain.Models;
using System.Globalization;

namespace JagFX.Desktop.Views.Inspector;

public partial class EnvelopeInspector : UserControl
{
    public EnvelopeInspector()
    {
        InitializeComponent();
    }
}

public class WaveformConverter : IValueConverter
{
    public static readonly WaveformConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        value is Waveform w ? (int)w : 0;

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        value is int i ? (Waveform)i : Waveform.Off;
}
