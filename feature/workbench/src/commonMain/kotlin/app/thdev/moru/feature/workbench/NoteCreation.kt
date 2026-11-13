package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.files.ProjectExplorerPolicy

/**
 * Normalize a user-typed note name into a safe project-relative path. Appends
 * `.md` when the final segment has no extension, and rejects blank names or any
 * `.`/`..` path segment. Returns null when the name cannot be used.
 */
fun newNoteRelativePath(rawName: String): String? = ProjectExplorerPolicy.newNoteRelativePath(rawName)
