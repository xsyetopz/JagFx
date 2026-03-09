using System.Collections.Immutable;
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using JagFX.Domain.Models;

namespace JagFX.Desktop.ViewModels;

public partial class SegmentViewModel : ObservableObject
{
    [ObservableProperty]
    private int _duration;

    [ObservableProperty]
    private int _targetLevel;
}

public partial class EnvelopeViewModel : ObservableObject
{
    [ObservableProperty]
    private Waveform _waveform = Waveform.Off;

    [ObservableProperty]
    private int _startValue;

    [ObservableProperty]
    private int _endValue;

    public ObservableCollection<SegmentViewModel> Segments { get; } = [];

    public bool IsEmpty => Segments.Count == 0 && Waveform == Waveform.Off;

    public void Load(Envelope envelope)
    {
        Waveform = envelope.Waveform;
        StartValue = envelope.StartValue;
        EndValue = envelope.EndValue;

        Segments.Clear();
        foreach (var seg in envelope.Segments)
        {
            Segments.Add(new SegmentViewModel
            {
                Duration = seg.Duration,
                TargetLevel = seg.TargetLevel
            });
        }

        OnPropertyChanged(nameof(IsEmpty));
    }

    public void Clear()
    {
        Waveform = Waveform.Off;
        StartValue = 0;
        EndValue = 0;
        Segments.Clear();
        OnPropertyChanged(nameof(IsEmpty));
    }

    public Envelope ToModel()
    {
        var segments = Segments
            .Select(s => new Segment(s.Duration, s.TargetLevel))
            .ToImmutableList();

        return new Envelope(Waveform, StartValue, EndValue, segments);
    }

    public void AddSegment(int duration, int peak)
    {
        Segments.Add(new SegmentViewModel
        {
            Duration = duration,
            TargetLevel = peak
        });
        OnPropertyChanged(nameof(IsEmpty));
    }

    [RelayCommand]
    private void AddDefaultSegment() => AddSegment(100, 0);

    [RelayCommand]
    private void RemoveSegment(SegmentViewModel seg)
    {
        Segments.Remove(seg);
        OnPropertyChanged(nameof(IsEmpty));
    }

    public void RemoveSegmentAt(int index)
    {
        if (index >= 0 && index < Segments.Count)
        {
            Segments.RemoveAt(index);
            OnPropertyChanged(nameof(IsEmpty));
        }
    }
}
