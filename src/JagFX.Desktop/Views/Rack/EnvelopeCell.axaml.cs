using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;
using JagFX.Desktop.Controls;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Views.Rack;

public partial class EnvelopeCell : UserControl
{
    private string _cellTitle = "";
    private RackCellType _cellType = RackCellType.Envelope;
    private EnvelopeViewModel? _envelope;
    private FilterViewModel? _filter;
    private IBrush? _lineColor;
    private INotifyPropertyChanged? _subscribedEnvelope;

    public MainViewModel? MainViewModel { get; set; }

    public string CellTitle
    {
        get => _cellTitle;
        set
        {
            _cellTitle = value;
            TitleText.Text = value;
        }
    }

    public RackCellType CellType
    {
        get => _cellType;
        set
        {
            _cellType = value;
            UpdateCanvasVisibility();
        }
    }

    public EnvelopeViewModel? Envelope
    {
        get => _envelope;
        set
        {
            if (_subscribedEnvelope is not null)
                _subscribedEnvelope.PropertyChanged -= OnEnvelopePropertyChanged;

            _envelope = value;
            EnvCanvas.Envelope = value;

            if (value is not null)
            {
                value.PropertyChanged += OnEnvelopePropertyChanged;
                _subscribedEnvelope = value;
            }
            else
            {
                _subscribedEnvelope = null;
            }

            UpdateDimming();
        }
    }

    public FilterViewModel? Filter
    {
        get => _filter;
        set
        {
            _filter = value;
            PzCanvas.Filter = value;
            FreqCanvas.Filter = value;
        }
    }

    public IBrush? LineColor
    {
        get => _lineColor;
        set
        {
            _lineColor = value;
            EnvCanvas.LineColor = value;

            if (value is SolidColorBrush scb)
            {
                TitleText.Foreground = new SolidColorBrush(
                    Color.FromArgb(160, scb.Color.R, scb.Color.G, scb.Color.B));
            }
        }
    }

    public float[]? WaveformSamples { set => WaveCanvas.Samples = value; }

    public EnvelopeCell()
    {
        InitializeComponent();
        PointerPressed += OnCellPressed;
    }

    public void SetSelected(bool selected)
    {
        CellBorder.BorderBrush = new SolidColorBrush(
            Color.Parse(selected ? "#4db8d4" : "#272727"));
        CellBorder.BorderThickness = new Thickness(selected ? 1.5 : 0.5);
        EnvCanvas.IsSelected = selected;
    }

    private void OnCellPressed(object? sender, PointerPressedEventArgs e)
    {
        if (MainViewModel is not null && CellType == RackCellType.Envelope)
        {
            MainViewModel.SelectEnvelope(CellTitle);
        }
    }

    private void OnEnvelopePropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(EnvelopeViewModel.IsEmpty))
            UpdateDimming();
    }

    private void UpdateDimming() => Opacity = _envelope?.IsEmpty != false ? 0.5 : 1.0;

    private void UpdateCanvasVisibility()
    {
        EnvCanvas.IsVisible = _cellType == RackCellType.Envelope;
        PzCanvas.IsVisible = _cellType == RackCellType.PoleZero;
        WaveCanvas.IsVisible = _cellType == RackCellType.Output;
        FreqCanvas.IsVisible = _cellType == RackCellType.BodePlot;
    }
}
