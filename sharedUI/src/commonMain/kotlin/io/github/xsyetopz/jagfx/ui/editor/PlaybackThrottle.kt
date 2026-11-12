package io.github.xsyetopz.jagfx.ui.editor

private const val REDUCED_MOTION_INTERVAL_MS = 1000L

/**
 * Least time in ms between two playhead updates. `0` passes every position the audio clock
 * reports (about every 10 ms on desktop and 16 ms in the browser), so the playhead keeps pace with
 * the sound; [reducedMotion] lowers it to 1 Hz.
 */
internal fun playbackIntervalMs(reducedMotion: Boolean): Long =
    if (reducedMotion) REDUCED_MOTION_INTERVAL_MS else 0L

/**
 * Thins stream of playback positions to one per [intervalMs]. It reads timestamps given to
 * [offer], never clock, so result depends on stream alone.
 */
internal class PlaybackThrottle(
    private val intervalMs: Long,
) {
    private var lastPassedAtMs: Long? = null
    private var held: Float? = null

    /** [position] when it is first or [intervalMs] has passed since last one passed, else `null`. */
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

    /** Newest position that [offer] held back, once; `null` when last one passed. */
    fun flush(): Float? = held.also { held = null }
}
