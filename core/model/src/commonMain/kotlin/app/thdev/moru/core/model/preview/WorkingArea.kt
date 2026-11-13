package app.thdev.moru.core.model.preview

/**
 * Files and folders (a folder ends in "/") one agent claimed, shown on the page as being worked on with [label] such
 * as "Claude working", in the [colorSlot]th color of the Preview's agent colors.
 */
data class WorkingArea(val paths: Set<String>, val label: String, val colorSlot: Int)
