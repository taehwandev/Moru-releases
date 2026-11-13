package app.thdev.moru.core.terminal.host

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * Streams over a socket channel that can be read on one thread while another writes. The JDK's `Channels` streams
 * take the channel's blocking lock for the whole of a read, so a write from another thread would wait until output
 * arrived, which in a terminal may be never.
 */
object SocketStreams {
    fun input(channel: SocketChannel): InputStream = object : InputStream() {
        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
        }

        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            return channel.read(ByteBuffer.wrap(target, offset, length))
        }

        override fun close() = channel.close()
    }

    fun output(channel: SocketChannel): OutputStream = object : OutputStream() {
        override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)

        override fun write(source: ByteArray, offset: Int, length: Int) {
            val buffer = ByteBuffer.wrap(source, offset, length)
            while (buffer.hasRemaining()) channel.write(buffer)
        }

        override fun close() = channel.close()
    }
}
