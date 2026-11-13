package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectContextLineTest {
    @Test
    fun titleNamesTheProjectAndTheSelectedTask() {
        assertEquals("Moru › Fix login", contextTitle("Moru", "Fix login"))
    }

    @Test
    fun titleSaysWhenNoTaskIsSelectedOrNoProjectIsOpen() {
        assertEquals("Moru › Ready for a task", contextTitle("Moru", null))
        assertEquals("No project selected", contextTitle(null, null))
    }

    @Test
    fun aFolderInAGitRepositoryShowsItsBranchAndFolder() {
        assertEquals("Branch main · …/git/Moru", contextDetail("main", "/Users/me/git/Moru"))
    }

    @Test
    fun withoutABranchTheFolderIsTheProjectFolder() {
        assertEquals("Project folder · …/git/Moru", contextDetail(null, "/Users/me/git/Moru"))
    }
}
