package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitGraphRow

/** Number of lane colors a palette is expected to provide. */
const val GIT_GRAPH_COLOR_COUNT = 8

/**
 * Assign every commit a lane and the lines to draw around it.
 *
 * [commits] must be newest first and topologically ordered (`git log --topo-order`), so
 * a child is always above its parents. The walk keeps, for each lane, the hash of the
 * commit that lane is waiting for. A commit takes the lane waiting for it; its first
 * parent continues in that same lane, which keeps a branch a straight vertical line.
 * Other parents join the lane already waiting for them or open a new one. Lanes are
 * emptied instead of removed, so lines never shift sideways.
 *
 * A parent missing from [commits] (a truncated history) keeps its lane to the bottom.
 */
fun layoutGitGraph(
    commits: List<GitCommit>,
    colorCount: Int = GIT_GRAPH_COLOR_COUNT,
    budget: GitGraphBudget? = null,
): List<GitGraphRow> {
    require(colorCount >= 3) { "At least three colors are needed to keep neighbors distinct" }
    budget?.checkParents(commits)
    val lanes = LaneState(colorCount, budget)
    return commits.map { commit -> lanes.place(commit) }
}

private class LaneState(private val colorCount: Int, private val budget: GitGraphBudget?) {
    private val waitingFor = mutableListOf<String?>()
    private val colors = mutableListOf<Int>()
    private var nextColor = 0
    private var projectedElements = 0

    fun place(commit: GitCommit): GitGraphRow {
        val converging = waitingFor.indices.filter { lane -> waitingFor[lane] == commit.hash }
        val nodeLane = converging.firstOrNull() ?: freeLane()
        val nodeColor = if (converging.isEmpty()) newColor(nodeLane) else colors[nodeLane]

        val throughCount = waitingFor.indices.count { lane ->
            waitingFor[lane] != null && lane != nodeLane && lane !in converging
        }
        reserveProjection(1 + throughCount + converging.size + commit.parents.size)
        val through = waitingFor.indices
            .filter { lane -> waitingFor[lane] != null && lane != nodeLane && lane !in converging }
            .map { lane -> GitGraphRow.Lane(lane, colors[lane]) }
        val incoming = converging.map { lane -> GitGraphRow.Link(lane, nodeLane, colors[lane]) }

        converging.forEach { lane -> waitingFor[lane] = null }
        val outgoing = followParents(commit, nodeLane, nodeColor)

        val used = through.map { it.lane } + incoming.flatMap { listOf(it.fromLane, it.toLane) } +
            outgoing.flatMap { listOf(it.fromLane, it.toLane) } + nodeLane
        val row = GitGraphRow(
            commit = commit,
            nodeLane = nodeLane,
            nodeColor = nodeColor,
            laneCount = used.max() + 1,
            through = through,
            incoming = incoming,
            outgoing = outgoing,
        )
        trimEmptyTrailingLanes()
        return row
    }

    private fun followParents(commit: GitCommit, nodeLane: Int, nodeColor: Int): List<GitGraphRow.Link> {
        val links = mutableListOf<GitGraphRow.Link>()
        commit.parents.forEachIndexed { index, parent ->
            val waiting = waitingFor.indexOf(parent)
            when {
                waiting >= 0 -> links += GitGraphRow.Link(nodeLane, waiting, colors[waiting])
                index == 0 -> {
                    waitingFor[nodeLane] = parent
                    colors[nodeLane] = nodeColor
                    links += GitGraphRow.Link(nodeLane, nodeLane, nodeColor)
                }
                else -> {
                    val lane = freeLane()
                    waitingFor[lane] = parent
                    colors[lane] = newColor(lane)
                    links += GitGraphRow.Link(nodeLane, lane, colors[lane])
                }
            }
        }
        return links
    }

    private fun freeLane(): Int {
        val free = waitingFor.indexOf(null)
        if (free >= 0) return free
        if (budget != null && waitingFor.size >= budget.maxActiveLanes) {
            throw GitGraphBudget.Exceeded("${budget.maxActiveLanes} active lanes")
        }
        waitingFor += null
        colors += 0
        return waitingFor.lastIndex
    }

    private fun reserveProjection(count: Int) {
        val limit = budget?.maxProjectedElements ?: return
        if (count > limit - projectedElements) throw GitGraphBudget.Exceeded("$limit graph elements")
        projectedElements += count
    }

    /** Next palette color that differs from the colors of the lanes on either side. */
    private fun newColor(lane: Int): Int {
        val neighbors = setOfNotNull(
            colors.getOrNull(lane - 1)?.takeIf { waitingFor.getOrNull(lane - 1) != null },
            colors.getOrNull(lane + 1)?.takeIf { waitingFor.getOrNull(lane + 1) != null },
        )
        repeat(colorCount) { offset ->
            val candidate = (nextColor + offset) % colorCount
            if (candidate !in neighbors) {
                nextColor = candidate + 1
                return candidate
            }
        }
        return nextColor++ % colorCount
    }

    private fun trimEmptyTrailingLanes() {
        while (waitingFor.isNotEmpty() && waitingFor.last() == null) {
            waitingFor.removeAt(waitingFor.lastIndex)
            colors.removeAt(colors.lastIndex)
        }
    }
}
