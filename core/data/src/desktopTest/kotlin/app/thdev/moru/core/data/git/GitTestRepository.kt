package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitRepositoryState
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class GitTestRepository : AutoCloseable {
    val base: Path = Files.createTempDirectory("moru-git-test-")
    val root: Path = Files.createDirectory(base.resolve("repository"))
    val adapter = DesktopGitRepository()
    val hooks: Path = Files.createDirectory(base.resolve("hooks"))

    init {
        git("init", "--initial-branch=main")
        configure(root)
    }

    fun configure(path: Path) {
        gitAt(path, "config", "user.name", "Git Workbench Test")
        gitAt(path, "config", "user.email", "git-workbench@example.invalid")
        gitAt(path, "config", "commit.gpgSign", "false")
        gitAt(path, "config", "tag.gpgSign", "false")
        gitAt(path, "config", "core.fsmonitor", "false")
        gitAt(path, "config", "core.hooksPath", hooks.toString())
    }
    fun write(name: String, content: String): Path = root.resolve(name).also {
        Files.createDirectories(it.parent)
        Files.writeString(it, content)
    }
    fun commit(content: String = "initial", name: String = "tracked.txt") {
        write(name, content)
        git("add", "--all")
        git("commit", "--message", "Fixture commit")
    }
    fun snapshot(): GitRepositorySnapshot = assertIs<GitRepositoryState.Loaded>(adapter.snapshot(root.toString())).snapshot
    fun execute(action: GitAction): GitActionResult = adapter.execute(root.toString(), snapshot().revision, action)
    fun success(action: GitAction) { assertIs<GitActionResult.Succeeded>(execute(action)) }
    fun git(vararg args: String): String = gitAt(root, *args)
    fun bare(name: String = "remote.git"): Path = Files.createDirectory(base.resolve(name)).also {
        gitAt(it, "init", "--bare", "--initial-branch=main")
    }
    fun clone(remote: Path, name: String = "peer"): Path {
        git("clone", "--", remote.toString(), base.resolve(name).toString())
        return base.resolve(name).also(::configure)
    }

    fun gitAt(path: Path, vararg args: String): String {
        val builder = ProcessBuilder(listOf("git", "--literal-pathspecs", "-C", path.toString()) + args)
            .redirectErrorStream(true)
        builder.environment().keys.filter { it.startsWith("GIT_") }.toList().forEach { builder.environment().remove(it) }
        builder.environment()["GIT_CONFIG_GLOBAL"] = "/dev/null"
        builder.environment()["GIT_CONFIG_SYSTEM"] = "/dev/null"
        builder.environment()["GIT_TERMINAL_PROMPT"] = "0"
        val process = builder.start()
        process.outputStream.close()
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Fixture Git command exceeded its deadline.")
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.exitValue() == 0, "Fixture Git failed: ${args.toList()}\n$output")
        return output.removeSuffix("\n")
    }

    override fun close() {
        Files.walk(base).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
    }
}
