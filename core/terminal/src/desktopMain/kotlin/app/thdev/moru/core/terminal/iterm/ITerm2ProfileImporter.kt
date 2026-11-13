package app.thdev.moru.core.terminal.iterm

import app.thdev.moru.core.model.terminal.TerminalProfileImport
import app.thdev.moru.core.terminal.BoundedProcessRunner
import java.awt.GraphicsEnvironment
import java.io.File

/** Reads the iTerm2 default profile on this Mac. Blocking: call it off the UI thread. */
object ITerm2ProfileImporter {
    private val DefaultPreferences = File(System.getProperty("user.home"), "Library/Preferences/com.googlecode.iterm2.plist")

    fun import(preferences: File = DefaultPreferences): TerminalProfileImport {
        if (!preferences.isFile) return TerminalProfileImport.NotFound
        // iTerm2 saves a binary plist; plutil turns it into XML without touching the file.
        val output = BoundedProcessRunner.run(
            listOf("/usr/bin/plutil", "-convert", "xml1", "-o", "-", preferences.absolutePath),
            timeoutMillis = 10_000,
        ) ?: return TerminalProfileImport.Failed("plutil did not finish")
        if (output.exitCode != 0) return TerminalProfileImport.Failed("plutil exited with ${output.exitCode}")
        val root = runCatching { PlistXml.parse(output.text()) }.getOrElse { error ->
            return TerminalProfileImport.Failed(error.message ?: "unreadable preferences")
        }
        @Suppress("UNCHECKED_CAST")
        val preferencesMap = root as? Map<String, Any?> ?: return TerminalProfileImport.Failed("unexpected preferences")
        return mapITerm2Preferences(preferencesMap, ::installedFontFamily)
    }

    /** The family of the installed font with this PostScript (or family) name. */
    private fun installedFontFamily(name: String): String? {
        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        environment.availableFontFamilyNames.firstOrNull { it.equals(name, ignoreCase = true) }?.let { return it }
        return environment.allFonts.firstOrNull { font ->
            font.psName.equals(name, ignoreCase = true) || font.fontName.equals(name, ignoreCase = true)
        }?.family
    }
}
