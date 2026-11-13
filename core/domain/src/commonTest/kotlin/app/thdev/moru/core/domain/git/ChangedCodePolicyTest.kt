package app.thdev.moru.core.domain.git

import app.thdev.moru.core.domain.git.ChangedCodePolicy.Entry.Folded
import app.thdev.moru.core.domain.git.ChangedCodePolicy.Entry.Line
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangedCodePolicyTest {
    private fun context(old: Int, new: Int) = DiffLine(DiffLineKind.Context, "", old, new)
    private fun added(new: Int) = DiffLine(DiffLineKind.Added, "", null, new)
    private fun removed(old: Int) = DiffLine(DiffLineKind.Removed, "", old, null)
    private fun file(vararg hunks: List<DiffLine>) =
        DiffFile("f", "f", DiffFileStatus.Modified, hunks.map { DiffHunk("@@", it) }, false)

    @Test fun hunkContextIsKeptAndTheRestIsFolded() {
        val change = file(listOf(context(19, 19), context(20, 20), removed(21), added(21), context(22, 22)),
            listOf(context(40, 40), added(41), context(42, 42)))
        assertEquals(listOf(Folded(18)) + (19..22).map(::Line) + Folded(17) + (40..42).map(::Line) + Folded(58),
            ChangedCodePolicy.entries(change, 100))
    }

    @Test fun touchingAndOverlappingHunksReadAsOneRange() {
        val change = file(listOf(context(1, 1), added(2)), listOf(context(3, 3), added(4)),
            listOf(context(3, 3), added(4), context(5, 5)))
        assertEquals((1..5).map(::Line) + Folded(5), ChangedCodePolicy.entries(change, 10))
    }

    @Test fun deletionKeepsSurroundingCurrentLines() {
        val change = file(listOf(context(4, 4), removed(5), context(6, 5)))
        assertEquals(listOf(Folded(3), Line(4), Line(5), Folded(5)), ChangedCodePolicy.entries(change, 10))
        assertEquals(emptySet(), ChangedCodePolicy.changedLineNumbers(change))
    }

    @Test fun newFileShowsEveryAddedLine() {
        val change = file((1..3).map(::added))
        assertEquals((1..3).map(::Line), ChangedCodePolicy.entries(change, 3))
        assertEquals(setOf(1, 2, 3), ChangedCodePolicy.changedLineNumbers(change))
    }

    @Test fun shorterPreviewClipsHunkRowsAndOnlyAddedLinesCarryMarks() {
        val change = file(listOf(context(0, 0), added(1), context(2, 2), added(99)))
        assertEquals(listOf(Line(1), Line(2)), ChangedCodePolicy.entries(change, 2))
        assertEquals(setOf(1, 99), ChangedCodePolicy.changedLineNumbers(change))
        assertEquals(emptyList(), ChangedCodePolicy.entries(change, 0))
    }

    @Test fun lineSlicesPreserveEmptyAndFinalNewlineBehavior() {
        fun lines(content: String) = ChangedCodePolicy.lineSlices(content).map { content.substring(it.start, it.end) }
        assertEquals(listOf(""), lines(""))
        assertEquals(listOf("a"), lines("a\n"))
        assertEquals(listOf("", ""), lines("\n\n"))
        assertEquals(listOf("a\r", "b"), lines("a\r\nb\n"))
        assertEquals(listOf("😀", "끝"), lines("😀\n끝"))
        assertEquals(listOf(Folded(1)), ChangedCodePolicy.entries(file(), lines("").size))
    }

    @Test fun preferredSelectionSurvivesRefreshAndFallsBackWhenGone() {
        assertEquals("b", ChangedCodePolicy.selectedPath(listOf("a", "b"), "b"))
        assertEquals("a", ChangedCodePolicy.selectedPath(listOf("a", "b"), "gone"))
        assertEquals("a", ChangedCodePolicy.selectedPath(listOf("a", "b"), null))
        assertNull(ChangedCodePolicy.selectedPath(emptyList(), "b"))
    }
}
