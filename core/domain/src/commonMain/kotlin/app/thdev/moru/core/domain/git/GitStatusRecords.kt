package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.GitWorkingFile

/** Portable interpretation of Git porcelain v1 -z records; transport and UTF-8 validation stay with callers. */
object GitStatusRecords {
    fun parseNulTerminated(text: String): List<GitWorkingFile> {
        if (text.isEmpty()) return emptyList()
        require(text.last() == '\u0000') { "Git returned incomplete discovery output." }
        return parse(text.dropLast(1).split('\u0000'))
    }

    fun parse(records: List<String>): List<GitWorkingFile> {
        val result = mutableListOf<GitWorkingFile>()
        var offset = 0
        while (offset < records.size) {
            val record = records[offset++]
            require(record.length >= 4 && record[2] == ' ') { "Git returned malformed file status." }
            require(record.take(2).all { it in " MADRCU?!T" }) { "Git returned unknown file status." }
            val path = record.substring(3)
            val original = if (record.take(2).any { it == 'R' || it == 'C' }) {
                require(offset < records.size) { "Git returned an incomplete rename." }
                records[offset++]
            } else null
            require(validPath(path) && (original == null || validPath(original))) { "Git returned an unsafe file path." }
            result += GitWorkingFile(path, record[0], record[1], original)
        }
        return result
    }

    fun validPath(value: String): Boolean = value.isNotEmpty() && !value.startsWith('/') &&
        value.split('/').none { it == ".." || it == "." || it.isEmpty() } && '\u0000' !in value
}
