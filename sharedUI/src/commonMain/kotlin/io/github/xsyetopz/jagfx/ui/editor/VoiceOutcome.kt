package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Voice

/** Result of edit of one voice: new voice, or reason edit is refused. */
internal sealed interface VoiceOutcome {
    data class Changed(
        val voice: Voice?,
    ) : VoiceOutcome

    data class Rejected(
        val reason: RejectReason,
    ) : VoiceOutcome
}
