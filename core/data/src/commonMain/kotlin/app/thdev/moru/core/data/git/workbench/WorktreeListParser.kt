package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.CodeWorktree

internal fun parseWorktreeList(output: String): List<CodeWorktree> = app.thdev.moru.core.domain.git.WorktreeListParser.parseWorktreeList(output)

internal fun parseWorktreeHeads(output: String): List<Pair<String, String>> = app.thdev.moru.core.domain.git.WorktreeListParser.parseWorktreeHeads(output)
