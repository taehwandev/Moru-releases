package app.thdev.moru.core.terminal

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreviewHttpProbeTest {
    private fun server(host: InetAddress, answer: (path: String) -> Pair<Int, String?>): HttpServer =
        HttpServer.create(InetSocketAddress(host, 0), 0).apply {
            createContext("/") { request ->
                val (status, type) = answer(request.requestURI.path)
                request.responseHeaders.add("Location", "http://127.0.0.1:1/")
                type?.let { request.responseHeaders.add("Content-Type", it) }
                request.sendResponseHeaders(status, -1)
                request.close()
            }
            start()
        }

    @Test
    fun pagesRedirectsAndErrorPagesAreServedButBareErrorsAreNot() {
        var status = 200
        var type: String? = "text/html; charset=utf-8"
        val server = server(InetAddress.getByName("127.0.0.1")) { status to type }
        try {
            for (code in listOf(200, 204, 302)) {
                status = code
                type = null
                assertTrue(respondsToPreviewHttp(server.address.port), "HTTP $code is a page")
            }
            for (code in listOf(404, 500)) {
                status = code
                type = "text/html"
                assertTrue(respondsToPreviewHttp(server.address.port), "an HTML error $code is a dev server's page")
            }
            // What MCP login callbacks (400) and Codex's own servers (401) answer: errors without a page.
            for (code in listOf(400, 401, 404, 405)) {
                status = code
                type = null
                assertFalse(respondsToPreviewHttp(server.address.port), "a bare $code is not a page")
            }
        } finally { server.stop(0) }
    }

    @Test
    fun theProbeDoesNotAskTheServerToBypassItsCache() {
        var cacheControl: List<String>? = null
        var pragma: List<String>? = null
        val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0).apply {
            createContext("/") { request ->
                cacheControl = request.requestHeaders["Cache-Control"]
                pragma = request.requestHeaders["Pragma"]
                request.sendResponseHeaders(200, -1)
                request.close()
            }
            start()
        }
        try {
            assertTrue(respondsToPreviewHttp(server.address.port))
            // `no-cache` makes a dev server such as Next skip its data cache and redo every read its page does.
            assertNull(cacheControl)
            assertNull(pragma)
        } finally { server.stop(0) }
    }

    @Test
    fun aServerOnlyOnIpv6IsFound() {
        val server = server(InetAddress.getByName("::1")) { 200 to "text/html" }
        try { assertTrue(respondsToPreviewHttp(server.address.port), "a dev server on localhost may take only ::1") }
        finally { server.stop(0) }
    }

    @Test
    fun internalSocketsEmptyRepliesAndSlowListenersAreNotWebServers() {
        for (reply in listOf("", "Gradle daemon", null)) {
            ServerSocket(0).use { server ->
                val responder = thread {
                    server.accept().use { socket ->
                        if (reply == null) Thread.sleep(700)
                        else socket.getOutputStream().write(reply.toByteArray())
                    }
                }
                try { assertFalse(respondsToPreviewHttp(server.localPort), "private reply: $reply") }
                finally { server.close(); responder.join(2000) }
            }
        }
        assertFalse(respondsToPreviewHttp(0))
        assertFalse(respondsToPreviewHttp(65536))
    }
}
