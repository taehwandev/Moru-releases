package app.thdev.moru.core.data.agent

/** The conversation an AGY process works on, from the log the process holds open: the last one it created or streamed. */
fun agyLogConversation(logText: String): String? =
    AgyConversation.findAll(logText).lastOrNull()?.groupValues?.get(1)

private val AgyConversation =
    Regex("""(?:Created|Streaming) conversation ([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})""")
