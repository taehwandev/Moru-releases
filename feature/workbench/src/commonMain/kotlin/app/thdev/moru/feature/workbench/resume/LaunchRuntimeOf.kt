package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * The agent a session was started to run, read from the first word of its startup command, or null for a plain
 * shell or any other program. Only an agent that can resume is returned.
 */
fun launchRuntimeOf(spec: TerminalSessionSpec): AgentRuntime? {
    val program = spec.startupCommand?.trim()?.substringBefore(' ')?.substringAfterLast('/')?.lowercase().orEmpty()
    return AgentRuntime.entries.firstOrNull { runtime -> runtime.program.isNotEmpty() && runtime.program == program && runtime.canResume }
}
