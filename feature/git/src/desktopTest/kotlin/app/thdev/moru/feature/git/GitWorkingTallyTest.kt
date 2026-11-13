package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitWorkingFile
import kotlin.test.Test
import kotlin.test.assertEquals

class GitWorkingTallyTest {
    @Test
    fun countsChangedAddedAndDeletedFilesApartWhetherStagedOrNot() {
        val files = listOf(
            GitWorkingFile("a.kt", ' ', 'M'),
            GitWorkingFile("b.kt", 'M', 'M'),
            GitWorkingFile("c.kt", 'R', ' ', originalPath = "old.kt"),
            GitWorkingFile("conflict.kt", 'U', 'U'),
            GitWorkingFile("new.txt", '?', '?'),
            GitWorkingFile("staged-new.kt", 'A', 'M'),
            GitWorkingFile("gone.kt", 'D', ' '),
            GitWorkingFile("removed.kt", ' ', 'D'),
        )

        assertEquals(GitWorkingTally(modified = 4, added = 2, deleted = 2), gitWorkingTally(files))
    }
}
