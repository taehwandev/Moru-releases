package app.thdev.moru.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource

/**
 * Moru first, then the workflows this user actually has, then what is not installed.
 * Folders are chosen with the system picker; nobody types a path here.
 */
@Composable
internal fun KnowledgeSourceList(
    sources: List<AgentWorkflowSource>,
    workflows: AgentWorkflowSettings,
    onWorkflowsChange: (AgentWorkflowSettings) -> Unit,
    onPickFolder: (title: String, startIn: String?) -> String?,
) {
    val moru = sources.firstOrNull { it.kind == AgentWorkflowKind.MoruCore }
    val others = sources.filter { it.kind != AgentWorkflowKind.MoruCore }
    val (mine, missing) = others.partition { it.found || it.kind == AgentWorkflowKind.Custom }

    KnowledgeSectionTitle("MORU CORE")
    moru?.let { source ->
        KnowledgeSourceRow(source = source.toCard()) {
            MoruButton(
                text = if (source.found) "Change…" else "Locate…",
                onClick = {
                    onPickFolder("Choose Moru Core folder", source.path)
                        ?.let { picked -> onWorkflowsChange(workflows.copy(moruRoot = picked)) }
                },
                type = MoruButtonType.SECONDARY,
            )
            if (source.configuredRoot != null) {
                MoruButton(
                    text = "Auto-detect",
                    onClick = { onWorkflowsChange(workflows.copy(moruRoot = null)) },
                    type = MoruButtonType.GHOST,
                )
            }
        }
    }

    KnowledgeSectionTitle("OTHER SOURCES")
    mine.forEach { source ->
        val configured = source.configuredRoot
        KnowledgeSourceRow(source = source.toCard()) {
            if (configured != null) {
                MoruIconButton(
                    onClick = { onWorkflowsChange(workflows.copy(customRoots = workflows.customRoots - configured)) },
                    description = "Remove ${source.path}",
                    size = 24.dp,
                ) {
                    MoruIcon(MoruIconName.Close)
                }
            }
        }
    }
    MoruButton(
        text = "Add folder…",
        onClick = {
            onPickFolder("Add workflow folder", null)
                ?.takeIf { picked -> picked !in workflows.customRoots }
                ?.let { picked -> onWorkflowsChange(workflows.copy(customRoots = workflows.customRoots + picked)) }
        },
        modifier = Modifier.padding(top = 4.dp),
        type = MoruButtonType.SECONDARY,
    )

    if (missing.isNotEmpty()) {
        KnowledgeSectionTitle("UNAVAILABLE SOURCES")
        missing.forEach { source -> KnowledgeSourceRow(source = source.toCard()) }
    }
}
