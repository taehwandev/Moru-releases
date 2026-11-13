package app.thdev.moru.app

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StartupIssueReportTest {
    private fun query(url: String): Map<String, String> =
        URI(url).rawQuery.split("&").associate { part ->
            part.substringBefore("=") to URLDecoder.decode(part.substringAfter("="), StandardCharsets.UTF_8)
        }

    @Test
    fun theIssueOpensOnTheReleasesRepositoryFilledInWithoutTheHomeFolder() {
        val error = IllegalStateException("Cannot open /Users/someone/Library/Application Support/Moru/catalog/moru-catalog.db")
        val url = startupIssueUrl(error, version = "26.42.4", system = "Mac OS X 26.0 aarch64", home = "/Users/someone")

        assertTrue(url.startsWith("https://github.com/taehwandev/Moru-kmp/issues/new?"))
        val fields = query(url)
        assertEquals("Moru could not start: Cannot open ~/Library/Application Support/Moru/catalog/moru-catalog.db", fields["title"])
        val body = fields.getValue("body")
        assertTrue("- Version: 26.42.4" in body && "- System: Mac OS X 26.0 aarch64" in body, body)
        assertTrue("IllegalStateException" in body, body)
        assertFalse("/Users/someone" in url || "/Users/someone" in body, "the home folder never goes into a public issue")
        assertEquals("bug", fields["labels"])
    }

    @Test
    fun aDeepStackStillMakesAShortAddress() {
        fun deep(n: Int): Nothing = if (n == 0) error("deep") else deep(n - 1)
        val error = runCatching { deep(500) }.exceptionOrNull()!!

        val url = startupIssueUrl(error, version = "1", system = "s", home = "/h")

        assertTrue(url.length < 16_000, "GitHub refuses very long addresses: ${url.length}")
    }
}
