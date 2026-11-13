package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalBackgroundColorReplyTest {
    @Test
    fun storesTheRgbThatWasSentEvenWhenTheCanvasChangedBeforeObservation() {
        val id = "theme-reply-race"
        try {
            val reply = terminalBackgroundColorReply("\u001b]11;rgb:1a1a/1b1b/2626\u001b\\")!!
            assertEquals(0x1a1b26, reply)
            SessionThemeColors.recordCanvas(id, 0xfafbfc)
            SessionThemeColors.recordBackgroundColor(id, reply)
            assertTrue(SessionThemeColors.hasStaleBackground(id))
        } finally {
            SessionThemeColors.forget(id)
        }
    }

    @Test
    fun acceptsBothTerminatorsAndScalesXtermRgbChannelWidths() {
        assertEquals(0xff0000, terminalBackgroundColorReply("\u001b]11;rgb:f/0/0\u0007"))
        assertEquals(0x123456, terminalBackgroundColorReply("\u001b]11;rgb:12/34/56\u001b\\"))
        assertNull(terminalBackgroundColorReply("\u001b]10;rgb:12/34/56\u0007"))
        assertNull(terminalBackgroundColorReply("\u001b]11;rgb:gg/34/56\u0007"))
        assertNull(terminalBackgroundColorReply("\u001b]11;rgb:12/34/56"))
    }
}
