package app.thdev.moru.feature.workbench

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruBadgeType

/**
 * How one [TaskDisplayState] is drawn. Meaning is carried by [icon] and the state's name as well as
 * color, so a state is never distinguishable by color alone. The name itself comes from the language
 * tables, so this holds no text a person reads.
 */
data class TaskStatusPresentation(
    val icon: MoruIconName,
    val badgeType: MoruBadgeType,
    val group: TaskGroup,
    val pulse: Boolean,
    val dashedOutline: Boolean,
)
