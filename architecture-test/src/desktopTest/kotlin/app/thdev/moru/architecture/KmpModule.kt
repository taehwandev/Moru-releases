package app.thdev.moru.architecture

import java.io.File

/** One Gradle module of the desktop-kmp build, read from disk. */
data class KmpModule(val path: String, val dir: File) {
    /** `:core:data` -> `app.thdev.moru.core.data`, the module's package root; `:core:remote-protocol` -> `core.remote.protocol`. */
    val packageRoot: String = "app.thdev.moru." +
        if (path == ":app-desktop") "app" else path.removePrefix(":").replace(':', '.').replace('-', '.')

    val kind: String = path.removePrefix(":").substringBefore(':')

    fun projectDependencies(): List<String> =
        PROJECT_DEPENDENCY.findAll(File(dir, "build.gradle.kts").readText())
            .map { it.groupValues[1] }
            .distinct()
            .toList()

    fun sourceFiles(mainOnly: Boolean): List<File> =
        File(dir, "src").listFiles().orEmpty()
            .filter { it.isDirectory && (!mainOnly || it.name.endsWith("Main")) }
            .flatMap { sourceSet -> sourceSet.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .sortedBy { it.path }

    fun relative(file: File): String = file.relativeTo(KmpSourceTree.root).invariantSeparatorsPath

    private companion object {
        val PROJECT_DEPENDENCY = Regex("""project\("(:[^"]+)"\)""")
    }
}
