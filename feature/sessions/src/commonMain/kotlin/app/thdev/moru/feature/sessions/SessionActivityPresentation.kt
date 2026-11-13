package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.model.session.SessionActivity

/**
 * How one [SessionActivity] is drawn; the icon and the state's name carry meaning so color is never
 * alone. The name and the hint for guessed states come from the language tables.
 */
data class SessionActivityPresentation(
    val icon: MoruIconName,
    val badgeType: MoruBadgeType,
    val needsAttention: Boolean,
)
