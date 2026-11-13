package app.thdev.moru.core.data.agent

import okio.Path.Companion.toPath

/**
 * The folders [command] works in, the last first: those it moves into with `cd`, those it points git at with `git -C`,
 * and the folders of the files a patch in it adds or updates by full path. Relative folders are read from [base].
 * Targets the shell expands (`~`, `$VAR`, `-`) are skipped because only the shell knows them.
 */
internal fun commandFolders(command: String, base: String?): List<String> {
    val moves = FolderTarget.findAll(command).map { match -> match.range.first to match.groupValues[1] }
        .filter { (_, target) -> target.isNotEmpty() && target[0] !in "~$-" }
        .mapNotNull { (at, target) ->
            when {
                target.startsWith("/") -> at to target.toPath().normalized().toString()
                base != null -> at to (base.toPath() / target).normalized().toString()
                else -> null
            }
        }
    val patched = PatchedFile.findAll(command).mapNotNull { match ->
        match.groupValues[1].toPath().normalized().parent?.let { folder -> match.range.first to folder.toString() }
    }
    return (moves + patched).sortedBy { (at, _) -> at }.map { (_, folder) -> folder }.toList().asReversed().distinct()
}

private val FolderTarget = Regex("""(?:^|[\s;&|("'])(?:cd|git\s+-C)\s+(?:\\?["'])?([^\s"'\\;&|)]+)""")
private val PatchedFile = Regex("""\*\*\* (?:Add|Update) File: (/[^\s"'\\]+)""")
