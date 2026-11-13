package app.thdev.moru.feature.workbench.statusbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole

/**
 * The bottom line of the workbench: how many Tasks wait for the user and run, and the Project's folder and branch.
 * Stateless: the host supplies everything it shows.
 */
@Composable
fun WorkbenchStatusBar(
    waiting: Int,
    working: Int,
    modifier: Modifier = Modifier,
    folder: String? = null,
    branch: String? = null,
) {
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .ckSurface(MoruSurfaceRole.Panel)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBarTaskCounts(waiting = waiting, working = working)
            if (folder != null) StatusBarProjectContext(folder, branch, Modifier.weight(1f))
            else androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}
