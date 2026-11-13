package app.thdev.moru.feature.workbench.restore

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.ProjectCodeScreen
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Code view opens again on the file that was open last time, and says when that changes. */
@OptIn(ExperimentalTestApi::class)
class CodeViewReopensItsFileTest {
    private fun preview(path: String) = WorkbenchFilePreview(
        path = path, name = path.substringAfterLast('/'), status = WorkbenchFilePreviewStatus.TEXT,
        sizeBytes = 5, content = "hello", summaryText = "",
    )

    @Test
    fun theLastOpenFileIsOpenAgainAndNoEmptyFrameErasesIt() = runDesktopComposeUiTest {
        val reported = mutableListOf<String?>()
        setContent {
            MoruTheme {
                ProjectCodeScreen(
                    workspacePath = "/work/a",
                    entries = emptyList(),
                    gitStatus = WorkbenchGitStatus.NotRepository,
                    onRefreshGitStatus = {},
                    onOpenProjectFile = ::preview,
                    onSaveProjectFile = { path, _ -> WorkbenchFileSaveOutcome.Failed(path, "") },
                    onCreateProjectFile = { path -> WorkbenchFileCreateOutcome.Failed(path, "") },
                    onRenameProjectFile = { path, _ -> WorkbenchFileRenameOutcome.Failed(path, "") },
                    onDeleteProjectFile = { path -> WorkbenchFileDeleteOutcome.Failed(path, "") },
                    onRefreshProjectTree = {},
                    openFilePath = "/work/a/README.md",
                    onOpenFileChange = { reported += it },
                )
            }
        }

        onAllNodesWithText("README.md", substring = true).onFirst().assertExists()
        runOnIdle { assertEquals(listOf<String?>("/work/a/README.md"), reported) }
    }

    @Test
    fun aFileThatIsGoneIsNotOpenedAndIsForgotten() = runDesktopComposeUiTest {
        val reported = mutableListOf<String?>()
        setContent {
            MoruTheme {
                ProjectCodeScreen(
                    workspacePath = "/work/a",
                    entries = emptyList(),
                    gitStatus = WorkbenchGitStatus.NotRepository,
                    onRefreshGitStatus = {},
                    onOpenProjectFile = { path -> WorkbenchFilePreview.unavailable(path, "gone") },
                    onSaveProjectFile = { path, _ -> WorkbenchFileSaveOutcome.Failed(path, "") },
                    onCreateProjectFile = { path -> WorkbenchFileCreateOutcome.Failed(path, "") },
                    onRenameProjectFile = { path, _ -> WorkbenchFileRenameOutcome.Failed(path, "") },
                    onDeleteProjectFile = { path -> WorkbenchFileDeleteOutcome.Failed(path, "") },
                    onRefreshProjectTree = {},
                    openFilePath = "/work/a/deleted.md",
                    onOpenFileChange = { reported += it },
                )
            }
        }

        onNodeWithText("deleted.md", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(setOf<String?>(null), reported.toSet()) }
    }
}
