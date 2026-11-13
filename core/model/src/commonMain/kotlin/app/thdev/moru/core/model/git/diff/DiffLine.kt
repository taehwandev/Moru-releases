package app.thdev.moru.core.model.git.diff

/**
 * One line of a hunk. [oldNumber] is null for an added line and [newNumber] is null for a
 * removed one, so a viewer can show both gutters without guessing.
 */
data class DiffLine(
    val kind: DiffLineKind,
    val text: String,
    val oldNumber: Int?,
    val newNumber: Int?,
)
