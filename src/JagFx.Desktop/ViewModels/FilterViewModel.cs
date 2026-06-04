using System.Collections.Immutable;
using CommunityToolkit.Mvvm.ComponentModel;
using JagFx.Domain.Models;

namespace JagFx.Desktop.ViewModels;

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

    public ImmutableArray<ImmutableArray<ImmutableArray<int>>> PolePhase { get; private set; }
    public ImmutableArray<ImmutableArray<ImmutableArray<int>>> PoleMagnitude { get; private set; }
    public ObservableCollection<FilterPoleViewModel> PoleControls { get; } = [];

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
        PolePhase = filter.PolePhase;
        PoleMagnitude = filter.PoleMagnitude;

        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
        RebuildPoleControls();
    }

    public void Clear()
    {
        HasFilter = false;
        PoleCount0 = 0;
        PoleCount1 = 0;
        UnityGain0 = 0;
        UnityGain1 = 0;
        PolePhase = default;
        PoleMagnitude = default;
        PoleControls.Clear();
    }

    public void UpdatePole(int channel, int phase, int index, int newPhase, int newMagnitude)
    {
        if (PolePhase.IsDefault || PoleMagnitude.IsDefault)
            return;
        if (channel < 0 || channel >= PolePhase.Length)
            return;
        if (PolePhase[channel].IsDefault || phase < 0 || phase >= PolePhase[channel].Length)
            return;
        if (
            PolePhase[channel][phase].IsDefault
            || index < 0
            || index >= PolePhase[channel][phase].Length
        )
            return;

        // Rebuild immutable arrays with the updated value
        var phaseArr = PolePhase[channel][phase].SetItem(index, newPhase);
        var phaseChannel = PolePhase[channel].SetItem(phase, phaseArr);
        PolePhase = PolePhase.SetItem(channel, phaseChannel);

        var magArr = PoleMagnitude[channel][phase].SetItem(index, newMagnitude);
        var magChannel = PoleMagnitude[channel].SetItem(phase, magArr);
        PoleMagnitude = PoleMagnitude.SetItem(channel, magChannel);

        RefreshPoleControl(channel, phase, index, newPhase, newMagnitude);
        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
    }

    partial void OnPoleCount0Changed(int oldValue, int newValue) =>
        ResizePoleArrays(0, oldValue, newValue);

    partial void OnPoleCount1Changed(int oldValue, int newValue) =>
        ResizePoleArrays(1, oldValue, newValue);

    private void ResizePoleArrays(int channel, int oldCount, int newCount)
    {
        if (PolePhase.IsDefault || PoleMagnitude.IsDefault)
            return;
        if (channel < 0 || channel >= PolePhase.Length)
            return;
        if (PolePhase[channel].IsDefault)
            return;

        for (var phase = 0; phase < PolePhase[channel].Length; phase++)
        {
            if (PolePhase[channel][phase].IsDefault)
                continue;

            var phaseArr = PolePhase[channel][phase];
            var magArr = PoleMagnitude[channel][phase];

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

            var phaseChannel = PolePhase[channel].SetItem(phase, phaseArr);
            PolePhase = PolePhase.SetItem(channel, phaseChannel);

            var magChannel = PoleMagnitude[channel].SetItem(phase, magArr);
            PoleMagnitude = PoleMagnitude.SetItem(channel, magChannel);
        }

        OnPropertyChanged(nameof(PolePhase));
        OnPropertyChanged(nameof(PoleMagnitude));
        RebuildPoleControls();
    }

    private void RebuildPoleControls()
    {
        PoleControls.Clear();

        if (PolePhase.IsDefault || PoleMagnitude.IsDefault)
            return;

        for (var channel = 0; channel < 2 && channel < PolePhase.Length; channel++)
        {
            var poleCount = channel == 0 ? PoleCount0 : PoleCount1;
            if (PolePhase[channel].IsDefault || PoleMagnitude[channel].IsDefault)
                continue;

            for (var phase = 0; phase < PolePhase[channel].Length; phase++)
            {
                if (PolePhase[channel][phase].IsDefault || PoleMagnitude[channel][phase].IsDefault)
                    continue;

                var count = Math.Min(
                    poleCount,
                    Math.Min(PolePhase[channel][phase].Length, PoleMagnitude[channel][phase].Length)
                );

                for (var index = 0; index < count; index++)
                {
                    PoleControls.Add(
                        new FilterPoleViewModel(
                            this,
                            channel,
                            phase,
                            index,
                            PolePhase[channel][phase][index],
                            PoleMagnitude[channel][phase][index]
                        )
                    );
                }
            }
        }
    }

    private void RefreshPoleControl(
        int channel,
        int phase,
        int index,
        int newPhase,
        int newMagnitude
    )
    {
        var control = PoleControls.FirstOrDefault(p =>
            p.Channel == channel && p.Phase == phase && p.Index == index
        );

        control?.SetValues(newPhase, newMagnitude);
    }

    public Filter? ToModel()
    {
        if (!HasFilter)
            return null;

        return new Filter(
            [PoleCount0, PoleCount1],
            [UnityGain0, UnityGain1],
            PolePhase,
            PoleMagnitude,
            null
        );
    }
}

