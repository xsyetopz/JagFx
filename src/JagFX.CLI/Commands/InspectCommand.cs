using JagFx.Core.Constants;
using JagFx.Io.Buffers;
using System.CommandLine;

namespace JagFx.Cli.Commands;

/// <summary>
/// CLI command for inspecting .synth file structure in assembly-like format.
/// </summary>
public class InspectCommand : Command
{
    public InspectCommand() : base("inspect", "Inspect synth file structure")
    {
        var fileArgument = new Argument<string>("file")
        {
            Description = "Path to .synth file"
        };

        Arguments.Add(fileArgument);

        SetAction((parseResult) =>
        {
            var filePath = parseResult.GetValue(fileArgument);
            return Execute(filePath);
        });
    }

    private static int Execute(string? filePath)
    {
        if (string.IsNullOrWhiteSpace(filePath))
        {
            Console.Error.WriteLine("Error: File path is required");
            return 1;
        }

        if (!File.Exists(filePath))
        {
            Console.Error.WriteLine($"Error: File not found: {filePath}");
            return 1;
        }

        var bytes = File.ReadAllBytes(filePath);
        var context = new InspectorContext(bytes);

        Console.WriteLine($"; File: {filePath}");
        Console.WriteLine($"; Size: {bytes.Length} bytes");
        Console.WriteLine();

        try
        {
            InspectVoices(context);
            InspectLoop(context);
            PrintSummary(context);
            return 0;
        }
        catch (Exception ex)
        {
            PrintError(context, ex);
            return 1;
        }
    }

    private static void InspectVoices(InspectorContext context)
    {
        for (var i = 0; i < AudioConstants.MaxVoices; i++)
        {
            if (context.Buffer.Remaining == 0) break;
            var marker = context.Buffer.Peek();
            if (marker == 0)
            {
                context.ReadByte("empty", $"voice {i}");
                continue;
            }

            InspectVoice(context, i);
        }
    }

    private static void InspectVoice(InspectorContext context, int voiceIndex)
    {
        var marker = context.Buffer.Peek();
        context.PrintLine($"voice {voiceIndex}", $"active, wf={GetWaveformName((byte)marker)}");

        InspectEnvelope(context, "penv");
        InspectEnvelope(context, "aenv");

        InspectOptionalLFO(context, "vib");
        InspectOptionalLFO(context, "trem");
        InspectOptionalLFO(context, "gate");

        InspectOscillators(context);

        context.ReadUSmart("echo", "feedback");
        context.ReadUSmart("", "mix");

        context.ReadUInt16("time", "dur");
        context.ReadUInt16("", "start");

        InspectFilter(context);
    }

    private static void InspectEnvelope(InspectorContext context, string _)
    {
        context.ReadByte("", "wf");
        context.ReadInt32("", "start");
        context.ReadInt32("", "end");
        var nSegs = context.ReadByte("", "segs");
        var maxSegs = context.Buffer.Remaining / 4;
        var segLimit = Math.Min(nSegs, maxSegs);
        for (var i = 0; i < segLimit; i++)
        {
            context.ReadUInt16("", $"seg{i}.dur");
            context.ReadUInt16("", $"seg{i}.peak");
        }
    }

    private static void InspectOptionalLFO(InspectorContext context, string label)
    {
        var marker = context.Buffer.Peek();
        if (marker == 0)
        {
            context.ReadByte("", $"{label}=none");
            return;
        }

        context.PrintLine(label, "present");
        InspectEnvelope(context, $"  {label}.rate");
        InspectEnvelope(context, $"  {label}.depth");
    }

    private static void InspectOscillators(InspectorContext context)
    {
        var index = 0;
        while (index < AudioConstants.MaxOscillators && context.Buffer.Remaining > 0)
        {
            var marker = context.Buffer.Peek();
            if (marker == 0)
            {
                context.ReadByte("", "osc=end");
                break;
            }

            context.ReadUSmart("", $"osc{index}");
            context.ReadSmart("", $"pitch");
            context.ReadUSmart("", $"delay");
            index++;
        }
    }

    private static void InspectFilter(InspectorContext context)
    {
        if (context.Buffer.Remaining < 1)
        {
            context.PrintLine("; filter", "none (EOF)");
            return;
        }

        var packed = context.Buffer.Peek();
        var pair0 = packed >> 4;
        var pair1 = packed & 0x0F;
        if (packed == 0)
        {
            context.ReadByte("", "filt=none");
            return;
        }

        context.ReadByte("", $"filt: ch0={pair0}, ch1={pair1}");
        context.ReadUInt16("", "unity0");
        context.ReadUInt16("", "unity1");
        var modmask = context.ReadByte("", $"modmask");

        InspectFilterPoles(context, pair0, pair1);
        InspectFilterModulation(context, pair0, pair1, modmask);
    }

