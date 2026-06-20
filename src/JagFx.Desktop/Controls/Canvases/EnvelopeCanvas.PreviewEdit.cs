using Avalonia.Controls;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public partial class EnvelopeCanvas
{
    private void BeginPreviewEdit()
    {
        if (TopLevel.GetTopLevel(this)?.DataContext is MainViewModel vm)
        {
            vm.BeginPreviewEdit();
        }
    }

    private void EndPreviewEdit()
    {
        if (TopLevel.GetTopLevel(this)?.DataContext is MainViewModel vm)
        {
            vm.EndPreviewEdit();
        }
    }

    private void RequestPreviewUpdate(bool immediate = false)
    {
        if (TopLevel.GetTopLevel(this)?.DataContext is MainViewModel vm)
        {
            vm.RequestPreviewUpdate(immediate);
        }
    }
}
