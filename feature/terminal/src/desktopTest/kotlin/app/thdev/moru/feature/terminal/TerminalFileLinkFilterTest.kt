package app.thdev.moru.feature.terminal

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalFileLinkFilterTest {
    @Test
    fun onlyAPathNamingAnExistingFileBecomesALinkThatOpensIt() {
        val folder = Files.createTempDirectory("links").toFile().canonicalFile
        File(folder, "docs").mkdirs()
        val prd = File(folder, "docs/PRD.md").apply { writeText("# PRD") }
        val opened = mutableListOf<String>()
        val line = "⏺ Wrote docs/PRD.md:3 and missing.md"

        val item = TerminalFileLinkFilter(folder.path) { opened += it }.apply(line)!!.items.single()
        assertEquals("docs/PRD.md:3", line.substring(item.startOffset, item.endOffset))

        item.linkInfo.navigate()
        assertEquals(listOf(prd.path + ":3"), opened)
    }

    @Test
    fun aLineWithoutFilesHasNoLinks() {
        assertNull(TerminalFileLinkFilter(System.getProperty("java.io.tmpdir")) {}.apply("Thinking… (3s · esc to interrupt)"))
    }
}
