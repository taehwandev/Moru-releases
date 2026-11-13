package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionPreviewUrlTest {
    @Test
    fun aPrintedAddressKeepsItsPathWhileThisTerminalServesItsPort() {
        val url = sessionPreviewUrl(
            printed = "http://localhost:3000/ko/account/",
            servedPorts = setOf(3000),
            served = "http://localhost:3000/",
        )

        assertEquals("http://localhost:3000/ko/account/", url)
    }

    @Test
    fun anAddressTheAgentOnlyMentionedIsNotThisSessionsPreview() {
        // An agent reported another project's dev server; nothing under this terminal listens on 3000.
        assertNull(sessionPreviewUrl(printed = "http://localhost:3000/ko/account/", servedPorts = emptySet(), served = null))
    }

    @Test
    fun theTerminalsOwnServerWinsOverAnAddressOnAnotherPort() {
        val url = sessionPreviewUrl(
            printed = "http://localhost:3000/",
            servedPorts = setOf(5173),
            served = "http://localhost:5173/",
        )

        assertEquals("http://localhost:5173/", url)
    }

    @Test
    fun anAddressWithoutAPortUsesTheSchemeDefault() {
        assertEquals("http://localhost/", sessionPreviewUrl("http://localhost/", setOf(80), served = null))
        assertNull(sessionPreviewUrl("http://localhost/", setOf(443), served = null))
    }
}
