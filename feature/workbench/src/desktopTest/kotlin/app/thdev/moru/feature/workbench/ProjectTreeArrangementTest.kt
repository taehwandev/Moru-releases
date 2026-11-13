package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.CodeExplorerSort
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectTreeArrangementTest {
    private fun folder(path: String, depth: Int, ignored: Boolean = false, deferred: Boolean = false) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, depth, isFile = false, isIgnored = ignored, isDeferred = deferred)
    private fun file(path: String, depth: Int, ignored: Boolean = false) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, depth, isFile = true, isIgnored = ignored)

    private val scan = listOf(
        folder("app", 0),
        folder("app/src", 1),
        folder("app/src/main", 2),
        file("app/src/main/Main.kt", 3),
        file("app/src/main/build.json", 3),
        file("app/src/main/Alpha.kt", 3),
        folder("build", 0, ignored = true),
        file("build/out.class", 1, ignored = true),
        folder("node_modules", 0, deferred = true),
        file("README.md", 0),
        file(".gitignore", 0),
    )

    private fun names(rows: List<WorkbenchProjectTreeEntry>) = rows.map { "  ".repeat(it.depth) + it.name }

    @Test
    fun theDefaultKeepsFoldersFirstByNameAndShowsIgnoredEntries() {
        assertEquals(
            listOf("app", "  src", "    main", "      Alpha.kt", "      build.json", "      Main.kt",
                "build", "  out.class", "node_modules", ".gitignore", "README.md"),
            names(arrangeProjectTree(scan, CodeExplorerOptions())),
        )
    }

    @Test
    fun sortingByTypeGroupsExtensionsAndFoldersCanMixWithFilesByName() {
        val byType = arrangeProjectTree(scan, CodeExplorerOptions(sortBy = CodeExplorerSort.Type))
        assertEquals(listOf("app", "  src", "    main", "      build.json", "      Alpha.kt", "      Main.kt"), names(byType).take(6))
        val mixed = arrangeProjectTree(scan, CodeExplorerOptions(foldersFirst = false))
        assertEquals(listOf(".gitignore", "app", "build", "node_modules", "README.md"), mixed.filter { it.depth == 0 }.map { it.name })
    }

    @Test
    fun hiddenIgnoredEntriesLeaveAndSingleFolderChainsMergeIntoOneRowForTheLastFolder() {
        val rows = arrangeProjectTree(scan, CodeExplorerOptions(showIgnored = false, mergeSingleFolders = true))
        assertEquals(
            listOf("app/src/main", "  Alpha.kt", "  build.json", "  Main.kt", "node_modules", ".gitignore", "README.md"),
            names(rows),
        )
        assertEquals("app/src/main", rows.first().path, "the merged row opens and closes the last folder")
    }

    @Test
    fun readChildrenOfADeferredFolderKeepTheirDepth() {
        val children = listOf(file("node_modules/b.js", 1), folder("node_modules/a", 1))
        assertEquals(listOf("  a", "  b.js"), names(arrangeProjectTree(children, CodeExplorerOptions())))
    }
}
