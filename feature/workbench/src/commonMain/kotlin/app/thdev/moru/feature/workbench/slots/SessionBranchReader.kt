package app.thdev.moru.feature.workbench.slots

/** The branch terminal [sessionId] works on, given the Task's [folder]; null when it is not in a git checkout. Blocking. */
typealias SessionBranchReader = (sessionId: String, folder: String) -> String?
