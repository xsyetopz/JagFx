package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.SynthLimits

/** Applies the outcome of a side effect to the state. */
internal fun reduceResult(
    state: EditorState,
    intent: ResultIntent,
): EditorState =
    when (intent) {
        is ResultIntent.Loaded -> {
            state.copy(
                patch = intent.patch,
                savedPatch = intent.patch,
                history = History(),
                selectedVoice =
                    intent.patch.voices
                        .indexOfFirst { it != null }
                        .coerceAtLeast(0),
                fileName = intent.name,
                filePath = intent.path,
                isCopyMode = false,
                bypassedFilters = List(SynthLimits.VOICE_SLOTS) { null },
                statusHint = StatusHint.Loaded(intent.name),
                preview = Preview.Empty,
            )
        }

        is ResultIntent.LoadFailed -> {
            state.copy(statusHint = StatusHint.OpenFailed(intent.name, intent.message))
        }

        is ResultIntent.Saved -> {
            state.copy(
                savedPatch = intent.patch,
                fileName = fileNameOf(intent.name),
                filePath = intent.path,
                statusHint = StatusHint.Saved(intent.name),
            )
        }

        is ResultIntent.Exported -> {
            state.copy(statusHint = StatusHint.Exported(intent.name))
        }

        is ResultIntent.SaveFailed -> {
            state.copy(statusHint = StatusHint.SaveFailed(intent.path, intent.message))
        }

        is ResultIntent.SampleLoaded, is ResultIntent.SampleSaved -> {
            reduceSampleResult(state, intent)
        }

        ResultIntent.PlaybackStarted -> {
            state.copy(isPlaying = true, playbackPosition = 0f)
        }

        is ResultIntent.PlaybackFailed -> {
            state.copy(statusHint = StatusHint.PlaybackFailed(intent.message))
        }

        ResultIntent.PlaybackStopped -> {
            state.copy(isPlaying = false, playbackPosition = 0f)
        }

        is ResultIntent.PlaybackPosition -> {
            state.copy(
                playbackPosition =
                    intent.fraction.takeUnless { it.isNaN() }?.coerceIn(0f, 1f) ?: 0f,
            )
        }

        is ResultIntent.PreviewRendered -> {
            state.copy(preview = Preview.fromPcm(intent.pcm))
        }
    }

private fun reduceSampleResult(
    state: EditorState,
    intent: ResultIntent,
): EditorState =
    when (intent) {
        is ResultIntent.SampleLoaded -> {
            state.copy(
                sample =
                    SampleDocument(
                        name = intent.name,
                        path = intent.path,
                        sample = intent.sample,
                        savedSample = intent.sample,
                        preview = Preview.fromPcm(intent.pcm),
                        loadedSampleCount = intent.sample.sampleCount,
                    ),
                statusHint = StatusHint.Loaded(intent.name),
            )
        }

        is ResultIntent.SampleSaved -> {
            state.copy(
                sample =
                    state.sample?.copy(
                        name = fileNameOf(intent.name),
                        path = intent.path,
                        savedSample = intent.sample,
                    ),
                statusHint = StatusHint.Saved(intent.name),
            )
        }

        else -> {
            state
        }
    }
