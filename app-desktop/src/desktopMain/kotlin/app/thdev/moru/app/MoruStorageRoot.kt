package app.thdev.moru.app

import java.io.File

/** Storage fallback when no actual Project is selected: `~/.moru-community/workspace`. */
internal fun defaultMoruStorageRoot(userHome: File): File =
    userHome.resolve(".moru-community/workspace").absoluteFile
