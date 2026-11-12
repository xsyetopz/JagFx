package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.model.Patch
import kotlin.test.assertEquals

internal fun EditorState.after(vararg intents: EditorIntent): EditorState =
    intents.fold(this, ::reduce)

internal fun assertRoundTrips(patch: Patch) {
    assertEquals(patch, SynthCodec.decode(SynthCodec.encode(patch)).patch)
}
