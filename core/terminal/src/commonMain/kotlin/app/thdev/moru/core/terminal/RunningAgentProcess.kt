package app.thdev.moru.core.terminal

/** Ephemeral process metadata used to read an exact native conversation; never persisted or shown as command text. */
class RunningAgentProcess(val pid: Long, val program: String, val arguments: List<String>)
