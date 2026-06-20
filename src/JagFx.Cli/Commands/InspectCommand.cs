using System.CommandLine;
using System.Globalization;
using System.Text;
using JagFx.Core.Constants;
using JagFx.Io;
using JagFx.Io.Buffers;

namespace JagFx.Cli.Commands;

/// <summary>
/// CLI command for inspecting .synth file structure in assembly-like format.
/// </summary>
internal sealed class InspectCommand : Command
{
    public InspectCommand()
        : base("inspect", "Inspect synth file structure")
    {
        var fileArgument = new Argument<string>("file") { Description = "Path to .synth file" };

        Arguments.Add(fileArgument);

        SetAction(
            (parseResult) =>
            {
                var filePath = parseResult.GetValue(fileArgument);
                return Execute(filePath);
            }
        );
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
        var synthCursor = new SynthInspectionCursor(bytes);

        Console.WriteLine($"; File: {filePath}");
        Console.WriteLine($"; Size: {bytes.Length} bytes");
        Console.WriteLine();

        try
        {
            Consume(SynthFileReader.Read(bytes));
            InspectVoices(synthCursor);
            InspectLoop(synthCursor);
            PrintSummary(synthCursor);
            return 0;
        }
        catch (Exception ex)
        {
            PrintError(synthCursor, ex);
            return 1;
        }
    }

    private static void InspectVoices(SynthInspectionCursor synthCursor)
    {
        for (var i = 0; i < AudioConstants.MaxVoices; i++)
        {
            if (synthCursor.Buffer.Remaining == 0)
            {
                break;
            }

            var marker = synthCursor.Buffer.Peek();
            if (marker == 0)
            {
                Consume(synthCursor.ReadByte("empty", $"voice {i}"));
                continue;
            }

            InspectVoice(synthCursor, i);
        }
    }

    private static void InspectVoice(SynthInspectionCursor synthCursor, int voiceIndex)
    {
        var marker = synthCursor.Buffer.Peek();
        synthCursor.PrintLine($"voice {voiceIndex}", $"active, wf={GetWaveformName((byte)marker)}");

        InspectEnvelope(synthCursor);
        InspectEnvelope(synthCursor);

        InspectOptionalLFO(synthCursor, "vib");
        InspectOptionalLFO(synthCursor, "trem");
        InspectOptionalLFO(synthCursor, "gate");

        InspectOscillators(synthCursor);

        Consume(synthCursor.ReadUSmart("echo", "feedback"));
        Consume(synthCursor.ReadUSmart("", "mix"));

        Consume(synthCursor.ReadUInt16("time", "dur"));
        Consume(synthCursor.ReadUInt16("", "start"));

        InspectFilter(synthCursor);
    }

    private static void InspectEnvelope(SynthInspectionCursor synthCursor)
    {
        Consume(synthCursor.ReadByte("", "wf"));
        Consume(synthCursor.ReadInt32("", "start"));
        Consume(synthCursor.ReadInt32("", "end"));
        var nSegs = synthCursor.ReadByte("", "segs");
        var maxSegs = synthCursor.Buffer.Remaining / 4;
        var segLimit = nSegs < maxSegs ? nSegs : maxSegs;
        for (var i = 0; i < segLimit; i++)
        {
            Consume(synthCursor.ReadUInt16("", $"seg{i}.dur"));
            Consume(synthCursor.ReadUInt16("", $"seg{i}.peak"));
        }
    }

    private static void InspectOptionalLFO(SynthInspectionCursor synthCursor, string label)
    {
        var marker = synthCursor.Buffer.Peek();
        if (marker == 0)
        {
            Consume(synthCursor.ReadByte("", $"{label}=none"));
            return;
        }

        synthCursor.PrintLine(label, "present");
        InspectEnvelope(synthCursor);
        InspectEnvelope(synthCursor);
    }

