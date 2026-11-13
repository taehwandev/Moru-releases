package app.thdev.moru.core.model

/**
 * One Task-scoped agent session descriptor. A provider-native resume locator is
 * opaque and scoped to this Work Context; it is only [ChildRecoveryState.Resumed]
 * after the provider confirms the locator.
 */
data class AgentSessionDescriptor(
    val id: String,
    val workContextId: String,
    val providerResumeLocator: String? = null,
    val recovery: ChildRecoveryState = ChildRecoveryState.New,
)
