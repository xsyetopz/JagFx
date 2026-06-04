using System.Globalization;
using Avalonia.Data.Converters;

namespace JagFx.Desktop.Converters;

public sealed class LoopCountTextConverter : IValueConverter
{
    public static readonly LoopCountTextConverter Instance = new();

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        var count = value switch
        {
            int i => i,
            decimal dec => (int)dec,
            long l => (int)l,
            _ => 0,
        };

        return count.ToString(culture);
    }

    public object ConvertBack(
        object? value,
        Type targetType,
        object? parameter,
        CultureInfo culture
    )
    {
        if (value is string text)
        {
            var trimmed = text.Trim();
            if (int.TryParse(trimmed, NumberStyles.Integer, culture, out var parsed))
                return Math.Clamp(parsed, 0, 999);
        }

        if (value is int i)
            return Math.Clamp(i, 0, 999);

        return 0;
    }
}
