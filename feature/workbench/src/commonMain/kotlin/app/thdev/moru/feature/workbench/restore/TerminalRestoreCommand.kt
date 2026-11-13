package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.feature.workbench.launch.AgentConversation

/**
 * What a terminal runs when it comes back after a restart. An agent resumes its known conversation without a
 * fresh-start fallback or replaying its first request. An agent without a known conversation starts empty;
 * anything else (a build, a dev server) comes back as a plain shell in the same folder.
 */
fun terminalRestoreCommand(startupCommand: String?): String? = AgentConversation.resumeCommandFor(startupCommand)
