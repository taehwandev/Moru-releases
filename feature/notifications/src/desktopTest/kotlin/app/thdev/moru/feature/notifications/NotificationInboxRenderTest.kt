package app.thdev.moru.feature.notifications

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** The oracle is reachable controls and growing text at a narrow width, with captured light/dark surfaces. */
@OptIn(ExperimentalTestApi::class)
class NotificationInboxRenderTest {
    @Test
    fun shortHistoryUsesItsContentHeightAndHasNoSettingsInside() = runDesktopComposeUiTest(width = 380, height = 700) {
        setContent { MoruTheme { NotificationInboxContent(NotificationInboxFixtures.populated, {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-quick-settings").assertDoesNotExist()
        onNodeWithTag("notification-delete-old").assertIsDisplayed()
        val inbox = onNodeWithTag("notification-inbox").fetchSemanticsNode().boundsInRoot
        assertTrue(inbox.height < 320f, "Short history must not leave a large blank area: $inbox")
    }

    @Test
    fun headerActionsFitAnEvenNarrowerHistoryWithAVersion() = runDesktopComposeUiTest(width = 260, height = 440) {
        setContent {
            MoruTheme {
                NotificationInboxContent(NotificationInboxFixtures.populated, {}, {}, {}, {}, {}, appVersion = "26.41.1")
            }
        }
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("notification-mark-all-read", "notification-clear-all", "notification-unread-count")) {
            onNodeWithTag(tag).assertIsDisplayed()
            val bounds = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= root.left && bounds.right <= root.right, "$tag must fit: $bounds")
        }
        captureInbox("notification-inbox-compact-260")
    }

    @Test
    fun lightHistoryFitsTheTitleBarPopover() = renderPopover(dark = false)

    @Test
    fun darkHistoryFitsTheTitleBarPopover() = renderPopover(dark = true)

    @Test
    fun narrowLightHistoryKeepsLongSourceNamesAndMessageInsideTheSurface() = renderNarrow(dark = false)

    @Test
    fun narrowDarkHistoryKeepsLongSourceNamesAndMessageInsideTheSurface() = renderNarrow(dark = true)

    @Test
    fun timestampUsesTheSelectedAppLanguage() = runDesktopComposeUiTest(width = 420, height = 800) {
        setContent {
            MoruTheme {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Korean) {
                    NotificationInboxContent(NotificationInboxFixtures.populated.copy(notifications = listOf(NotificationInboxFixtures.newest)), {}, {}, {}, {}, {})
                }
            }
        }
        onNodeWithTag("notification-time-new").assertTextEquals(
            notificationTimestampLabel(NotificationInboxFixtures.newest.createdAtMillis, UiLanguage.Korean),
        )
        captureInbox("notification-inbox-korean-date")
    }

    private fun renderPopover(dark: Boolean) = runDesktopComposeUiTest(width = 360, height = 440) {
        setContent { MoruTheme(darkTheme = dark) { NotificationInboxContent(NotificationInboxFixtures.populated, {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-mark-all-read").assertIsDisplayed()
        onNodeWithTag("notification-clear-all").assertIsDisplayed()
        onNodeWithTag("notification-message-new").assertIsDisplayed()
        onNodeWithTag("notification-delete-old").assertIsDisplayed()
        captureInbox("notification-inbox-glass-${if (dark) "dark" else "light"}")
    }

    private fun renderNarrow(dark: Boolean) = runDesktopComposeUiTest(width = 320, height = 1100) {
        val state = NotificationInboxFixtures.populated.copy(
            notifications = listOf(NotificationInboxFixtures.longNames, NotificationInboxFixtures.older),
            unavailableTaskIds = setOf(NotificationInboxFixtures.older.taskId),
        )
        setContent { MoruTheme(darkTheme = dark) { NotificationInboxContent(state, {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-message-new").assertTextEquals(NotificationInboxFixtures.longNames.message)
        onNodeWithTag("notification-delete-old").performScrollTo().assertIsDisplayed()
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("notification-delete-old", "notification-open-old", "notification-message-old")) {
            val bounds = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= root.left && bounds.right <= root.right, "$tag must fit the narrow surface: $bounds")
        }
        onNodeWithTag("notification-clear-all").performScrollTo().assertIsDisplayed()
        onNodeWithTag("notification-mark-all-read").assertIsDisplayed()
        onNodeWithTag("notification-message-new").assertIsDisplayed()
        captureInbox("notification-inbox-narrow-${if (dark) "dark" else "light"}")
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.captureInbox(name: String) {
    waitForIdle()
    val target = File("build/ui-captures/$name.png")
    target.parentFile.mkdirs()
    ImageIO.write(onNodeWithTag("notification-inbox").captureToImage().toAwtImage(), "png", target)
}
