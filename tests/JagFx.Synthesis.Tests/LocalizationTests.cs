using System.Globalization;
using JagFx.Desktop.Localization;
using Xunit;

namespace JagFx.Synthesis.Tests;

public class LocalizationTests
{
    private static readonly object CultureLock = new();

    [Theory]
    [InlineData("fr-CA", "fr")]
    [InlineData("de-AT", "de")]
    [InlineData("es-MX", "es")]
    [InlineData("it-CH", "it")]
    [InlineData("pt-PT", "pt-BR")]
    [InlineData("en-US", "en")]
    public void ResolveCulture_maps_supported_system_languages(
        string requestedName,
        string expectedName
    )
    {
        var culture = Localization.ResolveCulture(CultureInfo.GetCultureInfo(requestedName));
        Assert.Equal(expectedName, culture.Name);
    }

    [Fact]
    public void Get_uses_the_fallback_language_resource_for_fr_ca()
    {
        lock (CultureLock)
        {
            var original = Localization.CurrentCulture;

            try
            {
                Localization.SetCulture(CultureInfo.GetCultureInfo("fr-CA"));
                Assert.Equal("Commencez ici", Localization.Get("WorkflowStartTitle"));
            }
            finally
            {
                Localization.SetCulture(original);
            }
        }
    }

    [Fact]
    public void Get_uses_the_pt_br_resource_for_pt_pt()
    {
        lock (CultureLock)
        {
            var original = Localization.CurrentCulture;

            try
            {
                Localization.SetCulture(CultureInfo.GetCultureInfo("pt-PT"));
                Assert.Equal("Comece aqui", Localization.Get("WorkflowStartTitle"));
            }
            finally
            {
                Localization.SetCulture(original);
            }
        }
    }
}
