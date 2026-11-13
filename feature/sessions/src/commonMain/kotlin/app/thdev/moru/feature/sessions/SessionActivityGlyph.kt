package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.model.session.SessionActivity

/**
 * Which status mark a session is drawn with. Exhaustive over [SessionActivity], so a new state fails compilation
 * until it is given a mark. A still arc means observed activity; an ended program stays neutral because
 * process exit does not prove that a provider turn or Task goal completed.
 */
fun SessionActivity.glyphKind(): MoruStatusGlyphKind =
    when (this) {
        SessionActivity.Starting,
        SessionActivity.Working,
        -> MoruStatusGlyphKind.Working

        SessionActivity.Asking,
        SessionActivity.Quiet,
        -> MoruStatusGlyphKind.NeedsYou

        SessionActivity.Waiting,
        SessionActivity.Idle,
        SessionActivity.Exited,
        -> MoruStatusGlyphKind.Idle
        SessionActivity.Failed -> MoruStatusGlyphKind.Error
    }
