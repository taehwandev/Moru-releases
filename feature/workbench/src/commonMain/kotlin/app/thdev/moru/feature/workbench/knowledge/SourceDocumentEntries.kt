package app.thdev.moru.feature.workbench.knowledge

import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

/** Reconstruct the source's folder hierarchy from its bounded document index. */
internal fun sourceDocumentEntries(paths: List<String>): List<WorkbenchProjectTreeEntry> {
    val entries = linkedMapOf<String, WorkbenchProjectTreeEntry>()
    paths.distinct().forEach { path ->
        val segments = path.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return@forEach
        segments.forEachIndexed { depth, name ->
            val key = segments.take(depth + 1).joinToString("/")
            entries[key] = WorkbenchProjectTreeEntry(name, key, depth, isFile = depth == segments.lastIndex)
        }
    }
    val children = entries.values.groupBy { it.path.substringBeforeLast('/', "") }
    val rows = mutableListOf<WorkbenchProjectTreeEntry>()
    fun append(parent: String) {
        children[parent].orEmpty().sortedWith(compareBy<WorkbenchProjectTreeEntry> { it.isFile }.thenBy { it.name.lowercase() }).forEach { entry ->
            rows += entry
            if (!entry.isFile) append(entry.path)
        }
    }
    append("")
    return rows
}
