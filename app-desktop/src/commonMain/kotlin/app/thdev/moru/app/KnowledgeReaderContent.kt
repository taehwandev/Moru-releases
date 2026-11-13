package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex

@Composable
internal fun KnowledgeReaderContent(
    sourceState: KnowledgeSourcesState,
    selected: AgentWorkflowSource?,
    index: KnowledgeDocumentIndex?,
    document: WorkbenchFilePreview?,
    workflows: AgentWorkflowSettings,
    onSelectSource: (AgentWorkflowSource) -> Unit,
    onSelectDocument: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onRefresh: () -> Unit,
    onWorkflowsChange: (AgentWorkflowSettings) -> Unit,
    onPickFolder: (String, String?) -> String?,
) {
    Row(Modifier.fillMaxSize().background(MoruTheme.colors.background)) {
        KnowledgeBrowserSidebar(sourceState, selected, index, document?.path, workflows,
            onSelectSource, onSelectDocument, onRefresh, onWorkflowsChange, onPickFolder,
            Modifier.width(SourceExplorerWidth).fillMaxHeight())
        KnowledgeDocumentPane(selected, index, document, sourceState, onOpenLink, Modifier.weight(1f).fillMaxHeight())
    }
}

/** Match the Code explorer's pane width. */
private val SourceExplorerWidth = 280.dp
