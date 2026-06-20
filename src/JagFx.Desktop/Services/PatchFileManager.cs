using JagFx.Desktop.ViewModels;
using JagFx.Io;

namespace JagFx.Desktop.Services;

/// <summary>
/// Handles patch file I/O: load, save, and directory-based navigation.
/// </summary>
public class PatchFileManager(PatchViewModel patch)
{
    private readonly PatchViewModel _patch = patch;

    public string PatchName { get; private set; } = "untitled";
    public string? FilePath { get; private set; }

    public event Action? FileChanged;

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
        if (FilePath is null)
        {
            return;
        }

        var patchModel = _patch.ToModel();
        SynthFileWriter.WriteToPath(patchModel, FilePath);
        FileChanged?.Invoke();
    }

    public void SaveToPath(string path)
    {
        var patchModel = _patch.ToModel();
        SynthFileWriter.WriteToPath(patchModel, path);
        FilePath = path;
        PatchName = Path.GetFileNameWithoutExtension(path);
        FileChanged?.Invoke();
    }

    public void NavigatePatch(int direction)
    {
        if (FilePath is null)
        {
            return;
        }

        var dir = Path.GetDirectoryName(FilePath);
        if (dir is null)
        {
            return;
        }

        var files = Directory.GetFiles(dir, "*.synth").OrderBy(f => f).ToArray();

        var currentIndex = Array.IndexOf(files, FilePath);
        if (currentIndex < 0)
        {
            return;
        }

        var newIndex = currentIndex + direction;
        if (newIndex >= 0 && newIndex < files.Length)
        {
            LoadFromPath(files[newIndex]);
        }
    }
}
