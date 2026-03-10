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

    public void UpdatePole(int channel, int phase, int index, int newPhase, int newMagnitude)
    {
        if (_polePhase.IsDefault || _poleMagnitude.IsDefault) return;
        if (channel < 0 || channel >= _polePhase.Length) return;
        if (_polePhase[channel].IsDefault || phase < 0 || phase >= _polePhase[channel].Length) return;
        if (_polePhase[channel][phase].IsDefault || index < 0 || index >= _polePhase[channel][phase].Length) return;

        // Rebuild immutable arrays with the updated value
        var phaseArr = _polePhase[channel][phase].SetItem(index, newPhase);
        var phaseChannel = _polePhase[channel].SetItem(phase, phaseArr);
        _polePhase = _polePhase.SetItem(channel, phaseChannel);

        var magArr = _poleMagnitude[channel][phase].SetItem(index, newMagnitude);
        var magChannel = _poleMagnitude[channel].SetItem(phase, magArr);
        _poleMagnitude = _poleMagnitude.SetItem(channel, magChannel);

        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
    }

    partial void OnPoleCount0Changed(int oldValue, int newValue) => ResizePoleArrays(0, oldValue, newValue);
    partial void OnPoleCount1Changed(int oldValue, int newValue) => ResizePoleArrays(1, oldValue, newValue);

    private void ResizePoleArrays(int channel, int oldCount, int newCount)
    {
        if (_polePhase.IsDefault || _poleMagnitude.IsDefault) return;
        if (channel < 0 || channel >= _polePhase.Length) return;
        if (_polePhase[channel].IsDefault) return;

        for (var phase = 0; phase < _polePhase[channel].Length; phase++)
        {
            if (_polePhase[channel][phase].IsDefault) continue;

            var phaseArr = _polePhase[channel][phase];
            var magArr = _poleMagnitude[channel][phase];

            if (newCount > oldCount)
            {
                // Append zeros for new poles at origin
                var phaseBuilder = phaseArr.ToBuilder();
                var magBuilder = magArr.ToBuilder();
                for (var i = oldCount; i < newCount; i++)
                {
                    phaseBuilder.Add(0);
                    magBuilder.Add(0);
                }
                phaseArr = phaseBuilder.ToImmutable();
                magArr = magBuilder.ToImmutable();
            }
            else if (newCount < oldCount)
            {
                // Truncate
                phaseArr = phaseArr.RemoveRange(newCount, phaseArr.Length - newCount);
                magArr = magArr.RemoveRange(newCount, magArr.Length - newCount);
            }

            var phaseChannel = _polePhase[channel].SetItem(phase, phaseArr);
            _polePhase = _polePhase.SetItem(channel, phaseChannel);

            var magChannel = _poleMagnitude[channel].SetItem(phase, magArr);
            _poleMagnitude = _poleMagnitude.SetItem(channel, magChannel);
        }

        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
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
