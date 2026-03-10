using System.Globalization;
using Avalonia.Data.Converters;

namespace JagFx.Desktop.Converters;

public class RawToPercentConverter : IValueConverter
{
    public static readonly RawToPercentConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is int raw)
            return raw / 655.35;
        return 0.0;
    }

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is double d)
            return (int)Math.Round(d * 655.35);
        if (value is decimal dec)
            return (int)Math.Round((double)dec * 655.35);
        return 0;
    }
}
