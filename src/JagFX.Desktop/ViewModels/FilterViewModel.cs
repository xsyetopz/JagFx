using System.Collections.Immutable;
using CommunityToolkit.Mvvm.ComponentModel;
using JagFX.Domain.Models;

namespace JagFX.Desktop.ViewModels;

public partial class FilterViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _hasFilter;

    [ObservableProperty]
    private int _poleCount0;

    [ObservableProperty]
    private int _poleCount1;

    [ObservableProperty]
    private int _unityGain0;

    [ObservableProperty]
    private int _unityGain1;

    private ImmutableArray<ImmutableArray<ImmutableArray<int>>> _polePhase;
    private ImmutableArray<ImmutableArray<ImmutableArray<int>>> _poleMagnitude;

    public ImmutableArray<ImmutableArray<ImmutableArray<int>>> PolePhase => _polePhase;
    public ImmutableArray<ImmutableArray<ImmutableArray<int>>> PoleMagnitude => _poleMagnitude;

    public void Load(Filter? filter)
    {
        if (filter is null)
        {
            Clear();
            return;
        }

        HasFilter = true;
        PoleCount0 = filter.PoleCounts[0];
        PoleCount1 = filter.PoleCounts[1];
        UnityGain0 = filter.UnityGain[0];
        UnityGain1 = filter.UnityGain[1];
        _polePhase = filter.PolePhase;
        _poleMagnitude = filter.PoleMagnitude;

        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
    }

    public void Clear()
    {
        HasFilter = false;
        PoleCount0 = 0;
        PoleCount1 = 0;
        UnityGain0 = 0;
        UnityGain1 = 0;
        _polePhase = default;
        _poleMagnitude = default;
    }

    public Filter? ToModel()
    {
        if (!HasFilter) return null;

        return new Filter(
            ImmutableArray.Create(PoleCount0, PoleCount1),
            ImmutableArray.Create(UnityGain0, UnityGain1),
            _polePhase,
            _poleMagnitude,
            null);
    }
}
