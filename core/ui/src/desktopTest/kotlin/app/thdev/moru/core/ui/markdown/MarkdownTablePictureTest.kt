package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Draws a review-style table, short and long columns side by side, in both themes and a narrow width. With
 * `MORU_MARKDOWN_SHOT_DIR` set it keeps the pictures, so the layout can be looked at.
 */
@OptIn(ExperimentalTestApi::class)
class MarkdownTablePictureTest {
    private val table = """
        | # | 위치 | 문제 | 수정 크기 |
        |---|---|---|---|
        | 1 | `MarkdownLiveEditor.kt:82` | 마크다운 편집기에서 키를 누르거나 커서만 움직여도 문서 전체를 다시 처리하고 전체를 다시 배치합니다. 큰 문서에서는 입력이 느려질 수 있습니다. | 중간 |
        | 2 | `LabFilePreviewPanel.kt:107` | 리뷰 모드로 마크다운 파일을 열면 블록마다 파일 전체를 처음부터 다시 훑습니다. | 작음 |
        | 3 | `CodeFileView.kt` | 최대 512KB 파일을 하나의 텍스트로 통째로 그리고, 줄 번호도 최대 4,000개를 한 번에 만듭니다. | 큼. 화면에 보이는 줄만 그리게 바꾸면 여러 줄 드래그 선택이 달라질 수 있습니다 |
    """.trimIndent()

    @Test
    fun aTableSizesItsColumnsToWhatTheyHold() {
        draw("table-dark", dark = true, width = 900) {
            // The number column stays narrow and the prose column takes the room.
            val number = onNodeWithText("#").getBoundsInRoot()
            val problem = onNodeWithText("문제").getBoundsInRoot()
            assertTrue(number.right - number.left < 80.dp, "number column ${number.right - number.left}")
            assertTrue(problem.right - problem.left > 300.dp, "problem column ${problem.right - problem.left}")
        }
        draw("table-light", dark = false, width = 900)
        draw("table-narrow-dark", dark = true, width = 420)
    }

    @Test
    fun aTableThatFitsKeepsItsColumnsAndOneThatDoesNotSharesTheRoomByText() {
        val columns = listOf(MarkdownTableColumn(20, 30), MarkdownTableColumn(100, 600), MarkdownTableColumn(40, 200))
        assertEquals(listOf(30, 600, 200), markdownTableWidths(columns, 1_000))
        // 160 at their longest words, the other 240 shared by how much text each column has left to show.
        assertEquals(listOf(23, 279, 98), markdownTableWidths(columns, 400))
        // Too narrow for whole words: each starts at the floor and the room goes by text, most to the longest column.
        assertEquals(listOf(20, 47, 33), markdownTableWidths(columns, 100, floor = 25))
        // Too narrow even at the floor: the table keeps the floor and scrolls.
        assertEquals(listOf(20, 25, 25), markdownTableWidths(columns, 60, floor = 25))
    }

    private fun draw(name: String, dark: Boolean, width: Int, check: androidx.compose.ui.test.ComposeUiTest.() -> Unit = {}) =
        runDesktopComposeUiTest(width = width, height = 640) {
            setContent {
                MoruTheme(darkTheme = dark) {
                    MarkdownReadingView(table, onOpenWikiLink = {}, modifier = Modifier.ckSurface(MoruSurfaceRole.Page).padding(16.dp), padding = 0.dp)
                }
            }
            onNodeWithText("수정 크기").assertExists()
            check()
            System.getenv("MORU_MARKDOWN_SHOT_DIR")?.let { folder ->
                waitForIdle()
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(folder, "$name.png"))
            }
        }
}
