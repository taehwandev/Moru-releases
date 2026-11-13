package app.thdev.moru.core.designsystem.component

/**
 * One entry of a [MoruDropdownMenu]; a [checked] entry is the one currently chosen. An [icon] tells kinds of entries
 * apart at a glance, such as a main checkout and a worktree.
 */
data class MoruMenuItem(
    val label: String,
    val checked: Boolean = false,
    val icon: MoruIconName? = null,
    val onClick: () -> Unit,
)
