package app.thdev.moru.core.terminal.host

import java.nio.file.Path

/** Entry point of the terminal host process: `TerminalHostMain <socket path>`. */
object TerminalHostMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val socket = args.firstOrNull()?.let { Path.of(it) } ?: error("usage: TerminalHostMain <socket>")
        // A session of its own, so closing Moru (or stopping the build that ran it) does not take the terminals along.
        DetachedHostProcess.newSession()
        TerminalHost(socket).run()
        System.exit(0)
    }
}
