package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.launch.AgentConversation

/** How long an agent must have printed nothing before Moru restarts it for an update, so a pause mid-turn is safe. */
private const val UPDATE_RESTART_QUIET_MILLIS: Long = 30_000

/**
 * Decides whether the agent in [snapshot] should be restarted because its CLI was updated at [updatedAtMillis].
 * A session started after the update, or whose shell has no program running ([programRunning] false), already gets
 * the new version on its next start. One that is working, asking, or printed something in the last 30 seconds waits
 * for its turn to end. Only then is it restarted, and only automatically when the resume line names its own
 * conversation; otherwise the user decides.
 */
fun cliUpdateRestartStep(
    snapshot: TerminalSessionSnapshot,
    activity: SessionActivity,
    programRunning: Boolean?,
    updatedAtMillis: Long?,
    nowMillis: Long,
): CliUpdateRestartStep {
    if (updatedAtMillis == null || launchRuntimeOf(snapshot.spec) == null) return CliUpdateRestartStep.Done
    val started = snapshot.startedAtMillis
    if (started != null && started >= updatedAtMillis) return CliUpdateRestartStep.Done
    if (programRunning == false) return CliUpdateRestartStep.Done
    if (programRunning == null) return CliUpdateRestartStep.Wait
    if (activity != SessionActivity.Quiet && activity != SessionActivity.Waiting) return CliUpdateRestartStep.Wait
    val lastOutput = snapshot.lastOutputAtMillis ?: return CliUpdateRestartStep.Wait
    if (nowMillis - lastOutput < UPDATE_RESTART_QUIET_MILLIS) return CliUpdateRestartStep.Wait
    return if (AgentConversation.idIn(snapshot.spec.startupCommand) != null) {
        CliUpdateRestartStep.RestartNow
    } else {
        CliUpdateRestartStep.AskUser
    }
}
