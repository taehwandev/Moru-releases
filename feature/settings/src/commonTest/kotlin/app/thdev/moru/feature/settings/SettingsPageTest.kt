package app.thdev.moru.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsPageTest {
    @Test
    fun rememberedAgentCliPageRestoresUpdates() {
        assertEquals(SettingsPage.Updates, SettingsPage.named("AgentClis"))
        assertEquals(SettingsPage.Updates, SettingsPage.named("Updates"))
    }

    @Test
    fun otherPageNamesStillRestoreAndUnknownNamesUseGeneral() {
        SettingsPage.entries.forEach { page -> assertEquals(page, SettingsPage.named(page.name)) }
        assertEquals(SettingsPage.General, SettingsPage.named(null))
        assertEquals(SettingsPage.General, SettingsPage.named("Unknown"))
    }
}
