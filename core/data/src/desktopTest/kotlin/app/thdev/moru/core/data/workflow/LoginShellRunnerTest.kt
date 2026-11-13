package app.thdev.moru.core.data.workflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginShellRunnerTest {
    @Test
    fun eachLineIsPassedOnAndTheExitCodeComesBack() {
        val lines = mutableListOf<String>()
        val exit = LoginShellRunner().run("echo one; echo two >&2; exit 3", 30_000) { lines += it }

        assertEquals(3, exit)
        assertTrue(lines.containsAll(listOf("one", "two")), "$lines")
    }

    @Test
    fun aCommandWaitingForInputGetsNoneAndOneThatRunsTooLongIsStopped() {
        // Input is empty, so `read` gives up at once instead of hanging.
        assertEquals(1, LoginShellRunner().run("read answer", 30_000) {})
        assertNull(LoginShellRunner().run("sleep 30", 500) {})
    }

    @Test
    fun aValueWithQuotesStaysOneWord() {
        val lines = mutableListOf<String>()
        LoginShellRunner().run("printf '%s\\n' ${shellQuoted("it's a path")}", 30_000) { lines += it }
        assertEquals(listOf("it's a path"), lines.takeLast(1))
    }
}
