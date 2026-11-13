package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.feature.notifications.NotificationInboxContent
import app.thdev.moru.feature.notifications.NotificationInboxUiState
import java.awt.Frame
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NotificationInboxChromeWindowOsTest {
    @Test
    fun settingsIsReachableWithoutTheInboxAndClosesAnOpenInbox() {
        var destination by mutableStateOf(MoruDestination.Workbench)
        NativeWorkbenchTestWindow(520, 440) {
            MoruTheme {
                MoruFrame(destination, onSelect = { destination = it }, topInset = 28.dp,
                    inbox = { Inbox(1) }) { MoruText("Destination: $destination") }
            }
        }.use { ui ->
            ui.waitFor("persistent Settings menu") { ui.exists("Settings menu") }
            assertFalse(ui.exists("Mark all read"))
            ui.click("Settings menu")
            ui.waitFor("app menu") { ui.exists("Settings…, ⌘,") }
            assertEquals(MoruDestination.Workbench, destination)
            ui.click("Settings…, ⌘,")
            ui.waitFor("Settings selected") { ui.exists("Destination: Settings") }
            ui.click("Notifications")
            ui.waitFor("inbox over Settings") { ui.exists("Mark all read") }
            onEdt {
                val popup = frame().ownedWindows.single { it.isShowing }
                assertFalse(names(popup.accessibleContext).contains("Settings…, ⌘,"), "Settings must be outside the inbox")
            }
            ui.click("Settings menu")
            ui.waitFor("Settings menu closes the inbox") { !ui.exists("Mark all read") && ui.exists("Settings…, ⌘,") }
            ui.click("Settings…, ⌘,")
            assertEquals(MoruDestination.Settings, destination)
        }
    }

    @androidx.compose.runtime.Composable
    private fun Inbox(count: Int) {
        val rows = List(count) { index ->
            TaskNotification("notice-$index", "project", "task-$index", "Project", "Notification $index", "Task finished.", 1)
        }
        NotificationInboxContent(NotificationInboxUiState(loading = false, notifications = rows), {}, {}, {}, {}, {})
    }

    private fun frame(): Frame = Window.getWindows().filterIsInstance<Frame>()
        .single { it.title == "Task session test" && it.isShowing }

    private fun names(node: AccessibleContext): List<String> = listOfNotNull(node.accessibleName, node.accessibleDescription) +
        (0 until node.accessibleChildrenCount).flatMap { index -> node.getAccessibleChild(index)?.accessibleContext?.let(::names).orEmpty() }

    private fun <T> onEdt(action: () -> T): T {
        val value = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { value.set(runCatching(action)) }
        return value.get().getOrThrow()
    }
}
