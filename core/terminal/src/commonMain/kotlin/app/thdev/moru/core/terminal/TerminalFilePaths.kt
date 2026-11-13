package app.thdev.moru.core.terminal

/**
 * The spots in [line] that read like file paths, such as `docs/PRD.md`, `~/notes/plan.md`, `/tmp/a.kt:12` or the
 * `src/App.kt` inside Claude Code's `Update(src/App.kt)`. A word only counts with a folder or an extension, and web
 * addresses never count. Whether the file exists is for the caller to check.
 */
fun terminalFilePaths(line: String): List<TerminalFilePath> =
    PathPattern.findAll(line).mapNotNull { match ->
        val path = match.value.trimEnd('.')
        val bare = path.replace(LineSuffix, "")
        val named = '/' in bare || Extension.containsMatchIn(bare)
        if (!named || bare.all { it == '.' || it == '/' || it == '~' }) null
        else TerminalFilePath(match.range.first, match.range.first + path.length, path)
    }.toList()

// Not after a path character, so the tail of a web address (`//host/page`) or of a longer word never starts a path.
private val PathPattern = Regex("""(?<![\w./~:-])(?:~/|/)?[\w.@+-]+(?:/[\w.@+-]+)*/?(?::\d+(?::\d+)?)?""")
private val LineSuffix = Regex(""":\d+(?::\d+)?$""")
private val Extension = Regex("""\.[A-Za-z][A-Za-z0-9]{0,7}$""")
