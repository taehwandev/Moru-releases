package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import javax.imageio.ImageIO

class MoruDesktopRuntimePropertiesTest {
    @Test
    fun developmentRunUsesDistinctNameAndDevelopmentArtwork() = withDevelopment(true) {
        val properties = mutableMapOf<String, String>()
        MoruDesktopRuntimeProperties.configure(setProperty = properties::set)

        assertEquals("Moru Community Dev", MoruDesktopRuntimeProperties.applicationName)
        assertEquals("Moru Community Dev", properties[MoruDesktopRuntimeProperties.AppleApplicationNameProperty])
        assertContentEquals(resourcePixels("/icon-dev.png"), applicationPixels())
        assertFalse(resourcePixels("/icon.png").contentEquals(applicationPixels()))
    }

    @Test
    fun productionRunUsesOriginalNameAndArtwork() = withDevelopment(false) {
        val properties = mutableMapOf<String, String>()
        MoruDesktopRuntimeProperties.configure(setProperty = properties::set)

        assertEquals("Moru Community", MoruDesktopRuntimeProperties.applicationName)
        assertEquals("Moru Community", properties[MoruDesktopRuntimeProperties.AppleApplicationNameProperty])
        assertContentEquals(resourcePixels("/icon.png"), applicationPixels())
    }

    @Test
    fun setsApplicationNameWhenAbsent() {
        val properties = mutableMapOf<String, String>()

        MoruDesktopRuntimeProperties.configure(
            setProperty = properties::set,
        )

        assertEquals("Moru Community", properties[MoruDesktopRuntimeProperties.AppleApplicationNameProperty])
    }

    @Test
    fun overridesExistingApplicationName() {
        val properties = mutableMapOf(
            MoruDesktopRuntimeProperties.AppleApplicationNameProperty to "CustomMoru",
        )

        MoruDesktopRuntimeProperties.configure(
            setProperty = properties::set,
        )

        assertEquals("Moru Community", properties[MoruDesktopRuntimeProperties.AppleApplicationNameProperty])
    }

    @Test
    fun createsOwnedPopupWindowsAboveSwingTerminals() {
        val properties = mutableMapOf<String, String>()

        MoruDesktopRuntimeProperties.configure(setProperty = properties::set)

        assertEquals("WINDOW", properties[MoruDesktopRuntimeProperties.ComposeLayersTypeProperty])
    }

    @Test
    fun drawsSwingThroughOpenGlSoResizesKeepNoGraphicsMemory() {
        val properties = mutableMapOf<String, String>()

        MoruDesktopRuntimeProperties.configure(setProperty = properties::set)

        assertEquals("false", properties[MoruDesktopRuntimeProperties.Java2DMetalProperty])
    }

    @Test
    fun warmsUpGraphicsEnvironmentOnMacOsOnlyWhenHeadful() {
        var warmUpCount = 0

        MoruDesktopRuntimeProperties.warmUpGraphicsEnvironmentOnFirstThread(
            osName = "Mac OS X",
            isHeadless = { false },
            warmUp = { warmUpCount += 1 },
        )

        assertEquals(1, warmUpCount)
    }

    @Test
    fun skipsGraphicsEnvironmentWarmUpOnNonMacOs() {
        var warmUpCount = 0

        MoruDesktopRuntimeProperties.warmUpGraphicsEnvironmentOnFirstThread(
            osName = "Linux",
            isHeadless = { false },
            warmUp = { warmUpCount += 1 },
        )

        assertEquals(0, warmUpCount)
    }

    @Test
    fun skipsGraphicsEnvironmentWarmUpWhenHeadless() {
        var warmUpCount = 0

        MoruDesktopRuntimeProperties.warmUpGraphicsEnvironmentOnFirstThread(
            osName = "Mac OS X",
            isHeadless = { true },
            warmUp = { warmUpCount += 1 },
        )

        assertEquals(0, warmUpCount)
    }

    private fun applicationPixels(): IntArray = MoruDesktopRuntimeProperties.applicationIcon().let {
        it.getRGB(0, 0, it.width, it.height, null, 0, it.width)
    }

    private fun resourcePixels(path: String): IntArray = requireNotNull(javaClass.getResourceAsStream(path)).use {
        val image = ImageIO.read(it)
        image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    }

    private fun withDevelopment(development: Boolean, check: () -> Unit) {
        val previous = System.getProperty("moru.development")
        try {
            System.setProperty("moru.development", development.toString())
            check()
        } finally {
            if (previous == null) System.clearProperty("moru.development")
            else System.setProperty("moru.development", previous)
        }
    }
}
