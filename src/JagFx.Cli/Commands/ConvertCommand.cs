using JagFx.Io;
using JagFx.Synthesis.Core;
using System.CommandLine;

namespace JagFx.Cli.Commands;

/// <summary>
/// CLI command for converting .synth files to .wav format.
/// Supports both positional and flag-based arguments.
/// </summary>
public static class ConvertCommand
{
    private static readonly string[] SupportedInputFormats = [".synth"];
    private static readonly string[] SupportedOutputFormats = [".wav"];

    public static void Configure(RootCommand root)
    {
        var inputOpt = new Option<string?>("-i", "--input")
        {
            Description = "Input .synth file"
        };
        var outputOpt = new Option<string?>("-o", "--output")
        {
            Description = "Output .wav file"
        };
        var loopsOpt = new Option<int?>("-l", "--loops")
        {
            Description = "Loop count"
        };

        root.Options.Add(inputOpt);
        root.Options.Add(outputOpt);
        root.Options.Add(loopsOpt);

        var inputArg = new Argument<string?>("input") { Arity = ArgumentArity.ZeroOrOne };
        var outputArg = new Argument<string?>("output") { Arity = ArgumentArity.ZeroOrOne };
        var loopsArg = new Argument<int?>("loopCount") { Arity = ArgumentArity.ZeroOrOne };

        root.Add(inputArg);
        root.Add(outputArg);
        root.Add(loopsArg);

        root.SetAction(parseResult =>
        {
            var input = parseResult.GetValue(inputArg) ?? parseResult.GetValue(inputOpt);
            var output = parseResult.GetValue(outputArg) ?? parseResult.GetValue(outputOpt);
            var loops = parseResult.GetValue(loopsArg) ?? parseResult.GetValue(loopsOpt) ?? 1;
            return Execute(input, output, loops);
        });
    }

    private static int Execute(string? inputPath, string? outputPath, int loopCount)
    {
        if (string.IsNullOrWhiteSpace(inputPath) || string.IsNullOrWhiteSpace(outputPath))
        {
            Console.Error.WriteLine("Error: Input and output files are required.");
            Console.Error.WriteLine("Usage: jagfx input.synth output.wav [loopCount]");
            Console.Error.WriteLine("   or: jagfx -i input.synth -o output.wav [-l 4]");
            return 1;
        }

        if (!CommandHelpers.ValidateInputFile(inputPath))
            return 1;

        if (!CommandHelpers.ValidateFormats(inputPath, outputPath, SupportedInputFormats, SupportedOutputFormats))
            return 1;

        return ProcessConversion(inputPath, outputPath, loopCount);
    }

    private static int ProcessConversion(string inputPath, string outputPath, int loopCount)
    {
        try
        {
            Console.WriteLine($"Converting: {inputPath}");
            Console.WriteLine($"Output: {outputPath}");
            Console.WriteLine($"Loop count: {loopCount}");

            var patch = SynthFileReader.ReadFromPath(inputPath);
            var audio = PatchRenderer.Synthesize(patch, loopCount);
            WaveFileWriter.WriteToPath(audio.ToBytes16LE(), outputPath, bitsPerSample: 16);

            Console.WriteLine($"Successfully wrote {outputPath}");
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: Failed to process file: {ex.Message}");
            return 1;
        }
    }
}
