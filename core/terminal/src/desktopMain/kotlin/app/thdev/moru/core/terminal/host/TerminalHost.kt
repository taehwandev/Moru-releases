package app.thdev.moru.core.terminal.host

import app.thdev.moru.core.terminal.desktopTerminalEnvironment
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * The process that keeps Moru's terminals running while Moru itself is closed. It owns every PTY, keeps each one's
 * recent output, and serves one connection per open terminal on a Unix socket only this user can reach.
 *
 * A terminal ends when its process exits, when Moru asks for it to be closed, or when nobody has looked at it and it
 * has printed nothing for [abandonedAfterMillis]. The host exits by itself once it has no terminals and no Moru.
 */
class TerminalHost(
    private val socket: Path,
    private val abandonedAfterMillis: Long = 24 * 60 * 60 * 1000L,
    private val idleExitMillis: Long = 60_000L,
    private val replayBytes: Int = 2 * 1024 * 1024,
) {
    private class Hosted(val key: String, val process: PtyProcess, var columns: Int, var rows: Int, replayBytes: Int) {
        val output = OutputRing(replayBytes)
        @Volatile var client: DataOutputStream? = null
        @Volatile var exitCode: Int? = null
        @Volatile var exited = false
        @Volatile var lastActivity = System.currentTimeMillis()

        /** Ended on purpose: nobody needs to come back for its last output. */
        @Volatile var killed = false

        /**
         * Ends the terminal the way closing a terminal window does: a hang-up to everything running in it (the shell
         * and the agent it started), then a forced end for whatever is still there a moment later. An interactive
         * shell ignores a plain terminate request.
         */
        fun kill() {
            killed = true
            val pid = process.pid()
            runCatching { ProcessBuilder("/bin/kill", "-HUP", "--", "-$pid").redirectErrorStream(true).start().waitFor() }
            thread(name = "terminal-host-kill", isDaemon = true) {
                Thread.sleep(1_500)
                ProcessHandle.of(pid).ifPresent { shell ->
                    shell.descendants().forEach { it.destroyForcibly() }
                    shell.destroyForcibly()
                }
            }
        }
    }

    private val sessions = ConcurrentHashMap<String, Hosted>()
    private val connections = ConcurrentHashMap.newKeySet<SocketChannel>()
    @Volatile private var lastConnection = System.currentTimeMillis()

    /** Serves until there is nothing left to serve. */
    fun run() {
        Files.createDirectories(socket.parent)
        // Two Moru instances starting at once may each start a host; the second one leaves the first in charge.
        val alreadyServed = runCatching {
            SocketChannel.open(StandardProtocolFamily.UNIX).use { it.connect(UnixDomainSocketAddress.of(socket)) }
        }.isSuccess
        if (alreadyServed) return
        Files.deleteIfExists(socket)
        val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socket))
        socket.toFile().apply { setReadable(false, false); setWritable(false, false); setReadable(true, true); setWritable(true, true) }
        thread(name = "terminal-host-sweeper", isDaemon = true) { sweep(server) }
        while (server.isOpen) {
            // A failed accept while the server is still open (out of file descriptors, say) is passing; giving up on it
            // would end the host and every program running in it.
            val channel = runCatching { server.accept() }.getOrElse {
                if (!server.isOpen) break
                Thread.sleep(AcceptRetryMillis)
                continue
            }
            connections += channel
            lastConnection = System.currentTimeMillis()
            thread(name = "terminal-host-connection", isDaemon = true) { serve(channel) }
        }
        Files.deleteIfExists(socket)
    }

    private fun serve(channel: SocketChannel) {
        val input = DataInputStream(SocketStreams.input(channel).buffered())
        val output = DataOutputStream(SocketStreams.output(channel).buffered())
        var attached: Hosted? = null
        try {
            while (true) {
                val frame = HostFrames.read(input) ?: break
                when (frame.type) {
                    HostFrames.Ping -> HostFrames.write(output, HostFrames.Pong)
                    HostFrames.KillAll -> sessions.values.forEach { it.kill() }
                    HostFrames.Held -> {
                        val keys = sessions.keys.toList()
                        HostFrames.write(output, HostFrames.HeldKeys, HostFrames.payload { writeInt(keys.size); keys.forEach(::writeUTF) })
                    }
                    HostFrames.Open -> attached = open(frame.payload, output)
                    HostFrames.Input -> attached?.let { session ->
                        session.lastActivity = System.currentTimeMillis()
                        session.process.outputStream.apply { write(frame.payload); flush() }
                    }
                    HostFrames.Resize -> attached?.let { session ->
                        val sizes = DataInputStream(frame.payload.inputStream())
                        resize(session, sizes.readInt(), sizes.readInt())
                    }
                    HostFrames.Kill -> attached?.kill()
                }
            }
        } catch (_: Exception) {
            // A connection that breaks is the same as Moru going away: the terminal keeps running.
        } finally {
            attached?.let { session -> synchronized(session) { if (session.client === output) session.client = null } }
            connections -= channel
            lastConnection = System.currentTimeMillis()
            runCatching { channel.close() }
        }
    }

    /** Attaches to the terminal named in [payload], starting it first if it is not running. */
    private fun open(payload: ByteArray, output: DataOutputStream): Hosted {
        val request = DataInputStream(payload.inputStream())
        val key = request.readUTF()
        val directory = request.readUTF()
        val startupCommand = request.readUTF()
        val columns = request.readInt()
        val rows = request.readInt()
        val command = List(request.readInt()) { request.readUTF() }

        val existing = sessions[key]
        val session = existing ?: start(key, command, directory, startupCommand, columns, rows)
        synchronized(session) {
            session.client?.let { previous -> runCatching { previous.close() } }
            val replay = if (existing != null) session.output.replay() else ByteArray(0)
            HostFrames.write(output, HostFrames.Opened, HostFrames.payload {
                writeBoolean(existing == null)
                writeLong(session.process.pid())
                writeBoolean(replay.isNotEmpty())
            })
            if (replay.isNotEmpty()) HostFrames.write(output, HostFrames.Output, replay)
            if (session.exited) {
                HostFrames.write(output, HostFrames.Exit, HostFrames.payload { writeInt(session.exitCode ?: -1) })
                sessions.remove(key)
            } else {
                session.client = output
            }
        }
        if (existing != null && !session.exited) redraw(session, columns, rows)
        return session
    }

    private fun start(key: String, command: List<String>, directory: String, startupCommand: String, columns: Int, rows: Int): Hosted {
        val process = PtyProcessBuilder()
            .setCommand(command.toTypedArray())
            .setEnvironment(desktopTerminalEnvironment(sessionKey = key))
            .setDirectory(File(directory).absolutePath)
            .setInitialColumns(columns)
            .setInitialRows(rows)
            .start()
        val session = Hosted(key, process, columns, rows, replayBytes)
        sessions[key] = session
        if (startupCommand.isNotBlank()) {
            process.outputStream.write((if (startupCommand.endsWith("\n")) startupCommand else "$startupCommand\n").toByteArray())
            process.outputStream.flush()
        }
        thread(name = "terminal-host-output", isDaemon = true) { pump(session) }
        return session
    }

    private fun pump(session: Hosted) {
        val buffer = ByteArray(64 * 1024)
        val stream = session.process.inputStream
        while (true) {
            val read = runCatching { stream.read(buffer) }.getOrDefault(-1)
            if (read < 0) break
            if (read == 0) continue
            session.lastActivity = System.currentTimeMillis()
            synchronized(session) {
                session.output.append(buffer, 0, read)
                session.client?.let { client ->
                    runCatching { HostFrames.write(client, HostFrames.Output, buffer, 0, read) }
                        .onFailure { session.client = null }
                }
            }
        }
        val code = runCatching { session.process.waitFor() }.getOrNull()
        synchronized(session) {
            session.exitCode = code
            session.exited = true
            val client = session.client
            if (client != null) {
                runCatching { HostFrames.write(client, HostFrames.Exit, HostFrames.payload { writeInt(code ?: -1) }) }
            }
            // Kept only when it ended on its own while Moru was away, so Moru can show how it ended.
            if (client != null || session.killed) sessions.remove(session.key)
        }
    }

    private fun resize(session: Hosted, columns: Int, rows: Int) {
        if (columns <= 0 || rows <= 0) return
        session.columns = columns
        session.rows = rows
        runCatching { session.process.winSize = WinSize(columns, rows) }
    }

    /**
     * Full-screen programs repaint when the size changes. After a reattach at the same size, a one-row wiggle makes
     * them repaint everything, so the replayed screen ends up current.
     */
    private fun redraw(session: Hosted, columns: Int, rows: Int) {
        if (columns != session.columns || rows != session.rows || rows < 2) {
            resize(session, columns, rows)
            return
        }
        resize(session, columns, rows - 1)
        Thread.sleep(40)
        resize(session, columns, rows)
    }

    private fun sweep(server: ServerSocketChannel) {
        while (true) {
            Thread.sleep(10_000)
            val now = System.currentTimeMillis()
            sessions.values.filter { it.client == null && !it.exited && now - it.lastActivity > abandonedAfterMillis }
                .forEach { it.kill() }
            // Ended terminals that nobody came back for within ten minutes are forgotten.
            sessions.values.filter { it.exited && now - it.lastActivity > 10 * 60_000L }.forEach { sessions.remove(it.key) }
            if (sessions.isEmpty() && connections.isEmpty() && now - lastConnection > idleExitMillis) {
                runCatching { server.close() }
                return
            }
        }
    }
}

private const val AcceptRetryMillis = 200L
