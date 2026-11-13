package app.thdev.moru.core.terminal

import java.io.File

/**
 * The absolute path of the file [path] names on this computer, read from [workingDirectory] when it is relative and
 * from the home folder when it starts with `~/`, with the `:line` or `:line:column` it carried; null when no such
 * file exists.
 */
fun terminalFileTarget(path: String, workingDirectory: String): String? {
    val suffix = LineSuffix.find(path)?.value.orEmpty()
    val bare = path.removeSuffix(suffix)
    val expanded = if (bare.startsWith("~/")) System.getProperty("user.home") + bare.drop(1) else bare
    val file = File(expanded).let { if (it.isAbsolute) it else File(workingDirectory, expanded) }
    return runCatching { file.canonicalFile }.getOrNull()?.takeIf { it.isFile }?.let { it.path + suffix }
}

private val LineSuffix = Regex(""":\d+(?::\d+)?$""")
