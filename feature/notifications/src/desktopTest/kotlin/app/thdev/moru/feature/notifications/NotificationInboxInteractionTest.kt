package app.thdev.moru.feature.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NotificationInboxInteractionTest {
    @Test
    fun originalMessageSourcesAndSeparateHistoryActionsRemainReachable() = runDesktopComposeUiTest(width = 800, height = 1000) {
        val events = mutableListOf<String>()
        setContent {
            MoruTheme {
                NotificationInboxContent(
                    NotificationInboxFixtures.populated,
                    onOpen = { events += "open:${it.id}" },
                    onDelete = { events += "delete:$it" },
                    onClear = { events += "clear" },
                    onMarkAllRead = { events += "read-all" },
                    onRetry = { events += "retry" },
                )
            }
        }
        onNodeWithText("Alpha").assertIsDisplayed()
        onNodeWithText("Fix sign in").assertIsDisplayed()
        onNodeWithTag("notification-message-new").assertTextEquals(NotificationInboxFixtures.newest.message)
        onNodeWithTag("notification-unread-count").assertTextEquals("1 unread")
        onNodeWithTag("notification-unread-new").assertExists()
        onNodeWithTag("notification-unread-old").assertDoesNotExist()
        val newest = onNodeWithTag("notification-row-new").fetchSemanticsNode().boundsInRoot
        val older = onNodeWithTag("notification-row-old").fetchSemanticsNode().boundsInRoot
        assertTrue(newest.top < older.top, "The repository's newest-first order must be retained")
        onNodeWithTag("notification-time-new").assertTextEquals(
            notificationTimestampLabel(NotificationInboxFixtures.newest.createdAtMillis, UiLanguage.English),
        )
        onNodeWithTag("notification-open-new")
            .assertContentDescriptionEquals("Open task: Fix sign in in Alpha").performClick()
        onNodeWithTag("notification-delete-old").performScrollTo().performClick()
        onNodeWithTag("notification-mark-all-read").performScrollTo().performClick()
        onNodeWithTag("notification-clear-all").performClick()
        runOnIdle { assertEquals(listOf("open:new", "delete:old", "read-all", "clear"), events) }
    }

    @Test
    fun unavailableTaskStillAllowsDeletionOfItsHistoryRecord() = runDesktopComposeUiTest(width = 420, height = 800) {
        val deleted = mutableListOf<String>()
        var opens = 0
        val state = NotificationInboxFixtures.populated.copy(
            notifications = listOf(NotificationInboxFixtures.newest),
            unavailableTaskIds = setOf(NotificationInboxFixtures.newest.taskId),
        )
        setContent {
            MoruTheme {
                NotificationInboxContent(state, { opens++ }, { deleted += it }, {}, {}, {})
            }
        }
        onNodeWithTag("notification-unavailable-new").assertTextEquals("Task unavailable")
        onNodeWithTag("notification-open-new").assertIsNotEnabled()
        onNodeWithTag("notification-delete-new").assertIsEnabled().performClick()
        runOnIdle { assertEquals(0, opens); assertEquals(listOf("new"), deleted) }
    }

    @Test
    fun emptyHistoryExplainsWhereNewNotificationsWillAppear() = runDesktopComposeUiTest(width = 320, height = 700) {
        setContent { MoruTheme { NotificationInboxContent(NotificationInboxUiState(loading = false), {}, {}, {}, {}, {}) } }
        onNodeWithText("No notifications yet").assertIsDisplayed()
        onNodeWithText("New task updates will appear here.").assertIsDisplayed()
        onNodeWithTag("notification-unread-count").assertTextEquals("0 unread")
        onNodeWithTag("notification-clear-all").assertIsNotEnabled()
        onNodeWithTag("notification-mark-all-read").assertIsNotEnabled()
        onNodeWithTag("notification-loading").assertDoesNotExist()
    }

    @Test
    fun loadingDoesNotPresentAnEmptyHistoryOrEnableBulkActions() = runDesktopComposeUiTest(width = 320, height = 700) {
        setContent { MoruTheme { NotificationInboxContent(NotificationInboxUiState(), {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-loading").assertTextEquals("Loading notifications…")
        onNodeWithTag("notification-empty").assertDoesNotExist()
        onNodeWithTag("notification-clear-all").assertIsNotEnabled()
        onNodeWithTag("notification-mark-all-read").assertIsNotEnabled()
    }

    @Test
    fun failedMutationKeepsTheRecordVisibleAndAllowsRetryAndDeletion() = runDesktopComposeUiTest(width = 420, height = 900) {
        var retries = 0
        val state = NotificationInboxFixtures.populated.copy(
            notifications = listOf(NotificationInboxFixtures.newest),
            error = "Could not delete the notification. The saved history is unchanged.",
        )
        setContent { MoruTheme { NotificationInboxContent(state, {}, {}, {}, {}, { retries++ }) } }
        onNodeWithTag("notification-error-message").assertTextEquals(state.error!!)
        onNodeWithTag("notification-message-new").assertTextEquals(NotificationInboxFixtures.newest.message)
        onNodeWithTag("notification-delete-new").performScrollTo().assertIsEnabled()
        onNodeWithTag("notification-retry").performScrollTo().assertIsEnabled().performClick()
        runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun loadFailureIsExplicitAndDoesNotClaimThatHistoryIsEmpty() = runDesktopComposeUiTest(width = 320, height = 800) {
        val state = NotificationInboxUiState(loading = false, error = "Notification storage is unavailable.")
        setContent { MoruTheme { NotificationInboxContent(state, {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-error-message").assertTextEquals("Notification storage is unavailable.")
        onNodeWithTag("notification-retry").assertIsEnabled()
        onNodeWithTag("notification-empty").assertDoesNotExist()
    }

    @Test
    fun busyStateKeepsHistoryVisibleWhileDisablingConflictingActions() = runDesktopComposeUiTest(width = 420, height = 1000) {
        val state = NotificationInboxFixtures.populated.copy(
            notifications = listOf(NotificationInboxFixtures.newest), busy = true, error = "Retry is pending.",
        )
        setContent { MoruTheme { NotificationInboxContent(state, {}, {}, {}, {}, {}) } }
        onNodeWithTag("notification-busy").assertTextEquals("Updating notifications…")
        onNodeWithTag("notification-message-new").assertTextEquals(NotificationInboxFixtures.newest.message)
        for (tag in listOf("notification-open-new", "notification-delete-new", "notification-clear-all", "notification-mark-all-read", "notification-retry")) {
            onNodeWithTag(tag).performScrollTo().assertIsNotEnabled()
        }
    }

    @Test
    fun callerSnapshotsUpdateReadIndicatorsAndRemovedRows() = runDesktopComposeUiTest(width = 700, height = 1000) {
        var state by mutableStateOf(NotificationInboxFixtures.populated)
        setContent {
            MoruTheme {
                NotificationInboxContent(
                    state,
                    onOpen = { opened -> state = state.copy(notifications = state.notifications.map { if (it.id == opened.id) it.copy(read = true) else it }) },
                    onDelete = { deleted -> state = state.copy(notifications = state.notifications.filterNot { it.id == deleted }) },
                    onClear = { state = state.copy(notifications = emptyList()) },
                    onMarkAllRead = {},
                    onRetry = {},
                )
            }
        }
        onNodeWithTag("notification-open-new").performClick()
        onNodeWithTag("notification-unread-new").assertDoesNotExist()
        onNodeWithTag("notification-unread-count").assertTextEquals("0 unread")
        onNodeWithTag("notification-delete-new").performClick()
        onNodeWithTag("notification-row-new").assertDoesNotExist()
        onNodeWithTag("notification-message-old").assertTextEquals(NotificationInboxFixtures.older.message)
        onNodeWithTag("notification-clear-all").performScrollTo().performClick()
        onNodeWithText("No notifications yet").assertIsDisplayed()
    }

    @Test
    fun taskOpenAndDeleteCanBeActivatedWithTheKeyboard() = runDesktopComposeUiTest(width = 420, height = 800) {
        val events = mutableListOf<String>()
        val state = NotificationInboxFixtures.populated.copy(notifications = listOf(NotificationInboxFixtures.newest))
        setContent { MoruTheme { NotificationInboxContent(state, { events += "open" }, { events += "delete" }, {}, {}, {}) } }
        onNodeWithTag("notification-open-new").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("notification-open-new").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("notification-delete-new").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("notification-delete-new").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(listOf("open", "delete"), events) }
    }
}
