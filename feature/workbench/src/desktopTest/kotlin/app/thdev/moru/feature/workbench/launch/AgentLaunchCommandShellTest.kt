package app.thdev.moru.feature.workbench.launch

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs the generated command through a real POSIX shell with a stand-in agent that echoes its argument. */
class AgentLaunchCommandShellTest {
    private fun argumentSeenByAgent(prompt: String): String {
        val script = "claude() { test \"\$1\" = --session-id || exit 42; printf '%s' \"\$3\"; }; " + agentLaunchCommand(AgentRuntime.Claude, prompt)
        val process = ProcessBuilder("sh", "-c", script).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(10, TimeUnit.SECONDS)) { "shell did not finish" }
        return output
    }

    @Test
    fun theAgentReceivesTheExactPromptDespiteShellMetacharacters() {
        val prompt = "don't \$HOME `whoami` \"quoted\" ; rm -rf x\nsecond line & more"

        assertEquals(prompt, argumentSeenByAgent(prompt))
    }

    @Test
    fun aKoreanPromptSurvivesTheShell() {
        val prompt = "로그인 실패 시 에러 메시지가 안 보이는 문제 수정"

        assertEquals(prompt, argumentSeenByAgent(prompt))
    }

    @Test
    fun codexReceivesTheServerOptionAndTheExactPromptAsSeparateArguments() {
        val prompt = "don't expand \$HOME `whoami`\n로그인 오류 수정"
        val script = "codex() { test \"\$1\" = --no-daemon || exit 42; test \"\$#\" = 2 || exit 43; printf '%s' \"\$2\"; }; " +
            agentLaunchCommand(AgentRuntime.Codex, prompt)
        val process = ProcessBuilder("sh", "-c", script).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()

        check(process.waitFor(10, TimeUnit.SECONDS)) { "shell did not finish" }
        assertEquals(0, process.exitValue())
        assertEquals(prompt, output)
    }

    @Test
    fun codexResumeBypassesTheSharedServerAndFailureDoesNotStartAFreshConversation() {
        val script = "codex() { test \"\$1\" = --no-daemon || exit 42; shift; " +
            "if test \"\$1\" = resume; then test \"\$2\" = x9 || exit 43; printf 'resume\\n'; return 1; fi; " +
            "test \"\$#\" = 0 || exit 44; printf 'fresh\\n'; }; " + AgentConversation.resumeCommand(AgentRuntime.Codex, "x9")
        val process = ProcessBuilder("sh", "-c", script).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()

        check(process.waitFor(10, TimeUnit.SECONDS)) { "shell did not finish" }
        assertEquals(1, process.exitValue())
        assertEquals("resume\n", output)
    }
}
