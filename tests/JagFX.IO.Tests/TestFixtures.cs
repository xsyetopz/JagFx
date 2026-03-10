namespace JagFx.Io.Tests;

public static class TestFixtures
{
    public static byte[] CowDeath => LoadResource("cow_death.synth");
    public static byte[] ProtectFromMagic => LoadResource("protect_from_magic.synth");
    public static byte[] IceCast => LoadResource("ice_cast.synth");

    private static byte[] LoadResource(string name)
    {
        var baseDir = AppContext.BaseDirectory;
        var locations = new[]
        {
            Path.Combine(baseDir, name),
            Path.Combine(baseDir, "resources", name)
        };

        foreach (var path in locations)
        {
            var absolutePath = Path.GetFullPath(path);
            if (File.Exists(absolutePath))
                return File.ReadAllBytes(absolutePath);
        }

        throw new FileNotFoundException($"Resource not found: {name}");
    }
}
