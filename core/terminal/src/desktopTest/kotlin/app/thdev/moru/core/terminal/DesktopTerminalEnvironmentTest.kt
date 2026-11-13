package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopTerminalEnvironmentTest {
    @Test
    fun terminalsDoNotInheritTheHostsOwnHeapOptions() {
        val environment = desktopTerminalEnvironment(
            mapOf(JavaToolOptions to "-Xmx256m -XX:+UseSerialGC", InheritedJavaToolOptions to ""),
        )
        assertFalse(JavaToolOptions in environment)
        assertFalse(InheritedJavaToolOptions in environment)
    }

    @Test
    fun terminalsGetBackTheUsersOwnJavaToolOptions() {
        val environment = desktopTerminalEnvironment(
            mapOf(JavaToolOptions to "-Xmx256m -XX:+UseSerialGC", InheritedJavaToolOptions to "-Dfile.encoding=UTF-8"),
        )
        assertEquals("-Dfile.encoding=UTF-8", environment[JavaToolOptions])
        assertFalse(InheritedJavaToolOptions in environment)
    }

    @Test
    fun terminalsDoNotInheritAnAgentSessionThatLaunchedMoru() {
        val environment = desktopTerminalEnvironment(
            InheritedAgentSessionMarkers.associateWith { "1" } + mapOf("PATH" to "/usr/bin"),
        )
        InheritedAgentSessionMarkers.forEach { assertFalse(it in environment, it) }
        assertEquals("/usr/bin", environment["PATH"])
    }

    @Test
    fun claudeCodeUserSettingsStay() {
        val environment = desktopTerminalEnvironment(
            mapOf("CLAUDE_CODE_USE_BEDROCK" to "1", "CLAUDE_CODE_MAX_OUTPUT_TOKENS" to "8000"),
        )
        assertEquals("1", environment["CLAUDE_CODE_USE_BEDROCK"])
        assertEquals("8000", environment["CLAUDE_CODE_MAX_OUTPUT_TOKENS"])
    }

    @Test
    fun javaToolOptionsOutsideTheHostAreLeftAlone() {
        val environment = desktopTerminalEnvironment(mapOf(JavaToolOptions to "-Dfile.encoding=UTF-8"))
        assertEquals("-Dfile.encoding=UTF-8", environment[JavaToolOptions])
    }

    @Test
    fun namesTheTerminalAndNeverInheritsAnotherName() {
        val environment = desktopTerminalEnvironment(mapOf(MoruSessionIdVariable to "outer"), sessionKey = "task-a-1")
        assertEquals("task-a-1", environment[MoruSessionIdVariable])
        assertEquals(null, desktopTerminalEnvironment(mapOf(MoruSessionIdVariable to "outer"))[MoruSessionIdVariable])
    }
}
