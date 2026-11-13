package app.thdev.moru.core.domain.files

import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.CodeExplorerSort
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectExplorerPolicyTest {
    private fun dir(path: String, depth: Int = path.count { it == '/' }, ignored: Boolean = false, deferred: Boolean = false) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, depth, false, ignored, deferred)
    private fun file(path: String, depth: Int = path.count { it == '/' }, ignored: Boolean = false) =
        WorkbenchProjectTreeEntry(path.substringAfterLast('/'), path, depth, true, ignored)
    private val scan = listOf(
        dir("app"), dir("app/src"), dir("app/src/main"), file("app/src/main/Main.kt"),
        file("app/src/main/build.json"), file("app/src/main/Alpha.kt"), dir("build", ignored = true),
        file("build/out.class", ignored = true), dir("node_modules", deferred = true), file("README.md"), file(".gitignore"),
    )
    private val visible = listOf(dir("build", deferred = true), dir("src"), dir("src/main"),
        file("src/main/App.kt"), file("src/README.md"), file("settings.gradle.kts"))
    private fun names(rows: List<WorkbenchProjectTreeEntry>) = rows.map { "  ".repeat(it.depth) + it.name }

    @Test fun defaultsKeepEveryExistingOption() {
        assertEquals(CodeExplorerOptions(false, false, CodeExplorerSort.Name, true, true, false), ProjectExplorerPolicy.defaultOptions())
    }

    @Test fun noteNamesKeepExistingSeparatorTrimmingAndFinalSegmentExtensionRules() {
        val cases = mapOf("Ideas" to "Ideas.md", "notes/todo.txt" to "notes/todo.txt",
            "  notes\\deep\\note  " to "notes/deep/note.md", "/notes/name/" to "notes/name.md",
            "dir.with.dot/note" to "dir.with.dot/note.md", ".hidden" to ".hidden", "note." to "note.",
            "notes /😀" to "notes /😀.md")
        for ((raw, expected) in cases) assertEquals(expected, ProjectExplorerPolicy.newNoteRelativePath(raw))
    }

    @Test fun noteNamesRejectBlankAndDotSegmentsWithoutCollapsingInternalSeparators() {
        for (raw in listOf("", "   ", "/", "\\", ".", "..", "../secret", "notes/../../etc/passwd",
            "notes/./file", "notes//file", "notes/ /file", "notes/\t/file")) {
            assertNull(ProjectExplorerPolicy.newNoteRelativePath(raw))
        }
    }

    @Test fun noteNormalizationRetainsSourceControlCharactersForTheFileAdapterToValidate() {
        assertEquals("note\u0000.md", ProjectExplorerPolicy.newNoteRelativePath("note\u0000"))
        assertEquals("notes/line\nbreak.md", ProjectExplorerPolicy.newNoteRelativePath("notes/line\nbreak"))
        assertEquals("\u0085.md", ProjectExplorerPolicy.newNoteRelativePath("\u0085"))
    }

    @Test fun searchBranchKeepsKotlinBlankClassificationIncludingActiveNel() {
        for (query in listOf("", " ", "\t\r\n", "\u00a0", "\u2003", "\u2028")) {
            assertFalse(ProjectExplorerPolicy.isSearchActive(query))
        }
        for (query in listOf("file", " file ", "\u0085", "\u200b", "\u0000")) {
            assertTrue(ProjectExplorerPolicy.isSearchActive(query))
        }
    }

    @Test fun defaultArrangementKeepsFoldersFirstByNameAndShowsIgnoredEntries() {
        assertEquals(listOf("app", "  src", "    main", "      Alpha.kt", "      build.json", "      Main.kt",
            "build", "  out.class", "node_modules", ".gitignore", "README.md"),
            names(ProjectExplorerPolicy.arrange(scan, CodeExplorerOptions())))
    }

    @Test fun typeSortGroupsExtensionsAndNameSortCanMixFoldersWithFiles() {
        val byType = ProjectExplorerPolicy.arrange(scan, CodeExplorerOptions(sortBy = CodeExplorerSort.Type))
        assertEquals(listOf("app", "  src", "    main", "      build.json", "      Alpha.kt", "      Main.kt"), names(byType).take(6))
        val mixed = ProjectExplorerPolicy.arrange(scan, CodeExplorerOptions(foldersFirst = false))
        assertEquals(listOf(".gitignore", "app", "build", "node_modules", "README.md"), mixed.filter { it.depth == 0 }.map { it.name })
    }

    @Test fun hiddenIgnoredEntriesAndMergedFoldersKeepTheLastFoldersIdentity() {
        val rows = ProjectExplorerPolicy.arrange(scan, CodeExplorerOptions(showIgnored = false, mergeSingleFolders = true))
        assertEquals(listOf("app/src/main", "  Alpha.kt", "  build.json", "  Main.kt", "node_modules", ".gitignore", "README.md"), names(rows))
        assertEquals("app/src/main", rows.first().path)
    }

    @Test fun loadedChildrenKeepTheirStructuralBaseDepth() {
        val children = listOf(file("node_modules/b.js"), dir("node_modules/a"))
        assertEquals(listOf("  a", "  b.js"), names(ProjectExplorerPolicy.arrange(children, CodeExplorerOptions())))
    }

    @Test fun deferredParentsAndChildrenStopMergingEvenWhenALoadedChunkExists() {
        val options = CodeExplorerOptions(mergeSingleFolders = true)
        assertEquals(listOf("a", "  b"), names(ProjectExplorerPolicy.arrange(listOf(dir("a"), dir("a/b", deferred = true)), options)))
        val result = ProjectExplorerPolicy.project(listOf(dir("a", deferred = true)), options, setOf("a", "a/b"),
            mapOf("a" to listOf(dir("a/b"), file("a/b/file.kt"))))
        assertEquals(listOf("a", "  b", "    file.kt"), names(result.rows))
        assertTrue(result.rows.first().isDeferred)
        assertTrue(result.pendingFolders.isEmpty())
    }

    @Test fun ignoringASiblingCanEnableAMergeButNeverLeaksItsDescendants() {
        val entries = listOf(dir("a"), dir("a/ignored", ignored = true), file("a/ignored/child.kt"), dir("a/b"), file("a/b/file.kt"))
        val rows = ProjectExplorerPolicy.arrange(entries, CodeExplorerOptions(showIgnored = false, mergeSingleFolders = true))
        assertEquals(listOf("a/b", "  file.kt"), names(rows))
        assertFalse(rows.any { it.path.startsWith("a/ignored") })
    }

    @Test fun nameComparisonIsLowercaseStableAndLexicalRatherThanNatural() {
        val entries = listOf(file("b.kt"), file("A.kt"), file("a.kt"), file("B.kt"), file("f2.kt"), file("f10.kt"))
        assertEquals(listOf("A.kt", "a.kt", "b.kt", "B.kt", "f10.kt", "f2.kt"),
            ProjectExplorerPolicy.arrange(entries, CodeExplorerOptions()).map { it.name })
        val types = ProjectExplorerPolicy.arrange(listOf(file("b.KT"), dir("z"), file("plain"), file("a.js")),
            CodeExplorerOptions(sortBy = CodeExplorerSort.Type, foldersFirst = false))
        assertEquals(listOf("plain", "z", "a.js", "b.KT"), types.map { it.name })
    }

    @Test fun closedFoldersHideAllDescendants() {
        assertEquals(listOf("build", "src", "settings.gradle.kts"),
            ProjectExplorerPolicy.visibleRows(visible, emptySet(), emptyMap()).map { it.path })
    }

    @Test fun openFoldersShowOnlyTheirOpenDescendants() {
        assertEquals(listOf("build", "src", "src/main", "src/README.md", "settings.gradle.kts"),
            ProjectExplorerPolicy.visibleRows(visible, setOf("src"), emptyMap()).map { it.path })
    }

    @Test fun nestedDeferredFoldersShowLoadedChildrenAndRequestTheUnloadedOnes() {
        val loaded = mapOf("build" to listOf(dir("build/out", deferred = true), file("build/log.txt")))
        val expanded = setOf("build", "build/out")
        val rows = ProjectExplorerPolicy.visibleRows(visible, expanded, loaded)
        assertEquals(listOf("build", "build/out", "build/log.txt", "src", "settings.gradle.kts"), rows.map { it.path })
        assertEquals(listOf("build/out"), ProjectExplorerPolicy.deferredFoldersToLoad(rows, expanded, loaded))
    }

    @Test fun presentEmptyLoadedChunksDoNotRequestAnotherRead() {
        val entries = listOf(dir("build", deferred = true))
        assertEquals(listOf("build"), ProjectExplorerPolicy.project(entries, CodeExplorerOptions(), setOf("build"), emptyMap()).pendingFolders)
        val loaded = ProjectExplorerPolicy.project(entries, CodeExplorerOptions(), setOf("build"), mapOf("build" to emptyList()))
        assertEquals(entries, loaded.rows)
        assertTrue(loaded.pendingFolders.isEmpty())
    }

    @Test fun projectionArrangesEachLoadedChunkBeforeShowingIt() {
        val result = ProjectExplorerPolicy.project(listOf(dir("root", deferred = true)), CodeExplorerOptions(), setOf("root", "root/a"),
            mapOf("root" to listOf(file("root/z.kt"), dir("root/a"), file("root/a/b.kt"))))
        assertEquals(listOf("root", "root/a", "root/a/b.kt", "root/z.kt"), result.rows.map { it.path })
        assertEquals(listOf(0, 1, 2, 1), result.rows.map { it.depth })
    }

    @Test fun revealAddsOnlyAncestorsAndKeepsUnrelatedExpansion() {
        assertEquals(setOf("existing", "a", "a/b"), ProjectExplorerPolicy.revealFolders("a/b/file.kt", setOf("existing")))
        assertEquals(setOf("existing"), ProjectExplorerPolicy.revealFolders("file.kt", setOf("existing")))
    }

    @Test fun expandAllSkipsIgnoredDeferredAndFileRowsButIncludesLoadedKnownFolders() {
        val entries = listOf(dir("a"), dir("ignored", ignored = true), dir("deferred", deferred = true), file("f.kt"))
        val loaded = mapOf("a" to listOf(dir("a/sub"), dir("a/skipped", deferred = true)))
        assertEquals(setOf("existing", "a", "a/sub"), ProjectExplorerPolicy.expandAll(entries, loaded, setOf("existing")))
    }

    @Test fun revealIndexKeepsThreeRowsOfContextAndReportsAbsence() {
        val rows = (0..6).map { file("$it.kt") }
        assertEquals(0, ProjectExplorerPolicy.revealIndex(rows, "1.kt"))
        assertEquals(2, ProjectExplorerPolicy.revealIndex(rows, "5.kt"))
        assertEquals(-1, ProjectExplorerPolicy.revealIndex(rows, "missing.kt"))
    }

    @Test fun breadcrumbsCompactOnlyBeyondFourFolders() {
        assertTrue(ProjectExplorerPolicy.breadcrumbFolders("file.kt").isEmpty())
        assertEquals(listOf("a", "b", "c", "d"), ProjectExplorerPolicy.breadcrumbFolders("a/b/c/d/file.kt"))
        assertEquals(listOf("a", "…", "d", "e"), ProjectExplorerPolicy.breadcrumbFolders("a/b/c/d/e/file.kt"))
        assertEquals(listOf("a", "b"), ProjectExplorerPolicy.breadcrumbFolders("//a///b/file.kt"))
    }

    @Test fun missingIgnoreInformationUsesExactHeavySegmentsButAnEmptyResultDoesNot() {
        for (name in listOf(".moru", ".moru-community", ".gradle", ".idea", ".kotlin", ".venv", "Pods", "build", "dist", "node_modules", "target")) {
            assertTrue(ProjectExplorerPolicy.ignored("a/$name/file", null))
        }
        for (path in listOf("pods/file", "BUILD/file", "node_modules-old/file", "local.properties")) {
            assertFalse(ProjectExplorerPolicy.ignored(path, null))
        }
        assertFalse(ProjectExplorerPolicy.ignored("build/out", emptySet()))
    }

    @Test fun ignoreMatchesExactFilesAndSlashTerminatedAncestors() {
        val ignored = setOf("cache/", "file.txt", "bare")
        for (path in listOf("cache", "cache/deep/file", "file.txt", "bare")) assertTrue(ProjectExplorerPolicy.ignored(path, ignored))
        for (path in listOf("cache2/file", "bare/file", "file.txt/child")) assertFalse(ProjectExplorerPolicy.ignored(path, ignored))
    }

    @Test fun deferDecisionKeepsHeavyNameCaseLinksIgnoreAndDepthBoundaries() {
        assertFalse(ProjectExplorerPolicy.shouldDefer("src", false, false, 15, 16))
        assertTrue(ProjectExplorerPolicy.shouldDefer("src", false, false, 16, 16))
        assertTrue(ProjectExplorerPolicy.shouldDefer("src", true, false, 0, 16))
        assertTrue(ProjectExplorerPolicy.shouldDefer("src", false, true, 0, 16))
        assertTrue(ProjectExplorerPolicy.shouldDefer("Pods", false, false, 0, 16))
        assertFalse(ProjectExplorerPolicy.shouldDefer("pods", false, false, 0, 16))
        assertTrue(ProjectExplorerPolicy.shouldDefer("src", false, false, 0, 0))
    }

    @Test fun ancestorChangesKeepTheStrongestKindTrimTrailingSlashesAndSkipDeletions() {
        val changes = ProjectExplorerPolicy.changes(listOf(WorkbenchGitChange("dir/u", '?', '?'), WorkbenchGitChange("dir/a", 'A', null),
            WorkbenchGitChange("dir/m", null, 'M'), WorkbenchGitChange("dir/c", 'U', 'D'), WorkbenchGitChange("gone", null, 'D'),
            WorkbenchGitChange("trailing/", '?', '?'), WorkbenchGitChange("new/name", 'R', null, "old/name")))
        assertEquals(ProjectExplorerPolicy.Change.Conflicted, changes["dir"])
        assertEquals(ProjectExplorerPolicy.Change.Untracked, changes["dir/u"])
        assertEquals(ProjectExplorerPolicy.Change.Added, changes["dir/a"])
        assertEquals(ProjectExplorerPolicy.Change.Modified, changes["dir/m"])
        assertEquals(ProjectExplorerPolicy.Change.Untracked, changes["trailing"])
        assertEquals(ProjectExplorerPolicy.Change.Modified, changes["new/name"])
        assertFalse("old/name" in changes || "gone" in changes)
    }

    @Test fun additionAndDeletionPriorityDiffersFromTheGitRowColorPriority() {
        val ad = WorkbenchGitChange("ad", 'A', 'D')
        val aa = WorkbenchGitChange("aa", 'A', 'A')
        val dd = WorkbenchGitChange("dd", 'D', 'D')
        val changes = ProjectExplorerPolicy.changes(listOf(ad, aa, dd))
        assertEquals(mapOf("ad" to ProjectExplorerPolicy.Change.Added, "aa" to ProjectExplorerPolicy.Change.Added), changes)
        assertEquals("error", ProjectExplorerPolicy.gitColorRole(ad))
        assertEquals("error", ProjectExplorerPolicy.gitColorRole(dd))
        assertEquals("success", ProjectExplorerPolicy.gitColorRole(aa))
        assertEquals("primary", ProjectExplorerPolicy.gitColorRole(WorkbenchGitChange("u", '?', '?')))
        assertEquals("warning", ProjectExplorerPolicy.gitColorRole(WorkbenchGitChange("m", null, 'M')))
    }
}
