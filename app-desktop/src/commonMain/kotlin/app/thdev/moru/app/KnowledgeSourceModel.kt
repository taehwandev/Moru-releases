package app.thdev.moru.app

import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSource

internal data class KnowledgeSourceCard(
    val title: String,
    val symbol: String,
    val pathHint: String,
    val status: String,
    val badgeType: MoruBadgeType,
    val description: String,
    val capabilities: List<String>,
    val found: Boolean = true,
)

internal fun AgentWorkflowSource.toCard(): KnowledgeSourceCard {
    val (title, symbol) = when (kind) {
        AgentWorkflowKind.MoruCore -> "Moru Core" to "AP"
        AgentWorkflowKind.ProjectRules -> "Project rules" to "PR"
        AgentWorkflowKind.ClaudeCode -> "Claude Code" to "CC"
        AgentWorkflowKind.Codex -> "Codex" to "CX"
        AgentWorkflowKind.AgentSkills -> "Agent Skills" to "SK"
        AgentWorkflowKind.Hermes -> "Hermes Agent" to "HA"
        AgentWorkflowKind.KeyFlowVault -> "KeyFlow Vault" to "KF"
        AgentWorkflowKind.Custom -> "Custom root" to "CR"
    }
    val status = when {
        !found -> "not found"
        skillCount > 0 -> "$skillCount skills"
        else -> "found"
    }
    return KnowledgeSourceCard(
        title = title,
        symbol = symbol,
        pathHint = path,
        status = status,
        badgeType = if (found) MoruBadgeType.SUCCESS else MoruBadgeType.NEUTRAL,
        description = "",
        capabilities = instructionFiles,
        found = found,
    )
}
