package app.thdev.moru.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NotificationPermissionInteractionTest {
    @Test
    fun choiceSlotHasItsOwnKeyboardTargetAndDoesNotOpenOrDeleteReceipt() = runDesktopComposeUiTest(width = 320, height = 800) {
        val events = mutableListOf<String>()
        var pending by mutableStateOf(true)
        setContent {
            MoruTheme {
                NotificationInboxContent(
                    state = NotificationInboxFixtures.populated.copy(notifications = listOf(NotificationInboxFixtures.newest)),
                    onOpen = { events += "open" }, onDelete = { events += "delete" },
                    onClear = {}, onMarkAllRead = {}, onRetry = {},
                    permissionActions = { receipt ->
                        if (pending) BasicText(
                            "Allow this exact permission request",
                            Modifier.testTag("permission-${receipt.id}").clickable(role = Role.Button) {
                                events += "respond:${receipt.id}"
                                pending = false
                            },
                        )
                    },
                )
            }
        }
        onNodeWithTag("permission-new").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("permission-new").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("permission-new").assertDoesNotExist()
        runOnIdle { assertEquals(listOf("respond:new"), events) }
        onNodeWithTag("notification-open-new").performClick()
        onNodeWithTag("notification-delete-new").performClick()
        runOnIdle { assertEquals(listOf("respond:new", "open", "delete"), events) }
    }

    @Test
    fun unavailableOrBusyReceiptsNeverRenderPermissionActions() = runDesktopComposeUiTest(width = 320, height = 800) {
        var state by mutableStateOf(NotificationInboxFixtures.populated.copy(
            notifications = listOf(NotificationInboxFixtures.newest),
            unavailableTaskIds = setOf(NotificationInboxFixtures.newest.taskId),
        ))
        setContent {
            MoruTheme {
                NotificationInboxContent(state, {}, {}, {}, {}, {}, permissionActions = {
                    BasicText("Allow", Modifier.testTag("permission-choice"))
                })
            }
        }
        onNodeWithTag("permission-choice").assertDoesNotExist()
        runOnIdle { state = state.copy(unavailableTaskIds = emptySet(), busy = true) }
        onNodeWithTag("permission-choice").assertDoesNotExist()
        runOnIdle { state = state.copy(busy = false) }
        onNodeWithTag("permission-choice").assertIsDisplayed()
    }
}
