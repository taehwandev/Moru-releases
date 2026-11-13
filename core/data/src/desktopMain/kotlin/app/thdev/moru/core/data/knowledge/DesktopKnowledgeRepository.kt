package app.thdev.moru.core.data.knowledge

import app.thdev.moru.core.data.files.DesktopProjectFileRepository
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex
import java.io.File
import java.net.URI
import java.nio.file.Files

/** Read-only, bounded access to documents in an explicitly selected knowledge root. */
class DesktopKnowledgeRepository {
    fun documents(sourceRoot: String): KnowledgeDocumentIndex = try {
        val root = File(sourceRoot).canonicalFile
        if (!root.isDirectory) KnowledgeDocumentIndex(error = "Source folder is unavailable.")
        else scan(root)
    } catch (_: Exception) {
        KnowledgeDocumentIndex(error = "Unable to read source folder.")
    }

    fun open(sourceRoot: String, relativePath: String): WorkbenchFilePreview = try {
        if (!relativePath.isMarkdown()) WorkbenchFilePreview.unavailable(relativePath, "Only Markdown documents can be opened here.")
        else DesktopProjectFileRepository().open(sourceRoot, relativePath)
    } catch (_: Exception) {
        WorkbenchFilePreview.unavailable(relativePath, "Unable to read document.")
    }

    fun openLink(sourceRoot: String, fromPath: String, target: String): WorkbenchFilePreview = try {
        val path = target.substringBefore('#').substringBefore('?')
        val uri = URI(path.replace(" ", "%20"))
        if (uri.isAbsolute || path.startsWith('/')) {
            WorkbenchFilePreview.unavailable(target, "Link is outside this source.")
        } else {
            val decoded = uri.path ?: path
            val parent = File(fromPath).parent.orEmpty()
            val relative = File(parent, decoded).normalize().path
            val candidates = listOf(relative, "$relative.md")
            val direct = candidates.firstOrNull { File(sourceRoot, it).isFile }
            val wiki = if (direct == null && '/' !in decoded) {
                documents(sourceRoot).paths.filter { File(it).nameWithoutExtension == decoded }.singleOrNull()
            } else null
            open(sourceRoot, direct ?: wiki ?: relative)
        }
    } catch (_: Exception) {
        WorkbenchFilePreview.unavailable(target, "Unable to resolve document link.")
    }

    private fun scan(root: File): KnowledgeDocumentIndex {
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)
        val paths = mutableListOf<String>()
        var visited = 0
        var truncated = false
        while (queue.isNotEmpty()) {
            val (folder, depth) = queue.removeFirst()
            Files.newDirectoryStream(folder.toPath()).use { children ->
            for (entry in children) {
                val file = entry.toFile()
                if (++visited > MaxVisited || paths.size >= MaxDocuments) return KnowledgeDocumentIndex(paths.sorted(), truncated = true)
                if (Files.isSymbolicLink(file.toPath())) continue
                if (file.isDirectory) {
                    if (file.name in IgnoredDirectories || (file.name.startsWith('.') && file.name !in InstructionDirectories)) continue
                    if (depth >= MaxDepth) truncated = true else queue.add(file to depth + 1)
                } else if (file.isFile && file.name.isMarkdown() && file.canonicalFile.toPath().startsWith(root.toPath())) {
                    paths.add(file.relativeTo(root).invariantSeparatorsPath)
                }
            }
            }
        }
        return KnowledgeDocumentIndex(paths.sortedWith(compareBy<String> { '/' in it }.thenBy { it }), truncated)
    }

    private fun String.isMarkdown(): Boolean = endsWith(".md", true) || endsWith(".markdown", true)

    private companion object {
        const val MaxDocuments = 500
        const val MaxVisited = 10_000
        const val MaxDepth = 8
        val InstructionDirectories = setOf(".agent", ".agents", ".codex", ".claude", ".system")
        val IgnoredDirectories = setOf("node_modules", "build", "dist", "target", "logs", "sessions", "transcripts", "cache")
    }
}
