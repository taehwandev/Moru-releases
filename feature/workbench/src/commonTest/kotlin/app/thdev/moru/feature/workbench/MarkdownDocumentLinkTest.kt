package app.thdev.moru.feature.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkdownDocumentLinkTest {
    @Test
    fun resolvesRelativeEncodedPathsAndDiscardsFragments() {
        assertEquals("docs/next note.md", resolveMarkdownDocumentLink("docs/current.md", "next%20note.md#heading", emptyList()))
        assertEquals("README.md", resolveMarkdownDocumentLink("docs/current.md", "../README.md", emptyList()))
    }

    @Test
    fun resolvesOnlyUnambiguousWikiBasenames() {
        assertEquals("notes/Guide.md", resolveMarkdownDocumentLink("docs/current.md", "Guide", listOf("notes/Guide.md")))
        assertNull(resolveMarkdownDocumentLink("docs/current.md", "Guide", listOf("notes/Guide.md", "other/Guide.md")))
        assertEquals("docs/Guide.md", resolveMarkdownDocumentLink("docs/current.md", "Guide", listOf("docs/Guide.md", "notes/Guide.md")))
    }

    @Test
    fun rejectsSchemesAbsolutePathsAndEscapingOrMalformedTargets() {
        listOf("https://example.org", "file:///etc/passwd", "/private/file.md", "C:\\private\\file.md", "../../secret.md", "%2e%2e/%2e%2e/secret.md", "bad%ZZ.md", "#heading", "").forEach {
            assertNull(resolveMarkdownDocumentLink("docs/current.md", it, emptyList()), it)
        }
    }
}
