using System.Diagnostics;
using JagFx.Io;
using JagFx.Synthesis.Data;

namespace JagFx.Desktop.Services;

public class AudioPlaybackService : IDisposable
{
    private Process? _playbackProcess;
    private string? _tempWavPath;

    public event Action? PlaybackFinished;

    public bool IsPlaying => _playbackProcess is { HasExited: false };

    public async Task PlayAsync(AudioBuffer buffer)
    {
        Stop();

        _tempWavPath = Path.Combine(Path.GetTempPath(), $"jagfx_{Guid.NewGuid():N}.wav");
        await Task.Run(() => WaveFileWriter.WriteToPath(buffer.ToBytes16LE(), _tempWavPath, bitsPerSample: 16));
        _playbackProcess = StartAudioProcess(_tempWavPath);

        if (_playbackProcess is not null)
        {
            var process = _playbackProcess;
            _ = Task.Run(async () =>
            {
                try
                {
                    await process.WaitForExitAsync();
                    PlaybackFinished?.Invoke();
                }
                catch { /* process may have been killed */ }
            });
        }
    }

    private static Process? StartAudioProcess(string path)
    {
        if (OperatingSystem.IsMacOS())
            return Process.Start("afplay", path);
        if (OperatingSystem.IsLinux())
            return Process.Start("aplay", path);
        return Process.Start(new ProcessStartInfo(path) { UseShellExecute = true });
    }

    public void Stop()
    {
        if (_playbackProcess is { HasExited: false })
        {
            try { _playbackProcess.Kill(); }
            catch { /* process may have exited */ }
        }

        _playbackProcess?.Dispose();
        _playbackProcess = null;
        CleanupTempFile();
    }

    private void CleanupTempFile()
    {
        if (_tempWavPath is null) return;
        try { File.Delete(_tempWavPath); }
        catch { /* best effort */ }
        _tempWavPath = null;
    }

    public void Dispose()
    {
        Stop();
        GC.SuppressFinalize(this);
    }
}
