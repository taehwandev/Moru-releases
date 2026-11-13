package app.thdev.moru.core.data

import okio.Path.Companion.toOkioPath
import java.io.File

/** The workbench layout of the desktop app, beside the Project catalog in the app's data folder. */
fun defaultDesktopWorkbenchLayoutRepository(appDataDir: File): FileBackedWorkbenchLayoutRepository {
    val recovery = defaultDesktopTerminalRecoveryStore(appDataDir)
    return FileBackedWorkbenchLayoutRepository(appDataDir.resolve("workbench-layout.json").toOkioPath(), recoveryCommands = recovery::load)
}
