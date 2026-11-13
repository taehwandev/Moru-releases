package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import app.thdev.moru.feature.codepanel.ChangedCodeEntry.Folded
import app.thdev.moru.feature.codepanel.ChangedCodeEntry.Line
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangedCodeEntriesTest {
    private fun context(old: Int, new: Int) = DiffLine(DiffLineKind.Context, "", old, new)
    private fun added(new: Int) = DiffLine(DiffLineKind.Added, "", null, new)
    private fun removed(old: Int) = DiffLine(DiffLineKind.Removed, "", old, null)
    private fun file(vararg hunks: List<DiffLine>) =
        DiffFile("a.kt", "a.kt", DiffFileStatus.Modified, hunks.map { DiffHunk("@@", it) }, isBinary = false)

    @Test
    fun onlyTheChangedRangesShowWithTheRestFolded() {
        val change = file(
            listOf(context(19, 19), context(20, 20), removed(21), added(21), context(22, 22)),
            listOf(context(40, 40), added(41), context(42, 42)),
        )
        val entries = changedCodeEntries(change, 100)
        assertEquals(
            listOf(Folded(18)) + (19..22).map(::Line) + Folded(17) + (40..42).map(::Line) + Folded(58),
            entries,
        )
    }

    @Test
    fun hunksThatTouchReadAsOneRange() {
        val change = file(listOf(context(1, 1), added(2)), listOf(context(3, 3), added(4)))
        assertEquals((1..4).map(::Line) + Folded(6), changedCodeEntries(change, 10))
    }

    @Test
    fun aDeletionShowsTheLinesAroundIt() {
        val change = file(listOf(context(4, 4), removed(5), context(6, 5)))
        assertEquals(listOf(Folded(3), Line(4), Line(5), Folded(5)), changedCodeEntries(change, 10))
    }

    @Test
    fun aNewFileShowsWhole() {
        val change = file((1..3).map(::added))
        assertEquals((1..3).map(::Line), changedCodeEntries(change, 3))
    }
}
