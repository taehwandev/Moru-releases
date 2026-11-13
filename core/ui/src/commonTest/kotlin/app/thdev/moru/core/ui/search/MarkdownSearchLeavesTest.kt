package app.thdev.moru.core.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownSearchLeavesTest {
    @Test
    fun countsVisibleLabelsAcrossStylesAndLeavesDestinationsAndMarkupOut() {
        val leaves = markdownSearchLeaves("# **Hello** [world](https://hidden.example)\n\n```kotlin\nval x = \"world\"\n```", "reply")
        assertEquals(listOf("Hello world", "val x = \"world\""), leaves.map { it.text })
        assertEquals(1, leaves.flatMap { it.matches("hello world") }.size)
        assertEquals(2, leaves.flatMap { it.matches("world") }.size)
        assertTrue(leaves.flatMap { it.matches("hidden.example") }.isEmpty())
        assertTrue(leaves.flatMap { it.matches("**") }.isEmpty())
    }

    @Test
    fun duplicateParagraphsAndTableCellsHaveDifferentNavigationTargets() {
        val leaves = markdownSearchLeaves("same\n\nsame\n\n| same | same |\n| --- | --- |\n| same | same |", "reply")
        val matches = leaves.flatMap { it.matches("same") }
        assertEquals(6, matches.size)
        assertEquals(6, matches.map { it.path }.distinct().size)
        assertTrue(matches.any { it.path.endsWith("/row:1/cell:1/inline:0") })
    }

    @Test
    fun quotedTextAndImageSeparatedTextKeepReaderIdentities() {
        val leaves = markdownSearchLeaves("> quoted\n\nfirst ![picture](hidden.png) last", "reply")
        assertEquals(listOf("quoted", "first ", " last"), leaves.map { it.text })
        assertEquals("reply/block:0/block:0/inline:0", leaves.first().path)
    }
}
