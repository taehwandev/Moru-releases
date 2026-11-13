package app.thdev.moru.core.data.agent

/** A running `codex`: when it started, and the thread its command line names, if any. */
data class CodexProcess(val startedAtMillis: Long, val namedThread: String?)
