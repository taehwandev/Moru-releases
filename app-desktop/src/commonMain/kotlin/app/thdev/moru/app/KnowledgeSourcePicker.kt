package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruSelectField
import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSource

@Composable
internal fun KnowledgeSourcePicker(sources: List<AgentWorkflowSource>, selected: AgentWorkflowSource?, onSelect: (AgentWorkflowSource) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        MoruSelectField(selected?.toCard()?.title ?: "Choose source", { expanded = true },
            Modifier.fillMaxWidth().semantics { contentDescription = "Choose Agent OS source" }, enabled = sources.isNotEmpty())
        MoruDropdownMenu(expanded, { expanded = false }, sources.map { source ->
            val title = source.toCard().title + when {
                !source.found -> " · unavailable"
                source.kind == AgentWorkflowKind.Custom -> " · ${source.path.substringAfterLast('/')}"
                else -> ""
            }
            MoruMenuItem(title, checked = source == selected, icon = MoruIconName.Folder, onClick = { onSelect(source) })
        })
    }
}
