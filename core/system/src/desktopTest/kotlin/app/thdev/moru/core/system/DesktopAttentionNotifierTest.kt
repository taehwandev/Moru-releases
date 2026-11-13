package app.thdev.moru.core.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAttentionNotifierTest {
    @Test
    fun theNotificationCommandPassesTheTextAsArgumentsSoQuotesCannotBreakOut() {
        val command = osascriptNotificationCommand("Moru", "He said \"stop\" and \\ more")

        assertEquals("osascript", command.first())
        assertEquals(listOf("He said \"stop\" and \\ more", "Moru"), command.takeLast(2))
        assertTrue(command.none { part -> part.contains("He said") && part != "He said \"stop\" and \\ more" })
        assertTrue(command.contains("display notification (item 1 of argv) with title (item 2 of argv)"))
    }

    @Test
    fun theDesktopNotifierRunsTheCommandOnMacAndNeverElsewhereAndNeverFails() {
        val ran = mutableListOf<List<String>>()

        DesktopAttentionNotifier(osName = "Mac OS X", run = { ran += it }).show("Moru", "body")
        DesktopAttentionNotifier(osName = "Linux", run = { ran += it }).show("Moru", "body")
        DesktopAttentionNotifier(osName = "Mac OS X", run = { error("osascript missing") }).show("Moru", "body")

        assertEquals(1, ran.size)
        assertEquals("body", ran.single()[ran.single().size - 2])
    }

}
