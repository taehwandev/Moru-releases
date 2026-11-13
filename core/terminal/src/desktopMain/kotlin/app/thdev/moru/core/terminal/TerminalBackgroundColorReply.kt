package app.thdev.moru.core.terminal

/** The exact RGB in a complete OSC 11 reply, rather than the palette when its query happened. */
internal fun terminalBackgroundColorReply(input: String): Int? {
    if (!input.startsWith("\u001b]11;rgb:")) return null
    val match = BackgroundReply.matchEntire(input) ?: return null
    val channels = (1..3).map { index ->
        val hex = match.groupValues[index]
        val maximum = (1 shl (hex.length * 4)) - 1
        hex.toInt(16) * 255 / maximum
    }
    return (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
}

private val BackgroundReply = Regex("\u001b]11;rgb:([a-fA-F0-9]{1,4})/([a-fA-F0-9]{1,4})/([a-fA-F0-9]{1,4})(?:\u0007|\u001b\\\\)")
