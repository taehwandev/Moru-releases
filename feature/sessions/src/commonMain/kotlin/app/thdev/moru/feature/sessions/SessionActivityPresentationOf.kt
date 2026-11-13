package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.model.session.SessionActivity

/** Exhaustive over [SessionActivity], so a new state fails compilation until it is drawn. */
fun SessionActivity.presentation(): SessionActivityPresentation =
    when (this) {
        SessionActivity.Starting -> SessionActivityPresentation(
            icon = MoruIconName.Loader, badgeType = MoruBadgeType.NEUTRAL, needsAttention = false,
        )
        SessionActivity.Working -> SessionActivityPresentation(
            icon = MoruIconName.Play, badgeType = MoruBadgeType.INFO, needsAttention = false,
        )
        SessionActivity.Asking -> SessionActivityPresentation(
            icon = MoruIconName.Ellipsis, badgeType = MoruBadgeType.WARNING, needsAttention = true,
        )
        SessionActivity.Quiet -> SessionActivityPresentation(
            icon = MoruIconName.Ellipsis, badgeType = MoruBadgeType.WARNING, needsAttention = true,
        )
        SessionActivity.Waiting -> SessionActivityPresentation(
            icon = MoruIconName.Circle, badgeType = MoruBadgeType.NEUTRAL, needsAttention = false,
        )
        SessionActivity.Idle -> SessionActivityPresentation(
            icon = MoruIconName.Circle, badgeType = MoruBadgeType.NEUTRAL, needsAttention = false,
        )
        SessionActivity.Exited -> SessionActivityPresentation(
            icon = MoruIconName.Square, badgeType = MoruBadgeType.NEUTRAL, needsAttention = true,
        )
        SessionActivity.Failed -> SessionActivityPresentation(
            icon = MoruIconName.Close, badgeType = MoruBadgeType.ERROR, needsAttention = true,
        )
    }
