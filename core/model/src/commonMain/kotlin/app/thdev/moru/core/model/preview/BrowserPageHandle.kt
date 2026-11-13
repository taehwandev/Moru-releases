package app.thdev.moru.core.model.preview

/**
 * What the pane around a browser page view can ask of the page it shows. Calls before the page is open, or after it
 * closed, do nothing (a [check] then reports nothing found).
 */
class BrowserPageHandle {
    private var page: Actions? = null

    fun reload() {
        page?.reload()
    }

    /** Puts [text] in any language, Korean included, into the field that has focus in the page. */
    fun insertText(text: String) {
        page?.insertText(text)
    }

    /**
     * Outlines for a moment the elements on the page that came from [files] (paths from the repository root or the
     * dev server's root; a folder ends in "/"), labelled with [label] such as the terminal that changed them.
     */
    fun outline(files: Set<String>, label: String?) {
        page?.outline(files, label)
    }

    /** Keeps the elements of each claimed [areas] outlined as being worked on until the next call; empty clears them. */
    fun showWorking(areas: List<WorkingArea>) {
        page?.showWorking(areas)
    }

    /** Keeps the elements from [files] outlined as changed until the next call; empty clears them. */
    fun showChanges(files: Set<String>) {
        page?.showChanges(files)
    }

    /**
     * Looks, once the page has reloaded after a save, for the elements from [files] and whether the page shows a build
     * error, and reports both to [onResult].
     */
    fun check(files: Set<String>, onResult: (outlines: List<PageOutline>, reloadFailed: Boolean) -> Unit) {
        page?.check(files, onResult) ?: onResult(emptyList(), false)
    }

    /**
     * Lets [actions] answer this handle until the returned closer runs. A later bind replaces it, and closing a
     * replaced binding leaves the later one in place.
     */
    fun bind(actions: Actions): AutoCloseable {
        page = actions
        return AutoCloseable { if (page === actions) page = null }
    }

    /** What the page showing this handle does when asked. */
    interface Actions {
        fun reload()
        fun insertText(text: String)
        fun outline(files: Set<String>, label: String?)
        fun showWorking(areas: List<WorkingArea>)
        fun showChanges(files: Set<String>)
        fun check(files: Set<String>, onResult: (outlines: List<PageOutline>, reloadFailed: Boolean) -> Unit)
    }
}
