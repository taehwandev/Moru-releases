package app.thdev.moru.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex
import app.thdev.moru.core.ui.markdown.MarkdownReadingView

@Composable
internal fun KnowledgeDocumentPane(
    source: AgentWorkflowSource?,
    index: KnowledgeDocumentIndex?,
    document: WorkbenchFilePreview?,
    sources: KnowledgeSourcesState,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var raw by remember(source?.path, document?.path) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
        document?.let {
            Row(Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Row).padding(MoruTheme.spacing.small)) {
                Column(Modifier.weight(1f)) {
                    MoruText(it.name, style = MoruTheme.typography.body)
                    if ('/' in it.path) MoruText(it.path, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
                }
                MoruText("Read-only", Modifier.padding(horizontal = MoruTheme.spacing.small), style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
                if (it.content != null) MoruButton(if (raw) "Read" else "Source", { raw = !raw }, type = MoruButtonType.GHOST)
            }
            if (it.truncated) MoruText(it.truncationText ?: "Preview is truncated.", color = MoruTheme.colors.warning)
        }
        val message = when {
            sources.loading -> "Finding local sources…"
            sources.error != null -> sources.error
            source == null -> "Add a folder in Manage sources to read your agent documents."
            !source.found -> "This source is unavailable. Locate its folder in Manage sources."
            index == null -> "Finding documents…"
            index.error != null -> index.error
            index.paths.isEmpty() -> "No Markdown documents found in this source."
            document == null -> "Loading document…"
            document.content == null -> document.summaryText
            else -> null
        }
        if (message != null) MoruText(message, Modifier.padding(MoruTheme.spacing.medium), color = MoruTheme.colors.textSecondary)
        else androidx.compose.runtime.key(source?.path, document?.path) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (raw) SelectionContainer { MoruText(document?.content.orEmpty(), Modifier.padding(MoruTheme.spacing.medium), style = MoruTheme.typography.monospace) }
                else MarkdownReadingView(document?.content.orEmpty(), onOpenLink, Modifier.fillMaxWidth())
            }
        }
    }
}
