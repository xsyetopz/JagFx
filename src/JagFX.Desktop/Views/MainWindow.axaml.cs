using Avalonia.Controls;
using Avalonia.Input;
using JagFX.Desktop.Services;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Views;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        KeyDown += OnWindowKeyDown;
        DataContextChanged += OnDataContextChanged;
    }

    private void OnDataContextChanged(object? sender, EventArgs e)
    {
        if (DataContext is not MainViewModel vm) return;

        vm.RequestOpenDialog = async () =>
        {
            var path = await FileDialogService.OpenSynthFileAsync(this);
            if (path is not null) vm.LoadFromPath(path);
        };

        vm.RequestSaveAsDialog = async () =>
        {
            var path = await FileDialogService.SaveSynthFileAsync(this, vm.PatchName);
            if (path is not null) vm.SaveToPath(path);
        };

        vm.RequestExportDialog = async () =>
        {
            var path = await FileDialogService.SaveWavFileAsync(this, vm.PatchName);
            if (path is not null) await vm.ExportToPathAsync(path);
        };
    }

    private void OnWindowKeyDown(object? sender, KeyEventArgs e)
    {
        if (DataContext is not MainViewModel vm) return;

        switch (e.Key)
        {
            case Key.Space:
                vm.TogglePlayCommand.Execute(null);
                e.Handled = true;
                break;

            case Key.S when e.KeyModifiers == (KeyModifiers.Control | KeyModifiers.Shift):
                vm.SaveAsCommand.Execute(null);
                e.Handled = true;
                break;

            case Key.S when e.KeyModifiers.HasFlag(KeyModifiers.Control):
                vm.SaveCommand.Execute(null);
                e.Handled = true;
                break;

            case Key.O when e.KeyModifiers.HasFlag(KeyModifiers.Control):
                vm.OpenCommand.Execute(null);
                e.Handled = true;
                break;

            case Key.E when e.KeyModifiers.HasFlag(KeyModifiers.Control):
                vm.ExportCommand.Execute(null);
                e.Handled = true;
                break;
        }
    }
}
