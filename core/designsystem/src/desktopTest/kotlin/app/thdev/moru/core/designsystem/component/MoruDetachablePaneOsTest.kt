package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.defaultCkTypography
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class MoruDetachablePaneOsTest {
    @Test
    fun draftAndResourceOwnerSurviveNativeMovesAndCloseRestoresPane() {
        val mounts = AtomicInteger()
        val disposals = AtomicInteger()
        var detached by mutableStateOf(false)
        var ownerPresent by mutableStateOf(true)
        var context by mutableStateOf("Task A")
        var dark by mutableStateOf(true)
        var language by mutableStateOf(UiLanguage.Korean)
        var fontSize by mutableStateOf(18)
        NativePaneTestWindow {
            val typography = defaultCkTypography.copy(body = defaultCkTypography.body.copy(
                fontSize = fontSize.sp, fontFamily = FontFamily.Monospace,
            ))
            MoruTheme(darkTheme = dark, typography = typography) {
                CompositionLocalProvider(LocalUiLanguage provides language) {
                if (ownerPresent) MoruDetachablePane(detached, { detached = false }, context,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Button(onClick = { detached = false }) { Text("Restore") } },
                ) { slot ->
                    var draft by remember { mutableStateOf("Empty") }
                    DisposableEffect(Unit) {
                        mounts.incrementAndGet()
                        onDispose { disposals.incrementAndGet() }
                    }
                    Column(slot) {
                        Text("Context: $context")
                        Text("Language: ${LocalUiLanguage.current}")
                        Text("Font size: ${MoruTheme.typography.body.fontSize}")
                        Text("Font family: ${MoruTheme.typography.body.fontFamily}")
                        Text(if (MoruTheme.colors.isLight) "Light theme" else "Dark theme")
                        Text("Draft: $draft")
                        Button(onClick = { draft = "Keep this" }) { Text("Edit draft") }
                        Button(onClick = { detached = !detached }) { Text(if (detached) "Dock" else "Detach") }
                    }
                }
                }
            }
        }.use { window ->
            window.waitFor("docked font settings") {
                window.exists("Pane fixture", "Font size: 18.0.sp") &&
                    window.exists("Pane fixture", "Font family: ${FontFamily.Monospace}")
            }
            window.click("Pane fixture", "Edit draft")
            window.click("Pane fixture", "Detach")
            window.waitFor("detached window") { window.visible("Task A") && mounts.get() > 0 }
            assertEquals(1, mounts.get(), "Moving must retain the original content owner")
            assertEquals(0, disposals.get(), "Moving must not dispose a preview or stream")
            window.waitFor("detached draft") { window.exists("Task A", "Draft: Keep this") }
            window.waitFor("inherited product context") {
                window.exists("Task A", "Language: Korean") && window.exists("Task A", "Dark theme") &&
                    window.exists("Task A", "Font size: 18.0.sp")
            }
            window.resize("Task A", 480, 360)
            window.runOnEdt { context = "Task B"; language = UiLanguage.English; dark = false; fontSize = 20 }
            window.waitFor("current context") {
                window.exists("Task B", "Context: Task B") && window.exists("Task B", "Language: English") &&
                    window.exists("Task B", "Light theme") && window.exists("Task B", "Font size: 20.0.sp")
            }
            window.click("Task B", "Dock")
            window.waitFor("restored draft and updated font") {
                window.exists("Pane fixture", "Draft: Keep this") && window.exists("Pane fixture", "Font size: 20.0.sp") &&
                    !window.visible("Task B")
            }
            window.click("Pane fixture", "Detach")
            window.waitFor("second window") { window.visible("Task B") }
            window.closeNative("Task B")
            window.waitFor("native close docks") { window.exists("Pane fixture", "Draft: Keep this") && !window.visible("Task B") }
            window.click("Pane fixture", "Detach")
            window.waitFor("third window") { window.visible("Task B") }
            window.click("Pane fixture", "Restore")
            window.waitFor("main restore docks") { window.exists("Pane fixture", "Draft: Keep this") && !window.visible("Task B") }
            assertEquals(1, mounts.get())
            assertEquals(0, disposals.get())
            window.click("Pane fixture", "Detach")
            window.waitFor("owner window") { window.visible("Task B") }
            window.runOnEdt { ownerPresent = false }
            window.waitFor("owner cleanup") { disposals.get() == 1 && !window.visible("Task B") }
        }
        assertEquals(1, disposals.get())
    }
}
