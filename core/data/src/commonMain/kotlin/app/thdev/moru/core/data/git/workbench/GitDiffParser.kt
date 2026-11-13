package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.diff.DiffFile

/** Compatibility entry point; unified-patch interpretation is portable domain policy. */
fun parseGitDiff(patch: String): List<DiffFile> = app.thdev.moru.core.domain.git.parseGitDiff(patch)
