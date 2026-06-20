using System.Collections.Specialized;
using System.ComponentModel;
using Avalonia.Threading;
using JagFx.Core.Constants;
using JagFx.Desktop.Services;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Audio;
using JagFx.Synthesis.Core;

namespace JagFx.Desktop.ViewModels;

public partial class MainViewModel
{
    private void ScheduleRerender(bool immediate = false)
    {
        IsDirty = true;
        _bufferStale = true;

        if (!TrueWaveEnabled)
        {
            _renderCts?.Cancel();
        }

        _debounceTimer?.Dispose();
        if (!TrueWaveEnabled && _interactiveEditDepth > 0)
        {
            return;
        }

        if (immediate || TrueWaveEnabled)
        {
            QueuePreviewRender();
            return;
        }

        _debounceTimer = new System.Threading.Timer(
            _ => QueuePreviewRender(),
            null,
            CommittedPreviewDelayMs,
            Timeout.Infinite
        );
    }

    private void QueuePreviewRender()
    {
        _ = System.Threading.Interlocked.Exchange(ref _previewRenderPending, 1);

        if (System.Threading.Interlocked.Exchange(ref _previewRenderQueued, 1) == 1)
        {
            return;
        }

        Dispatcher.UIThread.Post(ProcessPreviewRenderQueue, DispatcherPriority.Input);
    }

    private void ProcessPreviewRenderQueue() => _ = ProcessPreviewRenderQueueAsync();