    private static void InspectFilterPoles(InspectorContext context, int pair0, int pair1)
    {
        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = channel == 0 ? pair0 : pair1;
            if (pairs == 0) continue;

            for (var p = 0; p < pairs; p++)
            {
                context.ReadUInt16("", $"ch{channel}.pole{p}.freq");
                context.ReadUInt16("", $"mag");
            }
        }
    }

    private static void InspectFilterModulation(InspectorContext context, int pair0, int pair1, int modmask)
    {
        if (modmask == 0) return;

        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = channel == 0 ? pair0 : pair1;
            for (var p = 0; p < pairs; p++)
            {
                if ((modmask & (1 << (channel * 4 + p))) != 0)
                {
                    context.ReadUInt16("", $"ch{channel}.pole{p}.freq_mod");
                    context.ReadUInt16("", $"mag_mod");
                }
            }
        }
        InspectEnvelopeSegments(context);
    }

    private static void InspectEnvelopeSegments(InspectorContext context)
    {
        var segmentCount = context.ReadByte("", "env_segs");
        var maxSegments = context.Buffer.Remaining / 4;
        var segmentLimit = Math.Min(segmentCount, maxSegments);
        for (var i = 0; i < segmentLimit; i++)
        {
            context.ReadUInt16("", $"seg{i}.dur");
            context.ReadUInt16("", $"seg{i}.peak");
        }
    }

    private static void InspectLoop(InspectorContext context)
    {
        if (context.Buffer.Remaining >= 4)
        {
            context.ReadUInt16("loop", "start");
            context.ReadUInt16("", "end");
        }
    }

    private static void PrintSummary(InspectorContext context)
    {
        Console.WriteLine($"; Parsed {context.Buffer.Position}/{context.Buffer.Data.Length} bytes ({context.Buffer.Position * 100.0 / context.Buffer.Data.Length:F1}%)");
        if (context.Buffer.Remaining > 0)
        {
            Console.WriteLine($"; Remaining: {context.Buffer.Remaining} bytes unparsed");
        }
    }

    private static void PrintError(InspectorContext context, Exception ex)
    {
        Console.WriteLine($"; ERROR at 0x{context.Buffer.Position:X4}: {ex.Message}");
    }

    private static string GetWaveformName(byte id) => id switch
    {
        0 => "off",
        1 => "square",
        2 => "sine",
        3 => "saw",
        4 => "noise",
        _ => $"?({id})"
    };

    private class InspectorContext(byte[] data)
    {
        public BinaryBuffer Buffer { get; } = new(data);

        public byte ReadByte(string mnemonic, string comment)
        {
            var value = (byte)ReadAndPrint(1, mnemonic, comment, Buffer.ReadUInt8);
            return value;
        }

        public int ReadSmart(string mnemonic, string comment)
            => ReadVariableLength(() => Buffer.ReadSmart(), mnemonic, comment);

        public int ReadUSmart(string mnemonic, string comment)
            => ReadVariableLength(() => Buffer.ReadUSmart(), mnemonic, comment);


        public int ReadInt32(string mnemonic, string comment)
            => ReadAndPrint(4, mnemonic, comment, Buffer.ReadInt32BigEndian);

        public int ReadUInt16(string mnemonic, string comment)
            => ReadAndPrint(2, mnemonic, comment, Buffer.ReadUInt16BigEndian);

        private int ReadVariableLength(Func<int> readFunc, string mnemonic, string comment)
        {
            var startPosition = Buffer.Position;
            var value = readFunc();
            var lengthFromStart = Buffer.Position - startPosition;
            var bytes = Buffer.Data.Skip(startPosition).Take(lengthFromStart).ToArray();
            PrintLine(startPosition, bytes, mnemonic, comment.Length > 0 ? $"{comment}={value}" : value.ToString());
            return value;
        }

        private int ReadAndPrint(int byteCount, string mnemonic, string comment, Func<int> readFunc)
        {
            var startPosition = Buffer.Position;
            var value = readFunc();
            var bytes = Buffer.Data.Skip(startPosition).Take(byteCount).ToArray();
            PrintLine(startPosition, bytes, mnemonic, comment.Length > 0 ? $"{comment}={value}" : value.ToString());
            return value;
        }

        public void PrintLine(string mnemonic, string comment)
        {
            PrintLine(Buffer.Position, [], mnemonic, comment);
        }

        private static void PrintLine(int pos, byte[] bytes, string mnemonic, string comment)
        {
            var hex = bytes.Length > 0 ? string.Join(" ", bytes.Select(b => b.ToString("X2"))) : "";
            if (hex.Length > 18) hex = hex[..15] + "...";
            var paddedMnemonic = mnemonic.PadRight(10);
            var comma = comment.Length > 0 && mnemonic.Length > 0 ? ", " : "";
            Console.WriteLine($"{pos:X4}: {hex,-18} {paddedMnemonic}{comma}{comment}");
        }
    }
}
