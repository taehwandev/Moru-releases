package app.thdev.moru.core.model.git.history

/**
 * What to draw for one commit row of the graph. Lanes are columns counted from 0 at the
 * left; [Lane.color] and [Link.color] are palette indexes, never RGB, so the same layout
 * works in every theme.
 *
 * Vertical pass-through lines are [through]. [incoming] links come from the row above
 * and end at the node; [outgoing] links leave the node toward the row below.
 */
data class GitGraphRow(
    val commit: GitCommit,
    val nodeLane: Int,
    val nodeColor: Int,
    val laneCount: Int,
    val through: List<Lane>,
    val incoming: List<Link>,
    val outgoing: List<Link>,
) {
    /** A lane that passes straight through this row without touching the node. */
    data class Lane(val lane: Int, val color: Int)

    /** A line between the node's lane and another lane, at the top or bottom edge of the row. */
    data class Link(val fromLane: Int, val toLane: Int, val color: Int)
}
