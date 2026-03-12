using JagFx.Io;
using JagFx.Io.Json;
using System.CommandLine;

namespace JagFx.Cli.Commands;

public class ToJsonCommand : Command
{
    public ToJsonCommand() : base("to-json", "Convert .synth binary to JSON format")
    {
        var inputArgument = new Argument<string>("input")
        {
            Description = "Path to input .synth file"
        };
        var outputArgument = new Argument<string?>("output")
        {
            Description = "Path to output .json file (stdout if omitted)",
            Arity = ArgumentArity.ZeroOrOne
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
        if (string.IsNullOrWhiteSpace(input))
        {
            Console.Error.WriteLine("Error: Input path is required");
            return 1;
        }

        if (!File.Exists(input))
        {
            Console.Error.WriteLine($"Error: File not found: {input}");
            return 1;
        }

        try
        {
            var patch = SynthFileReader.ReadFromPath(input);
            var json = SynthJsonSerializer.Serialize(patch);

            if (string.IsNullOrWhiteSpace(output))
            {
                Console.WriteLine(json);
            }
            else
            {
                File.WriteAllText(output, json);
                Console.WriteLine($"Wrote {output}");
            }

            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: {ex.Message}");
            return 1;
        }
    }
}
