package app.thdev.moru.app

/**
 * Where ⌘D and ⌘T act: the Task whose terminal last took the keyboard ([activeTerminal], its Task and session) while
 * that Task is still on screen, as the selected Task or the one [besideShown]; otherwise the selected Task. The session
 * is the one a split goes beside, null when the app's selected session should be used.
 */
internal data class ShortcutTarget(val taskId: String?, val sessionId: String?) {
    companion object {
        fun of(selectedTaskId: String?, besideShown: String?, activeTerminal: Pair<String, String>?): ShortcutTarget {
            val task = activeTerminal?.first?.takeIf { it == selectedTaskId || it == besideShown } ?: selectedTaskId
            return ShortcutTarget(task, activeTerminal?.takeIf { it.first == task }?.second)
        }
    }
}
