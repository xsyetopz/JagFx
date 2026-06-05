using System.Globalization;
using Avalonia.Data.Converters;

namespace JagFx.Desktop.Converters;

public class IntToDoubleConverter : IValueConverter
{
    public static readonly IntToDoubleConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        value is int i ? (double)i : (object)0.0;

    public object ConvertBack(
        object? value,
        Type targetType,
        object? parameter,
        CultureInfo culture
    )
    {
        return value is double d ? (int)Math.Round(d)
            : value is decimal dec ? (int)Math.Round((double)dec)
            : (object)0;
    }
}
