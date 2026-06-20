using System.Globalization;
using Avalonia.Data.Converters;
using JagFx.Core.Constants;

namespace JagFx.Desktop.Converters;

public class RawToGateMillisecondsConverter : IValueConverter
{
    public static readonly RawToGateMillisecondsConverter Instance = new();

    private const double CounterTicksPerSample = 256.0;

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        value is int raw
            ? raw / CounterTicksPerSample / AudioConstants.SampleRatePerMillisecond
            : 0.0;

    public object ConvertBack(
        object? value,
        Type targetType,
        object? parameter,
        CultureInfo culture
    )
    {
        return value is double milliseconds
                ? (int)
                    Math.Round(
                        milliseconds
                            * AudioConstants.SampleRatePerMillisecond
                            * CounterTicksPerSample
                    )
            : value is decimal decimalMilliseconds
                ? (int)
                    Math.Round(
                        (double)decimalMilliseconds
                            * AudioConstants.SampleRatePerMillisecond
                            * CounterTicksPerSample
                    )
            : 0;
    }
}
