package app.thdev.moru.core.system

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultFileSharingTest {
    @Test
    fun `unsupported systems do not extract or launch a helper`() {
        withResult { file ->
            assertFalse(launchResultFileShare(file, "Windows 11", { error("must not extract") }, { _, _ -> error("must not launch") }))
            assertFalse(launchResultFileShare(file, "Linux", { error("must not extract") }, { _, _ -> error("must not launch") }))
        }
    }

    @Test
    fun `invalid files missing helpers and process errors allow local save fallback`() {
        withResult { file ->
            assertFalse(launchResultFileShare(File(file.parentFile, "absent.md"), "Mac OS X", { error("must not extract") }, { _, _ -> error("must not launch") }))
            assertFalse(launchResultFileShare(file, "Mac OS X", { null }, { _, _ -> error("must not launch") }))
            assertFalse(launchResultFileShare(file, "Mac OS X", { file }, { _, _ -> throw java.io.IOException("launch failed") }))
        }
    }

    @Test
    fun `only the explicit chooser handshake counts as launched`() {
        withResult { file ->
            val helper = File(file.parentFile, "fake-helper")
            val shown = ShareProcess("shown\n")
            var arguments: Pair<File, File>? = null

            assertTrue(launchResultFileShare(file, "Mac OS X", { helper }) { binary, result ->
                arguments = binary to result
                shown
            })
            assertEquals(helper to file, arguments)
            assertFalse(shown.destroyed)

            val rejected = ShareProcess("unavailable\n")
            assertFalse(launchResultFileShare(file, "Mac OS X", { helper }) { _, _ -> rejected })
            assertTrue(rejected.destroyed)

            val stopped = ShareProcess("")
            assertFalse(launchResultFileShare(file, "Mac OS X", { helper }) { _, _ -> stopped })
            assertTrue(stopped.destroyed)
        }
    }

    private fun withResult(check: (File) -> Unit) {
        val directory = Files.createTempDirectory("result-sharing-test").toFile()
        try {
            check(File(directory, "result with spaces.md").apply { writeText("# Result\n") })
        } finally {
            directory.deleteRecursively()
        }
    }

    /** A local protocol fixture; it never starts a native sharing service. */
    private class ShareProcess(handshake: String) : Process() {
        private val input = ByteArrayInputStream(handshake.toByteArray())
        private val output = ByteArrayOutputStream()
        var destroyed = false
            private set
        override fun getInputStream(): InputStream = input
        override fun getOutputStream(): OutputStream = output
        override fun getErrorStream(): InputStream = ByteArrayInputStream(byteArrayOf())
        override fun waitFor(): Int = 0
        override fun exitValue(): Int = 0
        override fun destroy() { destroyed = true }
    }
}
