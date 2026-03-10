using Avalonia.Controls;
using Avalonia.Platform.Storage;

namespace JagFx.Desktop.Services;

public static class FileDialogService
{
    public static async Task<string?> OpenSynthFileAsync(Window window)
    {
        var result = await window.StorageProvider.OpenFilePickerAsync(new FilePickerOpenOptions
        {
            Title = "Open .synth file",
            AllowMultiple = false,
            FileTypeFilter =
            [
                new FilePickerFileType("Synth Files") { Patterns = ["*.synth"] },
                new FilePickerFileType("All Files") { Patterns = ["*"] }
            ]
        });

        return result.Count > 0 ? result[0].Path.LocalPath : null;
    }

    public static async Task<string?> SaveSynthFileAsync(Window window, string? suggestedName = null)
    {
        var result = await window.StorageProvider.SaveFilePickerAsync(new FilePickerSaveOptions
        {
            Title = "Save .synth file",
            SuggestedFileName = suggestedName ?? "untitled.synth",
            FileTypeChoices =
            [
                new FilePickerFileType("Synth Files") { Patterns = ["*.synth"] },
                new FilePickerFileType("WAV Files") { Patterns = ["*.wav"] }
            ]
        });

        return result?.Path.LocalPath;
    }

    public static async Task<string?> SaveWavFileAsync(Window window, string? suggestedName = null)
    {
        var result = await window.StorageProvider.SaveFilePickerAsync(new FilePickerSaveOptions
        {
            Title = "Export WAV file",
            SuggestedFileName = (suggestedName ?? "untitled") + ".wav",
            FileTypeChoices =
            [
                new FilePickerFileType("WAV Files") { Patterns = ["*.wav"] }
            ]
        });

        return result?.Path.LocalPath;
    }
}
