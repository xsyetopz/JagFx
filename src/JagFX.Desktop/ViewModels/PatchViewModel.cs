using System.Collections.Immutable;
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using JagFX.Core.Constants;
using JagFX.Domain.Models;

namespace JagFX.Desktop.ViewModels;

public partial class PatchViewModel : ObservableObject
{
    [ObservableProperty]
    private int _loopBegin;

    [ObservableProperty]
    private int _loopEnd;

    [ObservableProperty]
    private int _selectedVoiceIndex;

    public ObservableCollection<VoiceViewModel> Voices { get; }

    public VoiceViewModel SelectedVoice => Voices[SelectedVoiceIndex];

    public PatchViewModel()
    {
        Voices = new ObservableCollection<VoiceViewModel>(
            Enumerable.Range(0, AudioConstants.MaxVoices)
                .Select(i => new VoiceViewModel { Index = i }));
        SyncVoiceSelection();
    }

    partial void OnSelectedVoiceIndexChanged(int value) => SyncVoiceSelection();

    private void SyncVoiceSelection()
    {
        for (var i = 0; i < Voices.Count; i++)
            Voices[i].IsSelected = i == SelectedVoiceIndex;
        OnPropertyChanged(nameof(SelectedVoice));
    }

    public void Load(Patch patch)
    {
        LoopBegin = patch.Loop.BeginMs;
        LoopEnd = patch.Loop.EndMs;

        for (var i = 0; i < AudioConstants.MaxVoices; i++)
        {
            var voice = i < patch.Voices.Count ? patch.Voices[i] : null;
            Voices[i].Load(voice);
        }

        // Select first active voice, or voice 0
        var firstActive = patch.ActiveVoices.FirstOrDefault();
        SelectedVoiceIndex = firstActive.Voice is not null ? firstActive.Index : 0;
        SyncVoiceSelection();
    }

    public void Clear()
    {
        LoopBegin = 0;
        LoopEnd = 0;
        SelectedVoiceIndex = 0;

        foreach (var v in Voices)
            v.Clear();
        SyncVoiceSelection();
    }

    public Patch ToModel()
    {
        var voices = Voices
            .Select(v => v.ToModel())
            .ToImmutableList();

        return new Patch(voices, new LoopSegment(LoopBegin, LoopEnd), ImmutableList<string>.Empty);
    }
}
