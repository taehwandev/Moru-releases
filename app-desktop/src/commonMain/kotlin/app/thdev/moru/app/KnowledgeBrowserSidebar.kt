package app.thdev.moru.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex
import app.thdev.moru.feature.workbench.knowledge.ReadOnlySourceTree

@Composable
internal fun KnowledgeBrowserSidebar(
    sources: KnowledgeSourcesState,
    selected: AgentWorkflowSource?,
    index: KnowledgeDocumentIndex?,
    documentPath: String?,
    workflows: AgentWorkflowSettings,
    onSelectSource: (AgentWorkflowSource) -> Unit,
    onSelectDocument: (String) -> Unit,
    onRefresh: () -> Unit,
    onWorkflowsChange: (AgentWorkflowSettings) -> Unit,
    onPickFolder: (String, String?) -> String?,
    modifier: Modifier = Modifier,
) {
    var query by remember(selected?.path) { mutableStateOf("") }
    var managing by remember { mutableStateOf(false) }
    Column(modifier.ckSurface(MoruSurfaceRole.Panel)) {
        Column(Modifier.fillMaxWidth().padding(MoruTheme.spacing.small), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            Row {
                MoruText("AGENT OS", Modifier.weight(1f), style = MoruTheme.typography.caption)
                MoruIconButton(onRefresh, description = "Refresh sources and files") { MoruIcon(MoruIconName.Refresh) }
            }
            KnowledgeSourcePicker(sources.sources, selected) { onSelectSource(it); managing = false }
            selected?.let { source -> SelectionContainer {
                MoruText(source.path, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } }
        }
        MoruDivider()
        if (sources.loading) MoruText("Finding sources…", Modifier.padding(MoruTheme.spacing.small))
        sources.error?.let { MoruText(it, Modifier.padding(MoruTheme.spacing.small), color = MoruTheme.colors.error) }
        if (managing) {
            MoruButton("Back to files", { managing = false }, type = MoruButtonType.GHOST)
            Column(Modifier.weight(1f).padding(MoruTheme.spacing.small).verticalScroll(rememberScrollState())) {
                KnowledgeSourceList(sources.sources, workflows, onWorkflowsChange, onPickFolder)
            }
        } else {
            MoruTextField(query, { query = it }, Modifier.fillMaxWidth().padding(MoruTheme.spacing.small), placeholder = "Find file…", label = "Search documents")
            if (index?.truncated == true) MoruText("Showing a bounded file index.", Modifier.padding(MoruTheme.spacing.small), style = MoruTheme.typography.caption)
            when {
                selected == null -> MoruText("Choose an Agent OS source.", Modifier.padding(MoruTheme.spacing.small))
                !selected.found -> MoruText("Source folder is unavailable.", Modifier.padding(MoruTheme.spacing.small))
                index == null -> MoruText("Finding files…", Modifier.padding(MoruTheme.spacing.small))
                index.error != null -> MoruText(index.error.orEmpty(), Modifier.padding(MoruTheme.spacing.small), color = MoruTheme.colors.error)
                index.paths.isEmpty() -> MoruText("No Markdown files found.", Modifier.padding(MoruTheme.spacing.small))
            }
            ReadOnlySourceTree(selected?.path.orEmpty(), index?.paths.orEmpty(), documentPath, query, onSelectDocument, Modifier.weight(1f))
            MoruDivider()
            MoruButton("Manage sources…", { managing = true }, Modifier.fillMaxWidth().padding(MoruTheme.spacing.xSmall), type = MoruButtonType.GHOST)
        }
    }
}
