package app.thdev.moru.feature.git.history

import app.thdev.moru.core.model.git.history.GitGraphRow as ModelGraphRow
import app.thdev.moru.core.model.git.history.GitCommit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GitGraphLayoutTest {
    private fun commit(hash: String, vararg parents: String) = GitCommit(
        hash = hash,
        parents = parents.toList(),
        authorName = "Ada",
        authorEmail = "ada@example.com",
        epochSeconds = 0,
        refs = emptyList(),
        subject = hash,
    )

    private fun List<GitGraphRow>.row(hash: String) = single { it.commit.hash == hash }

    @Test
    fun linearHistoryStaysInOneStraightLane() {
        val rows = layoutGitGraph(listOf(commit("c3", "c2"), commit("c2", "c1"), commit("c1")))

        assertEquals(listOf(0, 0, 0), rows.map { it.nodeLane })
        assertEquals(listOf(1, 1, 1), rows.map { it.laneCount })
        assertEquals(emptyList(), rows.row("c3").incoming, "the newest commit has nothing above it")
        assertEquals(listOf(ModelGraphRow.Link(0, 0, 0)), rows.row("c2").incoming)
        assertEquals(listOf(ModelGraphRow.Link(0, 0, 0)), rows.row("c2").outgoing)
        assertEquals(emptyList(), rows.row("c1").outgoing, "a root commit ends its lane")
    }

    @Test
    fun mergeOpensASecondLaneAndBothParentsJoinTheirCommonAncestor() {
        val rows = layoutGitGraph(
            listOf(commit("m", "a", "b"), commit("a", "r"), commit("b", "r"), commit("r")),
        )

        val merge = rows.row("m")
        assertEquals(0, merge.nodeLane)
        assertEquals(
            listOf(ModelGraphRow.Link(0, 0, 0), ModelGraphRow.Link(0, 1, 1)),
            merge.outgoing,
            "first parent continues straight, second parent opens lane 1",
        )

        val first = rows.row("a")
        assertEquals(0, first.nodeLane)
        assertEquals(listOf(ModelGraphRow.Lane(1, 1)), first.through, "lane 1 keeps waiting for b")

        val second = rows.row("b")
        assertEquals(1, second.nodeLane)
        assertEquals(listOf(ModelGraphRow.Lane(0, 0)), second.through)
        assertEquals(
            listOf(ModelGraphRow.Link(1, 0, 0)),
            second.outgoing,
            "b bends back into the lane already waiting for r",
        )

        val root = rows.row("r")
        assertEquals(0, root.nodeLane)
        assertEquals(listOf(ModelGraphRow.Link(0, 0, 0)), root.incoming)
        assertEquals(listOf(2, 2, 2, 1), rows.map { it.laneCount })
    }

    @Test
    fun twoBranchTipsOverASharedParentUseTwoLanesThenConverge() {
        val rows = layoutGitGraph(listOf(commit("x", "p"), commit("y", "p"), commit("p")))

        val tip = rows.row("y")
        assertEquals(1, tip.nodeLane)
        assertEquals(listOf(ModelGraphRow.Lane(0, 0)), tip.through)
        assertEquals(listOf(ModelGraphRow.Link(1, 0, 0)), tip.outgoing)
        assertEquals(0, rows.row("p").nodeLane)
    }

    @Test
    fun aFreedLaneIsReusedByTheNextNewBranchTip() {
        val rows = layoutGitGraph(
            listOf(
                commit("x", "p1"),
                commit("y", "p2"),
                commit("p1"),
                commit("z", "p3"),
                commit("p2"),
                commit("p3"),
            ),
        )

        assertEquals(1, rows.row("y").nodeLane)
        assertEquals(0, rows.row("z").nodeLane, "lane 0 was freed when p1 ended, so z takes it instead of lane 2")
        assertTrue(rows.all { it.laneCount <= 2 }, "no row should ever need a third lane")
    }

    @Test
    fun aParentMissingFromTheHistoryKeepsItsLaneToTheBottom() {
        val rows = layoutGitGraph(listOf(commit("a", "not-loaded")))

        assertEquals(listOf(ModelGraphRow.Link(0, 0, 0)), rows.single().outgoing)
    }

    @Test
    fun adjacentLanesNeverShareAColor() {
        val octopus = listOf(
            commit("o", "a", "b", "c", "d"),
            commit("a"),
            commit("b"),
            commit("c"),
            commit("d"),
        )
        val merge = listOf(commit("m", "a", "b"), commit("a", "r"), commit("b", "r"), commit("r"))

        (layoutGitGraph(octopus) + layoutGitGraph(merge)).forEach { row ->
            val colorsByLane = (row.through.associate { it.lane to it.color } + (row.nodeLane to row.nodeColor))
            colorsByLane.forEach { (lane, color) ->
                val neighbor = colorsByLane[lane + 1]
                assertTrue(neighbor == null || neighbor != color, "row ${row.commit.hash}: lanes $lane and ${lane + 1} share color $color")
            }
        }
    }

    @Test
    fun octopusMergeFansOutToOneLanePerParent() {
        val rows = layoutGitGraph(
            listOf(commit("o", "a", "b", "c"), commit("a"), commit("b"), commit("c")),
        )

        assertEquals(listOf(0, 1, 2), rows.row("o").outgoing.map { it.toLane })
        assertEquals(3, rows.row("o").laneCount)
    }

    @Test
    fun layoutIsDeterministicAndCoversEveryCommitInOrder() {
        val commits = listOf(commit("m", "a", "b"), commit("a", "r"), commit("b", "r"), commit("r"))

        assertEquals(layoutGitGraph(commits), layoutGitGraph(commits))
        assertEquals(commits.map { it.hash }, layoutGitGraph(commits).map { it.commit.hash })
        assertEquals(emptyList(), layoutGitGraph(emptyList()))
    }

    @Test
    fun fewerThanThreeColorsIsRejectedBecauseNeighborsCouldNotStayDistinct() {
        assertFailsWith<IllegalArgumentException> { layoutGitGraph(emptyList(), colorCount = 2) }
    }
}
