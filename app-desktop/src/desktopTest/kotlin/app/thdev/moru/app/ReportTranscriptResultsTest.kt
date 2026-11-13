package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportTranscriptResultsTest {
    @Test
    fun theTranscriptIsTakenOnlyForTheMessageTheScreenShows() {
        val transcript = "26.42.11을 **빌드**하고 공증하는 중입니다.\n\n- 배포 대상: main"
        assertTrue(transcriptMatchesScreen(transcript, "26.42.11을 빌드하고 공증하는 중입니다.\n• 배포 대상: main"))
        assertFalse(transcriptMatchesScreen("The previous turn's report.", "26.42.11을 빌드하고 공증하는 중입니다."))
        // A message taller than the screen shows only its ending, below an earlier step's mark.
        val long = (1..40).joinToString("\n") { "Line $it of the report." }
        assertTrue(transcriptMatchesScreen(long, "Read(src/Main.kt)\nLine 39 of the report.\nLine 40 of the report."))
    }

    @Test
    fun seenResultsKeepOnlyTheNewestOnce() {
        assertEquals(listOf(2, 3, 1), listOf(1, 2, 3).plusSeen(1))
        assertEquals(200, (1..250).toList().plusSeen(999).size)
    }
}
