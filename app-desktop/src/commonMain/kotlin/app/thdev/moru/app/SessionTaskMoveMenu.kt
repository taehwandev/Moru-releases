package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem

/** Supplied by the Workbench host; other row consumers have no move action. */
internal val LocalSessionTaskMove = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
internal fun SessionTaskMoveMenu(sessionId: String, title: String, onMove: () -> Unit) {
    var expanded by remember(sessionId) { mutableStateOf(false) }
    Box {
        MoruIconButton(
            onClick = { expanded = true },
            description = "Session actions: $title",
            modifier = Modifier.testTag("session-actions:$sessionId"),
            size = 20.dp,
        ) { MoruIcon(MoruIconName.Ellipsis, size = 12.dp) }
        MoruDropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            items = listOf(MoruMenuItem("Move to Task…", onClick = onMove)),
        )
    }
}
