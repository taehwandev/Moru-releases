package app.thdev.moru.core.terminal.host

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * The wire format between Moru and its terminal host: each frame is a type byte, a length and that many payload
 * bytes. One connection carries one terminal; the first frame names it.
 */
internal object HostFrames {
    // Moru to host.
    const val Open: Byte = 1
    const val Input: Byte = 2
    const val Resize: Byte = 3
    const val Kill: Byte = 4
    const val Ping: Byte = 5
    const val KillAll: Byte = 6
    /** Asks which terminals the host holds; an older host ignores it, so the asker follows it with a [Ping]. */
    const val Held: Byte = 7

    // Host to Moru.
    const val Opened: Byte = 10
    const val Output: Byte = 11
    const val Exit: Byte = 12
    const val Pong: Byte = 13
    const val HeldKeys: Byte = 14

    const val MaxPayload = 8 * 1024 * 1024

    class Frame(val type: Byte, val payload: ByteArray)

    /** The next frame, or null at the end of the stream. */
    fun read(input: DataInputStream): Frame? {
        val type = try {
            input.readByte()
        } catch (end: EOFException) {
            return null
        }
        val length = input.readInt()
        require(length in 0..MaxPayload) { "frame of $length bytes" }
        val payload = ByteArray(length)
        input.readFully(payload)
        return Frame(type, payload)
    }

    fun write(output: DataOutputStream, type: Byte, payload: ByteArray = Empty, offset: Int = 0, length: Int = payload.size) {
        synchronized(output) {
            output.writeByte(type.toInt())
            output.writeInt(length)
            output.write(payload, offset, length)
            output.flush()
        }
    }

    fun payload(build: DataOutputStream.() -> Unit): ByteArray =
        ByteArrayOutputStream().also { bytes -> DataOutputStream(bytes).use(build) }.toByteArray()

    private val Empty = ByteArray(0)
}
