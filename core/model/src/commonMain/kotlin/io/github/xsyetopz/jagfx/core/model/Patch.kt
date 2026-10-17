package io.github.xsyetopz.jagfx.core.model

/** Loop region of a patch. The loop is active when [begin] is before [end]. */
public data class Loop(
    val begin: Milliseconds,
    val end: Milliseconds,
) {
    init {
        requireIn(SynthLimits.u16, begin.value, "Loop.begin")
        requireIn(SynthLimits.u16, end.value, "Loop.end")
    }

    public companion object {
        public val None: Loop = Loop(Milliseconds(0), Milliseconds(0))
    }
}

/** The value one `.synth` file encodes: ten voice slots and a loop region. */
public data class Patch(
    val voices: List<Voice?>,
    val loop: Loop,
) {
    init {
        require(voices.size == SynthLimits.VOICE_SLOTS) {
            "Patch.voices must have ${SynthLimits.VOICE_SLOTS} slots, had ${voices.size}"
        }
    }

    public companion object {
        public val Empty: Patch = Patch(List(SynthLimits.VOICE_SLOTS) { null }, Loop.None)
    }
}
