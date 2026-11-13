package app.thdev.moru.core.data.files

import app.thdev.moru.core.data.git.GitProcessRunner
import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import java.io.File
import java.nio.file.Files

/**
 * Reads a project folder as the Code tab lists it. Everything but `.git` is listed; what git ignores is marked so the
 * tree can draw it apart. Ignored folders, dependency and build folders, and symbolic links are listed without their
 * children, which [children] reads when the folder is opened, so a large `node_modules` never crowds out the sources.
 */
internal object WorkbenchProjectTreeScanner {
    fun scan(
        projectRoot: File,
        maxDepth: Int = 64,
        maxEntries: Int = 50_000,
    ): List<WorkbenchProjectTreeEntry> {
        if (!projectRoot.isDirectory) return emptyList()

        val root = projectRoot.canonicalFile
        val ignore = IgnoreRule.read(root, relativeDirectory = "")
        val entries = mutableListOf<WorkbenchProjectTreeEntry>()

        fun visit(directory: File, depth: Int) {
            for (child in directory.sortedChildren()) {
                if (entries.size >= maxEntries) return
                val relativePath = child.relativeTo(root).invariantSeparatorsPath
                val ignored = ignore.matches(relativePath)
                val descend = child.isDirectory && !ProjectExplorerPolicy.shouldDefer(
                    child.name, ignored, Files.isSymbolicLink(child.toPath()), depth, maxDepth,
                )
                entries += WorkbenchProjectTreeEntry(
                    name = child.name,
                    path = relativePath,
                    depth = depth,
                    isFile = child.isFile,
                    isIgnored = ignored,
                    isDeferred = child.isDirectory && !descend,
                )
                if (descend) visit(child, depth + 1)
            }
        }

        visit(root, depth = 0)
        return entries
    }

    /** The direct children of [relativePath]; every child folder is deferred again, one level per opening. */
    fun children(projectRoot: File, relativePath: String): List<WorkbenchProjectTreeEntry> {
        val root = projectRoot.canonicalFile
        val directory = File(root, relativePath).canonicalFile
        if (!directory.isDirectory || !directory.path.startsWith(root.path + File.separator)) return emptyList()
        val parentPath = directory.relativeTo(root).invariantSeparatorsPath
        val ignore = IgnoreRule.read(root, parentPath)
        val depth = parentPath.count { it == '/' } + 1
        return directory.sortedChildren().map { child ->
            val path = "$parentPath/${child.name}"
            WorkbenchProjectTreeEntry(
                name = child.name,
                path = path,
                depth = depth,
                isFile = child.isFile,
                isIgnored = ignore.matches(path),
                isDeferred = child.isDirectory,
            )
        }
    }

    private fun File.sortedChildren(): List<File> =
        listFiles()
            ?.filterNot { child -> child.name == ".git" }
            ?.sortedWith(compareBy<File> { child -> child.isFile }.thenBy { child -> child.name.lowercase() })
            .orEmpty()

    /**
     * What git ignores under a folder, from `git ls-files --ignored --directory`; a path is ignored when it or a
     * folder above it is listed. Outside a repository, the usual build and dependency folders stand in for it.
     */
    private class IgnoreRule(private val ignored: Set<String>?) {
        fun matches(relativePath: String): Boolean = ProjectExplorerPolicy.ignored(relativePath, ignored)

        companion object {
            fun read(root: File, relativeDirectory: String): IgnoreRule {
                val pathspec = if (relativeDirectory.isEmpty()) emptyList() else listOf("--", relativeDirectory)
                val result = runCatching {
                    GitProcessRunner(timeoutMillis = 10_000).run(
                        root.toPath(),
                        listOf("ls-files", "-z", "--others", "--ignored", "--exclude-standard", "--directory") + pathspec,
                    )
                }.getOrNull()
                if (result == null || result.uncertain || result.exitCode != 0) return IgnoreRule(null)
                val paths = result.stdout.toString(Charsets.UTF_8).split('\u0000').filterTo(HashSet()) { it.isNotEmpty() }
                return IgnoreRule(paths)
            }
        }
    }
}
