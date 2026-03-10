using Avalonia.Controls;
using Avalonia.Input;
using JagFx.Desktop.Services;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Views;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        KeyDown += OnWindowKeyDown;
        DataContextChanged += OnDataContextChanged;
        Closing += OnWindowClosing;
    }

    private void OnWindowClosing(object? sender, System.ComponentModel.CancelEventArgs e)
    {
        (DataContext as MainViewModel)?.Dispose();
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
            if (path is null) return;

            if (path.EndsWith(".wav", StringComparison.OrdinalIgnoreCase))
                await vm.ExportToPathAsync(path);
            else
                vm.SaveToPath(path);
        };

        // Select first envelope by default
        vm.SelectEnvelope("PITCH");
    }

    private void OnWindowKeyDown(object? sender, KeyEventArgs e)
    {
        if (DataContext is not MainViewModel vm) return;

        // Don't handle shortcuts when a text input is focused
        if (FocusManager?.GetFocusedElement() is TextBox or NumericUpDown) return;

        switch (e.Key)
        {
            case Key.Space:
                vm.TogglePlayCommand.Execute(null);
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

            case Key.V when e.KeyModifiers == KeyModifiers.None:
                vm.PlaySingleVoice = !vm.PlaySingleVoice;
                e.Handled = true;
                break;

            case Key.Up:
                vm.SelectEnvelopeByOffset(-1);
                e.Handled = true;
                break;

            case Key.Down:
                vm.SelectEnvelopeByOffset(1);
                e.Handled = true;
                break;
        }
    }
}
