package app.thdev.moru.core.terminal.external

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalTerminalLauncherTest {
    @Test
    fun shellScriptPreservesWorkspaceAndUsesResolvedShell() {
        val script = externalTerminalShellScript(
            workingDirectory = "/Users/test/Project's App",
            shell = "zsh",
            startupCommand = "printf \"ready\"",
        )

        assertTrue(script.contains("cd '/Users/test/Project'\"'\"'s App'"))
        assertTrue(script.contains("printf \"ready\""))
        assertTrue(script.endsWith("exec '/bin/zsh' '-l'"))
    }

    @Test
    fun macCommandUsesTerminalAppleScriptWithoutLaunchingIt() {
        val command = assertNotNull(
            externalTerminalLaunchCommand(
                session = terminalSession(shell = "bash -lc", startupCommand = "printf \"handoff\""),
                osName = "Mac OS X",
            ),
        )

        assertEquals("osascript", command.first())
        assertTrue(command[2].contains("tell application \"Terminal\" to do script"))
        assertTrue(command[2].contains("exec '/bin/bash' '-lc'"))
        assertTrue(command.last().contains("activate"))
    }

    @Test
    fun linuxCommandUsesAvailableTerminalEmulator() {
        val command = assertNotNull(
            externalTerminalLaunchCommand(
                session = terminalSession(workingDirectory = "/tmp/moru-workspace"),
                osName = "Linux",
                executableExists = { name -> name == "gnome-terminal" },
            ),
        )

        assertEquals("gnome-terminal", command.first())
        assertEquals("--working-directory=/tmp/moru-workspace", command[1])
        assertEquals(listOf("--", "sh", "-lc"), command.subList(2, 5))
        assertTrue(command.last().contains("exec '/bin/zsh' '-l'"))
    }

    @Test
    fun webPreviewSessionsDoNotExposeExternalTerminalCommand() {
        val command = externalTerminalLaunchCommand(
            session = terminalSession(kind = TerminalSessionKind.WEB_PREVIEW),
            osName = "Mac OS X",
        )

        assertNull(command)
    }

    private fun terminalSession(
        kind: TerminalSessionKind = TerminalSessionKind.TERMINAL,
        workingDirectory: String = "/workspace/app",
        shell: String = "zsh",
        startupCommand: String? = null,
    ): TerminalSessionSnapshot {
        return TerminalSessionSnapshot(
            id = "term-1",
            spec = TerminalSessionSpec(
                title = "Terminal 1",
                kind = kind,
                shell = shell,
                workingDirectory = workingDirectory,
                startupCommand = startupCommand,
            ),
            status = TerminalSessionStatus.READY,
        )
    }
}
