package app.thdev.moru.core.domain.files

import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.CodeExplorerSort
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitChange

/** Existing Explorer interpretation only. Callers own metadata reads, cache lifetime and scrolling. */
object ProjectExplorerPolicy {
    enum class Change(val letter: String) { Untracked("U"), Added("A"), Modified("M"), Conflicted("!") }
    data class Projection(val rows: List<WorkbenchProjectTreeEntry>, val pendingFolders: List<String>)

    fun defaultOptions(): CodeExplorerOptions = CodeExplorerOptions()

    /** Existing note-name interpretation; file adapters separately validate paths before writing. */
    fun newNoteRelativePath(rawName: String): String? {
        val normalized = rawName.trim().replace('\\', '/').trim('/')
        if (normalized.isEmpty()) return null
        val segments = normalized.split('/')
        if (segments.any { segment -> segment.isBlank() || segment == "." || segment == ".." }) return null
        return if (segments.last().contains('.')) normalized else "$normalized.md"
    }

    fun isSearchActive(query: String): Boolean = query.isNotBlank()

    /** Preserve Compose's separate arrangement of the initial scan and every loaded folder chunk. */
    fun project(
        entries: List<WorkbenchProjectTreeEntry>, options: CodeExplorerOptions, expanded: Set<String>,
        loaded: Map<String, List<WorkbenchProjectTreeEntry>>,
    ): Projection {
        val arranged = arrange(entries, options)
        val arrangedLoaded = loaded.mapValues { arrange(it.value, options) }
        val rows = visibleRows(arranged, expanded, arrangedLoaded)
        return Projection(rows, deferredFoldersToLoad(rows, expanded, loaded))
    }

    fun arrange(entries: List<WorkbenchProjectTreeEntry>, options: CodeExplorerOptions): List<WorkbenchProjectTreeEntry> {
        if (entries.isEmpty()) return entries
        val base = entries.minOf { it.depth }
        val roots = treeOf(entries)
        val out = mutableListOf<WorkbenchProjectTreeEntry>()
        fun emit(nodes: List<TreeNode>, depth: Int) {
            nodes.filter { options.showIgnored || !it.entry.isIgnored }.sortedWith(order(options)).forEach { node ->
                val merged = if (options.mergeSingleFolders) merge(node, options) else node
                out += merged.entry.copy(depth = depth)
                emit(merged.children, depth + 1)
            }
        }
        emit(roots, base)
        return out
    }

    fun visibleRows(
        entries: List<WorkbenchProjectTreeEntry>, expanded: Set<String>,
        loaded: Map<String, List<WorkbenchProjectTreeEntry>>,
    ): List<WorkbenchProjectTreeEntry> {
        val rows = mutableListOf<WorkbenchProjectTreeEntry>()
        fun walk(level: List<WorkbenchProjectTreeEntry>) {
            var closedDepth: Int? = null
            for (entry in level) {
                val hiddenBelow = closedDepth
                if (hiddenBelow != null && entry.depth > hiddenBelow) continue
                closedDepth = null
                rows += entry
                if (entry.isFile) continue
                when {
                    entry.path !in expanded -> closedDepth = entry.depth
                    entry.isDeferred -> loaded[entry.path]?.let(::walk)
                }
            }
        }
        walk(entries)
        return rows
    }

    /** A present empty chunk is loaded; only an absent key requests another read. */
    fun deferredFoldersToLoad(
        rows: List<WorkbenchProjectTreeEntry>, expanded: Set<String>,
        loaded: Map<String, List<WorkbenchProjectTreeEntry>>,
    ): List<String> = rows.filter { it.isDeferred && it.path in expanded && it.path !in loaded }.map { it.path }

    fun revealFolders(path: String, expanded: Set<String>): Set<String> {
        val parents = generateSequence(path.substringBeforeLast('/', "")) { it.substringBeforeLast('/', "") }
            .takeWhile { it.isNotEmpty() }.toSet()
        return expanded + parents
    }

    fun expandAll(
        entries: List<WorkbenchProjectTreeEntry>, loaded: Map<String, List<WorkbenchProjectTreeEntry>>, expanded: Set<String>,
    ): Set<String> = expanded + (entries + loaded.values.flatten())
        .filter { !it.isFile && !it.isDeferred && !it.isIgnored }.map { it.path }

