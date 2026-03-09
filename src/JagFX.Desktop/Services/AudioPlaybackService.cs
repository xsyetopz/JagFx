using System.Diagnostics;
using JagFX.Io;
using JagFX.Synthesis.Data;

namespace JagFX.Desktop.Services;

public class AudioPlaybackService : IDisposable
{
    private Process? _playbackProcess;
    private string? _tempWavPath;

    public bool IsPlaying => _playbackProcess is { HasExited: false };

    public void Play(AudioBuffer buffer)
    {
        Stop();

        _tempWavPath = Path.Combine(Path.GetTempPath(), $"jagfx_{Guid.NewGuid():N}.wav");
        WaveFileWriter.WriteToPath(buffer.ToUBytes(), _tempWavPath);
        _playbackProcess = Process.Start(new ProcessStartInfo(_tempWavPath) { UseShellExecute = true });
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
