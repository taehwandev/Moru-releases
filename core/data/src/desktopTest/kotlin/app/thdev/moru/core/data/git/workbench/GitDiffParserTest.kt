package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitDiffParserTest {
    private val modify = """
        diff --git a/src/Main.kt b/src/Main.kt
        index 1111111..2222222 100644
        --- a/src/Main.kt
        +++ b/src/Main.kt
        @@ -1,4 +1,5 @@ fun main
         keep
        -old
        +new
        +extra
         tail
        @@ -20,2 +21,2 @@
         x
        -y
        +z
    """.trimIndent()

    @Test
    fun readsAModifiedFileWithLineNumbersOnBothSides() {
        val file = parseGitDiff(modify).single()

        assertEquals(DiffFileStatus.Modified, file.status)
        assertEquals("src/Main.kt", file.displayPath)
        assertEquals(2, file.hunks.size)
        assertEquals("@@ -1,4 +1,5 @@ fun main", file.hunks[0].header)
        val lines = file.hunks[0].lines
        assertEquals(DiffLine(DiffLineKind.Context, "keep", 1, 1), lines[0])
        assertEquals(DiffLine(DiffLineKind.Removed, "old", 2, null), lines[1])
        assertEquals(DiffLine(DiffLineKind.Added, "new", null, 2), lines[2])
        assertEquals(DiffLine(DiffLineKind.Added, "extra", null, 3), lines[3])
        assertEquals(DiffLine(DiffLineKind.Context, "tail", 3, 4), lines[4])
        assertEquals(DiffLine(DiffLineKind.Removed, "y", 21, null), file.hunks[1].lines[1])
        assertEquals(DiffLine(DiffLineKind.Added, "z", null, 22), file.hunks[1].lines[2])
    }

    @Test
    fun countsAdditionsAndDeletions() {
        val file = parseGitDiff(modify).single()

        assertEquals(3, file.additions)
        assertEquals(2, file.deletions)
    }

    @Test
    fun anAddedFileHasNoOldPath() {
        val patch = """
            diff --git a/New.kt b/New.kt
            new file mode 100644
            index 0000000..3333333
            --- /dev/null
            +++ b/New.kt
            @@ -0,0 +1,2 @@
            +one
            +two
        """.trimIndent()

        val file = parseGitDiff(patch).single()

        assertEquals(DiffFileStatus.Added, file.status)
        assertNull(file.oldPath)
        assertEquals("New.kt", file.newPath)
        assertEquals(listOf(1, 2), file.hunks.single().lines.map { line -> line.newNumber })
    }

    @Test
    fun aDeletedFileKeepsItsOldPathForDisplay() {
        val patch = """
            diff --git a/Gone.kt b/Gone.kt
            deleted file mode 100644
            index 4444444..0000000
            --- a/Gone.kt
            +++ /dev/null
            @@ -1 +0,0 @@
            -bye
        """.trimIndent()

        val file = parseGitDiff(patch).single()

        assertEquals(DiffFileStatus.Deleted, file.status)
        assertNull(file.newPath)
        assertEquals("Gone.kt", file.displayPath)
    }

    @Test
    fun aPureRenameHasBothPathsAndNoHunks() {
        val patch = """
            diff --git a/Old Name.kt b/New Name.kt
            similarity index 100%
            rename from Old Name.kt
            rename to New Name.kt
        """.trimIndent()

        val file = parseGitDiff(patch).single()

        assertEquals(DiffFileStatus.Renamed, file.status)
        assertEquals("Old Name.kt", file.oldPath)
        assertEquals("New Name.kt", file.newPath)
        assertTrue(file.hunks.isEmpty())
    }

    @Test
    fun aBinaryFileIsMarkedInsteadOfLookingEmpty() {
        val patch = """
            diff --git a/logo.png b/logo.png
            index 5555555..6666666 100644
            Binary files a/logo.png and b/logo.png differ
        """.trimIndent()

        val file = parseGitDiff(patch).single()

        assertTrue(file.isBinary)
        assertTrue(file.hunks.isEmpty())
    }

    @Test
    fun aPathWithSpacesIsResolvedFromTheHeaderWhenNothingElseNamesIt() {
        val patch = """
            diff --git a/My Dir/a b.txt b/My Dir/a b.txt
            old mode 100644
            new mode 100755
        """.trimIndent()

        assertEquals("My Dir/a b.txt", parseGitDiff(patch).single().displayPath)
    }

    @Test
    fun theTabGitAddsAfterAPathWithASpaceIsNotPartOfThePath() {
        val patch = "diff --git a/my file.txt b/my file.txt\n--- a/my file.txt\t\n+++ b/my file.txt\t\n@@ -1 +1 @@\n-a\n+b"

        val file = parseGitDiff(patch).single()

        assertEquals("my file.txt", file.oldPath)
        assertEquals("my file.txt", file.newPath)
    }

    @Test
    fun theNoNewlineMarkerIsNotAContentLine() {
        val patch = """
            diff --git a/f b/f
            --- a/f
            +++ b/f
            @@ -1 +1 @@
            -a
            \ No newline at end of file
            +b
        """.trimIndent()

        val lines = parseGitDiff(patch).single().hunks.single().lines

        assertEquals(listOf(DiffLineKind.Removed, DiffLineKind.Added), lines.map { line -> line.kind })
    }

    @Test
    fun aRemovedLineThatLooksLikeAHeaderStaysContent() {
        val patch = """
            diff --git a/f b/f
            --- a/f
            +++ b/f
            @@ -1,2 +1,1 @@
            --- not a header
             keep
        """.trimIndent()

        val file = parseGitDiff(patch).single()

        assertEquals("f", file.displayPath)
        assertEquals(DiffLine(DiffLineKind.Removed, "-- not a header", 1, null), file.hunks.single().lines[0])
    }

    @Test
    fun severalFilesAreSeparated() {
        val files = parseGitDiff(modify + "\n" + "diff --git a/B.kt b/B.kt\n--- a/B.kt\n+++ b/B.kt\n@@ -1 +1 @@\n-a\n+b")

        assertEquals(listOf("src/Main.kt", "B.kt"), files.map { file -> file.displayPath })
    }

    @Test
    fun emptyOutputIsNoFiles() {
        assertTrue(parseGitDiff("").isEmpty())
    }
}
