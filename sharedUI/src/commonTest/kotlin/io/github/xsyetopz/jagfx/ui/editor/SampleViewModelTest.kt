package io.github.xsyetopz.jagfx.ui.editor

import androidx.lifecycle.viewModelScope
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SampleViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val files = SampleFiles()
    private val plays = mutableListOf<Pair<List<Short>, Int>>()
    private val audio =
        AudioOutput { pcm, rate ->
            plays += pcm.toList() to rate
            emptyFlow()
        }
    private val decoder =
        SampleDecoder { sample ->
            ShortArray(sample.sampleCount) { it.toShort() }
        }
    private val sampleBytes = JagVorbisCodec.encode(jagVorbis())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        EditorViewModel(
            files,
            audio,
            { _, _, _ -> ShortArray(0) },
            dispatcher,
            decoder,
        )

    private fun EditorViewModel.close() = viewModelScope.cancel()

    private fun EditorViewModel.openSample() {
        files.picked = OpenedFile("a.jagvorbis", "/dir/a.jagvorbis", sampleBytes)
        dispatch(RequestIntent.OpenSample)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun openSampleDecodesAndShowsDocument() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.openSample()

            val document = vm.state.value.sample!!
            assertEquals(listOf(PatchFileKind.Sample), files.pickedKinds)
            assertEquals("a", document.name)
            assertEquals("/dir/a.jagvorbis", document.path)
            assertEquals(jagVorbis(), document.sample)
            assertEquals(10, document.preview.samples.size)
            assertEquals(StatusHint.Loaded("a"), vm.state.value.statusHint)
            vm.close()
        }

    @Test
    fun loadFailureOfBadBytesSetsOpenFailedHint() =
        runTest(dispatcher) {
            val vm = viewModel()
            files.picked = OpenedFile("bad.jagvorbis", null, byteArrayOf(1, 2))

            vm.dispatch(RequestIntent.OpenSample)
            dispatcher.scheduler.advanceUntilIdle()

            assertNull(vm.state.value.sample)
            val hint = assertIs<StatusHint.OpenFailed>(vm.state.value.statusHint)
            assertEquals("bad.jagvorbis", hint.name)
            vm.close()
        }

    @Test
    fun saveWritesEditedHeaderToPathAndClearsDirty() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.openSample()

            vm.dispatch(SampleIntent.SetPingPong(true))
            vm.dispatch(RequestIntent.SaveSample)
            dispatcher.scheduler.advanceUntilIdle()

            val written = JagVorbisCodec.decode(files.stored.getValue("/dir/a.jagvorbis"))
            assertEquals(jagVorbis(pingPong = true), written)
            assertEquals(
                false,
                vm.state.value.sample!!
                    .isDirty,
            )
            vm.close()
        }

    @Test
    fun saveAsPicksSampleKindAndSuggestedName() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.openSample()
            files.saveAsPath = "/other/b.jagvorbis"

            vm.dispatch(RequestIntent.SaveSampleAs)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("a.jagvorbis"), files.suggestedNames)
            assertEquals(PatchFileKind.Sample, files.pickedKinds.last())
            assertEquals(
                "/other/b.jagvorbis",
                vm.state.value.sample!!
                    .path,
            )
            assertEquals(
                "b",
                vm.state.value.sample!!
                    .name,
            )
            vm.close()
        }

    @Test
    fun playbackUsesSampleRateAndLoopSequence() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.openSample()
            vm.dispatch(ViewIntent.SetLooping(true))
            vm.dispatch(ViewIntent.SetLoopCount(2))

            vm.dispatch(RequestIntent.TogglePlay)
            dispatcher.scheduler.advanceUntilIdle()

            val (pcm, rate) = plays.single()
            assertEquals(22050, rate)
            assertEquals(listOf<Short>(0, 1, 2, 3, 4, 5, 2, 3, 4, 5, 6, 7, 8, 9), pcm)
            vm.close()
        }

    @Test
    fun closeSampleReturnsToIntactPatch() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.dispatch(SlotIntent.SetVoiceEnabled(0, true))
            val patch = vm.state.value.patch
            vm.openSample()

            vm.dispatch(RequestIntent.CloseSample)

            assertNull(vm.state.value.sample)
            assertEquals(patch, vm.state.value.patch)
            assertEquals(0, plays.size)
            vm.close()
        }
}

private class SampleFiles : PatchFiles {
    val stored = mutableMapOf<String, ByteArray>()
    val suggestedNames = mutableListOf<String>()
    val pickedKinds = mutableListOf<PatchFileKind>()
    var picked: OpenedFile? = null
    var saveAsPath: String? = null

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
        stored[path] = bytes
    }

    override suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile? {
        pickedKinds += kind
        suggestedNames += suggestedName
        return saveAsPath?.let { path ->
            write(path, bytes)
            SavedFile(path.substringAfterLast('/'), path)
        }
    }
}
