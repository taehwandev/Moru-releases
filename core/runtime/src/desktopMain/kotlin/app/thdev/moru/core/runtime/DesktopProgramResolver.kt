package app.thdev.moru.core.runtime

import java.io.File

internal fun resolveDesktopProgram(program: String): File? {
    val direct = File(program)
    if (program.contains(File.separatorChar) || direct.isAbsolute) {
        return direct.takeIf { file -> file.isFile && file.canExecute() }
    }

    return desktopProgramSearchDirectories()
        .map { directory -> File(directory, program) }
        .firstOrNull { file -> file.isFile && file.canExecute() }
}

private fun desktopProgramSearchDirectories(): Sequence<File> {
    val home = File(System.getProperty("user.home").orEmpty())
    val pathDirectories = System.getenv("PATH")
        .orEmpty()
        .split(File.pathSeparator)
        .asSequence()
        .filter { path -> path.isNotBlank() }
        .map(::File)

    val fallbackDirectories = sequenceOf(
        File(home, ".local/bin"),
        File(home, "bin"),
        File("/opt/homebrew/bin"),
        File("/usr/local/bin"),
        File("/usr/bin"),
        File("/bin"),
        File("/Applications/cmux.app/Contents/Resources/bin"),
    )

    return (pathDirectories + fallbackDirectories)
        .distinctBy { directory -> directory.absolutePath }
}
