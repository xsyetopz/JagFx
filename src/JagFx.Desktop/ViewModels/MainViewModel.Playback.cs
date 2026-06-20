using Avalonia.Threading;
using CommunityToolkit.Mvvm.Input;
using JagFx.Desktop.Services;
using JagFx.Synthesis.Audio;

namespace JagFx.Desktop.ViewModels;

public partial class MainViewModel
{
    [RelayCommand]
    private async Task TogglePlayAsync()
    {
        _playback.Stop();
        StopPositionTimer();

        var buffer = _cachedBuffer;

        if (buffer is null || buffer.Length == 0)
        {
            buffer = await SynthesizeAndCacheAsync().ConfigureAwait(true);
        }
        else if (_bufferStale)
        {
            buffer = await SynthesizeAndCacheAsync().ConfigureAwait(true);
        }
        else if (_playback.HasWavFile)
        {
            IsPlaying = true;
            _playback.PlayFromCache();
            StartPositionTimer(buffer.Length / (double)buffer.SampleRate);
            return;
        }

        if (buffer is { Length: > 0 })
        {
            IsPlaying = true;
            await _playback.PlayAsync(buffer).ConfigureAwait(true);
            StartPositionTimer(buffer.Length / (double)buffer.SampleRate);
        }
    }

    private async Task<AudioBuffer> SynthesizeAndCacheAsync()
    {
        var patchModel = Patch.ToModel();
        var voiceFilter = PlaySingleVoice ? Patch.SelectedVoiceIndex : -1;

        patchModel = DefaultLoopIfUnset(patchModel);

        var buffer = await SynthesisService
            .RenderAsync(patchModel, loopCount: EffectiveLoopCount, voiceFilter: voiceFilter)
            .ConfigureAwait(true);
        _cachedBuffer = buffer;
        _bufferStale = false;

        // Always compute loop timing from patch domain object (loop region may have been defaulted above)
        if (!patchModel.ActiveVoices.IsEmpty)
        {
            var maxDurationMs = patchModel.ActiveVoices.Max(v =>
                v.Voice.DurationMs + v.Voice.OffsetMs
            );
            _singlePassDuration = maxDurationMs / 1000.0;
            var loopBeginMs = patchModel.Loop.BeginMs;
            var loopEndMs = patchModel.Loop.EndMs;
            if (loopEndMs > loopBeginMs && _singlePassDuration > 0)
            {
                _loopCycleDuration = (loopEndMs - loopBeginMs) / 1000.0;
                var totalMs = _singlePassDuration * 1000.0;
                _loopStartNormalized = loopBeginMs / totalMs;
                _loopEndNormalized = loopEndMs / totalMs;
            }
            else
            {
                _loopCycleDuration = 0;
            }
        }

        return buffer;
    }

    [RelayCommand]
    private void Stop()
    {
        _stoppingManually = true;
        _playback.Stop();
        StopPositionTimer();
        IsPlaying = false;
        _stoppingManually = false;
    }

    private void StartPositionTimer(double durationSeconds)
    {
        StopPositionTimer();
        _playbackDuration = durationSeconds;
        _playbackStart = DateTime.UtcNow;
        _positionTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(33) };
        _positionTimer.Tick += OnPositionTick;
        _positionTimer.Start();
    }

    private void OnPositionTick(object? sender, EventArgs e)
    {
        var elapsed = (DateTime.UtcNow - _playbackStart).TotalSeconds;

        if (IsLooping && LoopCount == 0 && _loopCycleDuration > 0)
        {
            if (elapsed < _singlePassDuration)
            {
                // First pass: linear sweep across full waveform
                PlaybackPosition = elapsed / _singlePassDuration;
            }
            else
            {
                // Subsequent passes: cycle through loop region only
                var loopElapsed = elapsed - _singlePassDuration;
                var cyclePos = (loopElapsed % _loopCycleDuration) / _loopCycleDuration;
                PlaybackPosition =
                    _loopStartNormalized + cyclePos * (_loopEndNormalized - _loopStartNormalized);
            }
        }
        else if (IsLooping && LoopCount == 0 && _singlePassDuration > 0)
        {
            // No loop region defined: cycle through full pass
            PlaybackPosition = (elapsed % _singlePassDuration) / _singlePassDuration;
        }
        else
        {
            PlaybackPosition = Math.Clamp(elapsed / _playbackDuration, 0, 1);
            if (PlaybackPosition >= 1.0)
            {
                StopPositionTimer();
                IsPlaying = false;
            }
        }
    }

    private void StopPositionTimer()
    {
        if (_positionTimer is not null)
        {
            _positionTimer.Tick -= OnPositionTick;
            _positionTimer.Stop();
            _positionTimer = null;
        }
        PlaybackPosition = 0;
    }

    private void OnPlaybackFinished()
    {
        Dispatcher.UIThread.Post(() =>
        {
            if (
                IsLooping
                && LoopCount == 0
                && !_stoppingManually
                && _cachedBuffer is { Length: > 0 }
            )
            {
                // Silently restart afplay — position continues cycling via modulo
                _playback.ReplayFromExistingFile();
                return;
            }

            StopPositionTimer();
            IsPlaying = false;
        });
    }
}
