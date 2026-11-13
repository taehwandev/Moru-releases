package app.thdev.moru.app

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruGlassRole
import app.thdev.moru.core.designsystem.component.moruGlassSurface
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsPopoverContentTest {
    @Test
    fun openingTheMenuDoesNothingAndEachActionInvokesOnlyItsCallback() = runDesktopComposeUiTest(width = 320, height = 300) {
        var settings = 0
        var notifications = 0
        var checks = 0
        var restarts = 0
        setContent {
            MoruTheme {
                SettingsPopoverContent("1.0.0", UpdateStatus.Idle, 3, { settings++ }, { notifications++ }, { checks++ }, { restarts++ },
                    Modifier.width(300.dp))
            }
        }
        runOnIdle { assertEquals(listOf(0, 0, 0, 0), listOf(settings, notifications, checks, restarts)) }
        onNodeWithText("3").assertExists()
        onNodeWithText("Moru 1.0.0").assertExists()
        onNodeWithTag("app-menu-settings").performClick()
        onNodeWithTag("app-menu-notifications").performClick()
        onNodeWithTag("app-menu-check-updates").performClick()
        runOnIdle { assertEquals(listOf(1, 1, 1, 0), listOf(settings, notifications, checks, restarts)) }
    }

    @Test
    fun aVerifiedReadyUpdateIsFirstAndRestartsOnlyOnUserActivation() = runDesktopComposeUiTest(width = 320, height = 300) {
        var restarts = 0
        var checks = 0
        setContent {
            MoruTheme {
                SettingsPopoverContent("1.0.0", UpdateStatus.Ready("1.1.0"), 0, {}, {}, { checks++ }, { restarts++ }, Modifier.width(300.dp))
            }
        }
        onNodeWithText("Update to 1.1.0 and restart").assertExists()
        val restart = onNodeWithTag("app-menu-restart")
        assertTrue(restart.fetchSemanticsNode().boundsInRoot.top < onNodeWithTag("app-menu-notifications").fetchSemanticsNode().boundsInRoot.top)
        runOnIdle { assertEquals(0, restarts); assertEquals(0, checks) }
        restart.performClick()
        runOnIdle { assertEquals(1, restarts); assertEquals(0, checks) }
    }

    @Test
    fun busyAndUnsupportedMenusCannotCheckForUpdates() {
        val cases = listOf(
            Triple("1.0.0", UpdateStatus.Checking, true),
            Triple("1.0.0", UpdateStatus.Downloading("1.1.0", 37), true),
            Triple("1.0.0", UpdateStatus.Installing("1.1.0"), true),
            Triple(null, UpdateStatus.Idle, true),
            Triple("1.0.0", UpdateStatus.Idle, false),
        )
        cases.forEach { (version, status, checkAvailable) ->
            runDesktopComposeUiTest(width = 320, height = 300) {
                var checks = 0
                setContent {
                    MoruTheme {
                        SettingsPopoverContent(version, status, 0, {}, {}, if (checkAvailable) ({ checks++ }) else null, {},
                            Modifier.width(300.dp))
                    }
                }
                onNodeWithTag("app-menu-check-updates").assertIsNotEnabled().performMouseInput {
                    moveTo(center); press(); release()
                }
                if (version == null || !checkAvailable) onNodeWithText("Updates require an installed Moru app.").assertExists()
                runOnIdle { assertEquals(0, checks) }
            }
        }
    }

    @Test
    fun narrowReadyVersionsWrapAndTheMenuRemainsCompactInBothThemes() {
        listOf(false, true).forEach { dark ->
            runDesktopComposeUiTest(width = 260, height = 340) {
                val version = "2026.10.09-beta.build-123456789"
                setContent {
                    MoruTheme(darkTheme = dark, materialStyle = MoruMaterialStyle.Apple) {
                        SettingsPopoverContent("2026.10.08", UpdateStatus.Ready(version), 12, {}, {}, {}, {},
                            Modifier.width(260.dp).moruGlassSurface(MoruGlassRole.Sheet, shadow = false))
                    }
                }
                onNodeWithText("Update to $version and restart").assertExists()
                onNodeWithText("Notifications").assertExists()
                onNodeWithText("Settings…").assertExists()
                val menu = onNodeWithTag("moru-settings-menu")
                val bounds = menu.fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.width <= 260f && bounds.height < 300f, "App actions must use a compact menu, not page padding")
                val artifact = File("build/ui-captures/settings-menu-ready-${if (dark) "dark" else "light"}.png")
                artifact.parentFile.mkdirs()
                ImageIO.write(menu.captureToImage().toAwtImage(), "png", artifact)
            }
        }
    }
}
