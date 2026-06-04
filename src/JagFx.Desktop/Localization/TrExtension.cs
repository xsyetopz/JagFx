using Avalonia.Markup.Xaml;

namespace JagFx.Desktop.Localization;

public sealed class TrExtension : MarkupExtension
{
    public TrExtension() { }

    public TrExtension(string key)
    {
        Key = key;
    }

    public string? Key { get; set; }

    public override object ProvideValue(IServiceProvider serviceProvider) =>
        string.IsNullOrWhiteSpace(Key) ? string.Empty : Localization.Get(Key);
}
