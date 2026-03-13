using System.Collections.Immutable;
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using JagFx.Domain.Models;

namespace JagFx.Desktop.ViewModels;

public partial class SegmentViewModel : ObservableObject
{
    [ObservableProperty]
    private int _index;

    [ObservableProperty]
    private int _duration;

    [ObservableProperty]
    private int _targetLevel;
}

public partial class EnvelopeViewModel : ObservableObject
{
    [ObservableProperty]
    private Waveform _waveform = Waveform.Off;

    public bool IsOffActive => Waveform == Waveform.Off;
    public bool IsSquareActive => Waveform == Waveform.Square;
    public bool IsSineActive => Waveform == Waveform.Sine;
    public bool IsSawActive => Waveform == Waveform.Saw;
    public bool IsNoiseActive => Waveform == Waveform.Noise;

    partial void OnWaveformChanged(Waveform value)
    {
        OnPropertyChanged(nameof(IsOffActive));
        OnPropertyChanged(nameof(IsSquareActive));
        OnPropertyChanged(nameof(IsSineActive));
        OnPropertyChanged(nameof(IsSawActive));
        OnPropertyChanged(nameof(IsNoiseActive));
    }

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
        for (var i = 0; i < envelope.Segments.Count; i++)
        {
            var seg = envelope.Segments[i];
            Segments.Add(new SegmentViewModel
            {
                Index = i,
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
            Index = Segments.Count,
            Duration = duration,
            TargetLevel = peak
        });
        OnPropertyChanged(nameof(IsEmpty));
    }

    [RelayCommand]
    private void SetWaveform(string waveformId) => Waveform = (Waveform)int.Parse(waveformId);

    [RelayCommand]
    private void AddDefaultSegment() => AddSegment(100, 0);

    [RelayCommand]
    private void RemoveSegment(SegmentViewModel seg)
    {
        Segments.Remove(seg);
        ReindexSegments();
        OnPropertyChanged(nameof(IsEmpty));
    }

    public void InsertSegment(int index, int duration, int targetLevel)
    {
        Segments.Insert(index, new SegmentViewModel
        {
            Index = index,
            Duration = duration,
            TargetLevel = targetLevel
        });
        ReindexSegments();
        OnPropertyChanged(nameof(IsEmpty));
    }

    public void RemoveSegmentAt(int index)
    {
        if (index >= 0 && index < Segments.Count)
        {
            Segments.RemoveAt(index);
            ReindexSegments();
            OnPropertyChanged(nameof(IsEmpty));
        }
    }

    private void ReindexSegments()
    {
        for (var i = 0; i < Segments.Count; i++)
            Segments[i].Index = i;
    }
}
