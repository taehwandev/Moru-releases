package app.thdev.moru.core.data

import java.io.File
import okio.Path.Companion.toOkioPath

/** Keeps filesystem-library types inside the desktop data adapter. */
fun defaultDesktopTerminalRecoveryStore(appDataDir: File): FileTerminalRecoveryStore =
    FileTerminalRecoveryStore(appDataDir.resolve("terminal-recovery.json").toOkioPath())
