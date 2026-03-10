using System.ComponentModel;
using System.Linq;
using Avalonia.Controls;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Views.Footer;

public partial class PartialsFooter : UserControl
{
    private VoiceViewModel? _subscribedVoice;
    private PatchViewModel? _subscribedPatch;
    private MainViewModel? _subscribedVm;

    public PartialsFooter()
    {
        InitializeComponent();
        Bank1Toggle.Click += (_, _) => SetBank(0);
        Bank2Toggle.Click += (_, _) => SetBank(5);
        BtnOne.Click += (_, _) => SetPlaySingleVoice(true);
        BtnAll.Click += (_, _) => SetPlaySingleVoice(false);
        BtnLoop.Click += (_, _) => ToggleLoop();
        BtnTrue.Click += (_, _) => ToggleTrueWave();
        BtnCopy.Click += (_, _) => CopyVoice();
        BtnReset.Click += (_, _) => ResetVoice();
        BtnMain.Click += (_, _) => SetGridMode(GridMode.Main);
        BtnFilt.Click += (_, _) => SetGridMode(GridMode.Filter);
        BtnBoth.Click += (_, _) => SetGridMode(GridMode.Both);
        DataContextChanged += (_, _) => OnViewModelChanged();
    }

    private void OnViewModelChanged()
    {
        if (_subscribedVm is not null)
            _subscribedVm.PropertyChanged -= OnMainVmPropertyChanged;

        if (DataContext is not MainViewModel vm) return;

        _subscribedVm = vm;
        vm.PropertyChanged += OnMainVmPropertyChanged;
        SubscribeToVoiceChanges(vm);
        BindPartialSlots();
    }

    private void OnMainVmPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
    }

    private void SubscribeToVoiceChanges(MainViewModel vm)
    {
        if (_subscribedVoice is not null)
            _subscribedVoice.PropertyChanged -= OnVoicePropertyChanged;

        if (_subscribedPatch is not null)
            _subscribedPatch.PropertyChanged -= OnPatchPropertyChanged;

        _subscribedPatch = vm.Patch;
        _subscribedPatch.PropertyChanged += OnPatchPropertyChanged;

        _subscribedVoice = vm.Patch.SelectedVoice;
        if (_subscribedVoice is not null)
            _subscribedVoice.PropertyChanged += OnVoicePropertyChanged;
    }

    private void OnPatchPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName != nameof(PatchViewModel.SelectedVoice)) return;

        if (DataContext is MainViewModel vm)
            SubscribeToVoiceChanges(vm);

        BindPartialSlots();
    }

    private void OnVoicePropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(VoiceViewModel.VisiblePartials))
            BindPartialSlots();
    }

    private void SetBank(int offset)
    {
        if (DataContext is not MainViewModel vm) return;
        var voice = vm.Patch.SelectedVoice;
        voice.PartialBankOffset = offset;
        Bank1Toggle.IsChecked = offset == 0;
        Bank2Toggle.IsChecked = offset == 5;
        BindPartialSlots();
    }

    private void SetPlaySingleVoice(bool single)
    {
        if (DataContext is MainViewModel vm)
            vm.PlaySingleVoice = single;
    }

    private void ToggleLoop()
    {
        if (DataContext is MainViewModel vm)
            vm.IsLooping = !vm.IsLooping;
    }

    private void ToggleTrueWave()
    {
        if (DataContext is MainViewModel vm)
            vm.TrueWaveEnabled = !vm.TrueWaveEnabled;
    }

    private void CopyVoice()
    {
        if (DataContext is MainViewModel vm)
            vm.CopyVoice();
    }

    private void ResetVoice()
    {
        if (DataContext is MainViewModel vm)
            vm.ResetVoice();
    }

    private void SetGridMode(GridMode mode)
    {
        if (DataContext is MainViewModel vm)
            vm.GridMode = mode;
    }

    private void BindPartialSlots()
    {
        if (DataContext is not MainViewModel vm) return;

        var partials = vm.Patch.SelectedVoice.VisiblePartials.ToArray();
        ContentControl[] slots = [Slot0, Slot1, Slot2, Slot3, Slot4];

        for (var i = 0; i < slots.Length; i++)
            slots[i].Content = i < partials.Length ? partials[i] : null;
    }
}
