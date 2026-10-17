package io.github.xsyetopz.jagfx.ui.editor

import androidx.lifecycle.viewModelScope
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.model.Patch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val files = FakeFiles()
    private val audio = FakeAudio()
    private val renderer = CountingRenderer()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = EditorViewModel(files, audio, renderer, dispatcher)

    private fun EditorViewModel.close() = viewModelScope.cancel()

    @Test
    fun editRendersOnePassPreviewOfWholePatch() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            dispatcher.scheduler.advanceUntilIdle()

            val call = renderer.calls.last()
            assertEquals(vm.state.value.patch, call.patch)
            assertEquals(1, call.loopCount)
            assertNull(call.slot)
            assertEquals(
                call.marker,
                vm.state.value.preview.samples
                    .single(),
            )
            vm.close()
        }

    @Test
    fun newerEditDiscardsPendingPreview() =
        runTest(dispatcher) {
            val vm = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            val before = renderer.calls.size

            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(VoiceIntent.SetDuration(500))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(before + 1, renderer.calls.size)
            assertEquals(vm.state.value.patch, renderer.calls.last().patch)
            vm.close()
        }

    @Test
    fun singleVoicePreviewRendersSelectedSlot() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(2, true))
            vm.dispatch(SlotIntent.SelectVoice(2))
            vm.dispatch(ViewIntent.SetPlaySingleVoice(true))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, renderer.calls.last().slot)
            vm.close()
        }

    @Test
    fun trueWaveOffRendersOnlyAtGestureEnd() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(ViewIntent.SetTrueWave(false))
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            dispatcher.scheduler.advanceUntilIdle()
            val before = renderer.calls.size

            vm.dispatch(HistoryIntent.BeginGesture)
            vm.dispatch(VoiceIntent.SetDuration(400))
            vm.dispatch(VoiceIntent.SetDuration(600))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(before, renderer.calls.size)

            vm.dispatch(HistoryIntent.EndGesture)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(before + 1, renderer.calls.size)
            assertEquals(vm.state.value.patch, renderer.calls.last().patch)
            vm.close()
        }

    @Test
    fun trueWaveOnRendersDuringGesture() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(HistoryIntent.BeginGesture)
            vm.dispatch(VoiceIntent.SetDuration(400))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(vm.state.value.patch, renderer.calls.last().patch)
            vm.close()
        }

    @Test
    fun openLoadsPickedFile() =
        runTest(dispatcher) {
            val patch = samplePatch()
            files.picked = OpenedFile("bell.synth", "/sfx/bell.synth", SynthCodec.encode(patch))
            val vm = viewModel()

            vm.dispatch(RequestIntent.Open)
            dispatcher.scheduler.advanceUntilIdle()

            val state = vm.state.value
            assertEquals(patch, state.patch)
            assertEquals("bell", state.fileName)
            assertEquals("/sfx/bell.synth", state.filePath)
            assertEquals(StatusHint.Loaded("bell"), state.statusHint)
            assertFalse(state.isDirty)
            vm.close()
        }

    @Test
    fun cancelledOpenKeepsState() =
        runTest(dispatcher) {
            val vm = viewModel()
            val before = vm.state.value

            vm.dispatch(RequestIntent.Open)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(before.patch, vm.state.value.patch)
            assertNull(vm.state.value.statusHint)
            vm.close()
        }

    @Test
    fun openOfUndecodableFileReportsFailure() =
        runTest(dispatcher) {
            val lfsPointer = "version https://git-lfs.github.com/spec/v1\n".encodeToByteArray()
            files.picked = OpenedFile("bad.synth", "/sfx/bad.synth", lfsPointer)
            val vm = viewModel()

            vm.dispatch(RequestIntent.Open)
            dispatcher.scheduler.advanceUntilIdle()

            val hint = assertIs<StatusHint.OpenFailed>(vm.state.value.statusHint)
            assertEquals("bad", hint.name)
            assertEquals(Patch.Empty, vm.state.value.patch)
            vm.close()
        }

    @Test
    fun importToneReplacesTheSelectedSlot() =
        runTest(dispatcher) {
            val voice = samplePatch().voices[0]!!
            files.picked = OpenedFile("hit.tone", "/sfx/hit.tone", SynthCodec.encodeTone(voice))
            val vm = viewModel()
            vm.dispatch(SlotIntent.SelectVoice(2))

            vm.dispatch(RequestIntent.ImportTone)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(PatchFileKind.Tone), files.pickedKinds)
            assertEquals(voice, vm.state.value.patch.voices[2])
            assertTrue(vm.state.value.isDirty)
            vm.close()
        }

    @Test
    fun importFilterReplacesTheFilterOfTheSelectedVoice() =
        runTest(dispatcher) {
            val filter = DefaultFilter.copy(unityGain0 = 5, unityGain1 = 5)
            files.picked =
                OpenedFile("lp.filter", "/sfx/lp.filter", SynthCodec.encodeFilter(filter))
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))

            vm.dispatch(RequestIntent.ImportFilter)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(PatchFileKind.Filter), files.pickedKinds)
            assertEquals(
                filter,
                vm.state.value.voice
                    ?.filter,
            )
            vm.close()
        }

    @Test
    fun exportToneAndFilterWriteTheirBlocks() =
        runTest(dispatcher) {
            files.saveAsPath = "/sfx/out"
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(FilterIntent.SetEnabled(true))
            val voice = vm.state.value.voice!!

            vm.dispatch(RequestIntent.ExportTone)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(voice, SynthCodec.decodeTone(files.stored.getValue("/sfx/out")).voice)

            vm.dispatch(RequestIntent.ExportFilter)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(
                voice.filter,
                SynthCodec.decodeFilter(files.stored.getValue("/sfx/out")).filter,
            )

            assertEquals(listOf(PatchFileKind.Tone, PatchFileKind.Filter), files.pickedKinds)
            assertEquals(listOf("untitled-1.tone", "untitled-1.filter"), files.suggestedNames)
            assertEquals(StatusHint.Exported("out"), vm.state.value.statusHint)
            vm.close()
        }

    @Test
    fun saveWithoutPathAsksThenWritesToChosenPath() =
        runTest(dispatcher) {
            files.saveAsPath = "/sfx/new.synth"
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))

            vm.dispatch(RequestIntent.Save)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("untitled.synth"), files.suggestedNames)
            val state = vm.state.value
            assertEquals("/sfx/new.synth", state.filePath)
            assertEquals("new", state.fileName)
            assertFalse(state.isDirty)
            assertEquals(
                state.patch,
                SynthCodec.decode(files.stored.getValue("/sfx/new.synth")).patch,
            )

            vm.dispatch(VoiceIntent.SetDuration(700))
            vm.dispatch(RequestIntent.Save)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, files.suggestedNames.size)
            assertEquals(
                vm.state.value.patch,
                SynthCodec.decode(files.stored.getValue("/sfx/new.synth")).patch,
            )
            vm.close()
        }

    @Test
    fun pathlessSaveKeepsNameAndAsksAgain() =
        runTest(dispatcher) {
            files.saveAsDownload = true
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))

            vm.dispatch(RequestIntent.Save)
            dispatcher.scheduler.advanceUntilIdle()
            val state = vm.state.value
            assertNull(state.filePath)
            assertEquals("untitled", state.fileName)
            assertFalse(state.isDirty)
            assertEquals(StatusHint.Saved("untitled.synth"), state.statusHint)

            vm.dispatch(RequestIntent.Save)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(2, files.suggestedNames.size)
            vm.close()
        }

    @Test
    fun failedWriteReportsAndStaysDirty() =
        runTest(dispatcher) {
            files.picked = OpenedFile("a.synth", "/sfx/a.synth", SynthCodec.encode(samplePatch()))
            files.failWrites = true
            val vm = viewModel()
            vm.dispatch(RequestIntent.Open)
            dispatcher.scheduler.advanceUntilIdle()
            vm.dispatch(VoiceIntent.SetDuration(700))

            vm.dispatch(RequestIntent.Save)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                StatusHint.SaveFailed("/sfx/a.synth", "disk full"),
                vm.state.value.statusHint,
            )
            assertTrue(vm.state.value.isDirty)
            vm.close()
        }

    @Test
    fun playReportsProgressThenStops() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.state.value.isPlaying)

            audio.progress.send(0.5f)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(0.5f, vm.state.value.playbackPosition)

            audio.finish()
            dispatcher.scheduler.advanceUntilIdle()
            assertFalse(vm.state.value.isPlaying)
            assertEquals(1, audio.plays)
            vm.close()
        }

    @Test
    fun playRendersLoopPassesAndPlaybackLoop() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(ViewIntent.SetLooping(true))
            vm.dispatch(ViewIntent.SetLoopCount(3))
            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()

            val call = renderer.calls.last()
            assertEquals(3, call.loopCount)
            assertEquals(vm.state.value.playbackPatch(), call.patch)
            vm.close()
        }

    @Test
    fun endlessLoopReplaysUntilStopped() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(ViewIntent.SetLooping(true))
            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(EditorState.INFINITE_LOOP_PASSES, renderer.calls.last().loopCount)

            audio.finish()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(2, audio.plays)
            assertTrue(vm.state.value.isPlaying)

            vm.dispatch(RequestIntent.Stop)
            dispatcher.scheduler.advanceUntilIdle()
            assertFalse(vm.state.value.isPlaying)
            assertEquals(2, audio.plays)
            vm.close()
        }

    @Test
    fun togglePlayWhilePlayingRestarts() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()

            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, audio.plays)
            assertEquals(1, audio.active)
            assertTrue(vm.state.value.isPlaying)
            vm.close()
        }

    @Test
    fun unavailableAudioReportsFailureAndStops() =
        runTest(dispatcher) {
            val failing = AudioOutput { _, _ -> flow { throw AudioOutputException("no device") } }
            val vm = EditorViewModel(files, failing, renderer, dispatcher)
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()

            assertFalse(vm.state.value.isPlaying)
            assertEquals(StatusHint.PlaybackFailed("no device"), vm.state.value.statusHint)
            vm.close()
        }

    private fun samplePatch(): Patch =
        EditorState()
            .after(SlotIntent.SetVoiceEnabled(0, true), VoiceIntent.SetDuration(250))
            .patch
}

