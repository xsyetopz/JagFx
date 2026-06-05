using System.Globalization;
using Avalonia.Data.Converters;

namespace JagFx.Desktop.Converters;

public class RawToPercentConverter : IValueConverter
{
    public static readonly RawToPercentConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        value is int raw ? raw / 655.35 : (object)0.0;

    public object ConvertBack(
        object? value,
        Type targetType,
        object? parameter,
        CultureInfo culture
    )
    {
        return value is double d ? (int)Math.Round(d * 655.35)
            : value is decimal dec ? (int)Math.Round((double)dec * 655.35)
            : (object)0;
    }
}
