package io.github.xsyetopz.jagfx.ui.editor

private const val NORMAL_INTERVAL_MS = 250L
private const val REDUCED_MOTION_INTERVAL_MS = 1000L

/** Least time in ms between two playhead updates: 4 Hz, or 1 Hz when [reducedMotion] is on. */
internal fun playbackIntervalMs(reducedMotion: Boolean): Long =
    if (reducedMotion) REDUCED_MOTION_INTERVAL_MS else NORMAL_INTERVAL_MS

/**
 * Thins a stream of playback positions to one per [intervalMs]. It reads the timestamps given to
 * [offer], never a clock, so the result depends on the stream alone.
 */
internal class PlaybackThrottle(
    private val intervalMs: Long,
) {
    private var lastPassedAtMs: Long? = null
    private var held: Float? = null

    /** [position] when it is the first or [intervalMs] has passed since the last one passed, else `null`. */
    fun offer(
        position: Float,
        nowMs: Long,
    ): Float? {
        val last = lastPassedAtMs
        if (last == null || nowMs - last !in 0 until intervalMs) {
            lastPassedAtMs = nowMs
            held = null
            return position
        }
        held = position
        return null
    }

    /** The newest position that [offer] held back, once; `null` when the last one passed. */
    fun flush(): Float? = held.also { held = null }
}
