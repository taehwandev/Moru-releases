package app.thdev.moru.feature.workbench

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruBadgeType

/**
 * Exhaustive over [TaskDisplayState]: a new state fails compilation until it is drawn. The state's
 * name is in the language tables ([app.thdev.moru.feature.workbench.i18n.WorkbenchStrings.statusLabel]).
 */
fun TaskDisplayState.presentation(): TaskStatusPresentation =
    when (this) {
        TaskDisplayState.Understanding -> TaskStatusPresentation(
            icon = MoruIconName.Circle, badgeType = MoruBadgeType.NEUTRAL, group = TaskGroup.Running,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Preparing -> TaskStatusPresentation(
            icon = MoruIconName.Loader, badgeType = MoruBadgeType.NEUTRAL, group = TaskGroup.Running,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Working -> TaskStatusPresentation(
            icon = MoruIconName.Play, badgeType = MoruBadgeType.INFO, group = TaskGroup.Running,
            pulse = true, dashedOutline = false,
        )
        TaskDisplayState.Checking -> TaskStatusPresentation(
            icon = MoruIconName.Target, badgeType = MoruBadgeType.INFO, group = TaskGroup.Running,
            pulse = true, dashedOutline = false,
        )
        TaskDisplayState.NeedsInput -> TaskStatusPresentation(
            icon = MoruIconName.Pencil, badgeType = MoruBadgeType.WARNING, group = TaskGroup.NeedsAttention,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.NeedsApproval -> TaskStatusPresentation(
            icon = MoruIconName.Flag, badgeType = MoruBadgeType.WARNING, group = TaskGroup.NeedsAttention,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Done -> TaskStatusPresentation(
            icon = MoruIconName.Check, badgeType = MoruBadgeType.SUCCESS, group = TaskGroup.Done,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Waiting -> TaskStatusPresentation(
            icon = MoruIconName.Ellipsis, badgeType = MoruBadgeType.NEUTRAL, group = TaskGroup.Done,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Stopped -> TaskStatusPresentation(
            icon = MoruIconName.Square, badgeType = MoruBadgeType.NEUTRAL, group = TaskGroup.Review,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Failed -> TaskStatusPresentation(
            icon = MoruIconName.Close, badgeType = MoruBadgeType.ERROR, group = TaskGroup.NeedsAttention,
            pulse = false, dashedOutline = false,
        )
        TaskDisplayState.Unknown -> TaskStatusPresentation(
            icon = MoruIconName.Question, badgeType = MoruBadgeType.NEUTRAL, group = TaskGroup.NeedsAttention,
            pulse = false, dashedOutline = true,
        )
    }
