namespace JagFx.Cli;

public static class Program
{
    public static Task<int> Main(string[] args) => JagFxCli.RunAsync(args);
}
