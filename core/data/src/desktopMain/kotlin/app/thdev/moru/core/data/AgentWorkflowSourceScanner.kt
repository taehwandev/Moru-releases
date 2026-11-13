package app.thdev.moru.core.data

import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import java.io.File

/**
 * Finds the agent workflows this user actually has. Nothing is listed as present
 * unless its folder exists; Moru Core is looked up from settings, then MORU_HOME, then its install location ~/.moru/core, then the usual clone location
 * (~/git/moru-core), so a Moru Core kept elsewhere is still found.
 */
class AgentWorkflowSourceScanner(
    private val home: File = File(System.getProperty("user.home")),
    private val env: (String) -> String? = System::getenv,
) {
    fun scan(settings: AgentWorkflowSettings, projectRoot: String?): List<AgentWorkflowSource> = buildList {
        add(moru(settings.moruRoot))
        projectRoot?.takeIf { it.isNotBlank() }?.let { add(source(AgentWorkflowKind.ProjectRules, File(it), ".agent/skills", requireMarker = true)) }
        add(source(AgentWorkflowKind.ClaudeCode, home.resolve(".claude"), "skills", requireMarker = true))
        add(source(AgentWorkflowKind.Codex, home.resolve(".codex"), "skills", requireMarker = true))
        add(source(AgentWorkflowKind.AgentSkills, home.resolve(".agents"), "skills"))
        add(source(AgentWorkflowKind.Hermes, home.resolve(".hermes"), "skills"))
        add(source(AgentWorkflowKind.KeyFlowVault, home.resolve("Documents/KeyFlowVault"), "skills"))
        settings.customRoots.filter { it.isNotBlank() }.distinct().forEach { root ->
            add(source(AgentWorkflowKind.Custom, expand(root), "skills").copy(configuredRoot = root))
        }
    }

    private fun moru(configured: String?): AgentWorkflowSource {
        val configuredRoot = configured?.trim()?.takeIf { it.isNotEmpty() }?.let(::expand)
        // A folder the user picked wins even when wrong, so the screen shows why Moru is missing.
        val root = configuredRoot
            ?: listOfNotNull(
                env("MORU_HOME")?.takeIf { it.isNotBlank() }?.let(::expand),
                home.resolve(".moru/core"),
                home.resolve("git/moru-core"),
            ).firstOrNull(::isMoruRoot)
            ?: return AgentWorkflowSource(AgentWorkflowKind.MoruCore, home.resolve("git/moru-core").path, found = false)
        if (!isMoruRoot(root)) return AgentWorkflowSource(AgentWorkflowKind.MoruCore, root.path, found = false, configuredRoot = configured)
        return AgentWorkflowSource(
            AgentWorkflowKind.MoruCore, root.path, found = true,
            skillCount = countSkills(root),
            instructionFiles = instructionFiles(root),
            configuredRoot = configured?.takeIf { configuredRoot != null },
        )
    }

    private fun isMoruRoot(root: File): Boolean =
        root.resolve("scripts/agent-hook.py").isFile || root.resolve("common/skills").isDirectory

    /** [requireMarker] keeps a bare folder (like ~/.claude holding only caches) from counting as a workflow. */
    private fun source(kind: AgentWorkflowKind, root: File, skillsDir: String, requireMarker: Boolean = false): AgentWorkflowSource {
        val skills = countSkills(root.resolve(skillsDir))
        val instructions = instructionFiles(root)
        val found = root.isDirectory && (!requireMarker || skills > 0 || instructions.isNotEmpty())
        return AgentWorkflowSource(kind, root.path, found, skills, instructions)
    }

    private fun instructionFiles(root: File): List<String> =
        InstructionFileNames.filter { root.resolve(it).isFile }

    private fun countSkills(dir: File): Int {
        if (!dir.isDirectory) return 0
        return dir.walkTopDown().maxDepth(SkillSearchDepth).count { it.isFile && it.name == "SKILL.md" }
    }

    private fun expand(path: String): File {
        val trimmed = path.trim()
        return when {
            trimmed == "~" -> home
            trimmed.startsWith("~/") -> home.resolve(trimmed.removePrefix("~/"))
            else -> File(trimmed)
        }
    }

    private companion object {
        val InstructionFileNames = listOf("AGENTS.md", "CLAUDE.md", "GEMINI.md")
        const val SkillSearchDepth = 4
    }
}
