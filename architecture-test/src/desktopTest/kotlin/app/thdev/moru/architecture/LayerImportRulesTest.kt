package app.thdev.moru.architecture

import java.io.File
import kotlin.test.Test

/**
 * Feature modules own UI only; storage, process execution, app data paths and
 * DI belong to core modules and the app target (ARCHITECTURE.md, Boundary Rules).
 */
class LayerImportRulesTest {
    private val modules = KmpSourceTree.modules()

    @Test
    fun `feature modules do not run processes, read the environment or touch the file system`() {
        val actual = modules.filter { it.kind == "feature" }
            .flatMap { module -> module.violations(FEATURE_FORBIDDEN) }
            .toSet()

        assertMatchesBaseline("Runtime work in a feature module", actual, ArchitectureBaseline.featureRuntimeAccess)
    }

    @Test
    fun `the app target does not run processes itself`() {
        val actual = modules.filter { it.path == ":app-desktop" }
            .flatMap { module -> module.violations(mapOf("ProcessBuilder" to "starts a process")) }
            .toSet()

        assertMatchesBaseline("Process execution in app-desktop", actual, ArchitectureBaseline.appProcessExecution)
    }

    @Test
    fun `Metro is used only by the app target`() {
        val actual = modules.filter { it.path != ":app-desktop" }
            .flatMap { module -> module.violations(mapOf("dev.zacsweers.metro" to "uses Metro")) }
            .toSet()

        assertMatchesBaseline("Metro outside app-desktop", actual, emptySet())
    }

    @Test
    fun `core model imports only Kotlin and its own package`() {
        val model = modules.single { it.path == ":core:model" }
        val actual = model.sourceFiles(mainOnly = true).flatMap { file ->
            IMPORT.findAll(file.readText())
                .map { it.groupValues[1] }
                .filterNot { it.startsWith("kotlin.") || it.startsWith("kotlinx.") || it.startsWith(model.packageRoot) }
                .map { "${model.relative(file)}: imports $it" }
                .toList()
        }.toSet()

        assertMatchesBaseline("Non-Kotlin import in core:model", actual, emptySet())
    }

    private fun KmpModule.violations(forbidden: Map<String, String>): List<String> =
        sourceFiles(mainOnly = true).flatMap { file -> file.violations(this, forbidden) }

    private fun File.violations(module: KmpModule, forbidden: Map<String, String>): List<String> {
        val text = readText()
        return forbidden.filter { (token, _) -> token in text }
            .map { (_, what) -> "${module.relative(this)}: $what" }
    }

    private companion object {
        val IMPORT = Regex("""(?m)^import\s+([\w.]+)""")

        val FEATURE_FORBIDDEN = linkedMapOf(
            "ProcessBuilder" to "starts a process",
            "Runtime.getRuntime()" to "uses Runtime",
            "System.getProperty(" to "reads a system property",
            "System.getenv(" to "reads the environment",
            "import java.io.File\n" to "imports java.io.File",
            "import java.nio.file." to "imports java.nio.file",
            "import app.thdev.moru.core.data." to "imports core:data",
        )
    }
}
