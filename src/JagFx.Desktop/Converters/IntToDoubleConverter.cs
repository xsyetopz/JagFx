using System.Globalization;
using Avalonia.Data.Converters;

namespace JagFx.Desktop.Converters;

public class IntToDoubleConverter : IValueConverter
{
    public static readonly IntToDoubleConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is int i)
            return (double)i;
        return 0.0;
    }

    public object ConvertBack(
        object? value,
        Type targetType,
        object? parameter,
        CultureInfo culture
    )
    {
        if (value is double d)
            return (int)Math.Round(d);
        if (value is decimal dec)
            return (int)Math.Round((double)dec);
        return 0;
    }
}
