package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.InvalidPatchException
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Voice

internal fun editSelectedVoice(
    state: EditorState,
    edit: (Voice) -> VoiceOutcome,
): EditorState {
    val voice = state.voice ?: return reject(state, RejectReason.DisabledSlot)
    return when (val outcome = edit(voice)) {
        is VoiceOutcome.Changed -> {
            commit(
                state,
                state.patch.withVoice(state.selectedVoice, outcome.voice),
            )
        }

        is VoiceOutcome.Rejected -> {
            reject(state, outcome.reason)
        }
    }
}

internal fun Patch.withVoice(
    slot: Int,
    voice: Voice?,
): Patch = copy(voices = voices.toMutableList().apply { set(slot, voice) })

internal fun reject(
    state: EditorState,
    reason: RejectReason,
): EditorState = state.copy(statusHint = StatusHint.EditRejected(reason))

/** Accepts [patch] if it changed and encodes, recording the old patch in the history. */
internal fun commit(
    state: EditorState,
    patch: Patch,
): EditorState {
    if (patch == state.patch) return state
    val failure =
        try {
            SynthCodec.encode(patch)
            null
        } catch (e: InvalidPatchException) {
            e
        }
    return if (failure != null) {
        state.copy(
            statusHint = StatusHint.EditRejected(RejectReason.NoBinaryEncoding, failure.invariant),
        )
    } else {
        state.copy(
            patch = patch,
            history = state.history.record(state.patch),
            statusHint = state.statusHint.takeUnless { it is StatusHint.EditRejected },
        )
    }
}
