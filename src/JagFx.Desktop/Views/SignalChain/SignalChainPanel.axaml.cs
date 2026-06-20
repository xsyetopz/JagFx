using Avalonia.Controls;
using JagFx.Desktop.Controls;
using JagFx.Desktop.Controls.Canvases;
using JagFx.Desktop.ViewModels;
using JagFx.Domain.Models;

namespace JagFx.Desktop.Views.SignalChain;

public partial class SignalChainPanel : UserControl
{
    private MainViewModel? _subscribedVm;
    private PatchViewModel? _subscribedPatch;

    private abstract record SlotBase(SignalChainSlot Slot, Border Container);

    private sealed record EnvelopeSlot(
        SignalChainSlot Slot,
        Border Container,
        EnvelopeCanvas Canvas
    ) : SlotBase(Slot, Container);

    private sealed record SpecialSlot(SignalChainSlot Slot, Border Container, Control Canvas)
        : SlotBase(Slot, Container);

    private readonly List<SlotBase> _slots = [];

    // 3×4 matrix layout: [row][col] = (slot, type)
    private static readonly (SignalChainSlot Slot, SlotType Type)[,] Matrix =
    {
        {
            (SignalChainSlot.Pitch, SlotType.Envelope),
            (SignalChainSlot.VibratoRate, SlotType.Envelope),
            (SignalChainSlot.VibratoDepth, SlotType.Envelope),
            (SignalChainSlot.PoleZero, SlotType.PoleZero),
        },
        {
            (SignalChainSlot.Volume, SlotType.Envelope),
            (SignalChainSlot.TremoloRate, SlotType.Envelope),
            (SignalChainSlot.TremoloDepth, SlotType.Envelope),
            (SignalChainSlot.Filter, SlotType.Envelope),
        },
        {
            (SignalChainSlot.GapOff, SlotType.Envelope),
            (SignalChainSlot.GapOn, SlotType.Envelope),
            (SignalChainSlot.Output, SlotType.Waveform),
            (SignalChainSlot.Bode, SlotType.Bode),
        },
    };

    private enum SlotType
    {
        Envelope,
        PoleZero,
        Waveform,
        Bode,
    }

    private WaveformCanvas? _outCanvas;
    private PoleZeroCanvas? _pzCanvas;
    private FrequencyResponseCanvas? _bodeCanvas;

    public SignalChainPanel()
    {
        InitializeComponent();
        BuildMatrix();
        DataContextChanged += OnDataContextChanged;
    }

    private void BuildMatrix()
    {
        for (var row = 0; row < 3; row++)
        {
            for (var col = 0; col < 4; col++)
            {
                var (slot, slotType) = Matrix[row, col];
                var color = ThemeColors.SlotColor(slot);
                var slotLabel = Loc.GetUpper($"Slot{slot}");
                var slotTooltip = Loc.Get($"SlotTooltip{slot}");

                var titleBlock = CreateSlotHeader(slotLabel, slotTooltip);

                var canvas = CreateSlotCanvas(slot, slotType, color);
                if (canvas is null)
                {
                    continue;
                }

                switch (canvas)
                {
                    case PoleZeroCanvas pzc:
                        _pzCanvas = pzc;
                        break;
                    case WaveformCanvas wc:
                        _outCanvas = wc;
                        break;
                    case FrequencyResponseCanvas frc:
                        _bodeCanvas = frc;
                        break;
                    default:
                        break;
                }

                var container = WrapInCell(titleBlock, canvas, row, col, slot, slotType);
                SlotBase slotRecord = canvas is EnvelopeCanvas ec
                    ? new EnvelopeSlot(slot, container, ec)
                    : new SpecialSlot(slot, container, canvas);

                _slots.Add(slotRecord);
            }
        }
    }
}
