package app.thdev.moru.core.terminal

internal const val JavaToolOptions = "JAVA_TOOL_OPTIONS"

/** What [JavaToolOptions] held before Moru set the terminal host's own heap there; empty when it held nothing. */
internal const val InheritedJavaToolOptions = "MORU_INHERITED_JAVA_TOOL_OPTIONS"

/**
 * Markers an agent session stamps on the processes it launches. When Moru itself was started from such a session (a
 * dev build run by Claude Code), every terminal inherited them, and Claude Code started there took itself for a child
 * session and turned transcript saving off. User settings such as `CLAUDE_CODE_USE_BEDROCK` are not markers and stay.
 */
internal val InheritedAgentSessionMarkers = setOf(
    "CLAUDECODE",
    "CLAUDE_CODE_CHILD_SESSION",
    "CLAUDE_CODE_ENTRYPOINT",
    "CLAUDE_CODE_EXECPATH",
    "CLAUDE_CODE_MESSAGING_SOCKET",
    "CLAUDE_CODE_MESSAGING_TOKEN",
    "CLAUDE_CODE_SESSION_ATTENDED",
    "CLAUDE_CODE_SESSION_ID",
    "CLAUDE_EFFORT",
    "CLAUDE_PID",
    "AI_AGENT",
)

/**
 * Names the terminal an agent runs in ([desktopTerminalEnvironment]'s `sessionKey`, the terminal host key), so a Moru
 * Core hook can say which terminal claimed or edited a file. Never inherited from the terminal Moru itself started in.
 */
const val MoruSessionIdVariable = "MORU_SESSION_ID"

/**
 * The environment an agent CLI started by Moru gets: Moru's own, without the markers of a parent agent session, and
 * [MoruSessionIdVariable] set to [sessionKey] when the terminal has one.
 */
fun desktopTerminalEnvironment(
    inheritedEnvironment: Map<String, String> = System.getenv(),
    sessionKey: String? = null,
): Map<String, String> {
    return inheritedEnvironment.toMutableMap().apply {
        keys.removeAll(InheritedAgentSessionMarkers)
        remove(MoruSessionIdVariable)
        sessionKey?.let { put(MoruSessionIdVariable, it) }
        remove("NO_COLOR")
        put("TERM", "xterm-256color")
        put("COLORTERM", "truecolor")
        put("CLICOLOR", "1")
        putIfAbsent("LANG", "en_US.UTF-8")
        remove(InheritedJavaToolOptions)?.let { previous ->
            if (previous.isEmpty()) remove(JavaToolOptions) else put(JavaToolOptions, previous)
        }
    }
}
