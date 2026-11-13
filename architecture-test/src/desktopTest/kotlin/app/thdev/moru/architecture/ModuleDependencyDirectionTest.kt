package app.thdev.moru.architecture

import kotlin.test.Test
import kotlin.test.assertTrue

/** `app-desktop -> feature:* -> core:*`; `core:data -> core:domain -> core:model`. */
class ModuleDependencyDirectionTest {
    private val modules = KmpSourceTree.modules()

    @Test
    fun `the source tree has the modules the rules are written for`() {
        val paths = modules.map { it.path }
        assertTrue(":app-desktop" in paths && ":core:model" in paths && ":feature:workbench" in paths, "$paths")
    }

    @Test
    fun `project dependencies point inward`() {
        val actual = modules.flatMap { module ->
            module.projectDependencies()
                .filterNot { allowedEdge(module.path, it) }
                .map { "${module.path} -> $it" }
        }.toSet()

        assertMatchesBaseline("Forbidden module dependency", actual, ArchitectureBaseline.forbiddenEdges)
    }

    @Test
    fun `every declared project dependency is used`() {
        val byPath = modules.associateBy { it.path }
        val actual = modules.flatMap { module ->
            val sources = module.sourceFiles(mainOnly = false).joinToString("\n") { it.readText() }
            module.projectDependencies()
                .filter { dependency -> byPath[dependency]?.let { "${it.packageRoot}." !in sources } ?: false }
                .map { "${module.path} -> $it" }
        }.toSet()

        assertMatchesBaseline("Declared but unused module dependency", actual, ArchitectureBaseline.unusedEdges)
    }

    private fun allowedEdge(from: String, to: String): Boolean {
        val fromKind = from.removePrefix(":").substringBefore(':')
        val toKind = to.removePrefix(":").substringBefore(':')
        return when {
            to == ":app-desktop" -> false
            from == ":app-desktop" -> toKind == "core" || toKind == "feature"
            fromKind == "feature" -> toKind == "core"
            from == ":core:model" -> false
            from == ":core:domain" -> to == ":core:model"
            from == ":core:data" -> to == ":core:domain" || to == ":core:model"
            // Shared feature UI sees the model and the design system only.
            from == ":core:ui" -> to == ":core:model" || to == ":core:designsystem"
            // Capability modules (terminal, device, emulator, browser, runtime, system) may compose each other,
            // but never reach the storage implementation or any UI module.
            fromKind == "core" -> toKind == "core" && to != ":core:data" && to != ":core:designsystem" && to != ":core:ui"
            else -> false
        }
    }
}
