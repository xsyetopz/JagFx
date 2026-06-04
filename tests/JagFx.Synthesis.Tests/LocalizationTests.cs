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
    public void ResolveCultureMapsSupportedSystemLanguages(
        string requestedName,
        string expectedName
    )
    {
        var culture = Localization.ResolveCulture(CultureInfo.GetCultureInfo(requestedName));
        Assert.Equal(expectedName, culture.Name);
    }

    [Fact]
    public void GetUsesFallbackLanguageResourceForFrCa()
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
    public void GetUsesPtBrResourceForPtPt()
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
