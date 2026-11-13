package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffLineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitDiffParserTest {
    @Test fun quotedRenameRetainsUtf8OctalAndControlCharacters() {
        val patch = """
            diff --git "a/old\303\251\t\"\\\n.kt" "b/new\360\237\230\200\n.kt"
            similarity index 100%
            rename from "old\303\251\t\"\\\n.kt"
            rename to "new\360\237\230\200\n.kt"
        """.trimIndent()
        val file = parseGitDiff(patch).single()
        assertEquals("oldé\t\"\\\n.kt", file.oldPath)
        assertEquals("new😀\n.kt", file.newPath)
        assertEquals(DiffFileStatus.Renamed, file.status)
        assertTrue(file.hunks.isEmpty())
    }

    @Test fun quotedFileHeadersDecodeLiteralMagicUnicodeAndTab() {
        val patch = """
            diff --git "a/--:(glob)*?[x]한글\t.txt" "b/--:(glob)*?[x]한글\t.txt"
            --- "a/--:(glob)*?[x]한글\t.txt"
            +++ "b/--:(glob)*?[x]한글\t.txt"
            @@ -1 +1 @@
            -old
            +new
        """.trimIndent()
        val file = parseGitDiff(patch).single()
        assertEquals("--:(glob)*?[x]한글\t.txt", file.displayPath)
        assertEquals(listOf(DiffLineKind.Removed, DiffLineKind.Added), file.hunks.single().lines.map { it.kind })
    }

    @Test fun malformedAndOverflowedHunkStartsDoNotCrashOrInventRows() {
        val patch = """
            diff --git a/f b/f
            @@ -999999999999999999999 +1 @@
            +ignored
            @@ malformed @@
            +also ignored
            @@ -1 +1 @@
            +kept
        """.trimIndent()
        val file = parseGitDiff(patch).single()
        assertEquals(1, file.hunks.size)
        assertEquals("kept", file.hunks.single().lines.single().text)
        assertEquals(1, file.hunks.single().lines.single().newNumber)
    }

    @Test fun incrementPastIntMaxNeverWrapsToANegativeLineNumber() {
        val patch = "diff --git a/f b/f\n@@ -2147483647,2 +2147483647,2 @@\n first\n second"
        val lines = parseGitDiff(patch).single().hunks.single().lines
        assertEquals(Int.MAX_VALUE, lines[0].newNumber)
        assertNull(lines[1].newNumber)
        assertNull(lines[1].oldNumber)
    }

    @Test fun patchFinalNewlineIsNotAnExtraContextLine() {
        val patch = "diff --git a/f b/f\n@@ -0,0 +1 @@\n+one\n"
        val lines = parseGitDiff(patch).single().hunks.single().lines
        assertEquals(1, lines.size)
        assertEquals(DiffLineKind.Added, lines.single().kind)
    }

    @Test fun malformedQuotedPathsAndInvalidUtf8OctalAreNotSilentlyRewritten() {
        for (path in listOf("\"a/bad\\q\"", "\"a/bad\\377\"", "\"a/unfinished")) {
            assertTrue(parseGitDiff("diff --git $path \"b/f\"\n").isEmpty())
        }
    }

    @Test fun mixedQuotedRenameHeaderKeepsBothLiteralPaths() {
        val patch = """
            diff --git a/old name "b/new\tname"
            rename from old name
            rename to "new\tname"
            --- a/old name
            +++ "b/new\tname"
        """.trimIndent()
        val file = parseGitDiff(patch).single()
        assertEquals("old name", file.oldPath)
        assertEquals("new\tname", file.newPath)
    }
}
