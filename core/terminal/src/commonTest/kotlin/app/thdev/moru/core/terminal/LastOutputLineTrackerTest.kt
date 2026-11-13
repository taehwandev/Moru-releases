package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastOutputLineTrackerTest {
    @Test
    fun keepsTheNewestLineThatSaysSomething() {
        val tracker = LastOutputLineTracker()

        assertEquals("Running tests", tracker.accept("Compiling\nRunning tests\n"))
    }

    @Test
    fun anUnfinishedPromptCountsBecauseItNeverEndsInANewline() {
        val tracker = LastOutputLineTracker()
        tracker.accept("Edit src/Main.kt\n")

        assertEquals("Proceed? (y/n)", tracker.accept("Proceed? (y/n) "))
    }

    @Test
    fun joinsALineSplitAcrossChunks() {
        val tracker = LastOutputLineTracker()
        tracker.accept("Reading the pro")

        assertEquals("Reading the project", tracker.accept("ject\n"))
    }

    @Test
    fun dropsColourAndCursorCodes() {
        val tracker = LastOutputLineTracker()

        assertEquals("error: build failed", tracker.accept("\u001B[31merror:\u001B[0m build failed\u001B[2K\n"))
    }

    @Test
    fun dropsKeyboardModeCodesThatCarryAPrivateMarker() {
        val tracker = LastOutputLineTracker()

        assertEquals(
            "auto mode on (shift+tab to cycle)",
            tracker.accept("auto mode on (shift+tab to cycle)\u001B[<u\u001B[>5u\u001B[>4;2m"),
        )
    }

    @Test
    fun aCursorMoveToAnotherRowEndsTheLineAndAMoveRightLeavesAGap() {
        val tracker = LastOutputLineTracker()

        assertEquals("What are we building", tracker.accept("~/git/KeyFlow\u001B[2BWhat\u001B[1Care\u001B[1Cwe building"))
    }

    @Test
    fun skipsBordersAndSpinnerFramesButKeepsTextInsideABorder() {
        val tracker = LastOutputLineTracker()
        tracker.accept("│ Thinking about the change │\n")

        assertEquals("Thinking about the change", tracker.accept("╰──────────────╯\n⠋\n"))
    }

    @Test
    fun aBareCarriageReturnStartsAFreshLineSoAProgressLineShowsItsLatestState() {
        val tracker = LastOutputLineTracker()

        assertEquals("Downloading 80%", tracker.accept("Downloading 20%\rDownloading 50%\rDownloading 80%"))
    }

    @Test
    fun nothingMeaningfulYetIsNull() {
        assertNull(LastOutputLineTracker().accept("\n  \n╭──╮\n"))
    }

    @Test
    fun aVeryLongLineIsShortened() {
        val line = LastOutputLineTracker().accept("x".repeat(500) + "\n")!!

        assertEquals(200, line.length)
        assertEquals('…', line.last())
    }
}
