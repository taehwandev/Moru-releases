package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitGraphRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GitGraphLayoutTest {
    private fun commit(hash: String, vararg parents: String) =
        GitCommit(hash, parents.toList(), "Ada", "ada@example.invalid", 0, emptyList(), hash)

    @Test fun sharedMergeLanesKeepTheExistingFirstParentShape() {
        val rows = layoutGitGraph(listOf(commit("m", "a", "b"), commit("a", "r"), commit("b", "r"), commit("r")))
        assertEquals(listOf(2, 2, 2, 1), rows.map { it.laneCount })
        assertEquals(listOf(GitGraphRow.Link(0, 0, 0), GitGraphRow.Link(0, 1, 1)), rows.first().outgoing)
        assertEquals(listOf(GitGraphRow.Link(1, 0, 0)), rows[2].outgoing)
        assertTrue(rows.last().outgoing.isEmpty())
    }

    @Test fun missingParentsContinueToTheTruncationBoundary() {
        val row = layoutGitGraph(listOf(commit("a", "not-loaded"))).single()
        assertEquals(listOf(GitGraphRow.Link(0, 0, 0)), row.outgoing)
    }

    @Test fun parentBudgetsRejectFanoutAndAggregateBeforeTheLaneWalk() {
        val budget = GitGraphBudget(64, 2048, 128, 65536)
        val fanout = commit("a", *(1..10000).map { "p$it" }.toTypedArray())
        assertFailsWith<GitGraphBudget.Exceeded> { layoutGitGraph(listOf(fanout), budget = budget) }
        val aggregate = listOf(commit("a", "p1", "p2", "p3"), commit("b", "p4", "p5", "p6"))
        assertFailsWith<GitGraphBudget.Exceeded> {
            layoutGitGraph(aggregate, budget = GitGraphBudget(4, 5, 128, 65536))
        }
    }

    @Test fun laneBudgetChecksBeforeAppendingAndDefaultCallerRemainsUnchanged() {
        val commits = (1..5).map { commit("tip$it", "missing$it") }
        assertFailsWith<GitGraphBudget.Exceeded> {
            layoutGitGraph(commits, budget = GitGraphBudget(64, 2048, 2, 65536))
        }
        assertEquals(5, layoutGitGraph(commits).last().laneCount)
    }

    @Test fun projectionBudgetReservesNodesAndLinksBeforeBuildingTheNextRow() {
        val commits = listOf(commit("c", "b"), commit("b", "a"), commit("a"))
        assertFailsWith<GitGraphBudget.Exceeded> {
            layoutGitGraph(commits, budget = GitGraphBudget(64, 2048, 128, 6))
        }
        assertEquals(3, layoutGitGraph(commits, budget = GitGraphBudget(64, 2048, 128, 7)).size)
    }

    @Test fun longLinearHistoryDoesNotConfuseUniqueParentsWithActiveLanes() {
        val commits = (301 downTo 1).map { commit("$it", "${it - 1}") }
        val rows = layoutGitGraph(commits, budget = GitGraphBudget(64, 2048, 128, 65536))
        assertEquals(301, rows.size)
        assertTrue(rows.all { it.laneCount == 1 })
    }
}
