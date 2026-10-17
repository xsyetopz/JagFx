package io.github.xsyetopz.jagfx.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisFormatException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the [EditorState] and performs the side effects of [RequestIntent]s.
 *
 * Every patch or playback-slot change renders a one-pass preview on [renderDispatcher]; a newer
 * render discards an older one. With true-wave off, renders wait until no gesture is open.
 * Opening a sample decodes it on [renderDispatcher] with [sampleDecoder].
 * Call [dispatch] from the main thread.
 */
internal class EditorViewModel(
    private val files: PatchFiles,
    private val audio: AudioOutput,
    private val renderer: PatchRenderer = PatchRenderer.Default,
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val sampleDecoder: SampleDecoder = SampleDecoder.Default,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = mutableState.asStateFlow()

    private val patchFiles = PatchFileTasks(files, { state.value }, ::dispatch)
    private val transfers = ToneFilterTransfer(files, { state.value }, ::dispatch)

    private var previewJob: Job? = null
    private var previewRequest: RenderRequest? = null
    private var playbackJob: Job? = null
    private var samplePcm = ShortArray(0)

    init {
        schedulePreview(mutableState.value)
    }

    fun dispatch(intent: EditorIntent) {
        val next = reduce(mutableState.value, intent)
        mutableState.value = next
        if (intent is RequestIntent) perform(intent) else schedulePreview(next)
    }

    private fun perform(intent: RequestIntent) {
        when (intent) {
            RequestIntent.Open -> launchFileTask { patchFiles.open() }

            RequestIntent.Save -> launchFileTask { patchFiles.save(pick = false) }

            RequestIntent.SaveAs -> launchFileTask { patchFiles.save(pick = true) }

            RequestIntent.ImportTone -> launchFileTask { transfers.importTone() }

            RequestIntent.ExportTone -> launchFileTask { transfers.exportTone() }

            RequestIntent.ImportFilter -> launchFileTask { transfers.importFilter() }

            RequestIntent.ExportFilter -> launchFileTask { transfers.exportFilter() }

            RequestIntent.OpenSample,
            RequestIntent.SaveSample,
            RequestIntent.SaveSampleAs,
            RequestIntent.CloseSample,
            -> performSample(intent)

            RequestIntent.TogglePlay -> play()

            RequestIntent.Stop -> playbackJob?.cancel()
        }
    }

    private fun performSample(intent: RequestIntent) {
        when (intent) {
            RequestIntent.OpenSample -> launchFileTask { openSample() }
            RequestIntent.SaveSample -> launchFileTask { patchFiles.saveSample(pick = false) }
            RequestIntent.SaveSampleAs -> launchFileTask { patchFiles.saveSample(pick = true) }
            RequestIntent.CloseSample -> playbackJob?.cancel()
            else -> Unit
        }
    }

    private fun schedulePreview(state: EditorState) {
        val request = RenderRequest(state.patch, loopCount = 1, slot = state.playbackSlot)
        val ready = state.trueWave || state.history.gestureDepth == 0
        if (!ready || request == previewRequest) return
        previewRequest = request
        previewJob?.cancel()
        previewJob =
            viewModelScope.launch {
                val pcm = render(request)
                dispatch(ResultIntent.PreviewRendered(pcm))
            }
    }

    /** Stops playback, then plays the patch. An endless loop replays until stopped. */
    private fun play() {
        val previous = playbackJob
        playbackJob =
            viewModelScope.launch {
                previous?.cancelAndJoin()
                dispatch(ResultIntent.PlaybackStarted)
                try {
                    do {
                        val state = mutableState.value
                        val sample = state.sample?.sample
                        val pcm: ShortArray
                        val sampleRate: Int
                        if (sample != null) {
                            pcm = samplePlayback(sample, state.effectiveLoopCount)
                            sampleRate = sample.sampleRate
                        } else {
                            val request =
                                RenderRequest(
                                    state.playbackPatch(),
                                    state.effectiveLoopCount,
                                    state.playbackSlot,
                                )
                            pcm = render(request)
                            sampleRate = Renderer.SAMPLE_RATE
                        }
                        audio.play(pcm, sampleRate).collect {
                            dispatch(ResultIntent.PlaybackPosition(it))
                        }
                    } while (mutableState.value.let { it.isLooping && it.loopCount == 0 })
                } catch (e: AudioOutputException) {
                    dispatch(ResultIntent.PlaybackFailed(e.message.orEmpty()))
                } finally {
                    dispatch(ResultIntent.PlaybackStopped)
                }
            }
    }

    private suspend fun render(request: RenderRequest): ShortArray =
        withContext(renderDispatcher) {
            renderer.render(request.patch, request.loopCount, request.slot)
        }

    private suspend fun samplePlayback(
        sample: JagVorbis,
        passes: Int,
    ): ShortArray = withContext(renderDispatcher) { samplePlaybackPcm(samplePcm, sample, passes) }

    private fun launchFileTask(task: suspend () -> Unit) {
        viewModelScope.launch { task() }
    }

    /** Asks for a `.jagvorbis` file and opens it as the sample document. */
    private suspend fun openSample() {
        val name = "${state.value.fileName}.${PatchFileKind.Sample.extension}"
        val file =
            try {
                files.pickAndRead(PatchFileKind.Sample)
            } catch (e: PatchFileException) {
                dispatch(ResultIntent.LoadFailed(name, e.message.orEmpty()))
                return
            } ?: return
        val intent =
            try {
                val sample = JagVorbisCodec.decode(file.bytes)
                val pcm = withContext(renderDispatcher) { sampleDecoder.decode(sample) }
                samplePcm = pcm
                playbackJob?.cancel()
                ResultIntent.SampleLoaded(fileNameOf(file.name), file.path, sample, pcm)
            } catch (e: JagVorbisFormatException) {
                ResultIntent.LoadFailed(file.name, e.message.orEmpty())
            } catch (e: IllegalStateException) {
                ResultIntent.LoadFailed(file.name, e.message.orEmpty())
            }
        dispatch(intent)
    }

    private data class RenderRequest(
        val patch: Patch,
        val loopCount: Int,
        val slot: Int?,
    )
}
