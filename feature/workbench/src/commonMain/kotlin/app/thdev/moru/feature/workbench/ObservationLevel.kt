package app.thdev.moru.feature.workbench

/**
 * How a Task's state is being observed, so the UI can say how far to trust it.
 * Any CLI agent can be watched at [Terminal]; richer levels exist only where the
 * agent or Moru Core reports more. The words for each level live in the language tables.
 */
enum class ObservationLevel {
    None,
    Terminal,
    MoruCore,
    AgentSpecific,
}
