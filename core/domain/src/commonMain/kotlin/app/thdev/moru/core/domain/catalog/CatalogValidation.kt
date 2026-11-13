package app.thdev.moru.core.domain.catalog

import app.thdev.moru.core.model.wireValue

/** Validation belongs at the catalog boundary, before restored state reaches a UI. */
internal object CatalogValidation {
    private val lineBreaks = Regex("\\s*[\\r\\n]+\\s*")
    const val MaxDescriptionBytes = 65_536

    fun name(value: String, limit: Int): String = truncate(value.replace(lineBreaks, " ").trim(), limit)

    /** Limits count UTF-16 units for parity; a scalar is never split and a cutoff cannot leave trailing whitespace. */
    fun truncate(value: String, limit: Int): String {
        var end = minOf(value.length, limit)
        if (end > 0 && end < value.length && value[end - 1] in '\uD800'..'\uDBFF' && value[end] in '\uDC00'..'\uDFFF') end--
        return value.substring(0, end).trimEnd()
    }

    fun wellFormed(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            when (value[index]) {
                in '\uD800'..'\uDBFF' -> {
                    if (index + 1 == value.length || value[index + 1] !in '\uDC00'..'\uDFFF') return false
                    index += 2
                }
                in '\uDC00'..'\uDFFF' -> return false
                else -> index++
            }
        }
        return true
    }

    fun identity(value: String): Boolean = value.isNotEmpty() && value.none { it.isWhitespace() || it == '\u0000' }

    fun absolutePath(value: String): Boolean = value.startsWith('/') && !value.contains('\u0000') &&
        (value == "/" || value.drop(1).split('/').all { it.isNotEmpty() && it != "." && it != ".." })

    fun check(snapshot: CatalogSnapshot): String? {
        if (snapshot.generation < 0) return "invalid_generation"
        if (snapshot.projects.size > 100 || snapshot.tasks.size > 2_000) return "catalog_limit"
        if (!listOfNotNull(snapshot.selectedProjectId, snapshot.selectedTaskId).all(::wellFormed)) return "invalid_text"
        val identities = mutableSetOf<String>()
        val paths = mutableSetOf<String>()
        var textBytes = 0L
        fun text(vararg values: String?): Boolean {
            for (value in values) {
                if (value != null) textBytes += value.encodeToByteArray().size
                if (textBytes > 4_194_304) return false
            }
            return true
        }
        for (project in snapshot.projects) {
            if (!listOfNotNull(project.id, project.rootId, project.name, project.path, project.lastTaskId).all(::wellFormed)) {
                return "invalid_text"
            }
            if (!identity(project.id) || !identity(project.rootId)) return "invalid_identity"
            if (!identities.add(project.id) || !identities.add(project.rootId)) return "duplicate_identity"
            if (!absolutePath(project.path)) return "invalid_path"
            if (!paths.add(project.path)) return "duplicate_path"
            if (project.name.isEmpty() || project.name.contains('\u0000') || name(project.name, 80) != project.name) {
                return "invalid_name"
            }
            if (project.createdAtMillis < 0 || project.updatedAtMillis < project.createdAtMillis) return "invalid_timestamp"
            if (!text(project.id, project.rootId, project.name, project.path, project.availability.wireValue, project.lastTaskId)) {
                return "text_limit"
            }
        }
        val projects = snapshot.projects.associateBy { it.id }
        for (task in snapshot.tasks) {
            if (!listOf(task.id, task.projectId, task.revisionId, task.title, task.desiredOutcome).all(::wellFormed)) {
                return "invalid_text"
            }
            if (!identity(task.id) || !identity(task.revisionId)) return "invalid_identity"
            if (!identities.add(task.id) || !identities.add(task.revisionId)) return "duplicate_identity"
            if (task.projectId !in projects) return "invalid_owner"
            if (task.title.isEmpty() || task.title.contains('\u0000') || name(task.title, 120) != task.title) return "invalid_name"
            if (task.desiredOutcome.contains('\u0000')) return "invalid_text"
            if (task.desiredOutcome.encodeToByteArray().size > MaxDescriptionBytes) return "description_limit"
            if (task.createdAtMillis < 0 || task.updatedAtMillis < task.createdAtMillis) return "invalid_timestamp"
            if (!text(task.id, task.projectId, task.revisionId, task.title, task.desiredOutcome, task.status.wireValue)) {
                return "text_limit"
            }
        }
        val tasks = snapshot.tasks.associateBy { it.id }
        for (project in snapshot.projects) {
            if (project.lastTaskId != null && tasks[project.lastTaskId]?.projectId != project.id) return "invalid_selection"
        }
        val selected = snapshot.selectedProjectId?.let { projects[it] ?: return "invalid_selection" }
        if (selected?.closed == true) return "invalid_selection"
        snapshot.selectedTaskId?.let { taskId ->
            if (selected == null || tasks[taskId]?.projectId != selected.id || selected.lastTaskId != taskId) {
                return "invalid_selection"
            }
        }
        if (!text(snapshot.selectedProjectId, snapshot.selectedTaskId)) return "text_limit"
        return null
    }
}
