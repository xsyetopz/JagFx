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
        BtnBankPrev.Click += (_, _) => CycleBank(-1);
        BtnBankNext.Click += (_, _) => CycleBank(1);
        BtnOne.Click += (_, _) => { if (DataContext is MainViewModel vm) vm.PlaySingleVoice = true; };
        BtnAll.Click += (_, _) => { if (DataContext is MainViewModel vm) vm.PlaySingleVoice = false; };
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

    private void CycleBank(int direction)
    {
        if (DataContext is not MainViewModel vm) return;
        var voice = vm.Patch.SelectedVoice;
        var newOffset = voice.PartialBankOffset + direction * 5;
        if (newOffset < 0 || newOffset > 5) return;
        voice.PartialBankOffset = newOffset;
        BindPartialSlots();
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
