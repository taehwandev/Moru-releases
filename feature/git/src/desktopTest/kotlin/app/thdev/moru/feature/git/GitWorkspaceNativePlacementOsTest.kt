package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitRepositoryState
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Uses the actual repository body in its own native fixture, without a shell or Git process. */
class GitWorkspaceNativePlacementOsTest {
    @Test
    fun nativePlacementRetainsCommitDraftSelectionAndCurrentCallbacksUntilOwnerTeardown() {
        val probe = GitPlacementProbe()
        NativeResultTestWindow { probe.Content() }.use { window ->
            val main = NativeResultTestWindow.MAIN_TITLE
            val changes = "project worktree · Changes"
            val diff = "project worktree · Commit · abcdef0"
            fun draftIn(title: String) = window.editableValue(title, "Commit message") == Draft
            fun detach() {
                window.click(main, "Detach Git detail pane")
                window.waitFor("detached commit draft") { window.visible(changes) && draftIn(changes) }
                assertTrue(window.exists(main, "Restore Git detail pane"))
                probe.assertNoPlacementMutation()
            }
            fun restored() {
                window.waitFor("restored commit draft") { draftIn(main) && !window.visible(changes) }
                probe.assertNoPlacementMutation()
            }

            window.waitFor("Git changes") { window.exists(main, "Detach Git detail pane") }
            window.replaceEditableText(main, "Commit message", Draft)
            window.waitFor("edited commit draft") { draftIn(main) && probe.state.commitMessage == Draft }

            detach()
            window.click(changes, "Restore Git detail pane")
            restored()
            detach()
            window.closeNative(changes)
            restored()
            detach()
            window.click(main, "Restore Git detail pane")
            restored()

            detach()
            window.resizeMain(520, 760)
            window.waitFor("compact navigation") { window.exists(main, "History") }
            window.click(main, "History")
            window.waitFor("compact History retains detached draft") { window.visible(changes) && draftIn(changes) }
            window.click(main, "Review commit, Author, just now, abcdef0")
            window.waitFor("detached detail follows selected commit") {
                window.visible(diff) && window.exists(diff, "Commit · abcdef0") && window.exists(diff, "Selected fixture diff")
            }
            window.runOnEdt { probe.refreshCurrentCallback() }
            window.waitFor("fresh selected diff") { window.exists(diff, "Selected fixture diff") }
            window.closeNative(diff)
            window.waitFor("native close keeps selected commit") {
                !window.visible(diff) && window.exists(main, "Commit · abcdef0")
            }
            window.click(main, "History")
            window.waitFor("hidden detail leaves navigation only") { !window.exists(main, "Detach Git detail pane") }
            window.click(main, "Working folder · 3 changed files")
            window.waitFor("working folder retains draft after commit review") { draftIn(main) }
            probe.assertNoPlacementMutation()

            detach()
            window.click(changes, "Stage ${GitManagementFixtures.unstaged.path}")
            window.waitFor("explicit staging uses current callback") { probe.mutations.isNotEmpty() }
            assertEquals(listOf(1 to GitManagementEvent.ChangeIndex(
                GitAction.Stage(listOf(GitManagementFixtures.unstaged.path)), "fresh-r2")), probe.mutations.toList())
            assertEquals(Draft, probe.state.commitMessage)
            window.runOnEdt { probe.ownerPresent = false }
            window.waitFor("repository owner closes detached window") { !window.visible(changes) }
            assertFalse(window.visible(diff))
        }
    }

    private companion object { const val Draft = "Keep this Git commit draft" }
}

private class GitPlacementProbe {
    var state by mutableStateOf(GitManagementUiState().afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot)))
    var ownerPresent by mutableStateOf(true)
    private var callbackVersion by mutableStateOf(0)
    val mutations = CopyOnWriteArrayList<Pair<Int, GitManagementEvent>>()
    private val commit = GitCommit("abcdef0123456789", emptyList(), "Author", "author@example.test",
        4_102_444_800L, emptyList(), "Review commit")
    private val history = GitHistorySource(
        loadHistory = { GitHistoryResult.Loaded(listOf(commit), false) },
        loadCommitDiff = { _, _ -> CommitDiffResult.Unavailable("Selected fixture diff") },
    )

    @Composable
    fun Content() {
        MoruTheme(darkTheme = false) {
            val currentVersion = callbackVersion
            if (ownerPresent) GitWorkspaceBody(state, GitManagementFixtures.Folder, history,
                onEvent = { event ->
                    if (event is GitManagementEvent.ChangeIndex || event is GitManagementEvent.Propose) {
                        mutations += currentVersion to event
                    }
                    state = state.edit(event, GitManagementFixtures.Folder)
                }, modifier = Modifier.fillMaxSize())
        }
    }

    fun refreshCurrentCallback() {
        callbackVersion = 1
        state = state.afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(revision = "fresh-r2")))
    }

    fun assertNoPlacementMutation() = assertTrue(mutations.isEmpty(), "Moving or reviewing a Git pane must not mutate Git")
}
