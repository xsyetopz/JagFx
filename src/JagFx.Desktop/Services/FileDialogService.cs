using Avalonia.Controls;
using Avalonia.Platform.Storage;
using Avalonia.Threading;

namespace JagFx.Desktop.Services;

public static class FileDialogService
{
    private static readonly SemaphoreSlim DialogLock = new(1, 1);

    public static async Task<string?> OpenSynthFileAsync(Window window)
    {
        if (!await DialogLock.WaitAsync(0).ConfigureAwait(true))
            return null;

        try
        {
            await ActivateOwnerAsync(window).ConfigureAwait(true);
            var result = await window
                .StorageProvider.OpenFilePickerAsync(
                    new FilePickerOpenOptions
                    {
                        Title = "Open .synth file",
                        AllowMultiple = false,
                        FileTypeFilter =
                        [
                            new FilePickerFileType("Synth Files") { Patterns = ["*.synth"] },
                            new FilePickerFileType("All Files") { Patterns = ["*"] },
                        ],
                    }
                )
                .ConfigureAwait(true);

            return result.Count > 0 ? result[0].Path.LocalPath : null;
        }
        finally
        {
            _ = DialogLock.Release();
        }
    }

    public static async Task<string?> SaveSynthFileAsync(
        Window window,
        string? suggestedName = null
    )
    {
        if (!await DialogLock.WaitAsync(0).ConfigureAwait(true))
            return null;

        try
        {
            await ActivateOwnerAsync(window).ConfigureAwait(true);
            var result = await window
                .StorageProvider.SaveFilePickerAsync(
                    new FilePickerSaveOptions
                    {
                        Title = "Save .synth file",
                        SuggestedFileName = suggestedName ?? "untitled.synth",
                        FileTypeChoices =
                        [
                            new FilePickerFileType("Synth Files") { Patterns = ["*.synth"] },
                            new FilePickerFileType("WAV Files") { Patterns = ["*.wav"] },
                        ],
                    }
                )
                .ConfigureAwait(true);

            return result?.Path.LocalPath;
        }
        finally
        {
            _ = DialogLock.Release();
        }
    }

    public static async Task<string?> SaveWavFileAsync(Window window, string? suggestedName = null)
    {
        if (!await DialogLock.WaitAsync(0).ConfigureAwait(true))
            return null;

        try
        {
            await ActivateOwnerAsync(window).ConfigureAwait(true);
            var result = await window
                .StorageProvider.SaveFilePickerAsync(
                    new FilePickerSaveOptions
                    {
                        Title = "Export WAV file",
                        SuggestedFileName = (suggestedName ?? "untitled") + ".wav",
                        FileTypeChoices =
                        [
                            new FilePickerFileType("WAV Files") { Patterns = ["*.wav"] },
                        ],
                    }
                )
                .ConfigureAwait(true);

            return result?.Path.LocalPath;
        }
        finally
        {
            _ = DialogLock.Release();
        }
    }

    private static Task ActivateOwnerAsync(Window window) =>
        Dispatcher
            .UIThread.InvokeAsync(() =>
            {
                window.Activate();
                _ = window.Focus();
            })
            .GetTask();
}
