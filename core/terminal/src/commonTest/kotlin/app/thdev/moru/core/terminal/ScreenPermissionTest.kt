package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScreenPermissionTest {
    @Test fun actualProviderChoicesAndCommandContextArePreserved() {
        val codex = screenPermission(listOf("Would you like to run the following command?", "$ rm -rf build",
            "› 1. Yes, proceed (y)", "2. Yes, and don't ask again for this command (p)",
            "3. No, and tell Codex what to do differently (esc)", "Press enter to confirm or esc to cancel"))
        assertNotNull(codex)
        assertEquals("$ rm -rf build", codex.details)
        assertEquals("Yes, and don't ask again for this command (p)", codex.choices[1].label)
        assertEquals("\u001b[B\u001b[B\r", permissionInput(codex, "3"))
        assertEquals("\r", permissionInput(codex, "1"))
        assertNull(permissionInput(codex, "$(touch /tmp/invalid)"))
        val gemini = screenPermission(listOf("Allow execution of: 'npm install'?", "● 1. Allow once", "2. Allow for this session", "3. No, suggest changes (esc)"))
        assertNotNull(gemini)
        assertEquals("Allow for this session", gemini.choices[1].label)
    }

    @Test fun editsAndYesNoPermissionsAreRecognized() {
        val claude = screenPermission(listOf("│ Update Main.kt │", "│ Do you want to make this edit? │", "│ ❯ 1. Yes │", "│ 2. No, and tell Claude what to do differently (esc) │"))
        assertNotNull(claude)
        assertEquals("Update Main.kt", claude.details)
        val yesNo = screenPermission(listOf("Overwrite config.json? (y/N)"))
        assertNotNull(yesNo)
        assertEquals("n\r", permissionInput(yesNo, "n"))
    }

    @Test fun arbitraryQuestionsReturnedShellsAndAmbiguousMenusHaveNoActions() {
        assertNull(screenPermission(listOf("Which model should I use?", "› 1. Alpha", "2. Beta")))
        assertNull(screenPermission(listOf("Which command should I run?", "› 1. rm", "2. pwd")))
        assertNull(screenPermission(listOf("Do you want to proceed?", "› 1. Yes", "2. No", "$ ")))
        assertNull(screenPermission(listOf("Do you want to proceed?", "› 1. Yes", "2. No", "$ echo press enter")))
        assertNull(screenPermission(listOf("Do you want to proceed?", "› 1. Yes", "1. No")))
        assertNull(screenPermission(listOf("Do you want to proceed?", "› 1. Yes, and don't ask again", "for every command in this session", "2. No")))
        assertNull(screenPermission(listOf("Do you want to proceed?", "› 1. Yes, " + "permission scope ".repeat(30), "2. No")))
        assertNull(screenPermission(listOf("Proceed?", "› 1. Yes")))
        assertNull(screenPermission(listOf("$ rm -rf ./build # overwrite? (y/n)")))
        assertNull(screenPermission(listOf("user@host % echo overwrite? (y/n)")))
        assertNull(screenPermission(listOf("echo 'Allow execution? (y/n)'")))
        assertNull(screenPermission(listOf("allow # overwrite? (y/n)")))
    }
}