    private static void InspectOscillators(SynthInspectionCursor synthCursor)
    {
        var index = 0;
        while (index < AudioConstants.MaxOscillators && synthCursor.Buffer.Remaining > 0)
        {
            var marker = synthCursor.Buffer.Peek();
            if (marker == 0)
            {
                Consume(synthCursor.ReadByte("", "osc=end"));
                break;
            }

            Consume(synthCursor.ReadUSmart("", $"osc{index}"));
            Consume(synthCursor.ReadSmart("", $"pitch"));
            Consume(synthCursor.ReadUSmart("", $"delay"));
            index++;
        }
    }

    private static void InspectFilter(SynthInspectionCursor synthCursor)
    {
        if (synthCursor.Buffer.Remaining < 1)
        {
            synthCursor.PrintLine("; filter", "none (EOF)");
            return;
        }

        var packed = synthCursor.Buffer.Peek();
        var pair0 = packed >> 4;
        var pair1 = packed & 0x0F;
        if (packed == 0)
        {
            Consume(synthCursor.ReadByte("", "filt=none"));
            return;
        }

        Consume(synthCursor.ReadByte("", $"filt: ch0={pair0}, ch1={pair1}"));
        Consume(synthCursor.ReadUInt16("", "unity0"));
        Consume(synthCursor.ReadUInt16("", "unity1"));
        var modmask = synthCursor.ReadByte("", $"modmask");

        InspectFilterPoles(synthCursor, pair0, pair1);
        InspectFilterModulation(synthCursor, pair0, pair1, modmask);
    }

