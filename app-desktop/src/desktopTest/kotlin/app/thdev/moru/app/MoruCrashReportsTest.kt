package app.thdev.moru.app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoruCrashReportsTest {
    private class Shown(val title: String, val message: String, val url: String)

    @Test
    fun theFirstCrashOfARunIsOfferedOnceAndForgottenOnceAnswered() {
        val dir = Files.createTempDirectory("moru-crash").toFile()
        try {
            val shown = mutableListOf<Shown>()
            val reports = MoruCrashReports(dir, "26.42.4", "macOS") { title, message, url ->
                // While the alert is open the crash is kept, in case Moru ends before the user answers.
                assertTrue(dir.resolve("logs/pending-crash.txt").isFile)
                shown += Shown(title, message, url)
            }

            reports.crashed(Thread.currentThread(), IllegalStateException("boom"))
            reports.crashed(Thread.currentThread(), IllegalStateException("again"))

            assertEquals(1, shown.size, "a repeating error does not stack alerts")
            assertTrue("boom" in shown.single().message)
            assertTrue(shown.single().url.startsWith("https://github.com/taehwandev/Moru-kmp/issues/new?title=Moru%20crashed"))
            assertFalse(reports.pendingFile.exists(), "an answered crash is not offered again")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aCrashThatEndedMoruBeforeAnyoneSawItIsOfferedOnTheNextStartOnlyOnce() {
        val dir = Files.createTempDirectory("moru-crash-next").toFile()
        try {
            // Moru ends while the alert is up: the user never answered it.
            val ended = MoruCrashReports(dir, "26.42.4", "macOS") { _, _, _ -> error("Moru ended") }
            ended.crashed(Thread.currentThread(), IllegalStateException("boom"))
            assertTrue(ended.pendingFile.isFile)

            val shown = mutableListOf<Shown>()
            val next = MoruCrashReports(dir, "26.42.4", "macOS") { title, message, url -> shown += Shown(title, message, url) }
            next.offerPrevious()
            next.offerPrevious()

            assertEquals(1, shown.size)
            assertEquals(systemAppStrings().crashLastTimeTitle, shown.single().title)
            assertTrue("boom" in shown.single().message)
            assertTrue(shown.single().url.startsWith("https://github.com/taehwandev/Moru-kmp/issues/new?"))
            assertFalse(next.pendingFile.exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
