using Avalonia;
using Optris.Icons.Avalonia;
using Optris.Icons.Avalonia.MaterialDesign;

namespace JagFx.Desktop;

internal static class Program
{
    [STAThread]
    public static void Main(string[] args) =>
        BuildAvaloniaApp().StartWithClassicDesktopLifetime(args);

    public static AppBuilder BuildAvaloniaApp()
    {
        _ = IconProvider.Current.Register<MaterialDesignIconProvider>();

        return AppBuilder.Configure<App>().UsePlatformDetect().WithInterFont().LogToTrace();
    }
}
