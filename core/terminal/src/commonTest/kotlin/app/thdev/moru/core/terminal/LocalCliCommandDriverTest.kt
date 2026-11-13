package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.LocalCliCommandDriver.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalCliCommandDriverTest {
    private val ready = listOf("OpenAI Codex", "› Ask Codex to do anything")
    private val echo = listOf("OpenAI Codex", "› /usage  view account usage or use a usage limit reset", "› /usage")
    private val menu = listOf("Usage", "Account usage and resets.", "› 1. View analytics", "2. Redeem reset")

    @Test fun `loading and trust menus receive no input`() {
        val driver = LocalCliCommandDriver("codex", "/usage")
        assertEquals(Step.Wait, driver.advance(listOf("OpenAI Codex", "loading", "› Ask Codex to do anything"), 0))
        assertEquals(Step.Wait, driver.advance(listOf("OpenAI Codex", "Do you trust this folder?", "› 1. Yes"), 100))
        assertEquals(Step.TypeCommand, driver.advance(ready, 200))
    }

    @Test fun `command is echoed and recognized before a separate Enter and no ticket is chosen`() {
        val driver = LocalCliCommandDriver("codex", "/usage")
        assertEquals(Step.TypeCommand, driver.advance(ready, 0))
        assertEquals(Step.Wait, driver.advance(ready, 100))
        assertEquals(Step.Wait, driver.advance(echo, 200))
        assertEquals(Step.Wait, driver.advance(echo, 399))
        assertEquals(Step.PressEnter, driver.advance(echo, 400))
        assertEquals(Step.Wait, driver.advance(echo, 500))
        assertEquals(Step.Complete, driver.advance(menu, 600))
        assertEquals(Step.Complete, driver.advance(menu, 700))
    }

    @Test fun `unknown command or cleared echo never gets Enter`() {
        val driver = LocalCliCommandDriver("codex", "/usage")
        driver.advance(ready, 0)
        assertEquals(Step.Wait, driver.advance(listOf("OpenAI Codex", "› /usage"), 100))
        driver.advance(echo, 200)
        assertEquals(Step.Wait, driver.advance(ready, 450))
        assertEquals(Step.Wait, driver.advance(echo, 500))
        assertEquals(Step.PressEnter, driver.advance(echo, 700))
    }

    @Test fun `local command driver refuses arbitrary user prompts`() {
        assertFailsWith<IllegalArgumentException> { LocalCliCommandDriver("codex", "run a task") }
        assertFailsWith<IllegalArgumentException> { LocalCliCommandDriver("other", "/usage") }
    }
}
