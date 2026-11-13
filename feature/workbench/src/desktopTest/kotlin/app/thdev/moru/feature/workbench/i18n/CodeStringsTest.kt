package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Code tab text in both languages. */
class CodeStringsTest {
    private val english = codeStrings(UiLanguage.English)
    private val korean = codeStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun CodeStrings.allText(): List<String> =
        listOf(
            explorerTitle, newNote, quickOpenPlaceholder, noMatches, newNotePlaceholder, create, rename, confirmDelete,
            cancel, delete, newPathPlaceholder, save, edit, read, unknownSize, savedBytes(12), selectFilePrompt, gitTitle,
            refresh, changedCount(3), clean, gitSummary(1, 2, 3),
        ) + WorkbenchFilePreviewStatus.entries.flatMap { listOf(previewKind(it), previewMessage(it)) }

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { hangul.containsMatchIn(it) })
        assertTrue(korean.save.contains(hangul))
        assertTrue(korean.previewMessage(WorkbenchFilePreviewStatus.BINARY).contains(hangul))
    }

    @Test
    fun everyPreviewStatusHasItsOwnKindAndMessage() {
        listOf(english, korean).forEach { table ->
            val kinds = WorkbenchFilePreviewStatus.entries.map { table.previewKind(it) }
            val messages = WorkbenchFilePreviewStatus.entries.map { table.previewMessage(it) }
            assertEquals(kinds.size, kinds.toSet().size)
            assertEquals(messages.size, messages.toSet().size)
        }
    }

    @Test
    fun countsAreCarriedThrough() {
        listOf(english, korean).forEach { table ->
            assertTrue(table.changedCount(7).contains("7"))
            assertTrue(table.savedBytes(42).contains("42"))
            assertTrue(table.gitSummary(1, 2, 3).let { "1" in it && "2" in it && "3" in it })
        }
    }
}
