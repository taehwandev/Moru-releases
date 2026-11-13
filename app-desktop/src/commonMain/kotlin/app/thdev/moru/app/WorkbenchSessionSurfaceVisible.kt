package app.thdev.moru.app

/** Matches the branches that can render a Task's sessions; Work itself can still be replaced by Code or Git. */
internal fun workbenchSessionSurfaceVisible(
    projectInOwnWindow: Boolean,
    projectAvailable: Boolean,
    taskSelected: Boolean,
    composing: Boolean,
    preparing: Boolean,
    failedLaunch: Boolean,
    workTab: Boolean = true,
): Boolean = !projectInOwnWindow && projectAvailable && taskSelected && !composing && !preparing && !failedLaunch && workTab
