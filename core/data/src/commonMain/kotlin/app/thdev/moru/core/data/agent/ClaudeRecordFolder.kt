package app.thdev.moru.core.data.agent

/**
 * The folder the last line of a Claude conversation record works in: Claude writes its current folder on every line, so
 * it names a worktree Claude moved into although Claude's own process stays in the folder it started in.
 */
fun claudeRecordFolder(recordTail: String): String? =
    CwdField.findAll(recordTail).lastOrNull()?.let { match -> unescapeJson(match.groupValues[1]) }
