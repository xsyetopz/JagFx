package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice

/**
 * Returns the state after [intent]. Pure: side effects belong to the caller, which reports
 * their outcome with a [ResultIntent].
 *
 * A patch edit that the binary format cannot encode keeps the old patch and sets
 * [StatusHint.EditRejected]. Accepted patch edits enter [History].
 */
internal fun reduce(
    state: EditorState,
    intent: EditorIntent,
): EditorState =
    when (intent) {
        is EnvelopeIntent -> {
            editSelectedVoice(state) { reduceEnvelope(it, intent) }
        }

        is VoiceIntent -> {
            editSelectedVoice(state) { VoiceOutcome.Changed(reduceVoice(it, intent)) }
        }

        is PartialIntent -> {
            editSelectedVoice(
                state,
            ) { VoiceOutcome.Changed(reducePartial(it, intent)) }
        }

        is FilterIntent.SetEnabled -> {
            setFilterEnabled(state, intent.enabled)
        }

        is FilterIntent -> {
            editSelectedVoice(state) { reduceFilter(it, intent) }
        }

        is SlotIntent -> {
            reduceSlot(state, intent)
        }

        is LoopIntent -> {
            reduceLoop(state, intent)
        }

        is ViewIntent -> {
            reduceView(state, intent)
        }

        is HistoryIntent -> {
            reduceHistory(state, intent)
        }

        is ResultIntent -> {
            reduceResult(state, intent)
        }

        RequestIntent.CloseSample -> {
            state.copy(sample = null)
        }

        is SampleIntent -> {
            reduceSample(state, intent)
        }

        is RequestIntent -> {
            state
        }
    }

/**
 * Turns the filter of the selected voice off, keeping it in [EditorState.bypassedFilters], or
 * on, bringing back the kept filter or [DefaultFilter].
 */
private fun setFilterEnabled(
    state: EditorState,
    enabled: Boolean,
): EditorState {
    val voice = state.voice ?: return reject(state, RejectReason.DisabledSlot)
    val current = voice.filter
    return if (enabled == (current != null)) state else switchFilter(state, voice, current)
}

private fun switchFilter(
    state: EditorState,
    voice: Voice,
    current: Filter?,
): EditorState {
    val slot = state.selectedVoice
    val filter = if (current == null) state.bypassedFilters[slot] ?: DefaultFilter else null
    val patch = state.patch.withVoice(slot, voice.copy(filter = filter))
    val committed = commit(state, patch)
    return if (committed.patch == patch) {
        committed.copy(
            bypassedFilters = state.bypassedFilters.toMutableList().apply { set(slot, current) },
        )
    } else {
        committed
    }
}

private fun reduceSlot(
    state: EditorState,
    intent: SlotIntent,
): EditorState =
    when (intent) {
        is SlotIntent.SelectVoice -> {
            selectVoice(
                state,
                intent.slot.coerceIn(
                    0,
                    SynthLimits.VOICE_SLOTS - 1,
                ),
            )
        }

        is SlotIntent.SetVoice -> {
            if (intent.slot in 0 until SynthLimits.VOICE_SLOTS) {
                commit(state, state.patch.withVoice(intent.slot, intent.voice))
            } else {
                state
            }
        }

        is SlotIntent.SetVoiceEnabled -> {
            when {
                intent.slot !in 0 until SynthLimits.VOICE_SLOTS -> state
                !intent.enabled -> commit(state, state.patch.withVoice(intent.slot, null))
                state.patch.voices[intent.slot] != null -> state
                else -> commit(state, state.patch.withVoice(intent.slot, DefaultVoice))
            }
        }

        SlotIntent.CopyVoice -> {
            state.copy(copiedVoice = state.voice, isCopyMode = true)
        }

        SlotIntent.ResetVoice -> {
            commit(state, state.patch.withVoice(state.selectedVoice, null))
        }

        is SlotIntent.SelectTarget -> {
            state.copy(selectedTarget = intent.target)
        }

        is SlotIntent.SelectTargetByOffset -> {
            val chain = SignalChainSlot.envelopeChain
            val index = chain.indexOf(state.selectedTarget).coerceAtLeast(0)
            state.copy(selectedTarget = chain[(index + intent.offset).mod(chain.size)])
        }
    }

/** Selects [slot]; in copy mode, pastes the copied voice into another slot and leaves copy mode. */
private fun selectVoice(
    state: EditorState,
    slot: Int,
): EditorState {
    val copied = state.copiedVoice
    val pasted =
        if (state.isCopyMode && copied != null && slot != state.selectedVoice) {
            commit(state, state.patch.withVoice(slot, copied)).copy(isCopyMode = false)
        } else {
            state
        }
    return pasted.copy(selectedVoice = slot)
}

private fun reduceLoop(
    state: EditorState,
    intent: LoopIntent,
): EditorState {
    val loop = state.patch.loop
    val newLoop =
        when (intent) {
            is LoopIntent.SetLoopBegin -> {
                Loop(
                    Milliseconds(intent.ms.coerceIn(0, SynthLimits.U16_MAX)),
                    loop.end,
                )
            }

            is LoopIntent.SetLoopEnd -> {
                Loop(
                    loop.begin,
                    Milliseconds(intent.ms.coerceIn(0, SynthLimits.U16_MAX)),
                )
            }
        }
    return commit(state, state.patch.copy(loop = newLoop))
}

private fun reduceView(
    state: EditorState,
    intent: ViewIntent,
): EditorState =
    when (intent) {
        is ViewIntent.SetTrueWave -> {
            state.copy(trueWave = intent.enabled)
        }

        is ViewIntent.SetPlaySingleVoice -> {
            state.copy(playSingleVoice = intent.enabled)
        }

        is ViewIntent.SetLooping -> {
            state.copy(isLooping = intent.enabled)
        }

        is ViewIntent.SetLoopCount -> {
            state.copy(
                loopCount = intent.count.coerceIn(0, EditorState.MAX_LOOP_COUNT),
            )
        }
    }

private fun reduceHistory(
    state: EditorState,
    intent: HistoryIntent,
): EditorState =
    when (intent) {
        HistoryIntent.Undo -> {
            state.history.undo(state.patch)?.let { (patch, history) ->
                state.copy(patch = patch, history = history)
            }
                ?: state
        }

        HistoryIntent.Redo -> {
            state.history.redo(state.patch)?.let { (patch, history) ->
                state.copy(patch = patch, history = history)
            }
                ?: state
        }

        HistoryIntent.BeginGesture -> {
            state.copy(history = state.history.beginGesture(state.patch))
        }

        HistoryIntent.EndGesture -> {
            state.copy(history = state.history.endGesture(state.patch))
        }
    }
