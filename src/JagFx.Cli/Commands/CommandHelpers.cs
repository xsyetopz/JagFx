namespace JagFx.Cli.Commands;

/// <summary>
/// Shared utilities for CLI commands.
/// </summary>
public static class CommandHelpers
{
    private const int ConsoleWidth = 78;

    /// <summary>
    /// Validates that the input file exists.
    /// </summary>
    public static bool ValidateInputFile(string path)
    {
        if (!File.Exists(path))
        {
            Console.Error.WriteLine($"Error: Input file not found: {path}");
            return false;
        }
        return true;
    }

    /// <summary>
    /// Validates input and output file formats.
    /// </summary>
    public static bool ValidateFormats(
        string inputPath,
        string outputPath,
        string[] supportedInputFormats,
        string[] supportedOutputFormats)
    {
        var inputExt = GetExtension(inputPath);
        var outputExt = GetExtension(outputPath);

        if (inputExt == ".tone")
        {
            Console.WriteLine("TBD: .tone file format not yet supported.");
            Console.WriteLine(" Mod Surma video showcased .tone files in a directory structure,");
            Console.WriteLine("but their binary format is unknown.");
            return false;
        }

        if (!supportedInputFormats.Contains(inputExt))
        {
            Console.Error.WriteLine($"Error: Unsupported input format: {inputExt}");
            Console.Error.WriteLine($"Supported formats: {string.Join(", ", supportedInputFormats)}");
            return false;
        }
        if (!supportedOutputFormats.Contains(outputExt))
        {
            Console.Error.WriteLine($"Error: Unsupported output format: {outputExt}");
            Console.Error.WriteLine($"Supported formats: {string.Join(", ", supportedOutputFormats)}");
            return false;
        }

        return true;
    }

    /// <summary>
    /// Gets the lowercase file extension from a path.
    /// </summary>
    public static string GetExtension(string path)
    {
        return Path.GetExtension(path).ToLowerInvariant();
    }

    /// <summary>
    /// Formats bytes as human-readable string.
    /// </summary>
    public static string FormatBytes(long bytes)
    {
        string[] suffixes = ["B", "KB", "MB", "GB"];
        int suffixIndex = 0;
        double size = bytes;

        while (size >= 1024 && suffixIndex < suffixes.Length - 1)
        {
            size /= 1024;
            suffixIndex++;
        }

        return $"{size:F2} {suffixes[suffixIndex]}";
    }

    /// <summary>
    /// Prints a formatted header for command output.
    /// </summary>
    public static void PrintHeader(string title)
    {
        var padding = Math.Max(0, ConsoleWidth - title.Length) / 2;
        var leftPad = new string('═', padding);
        var rightPad = new string('═', ConsoleWidth - title.Length - padding);

        Console.WriteLine($"╔{leftPad}{title}{rightPad}╗");
    }

    /// <summary>
    /// Prints a footer line.
    /// </summary>
    public static void PrintFooter()
    {
        Console.WriteLine("╚" + new string('═', ConsoleWidth) + "╝");
    }
}
