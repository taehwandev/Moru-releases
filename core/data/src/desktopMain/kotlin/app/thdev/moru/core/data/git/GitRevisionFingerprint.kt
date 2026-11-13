package app.thdev.moru.core.data.git

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

internal class GitRevisionFingerprint {
    private val digest = MessageDigest.getInstance("SHA-256")
    private val deadline = System.nanoTime() + 15_000_000_000L
    private var remaining = 268_435_456L
    private var entries = 0

    fun read(context: GitRepositoryDiscovery, evidence: GitReadEvidence): Pair<String, String?> {
        add(context.root.toString().toByteArray())
        evidence.values.forEach { (key, value) -> add(key.toByteArray()); add(value) }
        val paths = GitStatusParser.parse(evidence.bytes("status")).flatMap { listOfNotNull(it.path, it.originalPath) }
        paths.distinct().sorted().forEach {
            require(GitStatusParser.validPath(it)) { "Git returned an unsafe file path." }
            var parent = context.root.resolve(it).parent
            while (parent != context.root) {
                require(parent != null && !Files.isSymbolicLink(parent)) { "A changed path traverses a symbolic link. Refresh after resolving it." }
                parent = parent.parent
            }
            tree(context.root.resolve(it), context.root)
        }
        var operation: String? = null
        val markers = listOf("MERGE_HEAD", "REBASE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "BISECT_LOG",
            "rebase-merge", "rebase-apply", "sequencer")
        (listOf("index", "HEAD", "ORIG_HEAD", "FETCH_HEAD", "config.worktree") + markers).forEach { name ->
            val path = context.gitDirectory.resolve(name)
            tree(path, context.gitDirectory)
            if (name in markers && Files.exists(path, NOFOLLOW_LINKS)) operation = name
        }
        tree(context.commonDirectory.resolve("config"), context.commonDirectory)
        return digest.digest().joinToString("") { "%02x".format(it) } to operation
    }

    private fun tree(path: Path, base: Path) {
        bounded()
        if (!Files.exists(path, NOFOLLOW_LINKS)) { add("missing:$path".toByteArray()); return }
        if (!Files.isDirectory(path, NOFOLLOW_LINKS)) { file(path, base); return }
        val paths = mutableListOf<Path>()
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                bounded()
                return if (dir.fileName.toString() == ".git") FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                bounded(); paths.add(file)
                return FileVisitResult.CONTINUE
            }
        })
        paths.sortedBy { it.toString() }.forEach { file(it, base) }
    }

    private fun file(path: Path, base: Path) {
        bounded()
        add(base.relativize(path).toString().toByteArray())
        if (Files.isSymbolicLink(path)) { add(Files.readSymbolicLink(path).toString().toByteArray()); return }
        require(Files.isRegularFile(path, NOFOLLOW_LINKS)) { "Git file discovery includes an unsupported file type." }
        val size = Files.size(path)
        require(size <= remaining) { "Repository content exceeds the safe snapshot limit." }
        add("file:$size".toByteArray())
        Files.newInputStream(path).use {
            val buffer = ByteArray(8_192)
            while (true) {
                bounded()
                val count = it.read(buffer)
                if (count < 0) break
                remaining -= count
                require(remaining >= 0) { "Repository content exceeds the safe snapshot limit." }
                digest.update(buffer, 0, count)
            }
        }
    }

    private fun add(bytes: ByteArray) {
        digest.update(bytes.size.toString().toByteArray()); digest.update(0.toByte()); digest.update(bytes)
    }
    private fun bounded() {
        require(System.nanoTime() < deadline && ++entries <= 200_000) { "Repository snapshot exceeded its safe deadline or file limit." }
    }
}
