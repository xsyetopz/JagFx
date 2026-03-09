using JagFX.Desktop.ViewModels;
using JagFX.Io;

namespace JagFX.Desktop.Services;

/// <summary>
/// Handles patch file I/O: load, save, and directory-based navigation.
/// </summary>
public class PatchFileManager
{
    private readonly PatchViewModel _patch;

    public string PatchName { get; private set; } = "untitled";
    public string? FilePath { get; private set; }

    public event Action? FileChanged;

    public PatchFileManager(PatchViewModel patch)
    {
        _patch = patch;
    }

    public void LoadFromPath(string path)
    {
        var patch = SynthFileReader.ReadFromPath(path);
        _patch.Load(patch);
        FilePath = path;
        PatchName = Path.GetFileNameWithoutExtension(path);
        FileChanged?.Invoke();
    }

    public void Save()
    {
        if (FilePath is null) return;
        var model = _patch.ToModel();
        SynthFileWriter.WriteToPath(model, FilePath);
        FileChanged?.Invoke();
    }

    public void SaveToPath(string path)
    {
        var model = _patch.ToModel();
        SynthFileWriter.WriteToPath(model, path);
        FilePath = path;
        PatchName = Path.GetFileNameWithoutExtension(path);
        FileChanged?.Invoke();
    }

    public void NavigatePatch(int direction)
    {
        if (FilePath is null) return;

        var dir = Path.GetDirectoryName(FilePath);
        if (dir is null) return;

        var files = Directory.GetFiles(dir, "*.synth")
            .OrderBy(f => f)
            .ToArray();

        var currentIndex = Array.IndexOf(files, FilePath);
        if (currentIndex < 0) return;

        var newIndex = currentIndex + direction;
        if (newIndex >= 0 && newIndex < files.Length)
            LoadFromPath(files[newIndex]);
    }
}
