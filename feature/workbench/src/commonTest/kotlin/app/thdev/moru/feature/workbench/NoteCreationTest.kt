package app.thdev.moru.feature.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoteCreationTest {
    @Test
    fun appendsMarkdownExtensionWhenMissing() {
        assertEquals("Ideas.md", newNoteRelativePath("Ideas"))
    }

    @Test
    fun keepsExistingExtension() {
        assertEquals("notes/todo.txt", newNoteRelativePath("notes/todo.txt"))
    }

    @Test
    fun trimsAndNormalizesSeparators() {
        assertEquals("notes/deep/note.md", newNoteRelativePath("  notes\\deep\\note  "))
    }

    @Test
    fun rejectsBlankNames() {
        assertNull(newNoteRelativePath("   "))
        assertNull(newNoteRelativePath("/"))
    }

    @Test
    fun rejectsTraversalSegments() {
        assertNull(newNoteRelativePath("../secret"))
        assertNull(newNoteRelativePath("notes/../../etc/passwd"))
    }
}
