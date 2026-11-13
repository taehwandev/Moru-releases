package app.thdev.moru.feature.sessions

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionPermissionChoice
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.SessionPermissions
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SessionPermissionLayoutTest {
    @Test fun shortAndLongProviderChoicesStayInTheSameSidebarColumn() {
        val session = "permission-layout"
        val labels = listOf(
            listOf("Allow once", "Allow for this session", "Deny"),
            listOf("Allow this command once", "Allow npm test and its arguments for the rest of this session", "Deny this command and keep the session open"),
        )
        try {
            listOf(240, 360).forEach { width ->
                listOf(false, true).forEach { dark ->
                    runDesktopComposeUiTest(width = width, height = 520) {
                        fun observe(words: List<String>) {
                            SessionPermissions.observe(session, SessionPermissionPrompt("Allow command?", "npm test",
                                words.mapIndexed { index, label -> SessionPermissionChoice(index.toString(), label) })) { false }
                        }
                        observe(labels.first())
                        setContent {
                            MoruTheme(darkTheme = dark) {
                                SessionRowView(
                                    AgentSessionRow(session, "Run the tests", AgentRuntime.Named("Claude"), SessionActivity.Asking,
                                        null, 0, null, null, true, question = "Allow command?"),
                                    onClick = {},
                                )
                            }
                        }
                        labels.forEachIndexed { variant, words ->
                            runOnIdle { observe(words) }
                            val bounds = words.indices.map {
                                onNodeWithTag("permission-choice-$session-$it").fetchSemanticsNode().boundsInRoot
                            }
                            bounds.zipWithNext().forEach { (first, next) ->
                                assertEquals(first.left, next.left)
                                assertEquals(first.width, next.width)
                                assertTrue(first.bottom < next.top, "Long text cannot move a choice to another column")
                            }
                            assertTrue(bounds.first().width > width - 30, "The full row remains clickable")
                            if (variant == 1 && width == 240) {
                                assertTrue(bounds[1].height > bounds[0].height, "Provider scope wraps instead of being truncated")
                            }
                            System.getenv("MORU_PERMISSION_SHOT_DIR")?.let { folder ->
                                val file = File(folder, "session-$variant-$width-${if (dark) "dark" else "light"}.png")
                                file.parentFile.mkdirs()
                                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
                            }
                        }
                    }
                }
            }
        } finally { SessionPermissions.forget(session) }
    }
}
