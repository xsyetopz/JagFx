package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

/** Throws [InvalidPatchException] when the voice in [slot] breaks an invariant of spec §7. */
internal fun checkVoiceInvariants(
    slot: Int,
    voice: Voice,
) {
    fun fail(
        invariant: PatchInvariant,
        detail: String,
    ): Nothing = throw InvalidPatchException(invariant, slot, detail)

    if (voice.frequencyEnvelope.waveform ==
        Waveform.Off
    ) {
        fail(PatchInvariant.I1, "frequencyEnvelope")
    }
    if (voice.pitchLfo?.rate?.waveform == Waveform.Off) fail(PatchInvariant.I2, "pitchLfo.rate")
    if (voice.amplitudeLfo?.rate?.waveform ==
        Waveform.Off
    ) {
        fail(PatchInvariant.I2, "amplitudeLfo.rate")
    }
    if (voice.gate?.gapOff?.waveform == Waveform.Off) fail(PatchInvariant.I2, "gate.gapOff")
    voice.filter?.let { checkFilterInvariants(slot, it) }
}

/** Throws [InvalidPatchException] when the filter of the voice in [slot] breaks I3 or I4. */
internal fun checkFilterInvariants(
    slot: Int,
    filter: Filter,
) {
    fun fail(
        invariant: PatchInvariant,
        detail: String,
    ): Nothing = throw InvalidPatchException(invariant, slot, detail)

    if (filter.feedforward.isEmpty() &&
        filter.feedback.isEmpty()
    ) {
        fail(PatchInvariant.I3, "filter has no poles")
    }
    if ((filter.modulationEnvelope != null) != filter.needsModulationEnvelope) {
        val presence = if (filter.modulationEnvelope == null) "absent" else "present"
        fail(
            PatchInvariant.I4,
            "modulationEnvelope is $presence, " +
                "modulation mask is ${filter.modulationMask}, unity gains are " +
                "${filter.unityGain0} and ${filter.unityGain1}",
        )
    }
}
