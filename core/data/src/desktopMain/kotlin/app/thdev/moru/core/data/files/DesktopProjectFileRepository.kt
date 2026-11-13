package app.thdev.moru.core.data.files

import app.thdev.moru.core.domain.ProjectFileRepository
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import java.io.File

/** Project files on the local disk. */
class DesktopProjectFileRepository : ProjectFileRepository {
    override fun tree(projectRoot: String): List<WorkbenchProjectTreeEntry> =
        WorkbenchProjectTreeScanner.scan(File(projectRoot))

    override fun children(projectRoot: String, relativePath: String): List<WorkbenchProjectTreeEntry> =
        WorkbenchProjectTreeScanner.children(File(projectRoot), relativePath)

    override fun open(projectRoot: String, relativePath: String): WorkbenchFilePreview =
        File(projectRoot).openWorkbenchProjectFilePreview(relativePath)

    override fun save(projectRoot: String, relativePath: String, content: String): WorkbenchFileSaveOutcome =
        File(projectRoot).saveWorkbenchProjectFile(relativePath, content)

    override fun create(projectRoot: String, relativePath: String): WorkbenchFileCreateOutcome =
        File(projectRoot).createWorkbenchProjectFile(relativePath)

    override fun rename(projectRoot: String, fromRelativePath: String, toRelativePath: String): WorkbenchFileRenameOutcome =
        File(projectRoot).renameWorkbenchProjectFile(fromRelativePath, toRelativePath)

    override fun delete(projectRoot: String, relativePath: String): WorkbenchFileDeleteOutcome =
        File(projectRoot).deleteWorkbenchProjectFile(relativePath)
}
