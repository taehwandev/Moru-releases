package app.thdev.moru.core.terminal

import app.thdev.moru.core.domain.runtime.TaskSessionCapacity

/** Admission policy for new Task terminals; generic restoration preserves every existing session. */
object TaskTerminalSessionCapacity {
    const val Maximum = TaskSessionCapacity.Maximum

    class LimitReached(val taskKey: String) : IllegalStateException(
        TaskSessionCapacity.LimitMessage,
    )

    fun TerminalSessionManager.canAddTaskTerminalSession(taskKey: String): Boolean =
        TaskSessionCapacity.canAdd(listSessions().count { it.spec.kind.isTaskSession && it.spec.attachedItemId == taskKey })

    internal fun requireAvailable(manager: TerminalSessionManager, spec: TerminalSessionSpec) {
        require(spec.kind.isTaskSession) { "Task admission requires a terminal or Lab session." }
        val taskKey = requireNotNull(spec.attachedItemId) { "Task admission requires an owning Task." }
        if (!manager.canAddTaskTerminalSession(taskKey)) throw LimitReached(taskKey)
    }
}