    fun revealIndex(rows: List<WorkbenchProjectTreeEntry>, path: String): Int {
        val index = rows.indexOfFirst { it.path == path }
        return if (index < 0) -1 else (index - 3).coerceAtLeast(0)
    }

    fun breadcrumbFolders(path: String): List<String> {
        val folders = path.substringBeforeLast('/', "").split('/').filter(String::isNotEmpty)
        return if (folders.size > 4) listOf(folders.first(), "…") + folders.takeLast(2) else folders
    }

    fun changes(entries: List<WorkbenchGitChange>): Map<String, Change> {
        val changes = HashMap<String, Change>()
        for (change in entries) {
            val kind = when {
                change.indexStatus == 'U' || change.worktreeStatus == 'U' -> Change.Conflicted
                change.isUntracked -> Change.Untracked
                change.indexStatus == 'A' -> Change.Added
                change.indexStatus == 'D' || change.worktreeStatus == 'D' -> continue
                else -> Change.Modified
            }
            var path = change.path.trimEnd('/')
            while (path.isNotEmpty()) {
                val previous = changes[path]
                if (previous == null || previous.ordinal < kind.ordinal) changes[path] = kind
                path = path.substringBeforeLast('/', missingDelimiterValue = "")
            }
        }
        return changes
    }

    /** The GIT list uses its original badge order, independently of ancestor-decoration priority. */
    fun gitColorRole(change: WorkbenchGitChange): String = when {
        change.isUntracked -> "primary"
        change.indexStatus == 'D' || change.worktreeStatus == 'D' -> "error"
        change.indexStatus != null -> "success"
        else -> "warning"
    }

    /** Null means Git ignore information was unavailable; an empty collection is an authoritative empty result. */
    fun ignored(path: String, ignoredPaths: Collection<String>?): Boolean {
        if (ignoredPaths == null) return path.split('/').any { it in deferredFolderNames }
        if (path in ignoredPaths || "$path/" in ignoredPaths) return true
        var end = path.indexOf('/')
        while (end >= 0) {
            if (path.substring(0, end + 1) in ignoredPaths) return true
            end = path.indexOf('/', end + 1)
        }
        return false
    }

    fun shouldDefer(name: String, ignored: Boolean, symbolicLink: Boolean, depth: Int, maximumDepth: Int): Boolean =
        ignored || depth >= maximumDepth || name in deferredFolderNames || symbolicLink

    private val deferredFolderNames = setOf(
        ".moru", ".moru-community", ".moru-community-dev", ".gradle", ".idea", ".kotlin", ".venv", "Pods", "build", "dist", "node_modules", "target",
    )
    private class TreeNode(val entry: WorkbenchProjectTreeEntry, val children: MutableList<TreeNode> = mutableListOf())

    private fun treeOf(entries: List<WorkbenchProjectTreeEntry>): List<TreeNode> {
        val roots = mutableListOf<TreeNode>()
        val parents = ArrayDeque<TreeNode>()
        for (entry in entries) {
            while (parents.isNotEmpty() && parents.last().entry.depth >= entry.depth) parents.removeLast()
            val node = TreeNode(entry)
            (parents.lastOrNull()?.children ?: roots) += node
            if (!entry.isFile) parents += node
        }
        return roots
    }

    private fun merge(node: TreeNode, options: CodeExplorerOptions): TreeNode {
        var current = node
        var name = node.entry.name
        while (!current.entry.isFile && !current.entry.isDeferred) {
            val shown = current.children.filter { options.showIgnored || !it.entry.isIgnored }
            val only = shown.singleOrNull() ?: break
            if (only.entry.isFile || only.entry.isDeferred) break
            name += "/" + only.entry.name
            current = only
        }
        return if (current === node) node else TreeNode(current.entry.copy(name = name), current.children)
    }

    private fun order(options: CodeExplorerOptions): Comparator<TreeNode> {
        val byKind = compareBy<TreeNode> { if (options.foldersFirst) it.entry.isFile else false }
        val byType = if (options.sortBy == CodeExplorerSort.Type) {
            byKind.thenBy { if (it.entry.isFile) it.entry.name.substringAfterLast('.', "").lowercase() else "" }
        } else byKind
        return byType.thenBy { it.entry.name.lowercase() }
    }
}
