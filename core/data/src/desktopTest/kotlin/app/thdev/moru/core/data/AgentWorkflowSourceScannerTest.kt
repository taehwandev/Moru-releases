package app.thdev.moru.core.data

import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSettings
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentWorkflowSourceScannerTest {
    private val home = Files.createTempDirectory("moru-home").toFile()

    private fun moruAt(root: File): File = root.apply { resolve("scripts").mkdirs(); resolve("scripts/agent-hook.py").writeText("") }

    private fun scan(settings: AgentWorkflowSettings = AgentWorkflowSettings(), env: Map<String, String> = emptyMap()) =
        AgentWorkflowSourceScanner(home, env::get).scan(settings, projectRoot = null)

    private fun moru(settings: AgentWorkflowSettings = AgentWorkflowSettings(), env: Map<String, String> = emptyMap()) =
        scan(settings, env).single { it.kind == AgentWorkflowKind.MoruCore }

    @Test
    fun moruIsMissingWhenNothingIsInstalled() {
        assertFalse(moru().found)
    }

    @Test
    fun moruIsFoundInTheUsualCloneLocation() {
        val root = moruAt(home.resolve("git/moru-core"))
        assertEquals(root.path, moru().path)
        assertTrue(moru().found)
    }

    @Test
    fun moruHomeWinsOverTheUsualCloneLocation() {
        moruAt(home.resolve("git/moru-core"))
        val elsewhere = moruAt(home.resolve("work/moru"))
        assertEquals(elsewhere.path, moru(env = mapOf("MORU_HOME" to elsewhere.path)).path)
    }

    @Test
    fun theMoruCoreInstallLocationWinsOverAClone() {
        moruAt(home.resolve("git/moru-core"))
        val installed = moruAt(home.resolve(".moru/core"))
        assertEquals(installed.path, moru().path)
    }

    @Test
    fun aConfiguredMoruRootWinsAndExpandsHome() {
        moruAt(home.resolve("git/moru-core"))
        val mine = moruAt(home.resolve("tools/moru"))
        val source = moru(AgentWorkflowSettings(moruRoot = "~/tools/moru"), env = mapOf("MORU_HOME" to "/nowhere"))
        assertEquals(mine.path, source.path)
        assertTrue(source.found)
    }

    @Test
    fun aWrongConfiguredMoruRootIsReportedMissingInsteadOfFallingBack() {
        moruAt(home.resolve("git/moru-core"))
        val source = moru(AgentWorkflowSettings(moruRoot = "~/typo"))
        assertEquals(home.resolve("typo").path, source.path)
        assertFalse(source.found)
    }

    @Test
    fun theUsersOwnWorkflowsAreFoundWithTheirSkills() {
        home.resolve(".claude/skills/review").mkdirs()
        home.resolve(".claude/skills/review/SKILL.md").writeText("")
        home.resolve(".claude/CLAUDE.md").writeText("")
        val custom = home.resolve("flows").apply { resolve("skills/a").mkdirs(); resolve("skills/a/SKILL.md").writeText("") }

        val sources = scan(AgentWorkflowSettings(customRoots = listOf(custom.path)))

        val claude = sources.single { it.kind == AgentWorkflowKind.ClaudeCode }
        assertTrue(claude.found)
        assertEquals(1, claude.skillCount)
        assertEquals(listOf("CLAUDE.md"), claude.instructionFiles)
        val mine = sources.single { it.kind == AgentWorkflowKind.Custom }
        assertTrue(mine.found)
        assertEquals(1, mine.skillCount)
    }

    @Test
    fun aRuntimeFolderWithoutRulesOrSkillsIsNotAWorkflow() {
        home.resolve(".codex/sessions").mkdirs()
        assertFalse(scan().single { it.kind == AgentWorkflowKind.Codex }.found)
    }
}
