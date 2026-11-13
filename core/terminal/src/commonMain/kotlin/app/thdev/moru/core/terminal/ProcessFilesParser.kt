package app.thdev.moru.core.terminal

/**
 * Reads `lsof -F pfn` into [ProcessFiles]. In that output a line starting with `f` names a descriptor and the `n` line
 * after it the file it holds; descriptor `cwd` is the process's working folder and a numbered one a file it opened.
 * Every other line is a field Moru does not use.
 */
fun parseLsofProcessFiles(lsofFieldOutput: String): ProcessFiles {
    val folders = LinkedHashSet<String>()
    val files = LinkedHashSet<String>()
    var descriptor = ""
    for (line in lsofFieldOutput.lineSequence()) {
        if (line.length < 2) continue
        when (line[0]) {
            'p' -> descriptor = ""
            'f' -> descriptor = line.substring(1)
            'n' -> when {
                descriptor == "cwd" -> folders += line.substring(1)
                descriptor.firstOrNull()?.isDigit() == true -> files += line.substring(1)
            }
        }
    }
    return ProcessFiles(folders.toList(), files.toList())
}
