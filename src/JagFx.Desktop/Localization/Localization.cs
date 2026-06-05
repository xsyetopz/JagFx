using System.Globalization;
using System.Resources;

namespace JagFx.Desktop.Localization;

public static class Localization
{
    private static readonly CultureInfo DefaultCulture = CultureInfo.GetCultureInfo("en");
    private static readonly CultureInfo[] SupportedUiCultures =
    [
        CultureInfo.GetCultureInfo("fr"),
        CultureInfo.GetCultureInfo("de"),
        CultureInfo.GetCultureInfo("es"),
        CultureInfo.GetCultureInfo("it"),
        CultureInfo.GetCultureInfo("pt-BR"),
    ];

    private static readonly ResourceManager ResourceManager = new(
        "JagFx.Desktop.Lang.UiStrings",
        typeof(Localization).Assembly
    );

    public static CultureInfo CurrentCulture { get; private set; } = DefaultCulture;

    public static IReadOnlyList<CultureInfo> SupportedCultures => SupportedUiCultures;

    public static void ApplySystemCulture() =>
        SetCulture(ResolveCulture(CultureInfo.CurrentUICulture));

    public static void SetCulture(CultureInfo culture)
    {
        CurrentCulture = ResolveCulture(culture);
        CultureInfo.DefaultThreadCurrentCulture = CurrentCulture;
        CultureInfo.DefaultThreadCurrentUICulture = CurrentCulture;
        CultureInfo.CurrentCulture = CurrentCulture;
        CultureInfo.CurrentUICulture = CurrentCulture;
    }

    public static CultureInfo ResolveCulture(CultureInfo requestedCulture)
    {
        var exact = SupportedUiCultures.FirstOrDefault(culture =>
            string.Equals(culture.Name, requestedCulture.Name, StringComparison.OrdinalIgnoreCase)
        );
        if (exact is not null)
        {
            return exact;
        }

        var languageMatch = SupportedUiCultures.FirstOrDefault(culture =>
            string.Equals(
                culture.TwoLetterISOLanguageName,
                requestedCulture.TwoLetterISOLanguageName,
                StringComparison.OrdinalIgnoreCase
            )
        );
        return languageMatch ?? DefaultCulture;
    }

    public static string Get(string key) =>
        ResourceManager.GetString(key, CurrentCulture)
        ?? ResourceManager.GetString(key, DefaultCulture)
        ?? key;

    public static string GetUpper(string key) => Get(key).ToUpper(CurrentCulture);

    public static string Format(string key, params object?[] args) =>
        string.Format(CurrentCulture, Get(key), args);
}
