package app.thdev.moru.app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingDueTest {
    @Test
    fun theWelcomeIsDueForANewUserOrOneWhoLeftItUnfinishedButNotForAnExistingOne() {
        val dir = Files.createTempDirectory("moru-due").toFile()
        try {
            val settings = dir.resolve("settings.json")
            assertTrue(onboardingDue(settings), "no settings yet: a new user")

            settings.writeText("""{"general":{"remember_last_state":true}}""")
            assertFalse(onboardingDue(settings), "settings from before the welcome: an existing user")

            settings.writeText("""{"general":{"onboarding_done":false}}""")
            assertTrue(onboardingDue(settings), "written by a Moru that offers the welcome, which was left unfinished")
        } finally {
            dir.deleteRecursively()
        }
    }
}
