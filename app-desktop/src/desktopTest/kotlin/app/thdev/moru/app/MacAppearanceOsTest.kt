package app.thdev.moru.app

import app.thdev.moru.core.model.AppTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.feature.settings.SettingsScreen
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import javax.swing.JFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacAppearanceOsTest {
    @Test
    fun firstSettingsEntryIsDarkBeforeTheApplicationAppearanceCatchesUp() {
        if (!macWithDisplay()) return
        Toolkit.getDefaultToolkit()
        MacAppearance.apply(MacAppearanceLook.Light)
        val repository = object : SettingsRepository {
            override fun loadSettings() = AppSettings()
            override fun saveSettings(settings: AppSettings) {}
        }
        try {
            NativeWorkbenchTestWindow(1100, 760, materialStyle = MoruMaterialStyle.Apple, darkTheme = true) {
                var screen by remember { mutableStateOf(MoruDestination.Workbench) }
                MoruTheme(darkTheme = true, materialStyle = MoruMaterialStyle.Apple) {
                    MoruFrame(screen, { screen = it }, topInset = 28.dp) {
                        if (screen == MoruDestination.Settings) SettingsScreen(StaticRuntimeStatusRepository, repository)
                        else SwingPanel(factory = { JPanel() }, background = Color.Blue, modifier = Modifier.fillMaxSize())
                    }
                }
            }.use { window ->
                val fills = mutableListOf<java.awt.Color>()
                repeat(2) { entry ->
                    window.click("Settings")
                    window.waitFor("Settings") { window.exists("General") }
                    val name = "settings-system-dark-$entry"
                    window.capture(name)
                    val image = ImageIO.read(File("build/ui-captures/$name.png"))
                    val fill = java.awt.Color(image.getRGB(image.width / 2, image.height - 12))
                    fills += fill
                    window.click("Workbench")
                    MacAppearance.apply(MacAppearanceLook.Dark)
                }
                fills.forEachIndexed { entry, fill ->
                    assertTrue(fill.red < 100 && fill.green < 100 && fill.blue < 100, "Entry $entry: $fill")
                }
            }
        } finally {
            MacAppearance.apply(MacAppearanceLook.System)
        }
    }

    private fun macWithDisplay() = MoruWindowChrome.isMac(System.getProperty("os.name").orEmpty()) && !GraphicsEnvironment.isHeadless()

    private fun systemStyleName(): String {
        val process = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        return if (text == "Dark") "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
    }

    private fun waitFor(expected: String): Pair<String?, List<String?>> {
        val deadline = System.nanoTime() + 8_000_000_000L
        var seen: Pair<String?, List<String?>> = null to emptyList()
        while (System.nanoTime() < deadline) {
            seen = MacAppearance.effectiveName() to MacAppearance.windowEffectiveNames()
            if (seen.first == expected && seen.second.isNotEmpty() && seen.second.all { it == expected }) return seen
            Thread.sleep(100)
        }
        return seen
    }

    @Test
    fun otherSystemsAreLeftAloneAndSaySo() {
        assertFalse(MacAppearance.apply(MacAppearanceLook.Dark, osName = "Linux"))
        assertNull(MacAppearance.effectiveName(osName = "Windows 11"))
        assertEquals(emptyList(), MacAppearance.windowEffectiveNames(osName = "Linux"))
    }

    @Test
    fun theLooksCarryAppKitsOwnNames() {
        assertEquals("NSAppearanceNameDarkAqua", MacAppearanceLook.Dark.appKitName)
        assertEquals("NSAppearanceNameAqua", MacAppearanceLook.Light.appKitName)
        assertNull(MacAppearanceLook.System.appKitName)
    }

    @Test
    fun everyThemeMapsToTheLookOfItsColorsAndSystemFollowsTheComputer() {
        AppTheme.entries.forEach { theme ->
            val expected = when {
                theme == AppTheme.System -> MacAppearanceLook.System
                theme.resolveColors(true).isLight -> MacAppearanceLook.Light
                else -> MacAppearanceLook.Dark
            }
            assertEquals(expected, macAppearanceFor(theme, systemDark = true), "$theme")
        }
        assertEquals(MacAppearanceLook.Dark, macAppearanceFor(AppTheme.Dark, systemDark = false))
        assertEquals(MacAppearanceLook.Dark, macAppearanceFor(AppTheme.TokyoNight, systemDark = false))
        assertEquals(MacAppearanceLook.Light, macAppearanceFor(AppTheme.Light, systemDark = true))
    }

    @Test
    fun aRealWindowIsDrawnDarkOrLightWhateverTheComputerIsSetTo() {
        if (!macWithDisplay()) return
        Toolkit.getDefaultToolkit() // AppKit must be running before it is asked for its application.
        val frame = JFrame("appearance test").apply { setSize(300, 200); isVisible = true }
        try {
            // Java pins a new application to the light look, so each look below is a real change from the start.
            assertTrue(MacAppearance.apply(MacAppearanceLook.Dark), "the request did not reach AppKit")
            assertEquals("NSAppearanceNameDarkAqua" to true, waitFor("NSAppearanceNameDarkAqua").let { it.first to it.second.all { name -> name == "NSAppearanceNameDarkAqua" } })

            assertTrue(MacAppearance.apply(MacAppearanceLook.Light))
            assertEquals("NSAppearanceNameAqua", waitFor("NSAppearanceNameAqua").first)

            assertTrue(MacAppearance.apply(MacAppearanceLook.System))
            assertEquals(systemStyleName(), waitFor(systemStyleName()).first)
        } finally {
            MacAppearance.apply(MacAppearanceLook.System)
            frame.dispose()
        }
    }
}
