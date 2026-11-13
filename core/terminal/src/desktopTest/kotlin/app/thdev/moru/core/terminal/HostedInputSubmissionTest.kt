package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.host.HostFrames
import app.thdev.moru.core.terminal.host.HostTtyConnector
import app.thdev.moru.core.terminal.host.SocketStreams
import java.io.DataInputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HostedInputSubmissionTest {
    @Test
    fun hostedInputPreservesBytesAndOnlyEnterOutsidePasteSubmits() {
        val folder = Files.createTempDirectory("moru-submission").toFile()
        val address = UnixDomainSocketAddress.of(folder.resolve("host.sock").toPath())
        try {
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { server ->
                server.bind(address)
                SocketChannel.open(StandardProtocolFamily.UNIX).use { client ->
                    client.connect(address)
                    server.accept().use { peer ->
                        val input = DataInputStream(SocketStreams.input(peer))
                        val host = HostTtyConnector(client, UTF_8, "test", true, 0, {})
                        var submissions = 0
                        val connector = HostedTtyConnector(host, UTF_8, {}, {}, {}, null,
                            onInputSubmitted = { submissions += 1 })
                        fun write(text: String, expectedSubmissions: Int) {
                            connector.write(text)
                            val frame = assertNotNull(HostFrames.read(input))
                            assertEquals(HostFrames.Input, frame.type)
                            assertEquals(text, frame.payload.toString(UTF_8))
                            assertEquals(expectedSubmissions, submissions)
                        }
                        write("\u001B[I\u001B[O", 0)
                        write("draft", 0)
                        write("\u001B[200~pasted\nparagraph\u001B[201~", 0)
                        write("\r", 1)
                        connector.close()
                    }
                }
            }
        } finally {
            folder.deleteRecursively()
        }
    }
}
