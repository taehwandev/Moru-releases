package app.thdev.moru.core.model.session

/** Where a session's Task sits, so a session listed away from its Task can say whose it is. */
data class SessionTaskRef(val taskTitle: String, val projectId: String, val projectName: String)
