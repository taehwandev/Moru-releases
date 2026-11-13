package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.history.GitCommit

/** Strict admission for a complete bounded transport, before using the compatibility log parser. */
object GitLogRecords {
    fun parse(output: String): List<GitCommit> {
        if (output.isEmpty()) return emptyList()
        require('\u0000' !in output) { "Git returned invalid history text." }
        val complete = output.trimEnd('\n')
        require(complete.endsWith('\u001e')) { "Git returned incomplete history." }
        val records = complete.dropLast(1).split('\u001e')
        records.forEach { raw ->
            val fields = raw.trimStart('\n').split('\u001f')
            require(fields.size == 7 && fields.none { '\n' in it || '\r' in it }) {
                "Git returned malformed history."
            }
            require(isObjectID(fields[0]) && (fields[1].isEmpty() ||
                fields[1].split(' ').all(::isObjectID))) { "Git returned invalid commit identity." }
            require(fields[4].toLongOrNull() != null) { "Git returned an invalid commit date." }
        }
        val commits = parseGitLog(output)
        require(commits.size == records.size && commits.map { it.hash }.toSet().size == commits.size) {
            "Git returned duplicate or partial history."
        }
        return commits
    }

    fun isObjectID(value: String): Boolean = value.length in setOf(40, 64) &&
        value.all { it in '0'..'9' || it in 'a'..'f' }
}
