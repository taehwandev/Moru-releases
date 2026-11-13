package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.lightCkColors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import javax.swing.SwingUtilities

/** Uses a real `sh` PTY: the point is that the same live terminal comes back after its view is gone. */
class JediTermSessionRegistryTest {
    private val palette = TerminalPalette.from(lightCkColors)
    private var created = 0

    private fun acquire(key: String) = JediTermSessionRegistry.acquire(
        key = key,
        initialPalette = palette,
        create = { _, paletteHolder ->
            created += 1
            JediTermSession(
                shell = "/bin/sh",
                workingDirectory = System.getProperty("java.io.tmpdir"),
                startupCommand = null,
                onCommandCaptured = {},
                onOutput = {},
                onExit = {},
                onLocalServerUrl = {},
                onOutputLine = {},
                onProcessStarted = {},
                fontFamily = null,
                fontSize = 13,
                scrollbackLines = 100,
                paletteHolder = paletteHolder,
            )
        },
    )

    @AfterTest
    fun cleanUp() = JediTermSessionRegistry.releaseAll()

    @Test
    fun theSameKeyReturnsTheSameLiveTerminal() {
        val first = acquire("task-1")
        val second = acquire("task-1")

        assertSame(first, second)
        assertSame(1, created)
    }

    @Test
    fun differentKeysGetTheirOwnTerminals() {
        assertNotSame(acquire("task-1"), acquire("task-2"))
    }

    @Test
    fun releasingATerminalMeansTheNextAcquireStartsANewOne() {
        val first = acquire("task-1")
        JediTermSessionRegistry.release("task-1")
        val second = acquire("task-1")

        assertNotSame(first, second)
        assertSame(2, created)
    }

    @Test
    fun releasingAllClosesEveryTerminal() {
        val first = acquire("task-1")
        val other = acquire("task-2")
        JediTermSessionRegistry.releaseAll()

        assertNotSame(first, acquire("task-1"))
        assertNotSame(other, acquire("task-2"))
    }

    @Test
    fun aBlockedCloseDoesNotHoldTheRegistryLockOrTheUiThread() {
        acquire("closing")
        val other = acquire("other")
        val started = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val ended = CountDownLatch(1)
        try {
            SwingUtilities.invokeAndWait {
                JediTermSessionRegistry.release("closing") { session ->
                    started.countDown()
                    try { finish.await(5, TimeUnit.SECONDS) } finally { session.close(); ended.countDown() }
                }
            }
            assertTrue(started.await(2, TimeUnit.SECONDS))
            val inspected = CountDownLatch(1)
            thread(isDaemon = true) {
                assertNull(JediTermSessionRegistry.find("closing"))
                assertSame(other, JediTermSessionRegistry.find("other"))
                inspected.countDown()
            }
            assertTrue(inspected.await(1, TimeUnit.SECONDS), "Closing must not block other sessions")
        } finally {
            finish.countDown()
            assertTrue(ended.await(5, TimeUnit.SECONDS))
        }
    }
}
