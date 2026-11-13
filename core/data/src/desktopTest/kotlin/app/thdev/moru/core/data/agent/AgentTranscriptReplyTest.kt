package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentTranscriptReplyTest {
    @Test
    fun claudesLastWordIsItsNewestAssistantText() {
        val home = createTempDirectory("home").toFile()
        val id = "11111111-2222-3333-4444-555555555555"
        File(home, ".claude/projects/-work-app").apply { mkdirs() }.resolve("$id.jsonl").writeText(listOf(
            """{"type":"assistant","message":{"content":[{"type":"text","text":"Looking."}]}}""",
            """{"type":"assistant","message":{"content":[{"type":"tool_use","name":"Bash"}]}}""",
            """{"type":"user","message":{"content":[{"type":"tool_result","content":"ok"}]}}""",
            """{"type":"assistant","message":{"content":[{"type":"text","text":"Done: **14 tests** pass."}]}}""",
        ).joinToString("\n"))
        assertEquals("Done: **14 tests** pass.", agentTranscriptReply(AgentConversationRef("claude", id), home))
    }

    @Test
    fun codexsResultIsTheLastMessageOfItsOwnTurnNotAHelpers() {
        val lines = listOf(
            """{"type":"event_msg","payload":{"type":"task_complete","turn_id":"t1","root_turn_id":"t1","last_agent_message":"Links fixed."}}""",
            """{"type":"event_msg","payload":{"type":"task_complete","turn_id":"h1","root_turn_id":"t1","last_agent_message":"{\"outcome\":\"allow\"}"}}""",
        )
        assertEquals("Links fixed.", codexLastReply(lines))
    }

    @Test
    fun anotherAgentOrAnUnknownConversationHasNoTranscriptResult() {
        val home = createTempDirectory("home").toFile()
        assertNull(agentTranscriptReply(AgentConversationRef("agy", "x"), home))
        assertNull(agentTranscriptReply(AgentConversationRef("claude", null), home))
    }
}
