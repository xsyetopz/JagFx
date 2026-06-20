using System.ComponentModel;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Views.SignalChain;

public partial class SignalChainPanel
{
    private void OnDataContextChanged(object? sender, EventArgs e)
    {
        if (_subscribedVm is not null)
        {
            _subscribedVm.PropertyChanged -= OnVmPropertyChanged;
            UnsubscribePatch();
            UnsubscribeEnvelopeChanges();
        }

        if (DataContext is MainViewModel vm)
        {
            _subscribedVm = vm;
            vm.PropertyChanged += OnVmPropertyChanged;
            SubscribePatch(vm);
            BindAll(vm);
            UpdateSelection(vm.SelectedSlot);
            UpdateGridMode(vm.GridMode);
        }
        else
        {
            _subscribedVm = null;
        }
    }

    private void SubscribePatch(MainViewModel vm)
    {
        _subscribedPatch = vm.Patch;
        _subscribedPatch.PropertyChanged += OnPatchPropertyChanged;
    }

    private void UnsubscribePatch()
    {
        if (_subscribedPatch is not null)
        {
            _subscribedPatch.PropertyChanged -= OnPatchPropertyChanged;
            _subscribedPatch = null;
        }
    }

    private void OnPatchPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(PatchViewModel.SelectedVoice) && _subscribedVm is not null)
        {
            BindAll(_subscribedVm);
        }
    }

    private void OnVmPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (_subscribedVm is null)
        {
            return;
        }

        if (e.PropertyName == nameof(MainViewModel.SelectedSlot))
        {
            UpdateSelection(_subscribedVm.SelectedSlot);
        }

        if (e.PropertyName == nameof(MainViewModel.OutputSamples) && _outCanvas is not null)
        {
            _outCanvas.Samples = _subscribedVm.OutputSamples;
        }

        if (e.PropertyName == nameof(MainViewModel.PlaybackPosition) && _outCanvas is not null)
        {
            _outCanvas.PlaybackPosition = _subscribedVm.PlaybackPosition;
        }

        if (e.PropertyName == nameof(MainViewModel.GridMode))
        {
            UpdateGridMode(_subscribedVm.GridMode);
        }
    }

    private void BindAll(MainViewModel vm)
    {
        UnsubscribeEnvelopeChanges();
        var voice = vm.Patch.SelectedVoice;

        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot)
            {
                var (_, getter) = MainViewModel.SignalChain.FirstOrDefault(e =>
                    e.Slot == envSlot.Slot
                );
                if (getter is null)
                {
                    continue;
                }

                var envelope = getter(voice);
                envSlot.Canvas.Envelope = envelope;
                envelope.PropertyChanged += OnEnvelopePropertyChanged;
                UpdateDimming(envSlot, envelope);
            }
        }

        // Bind filter canvases
        var filter = voice.Filter;
        if (_pzCanvas is not null)
        {
            _pzCanvas.Filter = filter;
            _pzCanvas.InvalidateVisual();
        }

        if (_bodeCanvas is not null)
        {
            _bodeCanvas.Filter = filter;
            _bodeCanvas.InvalidateVisual();
        }

        // Bind output waveform
        if (_outCanvas is not null)
        {
            _outCanvas.Samples = vm.OutputSamples;
            _outCanvas.PlaybackPosition = vm.PlaybackPosition;
        }
    }

    private void UnsubscribeEnvelopeChanges()
    {
        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot && envSlot.Canvas.Envelope is { } env)
            {
                env.PropertyChanged -= OnEnvelopePropertyChanged;
            }
        }
    }

    private void OnEnvelopePropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName != nameof(EnvelopeViewModel.IsEmpty))
        {
            return;
        }

        if (sender is not EnvelopeViewModel env)
        {
            return;
        }

        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot && ReferenceEquals(envSlot.Canvas.Envelope, env))
            {
                UpdateDimming(envSlot, env);
            }
        }
    }
}
