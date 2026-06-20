using CommunityToolkit.Mvvm.Input;
using JagFx.Domain.Models;

namespace JagFx.Desktop.ViewModels;

public partial class MainViewModel
{
    [RelayCommand]
    private void SelectVoice(int index)
    {
        if (index >= 0 && index < Patch.Voices.Count)
        {
            if (IsCopyMode && _copiedVoice is not null && index != Patch.SelectedVoiceIndex)
            {
                Patch.Voices[index].Load(_copiedVoice);
                IsCopyMode = false;
                _bufferStale = true;
            }

            Patch.SelectedVoiceIndex = index;
            if (SelectedEnvelope is not null)
            {
                SelectEnvelope(SelectedSlot);
            }
        }
    }

    public static readonly (
        SignalChainSlot Slot,
        Func<VoiceViewModel, EnvelopeViewModel> Getter
    )[] SignalChain =
    [
        (SignalChainSlot.Pitch, v => v.Pitch),
        (SignalChainSlot.VibratoRate, v => v.VibratoRate),
        (SignalChainSlot.VibratoDepth, v => v.VibratoDepth),
        (SignalChainSlot.Volume, v => v.Volume),
        (SignalChainSlot.TremoloRate, v => v.TremoloRate),
        (SignalChainSlot.TremoloDepth, v => v.TremoloDepth),
        (SignalChainSlot.GapOff, v => v.GapOff),
        (SignalChainSlot.GapOn, v => v.GapOn),
        (SignalChainSlot.Filter, v => v.FilterEnvelope),
    ];

    private static (string Unit, double Min, double Max) GetStartEndMeta(SignalChainSlot slot) =>
        slot switch
        {
            SignalChainSlot.Pitch or SignalChainSlot.VibratoRate or SignalChainSlot.TremoloRate => (
                "Hz",
                0,
                11025
            ),
            SignalChainSlot.Volume
            or SignalChainSlot.VibratoDepth
            or SignalChainSlot.TremoloDepth
            or SignalChainSlot.Filter => ("%", -100, 100),
            SignalChainSlot.GapOff or SignalChainSlot.GapOn => ("ms", 0, 11.61),
            SignalChainSlot.PoleZero => ("%", -100, 100),
            SignalChainSlot.Output => ("%", -100, 100),
            SignalChainSlot.Bode => ("%", -100, 100),
            _ => ("%", -100, 100),
        };

    public string StartEndUnit => GetStartEndMeta(SelectedSlot).Unit;
    public double StartEndMin => GetStartEndMeta(SelectedSlot).Min;
    public double StartEndMax => GetStartEndMeta(SelectedSlot).Max;
    public bool IsSelectedEnvelopeFrequencyScale => SelectedSlot.UsesLogFrequencyScale();
    public bool IsSelectedEnvelopePercentScale => SelectedSlot.UsesPercentScale();
    public bool IsSelectedEnvelopeGateDurationScale => SelectedSlot.UsesGateDurationScale();

    public void SelectEnvelope(SignalChainSlot slot)
    {
        var voice = Patch.SelectedVoice;
        SelectedSlot = slot;

        var (_, getter) = SignalChain.FirstOrDefault(e => e.Slot == slot);
        SelectedEnvelope = getter is not null ? getter(voice) : null;

        OnPropertyChanged(nameof(StartEndUnit));
        OnPropertyChanged(nameof(StartEndMin));
        OnPropertyChanged(nameof(StartEndMax));
        OnPropertyChanged(nameof(IsSelectedEnvelopeFrequencyScale));
        OnPropertyChanged(nameof(IsSelectedEnvelopePercentScale));
        OnPropertyChanged(nameof(IsSelectedEnvelopeGateDurationScale));
    }

    public void SelectEnvelopeByOffset(int offset)
    {
        var currentIndex = Array.FindIndex(SignalChain, e => e.Slot == SelectedSlot);
        if (currentIndex < 0)
        {
            currentIndex = 0;
        }

        var newIndex = Math.Clamp(currentIndex + offset, 0, SignalChain.Length - 1);
        SelectEnvelope(SignalChain[newIndex].Slot);
    }

    public void CopyVoice()
    {
        _copiedVoice = Patch.SelectedVoice.ToModel();
        IsCopyMode = true;
    }

    public void ResetVoice()
    {
        Patch.SelectedVoice.Clear();
        _bufferStale = true;
        ScheduleRerender(immediate: true);
    }
}
