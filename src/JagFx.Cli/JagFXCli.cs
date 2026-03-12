using JagFx.Cli.Commands;
using System.CommandLine;

namespace JagFx.Cli;

/// <summary>
/// Entry point for the JagFx CLI application.
/// </summary>
public static class JagFxCli
{
    /// <summary>
    /// Runs the CLI with the provided arguments.
    /// </summary>
    public static Task<int> RunAsync(string[] args)
    {
        var rootCommand = BuildRootCommand();
        var parseResult = rootCommand.Parse(args);
        return parseResult.InvokeAsync(new InvocationConfiguration());
    }

    private static RootCommand BuildRootCommand()
    {
        var rootCommand = new RootCommand("JagFx CLI");
        ConvertCommand.Configure(rootCommand);

        rootCommand.Add(new InspectCommand());
        rootCommand.Add(new ToJsonCommand());
        rootCommand.Add(new FromJsonCommand());
        return rootCommand;
    }
}
