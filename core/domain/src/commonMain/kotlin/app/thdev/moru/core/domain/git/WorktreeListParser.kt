package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.CodeWorktree

object WorktreeListParser {
    /** Newline/C-quoted porcelain and `--porcelain -z` raw fields; Git lists the main record first. */
    fun parseWorktreeList(output: String): List<CodeWorktree> = worktreeRecords(output)
        .filter { lines -> lines.any { it.startsWith("worktree ") } }
        .mapIndexedNotNull { index, lines ->
            if ("bare" in lines) return@mapIndexedNotNull null
            val path = worktreePath(lines, '\u0000' in output) ?: return@mapIndexedNotNull null
            CodeWorktree(path, lines.firstOrNull { it.startsWith("branch ") }
                ?.removePrefix("branch ")?.removePrefix("refs/heads/")?.takeIf { it.isNotEmpty() }, index == 0)
        }

    /** Bare and unborn checkouts have no commit to compare. */
    fun parseWorktreeHeads(output: String): List<Pair<String, String>> = worktreeRecords(output).mapNotNull { lines ->
        if ("bare" in lines) return@mapNotNull null
        val path = worktreePath(lines, '\u0000' in output) ?: return@mapNotNull null
        val head = lines.firstOrNull { it.startsWith("HEAD ") }?.removePrefix("HEAD ")?.trim()
        if (head.isNullOrEmpty() || head.all { it == '0' }) null else path to head
    }

    private fun worktreeRecords(output: String): List<List<String>> {
        val separator = if ('\u0000' in output) '\u0000' else '\n'
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        for (field in output.split(separator)) {
            if (field.isEmpty()) { if (fields.isNotEmpty()) records += fields; fields = mutableListOf() }
            else fields += field
        }
        if (fields.isNotEmpty()) records += fields
        return records
    }

    private fun worktreePath(lines: List<String>, raw: Boolean): String? {
        val path = lines.firstOrNull { it.startsWith("worktree ") }?.removePrefix("worktree ") ?: return null
        return if (raw) path.takeIf { it.isNotEmpty() } else GitPathText.decode(path)
    }
}
