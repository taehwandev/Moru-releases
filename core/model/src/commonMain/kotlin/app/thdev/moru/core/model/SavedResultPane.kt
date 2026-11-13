package app.thdev.moru.core.model

import kotlinx.serialization.Serializable

/**
 * A Task's Result pane in a [WorkbenchLayout]: whether the user opened or folded it, the tab they picked (null is
 * "not chosen"), and its width.
 */
@Serializable
data class SavedResultPane(
    val open: Boolean? = null,
    val tab: String? = null,
    /** The Result pane's share (0..1) of the workbench width the user dragged it to; null is the default half. */
    val share: Float? = null,
    /** A Task beside whose Result this Task's pane shows; null shows its own. */
    val showsTask: String? = null,
)
