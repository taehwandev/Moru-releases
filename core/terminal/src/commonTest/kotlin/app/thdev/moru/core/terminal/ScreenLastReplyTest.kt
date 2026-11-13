package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScreenLastReplyTest {
    @Test
    fun theWholeLastMessageIsTheResultNotTheToolCallsBeforeIt() {
        val rows = listOf(
            "⏺ Bash(./gradlew test)",
            "  ⎿  BUILD SUCCESSFUL",
            "",
            "⏺ 미리보기 링크를 실제 페이지에만 띄우도록 바꿨습니다.",
            "  테스트 14개가 통과했습니다.",
            "",
            "",
            "",
            "  다음은 iOS입니다.",
            "✻ Churned for 2s",
            "────────────────",
            "❯ ",
        )
        assertEquals("미리보기 링크를 실제 페이지에만 띄우도록 바꿨습니다.\n테스트 14개가 통과했습니다.\n\n다음은 iOS입니다.", screenLastReply(rows))
    }

    @Test
    fun aScreenWithoutAnAgentMessageHasNoResult() {
        assertNull(screenLastReply(listOf("$ ls", "build  src", "$ ")))
    }
}
