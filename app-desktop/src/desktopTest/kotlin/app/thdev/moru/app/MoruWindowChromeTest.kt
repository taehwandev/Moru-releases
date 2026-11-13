package app.thdev.moru.app

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import javax.swing.JRootPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoruWindowChromeTest {
    @Test
    fun onMacTheTitleBarIsMadeTransparentTheContentRunsUnderItAndTheTitleTextIsHidden() {
        val rootPane = JRootPane()

        MoruWindowChrome.apply(rootPane, osName = "Mac OS X")

        assertEquals(true, rootPane.getClientProperty("apple.awt.fullWindowContent"))
        assertEquals(true, rootPane.getClientProperty("apple.awt.transparentTitleBar"))
        assertEquals(false, rootPane.getClientProperty("apple.awt.windowTitleVisible"))
    }

    @Test
    fun elsewhereTheWindowIsLeftAlone() {
        val rootPane = JRootPane()

        MoruWindowChrome.apply(rootPane, osName = "Linux")

        MoruWindowChrome.macProperties.keys.forEach { key -> assertNull(rootPane.getClientProperty(key)) }
    }

    @Test
    fun contentIsKeptClearOfTheWindowButtonsOnlyOnMac() {
        assertTrue(MoruWindowChrome.topInsetDp("Mac OS X") > 0)
        assertEquals(0, MoruWindowChrome.topInsetDp("Windows 11"))
        assertEquals(0, MoruWindowChrome.topInsetDp("Linux"))
    }

    @Test
    fun theJdkInUseUnderstandsTheseWindowProperties() {
        // The names are read from the bytes of the JDK's own macOS window class, so a rename there fails here instead
        // of silently leaving the title bar following the system. Only meaningful where that class exists.
        val file = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "java.desktop", "sun/lwawt/macosx/CPlatformWindow.class")
        if (!Files.exists(file)) return
        val text = String(Files.readAllBytes(file), Charsets.ISO_8859_1)
        MoruWindowChrome.macProperties.keys.forEach { key ->
            assertTrue(key in text, "$key is not a window property of this JDK")
        }
    }
}
