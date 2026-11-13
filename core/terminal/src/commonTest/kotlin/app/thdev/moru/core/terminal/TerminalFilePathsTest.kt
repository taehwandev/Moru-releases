package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalFilePathsTest {
    private fun paths(line: String) = terminalFilePaths(line).map { it.path }

    @Test
    fun findsRelativeHomeAndAbsolutePaths() {
        assertEquals(listOf("docs/PRD.md", "~/notes/plan.md", "/tmp/a.kt:12"), paths("see docs/PRD.md, ~/notes/plan.md and /tmp/a.kt:12."))
    }

    @Test
    fun findsThePathInsideAnAgentToolCall() {
        val line = "⏺ Update(desktop-kmp/core/App.kt)"
        val found = terminalFilePaths(line).single()
        assertEquals("desktop-kmp/core/App.kt", found.path)
        assertEquals(found.path, line.substring(found.start, found.end))
    }

    @Test
    fun findsAFileNameWithAnExtension() {
        assertEquals(listOf("README.md", "PLAN.md:3"), paths("Wrote README.md and `PLAN.md:3`"))
    }

    @Test
    fun skipsWebAddressesWordsAndNumbers() {
        assertEquals(emptyList(), paths("open https://example.org/docs/a.md now, 3.14 and v1.2 done ... / ./"))
    }
}
