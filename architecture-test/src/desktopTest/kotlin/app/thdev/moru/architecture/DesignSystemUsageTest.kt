package app.thdev.moru.architecture

import kotlin.test.Test

/**
 * Feature screens and the app target draw with core:designsystem: Moru components instead of raw
 * Material or foundation controls, and theme tokens instead of literal font sizes, radii and colours.
 * Literal values belong in core:designsystem, where every theme and the app font size can reach them.
 */
class DesignSystemUsageTest {
    private val callers = KmpSourceTree.modules().filter { it.kind == "feature" || it.path == ":app-desktop" }

    @Test
    fun `feature and app sources use Moru components, not raw Material or text fields`() {
        assertMatchesBaseline("Raw control outside core:designsystem", violations(RAW_CONTROLS), emptySet())
    }

    @Test
    fun `feature and app sources take font sizes and corner radii from the theme`() {
        assertMatchesBaseline("Literal size outside core:designsystem", violations(LITERAL_SIZES), emptySet())
    }

    @Test
    fun `feature and app sources take colours from the theme`() {
        assertMatchesBaseline("Literal colour outside core:designsystem", violations(LITERAL_COLORS), LITERAL_COLOR_ARTWORK)
    }

    private fun violations(rules: Map<Regex, String>): Set<String> =
        callers.flatMap { module ->
            module.sourceFiles(mainOnly = true).flatMap { file ->
                val text = file.readText()
                rules.filter { (pattern, _) -> pattern.containsMatchIn(text) }.map { (_, what) -> "${module.relative(file)}: $what" }
            }
        }.toSet()

    private companion object {
        val RAW_CONTROLS = linkedMapOf(
            Regex("""(?m)^import androidx\.compose\.material""") to "imports Material",
            Regex("""\bBasicTextField\(""") to "uses BasicTextField",
        )

        val LITERAL_SIZES = linkedMapOf(
            Regex("""\b\d+(\.\d+)?\.sp\b""") to "literal sp",
            Regex("""RoundedCornerShape\([^)]*\b\d+(\.\d+)?(\.dp\b|\))""") to "literal corner radius",
        )

        val LITERAL_COLORS = linkedMapOf(
            Regex("""\bColor\(0x""") to "literal colour",
        )

        /** Drawn artwork and data palettes, not UI chrome: git graph lane colours. */
        val LITERAL_COLOR_ARTWORK = setOf(
            "feature/git/src/commonMain/kotlin/app/thdev/moru/feature/git/history/GitGraphPalette.kt: literal colour",
        )
    }
}
