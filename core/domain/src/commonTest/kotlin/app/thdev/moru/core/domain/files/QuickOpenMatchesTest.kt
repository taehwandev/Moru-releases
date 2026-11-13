package app.thdev.moru.core.domain.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickOpenMatchesTest {
    @Test
    fun ranksFileNameBeforePathAndCapsResults() {
        val paths = listOf("deep/prd-other.txt", "deep/my-prd.txt", "docs/prd.txt", "prd.txt")
        assertEquals(listOf("prd.txt", "docs/prd.txt", "deep/prd-other.txt", "deep/my-prd.txt"),
            quickOpenMatches("prd", paths))
        assertEquals(40, quickOpenMatches("prd", (0..45).map { "other/prd-$it.txt" }).size)
    }

    @Test
    fun blankAndSubsequenceQueriesKeepExistingContract() {
        assertTrue(quickOpenMatches("  ", listOf("src/App.kt")).isEmpty())
        assertEquals(listOf("src/Application.kt"), quickOpenMatches("apln", listOf("src/Application.kt", "docs/notes.md")))
    }
}
