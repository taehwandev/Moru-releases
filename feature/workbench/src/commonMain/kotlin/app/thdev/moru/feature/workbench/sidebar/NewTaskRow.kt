package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

import androidx.compose.foundation.layout.Box
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.NewSessionPopover

/**
 * The labelled row at the foot of a Project card that adds a Task to it, aligned with the Task rows above.
 */
@Composable
internal fun NewTaskRow(
    projectName: String,
    expanded: Boolean = false,
    errorMessage: String? = null,
    onStart: (AgentRuntime) -> Unit = {},
    onDismiss: () -> Unit = {},
    onClick: () -> Unit,
) {
    val strings = workbenchStrings()
    val description = strings.newTaskIn(projectName)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MoruTheme.radius.small))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(name = MoruIconName.Plus, size = 14.dp, tint = MoruTheme.colors.textSecondary)
            MoruText(text = strings.newTaskLabel, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
        }
        NewSessionPopover(
            expanded = expanded,
            errorMessage = errorMessage,
            onStart = onStart,
            onDismiss = onDismiss,
        )
    }
}
