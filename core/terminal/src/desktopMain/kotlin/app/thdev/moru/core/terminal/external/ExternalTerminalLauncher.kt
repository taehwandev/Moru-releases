package app.thdev.moru.core.terminal.external

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.desktopTerminalShellCommand
import java.io.File

fun openExternalTerminalForSession(session: TerminalSessionSnapshot): ExternalTerminalLaunchResult {
    val command = externalTerminalLaunchCommand(session)
        ?: return ExternalTerminalLaunchResult(
            opened = false,
            message = "External terminal fallback is not supported on this desktop environment.",
        )

    return runCatching {
        ProcessBuilder(command).apply {
            session.spec.workingDirectory.existingDirectoryOrNull()?.let { directory(it) }
        }.start()
        ExternalTerminalLaunchResult(opened = true, message = "External terminal opened.")
    }.getOrElse { error ->
        ExternalTerminalLaunchResult(
            opened = false,
            message = error.message ?: "External terminal launch failed.",
        )
    }
}

internal fun externalTerminalLaunchCommand(
    session: TerminalSessionSnapshot,
    osName: String = System.getProperty("os.name"),
    executableExists: (String) -> Boolean = ::executableExistsOnPath,
): List<String>? {
    if (session.spec.kind != TerminalSessionKind.TERMINAL) return null

    val workingDirectory = session.spec.workingDirectory.resolvedWorkingDirectory()
    val shellScript = externalTerminalShellScript(
        workingDirectory = workingDirectory.absolutePath,
        shell = session.spec.shell,
        startupCommand = session.spec.startupCommand,
    )
    val normalizedOsName = osName.lowercase()

    return when {
        normalizedOsName.contains("mac") || normalizedOsName.contains("darwin") -> listOf(
            "osascript",
            "-e",
            "tell application \"Terminal\" to do script ${shellScript.appleScriptStringLiteral()}",
            "-e",
            "tell application \"Terminal\" to activate",
        )
        normalizedOsName.contains("win") -> listOf(
            "cmd.exe",
            "/c",
            "start",
            "Moru Terminal",
            "cmd.exe",
            "/K",
            "cd /d ${workingDirectory.absolutePath.windowsCommandQuoted()}",
        )
        else -> linuxTerminalLaunchCommand(
            workingDirectory = workingDirectory,
            shellScript = shellScript,
            executableExists = executableExists,
        )
    }
}

internal fun externalTerminalShellScript(
    workingDirectory: String,
    shell: String,
    startupCommand: String?,
): String {
    val shellCommand = desktopTerminalShellCommand(shell).joinToString(" ") { value ->
        value.shellSingleQuoted()
    }
    val commands = buildList {
        add("cd ${File(workingDirectory).absolutePath.shellSingleQuoted()}")
        startupCommand
            ?.takeIf { command -> command.isNotBlank() }
            ?.let { command -> add(command.normalizedShellSnippet()) }
        add("exec $shellCommand")
    }

    return commands.joinToString("; ")
}

private fun linuxTerminalLaunchCommand(
    workingDirectory: File,
    shellScript: String,
    executableExists: (String) -> Boolean,
): List<String>? {
    val cwd = workingDirectory.absolutePath

    return when {
        executableExists("gnome-terminal") -> listOf("gnome-terminal", "--working-directory=$cwd", "--", "sh", "-lc", shellScript)
        executableExists("konsole") -> listOf("konsole", "--workdir", cwd, "-e", "sh", "-lc", shellScript)
        executableExists("xfce4-terminal") -> listOf(
            "xfce4-terminal",
            "--working-directory",
            cwd,
            "--command",
            "sh -lc ${shellScript.shellSingleQuoted()}",
        )
        executableExists("x-terminal-emulator") -> listOf("x-terminal-emulator", "-e", "sh", "-lc", shellScript)
        executableExists("xterm") -> listOf("xterm", "-e", "sh", "-lc", shellScript)
        else -> null
    }
}

private fun String.resolvedWorkingDirectory(): File {
    return takeIf { path -> path.isNotBlank() }
        ?.let { path -> File(path).absoluteFile }
        ?: File(System.getProperty("user.home")).absoluteFile
}

private fun String.existingDirectoryOrNull(): File? {
    return resolvedWorkingDirectory().takeIf { file -> file.isDirectory }
}

private fun String.normalizedShellSnippet(): String {
    return lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotEmpty() }
        .joinToString("; ")
}

private fun String.shellSingleQuoted(): String {
    return "'" + replace("'", "'\"'\"'") + "'"
}

private fun String.appleScriptStringLiteral(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private fun String.windowsCommandQuoted(): String {
    return "\"" + replace("\"", "\\\"") + "\""
}

private fun executableExistsOnPath(command: String): Boolean {
    return System.getenv("PATH")
        .orEmpty()
        .split(File.pathSeparator)
        .filter { path -> path.isNotBlank() }
        .any { path -> File(path, command).canExecute() }
}
