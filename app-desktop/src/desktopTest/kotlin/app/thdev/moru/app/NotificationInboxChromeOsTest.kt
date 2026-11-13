package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.feature.notifications.NotificationInboxContent
import app.thdev.moru.feature.notifications.NotificationInboxUiState
import java.awt.Frame
import java.awt.Desktop
import java.awt.Rectangle
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/** The inbox popover in a focused native window. Needs the machine to itself. */
class NotificationInboxChromeOsTest {
    @Test
    fun shortAndLongHistoryFitTheActualNarrowNativeWindow() {
        var count by mutableStateOf(1)
        NativeWorkbenchTestWindow(360, 420) {
            MoruTheme(darkTheme = true, materialStyle = MoruMaterialStyle.Apple) {
                MoruFrame(MoruDestination.Workbench, {}, topInset = 28.dp,
                    inbox = { Inbox(count) }) { MoruText("Workbench behind the inbox") }
            }
        }.use { ui ->
            ui.waitFor("title-bar bell") { ui.exists("Notifications") }
            onEdt {
                frame().setLocation(120, 120)
                frame().toFront()
                Desktop.getDesktop().requestForeground(true)
            }
            ui.waitFor("native owner focused before opening") { onEdt { frame().isFocused } }
            ui.click("Notifications")
            ui.waitFor("short native inbox") { ui.exists("Notification 0") && popupBounds().height >= 100 }
            val short = popupBounds()
            assertInsideOwner(short)
            assertTrue(short.height < 260, "A short history must not reserve the maximum panel height: $short")
            ui.hoverWithoutMovingPopover("Open task: Notification 0 in Project")
            ui.capture("notification-inbox-compact-native")
            onEdt { count = 30 }
            ui.waitFor("long history grows within the window") {
                popupBounds().height > short.height && ownerBounds().contains(popupBounds())
            }
            assertInsideOwner(popupBounds())
            ui.hoverWithoutMovingPopover("Open task: Notification 0 in Project")
            ui.capture("notification-inbox-bounded-native")
            ui.pressEscapeInPopover()
            ui.waitFor("bounded inbox closes by Escape") { !ui.exists("Mark all read") }
            ui.click("Notifications")
            ui.waitFor("bounded inbox reopens") { ui.exists("Mark all read") && ownerBounds().contains(popupBounds()) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Inbox(count: Int) {
        val rows = List(count) { index ->
            TaskNotification("notice-$index", "project", "task-$index", "Project", "Notification $index", "Task finished.", 1)
        }
        NotificationInboxContent(NotificationInboxUiState(loading = false, notifications = rows), {}, {}, {}, {}, {})
    }

    private fun assertInsideOwner(popup: Rectangle) {
        val owner = ownerBounds()
        assertTrue(owner.contains(popup), "Native popup must fit its owner content: popup=$popup, owner=$owner")
    }

    private fun frame(): Frame = Window.getWindows().filterIsInstance<Frame>()
        .single { it.title == "Task session test" && it.isShowing }

    private fun popupBounds(): Rectangle = onEdt { frame().ownedWindows.single { it.isShowing }.bounds }

    private fun ownerBounds(): Rectangle = onEdt {
        val content = (frame() as RootPaneContainer).contentPane
        Rectangle(content.locationOnScreen, content.size)
    }

    private fun <T> onEdt(action: () -> T): T {
        val value = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { value.set(runCatching(action)) }
        return value.get().getOrThrow()
    }
}
