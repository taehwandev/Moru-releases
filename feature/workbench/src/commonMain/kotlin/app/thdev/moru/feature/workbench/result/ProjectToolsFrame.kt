package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.feature.workbench.i18n.resultStrings

/**
 * A Project view without a running Task (Work with no Task, the composer, or Code and Git before a Task starts), still
 * with the whole tool strip on the right edge, so the strip looks the same on every tab. A Task tool says it needs a Task. The open tool stays open while [content]
 * changes between tabs.
 */
@Composable
fun ProjectToolsFrame(
    projectKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var openTool by remember(projectKey) { mutableStateOf<ResultTab?>(null) }
    Row(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        openTool?.let { tool ->
            Box(Modifier.width(ToolPanelWidth).fillMaxHeight().ckSurface(MoruSurfaceRole.Panel)) {
                EmptyResultNote(resultStrings().needsTask, icon = tool.icon())
            }
        }
        ResultToolStrip(
            selected = openTool ?: ResultTab.Code,
            open = openTool != null,
            available = emptySet(),
            onPick = { tool -> openTool = if (openTool == tool) null else tool },
        )
    }
}

private val ToolPanelWidth = 420.dp