    private async Task ProcessPreviewRenderQueueAsync()
    {
        try
        {
            while (System.Threading.Interlocked.Exchange(ref _previewRenderPending, 0) == 1)
            {
                try
                {
                    await RenderAndCacheAsync().ConfigureAwait(true);
                }
                catch (OperationCanceledException ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Render canceled: {ex.Message}");
                }
                catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Render failed: {ex}");
                }
            }
        }
        finally
        {
            System.Threading.Volatile.Write(ref _previewRenderQueued, 0);

            if (
                System.Threading.Volatile.Read(ref _previewRenderPending) == 1
                && System.Threading.Interlocked.Exchange(ref _previewRenderQueued, 1) == 0
            )
            {
                Dispatcher.UIThread.Post(ProcessPreviewRenderQueue, DispatcherPriority.Input);
            }
        }
    }

    private async Task RenderAndCacheAsync()
    {
        _renderCts?.Cancel();
        _renderCts = new CancellationTokenSource();
        var cts = _renderCts;
        try
        {
            var patchModel = Patch.ToModel();
            var voiceFilter = PlaySingleVoice ? Patch.SelectedVoiceIndex : -1;

            // Always render single pass for waveform display.
            var buffer = await RenderPreviewAsync(patchModel, voiceFilter, cts.Token)
                .ConfigureAwait(true);
            if (cts.Token.IsCancellationRequested)
            {
                return;
            }

            _singlePassDuration = buffer.Length / (double)buffer.SampleRate;
            var loopBeginMs = Patch.LoopBegin;
            var loopEndMs = Patch.LoopEnd;
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

            NormalizeAndSetOutput(buffer);

            // If looping playback is active, re-render the playback buffer too
            if (IsPlaying)
            {
                patchModel = DefaultLoopIfUnset(patchModel);

                var playbackBuffer = await SynthesisService
                    .RenderAsync(
                        patchModel,
                        loopCount: EffectiveLoopCount,
                        voiceFilter: voiceFilter,
                        ct: cts.Token
                    )
                    .ConfigureAwait(true);
                if (cts.Token.IsCancellationRequested)
                {
                    return;
                }

                _cachedBuffer = playbackBuffer;
                _bufferStale = false;
                await _playback.UpdateWavAsync(playbackBuffer).ConfigureAwait(true);
            }
        }
        catch (OperationCanceledException ex)
        {
            System.Diagnostics.Debug.WriteLine($"Render canceled: {ex.Message}");
        }
    }

    private Patch DefaultLoopIfUnset(Patch patchModel)
    {
        if (
            IsLooping
            && patchModel.Loop.BeginMs >= patchModel.Loop.EndMs
            && !patchModel.ActiveVoices.IsEmpty
        )
        {
            var maxMs = patchModel.ActiveVoices.Max(v => v.Voice.DurationMs + v.Voice.OffsetMs);
            if (maxMs > 0)
            {
                return patchModel with { Loop = new LoopSegment(0, maxMs) };
            }
        }
        return patchModel;
    }

    private async Task<AudioBuffer> RenderPreviewAsync(
        Patch patchModel,
        int voiceFilter,
        CancellationToken ct
    ) => await Task.Run(() => RenderPreview(patchModel, voiceFilter, ct), ct).ConfigureAwait(false);

    private AudioBuffer RenderPreview(Patch patchModel, int voiceFilter, CancellationToken ct)
    {
        ct.ThrowIfCancellationRequested();

        var activeVoices =
            voiceFilter < 0
                ? patchModel.ActiveVoices
                : [.. patchModel.ActiveVoices.Where(v => v.Index == voiceFilter)];

        if (activeVoices.IsEmpty)
        {
            return AudioBuffer.Empty(0);
        }

        var maxDuration = activeVoices.Max(v => v.Voice.DurationMs + v.Voice.OffsetMs);
        if (maxDuration <= 0)
        {
            return AudioBuffer.Empty(0);
        }

        var sampleCount = (int)(maxDuration * AudioConstants.SampleRatePerMillisecond);
        var mix = new int[sampleCount];

        foreach (var (index, voice) in activeVoices)
        {
            ct.ThrowIfCancellationRequested();

            AudioBuffer? voiceBuffer;
            var needsRender = false;
            lock (_previewCacheLock)
            {
                needsRender =
                    !_previewVoiceCache.TryGetValue(index, out voiceBuffer)
                    || _dirtyPreviewVoices.Contains(index);
            }

            if (needsRender)
            {
                voiceBuffer = VoiceSynthesizer.Synthesize(voice, ct);
                ct.ThrowIfCancellationRequested();

                lock (_previewCacheLock)
                {
                    _previewVoiceCache[index] = voiceBuffer;
                    _ = _dirtyPreviewVoices.Remove(index);
                }
            }

            if (voiceBuffer is null)
            {
                continue;
            }

            var startOffset = (int)(voice.OffsetMs * AudioConstants.SampleRatePerMillisecond);
            for (var i = 0; i < voiceBuffer.Length; i++)
            {
                if ((i & 0x1FF) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                var pos = i + startOffset;
                if (pos >= 0 && pos < sampleCount)
                {
                    mix[pos] += voiceBuffer.Samples[i];
                }
            }
        }

        AudioMath.ClipInt16(mix, sampleCount);
        return new AudioBuffer(mix, AudioConstants.SampleRate);
    }

    private void NormalizeAndSetOutput(AudioBuffer buffer)
    {
        var samples = buffer.Samples;
        if (samples.Length <= 0)
        {
            return;
        }

        var maxAbs = 0;
        foreach (var s in samples)
        {
            var abs = Math.Abs(s);
            if (abs > maxAbs)
            {
                maxAbs = abs;
            }
        }

        var output = new float[samples.Length];
        if (maxAbs > 0)
        {
            var scale = 1.0f / maxAbs;
            for (var i = 0; i < samples.Length; i++)
            {
                output[i] = samples[i] * scale;
            }
        }

        OutputSamples = output;
    }

    private void OnFileChanged()
    {
        lock (_previewCacheLock)
        {
            _previewVoiceCache.Clear();
            _dirtyPreviewVoices.Clear();
        }
        PatchName = _fileManager.PatchName;
        FilePath = _fileManager.FilePath;
        IsDirty = false;
        QueuePreviewRender();
    }

    private void SubscribeVoiceChanges(VoiceViewModel voice)
    {
        UnsubscribeVoiceChanges();
        _subscribedVoice = voice;
        voice.PropertyChanged += OnVoicePropertyChanged;

        foreach (var env in GetVoiceEnvelopes(voice))
        {
            env.PropertyChanged += OnNestedPropertyChanged;
            env.Segments.CollectionChanged += OnSegmentsCollectionChanged;
            foreach (var seg in env.Segments)
            {
                seg.PropertyChanged += OnNestedPropertyChanged;
            }
        }

        voice.Filter.PropertyChanged += OnNestedPropertyChanged;
        foreach (var partial in voice.Partials)
        {
            partial.PropertyChanged += OnNestedPropertyChanged;
        }
    }

    private void UnsubscribeVoiceChanges()
    {
        if (_subscribedVoice is null)
        {
            return;
        }

        _subscribedVoice.PropertyChanged -= OnVoicePropertyChanged;

        foreach (var env in GetVoiceEnvelopes(_subscribedVoice))
        {
            env.PropertyChanged -= OnNestedPropertyChanged;
            env.Segments.CollectionChanged -= OnSegmentsCollectionChanged;
            foreach (var seg in env.Segments)
            {
                seg.PropertyChanged -= OnNestedPropertyChanged;
            }
        }

        _subscribedVoice.Filter.PropertyChanged -= OnNestedPropertyChanged;
        foreach (var partial in _subscribedVoice.Partials)
        {
            partial.PropertyChanged -= OnNestedPropertyChanged;
        }

        _subscribedVoice = null;
    }

    private static EnvelopeViewModel[] GetVoiceEnvelopes(VoiceViewModel v) =>
        [
            v.Pitch,
            v.Volume,
            v.VibratoRate,
            v.VibratoDepth,
            v.TremoloRate,
            v.TremoloDepth,
            v.FilterEnvelope,
            v.GapOff,
            v.GapOn,
        ];

    private void OnVoicePropertyChanged(object? s, PropertyChangedEventArgs e)
    {
        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void OnNestedPropertyChanged(object? s, PropertyChangedEventArgs e)
    {
        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void OnSegmentsCollectionChanged(object? s, NotifyCollectionChangedEventArgs e)
    {
        if (e.NewItems is not null)
        {
            foreach (SegmentViewModel seg in e.NewItems)
            {
                seg.PropertyChanged += OnNestedPropertyChanged;
            }
        }

        if (e.OldItems is not null)
        {
            foreach (SegmentViewModel seg in e.OldItems)
            {
                seg.PropertyChanged -= OnNestedPropertyChanged;
            }
        }

        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void MarkSelectedVoicePreviewDirty()
    {
        lock (_previewCacheLock)
        {
            _ = _dirtyPreviewVoices.Add(Patch.SelectedVoiceIndex);
        }
    }
}
