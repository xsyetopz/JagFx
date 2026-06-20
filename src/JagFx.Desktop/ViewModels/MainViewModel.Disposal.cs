namespace JagFx.Desktop.ViewModels;

public partial class MainViewModel
{
    public void Dispose()
    {
        _debounceTimer?.Dispose();
        StopPositionTimer();
        _renderCts?.Cancel();
        _renderCts?.Dispose();
        _playback.Dispose();
        UnsubscribeVoiceChanges();
        GC.SuppressFinalize(this);
    }
}
