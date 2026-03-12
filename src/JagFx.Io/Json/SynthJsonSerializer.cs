using System.Text.Json;
using JagFx.Domain.Models;

namespace JagFx.Io.Json;

public static class SynthJsonSerializer
{
    private static readonly JsonSerializerOptions Options = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull
    };

    public static string Serialize(Patch patch)
    {
        var json = SynthJsonMapper.ToJson(patch);
        return JsonSerializer.Serialize(json, Options);
    }

    public static Patch Deserialize(string json)
    {
        var patchJson = JsonSerializer.Deserialize<PatchJson>(json, Options)
            ?? throw new JsonException("Failed to deserialize JSON: result was null");
        return SynthJsonMapper.FromJson(patchJson);
    }

    public static void SerializeToPath(Patch patch, string path)
    {
        var json = Serialize(patch);
        File.WriteAllText(path, json);
    }

    public static Patch DeserializeFromPath(string path)
    {
        var json = File.ReadAllText(path);
        return Deserialize(json);
    }
}
