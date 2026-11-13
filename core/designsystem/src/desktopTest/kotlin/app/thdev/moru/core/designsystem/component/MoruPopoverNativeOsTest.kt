package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.Alignment
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Popover pixels and keyboard focus as the OS presents them. Needs the machine to itself. */
class MoruPopoverNativeOsTest {
    @Test
    fun repeatedDarkPopoversKeepThemeBackingInsteadOfWhiteWindowPixels() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        PopoverWindowFixture(true).use { fixture ->
            repeat(5) { cycle ->
                fixture.open(Alignment.TopStart)
                fixture.waitFor("compact popup settles") { fixture.onEdt { fixture.popup()?.height in 42..46 } }
                fixture.onEdt {
                    Desktop.getDesktop().requestForeground(true)
                    checkNotNull(fixture.popup()).apply { toFront(); requestFocus() }
                }
                fixture.waitFor("popup native focus") { fixture.onEdt { fixture.popup()?.isFocused == true } }
                Thread.sleep(200)
                val bounds = fixture.onEdt { checkNotNull(fixture.popup()).bounds }
                val pixels = Robot().createScreenCapture(bounds)
                val artifact = java.io.File("build/ui-captures/popover-backing-$cycle.png")
                artifact.parentFile.mkdirs()
                javax.imageio.ImageIO.write(pixels, "png", artifact)
                val pixel = java.awt.Color(pixels.getRGB(pixels.width - 12, pixels.height / 2))
                assertTrue(pixel.red < 48 && pixel.green < 48 && pixel.blue < 48,
                    "Dark popup backing must stay near the theme surface, not blend with white: $pixel")
                fixture.hide()
            }
        }
    }

    @Test
    fun modifierShortcutsEscapeAndClicksOutsideStillReachTheNativePopover() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        PopoverWindowFixture(false).use { fixture ->
            fixture.open(Alignment.TopEnd)
            fixture.waitFor("active popup or owner keyboard target") { fixture.onEdt { fixture.hasKeyboardTarget() } }
            fixture.postKey(KeyEvent.VK_1, '1')
            fixture.waitFor("menu-local shortcut") { fixture.onEdt { fixture.shortcuts == 1 } }
            fixture.postKey(KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)
            fixture.waitFor("native Escape closes once") { fixture.onEdt { fixture.popup() == null && fixture.dismissals == 1 } }

            fixture.open(Alignment.TopEnd)
            fixture.waitFor("reopened popup") { fixture.onEdt { fixture.popup() != null } }
            fixture.onEdt {
                val content = fixture.owner.contentPane
                content.dispatchEvent(MouseEvent(content, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                    2, content.height - 2, 1, false, MouseEvent.BUTTON1))
            }
            fixture.waitFor("native outside click closes once") { fixture.onEdt { fixture.popup() == null && fixture.dismissals == 2 } }
            assertEquals(1, fixture.onEdt { fixture.shortcuts })
        }
    }
}
