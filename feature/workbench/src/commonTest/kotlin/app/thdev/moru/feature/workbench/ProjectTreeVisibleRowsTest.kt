package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectTreeVisibleRowsTest {
    private fun dir(path: String, deferred: Boolean = false) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, path.count { it == '/' }, isFile = false, isDeferred = deferred)

    private fun file(path: String) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, path.count { it == '/' }, isFile = true)

    private val entries = listOf(
        dir("build", deferred = true),
        dir("src"),
        dir("src/main"),
        file("src/main/App.kt"),
        file("src/README.md"),
        file("settings.gradle.kts"),
    )

    @Test
    fun closedFoldersHideEverythingInside() {
        val rows = visibleProjectTreeRows(entries, expanded = emptySet(), loaded = emptyMap())
        assertEquals(listOf("build", "src", "settings.gradle.kts"), rows.map { it.path })
    }

    @Test
    fun openFolderShowsOnlyItsOpenDescendants() {
        val rows = visibleProjectTreeRows(entries, expanded = setOf("src"), loaded = emptyMap())
        assertEquals(listOf("build", "src", "src/main", "src/README.md", "settings.gradle.kts"), rows.map { it.path })
    }

    @Test
    fun openDeferredFolderShowsItsLoadedChildrenAndAsksForUnloadedOnes() {
        val loaded = mapOf("build" to listOf(dir("build/out", deferred = true), file("build/log.txt")))
        val expanded = setOf("build", "build/out")
        val rows = visibleProjectTreeRows(entries, expanded, loaded)
        assertEquals(listOf("build", "build/out", "build/log.txt", "src", "settings.gradle.kts"), rows.map { it.path })
        assertEquals(listOf("build/out"), deferredFoldersToLoad(rows, expanded, loaded))
    }
}
