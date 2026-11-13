package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.session.ConversationTurn
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentTranscriptThreadTest {
    @Test
    fun claudeTurnsPairWhatTheUserTypedWithEverythingTheAgentWroteBack() {
        val lines = listOf(
            """{"type":"user","message":{"role":"user","content":"<command-name>/clear</command-name>"}}""",
            """{"type":"user","message":{"role":"user","content":[{"type":"text","text":"Why do links open twice?"}]}}""",
            """{"type":"assistant","message":{"content":[{"type":"text","text":"Let me look."},{"type":"tool_use","name":"Read"}]}}""",
            """{"type":"user","message":{"content":[{"type":"tool_result","content":"file"}]}}""",
            """{"type":"assistant","isSidechain":true,"message":{"content":[{"type":"text","text":"helper"}]}}""",
            """{"type":"assistant","message":{"content":[{"type":"text","text":"The handler is bound twice."}]}}""",
            """{"type":"user","isMeta":true,"message":{"content":"Caveat: injected"}}""",
            """{"type":"user","message":{"content":"Fix it"}}""",
            """{"type":"assistant","message":{"content":[{"type":"text","text":"**Fixed**."}]}}""",
            "not json",
        )
        assertEquals(
            listOf(
                ConversationTurn("Why do links open twice?", "Let me look.\n\nThe handler is bound twice."),
                ConversationTurn("Fix it", "**Fixed**."),
            ),
            claudeThread(lines),
        )
    }

    @Test
    fun codexTurnsAreTheUsersOwnAndIgnoreReplayedAndHelperTurns() {
        fun user(turn: String, text: String) =
            """{"type":"response_item","payload":{"type":"message","role":"user","content":[{"type":"input_text","text":"$text"}],"internal_chat_message_metadata_passthrough":{"turn_id":"$turn"}}}"""
        fun complete(turn: String, root: String, text: String) =
            """{"type":"event_msg","payload":{"type":"task_complete","turn_id":"$turn","root_turn_id":"$root","last_agent_message":"$text"}}"""
        val lines = listOf(
            user("t1", "# AGENTS.md instructions for /x"),
            user("t1", "Add multi-login"),
            complete("h1", "t1", "helper done"),
            complete("t1", "t1", "Added multi-login."),
            user("t1", "Add multi-login"),
            user("t2", "<environment_context>"),
            user("t2", "Ship it"),
            complete("t2", "t2", "Shipped."),
        )
        assertEquals(
            listOf(ConversationTurn("Add multi-login", "Added multi-login."), ConversationTurn("Ship it", "Shipped.")),
            codexThread(lines),
        )
    }
}
