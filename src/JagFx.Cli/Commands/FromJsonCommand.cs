using JagFx.Io;
using JagFx.Io.Json;
using System.CommandLine;

namespace JagFx.Cli.Commands;

public class FromJsonCommand : Command
{
    public FromJsonCommand() : base("from-json", "Convert JSON to .synth binary format")
    {
        var inputArgument = new Argument<string>("input")
        {
            Description = "Path to input .json file"
        };
        var outputArgument = new Argument<string>("output")
        {
            Description = "Path to output .synth file"
        };

        Arguments.Add(inputArgument);
        Arguments.Add(outputArgument);

        SetAction((parseResult) =>
        {
            var input = parseResult.GetValue(inputArgument);
            var output = parseResult.GetValue(outputArgument);
            return Execute(input, output);
        });
    }

    private static int Execute(string? input, string? output)
    {
        if (string.IsNullOrWhiteSpace(input) || string.IsNullOrWhiteSpace(output))
        {
            Console.Error.WriteLine("Error: Both input and output paths are required");
            return 1;
        }

        if (!CommandHelpers.ValidateInputFile(input))
            return 1;

        try
        {
            var patch = SynthJsonSerializer.DeserializeFromPath(input);
            SynthFileWriter.WriteToPath(patch, output);
            Console.WriteLine($"Wrote {output}");
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: {ex.Message}");
            return 1;
        }
    }
}
