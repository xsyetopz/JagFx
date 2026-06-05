using Avalonia.Controls;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Views.Header;

public partial class HeaderBar : UserControl
{
    public HeaderBar()
    {
        InitializeComponent();
        BtnLoop.Click += (_, _) =>
        {
            if (DataContext is MainViewModel vm)
            {
                vm.IsLooping = !vm.IsLooping;
            }
        };
    }
}
