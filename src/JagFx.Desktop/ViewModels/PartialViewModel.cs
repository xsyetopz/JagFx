using CommunityToolkit.Mvvm.ComponentModel;
using JagFx.Core.Types;
using JagFx.Domain.Models;

namespace JagFx.Desktop.ViewModels;

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
    public string DisplayLabel => Loc.Format("PartialLabelFormat", DisplayIndex);

    partial void OnAmplitudeChanged(int value) => OnPropertyChanged(nameof(AmplitudeFraction));

    partial void OnDisplayIndexChanged(int value) => OnPropertyChanged(nameof(DisplayLabel));

    public void Load(VoicePartial partial)
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

    public VoicePartial ToModel() =>
        new(new Percent(Amplitude), PitchOffsetSemitones, new Milliseconds(DelayMs));
}
