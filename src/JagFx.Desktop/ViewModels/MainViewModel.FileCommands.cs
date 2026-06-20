using System.Globalization;
using CommunityToolkit.Mvvm.Input;
using JagFx.Desktop.Services;
using JagFx.Io;

namespace JagFx.Desktop.ViewModels;

public partial class MainViewModel
{
    [RelayCommand]
    private void Open() => _ = RequestOpenDialog?.Invoke();

    public bool TryLoadFromPath(string path)
    {
        try
        {
            _fileManager.LoadFromPath(path);
            StatusHint = Loc.Format("StatusLoadedFile", Path.GetFileName(path));
            return true;
        }
        catch (Exception ex)
            when (ex
                    is IOException
                        or UnauthorizedAccessException
                        or InvalidDataException
                        or ArgumentException
            )
        {
            StatusHint = Loc.Format("StatusCouldNotOpenFile", Path.GetFileName(path), ex.Message);
            return false;
        }
    }

    public void LoadFromPath(string path) => _fileManager.LoadFromPath(path);

    [RelayCommand]
    private void Save()
    {
        if (FilePath is not null)
        {
            _fileManager.Save();
            IsDirty = false;
        }
        else
        {
            _ = RequestSaveAsDialog?.Invoke();
        }
    }

    public void SaveToPath(string path) => _fileManager.SaveToPath(path);

    [RelayCommand]
    private void NavigatePatch(string direction) =>
        _fileManager.NavigatePatch(int.Parse(direction, CultureInfo.InvariantCulture));

    public async Task ExportToPathAsync(string path)
    {
        var patchModel = Patch.ToModel();
        var buffer = await SynthesisService.RenderAsync(patchModel).ConfigureAwait(true);
        WaveFileWriter.WriteToPath(buffer.ToUBytes(), path);
    }
}
