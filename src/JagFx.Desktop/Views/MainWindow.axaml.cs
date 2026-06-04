using Avalonia.Controls;
using Avalonia.Input;
using JagFx.Desktop.Services;
using JagFx.Desktop.ViewModels;
using JagFx.Domain.Models;

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

    private void OnWindowClosing(object? sender, System.ComponentModel.CancelEventArgs e) =>
        (DataContext as MainViewModel)?.Dispose();

    private void OnDataContextChanged(object? sender, EventArgs e)
    {
        if (DataContext is not MainViewModel vm)
            return;

        vm.RequestOpenDialog = async () =>
        {
            var path = await FileDialogService.OpenSynthFileAsync(this).ConfigureAwait(true);
            if (path is not null)
                _ = vm.TryLoadFromPath(path);
        };

        vm.RequestSaveAsDialog = async () =>
        {
            var path = await FileDialogService
                .SaveSynthFileAsync(this, vm.PatchName)
                .ConfigureAwait(true);
            if (path is null)
                return;

            if (path.EndsWith(".wav", StringComparison.OrdinalIgnoreCase))
                await vm.ExportToPathAsync(path).ConfigureAwait(true);
            else
                vm.SaveToPath(path);
        };

        // Select first envelope by default
        vm.SelectEnvelope(SignalChainSlot.Pitch);
    }

    private void OnWindowKeyDown(object? sender, KeyEventArgs e)
    {
        if (DataContext is not MainViewModel vm)
            return;

        // Don't handle shortcuts when a text input is focused
        if (FocusManager?.GetFocusedElement() is TextBox or NumericUpDown)
            return;

        if (e.Key == Key.Space)
        {
            vm.TogglePlayCommand.Execute(null);
            e.Handled = true;
        }
        else if (e.Key == Key.S && e.KeyModifiers.HasFlag(KeyModifiers.Control))
        {
            vm.SaveCommand.Execute(null);
            e.Handled = true;
        }
        else if (e.Key == Key.O && e.KeyModifiers.HasFlag(KeyModifiers.Control))
        {
            vm.OpenCommand.Execute(null);
            e.Handled = true;
        }
        else if (e.Key == Key.V && e.KeyModifiers == KeyModifiers.None)
        {
            vm.PlaySingleVoice = !vm.PlaySingleVoice;
            e.Handled = true;
        }
        else if (e.Key == Key.Up)
        {
            vm.SelectEnvelopeByOffset(-1);
            e.Handled = true;
        }
        else if (e.Key == Key.Down)
        {
            vm.SelectEnvelopeByOffset(1);
            e.Handled = true;
        }
    }
}
