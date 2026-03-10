using CommunityToolkit.Mvvm.ComponentModel;
using JagFX.Core.Types;
using JagFX.Domain.Models;

namespace JagFX.Desktop.ViewModels;

public partial class PartialViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _isActive;

    [ObservableProperty]
    private int _amplitude;

    [ObservableProperty]
    private int _pitchOffsetSemitones;

    [ObservableProperty]
    private int _delayMs;

    [ObservableProperty]
    private int _displayIndex = 1;

    public double AmplitudeFraction => Amplitude / 65535.0;

    partial void OnAmplitudeChanged(int value) => OnPropertyChanged(nameof(AmplitudeFraction));

    public void Load(Partial partial)
    {
        IsActive = true;
        Amplitude = partial.Amplitude.Value;
        PitchOffsetSemitones = partial.PitchOffsetSemitones;
        DelayMs = partial.Delay.Value;
    }

    public void Clear()
    {
        IsActive = false;
        Amplitude = 0;
        PitchOffsetSemitones = 0;
        DelayMs = 0;
    }

    public Partial ToModel() =>
        new(new Percent(Amplitude), PitchOffsetSemitones, new Milliseconds(DelayMs));
}
