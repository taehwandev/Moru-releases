package app.thdev.moru.feature.git.history

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitRowLayoutTest {
    private fun subjectRoom(available: Dp, layout: GitRowLayout): Dp =
        available - layout.graphWidth - GitTimeColumnWidth -
            (if (layout.showAuthor) GitAuthorColumnWidth else 0.dp) -
            (if (layout.showHash) GitHashColumnWidth else 0.dp)

    @Test
    fun theSubjectKeepsItsRoomHoweverManyLanesTheGraphHasAndHoweverNarrowThePane() {
        for (width in listOf(320, 360, 480, 520, 560, 640, 680, 760, 900, 1200, 1600)) {
            for (lanes in listOf(1, 4, 12, 30, 80)) {
                val available = width.dp
                val room = subjectRoom(available, gitRowLayout(available, lanes))
                assertTrue(room >= 200.dp, "subject has only $room of a $available pane with $lanes lanes")
            }
        }
    }

    @Test
    fun aGraphThatFitsKeepsItsFullLaneWidth() {
        assertEquals(GitLaneWidth * 3, gitRowLayout(1200.dp, 3).graphWidth)
    }

    @Test
    fun aNarrowPaneDropsTheHashThenTheAuthorBeforeTheSubject() {
        val narrow = gitRowLayout(400.dp, 10)
        val middle = gitRowLayout(600.dp, 10)

        assertEquals(false, narrow.showAuthor)
        assertEquals(false, narrow.showHash)
        assertEquals(true, middle.showAuthor)
        assertEquals(false, middle.showHash)
    }

    @Test
    fun aWidePaneShowsEverything() {
        val wide = gitRowLayout(1200.dp, 3)

        assertTrue(wide.showAuthor && wide.showHash)
    }
}
