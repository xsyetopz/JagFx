using Avalonia.Markup.Xaml;

namespace JagFx.Desktop.Localization;

public sealed class TrUpperExtension : MarkupExtension
{
    public TrUpperExtension() { }

    public TrUpperExtension(string key)
    {
        Key = key;
    }

    public string? Key { get; set; }

    public override object ProvideValue(IServiceProvider serviceProvider) =>
        string.IsNullOrWhiteSpace(Key) ? string.Empty : Localization.GetUpper(Key);
}
