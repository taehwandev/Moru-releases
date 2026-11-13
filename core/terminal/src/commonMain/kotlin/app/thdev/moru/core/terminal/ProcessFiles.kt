package app.thdev.moru.core.terminal

/**
 * What a set of processes has open: [workingFolders] each one works in and [openFiles] they hold, each without repeats
 * in the order printed.
 */
data class ProcessFiles(
    val workingFolders: List<String>,
    val openFiles: List<String>,
)