public partial class FilterPoleViewModel : ObservableObject
{
    private const double PhaseScaleFactor = 1.2207031e-4;
    private const double C1BaseFrequencyHz = 32.703197;
    private const double SampleRate = 22050.0;

    private readonly FilterViewModel _owner;
    private bool _isSyncing;
    private int _angleRaw;
    private int _magnitudeRaw;

    public FilterPoleViewModel(
        FilterViewModel owner,
        int channel,
        int phase,
        int index,
        int angleRaw,
        int magnitudeRaw
    )
    {
        _owner = owner;
        Channel = channel;
        Phase = phase;
        Index = index;
        _angleRaw = angleRaw;
        _magnitudeRaw = magnitudeRaw;
    }

    public int Channel { get; }
    public int Phase { get; }
    public int Index { get; }
    public string ChannelLabel => Channel == 0 ? "A" : "B";
    public string PoleLabel => $"{ChannelLabel}{Phase}{Index}";

    public int AngleRaw
    {
        get => _angleRaw;
        set
        {
            var bounded = Math.Clamp(value, 0, 65535);
            if (!SetProperty(ref _angleRaw, bounded))
                return;

            OnPropertyChanged(nameof(AngleDegrees));

            if (!_isSyncing)
                _owner.UpdatePole(Channel, Phase, Index, _angleRaw, _magnitudeRaw);
        }
    }

    public double AngleDegrees
    {
        get => RawPhaseToDegrees(_angleRaw);
        set => AngleRaw = DegreesToRawPhase(value);
    }

    public int MagnitudeRaw
    {
        get => _magnitudeRaw;
        set
        {
            var bounded = Math.Clamp(value, 0, 65535);
            if (!SetProperty(ref _magnitudeRaw, bounded))
                return;

            OnPropertyChanged(nameof(MagnitudePercent));

            if (!_isSyncing)
                _owner.UpdatePole(Channel, Phase, Index, _angleRaw, _magnitudeRaw);
        }
    }

    public double MagnitudePercent
    {
        get => _magnitudeRaw / 655.35;
        set
        {
            var bounded = Math.Clamp(value, 0.0, 100.0);
            MagnitudeRaw = (int)Math.Round(bounded * 655.35);
        }
    }

    public void SetValues(int angleRaw, int magnitudeRaw)
    {
        _isSyncing = true;
        AngleRaw = angleRaw;
        MagnitudeRaw = magnitudeRaw;
        _isSyncing = false;
        OnPropertyChanged(nameof(AngleDegrees));
        OnPropertyChanged(nameof(MagnitudePercent));
    }

    private static double RawPhaseToDegrees(int raw)
    {
        var scaled = Math.Clamp(raw, 0, ushort.MaxValue) * PhaseScaleFactor;
        var frequencyHz = Math.Pow(2.0, scaled) * C1BaseFrequencyHz;
        var radians = frequencyHz * 2.0 * Math.PI / SampleRate;
        return radians * 180.0 / Math.PI;
    }

    private static int DegreesToRawPhase(double degrees)
    {
        var radians =
            Math.Clamp(degrees, 0.0, RawPhaseToDegrees(ushort.MaxValue)) * Math.PI / 180.0;
        var frequencyHz = radians * SampleRate / (2.0 * Math.PI);
        frequencyHz = Math.Max(frequencyHz, 1.0);
        var scaled = Math.Log2(frequencyHz / C1BaseFrequencyHz);
        return Math.Clamp((int)(scaled / PhaseScaleFactor), 0, ushort.MaxValue);
    }
}
