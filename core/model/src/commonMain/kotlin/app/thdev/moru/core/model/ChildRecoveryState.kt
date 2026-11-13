package app.thdev.moru.core.model

/**
 * Honest recovery state for a runtime child. The UI must not claim a terminated
 * process was resumed when it was recreated.
 */
enum class ChildRecoveryState {
    New,
    Recreated,
    Resumed,
    Interrupted,
    RestoreFailed,
}
