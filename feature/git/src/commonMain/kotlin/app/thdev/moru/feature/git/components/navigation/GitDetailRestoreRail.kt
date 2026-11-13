package app.thdev.moru.feature.git.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole

/** The main window keeps a reachable restore control while detail is detached. */
@Composable
internal fun GitDetailRestoreRail(onRestore: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.ckSurface(MoruSurfaceRole.Panel), horizontalAlignment = Alignment.CenterHorizontally) {
        MoruIconButton(onRestore, "Restore Git detail pane", Modifier.testTag("git-detail-restore-rail"), size = 36.dp) {
            MoruIcon(MoruIconName.Dock)
        }
    }
}
