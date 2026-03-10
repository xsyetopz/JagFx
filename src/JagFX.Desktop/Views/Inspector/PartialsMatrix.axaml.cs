using Avalonia.Controls;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Views.Inspector;

public partial class PartialsMatrix : UserControl
{
    public PartialsMatrix()
    {
        InitializeComponent();
        Bank1Toggle.Click += (_, _) => SetBank(0);
        Bank2Toggle.Click += (_, _) => SetBank(5);
    }

    private void SetBank(int offset)
    {
        if (DataContext is not VoiceViewModel vm) return;
        vm.PartialBankOffset = offset;
        Bank1Toggle.IsChecked = offset == 0;
        Bank2Toggle.IsChecked = offset == 5;
    }
}
