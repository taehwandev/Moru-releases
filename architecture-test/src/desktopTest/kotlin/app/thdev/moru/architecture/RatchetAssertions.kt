package app.thdev.moru.architecture

import kotlin.test.fail

/**
 * Fails on a violation the baseline does not list, and on a baseline entry that
 * no longer occurs. The second half is the ratchet: fixing a violation must
 * also delete its entry, so the baseline only shrinks.
 */
fun assertMatchesBaseline(rule: String, actual: Set<String>, baseline: Set<String>) {
    val added = (actual - baseline).sorted()
    val fixed = (baseline - actual).sorted()
    if (added.isEmpty() && fixed.isEmpty()) return
    fail(
        buildString {
            appendLine("$rule (see desktop-kmp/ARCHITECTURE.md)")
            if (added.isNotEmpty()) {
                appendLine("New violations; fix them instead of adding them to the baseline:")
                added.forEach { appendLine("  \"$it\",") }
            }
            if (fixed.isNotEmpty()) {
                appendLine("Fixed violations; delete these entries from ArchitectureBaseline.kt:")
                fixed.forEach { appendLine("  \"$it\",") }
            }
        },
    )
}
