package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalBackgroundQueryTrackerTest {
    @Test
    fun aProgramCanAskAcrossAnyPtyChunkBoundaryWithEitherOscTerminator() {
        for (sequence in listOf("\u001b]11;?\u0007", "\u001b]11;?\u001b\\")) {
            for (split in 1 until sequence.length) {
                val tracker = TerminalBackgroundQueryTracker()
                assertFalse(tracker.accept(sequence.take(split)))
                assertTrue(tracker.accept(sequence.drop(split)))
            }
        }
    }

    @Test
    fun foregroundQueriesColorRepliesAndMalformedOutputDoNotRefreshTheBackground() {
        val tracker = TerminalBackgroundQueryTracker()
        assertFalse(tracker.accept("\u001b]10;?\u0007\u001b]11;rgb:ffff/ffff/ffff\u001b\\"))
        assertFalse(tracker.accept("\u001b]11;?broken\u0007"))
        assertTrue(tracker.accept("\u001b]11;?\u0007"))
    }

    @Test
    fun replayQueryCountIncludesEachQueryWithBothTerminators() {
        var queries = 0
        val tracker = TerminalBackgroundQueryTracker()
        "\u001b]11;?\u001b\\screen repaint\u001b[?1004h\u001b]11;?\u0007".forEach { char ->
            if (tracker.accept(char)) queries++
        }
        assertEquals(2, queries)
    }
}