private class RenderCall(
    val patch: Patch,
    val loopCount: Int,
    val slot: Int?,
    val marker: Float,
)

/** Returns one sample per call whose value identifies the call. */
private class CountingRenderer : PatchRenderer {
    val calls = mutableListOf<RenderCall>()

    override fun render(
        patch: Patch,
        loopCount: Int,
        slot: Int?,
    ): ShortArray {
        val sample = (calls.size + 1).toShort()
        calls +=
            RenderCall(patch, loopCount, slot, Preview.fromPcm(shortArrayOf(sample)).samples[0])
        return shortArrayOf(sample)
    }
}

/** Playback lasts until [finish]; [progress] values are emitted while playing. */
private class FakeAudio : AudioOutput {
    val progress = Channel<Float>(Channel.UNLIMITED)
    var plays = 0
    var active = 0

    override fun play(
        pcm: ShortArray,
        sampleRate: Int,
    ): Flow<Float> =
        flow {
            plays++
            active++
            try {
                for (value in progress) {
                    if (value < 0f) break
                    emit(value)
                }
            } finally {
                active--
            }
        }

    fun finish() {
        progress.trySend(-1f)
    }
}

private class FakeFiles : PatchFiles {
    val stored = mutableMapOf<String, ByteArray>()
    val suggestedNames = mutableListOf<String>()
    var picked: OpenedFile? = null
    var saveAsPath: String? = null
    var saveAsDownload = false
    var failWrites = false
    val pickedKinds = mutableListOf<PatchFileKind>()

    override suspend fun pickAndRead(kind: PatchFileKind): OpenedFile? {
        pickedKinds += kind
        return picked
    }

    override suspend fun read(path: String): ByteArray =
        stored[path] ?: throw PatchFileException("not found")

    override suspend fun write(
        path: String,
        bytes: ByteArray,
    ) {
        if (failWrites) throw PatchFileException("disk full")
        stored[path] = bytes
    }

    override suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile? {
        pickedKinds += kind
        suggestedNames += suggestedName
        if (saveAsDownload) return SavedFile(suggestedName, null)
        return saveAsPath?.let { path ->
            write(path, bytes)
            SavedFile(path.substringAfterLast('/'), path)
        }
    }
}
