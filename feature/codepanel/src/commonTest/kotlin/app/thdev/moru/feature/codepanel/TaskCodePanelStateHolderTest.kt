package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCodePanelStateHolderTest {
    private class FakeSource(private val stamps: List<String?>, private val result: CommitDiffResult? = null) : CodePanelSource {
        var stampReads = 0
        var changeReads = 0
        val changeRoots = mutableListOf<String>()

        override fun worktrees(projectFolder: String) = listOf(CodeWorktree("/repo", "main", isMain = true))

        override fun sessionWorktreeRoots(sessionId: String) = listOf("/repo")

        override fun changeStamp(worktreeRoot: String): String? = stamps[stampReads.coerceAtMost(stamps.lastIndex)].also { stampReads++ }

        override fun changes(worktreeRoot: String): CommitDiffResult {
            changeReads++
            changeRoots += worktreeRoot
            return result ?: CommitDiffResult.Loaded(emptyList(), truncated = false)
        }

        override fun readFile(worktreeRoot: String, relativePath: String) = WorkbenchFilePreview.unavailable(relativePath, "unused")
    }

    @Test
    fun changesAreRereadOnlyWhenTheStampMoves() = runTest {
        val source = FakeSource(listOf("a", "a", null, "b"))
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)

        val watch = launch { holder.watchChanges("/repo") }
        runCurrent()
        advanceTimeBy(350)
        watch.cancel()

        assertEquals(4, source.stampReads)
        assertEquals(2, source.changeReads, "first read, then only when the stamp became b")
        assertTrue(holder.changes is CommitDiffResult.Loaded)
    }

    @Test
    fun firstUnknownStampLoadsOnceAndAnEmptyLoadedSnapshotIsReused() = runTest {
        val source = FakeSource(listOf(null, null, null))
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)

        holder.watchChanges("/repo", repeat = false)
        val snapshot = holder.changes
        assertEquals(CommitDiffResult.Loaded(emptyList(), truncated = false), snapshot)
        repeat(2) { holder.watchChanges("/repo", repeat = false) }

        assertEquals(3, source.stampReads)
        assertEquals(1, source.changeReads)
        assertSame(snapshot, holder.changes)
    }

    @Test
    fun resumingAfterPauseChecksOnceAndReusesTheSnapshotUntilTheStampMoves() = runTest {
        val source = FakeSource(listOf("a", "a", null, "b", "b"))
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)
        val watch = launch { holder.watchChanges("/repo") }
        runCurrent()
        watch.cancelAndJoin()
        val snapshot = holder.changes

        holder.watchChanges("/repo", repeat = false)
        holder.watchChanges("/repo", repeat = false)
        assertEquals(1, source.changeReads)
        assertSame(snapshot, holder.changes)
        holder.watchChanges("/repo", repeat = false)
        holder.watchChanges("/repo", repeat = false)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(5, source.stampReads, "one stamp per resumed one-shot and no surviving watch loop")
        assertEquals(2, source.changeReads, "the moved stamp b rereads once")
    }

    @Test
    fun changingOrClearingTheRootInvalidatesEvenAnIdenticalStamp() = runTest {
        val source = FakeSource(listOf("a"))
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)

        holder.watchChanges("/repo", repeat = false)
        holder.watchChanges("/other", repeat = false)
        holder.watchChanges(null, repeat = false)
        assertNull(holder.changes)
        holder.watchChanges("/other", repeat = false)

        assertEquals(listOf("/repo", "/other", "/other"), source.changeRoots)
        assertEquals(3, source.stampReads)
        assertEquals(3, source.changeReads)
    }

    @Test
    fun anUnavailableResultRetainsTheExistingNonNullSnapshotBehavior() = runTest {
        val unavailable = CommitDiffResult.Unavailable("Cannot read changes")
        val source = FakeSource(listOf("a", null, "a"), unavailable)
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)

        repeat(3) { holder.watchChanges("/repo", repeat = false) }

        assertEquals(3, source.stampReads)
        assertEquals(1, source.changeReads)
        assertSame(unavailable, holder.changes)
    }

    @Test
    fun noRootClearsTheShownChanges() = runTest {
        val source = FakeSource(listOf("a"))
        val holder = TaskCodePanelStateHolder(source, CodePanelState(emptyMap()), StandardTestDispatcher(testScheduler), 100)
        val watch = launch { holder.watchChanges("/repo") }
        runCurrent()
        watch.cancel()

        holder.watchChanges(null)

        assertNull(holder.changes)
    }

    @Test
    fun followingPublishesTheCheckoutsAndTheTerminalsRoot() = runTest {
        val state = CodePanelState(emptyMap())
        val holder = TaskCodePanelStateHolder(FakeSource(listOf("a")), state, StandardTestDispatcher(testScheduler), 100)

        val follow = launch { holder.followWorktrees("task", "terminal", "session", "/repo") }
        runCurrent()
        follow.cancel()

        assertTrue(holder.checked)
        assertEquals(listOf("/repo"), holder.worktrees.map { it.path })
        assertEquals("/repo", holder.shown("task", "terminal"))
    }
}
