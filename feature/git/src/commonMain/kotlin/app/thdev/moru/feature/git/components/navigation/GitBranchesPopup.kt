package app.thdev.moru.feature.git.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.git.GitManagementEvent
import app.thdev.moru.feature.git.GitManagementUiState
import app.thdev.moru.feature.git.GitRefsSidebar

/** Keyboard-dismissable branch browser for windows that cannot fit a permanent refs column. */
@Composable
internal fun GitBranchesPopup(
    state: GitManagementUiState, height: Dp, onEvent: (GitManagementEvent) -> Unit, onDismiss: () -> Unit,
) {
    Popup(alignment = Alignment.TopStart, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)) {
        Column(Modifier.width(280.dp).height(height).testTag("git-branches-popup")
            .ckSurface(MoruSurfaceRole.Panel).padding(MoruTheme.spacing.small)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                MoruText("Branches", style = MoruTheme.typography.body)
                MoruButton("Done", onDismiss, type = MoruButtonType.GHOST)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GitRefsSidebar(state, { event ->
                    if (event is GitManagementEvent.Propose) onDismiss()
                    onEvent(event)
                }, Modifier.fillMaxSize())
            }
        }
    }
}
