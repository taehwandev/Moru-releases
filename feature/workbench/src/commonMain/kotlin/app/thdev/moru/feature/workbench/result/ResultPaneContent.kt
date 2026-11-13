package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.i18n.resultStrings
import app.thdev.moru.feature.workbench.slots.CodePanelSpec
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots

/** The selected Result tab, keeping the Task's identity while the pane changes width. */
@Composable
internal fun ResultPaneContent(
    selected: ResultTab,
    taskKey: String,
    workspacePath: String,
    sessions: TerminalSessionManager,
    slots: WorkbenchPaneSlots,
) {
    Box(Modifier.fillMaxSize()) {
        when (selected) {
            ResultTab.Code -> {
                val code = slots.code
                if (code == null) {
                    EmptyResultNote(resultStrings().codeNotRepository, icon = ResultTab.Code.icon())
                } else key(taskKey) {
                    code.pane(CodePanelSpec(taskKey, workspacePath, sessions), Modifier)
                }
            }
        }
    }
}
