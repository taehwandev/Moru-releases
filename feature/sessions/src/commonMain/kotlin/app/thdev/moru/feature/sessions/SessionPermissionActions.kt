package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.permissionSummary
import app.thdev.moru.core.terminal.permissionBrief
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruButtonSize

/** Shared live permission controls; history callers must supply the identity captured at receipt. */
@Composable
fun SessionPermissionActions(
    sessionId: String,
    expectedRequestId: String? = null,
    modifier: Modifier = Modifier,
    showQuestion: Boolean = true,
    onResponded: () -> Unit = {},
) {
    val requests by SessionPermissions.requests.collectAsState()
    val request = requests[sessionId]?.takeIf { expectedRequestId == null || it.id == expectedRequestId } ?: return
    var failed by remember(request.id) { mutableStateOf(false) }
    var detailsOpen by remember(request.id) { mutableStateOf(false) }
    val summary = remember(request) { permissionSummary(request.question, request.details) }
    Column(modifier.fillMaxWidth().testTag("permission-request-$sessionId"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showQuestion) MoruText(request.question, style = MoruTheme.typography.caption)
        if (request.details.isNotBlank()) {
            MoruText(if (summary.target != null) permissionBrief(summary, request.details) else request.details.take(160),
                style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
            MoruButton(if (detailsOpen) "Hide details" else "Details", onClick = { detailsOpen = !detailsOpen },
                type = MoruButtonType.GHOST, size = MoruButtonSize.Small)
            if (detailsOpen) MoruText(request.details, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            request.choices.forEach { choice ->
                SessionPermissionChoiceButton(
                    text = choice.label,
                    onClick = {
                        if (SessionPermissions.respond(sessionId, request.id, choice.id)) { failed = false; onResponded() }
                        else failed = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("permission-choice-$sessionId-${choice.id}"),
                )
            }
        }
        if (failed) MoruText("Could not send this response. Check the current request in the terminal and try again.",
            style = MoruTheme.typography.caption, color = MoruTheme.colors.error)
    }
}
