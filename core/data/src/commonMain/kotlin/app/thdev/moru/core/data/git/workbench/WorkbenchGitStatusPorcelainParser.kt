package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.WorkbenchGitChange
import app.thdev.moru.core.model.git.WorkbenchGitStatus

object WorkbenchGitStatusPorcelainParser {
    fun parse(output: String): WorkbenchGitStatus {
        val lines = output.lineSequence()
            .map { line -> line.trimEnd() }
            .filter { line -> line.isNotBlank() }
            .toList()

        val branchName = lines.firstOrNull { line -> line.startsWith("## ") }
            ?.removePrefix("## ")
            ?.substringBefore("...")
            ?.substringBefore(" [")
            ?.takeIf { name -> name.isNotBlank() }
            ?: "unknown"

        val changes = lines
            .filterNot { line -> line.startsWith("## ") }
            .mapNotNull(::parseChangeLine)

        return WorkbenchGitStatus(
            repositoryDetected = true,
            branchName = branchName,
            changes = changes,
            message = if (changes.isEmpty()) "Working tree clean." else null,
        )
    }

    private fun parseChangeLine(line: String): WorkbenchGitChange? {
        if (line.length < 4) return null
        val index = line[0].toStatusOrNull()
        val worktree = line[1].toStatusOrNull()
        val rawPath = line.drop(3).trim()
        if (rawPath.isBlank()) return null

        val originalPath = rawPath.substringBefore(" -> ", missingDelimiterValue = "")
            .takeIf { path -> path.isNotBlank() && " -> " in rawPath }
        val path = rawPath.substringAfter(" -> ", missingDelimiterValue = rawPath)

        return WorkbenchGitChange(
            path = path,
            indexStatus = index,
            worktreeStatus = worktree,
            originalPath = originalPath,
        )
    }

    private fun Char.toStatusOrNull(): Char? {
        return if (this == ' ') null else this
    }
}
