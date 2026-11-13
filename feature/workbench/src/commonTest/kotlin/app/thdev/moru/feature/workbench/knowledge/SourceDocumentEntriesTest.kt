package app.thdev.moru.feature.workbench.knowledge

import app.thdev.moru.feature.workbench.visibleProjectTreeRows
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceDocumentEntriesTest {
    @Test
    fun groupsDocumentsUnderRealFoldersInCodeExplorerOrder() {
        val entries = sourceDocumentEntries(listOf("AGENTS.md", "skills/review/SKILL.md", "skills/guide.md", "skills/guide.md"))
        assertEquals(listOf("skills", "skills/review", "skills/review/SKILL.md", "skills/guide.md", "AGENTS.md"), entries.map { it.path })
        assertEquals(listOf(0, 1, 2, 1, 0), entries.map { it.depth })
        assertEquals(listOf(false, false, true, true, true), entries.map { it.isFile })
    }

    @Test
    fun closedFoldersUseSameVisibilityPolicyAsCode() {
        val entries = sourceDocumentEntries(listOf("AGENTS.md", "skills/review/SKILL.md", "skills/guide.md"))
        assertEquals(listOf("skills", "AGENTS.md"), visibleProjectTreeRows(entries, emptySet(), emptyMap()).map { it.path })
        assertEquals(listOf("skills", "skills/review", "skills/guide.md", "AGENTS.md"), visibleProjectTreeRows(entries, setOf("skills"), emptyMap()).map { it.path })
    }

    @Test
    fun emptyAndInvalidPathsDoNotCreateBrokenRoots() {
        assertEquals(emptyList(), sourceDocumentEntries(listOf("", "/outside.md", "../outside.md", "skills/./file.md")))
    }
}
