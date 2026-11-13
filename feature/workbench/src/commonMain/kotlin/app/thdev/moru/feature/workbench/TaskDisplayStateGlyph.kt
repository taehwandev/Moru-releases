package app.thdev.moru.feature.workbench

import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind

/**
 * Which status mark a Task state is drawn with. Exhaustive over [TaskDisplayState], so a new state fails
 * compilation until it is given a mark. A Task that was stopped and one whose state could not be observed
 * share the empty ring and are told apart by their text label, never by looking finished.
 */
fun TaskDisplayState.glyphKind(): MoruStatusGlyphKind =
    when (this) {
        TaskDisplayState.Understanding,
        TaskDisplayState.Preparing,
        TaskDisplayState.Working,
        TaskDisplayState.Checking,
        -> MoruStatusGlyphKind.Working

        TaskDisplayState.NeedsInput,
        TaskDisplayState.NeedsApproval,
        -> MoruStatusGlyphKind.NeedsYou

        TaskDisplayState.Done -> MoruStatusGlyphKind.Done
        TaskDisplayState.Failed -> MoruStatusGlyphKind.Error
        TaskDisplayState.Waiting,
        TaskDisplayState.Stopped,
        TaskDisplayState.Unknown,
        -> MoruStatusGlyphKind.Idle
    }
