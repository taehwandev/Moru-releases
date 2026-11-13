package app.thdev.moru.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkdownLinkKindTest {
    private val localhost: (String) -> Boolean = { it.contains("//localhost") }

    @Test fun `web pages are told from pages this machine serves by the host's own test`() {
        assertEquals(MarkdownLinkKind.Web, markdownLinkKind("https://example.com/docs", localhost))
        assertEquals(MarkdownLinkKind.Web, markdownLinkKind("HTTP://example.com", localhost))
        assertEquals(MarkdownLinkKind.LocalPage, markdownLinkKind("http://localhost:3000/", localhost))
        // Without a host that opens local pages, every web page goes to the browser.
        assertEquals(MarkdownLinkKind.Web, markdownLinkKind("http://localhost:3000/"))
    }

    @Test fun `paths, file addresses and file lines are files`() {
        assertEquals(MarkdownLinkKind.File, markdownLinkKind("docs/PRD.md"))
        assertEquals(MarkdownLinkKind.File, markdownLinkKind("/Users/me/notes.txt"))
        assertEquals(MarkdownLinkKind.File, markdownLinkKind("~/.claude/skills/x/SKILL.md"))
        assertEquals(MarkdownLinkKind.File, markdownLinkKind("file:///tmp/a.md"))
        assertEquals(MarkdownLinkKind.File, markdownLinkKind("Main.kt:12:3"))
    }

    @Test fun `headings and schemes a document may not launch go nowhere`() {
        assertNull(markdownLinkKind("#install"))
        assertNull(markdownLinkKind("mailto:someone@example.com"))
        assertNull(markdownLinkKind("javascript:run()"))
    }
}
