using JagFX.Core.Constants;
using JagFX.Domain.Models;

namespace JagFX.Synthesis.Processing;

public class EnvelopeGenerator(Envelope envelope)
{
    private const int EnvelopeScaleFactor = 15;

    private int _amplitude;
    private int _delta;
    private int _position;
    private int _threshold;
    private int _ticks;

    public Envelope Envelope { get; } = envelope;

    public void Reset()
    {
        _threshold = 1;
        _position = 0;
        _delta = 0;
        _amplitude = Envelope.StartValue << EnvelopeScaleFactor;
        _ticks = 0;
    }

    public int Evaluate(int period)
    {
        if (Envelope.Segments.Count == 0)
        {
            return Envelope.StartValue;
        }

        if (_ticks >= _threshold)
        {
            AdvanceSegment(period);
        }

        _amplitude += _delta;
        _ticks++;
        return (_amplitude - _delta) >> EnvelopeScaleFactor;
    }

    private void AdvanceSegment(int period)
    {
        _amplitude = Envelope.Segments[_position].TargetLevel << EnvelopeScaleFactor;
        _position++;

        if (_position >= Envelope.Segments.Count)
        {
            _position = Envelope.Segments.Count - 1;
        }

        _threshold = (int)(Envelope.Segments[_position].Duration / (double)AudioConstants.FixedPoint.Scale * period);
        _delta = _threshold > _ticks
            ? ((Envelope.Segments[_position].TargetLevel << EnvelopeScaleFactor) - _amplitude) / (_threshold - _ticks)
            : 0;
    }
}
