package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.workbench.ProjectCodeScreen
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.git.WorkbenchGitChange
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test

/**
 * Pins the Code tab before any file is open: the tree with one folder open, an ignored build folder and file dimmed,
 * changed files in their git colors, and the prompt to pick a file.
 */
class ProjectCodeScreenshotTest {
    private val comparator = GoldenImageComparator()

    private val entries = listOf(
        WorkbenchProjectTreeEntry(path = "build", name = "build", depth = 0, isFile = false, isIgnored = true, isDeferred = true),
        WorkbenchProjectTreeEntry(path = "desktop-kmp", name = "desktop-kmp", depth = 0, isFile = false),
        WorkbenchProjectTreeEntry(path = "desktop-kmp/app-desktop", name = "app-desktop", depth = 1, isFile = false),
        WorkbenchProjectTreeEntry(path = "desktop-kmp/app-desktop/Main.kt", name = "Main.kt", depth = 2, isFile = true),
        WorkbenchProjectTreeEntry(path = "desktop-kmp/build.gradle.kts", name = "build.gradle.kts", depth = 1, isFile = true),
        WorkbenchProjectTreeEntry(path = "desktop-kmp/Notes.md", name = "Notes.md", depth = 1, isFile = true),
        WorkbenchProjectTreeEntry(path = "local.properties", name = "local.properties", depth = 0, isFile = true, isIgnored = true),
        WorkbenchProjectTreeEntry(path = "README.md", name = "README.md", depth = 0, isFile = true),
    )

    private val gitStatus = WorkbenchGitStatus(
        repositoryDetected = true,
        branchName = "main",
        changes = listOf(
            WorkbenchGitChange("desktop-kmp/build.gradle.kts", indexStatus = null, worktreeStatus = 'M'),
            WorkbenchGitChange("desktop-kmp/Notes.md", indexStatus = '?', worktreeStatus = '?'),
        ),
    )

    @Test
    fun codeTabInLightTheme() = assertScreen("project_code_light", lightCkColors)

    @Test
    fun codeTabInDarkTheme() = assertScreen("project_code_dark", darkCkColors)

    @Test
    fun codeTabInKorean() = assertScreen("project_code_ko_light", lightCkColors, UiLanguage.Korean)

    private fun assertScreen(name: String, colors: MoruColors, language: UiLanguage = UiLanguage.English) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = 960, windowHeight = 560, language = language) {
            ProjectCodeScreen(
                workspacePath = "/work/Moru",
                entries = entries,
                gitStatus = gitStatus,
                onRefreshGitStatus = {},
                onOpenProjectFile = { path -> WorkbenchFilePreview.unavailable(path, "not wired") },
                onSaveProjectFile = { path, _ -> WorkbenchFileSaveOutcome.Failed(path, "not wired") },
                onCreateProjectFile = { path -> WorkbenchFileCreateOutcome.Failed(path, "not wired") },
                onRenameProjectFile = { path, _ -> WorkbenchFileRenameOutcome.Failed(path, "not wired") },
                onDeleteProjectFile = { path -> WorkbenchFileDeleteOutcome.Failed(path, "not wired") },
                onRefreshProjectTree = {},
                expandedFolders = setOf("desktop-kmp"),
                modifier = Modifier.width(920.dp).height(520.dp),
            )
        }
}
