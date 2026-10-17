package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/**
 * Applies a header-only edit to the open sample. The end trim stays within `0..loadedSampleCount`,
 * `loopBegin` within `0..loopEnd`, and `loopEnd` within `loopBegin..sampleCount`. Trimming pulls
 * the loop region in so that it ends at or before the new sample count.
 */
internal fun reduceSample(
    state: EditorState,
    intent: SampleIntent,
): EditorState {
    val document = state.sample ?: return state
    val edited = editSample(document, intent)
    return if (edited ==
        document.sample
    ) {
        state
    } else {
        state.copy(sample = document.copy(sample = edited))
    }
}

private fun editSample(
    document: SampleDocument,
    intent: SampleIntent,
): JagVorbis {
    val sample = document.sample
    return when (intent) {
        is SampleIntent.SetSampleCount -> {
            val count = intent.count.coerceIn(0, document.loadedSampleCount)
            val end = minOf(sample.loopEnd, count)
            sample.copy(
                sampleCount = count,
                loopBegin = minOf(sample.loopBegin, end),
                loopEnd = end,
            )
        }

        is SampleIntent.SetLoopBegin -> {
            sample.copy(loopBegin = intent.sample.coerceIn(0, maxOf(sample.loopEnd, 0)))
        }

        is SampleIntent.SetLoopEnd -> {
            val low = minOf(sample.loopBegin.coerceAtLeast(0), sample.sampleCount)
            sample.copy(loopEnd = intent.sample.coerceIn(low, sample.sampleCount))
        }

        is SampleIntent.SetPingPong -> {
            sample.copy(pingPong = intent.enabled)
        }
    }
}
