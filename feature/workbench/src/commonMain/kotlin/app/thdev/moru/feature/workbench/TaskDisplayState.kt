package app.thdev.moru.feature.workbench

/**
 * What the Task row shows. It is [app.thdev.moru.core.model.TaskStatus] plus [Unknown], a UI-only state for
 * "we cannot observe this Task right now". [Unknown] is never persisted and never
 * collapses into Done or Failed: an unobservable Task must not look finished.
 */
enum class TaskDisplayState {
    Understanding,
    Preparing,
    Working,
    Checking,
    NeedsInput,
    NeedsApproval,
    Done,
    Waiting,
    Stopped,
    Failed,
    Unknown,
}
