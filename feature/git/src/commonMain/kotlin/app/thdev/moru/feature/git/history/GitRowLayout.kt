package app.thdev.moru.feature.git.history

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val GitLaneWidth = 16.dp
internal val GitAuthorColumnWidth = 112.dp
internal val GitTimeColumnWidth = 72.dp
internal val GitHashColumnWidth = 68.dp

private val SubjectMinWidth = 200.dp
/** Wide enough for the column's name, so a one-lane graph does not cut its header short. */
private val GraphMinWidth = 44.dp
private val ShowAuthorFrom = 560.dp
private val ShowHashFrom = 680.dp

/**
 * How wide each cell of a commit row is. The graph comes first, then the branch and tag labels and the subject, then
 * the author, age and hash in fixed columns on the right. The subject is read first and gets what is left: a narrow
 * pane drops the hash, then the author, and a graph with many lanes is compressed, before the subject gives up its
 * minimum width. Nothing is pushed out of view.
 */
internal data class GitRowLayout(
    val graphWidth: Dp,
    val showAuthor: Boolean,
    val showHash: Boolean,
)

internal fun gitRowLayout(available: Dp, laneCount: Int): GitRowLayout {
    val showAuthor = available >= ShowAuthorFrom
    val showHash = available >= ShowHashFrom
    val fixed = GitTimeColumnWidth +
        (if (showAuthor) GitAuthorColumnWidth else 0.dp) +
        (if (showHash) GitHashColumnWidth else 0.dp)
    val room = available - fixed - SubjectMinWidth
    val wanted = GitLaneWidth * laneCount.coerceAtLeast(1)
    val graphWidth = minOf(wanted, room).coerceAtLeast(GraphMinWidth)
    return GitRowLayout(graphWidth, showAuthor, showHash)
}
