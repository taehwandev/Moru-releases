package app.thdev.moru.core.domain

import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

/**
 * The files of a project folder as the Workbench reads and edits them. Relative paths must resolve
 * inside [projectRoot]; anything outside it fails instead of touching the disk. Call off the UI thread.
 */
interface ProjectFileRepository {
    fun tree(projectRoot: String): List<WorkbenchProjectTreeEntry>

    /** The direct children of a folder [tree] left deferred, so a large or ignored folder is read only when opened. */
    fun children(projectRoot: String, relativePath: String): List<WorkbenchProjectTreeEntry>

    fun open(projectRoot: String, relativePath: String): WorkbenchFilePreview

    fun save(projectRoot: String, relativePath: String, content: String): WorkbenchFileSaveOutcome

    fun create(projectRoot: String, relativePath: String): WorkbenchFileCreateOutcome

    fun rename(projectRoot: String, fromRelativePath: String, toRelativePath: String): WorkbenchFileRenameOutcome

    fun delete(projectRoot: String, relativePath: String): WorkbenchFileDeleteOutcome
}
