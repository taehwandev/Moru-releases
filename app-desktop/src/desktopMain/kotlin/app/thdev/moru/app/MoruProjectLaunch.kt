package app.thdev.moru.app

import java.io.File

/** Resolved folder-entry intent; it does not register or execute a Project. */
internal data class MoruProjectLaunch(
    val root: File?,
    val explicit: Boolean,
    val notice: String? = null,
)
