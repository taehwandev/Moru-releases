package app.thdev.moru.architecture

import java.io.File

/** The desktop-kmp checkout the rules read, passed in by the Gradle test task. */
object KmpSourceTree {
    val root: File = File(
        System.getProperty("moru.kmpRoot") ?: error("moru.kmpRoot is set by architecture-test/build.gradle.kts"),
    )

    /** Every product module listed in settings.gradle.kts, excluding this test module. */
    fun modules(): List<KmpModule> =
        Regex("""include\("(:[^"]+)"\)""")
            .findAll(File(root, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .filter { it != ":architecture-test" }
            .map { path -> KmpModule(path, File(root, path.removePrefix(":").replace(':', '/'))) }
            .toList()
}
