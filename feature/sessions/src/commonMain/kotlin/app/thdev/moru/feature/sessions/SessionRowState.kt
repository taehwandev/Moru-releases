package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.sessions.i18n.SessionsStrings

/** The row's state mark, accessible label, semantic tone, and emphasis. */
internal data class SessionRowState(
    val glyph: MoruStatusGlyphKind,
    val label: String,
    val badgeType: MoruBadgeType,
    val emphasis: SessionRowEmphasis = SessionRowEmphasis.Plain,
)

/**
 * A working row stands out; waiting, idle, and ended sessions fade.
 * Finished responses and states that need the user's attention remain readable.
 */
internal enum class SessionRowEmphasis { Active, Plain, Faded }

/**
 * An agent that stopped printing may have finished its turn; a plain terminal whose program runs
 * silently, such as a server, is running. Every other state is drawn as [SessionActivity] says.
 */
internal fun sessionRowState(
    row: AgentSessionRow,
    strings: SessionsStrings,
    hasPermissionRequest: Boolean = false,
): SessionRowState = when {
    hasPermissionRequest -> SessionRowState(
        MoruStatusGlyphKind.NeedsYou, strings.permissionRequestLabel, MoruBadgeType.WARNING,
    )
    row.awaitsReply -> SessionRowState(
        MoruStatusGlyphKind.NeedsYou, strings.awaitingReplyLabel, MoruBadgeType.WARNING,
    )
    row.finishedTurn -> SessionRowState(
        MoruStatusGlyphKind.Done, strings.activityLabel(SessionActivity.Quiet), MoruBadgeType.SUCCESS,
    )
    row.activity == SessionActivity.Quiet -> SessionRowState(
        MoruStatusGlyphKind.Working, strings.runningProgram, MoruBadgeType.INFO, SessionRowEmphasis.Active,
    )
    else -> row.activity.presentation().let { presentation ->
        SessionRowState(
            row.activity.glyphKind(), strings.activityLabel(row.activity), presentation.badgeType,
            row.activity.emphasis(),
        )
    }
}

private fun SessionActivity.emphasis(): SessionRowEmphasis = when (this) {
    SessionActivity.Starting, SessionActivity.Working -> SessionRowEmphasis.Active
    SessionActivity.Waiting, SessionActivity.Idle, SessionActivity.Exited -> SessionRowEmphasis.Faded
    SessionActivity.Asking, SessionActivity.Quiet, SessionActivity.Failed -> SessionRowEmphasis.Plain
}
