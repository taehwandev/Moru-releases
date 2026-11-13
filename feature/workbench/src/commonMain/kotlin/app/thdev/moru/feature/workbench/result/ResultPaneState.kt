package app.thdev.moru.feature.workbench.result

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.model.SavedResultPane

/** Saved per-Task choices, including the pane width, with window placement state local to the open workbench. */
internal class ResultPaneState(
    saved: Map<String, SavedResultPane>,
    /** The tab a Task shows before the user picks one. */
    private val defaultTab: ResultTab = ResultTab.Code,
    lastShare: Float? = null,
) {
    /** The width share last dragged on any Task, which a Task without its own width opens at. */
    var lastShare by mutableStateOf(lastShare)
        private set
    var detached by mutableStateOf(false)
        private set
    private var picked by mutableStateOf(saved.mapNotNull { (task, pane) ->
        pane.tab?.let { name -> ResultTab.entries.firstOrNull { it.name == name } }?.let { task to it }
    }.toMap())
    private var opened by mutableStateOf(saved.mapNotNull { (task, pane) -> pane.open?.let { task to it } }.toMap())
    private var shownTasks by mutableStateOf(saved.mapNotNull { (task, pane) -> pane.showsTask?.let { task to it } }.toMap())
    // A reset is kept as null, not dropped, so the saved layout (merged by Task) forgets the old width too.
    private var shares by mutableStateOf<Map<String, Float?>>(
        saved.mapNotNull { (task, pane) -> pane.share?.let { task to it } }.toMap(),
    )

    val savedPanes: Map<String, SavedResultPane>
        get() = (picked.keys + opened.keys + shares.keys + shownTasks.keys).associateWith { task ->
            SavedResultPane(
                open = opened[task],
                tab = picked[task]?.name,
                share = shares[task],
                showsTask = shownTasks[task],
            )
        }

    fun share(task: String): Float = shares[task] ?: lastShare ?: 0.5f
    fun setShare(task: String, share: Float) {
        shares = shares + (task to share)
        lastShare = share
    }

    /** Back to half, both for [task] and for Tasks that would follow the last dragged width. */
    fun resetShare(task: String) {
        shares = shares + (task to null)
        lastShare = null
    }

    fun selected(task: String): ResultTab = picked[task] ?: defaultTab

    fun isOpen(task: String, available: Set<ResultTab>): Boolean = opened[task] ?: available.isNotEmpty()
    fun select(task: String, tab: ResultTab) { picked = picked + (task to tab) }
    fun setOpen(task: String, open: Boolean) { opened = opened + (task to open) }

    /** The Task whose Result [task]'s pane shows: one beside it, or [task] itself. */
    fun shownTask(task: String): String? = shownTasks[task]
    fun showTask(task: String, shown: String) { shownTasks = if (shown == task) shownTasks - task else shownTasks + (task to shown) }
    fun followAvailable(task: String) { picked = picked - task }

    /** A tool picked on the strip: the open one folds, any other opens; a Result in its own window only switches. */
    fun pick(task: String, tab: ResultTab, available: Set<ResultTab>) {
        if (!detached && isOpen(task, available) && selected(task) == tab) {
            setOpen(task, false)
        } else {
            select(task, tab)
            if (!detached) setOpen(task, true)
        }
    }

    fun detach() { detached = true }

    fun restore(task: String) {
        detached = false
        setOpen(task, true)
    }
}
