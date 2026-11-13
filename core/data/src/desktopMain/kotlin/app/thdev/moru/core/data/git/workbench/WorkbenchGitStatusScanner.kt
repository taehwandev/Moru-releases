package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.data.git.GitProcessRunner
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import java.io.File

internal object WorkbenchGitStatusScanner {
    fun scan(projectRoot: File): WorkbenchGitStatus {
        if (!projectRoot.isDirectory) return WorkbenchGitStatus.NotRepository

        val result = runCatching {
            GitProcessRunner(timeoutMillis = GitStatusTimeoutMillis).run(
                projectRoot.toPath(), listOf("status", "--porcelain=v1", "-b"),
            )
        }.getOrElse { error ->
            return WorkbenchGitStatus(
                repositoryDetected = false,
                branchName = "git unavailable",
                changes = emptyList(),
                message = "Unable to run git: ${error.message ?: "unknown error"}",
            )
        }

        if (result.uncertain) {
            return WorkbenchGitStatus(
                repositoryDetected = false,
                branchName = if (result.exitCode == -1) "git timeout" else "git unavailable",
                changes = emptyList(),
                message = result.stderr,
            )
        }
        if (result.exitCode != 0) {
            return WorkbenchGitStatus.NotRepository.copy(
                message = result.stderr.lineSequence().firstOrNull { line -> line.isNotBlank() }
                    ?: "No git repository detected.",
            )
        }

        return WorkbenchGitStatusPorcelainParser.parse(result.text())
    }

    private const val GitStatusTimeoutMillis = 2_000L
}
