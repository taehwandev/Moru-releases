package app.thdev.moru.core.model

/**
 * One Task-scoped shell descriptor and its honest recovery state. A recreated
 * PTY is labeled [ChildRecoveryState.Recreated], never `Resumed`.
 */
data class TerminalSessionDescriptor(
    val id: String,
    val workContextId: String,
    val title: String,
    val recovery: ChildRecoveryState = ChildRecoveryState.New,
)
