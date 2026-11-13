package app.thdev.moru.core.terminal.host

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/**
 * Finds Moru's terminal host, starting it when it is not running, and opens terminals in it. The host is a second
 * copy of this program in its own session, so it outlives Moru; its socket sits in a folder only this user can open.
 */
object TerminalHostLauncher {
    /** Bumped for incompatible frame changes. Optional fields in a framed payload remain backward compatible. */
    private const val ProtocolVersion = 1
    const val HostArgument = "--terminal-host"

    @Volatile private var socket: Path? = null
    // A test points at a host it runs itself; starting a detached one for it would outlive the test run.
    @Volatile private var startsHost = true

    /** Where the host listens, under the app's data folder; set once at startup. Until then terminals run inside Moru. */
    fun configure(appDataDir: File) {
        startsHost = true
        val folder = appDataDir.resolve("run").toPath()
        Files.createDirectories(folder)
        runCatching { Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("rwx------")) }
        socket = socketFor(appDataDir)
    }

    /** True while a host started for [appDataDir] still answers, so its folder must not be moved from under it. */
    fun isRunning(appDataDir: File): Boolean = connect(socketFor(appDataDir))?.also { it.close() } != null

    /**
     * The socket for [appDataDir], in its run folder unless that path is too long for a socket.
     */
    internal fun socketFor(appDataDir: File, tempDir: Path = Path.of(System.getProperty("java.io.tmpdir"))): Path {
        appDataDir.resolve("run").toPath().resolve("terminal-host-$ProtocolVersion.sock")
            .takeIf { it.toString().length < MaxSocketPath }
            ?.let { return it }
        val suffix = "${appDataDir.absolutePath.hashCode()}-$ProtocolVersion.sock"
        return tempDir.resolve("moru-$suffix")
    }

    /** Where the host listens once [configure] ran. */
    fun socketPath(): Path? = socket

    /** Points at a host a test started itself, and never starts a detached one in its place. */
    fun configureSocketForTest(path: Path) {
        socket = path
        startsHost = false
    }

    /**
     * Opens terminal [key] in the host: the one already running under that key, or a new [command] in [directory]
     * that is first sent [startupCommand]. Null when no host could be reached; the caller then runs the terminal itself.
     * A host that was reached but did not answer in time may already be running the program, so it is asked once more
     * for the same key (which then attaches to that program) and, failing that, this throws: running the terminal in Moru
     * as well would start the program twice.
     */
    fun open(
        key: String,
        command: List<String>,
        directory: String,
        startupCommand: String?,
        columns: Int,
        rows: Int,
        onExit: (Int?) -> Unit,
        charset: Charset = StandardCharsets.UTF_8,
    ): HostTtyConnector? {
        val path = socket ?: return null
        fun attempt(channel: SocketChannel): Result<HostTtyConnector> =
            runCatching { handshake(channel, key, command, directory, startupCommand, columns, rows, onExit, charset) }
                .onFailure { runCatching { channel.close() } }
        val first = reach(path) ?: return null
        attempt(first).getOrNull()?.let { return it }
        val again = reach(path) ?: throw IOException("The terminal host stopped answering while it opened $key.")
        return attempt(again).getOrElse { error -> throw IOException("The terminal host did not open $key.", error) }
    }

    private fun reach(path: Path): SocketChannel? = connect(path) ?: synchronized(this) {
        connect(path) ?: run {
            if (startsHost) startHost(path)
            waitForHost(path)
        }
    }

    private fun handshake(
        channel: SocketChannel,
        key: String,
        command: List<String>,
        directory: String,
        startupCommand: String?,
        columns: Int,
        rows: Int,
        onExit: (Int?) -> Unit,
        charset: Charset,
    ): HostTtyConnector = SocketDeadline.run(channel, HandshakeTimeoutMillis) {
        val output = DataOutputStream(SocketStreams.output(channel).buffered())
        HostFrames.write(
            output,
            HostFrames.Open,
            HostFrames.payload {
                writeUTF(key)
                writeUTF(directory)
                writeUTF(startupCommand.orEmpty())
                writeInt(columns)
                writeInt(rows)
                writeInt(command.size)
                command.forEach(::writeUTF)
            },
        )
        val reply = HostFrames.read(DataInputStream(SocketStreams.input(channel)))
        require(reply?.type == HostFrames.Opened) { "the terminal host did not open $key" }
        val opened = DataInputStream(reply.payload.inputStream())
        val created = opened.readBoolean()
        val pid = opened.readLong()
        val hasReplay = if (opened.available() > 0) opened.readBoolean() else !created
        HostTtyConnector(channel, charset, command.joinToString(" "), created, pid, onExit, hasReplay) { size ->
            reattach(key, command, directory, size?.columns ?: columns, size?.rows ?: rows)
        }
    }

    /**
     * Opens [key] again on a new connection for a terminal whose connection was lost, only while the host still holds
     * it: it never starts a host, and never starts a program the host no longer has.
     */
    private fun reattach(key: String, command: List<String>, directory: String, columns: Int, rows: Int): HostTtyConnector.Reattached? {
        if (heldKeys()?.contains(key) != true) return null
        val path = socket ?: return null
        val channel = connect(path) ?: return null
        return runCatching {
            SocketDeadline.run(channel, HandshakeTimeoutMillis) {
                HostFrames.write(
                    DataOutputStream(SocketStreams.output(channel).buffered()),
                    HostFrames.Open,
                    HostFrames.payload {
                        writeUTF(key)
                        writeUTF(directory)
                        writeUTF("")
                        writeInt(columns)
                        writeInt(rows)
                        writeInt(command.size)
                        command.forEach(::writeUTF)
                    },
                )
                val reply = HostFrames.read(DataInputStream(SocketStreams.input(channel)))
                require(reply?.type == HostFrames.Opened) { "the terminal host did not reopen $key" }
                val opened = DataInputStream(reply.payload.inputStream())
                val created = opened.readBoolean()
                opened.readLong()
                HostTtyConnector.Reattached(channel, created, hasReplay = if (opened.available() > 0) opened.readBoolean() else !created)
            }
        }.getOrElse {
            runCatching { channel.close() }
            null
        }
    }

    /**
     * The keys of the terminals the host holds (running, or ended while Moru was away), so Moru can reattach to them at
     * once. Null when no host runs or it is too old to answer. Never starts a host.
     */
    fun heldKeys(): Set<String>? {
        val channel = socket?.let(::connect) ?: return null
        return runCatching {
            channel.use {
                SocketDeadline.run(channel, 2_000) {
                val output = DataOutputStream(SocketStreams.output(channel).buffered())
                HostFrames.write(output, HostFrames.Held)
                // An older host ignores Held and only answers the Ping, so the first reply tells which one this is.
                HostFrames.write(output, HostFrames.Ping)
                val reply = HostFrames.read(DataInputStream(SocketStreams.input(channel)))
                if (reply?.type != HostFrames.HeldKeys) return@run null
                val keys = DataInputStream(reply.payload.inputStream())
                List(keys.readInt()) { keys.readUTF() }.toSet()
                }
            }
        }.getOrNull()
    }

    /**
     * Ends terminal [key] in the host even when Moru never attached to it this run, such as one whose Task was deleted
     * before it was shown or one no terminal of Moru names any more. Only a key the host still holds is opened, so this
     * never starts a program; it works with hosts that predate it, by attaching and sending Kill. False when the host
     * does not hold [key] or cannot be reached.
     */
    fun terminate(key: String): Boolean {
        if (heldKeys()?.contains(key) != true) return false
        val connector = runCatching { open(key, listOf("/bin/sh"), System.getProperty("user.home"), null, 80, 24, onExit = {}) }.getOrNull() ?: return false
        if (connector.created) {
            // It ended between the check and the open, and the host started a fresh shell under the key; end that too.
            connector.terminate()
            return false
        }
        connector.terminate()
        return true
    }

    /** Ends every terminal in the host, for a full quit. */
    fun terminateAll() {
        val channel = socket?.let(::connect) ?: return
        runCatching { SocketDeadline.run(channel) { HostFrames.write(DataOutputStream(SocketStreams.output(channel)), HostFrames.KillAll) } }
        runCatching { channel.close() }
    }

    private fun connect(path: Path): SocketChannel? = DetachedHostProcess.connect(path)

    private fun waitForHost(path: Path): SocketChannel? {
        val deadline = System.currentTimeMillis() + StartTimeoutMillis
        while (System.currentTimeMillis() < deadline) {
            connect(path)?.let { return it }
            Thread.sleep(50)
        }
        return null
    }

    private fun startHost(path: Path) {
        DetachedHostProcess.start(TerminalHostMain::class.java.name, HostArgument, listOf(path.toString()),
            path.resolveSibling("terminal-host.log").toFile())
    }

    private const val StartTimeoutMillis = 8_000L
    private const val HandshakeTimeoutMillis = 5_000L
    private const val MaxSocketPath = 100
}
