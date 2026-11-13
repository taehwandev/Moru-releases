package app.thdev.moru.feature.workbench.resume

/** What to do with one agent session after its CLI was updated; [cliUpdateRestartStep] decides it. */
enum class CliUpdateRestartStep {
    /** Nothing to do now or ever for this update: not an agent, not updated, or already on the new version. */
    Done,

    /** The agent is busy, asking, or cannot be read yet; look again later. */
    Wait,

    /** The agent is quiet between turns and its own conversation is known: stop it and resume that conversation. */
    RestartNow,

    /** The agent is quiet but its conversation is not known, so a restart could not reopen it: ask the user. */
    AskUser,
}