    private static void InspectFilterPoles(SynthInspectionCursor synthCursor, int pair0, int pair1)
    {
        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = channel == 0 ? pair0 : pair1;
            if (pairs == 0)
            {
                continue;
            }

            for (var p = 0; p < pairs; p++)
            {
                Consume(synthCursor.ReadUInt16("", $"ch{channel}.pole{p}.freq"));
                Consume(synthCursor.ReadUInt16("", $"mag"));
            }
        }
    }

    private static void InspectFilterModulation(
        SynthInspectionCursor synthCursor,
        int pair0,
        int pair1,
        int modmask
    )
    {
        if (modmask == 0)
        {
            return;
        }

        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = channel == 0 ? pair0 : pair1;
            for (var p = 0; p < pairs; p++)
            {
                if ((modmask & (1 << (channel * 4 + p))) != 0)
                {
                    Consume(synthCursor.ReadUInt16("", $"ch{channel}.pole{p}.freq_mod"));
                    Consume(synthCursor.ReadUInt16("", $"mag_mod"));
                }
            }
        }
        InspectEnvelopeSegments(synthCursor);
    }

    private static void InspectEnvelopeSegments(SynthInspectionCursor synthCursor)
    {
        var segmentCount = synthCursor.ReadByte("", "env_segs");
        var maxSegments = synthCursor.Buffer.Remaining / 4;
        var segmentLimit = segmentCount < maxSegments ? segmentCount : maxSegments;
        for (var i = 0; i < segmentLimit; i++)
        {
            Consume(synthCursor.ReadUInt16("", $"seg{i}.dur"));
            Consume(synthCursor.ReadUInt16("", $"seg{i}.peak"));
        }
    }

    private static void InspectLoop(SynthInspectionCursor synthCursor)
    {
        if (synthCursor.Buffer.Remaining >= 4)
        {
            Consume(synthCursor.ReadUInt16("loop", "start"));
            Consume(synthCursor.ReadUInt16("", "end"));
        }
    }

    private static void PrintSummary(SynthInspectionCursor synthCursor)
    {
        Console.WriteLine(
            $"; Parsed {synthCursor.Buffer.Position}/{synthCursor.Buffer.Bytes.Length} bytes ({synthCursor.Buffer.Position * 100.0 / synthCursor.Buffer.Bytes.Length:F1}%)"
        );
        if (synthCursor.Buffer.Remaining > 0)
        {
            Console.WriteLine($"; Remaining: {synthCursor.Buffer.Remaining} bytes unparsed");
        }
    }

    private static void PrintError(SynthInspectionCursor synthCursor, Exception ex) =>
        Console.WriteLine($"; ERROR at 0x{synthCursor.Buffer.Position:X4}: {ex.Message}");

    private static string GetWaveformName(byte id) =>
        id switch
        {
            0 => "off",
            1 => "square",
            2 => "sine",
            3 => "saw",
            4 => "noise",
            _ => $"?({id})",
        };

    private static void Consume<T>(T value) => GC.KeepAlive(value);

    private sealed class SynthInspectionCursor(byte[] synthBytes)
    {
        public BinaryBuffer Buffer { get; } = new(synthBytes);

        public byte ReadByte(string mnemonic, string comment)
        {
            var value = (byte)ReadAndPrint(1, mnemonic, comment, Buffer.ReadUInt8);
            return value;
        }

        public int ReadSmart(string mnemonic, string comment) =>
            ReadVariableLength(() => Buffer.ReadSmart(), mnemonic, comment);

        public int ReadUSmart(string mnemonic, string comment) =>
            ReadVariableLength(() => Buffer.ReadUSmart(), mnemonic, comment);

        public int ReadInt32(string mnemonic, string comment) =>
            ReadAndPrint(4, mnemonic, comment, Buffer.ReadInt32BigEndian);

        public int ReadUInt16(string mnemonic, string comment) =>
            ReadAndPrint(2, mnemonic, comment, Buffer.ReadUInt16BigEndian);

        private int ReadVariableLength(Func<int> readFunc, string mnemonic, string comment)
        {
            var startPosition = Buffer.Position;
            var value = readFunc();
            var lengthFromStart = Buffer.Position - startPosition;
            var bytes = CopyBytes(startPosition, lengthFromStart);
            PrintLine(
                startPosition,
                bytes,
                mnemonic,
                comment.Length > 0
                    ? $"{comment}={value}"
                    : value.ToString(CultureInfo.InvariantCulture)
            );
            return value;
        }

        private int ReadAndPrint(int byteCount, string mnemonic, string comment, Func<int> readFunc)
        {
            var startPosition = Buffer.Position;
            var value = readFunc();
            var bytes = CopyBytes(startPosition, byteCount);
            PrintLine(
                startPosition,
                bytes,
                mnemonic,
                comment.Length > 0
                    ? $"{comment}={value}"
                    : value.ToString(CultureInfo.InvariantCulture)
            );
            return value;
        }

        public void PrintLine(string mnemonic, string comment) =>
            PrintLine(Buffer.Position, [], mnemonic, comment);

        private byte[] CopyBytes(int startPosition, int byteCount)
        {
            var bytes = new byte[byteCount];
            Array.Copy(Buffer.Bytes, startPosition, bytes, 0, byteCount);
            return bytes;
        }

        private static void PrintLine(int pos, byte[] bytes, string mnemonic, string comment)
        {
            var hex = FormatHex(bytes);
            if (hex.Length > 18)
            {
                hex = hex[..15] + "...";
            }

            var paddedMnemonic = mnemonic.PadRight(10);
            var comma = comment.Length > 0 && mnemonic.Length > 0 ? ", " : "";
            Console.WriteLine($"{pos:X4}: {hex.PadRight(18)} {paddedMnemonic}{comma}{comment}");
        }

        private static string FormatHex(byte[] bytes)
        {
            if (bytes.Length == 0)
            {
                return string.Empty;
            }

            var builder = new StringBuilder(bytes.Length * 3 - 1);
            for (var i = 0; i < bytes.Length; i++)
            {
                if (i > 0)
                {
                    Consume(builder.Append(' '));
                }

                Consume(builder.Append(bytes[i].ToString("X2", CultureInfo.InvariantCulture)));
            }

            return builder.ToString();
        }
    }
}
