package app.thdev.moru.core.terminal.host

import app.thdev.moru.core.terminal.InheritedJavaToolOptions
import app.thdev.moru.core.terminal.JavaToolOptions
import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel
import java.nio.file.Path

/**
 * A helper process of Moru that outlives it, such as the terminal host or the Lab host: a second copy of this program
 * in a session of its own, serving on a Unix socket.
 */
object DetachedHostProcess {
    private interface CLibrary : Library {
        fun setsid(): Int
    }

    /** Called first thing in the helper: a session of its own, so closing Moru (or the build that ran it) leaves it be. */
    fun newSession() {
        runCatching { Native.load("c", CLibrary::class.java).setsid() }
    }

    fun connect(socket: Path): SocketChannel? =
        runCatching { SocketChannel.open(StandardProtocolFamily.UNIX).apply { connect(UnixDomainSocketAddress.of(socket)) } }.getOrNull()

    /** The helper's socket, waited for up to [timeoutMillis] after it was started. */
    fun await(socket: Path, timeoutMillis: Long = 8_000): SocketChannel? {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            connect(socket)?.let { return it }
            Thread.sleep(50)
        }
        return null
    }

    /**
     * Starts the helper [mainClass] with [arguments] from the same code Moru runs: with the JDK's `java` and Moru's
     * class path when they exist (a development run), else through Moru's own launcher with [hostArgument] first (the
     * packaged app has no `java`). Its output goes to [log]; its heap grows to at most [maxHeap].
     */
    fun start(mainClass: String, hostArgument: String, arguments: List<String>, log: File, maxHeap: String = "256m") {
        val options = HostJavaOptions + "-Xmx$maxHeap"
        val javaHome = System.getProperty("java.home")
        val java = File(javaHome, "bin/java")
        val viaJava = java.canExecute()
        val command = if (viaJava) {
            listOf(java.absolutePath) + options + listOf(
                "-Dapple.awt.UIElement=true", "-Djava.awt.headless=true",
                "-cp", System.getProperty("java.class.path"), mainClass,
            ) + arguments
        } else {
            val launcher = ProcessHandle.current().info().command().orElse(null) ?: return
            listOf(launcher, hostArgument) + arguments
        }
        runCatching {
            ProcessBuilder(command)
                .apply {
                    // Moru's launcher takes no JVM options, so the helper gets its small heap from the environment. The
                    // programs it starts get back whatever the user had there.
                    if (!viaJava) environment().apply {
                        put(InheritedJavaToolOptions, remove(JavaToolOptions).orEmpty())
                        put(JavaToolOptions, options.joinToString(" "))
                    }
                }
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                .redirectErrorStream(true)
                .start()
        }
    }

    /**
     * A helper keeps little, but a JVM left to its defaults may grow to a quarter of the machine's memory before it
     * collects what it has already passed on.
     */
    private val HostJavaOptions = listOf("-Xms16m", "-XX:+UseSerialGC")
}
